package com.minimysql.jdbc;

import com.minimysql.protocol.OkPacket;
import com.minimysql.protocol.QueryResult;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.Date;
import java.sql.NClob;
import java.sql.ParameterMetaData;
import java.sql.PreparedStatement;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Calendar;

/**
 * PreparedStatement 迷你实现：客户端替换 ?，再走 COM_QUERY。
 */
public class MiniPreparedStatement implements PreparedStatement {

    /** 参数槽尚未赋值的占位。 */
    static final Object UNSET = new Object();

    private final MiniConnection connection;
    private final String[] sqlParts;
    private final Object[] params;

    private boolean closed;
    private ResultSet currentResultSet;
    private int updateCount = -1;

    MiniPreparedStatement(MiniConnection connection, String sql) throws SQLException {
        this.connection = connection;
        this.sqlParts = SqlInterpolator.splitByPlaceholders(sql);
        this.params = new Object[sqlParts.length - 1];
        Arrays.fill(params, UNSET);
    }

    private void checkOpen() throws SQLException {
        if (closed) {
            throw new SQLException("PreparedStatement 已关闭");
        }
        connection.session();
    }

    private void checkIndex(int parameterIndex) throws SQLException {
        if (parameterIndex < 1 || parameterIndex > params.length) {
            throw new SQLException("参数下标越界: " + parameterIndex
                    + "（共 " + params.length + " 个）");
        }
    }

    private void set(int parameterIndex, Object value) throws SQLException {
        checkOpen();
        checkIndex(parameterIndex);
        params[parameterIndex - 1] = value;
    }

    private String renderSql() throws SQLException {
        return SqlInterpolator.bind(sqlParts, params);
    }

    private void closeCurrentResultSet() throws SQLException {
        if (currentResultSet != null) {
            currentResultSet.close();
            currentResultSet = null;
        }
    }

    @Override
    public ResultSet executeQuery() throws SQLException {
        checkOpen();
        closeCurrentResultSet();
        String sql = renderSql();
        try {
            QueryResult result = connection.session().executeQuery(sql);
            currentResultSet = new MiniResultSet(this, result);
            updateCount = -1;
            return currentResultSet;
        } catch (IOException e) {
            throw JdbcExceptions.wrap(e);
        } catch (RuntimeException e) {
            throw JdbcExceptions.wrap(e);
        }
    }

    @Override
    public int executeUpdate() throws SQLException {
        checkOpen();
        closeCurrentResultSet();
        String sql = renderSql();
        try {
            OkPacket ok = connection.session().executeUpdate(sql);
            long affected = ok.affectedRows;
            if (affected > Integer.MAX_VALUE) {
                throw new SQLException("affectedRows 超出 int 范围: " + affected);
            }
            updateCount = (int) affected;
            return updateCount;
        } catch (IOException e) {
            throw JdbcExceptions.wrap(e);
        } catch (RuntimeException e) {
            throw JdbcExceptions.wrap(e);
        }
    }

    @Override
    public boolean execute() throws SQLException {
        String sql = renderSql().trim();
        if (sql.regionMatches(true, 0, "select", 0, 6)
                || sql.regionMatches(true, 0, "show", 0, 4)
                || sql.regionMatches(true, 0, "desc", 0, 4)
                || sql.regionMatches(true, 0, "explain", 0, 7)) {
            executeQuery();
            return true;
        }
        executeUpdate();
        return false;
    }

    @Override
    public void setNull(int parameterIndex, int sqlType) throws SQLException {
        set(parameterIndex, null);
    }

    @Override
    public void setBoolean(int parameterIndex, boolean x) throws SQLException {
        set(parameterIndex, x);
    }

    @Override
    public void setByte(int parameterIndex, byte x) throws SQLException {
        set(parameterIndex, x);
    }

    @Override
    public void setShort(int parameterIndex, short x) throws SQLException {
        set(parameterIndex, x);
    }

    @Override
    public void setInt(int parameterIndex, int x) throws SQLException {
        set(parameterIndex, x);
    }

    @Override
    public void setLong(int parameterIndex, long x) throws SQLException {
        set(parameterIndex, x);
    }

    @Override
    public void setFloat(int parameterIndex, float x) throws SQLException {
        set(parameterIndex, x);
    }

    @Override
    public void setDouble(int parameterIndex, double x) throws SQLException {
        set(parameterIndex, x);
    }

    @Override
    public void setBigDecimal(int parameterIndex, BigDecimal x) throws SQLException {
        set(parameterIndex, x);
    }

    @Override
    public void setString(int parameterIndex, String x) throws SQLException {
        set(parameterIndex, x);
    }

    @Override
    public void setBytes(int parameterIndex, byte[] x) throws SQLException {
        set(parameterIndex, x);
    }

    @Override
    public void setDate(int parameterIndex, Date x) throws SQLException {
        set(parameterIndex, x);
    }

    @Override
    public void setTime(int parameterIndex, Time x) throws SQLException {
        set(parameterIndex, x);
    }

    @Override
    public void setTimestamp(int parameterIndex, Timestamp x) throws SQLException {
        set(parameterIndex, x);
    }

