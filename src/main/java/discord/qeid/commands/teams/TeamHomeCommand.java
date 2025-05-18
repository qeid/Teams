package discord.qeid.commands.teams;

import com.mojang.brigadier.tree.LiteralCommandNode;
import discord.qeid.Teams;
import discord.qeid.model.Team;
import discord.qeid.utils.ConfigUtil;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import com.mojang.brigadier.Command;

public class TeamHomeCommand {
    public static LiteralCommandNode<CommandSourceStack> buildSubcommand() {
        return Commands.literal("home")
            .executes(ctx -> {
                CommandSender sender = ctx.getSource().getSender();
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(ConfigUtil.get("general.players-only"));
                    return Command.SINGLE_SUCCESS;
                }
                var teamManager = Teams.getInstance().getTeamManager();
                Team team = teamManager.getTeamByPlayer(player.getUniqueId());
                if (team == null) {
                    player.sendMessage(ConfigUtil.get("team.info.not-in-team"));
                    return Command.SINGLE_SUCCESS;
                }
                Location home = teamManager.getHome(team.getId());
                if (home == null) {
                    player.sendMessage(ConfigUtil.get("team.home.not-set"));
                    return Command.SINGLE_SUCCESS;
                }
                player.teleport(home);
                player.sendMessage(ConfigUtil.get("team.home.success"));
                return Command.SINGLE_SUCCESS;
            }).build();
    }
}
