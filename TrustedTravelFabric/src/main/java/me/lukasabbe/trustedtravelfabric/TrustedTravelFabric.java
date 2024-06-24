package me.lukasabbe.trustedtravelfabric;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import me.lukasabbe.trustedtravelfabric.config.Config;
import me.lukasabbe.trustedtravelfabric.config.ServerObj;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.network.packet.s2c.common.ServerTransferS2CPacket;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.io.FileNotFoundException;
import java.util.List;

public class TrustedTravelFabric implements DedicatedServerModInitializer {
    Config serverConfig;
    @Override
    public void onInitializeServer() {
        serverConfig = new Config();
        CommandRegistrationCallback.EVENT.register(
                ((dispatcher, registryAccess, environment) -> {
                    dispatcher.register(createServerCommands(serverConfig.servers));
                    dispatcher.register(createReloadCommand());
                }));

    }

    public LiteralArgumentBuilder<ServerCommandSource> createServerCommands(List<ServerObj> servers){
        LiteralArgumentBuilder<ServerCommandSource> manager = CommandManager.literal("server");
        servers.forEach(server -> manager.then(CommandManager.literal(server.name).executes(context -> {
            ServerTransferS2CPacket transferPacket = new ServerTransferS2CPacket(server.address, server.port);
            if(!context.getSource().isExecutedByPlayer()) return 0;
            ServerPlayerEntity player = context.getSource().getPlayer();
            player.networkHandler.sendPacket(transferPacket);
            return 1;
        })));

        return manager;
    }

    public LiteralArgumentBuilder<ServerCommandSource> createReloadCommand(){
        return CommandManager
                .literal("ttf")
                .requires(serverCommandSource -> serverCommandSource.hasPermissionLevel(1))
                .then(
                        CommandManager
                                .literal("reload")
                                .executes(context -> {
                                    serverConfig.reloadConfig();
                                    context.getSource().sendFeedback(()-> Text.literal("Reloaded config"),false);
                                    return 1;
                                }));
    }

}
