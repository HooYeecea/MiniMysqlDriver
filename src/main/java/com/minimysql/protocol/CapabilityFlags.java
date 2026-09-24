package com.minimysql.protocol;

/**
 * 客户端声明自己支持的能力（Handshake Response 里带上）。
 * 只列出本项目会用到的几位。
 */
public final class CapabilityFlags {

    public static final int CLIENT_LONG_PASSWORD = 1;
    public static final int CLIENT_FOUND_ROWS = 1 << 1;
    public static final int CLIENT_LONG_FLAG = 1 << 2;
    public static final int CLIENT_CONNECT_WITH_DB = 1 << 3;
    public static final int CLIENT_PROTOCOL_41 = 1 << 9;
    public static final int CLIENT_TRANSACTIONS = 1 << 13;
    public static final int CLIENT_SECURE_CONNECTION = 1 << 15;
    public static final int CLIENT_PLUGIN_AUTH = 1 << 19;
    public static final int CLIENT_PLUGIN_AUTH_LENENC_CLIENT_DATA = 1 << 21;
    public static final int CLIENT_DEPRECATE_EOF = 1 << 24;

    /** 迷你驱动声明的能力集合。 */
    public static final int CLIENT_BASIC =
            CLIENT_LONG_PASSWORD
                    | CLIENT_LONG_FLAG
                    | CLIENT_CONNECT_WITH_DB
                    | CLIENT_PROTOCOL_41
                    | CLIENT_TRANSACTIONS
                    | CLIENT_SECURE_CONNECTION
                    | CLIENT_PLUGIN_AUTH
                    | CLIENT_PLUGIN_AUTH_LENENC_CLIENT_DATA
                    | CLIENT_DEPRECATE_EOF;

    private CapabilityFlags() {
    }
}
