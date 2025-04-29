package com.healthmanagement.service.fitness;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.Comparator;
import java.time.DayOfWeek;
import java.time.temporal.TemporalAdjusters;



import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.healthmanagement.dao.fitness.BodyMetricDAO;
import com.healthmanagement.dao.fitness.ExerciseRecordDAO;
import com.healthmanagement.dao.fitness.ExerciseTypeCoefficientDAO;
import com.healthmanagement.dto.fitness.ExerciseRecordDTO;
import com.healthmanagement.dto.fitness.OverviewDataDTO;
import com.healthmanagement.model.fitness.BodyMetric;
import com.healthmanagement.model.fitness.ExerciseRecord;
import com.healthmanagement.model.fitness.ExerciseTypeCoefficient;
import com.healthmanagement.model.member.User;
import com.healthmanagement.service.member.UserService;

@Service
@RequiredArgsConstructor
public class ExerciseServiceImpl implements ExerciseService {

    private final ExerciseRecordDAO exerciseRecordRepo;
    private final BodyMetricDAO bodyMetricRepo;
    private final ExerciseTypeCoefficientDAO exerciseTypeCoefficientRepo;
    private final AchievementService achievementService;

    @Autowired(required = false)
    private UserService userService;

    @Transactional
    @Override
    public ExerciseRecordDTO saveExerciseRecord(ExerciseRecordDTO exerciseRecordDTO) {
        // 步驟 1: 從 BodyMetrics 中獲取用戶體重
        List<BodyMetric> bodyMetrics = bodyMetricRepo.findByUserId(exerciseRecordDTO.getUserId());
        if (bodyMetrics == null || bodyMetrics.isEmpty()) {
            throw new EntityNotFoundException("找不到用戶的身體數據。");
        }
        BodyMetric bodyMetric = bodyMetrics.get(0); // 或使用排序後的第一筆最新資料

        // 步驟 2: 根據運動類型獲取 MET 值
        ExerciseTypeCoefficient exerciseType = exerciseTypeCoefficientRepo
                .findByExerciseName(exerciseRecordDTO.getExerciseType())
                .orElseThrow(() -> new EntityNotFoundException("找不到該運動類型的 MET 值。"));

        // 步驟 3: 計算消耗的卡路里
        double weight = bodyMetric.getWeight(); // 用戶的體重
        double met = exerciseType.getMet().doubleValue(); // 運動的 MET 值
        double durationInHours = exerciseRecordDTO.getExerciseDuration() / 60.0; // 將運動時長轉換為小時

        // 計算消耗的卡路里
        double caloriesBurned = met * weight * durationInHours;

        // 設置計算後的卡路里消耗
        exerciseRecordDTO.setCaloriesBurned(caloriesBurned);

        // 步驟 4: 保存運動紀錄
        ExerciseRecord exerciseRecord = new ExerciseRecord();
        exerciseRecord.setUserId(exerciseRecordDTO.getUserId());
        exerciseRecord.setExerciseType(exerciseRecordDTO.getExerciseType());
        exerciseRecord.setExerciseDuration(exerciseRecordDTO.getExerciseDuration());
        exerciseRecord.setCaloriesBurned(caloriesBurned);
        exerciseRecord.setExerciseDate(exerciseRecordDTO.getExerciseDate());

        // 保存運動紀錄
        ExerciseRecord savedRecord = exerciseRecordRepo.save(exerciseRecord);

        // 步驟 5: 檢查並頒發運動相關的獎章
        Integer userId = exerciseRecordDTO.getUserId();
        long workoutCount = exerciseRecordRepo.countByUser_Id(userId); // 取得該使用者的運動總次數
        achievementService.checkAndAwardAchievements(userId, "WORKOUT_CREATED", (int) workoutCount);

        // 返回 DTO
        return toDTO(savedRecord);
    }

    @Override
    public void deleteExerciseRecord(Integer recordId) {
        exerciseRecordRepo.deleteById(recordId);
    }

    @Override
    public List<ExerciseRecordDTO> getExerciseRecordsByUserId(Integer userId) {
        List<Object[]> results = exerciseRecordRepo.findExerciseRecordsWithUserNameByUserId(userId);
        return results.stream()
                .map(result -> {
                    ExerciseRecord exerciseRecord = (ExerciseRecord) result[0];
                    String userName = (String) result[1];
                    return new ExerciseRecordDTO(exerciseRecord, userName);
                })
                .collect(Collectors.toList());
    }

