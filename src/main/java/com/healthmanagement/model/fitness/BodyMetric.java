package com.healthmanagement.model.fitness;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

import com.healthmanagement.model.member.User;

@Entity
@Table(name = "body_metrics")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BodyMetric {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")  
    private Integer id;

    @Column(name = "user_id", nullable = false)  
    private Integer userId;

    @Column(name = "weight", nullable = false)  
    private Double weight;

    @Column(name = "body_fat")  
    private Double bodyFat;

    @Column(name = "muscle_mass") 
    private Double muscleMass;

    @Column(name = "waist_circumference")  
    private Double waistCircumference;

    @Column(name = "hip_circumference")  
    private Double hipCircumference;

    @Column(name = "height", nullable = false)  
    private Double height;
    
    @Column(name = "bmi")
    private Double bmi;

    @Column(name = "date_recorded", nullable = false)  
    private LocalDate dateRecorded;
    
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false, insertable = false, updatable = false) 
	private User user;

}
