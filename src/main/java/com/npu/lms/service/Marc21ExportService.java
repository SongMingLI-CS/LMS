package com.npu.lms.service;

import com.npu.lms.entity.Book;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * P2 行业互操作：最小 MARC21 (ISO 2709) 导出器（无外部依赖）。
 *
 * <p>将馆藏书目导出为标准 MARC21 记录，覆盖常见书目字段
 * (001/008/020/100/245/260)，采用 UTF-8 编码（Leader/09 = 'a'），
 * 便于迁移或接入成熟图书馆生态（如图书馆集成系统 / ILS 批量导入）。</p>
 */
public final class Marc21ExportService {

    private static final byte FT = 0x1E; // 字段结束符
    private static final byte RT = 0x1D; // 记录结束符

    private Marc21ExportService() {
    }

    /** 将一组书目导出为多条串联的 MARC21 记录。 */
    public static byte[] exportBooks(List<Book> books) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            for (Book book : books) {
                out.write(toMarc21(book));
            }
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** 将单条书目导出为一条 MARC21 记录。 */
    public static byte[] toMarc21(Book book) {
        List<Field> fields = buildFields(book);

        // 1) 构建目录 + 数据区
        StringBuilder directory = new StringBuilder();
        ByteArrayOutputStream data = new ByteArrayOutputStream();
        int offset = 0;
        for (Field field : fields) {
            byte[] encoded = field.encode(); // 已含字段结束符 FT
            int fieldLength = encoded.length;
            directory.append(field.tag)
                    .append(String.format("%04d", fieldLength))
                    .append(String.format("%05d", offset));
            data.write(encoded, 0, encoded.length);
            offset += fieldLength;
        }
        byte[] dirBytes = directory.toString().getBytes(StandardCharsets.US_ASCII);
        int baseAddress = 24 + dirBytes.length + 1; // +1 = 目录后的字段结束符
        byte[] dataBytes = data.toByteArray();
        int recordLength = baseAddress + dataBytes.length + 1; // +1 = 记录结束符

        // 2) Leader (固定 24 字节)
        String leader = String.format("%05d", recordLength)  // 00-04 记录长度
                + "n"                                        // 05  记录状态: n=新增
                + "am"                                       // 06  资料类型: a=文字 / 07  书目层级: m=专著
                + " a"                                       // 08  控制类型: 空格 / 09  字符集: a=UTF-8
                + "22"                                       // 10  指示符长度 / 11  子字段代码长度
                + String.format("%05d", baseAddress)         // 12-16 数据基址
                + " a 4500";                                 // 17-23 著录/编目规则/未定义区
        if (leader.length() != 24) {
            throw new IllegalStateException("Leader 长度必须为 24，实际: " + leader.length());
        }

        // 3) 组装
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            out.write(leader.getBytes(StandardCharsets.US_ASCII));
            out.write(dirBytes);
            out.write(FT);
            out.write(dataBytes);
            out.write(RT);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return out.toByteArray();
    }

    private static List<Field> buildFields(Book book) {
        List<Field> fields = new ArrayList<>();
        fields.add(Field.control("001", String.valueOf(book.getId())));
        fields.add(Field.control("008", build008(book)));
        if (hasText(book.getIsbn())) {
            fields.add(Field.data("020", "  ", marcSub('a', book.getIsbn())));
        }
        if (hasText(book.getAuthor())) {
            fields.add(Field.data("100", "1 ", marcSub('a', book.getAuthor())));
        }
        if (hasText(book.getTitle())) {
            fields.add(Field.data("245", "10", marcSub('a', book.getTitle())));
        }
        StringBuilder publisher = new StringBuilder();
        if (hasText(book.getPublisher())) {
            publisher.append(marcSub('b', book.getPublisher()));
        }
        if (book.getPublicationDate() != null) {
            publisher.append(marcSub('c', String.valueOf(book.getPublicationDate().getYear())));
        }
        if (!publisher.isEmpty()) {
            fields.add(Field.data("260", "  ", publisher.toString()));
        }
        return fields;
    }

    /** 构造子字段串: 0x1F + code + value。 */
    private static String marcSub(char code, String value) {
        return "\u001F" + code + value;
    }

    /** 008 定长控制字段：日期/出版年等 40 字符编码数据。 */
    private static String build008(Book book) {
        char[] c = new char[40];
        Arrays.fill(c, ' ');
        String now = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM"));
        for (int i = 0; i < now.length() && i < 6; i++) {
            c[i] = now.charAt(i); // 00-05 档案日期
        }
        c[6] = 's'; // 06 日期类型: 单个日期
        int year = book.getPublicationDate() != null ? book.getPublicationDate().getYear() : 0;
        String yearStr = year > 0 ? String.format("%04d", year) : "    ";
        for (int i = 0; i < 4; i++) {
            c[7 + i] = yearStr.charAt(i); // 07-10 出版年
        }
        return new String(c);
    }

    private static boolean hasText(String s) {
        return s != null && !s.isBlank();
    }

    /** 目录字段：控制字段为原始值；数据字段为指示符 + 子字段串。 */
    private static final class Field {
        final String tag;
        final String body;

        Field(String tag, String body) {
            this.tag = tag;
            this.body = body;
        }

        static Field control(String tag, String value) {
            return new Field(tag, value);
        }

        static Field data(String tag, String indicators, String subfields) {
            return new Field(tag, indicators + subfields);
        }

        /** 输出数据字节并附字段结束符。 */
        byte[] encode() {
            try {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                out.write(body.getBytes(StandardCharsets.UTF_8));
                out.write(FT);
                return out.toByteArray();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }
}
