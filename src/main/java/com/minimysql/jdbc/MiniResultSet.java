package com.minimysql.jdbc;

import com.minimysql.protocol.ColumnDefinition;
import com.minimysql.protocol.QueryResult;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Date;
import java.sql.NClob;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Map;

/**
 * JDBC ResultSet：包装协议层 {@link QueryResult}，仅支持向前只读游标。
 */
public class MiniResultSet implements ResultSet {

    private final MiniStatement statement;
    private final QueryResult data;
    private final MiniResultSetMetaData metaData;

    /** 当前行下标，-1 表示在第一行之前。 */
    private int cursor = -1;
    private boolean closed;
    private boolean wasNull;

    MiniResultSet(MiniStatement statement, QueryResult data) {
        this.statement = statement;
        this.data = data;
        this.metaData = new MiniResultSetMetaData(data.columns);
    }

    private void checkOpen() throws SQLException {
        if (closed) {
            throw new SQLException("ResultSet 已关闭");
        }
    }

    private void checkRow() throws SQLException {
        checkOpen();
        if (cursor < 0 || cursor >= data.rows.size()) {
            throw new SQLException("当前没有有效数据行");
        }
    }

    private String raw(int columnIndex) throws SQLException {
        checkRow();
        if (columnIndex < 1 || columnIndex > data.columnCount()) {
            throw new SQLException("列下标越界: " + columnIndex);
        }
        String value = data.rows.get(cursor)[columnIndex - 1];
        wasNull = (value == null);
        return value;
    }

    private int indexOfLabel(String columnLabel) throws SQLException {
        if (columnLabel == null) {
            throw new SQLException("columnLabel 为 null");
        }
        for (int i = 0; i < data.columns.length; i++) {
            ColumnDefinition col = data.columns[i];
            if (columnLabel.equalsIgnoreCase(col.name)
                    || columnLabel.equalsIgnoreCase(col.orgName)) {
                return i + 1;
            }
        }
        throw new SQLException("找不到列: " + columnLabel);
    }

    @Override
    public boolean next() throws SQLException {
        checkOpen();
        if (cursor + 1 < data.rows.size()) {
            cursor++;
            return true;
        }
        cursor = data.rows.size();
        return false;
    }

    @Override
    public void close() {
        closed = true;
    }

    @Override
    public boolean wasNull() {
        return wasNull;
    }

    @Override
    public String getString(int columnIndex) throws SQLException {
        return raw(columnIndex);
    }

    @Override
    public boolean getBoolean(int columnIndex) throws SQLException {
        String v = raw(columnIndex);
        if (v == null) {
            return false;
        }
        return "1".equals(v) || "true".equalsIgnoreCase(v) || "yes".equalsIgnoreCase(v);
    }

    @Override
    public byte getByte(int columnIndex) throws SQLException {
        String v = raw(columnIndex);
        return v == null ? 0 : Byte.parseByte(v);
    }

    @Override
    public short getShort(int columnIndex) throws SQLException {
        String v = raw(columnIndex);
        return v == null ? 0 : Short.parseShort(v);
    }

    @Override
    public int getInt(int columnIndex) throws SQLException {
        String v = raw(columnIndex);
        return v == null ? 0 : Integer.parseInt(v);
    }

    @Override
    public long getLong(int columnIndex) throws SQLException {
        String v = raw(columnIndex);
        return v == null ? 0L : Long.parseLong(v);
    }

    @Override
    public float getFloat(int columnIndex) throws SQLException {
        String v = raw(columnIndex);
        return v == null ? 0f : Float.parseFloat(v);
    }

    @Override
    public double getDouble(int columnIndex) throws SQLException {
        String v = raw(columnIndex);
        return v == null ? 0d : Double.parseDouble(v);
    }

    @Override
    public BigDecimal getBigDecimal(int columnIndex, int scale) throws SQLException {
        String v = raw(columnIndex);
        return v == null ? null : new BigDecimal(v).setScale(scale, java.math.RoundingMode.HALF_UP);
    }

    @Override
    public byte[] getBytes(int columnIndex) throws SQLException {
        String v = raw(columnIndex);
        return v == null ? null : v.getBytes();
    }

