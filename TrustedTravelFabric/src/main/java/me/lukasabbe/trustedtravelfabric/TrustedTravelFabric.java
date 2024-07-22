package me.lukasabbe.trustedtravelfabric;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import me.lukasabbe.trustedtravelfabric.config.Config;
import me.lukasabbe.trustedtravelfabric.config.ServerObj;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.network.packet.s2c.common.ServerTransferS2CPacket;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.List;
import java.util.Optional;

public class TrustedTravelFabric implements DedicatedServerModInitializer {
    public static Config serverConfig;
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
        manager.then(
                CommandManager
                        .argument("servers", StringArgumentType.word())
                        .suggests(new ServerSuggestionProvider())
                        .executes(ctx -> serverCmd(servers, ctx)));
        return manager;
    }

    private int serverCmd(List<ServerObj> servers, CommandContext<ServerCommandSource> ctx) {
        String server = StringArgumentType.getString(ctx,"servers");
        Optional<ServerObj> OptionalServerObj = servers.stream().filter(args -> args.name.equals(server)).findFirst();
        if(OptionalServerObj.isEmpty()){
            ctx.getSource().sendError(Text.literal("There is no server with that name"));
            return 0;
        }
        ServerObj serverObj = OptionalServerObj.get();
        ServerTransferS2CPacket transferPacket = new ServerTransferS2CPacket(serverObj.address, serverObj.port);
        if(!ctx.getSource().isExecutedByPlayer()) {
            ctx.getSource().sendError(Text.literal("Only players can execute this command"));
            return 0;
        }
        ServerPlayerEntity player = ctx.getSource().getPlayer();
        player.networkHandler.sendPacket(transferPacket);
        return 1;
    }

    public LiteralArgumentBuilder<ServerCommandSource> createReloadCommand(){
        return CommandManager
                .literal("tt")
                .requires(serverCommandSource -> serverCommandSource.hasPermissionLevel(1))
                .then(CommandManager
                        .literal("reload")
                        .executes(context -> {
                            serverConfig.reloadConfig();
                            context.getSource().sendFeedback(()-> Text.literal("Reloaded config"),false);
                            return 1;
                        }));
    }

}
