package com.minimysql.protocol;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 服务器端预编译：COM_STMT_PREPARE / EXECUTE / CLOSE。
 */
public final class ComStmt {

    public static final byte COM_STMT_PREPARE = 0x16;
    public static final byte COM_STMT_EXECUTE = 0x17;
    public static final byte COM_STMT_CLOSE = 0x19;

    private ComStmt() {
    }

    public static PreparedStatementHandle prepare(PacketIO io, String sql) throws IOException {
        if (sql == null || sql.isEmpty()) {
            throw new IllegalArgumentException("SQL 不能为空");
        }
        io.setSequenceId(0);
        byte[] sqlBytes = sql.getBytes(StandardCharsets.UTF_8);
        byte[] payload = new byte[1 + sqlBytes.length];
        payload[0] = COM_STMT_PREPARE;
        System.arraycopy(sqlBytes, 0, payload, 1, sqlBytes.length);
        io.writePacketPayload(payload);

        byte[] first = io.readPacketPayload();
        if (ErrPacket.isErr(first)) {
            throw ErrPacket.parse(first).toException();
        }
        if (first.length < 12 || (first[0] & 0xFF) != 0x00) {
            throw new IOException("非法 COM_STMT_PREPARE 响应");
        }

        PacketReader reader = new PacketReader(first);
        reader.readUint8();
        int statementId = reader.readInt32();
        int numColumns = reader.readUint16();
        int numParams = reader.readUint16();
        reader.readUint8(); // filler
        // warning_count 可忽略

        skipDefinitions(io, numParams);
        ColumnDefinition[] columns = readDefinitions(io, numColumns);
        return new PreparedStatementHandle(statementId, numParams, columns);
    }

    public static StmtExecuteResult execute(PacketIO io, PreparedStatementHandle handle, Object[] params)
            throws IOException {
        if (params.length != handle.numParams) {
            throw new IllegalArgumentException(
                    "参数个数不匹配: 期望 " + handle.numParams + ", 实际 " + params.length);
        }
        io.setSequenceId(0);
        io.writePacketPayload(BinaryParameterEncoder.buildExecutePayload(handle.statementId, params));

        byte[] response = io.readPacketPayload();
        if (ErrPacket.isErr(response)) {
            throw ErrPacket.parse(response).toException();
        }
        if (handle.hasResultSet()) {
            return StmtExecuteResult.query(BinaryResultSetDecoder.decode(io, response));
        }
        if (response.length > 0 && (response[0] & 0xFF) == 0x00) {
            return StmtExecuteResult.ok(OkPacket.parse(response));
        }
        throw new IOException("COM_STMT_EXECUTE 响应无法识别，首字节=0x"
                + Integer.toHexString(response[0] & 0xFF));
    }

    public static void close(PacketIO io, int statementId) throws IOException {
        io.setSequenceId(0);
        PacketWriter w = new PacketWriter();
        w.writeUint8(COM_STMT_CLOSE);
        w.writeUint32(statementId & 0xFFFFFFFFL);
        io.writePacketPayload(w.toByteArray());
        // 服务器不回包
    }

    private static void skipDefinitions(PacketIO io, int count) throws IOException {
        if (count <= 0) {
            return;
        }
        for (int i = 0; i < count; i++) {
            io.readPacketPayload();
        }
        consumeMetadataTerminator(io);
    }

    private static ColumnDefinition[] readDefinitions(PacketIO io, int count) throws IOException {
        if (count <= 0) {
            return new ColumnDefinition[0];
        }
        ColumnDefinition[] columns = new ColumnDefinition[count];
        for (int i = 0; i < count; i++) {
            columns[i] = ColumnDefinition.parse(io.readPacketPayload());
        }
        consumeMetadataTerminator(io);
        return columns;
    }

    private static void consumeMetadataTerminator(PacketIO io) throws IOException {
        byte[] maybeEof = io.readPacketPayload();
        if (ErrPacket.isErr(maybeEof)) {
            throw ErrPacket.parse(maybeEof).toException();
        }
        int header = maybeEof.length == 0 ? -1 : (maybeEof[0] & 0xFF);
        if (header != 0x00
                && !ResultSetDecoder.isLegacyEof(maybeEof)
                && !ResultSetDecoder.isResultSetTerminator(maybeEof)) {
            throw new IOException("PREPARE 元数据后缺少 EOF/OK，首字节=0x"
                    + Integer.toHexString(header));
        }
    }
}
