package com.example.todaydeparture.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import com.example.todaydeparture.service.PublicDataService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

@RestController
@RequestMapping("/api")
public class ApiController {
	private final PublicDataService publicDataService;
	private final RestTemplate restTemplate = new RestTemplate();
	private final ObjectMapper objectMapper = new ObjectMapper();
	@Value("${tmap.api.key}")
	private String tmapAppKey;

	// 🌟 application.properties에서 ODsay 키 주입
	@Value("${odsay.api.key:default_key}")
	private String odsayApiKey;

	public ApiController(PublicDataService publicDataService) {
		this.publicDataService = publicDataService;
	}

	@GetMapping("/weather")
	public Map<String, String> weather(@RequestParam(name = "lat") String lat, @RequestParam(name = "lon") String lon) {
		return publicDataService.getWeatherData(lat, lon);
	}

	@GetMapping("/congestion")
	public Map<String, Object> congestion(
			@RequestParam(name = "line", required = false, defaultValue = "") String line,
			@RequestParam(name = "time", required = false, defaultValue = "") String time,
			@RequestParam(name = "station", required = false, defaultValue = "강남역") String station) {
		return publicDataService.getCongestionData(line, station, time);
	}

	@GetMapping("/nearby-bus")
	public List<Map<String, Object>> getNearbyBus(@RequestParam(name = "lat") String lat,
			@RequestParam(name = "lng") String lng) {
		return publicDataService.getNearbyStops(lat, lng);
	}

	@GetMapping("/places")
	public Map<String, Object> searchPlace(@RequestParam("keyword") String keyword) {
		Map<String, Object> result = new HashMap<>();
		try {
			Map<String, Double> coords = getActualCoords(keyword);
			result.put("name", keyword);
			result.put("lat", coords.get("lat"));
			result.put("lng", coords.get("lng"));
			result.put("success", true);
		} catch (Exception e) {
			result.put("success", false);
			result.put("message", "장소를 찾을 수 없습니다.");
		}
		return result;
	}

	@GetMapping("/places/suggest")
	public List<Map<String, Object>> suggestPlaces(@RequestParam("keyword") String keyword) {
		List<Map<String, Object>> places = new ArrayList<>();
		if (keyword == null || keyword.trim().length() < 2) return places;

		try {
			java.net.URI poiUri = UriComponentsBuilder
					.fromUriString("https://apis.openapi.sk.com/tmap/pois")
					.queryParam("version", "1")
					.queryParam("searchKeyword", keyword.trim())
					.queryParam("appKey", tmapAppKey)
					.queryParam("count", "8")
					.build()
					.encode()
					.toUri();
			JsonNode pois = objectMapper.readTree(restTemplate.getForObject(poiUri, String.class))
					.path("searchPoiInfo").path("pois").path("poi");
			if (!pois.isArray()) return places;

			for (JsonNode poi : pois) {
				Map<String, Object> place = new HashMap<>();
				place.put("name", poi.path("name").asText(""));
				place.put("category", poi.path("bizName").asText(""));
				place.put("address", buildPoiAddress(poi));
				place.put("lat", poi.path("noorLat").asDouble());
				place.put("lng", poi.path("noorLon").asDouble());
				places.add(place);
			}
		} catch (Exception e) {
			System.err.println("장소 자동완성 실패: " + e.getMessage());
		}
		return places;
	}

	private String buildPoiAddress(JsonNode poi) {
		String road = String.join(" ",
				poi.path("upperAddrName").asText(""),
				poi.path("middleAddrName").asText(""),
				poi.path("lowerAddrName").asText(""),
				poi.path("roadName").asText(""),
				poi.path("firstBuildNo").asText("")).replaceAll("\\s+", " ").trim();
		if (!road.isBlank()) return road;
		return String.join(" ",
				poi.path("upperAddrName").asText(""),
				poi.path("middleAddrName").asText(""),
				poi.path("lowerAddrName").asText(""),
				poi.path("detailAddrName").asText("")).replaceAll("\\s+", " ").trim();
	}

	// =========================================================================
	// 🌟 ODsay 대중교통 경로 & 세부 궤적(Polyline) 연동 API
	// =========================================================================

	/**
	 * 1. ODsay 대중교통 경로 검색 프록시
	 * GET /api/odsay/route?startX=...&startY=...&endX=...&endY=...
	 */
	@GetMapping("/odsay/route")
	public String getOdsayRoute(
			@RequestParam("startX") String startX,
			@RequestParam("startY") String startY,
			@RequestParam("endX") String endX,
			@RequestParam("endY") String endY) {

		String url = String.format(
			"https://api.odsay.com/v1/api/searchPubTransPathT?SX=%s&SY=%s&EX=%s&EY=%s&apiKey=%s",
			startX, startY, endX, endY, odsayApiKey
		);

		try {
			return restTemplate.getForObject(url, String.class);
		} catch (Exception e) {
			return "{\"error\": \"ODsay 경로 API 호출 실패\"}";
		}
	}

