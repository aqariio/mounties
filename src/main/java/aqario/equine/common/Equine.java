package aqario.equine.common;

import aqario.equine.common.config.EquineConfig;
import aqario.equine.common.network.ServerboundHorseRearUpPayload;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Equine implements ModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("Equine");
    public static final String ID = "equine";

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(ID, path);
    }

    @Override
    public void onInitialize() {
        LOGGER.info("Loading Equine");
        EquineConfig.init(ID, EquineConfig.class);
        PayloadTypeRegistry.serverboundPlay().register(
            ServerboundHorseRearUpPayload.TYPE,
            ServerboundHorseRearUpPayload.STREAM_CODEC
        );
        ServerPlayNetworking.registerGlobalReceiver(
            ServerboundHorseRearUpPayload.TYPE,
            (_, context) -> {
                ServerPlayer player = context.player();
                if(player.getControlledVehicle() instanceof AbstractHorse horse) {
                    horse.makeMad();
                }
            }
        );
    }
}
