package com.npu.lms.service;

import com.npu.lms.dto.*;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AnalysisExportService {

    // 注入你现有的数据服务
    @Autowired
    private RecordService recordService;

    @Autowired
    private BookService bookService;

    // --- 1. 热门图书 ---
    public ByteArrayInputStream exportPopularBooks() throws IOException {
        List<PopularBookDTO> data = recordService.getTopPopularBooks();
        List<String> headers = List.of("图书标题", "借阅次数");
        List<List<Object>> rows = data.stream()
                .map(dto -> List.of(dto.getTitle(), (Object)dto.getBorrowCount()))
                .collect(Collectors.toList());
        return createExcel("热门图书报表", headers, rows);
    }

    // --- 2. 活跃读者 ---
    public ByteArrayInputStream exportActiveUsers() throws IOException {
        List<ActiveUserDTO> data = recordService.getTopActiveUsers();
        List<String> headers = List.of("读者姓名", "借阅次数");
        List<List<Object>> rows = data.stream()
                .map(dto -> List.of(dto.getName(), (Object)dto.getBorrowCount()))
                .collect(Collectors.toList());
        return createExcel("活跃读者报表", headers, rows);
    }

    // --- 3. 高峰时段 ---
    public ByteArrayInputStream exportPeakTimes() throws IOException {
        List<PeakTimeDTO> data = recordService.getPeakBorrowingTimes();
        List<String> headers = List.of("时间段", "借阅次数");
        List<List<Object>> rows = data.stream()
                .map(dto -> List.of(dto.getHourSlot(), (Object)dto.getCount()))
                .collect(Collectors.toList());
        return createExcel("高峰时段报表", headers, rows);
    }

    // --- 4. 图书分类 ---
    public ByteArrayInputStream exportCategoryStats() throws IOException {
        List<CategoryStatsDTO> data = bookService.getBookCategoryStats();
        List<String> headers = List.of("图书分类", "数量");
        List<List<Object>> rows = data.stream()
                .map(dto -> List.of(dto.getCategory(), (Object)dto.getCount()))
                .collect(Collectors.toList());
        return createExcel("图书分类报表", headers, rows);
    }

    // --- 5. 逾期读者 ---
    public ByteArrayInputStream exportOverdueUsers() throws IOException {
        List<OverdueUserDTO> data = recordService.getOverdueUsers();
        List<String> headers = List.of("读者姓名", "逾期数量");
        List<List<Object>> rows = data.stream()
                .map(dto -> List.of(dto.getName(), (Object)dto.getOverdueCount()))
                .collect(Collectors.toList());
        return createExcel("逾期读者报表", headers, rows);
    }

    // --- 6. 滞销图书 ---
    public ByteArrayInputStream exportStagnantBooks() throws IOException {
        List<StagnantBookDTO> data = bookService.getStagnantBooks();
        List<String> headers = List.of("图书标题", "分类", "最后借阅时间");
        List<List<Object>> rows = data.stream()
                .map(dto -> List.of(dto.getTitle(), dto.getCategory(), (Object)dto.getLastBorrowTime()))
                .collect(Collectors.toList());
        return createExcel("滞销图书报表", headers, rows);
    }


    // --- 通用 Excel 生成器 ---
    // (这是你 ExcelExportService 中逻辑的通用版本)
    private ByteArrayInputStream createExcel(String sheetName, List<String> headers, List<List<Object>> rows) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet(sheetName);

            // 1. 创建表头行
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.size(); i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers.get(i));
            }

            // 2. 填充数据行
            int rowIdx = 1;
            for (List<Object> rowData : rows) {
                Row row = sheet.createRow(rowIdx++);
                for (int i = 0; i < rowData.size(); i++) {
                    Cell cell = row.createCell(i);
                    Object value = rowData.get(i);

                    // 设置单元格的值 (做了 null 检查)
                    if (value instanceof String) {
                        cell.setCellValue((String) value);
                    } else if (value instanceof Number) {
                        cell.setCellValue(((Number) value).doubleValue());
                    } else if (value != null) {
                        cell.setCellValue(value.toString());
                    } else {
                        cell.setCellValue(""); // null 值设置为空字符串
                    }
                }
            }

            // 3. 自动调整列宽 (可选, 但很实用)
            for (int i = 0; i < headers.size(); i++) {
                sheet.autoSizeColumn(i);
            }

            // 4. 将工作簿写入字节流
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        }
    }
}