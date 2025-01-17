package dristmine.dristbank;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public class ChequeListener implements Listener {
    private final ConfigManager configManager;
    private final MessageManager messageManager;

    public ChequeListener(DristBank dristBank, ConfigManager configManager, MessageManager messageManager) {
        this.configManager = configManager;
        this.messageManager = messageManager;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (!player.isSneaking() ||
                !(event.getAction().equals(Action.RIGHT_CLICK_AIR) || event.getAction().equals(Action.RIGHT_CLICK_BLOCK)) ||
                item == null || item.getType() != Material.PAPER) {
            return;
        }

        // Check if the paper item has a cheque value attached to it
        double chequeAmount = getChequeAmount(item);
        if (chequeAmount > 0) {
            // Check player's balance
            double currentBalance = configManager.getConfig().getDouble("player-info." + player.getUniqueId(), 0);

            // Add the cheque value to the player's balance
            configManager.getConfig().set("player-info." + player.getUniqueId(), currentBalance + chequeAmount);
            configManager.saveConfig();

            // Subtract 1 from the player's item in hand
            if (item.getAmount() > 1) {
                item.setAmount(item.getAmount() - 1);
            } else {
                player.getInventory().setItemInMainHand(null); // Delete the cheque item if there was only 1
            }

            // Notify the player about redeeming the cheque and show their current balance
            player.sendMessage(messageManager.getMessage("cheque-redeemed", chequeAmount, currentBalance + chequeAmount));
        }
    }

    private double getChequeAmount(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null && meta.hasDisplayName()) {
            NamespacedKey key = new NamespacedKey(DristBank.getPlugin(DristBank.class), "cheque-value");
            PersistentDataContainer container = meta.getPersistentDataContainer();
            if (container.has(key, PersistentDataType.DOUBLE)) {
                return container.get(key, PersistentDataType.DOUBLE);
            }
        }
        return 0;
    }
}
