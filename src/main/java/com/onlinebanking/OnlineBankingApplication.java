package com.onlinebanking;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.beans.factory.annotation.Autowired;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

@SpringBootApplication
public class OnlineBankingApplication {

    @Autowired
    private DataSource dataSource;

    public static void main(String[] args) {
        SpringApplication.run(OnlineBankingApplication.class, args);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onReady() throws SQLException {
        System.out.println("DataSource URL: " + dataSource.getConnection().getMetaData().getURL());
        System.out.println("DataSource User: " + dataSource.getConnection().getMetaData().getUserName());
    }
}
