package discord.qeid.database;

import discord.qeid.model.AdminLogEntry;
import discord.qeid.Teams;

import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AdminLogManager {
    private final Teams plugin;

    public AdminLogManager(Teams plugin) {
        this.plugin = plugin;
    }

    public void logAction(UUID executor, String executorName, String action, String teamName, String targetName, String reason) {
        try (PreparedStatement stmt = plugin.getDatabaseManager().getConnection().prepareStatement(
            "INSERT INTO admin_logs (executor_uuid, executor_name, action, team_name, target_name, reason, timestamp) VALUES (?, ?, ?, ?, ?, ?, ?)"
        )) {
            stmt.setString(1, executor.toString());
            stmt.setString(2, executorName);
            stmt.setString(3, action);
            stmt.setString(4, teamName);
            stmt.setString(5, targetName);
            stmt.setString(6, reason);
            stmt.setLong(7, Instant.now().getEpochSecond());
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<AdminLogEntry> getLogs(int page, int pageSize) {
        List<AdminLogEntry> logs = new ArrayList<>();
        try (PreparedStatement stmt = plugin.getDatabaseManager().getConnection().prepareStatement(
            "SELECT * FROM admin_logs ORDER BY timestamp DESC LIMIT ? OFFSET ?"
        )) {
            stmt.setInt(1, pageSize);
            stmt.setInt(2, (page - 1) * pageSize);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                logs.add(new AdminLogEntry(
                    rs.getInt("id"),
                    UUID.fromString(rs.getString("executor_uuid")),
                    rs.getString("executor_name"),
                    rs.getString("action"),
                    rs.getString("team_name"),
                    rs.getString("target_name"),
                    rs.getString("reason"),
                    rs.getLong("timestamp")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return logs;
    }

    public int getLogCount() {
        try (PreparedStatement stmt = plugin.getDatabaseManager().getConnection().prepareStatement(
            "SELECT COUNT(*) FROM admin_logs"
        )) {
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }
}
