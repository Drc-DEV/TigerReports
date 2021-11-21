package fr.mrtigreroux.tigerreports.utils;

import fr.mrtigreroux.tigerreports.data.config.ConfigFile;
import fr.mrtigreroux.tigerreports.objects.Report;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.time.ZoneId;

/**
 * @author MrTigreroux
 */

public class ConfigUtils {

    public static boolean isEnabled(ConfigurationSection config, String path) {
        return config.getBoolean(path);
    }

    public static boolean isEnabled(String path) {
        return isEnabled(ConfigFile.CONFIG.get(), path);
    }

    public static char getColorCharacter() {
        return ConfigFile.CONFIG.get().getString("Config.ColorCharacter", "&").charAt(0);
    }

    public static String getLineBreakSymbol() {
        return ConfigFile.CONFIG.get().getString("Config.LineBreakSymbol", "//");
    }

    public static String getInfoLanguage() {
        return ConfigFile.CONFIG.get().getString("Config.InfoLanguage", "French");
    }

    public static String getInfoMessage(String english, String french) {
        return "[TigerReports] " + (getInfoLanguage().equalsIgnoreCase("English") ? english : french);
    }

    public static boolean exist(ConfigurationSection config, String path) {
        return config.get(path) != null;
    }

    public static Material getMaterial(ConfigurationSection config, String path) {
        String icon = config.getString(path);
        return icon != null && icon.startsWith("Material-")
                ? Material.matchMaterial(icon.split("-")[1].toUpperCase().replace(":" + getDamage(config, path), ""))
                : null;
    }

    public static short getDamage(ConfigurationSection config, String path) {
        try {
            String icon = config.getString(path);
            return icon != null && icon.startsWith("Material-") && icon.contains(":")
                    ? Short.parseShort(icon.split(":")[1])
                    : 0;
        } catch (Exception noDamage) {
            return 0;
        }
    }

    public static String getSkull(ConfigurationSection config, String path) {
        String icon = config.getString(path);
        return icon != null && icon.startsWith("Skull-") ? icon.split("-")[1] : null;
    }

    public static boolean playersNotifications() {
        return isEnabled(ConfigFile.CONFIG.get(), "Config.Notifications.Players");
    }

    public static ZoneId getZoneId() {
        String time = ConfigFile.CONFIG.get().getString("Config.Time");
        if (!time.equals("default")) {
            try {
                return ZoneId.of(time);
            } catch (Exception ex) {
            }
        }
        return ZoneId.systemDefault();
    }

    public static void processCommands(ConfigurationSection config, String path, Report r, Player staff) {
        if (staff == null) {
            Bukkit.getLogger()
                    .warning(ConfigUtils.getInfoMessage(
                            "Could not process commands at <" + path + "> for report #" + r.getId()
                                    + " because staff is unknown.",
                            "Les commandes configur�es dans <" + path + "> pour le signalement #" + r.getId()
                                    + " n'ont pas pu �tre trait�es car le staff est inconnu."));
            return;
        }
        String reported = r.getPlayerName("Reported", false, false);
        String reportId = Integer.toString(r.getId());
        String[] reportersUniqueIds = r.getReportersUniqueIds();
        for (String command : config.getStringList(path)) {
            command = command.replace("_Reported_", reported)
                    .replace("_Staff_", staff.getName())
                    .replace("_Id_", reportId)
                    .replace("_Reason_", r.getReason(false));
            if (command.contains("_Reporter_")) {
                for (String uuid : reportersUniqueIds) {
                    executeCommand(staff, command.replace("_Reporter_", UserUtils.getName(uuid)));
                }
            } else {
                executeCommand(staff, command);
            }
        }
    }

    public static void executeCommand(Player p, String command) {
        if (command.startsWith("-CONSOLE")) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command.substring(9));
        } else {
            Bukkit.dispatchCommand(p, command);
        }
    }

}
