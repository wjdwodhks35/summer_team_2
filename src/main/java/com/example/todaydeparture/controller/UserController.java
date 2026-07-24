package com.example.todaydeparture.controller;

import com.example.todaydeparture.mapper.UserMapper;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

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
        Map<String, Object> existingUser = userMapper.findByEmail((String) params.get("email"));
        if (existingUser != null) {
            response.put("success", false);
            response.put("message", "이미 가입된 이메일입니다.");
            return response;
        }

        // 비밀번호 매핑 (실제 운영 시에는 BCrypt 암호화 필요)
        params.put("passwordHash", params.get("password"));
        if (!params.containsKey("preferredTransport")) {
            params.put("preferredTransport", "public_transport");
        }

        int result = userMapper.insertUser(params);
        response.put("success", result > 0);
        response.put("message", result > 0 ? "회원가입이 완료되었습니다." : "가입 실패");
        return response;
    }

    // 2. 로그인 API
    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Map<String, Object> params) {
        Map<String, Object> response = new HashMap<>();
        String email = (String) params.get("email");
        String password = (String) params.get("password");

        Map<String, Object> user = userMapper.findByEmail(email);
        
        if (user == null || !password.equals(user.get("passwordHash"))) {
            response.put("success", false);
            response.put("message", "이메일 또는 비밀번호가 일치하지 않습니다.");
            return response;
        }

        response.put("success", true);
        response.put("message", "로그인 성공");
        response.put("user", user); // 사용자 정보 전달
        return response;
    }
}