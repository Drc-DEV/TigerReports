package fr.mrtigreroux.tigerreports.utils;

import com.google.common.primitives.Ints;
import fr.mrtigreroux.tigerreports.TigerReports;
import fr.mrtigreroux.tigerreports.data.config.ConfigFile;
import fr.mrtigreroux.tigerreports.data.config.ConfigSound;
import fr.mrtigreroux.tigerreports.data.config.Message;
import fr.mrtigreroux.tigerreports.data.constants.MenuItem;
import fr.mrtigreroux.tigerreports.data.constants.Status;
import fr.mrtigreroux.tigerreports.data.database.Database;
import fr.mrtigreroux.tigerreports.events.NewReportEvent;
import fr.mrtigreroux.tigerreports.objects.Report;
import fr.mrtigreroux.tigerreports.objects.users.User;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * @author MrTigreroux
 */

public class ReportUtils {

    /**
     * @param rp the reported player instance
     * @param ru the reported user instance
     * @return fixed size list of reported user data
     */
    @SuppressWarnings("deprecation")
    public static List<Object> collectReportedData(Player rp, User ru) {
        return Arrays.asList(rp.getAddress().getAddress().toString(),
                MessageUtils.formatConfigLocation(rp.getLocation()), ru.getLastMessages(),
                rp.getGameMode().toString().toLowerCase(),
                !rp.getLocation().getBlock().getRelative(BlockFace.DOWN).getType().equals(Material.AIR),
                rp.isSneaking(), rp.isSprinting(),
                (int) Math.round(rp.getHealth()) + "/" + (int) Math.round(rp.getMaxHealth()), rp.getFoodLevel(),
                MessageUtils.formatConfigEffects(rp.getActivePotionEffects()));
    }

    @SuppressWarnings("deprecation")
    public static void sendReport(Report r, String server, boolean notify) {
        if (!ConfigUtils.isEnabled("Config.NotifyStackedReports"))
            return;

        try {
            Bukkit.getServer().getPluginManager().callEvent(new NewReportEvent(server, r));
        } catch (Exception ignored) {
        }
        if (!notify)
            return;

        int reportId = r.getId();

        TextComponent alert = new TextComponent();
        alert.setColor(ChatColor.valueOf(MessageUtils.getLastColor(Message.ALERT.get(), "_Reason_").name()));
        if (reportId == -1) {
            MessageUtils.sendStaffMessage(
                    Message.STAFF_MAX_REPORTS_REACHED.get().replace("_Amount_", Integer.toString(getMaxReports())),
                    ConfigSound.STAFF.get());
        } else {
            alert.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/reports #" + reportId));
            alert.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                    new ComponentBuilder(Message.ALERT_DETAILS.get().replace("_Report_", r.getName())).create()));
        }

        for (String line : Message.ALERT.get()
                .replace("_Server_", MessageUtils.getServerName(server))
                .replace("_Reporter_", r.getPlayerName(r.getLastReporterUniqueId(), "Reporter", false, true))
                .replace("_Reported_", r.getPlayerName("Reported", !ReportUtils.onlinePlayerRequired(), true))
                .replace("_Reason_", r.getReason(false))
                .split(ConfigUtils.getLineBreakSymbol())) {
            alert.setText(line);
            MessageUtils.sendStaffMessage(new TextComponent(alert), ConfigSound.REPORT.get());
        }
    }

    public static Report getEssentialOfReport(Map<String, Object> result) {
        if (result == null)
            return null;
        return new Report((int) result.get("report_id"), (String) result.get("status"),
                (String) result.get("appreciation"), (String) result.get("date"), (String) result.get("reported_uuid"),
                (String) result.get("reporter_uuid"), (String) result.get("reason"));
    }

    public static void addReports(String reporter, String reported, boolean archived, Inventory inv, int page,
                                  String actionsBefore, boolean archiveAction, String actionsAfter) {
        int size = inv.getSize();
        int firstReport = 1;
        if (page >= 2) {
            inv.setItem(size - 7, MenuItem.PAGE_SWITCH_PREVIOUS.get());
            firstReport += (page - 1) * 27;
        }

        int first = firstReport - 1;
        TigerReports tr = TigerReports.getInstance();
        Bukkit.getScheduler().runTaskAsynchronously(tr, () -> {
            List<Map<String, Object>> results = tr.getDb()
                    .query("SELECT report_id,status,appreciation,date,reported_uuid,reporter_uuid,reason FROM tigerreports_reports WHERE archived = ?"
                                    + (reporter != null ? " AND reporter_uuid LIKE '%" + reporter + "%'"
                                    : reported != null ? " AND reported_uuid = '" + reported + "'" : "")
                                    + (archived ? " ORDER BY report_id DESC" : "") + " LIMIT 28 OFFSET ?",
                            Arrays.asList(archived ? 1 : 0, first))
                    .getResultList();

            Bukkit.getScheduler().runTask(tr, () -> {
                int index = 0;
                for (int slot = 18; slot < 45; slot++) {
                    if (index == -1) {
                        inv.setItem(slot, null);
                    } else {
                        Report r = getEssentialOfReport(index < results.size() ? results.get(index) : null);
                        if (r == null) {
                            inv.setItem(slot, null);
                            index = -1;
                        } else {
                            inv.setItem(slot,
                                    r.getItem(actionsBefore + (archiveAction
                                            && (r.getStatus() == Status.DONE || !ReportUtils.onlyDoneArchives())
                                            ? Message.REPORT_ARCHIVE_ACTION.get()
                                            : "")
                                            + actionsAfter));
                            index++;
                        }
                    }
                }

                if (results.size() == 28)
                    inv.setItem(size - 3, MenuItem.PAGE_SWITCH_NEXT.get());
            });
        });
    }

    public static int getTotalReports(Database db) {
        Object o = db.query("SELECT COUNT(report_id) AS Total FROM tigerreports_reports", null).getResult(0, "Total");
        return o instanceof Integer ? (int) o : Ints.checkedCast((long) o);
    }

    public static int getMaxReports() {
        return ConfigFile.CONFIG.get().getInt("Config.MaxReports", 100);
    }

    public static boolean permissionRequired() {
        return ConfigUtils.isEnabled("Config.PermissionRequired");
    }

    public static boolean onlinePlayerRequired() {
        return ConfigUtils.isEnabled("Config.ReportOnline");
    }

    public static int getMinCharacters() {
        return ConfigFile.CONFIG.get().getInt("Config.MinCharacters", 4);
    }

    public static long getCooldown() {
        return ConfigFile.CONFIG.get().getLong("Config.ReportCooldown", 300);
    }

    public static long getAbusiveReportCooldown() {
        return ConfigFile.CONFIG.get().getLong("Config.AbusiveReport.Cooldown", 3600);
    }

    public static boolean onlyDoneArchives() {
        return ConfigUtils.isEnabled("Config.OnlyDoneArchives");
    }

    public static boolean stackReports() {
        return ConfigUtils.isEnabled("Config.StackReports");
    }

    public static boolean punishmentsEnabled() {
        return ConfigUtils.isEnabled("Config.Punishments.Enabled");
    }

}
