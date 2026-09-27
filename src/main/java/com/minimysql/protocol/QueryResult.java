package com.minimysql.protocol;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 协议层查询结果（尚未包装成 JDBC ResultSet）。
 * 文本协议下单元格都是字符串；SQL NULL 用 Java null 表示。
 */
public final class QueryResult {

    public final ColumnDefinition[] columns;
    public final List<String[]> rows;

    public QueryResult(ColumnDefinition[] columns, List<String[]> rows) {
        this.columns = columns;
        this.rows = Collections.unmodifiableList(rows);
    }

    public int columnCount() {
        return columns.length;
    }

    public int rowCount() {
        return rows.size();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("QueryResult{columns=").append(Arrays.toString(columns))
                .append(", rowCount=").append(rows.size()).append(", rows=[\n");
        for (String[] row : rows) {
            sb.append("  ").append(Arrays.toString(row)).append('\n');
        }
        sb.append("]}");
        return sb.toString();
    }
}
