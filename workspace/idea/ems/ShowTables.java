import java.sql.*;

public class ShowTables {
    public static void main(String[] args) {
        String url = System.getenv().getOrDefault(
                "MES_DB_URL",
                "jdbc:mysql://localhost:3306/mes?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai"
        );
        String username = System.getenv().getOrDefault("MES_DB_USERNAME", "root");
        String password = System.getenv().getOrDefault("MES_DB_PASSWORD", "your-db-password");

        try (Connection conn = DriverManager.getConnection(url, username, password)) {
            System.out.println("========== 数据库表结构 ==========\n");

            DatabaseMetaData metaData = conn.getMetaData();
            ResultSet tables = metaData.getTables("mes", null, "%", new String[]{"TABLE"});

            int tableCount = 0;
            while (tables.next()) {
                String tableName = tables.getString("TABLE_NAME");
                tableCount++;
                System.out.println("【表 " + tableCount + "】" + tableName);

                ResultSet columns = metaData.getColumns("mes", null, tableName, "%");
                while (columns.next()) {
                    String columnName = columns.getString("COLUMN_NAME");
                    String columnType = columns.getString("TYPE_NAME");
                    int columnSize = columns.getInt("COLUMN_SIZE");
                    String nullable = columns.getString("IS_NULLABLE");
                    String remarks = columns.getString("REMARKS");

                    System.out.printf("  %-25s %-15s(%d) 可空:%-3s %s\n",
                        columnName, columnType, columnSize, nullable,
                        remarks != null ? remarks : "");
                }
                System.out.println();
                columns.close();
            }

            System.out.println("========== 共 " + tableCount + " 张表 ==========");
            tables.close();

        } catch (Exception e) {
            System.err.println("错误: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
