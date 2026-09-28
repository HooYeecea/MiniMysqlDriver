package com.minimysql.step8;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * 第 8 步验证：事务 setAutoCommit / commit / rollback。
 *
 * 流程：
 * 1) 关闭自动提交
 * 2) 插入一行后 rollback → 应查不到
 * 3) 再插入一行后 commit → 应能查到
 */
public class Step8Transaction {

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
            // 清掉本演示用的标记行，避免干扰
            stmt.executeUpdate("DELETE FROM mini_driver_demo WHERE name LIKE 'step8-%'");

            conn.setAutoCommit(false);
            System.out.println("autoCommit=" + conn.getAutoCommit());

            try (PreparedStatement insert = conn.prepareStatement(
                    "INSERT INTO mini_driver_demo(name) VALUES (?)")) {
                insert.setString(1, "step8-rollback");
                insert.executeUpdate();
            }
            conn.rollback();
            System.out.println("rollback 后 count(step8-rollback)="
                    + count(conn, "step8-rollback"));

            try (PreparedStatement insert = conn.prepareStatement(
                    "INSERT INTO mini_driver_demo(name) VALUES (?)")) {
                insert.setString(1, "step8-commit");
                insert.executeUpdate();
            }
            conn.commit();
            System.out.println("commit 后 count(step8-commit)="
                    + count(conn, "step8-commit"));

            conn.setAutoCommit(true);
            System.out.println("恢复 autoCommit=" + conn.getAutoCommit());
        }
    }

    private static int count(Connection conn, String name) throws Exception {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT COUNT(*) FROM mini_driver_demo WHERE name = ?")) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }
}
