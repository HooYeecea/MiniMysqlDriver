package com.minimysql.protocol;

/**
 * MySQL 字段类型常量（协议里的 1 字节 type）。
 */
public final class MysqlType {

    public static final int DECIMAL = 0x00;
    public static final int TINY = 0x01;
    public static final int SHORT = 0x02;
    public static final int LONG = 0x03;
    public static final int FLOAT = 0x04;
    public static final int DOUBLE = 0x05;
    public static final int NULL = 0x06;
    public static final int TIMESTAMP = 0x07;
    public static final int LONGLONG = 0x08;
    public static final int INT24 = 0x09;
    public static final int DATE = 0x0a;
    public static final int TIME = 0x0b;
    public static final int DATETIME = 0x0c;
    public static final int YEAR = 0x0d;
    public static final int VARCHAR = 0x0f;
    public static final int BIT = 0x10;
    public static final int JSON = 0xf5;
    public static final int NEWDECIMAL = 0xf6;
    public static final int ENUM = 0xf7;
    public static final int SET = 0xf8;
    public static final int TINY_BLOB = 0xf9;
    public static final int MEDIUM_BLOB = 0xfa;
    public static final int LONG_BLOB = 0xfb;
    public static final int BLOB = 0xfc;
    public static final int VAR_STRING = 0xfd;
    public static final int STRING = 0xfe;
    public static final int GEOMETRY = 0xff;

    public static final int UNSIGNED_FLAG = 0x0020;

    private MysqlType() {
    }
}
