package com.minimysql.protocol;

import java.nio.charset.StandardCharsets;

/**
 * 在 payload 字节数组上按游标读取字段（小端 / length-encoded）。
 */
public final class PacketReader {

    private final byte[] data;
    private int pos;

    public PacketReader(byte[] data) {
        this.data = data;
        this.pos = 0;
    }

    public int position() {
        return pos;
    }

    public int remaining() {
        return data.length - pos;
    }

    public boolean hasRemaining() {
        return pos < data.length;
    }

    public int readUint8() {
        require(1);
        return data[pos++] & 0xFF;
    }

    public int readUint16() {
        require(2);
        int v = (data[pos] & 0xFF) | ((data[pos + 1] & 0xFF) << 8);
        pos += 2;
        return v;
    }

    public long readLengthEncodedInt() {
        require(1);
        int first = data[pos++] & 0xFF;
        if (first < 251) {
            return first;
        }
        if (first == 0xFC) {
            return readUint16();
        }
        if (first == 0xFD) {
            require(3);
            long v = (data[pos] & 0xFFL)
                    | ((data[pos + 1] & 0xFFL) << 8)
                    | ((data[pos + 2] & 0xFFL) << 16);
            pos += 3;
            return v;
        }
        if (first == 0xFE) {
            require(8);
            long v = (data[pos] & 0xFFL)
                    | ((data[pos + 1] & 0xFFL) << 8)
                    | ((data[pos + 2] & 0xFFL) << 16)
                    | ((data[pos + 3] & 0xFFL) << 24)
                    | ((data[pos + 4] & 0xFFL) << 32)
                    | ((data[pos + 5] & 0xFFL) << 40)
                    | ((data[pos + 6] & 0xFFL) << 48)
                    | ((data[pos + 7] & 0xFFL) << 56);
            pos += 8;
            return v;
        }
        throw new IllegalArgumentException("非法 length-encoded 整数前缀: 0x" + Integer.toHexString(first));
    }

    public byte[] readLengthEncodedBytes() {
        long len = readLengthEncodedInt();
        if (len < 0 || len > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("length-encoded 字节过长: " + len);
        }
        int n = (int) len;
        require(n);
        byte[] out = new byte[n];
        System.arraycopy(data, pos, out, 0, n);
        pos += n;
        return out;
    }

    public String readRestAsString() {
        String s = new String(data, pos, data.length - pos, StandardCharsets.UTF_8);
        pos = data.length;
        return s;
    }

    private void require(int n) {
        if (pos + n > data.length) {
            throw new IllegalArgumentException(
                    "包数据不足: need=" + n + ", remaining=" + remaining());
        }
    }
}
