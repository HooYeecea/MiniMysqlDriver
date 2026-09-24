package com.minimysql.protocol;

import java.io.IOException;

/**
 * 已完成握手+认证的协议会话（还不是 JDBC Connection）。
 * 方便第 3 步及后续在「已登录」状态下发命令。
 */
public final class MysqlSession implements AutoCloseable {

    private final PacketIO io;
    private final Handshake handshake;

    private MysqlSession(PacketIO io, Handshake handshake) {
        this.io = io;
        this.handshake = handshake;
    }

    public static MysqlSession connect(String host,
                                       int port,
                                       String user,
                                       String password,
                                       String database) throws IOException {
        PacketIO io = new PacketIO(host, port);
        try {
            Handshake handshake = Handshake.parse(io.readPacketPayload());
            Authenticator.authenticate(io, handshake, user, password, database);
            return new MysqlSession(io, handshake);
        } catch (IOException | RuntimeException e) {
            try {
                io.close();
            } catch (IOException ignored) {
                // 保留原始异常
            }
            throw e;
        }
    }

    public Handshake handshake() {
        return handshake;
    }

    public PacketIO io() {
        return io;
    }

    /** 执行无结果集 SQL。 */
    public OkPacket executeUpdate(String sql) throws IOException {
        return ComQuery.executeUpdate(io, sql);
    }

    @Override
    public void close() throws IOException {
        // COM_QUIT = 0x01，优雅断开；失败也无所谓，最终关 Socket
        try {
            io.setSequenceId(0);
            io.writePacketPayload(new byte[]{0x01});
        } catch (IOException ignored) {
            // ignore
        }
        io.close();
    }
}
