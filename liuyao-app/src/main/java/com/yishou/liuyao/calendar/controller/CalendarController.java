package com.yishou.liuyao.calendar.controller;

import com.yishou.liuyao.calendar.dto.VerificationEventDTO;
import com.yishou.liuyao.calendar.dto.VerificationEventPageDTO;
import com.yishou.liuyao.calendar.dto.VerificationFeedbackSubmitRequest;
import com.yishou.liuyao.calendar.service.VerificationEventService;
import com.yishou.liuyao.common.dto.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/calendar")
public class CalendarController {

    private final VerificationEventService verificationEventService;

    public CalendarController(VerificationEventService verificationEventService) {
        this.verificationEventService = verificationEventService;
    }

    @GetMapping("/events")
    public ApiResponse<VerificationEventPageDTO> listMonthlyEvents(@RequestParam(required = false) Long userId,
                                                                   @RequestParam int year,
                                                                   @RequestParam int month,
                                                                   @RequestParam(defaultValue = "1") int page,
                                                                   @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success(verificationEventService.listMonthlyEvents(userId, year, month, page, size));
    }

    @PostMapping("/events/{eventId}/feedback")
    public ApiResponse<VerificationEventDTO> submitFeedback(@PathVariable UUID eventId,
                                                            @RequestBody VerificationFeedbackSubmitRequest request) {
        return ApiResponse.success(verificationEventService.submitFeedback(eventId, request));
    }

    @GetMapping("/timeline")
    public ApiResponse<VerificationEventPageDTO> listTimeline(@RequestParam(required = false) Long userId,
                                                              @RequestParam(defaultValue = "1") int page,
                                                              @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success(verificationEventService.listTimeline(userId, page, size));
    }
}
