package com.example.todaydeparture.mapper;

import com.example.todaydeparture.dto.PurposeBufferPolicyDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PurposeMapper {

    List<PurposeBufferPolicyDto> findAllActive();

    PurposeBufferPolicyDto findByPurposeCode(@Param("purposeCode") String purposeCode);
}