package com.example.todaydeparture.service;

import com.example.todaydeparture.dto.CreateRoomRequest;
import com.example.todaydeparture.dto.JoinRoomRequest;
import com.example.todaydeparture.dto.LocationUpdateServerRequest;
import com.example.todaydeparture.dto.RoomFullResponse;
import com.example.todaydeparture.dto.RoomInfoDto;
import com.example.todaydeparture.dto.RoomInsertParam;
import com.example.todaydeparture.dto.RoomMemberDto;
import com.example.todaydeparture.dto.RoomMemberFullDto;
import com.example.todaydeparture.dto.RoomResponse;
import com.example.todaydeparture.dto.UserDto;
import com.example.todaydeparture.mapper.RoomMapper;
import com.example.todaydeparture.mapper.UserMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
public class RoomService {

    private final RoomMapper roomMapper;
    private final UserMapper userMapper;

    public RoomService(RoomMapper roomMapper, UserMapper userMapper) {
        this.roomMapper = roomMapper;
        this.userMapper = userMapper;
    }

    public RoomResponse createRoom(CreateRoomRequest req, String token) {
        UserDto user = userMapper.findByToken(token);
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다");
        }

        String destination = trimToNull(req.getDestination());
        String meetingTime = normalizeDateTime(req.getMeetingTime());
        if (destination == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "목적지를 입력해주세요");
        }
        if (meetingTime == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "약속 시간을 입력해주세요");
        }
        double destLat = req.getDestinationLatitude() != null ? req.getDestinationLatitude() : 0.0;
        double destLon = req.getDestinationLongitude() != null ? req.getDestinationLongitude() : 0.0;

        RoomInsertParam param = new RoomInsertParam();
        param.setHostUserId(user.getId());
        param.setTitle(trimToNull(req.getTitle()) != null ? req.getTitle().trim() : destination);
        param.setDestinationAddress(destination);
        param.setDestinationLatitude(destLat);
        param.setDestinationLongitude(destLon);
        param.setMeetingDatetime(meetingTime);
        param.setPurposeCode(req.getPurposeCode());

        roomMapper.insertRoomParam(param);
        Long roomId = param.getId();

        String inviteCode = UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();
        roomMapper.insertInviteCode(roomId, inviteCode);
        roomMapper.insertHostMember(roomId, user.getId(), user.getNickname());

        int memberCount = roomMapper.countMembers(roomId);
        return new RoomResponse(roomId, param.getTitle(), destination,
                meetingTime, inviteCode, memberCount, user.getNickname());
    }

    public RoomResponse joinRoom(JoinRoomRequest req, String token) {
        String inviteCode = trimToNull(req.getInviteCode());
        if (inviteCode == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "초대코드를 입력해주세요");
        }
        inviteCode = inviteCode.replace("-", "").replaceAll("\\s+", "").toUpperCase();
        Long roomId = roomMapper.findRoomIdByInviteCode(inviteCode);
        if (roomId == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "유효하지 않은 초대코드입니다");
        }

        Long userId = null;
        String nickname = trimToNull(req.getNickname()) != null ? req.getNickname().trim() : "게스트";
        String guestTokenHash = trimToNull(req.getGuestToken());

        if (guestTokenHash == null && token != null) {
            UserDto user = userMapper.findByToken(token);
            if (user != null) {
                userId = user.getId();
                nickname = user.getNickname();
            }
        }

        if (userId == null && guestTokenHash == null) {
            guestTokenHash = UUID.randomUUID().toString();
        }

        if (roomMapper.countMatchingMember(roomId, userId, guestTokenHash) == 0) {
            roomMapper.insertMember(roomId, userId, guestTokenHash, nickname);
        }

        RoomInfoDto info = roomMapper.findRoomById(roomId);
        int memberCount = roomMapper.countMembers(roomId);
        RoomFullResponse full = roomMapper.findRoomFullById(roomId);
        String hostName = (full != null && full.getHostName() != null) ? full.getHostName() : "";
        return new RoomResponse(info.getRoomId(), info.getTitle(), info.getDestination(),
                info.getMeetingTime(), info.getInviteCode(), memberCount, hostName);
    }

    public List<RoomMemberDto> getMembers(Long roomId) {
        return roomMapper.findMembersByRoomId(roomId);
    }

    public List<RoomFullResponse> getMyRooms(String token) {
        UserDto user = userMapper.findByToken(token);
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }
        List<RoomFullResponse> rooms = roomMapper.findRoomsByUserId(user.getId());
        return rooms != null ? rooms : Collections.emptyList();
    }

    public void leaveRoom(Long roomId, String token) {
        UserDto user = userMapper.findByToken(token);
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }
        if (roomMapper.deleteMemberByRoomAndUser(roomId, user.getId()) == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "참여 중인 약속방을 찾을 수 없습니다.");
        }
    }

    public RoomFullResponse getRoomInfo(Long roomId) {
        RoomFullResponse info = roomMapper.findRoomFullById(roomId);
        if (info == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "방을 찾을 수 없습니다");
        }
        List<RoomMemberFullDto> members = roomMapper.findMembersFullByRoomId(roomId);
        info.setMembers(members != null ? members : Collections.emptyList());
        info.setMemberCount(info.getMembers().size());
        return info;
    }

    public void updateLocation(Long roomId, String token, LocationUpdateServerRequest req) {
        if (token == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다");
        }
        UserDto user = userMapper.findByToken(token);
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다");
        }
        String address = trimToNull(req.getAddress()) != null ? req.getAddress().trim() : "출발지";
        Double lat = req.getLat();
        Double lon = req.getLon();

        // row가 없으면(예: 예전 방의 방장) 먼저 insert 후 update
        int updated = roomMapper.updateMemberLocation(roomId, user.getId(), address, lat, lon);
        if (updated == 0) {
            roomMapper.insertMember(roomId, user.getId(), null, user.getNickname());
            roomMapper.updateMemberLocation(roomId, user.getId(), address, lat, lon);
        }
    }

    private String normalizeDateTime(String value) {
        String trimmed = trimToNull(value);
        return trimmed == null ? null : trimmed.replace('T', ' ');
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
