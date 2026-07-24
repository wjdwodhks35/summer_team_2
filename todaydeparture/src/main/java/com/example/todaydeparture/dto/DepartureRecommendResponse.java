package com.example.todaydeparture.dto;

public class DepartureRecommendResponse {

    private Long tripRequestId;
    private Long tripResultId;

    private String recommendedDepartureTime;
    private String expectedArrivalTime;

    private int baseTravelMinutes;
    private int purposeBufferMinutes;
    private int personalBufferMinutes;
    private int totalBufferMinutes;

    private String riskLevel;
    private String summary;

    public Long getTripRequestId() {
        return tripRequestId;
    }

    public void setTripRequestId(Long tripRequestId) {
        this.tripRequestId = tripRequestId;
    }

    public Long getTripResultId() {
        return tripResultId;
    }

    public void setTripResultId(Long tripResultId) {
        this.tripResultId = tripResultId;
    }

    public String getRecommendedDepartureTime() {
        return recommendedDepartureTime;
    }

    public void setRecommendedDepartureTime(String recommendedDepartureTime) {
        this.recommendedDepartureTime = recommendedDepartureTime;
    }

    public String getExpectedArrivalTime() {
        return expectedArrivalTime;
    }

    public void setExpectedArrivalTime(String expectedArrivalTime) {
        this.expectedArrivalTime = expectedArrivalTime;
    }

    public int getBaseTravelMinutes() {
        return baseTravelMinutes;
    }

    public void setBaseTravelMinutes(int baseTravelMinutes) {
        this.baseTravelMinutes = baseTravelMinutes;
    }

    public int getPurposeBufferMinutes() {
        return purposeBufferMinutes;
    }

    public void setPurposeBufferMinutes(int purposeBufferMinutes) {
        this.purposeBufferMinutes = purposeBufferMinutes;
    }

    public int getPersonalBufferMinutes() {
        return personalBufferMinutes;
    }

    public void setPersonalBufferMinutes(int personalBufferMinutes) {
        this.personalBufferMinutes = personalBufferMinutes;
    }

    public int getTotalBufferMinutes() {
        return totalBufferMinutes;
    }

    public void setTotalBufferMinutes(int totalBufferMinutes) {
        this.totalBufferMinutes = totalBufferMinutes;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }
}