package com.minimysql.step5;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * 第 5 步验证：通过标准 JDBC API 使用迷你驱动。
 *
 * 只需保证 MiniDriver 在 classpath 上（SPI 会自动注册），
 * 也可以显式 Class.forName("com.minimysql.jdbc.MiniDriver")。
 */
public class Step5JdbcApi {

    private static final String URL = "jdbc:mysql://127.0.0.1:3306/test";
    private static final String USER = "root";
    private static final String PASSWORD = "root";

    public static void main(String[] args) throws Exception {
        // 触发静态注册；有 SPI 时其实可不写
        Class.forName("com.minimysql.jdbc.MiniDriver");

        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             Statement stmt = conn.createStatement()) {

            stmt.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS mini_driver_demo ("
                            + "id INT PRIMARY KEY AUTO_INCREMENT,"
                            + "name VARCHAR(50) NOT NULL)"
            );
            stmt.executeUpdate("INSERT INTO mini_driver_demo(name) VALUES ('step5-jdbc')");

            try (ResultSet rs = stmt.executeQuery(
                    "SELECT id, name FROM mini_driver_demo ORDER BY id")) {
                while (rs.next()) {
                    System.out.println("id=" + rs.getInt("id")
                            + ", name=" + rs.getString("name"));
                }
            }
        }
    }
}
