package discord.qeid.utils;

import discord.qeid.model.Team;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import discord.qeid.utils.ColorUtils;

import java.util.UUID;

public class DebugUtil {

    public static void sendTeamDebugInfo(CommandSender sender, Team team) {
        if (team == null) {
            sender.sendMessage(ColorUtils.format("&c[DEBUG] No team data found."));
            return;
        }

        sender.sendMessage(ColorUtils.format("&e[DEBUG] Team Info Dump:"));
        sender.sendMessage(ColorUtils.format("&7ID: &f" + team.getId()));
        sender.sendMessage(ColorUtils.format("&7Name: &f" + team.getName()));
        sender.sendMessage(ColorUtils.format("&7Tag: &f[" + team.getTag() + "]"));
        sender.sendMessage(ColorUtils.format("&7Owner: &f" + Bukkit.getOfflinePlayer(team.getOwner()).getName() + " &8(" + team.getOwner() + ")"));

        sendRoleList(sender, "Admins", team.getAdmins());
        sendRoleList(sender, "Mods", team.getMods());
        sendRoleList(sender, "Members", team.getMembers());
    }

    private static void sendRoleList(CommandSender sender, String label, Iterable<UUID> list) {
        StringBuilder builder = new StringBuilder("&7" + label + ": &f");

        boolean empty = true;
        for (UUID uuid : list) {
            String name = Bukkit.getOfflinePlayer(uuid).getName();
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
}
