package com.microservices.ratereview.domain;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InforBooKingDTO {
    private String bookingId;
    private LocalDate pickupTime;
    private LocalDate dropoffTime;
    private String location;
    private String paymentMethod;
	private String userId;
	private String vehicleId;
	private String token;
}
