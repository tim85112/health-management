package com.healthmanagement.model.fitness;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.healthmanagement.model.member.User;

@Entity
@Table(name = "achievements")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Achievements {
	 @Id
	    @GeneratedValue(strategy = GenerationType.IDENTITY)
	    @Column(name = "achievement_id")  
	    private Integer achievementId;

	    @Column(name = "user_id", nullable = false)  
	    private Integer userId;

	    @Column(name = "achievement_type", length = 50 , columnDefinition = "NVARCHAR(50)")  
	    private String achievementType;

	    @Column(name = "title", nullable = false, length = 100 , columnDefinition = "NVARCHAR(100)")  
	    private String title;

	    @Column(name = "description", columnDefinition = "TEXT" )  
	    private String description;

	    @Column(name = "achieved_date", nullable = false)  
	    private LocalDate achievedDate;

	    @ManyToOne
	    @JoinColumn(name = "user_id", nullable = false, insertable = false, updatable = false) 
            @JsonIgnore
	    private User user;
}