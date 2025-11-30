package com.familytree.service;

import com.familytree.dto.relationship.RelationshipPathResponse;
import com.familytree.dto.relationship.RelationshipPathResponse.PathStep;
import com.familytree.dto.relationship.RelationshipPathResponse.PersonSummary;
import com.familytree.dto.relationship.RelationshipPathResponse.RelationshipCategory;
import com.familytree.exception.BadRequestException;
import com.familytree.exception.ResourceNotFoundException;
import com.familytree.model.*;
import com.familytree.repository.IndividualRepository;
import com.familytree.repository.RelationshipRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

/**
 * Service for calculating and finding relationship paths between two individuals
 * in a family tree. Uses BFS to find the shortest path and then determines
 * the relationship name based on the path structure.
 *
 * Supports detailed Vietnamese relationship terms including:
 * - Paternal (nội) vs Maternal (ngoại) distinction
 * - Elder (bác) vs Younger (chú/cô/cậu/dì) distinction based on birth order
 * - In-law relationships (dâu/rể)
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class RelationshipPathService {

    private final RelationshipRepository relationshipRepository;
    private final IndividualRepository individualRepository;

    /**
     * Find the relationship between two individuals
     */
    public RelationshipPathResponse findRelationship(UUID treeId, UUID person1Id, UUID person2Id, String userEmail) {
        log.info("Finding relationship between {} and {} in tree {}", person1Id, person2Id, treeId);

        Individual person1 = individualRepository.findById(person1Id)
                .orElseThrow(() -> new ResourceNotFoundException("Person 1 not found with ID: " + person1Id));

        Individual person2 = individualRepository.findById(person2Id)
                .orElseThrow(() -> new ResourceNotFoundException("Person 2 not found with ID: " + person2Id));

        if (!person1.getTree().getId().equals(treeId) || !person2.getTree().getId().equals(treeId)) {
            throw new BadRequestException("Both individuals must belong to the specified tree");
        }

        if (person1Id.equals(person2Id)) {
            return buildSamePersonResponse(person1);
        }

        Map<UUID, List<FamilyConnection>> adjacencyMap = buildAdjacencyMap(treeId);
        List<PathNode> path = findPathBFS(person1Id, person2Id, adjacencyMap);

        if (path == null || path.isEmpty()) {
            return buildNotRelatedResponse(person1, person2);
        }

        return calculateRelationship(person1, person2, path);
    }

    // ==================== Data Structures ====================

    private static class FamilyConnection {
        UUID targetId;
        ConnectionType type;
        Individual targetIndividual;
        RelationshipType originalRelType; // To track if it's FATHER_CHILD or MOTHER_CHILD

        FamilyConnection(UUID targetId, ConnectionType type, Individual target, RelationshipType relType) {
            this.targetId = targetId;
            this.type = type;
            this.targetIndividual = target;
            this.originalRelType = relType;
        }
    }

    private enum ConnectionType {
        PARENT,
        CHILD,
        SPOUSE,
        SIBLING
    }

    private static class PathNode {
        UUID personId;
        Individual person;
        ConnectionType connectionFromPrevious;
        RelationshipType originalRelType;
        PathNode previous;

        PathNode(UUID personId, Individual person, ConnectionType type, RelationshipType relType, PathNode previous) {
            this.personId = personId;
            this.person = person;
            this.connectionFromPrevious = type;
            this.originalRelType = relType;
            this.previous = previous;
        }
    }

    /**
     * Context for determining Vietnamese relationship terms
     */
    private static class RelationshipContext {
        boolean isPaternal;          // true = bên nội, false = bên ngoại
        boolean isElderThanParent;   // true = bác, false = chú/cô/cậu/dì
        Gender parentGender;         // Gender of the connecting parent
        Gender targetGender;         // Gender of the person being referred to
        int generationsUp;
        int generationsDown;
        boolean hasSpouseStep;
        int siblingStepIndex;
        List<PathNode> path;
    }

    // ==================== Graph Building ====================

    private Map<UUID, List<FamilyConnection>> buildAdjacencyMap(UUID treeId) {
        Map<UUID, List<FamilyConnection>> adjacencyMap = new HashMap<>();
        List<Relationship> allRelationships = relationshipRepository.findByTreeId(treeId);

        // Track children by parent for inferring sibling relationships
        Map<UUID, List<Individual>> childrenByParent = new HashMap<>();

        for (Relationship rel : allRelationships) {
            UUID id1 = rel.getIndividual1().getId();
            UUID id2 = rel.getIndividual2().getId();

            adjacencyMap.computeIfAbsent(id1, k -> new ArrayList<>());
            adjacencyMap.computeIfAbsent(id2, k -> new ArrayList<>());

            switch (rel.getType()) {
                case PARENT_CHILD:
                case MOTHER_CHILD:
                case FATHER_CHILD:
                case ADOPTED_PARENT_CHILD:
                case STEP_PARENT_CHILD:
                    adjacencyMap.get(id1).add(new FamilyConnection(id2, ConnectionType.CHILD, rel.getIndividual2(), rel.getType()));
                    adjacencyMap.get(id2).add(new FamilyConnection(id1, ConnectionType.PARENT, rel.getIndividual1(), rel.getType()));
                    // Track children for sibling inference
                    childrenByParent.computeIfAbsent(id1, k -> new ArrayList<>()).add(rel.getIndividual2());
                    break;

                case SPOUSE:
                case PARTNER:
                    adjacencyMap.get(id1).add(new FamilyConnection(id2, ConnectionType.SPOUSE, rel.getIndividual2(), rel.getType()));
                    adjacencyMap.get(id2).add(new FamilyConnection(id1, ConnectionType.SPOUSE, rel.getIndividual1(), rel.getType()));
                    break;

                case SIBLING:
                    adjacencyMap.get(id1).add(new FamilyConnection(id2, ConnectionType.SIBLING, rel.getIndividual2(), rel.getType()));
                    adjacencyMap.get(id2).add(new FamilyConnection(id1, ConnectionType.SIBLING, rel.getIndividual1(), rel.getType()));
                    break;
            }
        }

        // Infer sibling relationships from shared parents
        for (List<Individual> children : childrenByParent.values()) {
            if (children.size() > 1) {
                for (int i = 0; i < children.size(); i++) {
                    for (int j = i + 1; j < children.size(); j++) {
                        Individual child1 = children.get(i);
                        Individual child2 = children.get(j);
                        UUID childId1 = child1.getId();
                        UUID childId2 = child2.getId();

                        // Check if sibling connection already exists
                        boolean alreadyConnected = adjacencyMap.get(childId1).stream()
                                .anyMatch(conn -> conn.targetId.equals(childId2) && conn.type == ConnectionType.SIBLING);

                        if (!alreadyConnected) {
                            adjacencyMap.get(childId1).add(new FamilyConnection(childId2, ConnectionType.SIBLING, child2, RelationshipType.SIBLING));
                            adjacencyMap.get(childId2).add(new FamilyConnection(childId1, ConnectionType.SIBLING, child1, RelationshipType.SIBLING));
                        }
                    }
                }
            }
        }

        return adjacencyMap;
    }

    private List<PathNode> findPathBFS(UUID startId, UUID endId, Map<UUID, List<FamilyConnection>> adjacencyMap) {
        if (!adjacencyMap.containsKey(startId) || !adjacencyMap.containsKey(endId)) {
            return null;
        }

        Queue<PathNode> queue = new LinkedList<>();
        Set<UUID> visited = new HashSet<>();

        Individual startPerson = individualRepository.findById(startId).orElse(null);
        queue.add(new PathNode(startId, startPerson, null, null, null));
        visited.add(startId);

        while (!queue.isEmpty()) {
            PathNode current = queue.poll();

            if (current.personId.equals(endId)) {
                List<PathNode> path = new ArrayList<>();
                PathNode node = current;
                while (node != null) {
                    path.add(0, node);
                    node = node.previous;
                }
                return path;
            }

            List<FamilyConnection> connections = adjacencyMap.get(current.personId);
            if (connections != null) {
                for (FamilyConnection conn : connections) {
                    if (!visited.contains(conn.targetId)) {
                        visited.add(conn.targetId);
                        queue.add(new PathNode(conn.targetId, conn.targetIndividual, conn.type, conn.originalRelType, current));
                    }
                }
            }
        }

        return null;
    }

    // ==================== Relationship Calculation ====================

    private RelationshipPathResponse calculateRelationship(Individual person1, Individual person2, List<PathNode> path) {
        RelationshipContext ctx = analyzePathContext(path);
        ctx.targetGender = person2.getGender();

        RelationshipCategory category;
        String relFromP1, relFromP2, relFromP1Vi, relFromP2Vi;
        PersonSummary commonAncestor = null;

        Gender gender1 = person1.getGender();
        Gender gender2 = person2.getGender();

        if (ctx.hasSpouseStep && ctx.generationsUp == 0 && ctx.generationsDown == 0 && ctx.siblingStepIndex == -1) {
            // Direct spouse
            // relFromP1 = what person1 is to person2 (husband/wife) - use gender1
            // relFromP2 = what person2 is to person1 (husband/wife) - use gender2
            category = RelationshipCategory.SPOUSE;
            relFromP1 = getSpouseTermEn(gender1);
            relFromP2 = getSpouseTermEn(gender2);
            relFromP1Vi = getSpouseTermVi(gender1);
            relFromP2Vi = getSpouseTermVi(gender2);

        } else if (ctx.generationsUp > 0 && ctx.generationsDown == 0 && ctx.siblingStepIndex == -1 && !ctx.hasSpouseStep) {
            // Direct ancestor (person2 is ancestor of person1)
            // person1 is descendant, person2 is ancestor
            // relFromP1 = what person1 is to person2 (son/grandson) - use gender1
            // relFromP2 = what person2 is to person1 (father/grandfather) - use gender2
            category = RelationshipCategory.DIRECT_ANCESTOR;
            relFromP1 = getDescendantTermEn(ctx.generationsUp, gender1);
            relFromP2 = getAncestorTermEn(ctx.generationsUp, gender2);
            relFromP1Vi = getDescendantTermVi(ctx.generationsUp, gender1, ctx);
            relFromP2Vi = getAncestorTermVi(ctx.generationsUp, gender2, ctx);

        } else if (ctx.generationsDown > 0 && ctx.generationsUp == 0 && ctx.siblingStepIndex == -1 && !ctx.hasSpouseStep) {
            // Direct descendant (person2 is descendant of person1)
            // person1 is ancestor, person2 is descendant
            // relFromP1 = what person1 is to person2 (father/grandfather) - use gender1
            // relFromP2 = what person2 is to person1 (son/grandson) - use gender2
            category = RelationshipCategory.DIRECT_DESCENDANT;
            relFromP1 = getAncestorTermEn(ctx.generationsDown, gender1);
            relFromP2 = getDescendantTermEn(ctx.generationsDown, gender2);
            relFromP1Vi = getAncestorTermVi(ctx.generationsDown, gender1, ctx);
            relFromP2Vi = getDescendantTermVi(ctx.generationsDown, gender2, ctx);

        } else if (ctx.siblingStepIndex > 0 && ctx.generationsUp == 0 && ctx.generationsDown == 0 && !ctx.hasSpouseStep) {
            // Direct sibling
            // relFromP1 = what person1 is to person2 (brother/sister) - use gender1
            // relFromP2 = what person2 is to person1 (brother/sister) - use gender2
            category = RelationshipCategory.SIBLING;
            boolean person2IsElder = isElderSibling(person1, person2);
            relFromP1 = getSiblingTermEn(gender1);
            relFromP2 = getSiblingTermEn(gender2);
            relFromP1Vi = getSiblingTermVi(gender1, !person2IsElder);  // person1 is elder if person2 is not elder
            relFromP2Vi = getSiblingTermVi(gender2, person2IsElder);

        } else if (ctx.generationsUp > 0 && ctx.siblingStepIndex > 0 && ctx.generationsDown == 0 && !ctx.hasSpouseStep) {
            // Uncle/Aunt type (person1 goes up to parent, parent has sibling = person2)
            // person1 is nephew/niece, person2 is uncle/aunt
            // relFromP1 = what person1 is to person2 (nephew/niece) - use gender1
            // relFromP2 = what person2 is to person1 (uncle/aunt) - use gender2
            determineUncleAuntContext(ctx, path);

            if (ctx.generationsUp == 1) {
                category = RelationshipCategory.UNCLE_AUNT;
                relFromP1 = getNephewNieceTermEn(gender1);
                relFromP2 = getUncleAuntTermEn(gender2);
                relFromP1Vi = getNephewNieceTermVi(gender1);
                relFromP2Vi = getUncleAuntTermVi(gender2, ctx);
            } else {
                category = RelationshipCategory.GRANDUNCLE_GRANDAUNT;
                relFromP1 = getGrandNephewNieceTermEn(ctx.generationsUp, gender1);
                relFromP2 = getGrandUncleAuntTermEn(ctx.generationsUp, gender2);
                relFromP1Vi = getGrandNephewNieceTermVi(ctx.generationsUp, gender1);
                relFromP2Vi = getGrandUncleAuntTermVi(ctx.generationsUp, gender2, ctx);
            }
            if (ctx.generationsUp < path.size()) {
                commonAncestor = toPersonSummary(path.get(ctx.generationsUp).person);
            }

        } else if (ctx.siblingStepIndex > 0 && ctx.generationsDown > 0 && ctx.generationsUp == 0 && !ctx.hasSpouseStep) {
            // Nephew/Niece type (person1's sibling has child = person2)
            // person1 is uncle/aunt of person2, person2 is nephew/niece of person1
            // relFromP1 = what person1 is to person2 (uncle/aunt) - use gender1
            // relFromP2 = what person2 is to person1 (nephew/niece) - use gender2

            // Determine if paternal or maternal based on the sibling (parent of person2)
            determineNephewNieceContext(ctx, path);

            if (ctx.generationsDown == 1) {
                category = RelationshipCategory.NEPHEW_NIECE;
                relFromP1 = getUncleAuntTermEn(gender1);
                relFromP2 = getNephewNieceTermEn(gender2);
                relFromP1Vi = getUncleAuntTermVi(gender1, ctx);
                relFromP2Vi = getNephewNieceTermVi(gender2);
            } else {
                category = RelationshipCategory.GRANDNEPHEW_GRANDNIECE;
                relFromP1 = getGrandUncleAuntTermEn(ctx.generationsDown, gender1);
                relFromP2 = getGrandNephewNieceTermEn(ctx.generationsDown, gender2);
                relFromP1Vi = getGrandUncleAuntTermVi(ctx.generationsDown, gender1, ctx);
                relFromP2Vi = getGrandNephewNieceTermVi(ctx.generationsDown, gender2);
            }

        } else if (ctx.generationsUp > 0 && ctx.generationsDown > 0 && !ctx.hasSpouseStep) {
            // Cousin relationship
            // relFromP1 = what person1 is to person2 (cousin) - use gender1
            // relFromP2 = what person2 is to person1 (cousin) - use gender2
            category = RelationshipCategory.COUSIN;
            int cousinDegree = Math.min(ctx.generationsUp, ctx.generationsDown);
            int removed = Math.abs(ctx.generationsUp - ctx.generationsDown);

            // Determine if person1's parent is elder sibling (for Vietnamese cousin terms)
            boolean person1ParentIsElder = determineCousinParentSeniority(path, ctx);

            // Determine generation direction for "cousin once removed" etc.
            // person1IsOlderGeneration = true means person1 is in an older generation (like uncle/aunt)
            // generationsUp < generationsDown means person1 goes up less, so person1 is in older generation
            boolean person1IsOlderGeneration = ctx.generationsUp < ctx.generationsDown;

            relFromP1 = getCousinTermEn(cousinDegree, removed, gender1);
            relFromP2 = getCousinTermEn(cousinDegree, removed, gender2);
            relFromP1Vi = getCousinTermVi(cousinDegree, removed, gender1, person1ParentIsElder, person1IsOlderGeneration);
            relFromP2Vi = getCousinTermVi(cousinDegree, removed, gender2, !person1ParentIsElder, !person1IsOlderGeneration);

            if (ctx.generationsUp < path.size()) {
                commonAncestor = toPersonSummary(path.get(ctx.generationsUp).person);
            }

        } else if (ctx.hasSpouseStep) {
            // In-law relationship
            category = RelationshipCategory.IN_LAW;
            String[] terms = getInLawTerms(ctx, path, gender1, gender2);
            relFromP1 = terms[0];
            relFromP2 = terms[1];
            relFromP1Vi = terms[2];
            relFromP2Vi = terms[3];

        } else {
            category = RelationshipCategory.NOT_RELATED;
            relFromP1 = "related";
            relFromP2 = "related";
            relFromP1Vi = "có quan hệ họ hàng";
            relFromP2Vi = "có quan hệ họ hàng";
        }

        List<PathStep> pathSteps = buildPathSteps(path);
        int generationDiff = ctx.generationsDown - ctx.generationsUp;

        return RelationshipPathResponse.builder()
                .person1(toPersonSummary(person1))
                .person2(toPersonSummary(person2))
                .relationshipFromPerson1(relFromP1)
                .relationshipFromPerson2(relFromP2)
                .relationshipFromPerson1Vi(relFromP1Vi)
                .relationshipFromPerson2Vi(relFromP2Vi)
                .category(category)
                .generationDifference(generationDiff)
                .path(pathSteps)
                .relationshipFound(true)
                .commonAncestor(commonAncestor)
                .build();
    }

    private RelationshipContext analyzePathContext(List<PathNode> path) {
        RelationshipContext ctx = new RelationshipContext();
        ctx.path = path;
        ctx.generationsUp = 0;
        ctx.generationsDown = 0;
        ctx.hasSpouseStep = false;
        ctx.siblingStepIndex = -1;
        ctx.isPaternal = true;
        ctx.isElderThanParent = false;

        for (int i = 1; i < path.size(); i++) {
            PathNode node = path.get(i);
            switch (node.connectionFromPrevious) {
                case PARENT:
                    ctx.generationsUp++;
                    // Track if going through father or mother (for determining nội/ngoại)
                    if (i == 1) {
                        if (node.originalRelType == RelationshipType.FATHER_CHILD) {
                            ctx.isPaternal = true;
                            ctx.parentGender = Gender.MALE;
                        } else if (node.originalRelType == RelationshipType.MOTHER_CHILD) {
                            ctx.isPaternal = false;
                            ctx.parentGender = Gender.FEMALE;
                        } else {
                            ctx.parentGender = node.person.getGender();
                            ctx.isPaternal = ctx.parentGender == Gender.MALE;
                        }
                    }
                    break;
                case CHILD:
                    ctx.generationsDown++;
                    // Track if going through son or daughter (for determining nội/ngoại when going down)
                    // For grandparent looking at grandchild: nội = through son, ngoại = through daughter
                    if (i == 1) {
                        if (node.originalRelType == RelationshipType.FATHER_CHILD) {
                            // person1 is father of node.person, so going through a child
                            // Check the child's gender to determine nội/ngoại
                            ctx.parentGender = node.person.getGender();
                            ctx.isPaternal = ctx.parentGender == Gender.MALE;
                        } else if (node.originalRelType == RelationshipType.MOTHER_CHILD) {
                            ctx.parentGender = node.person.getGender();
                            ctx.isPaternal = ctx.parentGender == Gender.MALE;
                        } else {
                            ctx.parentGender = node.person.getGender();
                            ctx.isPaternal = ctx.parentGender == Gender.MALE;
                        }
                    }
                    break;
                case SPOUSE:
                    ctx.hasSpouseStep = true;
                    break;
                case SIBLING:
                    ctx.siblingStepIndex = i;
                    break;
            }
        }

        return ctx;
    }

    private void determineUncleAuntContext(RelationshipContext ctx, List<PathNode> path) {
        // Find the parent node and the sibling (uncle/aunt) node
        if (path.size() >= 3) {
            PathNode parentNode = path.get(1); // First step is to parent
            PathNode uncleAuntNode = path.get(2); // Second step is sibling of parent

            // Determine if paternal or maternal
            if (parentNode.originalRelType == RelationshipType.FATHER_CHILD) {
                ctx.isPaternal = true;
                ctx.parentGender = Gender.MALE;
            } else if (parentNode.originalRelType == RelationshipType.MOTHER_CHILD) {
                ctx.isPaternal = false;
                ctx.parentGender = Gender.FEMALE;
            } else {
                ctx.parentGender = parentNode.person.getGender();
                ctx.isPaternal = ctx.parentGender == Gender.MALE;
            }

            // Determine if uncle/aunt is elder or younger than parent
            ctx.isElderThanParent = isElderSibling(parentNode.person, uncleAuntNode.person);
        }
    }

    /**
     * Determine context for nephew/niece relationship.
     * Path structure: person1 (uncle/aunt) -> sibling (parent of person2) -> child -> person2 (nephew/niece)
     *
     * For Vietnamese terms:
     * - If the sibling is male (father), person1 is "chú/bác/cô" (paternal uncle/aunt)
     * - If the sibling is female (mother), person1 is "cậu/bác/dì" (maternal uncle/aunt)
     */
    private void determineNephewNieceContext(RelationshipContext ctx, List<PathNode> path) {
        if (ctx.siblingStepIndex > 0 && ctx.siblingStepIndex < path.size()) {
            // The sibling node is the parent of person2
            PathNode siblingNode = path.get(ctx.siblingStepIndex);

            // Determine if paternal or maternal based on sibling's gender
            // sibling is male = paternal (chú/bác/cô), sibling is female = maternal (cậu/bác/dì)
            ctx.parentGender = siblingNode.person.getGender();
            ctx.isPaternal = ctx.parentGender == Gender.MALE;

            // For the elder/younger determination, compare person1 with their sibling
            PathNode person1Node = path.get(0);
            ctx.isElderThanParent = isElderSibling(siblingNode.person, person1Node.person);
        }
    }

    private boolean isElderSibling(Individual person1, Individual person2) {
        // person2 is elder than person1?
        LocalDate birth1 = person1.getBirthDate();
        LocalDate birth2 = person2.getBirthDate();

        if (birth1 != null && birth2 != null) {
            return birth2.isBefore(birth1);
        }
        // Default: assume person2 is elder if we can't determine
        return true;
    }

    // ==================== English Terms ====================

    private String getSpouseTermEn(Gender gender) {
        if (gender == Gender.MALE) return "husband";
        if (gender == Gender.FEMALE) return "wife";
        return "spouse";
    }

    private String getAncestorTermEn(int generations, Gender gender) {
        String base = gender == Gender.MALE ? "father" : gender == Gender.FEMALE ? "mother" : "parent";
        if (generations == 1) return base;

        String grandBase = gender == Gender.MALE ? "grandfather" : gender == Gender.FEMALE ? "grandmother" : "grandparent";
        if (generations == 2) return grandBase;

        return "great-".repeat(generations - 2) + grandBase;
    }

    private String getDescendantTermEn(int generations, Gender gender) {
        String base = gender == Gender.MALE ? "son" : gender == Gender.FEMALE ? "daughter" : "child";
        if (generations == 1) return base;

        String grandBase = gender == Gender.MALE ? "grandson" : gender == Gender.FEMALE ? "granddaughter" : "grandchild";
        if (generations == 2) return grandBase;

        return "great-".repeat(generations - 2) + grandBase;
    }

    private String getSiblingTermEn(Gender gender) {
        if (gender == Gender.MALE) return "brother";
        if (gender == Gender.FEMALE) return "sister";
        return "sibling";
    }

    private String getUncleAuntTermEn(Gender gender) {
        if (gender == Gender.MALE) return "uncle";
        if (gender == Gender.FEMALE) return "aunt";
        return "uncle/aunt";
    }

    private String getNephewNieceTermEn(Gender gender) {
        if (gender == Gender.MALE) return "nephew";
        if (gender == Gender.FEMALE) return "niece";
        return "nephew/niece";
    }

    private String getGrandUncleAuntTermEn(int generations, Gender gender) {
        String prefix = generations > 2 ? "great-".repeat(generations - 2) : "";
        String base = gender == Gender.MALE ? "grand-uncle" : gender == Gender.FEMALE ? "grand-aunt" : "grand-uncle/aunt";
        return prefix + base;
    }

    private String getGrandNephewNieceTermEn(int generations, Gender gender) {
        String prefix = generations > 2 ? "great-".repeat(generations - 2) : "";
        String base = gender == Gender.MALE ? "grand-nephew" : gender == Gender.FEMALE ? "grand-niece" : "grand-nephew/niece";
        return prefix + base;
    }

    private String getCousinTermEn(int degree, int removed, Gender gender) {
        String ordinal = degree == 1 ? "1st" : degree == 2 ? "2nd" : degree == 3 ? "3rd" : degree + "th";
        if (removed == 0) {
            return ordinal + " cousin";
        }
        String removedStr = removed == 1 ? "once" : removed == 2 ? "twice" : removed + " times";
        return ordinal + " cousin " + removedStr + " removed";
    }

    // ==================== Vietnamese Terms ====================

    private String getSpouseTermVi(Gender gender) {
        if (gender == Gender.MALE) return "chồng";
        if (gender == Gender.FEMALE) return "vợ";
        return "vợ/chồng";
    }

    private String getAncestorTermVi(int generations, Gender gender, RelationshipContext ctx) {
        if (generations == 1) {
            if (gender == Gender.MALE) return "cha (bố/ba)";
            if (gender == Gender.FEMALE) return "mẹ (má)";
            return "cha/mẹ";
        }
        if (generations == 2) {
            if (ctx.isPaternal) {
                return gender == Gender.MALE ? "ông nội" : gender == Gender.FEMALE ? "bà nội" : "ông bà nội";
            } else {
                return gender == Gender.MALE ? "ông ngoại" : gender == Gender.FEMALE ? "bà ngoại" : "ông bà ngoại";
            }
        }
        if (generations == 3) {
            return gender == Gender.MALE ? "cụ ông" : gender == Gender.FEMALE ? "cụ bà" : "cụ";
        }
        if (generations == 4) {
            return gender == Gender.MALE ? "kỵ ông" : gender == Gender.FEMALE ? "kỵ bà" : "kỵ";
        }
        if (generations == 5) {
            return "tiên tổ đời thứ " + generations;
        }
        return "tổ tiên đời thứ " + generations;
    }

    private String getDescendantTermVi(int generations, Gender gender, RelationshipContext ctx) {
        if (generations == 1) {
            if (gender == Gender.MALE) return "con trai";
            if (gender == Gender.FEMALE) return "con gái";
            return "con";
        }
        if (generations == 2) {
            // Cháu nội/ngoại depends on whether through son or daughter
            if (ctx.isPaternal) {
                return gender == Gender.MALE ? "cháu nội (trai)" : gender == Gender.FEMALE ? "cháu nội (gái)" : "cháu nội";
            } else {
                return gender == Gender.MALE ? "cháu ngoại (trai)" : gender == Gender.FEMALE ? "cháu ngoại (gái)" : "cháu ngoại";
            }
        }
        if (generations == 3) {
            return gender == Gender.MALE ? "chắt (trai)" : gender == Gender.FEMALE ? "chắt (gái)" : "chắt";
        }
        if (generations == 4) {
            return gender == Gender.MALE ? "chút (trai)" : gender == Gender.FEMALE ? "chút (gái)" : "chút";
        }
        if (generations == 5) {
            return gender == Gender.MALE ? "chít (trai)" : gender == Gender.FEMALE ? "chít (gái)" : "chít";
        }
        return "hậu duệ đời thứ " + generations;
    }

    private String getSiblingTermVi(Gender gender, boolean isElder) {
        if (gender == Gender.MALE) {
            return isElder ? "anh trai" : "em trai";
        }
        if (gender == Gender.FEMALE) {
            return isElder ? "chị gái" : "em gái";
        }
        return isElder ? "anh/chị" : "em";
    }

    /**
     * Vietnamese uncle/aunt terms:
     *
     * Miền Bắc (Northern Vietnam):
     * - Bên nội (paternal - father's side):
     *   - Bác (trai) = elder brother of father
     *   - Chú = younger brother of father
     *   - Bác (gái) = elder sister of father
     *   - Cô = younger sister of father
     * - Bên ngoại (maternal - mother's side):
     *   - Bác (trai) = elder brother of mother
     *   - Cậu = younger brother of mother
     *   - Bác (gái) = elder sister of mother
     *   - Dì = younger sister of mother
     *
     * Miền Trung (Central Vietnam):
     * - Bên nội (paternal - father's side):
     *   - Bác (trai) = elder brother of father
     *   - Chú = younger brother of father
     *   - O = sister of father (regardless of elder/younger)
     * - Bên ngoại (maternal - mother's side):
     *   - Cậu = brother of mother (regardless of elder/younger)
     *   - Dì = sister of mother (regardless of elder/younger)
     *
     * Note: This implementation uses Central Vietnamese terms as default,
     * with Northern terms in parentheses for reference.
     */
    private String getUncleAuntTermVi(Gender gender, RelationshipContext ctx) {
        if (ctx.isPaternal) {
            // Bên nội (father's side)
            if (gender == Gender.MALE) {
                return ctx.isElderThanParent ? "bác (trai)" : "chú";
            } else if (gender == Gender.FEMALE) {
                // Miền Trung: "o" cho cả chị và em gái của bố
                // Miền Bắc: "bác gái" (chị) hoặc "cô" (em)
                return "o (cô)";
            }
            return ctx.isElderThanParent ? "bác" : "chú/o";
        } else {
            // Bên ngoại (mother's side)
            if (gender == Gender.MALE) {
                // Cậu = anh/em trai của mẹ
                return "cậu";
            } else if (gender == Gender.FEMALE) {
                // Dì = chị/em gái của mẹ
                return "dì";
            }
            return "cậu/dì";
        }
    }

    private String getNephewNieceTermVi(Gender gender) {
        // Cháu (không phân biệt nội/ngoại khi là nephew/niece)
        if (gender == Gender.MALE) return "cháu trai";
        if (gender == Gender.FEMALE) return "cháu gái";
        return "cháu";
    }

    private String getGrandUncleAuntTermVi(int generations, Gender gender, RelationshipContext ctx) {
        if (generations == 2) {
            if (ctx.isPaternal) {
                // Bên nội (father's side)
                if (gender == Gender.MALE) {
                    return ctx.isElderThanParent ? "ông bác" : "ông chú";
                } else if (gender == Gender.FEMALE) {
                    // Miền Trung: bà o
                    return "bà o (bà cô)";
                }
            } else {
                // Bên ngoại (mother's side)
                if (gender == Gender.MALE) {
                    return "ông cậu";
                } else if (gender == Gender.FEMALE) {
                    return "bà dì";
                }
            }
        }
        return "họ hàng bề trên đời thứ " + generations;
    }

    private String getGrandNephewNieceTermVi(int generations, Gender gender) {
        if (generations == 2) {
            if (gender == Gender.MALE) return "cháu họ (trai)";
            if (gender == Gender.FEMALE) return "cháu họ (gái)";
            return "cháu họ";
        }
        return "cháu họ đời thứ " + generations;
    }

    /**
     * Vietnamese cousin terms based on PARENT'S birth order (not cousin's age):
     * - Anh họ = male cousin whose parent is OLDER sibling
     * - Chị họ = female cousin whose parent is OLDER sibling
     * - Em họ = cousin whose parent is YOUNGER sibling
     *
     * For "cousin once removed" (removed > 0):
     * - If THIS person is in older generation: chú họ/cô họ/dì họ/cậu họ
     * - If THIS person is in younger generation: cháu họ
     *
     * @param degree The degree of cousin relationship
     * @param removed Number of generations removed
     * @param gender The gender of the person being described
     * @param parentIsElder True if THIS person's parent is the elder sibling (for same generation)
     * @param isOlderGeneration True if THIS person is in the older generation
     */
    private String getCousinTermVi(int degree, int removed, Gender gender, boolean parentIsElder, boolean isOlderGeneration) {
        // Same generation cousins (removed == 0)
        if (removed == 0) {
            if (degree == 1) {
                // First cousins - use anh/chị/em họ based on parent's seniority
                if (gender == Gender.MALE) {
                    return parentIsElder ? "anh họ" : "em họ (trai)";
                } else if (gender == Gender.FEMALE) {
                    return parentIsElder ? "chị họ" : "em họ (gái)";
                }
                return parentIsElder ? "anh/chị họ" : "em họ";
            }
            return "anh chị em họ đời thứ " + degree;
        }

        // Different generation - "cousin once removed" etc.
        if (isOlderGeneration) {
            // This person is in older generation - they are like an uncle/aunt to the other
            // chú họ (male), cô họ/dì họ (female)
            if (gender == Gender.MALE) {
                return "chú họ";
            } else if (gender == Gender.FEMALE) {
                return "cô họ";  // Could also be "dì họ" depending on context
            }
            return "chú/cô họ";
        } else {
            // This person is in younger generation - they are like a nephew/niece to the other
            if (gender == Gender.MALE) {
                return "cháu họ (trai)";
            } else if (gender == Gender.FEMALE) {
                return "cháu họ (gái)";
            }
            return "cháu họ";
        }
    }

    /**
     * Determine if person1's parent is the elder sibling in a cousin relationship.
     * This is used for Vietnamese cousin terms where seniority is based on parent's birth order.
     *
     * Path structure for cousins:
     * person1 -> parent1 -> ... -> commonAncestor -> ... -> parent2 -> person2
     *
     * The sibling step is where parent1's lineage meets parent2's lineage.
     */
    private boolean determineCousinParentSeniority(List<PathNode> path, RelationshipContext ctx) {
        // Find the sibling connection in the path
        // For first cousins: path is [person1, parent1, parent2, person2]
        // The sibling connection is between parent1 (index 1) and parent2 (index 2)

        if (ctx.siblingStepIndex > 0 && ctx.siblingStepIndex < path.size()) {
            // The node before sibling step is person1's ancestor (parent/grandparent)
            PathNode person1Ancestor = path.get(ctx.siblingStepIndex - 1);
            // The node at sibling step is person2's ancestor
            PathNode person2Ancestor = path.get(ctx.siblingStepIndex);

            // Compare birth dates to determine who is elder
            return isElderSibling(person2Ancestor.person, person1Ancestor.person);
        }

        // Default: assume person1's parent is elder
        return true;
    }

    // ==================== In-law Terms ====================

    private String[] getInLawTerms(RelationshipContext ctx, List<PathNode> path, Gender gender1, Gender gender2) {
        // Analyze the path to determine the in-law relationship
        int spouseIndex = -1;
        for (int i = 1; i < path.size(); i++) {
            if (path.get(i).connectionFromPrevious == ConnectionType.SPOUSE) {
                spouseIndex = i;
                break;
            }
        }

        // Count steps before and after spouse connection
        int stepsBeforeSpouse = spouseIndex;
        int stepsAfterSpouse = path.size() - 1 - spouseIndex;

        // Determine relationship type based on path structure
        // Convention: relFromP1 = what person1 is to person2 (use gender1)
        //             relFromP2 = what person2 is to person1 (use gender2)
        if (stepsBeforeSpouse == 1 && stepsAfterSpouse == 0) {
            // Direct spouse (already handled above, but just in case)
            return new String[]{
                getSpouseTermEn(gender1), getSpouseTermEn(gender2),
                getSpouseTermVi(gender1), getSpouseTermVi(gender2)
            };
        }

        if (stepsBeforeSpouse == 0 && stepsAfterSpouse == 1) {
            // person1 -> spouse -> parent of spouse = parent-in-law
            // person1 is child-in-law to person2, person2 is parent-in-law to person1
            ConnectionType afterSpouse = path.get(path.size() - 1).connectionFromPrevious;
            if (afterSpouse == ConnectionType.PARENT) {
                return new String[]{
                    gender1 == Gender.MALE ? "son-in-law" : gender1 == Gender.FEMALE ? "daughter-in-law" : "child-in-law",
                    gender2 == Gender.MALE ? "father-in-law" : gender2 == Gender.FEMALE ? "mother-in-law" : "parent-in-law",
                    gender1 == Gender.MALE ? "con rể" : gender1 == Gender.FEMALE ? "con dâu" : "con dâu/rể",
                    gender2 == Gender.MALE ? "bố chồng/bố vợ" : gender2 == Gender.FEMALE ? "mẹ chồng/mẹ vợ" : "bố mẹ chồng/vợ"
                };
            }
        }

        if (stepsBeforeSpouse == 1 && path.get(1).connectionFromPrevious == ConnectionType.PARENT) {
            // person1 -> parent -> spouse of parent = step-parent
            // person1 is step-child to person2, person2 is step-parent to person1
            return new String[]{
                gender1 == Gender.MALE ? "step-son" : gender1 == Gender.FEMALE ? "step-daughter" : "step-child",
                gender2 == Gender.MALE ? "step-father" : gender2 == Gender.FEMALE ? "step-mother" : "step-parent",
                gender1 == Gender.MALE ? "con riêng (trai)" : gender1 == Gender.FEMALE ? "con riêng (gái)" : "con riêng",
                gender2 == Gender.MALE ? "cha dượng" : gender2 == Gender.FEMALE ? "mẹ kế" : "cha dượng/mẹ kế"
            };
        }

        if (stepsBeforeSpouse == 1 && path.get(1).connectionFromPrevious == ConnectionType.CHILD) {
            // person1 -> child -> spouse of child = child-in-law
            // person1 is parent-in-law to person2, person2 is child-in-law to person1
            return new String[]{
                gender1 == Gender.MALE ? "father-in-law" : gender1 == Gender.FEMALE ? "mother-in-law" : "parent-in-law",
                gender2 == Gender.MALE ? "son-in-law" : gender2 == Gender.FEMALE ? "daughter-in-law" : "child-in-law",
                gender1 == Gender.MALE ? "bố vợ/bố chồng" : gender1 == Gender.FEMALE ? "mẹ vợ/mẹ chồng" : "bố mẹ vợ/chồng",
                gender2 == Gender.MALE ? "con rể" : gender2 == Gender.FEMALE ? "con dâu" : "con dâu/rể"
            };
        }

        if ((stepsBeforeSpouse == 1 || stepsBeforeSpouse == 2) && path.get(1).connectionFromPrevious == ConnectionType.SIBLING && stepsAfterSpouse == 0) {
            // person1 -> sibling -> spouse = spouse of person1's sibling
            // Example: Phạm Thị Hương -> sibling (Kim Cúc) -> spouse (Nam Hưng)
            // person1 (Hương) is sibling of person2's spouse (Kim Cúc)
            // person2 (Nam Hưng) is spouse of person1's sibling (Kim Cúc)

            // In Vietnamese:
            // - person1 is "anh/chị/em vợ" (if sibling is wife) or "anh/chị/em chồng" (if sibling is husband)
            // - person2 is "anh rể/chị dâu/em rể/em dâu" of person1

            // Get the sibling (person1's sibling = person2's spouse)
            Individual sibling = path.get(1).person;
            Individual person1Individual = path.get(0).person;
            boolean person1IsElder = isElderSibling(sibling, person1Individual);
            Gender siblingGender = sibling.getGender();

            // Determine if sibling is wife or husband of person2
            // If sibling is FEMALE, sibling is wife of person2, so person1 is "anh/chị/em vợ"
            // If sibling is MALE, sibling is husband of person2, so person1 is "anh/chị/em chồng"
            String spouseType = siblingGender == Gender.FEMALE ? "vợ" : "chồng";

            // person1's relationship to person2: person1 is sibling of person2's spouse
            String relFromP1En = gender1 == Gender.MALE ? "brother-in-law" : gender1 == Gender.FEMALE ? "sister-in-law" : "sibling-in-law";
            String relFromP1Vi;
            if (person1IsElder) {
                // person1 is elder sibling of person2's spouse
                if (gender1 == Gender.MALE) {
                    relFromP1Vi = "anh " + spouseType;
                } else if (gender1 == Gender.FEMALE) {
                    relFromP1Vi = "chị " + spouseType;
                } else {
                    relFromP1Vi = "anh/chị " + spouseType;
                }
            } else {
                // person1 is younger sibling of person2's spouse
                relFromP1Vi = "em " + spouseType;
            }

            // person2's relationship to person1: person2 is spouse of person1's sibling
            // If sibling is elder than person1: anh rể/chị dâu (spouse of elder sibling)
            // If sibling is younger than person1: em rể/em dâu (spouse of younger sibling)
            String relFromP2En = gender2 == Gender.MALE ? "brother-in-law" : gender2 == Gender.FEMALE ? "sister-in-law" : "sibling-in-law";
            String relFromP2Vi;
            if (person1IsElder) {
                // person1 is elder, sibling is younger, so person2 is spouse of younger sibling = em rể/em dâu
                relFromP2Vi = gender2 == Gender.MALE ? "em rể" : gender2 == Gender.FEMALE ? "em dâu" : "em dâu/rể";
            } else {
                // person1 is younger, sibling is elder, so person2 is spouse of elder sibling = anh rể/chị dâu
                relFromP2Vi = gender2 == Gender.MALE ? "anh rể" : gender2 == Gender.FEMALE ? "chị dâu" : "anh/chị dâu/rể";
            }

            return new String[]{ relFromP1En, relFromP2En, relFromP1Vi, relFromP2Vi };
        }

        if ((stepsBeforeSpouse == 0 || stepsBeforeSpouse == 1) && (stepsAfterSpouse == 1 || stepsAfterSpouse == 2)) {
            ConnectionType afterSpouse = path.get(path.size() - 1).connectionFromPrevious;
            if (afterSpouse == ConnectionType.SIBLING) {
                // person1 -> spouse -> sibling of spouse = spouse's sibling
                // Example: Nam Hưng -> spouse (Kim Cúc) -> sibling (Hương)
                // person1 (Nam Hưng) is spouse of person2's sibling (Kim Cúc)
                // person2 (Hương) is sibling of person1's spouse (Kim Cúc)

                // Get the spouse and sibling to determine elder/younger
                Individual spouse = path.get(spouseIndex).person;
                Individual person2Individual = path.get(path.size() - 1).person;
                boolean person2IsElder = isElderSibling(spouse, person2Individual);
                Gender spouseGender = spouse.getGender();

                // Determine if spouse is wife or husband of person1
                // If spouse is FEMALE, spouse is wife of person1, so person2 is "anh/chị/em vợ"
                // If spouse is MALE, spouse is husband of person1, so person2 is "anh/chị/em chồng"
                String spouseType = spouseGender == Gender.FEMALE ? "vợ" : "chồng";

                // person1's relationship to person2: person1 is spouse of person2's sibling
                // If person2's sibling (person1's spouse) is elder than person2: em rể/em dâu
                // If person2's sibling (person1's spouse) is younger than person2: anh rể/chị dâu
                String relFromP1En = gender1 == Gender.MALE ? "brother-in-law" : gender1 == Gender.FEMALE ? "sister-in-law" : "sibling-in-law";
                String relFromP1Vi;
                if (person2IsElder) {
                    // person2 is elder than spouse, so person1 is spouse of younger sibling = em rể/em dâu
                    relFromP1Vi = gender1 == Gender.MALE ? "em rể" : gender1 == Gender.FEMALE ? "em dâu" : "em dâu/rể";
                } else {
                    // person2 is younger than spouse, so person1 is spouse of elder sibling = anh rể/chị dâu
                    relFromP1Vi = gender1 == Gender.MALE ? "anh rể" : gender1 == Gender.FEMALE ? "chị dâu" : "anh/chị dâu/rể";
                }

                // person2's relationship to person1: person2 is sibling of person1's spouse
                String relFromP2En = gender2 == Gender.MALE ? "brother-in-law" : gender2 == Gender.FEMALE ? "sister-in-law" : "sibling-in-law";
                String relFromP2Vi;
                if (person2IsElder) {
                    // person2 is elder sibling of person1's spouse
                    if (gender2 == Gender.MALE) {
                        relFromP2Vi = "anh " + spouseType;
                    } else if (gender2 == Gender.FEMALE) {
                        relFromP2Vi = "chị " + spouseType;
                    } else {
                        relFromP2Vi = "anh/chị " + spouseType;
                    }
                } else {
                    // person2 is younger sibling of person1's spouse
                    relFromP2Vi = "em " + spouseType;
                }

                return new String[]{ relFromP1En, relFromP2En, relFromP1Vi, relFromP2Vi };
            }
        }

        // Generic in-law fallback
        return new String[]{
            "relative by marriage",
            "relative by marriage",
            "họ hàng bên chồng/vợ",
            "họ hàng bên chồng/vợ"
        };
    }

    // ==================== Helper Methods ====================

    private List<PathStep> buildPathSteps(List<PathNode> path) {
        List<PathStep> pathSteps = new ArrayList<>();
        for (int i = 0; i < path.size(); i++) {
            PathNode node = path.get(i);
            String relToNext = "";
            String relToNextVi = "";
            if (i < path.size() - 1) {
                PathNode nextNode = path.get(i + 1);
                relToNext = getConnectionTypeNameEn(nextNode.connectionFromPrevious, nextNode.person.getGender());
                relToNextVi = getConnectionTypeNameVi(nextNode.connectionFromPrevious, nextNode.person.getGender());
            }
            pathSteps.add(PathStep.builder()
                    .person(toPersonSummary(node.person))
                    .relationshipToNext(relToNext)
                    .relationshipToNextVi(relToNextVi)
                    .build());
        }
        return pathSteps;
    }

    private String getConnectionTypeNameEn(ConnectionType type, Gender gender) {
        switch (type) {
            case PARENT:
                return gender == Gender.MALE ? "father" : gender == Gender.FEMALE ? "mother" : "parent";
            case CHILD:
                return gender == Gender.MALE ? "son" : gender == Gender.FEMALE ? "daughter" : "child";
            case SPOUSE:
                return gender == Gender.MALE ? "husband" : gender == Gender.FEMALE ? "wife" : "spouse";
            case SIBLING:
                return gender == Gender.MALE ? "brother" : gender == Gender.FEMALE ? "sister" : "sibling";
            default:
                return "relative";
        }
    }

    private String getConnectionTypeNameVi(ConnectionType type, Gender gender) {
        switch (type) {
            case PARENT:
                return gender == Gender.MALE ? "cha" : gender == Gender.FEMALE ? "mẹ" : "cha/mẹ";
            case CHILD:
                return gender == Gender.MALE ? "con trai" : gender == Gender.FEMALE ? "con gái" : "con";
            case SPOUSE:
                return gender == Gender.MALE ? "chồng" : gender == Gender.FEMALE ? "vợ" : "vợ/chồng";
            case SIBLING:
                return gender == Gender.MALE ? "anh/em trai" : gender == Gender.FEMALE ? "chị/em gái" : "anh chị em";
            default:
                return "họ hàng";
        }
    }

    // ==================== Response Builders ====================

    private RelationshipPathResponse buildSamePersonResponse(Individual person) {
        PersonSummary summary = toPersonSummary(person);
        return RelationshipPathResponse.builder()
                .person1(summary)
                .person2(summary)
                .relationshipFromPerson1("self")
                .relationshipFromPerson2("self")
                .relationshipFromPerson1Vi("chính mình")
                .relationshipFromPerson2Vi("chính mình")
                .category(RelationshipCategory.SELF)
                .generationDifference(0)
                .path(List.of(PathStep.builder()
                        .person(summary)
                        .relationshipToNext("")
                        .relationshipToNextVi("")
                        .build()))
                .relationshipFound(true)
                .build();
    }

    private RelationshipPathResponse buildNotRelatedResponse(Individual person1, Individual person2) {
        return RelationshipPathResponse.builder()
                .person1(toPersonSummary(person1))
                .person2(toPersonSummary(person2))
                .relationshipFromPerson1("not related")
                .relationshipFromPerson2("not related")
                .relationshipFromPerson1Vi("không có quan hệ họ hàng")
                .relationshipFromPerson2Vi("không có quan hệ họ hàng")
                .category(RelationshipCategory.NOT_RELATED)
                .generationDifference(0)
                .path(new ArrayList<>())
                .relationshipFound(false)
                .build();
    }

    private PersonSummary toPersonSummary(Individual individual) {
        if (individual == null) return null;
        String fullName = buildFullName(individual);
        return PersonSummary.builder()
                .id(individual.getId())
                .fullName(fullName)
                .givenName(individual.getGivenName())
                .surname(individual.getSurname())
                .gender(individual.getGender() != null ? individual.getGender().name() : null)
                .birthDate(individual.getBirthDate())
                .deathDate(individual.getDeathDate())
                .build();
    }

    /**
     * Build full name - Vietnamese name order: Surname + Suffix + Given Name
     */
    private String buildFullName(Individual individual) {
        StringBuilder fullName = new StringBuilder();
        if (individual.getSurname() != null && !individual.getSurname().isEmpty()) {
            fullName.append(individual.getSurname());
        }
        if (individual.getSuffix() != null && !individual.getSuffix().isEmpty()) {
            if (fullName.length() > 0) fullName.append(" ");
            fullName.append(individual.getSuffix());
        }
        if (individual.getGivenName() != null && !individual.getGivenName().isEmpty()) {
            if (fullName.length() > 0) fullName.append(" ");
            fullName.append(individual.getGivenName());
        }
        return fullName.length() > 0 ? fullName.toString() : "Unknown";
    }
}
