package com.healthmanagement.service.fitness;

import com.healthmanagement.dto.fitness.BodyMetricDTO;
import com.healthmanagement.dto.fitness.FitnessGoalDTO;
import com.healthmanagement.dto.fitness.RecommendationDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RecommendationService {

    private final BodyMetricService bodyMetricService;
    private final ExerciseService exerciseService;
    private final FitnessGoalService fitnessGoalService;

    public List<RecommendationDTO> getRecommendationsForUser(Integer userId) {
        List<RecommendationDTO> recommendations = new ArrayList<>();
        Optional<BodyMetricDTO> latestBodyMetric = bodyMetricService.findLatestByUserId(userId);
        List<FitnessGoalDTO> goals = fitnessGoalService.getAllFitnessGoalsByUserIdWithoutPagination(userId);

        latestBodyMetric.ifPresent(metric -> {
            double bmi = metric.getBmi();

            // 判斷增肌或減脂的建議方向 (基於 BMI)
            if (bmi < 18.5) {
                recommendations.add(new RecommendationDTO("體型建議", "你的BMI偏低，建議考慮以增肌為主，並注意均衡飲食。"));
                recommendations.addAll(getMuscleGainTrainingRecommendations());
                recommendations.addAll(getMuscleGainDietRecommendations());
            } else if (bmi >= 25) {
                recommendations.add(new RecommendationDTO("體型建議", "你的BMI偏高，建議考慮以減脂為主，並結合適當的運動。"));
                recommendations.addAll(getFatLossTrainingRecommendations());
                recommendations.addAll(getFatLossDietRecommendations());
            } else {
                recommendations.add(new RecommendationDTO("體型建議", "你的BMI在健康範圍內，可以根據你的個人目標選擇增肌、維持或適度減脂。"));
                if (goals.isEmpty()) {
                    recommendations.add(new RecommendationDTO("目標設定建議", "你還沒有設定任何健身目標，建議設定明確的目標，例如增肌、減脂或提高運動表現。"));
                }
            }

            LocalDate now = LocalDate.now();
            long workoutsThisWeek = exerciseService.getExerciseRecordsByUserId(userId).stream()
                    .filter(record -> record.getExerciseDate().isAfter(now.minusDays(now.getDayOfWeek().getValue() - 1))) // 使用 getExerciseDate()
                    .count();
            if (workoutsThisWeek < 3) {
                recommendations.add(new RecommendationDTO("訓練建議", "為了達到更好的健身效果，建議你本週至少進行三次訓練。"));
            }

            goals.forEach(goal -> {
                String goalDisplayName = goal.getGoalType(); // 或者 goal.getName() 如果你添加了 name 屬性
                if (goal.getEndDate() != null && goal.getEndDate().isBefore(now) && goal.getCurrentProgress() < goal.getTargetValue()) { // 使用 getTargetValue() 和 getCurrentProgress()
                    recommendations.add(new RecommendationDTO("目標設定建議", "你的目標 '" + goalDisplayName + "' 已過期，且尚未達成。建議重新評估或設定新的目標。"));
                }
            });
        });

        return recommendations;
    }

    // 增肌訓練建議
    private List<RecommendationDTO> getMuscleGainTrainingRecommendations() {
        List<RecommendationDTO> recommendations = new ArrayList<>();
        recommendations.add(new RecommendationDTO("訓練建議", "進行複合動作（如深蹲、臥推、硬舉、划船）以刺激多個肌群。"));
        recommendations.add(new RecommendationDTO("訓練建議", "每組重複次數約 6-12 次，並選擇能讓你感到挑戰的重量。"));
        recommendations.add(new RecommendationDTO("訓練建議", "注意訓練量和組間休息，確保肌肉有足夠的刺激和恢復時間。"));
        recommendations.add(new RecommendationDTO("訓練建議", "逐步增加訓練強度（例如增加重量、次數或組數）。"));
        return recommendations;
    }

    // 減脂訓練建議
    private List<RecommendationDTO> getFatLossTrainingRecommendations() {
        List<RecommendationDTO> recommendations = new ArrayList<>();
        recommendations.add(new RecommendationDTO("訓練建議", "結合有氧運動（如跑步、游泳、單車）和力量訓練。"));
        recommendations.add(new RecommendationDTO("訓練建議", "有氧運動可以幫助燃燒卡路里，力量訓練可以幫助維持肌肉量。"));
        recommendations.add(new RecommendationDTO("訓練建議", "可以嘗試高強度間歇訓練（HIIT）以提高燃脂效率。"));
        recommendations.add(new RecommendationDTO("訓練建議", "力量訓練可以採用較高的重複次數（12-15 次）和較短的組間休息。"));
        return recommendations;
    }

    // 增肌飲食建議
    private List<RecommendationDTO> getMuscleGainDietRecommendations() {
        List<RecommendationDTO> recommendations = new ArrayList<>();
        recommendations.add(new RecommendationDTO("飲食建議", "確保攝入足夠的蛋白質，約每公斤體重 1.6-2.2 克，以支持肌肉生長。"));
        recommendations.add(new RecommendationDTO("飲食建議", "攝入足夠的總卡路里，略高於你的消耗，以提供能量進行肌肉合成。"));
        recommendations.add(new RecommendationDTO("飲食建議", "選擇複合碳水化合物（如全穀物、蔬菜）作為能量來源。"));
        recommendations.add(new RecommendationDTO("飲食建議", "不要忽略健康脂肪的攝入，它們對激素水平和整體健康很重要。"));
        recommendations.add(new RecommendationDTO("飲食建議", "注意餐飲時間，特別是在訓練前後攝入蛋白質和碳水化合物。"));
        return recommendations;
    }

    // 減脂飲食建議
    private List<RecommendationDTO> getFatLossDietRecommendations() {
        List<RecommendationDTO> recommendations = new ArrayList<>();
        recommendations.add(new RecommendationDTO("飲食建議", "創造熱量缺口，攝入的卡路里要少於消耗的卡路里，以促使脂肪燃燒。"));
        recommendations.add(new RecommendationDTO("飲食建議", "保持足夠的蛋白質攝入，以幫助維持肌肉量。"));
        recommendations.add(new RecommendationDTO("飲食建議", "限制加工食品、高糖食物和不健康的脂肪。"));
        recommendations.add(new RecommendationDTO("飲食建議", "增加蔬菜和水果的攝入，它們富含纖維和營養。"));
        recommendations.add(new RecommendationDTO("飲食建議", "注意水分攝入，多喝水。"));
        return recommendations;
    }}