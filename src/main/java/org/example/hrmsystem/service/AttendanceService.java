package org.example.hrmsystem.service;

import org.example.hrmsystem.dto.AttendanceResponse;
import org.example.hrmsystem.model.Attendance;
import org.example.hrmsystem.model.AttendanceStatus;
import org.example.hrmsystem.repository.AttendanceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

@Service
public class AttendanceService {

    private static final Logger log = LoggerFactory.getLogger(AttendanceService.class);

    // Span-based shift policy: 09:00 – 17:00 (8h span, lunch included)
    private static final LocalTime SHIFT_START    = LocalTime.of(9, 0);   // check-in deadline = late threshold
    private static final LocalTime SHIFT_END      = LocalTime.of(17, 0);  // OT starts after this
    private static final double    HALF_DAY_SPAN  = 4.0;                  // span < 4h → HALF_DAY

    private final AttendanceRepository attendanceRepository;

    public AttendanceService(AttendanceRepository attendanceRepository) {
        this.attendanceRepository = attendanceRepository;
    }

    @Transactional
    public AttendanceResponse checkIn(Long employeeId, String note) {
        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();

        Optional<Attendance> existing = attendanceRepository.findByEmployeeIdAndAttendanceDate(employeeId, today);
        if (existing.isPresent()) {
            Attendance att = existing.get();
            if (att.getCheckIn() != null) {
                throw new IllegalStateException("Already checked in today at " + att.getCheckIn().toLocalTime());
            }
        }

        Attendance attendance = existing.orElse(new Attendance());
        attendance.setEmployeeId(employeeId);
        attendance.setAttendanceDate(today);
        attendance.setCheckIn(now);
        attendance.setStatus(
            now.toLocalTime().isAfter(SHIFT_START) ? AttendanceStatus.LATE : AttendanceStatus.PRESENT
        );
        if (note != null && !note.isBlank()) {
            attendance.setNote(note.trim());
        }

        Attendance saved = attendanceRepository.save(attendance);
        log.info("Employee {} checked in at {}", employeeId, now);

        return toResponse(saved, "Check-in successful");
    }

    @Transactional
    public AttendanceResponse checkOut(Long employeeId, String note) {
        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();

        Attendance attendance = attendanceRepository
            .findByEmployeeIdAndAttendanceDate(employeeId, today)
            .orElseThrow(() -> new IllegalStateException("No check-in record found for today. Please check in first."));

        if (attendance.getCheckIn() == null) {
            throw new IllegalStateException("Check-in record is missing. Please check in first.");
        }
        if (attendance.getCheckOut() != null) {
            throw new IllegalStateException("Already checked out today at " + attendance.getCheckOut().toLocalTime());
        }

        attendance.setCheckOut(now);

        // work_hours = raw span check-in → check-out (lunch included in span, not deducted)
        long minutesSpan = ChronoUnit.MINUTES.between(attendance.getCheckIn(), now);
        double hoursSpan = minutesSpan / 60.0;

        // overtime = time after max(checkIn, SHIFT_END) — prevents counting OT before employee arrived
        LocalDateTime shiftEndToday = now.toLocalDate().atTime(SHIFT_END);
        LocalDateTime otStart = attendance.getCheckIn().isAfter(shiftEndToday)
            ? attendance.getCheckIn()
            : shiftEndToday;
        long overtimeMinutes = now.isAfter(otStart)
            ? ChronoUnit.MINUTES.between(otStart, now)
            : 0L;
        double overtime = overtimeMinutes / 60.0;

        attendance.setWorkHours(BigDecimal.valueOf(hoursSpan).setScale(2, RoundingMode.HALF_UP));
        attendance.setOvertimeHours(BigDecimal.valueOf(overtime).setScale(2, RoundingMode.HALF_UP));

        // Recalculate status based on span and check-in time
        if (hoursSpan < HALF_DAY_SPAN) {
            attendance.setStatus(AttendanceStatus.HALF_DAY);
        } else if (attendance.getCheckIn().toLocalTime().isAfter(SHIFT_START)) {
            attendance.setStatus(AttendanceStatus.LATE);
        } else {
            attendance.setStatus(AttendanceStatus.PRESENT);
        }

        if (note != null && !note.isBlank()) {
            String existingNote = attendance.getNote();
            attendance.setNote(
                existingNote != null ? existingNote + " | Checkout: " + note.trim() : note.trim()
            );
        }

        Attendance saved = attendanceRepository.save(attendance);
        log.info("Employee {} checked out at {}, worked {}h", employeeId, now,
            saved.getWorkHours());

        return toResponse(saved, "Check-out successful. Span: " +
            saved.getWorkHours() + "h" +
            (overtime > 0 ? ", overtime: " + saved.getOvertimeHours() + "h" : ""));
    }

    public AttendanceResponse getTodayStatus(Long employeeId) {
        LocalDate today = LocalDate.now();
        Optional<Attendance> opt = attendanceRepository.findByEmployeeIdAndAttendanceDate(employeeId, today);
        if (opt.isEmpty()) {
            AttendanceResponse empty = new AttendanceResponse(
                null, employeeId, today, null, null,
                BigDecimal.ZERO, BigDecimal.ZERO, null, null, "No attendance record for today"
            );
            return empty;
        }
        return toResponse(opt.get(), null);
    }

    private AttendanceResponse toResponse(Attendance a, String message) {
        return new AttendanceResponse(
            a.getId(),
            a.getEmployeeId(),
            a.getAttendanceDate(),
            a.getCheckIn(),
            a.getCheckOut(),
            a.getWorkHours(),
            a.getOvertimeHours(),
            a.getStatus(),
            a.getNote(),
            message
        );
    }
}
