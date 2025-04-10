package com.healthmanagement.dao.fitness;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.healthmanagement.model.fitness.ExerciseTypeCoefficient;

public interface ExerciseTypeCoefficientDAO  extends JpaRepository<ExerciseTypeCoefficient, Integer>{
	Optional<ExerciseTypeCoefficient> findByExerciseName(String exerciseName);
	
}

