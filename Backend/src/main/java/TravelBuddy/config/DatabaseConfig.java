package TravelBuddy.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

@Configuration
public class DatabaseConfig {

    @Value("${spring.datasource.url:jdbc:XXX}") // TODO: change this to your database url   
    private String url;

    @Value("${spring.datasource.username:XXX}") // TODO: change this to your username
    private String username;

    @Value("${spring.datasource.password:XXX}") // TODO: change this to your password
    private String password;

    public String getUrl() {
        return url;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, username, password);
    }
}
