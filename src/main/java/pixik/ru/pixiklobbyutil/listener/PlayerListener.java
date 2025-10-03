package pixik.ru.pixiklobbyutil.listener;

import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.inventory.ItemStack;
import pixik.ru.pixiklobbyutil.PixikLobbyUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.List;

public class PlayerListener implements Listener {

    private final PixikLobbyUtil plugin;
    private final Map<UUID, Long> cooldowns = new HashMap<>();

    public PlayerListener(PixikLobbyUtil plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (plugin.getPluginConfig().getBoolean("auto_give.on_join", true)) {
            giveCompassIfAllowed(player);
        }
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        if (plugin.getPluginConfig().getBoolean("auto_give.on_respawn", true)) {
            giveCompassIfAllowed(player);
        }
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        if (plugin.getPluginConfig().getBoolean("auto_give.on_world_change", true)) {
            giveCompassIfAllowed(player);
        }

        if (plugin.getPluginConfig().getBoolean("auto_give.remove_on_disabled_world", true)) {
            String worldName = player.getWorld().getName();
            List<String> disabledWorlds = plugin.getPluginConfig().getStringList("worlds.disabled_worlds");
            if (disabledWorlds.contains(worldName)) {
                removeCompass(player);
            }
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item != null && plugin.isMenuCompass(item)) {
            Action action = event.getAction();
            String actionName = action.name();

            List<String> allowedClicks = plugin.getPluginConfig().getStringList("menu.click_types");
            boolean allowed = false;
            for (String click : allowedClicks) {
                if (actionName.contains(click.toUpperCase())) {
                    allowed = true;
                    break;
                }
            }

            if (allowed) {
                event.setCancelled(true);

                if (!checkCooldown(player)) {
                    return;
                }

                playClickSound(player);

                if (plugin.getPluginConfig().getBoolean("menu.send_click_message", true)) {
                    String message = plugin.getPluginConfig().getString("messages.click_message", "&aОткрываем меню...");
                    String prefix = plugin.getPluginConfig().getString("messages.prefix", "&6&lPixik&e&lLobby &8» &7");
                    player.sendMessage(prefix.replace('&', '§') + message.replace('&', '§'));
                }

                String command = plugin.getPluginConfig().getString("menu.command", "menu");
                player.performCommand(command);
            }
        }
    }

    @EventHandler
    public void onItemDrop(PlayerDropItemEvent event) {
        if (plugin.getPluginConfig().getBoolean("protection.prevent_drop", true)) {
            ItemStack item = event.getItemDrop().getItemStack();
            if (plugin.isMenuCompass(item)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        ItemStack currentItem = event.getCurrentItem();
        ItemStack cursorItem = event.getCursor();

        boolean isCurrentCompass = currentItem != null && plugin.isMenuCompass(currentItem);
        boolean isCursorCompass = cursorItem != null && plugin.isMenuCompass(cursorItem);

        if (isCurrentCompass || isCursorCompass) {
            boolean preventMove = plugin.getPluginConfig().getBoolean("protection.prevent_move", true);
            boolean preventOtherInventories = plugin.getPluginConfig().getBoolean("protection.prevent_other_inventories", true);
            int compassSlot = plugin.getCompassSlot();

            if (preventMove &&
                    event.getSlot() != compassSlot &&
                    event.getInventory().getType() == InventoryType.PLAYER) {
                event.setCancelled(true);
                return;
            }

            if (preventOtherInventories &&
                    event.getInventory().getType() != InventoryType.PLAYER) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onCraftItem(CraftItemEvent event) {
        if (plugin.getPluginConfig().getBoolean("protection.prevent_crafting", true)) {
            for (ItemStack item : event.getInventory().getMatrix()) {
                if (item != null && plugin.isMenuCompass(item)) {
                    event.setCancelled(true);
                    return;
                }
            }
        }
    }

    private void giveCompassIfAllowed(Player player) {
        String worldName = player.getWorld().getName();

        List<String> enabledWorlds = plugin.getPluginConfig().getStringList("worlds.enabled_worlds");
        List<String> disabledWorlds = plugin.getPluginConfig().getStringList("worlds.disabled_worlds");

        if ((!enabledWorlds.isEmpty() && !enabledWorlds.contains(worldName)) ||
                disabledWorlds.contains(worldName)) {
            return;
        }

        if (hasCompass(player)) {
            return;
        }

        giveCompass(player);
    }

    private void giveCompass(Player player) {
        ItemStack compass = plugin.createMenuCompass();
        player.getInventory().setItem(plugin.getCompassSlot(), compass);

        if (plugin.getPluginConfig().getBoolean("sounds.receive_sound")) {
            try {
                Sound sound = Sound.valueOf(plugin.getPluginConfig().getString("sounds.receive_sound", "ENTITY_ITEM_PICKUP"));
                float volume = (float) plugin.getPluginConfig().getDouble("sounds.receive_volume", 1.0);
                float pitch = (float) plugin.getPluginConfig().getDouble("sounds.receive_pitch", 1.0);
                player.playSound(player.getLocation(), sound, volume, pitch);
            } catch (IllegalArgumentException e) {
            }
        }
    }

    private void removeCompass(Player player) {
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (plugin.isMenuCompass(item)) {
                player.getInventory().setItem(i, null);
                break;
            }
        }
    }

    private boolean hasCompass(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (plugin.isMenuCompass(item)) {
                return true;
            }
        }
        return false;
    }

    private boolean checkCooldown(Player player) {
        if (player.hasPermission("pixiklobbyutil.bypass.cooldown")) {
            return true;
        }

        long cooldownTime = plugin.getPluginConfig().getLong("cooldowns.click_cooldown", 500);
        if (cooldownTime <= 0) {
            return true;
        }

        UUID playerId = player.getUniqueId();
        long currentTime = System.currentTimeMillis();
        Long lastClick = cooldowns.get(playerId);

        if (lastClick != null && currentTime - lastClick < cooldownTime) {
            return false;
        }

        cooldowns.put(playerId, currentTime);
        return true;
    }

    private void playClickSound(Player player) {
        if (plugin.getPluginConfig().getBoolean("sounds.click_sound")) {
            try {
                Sound sound = Sound.valueOf(plugin.getPluginConfig().getString("sounds.click_sound", "UI_BUTTON_CLICK"));
                float volume = (float) plugin.getPluginConfig().getDouble("sounds.click_volume", 1.0);
                float pitch = (float) plugin.getPluginConfig().getDouble("sounds.click_pitch", 1.0);
                player.playSound(player.getLocation(), sound, volume, pitch);
            } catch (IllegalArgumentException e) {
            }
        }
    }
}