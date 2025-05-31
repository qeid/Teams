package discord.qeid.commands.teams;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.tree.LiteralCommandNode;
import discord.qeid.Teams;
import discord.qeid.listeners.TeamMessengerListener;
import discord.qeid.model.Team;
import discord.qeid.model.TeamRoles;
import discord.qeid.utils.MessagesUtil;
import discord.qeid.utils.SoundUtil;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class TeamDelHomeCommand {
    /**
     * This command allows team owners and admins to delete the team's home location.
     * It checks if the player has the required permissions and if a home is set.
     * If successful, it broadcasts a message to the team and plays a success sound.
     */

    public static LiteralCommandNode<CommandSourceStack> buildSubcommand() {
        return Commands.literal("delhome")
            .executes(ctx -> {
                CommandSender sender = ctx.getSource().getSender();
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(MessagesUtil.get("general.players-only"));
                    return Command.SINGLE_SUCCESS;
                }
                var teamManager = Teams.getInstance().getTeamManager();
                Team team = teamManager.getTeamByPlayer(player.getUniqueId());
                if (team == null) {
                    player.sendMessage(MessagesUtil.get("team.info.not-in-team"));
                    player.playSound(player.getLocation(), SoundUtil.get("team.sounds.error"), 1.0F, 1.5F);
                    return Command.SINGLE_SUCCESS;
                }
                TeamRoles role = teamManager.getRole(team, player.getUniqueId());
                if (role != TeamRoles.ADMIN && role != TeamRoles.OWNER) {
                    player.sendMessage(MessagesUtil.get("team.delhome.no-permission"));
                    player.playSound(player.getLocation(), SoundUtil.get("team.sounds.error"), 1.0F, 1.5F);
                    return Command.SINGLE_SUCCESS;
                }

                Location home = teamManager.getHome(team.getId());
                if (home == null) {
                    player.sendMessage(MessagesUtil.get("team.delhome.not-set"));
                    player.playSound(player.getLocation(), SoundUtil.get("team.sounds.error"), 1.0F, 1.5F);
                    return Command.SINGLE_SUCCESS;
                }
                boolean success = teamManager.delHome(team.getId());
                if (success) {
                    player.sendMessage(MessagesUtil.get("team.delhome.success"));
                    player.playSound(player.getLocation(), SoundUtil.get("team.sounds.success"), 1.0F, 1.5F);
                    String broadcastMsg = MessagesUtil.get("team.notifications.home-deleted")
                        .replace("%executor%", player.getName());
                    TeamMessengerListener.broadcastWithRank(team, player.getUniqueId(), broadcastMsg);
                } else {
                    player.sendMessage(MessagesUtil.get("team.delhome.failed"));
                    player.playSound(player.getLocation(), SoundUtil.get("team.sounds.error"), 1.0F, 1.5F);
                }

                teamManager.logAudit(
                    team.getId(),
                    player.getUniqueId(),
                    "Delete Home",
                    player.getName() + " deleted the team home."
                );

                return Command.SINGLE_SUCCESS;
            }).build();
    }
}
