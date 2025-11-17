package com.npu.lms.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime; // 引入 LocalDateTime

@Entity
@Table(name = "borrow_records") // 确保表名与您的数据库一致
public class BorrowRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    // --- 【修改 1】 ---
    // 将 borrowDate (LocalDate) 更改为 borrowTime (LocalDateTime)
    @Column(name = "borrow_time")
    private LocalDateTime borrowTime;

    @Column(name = "due_date")
    private LocalDate dueDate;

    // --- 【修改 2】 ---
    // 将 returnDate (LocalDate) 更改为 returnTime (LocalDateTime)
    @Column(name = "return_time")
    private LocalDateTime returnTime;

    @Column(nullable = false)
    private String status; // 例如: "borrowed", "returned", "reserved"


    // --- Getters and Setters (请确保所有都已更新) ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public Book getBook() { return book; }
    public void setBook(Book book) { this.book = book; }

    // 对应 borrowTime
    public LocalDateTime getBorrowTime() { return borrowTime; }
    public void setBorrowTime(LocalDateTime borrowTime) { this.borrowTime = borrowTime; }

    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }

    // 对应 returnTime
    public LocalDateTime getReturnTime() { return returnTime; }
    public void setReturnTime(LocalDateTime returnTime) { this.returnTime = returnTime; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}