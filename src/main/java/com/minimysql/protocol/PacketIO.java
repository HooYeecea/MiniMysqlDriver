package com.minimysql.protocol;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

/**
 * MySQL 协议最底层：按「包」读写字节。
 *
 * 每个 MySQL Packet 格式：
 *   [3 bytes] payload 长度（小端）
 *   [1 byte ] 序号 sequenceId
 *   [N bytes] payload
 */
public class PacketIO implements AutoCloseable {

    private final Socket socket;
    private final InputStream in;
    private final OutputStream out;

    /** 当前要发送/期望接收的序号，握手阶段从 0 开始。 */
    private int sequenceId;

    public PacketIO(String host, int port) throws IOException {
        this.socket = new Socket(host, port);
        this.in = socket.getInputStream();
        this.out = socket.getOutputStream();
        this.sequenceId = 0;
    }

    public int getSequenceId() {
        return sequenceId;
    }

    public void setSequenceId(int sequenceId) {
        this.sequenceId = sequenceId & 0xFF;
    }

    /**
     * 读下一个完整 Packet 的 payload（不含 4 字节头）。
     * 读完后 sequenceId 更新为「刚读到的序号 + 1」，方便接着发下一个包。
     */
    public byte[] readPacketPayload() throws IOException {
        byte[] header = readFully(4);
        int length = (header[0] & 0xFF)
                | ((header[1] & 0xFF) << 8)
                | ((header[2] & 0xFF) << 16);
        int packetSeq = header[3] & 0xFF;

        byte[] payload = readFully(length);
        // 下一包序号 = 当前包序号 + 1
        this.sequenceId = (packetSeq + 1) & 0xFF;
        return payload;
    }

    /**
     * 发送一个 Packet：自动带上当前 sequenceId，发完后序号 +1。
     */
    public void writePacketPayload(byte[] payload) throws IOException {
        int length = payload.length;
        byte[] header = new byte[4];
        header[0] = (byte) (length & 0xFF);
        header[1] = (byte) ((length >> 8) & 0xFF);
        header[2] = (byte) ((length >> 16) & 0xFF);
        header[3] = (byte) (sequenceId & 0xFF);

        out.write(header);
        out.write(payload);
        out.flush();

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
        socket.close();
    }
}
