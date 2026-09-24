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

        // 客户端能力 = 自己支持的 ∩ 服务端支持的
        int clientFlags = CapabilityFlags.CLIENT_BASIC & handshake.capabilityFlags;
        // 这几位必须由客户端主动声明
        clientFlags |= CapabilityFlags.CLIENT_PROTOCOL_41;
        clientFlags |= CapabilityFlags.CLIENT_SECURE_CONNECTION;
        clientFlags |= CapabilityFlags.CLIENT_PLUGIN_AUTH;
        clientFlags |= CapabilityFlags.CLIENT_PLUGIN_AUTH_LENENC_CLIENT_DATA;
        if (database != null && !database.isEmpty()) {
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
        writeLengthEncodedString(out, authResponse);

        if ((clientFlags & CapabilityFlags.CLIENT_CONNECT_WITH_DB) != 0) {
            writeNullTerminated(out, database);
        }

        writeNullTerminated(out, plugin);
        return out.toByteArray();
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
