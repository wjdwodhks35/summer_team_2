package com.example.todaydeparture.controller;

import com.example.todaydeparture.dto.NotificationDto;
import com.example.todaydeparture.service.NotificationService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public List<NotificationDto> getNotifications() {
        return notificationService.getNotifications();
    }

    @GetMapping("/{notificationId}")
    public NotificationDto getNotification(@PathVariable("notificationId") Long notificationId) {
        return notificationService.getNotification(notificationId);
    }

    @PatchMapping("/{notificationId}/read")
    public String readNotification(@PathVariable("notificationId") Long notificationId) {
        notificationService.readNotification(notificationId);
        return "알림을 읽음 처리했습니다.";
    }
}