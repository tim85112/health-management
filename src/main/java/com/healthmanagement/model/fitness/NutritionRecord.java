package com.healthmanagement.model.fitness;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "nutrition_records")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NutritionRecord {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "record_id")  
    private Integer recordId;

    @Column(name = "user_id", nullable = false)  
    private Integer userId;

    @Column(name = "food_name", nullable = false, length = 100 , columnDefinition = "NVARCHAR(50)")  
    private String foodName;

    @Column(name = "calories")  
    private Integer calories;

    @Column(name = "protein")  
    private Float protein;

    @Column(name = "carbs")  
    private Float carbs;

    @Column(name = "fats")  
    private Float fats;

    @Column(name = "mealtime", length = 50 , columnDefinition = "NVARCHAR(50)")  
    private String mealtime;

    @Column(name = "record_date", nullable = false)  
    private LocalDateTime recordDate;
}
