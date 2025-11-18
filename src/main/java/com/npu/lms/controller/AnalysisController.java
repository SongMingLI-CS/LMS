package com.npu.lms.controller;

import com.npu.lms.service.AnalysisExportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.ByteArrayInputStream;
import java.io.IOException;

@RestController
@RequestMapping("/api/analysis/export") // 导出的 API 基础路径
@PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_SUPERADMIN')") // 整个模块仅限管理员
public class AnalysisController {

    @Autowired
    private AnalysisExportService exportService;

    // 辅助方法，用于构建文件下载的响应
    private ResponseEntity<InputStreamResource> createExcelResponse(ByteArrayInputStream in, String filename) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "attachment; filename=" + filename);

        return ResponseEntity
                .ok()
                .headers(headers)
                .contentType(MediaType.APPLICATION_OCTET_STREAM) // 强制下载
                .body(new InputStreamResource(in));
    }

    @GetMapping("/popular-books")
    public ResponseEntity<InputStreamResource> exportPopularBooks() throws IOException {
        ByteArrayInputStream in = exportService.exportPopularBooks();
        return createExcelResponse(in, "popular_books_report.xlsx");
    }

    @GetMapping("/active-users")
    public ResponseEntity<InputStreamResource> exportActiveUsers() throws IOException {
        ByteArrayInputStream in = exportService.exportActiveUsers();
        return createExcelResponse(in, "active_users_report.xlsx");
    }

    @GetMapping("/peak-times")
    public ResponseEntity<InputStreamResource> exportPeakTimes() throws IOException {
        ByteArrayInputStream in = exportService.exportPeakTimes();
        return createExcelResponse(in, "peak_times_report.xlsx");
    }

    @GetMapping("/categories")
    public ResponseEntity<InputStreamResource> exportCategoryStats() throws IOException {
        ByteArrayInputStream in = exportService.exportCategoryStats();
        return createExcelResponse(in, "category_stats_report.xlsx");
    }

    @GetMapping("/overdue-users")
    public ResponseEntity<InputStreamResource> exportOverdueUsers() throws IOException {
        ByteArrayInputStream in = exportService.exportOverdueUsers();
        return createExcelResponse(in, "overdue_users_report.xlsx");
    }

    @GetMapping("/stagnant-books")
    public ResponseEntity<InputStreamResource> exportStagnantBooks() throws IOException {
        ByteArrayInputStream in = exportService.exportStagnantBooks();
        return createExcelResponse(in, "stagnant_books_report.xlsx");
    }
}