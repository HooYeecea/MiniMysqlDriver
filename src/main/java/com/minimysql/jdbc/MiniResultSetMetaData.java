package com.minimysql.jdbc;

import com.minimysql.protocol.ColumnDefinition;

import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;

/**
 * 由协议层列定义映射出的 ResultSetMetaData。
 */
public class MiniResultSetMetaData implements ResultSetMetaData {

    private final ColumnDefinition[] columns;

    MiniResultSetMetaData(ColumnDefinition[] columns) {
        this.columns = columns;
    }

    private ColumnDefinition col(int column) throws SQLException {
        if (column < 1 || column > columns.length) {
            throw new SQLException("列下标越界: " + column);
        }
        return columns[column - 1];
    }

    @Override
    public int getColumnCount() {
        return columns.length;
    }

    @Override
    public boolean isAutoIncrement(int column) throws SQLException {
        return (col(column).flags & 0x0200) != 0; // AUTO_INCREMENT_FLAG
    }

    @Override
    public boolean isCaseSensitive(int column) {
        return true;
    }

    @Override
    public boolean isSearchable(int column) {
        return true;
    }

    @Override
    public boolean isCurrency(int column) {
        return false;
    }

    @Override
    public int isNullable(int column) throws SQLException {
        return (col(column).flags & 0x0001) != 0 // NOT_NULL_FLAG
                ? columnNoNulls
                : columnNullable;
    }

    @Override
    public boolean isSigned(int column) {
        return true;
    }

    @Override
    public int getColumnDisplaySize(int column) throws SQLException {
        long len = col(column).columnLength;
        return len > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) len;
    }

    @Override
    public String getColumnLabel(int column) throws SQLException {
        return col(column).name;
    }

    @Override
    public String getColumnName(int column) throws SQLException {
        String org = col(column).orgName;
        return (org == null || org.isEmpty()) ? col(column).name : org;
    }

    @Override
    public String getSchemaName(int column) throws SQLException {
        return col(column).schema;
    }

    @Override
    public int getPrecision(int column) throws SQLException {
        return getColumnDisplaySize(column);
    }

    @Override
    public int getScale(int column) throws SQLException {
        return col(column).decimals;
    }

    @Override
    public String getTableName(int column) throws SQLException {
        return col(column).table;
    }

    @Override
    public String getCatalogName(int column) throws SQLException {
        return col(column).catalog;
    }

    @Override
    public int getColumnType(int column) throws SQLException {
        return mapJdbcType(col(column).type);
    }

    @Override
    public String getColumnTypeName(int column) throws SQLException {
        return switch (col(column).type) {
            case 0x01 -> "TINYINT";
            case 0x02 -> "SMALLINT";
            case 0x03 -> "INT";
            case 0x08 -> "BIGINT";
            case 0x04 -> "FLOAT";
            case 0x05 -> "DOUBLE";
            case 0x0a -> "DATE";
            case 0x0c -> "DATETIME";
            case 0x07, 0x11 -> "TIMESTAMP";
            case 0x0f, 0xfd, 0xfe -> "VARCHAR";
            case 0xf6 -> "DECIMAL";
            default -> "UNKNOWN";
        };
    }

    @Override
    public boolean isReadOnly(int column) {
        return true;
    }

    @Override
    public boolean isWritable(int column) {
        return false;
    }

    @Override
    public boolean isDefinitelyWritable(int column) {
        return false;
    }

    @Override
    public String getColumnClassName(int column) throws SQLException {
        return switch (getColumnType(column)) {
            case Types.INTEGER, Types.TINYINT, Types.SMALLINT -> Integer.class.getName();
            case Types.BIGINT -> Long.class.getName();
            case Types.FLOAT -> Float.class.getName();
            case Types.DOUBLE -> Double.class.getName();
            case Types.DECIMAL -> java.math.BigDecimal.class.getName();
            case Types.DATE -> java.sql.Date.class.getName();
            case Types.TIMESTAMP -> java.sql.Timestamp.class.getName();
            default -> String.class.getName();
        };
    }

    private static int mapJdbcType(int mysqlType) {
        return switch (mysqlType) {
            case 0x01 -> Types.TINYINT;
            case 0x02 -> Types.SMALLINT;
            case 0x03, 0x09 -> Types.INTEGER;
            case 0x08 -> Types.BIGINT;
            case 0x04 -> Types.FLOAT;
            case 0x05 -> Types.DOUBLE;
            case 0x0a -> Types.DATE;
            case 0x0b -> Types.TIME;
            case 0x07, 0x0c, 0x11 -> Types.TIMESTAMP;
            case 0x00, 0xf6 -> Types.DECIMAL;
            default -> Types.VARCHAR;
        };
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        if (iface.isInstance(this)) {
            return iface.cast(this);
        }
        throw new SQLException("不是 " + iface);
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) {
        return iface.isInstance(this);
    }
}
