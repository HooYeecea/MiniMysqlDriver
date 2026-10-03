package com.minimysql.step9;

import com.minimysql.jdbc.MiniPreparedStatement;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * 第 9 步验证：服务器端预编译（COM_STMT_PREPARE / EXECUTE / CLOSE）+ 二进制行协议。
 *
 * 参数不再拼进 SQL 文本，而是按二进制发给服务器。
 */
public class Step9ServerPreparedStatement {

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

            try (PreparedStatement insert = conn.prepareStatement(
                    "INSERT INTO mini_driver_demo(name) VALUES (?)")) {
                MiniPreparedStatement mini = insert.unwrap(MiniPreparedStatement.class);
                System.out.println("PREPARE statement_id=" + mini.getStatementId()
                        + ", numParams=1（走 COM_STMT_PREPARE）");
                insert.setString(1, "step9-binary");
                System.out.println("INSERT affected=" + insert.executeUpdate());
            }

            try (PreparedStatement query = conn.prepareStatement(
                    "SELECT id, name FROM mini_driver_demo WHERE name = ?")) {
                MiniPreparedStatement mini = query.unwrap(MiniPreparedStatement.class);
                System.out.println("SELECT statement_id=" + mini.getStatementId()
                        + "（结果行为二进制协议）");
                query.setString(1, "step9-binary");
                try (ResultSet rs = query.executeQuery()) {
                    while (rs.next()) {
                        System.out.println("id=" + rs.getInt(1) + ", name=" + rs.getString(2));
                    }
                }
            }
        }
    }
}
