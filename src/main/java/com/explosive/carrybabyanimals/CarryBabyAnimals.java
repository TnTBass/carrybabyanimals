package com.explosive.carrybabyanimals;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class CarryBabyAnimals implements ModInitializer {
    public static final String MOD_ID = "carrybabyanimals";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Carry Baby Animals initialized");
    }
}
