package com.example.todaydeparture.controller;

import com.example.todaydeparture.mapper.UserMapper;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

    @PostMapping("/signup")
    public Map<String, Object> signup(@RequestBody Map<String, Object> params) {
        Map<String, Object> response = new HashMap<>();
        String email = (String) params.get("email");

        if (userMapper.findUserMapByEmail(email) != null) {
            response.put("success", false);
            response.put("message", "이미 가입된 이메일입니다.");
            return response;
        }

        params.put("passwordHash", params.get("password"));
        params.putIfAbsent("preferredTransport", "public_transport");

        int result = userMapper.insertUser(params);
        response.put("success", result > 0);
        response.put("message", result > 0 ? "회원가입이 완료되었습니다." : "가입 실패");
        return response;
    }

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Map<String, Object> params) {
        Map<String, Object> response = new HashMap<>();
        String email = (String) params.get("email");
        String password = (String) params.get("password");

        Map<String, Object> user = userMapper.findUserMapByEmail(email);
        if (user == null || !password.equals(user.get("passwordHash"))) {
            response.put("success", false);
            response.put("message", "이메일 또는 비밀번호가 일치하지 않습니다.");
            return response;
        }

        String accessToken = UUID.randomUUID().toString();
        Number userId = (Number) user.get("id");
        userMapper.updateToken(userId.longValue(), accessToken);
        user.put("accessToken", accessToken);

        response.put("success", true);
        response.put("message", "로그인 성공");
        response.put("user", user);
        return response;
    }
}
