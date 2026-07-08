import java.sql.*;
import java.io.*;
import java.nio.charset.StandardCharsets;

public class ShowTablesUTF8 {
    public static void main(String[] args) {
        String url = System.getenv().getOrDefault(
                "MES_DB_URL",
                "jdbc:mysql://localhost:3306/mes?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai&characterEncoding=UTF-8"
        );
        String username = System.getenv().getOrDefault("MES_DB_USERNAME", "root");
        String password = System.getenv().getOrDefault("MES_DB_PASSWORD", "your-db-password");

        try (Connection conn = DriverManager.getConnection(url, username, password);
             PrintWriter writer = new PrintWriter(new OutputStreamWriter(
                 new FileOutputStream("database_tables.txt"), StandardCharsets.UTF_8))) {

            writer.println("========== MES数据库表结构 ==========\n");

            DatabaseMetaData metaData = conn.getMetaData();
            ResultSet tables = metaData.getTables("mes", null, "%", new String[]{"TABLE"});

            int tableCount = 0;
            while (tables.next()) {
                String tableName = tables.getString("TABLE_NAME");
                tableCount++;
                writer.println("【表 " + tableCount + "】" + tableName);

                ResultSet columns = metaData.getColumns("mes", null, tableName, "%");
                while (columns.next()) {
                    String columnName = columns.getString("COLUMN_NAME");
                    String columnType = columns.getString("TYPE_NAME");
                    int columnSize = columns.getInt("COLUMN_SIZE");
                    String nullable = columns.getString("IS_NULLABLE");

                    writer.printf("  %-30s %-15s(%d) 可空:%-3s\n",
                        columnName, columnType, columnSize, nullable);
                }
                writer.println();
                columns.close();
            }

            writer.println("========== 共 " + tableCount + " 张表 ==========");
            tables.close();

            System.out.println("表结构已导出到 database_tables.txt");

        } catch (Exception e) {
            System.err.println("错误: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