    @Override
    public Date getDate(int columnIndex) throws SQLException {
        String v = raw(columnIndex);
        return v == null ? null : Date.valueOf(v.substring(0, Math.min(10, v.length())));
    }

    @Override
    public Time getTime(int columnIndex) throws SQLException {
        throw ConnectionUrl.unsupported("getTime");
    }

    @Override
    public Timestamp getTimestamp(int columnIndex) throws SQLException {
        String v = raw(columnIndex);
        if (v == null) {
            return null;
        }
        // MySQL 文本常为 "yyyy-MM-dd HH:mm:ss"
        String normalized = v.length() >= 19 ? v.substring(0, 19) : v;
        if (normalized.length() == 10) {
            normalized = normalized + " 00:00:00";
        }
        return Timestamp.valueOf(normalized.replace('T', ' '));
    }

    @Override
    public InputStream getAsciiStream(int columnIndex) throws SQLException {
        throw ConnectionUrl.unsupported("getAsciiStream");
    }

    @Override
    public InputStream getUnicodeStream(int columnIndex) throws SQLException {
        throw ConnectionUrl.unsupported("getUnicodeStream");
    }

    @Override
    public InputStream getBinaryStream(int columnIndex) throws SQLException {
        throw ConnectionUrl.unsupported("getBinaryStream");
    }

    @Override
    public String getString(String columnLabel) throws SQLException {
        return getString(indexOfLabel(columnLabel));
    }

    @Override
    public boolean getBoolean(String columnLabel) throws SQLException {
        return getBoolean(indexOfLabel(columnLabel));
    }

    @Override
    public byte getByte(String columnLabel) throws SQLException {
        return getByte(indexOfLabel(columnLabel));
    }

    @Override
    public short getShort(String columnLabel) throws SQLException {
        return getShort(indexOfLabel(columnLabel));
    }

    @Override
    public int getInt(String columnLabel) throws SQLException {
        return getInt(indexOfLabel(columnLabel));
    }

    @Override
    public long getLong(String columnLabel) throws SQLException {
        return getLong(indexOfLabel(columnLabel));
    }

    @Override
    public float getFloat(String columnLabel) throws SQLException {
        return getFloat(indexOfLabel(columnLabel));
    }

    @Override
    public double getDouble(String columnLabel) throws SQLException {
        return getDouble(indexOfLabel(columnLabel));
    }

    @Override
    public BigDecimal getBigDecimal(String columnLabel, int scale) throws SQLException {
        return getBigDecimal(indexOfLabel(columnLabel), scale);
    }

    @Override
    public byte[] getBytes(String columnLabel) throws SQLException {
        return getBytes(indexOfLabel(columnLabel));
    }

    @Override
    public Date getDate(String columnLabel) throws SQLException {
        return getDate(indexOfLabel(columnLabel));
    }

    @Override
    public Time getTime(String columnLabel) throws SQLException {
        return getTime(indexOfLabel(columnLabel));
    }

    @Override
    public Timestamp getTimestamp(String columnLabel) throws SQLException {
        return getTimestamp(indexOfLabel(columnLabel));
    }

    @Override
    public InputStream getAsciiStream(String columnLabel) throws SQLException {
        throw ConnectionUrl.unsupported("getAsciiStream");
    }

    @Override
    public InputStream getUnicodeStream(String columnLabel) throws SQLException {
        throw ConnectionUrl.unsupported("getUnicodeStream");
    }

    @Override
    public InputStream getBinaryStream(String columnLabel) throws SQLException {
        throw ConnectionUrl.unsupported("getBinaryStream");
    }

    @Override
    public SQLWarning getWarnings() {
        return null;
    }

    @Override
    public void clearWarnings() {
        // no-op
    }

    @Override
    public String getCursorName() throws SQLException {
        throw ConnectionUrl.unsupported("getCursorName");
    }

    @Override
    public ResultSetMetaData getMetaData() {
        return metaData;
    }

    @Override
    public Object getObject(int columnIndex) throws SQLException {
        return getString(columnIndex);
    }

    @Override
    public Object getObject(String columnLabel) throws SQLException {
        return getString(columnLabel);
    }

    @Override
    public int findColumn(String columnLabel) throws SQLException {
        return indexOfLabel(columnLabel);
    }

    @Override
    public Reader getCharacterStream(int columnIndex) throws SQLException {
        throw ConnectionUrl.unsupported("getCharacterStream");
    }

