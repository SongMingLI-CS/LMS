package com.npu.lms.service;

import com.npu.lms.repository.BookBatchRepository;
import com.npu.lms.repository.BookRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BookServiceTest {

    private BookRepository bookRepository;
    private BookService bookService;

    @BeforeEach
    void setUp() {
        bookRepository = mock(BookRepository.class);
        BookBatchRepository batchRepository = mock(BookBatchRepository.class);
        bookService = new BookService();
        ReflectionTestUtils.setField(bookService, "bookRepository", bookRepository);
        ReflectionTestUtils.setField(bookService, "bookBatchRepository", batchRepository);
    }

    @Test
    void searchCapsPageSizeTo100() {
        when(bookRepository.searchBooks(isNull(), isNull(), any(Pageable.class))).thenReturn(Page.empty());

        bookService.searchBooks(null, null, 0, 1000, "id,asc");

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(bookRepository).searchBooks(isNull(), isNull(), captor.capture());
        assertEquals(100, captor.getValue().getPageSize());
    }

    @Test
    void searchClampsNegativePageToZero() {
        when(bookRepository.searchBooks(isNull(), isNull(), any(Pageable.class))).thenReturn(Page.empty());

        bookService.searchBooks(null, null, -5, 20, null);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(bookRepository).searchBooks(isNull(), isNull(), captor.capture());
        assertEquals(0, captor.getValue().getPageNumber());
    }

    @Test
    void blankQueryAndCategoryBecomeNull() {
        when(bookRepository.searchBooks(isNull(), isNull(), any(Pageable.class))).thenReturn(Page.empty());

        bookService.searchBooks("   ", "", 0, 20, null);

        verify(bookRepository).searchBooks(isNull(), isNull(), any(Pageable.class));
    }

    @Test
    void parseSortRejectsUnknownField() {
        Sort sort = bookService.parseSort("hackedField,desc");
        assertEquals("id", sort.iterator().next().getProperty());
    }

    @Test
    void parseSortAcceptsKnownFieldDesc() {
        Sort sort = bookService.parseSort("title,desc");
        assertEquals("title", sort.iterator().next().getProperty());
        assertTrue(sort.getOrderFor("title").isDescending());
    }
}
