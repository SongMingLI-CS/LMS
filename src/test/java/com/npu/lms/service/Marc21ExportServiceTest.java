package com.npu.lms.service;

import com.npu.lms.entity.Book;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Marc21ExportServiceTest {

    private Book sampleBook() {
        Book book = new Book();
        book.setId(42L);
        book.setTitle("三体");
        book.setAuthor("刘慈欣");
        book.setIsbn("9787536692930");
        book.setPublisher("重庆出版社");
        book.setPublicationDate(LocalDate.of(2008, 1, 1));
        return book;
    }

    private String leaderOf(byte[] rec) {
        return new String(rec, 0, 24, StandardCharsets.US_ASCII);
    }

    @Test
    void leaderIs24BytesAndMatchesRecordLength() {
        byte[] rec = Marc21ExportService.toMarc21(sampleBook());
        String leader = leaderOf(rec);

        assertEquals(24, leader.length());
        int recordLength = Integer.parseInt(leader.substring(0, 5));
        int baseAddress = Integer.parseInt(leader.substring(12, 17));
        assertEquals(rec.length, recordLength);
        assertTrue(baseAddress >= 24);
        assertEquals('a', leader.charAt(9)); // UTF-8 字符集
        assertEquals((byte) 0x1D, rec[rec.length - 1]); // 记录结束符
    }

    @Test
    void directoryContainsExpectedFields() {
        byte[] rec = Marc21ExportService.toMarc21(sampleBook());
        int baseAddress = Integer.parseInt(leaderOf(rec).substring(12, 17));

        List<String> tags = new ArrayList<>();
        for (int pos = 24; pos < baseAddress - 1; pos += 12) {
            tags.add(new String(rec, pos, 3, StandardCharsets.US_ASCII));
        }
        assertTrue(tags.contains("001"));
        assertTrue(tags.contains("008"));
        assertTrue(tags.contains("020"));
        assertTrue(tags.contains("100"));
        assertTrue(tags.contains("245"));
        assertTrue(tags.contains("260"));
    }

    @Test
    void isbnRoundTripsInField020() {
        byte[] rec = Marc21ExportService.toMarc21(sampleBook());
        String body = fieldBody(rec, "020");
        assertNotNull(body);
        assertTrue(body.contains("9787536692930"));
    }

    @Test
    void titleAndPublisherRoundTrip() {
        byte[] rec = Marc21ExportService.toMarc21(sampleBook());
        String title = fieldBody(rec, "245");
        String publisher = fieldBody(rec, "260");
        assertNotNull(title);
        assertTrue(title.contains("三体"));
        assertNotNull(publisher);
        assertTrue(publisher.contains("重庆出版社"));
    }

    @Test
    void missingAuthorOmitsField100() {
        Book book = sampleBook();
        book.setAuthor("");
        byte[] rec = Marc21ExportService.toMarc21(book);
        assertNull(fieldBody(rec, "100"));
    }

    @Test
    void exportBooksConcatenatesRecords() {
        byte[] all = Marc21ExportService.exportBooks(List.of(sampleBook(), sampleBook()));
        int recordCount = 0;
        for (byte b : all) {
            if (b == 0x1D) {
                recordCount++;
            }
        }
        assertEquals(2, recordCount);
    }

    private String fieldBody(byte[] rec, String wantedTag) {
        String leader = leaderOf(rec);
        int baseAddress = Integer.parseInt(leader.substring(12, 17));
        for (int pos = 24; pos < baseAddress - 1; pos += 12) {
            String tag = new String(rec, pos, 3, StandardCharsets.US_ASCII);
            int fieldLength = Integer.parseInt(new String(rec, pos + 3, 4, StandardCharsets.US_ASCII));
            int fieldOffset = Integer.parseInt(new String(rec, pos + 7, 5, StandardCharsets.US_ASCII));
            if (tag.equals(wantedTag)) {
                // 字段数据 = 数据基址 + 偏移，长度去掉字段结束符
                return new String(rec, baseAddress + fieldOffset, fieldLength - 1, StandardCharsets.UTF_8);
            }
        }
        return null;
    }
}
