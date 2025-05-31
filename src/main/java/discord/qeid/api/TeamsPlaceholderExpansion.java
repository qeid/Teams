package discord.qeid.api;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import discord.qeid.Teams;
import discord.qeid.model.Team;

import java.util.Set;
import java.util.UUID;

public class TeamsPlaceholderExpansion extends PlaceholderExpansion {

    /**
     * PlaceholderAPI expansion for Teams plugin.
     * Provides team-related placeholders for players.
     *
     * @see <a href="*/

    @Override
    public @NotNull String getIdentifier() {
        return "teams";
    }

    @Override
    public @NotNull String getAuthor() {
        return "qeid";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.0.0";
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        Team team = Teams.getInstance().getTeamManager().getTeamByPlayer(player.getUniqueId());
        if (team == null) return "";

        String name = team.getName();
        String tag = team.getTag();
        String role = Teams.getInstance().getTeamManager().getRole(team, player.getUniqueId()).name();

        switch (params.toLowerCase()) {
            case "name":
                return name;
            case "name_uppercase":
                return name.toUpperCase();
            case "name_propercase":
                return properCase(name);
            case "tag":
                return tag;
            case "tag_uppercase":
                return tag.toUpperCase();
            case "tag_propercase":
                return properCase(tag);
            case "role":
                return role;
            case "role_uppercase":
                return role.toUpperCase();
            case "role_propercase":
                return properCase(role);
            case "owner":
                return Bukkit.getOfflinePlayer(team.getOwner()).getName();
            case "admins":
                return formatPlayerList(team.getAdmins());
            case "mods":
                return formatPlayerList(team.getMods());
            case "members":
                return formatPlayerList(team.getMembers());
            case "member_count":
                return String.valueOf(team.getMembers().size() + team.getAdmins().size() + team.getMods().size() + 1); // +1 for owner
            case "created":
                return java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
                    .withZone(java.time.ZoneId.systemDefault())
                    .format(java.time.Instant.ofEpochSecond(team.getCreatedAt()));
            case "home":
                var loc = Teams.getInstance().getTeamManager().getHome(team.getId());
                return (loc == null) ? "" : loc.getWorld().getName() + " " +
                    String.format("%.2f", loc.getX()) + ", " +
                    String.format("%.2f", loc.getY()) + ", " +
                    String.format("%.2f", loc.getZ());
            case "ban_count":
                return String.valueOf(Teams.getInstance().getTeamManager().getBannedPlayers(team.getId()).size());
            case "is_owner":
                return team.getOwner().equals(player.getUniqueId()) ? "Yes" : "No";
            case "is_admin":
                return team.getAdmins().contains(player.getUniqueId()) ? "Yes" : "No";
            case "is_mod":
                return team.getMods().contains(player.getUniqueId()) ? "Yes" : "No";
            case "is_member":
                return team.getMembers().contains(player.getUniqueId()) ? "Yes" : "No";
            default:
                return "";
        }
    }

    private String properCase(String s) {
        if (s == null || s.isEmpty()) return "";
        if (s.length() == 1) return s.toUpperCase();
        return s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
    }


    private String formatPlayerList(Set<UUID> uuids) {
        if (uuids.isEmpty()) return "";
            StringBuilder sb = new StringBuilder();
            for (UUID uuid : uuids) {
                String name = Bukkit.getOfflinePlayer(uuid).getName();
                if (name != null) sb.append(name).append(", ");
            }
            if (sb.length() > 2) sb.setLength(sb.length() - 2);
            return sb.toString();
        }
}