    @Override
    public Reader getCharacterStream(String columnLabel) throws SQLException {
        throw ConnectionUrl.unsupported("getCharacterStream");
    }

    @Override
    public BigDecimal getBigDecimal(int columnIndex) throws SQLException {
        String v = raw(columnIndex);
        return v == null ? null : new BigDecimal(v);
    }

    @Override
    public BigDecimal getBigDecimal(String columnLabel) throws SQLException {
        return getBigDecimal(indexOfLabel(columnLabel));
    }

    @Override
    public boolean isBeforeFirst() {
        return cursor < 0 && !data.rows.isEmpty();
    }

    @Override
    public boolean isAfterLast() {
        return !data.rows.isEmpty() && cursor >= data.rows.size();
    }

    @Override
    public boolean isFirst() {
        return cursor == 0 && !data.rows.isEmpty();
    }

    @Override
    public boolean isLast() {
        return !data.rows.isEmpty() && cursor == data.rows.size() - 1;
    }

    @Override
    public void beforeFirst() throws SQLException {
        throw ConnectionUrl.unsupported("beforeFirst");
    }

    @Override
    public void afterLast() throws SQLException {
        throw ConnectionUrl.unsupported("afterLast");
    }

    @Override
    public boolean first() throws SQLException {
        throw ConnectionUrl.unsupported("first");
    }

    @Override
    public boolean last() throws SQLException {
        throw ConnectionUrl.unsupported("last");
    }

    @Override
    public int getRow() {
        return cursor < 0 || cursor >= data.rows.size() ? 0 : cursor + 1;
    }

    @Override
    public boolean absolute(int row) throws SQLException {
        throw ConnectionUrl.unsupported("absolute");
    }

    @Override
    public boolean relative(int rows) throws SQLException {
        throw ConnectionUrl.unsupported("relative");
    }

    @Override
    public boolean previous() throws SQLException {
        throw ConnectionUrl.unsupported("previous");
    }

    @Override
    public void setFetchDirection(int direction) throws SQLException {
        if (direction != ResultSet.FETCH_FORWARD) {
            throw ConnectionUrl.unsupported("setFetchDirection");
        }
    }

    @Override
    public int getFetchDirection() {
        return ResultSet.FETCH_FORWARD;
    }

    @Override
    public void setFetchSize(int rows) {
        // ignore
    }

    @Override
    public int getFetchSize() {
        return 0;
    }

    @Override
    public int getType() {
        return ResultSet.TYPE_FORWARD_ONLY;
    }

    @Override
    public int getConcurrency() {
        return ResultSet.CONCUR_READ_ONLY;
    }

    @Override
    public boolean rowUpdated() {
        return false;
    }

    @Override
    public boolean rowInserted() {
        return false;
    }

    @Override
    public boolean rowDeleted() {
        return false;
    }

    @Override
    public void updateNull(int columnIndex) throws SQLException {
        throw ConnectionUrl.unsupported("updateXxx");
    }

    @Override
    public void updateBoolean(int columnIndex, boolean x) throws SQLException {
        throw ConnectionUrl.unsupported("updateXxx");
    }

    @Override
    public void updateByte(int columnIndex, byte x) throws SQLException {
        throw ConnectionUrl.unsupported("updateXxx");
    }

    @Override
    public void updateShort(int columnIndex, short x) throws SQLException {
        throw ConnectionUrl.unsupported("updateXxx");
    }

    @Override
    public void updateInt(int columnIndex, int x) throws SQLException {
        throw ConnectionUrl.unsupported("updateXxx");
    }

    @Override
    public void updateLong(int columnIndex, long x) throws SQLException {
        throw ConnectionUrl.unsupported("updateXxx");
    }

    @Override
    public void updateFloat(int columnIndex, float x) throws SQLException {
        throw ConnectionUrl.unsupported("updateXxx");
    }

    @Override
    public void updateDouble(int columnIndex, double x) throws SQLException {
        throw ConnectionUrl.unsupported("updateXxx");
    }

    @Override
    public void updateBigDecimal(int columnIndex, BigDecimal x) throws SQLException {
        throw ConnectionUrl.unsupported("updateXxx");
    }

