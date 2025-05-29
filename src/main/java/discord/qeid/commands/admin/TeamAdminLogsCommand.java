package discord.qeid.commands.admin;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import discord.qeid.Teams;
import discord.qeid.model.AdminLogEntry;
import discord.qeid.utils.ColorUtils;
import discord.qeid.utils.MessagesUtil;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class TeamAdminLogsCommand {

    private static final int PAGE_SIZE = 10;

    public static LiteralCommandNode<CommandSourceStack> buildSubcommand() {
        return Commands.literal("logs")
            .executes(ctx -> showLogs(ctx.getSource().getSender(), 1))
            .then(Commands.argument("page", IntegerArgumentType.integer(1))
                .executes(ctx -> {
                    int page = IntegerArgumentType.getInteger(ctx, "page");
                    return showLogs(ctx.getSource().getSender(), page);
                })
            ).build();
    }

    private static int showLogs(CommandSender sender, int page) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ColorUtils.format("&cOnly players can use this command."));
            return 1;
        }

        var logManager = Teams.getInstance().getAdminLogManager();
        int total = logManager.getLogCount();
        int maxPage = Math.max(1, (int) Math.ceil(total / (double) PAGE_SIZE));
        if (page > maxPage) page = maxPage;

        List<AdminLogEntry> logs = logManager.getLogs(page, PAGE_SIZE);

        // header
        List<String> headerLines = MessagesUtil.getMessages().getStringList("admin.logs.header");
        for (String line : headerLines) {
            player.sendMessage(ColorUtils.format(
                line.replace("%page%", String.valueOf(page))
                    .replace("%maxpage%", String.valueOf(maxPage))
            ));
        }

        // date format
        String dateFormat = MessagesUtil.getMessages().getString("admin.logs.date-format", "yyyy-MM-dd HH:mm:ss");
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern(dateFormat)
                .withZone(ZoneId.systemDefault());

        // entries
        String entryFormat = MessagesUtil.getMessages().getString("admin.logs.entry");
        String entryHoverFormat = String.join("\n", MessagesUtil.getMessages().getStringList("admin.logs.entry-hover"));

        if (logs.isEmpty()) {
            List<String> noEntriesLines = MessagesUtil.getMessages().getStringList("admin.logs.no-entries");
            for (String line : noEntriesLines) {
                player.sendMessage(ColorUtils.format(line));
            }
        } else {
            for (AdminLogEntry entry : logs) {
                String date = fmt.format(Instant.ofEpochSecond(entry.timestamp()));
                String summary = entryFormat
                        .replace("%date%", date)
                        .replace("%executor%", entry.executorName())
                        .replace("%action%", entry.action())
                        .replace("%team%", entry.teamName() == null ? MessagesUtil.get("team.null") : entry.teamName())
                        .replace("%target%", entry.targetName() == null ? MessagesUtil.get("team.null") : entry.targetName())
                        .replace("%reason%", entry.reason() == null ? MessagesUtil.get("team.null") : entry.reason());

                String hover = entryHoverFormat
                        .replace("%date%", date)
                        .replace("%executor%", entry.executorName())
                        .replace("%action%", entry.action())
                        .replace("%team%", entry.teamName() == null ? MessagesUtil.get("team.null") : entry.teamName())
                        .replace("%target%", entry.targetName() == null ? MessagesUtil.get("team.null") : entry.targetName())
                        .replace("%reason%", entry.reason() == null ? MessagesUtil.get("team.null") : entry.reason());

                Component line = ColorUtils.format(summary)
                        .hoverEvent(HoverEvent.showText(ColorUtils.format(hover)))
                        .clickEvent(ClickEvent.copyToClipboard(entry.reason() == null ? "" : entry.reason()));

                player.sendMessage(line);
            }
        }

        // nmav
        String navPrev = MessagesUtil.getMessages().getString("admin.logs.nav.prev", "&6[ <<< ]");
        String navNext = MessagesUtil.getMessages().getString("admin.logs.nav.next", "&6[ >>> ]");
        String navSeparator = MessagesUtil.getMessages().getString("admin.logs.nav.separator", " ");
        String navHoverPrev = MessagesUtil.getMessages().getString("admin.logs.nav.prev-tooltip", "&6&oBack to previous page");
        String navHoverNext = MessagesUtil.getMessages().getString("admin.logs.nav.next-tooltip", "&6&oGo to next page");

        Component prev = (page > 1)
                ? ColorUtils.format(navPrev)
                    .clickEvent(ClickEvent.runCommand("/teama logs " + (page - 1)))
                    .hoverEvent(HoverEvent.showText(ColorUtils.format(navHoverPrev)))
                : Component.empty();

        Component next = (page < maxPage)
                ? ColorUtils.format(navNext)
                    .clickEvent(ClickEvent.runCommand("/teama logs " + (page + 1)))
                    .hoverEvent(HoverEvent.showText(ColorUtils.format(navHoverNext)))
                : Component.empty();

        List<String> footerLines = MessagesUtil.getMessages().getStringList("admin.logs.footer");
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
            player.sendMessage(footer);
        }

        return 1;
    }
}
