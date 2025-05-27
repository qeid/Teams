package discord.qeid.database;

import discord.qeid.Teams;
import discord.qeid.commands.teams.TeamInfoCommand;
import discord.qeid.model.AuditLogEntry;
import discord.qeid.model.Team;
import discord.qeid.model.TeamBanInfo;
import discord.qeid.model.TeamRoles;
import discord.qeid.utils.ColorUtils;
import discord.qeid.utils.MessagesUtil;
import org.bukkit.entity.Player;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.sql.*;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class TeamManager {

    private final Teams plugin;
    private final DatabaseManager db;

    public TeamManager(Teams plugin) {
        this.plugin = plugin;
        this.db = plugin.getDatabaseManager();
    }

    public void migrateAddCreatedAt() {
        try (Statement stmt = db.getConnection().createStatement()) {
            stmt.executeUpdate("ALTER TABLE teams ADD COLUMN created_at INTEGER DEFAULT (strftime('%s','now'))");
        } catch (SQLException e) {
            // checkj dupe
            if (!e.getMessage().toLowerCase().contains("duplicate column")) {
                e.printStackTrace();
            }
        }
    }

    /*
    public void logTeamTableColumns() {
        try (Statement stmt = db.getConnection().createStatement();
             ResultSet rs = stmt.executeQuery("PRAGMA table_info(teams)")) {

            System.out.println("======= TEAM TABLE COLUMNS =======");
            while (rs.next()) {
                String columnName = rs.getString("name");
                System.out.println("Column: " + columnName);
            }
            System.out.println("==================================");

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    */



    public boolean teamExists(String name) {
        try (PreparedStatement stmt = db.getConnection().prepareStatement(
                "SELECT 1 FROM teams WHERE LOWER(name) = LOWER(?)")) {
            stmt.setString(1, name);
            ResultSet rs = stmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean createTeam(String name, UUID owner, String tag) {
        String randomId = generateRandomId();
        try (PreparedStatement stmt = db.getConnection().prepareStatement(
                "INSERT INTO teams (id, name, tag, owner_uuid, created_at) VALUES (?, ?, ?, ?, ?)")) {

            stmt.setString(1, randomId);
            stmt.setString(2, name);
            stmt.setString(3, tag);
            stmt.setString(4, owner.toString());
            stmt.setLong(5, System.currentTimeMillis() / 1000);
            stmt.executeUpdate();


            String teamId = randomId;



            PreparedStatement memberStmt = db.getConnection().prepareStatement(
                    "INSERT INTO team_members (team_id, player_uuid, role) VALUES (?, ?, ?)");
            memberStmt.setString(1, teamId);
            memberStmt.setString(2, owner.toString());
            memberStmt.setString(3, "OWNER");
            memberStmt.executeUpdate();

            return true;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public Team getTeamByPlayer(UUID player) {
        try (PreparedStatement stmt = db.getConnection().prepareStatement(
                "SELECT t.id, t.name, t.tag, t.owner_uuid, t.created_at FROM teams t " +
                "JOIN team_members tm ON tm.team_id = t.id " +
                "WHERE tm.player_uuid = ?")) {

            stmt.setString(1, player.toString());
            ResultSet rs = stmt.executeQuery();

            if (!rs.next()) return null;

            String id = rs.getString("id");
            String name = rs.getString("name");
            String tag = rs.getString("tag");
            UUID owner = UUID.fromString(rs.getString("owner_uuid"));
            long createdAt = rs.getLong("created_at");


            Map<TeamRoles, Set<UUID>> members = new HashMap<>();
            for (TeamRoles role : TeamRoles.values()) {
                members.put(role, new HashSet<>());
            }



            PreparedStatement roleStmt = db.getConnection().prepareStatement(
                    "SELECT player_uuid, role FROM team_members WHERE team_id = ?");
            roleStmt.setString(1, id);
            ResultSet roleRs = roleStmt.executeQuery();



            while (roleRs.next()) {
                UUID uuid = UUID.fromString(roleRs.getString("player_uuid"));
                String role = roleRs.getString("role");

                TeamRoles TeamRole;
                try {
                    TeamRole = TeamRoles.valueOf(role);
                } catch (IllegalArgumentException e) {
                    continue;
                }
                members.get(TeamRole).add(uuid);
            }

            //long createdAt = rs.getLong("created_at");
            return new Team(
                    id,
                    name,
                    tag,
                    owner,
                    members.get(TeamRoles.ADMIN),
                    members.get(TeamRoles.MOD),
                    members.get(TeamRoles.MEMBER),
                    createdAt
            );
        } catch (SQLException e) {

            e.printStackTrace();
            return null;
        }
    }
    public boolean addMemberToTeam(String teamId, UUID playerId) {
        try (PreparedStatement stmt = db.getConnection().prepareStatement(
                "INSERT INTO team_members (team_id, player_uuid, role) VALUES (?, ?, ?)")) {
            stmt.setString(1, teamId);
            stmt.setString(2, playerId.toString());
            stmt.setString(3, "MEMBER");
            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public Team getTeamById(String id) {
        try (PreparedStatement stmt = db.getConnection().prepareStatement(
                "SELECT * FROM teams WHERE id = ?")) {
            stmt.setString(1, id);
            ResultSet rs = stmt.executeQuery();
            if (!rs.next()) return null;

            UUID owner = UUID.fromString(rs.getString("owner_uuid"));
            long createdAt = rs.getLong("created_at");
            Map<TeamRoles, Set<UUID>> members = loadTeamMembers(id); // <-- load all roles

            return new Team(
                    rs.getString("id"),
                    rs.getString("name"),
                    rs.getString("tag"),
                    owner,
                    members.get(TeamRoles.ADMIN),
                    members.get(TeamRoles.MOD),
                    members.get(TeamRoles.MEMBER),
                    createdAt
            );
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }


   public Team getTeamByName(String name) {
        try (PreparedStatement stmt = db.getConnection().prepareStatement(
                "SELECT id, name, tag, owner_uuid, created_at FROM teams WHERE name = ?")) {
            stmt.setString(1, name);
            ResultSet rs = stmt.executeQuery();

            if (!rs.next()) return null;

            UUID owner = UUID.fromString(rs.getString("owner_uuid"));
            String teamId = rs.getString("id");
            Map<TeamRoles, Set<UUID>> members = loadTeamMembers(teamId);

            return new Team(
                teamId,
                rs.getString("name"),
                rs.getString("tag"),
                owner,
                members.get(TeamRoles.ADMIN),
                members.get(TeamRoles.MOD),
                members.get(TeamRoles.MEMBER),
                rs.getLong("created_at")
            );
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
}

    public boolean isBanned(String teamId, UUID player) {
        try (PreparedStatement stmt = db.getConnection().prepareStatement(
                "SELECT expires_at FROM team_bans WHERE team_id = ? AND player_uuid = ?")) {
            stmt.setString(1, teamId);
            stmt.setString(2, player.toString());

            ResultSet rs = stmt.executeQuery();
            if (!rs.next()) return false;

            long expires = rs.getLong("expires_at");
            if (expires == 0) return true; // Permanent
            return System.currentTimeMillis() / 1000 < expires;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean kickMember(String teamId, UUID memberId) {
        try (PreparedStatement stmt = db.getConnection().prepareStatement(
                "DELETE FROM team_members WHERE team_id = ? AND player_uuid = ?")) {
            stmt.setString(1, teamId);
            stmt.setString(2, memberId.toString());
            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean banPlayer(String teamId, UUID player, UUID executor, String reason, long durationSeconds) {
        long expires = (durationSeconds <= 0) ? 0 : (System.currentTimeMillis() / 1000 + durationSeconds);
        try (PreparedStatement stmt = db.getConnection().prepareStatement(
            "INSERT OR REPLACE INTO team_bans (team_id, player_uuid, executor_uuid, reason, expires_at, executed_at) " +
            "VALUES (?, ?, ?, ?, ?, ?)")) {

            stmt.setString(1, teamId);
            stmt.setString(2, player.toString());
            stmt.setString(3, executor.toString());
            stmt.setString(4, reason);
            stmt.setLong(5, expires);
            stmt.setLong(6, System.currentTimeMillis() / 1000);
            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public TeamBanInfo getBanInfo(String teamId, UUID player) {
        try (PreparedStatement stmt = db.getConnection().prepareStatement(
                "SELECT reason, expires_at FROM team_bans WHERE team_id = ? AND player_uuid = ?")) {
            stmt.setString(1, teamId);
            stmt.setString(2, player.toString());
            ResultSet rs = stmt.executeQuery();

            if (!rs.next()) return null;
            return new TeamBanInfo(rs.getString("reason"), rs.getLong("expires_at"));
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public boolean disbandTeam(String teamId) {
        Connection conn = db.getConnection();

        try {
            PreparedStatement members = conn.prepareStatement("DELETE FROM team_members WHERE team_id = ?");
            PreparedStatement bans = conn.prepareStatement("DELETE FROM team_bans WHERE team_id = ?");
            PreparedStatement team = conn.prepareStatement("DELETE FROM teams WHERE id = ?");

            members.setString(1, teamId);
            bans.setString(1, teamId);
            team.setString(1, teamId);

            members.executeUpdate();
            bans.executeUpdate();
            team.executeUpdate();

            // Clean up statements manually
            members.close();
            bans.close();
            team.close();

            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public List<String> getAllTeamNames() {
        List<String> names = new ArrayList<>();
        try (PreparedStatement stmt = db.getConnection().prepareStatement(
                "SELECT name FROM teams")) {
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                names.add(rs.getString("name"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return names;
    }

    public List<Team> getAllTeams() {
        List<Team> teams = new ArrayList<>();
        try (PreparedStatement stmt = db.getConnection().prepareStatement("SELECT id, name, tag, owner_uuid, created_at FROM teams");
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                UUID owner = UUID.fromString(rs.getString("owner_uuid"));
                Team team = new Team(
                    rs.getString("id"),
                    rs.getString("name"),
                    rs.getString("tag"),
                    owner,
                    Set.of(), Set.of(), Set.of(),
                    rs.getLong("created_at")
                );
                teams.add(team);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return teams;
    }

    private Map<TeamRoles, Set<UUID>> loadTeamMembers(String teamId) throws SQLException {
        Map<TeamRoles, Set<UUID>> members = new HashMap<>();
        for (TeamRoles role : TeamRoles.values()) {
            members.put(role, new HashSet<>());
        }

        PreparedStatement stmt = db.getConnection().prepareStatement(
            "SELECT player_uuid, role FROM team_members WHERE team_id = ?"
        );
        stmt.setString(1, teamId);
        ResultSet rs = stmt.executeQuery();

        while (rs.next()) {
            UUID uuid = UUID.fromString(rs.getString("player_uuid"));
            try {
                TeamRoles role = TeamRoles.valueOf(rs.getString("role"));
                members.get(role).add(uuid);
            } catch (IllegalArgumentException ignored) {}
        }

        stmt.close();
        return members;
    }

    public static TeamRoles getRole(Team team, UUID playerId) {
        if (team.getOwner().equals(playerId)) return TeamRoles.OWNER;
        if (team.getAdmins().contains(playerId)) return TeamRoles.ADMIN;
        if (team.getMods().contains(playerId)) return TeamRoles.MOD;
        if (team.getMembers().contains(playerId)) return TeamRoles.MEMBER;
        return TeamRoles.NONE;
    }

    public static void sendTeamInfo(Player viewer, Team team) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("M/dd/yy, h:mm a")
                .withZone(ZoneId.systemDefault());
        String dateFormatted = formatter.format(Instant.ofEpochSecond(team.getCreatedAt()));

        List<String> format = MessagesUtil.getMessages().getStringList("team.info.message");
        for (String line : format) {
            line = line
                .replace("%tag%", team.getTag())
                .replace("%name%", team.getName())
                .replace("%owner%", TeamInfoCommand.formatPlayerName(team.getOwner()))
                .replace("%admins%", TeamInfoCommand.formatPlayerList(team.getAdmins()))
                .replace("%mods%", TeamInfoCommand.formatPlayerList(team.getMods()))
                .replace("%members%", TeamInfoCommand.formatPlayerList(team.getMembers()))
                .replace("%created%", dateFormatted);

            viewer.sendMessage(ColorUtils.format(line));
        }
    }

    public boolean promoteToRole(String teamId, UUID playerId, String role) {
        Connection conn = db.getConnection();
        try {
            PreparedStatement clearOld = conn.prepareStatement(
                "DELETE FROM team_members WHERE team_id = ? AND player_uuid = ?");
            clearOld.setString(1, teamId);
            clearOld.setString(2, playerId.toString());
            clearOld.executeUpdate();

            PreparedStatement addNew = conn.prepareStatement(
                "INSERT INTO team_members (team_id, player_uuid, role) VALUES (?, ?, ?)");
            addNew.setString(1, teamId);
            addNew.setString(2, playerId.toString());
            addNew.setString(3, role);
            addNew.executeUpdate();


            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
    }
}


    public boolean demoteToRole(String teamId, UUID playerId, String newRole) {
        return promoteToRole(teamId, playerId, newRole); // same logic
    }

    public void setOwner(String teamId, UUID newOwner) {
        Connection conn = db.getConnection();
        try {
            PreparedStatement stmt = conn.prepareStatement("UPDATE teams SET owner_uuid = ? WHERE id = ?");
            stmt.setString(1, newOwner.toString());
            stmt.setString(2, teamId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public boolean removeMember(String teamId, UUID playerId) {
        Connection conn = db.getConnection();
        try {
            PreparedStatement stmt = conn.prepareStatement("DELETE FROM team_members WHERE team_id = ? AND player_uuid = ?");
            stmt.setString(1, teamId);
            stmt.setString(2, playerId.toString());
            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean setHome(String teamId, Location loc) {
        try (PreparedStatement stmt = db.getConnection().prepareStatement(
                "UPDATE teams SET home_world = ?, home_x = ?, home_y = ?, home_z = ? WHERE id = ?")) {
            stmt.setString(1, loc.getWorld().getName());
            stmt.setDouble(2, loc.getX());
            stmt.setDouble(3, loc.getY());
            stmt.setDouble(4, loc.getZ());
            stmt.setString(5, teamId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public Location getHome(String teamId) {
        try (PreparedStatement stmt = db.getConnection().prepareStatement(
                "SELECT home_world, home_x, home_y, home_z FROM teams WHERE id = ?")) {
            stmt.setString(1, teamId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                String worldName = rs.getString("home_world");
                if (worldName == null) return null;
                World world = Bukkit.getWorld(worldName);
                if (world == null) return null;
                double x = rs.getDouble("home_x");
                double y = rs.getDouble("home_y");
                double z = rs.getDouble("home_z");
                return new Location(world, x, y, z);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean delHome(String teamId) {
        try (PreparedStatement stmt = db.getConnection().prepareStatement(
                "UPDATE teams SET home_world = NULL, home_x = NULL, home_y = NULL, home_z = NULL WHERE id = ?")) {
            stmt.setString(1, teamId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public List<AuditLogEntry> getAuditLog(String teamId, int page, int pageSize) {
        List<AuditLogEntry> entries = new ArrayList<>();
        try (PreparedStatement stmt = db.getConnection().prepareStatement(
            "SELECT * FROM team_audit_log WHERE team_id = ? ORDER BY timestamp DESC LIMIT ? OFFSET ?"
        )) {
            stmt.setString(1, teamId);
            stmt.setInt(2, pageSize);
            stmt.setInt(3, (page - 1) * pageSize);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                entries.add(new AuditLogEntry(
                    rs.getInt("id"),
                    rs.getString("team_id"),
                    UUID.fromString(rs.getString("executor_uuid")),
                    rs.getString("action"),
                    rs.getString("info"),
                    rs.getLong("timestamp")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return entries;
    }

    public int getAuditLogCount(String teamId) {
        try (PreparedStatement stmt = db.getConnection().prepareStatement(
            "SELECT COUNT(*) FROM team_audit_log WHERE team_id = ?"
        )) {
            stmt.setString(1, teamId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }


    public void logAudit(String teamId, UUID executor, String action, String info) {
        try (PreparedStatement stmt = db.getConnection().prepareStatement(
            "INSERT INTO team_audit_log (team_id, executor_uuid, action, info, timestamp) VALUES (?, ?, ?, ?, ?)"
        )) {
            stmt.setString(1, teamId);
            stmt.setString(2, executor.toString());
            stmt.setString(3, action);
            stmt.setString(4, info);
            stmt.setLong(5, System.currentTimeMillis() / 1000);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public boolean setTag(String teamId, String newTag) {
        try (PreparedStatement stmt = db.getConnection().prepareStatement(
                "UPDATE teams SET tag = ? WHERE id = ?")) {
            stmt.setString(1, newTag);
            stmt.setString(2, teamId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }



    private String generateRandomId() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder id = new StringBuilder();
        Random random = new Random();

        for (int i = 0; i < 8; i++) {
            id.append(chars.charAt(random.nextInt(chars.length())));
        }

        return id.toString();
    }
}
