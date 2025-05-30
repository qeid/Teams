package discord.qeid.commands.teams;

import com.mojang.brigadier.tree.LiteralCommandNode;
import discord.qeid.Teams;
import discord.qeid.database.TeamManager;
import discord.qeid.listeners.TeamMessengerListener;
import discord.qeid.model.Team;
import discord.qeid.model.TeamRoles;
import discord.qeid.utils.MessagesUtil;
import discord.qeid.utils.SoundUtil;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import com.mojang.brigadier.Command;

public class TeamSetHomeCommand {

    /**
     * This command allows team owners and admins to set the team's home location.
     * It checks if the player has the required permissions and sets the home if successful.
     * If successful, it broadcasts a message to the team and plays a success sound.
     */

    public static LiteralCommandNode<CommandSourceStack> buildSubcommand() {
        return Commands.literal("sethome")
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
                TeamRoles role = TeamManager.getRole(team, player.getUniqueId());
                if (role != TeamRoles.ADMIN && role != TeamRoles.OWNER) {
                    player.sendMessage(MessagesUtil.get("team.sethome.no-permission"));
                    player.playSound(player.getLocation(), SoundUtil.get("team.sounds.error"), 1.0F, 1.5F);
                    return Command.SINGLE_SUCCESS;
                }
                boolean success = teamManager.setHome(team.getId(), player.getLocation());
                if (success) {
                    var loc = player.getLocation();
                    String msg = MessagesUtil.get("team.sethome.success")
                        .replace("%x%", String.format("%.2f", loc.getX()))
                        .replace("%y%", String.format("%.2f", loc.getY()))
                        .replace("%z%", String.format("%.2f", loc.getZ()));
                    player.sendMessage(msg);

                    String broadcastMsg = MessagesUtil.get("team.notifications.home-set")
                        .replace("%executor%", player.getName())
                        .replace("%x%", String.format("%.2f", loc.getX()))
                        .replace("%y%", String.format("%.2f", loc.getY()))
                        .replace("%z%", String.format("%.2f", loc.getZ()));
                    TeamMessengerListener.broadcastWithRank(team, player.getUniqueId(), broadcastMsg);
                    player.playSound(player.getLocation(), SoundUtil.get("team.sounds.success"), 1.0F, 1.5F);
                } else {
                    player.sendMessage(MessagesUtil.get("team.sethome.failed"));
                    player.playSound(player.getLocation(), SoundUtil.get("team.sounds.error"), 1.0F, 1.5F);
                }

                var loc = player.getLocation();
                teamManager.logAudit(
                    team.getId(),
                    player.getUniqueId(),
                    "Set Home",
                    player.getName() + " set the team home to X: " +
                        String.format("%.2f", loc.getX()) + " Y: " +
                        String.format("%.2f", loc.getY()) + " Z: " +
                        String.format("%.2f", loc.getZ()) + " (" + loc.getWorld().getName() + ")"
                );

                return Command.SINGLE_SUCCESS;
            }).build();
    }
}

