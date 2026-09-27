package com.minimysql.step6;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * 第 6 步验证：故意执行错误 SQL，观察 SQLException 是否带上
 * MySQL errorCode 与 SQLState。
 */
public class Step6SqlException {

    private static final String URL = "jdbc:mysql://127.0.0.1:3306/test";
    private static final String USER = "root";
    private static final String PASSWORD = "root";

    public static void main(String[] args) throws Exception {
        Class.forName("com.minimysql.jdbc.MiniDriver");

        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             Statement stmt = conn.createStatement()) {

            try {
                stmt.executeQuery("SELECT * FROM table_does_not_exist_xyz");
                System.out.println("未抛异常，不符合预期");
            } catch (SQLException e) {
                System.out.println("捕获 SQLException（符合预期）");
                System.out.println("  message   = " + e.getMessage());
                System.out.println("  SQLState  = " + e.getSQLState());
                System.out.println("  errorCode = " + e.getErrorCode());
                // 常见：表不存在 → errorCode=1146, SQLState=42S02
            }
        }
    }
}
