package com.example.todaydeparture.service;

import com.example.todaydeparture.dto.DepartureRecommendRequest;
import com.example.todaydeparture.dto.DepartureRecommendResponse;
import com.example.todaydeparture.dto.TripRequestInsertDto;
import com.example.todaydeparture.dto.TripResultInsertDto;
import com.example.todaydeparture.mapper.DepartureMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.todaydeparture.dto.DepartureResultDetailDto;

import java.time.LocalDateTime;

@Service
public class DepartureRecommendService {

    private final DepartureMapper departureMapper;

    public DepartureRecommendService(DepartureMapper departureMapper) {
        this.departureMapper = departureMapper;
    }

    public DepartureResultDetailDto getResult(Long tripResultId) {
        return departureMapper.findResultById(tripResultId);
    }
    
    @Transactional
    public DepartureRecommendResponse recommend(DepartureRecommendRequest request) {
        validateRequest(request);

        int baseTravelMinutes = request.getBaseTravelMinutes();
        int personalBufferMinutes = request.getPersonalBufferMinutes() == null
                ? 0
                : request.getPersonalBufferMinutes();

        Integer purposeBuffer = departureMapper.findPurposeBufferMinutes(request.getPurposeCode());
        int purposeBufferMinutes = purposeBuffer == null ? 5 : purposeBuffer;

        int totalBufferMinutes = purposeBufferMinutes + personalBufferMinutes;

        // 🌟 [안전 장치] 날짜/시간 문자열 포맷 보정 (" " -> "T" 변환 및 초 단위 보완)
        String rawDateTime = request.getTargetDateTime().trim().replace(" ", "T");
        if (rawDateTime.length() == 16) { // 예: "2026-07-24T09:00" 인 경우 ":00" 보완
            rawDateTime += ":00";
        }

        LocalDateTime targetArrivalTime = LocalDateTime.parse(rawDateTime);

        LocalDateTime recommendedDepartureTime =
                targetArrivalTime.minusMinutes(baseTravelMinutes + totalBufferMinutes);

        String riskLevel = calculateRiskLevel(totalBufferMinutes);
        int riskScore = calculateRiskScore(totalBufferMinutes);

        String summary = targetArrivalTime.toLocalTime()
                + "까지 도착하려면 "
                + recommendedDepartureTime.toLocalTime()
                + "쯤 출발하는 것을 추천합니다.";

        TripRequestInsertDto tripRequest = new TripRequestInsertDto();
        tripRequest.setUserId(request.getUserId());
        tripRequest.setStartName(request.getStartName());
        tripRequest.setStartAddress(request.getStartAddress());
        tripRequest.setStartLatitude(request.getStartLatitude());
        tripRequest.setStartLongitude(request.getStartLongitude());
        tripRequest.setDestinationName(request.getDestinationName());
        tripRequest.setDestinationAddress(request.getDestinationAddress());
        tripRequest.setDestinationLatitude(request.getDestinationLatitude());
        tripRequest.setDestinationLongitude(request.getDestinationLongitude());
        tripRequest.setTargetType("arrival");
        tripRequest.setTargetDatetime(targetArrivalTime);
        tripRequest.setPurposeCode(request.getPurposeCode());
        tripRequest.setPreferredTransport(request.getPreferredTransport());
        tripRequest.setBaseTravelMinutes(baseTravelMinutes);
        tripRequest.setPersonalBufferMinutes(personalBufferMinutes);
        tripRequest.setRequestClient(request.getRequestClient());
        tripRequest.setRequestStatus("calculated");

        departureMapper.insertTripRequest(tripRequest);

        TripResultInsertDto tripResult = new TripResultInsertDto();
        tripResult.setTripRequestId(tripRequest.getId());
        tripResult.setRecommendedDepartureTime(recommendedDepartureTime);
        tripResult.setExpectedArrivalTime(targetArrivalTime);
        tripResult.setBaseTravelMinutes(baseTravelMinutes);
        tripResult.setPurposeBufferMinutes(purposeBufferMinutes);
        tripResult.setPersonalBufferMinutes(personalBufferMinutes);
        tripResult.setTotalBufferMinutes(totalBufferMinutes);
        tripResult.setRecommendedTransport(request.getPreferredTransport());
        tripResult.setRiskScore(riskScore);
        tripResult.setRiskLevel(riskLevel);
        tripResult.setRecommendationSummary(summary);

        departureMapper.insertTripResult(tripResult);

        departureMapper.insertNotification(
                request.getUserId(),
                tripResult.getId(),
                "departure_time",
                "출발 시간 알림",
                summary,
                recommendedDepartureTime
        );

        DepartureRecommendResponse response = new DepartureRecommendResponse();
        response.setTripRequestId(tripRequest.getId());
        response.setTripResultId(tripResult.getId());
        response.setRecommendedDepartureTime(recommendedDepartureTime.toString());
        response.setExpectedArrivalTime(targetArrivalTime.toString());
        response.setBaseTravelMinutes(baseTravelMinutes);
        response.setPurposeBufferMinutes(purposeBufferMinutes);
        response.setPersonalBufferMinutes(personalBufferMinutes);
        response.setTotalBufferMinutes(totalBufferMinutes);
        response.setRiskLevel(riskLevel);
        response.setSummary(summary);

        return response;
    }

    private void validateRequest(DepartureRecommendRequest request) {
        if (request.getStartAddress() == null || request.getStartAddress().isBlank()) {
            throw new IllegalArgumentException("출발지를 입력해야 합니다.");
        }

        if (request.getDestinationAddress() == null || request.getDestinationAddress().isBlank()) {
            throw new IllegalArgumentException("도착지를 입력해야 합니다.");
        }

        if (request.getTargetDateTime() == null || request.getTargetDateTime().isBlank()) {
            throw new IllegalArgumentException("도착 희망 시간을 입력해야 합니다.");
        }

        if (request.getPurposeCode() == null || request.getPurposeCode().isBlank()) {
            throw new IllegalArgumentException("이동 목적을 선택해야 합니다.");
        }

        if (request.getBaseTravelMinutes() == null || request.getBaseTravelMinutes() <= 0) {
            throw new IllegalArgumentException("기본 이동 시간은 1분 이상이어야 합니다.");
        }

        if (request.getPreferredTransport() == null || request.getPreferredTransport().isBlank()) {
            request.setPreferredTransport("public_transport");
        }

        if (request.getRequestClient() == null || request.getRequestClient().isBlank()) {
            request.setRequestClient("web");
        }
    }

    private String calculateRiskLevel(int totalBufferMinutes) {
        if (totalBufferMinutes >= 30) {
            return "low";
        }

        if (totalBufferMinutes >= 15) {
            return "normal";
        }

        return "high";
    }

    private int calculateRiskScore(int totalBufferMinutes) {
        if (totalBufferMinutes >= 30) {
            return 20;
        }

        if (totalBufferMinutes >= 15) {
            return 50;
        }

        return 80;
    }
}