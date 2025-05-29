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
import discord.qeid.utils.SoundUtil;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

public class TeamAuditLogCommand {

    private static final int PAGE_SIZE = 10;

    public static LiteralCommandNode<CommandSourceStack> buildSubcommand() {
        return Commands.literal("auditlog")
                .executes(ctx -> showLogAsync(ctx.getSource().getSender(), 1))
                .then(Commands.argument("page", IntegerArgumentType.integer(1))
                        .executes(ctx -> {
                            int page = IntegerArgumentType.getInteger(ctx, "page");
                            return showLogAsync(ctx.getSource().getSender(), page);
                        })
                ).build();
    }

    private static int showLogAsync(CommandSender sender, int page) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ColorUtils.format("&cOnly players can use this command."));
            return Command.SINGLE_SUCCESS;
        }
        UUID playerId = player.getUniqueId();

        Bukkit.getScheduler().runTaskAsynchronously(Teams.getInstance(), () -> {
            TeamManager teamManager = Teams.getInstance().getTeamManager();
            Team team = teamManager.getTeamByPlayer(playerId);

            if (team == null) {
                Bukkit.getScheduler().runTask(Teams.getInstance(), () -> {
                    player.sendMessage(MessagesUtil.get("team.info.not-in-team"));
                    player.playSound(player.getLocation(), SoundUtil.get("team.sounds.error"), 1.0F, 1.5F);
                });
                return;
            }

            TeamRoles role = TeamManager.getRole(team, playerId);
            if (role != TeamRoles.ADMIN && role != TeamRoles.OWNER) {
                Bukkit.getScheduler().runTask(Teams.getInstance(), () -> {
                    player.sendMessage(MessagesUtil.get("team.auditlog.no-permission"));
                    player.playSound(player.getLocation(), SoundUtil.get("team.sounds.error"), 1.0F, 1.5F);
                });
                return;
            }

            int total = teamManager.getAuditLogCount(team.getId());
            int maxPage = Math.max(1, (int) Math.ceil(total / (double) PAGE_SIZE));
            int safePage = Math.min(page, maxPage);

            List<AuditLogEntry> entries = teamManager.getAuditLog(team.getId(), safePage, PAGE_SIZE);

            Bukkit.getScheduler().runTask(Teams.getInstance(), () -> {
                // head
                List<String> headerLines = MessagesUtil.getMessages().getStringList("team.auditlog.pages.header");
                for (String line : headerLines) {
                    player.sendMessage(ColorUtils.format(
                        line.replace("%page%", String.valueOf(safePage))
                            .replace("%maxpage%", String.valueOf(maxPage))
                    ));
                }

                // date
                String dateFormat = MessagesUtil.getMessages().getString("team.auditlog.date-format", "yyyy-MM-dd HH:mm:ss");
                DateTimeFormatter fmt = DateTimeFormatter.ofPattern(dateFormat)
                        .withZone(ZoneId.systemDefault());

                // e
                String entryFormat = MessagesUtil.getMessages().getString("team.auditlog.pages.entry");
                String entryHoverFormat = String.join("\n", MessagesUtil.getMessages().getStringList("team.auditlog.pages.entry-hover"));

                if (entries.isEmpty()) {
                    List<String> noEntriesLines = MessagesUtil.getMessages().getStringList("team.auditlog.pages.no-entries");
                    for (String line : noEntriesLines) {
                        player.sendMessage(ColorUtils.format(line));
                    }
                } else {
                    for (AuditLogEntry entry : entries) {
                        String executorName = Bukkit.getOfflinePlayer(entry.executor()).getName();
                        String date = fmt.format(Instant.ofEpochSecond(entry.timestamp()));
                        String info = entry.info() == null ? "" : entry.info();

                        String summary = entryFormat
                                .replace("%date%", date)
                                .replace("%executor%", executorName)
                                .replace("%action%", entry.action())
                                .replace("%info%", info);

                        String hover = entryHoverFormat
                                .replace("%date%", date)
                                .replace("%executor%", executorName)
                                .replace("%action%", entry.action())
                                .replace("%info%", info);

                        Component line = ColorUtils.format(summary)
                                .hoverEvent(HoverEvent.showText(ColorUtils.format(hover)))
                                .clickEvent(ClickEvent.copyToClipboard(info));

                        player.sendMessage(line);
                    }
                }

                // nabv
                String navPrev = MessagesUtil.getMessages().getString("team.auditlog.pages.nav.prev", "&#db7dac&l[ <<< ]");
                String navNext = MessagesUtil.getMessages().getString("team.auditlog.pages.nav.next", "&#db7dac&l[ >>> ]");
                String navSeparator = MessagesUtil.getMessages().getString("team.auditlog.pages.nav.separator", " &8| ");
                String navHoverPrev = MessagesUtil.getMessages().getString("team.auditlog.pages.nav.prev-tooltip", "&#db7dac&oBack to previous page");
                String navHoverNext = MessagesUtil.getMessages().getString("team.auditlog.pages.nav.next-tooltip", "&#db7dac&oGo to next page");

                Component prev = (safePage > 1)
                        ? ColorUtils.format(navPrev)
                            .clickEvent(ClickEvent.runCommand("/team auditlog " + (safePage - 1)))
                            .hoverEvent(HoverEvent.showText(ColorUtils.format(navHoverPrev)))
                        : Component.empty();

                Component next = (safePage < maxPage)
                        ? ColorUtils.format(navNext)
                            .clickEvent(ClickEvent.runCommand("/team auditlog " + (safePage + 1)))
                            .hoverEvent(HoverEvent.showText(ColorUtils.format(navHoverNext)))
                        : Component.empty();

                List<String> footerLines = MessagesUtil.getMessages().getStringList("team.auditlog.pages.footer");
                for (String footerTemplate : footerLines) {
                    String line = footerTemplate
                        .replace("%separator%", navSeparator)
                        .replace("%page%", String.valueOf(safePage))
                        .replace("%maxpage%", String.valueOf(maxPage));
                    String[] partsPrev = line.split("%prev%", -1);
                    Component footer = Component.empty();

                    if (partsPrev.length > 1) {
                        footer = footer.append(ColorUtils.format(partsPrev[0]));
                        footer = footer.append(prev);
                        line = partsPrev[1];
                    } else {
                        line = partsPrev[0];
                    }

                    String[] partsNext = line.split("%next%", -1);
                    if (partsNext.length > 1) {
                        footer = footer.append(ColorUtils.format(partsNext[0]));
                        footer = footer.append(next);
                        footer = footer.append(ColorUtils.format(partsNext[1]));
                    } else {
                        footer = footer.append(ColorUtils.format(partsNext[0]));
                    }

                    player.sendMessage(footer);
                }

                player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_BREAK, 1.0F, 1.5F);
            });
        });
        return Command.SINGLE_SUCCESS;
    }
}
