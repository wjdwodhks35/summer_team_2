package com.example.todaydeparture.service;

import com.example.todaydeparture.dto.NotificationDto;
import com.example.todaydeparture.mapper.NotificationMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NotificationService {

    private final NotificationMapper notificationMapper;

    public NotificationService(NotificationMapper notificationMapper) {
        this.notificationMapper = notificationMapper;
    }

    public List<NotificationDto> getNotifications() {
        return notificationMapper.findAll();
    }

    public NotificationDto getNotification(Long notificationId) {
        return notificationMapper.findById(notificationId);
    }

    public void readNotification(Long notificationId) {
        notificationMapper.markAsRead(notificationId);
    }
}