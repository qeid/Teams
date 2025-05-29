package discord.qeid.commands.admin;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import discord.qeid.Teams;
import discord.qeid.database.TeamManager;
import discord.qeid.model.Team;
import discord.qeid.model.TeamBanInfo;
import discord.qeid.utils.ColorUtils;
import discord.qeid.utils.DurationUtil;
import discord.qeid.utils.MessagesUtil;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class TeamAdminBansCommand {

    private static final int PAGE_SIZE = 10;

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
                    return showBans(ctx.getSource().getSender(), teamName, 1);
                })
                .then(Commands.argument("page", IntegerArgumentType.integer(1))
                    .executes(ctx -> {
                        String teamName = StringArgumentType.getString(ctx, "team");
                        int page = IntegerArgumentType.getInteger(ctx, "page");
                        return showBans(ctx.getSource().getSender(), teamName, page);
                    })
                )
            ).build();
    }

    private static int showBans(CommandSender sender, String teamName, int page) {
        TeamManager manager = Teams.getInstance().getTeamManager();
        Team team = manager.getTeamByName(teamName);

        if (team == null) {
            sender.sendMessage(ColorUtils.format("&cThat team does not exist."));
            return 1;
        }

        // Get all bans for the team (with executor, reason, expires, executed_at)
        List<TeamBanInfo> bans = manager.getAllBans(team.getId());
        int total = bans.size();
        int maxPage = Math.max(1, (int) Math.ceil(total / (double) PAGE_SIZE));
        if (page > maxPage) page = maxPage;

        // Header
        List<String> headerLines = MessagesUtil.getMessages().getStringList("admin.bans.header");
        for (String line : headerLines) {
            sender.sendMessage(ColorUtils.format(
                line.replace("%team%", team.getName())
                    .replace("%page%", String.valueOf(page))
                    .replace("%maxpage%", String.valueOf(maxPage))
            ));
        }

        // Date format
        String dateFormat = MessagesUtil.getMessages().getString("admin.bans.date-format", "yyyy-MM-dd HH:mm:ss");
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern(dateFormat)
                .withZone(ZoneId.systemDefault());

        // Entries
        String entryFormat = MessagesUtil.getMessages().getString("admin.bans.entry");
        String entryHoverFormat = String.join("\n", MessagesUtil.getMessages().getStringList("admin.bans.entry-hover"));

        if (bans.isEmpty()) {
            List<String> noEntriesLines = MessagesUtil.getMessages().getStringList("admin.bans.no-entries");
            for (String line : noEntriesLines) {
                sender.sendMessage(ColorUtils.format(line));
            }
        } else {
            int start = (page - 1) * PAGE_SIZE;
            int end = Math.min(start + PAGE_SIZE, bans.size());
            List<TeamBanInfo> pageBans = bans.subList(start, end);

            for (TeamBanInfo info : pageBans) {
                OfflinePlayer p = Bukkit.getOfflinePlayer(info.player());
                String playerName = p.getName() != null ? p.getName() : info.player().toString();
                String reason = info.reason() == null ? "&8(none)" : info.reason();
                String executorName = info.executor() != null ? Bukkit.getOfflinePlayer(info.executor()).getName() : MessagesUtil.get("team.null");
                String expires = info.expiresAt() > 0
                        ? fmt.format(Instant.ofEpochSecond(info.expiresAt()))
                        : "Permanent";
                String duration = info.expiresAt() > 0
                        ? DurationUtil.formatFullDuration(info.expiresAt() - info.executedAt())
                        : "Permanent";
                String dateUnbanned = info.expiresAt() > 0
                        ? fmt.format(Instant.ofEpochSecond(info.expiresAt()))
                        : "Never";
                String dateBanned = info.executedAt() > 0
                        ? fmt.format(Instant.ofEpochSecond(info.executedAt()))
                        : MessagesUtil.get("team.null");

                String summary = entryFormat
                        .replace("%player%", playerName)
                        .replace("%reason%", reason)
                        .replace("%expires%", expires)
                        .replace("%date%", dateBanned);

                String hover = entryHoverFormat
                        .replace("%player%", playerName)
                        .replace("%reason%", reason)
                        .replace("%duration%", duration)
                        .replace("%date_unbanned%", dateUnbanned)
                        .replace("%executor%", executorName)
                        .replace("%date_banned%", dateBanned);

                Component line = ColorUtils.format(summary)
                        .hoverEvent(HoverEvent.showText(ColorUtils.format(hover)))
                        .clickEvent(ClickEvent.copyToClipboard(reason));

                sender.sendMessage(line);
            }
        }

        // Navigation
        String navPrev = MessagesUtil.getMessages().getString("admin.bans.nav.prev", "&6[ <<< ]");
        String navNext = MessagesUtil.getMessages().getString("admin.bans.nav.next", "&6[ >>> ]");
        String navSeparator = MessagesUtil.getMessages().getString("admin.bans.nav.separator", " ");
        String navHoverPrev = MessagesUtil.getMessages().getString("admin.bans.nav.prev-tooltip", "&6&oBack to previous page");
        String navHoverNext = MessagesUtil.getMessages().getString("admin.bans.nav.next-tooltip", "&6&oGo to next page");

        Component prev = (page > 1)
                ? ColorUtils.format(navPrev)
                    .clickEvent(ClickEvent.runCommand("/teama bans \"" + team.getName() + "\" " + (page - 1)))
                    .hoverEvent(HoverEvent.showText(ColorUtils.format(navHoverPrev)))
                : Component.empty();

        Component next = (page < maxPage)
                ? ColorUtils.format(navNext)
                    .clickEvent(ClickEvent.runCommand("/teama bans \"" + team.getName() + "\" " + (page + 1)))
                    .hoverEvent(HoverEvent.showText(ColorUtils.format(navHoverNext)))
                : Component.empty();

        List<String> footerLines = MessagesUtil.getMessages().getStringList("admin.bans.footer");
        for (String footerTemplate : footerLines) {
            Component footer = ColorUtils.format(
                footerTemplate
                    .replace("%prev%", "")
                    .replace("%next%", "")
                    .replace("%separator%", navSeparator)
                    .replace("%page%", String.valueOf(page))
                    .replace("%maxpage%", String.valueOf(maxPage))
            );
            footer = footer
                .replaceText(builder -> builder.matchLiteral("%prev%").replacement(prev))
                .replaceText(builder -> builder.matchLiteral("%next%").replacement(next));
            sender.sendMessage(footer);
        }

        return 1;
    }
}