    @Override
    public void updateString(int columnIndex, String x) throws SQLException {
        throw ConnectionUrl.unsupported("updateXxx");
    }

    @Override
    public void updateBytes(int columnIndex, byte[] x) throws SQLException {
        throw ConnectionUrl.unsupported("updateXxx");
    }

    @Override
    public void updateDate(int columnIndex, Date x) throws SQLException {
        throw ConnectionUrl.unsupported("updateXxx");
    }

    @Override
    public void updateTime(int columnIndex, Time x) throws SQLException {
        throw ConnectionUrl.unsupported("updateXxx");
    }

    @Override
    public void updateTimestamp(int columnIndex, Timestamp x) throws SQLException {
        throw ConnectionUrl.unsupported("updateXxx");
    }

    @Override
    public void updateAsciiStream(int columnIndex, InputStream x, int length) throws SQLException {
        throw ConnectionUrl.unsupported("updateXxx");
    }

    @Override
    public void updateBinaryStream(int columnIndex, InputStream x, int length) throws SQLException {
        throw ConnectionUrl.unsupported("updateXxx");
    }

    @Override
    public void updateCharacterStream(int columnIndex, Reader x, int length) throws SQLException {
        throw ConnectionUrl.unsupported("updateXxx");
    }

    @Override
    public void updateObject(int columnIndex, Object x, int scaleOrLength) throws SQLException {
        throw ConnectionUrl.unsupported("updateXxx");
    }

    @Override
    public void updateObject(int columnIndex, Object x) throws SQLException {
        throw ConnectionUrl.unsupported("updateXxx");
    }

    @Override
    public void updateNull(String columnLabel) throws SQLException {
        throw ConnectionUrl.unsupported("updateXxx");
    }

    @Override
    public void updateBoolean(String columnLabel, boolean x) throws SQLException {
        throw ConnectionUrl.unsupported("updateXxx");
    }

    @Override
    public void updateByte(String columnLabel, byte x) throws SQLException {
        throw ConnectionUrl.unsupported("updateXxx");
    }

    @Override
    public void updateShort(String columnLabel, short x) throws SQLException {
        throw ConnectionUrl.unsupported("updateXxx");
    }

    @Override
    public void updateInt(String columnLabel, int x) throws SQLException {
        throw ConnectionUrl.unsupported("updateXxx");
    }

    @Override
    public void updateLong(String columnLabel, long x) throws SQLException {
        throw ConnectionUrl.unsupported("updateXxx");
    }

    @Override
    public void updateFloat(String columnLabel, float x) throws SQLException {
        throw ConnectionUrl.unsupported("updateXxx");
    }

    @Override
    public void updateDouble(String columnLabel, double x) throws SQLException {
        throw ConnectionUrl.unsupported("updateXxx");
    }

    @Override
    public void updateBigDecimal(String columnLabel, BigDecimal x) throws SQLException {
        throw ConnectionUrl.unsupported("updateXxx");
    }

    @Override
    public void updateString(String columnLabel, String x) throws SQLException {
        throw ConnectionUrl.unsupported("updateXxx");
    }

    @Override
    public void updateBytes(String columnLabel, byte[] x) throws SQLException {
        throw ConnectionUrl.unsupported("updateXxx");
    }

    @Override
    public void updateDate(String columnLabel, Date x) throws SQLException {
        throw ConnectionUrl.unsupported("updateXxx");
    }

    @Override
    public void updateTime(String columnLabel, Time x) throws SQLException {
        throw ConnectionUrl.unsupported("updateXxx");
    }

    @Override
    public void updateTimestamp(String columnLabel, Timestamp x) throws SQLException {
        throw ConnectionUrl.unsupported("updateXxx");
    }

    @Override
    public void updateAsciiStream(String columnLabel, InputStream x, int length) throws SQLException {
        throw ConnectionUrl.unsupported("updateXxx");
    }

    @Override
    public void updateBinaryStream(String columnLabel, InputStream x, int length) throws SQLException {
        throw ConnectionUrl.unsupported("updateXxx");
    }

    @Override
    public void updateCharacterStream(String columnLabel, Reader reader, int length) throws SQLException {
        throw ConnectionUrl.unsupported("updateXxx");
    }

    @Override
    public void updateObject(String columnLabel, Object x, int scaleOrLength) throws SQLException {
        throw ConnectionUrl.unsupported("updateXxx");
    }

