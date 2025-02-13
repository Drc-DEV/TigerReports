package fr.mrtigreroux.tigerreports.managers;

import fr.mrtigreroux.tigerreports.TigerReports;
import fr.mrtigreroux.tigerreports.data.database.Database;
import fr.mrtigreroux.tigerreports.objects.users.OfflineUser;
import fr.mrtigreroux.tigerreports.objects.users.OnlineUser;
import fr.mrtigreroux.tigerreports.objects.users.User;
import fr.mrtigreroux.tigerreports.utils.ConfigUtils;
import fr.mrtigreroux.tigerreports.utils.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * @author MrTigreroux
 */

public class UsersManager {

    private final Map<String, User> users = new HashMap<>();
    private final Map<String, String> lastNameFound = new HashMap<>();
    private final Map<String, String> lastUniqueIdFound = new HashMap<>();
    private final List<String> exemptedPlayers = new ArrayList<>();

    public UsersManager() {
    }

    public void addExemptedPlayer(String name) {
        if (name != null && !exemptedPlayers.contains(name))
            exemptedPlayers.add(name);
    }

    public void removeExemptedPlayer(String name) {
        exemptedPlayers.remove(name);
    }

    public List<String> getExemptedPlayers() {
        return exemptedPlayers;
    }

    public void saveUser(User u) {
        users.put(u.getUniqueId(), u);
    }

    public void removeUser(String uuid) {
        users.remove(uuid);
    }

    public User getSavedUser(String uuid) {
        return users.get(uuid);
    }

    public User getUser(String uuid) {
        if (uuid == null)
            return null;
        User u = users.get(uuid);
        if (u == null) {
            try {
                u = new OnlineUser(Bukkit.getPlayer(UUID.fromString(uuid)));
            } catch (Exception offlinePlayer) {
                u = new OfflineUser(uuid);
            }
            saveUser(u);
        }
        return u;
    }

    public OnlineUser getOnlineUser(Player p) {
        User u = users.get(p.getUniqueId().toString());
        if (u == null) {
            u = new OnlineUser(p);
            saveUser(u);
        } else if (!(u instanceof OnlineUser) || !((OnlineUser) u).getPlayer().equals(p)) {
            List<String> lastMessages = u.lastMessages;
            u = new OnlineUser(p);
            u.lastMessages = lastMessages;
            saveUser(u);
        }
        return (OnlineUser) u;
    }

    public Collection<User> getUsers() {
        return users.values();
    }

    public void clearUsers() {
        users.clear();
    }

    public String getUniqueId(String name) {
        String uuid = lastUniqueIdFound.get(name);
        if (uuid == null) {
            @SuppressWarnings("deprecation")
            OfflinePlayer p = Bukkit.getOfflinePlayer(name);
            if (p != null)
                uuid = p.getUniqueId().toString();
        }
        if (uuid != null) {
            lastUniqueIdFound.put(name, uuid);
            return uuid;
        }
        Bukkit.getLogger()
                .warning(ConfigUtils.getInfoMessage("The UUID of the name <" + name + "> was not found.",
                        "L'UUID du pseudo <" + name + "> n'a pas ete trouve."));
        return null;
    }

    public String getName(String uuid, UUID uniqueId) {
        return getName(uuid, uniqueId, null);
    }

    public String getName(String uuid, UUID uniqueId, OfflinePlayer p) {
        if (uniqueId != null) {
            String name = lastNameFound.get(uuid);
            if (name == null) {
                if (p == null) {
                    p = Bukkit.getOfflinePlayer(uniqueId);
                }
                if (p != null) {
                    name = p.getName();
                }
            }
            if (name == null)
                try {
                    name = (String) TigerReports.getInstance()
                            .getDb()
                            .query("SELECT name FROM tigerreports_users WHERE uuid = ?", Arrays.asList(uuid))
                            .getResult(0, "name");
                } catch (Exception nameNotFound) {
                }
            if (name != null) {
                lastNameFound.put(uuid, name);
                return name;
            }
        }
        Bukkit.getLogger()
                .warning(ConfigUtils.getInfoMessage("The name of the UUID <" + uuid + "> was not found.",
                        "Le pseudo de l'UUID <" + uuid + "> n'a pas ete trouve."));
        return null;
    }

    public void startCooldownForUsers(String[] usersUuid, long seconds, Database db) {
        int amount = usersUuid.length;
        if (amount <= 0)
            return;

        if (amount == 1) {
            User u = getUser(usersUuid[0]);
            if (u != null) {
                u.startCooldown(seconds, false);
            }
            return;
        }

        String cooldown = MessageUtils.getRelativeDate(seconds);

        StringBuilder query = new StringBuilder("UPDATE tigerreports_users SET cooldown = ? WHERE uuid IN (?");
        for (int i = 1; i < amount; i++) {
            query.append(",?");
        }
        query.append(")");

        List<Object> queryParams = new ArrayList<>(Arrays.asList((Object[]) usersUuid));
        queryParams.add(0, cooldown);
        db.updateAsynchronously(query.toString(), queryParams);
    }

}
