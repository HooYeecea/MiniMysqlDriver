package com.minimysql.protocol;

import java.nio.charset.StandardCharsets;

/**
 * ERR Packet：命令执行失败时的回包。
 *
 * 格式（PROTOCOL_41）：
 *   0xFF
 *   error_code (2)
 *   '#' + sql_state (5)   可选但常见
 *   message
 */
public final class ErrPacket {

    public final int errorCode;
    public final String sqlState;
    public final String message;

    private ErrPacket(int errorCode, String sqlState, String message) {
        this.errorCode = errorCode;
        this.sqlState = sqlState;
        this.message = message;
    }

    public static boolean isErr(byte[] payload) {
        return payload != null && payload.length > 0 && (payload[0] & 0xFF) == 0xFF;
    }

    public static ErrPacket parse(byte[] payload) {
        if (!isErr(payload)) {
            throw new IllegalArgumentException("不是 ERR Packet");
        }
        if (payload.length < 3) {
            throw new IllegalArgumentException("ERR 包过短: length=" + payload.length);
        }
        int errorCode = (payload[1] & 0xFF) | ((payload[2] & 0xFF) << 8);
        int pos = 3;
        String sqlState = null;
        if (pos < payload.length && payload[pos] == '#') {
            if (pos + 6 > payload.length) {
                throw new IllegalArgumentException("ERR 包 sqlState 字段不完整");
            }
            sqlState = new String(payload, pos + 1, 5, StandardCharsets.US_ASCII);
            pos += 6;
        }
        String message = new String(payload, pos, payload.length - pos, StandardCharsets.UTF_8);
        return new ErrPacket(errorCode, sqlState, message);
    }

    @Override
    public String toString() {
        return "ErrPacket{errorCode=" + errorCode
                + ", sqlState='" + sqlState + '\''
                + ", message='" + message + "'}";
    }
}
