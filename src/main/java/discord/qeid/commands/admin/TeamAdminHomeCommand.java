package discord.qeid.commands.admin;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import discord.qeid.Teams;
import discord.qeid.database.TeamManager;
import discord.qeid.model.Team;
import discord.qeid.utils.MessagesUtil;
import discord.qeid.utils.SoundUtil;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class TeamAdminHomeCommand {

    public static LiteralCommandNode<CommandSourceStack> buildSubcommand() {
        return Commands.literal("home")
            .then(Commands.argument("team name", StringArgumentType.greedyString())
                .suggests((ctx, builder) -> {
                    Teams.getInstance().getTeamManager().getAllTeams().stream()
                        .map(Team::getName)
                        .filter(name -> name.toLowerCase().startsWith(builder.getRemainingLowerCase()))
                        .forEach(builder::suggest);
                    return builder.buildFuture();
                })
                .executes(ctx -> {
                    String teamName = StringArgumentType.getString(ctx, "team name");
                    return execute(ctx.getSource().getSender(), teamName);
                })
            ).build();
    }

    private static int execute(CommandSender sender, String teamName) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(MessagesUtil.get("general.players-only"));
            return 1;
        }

        TeamManager teamManager = Teams.getInstance().getTeamManager();
        Team team = teamManager.getTeamByName(teamName);

        if (team == null) {
            player.sendMessage(MessagesUtil.get("admin.home.not-found"));
            player.playSound(player.getLocation(), SoundUtil.get("team.sounds.error"), 1.0F, 1.5F);
            return 1;
        }

        Location home = teamManager.getHome(team.getId());
        if (home == null) {
            player.sendMessage(MessagesUtil.get("admin.home.not-set"));
            player.playSound(player.getLocation(), SoundUtil.get("team.sounds.error"), 1.0F, 1.5F);
            return 1;
        }

        player.teleport(home);
        player.sendMessage(MessagesUtil.get("admin.home.success")
            .replace("%team%", team.getName()));
        player.playSound(player.getLocation(), SoundUtil.get("team.sounds.success"), 1.0F, 1.5F);

        return 1;
    }
}
