package discord.qeid;

import discord.qeid.api.TeamsPlaceholderExpansion;
import discord.qeid.commands.AdminCommandTree;
import discord.qeid.commands.CommandTree;
import discord.qeid.database.AdminLogManager;
import discord.qeid.database.DatabaseManager;
import discord.qeid.database.PlayerDataManager;
import discord.qeid.database.TeamManager;
import discord.qeid.listeners.TeamChatListener;
import discord.qeid.utils.MessagesUtil;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.plugin.java.JavaPlugin;


public class Teams extends JavaPlugin {

    /**
     * Teams plugin main class.
     * This class initializes the plugin, registers commands, and manages the lifecycle of the plugin.*/

    private static Teams instance;

    private DatabaseManager databaseManager; // register the db
    private TeamManager teamManager; // register manager
    private PlayerDataManager playerDataManager; //register temp player data
    private AdminLogManager adminLogManager;



    @Override
    public void onEnable() {


        instance = this;

        saveDefaultConfig();
        MessagesUtil.loadMessages(this);

        this.databaseManager = new DatabaseManager(this);
        this.teamManager = new TeamManager(this);
        this.playerDataManager = new PlayerDataManager(this);
        this.adminLogManager = new AdminLogManager(this);



        getTeamManager().migrateAddCreatedAt();
        getServer().getPluginManager().registerEvents(new TeamChatListener(), this);


        // register commands
        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            event.registrar().register(new AdminCommandTree(this).build());
            event.registrar().register(new CommandTree(this).build());

        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new TeamsPlaceholderExpansion().register();
            getLogger().info("PlaceholderAPI detected. Teams placeholders registered.");
    } else {
            getLogger().warning("==============================================");
            getLogger().warning("PlaceholderAPI not found! Teams placeholders will NOT work.");
            getLogger().warning("Download it from: https://www.spigotmc.org/resources/placeholderapi.6245/");
            getLogger().warning("==============================================");
        }

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
    public AdminLogManager getAdminLogManager() {
        return adminLogManager;
    }



}
