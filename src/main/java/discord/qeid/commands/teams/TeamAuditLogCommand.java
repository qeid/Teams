package discord.qeid.commands.teams;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import discord.qeid.Teams;
import discord.qeid.database.TeamManager;
import discord.qeid.model.AuditLogEntry;
import discord.qeid.model.Team;
import discord.qeid.model.TeamRoles;
import discord.qeid.utils.ColorUtils;
import discord.qeid.utils.MessagesUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;


public class TeamAuditLogCommand {

    private static final int PAGE_SIZE = 10;

    public static LiteralCommandNode<CommandSourceStack> buildSubcommand() {
        return Commands.literal("auditlog")
                .executes(ctx -> showLog(ctx.getSource().getSender(), 1))
                .then(Commands.argument("page", IntegerArgumentType.integer(1))
                        .executes(ctx -> {
                            int page = IntegerArgumentType.getInteger(ctx, "page");
                            return showLog(ctx.getSource().getSender(), page);
                        })
                ).build();
    }

    private static int showLog(CommandSender sender, int page) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ColorUtils.format("&cOnly players can use this command."));
            return Command.SINGLE_SUCCESS;
        }
        TeamManager teamManager = Teams.getInstance().getTeamManager();
        Team team = teamManager.getTeamByPlayer(player.getUniqueId());
        if (team == null) {
            player.sendMessage(MessagesUtil.get("team.info.not-in-team"));
            return Command.SINGLE_SUCCESS;
        }

        TeamRoles role = TeamManager.getRole(team, player.getUniqueId());
        if (role != TeamRoles.ADMIN && role != TeamRoles.OWNER) {
            player.sendMessage(MessagesUtil.get("team.auditlog.no-permission"));
            return 1;
        }

        int total = teamManager.getAuditLogCount(team.getId());
        int maxPage = Math.max(1, (int) Math.ceil(total / (double) PAGE_SIZE));
        if (page > maxPage) page = maxPage;

        List<AuditLogEntry> entries = teamManager.getAuditLog(team.getId(), page, PAGE_SIZE);

        // Load messages from messages.yml
        String header = MessagesUtil.get("team.auditlog.header")
                .replace("%page%", String.valueOf(page))
                .replace("%maxpage%", String.valueOf(maxPage));
        String noEntries = MessagesUtil.get("team.auditlog.no-entries");
        String entryFormat = MessagesUtil.get("team.auditlog.entry");
        String entryHover = MessagesUtil.get("team.auditlog.entry-hover");
        String dateFormat = MessagesUtil.getMessages().getString("team.auditlog.date-format", "yyyy-MM-dd HH:mm:ss");
        String navPrev = MessagesUtil.get("team.auditlog.nav-prev");
        String navNext = MessagesUtil.get("team.auditlog.nav-next");
        String navSeparator = MessagesUtil.get("team.auditlog.nav-separator");
        String navHoverPrev = MessagesUtil.get("team.auditlog.nav-hover-prev");
        String navHoverNext = MessagesUtil.get("team.auditlog.nav-hover-next");

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern(dateFormat)
                .withZone(ZoneId.systemDefault());

        player.sendMessage(ColorUtils.format(header));

        if (entries.isEmpty()) {
            player.sendMessage(ColorUtils.format(noEntries));
            return Command.SINGLE_SUCCESS;
        }

        for (AuditLogEntry entry : entries) {
            String executorName = Bukkit.getOfflinePlayer(entry.executor()).getName();
            String date = fmt.format(Instant.ofEpochSecond(entry.timestamp()));
            String info = entry.info() == null ? "" : entry.info();

            String summary = entryFormat
                    .replace("%date%", date)
                    .replace("%executor%", executorName)
                    .replace("%action%", entry.action())
                    .replace("%info%", info);

            String hover = entryHover
                    .replace("%date%", date)
                    .replace("%executor%", executorName)
                    .replace("%action%", entry.action())
                    .replace("%info%", info);

            Component line = ColorUtils.format(summary)
                    .hoverEvent(HoverEvent.showText(ColorUtils.format(hover)))
                    .clickEvent(ClickEvent.copyToClipboard(info));

            player.sendMessage(line);
        }

        // Navigation
        if (maxPage > 1) {
            Component nav = Component.empty();
            if (page > 1) {
                nav = nav.append(
                        ColorUtils.format(navPrev)
                                .clickEvent(ClickEvent.runCommand("/team auditlog " + (page - 1)))
                                .hoverEvent(HoverEvent.showText(ColorUtils.format(navHoverPrev)))
                );
            }
            nav = nav.append(ColorUtils.format(navSeparator));
            nav = nav.append(Component.text("Page " + page + "/" + maxPage, NamedTextColor.GRAY));
            nav = nav.append(ColorUtils.format(navSeparator));
            if (page < maxPage) {
                nav = nav.append(
                        ColorUtils.format(navNext)
                                .clickEvent(ClickEvent.runCommand("/team auditlog " + (page + 1)))
                                .hoverEvent(HoverEvent.showText(ColorUtils.format(navHoverNext)))
                );
            }
            player.sendMessage(nav);
        }

        return Command.SINGLE_SUCCESS;
    }
}