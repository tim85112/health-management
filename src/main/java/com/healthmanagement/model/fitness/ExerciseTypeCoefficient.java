package com.healthmanagement.model.fitness;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "exercise_type_coefficients")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExerciseTypeCoefficient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "exercise_type_id")  
    private Integer exerciseTypeId;

    @Column(name = "exercise_name", nullable = false, length = 100 , columnDefinition = "NVARCHAR(50)") 
    private String exerciseName;

    @Column(name = "met", nullable = false)  
    private Double met;
}
