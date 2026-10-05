package com.minimysql.jdbc;

import com.minimysql.protocol.ColumnDefinition;
import com.minimysql.protocol.MysqlType;
import com.minimysql.protocol.OkPacket;
import com.minimysql.protocol.QueryResult;

import java.util.ArrayList;
import java.util.List;

/**
 * 把 OK Packet 的 last_insert_id 转成 JDBC getGeneratedKeys() 结果集。
 *
 * 多行 INSERT 时，MySQL 只回第一个自增 ID；在 auto_increment_increment=1 的常见配置下，
 * 后续 ID 按 lastInsertId, lastInsertId+1, ... 补齐。
 */
final class GeneratedKeys {

    private static final ColumnDefinition[] COLUMNS = {
            ColumnDefinition.synthetic("GENERATED_KEY", MysqlType.LONGLONG, 20)
    };

    private GeneratedKeys() {
    }

    static QueryResult empty() {
        return new QueryResult(COLUMNS, List.of());
    }

    static QueryResult fromOk(OkPacket ok) {
        if (ok == null || ok.lastInsertId <= 0) {
            return empty();
        }
        long count = ok.affectedRows > 0 ? ok.affectedRows : 1;
        List<String[]> rows = new ArrayList<>();
        for (long i = 0; i < count; i++) {
            rows.add(new String[]{Long.toString(ok.lastInsertId + i)});
        }
        return new QueryResult(COLUMNS, rows);
    }
}
