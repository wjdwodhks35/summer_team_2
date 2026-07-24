package com.example.todaydeparture.dto;

public class PurposeBufferPolicyDto {

    private String purposeCode;
    private String purposeName;
    private Integer defaultBufferMinutes;
    private Integer minBufferMinutes;
    private Integer maxBufferMinutes;
    private String description;
    private Boolean isActive;

    public String getPurposeCode() {
        return purposeCode;
    }

    public void setPurposeCode(String purposeCode) {
        this.purposeCode = purposeCode;
    }

    public String getPurposeName() {
        return purposeName;
    }

    public void setPurposeName(String purposeName) {
        this.purposeName = purposeName;
    }

    public Integer getDefaultBufferMinutes() {
        return defaultBufferMinutes;
    }

    public void setDefaultBufferMinutes(Integer defaultBufferMinutes) {
        this.defaultBufferMinutes = defaultBufferMinutes;
    }

    public Integer getMinBufferMinutes() {
        return minBufferMinutes;
    }

    public void setMinBufferMinutes(Integer minBufferMinutes) {
        this.minBufferMinutes = minBufferMinutes;
    }

    public Integer getMaxBufferMinutes() {
        return maxBufferMinutes;
    }

    public void setMaxBufferMinutes(Integer maxBufferMinutes) {
        this.maxBufferMinutes = maxBufferMinutes;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean active) {
        isActive = active;
    }
}