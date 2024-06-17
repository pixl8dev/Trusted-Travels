package me.lukasabbe.trustedtravelplugin;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public final class TrustedTravelPlugin extends JavaPlugin {
    List<ServerObj> servers = new ArrayList<>();
    public static TrustedTravelPlugin instance;
    @Override
    public void onEnable() {
        saveDefaultConfig();
        instance = this;
        ConfigurationSection section = getConfig().getConfigurationSection("servers");
        assert section != null;
        for(String key : section.getKeys(false)){
            servers.add(new ServerObj(
                    key,
                    getConfig().getString("servers."+key+".address"),
                    getConfig().getInt("servers."+key+".port")
            ));
        }
        this.getCommand("server").setExecutor(new ServerTransferCommand());
        this.getCommand("server").setTabCompleter(new OnTabCompletCommandServerTransfer());
    }

}
