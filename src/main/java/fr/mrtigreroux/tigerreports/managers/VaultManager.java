package fr.mrtigreroux.tigerreports.managers;

import fr.mrtigreroux.tigerreports.TigerReports;
import fr.mrtigreroux.tigerreports.data.config.ConfigFile;
import fr.mrtigreroux.tigerreports.utils.ConfigUtils;
import fr.mrtigreroux.tigerreports.utils.MessageUtils;
import net.milkbowl.vault.chat.Chat;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.HashMap;
import java.util.Map;

/**
 * @author MrTigreroux
 */

public class VaultManager {

    private boolean isVaultInstalled;
    private Chat chat = null;

    private boolean displayForStaff = false;
    private boolean displayForPlayers = false;

    private Map<String, String> displayNames = new HashMap<>();

    public VaultManager(boolean isVaultInstalled) {
        this.isVaultInstalled = isVaultInstalled;
        load();
    }

    public void load() {
        if (setupChat()) {
            FileConfiguration configFile = ConfigFile.CONFIG.get();
            displayForStaff = ConfigUtils.isEnabled(configFile, "VaultChat.DisplayForStaff");
            displayForPlayers = ConfigUtils.isEnabled(configFile, "VaultChat.DisplayForPlayers");
        }
    }

    private boolean isEnabled() {
        return isVaultInstalled && ConfigUtils.isEnabled("VaultChat.Enabled");
    }

    private boolean setupChat() {
        if (!isEnabled())
            return false;
        if (chat != null)
            return true;

        RegisteredServiceProvider<Chat> rsp = Bukkit.getServer().getServicesManager().getRegistration(Chat.class);

        if (rsp != null) {
            chat = rsp.getProvider();
            if (chat != null) {
                Bukkit.getLogger()
                        .info(ConfigUtils.getInfoMessage(
                                "The plugin is using the prefixes and suffixes from the chat of Vault plugin to display player names.",
                                "Le plugin utilise les prefixes et suffixes du chat du plugin Vault pour afficher les noms des joueurs."));
                return true;
            }
        }

        MessageUtils.logSevere(ConfigUtils.getInfoMessage("The Chat of Vault plugin could not be used.",
                "Le chat du plugin Vault n'a pas pu etre utilise."));
        return false;
    }

    /**
     * Must be accessed asynchronously if player is offline
     */
    private String getVaultDisplayName(OfflinePlayer p) {
        String name = p.getName();
        if (name == null)
            return null;

        String vaultDisplayName = MessageUtils.translateColorCodes(ConfigFile.CONFIG.get()
                .getString("VaultChat.Format")
                .replace("_Prefix_", chat.getPlayerPrefix(null, p))
                .replace("_Name_", name)
                .replace("_Suffix_", chat.getPlayerSuffix(null, p)));

        displayNames.put(p.getUniqueId().toString(), vaultDisplayName);
        return vaultDisplayName;
    }

    public String getPlayerDisplayName(OfflinePlayer p, boolean staff) {
        if (!(((staff && displayForStaff) || (!staff && displayForPlayers)) && setupChat()))
            return p.getName();

        if (!p.isOnline()) {
            String uuid = p.getUniqueId().toString();
            String lastDisplayName = displayNames.get(uuid);
            if (lastDisplayName != null)
                return lastDisplayName;

            Bukkit.getScheduler().runTaskAsynchronously(TigerReports.getInstance(), () -> {
                getVaultDisplayName(p); // Collected and saved for next time.
            });

            return p.getName();
        }

        return getVaultDisplayName(p);
    }

    public String getOnlinePlayerDisplayName(Player p) {
        return setupChat() ? getVaultDisplayName(p) : p.getDisplayName();
    }

}
