package discord.qeid.database;

import discord.qeid.Teams;

import java.io.InputStream;
import java.sql.*;
import java.util.Scanner;

public class DatabaseManager {
    private final Teams plugin;
    private Connection connection;

    public DatabaseManager(Teams plugin) {
        this.plugin = plugin;
        connect();
        initializeSchema();
    }

    private void connect() {
        try {
            String url = "jdbc:sqlite:" + plugin.getDataFolder().getAbsolutePath() + "/teams.db";
            connection = DriverManager.getConnection(url);
            plugin.getLogger().info("Connected to SQLite.");
        } catch (SQLException e) {
            plugin.getLogger().severe("SQLite connection failed.");
            e.printStackTrace();
        }
    }

    private void initializeSchema() {
        try (InputStream stream = plugin.getResource("schema.sql")) {
            if (stream == null) {
                plugin.getLogger().warning("schema.sql not found!");
                return;
            }

            Scanner scanner = new Scanner(stream).useDelimiter(";");
            Statement stmt = connection.createStatement();

            while (scanner.hasNext()) {
                String sql = scanner.next().trim();
                if (!sql.isEmpty()) {
                    stmt.execute(sql);
                }
            }

            stmt.close();
            plugin.getLogger().info("SQLite schema initialized.");
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to initialize schema.");
            e.printStackTrace();
        }
    }


    public Connection getConnection() {
        return connection;
    }




    public void close() {
        try {
            if (connection != null) connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
