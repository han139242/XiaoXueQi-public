import java.sql.Connection;
import java.sql.DriverManager;

public class TestConnection {
    public static void main(String[] args) {
        String url = System.getenv().getOrDefault(
                "MES_DB_URL",
                "jdbc:mysql://localhost:3306/mes?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai"
        );
        String username = System.getenv().getOrDefault("MES_DB_USERNAME", "root");
        String password = System.getenv().getOrDefault("MES_DB_PASSWORD", "your-db-password");

        System.out.println("正在测试数据库连接...");
        System.out.println("URL: " + url);
        System.out.println("用户名: " + username);
        System.out.println();

        try {
            // 加载驱动
            Class.forName("com.mysql.cj.jdbc.Driver");
            System.out.println("✓ MySQL 驱动加载成功");

            // 尝试连接
            Connection conn = DriverManager.getConnection(url, username, password);
            System.out.println("✓ 数据库连接成功！");
            System.out.println("✓ 数据库: mes");

            conn.close();
            System.out.println("\n结论: 配置正确，可以连接到数据库！");

        } catch (ClassNotFoundException e) {
            System.out.println("✗ MySQL 驱动未找到");
            System.out.println("  原因: 缺少 mysql-connector-j 依赖");
            System.out.println("  解决: 需要在 pom.xml 中添加 MySQL 驱动（已添加，需要刷新）");
        } catch (java.sql.SQLException e) {
            System.out.println("✗ 数据库连接失败");
            System.out.println("  错误信息: " + e.getMessage());

            if (e.getMessage().contains("Unknown database")) {
                System.out.println("  原因: 数据库 'mes' 不存在");
                System.out.println("  解决: 需要先创建数据库: CREATE DATABASE mes;");
            } else if (e.getMessage().contains("Access denied")) {
                System.out.println("  原因: 用户名或密码错误");
                System.out.println("  解决: 检查 root 用户密码是否与环境变量 MES_DB_PASSWORD 一致");
            } else if (e.getMessage().contains("Communications link failure")) {
                System.out.println("  原因: MySQL 服务未启动或端口不是 3306");
                System.out.println("  解决: 启动 MySQL 服务");
            }
        } catch (Exception e) {
            System.out.println("✗ 未知错误: " + e.getMessage());
        }
    }
}
