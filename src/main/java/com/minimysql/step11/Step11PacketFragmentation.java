package com.minimysql.step11;

import com.minimysql.protocol.PacketIO;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;

/**
 * 第 11 步验证：逻辑 payload 超过单包上限时的拆包 / 拼包。
 *
 * 不必连 MySQL。把单包上限缩小到 8 字节，便于观察物理包个数。
 *
 * 规则：
 * - 每包最多 max 字节
 * - 若逻辑长度恰好是 max 的整数倍，末尾再发一个长度为 0 的包
 */
public class Step11PacketFragmentation {

    private static final int FAKE_MAX = 8;

    public static void main(String[] args) throws Exception {
        roundTrip("短包", bytes(7), 1);
        roundTrip("刚好 1 个满包 + 空结束包", bytes(8), 2);
        roundTrip("两满包 + 余数", bytes(20), 3);
        roundTrip("空 payload", new byte[0], 1);
        System.out.println("全部 round-trip 成功");
    }

    private static void roundTrip(String title, byte[] payload, int expectedPhysicalPackets)
            throws Exception {
        ByteArrayOutputStream framed = new ByteArrayOutputStream();
        try (PacketIO writer = new PacketIO(InputStreamNull.INSTANCE, framed, FAKE_MAX)) {
            writer.writePacketPayload(payload);
        }

        byte[] wire = framed.toByteArray();
        int physical = countPhysicalPackets(wire);
        System.out.println(title
                + ": logical=" + payload.length
                + ", wire=" + wire.length
                + ", physicalPackets=" + physical);

        if (physical != expectedPhysicalPackets) {
            throw new IllegalStateException(
                    "物理包数量不符: 期望 " + expectedPhysicalPackets + ", 实际 " + physical);
        }

        try (PacketIO reader = new PacketIO(new ByteArrayInputStream(wire), OutputStreamNull.INSTANCE, FAKE_MAX)) {
            byte[] restored = reader.readPacketPayload();
            if (!Arrays.equals(payload, restored)) {
                throw new IllegalStateException("拼包后内容不一致: " + title);
            }
        }
    }

    private static int countPhysicalPackets(byte[] wire) {
        int n = 0;
        int pos = 0;
        while (pos + 4 <= wire.length) {
            int len = (wire[pos] & 0xFF)
                    | ((wire[pos + 1] & 0xFF) << 8)
                    | ((wire[pos + 2] & 0xFF) << 16);
            pos += 4 + len;
            n++;
        }
        if (pos != wire.length) {
            throw new IllegalStateException("帧边界错误: pos=" + pos + ", length=" + wire.length);
        }
        return n;
    }

    private static byte[] bytes(int n) {
        byte[] data = new byte[n];
        for (int i = 0; i < n; i++) {
            data[i] = (byte) (i + 1);
        }
        return data;
    }

    /** 写侧不读；读侧不写。 */
    private static final class InputStreamNull extends java.io.InputStream {
        static final InputStreamNull INSTANCE = new InputStreamNull();

        @Override
        public int read() {
            return -1;
        }
    }

    private static final class OutputStreamNull extends java.io.OutputStream {
        static final OutputStreamNull INSTANCE = new OutputStreamNull();

        @Override
        public void write(int b) {
            // ignore
        }
    }
}
