package discord.qeid.commands.admin;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import discord.qeid.Teams;
import discord.qeid.database.TeamManager;
import discord.qeid.model.AuditLogEntry;
import discord.qeid.model.Team;
import discord.qeid.utils.ColorUtils;
import discord.qeid.utils.MessagesUtil;
import discord.qeid.utils.SoundUtil;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class TeamAdminAuditLogCommand {

    private static final int PAGE_SIZE = 10;

    public static LiteralCommandNode<CommandSourceStack> buildSubcommand() {
        return Commands.literal("auditlog")
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
                    return showLog(ctx.getSource().getSender(), teamName, 1);
                })
                .then(Commands.argument("page", IntegerArgumentType.integer(1))
                    .executes(ctx -> {
                        String teamName = StringArgumentType.getString(ctx, "team");
                        int page = IntegerArgumentType.getInteger(ctx, "page");
                        return showLog(ctx.getSource().getSender(), teamName, page);
                    })
                )
            ).build();
    }

    private static int showLog(CommandSender sender, String teamName, int page) {
        TeamManager teamManager = Teams.getInstance().getTeamManager();
        Team team = teamManager.getTeamByName(teamName);

        if (team == null) {
            sender.sendMessage(ColorUtils.format("&cThat Team does not exist."));
            ((Player)sender).playSound(((Player)sender).getLocation(), SoundUtil.get("team.sounds.error"), 1.0F, 1.5F);
            return 1;
        }

        int total = teamManager.getAuditLogCount(team.getId());
        int maxPage = Math.max(1, (int) Math.ceil(total / (double) PAGE_SIZE));
        if (page > maxPage) page = maxPage;

        List<AuditLogEntry> entries = teamManager.getAuditLog(team.getId(), page, PAGE_SIZE);

        // header
        List<String> headerLines = MessagesUtil.getMessages().getStringList("admin.auditlog.header");
        for (String line : headerLines) {
            sender.sendMessage(ColorUtils.format(
                line.replace("%team%", team.getName())
                    .replace("%page%", String.valueOf(page))
                    .replace("%maxpage%", String.valueOf(maxPage))
            ));
        }

        // date format
        String dateFormat = MessagesUtil.getMessages().getString("admin.auditlog.date-format", "yyyy-MM-dd HH:mm:ss");
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern(dateFormat)
                .withZone(ZoneId.systemDefault());

        // entries
        String entryFormat = MessagesUtil.getMessages().getString("admin.auditlog.entry");
        String entryHoverFormat = String.join("\n", MessagesUtil.getMessages().getStringList("admin.auditlog.entry-hover"));

        if (entries.isEmpty()) {
            List<String> noEntriesLines = MessagesUtil.getMessages().getStringList("admin.auditlog.no-entries");
            for (String line : noEntriesLines) {
                sender.sendMessage(ColorUtils.format(line));
            }
        } else {
            for (AuditLogEntry entry : entries) {
                String executorName = Bukkit.getOfflinePlayer(entry.executor()).getName();
                String date = fmt.format(Instant.ofEpochSecond(entry.timestamp()));
                String summary = entryFormat
                        .replace("%date%", date)
                        .replace("%executor%", executorName)
                        .replace("%action%", entry.action())
                        .replace("%info%", entry.info() == null ? MessagesUtil.get("team.null") : entry.info());

                String hover = entryHoverFormat
                        .replace("%date%", date)
                        .replace("%executor%", executorName)
                        .replace("%action%", entry.action())
                        .replace("%info%", entry.info() == null ? MessagesUtil.get("team.null") : entry.info());

                Component line = ColorUtils.format(summary)
                        .hoverEvent(HoverEvent.showText(ColorUtils.format(hover)))
                        .clickEvent(ClickEvent.copyToClipboard(entry.info() == null ? "" : entry.info()));

                sender.sendMessage(line);
            }
        }

        // nav
        String navPrev = MessagesUtil.getMessages().getString("admin.auditlog.nav.prev", "&6[ <<< ]");
        String navNext = MessagesUtil.getMessages().getString("admin.auditlog.nav.next", "&6[ >>> ]");
        String navSeparator = MessagesUtil.getMessages().getString("admin.auditlog.nav.separator", " ");
        String navHoverPrev = MessagesUtil.getMessages().getString("admin.auditlog.nav.prev-tooltip", "&6&oBack to previous page");
        String navHoverNext = MessagesUtil.getMessages().getString("admin.auditlog.nav.next-tooltip", "&6&oGo to next page");

        Component prev = (page > 1)
                ? ColorUtils.format(navPrev)
                    .clickEvent(ClickEvent.runCommand("/teama auditlog " + team.getName() + " " + (page - 1)))
                    .hoverEvent(HoverEvent.showText(ColorUtils.format(navHoverPrev)))
                : Component.empty();

        Component next = (page < maxPage)
                ? ColorUtils.format(navNext)
                    .clickEvent(ClickEvent.runCommand("/teama auditlog " + team.getName() + " " + (page + 1)))
                    .hoverEvent(HoverEvent.showText(ColorUtils.format(navHoverNext)))
                : Component.empty();

        List<String> footerLines = MessagesUtil.getMessages().getStringList("admin.auditlog.footer");
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
