import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

public class ManualDataSourceTest {
    public static void main(String[] args) {
        String host = System.getenv().getOrDefault("DB_HOST", "localhost");
        String port = System.getenv().getOrDefault("DB_PORT", "5432");
        String dbName = System.getenv().getOrDefault("DB_NAME", "online_banking");
        String username = System.getenv().getOrDefault("DB_USERNAME", "postgres");
        String password = System.getenv().getOrDefault("DB_PASSWORD", "your-db-password");

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(String.format("jdbc:postgresql://%s:%s/%s", host, port, dbName));
        config.setUsername(username);
        config.setPassword(password);
        config.setDriverClassName("org.postgresql.Driver");

        DataSource dataSource = new HikariDataSource(config);

        try (Connection conn = dataSource.getConnection()) {
            System.out.println("Manual DataSource connection successful!");
            System.out.println("URL: " + conn.getMetaData().getURL());
            System.out.println("User: " + conn.getMetaData().getUserName());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
