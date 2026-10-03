package com.minimysql.protocol;

/**
 * COM_STMT_EXECUTE 的两种结果：无结果集 OK，或二进制结果集。
 */
public final class StmtExecuteResult {

    public final OkPacket ok;
    public final QueryResult query;

    private StmtExecuteResult(OkPacket ok, QueryResult query) {
        this.ok = ok;
        this.query = query;
    }

    public static StmtExecuteResult ok(OkPacket ok) {
        return new StmtExecuteResult(ok, null);
    }

    public static StmtExecuteResult query(QueryResult query) {
        return new StmtExecuteResult(null, query);
    }

    public boolean isQuery() {
        return query != null;
    }
}
