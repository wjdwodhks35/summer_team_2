package com.example.todaydeparture.controller;

import com.example.todaydeparture.dto.PurposeBufferPolicyDto;
import com.example.todaydeparture.service.PurposeService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/purposes")
public class PurposeController {

    private final PurposeService purposeService;

    public PurposeController(PurposeService purposeService) {
        this.purposeService = purposeService;
    }

    @GetMapping
    public List<PurposeBufferPolicyDto> getPurposes() {
        return purposeService.getActivePurposes();
    }

    @GetMapping("/{purposeCode}")
    public PurposeBufferPolicyDto getPurpose(@PathVariable("purposeCode") String purposeCode) {
        return purposeService.getPurpose(purposeCode);
    }
}