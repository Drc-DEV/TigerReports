package fr.mrtigreroux.tigerreports;

import fr.mrtigreroux.tigerreports.commands.ReportCommand;
import fr.mrtigreroux.tigerreports.commands.ReportsCommand;
import fr.mrtigreroux.tigerreports.data.config.ConfigFile;
import fr.mrtigreroux.tigerreports.data.constants.MenuItem;
import fr.mrtigreroux.tigerreports.data.constants.MenuRawItem;
import fr.mrtigreroux.tigerreports.data.database.Database;
import fr.mrtigreroux.tigerreports.data.database.MySQL;
import fr.mrtigreroux.tigerreports.data.database.SQLite;
import fr.mrtigreroux.tigerreports.listeners.InventoryListener;
import fr.mrtigreroux.tigerreports.listeners.PlayerListener;
import fr.mrtigreroux.tigerreports.managers.*;
import fr.mrtigreroux.tigerreports.objects.users.OnlineUser;
import fr.mrtigreroux.tigerreports.objects.users.User;
import fr.mrtigreroux.tigerreports.runnables.ReportsNotifier;
import fr.mrtigreroux.tigerreports.utils.ConfigUtils;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collection;
import java.util.logging.Logger;

/**
 * @author MrTigreroux Published on: 30/06/2016
 */

public class TigerReports extends JavaPlugin {

    private static TigerReports instance;

    private Database database;
    private BungeeManager bungeeManager;
    private UsersManager usersManager;
    private ReportsManager reportsManager;
    private VaultManager vaultManager = null;

    public TigerReports() {
    }

    public void load() {
        for (ConfigFile configFiles : ConfigFile.values())
            configFiles.load();

        ReportsNotifier.start();
        MenuItem.init();
        if (vaultManager != null)
            vaultManager.load();
    }

    @Override
    public void onEnable() {
        instance = this;
        MenuRawItem.init();
        load();

        PluginManager pm = Bukkit.getServer().getPluginManager();
        pm.registerEvents(new InventoryListener(this), this);
        pm.registerEvents(new PlayerListener(this), this);

        getCommand("report").setExecutor(new ReportCommand(this));
        getCommand("reports").setExecutor(new ReportsCommand(this));

        usersManager = new UsersManager();
        reportsManager = new ReportsManager();
        bungeeManager = new BungeeManager(this);
        vaultManager = new VaultManager(pm.getPlugin("Vault") != null);

        initializeDatabase();
    }

    public void unload() {
        if (database != null)
            database.closeConnection();

        if (usersManager != null) {
            Collection<User> users = usersManager.getUsers();
            if (users != null && !users.isEmpty()) {
                for (User u : users) {
                    if (u instanceof OnlineUser) {
                        OnlineUser ou = (OnlineUser) u;
                        if (ou.getOpenedMenu() != null)
                            ou.getPlayer().closeInventory();
                    }
                }
            }
        }
    }

    @Override
    public void onDisable() {
        unload();
    }

    public static TigerReports getInstance() {
        return instance;
    }

    public Database getDb() {
        return database;
    }

    public BungeeManager getBungeeManager() {
        return bungeeManager;
    }

    public UsersManager getUsersManager() {
        return usersManager;
    }

    public ReportsManager getReportsManager() {
        return reportsManager;
    }

    public VaultManager getVaultManager() {
        return vaultManager;
    }

    public void initializeDatabase() {
        Bukkit.getScheduler().runTaskAsynchronously(instance, () -> {
            Logger logger = Bukkit.getLogger();
            try {
                FileConfiguration config = ConfigFile.CONFIG.get();
                MySQL mysql = new MySQL(config.getString("MySQL.Host"), config.getInt("MySQL.Port"),
                        config.getString("MySQL.Database"), config.getString("MySQL.Username"),
                        config.getString("MySQL.Password"), ConfigUtils.isEnabled(config, "MySQL.UseSSL"),
                        ConfigUtils.isEnabled(config, "MySQL.VerifyServerCertificate"));
                mysql.check();
                database = mysql;
                logger.info(ConfigUtils.getInfoMessage("The plugin is using a MySQL database.",
                        "Le plugin utilise une base de donnees MySQL."));
            } catch (Exception invalidMySQL) {
                database = new SQLite();
                logger.info(ConfigUtils.getInfoMessage("The plugin is using the SQLite (default) database.",
                        "Le plugin utilise la base de donnees SQLite (par defaut)."));
            }
            database.initialize();
        });
    }

}
