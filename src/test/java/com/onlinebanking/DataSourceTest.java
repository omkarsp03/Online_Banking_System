package com.onlinebanking;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

@SpringBootTest
@ActiveProfiles("test")
public class DataSourceTest {

    @Autowired
    private DataSource dataSource;

    @Test
    public void testConnection() throws SQLException {
        try (Connection conn = dataSource.getConnection()) {
            System.out.println("Connected successfully!");
            System.out.println("URL: " + conn.getMetaData().getURL());
            System.out.println("User: " + conn.getMetaData().getUserName());
        }
    }
}
