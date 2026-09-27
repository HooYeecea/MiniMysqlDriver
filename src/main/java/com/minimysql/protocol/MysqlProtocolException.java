package com.minimysql.protocol;

import java.io.IOException;

/**
 * 协议层错误：携带 MySQL errorCode / SQLState，供 JDBC 层转成 SQLException。
 */
public class MysqlProtocolException extends IOException {

    private final int errorCode;
    private final String sqlState;

    public MysqlProtocolException(int errorCode, String sqlState, String message) {
        super(message);
        this.errorCode = errorCode;
        this.sqlState = sqlState;
    }

    public MysqlProtocolException(ErrPacket err) {
        this(err.errorCode, err.sqlState, err.message);
    }

    public int getErrorCode() {
        return errorCode;
    }

    public String getSqlState() {
        return sqlState;
    }

    public static MysqlProtocolException fromErrPayload(byte[] payload) {
        return new MysqlProtocolException(ErrPacket.parse(payload));
    }
}
