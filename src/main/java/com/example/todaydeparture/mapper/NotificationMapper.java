package com.example.todaydeparture.mapper;

import com.example.todaydeparture.dto.NotificationDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface NotificationMapper {

    List<NotificationDto> findAll();

    NotificationDto findById(@Param("notificationId") Long notificationId);

    int markAsRead(@Param("notificationId") Long notificationId);
}