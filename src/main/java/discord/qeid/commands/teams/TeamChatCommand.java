package discord.qeid.commands.teams;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import discord.qeid.Teams;
import discord.qeid.model.Team;
import discord.qeid.model.TeamRoles;
import discord.qeid.utils.MessagesUtil;
import discord.qeid.utils.SoundUtil;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

import static discord.qeid.utils.ColorUtils.coloredRank;
import static discord.qeid.utils.ColorUtils.formatLegacy;

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
                    return sendTeamChatAsync(player, message);
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
        player.sendMessage(formatLegacy(MessagesUtil.get("team.chat." + (toggled ? "toggled-on" : "toggled-off"))));
        player.playSound(player.getLocation(), SoundUtil.get("team.sounds.success"), 1.0F, 1.5F);
        return 1;
    }

    private static int sendTeamChatAsync(Player player, String message) {
        UUID uuid = player.getUniqueId();
        Bukkit.getScheduler().runTaskAsynchronously(Teams.getInstance(), () -> {
            var teamManager = Teams.getInstance().getTeamManager();
            Team team = teamManager.getTeamByPlayer(uuid);

            if (team == null) {
                Bukkit.getScheduler().runTask(Teams.getInstance(), () -> {
                    player.sendMessage(formatLegacy(MessagesUtil.get("team.chat.not-in-team")));
                    player.playSound(player.getLocation(), SoundUtil.get("team.sounds.error"), 1.0F, 1.5F);
                });
                return;
            }
            TeamRoles role = discord.qeid.database.TeamManager.getRole(team, uuid);
            String rank = coloredRank(role, true);

            String format = MessagesUtil.get("team.chat.format")
                .replace("%tag%", team.getTag())
                .replace("%player%", player.getName())
                .replace("%message%", message)
                .replace("%rank%", rank);

            Bukkit.getScheduler().runTask(Teams.getInstance(), () -> {
                for (UUID member : discord.qeid.listeners.TeamMessengerListener.getAllTeamMembers(team)) {
                    Player p = player.getServer().getPlayer(member);
                    if (p != null && p.isOnline()) {
                        p.sendMessage(formatLegacy(format));
                        p.playSound(p.getLocation(), SoundUtil.get("team.sounds.notification"), 1.0F, 1.0F);
                    }
                }
            });
        });
        return 1;
    }
}