    @Transactional
    @Override
    public ExerciseRecordDTO updateExerciseRecord(Integer recordId, ExerciseRecordDTO exerciseRecordDTO) {
        Optional<ExerciseRecord> existingRecordOptional = exerciseRecordRepo.findById(recordId);
        return existingRecordOptional.map(record -> {
            record.setExerciseType(exerciseRecordDTO.getExerciseType());
            record.setExerciseDuration(exerciseRecordDTO.getExerciseDuration());
            record.setExerciseDate(exerciseRecordDTO.getExerciseDate());

            // 重新計算 caloriesBurned
            ExerciseTypeCoefficient exerciseType = exerciseTypeCoefficientRepo
                    .findByExerciseName(exerciseRecordDTO.getExerciseType())
                    .orElseThrow(() -> new EntityNotFoundException("找不到該運動類型的 MET 值。"));

            List<BodyMetric> bodyMetrics = bodyMetricRepo.findByUserId(record.getUserId());
            if (bodyMetrics == null || bodyMetrics.isEmpty()) {
                throw new EntityNotFoundException("找不到用戶的身體數據。");
            }
            BodyMetric bodyMetric = bodyMetrics.get(0); // 或使用排序後的第一筆最新資料

            double weight = bodyMetric.getWeight();
            double met = exerciseType.getMet().doubleValue();
            double durationInHours = exerciseRecordDTO.getExerciseDuration() / 60.0;
            double caloriesBurned = met * weight * durationInHours;
            record.setCaloriesBurned(caloriesBurned);

            return toDTO(exerciseRecordRepo.save(record));
        }).orElse(null);
    }

