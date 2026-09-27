package com.minimysql.jdbc;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * JDBC Driver 入口。可通过 SPI 或 Class.forName 注册。
 *
 * URL 示例：jdbc:mysql://127.0.0.1:3306/test
 */
public class MiniDriver implements Driver {

    static {
        try {
            DriverManager.registerDriver(new MiniDriver());
        } catch (SQLException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    @Override
    public Connection connect(String url, Properties info) throws SQLException {
        if (!acceptsURL(url)) {
            return null; // DriverManager 约定：不支持则返回 null
        }
        ConnectionUrl parsed = ConnectionUrl.parse(url);
        String user = info == null ? null : info.getProperty("user");
        String password = info == null ? null : info.getProperty("password");
        if (user == null) {
            user = "";
        }
        if (password == null) {
            password = "";
        }
        return MiniConnection.open(parsed, user, password);
    }

    @Override
    public boolean acceptsURL(String url) {
        return ConnectionUrl.accepts(url);
    }

    @Override
    public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) {
        return new DriverPropertyInfo[0];
    }

    @Override
    public int getMajorVersion() {
        return 1;
    }

    @Override
    public int getMinorVersion() {
        return 0;
    }

    @Override
    public boolean jdbcCompliant() {
        return false;
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        throw new SQLFeatureNotSupportedException("getParentLogger");
    }
}
