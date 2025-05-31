package discord.qeid.commands.teams;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import discord.qeid.Teams;
import discord.qeid.model.Team;
import discord.qeid.utils.MessagesUtil;
import discord.qeid.utils.SoundUtil;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

import static discord.qeid.utils.ColorUtils.applyTagCase;

public class TeamCreateCommand {

    /**
     * A command to create a new team.
     * Usage: /team create <team name>
     * The team name is the first word of the input, and it will be used as the team tag.
     * The tag is clamped to a maximum length defined in the config.
     */

    // Map<UUID, lastCreateTimeMillis>
    private static final Map<UUID, Long> lastCreate = new HashMap<>();

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
        int cooldownSeconds = config.getInt("team.create.cooldown-seconds", 600);

        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        long last = lastCreate.getOrDefault(uuid, 0L);
        long wait = (last + cooldownSeconds * 1000L) - now;
        if (wait > 0) {
            long seconds = wait / 1000;
            player.sendMessage(MessagesUtil.get("team.create.cooldown")
                .replace("%cooldown%", String.valueOf(seconds)));
            player.playSound(player.getLocation(), SoundUtil.get("team.sounds.error"), 1.0F, 1.5F);
            return Command.SINGLE_SUCCESS;
        }

        String teamName = StringArgumentType.getString(ctx, "team name").trim();

        // validate name length
        if (teamName.length() < minNameLength) {
            player.sendMessage(MessagesUtil.get("team.create.name-too-short")
                .replace("%min%", String.valueOf(minNameLength)));
            player.playSound(player.getLocation(), SoundUtil.get("team.sounds.error"), 1.0F, 1.5F);
            return Command.SINGLE_SUCCESS;
        }
        if (teamName.length() > maxNameLength) {
            player.sendMessage(MessagesUtil.get("team.create.length-error")
                .replace("%max%", String.valueOf(maxNameLength)));
            player.playSound(player.getLocation(), SoundUtil.get("team.sounds.error"), 1.0F, 1.5F);
            return Command.SINGLE_SUCCESS;
        }

        // validate name characters
        if (!Pattern.matches(nameRegex, teamName)) {
            player.sendMessage(MessagesUtil.get("team.create.invalid-name"));
            player.playSound(player.getLocation(), SoundUtil.get("team.sounds.error"), 1.0F, 1.5F);
            return Command.SINGLE_SUCCESS;
        }

        // tag is first word, clamped to max tag length
        String tag = teamName.split(" ")[0];
        tag = applyTagCase(tag, tagCase);

        if (tag.length() < minTagLength) {
            player.sendMessage(MessagesUtil.get("team.create.tag-too-short")
                .replace("%min%", String.valueOf(minTagLength)));
            player.playSound(player.getLocation(), SoundUtil.get("team.sounds.error"), 1.0F, 1.5F);
            return Command.SINGLE_SUCCESS;
        }
        if (tag.length() > maxTagLength) {
            tag = tag.substring(0, maxTagLength);
        }

        // validate tag characters
        if (!Pattern.matches(tagRegex, tag)) {
            player.sendMessage(MessagesUtil.get("team.create.invalid-tag"));
            player.playSound(player.getLocation(), SoundUtil.get("team.sounds.error"), 1.0F, 1.5F);
            return Command.SINGLE_SUCCESS;
        }

        var teamManager = Teams.getInstance().getTeamManager();

        // already in a team
        Team existing = teamManager.getTeamByPlayer(player.getUniqueId());
        if (existing != null) {
            player.sendMessage(MessagesUtil.get("team.create.in-team"));
            player.playSound(player.getLocation(), SoundUtil.get("team.sounds.error"), 1.0F, 1.5F);
            return Command.SINGLE_SUCCESS;
        }

        // team name taken?
        if (teamManager.teamExists(teamName)) {
            player.sendMessage(MessagesUtil.get("team.create.exists"));
            player.playSound(player.getLocation(), SoundUtil.get("team.sounds.error"), 1.0F, 1.5F);
            return Command.SINGLE_SUCCESS;
        }

        // tag taken? (disambiguity check)
        Team tagExisting = teamManager.getTeamByTag(tag);
        if (tagExisting != null) {
            player.sendMessage(MessagesUtil.get("team.tag.exists")
                .replace("%tag%", tag));
            player.playSound(player.getLocation(), SoundUtil.get("team.sounds.error"), 1.0F, 1.5F);
            return Command.SINGLE_SUCCESS;
        }

        // try to create team
        boolean success = teamManager.createTeam(teamName, player.getUniqueId(), tag);

        if (!success) {
            player.sendMessage(MessagesUtil.get("team.create.error"));
            player.playSound(player.getLocation(), SoundUtil.get("team.sounds.error"), 1.0F, 1.5F);
            return Command.SINGLE_SUCCESS;
        }

        // Set cooldown
        lastCreate.put(uuid, now);

        player.sendMessage(MessagesUtil.get("team.create.success")
                .replace("%name%", teamName)
                .replace("%tag%", tag));
        player.playSound(player.getLocation(), SoundUtil.get("team.sounds.success"), 1.0F, 1.5F);

        Team team = teamManager.getTeamByPlayer(player.getUniqueId());
        //DebugUtil.sendTeamDebugInfo(player, team);

        return Command.SINGLE_SUCCESS;
    }
}