    @Override
    public void updateObject(String columnLabel, Object x) throws SQLException {
        throw ConnectionUrl.unsupported("updateXxx");
    }

    @Override
    public void insertRow() throws SQLException {
        throw ConnectionUrl.unsupported("insertRow");
    }

    @Override
    public void updateRow() throws SQLException {
        throw ConnectionUrl.unsupported("updateRow");
    }

    @Override
    public void deleteRow() throws SQLException {
        throw ConnectionUrl.unsupported("deleteRow");
    }

    @Override
    public void refreshRow() throws SQLException {
        throw ConnectionUrl.unsupported("refreshRow");
    }

    @Override
    public void cancelRowUpdates() throws SQLException {
        throw ConnectionUrl.unsupported("cancelRowUpdates");
    }

    @Override
    public void moveToInsertRow() throws SQLException {
        throw ConnectionUrl.unsupported("moveToInsertRow");
    }

    @Override
    public void moveToCurrentRow() throws SQLException {
        throw ConnectionUrl.unsupported("moveToCurrentRow");
    }

    @Override
    public Statement getStatement() {
        return statement;
    }

    @Override
    public Object getObject(int columnIndex, Map<String, Class<?>> map) throws SQLException {
        return getObject(columnIndex);
    }

    @Override
    public Ref getRef(int columnIndex) throws SQLException {
        throw ConnectionUrl.unsupported("getRef");
    }

    @Override
    public Blob getBlob(int columnIndex) throws SQLException {
        throw ConnectionUrl.unsupported("getBlob");
    }

    @Override
    public Clob getClob(int columnIndex) throws SQLException {
        throw ConnectionUrl.unsupported("getClob");
    }

    @Override
    public Array getArray(int columnIndex) throws SQLException {
        throw ConnectionUrl.unsupported("getArray");
    }

    @Override
    public Object getObject(String columnLabel, Map<String, Class<?>> map) throws SQLException {
        return getObject(columnLabel);
    }

    @Override
    public Ref getRef(String columnLabel) throws SQLException {
        throw ConnectionUrl.unsupported("getRef");
    }

    @Override
    public Blob getBlob(String columnLabel) throws SQLException {
        throw ConnectionUrl.unsupported("getBlob");
    }

    @Override
    public Clob getClob(String columnLabel) throws SQLException {
        throw ConnectionUrl.unsupported("getClob");
    }

    @Override
    public Array getArray(String columnLabel) throws SQLException {
        throw ConnectionUrl.unsupported("getArray");
    }

    @Override
    public Date getDate(int columnIndex, Calendar cal) throws SQLException {
        return getDate(columnIndex);
    }

    @Override
    public Date getDate(String columnLabel, Calendar cal) throws SQLException {
        return getDate(columnLabel);
    }

    @Override
    public Time getTime(int columnIndex, Calendar cal) throws SQLException {
        return getTime(columnIndex);
    }

    @Override
    public Time getTime(String columnLabel, Calendar cal) throws SQLException {
        return getTime(columnLabel);
    }

    @Override
    public Timestamp getTimestamp(int columnIndex, Calendar cal) throws SQLException {
        return getTimestamp(columnIndex);
    }

    @Override
    public Timestamp getTimestamp(String columnLabel, Calendar cal) throws SQLException {
        return getTimestamp(columnLabel);
    }

    @Override
    public URL getURL(int columnIndex) throws SQLException {
        throw ConnectionUrl.unsupported("getURL");
    }

    @Override
    public URL getURL(String columnLabel) throws SQLException {
        throw ConnectionUrl.unsupported("getURL");
    }

    @Override
    public void updateRef(int columnIndex, Ref x) throws SQLException {
        throw ConnectionUrl.unsupported("updateXxx");
    }

    @Override
    public void updateRef(String columnLabel, Ref x) throws SQLException {
        throw ConnectionUrl.unsupported("updateXxx");
    }

    @Override
    public void updateBlob(int columnIndex, Blob x) throws SQLException {
        throw ConnectionUrl.unsupported("updateXxx");
    }

    @Override
    public void updateBlob(String columnLabel, Blob x) throws SQLException {
        throw ConnectionUrl.unsupported("updateXxx");
    }

