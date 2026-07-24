package com.example.todaydeparture.service;

import com.example.todaydeparture.dto.*;
import java.util.Collections;
import com.example.todaydeparture.mapper.RoomMapper;
import com.example.todaydeparture.mapper.UserMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

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

        RoomInsertParam param = new RoomInsertParam();
        param.setHostUserId(user.getId());
        param.setTitle(req.getTitle());
        param.setDestinationAddress(
            (req.getDestination() == null || req.getDestination().isEmpty()) ? "미정" : req.getDestination()
        );
        param.setMeetingDatetime(req.getMeetingTime());

        roomMapper.insertRoomParam(param);  // param.getId()에 생성된 roomId 들어옴
        Long roomId = param.getId();

        String inviteCode = UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();
        roomMapper.insertInviteCode(roomId, inviteCode);
        roomMapper.insertHostMember(roomId, user.getId(), user.getNickname());

        int memberCount = roomMapper.countMembers(roomId);
        return new RoomResponse(roomId, req.getTitle(), req.getDestination(),
                req.getMeetingTime(), inviteCode, memberCount, user.getNickname());
    }

    public RoomResponse joinRoom(JoinRoomRequest req, String token) {
        String inviteCode = req.getInviteCode();
        if (inviteCode == null || inviteCode.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "초대코드를 입력해주세요");
        }
        inviteCode = inviteCode.replace("-", "").replaceAll("\\s+", "").toUpperCase();
        Long roomId = roomMapper.findRoomIdByInviteCode(inviteCode);
        if (roomId == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "유효하지 않은 초대코드입니다");
        }

        Long userId = null;
        String nickname = (req.getNickname() == null || req.getNickname().isBlank())
                ? "게스트" : req.getNickname().trim();
        String guestTokenHash = req.getGuestToken();

        if (req.getGuestToken() == null && token != null) {
            UserDto user = userMapper.findByToken(token);
            if (user != null) {
                userId = user.getId();
                nickname = user.getNickname();
                guestTokenHash = null;
            }
        }

        if (userId == null && (guestTokenHash == null || guestTokenHash.isBlank())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "게스트 토큰이 필요합니다");
        }

        if (roomMapper.countMatchingMember(roomId, userId, guestTokenHash) == 0) {
            roomMapper.insertMember(roomId, userId, guestTokenHash, nickname);
        }

        RoomInfoDto info = roomMapper.findRoomById(roomId);
        int memberCount = roomMapper.countMembers(roomId);
        // 방장 이름 가져오기 (findRoomFullById로 한번에)
        RoomFullResponse full = roomMapper.findRoomFullById(roomId);
        String hostName = (full != null && full.getHostName() != null) ? full.getHostName() : "";
        return new RoomResponse(info.getRoomId(), info.getTitle(), info.getDestination(),
                info.getMeetingTime(), info.getInviteCode(), memberCount, hostName);
    }

    public List<RoomMemberDto> getMembers(Long roomId) {
        return roomMapper.findMembersByRoomId(roomId);
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
        String address = (req.getAddress() != null && !req.getAddress().isEmpty())
                ? req.getAddress() : "현재 위치";
        roomMapper.updateMemberLocation(roomId, user.getId(), address);
    }
}
