package aqario.mounties.common;

import org.quiltmc.loader.api.ModContainer;
import org.quiltmc.qsl.base.api.entrypoint.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Mounties implements ModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("Mounties");
    public static final String ID = "mounties";

    @Override
    public void onInitialize(ModContainer mod) {
        LOGGER.info("Loading {}", mod.metadata().name());
    }
}
