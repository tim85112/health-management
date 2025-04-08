package com.healthmanagement.service.fitness;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.healthmanagement.dao.fitness.BodyMetricDAO;
import com.healthmanagement.dto.fitness.BodyMetricDTO;
import com.healthmanagement.model.fitness.BodyMetric;

import java.util.List;
import java.util.Optional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BodyMetricServiceImpl implements BodyMetricService {

    @Autowired
    private BodyMetricDAO bodyMetricRepo;

    @Override
    public BodyMetricDTO saveBodyMetrics(BodyMetricDTO bodyMetricDTO) {
        BodyMetric bodyMetric = new BodyMetric();
        bodyMetric.setUserId(bodyMetricDTO.getUserId());
        bodyMetric.setWeight(bodyMetricDTO.getWeight());
        bodyMetric.setBodyFat(bodyMetricDTO.getBodyFat());
        bodyMetric.setHeight(bodyMetricDTO.getHeight());
        bodyMetric.setWaistCircumference(bodyMetricDTO.getWaistCircumference());
        bodyMetric.setHipCircumference(bodyMetricDTO.getHipCircumference());

        // 計算 BMI
        double bmi = calculateBMI(bodyMetricDTO.getWeight(), bodyMetricDTO.getHeight());
        bodyMetric.setBmi(bmi);

        // 保存到資料庫
        bodyMetricRepo.save(bodyMetric);

        // 更新並返回 DTO
        bodyMetricDTO.setBmi(bmi);  // 把計算出來的 BMI 放到 DTO 中
        return bodyMetricDTO;
    }

    @Override
    public BodyMetricDTO calculateBMI(BodyMetricDTO bodyMetricDTO) {
        // 使用 BMI 計算邏輯
        double bmi = calculateBMI(bodyMetricDTO.getWeight(), bodyMetricDTO.getHeight());
        bodyMetricDTO.setBmi(bmi);
        return bodyMetricDTO;
    }

    @Override
    public void deleteBodyMetric(Integer bodyMetricId) {
        // 查找並刪除指定的 BodyMetric
        bodyMetricRepo.deleteById(bodyMetricId);
    }

    @Override
    public List<BodyMetricDTO> getAllBodyMetrics() {
        // 查詢所有身體數據
        List<BodyMetric> bodyMetrics = bodyMetricRepo.findAll();
        
        // 把 Entity 轉換成 DTO
        return bodyMetrics.stream().map(this::convertToDTO).toList();
    }

    @Override
    public List<BodyMetricDTO> findByUserId(Integer userId) {
        // 根據 userId 查詢所有身體數據
        List<BodyMetric> bodyMetrics = bodyMetricRepo.findByUserId(userId);
        
        // 轉換為 DTO 並返回
        return bodyMetrics.stream().map(this::convertToDTO).toList();
    }
    

    @Override
    public BodyMetricDTO updateBodyMetric(Integer bodyMetricId, BodyMetricDTO bodyMetricDTO) {
        // 查找 BodyMetric 並更新
        Optional<BodyMetric> existingBodyMetricOpt = bodyMetricRepo.findById(bodyMetricId);

        if (existingBodyMetricOpt.isPresent()) {
            BodyMetric existingBodyMetric = existingBodyMetricOpt.get();
            existingBodyMetric.setWeight(bodyMetricDTO.getWeight());
            existingBodyMetric.setBodyFat(bodyMetricDTO.getBodyFat());
            existingBodyMetric.setHeight(bodyMetricDTO.getHeight());
            existingBodyMetric.setWaistCircumference(bodyMetricDTO.getWaistCircumference());
            existingBodyMetric.setHipCircumference(bodyMetricDTO.getHipCircumference());

            // 計算並更新 BMI
            double bmi = calculateBMI(bodyMetricDTO.getWeight(), bodyMetricDTO.getHeight());
            existingBodyMetric.setBmi(bmi);

            // 保存更新到資料庫
            bodyMetricRepo.save(existingBodyMetric);

            // 返回更新後的 DTO
            bodyMetricDTO.setBmi(bmi);
            return bodyMetricDTO;
        } else {
            // 找不到 BodyMetric，返回 null 或拋出異常
            return null;
        }
    }

    // 獨立的 BMI 計算方法
    private double calculateBMI(double weight, double height) {
        return weight / (height / 100) / (height / 100);  // BMI = 體重(kg) / 身高(m)^2
    }

    private BodyMetricDTO convertToDTO(BodyMetric bodyMetric) {
        return BodyMetricDTO.fromEntity(bodyMetric);
    }

}
