package com.minimysql.protocol;

import java.io.ByteArrayOutputStream;

/**
 * 组装 MySQL 协议 payload（小端 / length-encoded）。
 */
public final class PacketWriter {

    private final ByteArrayOutputStream out = new ByteArrayOutputStream();

    public void writeUint8(int value) {
        out.write(value & 0xFF);
    }

    public void writeUint16(int value) {
        out.write(value & 0xFF);
        out.write((value >> 8) & 0xFF);
    }

    public void writeUint32(long value) {
        out.write((int) (value & 0xFF));
        out.write((int) ((value >> 8) & 0xFF));
        out.write((int) ((value >> 16) & 0xFF));
        out.write((int) ((value >> 24) & 0xFF));
    }

    public void writeInt64(long value) {
        for (int i = 0; i < 8; i++) {
            out.write((int) ((value >> (8 * i)) & 0xFF));
        }
    }

    public void writeBytes(byte[] bytes) {
        if (bytes.length > 0) {
            out.write(bytes, 0, bytes.length);
        }
    }

    public void writeLengthEncodedInt(long value) {
        if (value < 251) {
            writeUint8((int) value);
        } else if (value < 65536) {
            writeUint8(0xFC);
            writeUint16((int) value);
        } else if (value < 16777216) {
            writeUint8(0xFD);
            out.write((int) (value & 0xFF));
            out.write((int) ((value >> 8) & 0xFF));
            out.write((int) ((value >> 16) & 0xFF));
        } else {
            writeUint8(0xFE);
            writeInt64(value);
        }
    }

    public void writeLengthEncodedBytes(byte[] bytes) {
        writeLengthEncodedInt(bytes.length);
        writeBytes(bytes);
    }

    public byte[] toByteArray() {
        return out.toByteArray();
    }
}