    @Override
    public void updateClob(int columnIndex, Clob x) throws SQLException {
        throw ConnectionUrl.unsupported("updateXxx");
    }

    @Override
    public void updateClob(String columnLabel, Clob x) throws SQLException {
        throw ConnectionUrl.unsupported("updateXxx");
    }

    @Override
    public void updateArray(int columnIndex, Array x) throws SQLException {
        throw ConnectionUrl.unsupported("updateXxx");
    }

    @Override
    public void updateArray(String columnLabel, Array x) throws SQLException {
        throw ConnectionUrl.unsupported("updateXxx");
    }

    @Override
    public RowId getRowId(int columnIndex) throws SQLException {
        throw ConnectionUrl.unsupported("getRowId");
    }

    @Override
    public RowId getRowId(String columnLabel) throws SQLException {
        throw ConnectionUrl.unsupported("getRowId");
    }

    @Override
    public void updateRowId(int columnIndex, RowId x) throws SQLException {
        throw ConnectionUrl.unsupported("updateXxx");
    }

    @Override
    public void updateRowId(String columnLabel, RowId x) throws SQLException {
        throw ConnectionUrl.unsupported("updateXxx");
    }

    @Override
    public int getHoldability() {
        return ResultSet.HOLD_CURSORS_OVER_COMMIT;
    }

    @Override
    public boolean isClosed() {
        return closed;
    }

    @Override
    public void updateNString(int columnIndex, String nString) throws SQLException {
        throw ConnectionUrl.unsupported("updateXxx");
    }

    @Override
    public void updateNString(String columnLabel, String nString) throws SQLException {
        throw ConnectionUrl.unsupported("updateXxx");
    }

    @Override
    public void updateNClob(int columnIndex, NClob nClob) throws SQLException {
        throw ConnectionUrl.unsupported("updateXxx");
    }

    @Override
    public void updateNClob(String columnLabel, NClob nClob) throws SQLException {
        throw ConnectionUrl.unsupported("updateXxx");
    }

    @Override
    public NClob getNClob(int columnIndex) throws SQLException {
        throw ConnectionUrl.unsupported("getNClob");
    }

    @Override
    public NClob getNClob(String columnLabel) throws SQLException {
        throw ConnectionUrl.unsupported("getNClob");
    }

    @Override
    public SQLXML getSQLXML(int columnIndex) throws SQLException {
        throw ConnectionUrl.unsupported("getSQLXML");
    }

    @Override
    public SQLXML getSQLXML(String columnLabel) throws SQLException {
        throw ConnectionUrl.unsupported("getSQLXML");
    }

    @Override
    public void updateSQLXML(int columnIndex, SQLXML xmlObject) throws SQLException {
        throw ConnectionUrl.unsupported("updateXxx");
    }

    @Override
    public void updateSQLXML(String columnLabel, SQLXML xmlObject) throws SQLException {
        throw ConnectionUrl.unsupported("updateXxx");
    }

    @Override
    public String getNString(int columnIndex) throws SQLException {
        return getString(columnIndex);
    }

    @Override
    public String getNString(String columnLabel) throws SQLException {
        return getString(columnLabel);
    }

    @Override
    public Reader getNCharacterStream(int columnIndex) throws SQLException {
        throw ConnectionUrl.unsupported("getNCharacterStream");
    }

    @Override
    public Reader getNCharacterStream(String columnLabel) throws SQLException {
        throw ConnectionUrl.unsupported("getNCharacterStream");
    }

    @Override
    public void updateNCharacterStream(int columnIndex, Reader x, long length) throws SQLException {
        throw ConnectionUrl.unsupported("updateXxx");
    }

    @Override
    public void updateNCharacterStream(String columnLabel, Reader reader, long length) throws SQLException {
        throw ConnectionUrl.unsupported("updateXxx");
    }

    @Override
    public void updateAsciiStream(int columnIndex, InputStream x, long length) throws SQLException {
        throw ConnectionUrl.unsupported("updateXxx");
    }

    @Override
    public void updateBinaryStream(int columnIndex, InputStream x, long length) throws SQLException {
        throw ConnectionUrl.unsupported("updateXxx");
    }

    @Override
    public void updateCharacterStream(int columnIndex, Reader x, long length) throws SQLException {
        throw ConnectionUrl.unsupported("updateXxx");
    }

