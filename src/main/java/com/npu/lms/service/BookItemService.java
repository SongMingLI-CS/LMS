package com.npu.lms.service;

import com.npu.lms.entity.Book;
import com.npu.lms.entity.BookItem;
import com.npu.lms.entity.Branch;
import com.npu.lms.repository.BookItemRepository;
import com.npu.lms.repository.BookRepository;
import com.npu.lms.repository.BranchRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * P1 馆藏建模：馆藏单册服务。
 *
 * <p>负责为书目生成馆藏单册（条码 + 分馆 + 书架 + 状态），支持按条码/书目查询与存量回填。</p>
 */
@Service
public class BookItemService {

    /** 默认书架号（后续可扩展为书架实体） */
    public static final String DEFAULT_SHELF_NO = "SHELF-A";

    @Autowired
    private BookItemRepository bookItemRepository;

    @Autowired
    private BranchRepository branchRepository;

    @Autowired
    private BookRepository bookRepository;

    /**
     * 为书目生成馆藏单册：共 stock 条，前 available 条为可借，其余为已借出。
     */
    @Transactional
    public List<BookItem> generateItemsForBook(Book book) {
        Branch branch = getDefaultBranch();
        int stock = Math.max(book.getStock(), 0);
        int available = Math.min(Math.max(book.getAvailable(), 0), stock);

        List<BookItem> items = new ArrayList<>();
        for (int i = 1; i <= stock; i++) {
            BookItem item = new BookItem();
            item.setBarcode(generateBarcode(book.getId(), i));
            item.setBook(book);
            item.setBranch(branch);
            item.setShelfNo(DEFAULT_SHELF_NO);
            item.setStatus(i <= available ? BookItem.STATUS_AVAILABLE : BookItem.STATUS_BORROWED);
            item.setAcquiredAt(LocalDateTime.now());
            items.add(item);
        }
        return bookItemRepository.saveAll(items);
    }

    /**
     * 回填存量：为尚无单册的书目生成单册（幂等，可重复调用）。
     *
     * @return 本次新生成的单册数量
     */
    @Transactional
    public int syncAllBooks() {
        int created = 0;
        for (Book book : bookRepository.findAll()) {
            if (bookItemRepository.countByBookId(book.getId()) == 0) {
                created += generateItemsForBook(book).size();
            }
        }
        return created;
    }

    public BookItem findByBarcode(String barcode) {
        return bookItemRepository.findByBarcode(barcode).orElse(null);
    }

    public List<BookItem> findByBookId(Long bookId) {
        return bookItemRepository.findByBookId(bookId);
    }

    public long countByBookAndStatus(Long bookId, String status) {
        return bookItemRepository.countByBookIdAndStatus(bookId, status);
    }

    private Branch getDefaultBranch() {
        return branchRepository.findByCode("MAIN").orElseGet(() -> {
            Branch branch = new Branch();
            branch.setName("主馆");
            branch.setCode("MAIN");
            branch.setAddress("默认馆舍");
            return branchRepository.save(branch);
        });
    }

    private String generateBarcode(Long bookId, int seq) {
        return String.format("BC%06d-%03d", bookId, seq);
    }
}
