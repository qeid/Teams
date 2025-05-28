package discord.qeid.utils;

import discord.qeid.Teams;
import discord.qeid.database.TeamManager;
import discord.qeid.model.AuditLogEntry;
import discord.qeid.model.Team;
import discord.qeid.model.TeamBanInfo;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class DebugUtil {

    public static void sendTeamDebugInfo(CommandSender sender, Team team) {
        if (team == null) {
            sender.sendMessage(ColorUtils.format("&c[DEBUG] No team data found."));
            return;
        }

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                .withZone(ZoneId.systemDefault());

        sender.sendMessage(ColorUtils.format("&e[DEBUG] Team Info Dump:"));
        sender.sendMessage(ColorUtils.format("&7ID: &f" + team.getId()));
        sender.sendMessage(ColorUtils.format("&7Name: &f" + team.getName()));
        sender.sendMessage(ColorUtils.format("&7Tag: &f[" + team.getTag() + "]"));
        sender.sendMessage(ColorUtils.format("&7Owner: &f" + getName(team.getOwner()) + " &8(" + team.getOwner() + ")"));
        sender.sendMessage(ColorUtils.format("&7Created: &f" + fmt.format(Instant.ofEpochSecond(team.getCreatedAt()))));

        sendRoleList(sender, "Admins", team.getAdmins());
        sendRoleList(sender, "Mods", team.getMods());
        sendRoleList(sender, "Members", team.getMembers());

        // Ban list
        TeamManager manager = Teams.getInstance().getTeamManager();
        List<String> banLines = new ArrayList<>();
        try {
            // You may want to add a getBannedPlayers method to TeamManager if you haven't already
            List<UUID> banned = manager.getBannedPlayers(team.getId());
            for (UUID uuid : banned) {
                TeamBanInfo info = manager.getBanInfo(team.getId(), uuid);
                String name = getName(uuid);
                String reason = info != null ? info.reason() : "Unknown";
                String expires = info != null && info.expiresAt() > 0
                        ? fmt.format(Instant.ofEpochSecond(info.expiresAt()))
                        : "Permanent";
                banLines.add("&c" + name + " &7(" + uuid + ") &8- &cReason: &f" + reason + " &8| &cExpires: &f" + expires);
            }
        } catch (Exception e) {
            banLines.add("&c[Error loading ban list]");
        }
        if (banLines.isEmpty()) {
            sender.sendMessage(ColorUtils.format("&7Bans: &8(none)"));
        } else {
            sender.sendMessage(ColorUtils.format("&7Bans:"));
            for (String line : banLines) {
                sender.sendMessage(ColorUtils.format("  " + line));
            }
        }

        // Audit log summary (last 5 entries)
        List<AuditLogEntry> audit = manager.getAuditLog(team.getId(), 1, 5);
        if (audit.isEmpty()) {
            sender.sendMessage(ColorUtils.format("&7Audit Log: &8(none)"));
        } else {
            sender.sendMessage(ColorUtils.format("&7Audit Log (last 5):"));
            for (AuditLogEntry entry : audit) {
                String date = fmt.format(Instant.ofEpochSecond(entry.timestamp()));
                String actor = getName(entry.executor());
                sender.sendMessage(ColorUtils.format("  &8[&7" + date + "&8] &f" + actor + " &7- &e" + entry.action() + " &8| &7" + entry.info()));
            }
        }
    }

    private static void sendRoleList(CommandSender sender, String label, Iterable<UUID> list) {
        StringBuilder builder = new StringBuilder("&7" + label + ": &f");
        boolean empty = true;
        for (UUID uuid : list) {
            String name = getName(uuid);
            builder.append(name).append(", ");
            empty = false;
        }
        if (empty) {
            sender.sendMessage(ColorUtils.format("&7" + label + ": &8(none)"));
        } else {
            String output = builder.toString();
            if (output.endsWith(", ")) {
                output = output.substring(0, output.length() - 2);
            }
            sender.sendMessage(ColorUtils.format(output));
        }
    }

    private static String getName(UUID uuid) {
        OfflinePlayer p = Bukkit.getOfflinePlayer(uuid);
        return p.getName() != null ? p.getName() : uuid.toString();
    }
}
