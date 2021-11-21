package fr.mrtigreroux.tigerreports.listeners;

import fr.mrtigreroux.tigerreports.TigerReports;
import fr.mrtigreroux.tigerreports.objects.users.OnlineUser;
import fr.mrtigreroux.tigerreports.runnables.MenuUpdater;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.Inventory;

/**
 * @author MrTigreroux
 */

public class InventoryListener implements Listener {

    private TigerReports tr;

    public InventoryListener(TigerReports tr) {
        this.tr = tr;
    }

    @EventHandler(priority = EventPriority.LOW)
    private void onInventoryDrag(InventoryDragEvent e) {
        if (checkMenuAction(e.getWhoClicked(), e.getInventory()) != null)
            e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOW)
    private void onInventoryClick(InventoryClickEvent e) {
        Inventory inv = e.getClickedInventory();
        OnlineUser u = checkMenuAction(e.getWhoClicked(), inv);
        if (u != null) {
            if (inv.getType() == InventoryType.CHEST) {
                e.setCancelled(true);
                if (e.getCursor().getType() == Material.AIR)
                    u.getOpenedMenu().click(e.getCurrentItem(), e.getSlot(), e.getClick());
            } else if (inv.getType() == InventoryType.PLAYER
                    && (e.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY
                    || e.getAction() == InventoryAction.COLLECT_TO_CURSOR)) {
                e.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    private void onInventoryClose(InventoryCloseEvent e) {
        OnlineUser u = tr.getUsersManager().getOnlineUser((Player) e.getPlayer());
        MenuUpdater.removeUser(u);
        u.setOpenedMenu(null);
        try {
            tr.getDb().startClosing();
        } catch (Exception ignored) {
        }
    }

    private OnlineUser checkMenuAction(HumanEntity whoClicked, Inventory inv) {
        if (!(whoClicked instanceof Player) || inv == null)
            return null;
        OnlineUser u = tr.getUsersManager().getOnlineUser((Player) whoClicked);
        return u.getOpenedMenu() != null ? u : null;
    }

}
