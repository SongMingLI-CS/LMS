package com.npu.lms.service;

import com.npu.lms.entity.Book;
import com.npu.lms.entity.BookItem;
import com.npu.lms.entity.Branch;
import com.npu.lms.repository.BookItemRepository;
import com.npu.lms.repository.BookRepository;
import com.npu.lms.repository.BranchRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BookItemServiceTest {

    private BookItemRepository bookItemRepository;
    private BranchRepository branchRepository;
    private BookItemService service;

    @BeforeEach
    void setUp() {
        bookItemRepository = mock(BookItemRepository.class);
        branchRepository = mock(BranchRepository.class);
        BookRepository bookRepository = mock(BookRepository.class);

        service = new BookItemService();
        ReflectionTestUtils.setField(service, "bookItemRepository", bookItemRepository);
        ReflectionTestUtils.setField(service, "branchRepository", branchRepository);
        ReflectionTestUtils.setField(service, "bookRepository", bookRepository);

        Branch branch = new Branch();
        branch.setName("主馆");
        branch.setCode("MAIN");
        when(branchRepository.findByCode("MAIN")).thenReturn(Optional.of(branch));
        when(bookItemRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void generatesStockItemsWithCorrectStatusSplit() {
        Book book = new Book();
        book.setId(1L);
        book.setStock(5);
        book.setAvailable(3);

        List<BookItem> items = service.generateItemsForBook(book);

        assertEquals(5, items.size());
        assertEquals(3, items.stream().filter(i -> BookItem.STATUS_AVAILABLE.equals(i.getStatus())).count());
        assertEquals(2, items.stream().filter(i -> BookItem.STATUS_BORROWED.equals(i.getStatus())).count());
        assertEquals("BC000001-001", items.get(0).getBarcode());
        assertEquals("BC000001-005", items.get(4).getBarcode());
        assertEquals("主馆", items.get(0).getBranch().getName());
    }

    @Test
    void generatesNoItemsWhenStockIsZero() {
        Book book = new Book();
        book.setId(2L);
        book.setStock(0);
        book.setAvailable(0);

        List<BookItem> items = service.generateItemsForBook(book);

        assertEquals(0, items.size());
    }

    @Test
    void findByBarcodeReturnsNullWhenMissing() {
        when(bookItemRepository.findByBarcode("NOPE")).thenReturn(Optional.empty());
        assertNull(service.findByBarcode("NOPE"));
    }
}
