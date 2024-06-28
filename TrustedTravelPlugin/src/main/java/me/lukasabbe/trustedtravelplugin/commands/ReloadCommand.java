package me.lukasabbe.trustedtravelplugin.commands;

import me.lukasabbe.trustedtravelplugin.TrustedTravelPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class ReloadCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] args) {
        if(!(args[0].equals("reload"))) return false;
        TrustedTravelPlugin.instance.reloadConfig();
        TrustedTravelPlugin.instance.setServers();
        commandSender.sendMessage("Reloaded config");
        return true;
    }
}
