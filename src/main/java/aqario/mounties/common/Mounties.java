package aqario.mounties.common;

import aqario.mounties.common.config.MountiesConfig;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Mounties implements ModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("Mounties");
    public static final String ID = "mounties";

    @Override
    public void onInitialize() {
        LOGGER.info("Loading Mounties");
        MountiesConfig.init(ID, MountiesConfig.class);
    }
}
