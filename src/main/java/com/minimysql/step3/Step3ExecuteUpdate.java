package com.minimysql.step3;

import com.minimysql.protocol.MysqlSession;
import com.minimysql.protocol.OkPacket;

/**
 * 第 3 步验证：登录后发 COM_QUERY，执行无结果集 SQL，解析 OK Packet。
 *
 * 请改成你的账号密码；并确保 DATABASE 已存在。
 */
public class Step3ExecuteUpdate {

    private static final String HOST = "127.0.0.1";
    private static final int PORT = 3306;
    private static final String USER = "root";
    private static final String PASSWORD = "root";
    private static final String DATABASE = "test";

    public static void main(String[] args) throws Exception {
        try (MysqlSession session = MysqlSession.connect(HOST, PORT, USER, PASSWORD, DATABASE)) {
            System.out.println("登录成功，server=" + session.handshake().serverVersion);

            // 建一张演示表（已存在会失败也没关系，可先手动建）
            OkPacket create = session.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS mini_driver_demo ("
                            + "id INT PRIMARY KEY AUTO_INCREMENT,"
                            + "name VARCHAR(50) NOT NULL)"
            );
            System.out.println("CREATE: " + create);

            OkPacket insert = session.executeUpdate(
                    "INSERT INTO mini_driver_demo(name) VALUES ('step3')"
            );
            System.out.println("INSERT: " + insert);
            System.out.println("affectedRows=" + insert.affectedRows
                    + ", lastInsertId=" + insert.lastInsertId);
        }
    }
}
