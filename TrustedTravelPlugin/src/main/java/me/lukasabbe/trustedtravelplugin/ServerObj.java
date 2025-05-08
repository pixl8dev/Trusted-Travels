package me.lukasabbe.trustedtravelplugin;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.concurrent.CompletableFuture;

public class ServerObj {
    public String name;
    public String address;
    public int port;

    public ServerObj(String name, String adress, int port) {
        this.name = name;
        this.address = adress;
        this.port = port;
    }

    public CompletableFuture<Boolean> pingServer() {
        return MinecraftServerPinger.pingServer(address, port);
    }

    public void transferPlayer(Player player) {
        pingServer().thenAccept(isOnline -> {
            if (isOnline) {
                player.transfer(address, port);
            } else {
                Bukkit.getScheduler().runTask(TrustedTravelPlugin.instance, () -> 
                    player.sendMessage("§cThe server §6" + name + "§c is currently offline."));
            }
        });
    }
}
