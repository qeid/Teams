package discord.qeid.commands.teams;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import discord.qeid.Teams;
import discord.qeid.database.TeamManager;
import discord.qeid.model.Team;
import discord.qeid.listeners.TeamMessengerListener;
import discord.qeid.utils.MessagesUtil;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.*;

public class TeamTransferCommand {

    private static final Map<UUID, UUID> pendingTransfers = new HashMap<>();
    private static final Map<UUID, Long> pendingTimes = new HashMap<>();

    public static LiteralCommandNode<CommandSourceStack> buildSubcommand() {
        return Commands.literal("transfer")
            .then(Commands.argument("member", StringArgumentType.word())
                .suggests((ctx, builder) -> {
                    CommandSender sender = ctx.getSource().getSender();
                    //System.out.println("[DEBUG][TRANSFER] Suggestion fired for input: " + builder.getInput());

                    if (!(sender instanceof Player player)) {
                        //System.out.println("[DEBUG][TRANSFER] Not a player.");
                        return builder.buildFuture();
                    }

                    UUID uuid = player.getUniqueId();
                    Team team = Teams.getInstance().getTeamManager().getTeamByPlayer(uuid);
                    if (team == null) {
                        //System.out.println("[DEBUG][TRANSFER] No team found for player: " + player.getName());
                        return builder.buildFuture();
                    }

                    //System.out.println("[DEBUG][TRANSFER] Suggesting names...");
                    Set<UUID> allMembers = TeamMessengerListener.getAllTeamMembers(team);
                    allMembers.stream()
                        .filter(id -> !id.equals(uuid))
                        .map(Bukkit::getOfflinePlayer)
                        .map(OfflinePlayer::getName)
                        .filter(name -> name != null && name.toLowerCase().startsWith(builder.getRemainingLowerCase()))
                        //.peek(name -> System.out.println("[DEBUG][TRANSFER] Suggesting: " + name))
                        .forEach(builder::suggest);

                    return builder.buildFuture();
                })
                .executes(ctx -> {
                    CommandSender sender = ctx.getSource().getSender();
                    if (!(sender instanceof Player executor)) {
                        sender.sendMessage(MessagesUtil.get("general.players-only"));
                        return Command.SINGLE_SUCCESS;
                    }

                    String targetName = StringArgumentType.getString(ctx, "member");
                    OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
                    UUID executorId = executor.getUniqueId();
                    UUID targetId = target.getUniqueId();

                    TeamManager teamManager = Teams.getInstance().getTeamManager();
                    Team team = teamManager.getTeamByPlayer(executorId);

                    if (team == null) {
                        executor.sendMessage(MessagesUtil.get("team.transfer.not-in-team"));
                        return Command.SINGLE_SUCCESS;
                    }



                    if (!team.getOwner().equals(executorId)) {
                        executor.sendMessage(MessagesUtil.get("team.transfer.not-owner"));
                        return Command.SINGLE_SUCCESS;
                    }

                    if (!team.getAdmins().contains(targetId)
                        && !team.getMods().contains(targetId)
                        && !team.getMembers().contains(targetId)) {
                        executor.sendMessage(MessagesUtil.get("team.transfer.not-member"));
                        return Command.SINGLE_SUCCESS;
                    }
                    if(executorId.equals(targetId)) { executor.sendMessage(MessagesUtil.get("team.transfer.self")); }

                    // Handle confirmation
                    if (pendingTransfers.containsKey(executorId)
                        && pendingTransfers.get(executorId).equals(targetId)) {

                        long started = pendingTimes.getOrDefault(executorId, 0L);
                        if (System.currentTimeMillis() - started <= 10_000) {
                            pendingTransfers.remove(executorId);
                            pendingTimes.remove(executorId);


                            // update team ownership in DB
                            teamManager.setOwner(team.getId(), targetId);

                            // remove new owner from old role if exists
                            teamManager.removeMember(team.getId(), targetId);

                            // promote new owner to OWNER
                            teamManager.promoteToRole(team.getId(), targetId, "OWNER");

                            // remove old owner from owner role
                            teamManager.removeMember(team.getId(), executorId);

                            // promote old owner to ADMIN
                            teamManager.promoteToRole(team.getId(), executorId, "ADMIN");



                            if (targetId.equals(executorId)) {
                                executor.sendMessage(MessagesUtil.get("team.transfer.failed"));
                                return Command.SINGLE_SUCCESS;
                            }

                            executor.sendMessage(MessagesUtil.get("team.transfer.success")
                                .replace("%target%", target.getName()));

                            Team updatedTeam = teamManager.getTeamById(team.getId());
                            TeamMessengerListener.broadcastWithTwo(updatedTeam, executorId, targetId,
                                MessagesUtil.get("team.notifications.transferred")
                                    .replace("%old-owner%", executor.getName())
                                    .replace("%new-owner%", target.getName()));
                            return Command.SINGLE_SUCCESS;

                        } else {
                            pendingTransfers.remove(executorId);
                            pendingTimes.remove(executorId);
                            executor.sendMessage(MessagesUtil.get("team.transfer.timeout"));
                            return Command.SINGLE_SUCCESS;
                        }
                    }

                    // confirm stuff
                    pendingTransfers.put(executorId, targetId);
                    pendingTimes.put(executorId, System.currentTimeMillis());

                    executor.sendMessage(MessagesUtil.get("team.transfer.confirm")
                        .replace("%target%", target.getName()));

                    Bukkit.getScheduler().runTaskLater(Teams.getInstance(), () -> {
                        if (pendingTransfers.containsKey(executorId)) {
                            pendingTransfers.remove(executorId);
                            pendingTimes.remove(executorId);

                            if (teamManager.getTeamByPlayer(executorId) != null) {
                                executor.sendMessage(MessagesUtil.get("team.transfer.timeout"));
                            }
                        }
                    }, 200L); // 10 seconds (should be configurable laters)
                    return Command.SINGLE_SUCCESS;
                })).build();
    }
}
