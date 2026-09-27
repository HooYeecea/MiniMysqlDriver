package com.minimysql.jdbc;

import com.minimysql.protocol.OkPacket;
import com.minimysql.protocol.QueryResult;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;

/**
 * JDBC Statement：把 SQL 交给底层 {@link com.minimysql.protocol.MysqlSession}。
 */
public class MiniStatement implements Statement {

    private final MiniConnection connection;
    private boolean closed;
    private ResultSet currentResultSet;
    private int updateCount = -1;

    MiniStatement(MiniConnection connection) {
        this.connection = connection;
    }

    private void checkOpen() throws SQLException {
        if (closed) {
            throw new SQLException("Statement 已关闭");
        }
        connection.session(); // 顺带检查 Connection
    }

    private void closeCurrentResultSet() throws SQLException {
        if (currentResultSet != null) {
            currentResultSet.close();
            currentResultSet = null;
        }
    }

    @Override
    public ResultSet executeQuery(String sql) throws SQLException {
        checkOpen();
        closeCurrentResultSet();
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
    public int executeUpdate(String sql) throws SQLException {
        checkOpen();
        closeCurrentResultSet();
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
    public boolean execute(String sql) throws SQLException {
        checkOpen();
        String trimmed = sql == null ? "" : sql.trim();
        if (trimmed.regionMatches(true, 0, "select", 0, 6)
                || trimmed.regionMatches(true, 0, "show", 0, 4)
                || trimmed.regionMatches(true, 0, "desc", 0, 4)
                || trimmed.regionMatches(true, 0, "explain", 0, 7)) {
            executeQuery(sql);
            return true;
        }
        executeUpdate(sql);
        return false;
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
        throw ConnectionUrl.unsupported("addBatch");
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
        return executeUpdate(sql);
    }

    @Override
    public int executeUpdate(String sql, int[] columnIndexes) throws SQLException {
        return executeUpdate(sql);
    }

    @Override
    public int executeUpdate(String sql, String[] columnNames) throws SQLException {
        return executeUpdate(sql);
    }

    @Override
    public boolean execute(String sql, int autoGeneratedKeys) throws SQLException {
        return execute(sql);
    }

    @Override
    public boolean execute(String sql, int[] columnIndexes) throws SQLException {
        return execute(sql);
    }

    @Override
    public boolean execute(String sql, String[] columnNames) throws SQLException {
        return execute(sql);
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
