package discord.qeid.commands.teams;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import discord.qeid.Teams;
import discord.qeid.model.Team;
import discord.qeid.utils.MessagesUtil;
import discord.qeid.listeners.TeamMessengerListener;
import discord.qeid.utils.SoundUtil;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

public class TeamInviteCommand {

    /**
     * This command allows team members to invite other players to their team.
     * It checks permissions based on roles and handles the invite process.
     * The command can be used by team owners, admins, and mods to manage team membership.
     */

    public static LiteralCommandNode<CommandSourceStack> buildSubcommand() {
        return Commands.literal("invite")
            .then(Commands.argument("player", StringArgumentType.word())
                .suggests((ctx, builder) -> {
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        builder.suggest(p.getName());
                    }
                    return builder.buildFuture();
                })
                .executes(ctx -> {
                    CommandSender sender = ctx.getSource().getSender();

                    if (!(sender instanceof Player inviter)) {
                        sender.sendMessage(MessagesUtil.get("general.players-only"));
                        return Command.SINGLE_SUCCESS;
                    }

                    String targetName = StringArgumentType.getString(ctx, "player");
                    Player target = Bukkit.getPlayerExact(targetName);

                    if (target == null || !target.isOnline()) {
                        inviter.sendMessage(MessagesUtil.get("team.invite.player_not_found"));
                        return Command.SINGLE_SUCCESS;
                    }

                    UUID targetUUID = target.getUniqueId();
                    UUID inviterUUID = inviter.getUniqueId();

                    var teamManager = Teams.getInstance().getTeamManager();
                    var playerData = Teams.getInstance().getPlayerDataManager();

                    Team team = teamManager.getTeamByPlayer(inviterUUID);
                    if (team == null) {
                        inviter.sendMessage(MessagesUtil.get("team.invite.no_team"));
                        inviter.playSound(inviter.getLocation(), SoundUtil.get("team.sounds.error"), 1.0F, 1.5F);
                        return Command.SINGLE_SUCCESS;
                    }

                    if (teamManager.isBanned(team.getId(), targetUUID)) {
                        inviter.sendMessage(MessagesUtil.get("team.invite.banned"));
                        inviter.playSound(inviter.getLocation(), SoundUtil.get("team.sounds.error"), 1.0F, 1.5F);
                        return Command.SINGLE_SUCCESS;
                    }

                    if (!inviterUUID.equals(team.getOwner())
                        && !team.getAdmins().contains(inviterUUID)
                        && !team.getMods().contains(inviterUUID)) {
                        inviter.sendMessage(MessagesUtil.get("team.invite.no_permission"));
                        inviter.playSound(inviter.getLocation(), SoundUtil.get("team.sounds.error"), 1.0F, 1.5F);
                        return Command.SINGLE_SUCCESS;
                    }

                    Team targetTeam = teamManager.getTeamByPlayer(targetUUID);
                    if (targetTeam != null) {
                        inviter.sendMessage(MessagesUtil.get("team.invite.already_in_team"));
                        inviter.playSound(inviter.getLocation(), SoundUtil.get("team.sounds.error"), 1.0F, 1.5F);
                        return Command.SINGLE_SUCCESS;
                    }

                    if (playerData.hasInvite(targetUUID, team.getId())) {
                        inviter.sendMessage(MessagesUtil.get("team.invite.already_invited"));
                        inviter.playSound(inviter.getLocation(), SoundUtil.get("team.sounds.error"), 1.0F, 1.5F);
                        return Command.SINGLE_SUCCESS;
                    }

                    playerData.addInvite(targetUUID, team.getId());

                    Bukkit.getScheduler().runTaskLater(Teams.getInstance(), () -> {
                        if (teamManager.getTeamByPlayer(targetUUID) != null) return;
                        if (!playerData.hasInvite(targetUUID, team.getId())) return;

                        playerData.removeInvite(targetUUID, team.getId());
                        target.sendMessage(MessagesUtil.get("team.invite.expired-invitee").replace("%team%", team.getName()));
                        TeamMessengerListener.broadcast(team,
                            MessagesUtil.get("team.invite.expired-team").replace("%target%", target.getName()));
                    }, 20L * 60 * 5); // 5 minutes

                    inviter.sendMessage(MessagesUtil.get("team.invite.sent").replace("%target%", target.getName()));
                    MiniMessage mm = MiniMessage.miniMessage();
                    LegacyComponentSerializer legacy = LegacyComponentSerializer.legacyAmpersand();

                    String teamName = team.getName();
                    String tag = team.getTag();
                    String tagUpper = tag.toUpperCase();
                    String inviterName = inviter.getName();

                    List<String> lines = MessagesUtil.getMessages().getStringList("team.invite.invitee");
                    for (String line : lines) {
                        if (line.contains("<click:") || line.contains("<hover:")) {
                            Component comp = mm.deserialize(line.replace("%team%", teamName).replace("%inviter%", inviterName).replace("%tag%", tag).replace("%tag_upper%", tagUpper));
                            target.sendMessage(comp);
                        } else {
                            Component comp = legacy.deserialize(line.replace("%team%", teamName).replace("%inviter%", inviterName).replace("%tag%", tag).replace("%tag_upper%", tagUpper));
                            target.sendMessage(comp);
                        }
                    }

                    TeamMessengerListener.broadcastWithRank(team, inviterUUID, MessagesUtil.get("team.notifications.invite-sent")
                        .replace("%sender%", inviter.getName())
                        .replace("%target%", target.getName()));

                    teamManager.logAudit(
                        team.getId(),
                        inviterUUID, // the player who sent the invite
                        "Invite",
                        inviter.getName() + " invited " + target.getName() + " to the team."
                    );
                    inviter.playSound(inviter.getLocation(), SoundUtil.get("team.sounds.success"), 1.0F, 1.5F);

                    return Command.SINGLE_SUCCESS;
                })
            )
            .build();
    }
}