    @Override
    public void updateAsciiStream(String columnLabel, InputStream x, long length) throws SQLException {
        throw ConnectionUrl.unsupported("updateXxx");
    }

    @Override
    public void updateBinaryStream(String columnLabel, InputStream x, long length) throws SQLException {
        throw ConnectionUrl.unsupported("updateXxx");
    }

    @Override
    public void updateCharacterStream(String columnLabel, Reader reader, long length) throws SQLException {
        throw ConnectionUrl.unsupported("updateXxx");
    }

    @Override
    public void updateBlob(int columnIndex, InputStream inputStream, long length) throws SQLException {
        throw ConnectionUrl.unsupported("updateXxx");
    }

    @Override
    public void updateBlob(String columnLabel, InputStream inputStream, long length) throws SQLException {
        throw ConnectionUrl.unsupported("updateXxx");
    }

    @Override
    public void updateClob(int columnIndex, Reader reader, long length) throws SQLException {
        throw ConnectionUrl.unsupported("updateXxx");
    }

    @Override
    public void updateClob(String columnLabel, Reader reader, long length) throws SQLException {
        throw ConnectionUrl.unsupported("updateXxx");
    }

    @Override
    public void updateNClob(int columnIndex, Reader reader, long length) throws SQLException {
        throw ConnectionUrl.unsupported("updateXxx");
    }

    @Override
    public void updateNClob(String columnLabel, Reader reader, long length) throws SQLException {
        throw ConnectionUrl.unsupported("updateXxx");
    }

    @Override
    public void updateNCharacterStream(int columnIndex, Reader x) throws SQLException {
        throw ConnectionUrl.unsupported("updateXxx");
    }

    @Override
    public void updateNCharacterStream(String columnLabel, Reader reader) throws SQLException {
        throw ConnectionUrl.unsupported("updateXxx");
    }

    @Override
    public void updateAsciiStream(int columnIndex, InputStream x) throws SQLException {
        throw ConnectionUrl.unsupported("updateXxx");
    }

    @Override
    public void updateBinaryStream(int columnIndex, InputStream x) throws SQLException {
        throw ConnectionUrl.unsupported("updateXxx");
    }

    @Override
    public void updateCharacterStream(int columnIndex, Reader x) throws SQLException {
        throw ConnectionUrl.unsupported("updateXxx");
    }

    @Override
    public void updateAsciiStream(String columnLabel, InputStream x) throws SQLException {
        throw ConnectionUrl.unsupported("updateXxx");
    }

    @Override
    public void updateBinaryStream(String columnLabel, InputStream x) throws SQLException {
        throw ConnectionUrl.unsupported("updateXxx");
    }

    @Override
    public void updateCharacterStream(String columnLabel, Reader reader) throws SQLException {
        throw ConnectionUrl.unsupported("updateXxx");
    }

    @Override
    public void updateBlob(int columnIndex, InputStream inputStream) throws SQLException {
        throw ConnectionUrl.unsupported("updateXxx");
    }

    @Override
    public void updateBlob(String columnLabel, InputStream inputStream) throws SQLException {
        throw ConnectionUrl.unsupported("updateXxx");
    }

    @Override
    public void updateClob(int columnIndex, Reader reader) throws SQLException {
        throw ConnectionUrl.unsupported("updateXxx");
    }

    @Override
    public void updateClob(String columnLabel, Reader reader) throws SQLException {
        throw ConnectionUrl.unsupported("updateXxx");
    }

    @Override
    public void updateNClob(int columnIndex, Reader reader) throws SQLException {
        throw ConnectionUrl.unsupported("updateXxx");
    }

    @Override
    public void updateNClob(String columnLabel, Reader reader) throws SQLException {
        throw ConnectionUrl.unsupported("updateXxx");
    }

    @Override
    public <T> T getObject(int columnIndex, Class<T> type) throws SQLException {
        Object value = getObject(columnIndex);
        if (value == null) {
            return null;
        }
        if (type.isInstance(value)) {
            return type.cast(value);
        }
        throw new SQLException("无法转换列为 " + type.getName());
    }

    @Override
    public <T> T getObject(String columnLabel, Class<T> type) throws SQLException {
        return getObject(indexOfLabel(columnLabel), type);
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
