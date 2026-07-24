-- =========================================================
-- 오늘어때 / 몇시에나가 전체 DB 스키마
-- Spring Boot 실행 시 자동 생성용
-- =========================================================

ALTER DATABASE today_departure CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 1. 사용자
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) UNIQUE,
    password_hash VARCHAR(255),
    nickname VARCHAR(50) NOT NULL,
    default_start_location VARCHAR(255),
    preferred_transport VARCHAR(30) DEFAULT 'public_transport',
    is_active TINYINT(1) DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='사용자 정보';

ALTER TABLE users ADD COLUMN IF NOT EXISTS access_token VARCHAR(255);


-- 2. 이동 목적별 여유 시간 정책
CREATE TABLE IF NOT EXISTS purpose_buffer_policies (
    purpose_code VARCHAR(50) PRIMARY KEY,
    purpose_name VARCHAR(100) NOT NULL,
    default_buffer_minutes INT NOT NULL,
    min_buffer_minutes INT DEFAULT 0,
    max_buffer_minutes INT DEFAULT 120,
    description VARCHAR(255),
    is_active TINYINT(1) DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='이동 목적별 기본 여유 시간 정책';


-- 3. 사용자 저장 장소
CREATE TABLE IF NOT EXISTS user_places (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    place_name VARCHAR(100) NOT NULL,
    address VARCHAR(255) NOT NULL,
    road_address VARCHAR(255),
    latitude DECIMAL(10, 7) NOT NULL,
    longitude DECIMAL(10, 7) NOT NULL,
    place_type VARCHAR(30) DEFAULT 'etc',
    is_default TINYINT(1) DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_user_places_user
        FOREIGN KEY (user_id) REFERENCES users(id)
        ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='집, 학교, 회사 등 자주 쓰는 장소';


-- 4. 개인 출발/도착 추천 요청
CREATE TABLE IF NOT EXISTS trip_requests (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NULL,

    start_name VARCHAR(100),
    start_address VARCHAR(255) NOT NULL,
    start_latitude DECIMAL(10, 7),
    start_longitude DECIMAL(10, 7),

    destination_name VARCHAR(100),
    destination_address VARCHAR(255) NOT NULL,
    destination_latitude DECIMAL(10, 7),
    destination_longitude DECIMAL(10, 7),

    target_type VARCHAR(20) NOT NULL COMMENT 'departure 또는 arrival',
    target_datetime DATETIME NOT NULL COMMENT '출발 희망 시간 또는 도착 희망 시간',

    purpose_code VARCHAR(50) NOT NULL,
    preferred_transport VARCHAR(30) DEFAULT 'public_transport',

    walking_limit_minutes INT,
    max_transfer_count INT,
    base_travel_minutes INT DEFAULT 0,
    personal_buffer_minutes INT DEFAULT 0,

    request_client VARCHAR(20) DEFAULT 'unknown' COMMENT 'web, android, ios',
    request_status VARCHAR(30) DEFAULT 'requested',

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_trip_requests_user
        FOREIGN KEY (user_id) REFERENCES users(id)
        ON DELETE SET NULL,

    CONSTRAINT fk_trip_requests_purpose
        FOREIGN KEY (purpose_code) REFERENCES purpose_buffer_policies(purpose_code)
) ENGINE=InnoDB COMMENT='개인 출발/도착 추천 요청';


-- 5. 개인 추천 결과
CREATE TABLE IF NOT EXISTS trip_results (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    trip_request_id BIGINT NOT NULL,

    recommended_departure_time DATETIME,
    expected_arrival_time DATETIME,

    base_travel_minutes INT DEFAULT 0,
    transport_wait_minutes INT DEFAULT 0,
    weather_buffer_minutes INT DEFAULT 0,
    air_quality_buffer_minutes INT DEFAULT 0,
    traffic_buffer_minutes INT DEFAULT 0,
    congestion_buffer_minutes INT DEFAULT 0,
    disaster_buffer_minutes INT DEFAULT 0,
    purpose_buffer_minutes INT DEFAULT 0,
    personal_buffer_minutes INT DEFAULT 0,
    total_buffer_minutes INT DEFAULT 0,

    recommended_transport VARCHAR(30) DEFAULT 'public_transport',
    risk_score INT DEFAULT 0,
    risk_level VARCHAR(30) DEFAULT 'normal',

    weather_summary VARCHAR(255),
    air_quality_summary VARCHAR(255),
    congestion_summary VARCHAR(255),
    recommendation_summary TEXT,

    required_items JSON,
    calculation_detail JSON,
    raw_provider_data JSON,

    calculated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_trip_results_request
        FOREIGN KEY (trip_request_id) REFERENCES trip_requests(id)
        ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='개인 추천 출발 시간 계산 결과';


-- 6. 개인 추천 대체 경로
CREATE TABLE IF NOT EXISTS trip_route_options (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    trip_result_id BIGINT NOT NULL,

    option_type VARCHAR(30) DEFAULT 'fastest' COMMENT 'fastest, stable, less_crowded, weather_safe 등',
    provider VARCHAR(50) DEFAULT 'tmap',
    route_rank INT DEFAULT 1,

    total_travel_minutes INT NOT NULL,
    total_walk_minutes INT DEFAULT 0,
    transfer_count INT DEFAULT 0,
    fare INT,

    main_transport VARCHAR(30) DEFAULT 'mixed',
    average_congestion_rate DECIMAL(6, 2),
    risk_score INT DEFAULT 0,

    summary VARCHAR(255),
    route_steps JSON,
    route_geometry JSON,
    raw_route_json JSON,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_trip_route_options_result
        FOREIGN KEY (trip_result_id) REFERENCES trip_results(id)
        ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='개인 추천 결과의 대체 경로 목록';


-- 7. 약속방
CREATE TABLE IF NOT EXISTS rooms (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    host_user_id BIGINT NOT NULL,

    title VARCHAR(100) NOT NULL,
    description TEXT,

    destination_name VARCHAR(100),
    destination_address VARCHAR(255) NOT NULL,
    destination_latitude DECIMAL(10, 7) NOT NULL,
    destination_longitude DECIMAL(10, 7) NOT NULL,

    meeting_datetime DATETIME NOT NULL,
    recommended_arrival_datetime DATETIME NOT NULL,

    purpose_code VARCHAR(50) NOT NULL,
    default_buffer_minutes INT DEFAULT 10,
    max_members INT DEFAULT 10,

    status VARCHAR(30) DEFAULT 'open',
    reveal_destination_detail TINYINT(1) DEFAULT 1,
    share_late_risk TINYINT(1) DEFAULT 1,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    closed_at DATETIME,

    CONSTRAINT fk_rooms_host
        FOREIGN KEY (host_user_id) REFERENCES users(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_rooms_purpose
        FOREIGN KEY (purpose_code) REFERENCES purpose_buffer_policies(purpose_code)
) ENGINE=InnoDB COMMENT='약속방 기본 정보';


-- 8. 약속방 초대코드
CREATE TABLE IF NOT EXISTS room_invite_codes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    room_id BIGINT NOT NULL,

    invite_code VARCHAR(32) NOT NULL UNIQUE,
    is_active TINYINT(1) DEFAULT 1,
    expires_at DATETIME,

    created_by_user_id BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    disabled_at DATETIME,

    CONSTRAINT fk_invite_codes_room
        FOREIGN KEY (room_id) REFERENCES rooms(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_invite_codes_creator
        FOREIGN KEY (created_by_user_id) REFERENCES users(id)
        ON DELETE SET NULL
) ENGINE=InnoDB COMMENT='약속방 초대코드';


-- 9. 약속방 참여자
CREATE TABLE IF NOT EXISTS room_members (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    room_id BIGINT NOT NULL,
    user_id BIGINT NULL,

    guest_token_hash VARCHAR(255),

    nickname VARCHAR(50) NOT NULL,
    role VARCHAR(20) DEFAULT 'member',

    start_area_name VARCHAR(100),
    start_name VARCHAR(100),
    start_address VARCHAR(255),
    start_latitude DECIMAL(10, 7),
    start_longitude DECIMAL(10, 7),

    preferred_transport VARCHAR(30) DEFAULT 'public_transport',
    walking_limit_minutes INT,
    max_transfer_count INT,
    personal_buffer_minutes INT DEFAULT 0,

    member_status VARCHAR(30) DEFAULT 'not_configured',
    join_client VARCHAR(20) DEFAULT 'unknown',

    joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    departed_at DATETIME,
    arrived_at DATETIME,
    left_at DATETIME,

    CONSTRAINT fk_room_members_room
        FOREIGN KEY (room_id) REFERENCES rooms(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_room_members_user
        FOREIGN KEY (user_id) REFERENCES users(id)
        ON DELETE SET NULL
) ENGINE=InnoDB COMMENT='약속방 참여자 정보';


-- 10. 약속방 참여자별 추천 결과
CREATE TABLE IF NOT EXISTS room_member_results (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    room_member_id BIGINT NOT NULL,
    room_id BIGINT NOT NULL,

    recommended_departure_time DATETIME,
    expected_arrival_time DATETIME,

    base_travel_minutes INT DEFAULT 0,
    transport_wait_minutes INT DEFAULT 0,
    weather_buffer_minutes INT DEFAULT 0,
    traffic_buffer_minutes INT DEFAULT 0,
    congestion_buffer_minutes INT DEFAULT 0,
    disaster_buffer_minutes INT DEFAULT 0,
    purpose_buffer_minutes INT DEFAULT 0,
    personal_buffer_minutes INT DEFAULT 0,
    total_buffer_minutes INT DEFAULT 0,

    recommended_transport VARCHAR(30) DEFAULT 'public_transport',
    transfer_count INT,
    total_walk_minutes INT,
    fare INT,

    risk_score INT DEFAULT 0,
    risk_level VARCHAR(30) DEFAULT 'normal',

    subway_congestion_summary VARCHAR(255),
    weather_summary VARCHAR(255),
    air_quality_summary VARCHAR(255),
    reason_summary TEXT,

    required_items JSON,
    route_steps JSON,
    raw_route_json JSON,

    calculated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_room_member_results_member
        FOREIGN KEY (room_member_id) REFERENCES room_members(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_room_member_results_room
        FOREIGN KEY (room_id) REFERENCES rooms(id)
        ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='약속방 참여자별 추천 출발 시간 결과';


-- 11. 약속방 전체 요약
CREATE TABLE IF NOT EXISTS room_summary_snapshots (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    room_id BIGINT NOT NULL,

    total_members INT DEFAULT 0,
    configured_members INT DEFAULT 0,
    expected_on_time_members INT DEFAULT 0,
    high_risk_members INT DEFAULT 0,

    latest_expected_arrival_time DATETIME,
    group_risk_score INT DEFAULT 0,
    group_risk_level VARCHAR(30) DEFAULT 'normal',

    suggested_meeting_datetime DATETIME,
    summary_message TEXT,
    risk_factors JSON,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_room_summary_room
        FOREIGN KEY (room_id) REFERENCES rooms(id)
        ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='약속방 전체 정시 도착 가능성 요약';


-- 12. 약속방 변경 이력
CREATE TABLE IF NOT EXISTS room_change_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    room_id BIGINT NOT NULL,
    changed_by_user_id BIGINT NULL,

    change_type VARCHAR(50) NOT NULL,
    before_value JSON,
    after_value JSON,
    message VARCHAR(255),

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_room_change_logs_room
        FOREIGN KEY (room_id) REFERENCES rooms(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_room_change_logs_user
        FOREIGN KEY (changed_by_user_id) REFERENCES users(id)
        ON DELETE SET NULL
) ENGINE=InnoDB COMMENT='약속방 변경 이력';


-- 13. 알림
CREATE TABLE IF NOT EXISTS notifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NULL,
    room_id BIGINT NULL,
    room_member_id BIGINT NULL,
    trip_result_id BIGINT NULL,

    notification_type VARCHAR(50) NOT NULL,
    notification_channel VARCHAR(30) DEFAULT 'in_app',

    title VARCHAR(100) NOT NULL,
    message TEXT NOT NULL,

    scheduled_at DATETIME,
    sent_at DATETIME,
    read_at DATETIME,
    is_read TINYINT(1) DEFAULT 0,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_notifications_user
        FOREIGN KEY (user_id) REFERENCES users(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_notifications_room
        FOREIGN KEY (room_id) REFERENCES rooms(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_notifications_member
        FOREIGN KEY (room_member_id) REFERENCES room_members(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_notifications_trip_result
        FOREIGN KEY (trip_result_id) REFERENCES trip_results(id)
        ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='출발 알림, 약속 변경 알림';


-- 14. 기상청 날씨 데이터 캐시
CREATE TABLE IF NOT EXISTS weather_snapshots (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    provider VARCHAR(50) DEFAULT 'kma',

    location_key VARCHAR(100) NOT NULL,
    grid_x INT,
    grid_y INT,
    latitude DECIMAL(10, 7),
    longitude DECIMAL(10, 7),

    base_date VARCHAR(8),
    base_time VARCHAR(4),
    forecast_datetime DATETIME NOT NULL,

    sky_status VARCHAR(50),
    precipitation_type VARCHAR(50),
    precipitation_probability INT,
    precipitation_amount VARCHAR(50),
    temperature DECIMAL(5, 2),
    humidity INT,
    wind_speed DECIMAL(5, 2),
    snowfall VARCHAR(50),

    raw_json JSON,
    fetched_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expires_at DATETIME NOT NULL,

    UNIQUE KEY uq_weather_snapshot (provider, location_key, forecast_datetime)
) ENGINE=InnoDB COMMENT='기상청 단기예보/초단기예보 캐시';


-- 15. 에어코리아 대기질 데이터 캐시
CREATE TABLE IF NOT EXISTS air_quality_snapshots (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    provider VARCHAR(50) DEFAULT 'airkorea',

    station_name VARCHAR(100) NOT NULL,
    sido_name VARCHAR(50),
    station_latitude DECIMAL(10, 7),
    station_longitude DECIMAL(10, 7),

    data_time DATETIME NOT NULL,

    pm10_value INT,
    pm25_value INT,
    o3_value DECIMAL(6, 4),
    no2_value DECIMAL(6, 4),
    co_value DECIMAL(6, 3),
    so2_value DECIMAL(6, 4),

    khai_value INT,
    khai_grade VARCHAR(20),
    pm10_grade VARCHAR(20),
    pm25_grade VARCHAR(20),

    raw_json JSON,
    fetched_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expires_at DATETIME NOT NULL,

    UNIQUE KEY uq_air_quality_station_time (station_name, data_time)
) ENGINE=InnoDB COMMENT='에어코리아 대기질 정보 캐시';


-- 16. 지하철 평균 혼잡도 통계
CREATE TABLE IF NOT EXISTS subway_congestion_stats (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,

    data_source VARCHAR(100) NOT NULL,
    line_name VARCHAR(50) NOT NULL,
    station_name VARCHAR(100) NOT NULL,
    direction VARCHAR(100) NOT NULL,

    day_type VARCHAR(30) NOT NULL COMMENT 'weekday, saturday, sunday_holiday',
    time_slot_start TIME NOT NULL,
    time_slot_end TIME NOT NULL,

    congestion_rate DECIMAL(6, 2) NOT NULL,
    congestion_level VARCHAR(30) DEFAULT 'unknown',
    quarter VARCHAR(20),

    raw_json JSON,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    UNIQUE KEY uq_subway_congestion (
        data_source,
        line_name,
        station_name,
        direction,
        day_type,
        time_slot_start,
        quarter
    )
) ENGINE=InnoDB COMMENT='공공데이터 기반 지하철 시간대별 평균 혼잡도';


-- 17. 지하철 지연/사고/무정차 알림
CREATE TABLE IF NOT EXISTS subway_alerts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,

    provider VARCHAR(100) NOT NULL,
    line_name VARCHAR(50),
    station_name VARCHAR(100),

    alert_type VARCHAR(50) DEFAULT 'etc',
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,

    started_at DATETIME,
    ended_at DATETIME,

    raw_json JSON,
    fetched_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expires_at DATETIME NOT NULL
) ENGINE=InnoDB COMMENT='지하철 지연, 사고, 무정차 등 이례상황 캐시';


-- 18. 외부 API 응답 캐시
CREATE TABLE IF NOT EXISTS api_cache (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,

    provider VARCHAR(50) NOT NULL,
    endpoint VARCHAR(255) NOT NULL,
    cache_key VARCHAR(191) NOT NULL UNIQUE,

    request_hash VARCHAR(128),
    response_json JSON NOT NULL,
    status_code INT,

    fetched_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expires_at DATETIME NOT NULL
) ENGINE=InnoDB COMMENT='외부 API 응답 캐시';


-- 19. 외부 API 호출 로그
CREATE TABLE IF NOT EXISTS api_call_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,

    provider VARCHAR(50) NOT NULL,
    endpoint VARCHAR(255) NOT NULL,
    request_hash VARCHAR(128),

    status_code INT,
    success TINYINT(1) DEFAULT 0,
    error_message TEXT,
    elapsed_ms INT,

    called_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='외부 API 호출 이력';


-- 20. 로그인 유지용 Refresh Token
CREATE TABLE IF NOT EXISTS auth_refresh_tokens (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,

    user_id BIGINT NOT NULL,
    refresh_token_hash VARCHAR(255) NOT NULL,
    client_type VARCHAR(20) NOT NULL,

    device_id BIGINT,
    user_agent VARCHAR(500),
    ip_address VARCHAR(45),

    issued_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expires_at DATETIME NOT NULL,
    revoked_at DATETIME,
    is_active TINYINT(1) DEFAULT 1,

    CONSTRAINT fk_auth_refresh_tokens_user
        FOREIGN KEY (user_id) REFERENCES users(id)
        ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='웹/앱 로그인 유지용 Refresh Token';


-- 21. 사용자 기기
CREATE TABLE IF NOT EXISTS user_devices (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,

    user_id BIGINT NOT NULL,

    client_type VARCHAR(20) NOT NULL,
    device_name VARCHAR(100),
    device_uuid VARCHAR(255),

    push_token VARCHAR(500),
    push_enabled TINYINT(1) DEFAULT 1,

    last_login_at DATETIME,
    last_active_at DATETIME,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_user_devices_user
        FOREIGN KEY (user_id) REFERENCES users(id)
        ON DELETE CASCADE,

    UNIQUE KEY uq_user_device_uuid (user_id, device_uuid)
) ENGINE=InnoDB COMMENT='웹/모바일 사용자 기기 및 푸시 토큰 관리';


-- 22. 알림 발송 기록
CREATE TABLE IF NOT EXISTS notification_deliveries (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,

    notification_id BIGINT NOT NULL,
    user_device_id BIGINT,

    channel VARCHAR(30) DEFAULT 'in_app',
    delivery_status VARCHAR(30) DEFAULT 'pending',

    sent_at DATETIME,
    failed_at DATETIME,
    read_at DATETIME,
    error_message TEXT,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_notification_deliveries_notification
        FOREIGN KEY (notification_id) REFERENCES notifications(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_notification_deliveries_device
        FOREIGN KEY (user_device_id) REFERENCES user_devices(id)
        ON DELETE SET NULL
) ENGINE=InnoDB COMMENT='웹/앱 알림 발송 기록';


-- 23. 웹/앱 활동 로그
CREATE TABLE IF NOT EXISTS app_activity_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,

    user_id BIGINT NULL,
    client_type VARCHAR(20) DEFAULT 'unknown',

    action_type VARCHAR(100) NOT NULL,
    target_type VARCHAR(100),
    target_id BIGINT,

    description VARCHAR(255),
    metadata JSON,

    ip_address VARCHAR(45),
    user_agent VARCHAR(500),

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_app_activity_logs_user
        FOREIGN KEY (user_id) REFERENCES users(id)
        ON DELETE SET NULL
) ENGINE=InnoDB COMMENT='웹/모바일 앱 사용자 활동 기록';