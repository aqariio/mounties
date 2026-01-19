package aqario.mounties.common;

import aqario.mounties.common.config.MountiesConfig;
import aqario.mounties.common.network.ServerboundHorseRearUpPayload;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Mounties implements ModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("Mounties");
    public static final String ID = "mounties";

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(ID, path);
    }

    @Override
    public void onInitialize() {
        LOGGER.info("Loading Mounties");
        MountiesConfig.init(ID, MountiesConfig.class);
        PayloadTypeRegistry.playC2S().register(
            ServerboundHorseRearUpPayload.TYPE,
            ServerboundHorseRearUpPayload.STREAM_CODEC
        );
        ServerPlayNetworking.registerGlobalReceiver(
            ServerboundHorseRearUpPayload.TYPE,
            (payload, context) -> {
                ServerPlayer player = context.player();
                if(player.getControlledVehicle() instanceof AbstractHorse horse) {
                    horse.makeMad();
                }
            }
        );
    }
}
