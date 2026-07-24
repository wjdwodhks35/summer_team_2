package com.example.todaydeparture.mapper;

import com.example.todaydeparture.dto.UserDto;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.Map;

@Mapper
public interface UserMapper {

    @Select("SELECT id, email, password_hash, nickname, access_token FROM users WHERE email = #{email}")
    UserDto findByEmail(String email);

    @Select("SELECT id, email, nickname, access_token FROM users WHERE access_token = #{token}")
    UserDto findByToken(String token);

    @Select("SELECT COUNT(*) FROM users WHERE email = #{email}")
    int countByEmail(String email);

    @Insert("INSERT INTO users (email, password_hash, nickname, access_token) " +
            "VALUES (#{email}, #{passwordHash}, #{nickname}, #{accessToken})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(UserDto user);

    @Update("UPDATE users SET access_token = #{accessToken} WHERE id = #{id}")
    void updateToken(@Param("id") Long id, @Param("accessToken") String accessToken);

    @Insert("INSERT INTO users (email, password_hash, nickname, preferred_transport) " +
            "VALUES (#{email}, #{passwordHash}, #{nickname}, #{preferredTransport})")
    int insertUser(Map<String, Object> params);

    @Select("SELECT id, email, password_hash AS passwordHash, nickname, " +
            "default_start_location AS defaultStartLocation, preferred_transport AS preferredTransport, " +
            "is_active AS isActive, access_token AS accessToken " +
            "FROM users WHERE email = #{email} AND is_active = 1")
    Map<String, Object> findUserMapByEmail(@Param("email") String email);

    @Update("UPDATE users SET nickname = #{nickname}, default_start_location = #{defaultStartLocation}, " +
            "preferred_transport = #{preferredTransport}, updated_at = NOW() WHERE id = #{userId}")
    int updateUserProfile(Map<String, Object> params);
}
