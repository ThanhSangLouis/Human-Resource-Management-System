package org.example.hrmsystem.controller;

import org.example.hrmsystem.dto.AttendanceResponse;
import org.example.hrmsystem.security.AppUserDetails;
import org.example.hrmsystem.service.AttendanceService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/attendance")
public class AttendanceController {

    private final AttendanceService attendanceService;

    public AttendanceController(AttendanceService attendanceService) {
        this.attendanceService = attendanceService;
    }

    @PostMapping("/check-in")
    public ResponseEntity<?> checkIn(
            @AuthenticationPrincipal AppUserDetails userDetails,
            @RequestParam(required = false) String note
    ) {
        try {
            Long employeeId = resolveEmployeeId(userDetails);
            AttendanceResponse response = attendanceService.checkIn(employeeId, note);
            return ResponseEntity.ok(response);
        } catch (IllegalStateException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
    }

    @PostMapping("/check-out")
    public ResponseEntity<?> checkOut(
            @AuthenticationPrincipal AppUserDetails userDetails,
            @RequestParam(required = false) String note
    ) {
        try {
            Long employeeId = resolveEmployeeId(userDetails);
            AttendanceResponse response = attendanceService.checkOut(employeeId, note);
            return ResponseEntity.ok(response);
        } catch (IllegalStateException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
    }

    @GetMapping("/today")
    public ResponseEntity<AttendanceResponse> todayStatus(
            @AuthenticationPrincipal AppUserDetails userDetails
    ) {
        Long employeeId = resolveEmployeeId(userDetails);
        return ResponseEntity.ok(attendanceService.getTodayStatus(employeeId));
    }

    private Long resolveEmployeeId(AppUserDetails userDetails) {
        Long employeeId = userDetails.getEmployeeId();
        if (employeeId == null) {
            // Fallback: use userId as employeeId for admin/hr accounts without employee profile
            employeeId = userDetails.getUserId();
        }
        return employeeId;
    }
}
