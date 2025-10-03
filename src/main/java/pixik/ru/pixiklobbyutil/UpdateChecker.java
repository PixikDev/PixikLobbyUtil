package pixik.ru.pixiklobbyutil;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class UpdateChecker {

    private final JavaPlugin plugin;
    private final String currentVersion;

    public UpdateChecker(JavaPlugin plugin) {
        this.plugin = plugin;
        this.currentVersion = plugin.getDescription().getVersion();
    }

    public void checkForUpdate() {
        if (!plugin.getConfig().getBoolean("advanced.update_checker", true)) {
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                HttpURLConnection connection = (HttpURLConnection) new URL(
                        "https://api.github.com/repos/PixikDev/PixikLobbyUtil/releases/latest"
                ).openConnection();

                connection.setRequestMethod("GET");
                connection.setRequestProperty("User-Agent", "PixikLobbyUtil");
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);

                int responseCode = connection.getResponseCode();
                if (responseCode != 200) {
                    if (plugin.getConfig().getBoolean("advanced.debug", false)) {
                        plugin.getLogger().warning("Failed to check for updates. Response code: " + responseCode);
                    }
                    return;
                }

                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                String latestVersion = parseVersionFromJson(response.toString());

                if (isNewerVersion(latestVersion, currentVersion)) {
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        String prefix = plugin.getConfig().getString("messages.prefix", "&6&lPixik&e&lLobby &8» &7");
                        String message = "&eДоступно обновление! Текущая версия: &c" + currentVersion +
                                "&e, новая версия: &a" + latestVersion +
                                "&e. Скачайте с: &bhttps://github.com/PixikDev/PixikLobbyUtil/releases";

                        plugin.getLogger().info(ChatColor.translateAlternateColorCodes('&', message));

                        // Уведомление для админов в игре
                        for (org.bukkit.entity.Player player : Bukkit.getOnlinePlayers()) {
                            if (player.hasPermission("pixiklobbyutil.admin")) {
                                player.sendMessage(prefix.replace('&', '§') + message.replace('&', '§'));
                            }
                        }
                    });
                } else {
                    if (plugin.getConfig().getBoolean("advanced.debug", false)) {
                        plugin.getLogger().info("Плагин обновлен до последней версии: " + currentVersion);
                    }
                }

            } catch (Exception e) {
                if (plugin.getConfig().getBoolean("advanced.debug", false)) {
                    plugin.getLogger().warning("Ошибка при проверке обновлений: " + e.getMessage());
                }
            }
        });
    }

    private String parseVersionFromJson(String json) {
        try {
            // Ищем tag_name в JSON ответе GitHub API
            int startIndex = json.indexOf("\"tag_name\":\"") + 12;
            int endIndex = json.indexOf("\"", startIndex);
            if (startIndex >= 12 && endIndex > startIndex) {
                String tag = json.substring(startIndex, endIndex);
                // Убираем "PixikLobbyUtil" из тега если есть
                if (tag.startsWith("PixikLobbyUtil")) {
                    return tag.replace("PixikLobbyUtil", "").replace("-", "").trim();
                }
                return tag.replace("v", "").replace("-", "").trim();
            }
        } catch (Exception e) {
            if (plugin.getConfig().getBoolean("advanced.debug", false)) {
                plugin.getLogger().warning("Ошибка парсинга версии из JSON: " + e.getMessage());
            }
        }
        return currentVersion;
    }

    private boolean isNewerVersion(String latest, String current) {
        if (latest == null || latest.equals(current)) {
            return false;
        }

        try {
            String[] latestParts = latest.split("\\.");
            String[] currentParts = current.split("\\.");

            int length = Math.max(latestParts.length, currentParts.length);
            for (int i = 0; i < length; i++) {
                int latestPart = i < latestParts.length ? Integer.parseInt(latestParts[i].replaceAll("[^0-9]", "")) : 0;
                int currentPart = i < currentParts.length ? Integer.parseInt(currentParts[i].replaceAll("[^0-9]", "")) : 0;

                if (latestPart > currentPart) {
                    return true;
                } else if (latestPart < currentPart) {
                    return false;
                }
            }
        } catch (NumberFormatException e) {
            if (plugin.getConfig().getBoolean("advanced.debug", false)) {
                plugin.getLogger().warning("Ошибка сравнения версий: " + e.getMessage());
            }
        }

        return false;
    }
}