    @Override
    public List<ExerciseRecordDTO> getExerciseRecordsByUserIdAndUserName(Integer userId, String userName) {
        Specification<ExerciseRecord> spec = (root, query, criteriaBuilder) -> {
            Join<ExerciseRecord, User> userJoin = root.join("user"); // 假設 ExerciseRecord 有 user 欄位
            Predicate userIdPredicate = criteriaBuilder.equal(root.get("userId"), userId);
            Predicate userNamePredicate = criteriaBuilder.like(userJoin.get("name"), "%" + userName + "%");
            return criteriaBuilder.and(userIdPredicate, userNamePredicate);
        };
        return exerciseRecordRepo.findAll(spec).stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Override
    public List<ExerciseRecordDTO> getExerciseRecordsByUserName(String userName) {
        Specification<ExerciseRecord> spec = (root, query, criteriaBuilder) -> {
            Join<ExerciseRecord, User> userJoin = root.join("user"); // 假設 ExerciseRecord 有 user 欄位
            return criteriaBuilder.like(userJoin.get("name"), "%" + userName + "%");
        };
        return exerciseRecordRepo.findAll(spec).stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Override
    public Page<ExerciseRecordDTO> getAllExerciseRecords(Pageable pageable, Integer userId, String userName,
            String exerciseType, String startDate, String endDate) {
        LocalDate startLocalDate = parseDate(startDate);
        LocalDate endLocalDate = parseDate(endDate);

        Specification<ExerciseRecord> spec = (root, query, criteriaBuilder) -> {
            Predicate predicate = criteriaBuilder.conjunction();

            if (userId != null) {
                predicate = criteriaBuilder.and(predicate, criteriaBuilder.equal(root.get("userId"), userId));
            }

            if (userName != null && !userName.isEmpty()) {
                // 假設 userName 儲存在 User 表中，你需要 join 查詢
                Join<ExerciseRecord, User> userJoin = root.join("user"); // 假設 ExerciseRecord 有 user 欄位
                predicate = criteriaBuilder.and(predicate,
                        criteriaBuilder.like(userJoin.get("name"), "%" + userName + "%"));
            }

            if (exerciseType != null && !exerciseType.isEmpty()) {
                predicate = criteriaBuilder.and(predicate,
                        criteriaBuilder.equal(root.get("exerciseType"), exerciseType));
            }

            if (startLocalDate != null && endLocalDate != null) {
                predicate = criteriaBuilder.and(predicate,
                        criteriaBuilder.between(root.get("exerciseDate"), startLocalDate, endLocalDate));
            } else if (startLocalDate != null) {
                predicate = criteriaBuilder.and(predicate,
                        criteriaBuilder.greaterThanOrEqualTo(root.get("exerciseDate"), startLocalDate));
            } else if (endLocalDate != null) {
                predicate = criteriaBuilder.and(predicate,
                        criteriaBuilder.lessThanOrEqualTo(root.get("exerciseDate"), endLocalDate));
            }

            return predicate;
        };

        Page<ExerciseRecord> exerciseRecordPage = exerciseRecordRepo.findAll(spec, pageable);
        return exerciseRecordPage.map(this::toDTO);
    }
    @Override
    public OverviewDataDTO getOverviewDataForUser(Integer userId) {
        return getOverviewDataForUser(userId, "week");
    }

    // 新的帶有時間範圍的方法
    @Override
    public OverviewDataDTO getOverviewDataForUser(Integer userId, String timeRange) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate;

        // 根據不同的時間範圍設定起始日期
        switch (timeRange) {
            case "week":
            	  startDate = endDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                  break;
              case "month":
                  // 本月（從本月第一天到今天）
                  startDate = endDate.withDayOfMonth(1);
                  break;
              case "quarter":
                  // 本季（從本季第一天到今天）
                  int currentMonth = endDate.getMonthValue();
                  int firstMonthOfQuarter = ((currentMonth - 1) / 3) * 3 + 1;
                  startDate = endDate.withMonth(firstMonthOfQuarter).withDayOfMonth(1);
                  break;
              default:
                  startDate = endDate.minusWeeks(1);
          }

          // 調試日誌
          System.out.println("Time Range: " + timeRange);
          System.out.println("Start Date: " + startDate);
          System.out.println("End Date: " + endDate);

        // 使用日期範圍查詢
        List<ExerciseRecord> userRecords = exerciseRecordRepo.findByUserIdAndExerciseDateBetween(
            userId, 
            startDate, 
            endDate
        );
        System.out.println("Records Count: " + userRecords.size());
        int totalWorkoutTime = userRecords.stream()
                .mapToInt(ExerciseRecord::getExerciseDuration)
                .sum();

        double totalCaloriesBurned = userRecords.stream()
                .mapToDouble(ExerciseRecord::getCaloriesBurned)
                .sum();

        int workoutCount = userRecords.size();

        // 計算連續運動天數的改進邏輯
        int consecutiveDays = calculateConsecutiveWorkoutDays(userRecords);

        return new OverviewDataDTO(
            totalWorkoutTime, 
            totalCaloriesBurned, 
            workoutCount, 
            consecutiveDays
        );
    }

    // 連續運動天數計算方法（之前提供的方法）
    private int calculateConsecutiveWorkoutDays(List<ExerciseRecord> records) {
        if (records == null || records.isEmpty()) {
            return 0;
        }

        // 去重複並降序排序
        List<LocalDate> sortedUniqueDates = records.stream()
            .map(ExerciseRecord::getExerciseDate)
            .distinct()
            .sorted(Comparator.reverseOrder())
            .collect(Collectors.toList());

        int consecutiveDays = 0;
        LocalDate currentDate = LocalDate.now();

        for (LocalDate workoutDate : sortedUniqueDates) {
            if (workoutDate.isEqual(currentDate)) {
                consecutiveDays++;
                currentDate = currentDate.minusDays(1);
            } else if (workoutDate.isEqual(currentDate.minusDays(1))) {
                consecutiveDays++;
                currentDate = currentDate.minusDays(1);
            } else {
                break; // 如果日期不連續，則停止計算
            }
        }

        return consecutiveDays;
    }


    private LocalDate parseDate(String dateStr) {
        if (dateStr != null && !dateStr.isEmpty()) {
            try {
                return LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            } catch (DateTimeParseException e) {
                System.err.println("日期格式錯誤: " + e.getMessage());
                return null;
            }
        }
        return null;
    }

    private ExerciseRecordDTO toDTO(ExerciseRecord exerciseRecord) {
        String userName = null;
        if (exerciseRecord.getUser() != null) {
            userName = exerciseRecord.getUser().getName(); 
        }
        return new ExerciseRecordDTO(exerciseRecord, userName);
    }
}