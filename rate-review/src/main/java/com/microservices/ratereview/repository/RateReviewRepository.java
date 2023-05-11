package com.microservices.ratereview.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.microservices.ratereview.dto.HistoryRateReviewDTO;
import com.microservices.ratereview.entity.HistoryRateReviewEntity;

public interface RateReviewRepository extends JpaRepository<HistoryRateReviewEntity, Integer> {

    // Avg rate for vehicle
    @Query(value = "SELECT AVG(NUM_RATE) FROM TB_HISTORY_RATE_REVIEW WHERE ID_VEHICLE = ?1 AND FLAG_REVIEW = 2",
            nativeQuery = true)
    public int avgRateNumVehicle(int idVehicle);

    public List<HistoryRateReviewEntity> findByIdVehicle(int idVehicle);
}
