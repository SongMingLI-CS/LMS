package com.npu.lms.controller;

import com.npu.lms.entity.BookItem;
import com.npu.lms.service.BookItemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * P1 馆藏建模：馆藏单册管理接口（仅限管理员/超级管理员）。
 */
@RestController
@RequestMapping("/api/items")
@PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_SUPERADMIN')")
public class BookItemController {

    @Autowired
    private BookItemService bookItemService;

    /** 按书目查看其全部馆藏单册 */
    @GetMapping("/book/{bookId}")
    public ResponseEntity<List<BookItem>> getItemsByBook(@PathVariable Long bookId) {
        return ResponseEntity.ok(bookItemService.findByBookId(bookId));
    }

    /** 按条码查找单册（盘点/追踪） */
    @GetMapping("/barcode/{barcode}")
    public ResponseEntity<?> getItemByBarcode(@PathVariable String barcode) {
        BookItem item = bookItemService.findByBarcode(barcode);
        if (item == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(item);
    }

    /** 回填存量：为尚无单册的书目生成单册 */
    @PostMapping("/sync")
    public ResponseEntity<?> syncItems() {
        int created = bookItemService.syncAllBooks();
        return ResponseEntity.ok(Map.of("message", "馆藏单册回填完成", "created", created));
    }
}
