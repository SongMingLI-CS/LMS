package com.npu.lms.controller;

import com.npu.lms.dto.CategoryStatsDTO;
import com.npu.lms.dto.StagnantBookDTO;
import com.npu.lms.entity.Book;
import com.npu.lms.service.BookService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;
import java.util.Map;

@RestController
@RequestMapping("/api/books")
public class BookController {

    @Autowired
    private BookService bookService;

    // --- (基础 CRUD 接口) ---

    // 获取所有图书 (供图书管理页面使用)
    @GetMapping
    public List<Book> getAllBooks() {
        return bookService.getAllBooks();
    }

    // --- 【P1 检索能力】分页 + 搜索 + 排序 + 分类分面 ---
    @GetMapping("/search")
    public Page<Book> searchBooks(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id,asc") String sort) {
        return bookService.searchBooks(q, category, page, size, sort);
    }

    // 图书入库 (新增)
    @PostMapping
    public Book createBook(@RequestBody Book book) {
        // 在 V2 中，我们应该使用 DTO，但目前保持实体以匹配前端
        return bookService.saveBook(book);
    }

    // 图书编辑 (更新)
    @PutMapping("/{id}")
    public Book updateBook(@PathVariable Long id, @RequestBody Book bookDetails) {
        // 使用 Service 层的更新逻辑
        return bookService.updateBook(id, bookDetails);
    }

    // 图书出库 (删除)
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteBook(@PathVariable Long id) {
        bookService.deleteBook(id);
        return ResponseEntity.ok().build();
    }

    /**
     * 【新增 V3】批量导入图书
     * 仅限管理员/超级管理员
     */
    @PostMapping("/upload")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_SUPERADMIN')") // 确保权限
    public ResponseEntity<Map<String, Object>> uploadBooks(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "上传文件不能为空"));
        }

        try {
            Map<String, Object> result = bookService.importBooksFromExcel(file);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "导入失败: " + e.getMessage()));
        }
    }

    /**
     * 【新增 V3】获取低库存图书列表
     * 仅限管理员/超级管理员
     */
    @GetMapping("/low-stock")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_SUPERADMIN')")
    public ResponseEntity<List<Book>> getLowStockBooks() {
        return ResponseEntity.ok(bookService.getLowStockBooks());
    }

    // --- 【数据分析接口 (2 个)】 ---

    // 图书分类占比
    @GetMapping("/analysis/categories")
    public ResponseEntity<List<CategoryStatsDTO>> getCategoryStats() {
        return ResponseEntity.ok(bookService.getBookCategoryStats());
    }

    // 【新增】滞销图书
    @GetMapping("/analysis/stagnant-books")
    public ResponseEntity<List<StagnantBookDTO>> getStagnantBooks() {
        return ResponseEntity.ok(bookService.getStagnantBooks());
    }
}