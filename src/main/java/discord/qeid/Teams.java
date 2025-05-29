package discord.qeid;

import discord.qeid.commands.AdminCommandTree;
import discord.qeid.commands.CommandTree;
import discord.qeid.database.DatabaseManager;
import discord.qeid.database.PlayerDataManager;
import discord.qeid.database.TeamManager;
import discord.qeid.listeners.TeamChatListener;
import discord.qeid.utils.MessagesUtil;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.plugin.java.JavaPlugin;


public class Teams extends JavaPlugin {

    private static Teams instance;

    private DatabaseManager databaseManager; // register the db
    private TeamManager teamManager; // register manager
    private PlayerDataManager playerDataManager; //register temp player data



    @Override
    public void onEnable() {


        instance = this;

        saveDefaultConfig();
        MessagesUtil.loadMessages(this);

        this.databaseManager = new DatabaseManager(this);
        this.teamManager = new TeamManager(this);
        this.playerDataManager = new PlayerDataManager(this);



        getTeamManager().migrateAddCreatedAt();
        getServer().getPluginManager().registerEvents(new TeamChatListener(), this);

        //getTeamManager().logTeamTa    bleColumns();

        // register command
        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            event.registrar().register(new CommandTree(this).build());
            event.registrar().register(new AdminCommandTree(this).build());

        });

        getLogger().info("Teams plugin enabled!");
    }

    @Override
    public void onDisable() {
        if (databaseManager != null) {
            databaseManager.close();
        }
        getLogger().info("Teams plugin disabled.");
    }

    public static Teams getInstance() {
        return instance;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }
    public TeamManager getTeamManager() {
        return teamManager;
    }

    public PlayerDataManager getPlayerDataManager() {
        return playerDataManager;
    }


}
