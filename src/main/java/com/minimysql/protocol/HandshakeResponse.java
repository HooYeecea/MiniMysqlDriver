package com.minimysql.protocol;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

/**
 * 构造客户端 Handshake Response（登录包）。
 */
public final class HandshakeResponse {

    private HandshakeResponse() {
    }

    public static byte[] build(Handshake handshake,
                               String username,
                               String password,
                               String database) {
        String plugin = handshake.authPluginName;
        if (plugin == null || plugin.isEmpty()) {
            plugin = "mysql_native_password";
        }

        // 客户端能力 = 自己支持的 ∩ 服务端支持的（不强制声称服务端没有的能力）
        int clientFlags = CapabilityFlags.CLIENT_BASIC & handshake.capabilityFlags;

        int required = CapabilityFlags.CLIENT_PROTOCOL_41
                | CapabilityFlags.CLIENT_SECURE_CONNECTION
                | CapabilityFlags.CLIENT_PLUGIN_AUTH;
        if ((clientFlags & required) != required) {
            throw new IllegalStateException(
                    "服务端缺少必要能力标志，capabilityFlags=0x"
                            + Integer.toHexString(handshake.capabilityFlags));
        }

        if (database != null && !database.isEmpty()) {
            if ((handshake.capabilityFlags & CapabilityFlags.CLIENT_CONNECT_WITH_DB) == 0) {
                throw new IllegalStateException("服务端不支持 CONNECT_WITH_DB，无法指定 database");
            }
            clientFlags |= CapabilityFlags.CLIENT_CONNECT_WITH_DB;
        } else {
            clientFlags &= ~CapabilityFlags.CLIENT_CONNECT_WITH_DB;
        }

        byte[] authResponse = PasswordEncryption.scramble(plugin, password, handshake.authPluginData);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        writeUint32(out, clientFlags);
        writeUint32(out, 16 * 1024 * 1024); // max packet size: 16MB
        out.write(handshake.characterSet == 0 ? 33 : handshake.characterSet); // 33 = utf8mb3_general_ci，常用
        // reserved 23 bytes
        out.write(new byte[23], 0, 23);

        writeNullTerminated(out, username);
        writeAuthResponse(out, clientFlags, authResponse);

        if ((clientFlags & CapabilityFlags.CLIENT_CONNECT_WITH_DB) != 0) {
            writeNullTerminated(out, database);
        }

        writeNullTerminated(out, plugin);
        return out.toByteArray();
    }

    /**
     * 按协商到的能力写 auth-response：
     * - PLUGIN_AUTH_LENENC_CLIENT_DATA → length-encoded
     * - 否则 SECURE_CONNECTION → 1 字节长度 + 数据
     */
    private static void writeAuthResponse(ByteArrayOutputStream out, int clientFlags, byte[] authResponse) {
        if ((clientFlags & CapabilityFlags.CLIENT_PLUGIN_AUTH_LENENC_CLIENT_DATA) != 0) {
            writeLengthEncodedString(out, authResponse);
        } else {
            out.write(authResponse.length & 0xFF);
            out.write(authResponse, 0, authResponse.length);
        }
    }

    private static void writeUint32(ByteArrayOutputStream out, int value) {
        out.write(value & 0xFF);
        out.write((value >> 8) & 0xFF);
        out.write((value >> 16) & 0xFF);
        out.write((value >> 24) & 0xFF);
    }

    private static void writeNullTerminated(ByteArrayOutputStream out, String s) {
        if (s != null) {
            byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
            out.write(bytes, 0, bytes.length);
        }
        out.write(0);
    }

    private static void writeLengthEncodedString(ByteArrayOutputStream out, byte[] data) {
        writeLengthEncodedInt(out, data.length);
        out.write(data, 0, data.length);
    }

    private static void writeLengthEncodedInt(ByteArrayOutputStream out, int value) {
        if (value < 251) {
            out.write(value);
        } else if (value < 65536) {
            out.write(0xFC);
            out.write(value & 0xFF);
            out.write((value >> 8) & 0xFF);
        } else {
            out.write(0xFD);
            out.write(value & 0xFF);
            out.write((value >> 8) & 0xFF);
            out.write((value >> 16) & 0xFF);
        }
    }
}
