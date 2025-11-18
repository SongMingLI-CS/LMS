package com.npu.lms.service;

import com.npu.lms.dto.CategoryStatsDTO;
import com.npu.lms.dto.StagnantBookDTO;
import com.npu.lms.entity.Book;
import com.npu.lms.repository.BookRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.web.multipart.MultipartFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import org.apache.poi.ss.usermodel.CellType; // 【新增】

import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.time.LocalDate;

@Service
public class BookService {

    @Autowired
    private BookRepository bookRepository;
    private static final Logger log = LoggerFactory.getLogger(BookService.class); // 【新增】
    // --- (基础 CRUD 方法) ---

    // 用于借阅记录 DTO 映射
    public Book findBookById(Long id) {
        return bookRepository.findById(id).orElse(null);
    }

    // 用于图书管理页面
    public List<Book> getAllBooks() {
        return bookRepository.findAll();
    }

    // 用于图书入库
    @Transactional
    public Book saveBook(Book book) {
        return bookRepository.save(book);
    }

    // 用于图书出库
    @Transactional
    public void deleteBook(Long id) {
        bookRepository.deleteById(id);
    }

    // 用于图书编辑
    @Transactional
    public Book updateBook(Long id, Book bookDetails) {
        Book book = findBookById(id);
        if (book == null) {
            throw new RuntimeException("未找到 ID 为 " + id + " 的图书");
        }

        // 更新所有 V2 字段
        book.setTitle(bookDetails.getTitle());
        book.setAuthor(bookDetails.getAuthor());
        book.setIsbn(bookDetails.getIsbn());
        book.setStock(bookDetails.getStock());
        book.setAvailable(bookDetails.getAvailable());
        book.setCategory(bookDetails.getCategory());
        book.setCover(bookDetails.getCover());
        book.setPublisher(bookDetails.getPublisher());
        book.setPublicationDate(bookDetails.getPublicationDate());
        book.setStatus(bookDetails.getStatus());
        book.setPrice(bookDetails.getPrice());
        book.setSupplier(bookDetails.getSupplier());
        book.setIntroduction(bookDetails.getIntroduction());

        return bookRepository.save(book);
    }

    // --- 【数据分析方法 (2 个)】 ---

    // 1. 图书分类占比
    public List<CategoryStatsDTO> getBookCategoryStats() {
        return bookRepository.getBookCategoryStats();
    }

    // 2. 滞销图书
    public List<StagnantBookDTO> getStagnantBooks() {
        // 定义“滞销”为 6 个月内未被借阅
        LocalDateTime sixMonthsAgo = LocalDateTime.now().minusMonths(6);
        // 返回 Top 10 滞销图书
        return bookRepository.findStagnantBooks(sixMonthsAgo, PageRequest.of(0, 10));
    }

    /**
     * 【新增 V3】从 Excel 批量导入图书
     * @param file 上传的 .xlsx 文件
     * @return 包含成功和失败信息的 Map
     */
    @Transactional
    public Map<String, Object> importBooksFromExcel(MultipartFile file) {
        List<Book> booksToSave = new ArrayList<>();
        List<String> errorMessages = new ArrayList<>();
        int successCount = 0;
        int failCount = 0;

        try (InputStream is = file.getInputStream()) {
            Workbook workbook = new XSSFWorkbook(is);
            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rows = sheet.iterator();

            // 1. 跳过表头 (第一行)
            if (rows.hasNext()) {
                rows.next();
            }

            // 2. 遍历数据行
            while (rows.hasNext()) {
                Row currentRow = rows.next();
                try {
                    // --- 开始读取列数据 ---
                    // (假设: 0:书名, 1:作者, 2:ISBN, 3:库存, 4:分类, 5:出版社, 6:价格)

                    String title = getCellStringValue(currentRow.getCell(0));
                    String author = getCellStringValue(currentRow.getCell(1));
                    String isbn = getCellStringValue(currentRow.getCell(2));

                    // --- 校验关键字段 ---
                    if (title == null || title.isEmpty() || isbn == null || isbn.isEmpty()) {
                        throw new Exception("书名和 ISBN 不能为空");
                    }

                    // --- 检查 ISBN 是否已存在 ---
                    if (bookRepository.findByIsbn(isbn).isPresent()) {
                        throw new Exception("ISBN 已存在: " + isbn);
                    }

                    Book book = new Book();
                    book.setTitle(title);
                    book.setAuthor(author);
                    book.setIsbn(isbn);

                    // 读取数字字段 (库存)
                    int stock = (int) getCellNumericValue(currentRow.getCell(3), 1.0); // 默认 1
                    book.setStock(stock);
                    book.setAvailable(stock); // 默认可借阅数 = 总库存

                    // 读取可选字段
                    book.setCategory(getCellStringValue(currentRow.getCell(4)));
                    book.setPublisher(getCellStringValue(currentRow.getCell(5)));

                    // 读取价格字段
                    double price = getCellNumericValue(currentRow.getCell(6), 0.0);
                    book.setPrice(BigDecimal.valueOf(price));

                    // (其他字段如 cover, introduction 可以留空或设置默认值)
                    book.setStatus("可借");

                    booksToSave.add(book);
                    successCount++;

                } catch (Exception e) {
                    failCount++;
                    String errorMsg = "第 " + (currentRow.getRowNum() + 1) + " 行导入失败: " + e.getMessage();
                    errorMessages.add(errorMsg);
                    log.warn(errorMsg); // 记录警告日志
                }
            }

            // 3. 批量保存到数据库
            if (!booksToSave.isEmpty()) {
                bookRepository.saveAll(booksToSave);
            }

        } catch (Exception e) {
            log.error("解析 Excel 文件失败", e);
            errorMessages.add("文件处理失败: " + e.getMessage());
            failCount = -1; // -1 表示文件级别错误
        }

        // 4. 返回处理结果
        Map<String, Object> result = new HashMap<>();
        result.put("success", successCount);
        result.put("failed", failCount);
        result.put("errors", errorMessages);
        return result;
    }

    /**
     * 【新增 V3】获取所有低库存图书 (供仪表盘使用)
     * @return
     */
    public List<Book> getLowStockBooks() {
        // 重用 NotificationService 中的阈值
        return bookRepository.findByAvailableLessThan(5);
    }

    // --- 【V3 优化版】辅助方法：安全地获取单元格字符串值 ---
    private String getCellStringValue(Cell cell) {
        if (cell == null) {
            return null;
        }
        // 使用 Java 21 的 enhanced switch
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> {
                // 【修复】使用 BigDecimal.valueOf() 避免精度问题
                yield BigDecimal.valueOf(cell.getNumericCellValue()).toPlainString();
            }
            default -> null;
        };
    }

    // --- 【V3 优化版】辅助方法：安全地获取单元格数值 ---
    private double getCellNumericValue(Cell cell, double defaultValue) {
        if (cell == null) {
            return defaultValue;
        }
        // 使用 Java 21 的 enhanced switch
        return switch (cell.getCellType()) {
            case NUMERIC -> cell.getNumericCellValue();
            case STRING -> {
                try {
                    yield Double.parseDouble(cell.getStringCellValue().trim());
                } catch (NumberFormatException e) {
                    yield defaultValue;
                }
            }
            default -> defaultValue;
        };
    }
}