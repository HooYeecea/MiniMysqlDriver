package com.minimysql.step7;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * 第 7 步验证：PreparedStatement（客户端绑定 ? 后走 COM_QUERY）。
 */
public class Step7PreparedStatement {

    private static final String URL = "jdbc:mysql://127.0.0.1:3306/test";
    private static final String USER = "root";
    private static final String PASSWORD = "root";

    public static void main(String[] args) throws Exception {
        Class.forName("com.minimysql.jdbc.MiniDriver");

        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD)) {
            try (PreparedStatement create = conn.prepareStatement(
                    "CREATE TABLE IF NOT EXISTS mini_driver_demo ("
                            + "id INT PRIMARY KEY AUTO_INCREMENT,"
                            + "name VARCHAR(50) NOT NULL)")) {
                create.executeUpdate();
            }

            try (PreparedStatement insert = conn.prepareStatement(
                    "INSERT INTO mini_driver_demo(name) VALUES (?)")) {
                insert.setString(1, "step7-prep");
                int n = insert.executeUpdate();
                System.out.println("insert affected=" + n);
            }

            try (PreparedStatement query = conn.prepareStatement(
                    "SELECT id, name FROM mini_driver_demo WHERE name = ?")) {
                query.setString(1, "step7-prep");
                try (ResultSet rs = query.executeQuery()) {
                    while (rs.next()) {
                        System.out.println("id=" + rs.getInt(1) + ", name=" + rs.getString(2));
                    }
                }
            }
        }
    }
}
