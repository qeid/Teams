package discord.qeid.commands.teams;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import discord.qeid.Teams;
import discord.qeid.model.Team;
import discord.qeid.utils.MessagesUtil;
import discord.qeid.utils.DebugUtil;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.regex.Pattern;

import static discord.qeid.utils.ColorUtils.applyTagCase;
import static org.apache.commons.lang3.StringUtils.capitalize;

public class TeamCreateCommand {

    public static LiteralCommandNode<CommandSourceStack> buildSubcommand() {
        return Commands.literal("create")
            .then(Commands.argument("team name", StringArgumentType.greedyString())
                .executes(TeamCreateCommand::execute)
            ).build();
    }

    private static int execute(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();

        if (!(sender instanceof Player player)) {
            sender.sendMessage(MessagesUtil.get("team.create.not-player"));
            return Command.SINGLE_SUCCESS;
        }

        FileConfiguration config = Teams.getInstance().getConfig();

        int minNameLength = config.getInt("team.create.min-name-length", 3);
        int maxNameLength = config.getInt("team.create.max-name-length", 32);
        int minTagLength = config.getInt("team.create.min-tag-length", 2);
        int maxTagLength = config.getInt("team.create.max-tag-length", 5);
        String nameRegex = config.getString("team.create.name-regex", "^[A-Za-z0-9 _-]+$");
        String tagRegex = config.getString("team.create.tag-regex", "^[A-Za-z0-9]+$");
        
        String tagCase = config.getString("team.create.tag-case", "upper");


        String teamName = StringArgumentType.getString(ctx, "team name").trim();

        // validate name length
        if (teamName.length() < minNameLength) {
            player.sendMessage(MessagesUtil.get("team.create.name-too-short")
                .replace("%min%", String.valueOf(minNameLength)));
            return Command.SINGLE_SUCCESS;
        }
        if (teamName.length() > maxNameLength) {
            player.sendMessage(MessagesUtil.get("team.create.length-error")
                .replace("%max%", String.valueOf(maxNameLength)));
            return Command.SINGLE_SUCCESS;
        }

        // validate name characters
        if (!Pattern.matches(nameRegex, teamName)) {
            player.sendMessage(MessagesUtil.get("team.create.invalid-name"));
            return Command.SINGLE_SUCCESS;
        }

        // tag is first word, clamped to max tag length
        String tag = teamName.split(" ")[0];
        if (tag.length() < minTagLength) {
            player.sendMessage(MessagesUtil.get("team.create.tag-too-short")
                .replace("%min%", String.valueOf(minTagLength)));
            return Command.SINGLE_SUCCESS;
        }
        if (tag.length() > maxTagLength) {
            tag = applyTagCase(tag, tagCase);
        }

        // validate tag characters
        if (!Pattern.matches(tagRegex, tag)) {
            player.sendMessage(MessagesUtil.get("team.create.invalid-tag"));
            return Command.SINGLE_SUCCESS;
        }

        var teamManager = Teams.getInstance().getTeamManager();

        // already in a team
        Team existing = teamManager.getTeamByPlayer(player.getUniqueId());
        if (existing != null) {
            player.sendMessage(MessagesUtil.get("team.create.in-team"));
            return Command.SINGLE_SUCCESS;
        }

        // team name taken?
        if (teamManager.teamExists(teamName)) {
            player.sendMessage(MessagesUtil.get("team.create.exists"));
            return Command.SINGLE_SUCCESS;
        }

        // try to create team
        boolean success = teamManager.createTeam(teamName, player.getUniqueId(), tag);

        if (!success) {
            player.sendMessage(MessagesUtil.get("team.create.error"));
            return Command.SINGLE_SUCCESS;
        }

        player.sendMessage(MessagesUtil.get("team.create.success")
                .replace("%name%", teamName)
                .replace("%tag%", tag));
        // TODO: Add audit log here


        Team team = teamManager.getTeamByPlayer(player.getUniqueId());
        DebugUtil.sendTeamDebugInfo(player, team);

        return Command.SINGLE_SUCCESS;
    }
}
