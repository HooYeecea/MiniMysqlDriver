package com.minimysql.jdbc;

import com.minimysql.protocol.MysqlSession;

import java.io.IOException;
import java.sql.Array;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.NClob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLClientInfoException;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Savepoint;
import java.sql.Statement;
import java.sql.Struct;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executor;

/**
 * JDBC Connection：内部持有已认证的 {@link MysqlSession}。
 */
public class MiniConnection implements Connection {

    private final MysqlSession session;
    private boolean closed;

    private MiniConnection(MysqlSession session) {
        this.session = session;
        this.closed = false;
    }

    static MiniConnection open(ConnectionUrl url, String user, String password) throws SQLException {
        try {
            MysqlSession session = MysqlSession.connect(
                    url.host, url.port, user, password, url.database);
            return new MiniConnection(session);
        } catch (IOException e) {
            throw JdbcExceptions.wrap(e);
        } catch (RuntimeException e) {
            throw JdbcExceptions.wrap(e);
        }
    }

    MysqlSession session() throws SQLException {
        checkOpen();
        return session;
    }

    private void checkOpen() throws SQLException {
        if (closed) {
            throw new SQLException("Connection 已关闭");
        }
    }

    @Override
    public Statement createStatement() throws SQLException {
        checkOpen();
        return new MiniStatement(this);
    }

    @Override
    public void close() throws SQLException {
        if (closed) {
            return;
        }
        closed = true;
        try {
            session.close();
        } catch (IOException e) {
            throw JdbcExceptions.wrap(e);
        }
    }

    @Override
    public boolean isClosed() {
        return closed;
    }

    @Override
    public boolean isValid(int timeout) throws SQLException {
        return !closed;
    }

    @Override
    public String nativeSQL(String sql) {
        return sql;
    }

    @Override
    public void setAutoCommit(boolean autoCommit) throws SQLException {
        if (!autoCommit) {
            throw ConnectionUrl.unsupported("setAutoCommit(false)");
        }
    }

    @Override
    public boolean getAutoCommit() {
        return true;
    }

    @Override
    public void commit() throws SQLException {
        throw ConnectionUrl.unsupported("commit");
    }

    @Override
    public void rollback() throws SQLException {
        throw ConnectionUrl.unsupported("rollback");
    }

    @Override
    public DatabaseMetaData getMetaData() throws SQLException {
        throw ConnectionUrl.unsupported("getMetaData");
    }

    @Override
    public void setReadOnly(boolean readOnly) {
        // ignore
    }

    @Override
    public boolean isReadOnly() {
        return false;
    }

    @Override
    public void setCatalog(String catalog) throws SQLException {
        throw ConnectionUrl.unsupported("setCatalog");
    }

    @Override
    public String getCatalog() {
        return null;
    }

    @Override
    public void setTransactionIsolation(int level) throws SQLException {
        throw ConnectionUrl.unsupported("setTransactionIsolation");
    }

    @Override
    public int getTransactionIsolation() {
        return Connection.TRANSACTION_REPEATABLE_READ;
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
    public Statement createStatement(int resultSetType, int resultSetConcurrency) throws SQLException {
        if (resultSetType != ResultSet.TYPE_FORWARD_ONLY
                || resultSetConcurrency != ResultSet.CONCUR_READ_ONLY) {
            throw ConnectionUrl.unsupported("createStatement(type, concurrency)");
        }
        return createStatement();
    }

    @Override
    public PreparedStatement prepareStatement(String sql) throws SQLException {
        checkOpen();
        return new MiniPreparedStatement(this, sql);
    }

    @Override
    public CallableStatement prepareCall(String sql) throws SQLException {
        throw ConnectionUrl.unsupported("prepareCall");
    }

    @Override
    public Map<String, Class<?>> getTypeMap() throws SQLException {
        throw ConnectionUrl.unsupported("getTypeMap");
    }

    @Override
    public void setTypeMap(Map<String, Class<?>> map) throws SQLException {
        throw ConnectionUrl.unsupported("setTypeMap");
    }

    @Override
    public void setHoldability(int holdability) throws SQLException {
        throw ConnectionUrl.unsupported("setHoldability");
    }

    @Override
    public int getHoldability() {
        return ResultSet.HOLD_CURSORS_OVER_COMMIT;
    }

    @Override
    public Savepoint setSavepoint() throws SQLException {
        throw ConnectionUrl.unsupported("setSavepoint");
    }

    @Override
    public Savepoint setSavepoint(String name) throws SQLException {
        throw ConnectionUrl.unsupported("setSavepoint");
    }

    @Override
    public void rollback(Savepoint savepoint) throws SQLException {
        throw ConnectionUrl.unsupported("rollback(Savepoint)");
    }

    @Override
    public void releaseSavepoint(Savepoint savepoint) throws SQLException {
        throw ConnectionUrl.unsupported("releaseSavepoint");
    }

    @Override
    public Statement createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability)
            throws SQLException {
        return createStatement(resultSetType, resultSetConcurrency);
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency)
            throws SQLException {
        throw ConnectionUrl.unsupported("prepareStatement");
    }

    @Override
    public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency)
            throws SQLException {
        throw ConnectionUrl.unsupported("prepareCall");
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency,
                                              int resultSetHoldability) throws SQLException {
        throw ConnectionUrl.unsupported("prepareStatement");
    }

    @Override
    public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency,
                                         int resultSetHoldability) throws SQLException {
        throw ConnectionUrl.unsupported("prepareCall");
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) throws SQLException {
        throw ConnectionUrl.unsupported("prepareStatement");
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException {
        throw ConnectionUrl.unsupported("prepareStatement");
    }

    @Override
    public PreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException {
        throw ConnectionUrl.unsupported("prepareStatement");
    }

    @Override
    public Clob createClob() throws SQLException {
        throw ConnectionUrl.unsupported("createClob");
    }

    @Override
    public Blob createBlob() throws SQLException {
        throw ConnectionUrl.unsupported("createBlob");
    }

    @Override
    public NClob createNClob() throws SQLException {
        throw ConnectionUrl.unsupported("createNClob");
    }

    @Override
    public SQLXML createSQLXML() throws SQLException {
        throw ConnectionUrl.unsupported("createSQLXML");
    }

    @Override
    public void setClientInfo(String name, String value) throws SQLClientInfoException {
        throw new SQLClientInfoException();
    }

    @Override
    public void setClientInfo(Properties properties) throws SQLClientInfoException {
        throw new SQLClientInfoException();
    }

    @Override
    public String getClientInfo(String name) {
        return null;
    }

    @Override
    public Properties getClientInfo() {
        return new Properties();
    }

    @Override
    public Array createArrayOf(String typeName, Object[] elements) throws SQLException {
        throw ConnectionUrl.unsupported("createArrayOf");
    }

    @Override
    public Struct createStruct(String typeName, Object[] attributes) throws SQLException {
        throw ConnectionUrl.unsupported("createStruct");
    }

    @Override
    public void setSchema(String schema) throws SQLException {
        throw ConnectionUrl.unsupported("setSchema");
    }

    @Override
    public String getSchema() {
        return null;
    }

    @Override
    public void abort(Executor executor) throws SQLException {
        close();
    }

    @Override
    public void setNetworkTimeout(Executor executor, int milliseconds) throws SQLException {
        throw ConnectionUrl.unsupported("setNetworkTimeout");
    }

    @Override
    public int getNetworkTimeout() {
        return 0;
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
