package discord.qeid.commands;

import com.mojang.brigadier.tree.LiteralCommandNode;
import discord.qeid.Teams;
import discord.qeid.commands.admin.TeamReloadCommand;
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
            .executes(TeamHelpCommand::runAdminHelp)
            .then(TeamReloadCommand.buildSubcommand())
        .build();
    }
}