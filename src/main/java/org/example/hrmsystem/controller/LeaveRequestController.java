package org.example.hrmsystem.controller;

import jakarta.validation.Valid;
import org.example.hrmsystem.dto.LeaveRequestCreateDto;
import org.example.hrmsystem.dto.LeaveRequestResponse;
import org.example.hrmsystem.security.AppUserDetails;
import org.example.hrmsystem.service.LeaveRequestService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/leave")
public class LeaveRequestController {

    private final LeaveRequestService leaveRequestService;

    public LeaveRequestController(LeaveRequestService leaveRequestService) {
        this.leaveRequestService = leaveRequestService;
    }

    @PostMapping
    public ResponseEntity<?> create(
            @AuthenticationPrincipal AppUserDetails userDetails,
            @Valid @RequestBody LeaveRequestCreateDto dto
    ) {
        try {
            Long employeeId = resolveEmployeeId(userDetails);
            LeaveRequestResponse response = leaveRequestService.create(employeeId, dto);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
    }

    @GetMapping("/my")
    public ResponseEntity<Map<String, Object>> myLeaves(
            @AuthenticationPrincipal AppUserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Long employeeId = resolveEmployeeId(userDetails);
        return ResponseEntity.ok(leaveRequestService.getMyLeaves(employeeId, page, size));
    }

    private Long resolveEmployeeId(AppUserDetails userDetails) {
        Long employeeId = userDetails.getEmployeeId();
        if (employeeId == null) {
            employeeId = userDetails.getUserId();
        }
        return employeeId;
    }
}
