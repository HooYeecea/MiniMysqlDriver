package com.minimysql.protocol;

/**
 * COM_STMT_PREPARE 成功后的句柄：statement_id、参数个数、结果列。
 */
public final class PreparedStatementHandle {

    public final int statementId;
    public final int numParams;
    public final ColumnDefinition[] columns;

    public PreparedStatementHandle(int statementId, int numParams, ColumnDefinition[] columns) {
        this.statementId = statementId;
        this.numParams = numParams;
        this.columns = columns;
    }

    public boolean hasResultSet() {
        return columns.length > 0;
    }
}
