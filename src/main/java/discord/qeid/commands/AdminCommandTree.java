package discord.qeid.commands;

import com.mojang.brigadier.tree.LiteralCommandNode;
import discord.qeid.Teams;
import discord.qeid.commands.admin.*;
import discord.qeid.commands.teams.TeamHelpCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;

public class AdminCommandTree {

    private final Teams plugin;

    public AdminCommandTree(Teams plugin) {
        this.plugin = plugin;
    }

    public static LiteralCommandNode<CommandSourceStack> build() {
        return Commands.literal("teama")
            .requires(ctx -> ctx.getSender().hasPermission(Teams.getInstance().getConfig().getString("admin.permission", "teams.admin")))
            .executes(TeamHelpCommand::runAdminHelp)
            .then(TeamAdminReloadCommand.buildSubcommand())
            .then(TeamAdminForceJoinCommand.buildSubcommand())
            .then(TeamAdminLogsCommand.buildSubcommand())
            .then(TeamAdminForceDisbandCommand.buildSubcommand())
        .build();
    }
}