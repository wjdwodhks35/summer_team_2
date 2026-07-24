package com.example.todaydeparture.mapper;

import com.example.todaydeparture.dto.RoomInfoDto;
import com.example.todaydeparture.dto.RoomMemberDto;
import com.example.todaydeparture.dto.RoomInsertParam;
import com.example.todaydeparture.dto.RoomFullResponse;
import com.example.todaydeparture.dto.RoomMemberFullDto;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface RoomMapper {

    @Insert("INSERT INTO rooms (host_user_id, title, destination_address, " +
            "destination_latitude, destination_longitude, " +
            "meeting_datetime, recommended_arrival_datetime, purpose_code) " +
            "VALUES (#{hostUserId}, #{title}, #{destinationAddress}, " +
            "0.0, 0.0, " +
            "IFNULL(NULLIF(#{meetingDatetime},''), DATE_ADD(NOW(), INTERVAL 1 DAY)), " +
            "IFNULL(NULLIF(#{meetingDatetime},''), DATE_ADD(NOW(), INTERVAL 1 DAY)), " +
            "'GENERAL')")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertRoomParam(RoomInsertParam param);

    @Insert("INSERT INTO room_invite_codes (room_id, invite_code, expires_at) " +
            "VALUES (#{roomId}, #{inviteCode}, DATE_ADD(NOW(), INTERVAL 7 DAY))")
    int insertInviteCode(@Param("roomId") Long roomId, @Param("inviteCode") String inviteCode);

    @Select("SELECT room_id FROM room_invite_codes " +
            "WHERE invite_code = #{inviteCode} AND is_active = 1 " +
            "AND (expires_at IS NULL OR expires_at > NOW())")
    Long findRoomIdByInviteCode(String inviteCode);

    @Select("SELECT r.id as roomId, r.title, r.destination_address as destination, " +
            "DATE_FORMAT(r.meeting_datetime, '%Y-%m-%dT%H:%i:%s') as meetingTime, " +
            "i.invite_code as inviteCode " +
            "FROM rooms r " +
            "LEFT JOIN room_invite_codes i ON r.id = i.room_id AND i.is_active = 1 " +
            "WHERE r.id = #{roomId} LIMIT 1")
    RoomInfoDto findRoomById(Long roomId);

    @Insert("INSERT INTO room_members (room_id, user_id, guest_token_hash, nickname, role) " +
            "VALUES (#{roomId}, #{userId}, #{guestTokenHash}, #{nickname}, 'member')")
    int insertMember(@Param("roomId") Long roomId,
                     @Param("userId") Long userId,
                     @Param("guestTokenHash") String guestTokenHash,
                     @Param("nickname") String nickname);

    @Insert("INSERT INTO room_members (room_id, user_id, nickname, role) " +
            "VALUES (#{roomId}, #{userId}, #{nickname}, 'host')")
    int insertHostMember(@Param("roomId") Long roomId,
                         @Param("userId") Long userId,
                         @Param("nickname") String nickname);

    @Select("SELECT COUNT(*) FROM room_members WHERE room_id = #{roomId} " +
            "AND ((#{userId} IS NOT NULL AND user_id = #{userId}) " +
            "OR (#{guestTokenHash} IS NOT NULL AND guest_token_hash = #{guestTokenHash}))")
    int countMatchingMember(@Param("roomId") Long roomId,
                            @Param("userId") Long userId,
                            @Param("guestTokenHash") String guestTokenHash);

    @Select("SELECT COUNT(*) FROM room_members WHERE room_id = #{roomId}")
    int countMembers(Long roomId);

    @Select("SELECT id as memberId, nickname, start_address as currentLocation, " +
            "NULL as recommendedDepartureTime, " +
            "(member_status = 'ready') as isReady " +
            "FROM room_members WHERE room_id = #{roomId}")
    List<RoomMemberDto> findMembersByRoomId(Long roomId);

    // ── 방 전체 정보 (hostName 포함) ──────────────────────
    @Select("SELECT r.id as id, r.title as name, " +
            "COALESCE(i.invite_code, '') as code, " +
            "r.destination_address as destination, " +
            "r.destination_latitude as destLat, " +
            "r.destination_longitude as destLon, " +
            "DATE_FORMAT(r.meeting_datetime, '%Y-%m-%d %H:%i') as appointmentTime, " +
            "COALESCE(u.nickname, '알 수 없음') as hostName " +
            "FROM rooms r " +
            "LEFT JOIN users u ON r.host_user_id = u.id " +
            "LEFT JOIN room_invite_codes i ON r.id = i.room_id AND i.is_active = 1 " +
            "WHERE r.id = #{roomId} LIMIT 1")
    RoomFullResponse findRoomFullById(Long roomId);

    // ── 멤버 전체 정보 (isHost, 출발지 포함) ───────────────
    @Select("SELECT rm.user_id as userId, rm.nickname as name, " +
            "(r.host_user_id = rm.user_id) as isHost, " +
            "(rm.start_address IS NOT NULL AND rm.start_address != '') as isLocationShared, " +
            "NULL as departureLat, NULL as departureLon, " +
            "rm.start_address as departureAddress, " +
            "NULL as estimatedTravelMin, NULL as recommendedDepartureTime " +
            "FROM room_members rm " +
            "JOIN rooms r ON rm.room_id = r.id " +
            "WHERE rm.room_id = #{roomId}")
    List<RoomMemberFullDto> findMembersFullByRoomId(Long roomId);

    // ── 멤버 출발지 업데이트 ───────────────────────────────
    @Update("UPDATE room_members SET start_address = #{address}, member_status = 'ready' " +
            "WHERE room_id = #{roomId} AND user_id = #{userId}")
    int updateMemberLocation(@Param("roomId") Long roomId,
                             @Param("userId") Long userId,
                             @Param("address") String address);
}
