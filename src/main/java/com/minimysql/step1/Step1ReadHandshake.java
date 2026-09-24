package com.minimysql.step1;

import com.minimysql.protocol.Handshake;
import com.minimysql.protocol.PacketIO;

/**
 * 第 1 步验证：TCP 连上 MySQL，读出并打印 Handshake。
 *
 * 运行前请确保本机 MySQL 已启动（默认 3306）。
 * 改下面的 HOST / PORT 即可。
 *
 * 本步故意不做登录，所以连上读完握手后会直接关闭。
 * 服务端可能会在日志里看到一次未完成的连接，这是正常的。
 */
public class Step1ReadHandshake {

    private static final String HOST = "127.0.0.1";
    private static final int PORT = 3306;

    public static void main(String[] args) throws Exception {
        System.out.println("正在连接 " + HOST + ":" + PORT + " ...");

        try (PacketIO io = new PacketIO(HOST, PORT)) {
            byte[] payload = io.readPacketPayload();
            Handshake handshake = Handshake.parse(payload);

            System.out.println("握手成功，解析结果：");
            System.out.println(handshake);
            System.out.println("scramble(hex) = " + toHex(handshake.authPluginData));
        }
    }

    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
