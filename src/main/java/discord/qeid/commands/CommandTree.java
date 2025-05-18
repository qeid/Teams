package discord.qeid.commands;

import com.mojang.brigadier.tree.LiteralCommandNode;
import discord.qeid.Teams;
import discord.qeid.commands.teams.*;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;

public class CommandTree {

    private final Teams plugin;

    public CommandTree(Teams plugin) {
        this.plugin = plugin;
    }

    public static LiteralCommandNode<CommandSourceStack> build() {
        return Commands.literal("team")
            .executes(TeamHelpCommand::runHelpDefault)
            .then(TeamBanCommand.buildSubcommand())
            .then(TeamHelpCommand.buildSubcommand())
            .then(TeamCreateCommand.buildSubcommand())
            .then(TeamInviteCommand.buildSubcommand())
            .then(TeamJoinCommand.buildSubcommand())
            .then(TeamKickCommand.buildSubcommand())
            .then(TeamLeaveCommand.buildSubcommand())
            .then(TeamDisbandCommand.buildSubcommand())
            .then(TeamInfoCommand.buildSubcommand())
            .then(TeamTransferCommand.buildSubcommand())
            .then(TeamWhoCommand.buildSubcommand())
            .then(TeamPromoteCommand.buildSubcommand())
            .then(TeamDemoteCommand.buildSubcommand())
            .then(TeamSetHomeCommand.buildSubcommand())
            .then(TeamHomeCommand.buildSubcommand())
            .then(TeamDelHomeCommand.buildSubcommand())

        .build();

    }



}