	/**
	 * 2. ODsay 세부 궤적 좌표 로드 프록시
	 * GET /api/odsay/lane?mapObject=...
	 */
	@GetMapping("/odsay/lane")
	public String getOdsayLane(@RequestParam("mapObject") String mapObject) {
		String url = String.format(
			"https://api.odsay.com/v1/api/loadLane?mapObject=0:0@%s&apiKey=%s",
			mapObject, odsayApiKey
		);

		try {
			return restTemplate.getForObject(url, String.class);
		} catch (Exception e) {
			return "{\"error\": \"ODsay 궤적 API 호출 실패\"}";
		}
	}

	// =========================================================================

	@GetMapping("/search")
	public Map<String, Object> runSearch(
			@RequestParam(name = "origin", required = false, defaultValue = "") String origin,
			@RequestParam(name = "dest", required = false, defaultValue = "") String dest) {

		Map<String, Object> result = new HashMap<>();
		List<Map<String, Object>> routeSegments = new ArrayList<>();
		int totalTimeMinutes = 46;

		try {
			Map<String, Double> startCoords = getActualCoords(origin);
			Map<String, Double> endCoords = getActualCoords(dest);

			result.put("startCoords", startCoords);
			result.put("endCoords", endCoords);

			String transitUrl = "https://apis.openapi.sk.com/transit/routes?version=1&format=json";
			Map<String, Object> requestBody = new HashMap<>();
			requestBody.put("startX", String.valueOf(startCoords.get("lng")));
			requestBody.put("startY", String.valueOf(startCoords.get("lat")));
			requestBody.put("endX", String.valueOf(endCoords.get("lng")));
			requestBody.put("endY", String.valueOf(endCoords.get("lat")));
			requestBody.put("lang", 0);

			HttpHeaders headers = new HttpHeaders();
			headers.set("appKey", tmapAppKey);
			headers.setAccept(List.of(MediaType.APPLICATION_JSON));
			headers.setContentType(MediaType.APPLICATION_JSON);

			HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
			ResponseEntity<String> response = restTemplate.exchange(transitUrl, HttpMethod.POST, entity, String.class);

			if (response.getBody() != null && !response.getBody().trim().isEmpty()
					&& !response.getBody().contains("error")) {
				JsonNode root = objectMapper.readTree(response.getBody());
				JsonNode itineraries = root.path("metaData").path("plan").path("itineraries");

				if (itineraries.isArray() && itineraries.size() > 0) {
					JsonNode bestRoute = itineraries.get(0);
					totalTimeMinutes = bestRoute.path("totalTime").asInt(2760) / 60;

					result.put("transferCount", bestRoute.path("transferCount").asInt(0));
					result.put("totalWalkMinutes", (int) Math.ceil(bestRoute.path("totalWalkTime").asInt(0) / 60.0));
					result.put("fare", bestRoute.path("fare").path("regular").path("totalFare").asInt(0));

					JsonNode legs = bestRoute.path("legs");
					if (legs.isArray()) {
						for (JsonNode path : legs) {
							Map<String, Object> segment = new HashMap<>();
							String mode = path.path("mode").asText("WALK").toUpperCase();
							int sectionTime = (int) Math.ceil(path.path("sectionTime").asInt(0) / 60.0);
							segment.put("mode", mode);
							segment.put("time", Math.max(sectionTime, 1));
							segment.put("distance", path.path("distance").asInt(0));
							segment.put("routeLine", path.path("route").asText(modeLabel(mode)));
							segment.put("routeColor", normalizeColor(path.path("routeColor").asText("")));
							segment.put("routeType", path.path("type").asInt(0));
							segment.put("startName", path.path("start").path("name").asText("출발지"));
							segment.put("endName", path.path("end").path("name").asText("도착지"));
							segment.put("startLat", path.path("start").path("lat").asDouble());
							segment.put("startLng", path.path("start").path("lon").asDouble());
							segment.put("endLat", path.path("end").path("lat").asDouble());
							segment.put("endLng", path.path("end").path("lon").asDouble());
							segment.put("geometry", extractGeometry(path));
							routeSegments.add(segment);
						}
					}
				}
			} else {
				throw new RuntimeException("TMAP 대중교통 데이터 규격 부재");
			}

		} catch (Exception e) {
			System.err.println("⚠️ [동적 경로 매핑 가동 중단]: " + e.getMessage());

			result.put("error", "실시간 대중교통 경로를 불러오지 못했습니다.");
			result.put("errorDetail", e.getMessage());
		}

		LocalDateTime now = LocalDateTime.now();
		DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

		result.put("departTime", now.format(timeFormatter));
		result.put("arriveTime", now.plusMinutes(totalTimeMinutes).format(timeFormatter));
		result.put("totalTimeMinutes", totalTimeMinutes);
		result.put("segments", routeSegments);
		result.put("description", String.format("지금 나가면 약 %d분 뒤 도착 · 대중교통 최적 경로 · 목적지: %s", totalTimeMinutes, dest));

		return result;
	}

