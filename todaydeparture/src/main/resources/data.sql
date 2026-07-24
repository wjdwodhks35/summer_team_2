INSERT INTO purpose_buffer_policies
(purpose_code, purpose_name, default_buffer_minutes, min_buffer_minutes, max_buffer_minutes, description)
VALUES
('general', '일반 외출', 5, 0, 30, '가벼운 외출'),
('appointment', '약속', 10, 5, 40, '약속 시간 전 도착 권장'),
('school', '등교', 10, 5, 40, '수업 시작 전 도착 권장'),
('work', '출근', 15, 5, 50, '업무 시작 전 도착 권장'),
('hospital', '병원 예약', 15, 10, 50, '예약 시간 전 도착 권장'),
('exam', '시험', 30, 20, 60, '시험 전 여유 도착 권장'),
('interview', '면접', 30, 20, 60, '면접 전 여유 도착 권장'),
('train', '기차 탑승', 30, 20, 90, '기차 출발 전 도착 권장'),
('airport', '공항 이동', 60, 40, 180, '공항 수속 시간 고려')
ON DUPLICATE KEY UPDATE
purpose_name = VALUES(purpose_name),
default_buffer_minutes = VALUES(default_buffer_minutes),
min_buffer_minutes = VALUES(min_buffer_minutes),
max_buffer_minutes = VALUES(max_buffer_minutes),
description = VALUES(description);