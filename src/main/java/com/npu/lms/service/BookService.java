package com.npu.lms.service;

import com.npu.lms.dto.CategoryStatsDTO;
import com.npu.lms.dto.StagnantBookDTO;
import com.npu.lms.entity.Book;
import com.npu.lms.entity.BookBatch;
import com.npu.lms.repository.BookBatchRepository;
import com.npu.lms.repository.BookRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import org.apache.poi.ss.usermodel.CellType; // 【新增】

import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.time.LocalDate;

@Service
public class BookService {

    @Autowired
    private BookRepository bookRepository;
    @Autowired // 【新增】
    private BookBatchRepository bookBatchRepository;

    @Autowired
    private BookItemService bookItemService;
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

    // --- 【P1 检索能力】分页 + 搜索 + 排序 + 分类分面 ---

    /** 分页大小上限，防止大响应拖垮接口 */
    public static final int MAX_PAGE_SIZE = 100;

    /** 允许排序的字段白名单，防止任意属性排序注入 */
    private static final Set<String> SORTABLE_FIELDS =
            Set.of("id", "title", "author", "isbn", "stock", "available", "category", "price", "publicationDate");

    /**
     * 分页检索图书：q 匹配书名/作者/ISBN，category 精确过滤，并强制分页上限。
     */
    public Page<Book> searchBooks(String q, String category, int page, int size, String sort) {
        Pageable pageable = buildPageable(page, size, sort);
        return bookRepository.searchBooks(trimToNull(q), trimToNull(category), pageable);
    }

    Pageable buildPageable(int page, int size, String sort) {
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        int safePage = Math.max(page, 0);
        return PageRequest.of(safePage, safeSize, parseSort(sort));
    }

    Sort parseSort(String sort) {
        if (sort == null || sort.isBlank()) {
            return Sort.by(Sort.Direction.ASC, "id");
        }
        String[] parts = sort.split(",");
        String field = parts[0].trim();
        if (!SORTABLE_FIELDS.contains(field)) {
            field = "id";
        }
        Sort.Direction direction = (parts.length > 1 && "desc".equalsIgnoreCase(parts[1].trim()))
                ? Sort.Direction.DESC : Sort.Direction.ASC;
        return Sort.by(direction, field);
    }

    private String trimToNull(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        return s.trim();
    }

    // 用于图书入库
    @Transactional
    public Book saveBook(Book book) {
        // 1. 保存图书实体
        Book savedBook = bookRepository.save(book);

        // 2. 【新增】自动创建第一批次
        //    (我们假设 saveBook 总是用于新书入库，所以 stock > 0)
        if (book.getId() == null && savedBook.getStock() > 0) {
            BookBatch batch = new BookBatch();
            batch.setBook(savedBook);
            batch.setQuantity(savedBook.getStock());
            batch.setSupplier(savedBook.getSupplier());
            batch.setPrice(savedBook.getPrice());
            // batch.setPurchaseDate(LocalDate.now()); // 构造函数已设置

            bookBatchRepository.save(batch);
            log.info("【批次】已为新书 (ID: {}) 创建了初始批次，数量: {}", savedBook.getId(), savedBook.getStock());

            // P1 馆藏建模：为新书生成馆藏单册
            bookItemService.generateItemsForBook(savedBook);
        }

        return savedBook; // 3. 返回保存的图书
    }

    // 用于图书出库
    @Transactional
    public void deleteBook(Long id) {
        bookRepository.deleteById(id);
    }

    // 用于图书编辑
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
        // book.setStock(bookDetails.getStock());
        // book.setAvailable(bookDetails.getAvailable());//管理员在编辑图书信息时不能修改库存
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
    public Map<String, Object> importBooksFromExcel(MultipartFile file) throws IOException {
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
            if (!booksToSave.isEmpty()) {
                // 3a. 保存所有图书
                List<Book> savedBooks = bookRepository.saveAll(booksToSave);
                log.info("【导入】成功保存 {} 本图书。", savedBooks.size());

                // 3b. 【新增】为所有已保存的图书创建批次
                List<BookBatch> batchesToSave = new ArrayList<>();
                for (Book savedBook : savedBooks) {
                    BookBatch batch = new BookBatch();
                    batch.setBook(savedBook);
                    batch.setQuantity(savedBook.getStock());
                    batch.setSupplier(savedBook.getSupplier());
                    batch.setPrice(savedBook.getPrice());
                    // batch.setPurchaseDate(LocalDate.now()); // 构造函数已设置
                    batchesToSave.add(batch);
                }
                bookBatchRepository.saveAll(batchesToSave);
                log.info("【导入】成功为 {} 本图书创建了批次记录。", batchesToSave.size());
            }

            // 4. 返回处理结果
            Map<String, Object> result = new HashMap<>();
            result.put("success", successCount);
            result.put("failed", failCount);
            result.put("errors", errorMessages);
            return result;
        }
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