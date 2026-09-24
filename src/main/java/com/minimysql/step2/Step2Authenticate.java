package com.minimysql.step2;

import com.minimysql.protocol.Authenticator;
import com.minimysql.protocol.Handshake;
import com.minimysql.protocol.PacketIO;

/**
 * 第 2 步验证：TCP 连接 → 读 Handshake → 发认证包 → 登录成功。
 *
 * 请改成你本机的账号密码；database 可先留空字符串。
 */
public class Step2Authenticate {

    private static final String HOST = "127.0.0.1";
    private static final int PORT = 3306;
    private static final String USER = "root";
    private static final String PASSWORD = "123456";
    private static final String DATABASE = "";

    public static void main(String[] args) throws Exception {
        System.out.println("正在连接 " + HOST + ":" + PORT + " ...");

        try (PacketIO io = new PacketIO(HOST, PORT)) {
            byte[] payload = io.readPacketPayload();
            Handshake handshake = Handshake.parse(payload);
            System.out.println("收到握手: " + handshake);

            System.out.println("正在认证用户: " + USER + " , 插件: " + handshake.authPluginName);
            Authenticator.authenticate(io, handshake, USER, PASSWORD, DATABASE);

            System.out.println("登录成功！");
        }
    }
}
