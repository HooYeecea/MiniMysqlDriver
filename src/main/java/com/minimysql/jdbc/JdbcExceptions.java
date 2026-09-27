package com.minimysql.jdbc;

import java.io.IOException;
import java.sql.SQLException;

final class JdbcExceptions {

    private JdbcExceptions() {
    }

    static SQLException wrap(IOException e) {
        return new SQLException(e.getMessage(), e);
    }

    static SQLException wrap(RuntimeException e) {
        return new SQLException(e.getMessage(), e);
    }
}
