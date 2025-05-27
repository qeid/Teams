package discord.qeid.commands.teams;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import discord.qeid.Teams;
import discord.qeid.utils.MessagesUtil;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class TeamHelpCommand {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    public static LiteralCommandNode<CommandSourceStack> buildSubcommand() {

        return Commands.literal("help")
            .executes(ctx -> runHelp(ctx, 1))
            .then(Commands.argument("page", IntegerArgumentType.integer(0, 3))
                .executes(ctx -> {
                    int page = IntegerArgumentType.getInteger(ctx, "page");
                    return runHelp(ctx, page);
                })
            ).build();
    }

    public static int runHelpDefault(CommandContext<CommandSourceStack> ctx) {
        return runHelp(ctx, 1);
    }

    private static int runHelp(CommandContext<CommandSourceStack> ctx, int page) {
        CommandSender sender = ctx.getSource().getSender();
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can use this command.");
            return 1;
        }

        // Play sound
        String soundName = MessagesUtil.getMessages().getString("team.help.sound", "entity.experience_orb.pickup");
        try {
            player.playSound(player.getLocation(), Sound.valueOf(soundName.toUpperCase().replace('.', '_')), 1f, 2f + (page * 0.5f));
        } catch (Exception ignored) {}

        List<String> lines = MessagesUtil.getMessages().getStringList("team.help.pages." + page);
        if (lines.isEmpty()) {
            player.sendMessage("§cNo help message found for page " + page + ".");
            return 1;
        }

        // Build navigation buttons
        String navNext = MessagesUtil.getMessages().getString("team.help.footer.nav.next", "&b[» Next Page]");
        String navPrev = MessagesUtil.getMessages().getString("team.help.footer.nav.prev", "&b[« Prev Page]");
        String navAdmin = MessagesUtil.getMessages().getString("team.help.footer.nav.admin", "&6[★ Admin Commands]");
        String navUser = MessagesUtil.getMessages().getString("team.help.footer.nav.user", "&7[☆ User Commands]");
        String navNextTooltip = MessagesUtil.getMessages().getString("team.help.footer.nav.next-tooltip", "&7Go to next page");
        String navPrevTooltip = MessagesUtil.getMessages().getString("team.help.footer.nav.prev-tooltip", "&7Go to previous page");
        String navAdminTooltip = MessagesUtil.getMessages().getString("team.help.footer.nav.admin-tooltip", "&6View admin commands");
        String navUserTooltip = MessagesUtil.getMessages().getString("team.help.footer.nav.user-tooltip", "&7View user commands");

        boolean isAdmin = player.hasPermission(Teams.getInstance().getConfig().getString("admin.permission", "teams.admin"));

        final Component next = (page < 3)
            ? LEGACY.deserialize(navNext)
                .clickEvent(ClickEvent.runCommand("/team help " + (page + 1)))
                .hoverEvent(HoverEvent.showText(LEGACY.deserialize(navNextTooltip)))
            : Component.empty();

        final Component prev = (page > 1)
            ? LEGACY.deserialize(navPrev)
                .clickEvent(ClickEvent.runCommand("/team help " + (page - 1)))
                .hoverEvent(HoverEvent.showText(LEGACY.deserialize(navPrevTooltip)))
            : Component.empty();

        final Component admin = (page != 0 && isAdmin)
            ? LEGACY.deserialize(navAdmin)
                .clickEvent(ClickEvent.runCommand("/team help 0"))
                .hoverEvent(HoverEvent.showText(LEGACY.deserialize(navAdminTooltip)))
            : Component.empty();

        final Component user = (page == 0)
            ? LEGACY.deserialize(navUser)
                .clickEvent(ClickEvent.runCommand("/team help 1"))
                .hoverEvent(HoverEvent.showText(LEGACY.deserialize(navUserTooltip)))
            : Component.empty();

        // Build footer
        String footerTemplate = MessagesUtil.getMessages().getString("team.help.footer." + page, "");
        final Component footerBase = footerTemplate.isEmpty() ? Component.empty() : LEGACY.deserialize(footerTemplate);

        final Component footer = footerBase
            .replaceText(builder -> builder.matchLiteral("%next%").replacement(next))
            .replaceText(builder -> builder.matchLiteral("%prev%").replacement(prev))
            .replaceText(builder -> builder.matchLiteral("%admin%").replacement(admin))
            .replaceText(builder -> builder.matchLiteral("%user%").replacement(user));

        // Send lines
        for (String line : lines) {
            if (line.equals("%footer%")) {
                player.sendMessage(footer);
            } else {
                player.sendMessage(LEGACY.deserialize(line));
            }
        }

        return 1;
    }
}
