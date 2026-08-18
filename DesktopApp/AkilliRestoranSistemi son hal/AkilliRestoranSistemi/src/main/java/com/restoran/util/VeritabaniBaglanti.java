package com.restoran.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class VeritabaniBaglanti {
    private static final String DB_URL = Env.get("DB_URL", "jdbc:sqlite:restoran.db");
    private static Connection connection = null;

    private VeritabaniBaglanti() { }

    public static Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                Class.forName("org.sqlite.JDBC");
                connection = DriverManager.getConnection(DB_URL);
            }
        } catch (ClassNotFoundException e) {
            System.err.println("SQLite JDBC sürücüsü bulunamadı.");
            e.printStackTrace();
            return null;
        } catch (SQLException e) {
            System.err.println("Veritabanı bağlantı hatası: " + e.getMessage());
            connection = null;
            try {
                connection = DriverManager.getConnection(DB_URL);
            } catch (SQLException retry) {
                System.err.println("Veritabanı bağlantısı yeniden kurulamadı: " + retry.getMessage());
                retry.printStackTrace();
            }
        }
        return connection;
    }

    public static void closeConnection() {
        if (connection != null) {
            try {
                connection.close();
                connection = null;
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}
