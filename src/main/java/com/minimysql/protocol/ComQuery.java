package com.minimysql.protocol;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * COM_QUERY：发送一条 SQL。
 *
 * 本步只处理「无结果集」响应（OK / ERR）。
 * SELECT 等会返回结果集，留给第 4 步。
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
        if (sql == null || sql.isEmpty()) {
            throw new IllegalArgumentException("SQL 不能为空");
        }

        io.setSequenceId(0);

        byte[] sqlBytes = sql.getBytes(StandardCharsets.UTF_8);
        byte[] payload = new byte[1 + sqlBytes.length];
        payload[0] = COM_QUERY;
        System.arraycopy(sqlBytes, 0, payload, 1, sqlBytes.length);
        io.writePacketPayload(payload);

        byte[] response = io.readPacketPayload();
        if (ErrPacket.isErr(response)) {
            ErrPacket err = ErrPacket.parse(response);
            throw new IOException("SQL 执行失败 [" + err.errorCode + "] "
                    + err.sqlState + " " + err.message);
        }
        if (response.length > 0 && (response[0] & 0xFF) == 0x00) {
            return OkPacket.parse(response);
        }

        // 结果集通常以列数开头（length-encoded，且不是 0x00/0xFF）
        throw new IOException(
                "收到结果集响应，本步只支持无结果集 SQL（INSERT/UPDATE/DELETE/DDL）。"
                        + " 若要 SELECT，请等第 4 步。首字节=0x"
                        + Integer.toHexString(response[0] & 0xFF));
    }
}