	private List<Map<String, Double>> extractGeometry(JsonNode leg) {
		List<Map<String, Double>> points = new ArrayList<>();
		if ("WALK".equalsIgnoreCase(leg.path("mode").asText())) {
			for (JsonNode step : leg.path("steps")) {
				appendLineString(points, step.path("linestring").asText(""));
			}
		} else {
			appendLineString(points, leg.path("passShape").path("linestring").asText(""));
		}
		if (points.isEmpty()) {
			addPoint(points, leg.path("start").path("lat").asDouble(), leg.path("start").path("lon").asDouble());
			addPoint(points, leg.path("end").path("lat").asDouble(), leg.path("end").path("lon").asDouble());
		}
		return points;
	}

	private void appendLineString(List<Map<String, Double>> points, String lineString) {
		for (String pair : lineString.trim().split("\\s+")) {
			String[] xy = pair.split(",");
			if (xy.length != 2) continue;
			try {
				addPoint(points, Double.parseDouble(xy[1]), Double.parseDouble(xy[0]));
			} catch (NumberFormatException ignored) {
			}
		}
	}

	private void addPoint(List<Map<String, Double>> points, double lat, double lng) {
		if (lat == 0 || lng == 0) return;
		Map<String, Double> point = new HashMap<>();
		point.put("lat", lat);
		point.put("lng", lng);
		points.add(point);
	}

	private String modeLabel(String mode) {
		if ("BUS".equals(mode)) return "버스";
		if ("SUBWAY".equals(mode)) return "지하철";
		if ("TRAIN".equals(mode)) return "기차";
		if ("EXPRESSBUS".equals(mode)) return "고속·시외버스";
		return "도보";
	}

	private String normalizeColor(String color) {
		if (color == null || color.isBlank()) return "#2D6A6A";
		return color.startsWith("#") ? color : "#" + color;
	}

	private Map<String, Double> getActualCoords(String keyword) throws Exception {
		Map<String, Double> coords = new HashMap<>();

		if (keyword == null || keyword.trim().isEmpty() || keyword.contains("공학대") || keyword.contains("한공대")
				|| keyword.contains("정왕")) {
			coords.put("lat", 37.3401);
			coords.put("lng", 126.7335);
			return coords;
		}
		if (keyword.contains("강남")) {
			coords.put("lat", 37.4979);
			coords.put("lng", 127.0276);
			return coords;
		}

		try {
			java.net.URI poiUri = UriComponentsBuilder
					.fromUriString("https://apis.openapi.sk.com/tmap/pois")
					.queryParam("version", "1")
					.queryParam("searchKeyword", keyword)
					.queryParam("appKey", tmapAppKey)
					.queryParam("count", "1")
					.build()
					.encode()
					.toUri();

			String response = restTemplate.getForObject(poiUri, String.class);

			if (response == null || response.trim().isEmpty()) {
				throw new IllegalArgumentException("TMAP POI 응답 바디가 비어있습니다.");
			}

			JsonNode root = objectMapper.readTree(response);
			JsonNode poiInfo = root.path("searchPoiInfo");

			if (poiInfo.isMissingNode() || poiInfo.isNull() || poiInfo.path("pois").isMissingNode()) {
				throw new IllegalArgumentException("검색 결과 일치 항목 없음");
			}

			JsonNode poi = poiInfo.path("pois").path("poi").get(0);
			if (poi == null || poi.isMissingNode()) {
				throw new IllegalArgumentException("상세 장소 데이터가 존재하지 않습니다.");
			}

			coords.put("lat", poi.path("noorLat").asDouble());
			coords.put("lng", poi.path("noorLon").asDouble());

		} catch (Exception e) {
			throw new IllegalArgumentException("장소 검색 실패: " + keyword, e);
		}

		return coords;
	}
}
