package com.minimysql.protocol;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * 解析服务端发来的 Initial Handshake Packet（协议版本 10）。
 *
 * 连上 MySQL 后，服务端主动发来的第一个包就是它。
 */
public class Handshake {

    public final int protocolVersion;
    public final String serverVersion;
    public final long connectionId;
    public final byte[] authPluginData;
    public final int capabilityFlags;
    public final int characterSet;
    public final int statusFlags;
    public final String authPluginName;

    private Handshake(int protocolVersion,
                      String serverVersion,
                      long connectionId,
                      byte[] authPluginData,
                      int capabilityFlags,
                      int characterSet,
                      int statusFlags,
                      String authPluginName) {
        this.protocolVersion = protocolVersion;
        this.serverVersion = serverVersion;
        this.connectionId = connectionId;
        this.authPluginData = authPluginData;
        this.capabilityFlags = capabilityFlags;
        this.characterSet = characterSet;
        this.statusFlags = statusFlags;
        this.authPluginName = authPluginName;
    }

    public static Handshake parse(byte[] payload) {
        int pos = 0;

        int protocolVersion = payload[pos++] & 0xFF;
        if (protocolVersion != 10) {
            throw new IllegalStateException("仅支持 Handshake V10，实际版本=" + protocolVersion);
        }

        // server version: 以 0x00 结尾的字符串
        int versionEnd = indexOfNull(payload, pos);
        String serverVersion = new String(payload, pos, versionEnd - pos, StandardCharsets.US_ASCII);
        pos = versionEnd + 1;

        // connection id: 4 bytes little-endian
        long connectionId = readUint32(payload, pos);
        pos += 4;

        // auth-plugin-data-part-1: 8 bytes
        byte[] part1 = Arrays.copyOfRange(payload, pos, pos + 8);
        pos += 8;

        // filler: 1 byte, 应为 0x00
        pos += 1;

        // capability flags (lower 2 bytes)
        int capabilityLower = readUint16(payload, pos);
        pos += 2;

        int characterSet = 0;
        int statusFlags = 0;
        int capabilityUpper = 0;
        int authPluginDataLen = 0;
        byte[] part2 = new byte[0];
        String authPluginName = "";

        // 后面字段在「还有剩余字节」时才存在（能力协商后的完整握手）
        if (pos < payload.length) {
            characterSet = payload[pos++] & 0xFF;
            statusFlags = readUint16(payload, pos);
            pos += 2;
            capabilityUpper = readUint16(payload, pos);
            pos += 2;
            authPluginDataLen = payload[pos++] & 0xFF;
            // reserved: 10 bytes
            pos += 10;

            // auth-plugin-data-part-2：长度为 max(13, authPluginDataLen - 8)
            int part2Len = Math.max(13, authPluginDataLen - 8);
            part2 = Arrays.copyOfRange(payload, pos, pos + part2Len);
            pos += part2Len;

            if (pos < payload.length) {
                int nameEnd = indexOfNull(payload, pos);
                authPluginName = new String(payload, pos, nameEnd - pos, StandardCharsets.US_ASCII);
            }
        }

        int capabilityFlags = capabilityLower | (capabilityUpper << 16);

        // scramble = part1 + part2（去掉末尾的 0x00）
        byte[] scramble = mergeScramble(part1, part2);

        return new Handshake(
                protocolVersion,
                serverVersion,
                connectionId,
                scramble,
                capabilityFlags,
                characterSet,
                statusFlags,
                authPluginName
        );
    }

    private static byte[] mergeScramble(byte[] part1, byte[] part2) {
        // part2 末尾通常带一个 0x00，拼 scramble 时要去掉
        int part2Useful = part2.length;
        while (part2Useful > 0 && part2[part2Useful - 1] == 0) {
            part2Useful--;
        }
        byte[] scramble = new byte[part1.length + part2Useful];
        System.arraycopy(part1, 0, scramble, 0, part1.length);
        System.arraycopy(part2, 0, scramble, part1.length, part2Useful);
        return scramble;
    }

    private static int indexOfNull(byte[] data, int from) {
        for (int i = from; i < data.length; i++) {
            if (data[i] == 0) {
                return i;
            }
        }
        throw new IllegalArgumentException("缺少 null 终止符，from=" + from);
    }

    private static int readUint16(byte[] data, int pos) {
        return (data[pos] & 0xFF) | ((data[pos + 1] & 0xFF) << 8);
    }

    private static long readUint32(byte[] data, int pos) {
        return (data[pos] & 0xFFL)
                | ((data[pos + 1] & 0xFFL) << 8)
                | ((data[pos + 2] & 0xFFL) << 16)
                | ((data[pos + 3] & 0xFFL) << 24);
    }

    @Override
    public String toString() {
        return "Handshake{" +
                "protocolVersion=" + protocolVersion +
                ", serverVersion='" + serverVersion + '\'' +
                ", connectionId=" + connectionId +
                ", capabilityFlags=0x" + Integer.toHexString(capabilityFlags) +
                ", characterSet=" + characterSet +
                ", statusFlags=0x" + Integer.toHexString(statusFlags) +
                ", authPluginName='" + authPluginName + '\'' +
                ", authPluginDataLen=" + authPluginData.length +
                '}';
    }
}
