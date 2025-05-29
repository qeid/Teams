package discord.qeid.commands.teams;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import discord.qeid.Teams;
import discord.qeid.database.TeamManager;
import discord.qeid.model.Team;
import discord.qeid.model.TeamRoles;
import discord.qeid.utils.MessagesUtil;
import discord.qeid.listeners.TeamMessengerListener;
import discord.qeid.utils.SoundUtil;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Objects;
import java.util.UUID;

import static discord.qeid.utils.ColorUtils.coloredRank;
import static discord.qeid.utils.ColorUtils.formatLegacy;


public class TeamPromoteCommand {

    public static LiteralCommandNode<CommandSourceStack> buildSubcommand() {
        return Commands.literal("promote")
            .then(Commands.argument("player", StringArgumentType.word())
                .suggests((ctx, builder) -> {
                    //System.out.println("[DEBUG][PROMOTE] Suggestions loaded:");
                    CommandSender sender = ctx.getSource().getSender();
                    if (!(sender instanceof Player p)) return builder.buildFuture();

                    UUID uuid = p.getUniqueId();
                    Team team = Teams.getInstance().getTeamManager().getTeamByPlayer(uuid);
                    if (team == null) return builder.buildFuture();

                    TeamMessengerListener.getAllTeamMembers(team).stream()
                        .filter(id -> !id.equals(uuid))
                        .map(Bukkit::getOfflinePlayer)
                        .map(OfflinePlayer::getName)
                        .filter(Objects::nonNull)
                        .filter(name -> name.toLowerCase().startsWith(builder.getRemainingLowerCase()))
                        .forEach(builder::suggest);

                    return builder.buildFuture();
                })
                .executes(ctx -> {
                    CommandSender sender = ctx.getSource().getSender();
                    if (!(sender instanceof Player executor)) return Command.SINGLE_SUCCESS;

                    String name = StringArgumentType.getString(ctx, "player");
                    OfflinePlayer target = Bukkit.getOfflinePlayer(name);
                    if (target == null || target.getName() == null) {
                        sender.sendMessage(MessagesUtil.get("team.promote.not-found"));
                        ((Player)sender).playSound(((Player)sender).getLocation(), SoundUtil.get("team.sounds.error"), 1.0F, 1.5F);
                        return Command.SINGLE_SUCCESS;
                    }

                    UUID execId = executor.getUniqueId();
                    UUID targetId = target.getUniqueId();
                    TeamManager manager = Teams.getInstance().getTeamManager();
                    Team team = manager.getTeamByPlayer(execId);
                    if (team == null) {
                        sender.sendMessage(MessagesUtil.get("team.promote.not-in-team"));
                        ((Player)sender).playSound(((Player)sender).getLocation(), SoundUtil.get("team.sounds.error"), 1.0F, 1.5F);
                        return Command.SINGLE_SUCCESS;
                    }

                    if (!TeamMessengerListener.getAllTeamMembers(team).contains(targetId)) {
                        sender.sendMessage(MessagesUtil.get("team.promote.not-in-team"));
                        ((Player)sender).playSound(((Player)sender).getLocation(), SoundUtil.get("team.sounds.error"), 1.0F, 1.5F);
                        return Command.SINGLE_SUCCESS;
                    }

                    TeamRoles execRole = manager.getRole(team, execId);
                    TeamRoles oldRole = manager.getRole(team, targetId);

                    // chain
                    String newRole = switch (oldRole) {
                        case MEMBER -> TeamRoles.MOD.name();
                        case MOD -> TeamRoles.ADMIN.name();
                        default -> null;
                    };

                    if (newRole == null) {
                        sender.sendMessage(MessagesUtil.get("team.promote.invalid-target"));
                        ((Player)sender).playSound(((Player)sender).getLocation(), SoundUtil.get("team.sounds.error"), 1.0F, 1.5F);
                        return Command.SINGLE_SUCCESS;
                    }

                    // permission checks
                    if (!canPromote(execRole, oldRole)) {
                        sender.sendMessage(MessagesUtil.get("team.promote.no-permission"));
                        ((Player)sender).playSound(((Player)sender).getLocation(), SoundUtil.get("team.sounds.error"), 1.0F, 1.5F);
                        return Command.SINGLE_SUCCESS;
                    }

                    if (!manager.promoteToRole(team.getId(), targetId, newRole)) {
                        sender.sendMessage(MessagesUtil.get("team.promote.failed"));
                        ((Player)sender).playSound(((Player)sender).getLocation(), SoundUtil.get("team.sounds.error"), 1.0F, 1.5F);
                        return Command.SINGLE_SUCCESS;
                    }

                    team = manager.getTeamById(team.getId());


                    String oldRoleColor = coloredRank(oldRole, true);
                    String newRoleColor = coloredRank(TeamRoles.valueOf(newRole), true);

                    sender.sendMessage(MessagesUtil.get("team.promote.success")
                        .replace("%target%", target.getName())
                        .replace("%oldrole%", formatLegacy(oldRoleColor))
                        .replace("%newrole%", formatLegacy(newRoleColor)));
                    ((Player)sender).playSound(((Player)sender).getLocation(), SoundUtil.get("team.sounds.success"), 1.0F, 1.5F);

                    Team updatedTeam = Teams.getInstance().getTeamManager().getTeamById(team.getId());

                    TeamMessengerListener.broadcastWithTwo(updatedTeam, execId, targetId, MessagesUtil.get("team.notifications.promoted")
                        .replace("%target%", target.getName())
                        .replace("%executor%", executor.getName())
                        .replace("%oldrole%", oldRoleColor)
                        .replace("%newrole%", newRoleColor));

                    manager.logAudit(
                        team.getId(),
                        executor.getUniqueId(),
                        "Promote",
                        executor.getName() + " promoted " + target.getName() + " from " + oldRole.name() + " to " + newRole + "."
                    );




                    return Command.SINGLE_SUCCESS;


                })).build();
    }

    private static boolean canPromote(TeamRoles executor, TeamRoles target) {
        if (executor == TeamRoles.OWNER) return true;
        if (executor == TeamRoles.ADMIN && target == TeamRoles.MOD) return true;
        return false;
    }
}
