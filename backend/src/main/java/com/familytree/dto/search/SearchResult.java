package com.familytree.dto.search;

import com.familytree.model.Gender;
import com.familytree.model.Individual;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Search result for an individual
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SearchResult {

    private String id;
    private String givenName;
    private String surname;
    private String fullName;
    private Gender gender;
    private LocalDate birthDate;
    private String birthPlace;
    private LocalDate deathDate;
    private String deathPlace;
    private String treeId;
    private String treeName;
    private double relevanceScore;

    /**
     * Create from Individual entity
     */
    public static SearchResult fromEntity(Individual individual) {
        return SearchResult.builder()
                .id(individual.getId().toString())
                .givenName(individual.getGivenName())
                .surname(individual.getSurname())
                .fullName(individual.getGivenName() + " " + individual.getSurname())
                .gender(individual.getGender())
                .birthDate(individual.getBirthDate())
                .birthPlace(individual.getBirthPlace())
                .deathDate(individual.getDeathDate())
                .deathPlace(individual.getDeathPlace())
                .treeId(individual.getTree().getId().toString())
                .treeName(individual.getTree().getName())
                .relevanceScore(1.0)
                .build();
    }
}
