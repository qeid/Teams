package discord.qeid.commands.teams;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import discord.qeid.Teams;
import discord.qeid.model.Team;
import discord.qeid.utils.ConfigUtil;
import discord.qeid.utils.DebugUtil;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class TeamCreateCommand {

    public static LiteralCommandNode<CommandSourceStack> buildSubcommand() {
        return Commands.literal("create")
            .then(Commands.argument("team name", StringArgumentType.greedyString())
                .executes(TeamCreateCommand::execute)
            ).build();
}

    private static int execute(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();

        // check if sender is a player
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ConfigUtil.get("team.create.not-player"));
            return 0;
        }

        String teamName = StringArgumentType.getString(ctx, "team name").trim();
        String tag = teamName.split(" ")[0]; // First word becomes tag
        tag = tag.length() > 5 ? tag.substring(0, 5) : tag; // Clamp to 5 chars max

        var teamManager = Teams.getInstance().getTeamManager();

        // check if already in a team
        Team existing = teamManager.getTeamByPlayer(player.getUniqueId());
        if (existing != null) {
            player.sendMessage(ConfigUtil.get("team.create.in-team"));
            return 0;
        }

        // check if team name is taken
        if (teamManager.teamExists(teamName)) {
            player.sendMessage(ConfigUtil.get("team.create.exists"));
            return 0;
        }


        // check length of team name
        if (teamName.length() > 32) {
            player.sendMessage(ConfigUtil.get("team.create.length-error"));
            return 0;
        }

        // try to create team
        boolean success = teamManager.createTeam(teamName, player.getUniqueId(), tag);

        if (!success) {
            player.sendMessage(ConfigUtil.get("team.create.error"));
            return 0;
        }

        player.sendMessage(ConfigUtil.get("team.create.success")
                .replace("%name%", teamName)
                .replace("%tag%", tag));
        // TODO: Add audit log here

        // debugging
        Team team = teamManager.getTeamByPlayer(player.getUniqueId()); // reload it
        DebugUtil.sendTeamDebugInfo(player, team);

        return 1;
    }
}
