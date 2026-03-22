package org.example.hrmsystem.service;

import org.example.hrmsystem.dto.LeaveRequestCreateDto;
import org.example.hrmsystem.dto.LeaveRequestResponse;
import org.example.hrmsystem.model.Employee;
import org.example.hrmsystem.model.LeaveRequest;
import org.example.hrmsystem.model.LeaveStatus;
import org.example.hrmsystem.model.LeaveType;
import org.example.hrmsystem.repository.EmployeeRepository;
import org.example.hrmsystem.repository.LeaveRequestRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
public class LeaveRequestService {

    private static final Logger log = LoggerFactory.getLogger(LeaveRequestService.class);

    private final LeaveRequestRepository leaveRequestRepository;
    private final EmployeeRepository employeeRepository;

    public LeaveRequestService(
            LeaveRequestRepository leaveRequestRepository,
            EmployeeRepository employeeRepository
    ) {
        this.leaveRequestRepository = leaveRequestRepository;
        this.employeeRepository = employeeRepository;
    }

    @Transactional
    public LeaveRequestResponse create(Long employeeId, LeaveRequestCreateDto dto) {
        LocalDate today = LocalDate.now();

        // Validate leave type
        LeaveType leaveType;
        try {
            leaveType = LeaveType.valueOf(dto.getLeaveType().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid leave type: " + dto.getLeaveType());
        }

        // Validate dates
        if (dto.getStartDate().isBefore(today)) {
            throw new IllegalArgumentException("Start date cannot be in the past");
        }
        if (dto.getEndDate().isBefore(dto.getStartDate())) {
            throw new IllegalArgumentException("End date must be on or after start date");
        }

        // Check for overlapping leave requests (PENDING or APPROVED)
        boolean hasOverlap = leaveRequestRepository
            .existsByEmployeeIdAndStatusInAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                employeeId,
                List.of(LeaveStatus.PENDING, LeaveStatus.APPROVED),
                dto.getEndDate(),
                dto.getStartDate()
            );
        if (hasOverlap) {
            throw new IllegalArgumentException(
                "You already have a pending or approved leave request that overlaps with these dates"
            );
        }

        // Count working days inclusive (Mon–Sat), exclude Sunday only
        int totalDays = countWorkingDays(dto.getStartDate(), dto.getEndDate());

        LeaveRequest request = new LeaveRequest();
        request.setEmployeeId(employeeId);
        request.setLeaveType(leaveType);
        request.setStartDate(dto.getStartDate());
        request.setEndDate(dto.getEndDate());
        request.setTotalDays(totalDays);
        request.setReason(dto.getReason() != null ? dto.getReason().trim() : null);
        request.setStatus(LeaveStatus.PENDING);

        LeaveRequest saved = leaveRequestRepository.save(request);
        log.info("Employee {} submitted leave request id={} ({} to {}, {} days)",
            employeeId, saved.getId(), saved.getStartDate(), saved.getEndDate(), saved.getTotalDays());

        String employeeName = resolveEmployeeName(employeeId);
        return toResponse(saved, employeeName, "Leave request submitted successfully");
    }

    public Map<String, Object> getMyLeaves(Long employeeId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<LeaveRequest> pageResult = leaveRequestRepository
            .findByEmployeeIdOrderByCreatedAtDesc(employeeId, pageable);

        String employeeName = resolveEmployeeName(employeeId);

        List<LeaveRequestResponse> content = pageResult.getContent().stream()
            .map(lr -> toResponse(lr, employeeName, null))
            .toList();

        return Map.of(
            "content", content,
            "page", pageResult.getNumber(),
            "size", pageResult.getSize(),
            "totalElements", pageResult.getTotalElements(),
            "totalPages", pageResult.getTotalPages(),
            "last", pageResult.isLast()
        );
    }

    private String resolveEmployeeName(Long employeeId) {
        return employeeRepository.findById(employeeId)
            .map(Employee::getFullName)
            .orElse("Employee #" + employeeId);
    }

    // Count Mon–Sat, skip Sunday only
    private int countWorkingDays(LocalDate start, LocalDate end) {
        int count = 0;
        LocalDate current = start;
        while (!current.isAfter(end)) {
            if (current.getDayOfWeek() != DayOfWeek.SUNDAY) {
                count++;
            }
            current = current.plusDays(1);
        }
        return count;
    }

    private LeaveRequestResponse toResponse(LeaveRequest lr, String employeeName, String message) {
        return new LeaveRequestResponse(
            lr.getId(),
            lr.getEmployeeId(),
            employeeName,
            lr.getLeaveType(),
            lr.getStartDate(),
            lr.getEndDate(),
            lr.getTotalDays(),
            lr.getReason(),
            lr.getStatus(),
            lr.getApprovedAt(),
            lr.getCreatedAt(),
            message
        );
    }
}
