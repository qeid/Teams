package discord.qeid.commands.teams;

import com.mojang.brigadier.tree.LiteralCommandNode;
import discord.qeid.Teams;
import discord.qeid.model.Team;
import discord.qeid.utils.MessagesUtil;
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
                    sender.sendMessage(MessagesUtil.get("general.players-only"));
                    return Command.SINGLE_SUCCESS;
                }
                var teamManager = Teams.getInstance().getTeamManager();
                Team team = teamManager.getTeamByPlayer(player.getUniqueId());
                if (team == null) {
                    player.sendMessage(MessagesUtil.get("team.info.not-in-team"));
                    return Command.SINGLE_SUCCESS;
                }
                Location home = teamManager.getHome(team.getId());
                if (home == null) {
                    player.sendMessage(MessagesUtil.get("team.home.not-set"));
                    return Command.SINGLE_SUCCESS;
                }
                player.teleport(home);
                player.sendMessage(MessagesUtil.get("team.home.success"));
                return Command.SINGLE_SUCCESS;
            }).build();
    }
}
