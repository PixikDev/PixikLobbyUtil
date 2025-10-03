package pixik.ru.pixiklobbyutil;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import pixik.ru.pixiklobbyutil.commands.CommandHandler;
import pixik.ru.pixiklobbyutil.listener.PlayerListener;

import java.util.List;
import java.util.stream.Collectors;

public class PixikLobbyUtil extends JavaPlugin {

    private static PixikLobbyUtil instance;
    private NamespacedKey menuEmeraldKey;
    private FileConfiguration config;

    @Override
    public void onEnable() {
        instance = this;
        menuEmeraldKey = new NamespacedKey(this, "pixik_menu_emerald");

        saveDefaultConfig();
        config = getConfig();

        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        getCommand("pixiklobbyutil").setExecutor(new CommandHandler(this));

        getLogger().info("PixikLobbyUtil plugin enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("PixikLobbyUtil plugin disabled!");
    }

    public void reloadPluginConfig() {
        reloadConfig();
        config = getConfig();
    }

    public static PixikLobbyUtil getInstance() {
        return instance;
    }

    public NamespacedKey getMenuEmeraldKey() {
        return menuEmeraldKey;
    }

    public FileConfiguration getPluginConfig() {
        return config;
    }

    public ItemStack createMenuEmerald() {
        String materialName = config.getString("compass.material", "EMERALD");
        Material material = Material.getMaterial(materialName);
        if (material == null) {
            material = Material.EMERALD;
        }

        ItemStack emerald = new ItemStack(material);
        ItemMeta meta = emerald.getItemMeta();

        String displayName = config.getString("compass.display_name", "&6&lМЕНЮ");
        meta.displayName(net.kyori.adventure.text.Component.text(displayName.replace('&', '§')));

        List<String> lore = config.getStringList("compass.lore");
        if (!lore.isEmpty()) {
            List<net.kyori.adventure.text.Component> loreComponents = lore.stream()
                    .map(line -> net.kyori.adventure.text.Component.text(line.replace('&', '§')))
                    .collect(Collectors.toList());
            meta.lore(loreComponents);
        }

        meta.getPersistentDataContainer().set(menuEmeraldKey, PersistentDataType.BYTE, (byte) 1);

        if (config.contains("compass.custom_model_data")) {
            meta.setCustomModelData(config.getInt("compass.custom_model_data"));
        }

        if (config.getBoolean("compass.glowing", false)) {
            meta.setEnchantmentGlintOverride(true);
        }

        meta.setUnbreakable(true);

        emerald.setItemMeta(meta);
        return emerald;
    }

    public boolean isMenuEmerald(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return false;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }

        Byte value = meta.getPersistentDataContainer().get(menuEmeraldKey, PersistentDataType.BYTE);
        return value != null && value == (byte) 1;
    }

    public int getMenuEmeraldSlot() {
        return config.getInt("compass.slot", 4);
    }
}