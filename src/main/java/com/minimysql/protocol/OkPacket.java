package com.minimysql.protocol;

/**
 * OK Packet：无结果集 SQL（INSERT/UPDATE/DELETE/DDL 等）成功时的回包。
 *
 * 格式（PROTOCOL_41）：
 *   0x00
 *   affected_rows   (length-encoded)
 *   last_insert_id  (length-encoded)
 *   status_flags    (2 bytes)
 *   warnings        (2 bytes)
 *   info            (剩余字节，可选)
 */
public final class OkPacket {

    public final long affectedRows;
    public final long lastInsertId;
    public final int statusFlags;
    public final int warnings;
    public final String info;

    private OkPacket(long affectedRows, long lastInsertId, int statusFlags, int warnings, String info) {
        this.affectedRows = affectedRows;
        this.lastInsertId = lastInsertId;
        this.statusFlags = statusFlags;
        this.warnings = warnings;
        this.info = info;
    }

    public static OkPacket parse(byte[] payload) {
        if (payload == null || payload.length < 1 || (payload[0] & 0xFF) != 0x00) {
            throw new IllegalArgumentException("不是 OK Packet");
        }
        PacketReader reader = new PacketReader(payload);
        reader.readUint8(); // header 0x00
        long affectedRows = reader.readLengthEncodedInt();
        long lastInsertId = reader.readLengthEncodedInt();
        int statusFlags = reader.remaining() >= 2 ? reader.readUint16() : 0;
        int warnings = reader.remaining() >= 2 ? reader.readUint16() : 0;
        String info = reader.hasRemaining() ? reader.readRestAsString() : "";
        return new OkPacket(affectedRows, lastInsertId, statusFlags, warnings, info);
    }

    @Override
    public String toString() {
        return "OkPacket{affectedRows=" + affectedRows
                + ", lastInsertId=" + lastInsertId
                + ", statusFlags=0x" + Integer.toHexString(statusFlags)
                + ", warnings=" + warnings
                + ", info='" + info + "'}";
    }
}
