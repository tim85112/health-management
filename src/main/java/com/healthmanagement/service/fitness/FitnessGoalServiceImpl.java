package com.healthmanagement.service.fitness;

import com.healthmanagement.dao.fitness.FitnessGoalDAO;
import com.healthmanagement.dao.member.UserDAO;
import com.healthmanagement.dto.fitness.FitnessGoalDTO;
import com.healthmanagement.model.fitness.FitnessGoal;
import com.healthmanagement.model.member.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class FitnessGoalServiceImpl implements FitnessGoalService {

	private final FitnessGoalDAO fitnessGoalRepo;
	private final UserDAO userRepo;

	@Override
	public FitnessGoalDTO createFitnessGoal(FitnessGoalDTO fitnessGoalDTO) {
		User user = userRepo.findById(fitnessGoalDTO.getUserId())
				.orElseThrow(() -> new RuntimeException("User not found"));

		FitnessGoal fitnessGoal = new FitnessGoal();
		fitnessGoal.setUser(user);
		fitnessGoal.setGoalType(fitnessGoalDTO.getGoalType());
		fitnessGoal.setTargetValue(fitnessGoalDTO.getTargetValue());
		fitnessGoal.setCurrentProgress(fitnessGoalDTO.getCurrentProgress());
		fitnessGoal.setUnit(fitnessGoalDTO.getUnit());
		fitnessGoal.setStartDate(fitnessGoalDTO.getStartDate());
		fitnessGoal.setEndDate(fitnessGoalDTO.getEndDate());
		fitnessGoal.setStatus(fitnessGoalDTO.getStatus());

		FitnessGoal savedFitnessGoal = fitnessGoalRepo.save(fitnessGoal);
		return new FitnessGoalDTO(savedFitnessGoal);
	}

	@Override
	public FitnessGoalDTO updateFitnessGoal(Integer goalId, FitnessGoalDTO fitnessGoalDTO) {
		FitnessGoal fitnessGoal = fitnessGoalRepo.findById(goalId)
				.orElseThrow(() -> new IllegalArgumentException("目標ID不存在"));

		fitnessGoal.setGoalType(fitnessGoalDTO.getGoalType());
		fitnessGoal.setTargetValue(fitnessGoalDTO.getTargetValue());
		fitnessGoal.setCurrentProgress(fitnessGoalDTO.getCurrentProgress());
		fitnessGoal.setUnit(fitnessGoalDTO.getUnit());
		fitnessGoal.setStartDate(fitnessGoalDTO.getStartDate());
		fitnessGoal.setEndDate(fitnessGoalDTO.getEndDate());
		fitnessGoal.setStatus(fitnessGoalDTO.getStatus());

		fitnessGoal = fitnessGoalRepo.save(fitnessGoal);

		return new FitnessGoalDTO(fitnessGoal);
	}

	@Override
	public void deleteFitnessGoal(Integer goalId) {
		fitnessGoalRepo.deleteById(goalId);
	}

	@Override
	public FitnessGoalDTO getFitnessGoalById(Integer goalId) {
		FitnessGoal fitnessGoal = fitnessGoalRepo.findById(goalId)
				.orElseThrow(() -> new IllegalArgumentException("目標ID不存在"));
		return new FitnessGoalDTO(fitnessGoal);
	}

	@Override
	public List<FitnessGoalDTO> getAllFitnessGoalsByUserId(Integer userId) {
		List<FitnessGoal> fitnessGoals = fitnessGoalRepo.findByUserId(userId);
		return fitnessGoals.stream().map(FitnessGoalDTO::new).collect(Collectors.toList());
	}

	@Override
	public List<FitnessGoalDTO> getFitnessGoalByUserId(Integer userId) {
		return getAllFitnessGoalsByUserId(userId);
	}

	@Override
	public List<FitnessGoalDTO> getAllFitnessGoalsByUserName(String name) {
		List<FitnessGoal> fitnessGoals = fitnessGoalRepo.findByUserUserName(name);
		return fitnessGoals.stream().map(FitnessGoalDTO::new).collect(Collectors.toList());
	}

	@Override
	public List<FitnessGoalDTO> getAllFitnessGoalsByDateRange(String startDateStr, String endDateStr) {
		LocalDate startDate = null;
		LocalDate endDate = null;
		DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE;

		try {
			if (startDateStr != null && !startDateStr.isEmpty()) {
				startDate = LocalDate.parse(startDateStr, formatter);
			}
			if (endDateStr != null && !endDateStr.isEmpty()) {
				endDate = LocalDate.parse(endDateStr, formatter);
			}
		} catch (DateTimeParseException e) {
			throw new IllegalArgumentException("日期格式不正確，應為YYYY-MM-DD", e);
		}

		return fitnessGoalRepo.findByStartDateGreaterThanEqualAndEndDateLessThanEqual(startDate, endDate).stream()
				.map(FitnessGoalDTO::new).collect(Collectors.toList());
	}

	@Override
	public List<FitnessGoalDTO> getAllFitnessGoalsByUserIdAndDateRange(Integer userId, String startDateStr,
			String endDateStr) {
		LocalDate startDate = null;
		LocalDate endDate = null;
		DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE;

		try {
			if (startDateStr != null && !startDateStr.isEmpty()) {
				startDate = LocalDate.parse(startDateStr, formatter);
			}
			if (endDateStr != null && !endDateStr.isEmpty()) {
				endDate = LocalDate.parse(endDateStr, formatter);
			}
		} catch (DateTimeParseException e) {
			throw new IllegalArgumentException("日期格式不正確，應為YYYY-MM-DD", e);
		}

		return fitnessGoalRepo
				.findByUserIdAndStartDateGreaterThanEqualAndEndDateLessThanEqual(userId, startDate, endDate)
				.stream().map(FitnessGoalDTO::new).collect(Collectors.toList());
	}
}