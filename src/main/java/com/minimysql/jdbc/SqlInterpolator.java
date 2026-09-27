package com.minimysql.jdbc;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * 把带 ? 的 SQL 与参数拼成最终 SQL（客户端绑定，仍走 COM_QUERY）。
 *
 * 注意：不会发往服务器做 COM_STMT_PREPARE；这是迷你驱动的简化实现。
 */
final class SqlInterpolator {

    private SqlInterpolator() {
    }

    /**
     * 按 ? 拆分 SQL，忽略引号内的问号。
     */
    static String[] splitByPlaceholders(String sql) throws SQLException {
        if (sql == null) {
            throw new SQLException("SQL 不能为 null");
        }
        List<String> parts = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inSingle = false;
        boolean inDouble = false;
        boolean escape = false;

        for (int i = 0; i < sql.length(); i++) {
            char c = sql.charAt(i);
            if (escape) {
                current.append(c);
                escape = false;
                continue;
            }
            if (c == '\\' && (inSingle || inDouble)) {
                current.append(c);
                escape = true;
                continue;
            }
            if (c == '\'' && !inDouble) {
                inSingle = !inSingle;
                current.append(c);
                continue;
            }
            if (c == '"' && !inSingle) {
                inDouble = !inDouble;
                current.append(c);
                continue;
            }
            if (c == '?' && !inSingle && !inDouble) {
                parts.add(current.toString());
                current.setLength(0);
                continue;
            }
            current.append(c);
        }
        if (inSingle || inDouble) {
            throw new SQLException("SQL 引号未闭合");
        }
        parts.add(current.toString());
        return parts.toArray(new String[0]);
    }

    static String bind(String[] parts, Object[] params) throws SQLException {
        int expected = parts.length - 1;
        if (params.length != expected) {
            throw new SQLException("参数个数不匹配: 期望 " + expected + ", 实际 " + params.length);
        }
        for (int i = 0; i < params.length; i++) {
            if (params[i] == MiniPreparedStatement.UNSET) {
                throw new SQLException("参数尚未设置: index=" + (i + 1));
            }
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < expected; i++) {
            sb.append(parts[i]);
            sb.append(toLiteral(params[i]));
        }
        sb.append(parts[expected]);
        return sb.toString();
    }

    static String toLiteral(Object value) throws SQLException {
        if (value == null) {
            return "NULL";
        }
        if (value instanceof Boolean b) {
            return b ? "1" : "0";
        }
        if (value instanceof Byte || value instanceof Short
                || value instanceof Integer || value instanceof Long
                || value instanceof Float || value instanceof Double
                || value instanceof BigDecimal) {
            return value.toString();
        }
        if (value instanceof byte[] bytes) {
            return "X'" + toHex(bytes) + "'";
        }
        if (value instanceof Date d) {
            return "'" + d + "'";
        }
        if (value instanceof Time t) {
            return "'" + t + "'";
        }
        if (value instanceof Timestamp ts) {
            String s = ts.toString();
            // Timestamp.toString 可能带纳秒，截到秒级足够
            if (s.length() > 19) {
                s = s.substring(0, 19);
            }
            return "'" + s + "'";
        }
        if (value instanceof String s) {
            return "'" + escape(s) + "'";
        }
        throw new SQLException("暂不支持的参数类型: " + value.getClass().getName());
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\").replace("'", "\\'");
    }

    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }
}
