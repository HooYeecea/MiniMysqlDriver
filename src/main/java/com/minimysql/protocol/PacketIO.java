package com.minimysql.protocol;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

/**
 * MySQL 协议最底层：按「包」读写字节。
 *
 * 每个物理 Packet：
 *   [3 bytes] payload 长度（小端）
 *   [1 byte ] 序号 sequenceId
 *   [N bytes] payload
 *
 * 单个物理包 payload 上限为 {@link #MAX_PAYLOAD_SIZE}（0xFFFFFF，约 16MB）。
 * 更大的逻辑 payload 会拆成多个物理包；若长度恰好是上限的整数倍，末尾再跟一个长度为 0 的包。
 */
public class PacketIO implements AutoCloseable {

    /** MySQL 单包 payload 上限（3 字节长度字段）。 */
    public static final int MAX_PAYLOAD_SIZE = 0xFFFFFF;

    private final Socket socket;
    private final InputStream in;
    private final OutputStream out;
    private final int maxPayloadSize;

    /** 当前要发送/期望接收的序号，握手阶段从 0 开始。 */
    private int sequenceId;

    public PacketIO(String host, int port) throws IOException {
        this.socket = new Socket(host, port);
        this.in = socket.getInputStream();
        this.out = socket.getOutputStream();
        this.maxPayloadSize = MAX_PAYLOAD_SIZE;
        this.sequenceId = 0;
    }

    /** 便于单测 / 演示：对任意流做拆包拼包。 */
    public PacketIO(InputStream in, OutputStream out) {
        this(in, out, MAX_PAYLOAD_SIZE);
    }

    public PacketIO(InputStream in, OutputStream out, int maxPayloadSize) {
        if (maxPayloadSize <= 0 || maxPayloadSize > MAX_PAYLOAD_SIZE) {
            throw new IllegalArgumentException("maxPayloadSize 必须在 1.." + MAX_PAYLOAD_SIZE);
        }
        this.socket = null;
        this.in = in;
        this.out = out;
        this.maxPayloadSize = maxPayloadSize;
        this.sequenceId = 0;
    }

    public int getSequenceId() {
        return sequenceId;
    }

    public void setSequenceId(int sequenceId) {
        this.sequenceId = sequenceId & 0xFF;
    }

    /**
     * 读下一个逻辑 Packet 的完整 payload。
     * 若连续物理包长度为上限，会一直拼到出现一个更短的包（可能长度为 0）。
     */
    public byte[] readPacketPayload() throws IOException {
        PhysicalPacket first = readPhysical();
        if (first.length < maxPayloadSize) {
            this.sequenceId = (first.sequenceId + 1) & 0xFF;
            return first.payload;
        }

        ByteArrayOutputStream acc = new ByteArrayOutputStream();
        acc.write(first.payload);
        int lastSeq = first.sequenceId;
        int length = first.length;
        while (length == maxPayloadSize) {
            PhysicalPacket next = readPhysical();
            acc.write(next.payload);
            lastSeq = next.sequenceId;
            length = next.length;
        }
        this.sequenceId = (lastSeq + 1) & 0xFF;
        return acc.toByteArray();
    }

    /**
     * 发送一个逻辑 Packet：必要时拆成多个物理包，每个包 sequenceId 递增。
     */
    public void writePacketPayload(byte[] payload) throws IOException {
        if (payload == null) {
            throw new IllegalArgumentException("payload 不能为 null");
        }
        int offset = 0;
        int remaining = payload.length;
        while (remaining >= maxPayloadSize) {
            writePhysical(payload, offset, maxPayloadSize);
            offset += maxPayloadSize;
            remaining -= maxPayloadSize;
        }
        writePhysical(payload, offset, remaining);
        out.flush();
    }

    private PhysicalPacket readPhysical() throws IOException {
        byte[] header = readFully(4);
        int length = (header[0] & 0xFF)
                | ((header[1] & 0xFF) << 8)
                | ((header[2] & 0xFF) << 16);
        int packetSeq = header[3] & 0xFF;
        if (length < 0 || length > maxPayloadSize) {
            throw new IOException("非法包长度: " + length);
        }
        byte[] payload = length == 0 ? new byte[0] : readFully(length);
        return new PhysicalPacket(length, packetSeq, payload);
    }

    private void writePhysical(byte[] payload, int offset, int length) throws IOException {
        byte[] header = new byte[4];
        header[0] = (byte) (length & 0xFF);
        header[1] = (byte) ((length >> 8) & 0xFF);
        header[2] = (byte) ((length >> 16) & 0xFF);
        header[3] = (byte) (sequenceId & 0xFF);
        out.write(header);
        if (length > 0) {
            out.write(payload, offset, length);
        }
        sequenceId = (sequenceId + 1) & 0xFF;
    }

    private byte[] readFully(int len) throws IOException {
        byte[] buf = new byte[len];
        int off = 0;
        while (off < len) {
            int n = in.read(buf, off, len - off);
            if (n < 0) {
                throw new IOException("连接已关闭，期望再读 " + (len - off) + " 字节");
            }
            off += n;
        }
        return buf;
    }

    @Override
    public void close() throws IOException {
        if (socket != null) {
            socket.close();
            return;
        }
        IOException first = null;
        try {
            in.close();
        } catch (IOException e) {
            first = e;
        }
        try {
            out.close();
        } catch (IOException e) {
            if (first == null) {
                first = e;
            } else {
                first.addSuppressed(e);
            }
        }
        if (first != null) {
            throw first;
        }
    }

    private record PhysicalPacket(int length, int sequenceId, byte[] payload) {
    }
}
