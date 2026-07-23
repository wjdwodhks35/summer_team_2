package com.example.todaydeparture.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.Map;

@Mapper
public interface UserMapper {
    // 1. 회원가입 (users 테이블에 INSERT)
    int insertUser(Map<String, Object> params);

    // 2. 로그인 및 회원 조회 (email로 정보 찾기)
    Map<String, Object> findByEmail(@Param("email") String email);

    // 3. 프로필 정보 수정
    int updateUserProfile(Map<String, Object> params);
}