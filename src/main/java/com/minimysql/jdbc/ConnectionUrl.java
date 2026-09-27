package com.minimysql.jdbc;

import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 解析 jdbc:mysql://host:port/database
 */
final class ConnectionUrl {

    private static final Pattern URL_PATTERN = Pattern.compile(
            "^jdbc:mysql://([^/:?]+)(?::(\\d+))?(?:/([^?]*))?(?:\\?.*)?$",
            Pattern.CASE_INSENSITIVE
    );

    final String host;
    final int port;
    final String database;

    private ConnectionUrl(String host, int port, String database) {
        this.host = host;
        this.port = port;
        this.database = database;
    }

    static boolean accepts(String url) {
        return url != null && url.toLowerCase().startsWith("jdbc:mysql:");
    }

    static ConnectionUrl parse(String url) throws SQLException {
        if (!accepts(url)) {
            throw new SQLException("不支持的 URL: " + url);
        }
        Matcher m = URL_PATTERN.matcher(url);
        if (!m.matches()) {
            throw new SQLException("无法解析 URL: " + url
                    + "，期望格式 jdbc:mysql://host:port/database");
        }
        String host = m.group(1);
        int port = m.group(2) == null ? 3306 : Integer.parseInt(m.group(2));
        String database = m.group(3) == null ? "" : m.group(3);
        return new ConnectionUrl(host, port, database);
    }

    static SQLException unsupported(String method) {
        return new SQLFeatureNotSupportedException("MiniMySQLDriver 暂不支持: " + method);
    }
}
