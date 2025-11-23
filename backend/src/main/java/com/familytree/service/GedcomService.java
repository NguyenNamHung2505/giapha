package com.familytree.service;

import com.familytree.dto.gedcom.GedcomImportResult;
import com.familytree.model.*;
import com.familytree.repository.FamilyTreeRepository;
import com.familytree.repository.IndividualRepository;
import com.familytree.repository.RelationshipRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.gedcom4j.model.*;
import org.gedcom4j.parser.GedcomParser;
import org.gedcom4j.parser.GedcomParserException;
import org.gedcom4j.writer.GedcomWriter;
import org.gedcom4j.writer.GedcomWriterException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for importing and exporting GEDCOM files
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
            DateTimeFormatter.ofPattern("d MMM yyyy"),
            DateTimeFormatter.ofPattern("MMM yyyy"),
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

            // Parse GEDCOM file
            log.info("Parsing GEDCOM file: {}", filename);
            GedcomParser parser = new GedcomParser();
            parser.load(file.getInputStream());

            if (parser.getErrors().isEmpty() && parser.getWarnings().isEmpty()) {
                log.info("GEDCOM file parsed successfully");
            } else {
                parser.getErrors().forEach(error -> {
                    log.warn("GEDCOM parse error: {}", error);
                    result.addError("Parse error: " + error);
                });
                parser.getWarnings().forEach(warning -> {
                    log.warn("GEDCOM parse warning: {}", warning);
                    result.addWarning("Parse warning: " + warning);
                });
            }

            Gedcom gedcom = parser.getGedcom();
            if (gedcom == null) {
                result.addError("Failed to parse GEDCOM file");
                result.setSuccess(false);
                return result;
            }

            // Process individuals
            log.info("Processing {} individuals", gedcom.getIndividuals().size());
            Map<String, Individual> individualMap = processIndividuals(gedcom, tree, result);
            result.setIndividualsImported(individualMap.size());

            // Process families (relationships)
            log.info("Processing {} families", gedcom.getFamilies().size());
            List<Relationship> relationships = processFamilies(gedcom, individualMap, tree, result);
            result.setRelationshipsImported(relationships.size());

            result.setSuccess(true);
            log.info("GEDCOM import completed: {} individuals, {} relationships",
                    individualMap.size(), relationships.size());

        } catch (GedcomParserException e) {
            log.error("GEDCOM parsing error", e);
            result.addError("Failed to parse GEDCOM: " + e.getMessage());
            result.setSuccess(false);
        } catch (IOException e) {
            log.error("IO error reading GEDCOM file", e);
            result.addError("Failed to read file: " + e.getMessage());
            result.setSuccess(false);
        } catch (Exception e) {
            log.error("Unexpected error during GEDCOM import", e);
            result.addError("Unexpected error: " + e.getMessage());
            result.setSuccess(false);
        } finally {
            long endTime = System.currentTimeMillis();
            result.setProcessingTimeMs(endTime - startTime);
        }

        return result;
    }

    /**
     * Process GEDCOM individuals and save to database
     */
    private Map<String, Individual> processIndividuals(Gedcom gedcom, FamilyTree tree, GedcomImportResult result) {
        Map<String, Individual> individualMap = new HashMap<>();

        for (org.gedcom4j.model.Individual gedcomIndividual : gedcom.getIndividuals().values()) {
            try {
                Individual individual = Individual.builder()
                        .tree(tree)
                        .build();

                // Extract name
                if (gedcomIndividual.getNames() != null && !gedcomIndividual.getNames().isEmpty()) {
                    PersonalName name = gedcomIndividual.getNames().get(0);
                    if (name.getGivenName() != null) {
                        individual.setGivenName(name.getGivenName().getValue());
                    }
                    if (name.getSurname() != null) {
                        individual.setSurname(name.getSurname().getValue());
                    }
                    if (name.getSuffix() != null) {
                        individual.setSuffix(name.getSuffix().getValue());
                    }
                }

                // Extract gender
                if (gedcomIndividual.getSex() != null) {
                    String sexValue = gedcomIndividual.getSex().getValue();
                    if (sexValue != null) {
                        switch (sexValue.toUpperCase()) {
                            case "M":
                                individual.setGender(Gender.MALE);
                                break;
                            case "F":
                                individual.setGender(Gender.FEMALE);
                                break;
                            default:
                                individual.setGender(Gender.UNKNOWN);
                        }
                    }
                }

                // Extract birth info
                List<IndividualEvent> birthEvents = gedcomIndividual.getEventsOfType(IndividualEventType.BIRTH);
                if (!birthEvents.isEmpty()) {
                    IndividualEvent birth = birthEvents.get(0);
                    if (birth.getDate() != null) {
                        LocalDate birthDate = parseGedcomDate(birth.getDate().getValue());
                        individual.setBirthDate(birthDate);
                    }
                    if (birth.getPlace() != null) {
                        individual.setBirthPlace(birth.getPlace().getPlaceName());
                    }
                }

                // Extract death info
                List<IndividualEvent> deathEvents = gedcomIndividual.getEventsOfType(IndividualEventType.DEATH);
                if (!deathEvents.isEmpty()) {
                    IndividualEvent death = deathEvents.get(0);
                    if (death.getDate() != null) {
                        LocalDate deathDate = parseGedcomDate(death.getDate().getValue());
                        individual.setDeathDate(deathDate);
                    }
                    if (death.getPlace() != null) {
                        individual.setDeathPlace(death.getPlace().getPlaceName());
                    }
                }

                // Extract notes
                if (gedcomIndividual.getNoteStructures() != null && !gedcomIndividual.getNoteStructures().isEmpty()) {
                    StringBuilder notes = new StringBuilder();
                    for (NoteStructure note : gedcomIndividual.getNoteStructures()) {
                        if (note.getLines() != null) {
                            notes.append(String.join("\n", note.getLines())).append("\n");
                        }
                    }
                    individual.setBiography(notes.toString().trim());
                }

                // Save individual
                Individual saved = individualRepository.save(individual);

                // Map GEDCOM ID to database ID
                String gedcomId = gedcomIndividual.getXref();
                individualMap.put(gedcomId, saved);

                log.debug("Imported individual: {} {} ({})",
                        individual.getGivenName(), individual.getSurname(), gedcomId);

            } catch (Exception e) {
                log.error("Error processing individual {}: {}", gedcomIndividual.getXref(), e.getMessage());
                result.addWarning("Failed to import individual " + gedcomIndividual.getXref() + ": " + e.getMessage());
            }
        }

        return individualMap;
    }

    /**
     * Process GEDCOM families and create relationships
     */
    private List<Relationship> processFamilies(Gedcom gedcom, Map<String, Individual> individualMap,
                                               FamilyTree tree, GedcomImportResult result) {
        List<Relationship> relationships = new ArrayList<>();

        for (Family family : gedcom.getFamilies().values()) {
            try {
                // Get spouses
                Individual husband = null;
                Individual wife = null;

                if (family.getHusband() != null && family.getHusband().getXref() != null) {
                    husband = individualMap.get(family.getHusband().getXref());
                }
                if (family.getWife() != null && family.getWife() != null) {
                    wife = individualMap.get(family.getWife().getXref());
                }

                // Create spouse relationship
                if (husband != null && wife != null) {
                    Relationship spouseRel = Relationship.builder()
                            .tree(tree)
                            .individual1(husband)
                            .individual2(wife)
                            .type(RelationshipType.SPOUSE)
                            .build();

                    // Extract marriage date
                    List<FamilyEvent> marriages = family.getEventsOfType(FamilyEventType.MARRIAGE);
                    if (!marriages.isEmpty() && marriages.get(0).getDate() != null) {
                        LocalDate marriageDate = parseGedcomDate(marriages.get(0).getDate().getValue());
                        spouseRel.setStartDate(marriageDate);
                    }

                    // Extract divorce date
                    List<FamilyEvent> divorces = family.getEventsOfType(FamilyEventType.DIVORCE);
                    if (!divorces.isEmpty() && divorces.get(0).getDate() != null) {
                        LocalDate divorceDate = parseGedcomDate(divorces.get(0).getDate().getValue());
                        spouseRel.setEndDate(divorceDate);
                    }

                    relationships.add(relationshipRepository.save(spouseRel));
                    log.debug("Created spouse relationship: {} <-> {}", husband.getId(), wife.getId());
                }

                // Create parent-child relationships
                if (family.getChildren() != null) {
                    for (org.gedcom4j.model.Individual gedcomChild : family.getChildren()) {
                        Individual child = individualMap.get(gedcomChild.getXref());

                        if (child != null) {
                            // Create relationship with husband (father)
                            if (husband != null) {
                                Relationship parentRel = Relationship.builder()
                                        .tree(tree)
                                        .individual1(husband)
                                        .individual2(child)
                                        .type(RelationshipType.PARENT_CHILD)
                                        .build();
                                relationships.add(relationshipRepository.save(parentRel));
                                log.debug("Created parent-child: {} -> {}", husband.getId(), child.getId());
                            }

                            // Create relationship with wife (mother)
                            if (wife != null) {
                                Relationship parentRel = Relationship.builder()
                                        .tree(tree)
                                        .individual1(wife)
                                        .individual2(child)
                                        .type(RelationshipType.PARENT_CHILD)
                                        .build();
                                relationships.add(relationshipRepository.save(parentRel));
                                log.debug("Created parent-child: {} -> {}", wife.getId(), child.getId());
                            }
                        }
                    }
                }

            } catch (Exception e) {
                log.error("Error processing family {}: {}", family.getXref(), e.getMessage());
                result.addWarning("Failed to import family " + family.getXref() + ": " + e.getMessage());
            }
        }

        return relationships;
    }

    /**
     * Parse GEDCOM date string to LocalDate
     */
    private LocalDate parseGedcomDate(String gedcomDate) {
        if (gedcomDate == null || gedcomDate.trim().isEmpty()) {
            return null;
        }

        // Clean up date string
        String cleanDate = gedcomDate.trim().toUpperCase();

        // Remove qualifiers (ABT, BEF, AFT, CAL, EST, etc.)
        cleanDate = cleanDate.replaceAll("^(ABT|ABOUT|BEF|BEFORE|AFT|AFTER|CAL|EST|FROM|TO)\\s+", "");

        // Handle "BET ... AND ..." - take first date
        if (cleanDate.startsWith("BET") || cleanDate.startsWith("BETWEEN")) {
            cleanDate = cleanDate.replaceAll("^(BET|BETWEEN)\\s+", "");
            int andPos = cleanDate.indexOf(" AND ");
            if (andPos > 0) {
                cleanDate = cleanDate.substring(0, andPos).trim();
            }
        }

        // Try different date formats
        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            try {
                return LocalDate.parse(cleanDate, formatter);
            } catch (DateTimeParseException e) {
                // Try next formatter
            }
        }

        // If all else fails, try to extract year only
        try {
            String[] parts = cleanDate.split("\\s+");
            for (String part : parts) {
                if (part.matches("\\d{4}")) {
                    return LocalDate.of(Integer.parseInt(part), 1, 1);
                }
            }
        } catch (Exception e) {
            log.warn("Could not parse date: {}", gedcomDate);
        }

        return null;
    }

    /**
     * Export a family tree to GEDCOM format
     */
    @Transactional(readOnly = true)
    public byte[] exportGedcom(UUID treeId, UUID userId) throws GedcomWriterException, IOException {
        // Verify tree access
        FamilyTree tree = treeRepository.findById(treeId)
                .orElseThrow(() -> new RuntimeException("Tree not found"));

        if (!permissionService.canViewTree(userId, treeId)) {
            throw new RuntimeException("You don't have permission to view this tree");
        }

        // Create GEDCOM structure
        Gedcom gedcom = new Gedcom();

        // Set header
        Header header = new Header();
        header.setGedcomVersion(new GedcomVersion());
        header.getGedcomVersion().setVersionNumber("5.5.1");

        SourceSystem sourceSystem = new SourceSystem();
        sourceSystem.setSystemId("Family Tree Manager");
        header.setSourceSystem(sourceSystem);

        gedcom.setHeader(header);

        // Load all individuals and relationships
        List<Individual> individuals = individualRepository.findByTreeId(treeId);
        List<Relationship> relationships = relationshipRepository.findByTreeId(treeId);

        // Create maps for lookup
        Map<UUID, String> individualToGedcomId = new HashMap<>();
        Map<String, org.gedcom4j.model.Individual> gedcomIndividuals = new HashMap<>();

        // Add individuals
        int individualCounter = 1;
        for (Individual individual : individuals) {
            String gedcomId = "@I" + individualCounter++ + "@";
            individualToGedcomId.put(individual.getId(), gedcomId);

            org.gedcom4j.model.Individual gedcomIndividual = new org.gedcom4j.model.Individual();
            gedcomIndividual.setXref(gedcomId);

            // Add name
            PersonalName name = new PersonalName();
            if (individual.getGivenName() != null) {
                name.setGivenName(new StringWithCustomFacts(individual.getGivenName()));
            }
            if (individual.getSurname() != null) {
                name.setSurname(new StringWithCustomFacts(individual.getSurname()));
            }
            if (individual.getSuffix() != null) {
                name.setSuffix(new StringWithCustomFacts(individual.getSuffix()));
            }
            gedcomIndividual.getNames().add(name);

            // Add gender
            if (individual.getGender() != null) {
                StringWithCustomFacts sex = new StringWithCustomFacts();
                switch (individual.getGender()) {
                    case MALE:
                        sex.setValue("M");
                        break;
                    case FEMALE:
                        sex.setValue("F");
                        break;
                    default:
                        sex.setValue("U");
                }
                gedcomIndividual.setSex(sex);
            }

            // Add birth event
            if (individual.getBirthDate() != null || individual.getBirthPlace() != null) {
                IndividualEvent birth = new IndividualEvent();
                birth.setType(IndividualEventType.BIRTH);
                if (individual.getBirthDate() != null) {
                    birth.setDate(new StringWithCustomFacts(formatGedcomDate(individual.getBirthDate())));
                }
                if (individual.getBirthPlace() != null) {
                    Place place = new Place();
                    place.setPlaceName(individual.getBirthPlace());
                    birth.setPlace(place);
                }
                gedcomIndividual.getEvents().add(birth);
            }

            // Add death event
            if (individual.getDeathDate() != null || individual.getDeathPlace() != null) {
                IndividualEvent death = new IndividualEvent();
                death.setType(IndividualEventType.DEATH);
                if (individual.getDeathDate() != null) {
                    death.setDate(new StringWithCustomFacts(formatGedcomDate(individual.getDeathDate())));
                }
                if (individual.getDeathPlace() != null) {
                    Place place = new Place();
                    place.setPlaceName(individual.getDeathPlace());
                    death.setPlace(place);
                }
                gedcomIndividual.getEvents().add(death);
            }

            // Add notes
            if (individual.getBiography() != null && !individual.getBiography().isEmpty()) {
                NoteStructure note = new NoteStructure();
                note.getLines().add(individual.getBiography());
                gedcomIndividual.getNoteStructures().add(note);
            }

            gedcom.getIndividuals().put(gedcomId, gedcomIndividual);
            gedcomIndividuals.put(gedcomId, gedcomIndividual);
        }

        // Group relationships into families
        Map<String, List<Relationship>> familyGroups = new HashMap<>();
        for (Relationship rel : relationships) {
            if (rel.getType() == RelationshipType.SPOUSE || rel.getType() == RelationshipType.PARTNER) {
                // Create family key from sorted spouse IDs
                String key = Stream.of(rel.getIndividual1().getId(), rel.getIndividual2().getId())
                        .sorted()
                        .map(UUID::toString)
                        .collect(Collectors.joining("-"));
                familyGroups.computeIfAbsent(key, k -> new ArrayList<>()).add(rel);
            }
        }

        // Add families
        int familyCounter = 1;
        for (Map.Entry<String, List<Relationship>> entry : familyGroups.entrySet()) {
            Family family = new Family();
            family.setXref("@F" + familyCounter++ + "@");

            // Find spouse relationship
            Relationship spouseRel = entry.getValue().stream()
                    .filter(r -> r.getType() == RelationshipType.SPOUSE || r.getType() == RelationshipType.PARTNER)
                    .findFirst()
                    .orElse(null);

            if (spouseRel != null) {
                String husbandId = individualToGedcomId.get(spouseRel.getIndividual1().getId());
                String wifeId = individualToGedcomId.get(spouseRel.getIndividual2().getId());

                if (husbandId != null && wifeId != null) {
                    family.setHusband(gedcomIndividuals.get(husbandId));
                    family.setWife(gedcomIndividuals.get(wifeId));

                    // Add marriage date
                    if (spouseRel.getStartDate() != null) {
                        FamilyEvent marriage = new FamilyEvent();
                        marriage.setType(FamilyEventType.MARRIAGE);
                        marriage.setDate(new StringWithCustomFacts(formatGedcomDate(spouseRel.getStartDate())));
                        family.getEvents().add(marriage);
                    }

                    // Add divorce date
                    if (spouseRel.getEndDate() != null) {
                        FamilyEvent divorce = new FamilyEvent();
                        divorce.setType(FamilyEventType.DIVORCE);
                        divorce.setDate(new StringWithCustomFacts(formatGedcomDate(spouseRel.getEndDate())));
                        family.getEvents().add(divorce);
                    }

                    // Find children of this couple
                    for (Relationship childRel : relationships) {
                        if (childRel.getType() == RelationshipType.PARENT_CHILD &&
                            (childRel.getIndividual1().getId().equals(spouseRel.getIndividual1().getId()) ||
                             childRel.getIndividual1().getId().equals(spouseRel.getIndividual2().getId()))) {

                            String childId = individualToGedcomId.get(childRel.getIndividual2().getId());
                            if (childId != null) {
                                org.gedcom4j.model.Individual childIndividual = gedcomIndividuals.get(childId);
                                if (childIndividual != null && !family.getChildren().contains(childIndividual)) {
                                    family.getChildren().add(childIndividual);
                                }
                            }
                        }
                    }

                    gedcom.getFamilies().put(family.getXref(), family);
                }
            }
        }

        // Write GEDCOM to byte array
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        GedcomWriter writer = new GedcomWriter(gedcom);
        writer.write(outputStream);

        return outputStream.toByteArray();
    }

    /**
     * Format LocalDate to GEDCOM date format
     */
    private String formatGedcomDate(LocalDate date) {
        if (date == null) {
            return null;
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH);
        return date.format(formatter).toUpperCase();
    }
}
