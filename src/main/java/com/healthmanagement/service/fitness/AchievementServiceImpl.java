package com.healthmanagement.service.fitness;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.healthmanagement.model.member.User;

import com.healthmanagement.dto.fitness.AchievementDTO;
import com.healthmanagement.dao.fitness.*;
import com.healthmanagement.dao.member.*;
import com.healthmanagement.model.fitness.Achievements;

import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.time.format.DateTimeFormatter;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AchievementServiceImpl implements AchievementService {

	private final AchievementsDAO achievementsRepo;
	private final UserDAO userRepo;
	private final AchievementDefinitionDAO achievementDefinitionRepo;

	@Override
	public List<AchievementDTO> getUserAchievements(Integer userId) {
		return achievementsRepo.findByUserId(userId).stream().map(this::convertToDto).collect(Collectors.toList());
	}

	@Transactional
	@Override
	public void checkAndAwardAchievements(Integer userId, String triggerEvent, Object data) {
		System.out.println("AchievementServiceImpl - checkAndAwardAchievements 被調用了！使用者 ID: " + userId + ", 事件: " + triggerEvent + ", 數據: " + data);
	    achievementDefinitionRepo.findByTriggerEvent(triggerEvent).forEach(definition -> {
	       			boolean shouldAward = false;

			switch (triggerEvent) {
			case "WORKOUT_CREATED":
				if (data instanceof Integer) {
					int workoutCount = (Integer) data;
					switch (definition.getAchievementType()) {
					case "FIRST_WORKOUT":
						shouldAward = workoutCount == 1;
						break;
					case "5_WORKOUTS":
						shouldAward = workoutCount == 5;
						break;
					case "10_WORKOUTS":
						shouldAward = workoutCount == 10;
						break;
					case "25_WORKOUTS":
						shouldAward = workoutCount == 25;
						break;
					case "50_WORKOUTS":
						shouldAward = workoutCount == 50;
						break;
					}
				}
				break;
			case "USER_LOGGED_IN": // 假設有這個事件
				if (data instanceof Integer) {
					int consecutiveDays = (Integer) data;
					switch (definition.getAchievementType()) {
					case "LOGIN_1_DAY":
						shouldAward = consecutiveDays == 1;
						break;
					case "LOGIN_3_DAYS":
						shouldAward = consecutiveDays == 3;
						break;
					case "LOGIN_7_DAYS":
						shouldAward = consecutiveDays == 7;
						break;
					case "LOGIN_30_DAYS":
						shouldAward = consecutiveDays == 30;
						break;
					case "LOGIN_90_DAYS":
						shouldAward = consecutiveDays == 90;
						break;
					}
				}
				break;
			case "GOAL_CREATED":
				if (definition.getAchievementType().equals("GOAL_CREATED")) {
					shouldAward = true;
				}
				break;
			case "GOAL_COMPLETED":
				if (data instanceof Integer) {
					int completedGoals = (Integer) data;
					switch (definition.getAchievementType()) {
					case "GOAL_COMPLETED_1":
						shouldAward = completedGoals == 1;
						break;
					case "GOAL_COMPLETED_5":
						shouldAward = completedGoals == 5;
						break;
					case "GOAL_COMPLETED_10":
						shouldAward = completedGoals == 10;
						break;
					case "GOAL_COMPLETED_25":
						shouldAward = completedGoals == 25;
						break;
					}
				}
				break;
			case "BODY_DATA_CREATED":
				if (data instanceof Integer) {
					int bodyDataCount = (Integer) data;
					switch (definition.getAchievementType()) {
					case "FIRST_BODY_DATA":
						shouldAward = bodyDataCount == 1;
						break;
					case "10_BODY_DATA":
						shouldAward = bodyDataCount == 10;
						break;
					case "25_BODY_DATA":
						shouldAward = bodyDataCount == 25;
						break;
					}
				}
				break;
			case "DIET_DATA_CREATED":
				if (data instanceof Integer) {
					int dietLogCount = (Integer) data;
					switch (definition.getAchievementType()) {
					case "FIRST_DIET_LOG":
						shouldAward = dietLogCount == 1;
						break;
					case "10_DIET_LOGS":
						shouldAward = dietLogCount == 10;
						break;
					case "25_DIET_LOGS":
						shouldAward = dietLogCount == 25;
						break;
					}
				}
				break;
			case "SOCIAL_POST_CREATED":
				if (data instanceof Integer) {
					int postCount = (Integer) data;
					switch (definition.getAchievementType()) {
					case "FIRST_POST":
						shouldAward = postCount == 1;
						break;
					case "5_POSTS":
						shouldAward = postCount == 5;
						break;
					case "10_POSTS":
						shouldAward = postCount == 10;
						break;
					}
				}
				break;
			case "SOCIAL_COMMENT_CREATED":
				if (data instanceof Integer) {
					int commentCount = (Integer) data;
					switch (definition.getAchievementType()) {
					case "FIRST_COMMENT":
						shouldAward = commentCount == 1;
						break;
					case "5_COMMENTS":
						shouldAward = commentCount == 5;
						break;
					case "10_COMMENTS":
						shouldAward = commentCount == 10;
						break;
					}
				}
				break;
			// 可以根據 achievement_definitions 表中的 trigger_event 繼續添加 case
			}
			 if (shouldAward) {
	                Optional<Achievements> existingAchievement;
	                try {
	                    existingAchievement = achievementsRepo.findByUserIdAndAchievementType(userId,
	                            definition.getAchievementType());
	                    if (existingAchievement.isEmpty()) {
	                        addAchievementInternal(userId, definition.getAchievementType(), definition.getTitle(),
	                                definition.getDescription());
	                    }
	                } catch (Exception e) {
	                    System.err.println("檢查或頒發獎章時發生錯誤: " + e.getMessage());
	                    e.printStackTrace(); // 打印更詳細的錯誤堆疊
	                    // 這裡可以選擇繼續處理下一個獎章定義，或者直接返回
	                }
	            }
	        });

		// TODO: 處理基於 trigger_condition 的獎章 (需要定期檢查)
		// 這部分可以使用 @Scheduled 註解的方法來實現，
		// 需要解析 trigger_condition 字串並評估是否滿足條件。
	}

	// 內部方法，避免在 checkAndAwardAchievements 中直接調用 public addAchievement 導致可能的
	@Transactional
	private void addAchievementInternal(Integer userId, String achievementType, String title, String description) {
		System.out.println("AchievementServiceImpl - addAchievementInternal - 開始頒發獎章:");
		System.out.println("AchievementServiceImpl - addAchievementInternal -   使用者 ID: " + userId);
		System.out.println("AchievementServiceImpl - addAchievementInternal -   獎章類型: " + achievementType);
		System.out.println("AchievementServiceImpl - addAchievementInternal -   標題: " + title);
		System.out.println("AchievementServiceImpl - addAchievementInternal -   描述: " + description);

		Achievements achievement = Achievements.builder().userId(userId).achievementType(achievementType).title(title)
				.description(description).achievedDate(LocalDate.now()).build();
		System.out.println("AchievementServiceImpl - addAchievementInternal - 準備保存獎章到資料庫...");
		achievementsRepo.save(achievement);
		System.out.println("AchievementServiceImpl - addAchievementInternal - 獎章已成功保存到資料庫。");
	}

	@Override
	public AchievementDTO addAchievement(Integer userId, String achievementType, String title, String description) {
		Achievements achievement = Achievements.builder().userId(userId).achievementType(achievementType).title(title)
				.description(description).achievedDate(LocalDate.now()).build();
		Achievements savedAchievement = achievementsRepo.save(achievement);
		return convertToDto(savedAchievement);
	}

	@Override
	public Page<AchievementDTO> getAllAchievements(Pageable pageable) {
		return achievementsRepo.findAll(pageable).map(this::convertToDto);
	}

	@Override
	public AchievementDTO getAchievementById(Integer achievementId) {
		return achievementsRepo.findById(achievementId).map(this::convertToDto)
				.orElseThrow(() -> new EntityNotFoundException("Achievement not found with id: " + achievementId));
	}

	@Override
	public void deleteAchievement(Integer achievementId) {
		achievementsRepo.deleteById(achievementId);
	}

	@Override
	public Page<AchievementDTO> searchAchievements(Integer userId, String name, String achievementType, String title,
			String startDate, String endDate, Pageable pageable) {
		Specification<Achievements> spec = (root, query, criteriaBuilder) -> {
			java.util.List<Predicate> predicates = new java.util.ArrayList<>();

			if (userId != null) {
				predicates.add(criteriaBuilder.equal(root.get("userId"), userId));
			}

			if (name != null && !name.isEmpty()) {
				// 使用 JOIN 查詢 user 表格的 name
				jakarta.persistence.criteria.Join<Achievements, User> userJoin = root.join("user");
				predicates.add(criteriaBuilder.like(userJoin.get("name"), "%" + name + "%"));
			}

			if (achievementType != null && !achievementType.isEmpty()) {
				predicates.add(criteriaBuilder.equal(root.get("achievementType"), achievementType));
			}

			if (title != null && !title.isEmpty()) {
				predicates.add(criteriaBuilder.like(root.get("title"), "%" + title + "%"));
			}

			DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE;
			if (startDate != null && !startDate.isEmpty()) {
				predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("achievedDate"),
						LocalDate.parse(startDate, formatter)));
			}

			if (endDate != null && !endDate.isEmpty()) {
				predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("achievedDate"),
						LocalDate.parse(endDate, formatter)));
			}

			return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
		};

		return achievementsRepo.findAll(spec, pageable).map(this::convertToDto);
	}

	private AchievementDTO convertToDto(Achievements achievement) {
		return AchievementDTO.builder().achievementId(achievement.getAchievementId()).userId(achievement.getUserId())
				.achievementType(achievement.getAchievementType()).title(achievement.getTitle())
				.description(achievement.getDescription()).achievedDate(achievement.getAchievedDate()).build();
	}
}