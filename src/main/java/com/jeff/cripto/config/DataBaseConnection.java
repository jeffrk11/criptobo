package com.jeff.cripto.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.postgresql.jdbc.PgConnection;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DataBaseConnection {

    private static final HikariDataSource dataSource;

    static {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(ConfigLoader.get("database.url"));
        config.setUsername(ConfigLoader.get("database.user"));
        config.setPassword(ConfigLoader.get("database.password"));
        config.setMaximumPoolSize(1);
        dataSource = new HikariDataSource(config);
    }


    public static Connection getConnection() throws SQLException {

        return dataSource.getConnection();
    }

}
