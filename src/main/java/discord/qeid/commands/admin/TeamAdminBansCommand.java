package discord.qeid.commands.admin;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import discord.qeid.Teams;
import discord.qeid.database.TeamManager;
import discord.qeid.model.Team;
import discord.qeid.model.TeamBanInfo;
import discord.qeid.utils.ColorUtils;
import discord.qeid.utils.MessagesUtil;
import discord.qeid.utils.SoundUtil;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

public class TeamAdminBansCommand {

    public static LiteralCommandNode<CommandSourceStack> buildSubcommand() {
        return Commands.literal("bans")
            .then(Commands.argument("team", StringArgumentType.greedyString())
                .suggests((ctx, builder) -> {
                    Teams.getInstance().getTeamManager().getAllTeams().stream()
                        .map(Team::getName)
                        .filter(name -> name.toLowerCase().startsWith(builder.getRemainingLowerCase()))
                        .forEach(builder::suggest);
                    return builder.buildFuture();
                })
                .executes(ctx -> {
                    String teamName = StringArgumentType.getString(ctx, "team");
                    return showBans(ctx.getSource().getSender(), teamName);
                })
            ).build();
    }

    private static int showBans(CommandSender sender, String teamName) {
        TeamManager manager = Teams.getInstance().getTeamManager();
        Team team = manager.getTeamByName(teamName);

        if (team == null) {
            sender.sendMessage(ColorUtils.format("&cThat Team does not exist."));
            ((Player)sender).playSound(((Player)sender).getLocation(), SoundUtil.get("team.sounds.error"), 1.0F, 1.5F);
            return 1;
        }

        // header
        List<String> headerLines = MessagesUtil.getMessages().getStringList("admin.bans.header");
        for (String line : headerLines) {
            sender.sendMessage(ColorUtils.format(line.replace("%team%", team.getName())));

        }

        // ban list
        List<UUID> banned = manager.getBannedPlayers(team.getId());
        String entryFormat = MessagesUtil.getMessages().getString("admin.bans.entry");
        String none = MessagesUtil.getMessages().getString("admin.bans.no-entries", "&8(none)");
        String dateFormat = MessagesUtil.getMessages().getString("admin.auditlog.date-format", "yyyy-MM-dd HH:mm:ss");
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern(dateFormat)
                .withZone(ZoneId.systemDefault());

        if (banned.isEmpty()) {
            List<String> noEntriesLines = MessagesUtil.getMessages().getStringList("admin.bans.no-entries");
            for (String line : noEntriesLines) {
                sender.sendMessage(ColorUtils.format(line));
            }
        } else {
            for (UUID uuid : banned) {
                OfflinePlayer p = Bukkit.getOfflinePlayer(uuid);
                TeamBanInfo info = manager.getBanInfo(team.getId(), uuid);
                String reason = info != null ? info.reason() : MessagesUtil.get("team.null");
                String expires = info != null && info.expiresAt() > 0
                        ? fmt.format(Instant.ofEpochSecond(info.expiresAt()))
                        : "Permanent";
                String entry = entryFormat
                        .replace("%player%", p.getName() != null ? p.getName() : uuid.toString())
                        .replace("%uuid%", uuid.toString())
                        .replace("%reason%", reason)
                        .replace("%expires%", expires);
                sender.sendMessage(ColorUtils.format(entry));
            }
        }

        // footer
        List<String> footerLines = MessagesUtil.getMessages().getStringList("admin.bans.footer");
        for (String line : footerLines) {
            sender.sendMessage(ColorUtils.format(line));
        }

        return 1;
    }
}
