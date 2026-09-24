package com.minimysql.protocol;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * 认证阶段服务端回包的几种结果。
 *
 * 首字节含义：
 *   0x00 → OK（登录成功）
 *   0xFF → ERR（失败）
 *   0xFE → Auth Switch（换一种认证插件）
 *   0x01 → Auth More Data（caching_sha2 继续交互）
 */
public final class AuthResult {

    public enum Kind {
        OK,
        ERR,
        AUTH_SWITCH,
        AUTH_MORE_DATA
    }

    public final Kind kind;
    public final String message;
    public final int errorCode;
    public final String sqlState;
    public final String switchPlugin;
    public final byte[] switchScramble;
    public final byte[] moreData;

    private AuthResult(Kind kind,
                       String message,
                       int errorCode,
                       String sqlState,
                       String switchPlugin,
                       byte[] switchScramble,
                       byte[] moreData) {
        this.kind = kind;
        this.message = message;
        this.errorCode = errorCode;
        this.sqlState = sqlState;
        this.switchPlugin = switchPlugin;
        this.switchScramble = switchScramble;
        this.moreData = moreData;
    }

    public static AuthResult parse(byte[] payload) {
        if (payload.length == 0) {
            throw new IllegalArgumentException("空认证响应");
        }
        int header = payload[0] & 0xFF;

        if (header == 0x00) {
            return new AuthResult(Kind.OK, "OK", 0, null, null, null, null);
        }
        if (header == 0xFF) {
            int errorCode = (payload[1] & 0xFF) | ((payload[2] & 0xFF) << 8);
            int pos = 3;
            String sqlState = null;
            if (pos < payload.length && payload[pos] == '#') {
                sqlState = new String(payload, pos + 1, 5, StandardCharsets.US_ASCII);
                pos += 6;
            }
            String message = new String(payload, pos, payload.length - pos, StandardCharsets.UTF_8);
            return new AuthResult(Kind.ERR, message, errorCode, sqlState, null, null, null);
        }
        if (header == 0xFE) {
            // Auth Switch Request: 0xFE + plugin_name\0 + scramble\0
            int pos = 1;
            int nameEnd = indexOfNull(payload, pos);
            String plugin = new String(payload, pos, nameEnd - pos, StandardCharsets.US_ASCII);
            pos = nameEnd + 1;
            int scrambleEnd = indexOfNull(payload, pos);
            byte[] scramble = Arrays.copyOfRange(payload, pos, scrambleEnd);
            return new AuthResult(Kind.AUTH_SWITCH, null, 0, null, plugin, scramble, null);
        }
        if (header == 0x01) {
            byte[] more = Arrays.copyOfRange(payload, 1, payload.length);
            return new AuthResult(Kind.AUTH_MORE_DATA, null, 0, null, null, null, more);
        }
        throw new IllegalStateException("未知认证响应头: 0x" + Integer.toHexString(header));
    }

    private static int indexOfNull(byte[] data, int from) {
        for (int i = from; i < data.length; i++) {
            if (data[i] == 0) {
                return i;
            }
        }
        return data.length;
    }

    @Override
    public String toString() {
        return "AuthResult{kind=" + kind
                + ", errorCode=" + errorCode
                + ", sqlState=" + sqlState
                + ", message='" + message + '\''
                + ", switchPlugin='" + switchPlugin + '\''
                + '}';
    }
}
