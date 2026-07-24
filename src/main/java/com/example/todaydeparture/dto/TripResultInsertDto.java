package com.example.todaydeparture.dto;

import java.time.LocalDateTime;

public class TripResultInsertDto {

    private Long id;
    private Long tripRequestId;

    private LocalDateTime recommendedDepartureTime;
    private LocalDateTime expectedArrivalTime;

    private Integer baseTravelMinutes;
    private Integer purposeBufferMinutes;
    private Integer personalBufferMinutes;
    private Integer totalBufferMinutes;

    private Integer riskScore;
    private String riskLevel;

    private String recommendedTransport;
    private String recommendationSummary;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTripRequestId() {
        return tripRequestId;
    }

    public void setTripRequestId(Long tripRequestId) {
        this.tripRequestId = tripRequestId;
    }

    public LocalDateTime getRecommendedDepartureTime() {
        return recommendedDepartureTime;
    }

    public void setRecommendedDepartureTime(LocalDateTime recommendedDepartureTime) {
        this.recommendedDepartureTime = recommendedDepartureTime;
    }

    public LocalDateTime getExpectedArrivalTime() {
        return expectedArrivalTime;
    }

    public void setExpectedArrivalTime(LocalDateTime expectedArrivalTime) {
        this.expectedArrivalTime = expectedArrivalTime;
    }

    public Integer getBaseTravelMinutes() {
        return baseTravelMinutes;
    }

    public void setBaseTravelMinutes(Integer baseTravelMinutes) {
        this.baseTravelMinutes = baseTravelMinutes;
    }

    public Integer getPurposeBufferMinutes() {
        return purposeBufferMinutes;
    }

    public void setPurposeBufferMinutes(Integer purposeBufferMinutes) {
        this.purposeBufferMinutes = purposeBufferMinutes;
    }

    public Integer getPersonalBufferMinutes() {
        return personalBufferMinutes;
    }

    public void setPersonalBufferMinutes(Integer personalBufferMinutes) {
        this.personalBufferMinutes = personalBufferMinutes;
    }

    public Integer getTotalBufferMinutes() {
        return totalBufferMinutes;
    }

    public void setTotalBufferMinutes(Integer totalBufferMinutes) {
        this.totalBufferMinutes = totalBufferMinutes;
    }

    public Integer getRiskScore() {
        return riskScore;
    }

    public void setRiskScore(Integer riskScore) {
        this.riskScore = riskScore;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }

    public String getRecommendedTransport() {
        return recommendedTransport;
    }

    public void setRecommendedTransport(String recommendedTransport) {
        this.recommendedTransport = recommendedTransport;
    }

    public String getRecommendationSummary() {
        return recommendationSummary;
    }

    public void setRecommendationSummary(String recommendationSummary) {
        this.recommendationSummary = recommendationSummary;
    }
}