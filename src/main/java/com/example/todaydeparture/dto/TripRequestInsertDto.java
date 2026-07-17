package com.example.todaydeparture.dto;

import java.time.LocalDateTime;

public class TripRequestInsertDto {

    private Long id;
    private Long userId;

    private String startName;
    private String startAddress;
    private Double startLatitude;
    private Double startLongitude;

    private String destinationName;
    private String destinationAddress;
    private Double destinationLatitude;
    private Double destinationLongitude;

    private String targetType;
    private LocalDateTime targetDatetime;

    private String purposeCode;
    private String preferredTransport;

    private Integer baseTravelMinutes;
    private Integer personalBufferMinutes;

    private String requestClient;
    private String requestStatus;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
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

    public Double getStartLatitude() {
        return startLatitude;
    }

    public void setStartLatitude(Double startLatitude) {
        this.startLatitude = startLatitude;
    }

    public Double getStartLongitude() {
        return startLongitude;
    }

    public void setStartLongitude(Double startLongitude) {
        this.startLongitude = startLongitude;
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

    public Double getDestinationLatitude() {
        return destinationLatitude;
    }

    public void setDestinationLatitude(Double destinationLatitude) {
        this.destinationLatitude = destinationLatitude;
    }

    public Double getDestinationLongitude() {
        return destinationLongitude;
    }

    public void setDestinationLongitude(Double destinationLongitude) {
        this.destinationLongitude = destinationLongitude;
    }

    public String getTargetType() {
        return targetType;
    }

    public void setTargetType(String targetType) {
        this.targetType = targetType;
    }

    public LocalDateTime getTargetDatetime() {
        return targetDatetime;
    }

    public void setTargetDatetime(LocalDateTime targetDatetime) {
        this.targetDatetime = targetDatetime;
    }

    public String getPurposeCode() {
        return purposeCode;
    }

    public void setPurposeCode(String purposeCode) {
        this.purposeCode = purposeCode;
    }

    public String getPreferredTransport() {
        return preferredTransport;
    }

    public void setPreferredTransport(String preferredTransport) {
        this.preferredTransport = preferredTransport;
    }

    public Integer getBaseTravelMinutes() {
        return baseTravelMinutes;
    }

    public void setBaseTravelMinutes(Integer baseTravelMinutes) {
        this.baseTravelMinutes = baseTravelMinutes;
    }

    public Integer getPersonalBufferMinutes() {
        return personalBufferMinutes;
    }

    public void setPersonalBufferMinutes(Integer personalBufferMinutes) {
        this.personalBufferMinutes = personalBufferMinutes;
    }

    public String getRequestClient() {
        return requestClient;
    }

    public void setRequestClient(String requestClient) {
        this.requestClient = requestClient;
    }

    public String getRequestStatus() {
        return requestStatus;
    }

    public void setRequestStatus(String requestStatus) {
        this.requestStatus = requestStatus;
    }
}