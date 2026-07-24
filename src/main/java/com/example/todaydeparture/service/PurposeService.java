package com.example.todaydeparture.service;

import com.example.todaydeparture.dto.PurposeBufferPolicyDto;
import com.example.todaydeparture.mapper.PurposeMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PurposeService {

    private final PurposeMapper purposeMapper;

    public PurposeService(PurposeMapper purposeMapper) {
        this.purposeMapper = purposeMapper;
    }

    public List<PurposeBufferPolicyDto> getActivePurposes() {
        return purposeMapper.findAllActive();
    }

    public PurposeBufferPolicyDto getPurpose(String purposeCode) {
        return purposeMapper.findByPurposeCode(purposeCode);
    }
}