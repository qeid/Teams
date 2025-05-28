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
import discord.qeid.utils.DurationUtil;
import discord.qeid.listeners.TeamMessengerListener;
import discord.qeid.utils.SoundUtil;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Set;
import java.util.UUID;

import static discord.qeid.utils.DurationUtil.formatFullDuration;

public class TeamBanCommand {

    private final Teams plugin;

    public TeamBanCommand(Teams plugin) {
        this.plugin = plugin;
    }

    public static LiteralCommandNode<CommandSourceStack> buildSubcommand() {
        return Commands.literal("ban")
            .then(Commands.argument("player", StringArgumentType.word())
                .suggests((ctx, builder) -> {
                    CommandSender sender = ctx.getSource().getSender();
                    if (!(sender instanceof Player player)) return builder.buildFuture();

                    UUID uuid = player.getUniqueId();
                    Team team = Teams.getInstance().getTeamManager().getTeamByPlayer(uuid);
                    if (team == null) return builder.buildFuture();

                    Set<UUID> allMembers = TeamMessengerListener.getAllTeamMembers(team);
                    allMembers.stream()
                        .filter(id -> !id.equals(uuid))
                        .map(Bukkit::getOfflinePlayer)
                        .map(OfflinePlayer::getName)
                        .filter(name -> name != null && name.toLowerCase().startsWith(builder.getRemainingLowerCase()))
                        .forEach(builder::suggest);

                    return builder.buildFuture();
                })
                .then(Commands.argument("reason / duration", StringArgumentType.greedyString())
                    .executes(ctx -> {
                        String playerName = StringArgumentType.getString(ctx, "player");
                        String input = StringArgumentType.getString(ctx, "reason / duration");
                        return execute(ctx.getSource().getSender(), playerName, input);
                    }))
                .executes(ctx -> {
                    String playerName = StringArgumentType.getString(ctx, "player");
                    return execute(ctx.getSource().getSender(), playerName, "");
                })
            )
            .build();
    }

    private static int execute(CommandSender sender, String targetName, String input) {
        if (!(sender instanceof Player executor)) {
            sender.sendMessage(ColorUtils.format("&cOnly players can use this command."));
            return Command.SINGLE_SUCCESS;
        }

        UUID executorId = executor.getUniqueId();
        TeamManager teamManager = Teams.getInstance().getTeamManager();
        Team team = teamManager.getTeamByPlayer(executorId);

        if (team == null) {
            sender.sendMessage(MessagesUtil.get("team.ban.not-in-team"));
            ((Player)sender).playSound(((Player)sender).getLocation(), SoundUtil.get("team.sounds.error"), 1.0F, 1.5F);
            return Command.SINGLE_SUCCESS;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        if (target == null || target.getName() == null) {
            sender.sendMessage(MessagesUtil.get("team.ban.not-in-team"));
            ((Player)sender).playSound(((Player)sender).getLocation(), SoundUtil.get("team.sounds.error"), 1.0F, 1.5F);

            return Command.SINGLE_SUCCESS;
        }

        UUID targetId = target.getUniqueId();
        if (!TeamMessengerListener.getAllTeamMembers(team).contains(targetId)) {
            sender.sendMessage(MessagesUtil.get("team.ban.not-in-team"));
            ((Player)sender).playSound(((Player)sender).getLocation(), SoundUtil.get("team.sounds.error"), 1.0F, 1.5F);
            return Command.SINGLE_SUCCESS;
        }

        if (targetId.equals(executorId)) {
            sender.sendMessage(MessagesUtil.get("team.ban.self"));
            ((Player)sender).playSound(((Player)sender).getLocation(), SoundUtil.get("team.sounds.error"), 1.0F, 1.5F);
            return Command.SINGLE_SUCCESS;
        }

        TeamRoles executorRole = getRole(team, executorId);
        TeamRoles targetRole = getRole(team, targetId);

        if (!canBan(executorRole, targetRole)) {
            sender.sendMessage(MessagesUtil.get("team.ban.no-permission"));
            ((Player)sender).playSound(((Player)sender).getLocation(), SoundUtil.get("team.sounds.error"), 1.0F, 1.5F);
            return Command.SINGLE_SUCCESS;
        }

        long durationSeconds = DurationUtil.parseDurationSeconds(input);
        String reason = DurationUtil.stripDuration(input);
        if (reason.isEmpty()) reason = "No reason specified";

        if (teamManager.isBanned(team.getId(), targetId)) {
            sender.sendMessage(MessagesUtil.get("team.ban.already-banned"));
            ((Player)sender).playSound(((Player)sender).getLocation(), SoundUtil.get("team.sounds.error"), 1.0F, 1.5F);
            return Command.SINGLE_SUCCESS;
        }

        boolean success = teamManager.banPlayer(
            team.getId(), targetId, executorId, reason, durationSeconds
        );

        if (!success) {
            sender.sendMessage(MessagesUtil.get("team.ban.failed"));
            ((Player)sender).playSound(((Player)sender).getLocation(), SoundUtil.get("team.sounds.error"), 1.0F, 1.5F);
            return Command.SINGLE_SUCCESS;
        }

        teamManager.kickMember(team.getId(), targetId);
        team = teamManager.getTeamById(team.getId());

        String auditInfo = "Banned " + target.getName() +
            " for " + (durationSeconds > 0 ? DurationUtil.formatFullDuration(durationSeconds) : "Permanent") +
            ". Reason: " + reason;
        teamManager.logAudit(
            team.getId(),
            executorId,
            "Ban",
            auditInfo
        );

        sender.sendMessage(MessagesUtil.get("team.ban.success")
            .replace("%target%", target.getName())
            .replace("%reason%", reason)
            .replace("%duration%", formatFullDuration(durationSeconds)));

        TeamMessengerListener.broadcastWithRank(team, executorId, MessagesUtil.get("team.notifications.player-banned")
            .replace("%target%", target.getName())
            .replace("%reason%", reason)
            .replace("%executor%", executor.getName())
            .replace("%duration%", formatFullDuration(durationSeconds)));

        Player onlineTarget = Bukkit.getPlayer(targetId);
        if (onlineTarget != null) {
            onlineTarget.sendMessage(MessagesUtil.get("team.ban.banned")
                .replace("%team%", team.getName())
                .replace("%executor%", executor.getName())
                .replace("%reason%", reason)
                .replace("%duration%", formatFullDuration(durationSeconds)));
        }
        ((Player)sender).playSound(((Player)sender).getLocation(), SoundUtil.get("team.sounds.success"), 1.0F, 1.5F);

        return Command.SINGLE_SUCCESS;
    }

    private static boolean canBan(TeamRoles executor, TeamRoles target) {
        if (executor == TeamRoles.OWNER) return true;
        if (executor == TeamRoles.ADMIN && target == TeamRoles.MEMBER) return true;
        return false;
    }

    private static TeamRoles getRole(Team team, UUID playerId) {
        if (team.getOwner().equals(playerId)) return TeamRoles.OWNER;
        if (team.getAdmins().contains(playerId)) return TeamRoles.ADMIN;
        if (team.getMods().contains(playerId)) return TeamRoles.MOD;
        if (team.getMembers().contains(playerId)) return TeamRoles.MEMBER;
        return TeamRoles.NONE;
    }
}
