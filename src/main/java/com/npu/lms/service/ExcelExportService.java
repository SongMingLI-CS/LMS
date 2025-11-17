package com.npu.lms.service;

import com.npu.lms.dto.RecordDTO;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

@Service
public class ExcelExportService {

    /**
     * 将借阅记录列表转换为 Excel 文件的字节流
     */
    public ByteArrayInputStream exportRecordsToExcel(List<RecordDTO> records) throws IOException {

        // 1. 创建一个新的 Excel 工作簿 (XLSX 格式)
        try (Workbook workbook = new XSSFWorkbook()) {

            // 2. 创建一个工作表
            Sheet sheet = workbook.createSheet("Borrow Records");

            // 3. 创建表头行
            Row headerRow = sheet.createRow(0);
            String[] headers = {"ID", "书名", "用户名", "借阅日期", "应还日期", "状态"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
            }

            // 4. 填充数据行
            int rowIdx = 1;
            for (RecordDTO record : records) {
                Row row = sheet.createRow(rowIdx++);

                row.createCell(0).setCellValue(record.getId());
                row.createCell(1).setCellValue(record.getBookTitle());
                row.createCell(2).setCellValue(record.getUserName());
                // 日期需要转换为字符串
                row.createCell(3).setCellValue(record.getBorrowDate() != null ? record.getBorrowDate().toString() : "");
                row.createCell(4).setCellValue(record.getDueDate() != null ? record.getDueDate().toString() : "");
                row.createCell(5).setCellValue(record.getStatus());
            }

            // 5. 将工作簿写入字节流
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        }
    }
}