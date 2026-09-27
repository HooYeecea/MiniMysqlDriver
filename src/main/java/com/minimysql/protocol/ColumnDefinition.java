package com.minimysql.protocol;

/**
 * Column Definition（PROTOCOL_41）包解析结果。
 */
public final class ColumnDefinition {

    public final String catalog;
    public final String schema;
    public final String table;
    public final String orgTable;
    public final String name;
    public final String orgName;
    public final int characterSet;
    public final long columnLength;
    public final int type;
    public final int flags;
    public final int decimals;

    private ColumnDefinition(String catalog,
                             String schema,
                             String table,
                             String orgTable,
                             String name,
                             String orgName,
                             int characterSet,
                             long columnLength,
                             int type,
                             int flags,
                             int decimals) {
        this.catalog = catalog;
        this.schema = schema;
        this.table = table;
        this.orgTable = orgTable;
        this.name = name;
        this.orgName = orgName;
        this.characterSet = characterSet;
        this.columnLength = columnLength;
        this.type = type;
        this.flags = flags;
        this.decimals = decimals;
    }

    public static ColumnDefinition parse(byte[] payload) {
        PacketReader reader = new PacketReader(payload);
        String catalog = reader.readLengthEncodedString();
        String schema = reader.readLengthEncodedString();
        String table = reader.readLengthEncodedString();
        String orgTable = reader.readLengthEncodedString();
        String name = reader.readLengthEncodedString();
        String orgName = reader.readLengthEncodedString();

        int fillerLen = reader.readUint8(); // 固定为 0x0c
        if (fillerLen != 0x0c) {
            throw new IllegalArgumentException("ColumnDefinition 固定长度字段应为 0x0c，实际=0x"
                    + Integer.toHexString(fillerLen));
        }

        int characterSet = reader.readUint16();
        long columnLength = reader.readUint32();
        int type = reader.readUint8();
        int flags = reader.readUint16();
        int decimals = reader.readUint8();
        // 末尾 2 字节 filler，可忽略

        return new ColumnDefinition(
                catalog, schema, table, orgTable, name, orgName,
                characterSet, columnLength, type, flags, decimals
        );
    }

    @Override
    public String toString() {
        return name + "(" + typeName(type) + ")";
    }

    private static String typeName(int type) {
        return switch (type) {
            case 0x00 -> "DECIMAL";
            case 0x01 -> "TINY";
            case 0x02 -> "SHORT";
            case 0x03 -> "LONG";
            case 0x04 -> "FLOAT";
            case 0x05 -> "DOUBLE";
            case 0x06 -> "NULL";
            case 0x07 -> "TIMESTAMP";
            case 0x08 -> "LONGLONG";
            case 0x09 -> "INT24";
            case 0x0a -> "DATE";
            case 0x0b -> "TIME";
            case 0x0c -> "DATETIME";
            case 0x0d -> "YEAR";
            case 0x0f -> "VARCHAR";
            case 0xf6 -> "NEWDECIMAL";
            case 0xfd -> "VAR_STRING";
            case 0xfe -> "STRING";
            case 0xff -> "GEOMETRY";
            default -> "TYPE_" + type;
        };
    }
}
