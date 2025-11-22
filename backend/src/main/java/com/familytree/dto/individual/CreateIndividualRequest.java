package com.familytree.dto.individual;

import com.familytree.model.Gender;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Size;
import java.time.LocalDate;

/**
 * DTO for creating a new individual
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateIndividualRequest {

    @Size(max = 255, message = "Given name must not exceed 255 characters")
    private String givenName;

    @Size(max = 255, message = "Surname must not exceed 255 characters")
    private String surname;

    @Size(max = 50, message = "Suffix must not exceed 50 characters")
    private String suffix;

    private Gender gender;

    private LocalDate birthDate;

    @Size(max = 500, message = "Birth place must not exceed 500 characters")
    private String birthPlace;

    private LocalDate deathDate;

    @Size(max = 500, message = "Death place must not exceed 500 characters")
    private String deathPlace;

    @Size(max = 10000, message = "Biography must not exceed 10000 characters")
    private String biography;
}
