package com.npu.lms.repository;

import com.npu.lms.entity.BookItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookItemRepository extends JpaRepository<BookItem, Long> {

    Optional<BookItem> findByBarcode(String barcode);

    List<BookItem> findByBookId(Long bookId);

    long countByBookId(Long bookId);

    long countByBookIdAndStatus(Long bookId, String status);

    boolean existsByBarcode(String barcode);
}
