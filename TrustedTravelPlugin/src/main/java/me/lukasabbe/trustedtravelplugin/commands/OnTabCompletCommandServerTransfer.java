package me.lukasabbe.trustedtravelplugin.commands;

import me.lukasabbe.trustedtravelplugin.ServerObj;
import me.lukasabbe.trustedtravelplugin.TrustedTravelPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.List;

public class OnTabCompletCommandServerTransfer implements TabCompleter {
    @Override
    public List<String> onTabComplete(CommandSender commandSender, Command command, String s, String[] strings) {
        List<String> serverNames = new ArrayList<>();
        List<ServerObj> servers = TrustedTravelPlugin.instance.servers;
        for(ServerObj server : servers){
            serverNames.add(server.name);
        }
        return serverNames;
    }
}
