package com.minimysql.protocol;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * COM_QUERY：发送一条 SQL。
 *
 * - {@link #executeUpdate}：无结果集（OK / ERR）
 * - {@link #executeQuery}：有结果集（列定义 + 行）
 */
public final class ComQuery {

    public static final byte COM_QUERY = 0x03;

    private ComQuery() {
    }

    /**
     * 执行无结果集 SQL，返回 OK Packet。
     * 每条命令前会把 sequenceId 重置为 0（协议要求）。
     */
    public static OkPacket executeUpdate(PacketIO io, String sql) throws IOException {
        byte[] response = sendQuery(io, sql);
        if (ErrPacket.isErr(response)) {
            throw ErrPacket.parse(response).toException();
        }
        if (response.length > 0 && (response[0] & 0xFF) == 0x00) {
            return OkPacket.parse(response);
        }

        throw new IOException(
                "收到结果集响应，请改用 executeQuery。"
                        + " 首字节=0x" + Integer.toHexString(response[0] & 0xFF));
    }

    /**
     * 执行 SELECT 等有结果集的 SQL。
     */
    public static QueryResult executeQuery(PacketIO io, String sql) throws IOException {
        byte[] response = sendQuery(io, sql);
        return ResultSetDecoder.decode(io, response);
    }

    private static byte[] sendQuery(PacketIO io, String sql) throws IOException {
        if (sql == null || sql.isEmpty()) {
            throw new IllegalArgumentException("SQL 不能为空");
        }

        io.setSequenceId(0);

        byte[] sqlBytes = sql.getBytes(StandardCharsets.UTF_8);
        byte[] payload = new byte[1 + sqlBytes.length];
        payload[0] = COM_QUERY;
        System.arraycopy(sqlBytes, 0, payload, 1, sqlBytes.length);
        io.writePacketPayload(payload);

        return io.readPacketPayload();
    }
}
