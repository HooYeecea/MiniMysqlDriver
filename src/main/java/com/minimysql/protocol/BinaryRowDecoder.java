package com.minimysql.protocol;

import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

/**
 * 解析二进制协议结果集中的一行，转成字符串（供现有 ResultSet 复用）。
 */
public final class BinaryRowDecoder {

    private BinaryRowDecoder() {
    }

    public static String[] decode(byte[] payload, ColumnDefinition[] columns) {
        if (payload.length == 0 || (payload[0] & 0xFF) != 0x00) {
            throw new IllegalArgumentException("不是二进制行：首字节应为 0x00");
        }
        PacketReader reader = new PacketReader(payload);
        reader.readUint8(); // header

        int columnCount = columns.length;
        int nullMapLen = (columnCount + 7 + 2) / 8;
        byte[] nullMap = reader.readBytes(nullMapLen);

        String[] values = new String[columnCount];
        for (int i = 0; i < columnCount; i++) {
            int bit = i + 2;
            boolean isNull = (nullMap[bit / 8] & (1 << (bit % 8))) != 0;
            if (isNull) {
                values[i] = null;
            } else {
                values[i] = readValue(reader, columns[i]);
            }
        }
        return values;
    }

    private static String readValue(PacketReader reader, ColumnDefinition column) {
        boolean unsigned = (column.flags & MysqlType.UNSIGNED_FLAG) != 0;
        return switch (column.type) {
            case MysqlType.TINY -> unsigned
                    ? Integer.toString(reader.readUint8())
                    : Integer.toString((byte) reader.readUint8());
            case MysqlType.SHORT, MysqlType.YEAR -> unsigned
                    ? Integer.toString(reader.readUint16())
                    : Integer.toString(reader.readInt16());
            case MysqlType.LONG, MysqlType.INT24 -> unsigned
                    ? Long.toString(reader.readUint32())
                    : Integer.toString(reader.readInt32());
            case MysqlType.LONGLONG -> {
                long v = reader.readInt64();
                yield unsigned ? Long.toUnsignedString(v) : Long.toString(v);
            }
            case MysqlType.FLOAT -> Float.toString(reader.readFloat());
            case MysqlType.DOUBLE -> Double.toString(reader.readDouble());
            case MysqlType.DATE -> readDate(reader);
            case MysqlType.TIMESTAMP, MysqlType.DATETIME -> readDateTime(reader);
            case MysqlType.TIME -> readTime(reader);
            case MysqlType.BIT -> HexFormat.of().formatHex(reader.readLengthEncodedBytes());
            default -> new String(reader.readLengthEncodedBytes(), StandardCharsets.UTF_8);
        };
    }

    private static String readDate(PacketReader reader) {
        int len = reader.readUint8();
        if (len == 0) {
            return "0000-00-00";
        }
        int year = reader.readUint16();
        int month = reader.readUint8();
        int day = reader.readUint8();
        if (len > 4) {
            reader.readBytes(len - 4);
        }
        return "%04d-%02d-%02d".formatted(year, month, day);
    }

    private static String readDateTime(PacketReader reader) {
        int len = reader.readUint8();
        if (len == 0) {
            return "0000-00-00 00:00:00";
        }
        int year = 0;
        int month = 0;
        int day = 0;
        int hour = 0;
        int minute = 0;
        int second = 0;
        if (len >= 4) {
            year = reader.readUint16();
            month = reader.readUint8();
            day = reader.readUint8();
        }
        if (len >= 7) {
            hour = reader.readUint8();
            minute = reader.readUint8();
            second = reader.readUint8();
        }
        if (len >= 11) {
            reader.readInt32(); // microseconds，迷你驱动忽略
        } else if (len > 7) {
            reader.readBytes(len - 7);
        } else if (len > 4 && len < 7) {
            reader.readBytes(len - 4);
        }
        return "%04d-%02d-%02d %02d:%02d:%02d".formatted(year, month, day, hour, minute, second);
    }

    private static String readTime(PacketReader reader) {
        int len = reader.readUint8();
        if (len == 0) {
            return "00:00:00";
        }
        int negative = reader.readUint8();
        long days = reader.readUint32();
        int hour = reader.readUint8();
        int minute = reader.readUint8();
        int second = reader.readUint8();
        if (len >= 12) {
            reader.readInt32();
        } else if (len > 8) {
            reader.readBytes(len - 8);
        }
        hour += (int) (days * 24);
        String body = "%02d:%02d:%02d".formatted(hour, minute, second);
        return negative != 0 ? "-" + body : body;
    }
}
