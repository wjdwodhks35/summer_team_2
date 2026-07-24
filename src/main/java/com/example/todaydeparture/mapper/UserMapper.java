package com.example.todaydeparture.mapper;

import com.example.todaydeparture.dto.UserDto;
import org.apache.ibatis.annotations.*;

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
}