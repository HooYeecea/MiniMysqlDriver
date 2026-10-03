package com.minimysql.protocol;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Calendar;

/**
 * 把 Java 参数编码成 COM_STMT_EXECUTE 的二进制参数区。
 */
public final class BinaryParameterEncoder {

    private BinaryParameterEncoder() {
    }

    public static byte[] buildExecutePayload(int statementId, Object[] params) {
        PacketWriter w = new PacketWriter();
        w.writeUint8(ComStmt.COM_STMT_EXECUTE);
        w.writeUint32(statementId & 0xFFFFFFFFL);
        w.writeUint8(0); // CURSOR_TYPE_NO_CURSOR
        w.writeUint32(1); // iteration-count

        if (params.length > 0) {
            int nullMapLen = (params.length + 7) / 8;
            byte[] nullMap = new byte[nullMapLen];
            for (int i = 0; i < params.length; i++) {
                if (params[i] == null) {
                    nullMap[i / 8] |= (byte) (1 << (i % 8));
                }
            }
            w.writeBytes(nullMap);
            w.writeUint8(1); // new-params-bound-flag
            for (Object p : params) {
                int type = mysqlTypeOf(p);
                w.writeUint8(type);
                w.writeUint8(0); // unsigned flag
            }
            for (Object p : params) {
                if (p != null) {
                    writeValue(w, p);
                }
            }
        }
        return w.toByteArray();
    }

    private static int mysqlTypeOf(Object value) {
        if (value == null) {
            return MysqlType.NULL;
        }
        if (value instanceof Boolean || value instanceof Byte) {
            return MysqlType.TINY;
        }
        if (value instanceof Short) {
            return MysqlType.SHORT;
        }
        if (value instanceof Integer) {
            return MysqlType.LONG;
        }
        if (value instanceof Long) {
            return MysqlType.LONGLONG;
        }
        if (value instanceof Float) {
            return MysqlType.FLOAT;
        }
        if (value instanceof Double) {
            return MysqlType.DOUBLE;
        }
        if (value instanceof BigDecimal) {
            return MysqlType.NEWDECIMAL;
        }
        if (value instanceof Date || value instanceof LocalDate) {
            return MysqlType.DATE;
        }
        if (value instanceof Time || value instanceof LocalTime) {
            return MysqlType.TIME;
        }
        if (value instanceof Timestamp || value instanceof LocalDateTime) {
            return MysqlType.DATETIME;
        }
        if (value instanceof byte[]) {
            return MysqlType.BLOB;
        }
        return MysqlType.VAR_STRING;
    }

    private static void writeValue(PacketWriter w, Object value) {
        if (value instanceof Boolean b) {
            w.writeUint8(b ? 1 : 0);
            return;
        }
        if (value instanceof Byte b) {
            w.writeUint8(b);
            return;
        }
        if (value instanceof Short s) {
            w.writeUint16(s);
            return;
        }
        if (value instanceof Integer i) {
            w.writeUint32(i);
            return;
        }
        if (value instanceof Long l) {
            w.writeInt64(l);
            return;
        }
        if (value instanceof Float f) {
            w.writeUint32(Float.floatToIntBits(f));
            return;
        }
        if (value instanceof Double d) {
            w.writeInt64(Double.doubleToLongBits(d));
            return;
        }
        if (value instanceof BigDecimal dec) {
            w.writeLengthEncodedBytes(dec.toPlainString().getBytes(StandardCharsets.UTF_8));
            return;
        }
        if (value instanceof Date date) {
            writeDate(w, date.toLocalDate());
            return;
        }
        if (value instanceof LocalDate localDate) {
            writeDate(w, localDate);
            return;
        }
        if (value instanceof Time time) {
            Calendar cal = Calendar.getInstance();
            cal.setTime(time);
            writeTime(w, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), cal.get(Calendar.SECOND));
            return;
        }
        if (value instanceof LocalTime localTime) {
            writeTime(w, localTime.getHour(), localTime.getMinute(), localTime.getSecond());
            return;
        }
        if (value instanceof Timestamp ts) {
            writeDateTime(w, ts.toLocalDateTime());
            return;
        }
        if (value instanceof LocalDateTime ldt) {
            writeDateTime(w, ldt);
            return;
        }
        if (value instanceof byte[] bytes) {
            w.writeLengthEncodedBytes(bytes);
            return;
        }
        w.writeLengthEncodedBytes(String.valueOf(value).getBytes(StandardCharsets.UTF_8));
    }

    private static void writeDate(PacketWriter w, LocalDate date) {
        w.writeUint8(4);
        w.writeUint16(date.getYear());
        w.writeUint8(date.getMonthValue());
        w.writeUint8(date.getDayOfMonth());
    }

    private static void writeTime(PacketWriter w, int hour, int minute, int second) {
        w.writeUint8(8);
        w.writeUint8(0); // not negative
        w.writeUint32(0); // days
        w.writeUint8(hour);
        w.writeUint8(minute);
        w.writeUint8(second);
    }

    private static void writeDateTime(PacketWriter w, LocalDateTime dt) {
        w.writeUint8(7);
        w.writeUint16(dt.getYear());
        w.writeUint8(dt.getMonthValue());
        w.writeUint8(dt.getDayOfMonth());
        w.writeUint8(dt.getHour());
        w.writeUint8(dt.getMinute());
        w.writeUint8(dt.getSecond());
    }
}
