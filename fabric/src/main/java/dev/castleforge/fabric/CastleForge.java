package dev.castleforge.fabric;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class CastleForge implements ModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("castleforge");
    @Override public void onInitialize() {
        LOGGER.info("CastleForge development foundation loaded. Automatic world generation is disabled.");
    }
}
