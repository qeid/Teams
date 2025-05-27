package discord.qeid.commands.teams;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import discord.qeid.Teams;
import discord.qeid.model.Team;
import discord.qeid.utils.MessagesUtil;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class TeamChatCommand {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    public static LiteralCommandNode<CommandSourceStack> buildSubcommand() {
        return Commands.literal("chat")
            .executes(ctx -> toggleChat(ctx.getSource().getSender()))
            .then(Commands.argument("message", StringArgumentType.greedyString())
                .executes(ctx -> {
                    CommandSender sender = ctx.getSource().getSender();
                    if (!(sender instanceof Player player)) {
                        sender.sendMessage("§cOnly players can use this command.");
                        return 1;
                    }
                    String message = StringArgumentType.getString(ctx, "message");
                    return sendTeamChat(player, message);
                })
            ).build();
    }

    private static int toggleChat(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can use this command.");
            return 1;
        }
        UUID uuid = player.getUniqueId();
        var dataManager = Teams.getInstance().getPlayerDataManager();
        boolean toggled = !dataManager.isTeamChatToggled(uuid);
        dataManager.setTeamChatToggled(uuid, toggled);
        player.sendMessage(LEGACY.deserialize(MessagesUtil.get("team.chat." + (toggled ? "toggled-on" : "toggled-off"))));
        return 1;
    }

    private static int sendTeamChat(Player player, String message) {
        UUID uuid = player.getUniqueId();
        var teamManager = Teams.getInstance().getTeamManager();
        Team team = teamManager.getTeamByPlayer(uuid);
        if (team == null) {
            player.sendMessage(LEGACY.deserialize(MessagesUtil.get("team.chat.not-in-team")));
            return 1;
        }
        String format = MessagesUtil.get("team.chat.format")
            .replace("%tag%", team.getTag())
            .replace("%player%", player.getName())
            .replace("%message%", message);
        for (UUID member : discord.qeid.listeners.TeamMessengerListener.getAllTeamMembers(team)) {
            Player p = player.getServer().getPlayer(member);
            if (p != null && p.isOnline()) {
                p.sendMessage(LEGACY.deserialize(format));
            }
        }
        return 1;
    }
}
