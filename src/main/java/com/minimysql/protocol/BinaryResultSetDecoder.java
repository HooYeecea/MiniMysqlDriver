package com.minimysql.protocol;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 解析 COM_STMT_EXECUTE 返回的二进制结果集。
 */
public final class BinaryResultSetDecoder {

    private BinaryResultSetDecoder() {
    }

    public static QueryResult decode(PacketIO io, byte[] firstPayload) throws IOException {
        if (ErrPacket.isErr(firstPayload)) {
            throw ErrPacket.parse(firstPayload).toException();
        }
        if (firstPayload.length > 0 && (firstPayload[0] & 0xFF) == 0x00) {
            throw new IOException("收到 OK Packet，这不是结果集。");
        }

        PacketReader countReader = new PacketReader(firstPayload);
        long columnCountLong = countReader.readLengthEncodedInt();
        if (columnCountLong <= 0 || columnCountLong > Integer.MAX_VALUE) {
            throw new IOException("非法列数: " + columnCountLong);
        }
        int columnCount = (int) columnCountLong;

        ColumnDefinition[] columns = new ColumnDefinition[columnCount];
        for (int i = 0; i < columnCount; i++) {
            columns[i] = ColumnDefinition.parse(io.readPacketPayload());
        }

        byte[] maybeEof = io.readPacketPayload();
        if (ResultSetDecoder.isLegacyEof(maybeEof)) {
            maybeEof = io.readPacketPayload();
        }

        List<String[]> rows = new ArrayList<>();
        byte[] rowPacket = maybeEof;
        while (!ResultSetDecoder.isResultSetTerminator(rowPacket)) {
            if (ErrPacket.isErr(rowPacket)) {
                throw ErrPacket.parse(rowPacket).toException();
            }
            rows.add(BinaryRowDecoder.decode(rowPacket, columns));
            rowPacket = io.readPacketPayload();
        }
        return new QueryResult(columns, rows);
    }
}
