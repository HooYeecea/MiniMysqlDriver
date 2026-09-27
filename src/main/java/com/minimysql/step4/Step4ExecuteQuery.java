package com.minimysql.step4;

import com.minimysql.protocol.ColumnDefinition;
import com.minimysql.protocol.MysqlSession;
import com.minimysql.protocol.QueryResult;

/**
 * 第 4 步验证：登录后 SELECT，解析列定义与文本行数据。
 *
 * 依赖第 3 步建过的表 mini_driver_demo；若没有会先插入一行。
 */
public class Step4ExecuteQuery {

    private static final String HOST = "127.0.0.1";
    private static final int PORT = 3306;
    private static final String USER = "root";
    private static final String PASSWORD = "root";
    private static final String DATABASE = "test";

    public static void main(String[] args) throws Exception {
        try (MysqlSession session = MysqlSession.connect(HOST, PORT, USER, PASSWORD, DATABASE)) {
            System.out.println("登录成功，server=" + session.handshake().serverVersion);

            session.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS mini_driver_demo ("
                            + "id INT PRIMARY KEY AUTO_INCREMENT,"
                            + "name VARCHAR(50) NOT NULL)"
            );
            session.executeUpdate("INSERT INTO mini_driver_demo(name) VALUES ('step4')");

            QueryResult result = session.executeQuery(
                    "SELECT id, name FROM mini_driver_demo ORDER BY id"
            );

            System.out.println("列数=" + result.columnCount() + ", 行数=" + result.rowCount());
            System.out.print("列: ");
            for (ColumnDefinition col : result.columns) {
                System.out.print(col + " ");
            }
            System.out.println();

            for (String[] row : result.rows) {
                System.out.println("row: id=" + row[0] + ", name=" + row[1]);
            }
        }
    }
}
