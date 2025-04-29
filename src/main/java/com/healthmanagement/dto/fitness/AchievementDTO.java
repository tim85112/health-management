package com.healthmanagement.dto.fitness;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@Builder
public class AchievementDTO {
	private Integer achievementId;
	private Integer userId;
	private String achievementType;
	private String title;
	private String description;
	private LocalDate achievedDate;

	public AchievementDTO(Integer achievementId, Integer userId, String achievementType, String title,
			String description, LocalDate achievedDate) {
		super();
		this.achievementId = achievementId;
		this.userId = userId;
		this.achievementType = achievementType;
		this.title = title;
		this.description = description;
		this.achievedDate = achievedDate;
	}

}