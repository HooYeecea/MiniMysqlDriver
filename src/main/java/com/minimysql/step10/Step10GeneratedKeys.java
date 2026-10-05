package com.minimysql.step10;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * 第 10 步验证：INSERT 后 getGeneratedKeys() 读取 OK Packet 的 last_insert_id。
 */
public class Step10GeneratedKeys {

    private static final String URL = "jdbc:mysql://127.0.0.1:3306/test";
    private static final String USER = "root";
    private static final String PASSWORD = "root";

    public static void main(String[] args) throws Exception {
        Class.forName("com.minimysql.jdbc.MiniDriver");

        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             Statement stmt = conn.createStatement()) {

            stmt.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS mini_driver_demo ("
                            + "id INT PRIMARY KEY AUTO_INCREMENT,"
                            + "name VARCHAR(50) NOT NULL)"
            );

            stmt.executeUpdate(
                    "INSERT INTO mini_driver_demo(name) VALUES ('step10-stmt')",
                    Statement.RETURN_GENERATED_KEYS
            );
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    System.out.println("Statement GENERATED_KEY=" + keys.getLong(1));
                }
            }

            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO mini_driver_demo(name) VALUES (?)",
                    Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, "step10-prep");
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        System.out.println("PreparedStatement GENERATED_KEY=" + keys.getLong(1));
                    }
                }
            }
        }
    }
}
