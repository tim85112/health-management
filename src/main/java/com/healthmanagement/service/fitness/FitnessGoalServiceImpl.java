package com.healthmanagement.service.fitness;

import com.healthmanagement.dao.fitness.FitnessGoalDAO;
import com.healthmanagement.dao.member.UserDAO;
import com.healthmanagement.dto.fitness.BodyMetricDTO;
import com.healthmanagement.dto.fitness.FitnessGoalDTO;
import com.healthmanagement.model.fitness.FitnessGoal;
import com.healthmanagement.model.member.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class FitnessGoalServiceImpl implements FitnessGoalService {

    private final FitnessGoalDAO fitnessGoalRepo;
    private final UserDAO userRepo;
    private final AchievementService achievementService;
    private final BodyMetricService bodyMetricService; // 注入 BodyMetricService


    @Override
    public FitnessGoalDTO createFitnessGoal(FitnessGoalDTO fitnessGoalDTO) {
        User user = userRepo.findById(fitnessGoalDTO.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));
        FitnessGoal fitnessGoal = mapToEntity(fitnessGoalDTO, user);

        // 根據目標類型抓取最近一次的身體數據並設定起始值
        setStartValues(fitnessGoal, fitnessGoalDTO.getUserId());

        FitnessGoal savedFitnessGoal = fitnessGoalRepo.save(fitnessGoal);
        // 檢查並頒發 "目標設定者" 獎章
        System.out.println("FitnessGoalServiceImpl - createFitnessGoal - 觸發獎章檢查 - 使用者 ID: " + fitnessGoalDTO.getUserId() + ", 事件: GOAL_CREATED, 數據: null");
        achievementService.checkAndAwardAchievements(fitnessGoalDTO.getUserId(), "GOAL_CREATED", null);

        return mapToDTO(savedFitnessGoal);
    }

    @Override
    @Transactional
    public FitnessGoalDTO updateFitnessGoal(Integer goalId, FitnessGoalDTO fitnessGoalDTO) {
        FitnessGoal existingFitnessGoal = fitnessGoalRepo.findById(goalId)
                .orElseThrow(() -> new IllegalArgumentException("目標ID不存在"));

        // 儲存原始目標類型
        String originalGoalType = existingFitnessGoal.getGoalType();

        existingFitnessGoal.setGoalType(fitnessGoalDTO.getGoalType());
        existingFitnessGoal.setTargetValue(fitnessGoalDTO.getTargetValue());
        existingFitnessGoal.setCurrentProgress(fitnessGoalDTO.getCurrentProgress()); // 先設定前端傳來的進度
        existingFitnessGoal.setUnit(fitnessGoalDTO.getUnit());
        existingFitnessGoal.setStartDate(fitnessGoalDTO.getStartDate());
        existingFitnessGoal.setEndDate(fitnessGoalDTO.getEndDate());
        existingFitnessGoal.setStatus(fitnessGoalDTO.getStatus());
        existingFitnessGoal.setStartWeight(fitnessGoalDTO.getStartWeight());
        existingFitnessGoal.setStartBodyFat(fitnessGoalDTO.getStartBodyFat());
        existingFitnessGoal.setStartMuscleMass(fitnessGoalDTO.getStartMuscleMass());

        Integer userId = existingFitnessGoal.getUser().getUserId();

        // 如果目標類型發生改變，重新抓取並設定起始值
        if (!originalGoalType.equalsIgnoreCase(fitnessGoalDTO.getGoalType())) {
            setStartValues(existingFitnessGoal, userId);
        }

        FitnessGoal updatedFitnessGoal = fitnessGoalRepo.save(existingFitnessGoal);

        // 檢查目標是否已完成，並頒發相關獎章
        if ("COMPLETED".equalsIgnoreCase(fitnessGoalDTO.getStatus())) {
            long completedGoalsCount = fitnessGoalRepo.countByUserIdAndStatus(userId, "COMPLETED");
            System.out.println("FitnessGoalServiceImpl - updateFitnessGoal - 目標狀態更新為 COMPLETED - 觸發獎章檢查 - 使用者 ID: " + userId + ", 事件: GOAL_COMPLETED, 數據: " + completedGoalsCount);
            achievementService.checkAndAwardAchievements(userId, "GOAL_COMPLETED", (int) completedGoalsCount);
        }
        return mapToDTO(updatedFitnessGoal); // 使用 mapToDTO 實時計算進度
    }

    @Override
    public void deleteFitnessGoal(Integer goalId) {
        fitnessGoalRepo.deleteById(goalId);
    }

    @Override
    public FitnessGoalDTO getFitnessGoalById(Integer goalId) {
        FitnessGoal fitnessGoal = fitnessGoalRepo.findById(goalId)
                .orElseThrow(() -> new IllegalArgumentException("目標ID不存在"));
        return mapToDTO(fitnessGoal);
    }

    @Override
    public Page<FitnessGoalDTO> getAllFitnessGoalsByUserId(Integer userId, Pageable pageable) {
        Page<FitnessGoal> fitnessGoalsPage = fitnessGoalRepo.findByUserId(userId, pageable);
        return fitnessGoalsPage.map(this::mapToDTO);
    }

    @Override
    public Page<FitnessGoalDTO> getAllFitnessGoalsByUserName(String name, Pageable pageable) {
        Page<FitnessGoal> fitnessGoalsPage = fitnessGoalRepo.findByUserUserNameContaining(name, pageable);
        return fitnessGoalsPage.map(this::mapToDTO);
    }

    @Override
    public Page<FitnessGoalDTO> getAllFitnessGoalsByDateRange(String startDateStr, String endDateStr,
                                                              Pageable pageable) {
        LocalDate startDate = parseDate(startDateStr);
        LocalDate endDate = parseDate(endDateStr);
        Page<FitnessGoal> fitnessGoalsPage = fitnessGoalRepo
                .findByStartDateGreaterThanEqualAndEndDateLessThanEqual(startDate, endDate, pageable);
        return fitnessGoalsPage.map(this::mapToDTO);
    }

    @Override
    public Page<FitnessGoalDTO> getAllFitnessGoalsByUserIdAndDateRange(Integer userId, String startDateStr,
                                                                       String endDateStr, Pageable pageable) {
        LocalDate startDate = parseDate(startDateStr);
        LocalDate endDate = parseDate(endDateStr);
        Page<FitnessGoal> fitnessGoalsPage = fitnessGoalRepo
                .findByUserIdAndStartDateGreaterThanEqualAndEndDateLessThanEqual(userId, startDate, endDate,
                        pageable);
        return fitnessGoalsPage.map(this::mapToDTO);
    }

    @Override
    public Page<FitnessGoalDTO> getAllFitnessGoalsByCriteria(Integer userId, String name, String startDateStr,
                                                             String endDateStr, String goalType, String status, Pageable pageable) {
        LocalDate startDate = parseDate(startDateStr);
        LocalDate endDate = parseDate(endDateStr);
        Integer currentUserId = userId;
        return fitnessGoalRepo.findByOptionalCriteria(userId, StringUtils.hasText(name) ? "%" + name + "%" : null,
                startDate, endDate, goalType, status, pageable).map(this::mapToDTO);
    }
    
    @Override
    public List<FitnessGoalDTO> getAllGoalsWithProgress(int userId) {
        List<FitnessGoal> goals = fitnessGoalRepo.findByUserId(userId);
        return goals.stream()
                    .map(this::mapToDTO)
                    .collect(Collectors.toList());
    }
    


    @Override
    @Transactional
    public FitnessGoalDTO updateGoalProgress(Integer goalId, Double progressValue) {
        FitnessGoal existingFitnessGoal = fitnessGoalRepo.findById(goalId)
                .orElseThrow(() -> new IllegalArgumentException("目標ID不存在"));

        existingFitnessGoal.setCurrentProgress(Math.min(200.0, Math.max(0.0, progressValue))); // 限制前端傳來的百分比

        if (existingFitnessGoal.getTargetValue() != null && existingFitnessGoal.getCurrentProgress() >= 100 && !"COMPLETED".equalsIgnoreCase(existingFitnessGoal.getStatus())) {
            existingFitnessGoal.setStatus("COMPLETED");
            Integer userId = existingFitnessGoal.getUser().getUserId();
            long completedGoalsCount = fitnessGoalRepo.countByUserIdAndStatus(userId, "COMPLETED");
            System.out.println("FitnessGoalServiceImpl - updateGoalProgress - 目標 ID: " + goalId + " 已完成 - 觸發獎章檢查 - 使用者 ID: " + userId + ", 事件: GOAL_COMPLETED, 數據: " + completedGoalsCount);
            achievementService.checkAndAwardAchievements(userId, "GOAL_COMPLETED", (int) completedGoalsCount);
        } else if (existingFitnessGoal.getTargetValue() != null && existingFitnessGoal.getCurrentProgress() < 100 && "COMPLETED".equalsIgnoreCase(existingFitnessGoal.getStatus())) {
            existingFitnessGoal.setStatus("IN_PROGRESS"); // 如果進度倒退，改回進行中
            System.out.println("FitnessGoalServiceImpl - updateGoalProgress - 目標 ID: " + goalId + " 進度倒退，狀態改回 IN_PROGRESS");
        } else if (existingFitnessGoal.getTargetValue() == null && !"COMPLETED".equalsIgnoreCase(existingFitnessGoal.getStatus())) {
            existingFitnessGoal.setStatus("COMPLETED"); // 如果沒有目標值，任何進度更新都視為完成
            Integer userId = existingFitnessGoal.getUser().getUserId();
            long completedGoalsCount = fitnessGoalRepo.countByUserIdAndStatus(userId, "COMPLETED");
            System.out.println("FitnessGoalServiceImpl - updateGoalProgress - 目標 ID: " + goalId + " 無目標值，視為完成 - 觸發獎章檢查 - 使用者 ID: " + userId + ", 事件: GOAL_COMPLETED, 數據: " + completedGoalsCount);
            achievementService.checkAndAwardAchievements(userId, "GOAL_COMPLETED", (int) completedGoalsCount);
        }
        FitnessGoal updatedFitnessGoal = fitnessGoalRepo.save(existingFitnessGoal);
        return mapToDTO(updatedFitnessGoal);
    }

    private void setStartValues(FitnessGoal fitnessGoal, Integer userId) {
        bodyMetricService.findLatestByUserId(userId).ifPresent(bodyMetricDTO -> {
            if ("減重".equalsIgnoreCase(fitnessGoal.getGoalType())) {
                if (bodyMetricDTO.getWeight() != null) {
                    fitnessGoal.setStartWeight(bodyMetricDTO.getWeight().floatValue()); // 轉換為 Float
                }
            } else if ("減脂".equalsIgnoreCase(fitnessGoal.getGoalType())) {
                if (bodyMetricDTO.getBodyFat() != null) {
                    fitnessGoal.setStartBodyFat(bodyMetricDTO.getBodyFat().floatValue()); // 轉換為 Float
                }
            } else if ("增肌".equalsIgnoreCase(fitnessGoal.getGoalType())) {
                if (bodyMetricDTO.getMuscleMass() != null) {
                    fitnessGoal.setStartMuscleMass(bodyMetricDTO.getMuscleMass().floatValue()); // 轉換為 Float
                }
            }
        });
    }

    private FitnessGoalDTO mapToDTO(FitnessGoal fitnessGoal) {
        FitnessGoalDTO dto = new FitnessGoalDTO();
        dto.setGoalId(fitnessGoal.getGoalId());
        dto.setUserId(fitnessGoal.getUser().getUserId());
        dto.setGoalType(fitnessGoal.getGoalType());
        dto.setTargetValue(fitnessGoal.getTargetValue());
        dto.setUnit(fitnessGoal.getUnit());
        dto.setStatus(fitnessGoal.getStatus());
        dto.setStartDate(fitnessGoal.getStartDate());
        dto.setEndDate(fitnessGoal.getEndDate());
        dto.setStartWeight(fitnessGoal.getStartWeight());
        dto.setStartBodyFat(fitnessGoal.getStartBodyFat());
        dto.setStartMuscleMass(fitnessGoal.getStartMuscleMass());

        // 實時計算進度
        Double currentProgress = calculateCurrentProgress(fitnessGoal);
        dto.setCurrentProgress(currentProgress);

        return dto;
    }

    private Double calculateCurrentProgress(FitnessGoal goal) {
        Double progress = 0.0;
        Integer userId = goal.getUser().getUserId();
        Optional<BodyMetricDTO> latestMetric = bodyMetricService.findLatestByUserId(userId);

        if (latestMetric.isPresent()) {
            BodyMetricDTO currentBodyData = latestMetric.get();
            if ("減重".equalsIgnoreCase(goal.getGoalType())) {
                if (goal.getTargetValue() != null && currentBodyData.getWeight() != null && goal.getStartWeight() != null) {
                    double startWeight = goal.getStartWeight();
                    double targetValue = goal.getTargetValue();
                    double currentWeight = currentBodyData.getWeight();
                    double weightDiff = startWeight - currentWeight;
                    double targetDiff = targetValue;
                    if (targetDiff != 0) {
                        progress = Math.min(100.0, Math.max(0.0, (weightDiff / targetDiff) * 100));
                    } else {
                        progress = weightDiff > 0 ? 100.0 : 0.0;
                    }
                }
            } else if ("增肌".equalsIgnoreCase(goal.getGoalType())) {
                if (goal.getTargetValue() != null && currentBodyData.getMuscleMass() != null && goal.getStartMuscleMass() != null) {
                    double muscleGain = currentBodyData.getMuscleMass() - goal.getStartMuscleMass();
                    double targetGain = goal.getTargetValue();
                    if (targetGain != 0) {
                        progress = Math.min(100.0, Math.max(0.0, (muscleGain / targetGain) * 100));
                    } else {
                        progress = muscleGain > 0 ? 100.0 : 0.0;
                    }
                }
            } else if ("減脂".equalsIgnoreCase(goal.getGoalType())) {
                if (goal.getTargetValue() != null && currentBodyData.getBodyFat() != null && goal.getStartBodyFat() != null) {
                    double fatLoss = goal.getStartBodyFat() - currentBodyData.getBodyFat();
                    double targetLoss = goal.getTargetValue();
                    if (targetLoss != 0) {
                        progress = Math.min(100.0, Math.max(0.0, (fatLoss / targetLoss) * 100));
                    } else {
                        progress = fatLoss > 0 ? 100.0 : 0.0;
                    }
                }
            }
        }
        return progress;
    }
    @Scheduled(cron = "0 0 * * * ?") // 每小時執行一次
    public void updateGoalsProgress() {
        // 更新目標的進度邏輯，可以是根據某些條件來更新目標
        List<FitnessGoal> goals = fitnessGoalRepo.findAll();
        for (FitnessGoal goal : goals) {
            Double progress = calculateCurrentProgress(goal);
            goal.setCurrentProgress(progress);
            fitnessGoalRepo.save(goal);
        }
    }


    private FitnessGoal mapToEntity(FitnessGoalDTO dto, User user) {
        FitnessGoal entity = new FitnessGoal();
        entity.setGoalId(dto.getGoalId());
        entity.setUser(user);
        entity.setGoalType(dto.getGoalType());
        entity.setTargetValue(dto.getTargetValue());
        entity.setUnit(dto.getUnit());
        entity.setStatus(dto.getStatus());
        entity.setStartDate(dto.getStartDate());
        entity.setEndDate(dto.getEndDate());
        entity.setStartWeight(dto.getStartWeight());
        entity.setStartBodyFat(dto.getStartBodyFat());
        entity.setStartMuscleMass(dto.getStartMuscleMass());
        return entity;
    }

    private LocalDate parseDate(String dateStr) {
        if (dateStr == null) return null;
        try {
            return LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
