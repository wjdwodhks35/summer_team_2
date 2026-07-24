package com.example.todaydeparture.controller;

import com.example.todaydeparture.dto.UserDto;
import com.example.todaydeparture.mapper.UserMapper;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserMapper userMapper;

    public UserController(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    // 1. 회원가입 API
    @PostMapping("/signup")
    public Map<String, Object> signup(@RequestBody Map<String, Object> params) {
        Map<String, Object> response = new HashMap<>();

        // 이메일 중복 검사
        if (userMapper.countByEmail((String) params.get("email")) > 0) {
            response.put("success", false);
            response.put("message", "이미 가입된 이메일입니다.");
            return response;
        }

        UserDto user = new UserDto();
        user.setEmail((String) params.get("email"));
        user.setPasswordHash((String) params.get("password")); // 운영 시 BCrypt 암호화 필요
        user.setNickname((String) params.getOrDefault("nickname", "사용자"));
        user.setAccessToken(UUID.randomUUID().toString());

        int result = userMapper.insert(user);
        response.put("success", result > 0);
        response.put("message", result > 0 ? "회원가입이 완료되었습니다." : "가입 실패");
        return response;
    }

    // 2. 로그인 API
    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Map<String, Object> params) {
        Map<String, Object> response = new HashMap<>();
        String email    = (String) params.get("email");
        String password = (String) params.get("password");

        UserDto user = userMapper.findByEmail(email);

        if (user == null || !password.equals(user.getPasswordHash())) {
            response.put("success", false);
            response.put("message", "이메일 또는 비밀번호가 일치하지 않습니다.");
            return response;
        }

        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id",          user.getId());
        userInfo.put("email",       user.getEmail());
        userInfo.put("nickname",    user.getNickname());
        userInfo.put("accessToken", user.getAccessToken());

        response.put("success", true);
        response.put("message", "로그인 성공");
        response.put("user", userInfo);
        return response;
    }
}