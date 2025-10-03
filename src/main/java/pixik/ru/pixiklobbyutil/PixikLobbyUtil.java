package pixik.ru.pixiklobbyutil;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import pixik.ru.pixiklobbyutil.listener.PlayerListener;

import java.util.List;
import java.util.stream.Collectors;

public class PixikLobbyUtil extends JavaPlugin {

    private static PixikLobbyUtil instance;
    private NamespacedKey compassKey;
    private FileConfiguration config;

    @Override
    public void onEnable() {
        instance = this;
        compassKey = new NamespacedKey(this, "pixik_menu_compass");

        saveDefaultConfig();
        config = getConfig();

        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);

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

    public NamespacedKey getCompassKey() {
        return compassKey;
    }

    public FileConfiguration getPluginConfig() {
        return config;
    }

    public ItemStack createMenuCompass() {
        String materialName = config.getString("compass.material", "COMPASS");
        Material material = Material.getMaterial(materialName);
        if (material == null) {
            material = Material.COMPASS;
        }

        ItemStack compass = new ItemStack(material);
        ItemMeta meta = compass.getItemMeta();

        String displayName = config.getString("compass.display_name", "&6&lМЕНЮ");
        meta.displayName(net.kyori.adventure.text.Component.text(displayName.replace('&', '§')));

        List<String> lore = config.getStringList("compass.lore");
        if (!lore.isEmpty()) {
            List<net.kyori.adventure.text.Component> loreComponents = lore.stream()
                    .map(line -> net.kyori.adventure.text.Component.text(line.replace('&', '§')))
                    .collect(Collectors.toList());
            meta.lore(loreComponents);
        }

        meta.getPersistentDataContainer().set(compassKey, PersistentDataType.BYTE, (byte) 1);

        if (config.contains("compass.custom_model_data")) {
            meta.setCustomModelData(config.getInt("compass.custom_model_data"));
        }

        if (config.getBoolean("compass.glowing", false)) {
            meta.setEnchantmentGlintOverride(true);
        }

        meta.setUnbreakable(true);

        compass.setItemMeta(meta);
        return compass;
    }

    public boolean isMenuCompass(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return false;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }

        Byte value = meta.getPersistentDataContainer().get(compassKey, PersistentDataType.BYTE);
        return value != null && value == (byte) 1;
    }

    public int getCompassSlot() {
        return config.getInt("compass.slot", 4);
    }
}