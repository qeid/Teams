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

public class TeamDemoteCommand {
    /**
     * This command allows team members to demote other players within their team.
     * It checks permissions based on roles and handles the demotion process.
     * The command can be used by team owners and admins to manage team membership.
     */

    public static LiteralCommandNode<CommandSourceStack> buildSubcommand() {
        return Commands.literal("demote")
            .then(Commands.argument("player", StringArgumentType.word())
                .suggests((ctx, builder) -> {
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
                        sender.sendMessage(MessagesUtil.get("team.demote.not-found"));
                        ((Player)sender).playSound(((Player)sender).getLocation(), SoundUtil.get("team.sounds.error"), 1.0F, 1.5F);
                        return Command.SINGLE_SUCCESS;
                    }

                    UUID execId = executor.getUniqueId();
                    UUID targetId = target.getUniqueId();

                    // Run all DB logic async
                    Bukkit.getScheduler().runTaskAsynchronously(Teams.getInstance(), () -> {
                        TeamManager manager = Teams.getInstance().getTeamManager();
                        Team team = manager.getTeamByPlayer(execId);
                        if (team == null) {
                            Bukkit.getScheduler().runTask(Teams.getInstance(), () -> {
                                sender.sendMessage(MessagesUtil.get("team.demote.not-in-team"));
                                ((Player)sender).playSound(((Player)sender).getLocation(), SoundUtil.get("team.sounds.error"), 1.0F, 1.5F);
                            });
                            return;
                        }

                        if (!TeamMessengerListener.getAllTeamMembers(team).contains(targetId)) {
                            Bukkit.getScheduler().runTask(Teams.getInstance(), () -> {
                                sender.sendMessage(MessagesUtil.get("team.demote.not-in-team"));
                                ((Player)sender).playSound(((Player)sender).getLocation(), SoundUtil.get("team.sounds.error"), 1.0F, 1.5F);
                            });
                            return;
                        }

                        TeamRoles execRole = TeamManager.getRole(team, execId);
                        TeamRoles oldRole = TeamManager.getRole(team, targetId);

                        String newRole = switch (oldRole) {
                            case ADMIN -> TeamRoles.MOD.name();
                            case MOD -> TeamRoles.MEMBER.name();
                            default -> null;
                        };

                        if (newRole == null) {
                            Bukkit.getScheduler().runTask(Teams.getInstance(), () -> {
                                sender.sendMessage(MessagesUtil.get("team.demote.invalid-target"));
                                ((Player)sender).playSound(((Player)sender).getLocation(), SoundUtil.get("team.sounds.error"), 1.0F, 1.5F);
                            });
                            return;
                        }

                        if (!canDemote(execRole, oldRole)) {
                            Bukkit.getScheduler().runTask(Teams.getInstance(), () -> {
                                sender.sendMessage(MessagesUtil.get("team.demote.no-permission"));
                                ((Player)sender).playSound(((Player)sender).getLocation(), SoundUtil.get("team.sounds.error"), 1.0F, 1.5F);
                            });
                            return;
                        }

                        boolean demoted = manager.demoteToRole(team.getId(), targetId, newRole);
                        if (!demoted) {
                            Bukkit.getScheduler().runTask(Teams.getInstance(), () -> {
                                sender.sendMessage(MessagesUtil.get("team.demote.failed"));
                                ((Player)sender).playSound(((Player)sender).getLocation(), SoundUtil.get("team.sounds.error"), 1.0F, 1.5F);
                            });
                            return;
                        }

                        // Reload team after demote
                        Team updatedTeam = manager.getTeamById(team.getId());
                        String oldRoleColor = coloredRank(oldRole, true);
                        String newRoleColor = coloredRank(TeamRoles.valueOf(newRole), true);

                        Bukkit.getScheduler().runTask(Teams.getInstance(), () -> {
                            sender.sendMessage(MessagesUtil.get("team.demote.success")
                                .replace("%target%", target.getName())
                                .replace("%oldrole%", formatLegacy(oldRoleColor))
                                .replace("%newrole%", formatLegacy(newRoleColor)));
                            ((Player)sender).playSound(((Player)sender).getLocation(), SoundUtil.get("team.sounds.success"), 1.0F, 1.5F);

                            TeamMessengerListener.broadcastWithTwo(updatedTeam, execId, targetId, MessagesUtil.get("team.notifications.demoted")
                                .replace("%target%", target.getName())
                                .replace("%executor%", executor.getName())
                                .replace("%oldrole%", oldRoleColor)
                                .replace("%newrole%", newRoleColor));

                            manager.logAudit(
                                team.getId(),
                                executor.getUniqueId(),
                                "Demote",
                                executor.getName() + " demoted " + target.getName() + " from " + oldRole.name() + " to " + newRole + "."
                            );
                        });
                    });

                    return Command.SINGLE_SUCCESS;
                })).build();
    }

    private static boolean canDemote(TeamRoles executor, TeamRoles target) {
        if (executor == TeamRoles.OWNER) return true;
        if (executor == TeamRoles.ADMIN && target == TeamRoles.MOD) return true;
        return false;
    }
}
