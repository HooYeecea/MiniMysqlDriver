package com.minimysql.jdbc;

import com.minimysql.protocol.MysqlProtocolException;

import java.io.IOException;
import java.sql.SQLException;

/**
 * 把协议层异常转成 JDBC {@link SQLException}。
 *
 * {@link MysqlProtocolException} 会带上 vendorCode（MySQL errorCode）和 SQLState。
 */
final class JdbcExceptions {

    private JdbcExceptions() {
    }

    static SQLException wrap(IOException e) {
        if (e instanceof MysqlProtocolException mysqlEx) {
            String sqlState = mysqlEx.getSqlState();
            if (sqlState == null || sqlState.isEmpty()) {
                sqlState = "HY000"; // general error
            }
            return new SQLException(mysqlEx.getMessage(), sqlState, mysqlEx.getErrorCode(), mysqlEx);
        }
        return new SQLException(e.getMessage(), e);
    }

    static SQLException wrap(RuntimeException e) {
        return new SQLException(e.getMessage(), e);
    }
}