    @Override
    public void setAsciiStream(int parameterIndex, InputStream x, int length) throws SQLException {
        throw ConnectionUrl.unsupported("setAsciiStream");
    }

    @Override
    public void setUnicodeStream(int parameterIndex, InputStream x, int length) throws SQLException {
        throw ConnectionUrl.unsupported("setUnicodeStream");
    }

    @Override
    public void setBinaryStream(int parameterIndex, InputStream x, int length) throws SQLException {
        throw ConnectionUrl.unsupported("setBinaryStream");
    }

    @Override
    public void clearParameters() throws SQLException {
        checkOpen();
        Arrays.fill(params, UNSET);
    }

    @Override
    public void setObject(int parameterIndex, Object x, int targetSqlType) throws SQLException {
        set(parameterIndex, x);
    }

    @Override
    public void setObject(int parameterIndex, Object x) throws SQLException {
        set(parameterIndex, x);
    }

    @Override
    public void addBatch() throws SQLException {
        throw ConnectionUrl.unsupported("addBatch");
    }

    @Override
    public void setCharacterStream(int parameterIndex, Reader reader, int length) throws SQLException {
        throw ConnectionUrl.unsupported("setCharacterStream");
    }

    @Override
    public void setRef(int parameterIndex, Ref x) throws SQLException {
        throw ConnectionUrl.unsupported("setRef");
    }

    @Override
    public void setBlob(int parameterIndex, Blob x) throws SQLException {
        throw ConnectionUrl.unsupported("setBlob");
    }

    @Override
    public void setClob(int parameterIndex, Clob x) throws SQLException {
        throw ConnectionUrl.unsupported("setClob");
    }

    @Override
    public void setArray(int parameterIndex, Array x) throws SQLException {
        throw ConnectionUrl.unsupported("setArray");
    }

    @Override
    public ResultSetMetaData getMetaData() throws SQLException {
        throw ConnectionUrl.unsupported("PreparedStatement.getMetaData");
    }

    @Override
    public void setDate(int parameterIndex, Date x, Calendar cal) throws SQLException {
        setDate(parameterIndex, x);
    }

    @Override
    public void setTime(int parameterIndex, Time x, Calendar cal) throws SQLException {
        setTime(parameterIndex, x);
    }

    @Override
    public void setTimestamp(int parameterIndex, Timestamp x, Calendar cal) throws SQLException {
        setTimestamp(parameterIndex, x);
    }

    @Override
    public void setNull(int parameterIndex, int sqlType, String typeName) throws SQLException {
        setNull(parameterIndex, sqlType);
    }

    @Override
    public void setURL(int parameterIndex, URL x) throws SQLException {
        set(parameterIndex, x == null ? null : x.toString());
    }

    @Override
    public ParameterMetaData getParameterMetaData() throws SQLException {
        throw ConnectionUrl.unsupported("getParameterMetaData");
    }

    @Override
    public void setRowId(int parameterIndex, RowId x) throws SQLException {
        throw ConnectionUrl.unsupported("setRowId");
    }

    @Override
    public void setNString(int parameterIndex, String value) throws SQLException {
        setString(parameterIndex, value);
    }

    @Override
    public void setNCharacterStream(int parameterIndex, Reader value, long length) throws SQLException {
        throw ConnectionUrl.unsupported("setNCharacterStream");
    }

    @Override
    public void setNClob(int parameterIndex, NClob value) throws SQLException {
        throw ConnectionUrl.unsupported("setNClob");
    }

    @Override
    public void setClob(int parameterIndex, Reader reader, long length) throws SQLException {
        throw ConnectionUrl.unsupported("setClob");
    }

    @Override
    public void setBlob(int parameterIndex, InputStream inputStream, long length) throws SQLException {
        throw ConnectionUrl.unsupported("setBlob");
    }

    @Override
    public void setNClob(int parameterIndex, Reader reader, long length) throws SQLException {
        throw ConnectionUrl.unsupported("setNClob");
    }

    @Override
    public void setSQLXML(int parameterIndex, SQLXML xmlObject) throws SQLException {
        throw ConnectionUrl.unsupported("setSQLXML");
    }

    @Override
    public void setObject(int parameterIndex, Object x, int targetSqlType, int scaleOrLength)
            throws SQLException {
        set(parameterIndex, x);
    }

    @Override
    public void setAsciiStream(int parameterIndex, InputStream x, long length) throws SQLException {
        throw ConnectionUrl.unsupported("setAsciiStream");
    }

    @Override
    public void setBinaryStream(int parameterIndex, InputStream x, long length) throws SQLException {
        throw ConnectionUrl.unsupported("setBinaryStream");
    }

    @Override
    public void setCharacterStream(int parameterIndex, Reader reader, long length) throws SQLException {
        throw ConnectionUrl.unsupported("setCharacterStream");
    }

    @Override
    public void setAsciiStream(int parameterIndex, InputStream x) throws SQLException {
        throw ConnectionUrl.unsupported("setAsciiStream");
    }

    @Override
    public void setBinaryStream(int parameterIndex, InputStream x) throws SQLException {
        throw ConnectionUrl.unsupported("setBinaryStream");
    }

