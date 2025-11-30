package com.familytree.service;

import com.familytree.dto.gedcom.GedcomImportResult;
import com.familytree.model.*;
import com.familytree.repository.FamilyTreeRepository;
import com.familytree.repository.IndividualRepository;
import com.familytree.repository.RelationshipRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.gedcom4j.model.*;
import org.gedcom4j.model.enumerations.IndividualEventType;
import org.gedcom4j.model.enumerations.FamilyEventType;
import org.gedcom4j.model.enumerations.SupportedVersion;
import org.gedcom4j.exception.GedcomWriterException;
import org.gedcom4j.exception.WriterCancelledException;
import org.gedcom4j.parser.GedcomParser;
import org.gedcom4j.writer.GedcomWriter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

/**
 * Service for importing and exporting GEDCOM files (gedcom4j 4.0.1 compatible)
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class GedcomService {

    private final FamilyTreeRepository treeRepository;
    private final IndividualRepository individualRepository;
    private final RelationshipRepository relationshipRepository;
    private final PermissionService permissionService;

    private static final int MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
    private static final DateTimeFormatter[] DATE_FORMATTERS = {
            DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("MMM yyyy", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("yyyy")
    };

    /**
     * Import a GEDCOM file into a family tree
     */
    @Transactional
    public GedcomImportResult importGedcom(MultipartFile file, UUID treeId, UUID userId) {
        long startTime = System.currentTimeMillis();

        GedcomImportResult result = GedcomImportResult.builder()
                .treeId(treeId.toString())
                .build();

        try {
            // Validate file
            if (file.isEmpty()) {
                result.addError("File is empty");
                result.setSuccess(false);
                return result;
            }

            if (file.getSize() > MAX_FILE_SIZE) {
                result.addError("File size exceeds 10MB limit");
                result.setSuccess(false);
                return result;
            }

            String filename = file.getOriginalFilename();
            if (filename == null || !filename.toLowerCase().endsWith(".ged")) {
                result.addError("File must have .ged extension");
                result.setSuccess(false);
                return result;
            }

            // Verify tree ownership
            FamilyTree tree = treeRepository.findById(treeId)
                    .orElseThrow(() -> new RuntimeException("Tree not found"));

            if (!permissionService.canModifyTree(userId, treeId)) {
                result.addError("You don't have permission to modify this tree");
                result.setSuccess(false);
                return result;
            }

            // Parse GEDCOM file using BufferedInputStream
            log.info("Parsing GEDCOM file: {}", filename);
            GedcomParser parser = new GedcomParser();
            BufferedInputStream bis = new BufferedInputStream(file.getInputStream());
            parser.load(bis);

            // Check for parsing errors/warnings using getters
            List<String> errors = parser.getErrors();
            List<String> warnings = parser.getWarnings();

            if (!errors.isEmpty() || !warnings.isEmpty()) {
                errors.forEach(error -> {
                    log.warn("GEDCOM parse error: {}", error);
                    result.addError("Parse error: " + error);
                });
                warnings.forEach(warning -> {
                    log.info("GEDCOM parse warning: {}", warning);
                    result.addWarning("Warning: " + warning);
                });
            }

            Gedcom gedcom = parser.getGedcom();
            if (gedcom == null) {
                result.addError("Failed to parse GEDCOM file");
                result.setSuccess(false);
                return result;
            }

            // Process individuals
            Map<String, org.gedcom4j.model.Individual> gedcomIndividuals = gedcom.getIndividuals();
            log.info("Processing {} individuals", gedcomIndividuals.size());
            Map<String, com.familytree.model.Individual> individualMap = processIndividuals(gedcomIndividuals, tree, result);
            result.setIndividualsImported(individualMap.size());

            // Process families (relationships)
            Map<String, Family> gedcomFamilies = gedcom.getFamilies();
            log.info("Processing {} families", gedcomFamilies.size());
            List<Relationship> relationships = processFamilies(gedcomFamilies, individualMap, tree, result);
            result.setRelationshipsImported(relationships.size());

            result.setSuccess(true);
            log.info("GEDCOM import completed: {} individuals, {} relationships",
                    individualMap.size(), relationships.size());

        } catch (IOException e) {
            log.error("IO error reading GEDCOM file", e);
            result.addError("Failed to read file: " + e.getMessage());
            result.setSuccess(false);
        } catch (Exception e) {
            log.error("Unexpected error during GEDCOM import", e);
            result.addError("Unexpected error: " + e.getMessage());
            result.setSuccess(false);
        } finally {
            result.setProcessingTimeMs(System.currentTimeMillis() - startTime);
        }

        return result;
    }

    /**
     * Process GEDCOM individuals and save to database
     */
    private Map<String, com.familytree.model.Individual> processIndividuals(
            Map<String, org.gedcom4j.model.Individual> gedcomIndividuals,
            FamilyTree tree, GedcomImportResult result) {

        Map<String, com.familytree.model.Individual> individualMap = new HashMap<>();

        for (Map.Entry<String, org.gedcom4j.model.Individual> entry : gedcomIndividuals.entrySet()) {
            String xref = entry.getKey();
            org.gedcom4j.model.Individual gedcomIndividual = entry.getValue();

            try {
                com.familytree.model.Individual individual = com.familytree.model.Individual.builder()
                        .tree(tree)
                        .build();

                // Extract name using getter
                List<PersonalName> names = gedcomIndividual.getNames();
                if (names != null && !names.isEmpty()) {
                    PersonalName name = names.get(0);
                    
                    // Try to get surname first
                    StringWithCustomFacts surname = name.getSurname();
                    if (surname != null) {
                        individual.setSurname(surname.getValue());
                    }
                    
                    // Get given name - may contain middle name for Vietnamese names
                    StringWithCustomFacts givenName = name.getGivenName();
                    if (givenName != null && givenName.getValue() != null) {
                        String givenNameValue = givenName.getValue().trim();
                        
                        // For Vietnamese names, given name may contain "MiddleName GivenName"
                        // e.g., "Nam Hưng" where "Nam" is middle name and "Hưng" is given name
                        String[] nameParts = givenNameValue.split("\\s+");
                        if (nameParts.length > 1) {
                            // Last part is the given name, rest is middle name
                            individual.setGivenName(nameParts[nameParts.length - 1]);
                            StringBuilder middleName = new StringBuilder();
                            for (int i = 0; i < nameParts.length - 1; i++) {
                                if (middleName.length() > 0) middleName.append(" ");
                                middleName.append(nameParts[i]);
                            }
                            individual.setMiddleName(middleName.toString());
                        } else {
                            // Single part - just the given name
                            individual.setGivenName(givenNameValue);
                        }
                    }
                    
                    // Also try to parse from the basic name format "Surname MiddleName /GivenName/"
                    // or "Surname /MiddleName GivenName/"
                    if (individual.getGivenName() == null || individual.getSurname() == null) {
                        String basicName = name.getBasic();
                        if (basicName != null && !basicName.isEmpty()) {
                            parseVietnameseNameFromBasic(individual, basicName);
                        }
                    }
                }

                // Extract gender using getter
                StringWithCustomFacts sex = gedcomIndividual.getSex();
                if (sex != null && sex.getValue() != null) {
                    String sexValue = sex.getValue().toUpperCase();
                    if ("M".equals(sexValue)) {
                        individual.setGender(Gender.MALE);
                    } else if ("F".equals(sexValue)) {
                        individual.setGender(Gender.FEMALE);
                    } else {
                        individual.setGender(Gender.OTHER);
                    }
                }

                // Extract events using getter
                List<IndividualEvent> events = gedcomIndividual.getEvents();
                if (events != null) {
                    for (IndividualEvent event : events) {
                        IndividualEventType eventType = event.getType();
                        if (eventType == IndividualEventType.BIRTH) {
                            StringWithCustomFacts date = event.getDate();
                            if (date != null && date.getValue() != null) {
                                LocalDate birthDate = parseGedcomDate(date.getValue());
                                individual.setBirthDate(birthDate);
                            }
                            Place place = event.getPlace();
                            if (place != null && place.getPlaceName() != null) {
                                individual.setBirthPlace(place.getPlaceName());
                            }
                        } else if (eventType == IndividualEventType.DEATH) {
                            StringWithCustomFacts date = event.getDate();
                            if (date != null && date.getValue() != null) {
                                LocalDate deathDate = parseGedcomDate(date.getValue());
                                individual.setDeathDate(deathDate);
                            }
                            Place place = event.getPlace();
                            if (place != null && place.getPlaceName() != null) {
                                individual.setDeathPlace(place.getPlaceName());
                            }
                        }
                    }
                }

                // Save individual
                individual = individualRepository.save(individual);
                individualMap.put(xref, individual);

            } catch (Exception e) {
                log.error("Error processing individual {}: {}", xref, e.getMessage());
                result.addWarning("Failed to import individual " + xref + ": " + e.getMessage());
            }
        }

        return individualMap;
    }

    /**
     * Process GEDCOM families and create relationships
     */
    private List<Relationship> processFamilies(
            Map<String, Family> gedcomFamilies,
            Map<String, com.familytree.model.Individual> individualMap,
            FamilyTree tree, GedcomImportResult result) {

        List<Relationship> relationships = new ArrayList<>();

        for (Map.Entry<String, Family> entry : gedcomFamilies.entrySet()) {
            String xref = entry.getKey();
            Family family = entry.getValue();

            try {
                // Get spouses using getters - gedcom4j 4.x returns IndividualReference
                com.familytree.model.Individual husband = null;
                com.familytree.model.Individual wife = null;

                IndividualReference husbandRef = family.getHusband();
                if (husbandRef != null && husbandRef.getIndividual() != null) {
                    husband = individualMap.get(husbandRef.getIndividual().getXref());
                }

                IndividualReference wifeRef = family.getWife();
                if (wifeRef != null && wifeRef.getIndividual() != null) {
                    wife = individualMap.get(wifeRef.getIndividual().getXref());
                }

                // Create spouse relationship
                if (husband != null && wife != null) {
                    Relationship spouseRel = Relationship.builder()
                            .tree(tree)
                            .individual1(husband)
                            .individual2(wife)
                            .type(RelationshipType.SPOUSE)
                            .build();

                    // Extract marriage event
                    List<FamilyEvent> familyEvents = family.getEvents();
                    if (familyEvents != null) {
                        for (FamilyEvent event : familyEvents) {
                            if (event.getType() == FamilyEventType.MARRIAGE) {
                                StringWithCustomFacts date = event.getDate();
                                if (date != null && date.getValue() != null) {
                                    LocalDate marriageDate = parseGedcomDate(date.getValue());
                                    spouseRel.setStartDate(marriageDate);
                                }
                            }
                        }
                    }

                    spouseRel = relationshipRepository.save(spouseRel);
                    relationships.add(spouseRel);
                }

                // Create parent-child relationships - gedcom4j 4.x returns List<IndividualReference>
                List<IndividualReference> children = family.getChildren();
                if (children != null) {
                    for (IndividualReference childRef : children) {
                        if (childRef.getIndividual() == null) continue;
                        com.familytree.model.Individual child = individualMap.get(childRef.getIndividual().getXref());
                        if (child != null) {
                            // Father-child relationship
                            if (husband != null) {
                                Relationship fatherRel = Relationship.builder()
                                        .tree(tree)
                                        .individual1(husband)
                                        .individual2(child)
                                        .type(RelationshipType.PARENT_CHILD)
                                        .build();
                                fatherRel = relationshipRepository.save(fatherRel);
                                relationships.add(fatherRel);
                            }

                            // Mother-child relationship
                            if (wife != null) {
                                Relationship motherRel = Relationship.builder()
                                        .tree(tree)
                                        .individual1(wife)
                                        .individual2(child)
                                        .type(RelationshipType.PARENT_CHILD)
                                        .build();
                                motherRel = relationshipRepository.save(motherRel);
                                relationships.add(motherRel);
                            }
                        }
                    }
                }

            } catch (Exception e) {
                log.error("Error processing family {}: {}", xref, e.getMessage());
                result.addWarning("Failed to import family " + xref + ": " + e.getMessage());
            }
        }

        return relationships;
    }

    /**
     * Export a family tree to GEDCOM format
     */
    @Transactional(readOnly = true)
    public byte[] exportGedcom(UUID treeId, UUID userId) throws IOException, GedcomWriterException, WriterCancelledException {
        // Verify tree access
        FamilyTree tree = treeRepository.findById(treeId)
                .orElseThrow(() -> new RuntimeException("Tree not found"));

        if (!permissionService.canViewTree(userId, treeId)) {
            throw new RuntimeException("You don't have permission to view this tree");
        }

        // Create GEDCOM structure
        Gedcom gedcom = new Gedcom();
        
        // Create header
        Header header = new Header();
        GedcomVersion gedcomVersion = new GedcomVersion();
        gedcomVersion.setVersionNumber(SupportedVersion.V5_5_1);
        header.setGedcomVersion(gedcomVersion);

        // Source system
        SourceSystem sourceSystem = new SourceSystem();
        sourceSystem.setSystemId("FamilyTreeManager");
        header.setSourceSystem(sourceSystem);
        
        // Character set
        CharacterSet characterSet = new CharacterSet();
        characterSet.setCharacterSetName(new StringWithCustomFacts("UTF-8"));
        header.setCharacterSet(characterSet);
        
        gedcom.setHeader(header);
        
        // Create submitter (required for valid GEDCOM)
        Submitter submitter = new Submitter();
        submitter.setXref("@SUBM@");
        submitter.setName(new StringWithCustomFacts(tree.getName() != null ? tree.getName() : "Family Tree Manager"));
        gedcom.getSubmitters().put("@SUBM@", submitter);
        
        // Link submitter to header
        SubmitterReference submitterRef = new SubmitterReference();
        submitterRef.setSubmitter(submitter);
        header.setSubmitterReference(submitterRef);
        
        // Set trailer
        gedcom.setTrailer(new Trailer());

        // Get all individuals and relationships (with individuals eagerly loaded)
        List<com.familytree.model.Individual> individuals = individualRepository.findByTreeId(treeId);
        List<Relationship> relationships = relationshipRepository.findByTreeIdWithIndividuals(treeId);
        
        log.info("Exporting tree {} with {} individuals and {} relationships", 
                treeId, individuals.size(), relationships.size());
        
        // Log relationship types for debugging
        long spouseCount = relationships.stream()
                .filter(r -> r.getType() == RelationshipType.SPOUSE || r.getType() == RelationshipType.PARTNER)
                .count();
        long parentChildCount = relationships.stream()
                .filter(r -> r.getType() == RelationshipType.PARENT_CHILD)
                .count();
        log.info("Found {} spouse relationships and {} parent-child relationships", spouseCount, parentChildCount);

        // Map our individuals to GEDCOM individuals
        Map<UUID, org.gedcom4j.model.Individual> gedcomIndividualMap = new HashMap<>();

        for (com.familytree.model.Individual ind : individuals) {
            String xref = "@I" + ind.getId().toString().replace("-", "").substring(0, 8) + "@";
            org.gedcom4j.model.Individual gedcomInd = new org.gedcom4j.model.Individual();
            gedcomInd.setXref(xref);

            // Name - Vietnamese format: Surname MiddleName /GivenName/
            // GEDCOM format: GivenName(GIVN) includes middle name, Surname(SURN) separate
            // Note: In this system, "suffix" field is used for Vietnamese middle name (tên đệm)
            PersonalName name = new PersonalName();
            
            // Build full given name for GEDCOM (middle name + given name)
            // Priority: middleName field, then suffix field (for backward compatibility)
            StringBuilder fullGivenName = new StringBuilder();
            
            // First try middleName field
            if (ind.getMiddleName() != null && !ind.getMiddleName().isEmpty()) {
                fullGivenName.append(ind.getMiddleName());
            } 
            // Fallback to suffix field (used for middle name in Vietnamese)
            else if (ind.getSuffix() != null && !ind.getSuffix().isEmpty()) {
                fullGivenName.append(ind.getSuffix());
            }
            
            if (ind.getGivenName() != null && !ind.getGivenName().isEmpty()) {
                if (fullGivenName.length() > 0) fullGivenName.append(" ");
                fullGivenName.append(ind.getGivenName());
            }
            
            // Standard GEDCOM basic format: Given /Surname/
            String basicName = "";
            if (fullGivenName.length() > 0) {
                basicName = fullGivenName.toString();
            }
            if (ind.getSurname() != null && !ind.getSurname().isEmpty()) {
                if (!basicName.isEmpty()) basicName += " ";
                basicName += "/" + ind.getSurname() + "/";
            }
            name.setBasic(basicName);

            // Set structured name parts
            if (fullGivenName.length() > 0) {
                name.setGivenName(new StringWithCustomFacts(fullGivenName.toString()));
            }
            if (ind.getSurname() != null && !ind.getSurname().isEmpty()) {
                name.setSurname(new StringWithCustomFacts(ind.getSurname()));
            }

            gedcomInd.getNames(true).add(name);

            // Gender
            if (ind.getGender() != null) {
                gedcomInd.setSex(new StringWithCustomFacts(
                        ind.getGender() == Gender.MALE ? "M" :
                        ind.getGender() == Gender.FEMALE ? "F" : "U"
                ));
            }

            // Birth event
            if (ind.getBirthDate() != null || ind.getBirthPlace() != null) {
                IndividualEvent birth = new IndividualEvent();
                birth.setType(IndividualEventType.BIRTH);
                if (ind.getBirthDate() != null) {
                    birth.setDate(new StringWithCustomFacts(formatGedcomDate(ind.getBirthDate())));
                }
                if (ind.getBirthPlace() != null) {
                    Place place = new Place();
                    place.setPlaceName(ind.getBirthPlace());
                    birth.setPlace(place);
                }
                gedcomInd.getEvents(true).add(birth);
            }

            // Death event
            if (ind.getDeathDate() != null || ind.getDeathPlace() != null) {
                IndividualEvent death = new IndividualEvent();
                death.setType(IndividualEventType.DEATH);
                if (ind.getDeathDate() != null) {
                    death.setDate(new StringWithCustomFacts(formatGedcomDate(ind.getDeathDate())));
                }
                if (ind.getDeathPlace() != null) {
                    Place place = new Place();
                    place.setPlaceName(ind.getDeathPlace());
                    death.setPlace(place);
                }
                gedcomInd.getEvents(true).add(death);
            }

            gedcom.getIndividuals().put(xref, gedcomInd);
            gedcomIndividualMap.put(ind.getId(), gedcomInd);
        }

        // Build a lookup map from individual entity to their UUID (for consistent comparison)
        Map<UUID, com.familytree.model.Individual> individualEntityMap = new HashMap<>();
        for (com.familytree.model.Individual ind : individuals) {
            individualEntityMap.put(ind.getId(), ind);
        }
        
        // Process relationships and create families
        Map<String, Family> familyMap = new HashMap<>();
        // Map to track which individuals are already in a family as spouse
        Map<UUID, String> individualToFamilyMap = new HashMap<>();
        // Track children added to each family to prevent duplicates
        Map<String, Set<String>> familyChildrenSet = new HashMap<>();
        // Map to store Family objects by spouse UUIDs for better lookup
        Map<UUID, Family> spouseToFamilyObjMap = new HashMap<>();

        // First pass: Create families from SPOUSE relationships
        for (Relationship rel : relationships) {
            if (rel.getType() == RelationshipType.SPOUSE || rel.getType() == RelationshipType.PARTNER) {
                // Get individual IDs - use safe extraction
                UUID ind1Id = extractIndividualId(rel.getIndividual1(), individuals);
                UUID ind2Id = extractIndividualId(rel.getIndividual2(), individuals);
                
                if (ind1Id == null || ind2Id == null) {
                    log.warn("SPOUSE relationship {} has null individual IDs: ind1={}, ind2={}", 
                            rel.getId(), ind1Id, ind2Id);
                    continue;
                }
                
                String familyId = "@F" + rel.getId().toString().replace("-", "").substring(0, 8) + "@";
                Family family = new Family();
                family.setXref(familyId);

                org.gedcom4j.model.Individual gedcomInd1 = gedcomIndividualMap.get(ind1Id);
                org.gedcom4j.model.Individual gedcomInd2 = gedcomIndividualMap.get(ind2Id);
                
                if (gedcomInd1 == null || gedcomInd2 == null) {
                    log.warn("SPOUSE relationship {} - could not find GEDCOM individuals: gedcomInd1={}, gedcomInd2={}", 
                            rel.getId(), gedcomInd1 != null, gedcomInd2 != null);
                    continue;
                }

                // Determine husband and wife based on gender from our entity map
                org.gedcom4j.model.Individual husband = null;
                org.gedcom4j.model.Individual wife = null;
                
                com.familytree.model.Individual entity1 = individualEntityMap.get(ind1Id);
                com.familytree.model.Individual entity2 = individualEntityMap.get(ind2Id);
                
                Gender gender1 = entity1 != null ? entity1.getGender() : null;
                Gender gender2 = entity2 != null ? entity2.getGender() : null;
                
                if (gender1 == Gender.MALE && gender2 == Gender.FEMALE) {
                    husband = gedcomInd1;
                    wife = gedcomInd2;
                } else if (gender1 == Gender.FEMALE && gender2 == Gender.MALE) {
                    husband = gedcomInd2;
                    wife = gedcomInd1;
                } else if (gender1 == Gender.MALE) {
                    husband = gedcomInd1;
                    wife = gedcomInd2;
                } else if (gender2 == Gender.MALE) {
                    husband = gedcomInd2;
                    wife = gedcomInd1;
                } else {
                    husband = gedcomInd1;
                    wife = gedcomInd2;
                }
                
                IndividualReference husbandRef = new IndividualReference();
                husbandRef.setIndividual(husband);
                family.setHusband(husbandRef);
                
                IndividualReference wifeRef = new IndividualReference();
                wifeRef.setIndividual(wife);
                family.setWife(wifeRef);

                // Marriage event
                if (rel.getStartDate() != null) {
                    FamilyEvent marriage = new FamilyEvent();
                    marriage.setType(FamilyEventType.MARRIAGE);
                    marriage.setDate(new StringWithCustomFacts(formatGedcomDate(rel.getStartDate())));
                    family.getEvents(true).add(marriage);
                }
                
                familyMap.put(familyId, family);
                familyChildrenSet.put(familyId, new HashSet<>());
                
                // Track which family each individual belongs to
                individualToFamilyMap.put(ind1Id, familyId);
                individualToFamilyMap.put(ind2Id, familyId);
                spouseToFamilyObjMap.put(ind1Id, family);
                spouseToFamilyObjMap.put(ind2Id, family);
                
                log.debug("Created family {} with spouse1={} spouse2={}", familyId, ind1Id, ind2Id);
            }
        }
        
        log.info("Created {} families from SPOUSE relationships. individualToFamilyMap has {} entries", 
                familyMap.size(), individualToFamilyMap.size());

        // Second pass: Add children to families (avoiding duplicates)
        // PARENT_CHILD relationship can be stored in either direction:
        // - individual1 = parent, individual2 = child, OR
        // - individual1 = child, individual2 = parent
        // We check both directions to find which one is the parent (in a family)
        int childrenAdded = 0;
        int childrenSkipped = 0;
        
        for (Relationship rel : relationships) {
            if (rel.getType() == RelationshipType.PARENT_CHILD || 
                rel.getType() == RelationshipType.MOTHER_CHILD ||
                rel.getType() == RelationshipType.FATHER_CHILD ||
                rel.getType() == RelationshipType.ADOPTED_PARENT_CHILD ||
                rel.getType() == RelationshipType.STEP_PARENT_CHILD) {
                
                // Get individual IDs safely
                UUID id1 = extractIndividualId(rel.getIndividual1(), individuals);
                UUID id2 = extractIndividualId(rel.getIndividual2(), individuals);
                
                if (id1 == null || id2 == null) {
                    log.warn("PARENT_CHILD relationship {} has null IDs: id1={}, id2={}", 
                            rel.getId(), id1, id2);
                    childrenSkipped++;
                    continue;
                }
                
                // Try both directions to find parent and child
                UUID parentId = null;
                UUID childId = null;
                
                // Check if individual1 is in a family (meaning it's the parent)
                if (individualToFamilyMap.containsKey(id1)) {
                    parentId = id1;
                    childId = id2;
                } 
                // Check if individual2 is in a family (meaning it's the parent)
                else if (individualToFamilyMap.containsKey(id2)) {
                    parentId = id2;
                    childId = id1;
                } 
                else {
                    // Neither is in a family - skip this relationship
                    log.debug("PARENT_CHILD relationship {} has no parent in any family (id1={}, id2={})", 
                            rel.getId(), id1, id2);
                    childrenSkipped++;
                    continue;
                }
                
                org.gedcom4j.model.Individual child = gedcomIndividualMap.get(childId);
                if (child == null) {
                    log.warn("Child {} not found in gedcomIndividualMap for relationship {}", childId, rel.getId());
                    childrenSkipped++;
                    continue;
                }

                String familyId = individualToFamilyMap.get(parentId);
                Family family = familyMap.get(familyId);
                Set<String> addedChildren = familyChildrenSet.get(familyId);
                
                // Only add child if not already added to this family
                if (addedChildren != null && !addedChildren.contains(child.getXref())) {
                    IndividualReference childRef = new IndividualReference();
                    childRef.setIndividual(child);
                    family.getChildren(true).add(childRef);
                    addedChildren.add(child.getXref());
                    childrenAdded++;
                    log.debug("Added child {} ({}) to family {}", child.getXref(), childId, familyId);
                }
            }
        }
        
        log.info("Processed PARENT_CHILD relationships: {} children added, {} skipped", childrenAdded, childrenSkipped);

        // Add families to gedcom
        gedcom.getFamilies().putAll(familyMap);

        // Write GEDCOM to byte array
        GedcomWriter writer = new GedcomWriter(gedcom);
        // Suppress validation to allow export even with minor structural issues
        writer.setValidationSuppressed(true);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        writer.write(baos);
        return baos.toByteArray();
    }

    /**
     * Parse GEDCOM date string to LocalDate
     */
    private LocalDate parseGedcomDate(String gedcomDate) {
        if (gedcomDate == null || gedcomDate.trim().isEmpty()) {
            return null;
        }

        // Remove qualifiers like ABT, BEF, AFT, etc.
        String cleanDate = gedcomDate.replaceAll("(?i)(ABT|BEF|AFT|CAL|EST|BET|AND)\\s+", "").trim();

        // Try different date formats
        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            try {
                return LocalDate.parse(cleanDate, formatter);
            } catch (DateTimeParseException e) {
                // Try next formatter
            }
        }

        // Try to extract just the year
        try {
            int year = Integer.parseInt(cleanDate);
            return LocalDate.of(year, 1, 1);
        } catch (NumberFormatException e) {
            log.warn("Could not parse date: {}", gedcomDate);
            return null;
        }
    }

    /**
     * Format LocalDate to GEDCOM date string
     */
    private String formatGedcomDate(LocalDate date) {
        if (date == null) {
            return "";
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH);
        return date.format(formatter).toUpperCase();
    }
    
    /**
     * Safely extract individual ID, handling potential Hibernate proxy issues
     */
    private UUID extractIndividualId(com.familytree.model.Individual individual, 
                                      List<com.familytree.model.Individual> allIndividuals) {
        if (individual == null) {
            return null;
        }
        
        // Try direct ID access first
        UUID directId = individual.getId();
        if (directId != null) {
            return directId;
        }
        
        // Fallback: Try to match by other properties if ID is null (proxy issue)
        log.warn("Individual has null ID, attempting to match by properties");
        for (com.familytree.model.Individual ind : allIndividuals) {
            // Try matching by name combination
            if (ind.getGivenName() != null && ind.getSurname() != null &&
                ind.getGivenName().equals(individual.getGivenName()) &&
                ind.getSurname().equals(individual.getSurname()) &&
                ind.getBirthDate() != null && ind.getBirthDate().equals(individual.getBirthDate())) {
                return ind.getId();
            }
        }
        
        return null;
    }
    
    /**
     * Parse Vietnamese name from GEDCOM basic name format
     * Vietnamese names are typically: Surname MiddleName GivenName
     * GEDCOM basic format is usually: GivenName /Surname/ or Surname /GivenName/
     * 
     * This method tries to intelligently parse Vietnamese names
     */
    private void parseVietnameseNameFromBasic(com.familytree.model.Individual individual, String basicName) {
        if (basicName == null || basicName.isEmpty()) {
            return;
        }
        
        // Remove extra spaces and trim
        basicName = basicName.trim().replaceAll("\\s+", " ");
        
        // Check if there's a surname in slashes: "Given /Surname/" or "/Surname/ Given"
        java.util.regex.Pattern surnamePattern = java.util.regex.Pattern.compile("/([^/]+)/");
        java.util.regex.Matcher matcher = surnamePattern.matcher(basicName);
        
        if (matcher.find()) {
            // Found surname in slashes
            String surnameInSlash = matcher.group(1).trim();
            String restOfName = basicName.replaceAll("/[^/]+/", "").trim();
            
            if (individual.getSurname() == null || individual.getSurname().isEmpty()) {
                individual.setSurname(surnameInSlash);
            }
            
            // Parse the rest as middle name + given name
            if (restOfName != null && !restOfName.isEmpty()) {
                String[] parts = restOfName.split("\\s+");
                if (parts.length > 1) {
                    // Last part is given name, rest is middle name
                    individual.setGivenName(parts[parts.length - 1]);
                    StringBuilder middleName = new StringBuilder();
                    for (int i = 0; i < parts.length - 1; i++) {
                        if (middleName.length() > 0) middleName.append(" ");
                        middleName.append(parts[i]);
                    }
                    if (individual.getMiddleName() == null || individual.getMiddleName().isEmpty()) {
                        individual.setMiddleName(middleName.toString());
                    }
                } else if (parts.length == 1) {
                    if (individual.getGivenName() == null || individual.getGivenName().isEmpty()) {
                        individual.setGivenName(parts[0]);
                    }
                }
            }
        } else {
            // No surname in slashes - try Vietnamese order: Surname MiddleName GivenName
            String[] parts = basicName.split("\\s+");
            if (parts.length >= 3) {
                // First part is surname, last part is given name, middle parts are middle name
                if (individual.getSurname() == null || individual.getSurname().isEmpty()) {
                    individual.setSurname(parts[0]);
                }
                if (individual.getGivenName() == null || individual.getGivenName().isEmpty()) {
                    individual.setGivenName(parts[parts.length - 1]);
                }
                StringBuilder middleName = new StringBuilder();
                for (int i = 1; i < parts.length - 1; i++) {
                    if (middleName.length() > 0) middleName.append(" ");
                    middleName.append(parts[i]);
                }
                if (individual.getMiddleName() == null || individual.getMiddleName().isEmpty()) {
                    individual.setMiddleName(middleName.toString());
                }
            } else if (parts.length == 2) {
                // Two parts: could be "Surname GivenName" 
                if (individual.getSurname() == null || individual.getSurname().isEmpty()) {
                    individual.setSurname(parts[0]);
                }
                if (individual.getGivenName() == null || individual.getGivenName().isEmpty()) {
                    individual.setGivenName(parts[1]);
                }
            } else if (parts.length == 1) {
                // Single name - use as given name
                if (individual.getGivenName() == null || individual.getGivenName().isEmpty()) {
                    individual.setGivenName(parts[0]);
                }
            }
        }
    }
}
