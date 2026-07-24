package com.example.todaydeparture.service;

import com.example.todaydeparture.dto.*;
import com.example.todaydeparture.mapper.UserMapper;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
public class AuthService {

    private final UserMapper userMapper;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AuthService(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    public AuthResponse register(RegisterRequest req) {
        if (userMapper.countByEmail(req.getEmail()) > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다");
        }
        UserDto user = new UserDto();
        user.setEmail(req.getEmail());
        user.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        user.setNickname(req.getNickname());
        user.setAccessToken(UUID.randomUUID().toString());

        userMapper.insert(user);
        return new AuthResponse(user.getAccessToken(), user.getId(), user.getNickname());
    }

    public AuthResponse login(LoginRequest req) {
        UserDto user = userMapper.findByEmail(req.getEmail());
        if (user == null || !passwordEncoder.matches(req.getPassword(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다");
        }
        String token = UUID.randomUUID().toString();
        userMapper.updateToken(user.getId(), token);
        return new AuthResponse(token, user.getId(), user.getNickname());
    }
}