    @Override
    public void setCharacterStream(int parameterIndex, Reader reader) throws SQLException {
        throw ConnectionUrl.unsupported("setCharacterStream");
    }

    @Override
    public void setNCharacterStream(int parameterIndex, Reader value) throws SQLException {
        throw ConnectionUrl.unsupported("setNCharacterStream");
    }

    @Override
    public void setClob(int parameterIndex, Reader reader) throws SQLException {
        throw ConnectionUrl.unsupported("setClob");
    }

    @Override
    public void setBlob(int parameterIndex, InputStream inputStream) throws SQLException {
        throw ConnectionUrl.unsupported("setBlob");
    }

    @Override
    public void setNClob(int parameterIndex, Reader reader) throws SQLException {
        throw ConnectionUrl.unsupported("setNClob");
    }

    // ---- Statement 方法 ----

    @Override
    public ResultSet executeQuery(String sql) throws SQLException {
        throw ConnectionUrl.unsupported("PreparedStatement.executeQuery(String)");
    }

    @Override
    public int executeUpdate(String sql) throws SQLException {
        throw ConnectionUrl.unsupported("PreparedStatement.executeUpdate(String)");
    }

    @Override
    public void close() throws SQLException {
        if (closed) {
            return;
        }
        closed = true;
        closeCurrentResultSet();
    }

    @Override
    public int getMaxFieldSize() {
        return 0;
    }

    @Override
    public void setMaxFieldSize(int max) throws SQLException {
        throw ConnectionUrl.unsupported("setMaxFieldSize");
    }

    @Override
    public int getMaxRows() {
        return 0;
    }

    @Override
    public void setMaxRows(int max) throws SQLException {
        throw ConnectionUrl.unsupported("setMaxRows");
    }

    @Override
    public void setEscapeProcessing(boolean enable) {
        // no-op
    }

    @Override
    public int getQueryTimeout() {
        return 0;
    }

    @Override
    public void setQueryTimeout(int seconds) throws SQLException {
        throw ConnectionUrl.unsupported("setQueryTimeout");
    }

    @Override
    public void cancel() throws SQLException {
        throw ConnectionUrl.unsupported("cancel");
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
    public void setCursorName(String name) throws SQLException {
        throw ConnectionUrl.unsupported("setCursorName");
    }

    @Override
    public boolean execute(String sql) throws SQLException {
        throw ConnectionUrl.unsupported("PreparedStatement.execute(String)");
    }

    @Override
    public ResultSet getResultSet() {
        return currentResultSet;
    }

    @Override
    public int getUpdateCount() {
        return updateCount;
    }

    @Override
    public boolean getMoreResults() throws SQLException {
        closeCurrentResultSet();
        updateCount = -1;
        return false;
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
    public int getResultSetConcurrency() {
        return ResultSet.CONCUR_READ_ONLY;
    }

    @Override
    public int getResultSetType() {
        return ResultSet.TYPE_FORWARD_ONLY;
    }

    @Override
    public void addBatch(String sql) throws SQLException {
        throw ConnectionUrl.unsupported("addBatch(String)");
    }

    @Override
    public void clearBatch() {
        // no-op
    }

    @Override
    public int[] executeBatch() throws SQLException {
        throw ConnectionUrl.unsupported("executeBatch");
    }

    @Override
    public Connection getConnection() {
        return connection;
    }

    @Override
    public boolean getMoreResults(int current) throws SQLException {
        return getMoreResults();
    }

    @Override
    public ResultSet getGeneratedKeys() throws SQLException {
        throw ConnectionUrl.unsupported("getGeneratedKeys");
    }

    @Override
    public int executeUpdate(String sql, int autoGeneratedKeys) throws SQLException {
        throw ConnectionUrl.unsupported("executeUpdate(String, ...)");
    }

    @Override
    public int executeUpdate(String sql, int[] columnIndexes) throws SQLException {
        throw ConnectionUrl.unsupported("executeUpdate(String, ...)");
    }

    @Override
    public int executeUpdate(String sql, String[] columnNames) throws SQLException {
        throw ConnectionUrl.unsupported("executeUpdate(String, ...)");
    }

    @Override
    public boolean execute(String sql, int autoGeneratedKeys) throws SQLException {
        throw ConnectionUrl.unsupported("execute(String, ...)");
    }

    @Override
    public boolean execute(String sql, int[] columnIndexes) throws SQLException {
        throw ConnectionUrl.unsupported("execute(String, ...)");
    }

    @Override
    public boolean execute(String sql, String[] columnNames) throws SQLException {
        throw ConnectionUrl.unsupported("execute(String, ...)");
    }

    @Override
    public int getResultSetHoldability() {
        return ResultSet.HOLD_CURSORS_OVER_COMMIT;
    }

    @Override
    public boolean isClosed() {
        return closed;
    }

    @Override
    public void setPoolable(boolean poolable) {
        // ignore
    }

    @Override
    public boolean isPoolable() {
        return false;
    }

    @Override
    public void closeOnCompletion() {
        // ignore
    }

    @Override
    public boolean isCloseOnCompletion() {
        return false;
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
