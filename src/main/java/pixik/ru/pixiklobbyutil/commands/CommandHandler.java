package pixik.ru.pixiklobbyutil.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import pixik.ru.pixiklobbyutil.PixikLobbyUtil;

public class CommandHandler implements CommandExecutor {

    private final PixikLobbyUtil plugin;

    public CommandHandler(PixikLobbyUtil plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("pixiklobbyutil.admin")) {
                sendNoPermission(sender);
                return true;
            }

            plugin.reloadPluginConfig();
            sender.sendMessage(getMessage("plugin_reloaded"));
            return true;
        }

        sendHelp(sender);
        return true;
    }

    private void sendHelp(CommandSender sender) {
        String prefix = plugin.getPluginConfig().getString("messages.prefix", "&6&lPixik&e&lLobby &8» &7").replace('&', '§');

        sender.sendMessage(prefix + "§6PixikLobbyUtil Commands:");
        sender.sendMessage("§e/pixiklobbyutil reload §7- Перезагрузить конфиг");
    }

    private void sendNoPermission(CommandSender sender) {
        sender.sendMessage(getMessage("no_permission"));
    }

    private String getMessage(String key) {
        String prefix = plugin.getPluginConfig().getString("messages.prefix", "&6&lPixik&e&lLobby &8» &7");
        String message = plugin.getPluginConfig().getString("messages." + key, "");
        return prefix.replace('&', '§') + message.replace('&', '§');
    }
}