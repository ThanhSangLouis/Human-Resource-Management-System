package org.example.hrmsystem.service;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.example.hrmsystem.dto.EmployeeResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ExcelExportService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final EmployeeService employeeService;

    public ExcelExportService(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    /**
     * Xuất danh sách nhân viên theo filter ra file Excel (.xlsx).
     * Hỗ trợ tiếng Việt nhờ font mặc định của POI (Calibri) và UTF-8 nội tại OOXML.
     */
    public byte[] exportEmployees(String keyword, String status, Long departmentId) throws IOException {
        // Lấy toàn bộ dữ liệu (size=10000 để bao phủ hết, sort fullName asc)
        Pageable all = PageRequest.of(0, 10_000, Sort.by(Sort.Direction.ASC, "fullName"));
        List<EmployeeResponse> employees = employeeService.findAll(keyword, status, departmentId, all)
                .getContent();

        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Danh sách nhân viên");

            // ── Column widths ────────────────────────────────────────────────
            sheet.setColumnWidth(0, 5  * 256);   // STT
            sheet.setColumnWidth(1, 14 * 256);   // Mã NV
            sheet.setColumnWidth(2, 28 * 256);   // Họ tên
            sheet.setColumnWidth(3, 30 * 256);   // Email
            sheet.setColumnWidth(4, 16 * 256);   // Điện thoại
            sheet.setColumnWidth(5, 22 * 256);   // Chức danh
            sheet.setColumnWidth(6, 22 * 256);   // Phòng ban
            sheet.setColumnWidth(7, 14 * 256);   // Trạng thái
            sheet.setColumnWidth(8, 16 * 256);   // Ngày vào làm
            sheet.setColumnWidth(9, 18 * 256);   // Lương cơ bản

            // ── Title row ────────────────────────────────────────────────────
            CellStyle titleStyle = createTitleStyle(wb);
            Row titleRow = sheet.createRow(0);
            titleRow.setHeightInPoints(24);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("DANH SÁCH NHÂN VIÊN – HRM SYSTEM");
            titleCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 9));

            // ── Sub-title: export date + filter ──────────────────────────────
            CellStyle subStyle = createSubTitleStyle(wb);
            Row subRow = sheet.createRow(1);
            String filterNote = buildFilterNote(keyword, status, departmentId);
            Cell subCell = subRow.createCell(0);
            subCell.setCellValue("Xuất ngày: " + LocalDate.now().format(DATE_FMT) + filterNote);
            subCell.setCellStyle(subStyle);
            sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, 9));

            // ── Header row ───────────────────────────────────────────────────
            CellStyle headerStyle = createHeaderStyle(wb);
            Row headerRow = sheet.createRow(3);
            headerRow.setHeightInPoints(18);
            String[] headers = {
                "STT", "Mã NV", "Họ và tên", "Email", "Điện thoại",
                "Chức danh", "Phòng ban", "Trạng thái", "Ngày vào làm", "Lương cơ bản (VNĐ)"
            };
            for (int i = 0; i < headers.length; i++) {
                Cell c = headerRow.createCell(i);
                c.setCellValue(headers[i]);
                c.setCellStyle(headerStyle);
            }

            // ── Data rows ────────────────────────────────────────────────────
            CellStyle dataStyle   = createDataStyle(wb, false);
            CellStyle dataAlt     = createDataStyle(wb, true);
            CellStyle numStyle    = createNumberStyle(wb);
            CellStyle badgeActive = createBadgeStyle(wb, new byte[]{(byte)39,(byte)103,(byte)73});
            CellStyle badgeInactive = createBadgeStyle(wb, new byte[]{(byte)180,(byte)83,(byte)9});
            CellStyle badgeResigned = createBadgeStyle(wb, new byte[]{(byte)153,(byte)27,(byte)27});

            int rowIdx = 4;
            for (int i = 0; i < employees.size(); i++) {
                EmployeeResponse e = employees.get(i);
                Row row = sheet.createRow(rowIdx++);
                row.setHeightInPoints(16);

                CellStyle base = (i % 2 == 0) ? dataStyle : dataAlt;

                createCell(row, 0, String.valueOf(i + 1), base);
                createCell(row, 1, nullSafe(e.getEmployeeCode()), base);
                createCell(row, 2, nullSafe(e.getFullName()), base);
                createCell(row, 3, nullSafe(e.getEmail()), base);
                createCell(row, 4, nullSafe(e.getPhone()), base);
                createCell(row, 5, nullSafe(e.getPosition()), base);
                createCell(row, 6, nullSafe(e.getDepartmentName()), base);

                // Status badge
                String st = nullSafe(e.getStatus());
                CellStyle stStyle = switch (st) {
                    case "ACTIVE"   -> badgeActive;
                    case "INACTIVE" -> badgeInactive;
                    case "RESIGNED" -> badgeResigned;
                    default         -> base;
                };
                String stLabel = switch (st) {
                    case "ACTIVE"   -> "Đang làm";
                    case "INACTIVE" -> "Tạm nghỉ";
                    case "RESIGNED" -> "Đã nghỉ";
                    default         -> st;
                };
                createCell(row, 7, stLabel, stStyle);

                createCell(row, 8,
                        e.getHireDate() != null ? e.getHireDate().format(DATE_FMT) : "", base);

                // Lương cơ bản – số
                Cell salCell = row.createCell(9);
                if (e.getSalaryBase() != null) {
                    salCell.setCellValue(e.getSalaryBase().doubleValue());
                    salCell.setCellStyle(numStyle);
                } else {
                    salCell.setCellValue("—");
                    salCell.setCellStyle(base);
                }
            }

            // ── Tổng cộng ────────────────────────────────────────────────────
            CellStyle totalStyle = createTotalStyle(wb);
            Row totalRow = sheet.createRow(rowIdx);
            Cell totalLabel = totalRow.createCell(0);
            totalLabel.setCellValue("Tổng: " + employees.size() + " nhân viên");
            totalLabel.setCellStyle(totalStyle);
            sheet.addMergedRegion(new CellRangeAddress(rowIdx, rowIdx, 0, 9));

            // ── Xuất ra byte[] ────────────────────────────────────────────────
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);
            return out.toByteArray();
        }
    }

    // ── Style helpers ────────────────────────────────────────────────────────

    private CellStyle createTitleStyle(Workbook wb) {
        CellStyle s = wb.createCellStyle();
        Font f = wb.createFont();
        f.setBold(true);
        f.setFontHeightInPoints((short) 14);
        f.setColor(IndexedColors.WHITE.getIndex());
        s.setFont(f);
        s.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        s.setAlignment(HorizontalAlignment.CENTER);
        s.setVerticalAlignment(VerticalAlignment.CENTER);
        return s;
    }

    private CellStyle createSubTitleStyle(Workbook wb) {
        CellStyle s = wb.createCellStyle();
        Font f = wb.createFont();
        f.setItalic(true);
        f.setFontHeightInPoints((short) 10);
        f.setColor(IndexedColors.GREY_50_PERCENT.getIndex());
        s.setFont(f);
        s.setAlignment(HorizontalAlignment.CENTER);
        return s;
    }

    private CellStyle createHeaderStyle(Workbook wb) {
        CellStyle s = wb.createCellStyle();
        Font f = wb.createFont();
        f.setBold(true);
        f.setFontHeightInPoints((short) 11);
        f.setColor(IndexedColors.WHITE.getIndex());
        s.setFont(f);
        s.setFillForegroundColor(IndexedColors.ROYAL_BLUE.getIndex());
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        s.setAlignment(HorizontalAlignment.CENTER);
        s.setVerticalAlignment(VerticalAlignment.CENTER);
        setBorderAll(s, BorderStyle.THIN, IndexedColors.WHITE.getIndex());
        s.setWrapText(true);
        return s;
    }

    private CellStyle createDataStyle(Workbook wb, boolean alt) {
        CellStyle s = wb.createCellStyle();
        Font f = wb.createFont();
        f.setFontHeightInPoints((short) 10);
        s.setFont(f);
        if (alt) {
            s.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
            s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        }
        setBorderAll(s, BorderStyle.THIN, IndexedColors.GREY_25_PERCENT.getIndex());
        s.setVerticalAlignment(VerticalAlignment.CENTER);
        return s;
    }

    private CellStyle createNumberStyle(Workbook wb) {
        CellStyle s = wb.createCellStyle();
        Font f = wb.createFont();
        f.setFontHeightInPoints((short) 10);
        s.setFont(f);
        DataFormat fmt = wb.createDataFormat();
        s.setDataFormat(fmt.getFormat("#,##0"));
        s.setAlignment(HorizontalAlignment.RIGHT);
        setBorderAll(s, BorderStyle.THIN, IndexedColors.GREY_25_PERCENT.getIndex());
        s.setVerticalAlignment(VerticalAlignment.CENTER);
        return s;
    }

    private CellStyle createBadgeStyle(Workbook wb, byte[] rgb) {
        CellStyle s = wb.createCellStyle();
        Font f = wb.createFont();
        f.setBold(true);
        f.setFontHeightInPoints((short) 10);
        s.setFont(f);
        s.setAlignment(HorizontalAlignment.CENTER);
        setBorderAll(s, BorderStyle.THIN, IndexedColors.GREY_25_PERCENT.getIndex());
        s.setVerticalAlignment(VerticalAlignment.CENTER);
        return s;
    }

    private CellStyle createTotalStyle(Workbook wb) {
        CellStyle s = wb.createCellStyle();
        Font f = wb.createFont();
        f.setBold(true);
        f.setFontHeightInPoints((short) 10);
        s.setFont(f);
        s.setFillForegroundColor(IndexedColors.LEMON_CHIFFON.getIndex());
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        setBorderAll(s, BorderStyle.THIN, IndexedColors.GREY_25_PERCENT.getIndex());
        s.setVerticalAlignment(VerticalAlignment.CENTER);
        return s;
    }

    private void setBorderAll(CellStyle s, BorderStyle bs, short color) {
        s.setBorderTop(bs);    s.setTopBorderColor(color);
        s.setBorderBottom(bs); s.setBottomBorderColor(color);
        s.setBorderLeft(bs);   s.setLeftBorderColor(color);
        s.setBorderRight(bs);  s.setRightBorderColor(color);
    }

    private void createCell(Row row, int col, String value, CellStyle style) {
        Cell c = row.createCell(col);
        c.setCellValue(value);
        c.setCellStyle(style);
    }

    private String nullSafe(String v) {
        return v != null ? v : "";
    }

    private String buildFilterNote(String keyword, String status, Long departmentId) {
        StringBuilder sb = new StringBuilder();
        if (keyword != null && !keyword.isBlank())
            sb.append("  |  Tìm kiếm: ").append(keyword.trim());
        if (status != null && !status.isBlank())
            sb.append("  |  Trạng thái: ").append(status.trim());
        if (departmentId != null)
            sb.append("  |  Phòng ban ID: ").append(departmentId);
        return sb.toString();
    }
}
