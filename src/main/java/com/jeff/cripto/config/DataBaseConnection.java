package com.jeff.cripto.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DataBaseConnection {
    private static final String URL = ConfigLoader.get("database.url");
    private static final String USER = ConfigLoader.get("database.user");
    private static final String PASSWORD = ConfigLoader.get("database.password");

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

}
