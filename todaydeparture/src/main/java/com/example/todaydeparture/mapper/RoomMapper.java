package com.example.todaydeparture.mapper;

import com.example.todaydeparture.dto.RoomInfoDto;
import com.example.todaydeparture.dto.RoomMemberDto;
import com.example.todaydeparture.dto.RoomInsertParam;
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

    @Select("SELECT room_id FROM room_invite_codes WHERE invite_code = #{inviteCode} AND is_active = 1")
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

    @Select("SELECT COUNT(*) FROM room_members WHERE room_id = #{roomId}")
    int countMembers(Long roomId);

    @Select("SELECT id as memberId, nickname, start_address as currentLocation, " +
            "NULL as recommendedDepartureTime, " +
            "(member_status = 'ready') as isReady " +
            "FROM room_members WHERE room_id = #{roomId}")
    List<RoomMemberDto> findMembersByRoomId(Long roomId);
}