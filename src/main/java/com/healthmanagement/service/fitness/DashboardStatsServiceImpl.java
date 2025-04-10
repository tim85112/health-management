package com.healthmanagement.service.fitness;

import com.healthmanagement.dto.fitness.DashboardStatsDTO;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardStatsServiceImpl implements DashboardStatsService {

    private final EntityManager entityManager;

    @Override
    public DashboardStatsDTO getDashboardStats() {
        Query query = entityManager.createNativeQuery("SELECT * FROM dashboard_stat");
        List<Object[]> results = query.getResultList();

        if (!results.isEmpty()) {
            Object[] row = results.get(0);
            DashboardStatsDTO stats = new DashboardStatsDTO();
            stats.setTotalUsers(((Number) row[0]).intValue());
            stats.setTotalWorkouts(((Number) row[1]).intValue());
            stats.setTotalWorkoutMinutes(((Number) row[2]).intValue());
            stats.setTotalCaloriesBurned(((Number) row[3]).doubleValue()); 
            stats.setActiveUsersThisWeek(((Number) row[4]).intValue());
            return stats;
        }
        return null; // 或者拋出異常
    }
}