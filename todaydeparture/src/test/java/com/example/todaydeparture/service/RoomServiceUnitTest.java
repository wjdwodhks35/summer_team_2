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
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class RoomServiceUnitTest {

    @Test
    void createRoomPersistsRequestValuesInsteadOfFallbackConstants() {
        FakeRoomMapper roomMapper = new FakeRoomMapper();
        RoomService service = new RoomService(roomMapper, new FakeUserMapper());

        CreateRoomRequest request = new CreateRoomRequest();
        request.setTitle("팀 회의");
        request.setDestination("서울역");
        request.setMeetingTime("2026-07-24T19:30:00");
        request.setDestinationLatitude(37.5547);
        request.setDestinationLongitude(126.9706);
        request.setPurposeCode("appointment");

        RoomResponse response = service.createRoom(request, "token-1");

        assertThat(response.getRoomId()).isEqualTo(100L);
        assertThat(response.getDestination()).isEqualTo("서울역");
        assertThat(response.getMeetingTime()).isEqualTo("2026-07-24 19:30:00");
        assertThat(response.getInviteCode()).hasSize(6);
        assertThat(roomMapper.insertedRoom.getDestinationLatitude()).isEqualTo(37.5547);
        assertThat(roomMapper.insertedRoom.getDestinationLongitude()).isEqualTo(126.9706);
        assertThat(roomMapper.insertedRoom.getPurposeCode()).isEqualTo("appointment");
    }

    @Test
    void joinRoomUsesInviteCodeAndReturnsStoredRoomInfo() {
        FakeRoomMapper roomMapper = new FakeRoomMapper();
        RoomService service = new RoomService(roomMapper, new FakeUserMapper());

        JoinRoomRequest request = new JoinRoomRequest();
        request.setInviteCode("abc123");
        request.setNickname("게스트A");

        RoomResponse response = service.joinRoom(request, null);

        assertThat(response.getRoomId()).isEqualTo(100L);
        assertThat(response.getInviteCode()).isEqualTo("ABC123");
        assertThat(response.getDestination()).isEqualTo("서울역");
        assertThat(roomMapper.insertedMemberNickname).isEqualTo("게스트A");
    }

    private static class FakeUserMapper implements UserMapper {
        @Override
        public UserDto findByEmail(String email) {
            return null;
        }

        @Override
        public UserDto findByToken(String token) {
            if (!"token-1".equals(token)) {
                return null;
            }
            UserDto user = new UserDto();
            user.setId(1L);
            user.setEmail("test@example.com");
            user.setNickname("테스터");
            user.setAccessToken(token);
            return user;
        }

        @Override
        public int countByEmail(String email) {
            return 0;
        }

        @Override
        public int insert(UserDto user) {
            return 1;
        }

        @Override
        public void updateToken(Long id, String accessToken) {
        }

        @Override
        public int insertUser(Map<String, Object> params) {
            return 1;
        }

        @Override
        public Map<String, Object> findUserMapByEmail(String email) {
            return null;
        }

        @Override
        public int updateUserProfile(Map<String, Object> params) {
            return 1;
        }
    }

    private static class FakeRoomMapper implements RoomMapper {
        RoomInsertParam insertedRoom;
        String insertedInviteCode = "ABC123";
        String insertedMemberNickname;
        final List<RoomMemberFullDto> members = new ArrayList<>();

        @Override
        public int insertRoomParam(RoomInsertParam param) {
            param.setId(100L);
            insertedRoom = param;
            return 1;
        }

        @Override
        public int insertInviteCode(Long roomId, String inviteCode) {
            insertedInviteCode = inviteCode;
            return 1;
        }

        @Override
        public Long findRoomIdByInviteCode(String inviteCode) {
            return "ABC123".equals(inviteCode) ? 100L : null;
        }

        @Override
        public RoomInfoDto findRoomById(Long roomId) {
            RoomInfoDto info = new RoomInfoDto();
            info.setRoomId(roomId);
            info.setTitle("팀 회의");
            info.setDestination("서울역");
            info.setMeetingTime("2026-07-24T19:30:00");
            info.setInviteCode(insertedInviteCode);
            return info;
        }

        @Override
        public int insertMember(Long roomId, Long userId, String guestTokenHash, String nickname) {
            insertedMemberNickname = nickname;
            return 1;
        }

        @Override
        public int insertHostMember(Long roomId, Long userId, String nickname) {
            RoomMemberFullDto member = new RoomMemberFullDto();
            member.setUserId(userId);
            member.setName(nickname);
            member.setHost(true);
            members.add(member);
            return 1;
        }

        @Override
        public int countMatchingMember(Long roomId, Long userId, String guestTokenHash) {
            return 0;
        }

        @Override
        public int countMembers(Long roomId) {
            return Math.max(1, members.size());
        }

        @Override
        public List<RoomMemberDto> findMembersByRoomId(Long roomId) {
            return List.of();
        }

        @Override
        public RoomFullResponse findRoomFullById(Long roomId) {
            RoomFullResponse response = new RoomFullResponse();
            response.setId(roomId);
            response.setName("팀 회의");
            response.setCode(insertedInviteCode);
            response.setDestination("서울역");
            response.setHostName("테스터");
            return response;
        }

        @Override
        public List<RoomFullResponse> findRoomsByUserId(Long userId) {
            return List.of(findRoomFullById(100L));
        }

        @Override
        public int deleteMemberByRoomAndUser(Long roomId, Long userId) {
            return roomId == 100L && userId == 1L ? 1 : 0;
        }

        @Override
        public List<RoomMemberFullDto> findMembersFullByRoomId(Long roomId) {
            return members;
        }

        @Override
        public int updateMemberLocation(Long roomId, Long userId, String address) {
            return 1;
        }
    }
}
