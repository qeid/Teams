package discord.qeid.commands.teams;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import discord.qeid.Teams;
import discord.qeid.database.TeamManager;
import discord.qeid.model.Team;
import discord.qeid.model.TeamRoles;
import discord.qeid.utils.ColorUtils;
import discord.qeid.utils.MessagesUtil;
import discord.qeid.listeners.TeamMessengerListener;
import discord.qeid.utils.SoundUtil;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class TeamUnbanCommand {

    public static LiteralCommandNode<CommandSourceStack> buildSubcommand() {
        return Commands.literal("unban")
            .then(Commands.argument("player", StringArgumentType.word())
                .suggests((ctx, builder) -> {
                    CommandSender sender = ctx.getSource().getSender();
                    if (!(sender instanceof Player player)) return builder.buildFuture();

                    UUID uuid = player.getUniqueId();
                    Team team = Teams.getInstance().getTeamManager().getTeamByPlayer(uuid);
                    if (team == null) return builder.buildFuture();

                    // Suggest banned players
                    Teams.getInstance().getTeamManager().getBannedPlayers(team.getId()).stream()
                        .map(Bukkit::getOfflinePlayer)
                        .map(OfflinePlayer::getName)
                        .filter(name -> name != null && name.toLowerCase().startsWith(builder.getRemainingLowerCase()))
                        .forEach(builder::suggest);

                    return builder.buildFuture();
                })
                .then(Commands.argument("reason", StringArgumentType.greedyString())
                    .executes(ctx -> execute(ctx.getSource().getSender(),
                        StringArgumentType.getString(ctx, "player"),
                        StringArgumentType.getString(ctx, "reason")))
                )
                .executes(ctx -> execute(ctx.getSource().getSender(),
                    StringArgumentType.getString(ctx, "player"),
                    "No reason specified"))
            ).build();
    }

    private static int execute(CommandSender sender, String targetName, String reason) {
        if (!(sender instanceof Player executor)) {
            sender.sendMessage(ColorUtils.format("&cOnly players can use this command."));
            return Command.SINGLE_SUCCESS;
        }

        UUID executorId = executor.getUniqueId();
        TeamManager teamManager = Teams.getInstance().getTeamManager();
        Team team = teamManager.getTeamByPlayer(executorId);

        if (team == null) {
            sender.sendMessage(MessagesUtil.get("team.unban.not-in-team"));
            ((Player)sender).playSound(((Player)sender).getLocation(), SoundUtil.get("team.sounds.error"), 1.0F, 1.5F);

            return Command.SINGLE_SUCCESS;
        }

        TeamRoles executorRole = TeamManager.getRole(team, executorId);
        if (executorRole != TeamRoles.ADMIN && executorRole != TeamRoles.OWNER) {
            sender.sendMessage(MessagesUtil.get("team.unban.no-permission"));
            ((Player)sender).playSound(((Player)sender).getLocation(), SoundUtil.get("team.sounds.error"), 1.0F, 1.5F);
            return Command.SINGLE_SUCCESS;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        if (target == null || target.getName() == null) {
            sender.sendMessage(MessagesUtil.get("team.unban.not-in-team"));
            ((Player)sender).playSound(((Player)sender).getLocation(), SoundUtil.get("team.sounds.error"), 1.0F, 1.5F);
            return Command.SINGLE_SUCCESS;
        }

        UUID targetId = target.getUniqueId();
        if (!teamManager.isBanned(team.getId(), targetId)) {
            sender.sendMessage(MessagesUtil.get("team.unban.not-banned"));
            ((Player)sender).playSound(((Player)sender).getLocation(), SoundUtil.get("team.sounds.error"), 1.0F, 1.5F);
            return Command.SINGLE_SUCCESS;
        }

        boolean success = teamManager.unbanPlayer(team.getId(), targetId);
        if (!success) {
            sender.sendMessage(MessagesUtil.get("team.unban.failed"));
            ((Player)sender).playSound(((Player)sender).getLocation(), SoundUtil.get("team.sounds.error"), 1.0F, 1.5F);
            return Command.SINGLE_SUCCESS;
        }

        // Audit log
        teamManager.logAudit(
            team.getId(),
            executorId,
            "UNBAN",
            executor.getName() + " unbanned " + target.getName() + ". Reason: " + reason
        );

        // Notify executor
        sender.sendMessage(MessagesUtil.get("team.unban.success")
            .replace("%target%", target.getName())
            .replace("%reason%", reason));

        // Notify team
        TeamMessengerListener.broadcastWithRank(team, executorId, MessagesUtil.get("team.notifications.player-unbanned")
            .replace("%target%", target.getName())
            .replace("%executor%", executor.getName())
            .replace("%reason%", reason));

        // Notify unbanned player if online
        Player onlineTarget = Bukkit.getPlayer(targetId);
        if (onlineTarget != null) {
            onlineTarget.sendMessage(MessagesUtil.get("team.unban.you-were-unbanned")
                .replace("%team%", team.getName())
                .replace("%reason%", reason));
        }
        ((Player)sender).playSound(((Player)sender).getLocation(), SoundUtil.get("team.sounds.success"), 1.0F, 1.5F);

        return Command.SINGLE_SUCCESS;
    }
}
