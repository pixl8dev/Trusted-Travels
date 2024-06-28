package me.lukasabbe.trustedtravelplugin;

import me.lukasabbe.trustedtravelplugin.commands.OnTabCompletCommandServerTransfer;
import me.lukasabbe.trustedtravelplugin.commands.OnTabCompletReloadCommand;
import me.lukasabbe.trustedtravelplugin.commands.ReloadCommand;
import me.lukasabbe.trustedtravelplugin.commands.ServerTransferCommand;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public final class TrustedTravelPlugin extends JavaPlugin {
    public List<ServerObj> servers = new ArrayList<>();
    public static TrustedTravelPlugin instance;
    @Override
    public void onEnable() {
        saveDefaultConfig();
        instance = this;
        setServers();
        this.getCommand("server").setExecutor(new ServerTransferCommand());
        this.getCommand("server").setTabCompleter(new OnTabCompletCommandServerTransfer());
        this.getCommand("tt").setExecutor(new ReloadCommand());
        this.getCommand("tt").setTabCompleter(new OnTabCompletReloadCommand());
    }

    public void setServers() {
        servers.clear();
        ConfigurationSection section = getConfig().getConfigurationSection("servers");
        assert section != null;
        for(String key : section.getKeys(false)){
            servers.add(new ServerObj(
                    key,
                    getConfig().getString("servers."+key+".address"),
                    getConfig().getInt("servers."+key+".port")
            ));
        }
    }

}
