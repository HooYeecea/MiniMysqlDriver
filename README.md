# MiniMySQLDriver

从零实现的迷你 **MySQL JDBC 驱动**，用于学习：

- MySQL 客户端/服务端二进制协议（跑在 TCP 上）
- JDBC 核心接口如何接到协议层

目标不是替代官方 `mysql-connector-j`，而是把「驱动到底在干什么」拆开看清楚。

## 它能做什么

| 能力 | 说明 |
|------|------|
| 连接 / 认证 | TCP + Handshake，支持 `mysql_native_password` / `caching_sha2_password` |
| 执行 SQL | `COM_QUERY`：DDL / DML / `SELECT` |
| 预编译 | `COM_STMT_PREPARE` / `EXECUTE` / `CLOSE` + 二进制行协议 |
| JDBC API | `Driver` / `Connection` / `Statement` / `PreparedStatement` / `ResultSet` / `getGeneratedKeys` |
| 错误映射 | ERR Packet → 带 `errorCode` / `SQLState` 的 `SQLException` |
| 事务 | `setAutoCommit` / `commit` / `rollback` |
| 大包 | 逻辑 payload 超过 16MB 时自动拆包 / 拼包 |

## 刻意简化的部分

- 大量 JDBC 方法直接抛 `SQLFeatureNotSupportedException`
- 未实现：SSL、连接池、存储过程、完整类型映射等

## 架构

```text
应用代码
  └─ JDBC（com.minimysql.jdbc）
        MiniDriver / MiniConnection / MiniStatement / …
            └─ 协议会话（com.minimysql.protocol）
                  MysqlSession / PacketIO / Handshake / ComQuery / …
                      └─ TCP Socket → MySQL Server
```

两层分工：

1. **protocol**：读写 MySQL Packet，完成握手、认证、查询、结果集解析  
2. **jdbc**：实现 `java.sql.*`，挂到 `DriverManager`

## 学习步骤（建议按顺序跑）

每一步都对应一个可运行的 `main`：

| 步骤 | 入口类 | 内容 |
|------|--------|------|
| 1 | `com.minimysql.step1.Step1ReadHandshake` | TCP 连接，读 Handshake |
| 2 | `com.minimysql.step2.Step2Authenticate` | 发认证包，登录成功 |
| 3 | `com.minimysql.step3.Step3ExecuteUpdate` | `COM_QUERY` 无结果集（建表 / INSERT） |
| 4 | `com.minimysql.step4.Step4ExecuteQuery` | 解析 `SELECT` 结果集 |
| 5 | `com.minimysql.step5.Step5JdbcApi` | 标准 JDBC：`DriverManager` |
| 6 | `com.minimysql.step6.Step6SqlException` | ERR → `SQLException` |
| 7 | `com.minimysql.step7.Step7PreparedStatement` | `PreparedStatement`（现已走服务器端预编译） |
| 8 | `com.minimysql.step8.Step8Transaction` | 事务提交 / 回滚 |
| 9 | `com.minimysql.step9.Step9ServerPreparedStatement` | `COM_STMT_*` + 二进制结果行 |
| 10 | `com.minimysql.step10.Step10GeneratedKeys` | INSERT 后 `getGeneratedKeys()` |
| 11 | `com.minimysql.step11.Step11PacketFragmentation` | Packet 拆包 / 拼包（不必连库） |

## 环境要求

- JDK 21+
- Maven 3.x
- 本机可访问的 MySQL（默认 `127.0.0.1:3306`）
- 事务演示需要 InnoDB（MySQL 8 默认一般即可）

## 快速开始

1. 修改各 `step*` 类中的账号、密码、库名（演示默认多为 `root` / `root`，库 `test`）
2. 确保数据库已创建，例如：

```sql
CREATE DATABASE IF NOT EXISTS test;
```

3. 编译并运行某一步：

```bash
mvn compile

# 示例：跑第 5 步 JDBC 演示
mvn -q exec:java -Dexec.mainClass=com.minimysql.step5.Step5JdbcApi
```

也可在 IDE 中直接运行对应类的 `main`。

## 标准 JDBC 用法示例

```java
Class.forName("com.minimysql.jdbc.MiniDriver");

try (Connection conn = DriverManager.getConnection(
        "jdbc:mysql://127.0.0.1:3306/test", "root", "root");
     Statement stmt = conn.createStatement();
     ResultSet rs = stmt.executeQuery("SELECT id, name FROM mini_driver_demo")) {

    while (rs.next()) {
        System.out.println(rs.getInt("id") + " " + rs.getString("name"));
    }
}
```

驱动通过 SPI 注册（`META-INF/services/java.sql.Driver`）；`Class.forName` 可选，显式写上更直观。

## 目录结构

```text
src/main/java/com/minimysql/
├── protocol/     # MySQL 协议：Packet、握手、认证、查询、结果集
├── jdbc/         # JDBC 接口实现
└── step1…step8/  # 分步演示入口

src/main/resources/
└── META-INF/services/java.sql.Driver
```

## URL 格式

```text
jdbc:mysql://host:port/database
```

示例：`jdbc:mysql://127.0.0.1:3306/test`（端口可省略，默认 3306）

## 说明

本项目仅供学习与练习。生产环境请使用官方 [MySQL Connector/J](https://dev.mysql.com/downloads/connector/j/)。
