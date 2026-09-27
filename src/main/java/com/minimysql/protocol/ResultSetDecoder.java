package com.minimysql.protocol;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 解析 COM_QUERY 返回的文本结果集。
 *
 * 流程（已协商 CLIENT_DEPRECATE_EOF 时）：
 *   1) column_count
 *   2) column_count 个 Column Definition
 *   3) 直接开始 Row Data（中间不再有旧版 EOF）
 *   4) 以 0xFE 开头的结束包收尾（OK-as-EOF）
 *   若未协商 DEPRECATE_EOF：列定义后会多一个旧 EOF，行后再一个 EOF。
 */
public final class ResultSetDecoder {

    private ResultSetDecoder() {
    }

    public static QueryResult decode(PacketIO io, byte[] firstPayload) throws IOException {
        if (ErrPacket.isErr(firstPayload)) {
            throw ErrPacket.parse(firstPayload).toException();
        }
        if (firstPayload.length > 0 && (firstPayload[0] & 0xFF) == 0x00) {
            throw new IOException("收到 OK Packet，这不是结果集。请用 executeUpdate 执行无结果集 SQL。");
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

        // 未启用 DEPRECATE_EOF 时，列定义后还有一个 EOF，需要吃掉
        byte[] maybeEof = io.readPacketPayload();
        if (isLegacyEof(maybeEof)) {
            // 旧协议：再开始读行
            maybeEof = io.readPacketPayload();
        }

        List<String[]> rows = new ArrayList<>();
        byte[] rowPacket = maybeEof;
        while (!isResultSetTerminator(rowPacket)) {
            if (ErrPacket.isErr(rowPacket)) {
                throw ErrPacket.parse(rowPacket).toException();
            }
            rows.add(parseTextRow(rowPacket, columnCount));
            rowPacket = io.readPacketPayload();
        }

        return new QueryResult(columns, rows);
    }

    private static String[] parseTextRow(byte[] payload, int columnCount) {
        PacketReader reader = new PacketReader(payload);
        String[] values = new String[columnCount];
        for (int i = 0; i < columnCount; i++) {
            values[i] = reader.readNullableLengthEncodedString();
        }
        return values;
    }

    /** 旧版 EOF：首字节 0xFE，且 payload 很短（通常 5 字节）。 */
    private static boolean isLegacyEof(byte[] payload) {
        return payload.length > 0
                && (payload[0] & 0xFF) == 0xFE
                && payload.length < 9;
    }

    /**
     * 结果集结束标记：
     * - 旧 EOF：0xFE 且长度 &lt; 9
     * - CLIENT_DEPRECATE_EOF：结尾 OK 也以 0xFE 开头（长度通常 ≥ 9）
     *
     * 迷你实现：行数据阶段凡首字节为 0xFE 即视为结束
     * （极端超长首字段可能误判，练习项目可忽略）。
     */
    private static boolean isResultSetTerminator(byte[] payload) {
        return payload.length > 0 && (payload[0] & 0xFF) == 0xFE;
    }
}
