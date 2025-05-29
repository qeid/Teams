package discord.qeid.commands.admin;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import discord.qeid.Teams;
import discord.qeid.database.TeamManager;
import discord.qeid.model.Team;
import discord.qeid.utils.ColorUtils;
import discord.qeid.utils.MessagesUtil;
import discord.qeid.utils.SoundUtil;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class TeamAdminInfoCommand {

    public static LiteralCommandNode<CommandSourceStack> buildSubcommand() {
        return Commands.literal("info")
            .then(Commands.argument("team", StringArgumentType.word())
                .suggests((ctx, builder) -> {
                    Teams.getInstance().getTeamManager().getAllTeams().stream()
                        .map(Team::getName)
                        .filter(name -> name.toLowerCase().startsWith(builder.getRemainingLowerCase()))
                        .forEach(builder::suggest);
                    return builder.buildFuture();
                })
                .executes(ctx -> {
                    String teamName = StringArgumentType.getString(ctx, "team");
                    return execute(ctx.getSource().getSender(), teamName);
                })
            ).build();
    }

    private static int execute(CommandSender sender, String teamName) {
        TeamManager manager = Teams.getInstance().getTeamManager();
        Team team = manager.getTeamByName(teamName);

        if (team == null) {
            sender.sendMessage(ColorUtils.format("&cThat Team does not exist."));
            ((Player)sender).playSound(((Player)sender).getLocation(), SoundUtil.get("team.sounds.error"), 1.0F, 1.5F);
            return 1;
        }

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                .withZone(ZoneId.systemDefault());

        // header
        List<String> headerLines = MessagesUtil.getMessages().getStringList("admin.info.header");
        for (String line : headerLines) {
            sender.sendMessage(ColorUtils.format(line.replace("%team%", team.getName())));
        }

        // info
        sendSection(sender, "id", "%id%", team.getId());
        sendSection(sender, "name", "%name%", team.getName());
        sendSection(sender, "tag", "%tag%", team.getTag());
        sendSection(sender, "owner", "%owner%", getName(team.getOwner()), "%owner_uuid%", team.getOwner().toString());
        sendSection(sender, "created", "%created%", fmt.format(Instant.ofEpochSecond(team.getCreatedAt())));

        // Home
        var home = manager.getHome(team.getId());
        String homeStr = (home == null)
                ? MessagesUtil.get("team.null")
                : home.getWorld().getName() + " " +
                  String.format("%.2f", home.getX()) + ", " +
                  String.format("%.2f", home.getY()) + ", " +
                  String.format("%.2f", home.getZ());
        String homeLine = MessagesUtil.getMessages().getString("admin.info.section.home")
                .replace("%home%", homeStr)
                .replace("%team%", team.getName());
        sender.sendMessage(ColorUtils.format(homeLine));

        // roles
        sendSection(sender, "admins", "%admins%", formatPlayerList(team.getAdmins()));
        sendSection(sender, "mods", "%mods%", formatPlayerList(team.getMods()));
        sendSection(sender, "members", "%members%", formatPlayerList(team.getMembers()));

        // alog
        String auditLogLine = MessagesUtil.getMessages().getString("admin.info.section.auditlog")
                .replace("%team%", team.getName());
        sender.sendMessage(ColorUtils.format(auditLogLine));

        // bans
        String bansLine = MessagesUtil.getMessages().getString("admin.info.section.bans")
                .replace("%team%", team.getName());
        sender.sendMessage(ColorUtils.format(bansLine));

        return 1;
    }

    private static void sendSection(CommandSender sender, String section, String placeholder, String value) {
        String line = MessagesUtil.getMessages().getString("admin.info.section." + section);
        if (line == null) return;
        sender.sendMessage(ColorUtils.format(line.replace(placeholder, value)));
    }

    private static void sendSection(CommandSender sender, String section, String placeholder1, String value1, String placeholder2, String value2) {
        String line = MessagesUtil.getMessages().getString("admin.info.section." + section);
        if (line == null) return;
        sender.sendMessage(ColorUtils.format(line.replace(placeholder1, value1).replace(placeholder2, value2)));
    }

    private static String formatPlayerList(Set<UUID> players) {
        if (players.isEmpty()) return MessagesUtil.get("team.null");
        StringBuilder sb = new StringBuilder();
        for (UUID uuid : players) {
            String name = getName(uuid);
            sb.append(name).append(", ");
        }
        String out = sb.toString();
        if (out.endsWith(", ")) out = out.substring(0, out.length() - 2);
        return out;
    }

    private static String getName(UUID uuid) {
        OfflinePlayer p = Bukkit.getOfflinePlayer(uuid);
        return p.getName() != null ? p.getName() : uuid.toString();
    }
}
