package discord.qeid.database;

import discord.qeid.Teams;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class PlayerDataManager {

    private final Teams plugin;
    private final Map<UUID, Set<String>> pendingInvites = new HashMap<>();

    public PlayerDataManager(Teams plugin) {
        this.plugin = plugin;
        loadInvitesFromDatabase();
    }

    private void loadInvitesFromDatabase() {
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "SELECT player_uuid, team_id FROM team_invites"
             );
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                UUID player = UUID.fromString(rs.getString("player_uuid"));
                String teamId = rs.getString("team_id");
                pendingInvites.computeIfAbsent(player, k -> new HashSet<>()).add(teamId);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void addInvite(UUID target, String teamId) {
        pendingInvites.computeIfAbsent(target, k -> new HashSet<>()).add(teamId);
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "INSERT OR IGNORE INTO team_invites (player_uuid, team_id, invited_at) VALUES (?, ?, ?)"
             )) {
            stmt.setString(1, target.toString());
            stmt.setString(2, teamId);
            stmt.setLong(3, System.currentTimeMillis() / 1000);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public boolean hasInvite(UUID target, String teamId) {
        return pendingInvites.getOrDefault(target, Collections.emptySet()).contains(teamId);
    }

    public void removeInvite(UUID target, String teamId) {
        Set<String> invites = pendingInvites.get(target);
        if (invites != null) {
            invites.remove(teamId);
            if (invites.isEmpty()) {
                pendingInvites.remove(target);
            }
        }
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "DELETE FROM team_invites WHERE player_uuid = ? AND team_id = ?"
             )) {
            stmt.setString(1, target.toString());
            stmt.setString(2, teamId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Set<String> getInvites(UUID target) {
        return pendingInvites.getOrDefault(target, Collections.emptySet());
    }

    public void clear(UUID target) {
        pendingInvites.remove(target);
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "DELETE FROM team_invites WHERE player_uuid = ?"
             )) {
            stmt.setString(1, target.toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
