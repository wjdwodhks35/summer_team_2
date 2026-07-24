package com.example.todaydeparture.dto;

public class DepartureResultDetailDto {

    private Long tripRequestId;
    private Long tripResultId;

    private String startName;
    private String startAddress;
    private String destinationName;
    private String destinationAddress;

    private String purposeCode;
    private String targetDatetime;

    private String recommendedDepartureTime;
    private String expectedArrivalTime;

    private Integer baseTravelMinutes;
    private Integer purposeBufferMinutes;
    private Integer personalBufferMinutes;
    private Integer totalBufferMinutes;

    private String riskLevel;
    private String recommendationSummary;

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

    public String getStartName() {
        return startName;
    }

    public void setStartName(String startName) {
        this.startName = startName;
    }

    public String getStartAddress() {
        return startAddress;
    }

    public void setStartAddress(String startAddress) {
        this.startAddress = startAddress;
    }

    public String getDestinationName() {
        return destinationName;
    }

    public void setDestinationName(String destinationName) {
        this.destinationName = destinationName;
    }

    public String getDestinationAddress() {
        return destinationAddress;
    }

    public void setDestinationAddress(String destinationAddress) {
        this.destinationAddress = destinationAddress;
    }

    public String getPurposeCode() {
        return purposeCode;
    }

    public void setPurposeCode(String purposeCode) {
        this.purposeCode = purposeCode;
    }

    public String getTargetDatetime() {
        return targetDatetime;
    }

    public void setTargetDatetime(String targetDatetime) {
        this.targetDatetime = targetDatetime;
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

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }

    public String getRecommendationSummary() {
        return recommendationSummary;
    }

    public void setRecommendationSummary(String recommendationSummary) {
        this.recommendationSummary = recommendationSummary;
    }
}