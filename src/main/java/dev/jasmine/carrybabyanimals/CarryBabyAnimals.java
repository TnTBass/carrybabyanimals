package dev.jasmine.carrybabyanimals;

import dev.jasmine.carrybabyanimals.carry.CarryAiController;
import dev.jasmine.carrybabyanimals.carry.CarryAttachment;
import dev.jasmine.carrybabyanimals.carry.CarryEligibility;
import dev.jasmine.carrybabyanimals.carry.CarryInteractionHandler;
import dev.jasmine.carrybabyanimals.carry.CarryManager;
import dev.jasmine.carrybabyanimals.config.AnimalAliasRegistry;
import dev.jasmine.carrybabyanimals.config.CarryConfigManager;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class CarryBabyAnimals implements ModInitializer {
    public static final String MOD_ID = "carrybabyanimals";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static final CarryConfigManager CONFIG = new CarryConfigManager();
    public static final CarryManager CARRY_MANAGER = new CarryManager();
    public static final CarryAiController AI_CONTROLLER = new CarryAiController();
    public static final CarryInteractionHandler INTERACTIONS = new CarryInteractionHandler(
            CARRY_MANAGER,
            new CarryEligibility(AnimalAliasRegistry.createDefault()),
            CONFIG,
            new CarryAttachment(),
            AI_CONTROLLER
    );

    @Override
    public void onInitialize() {
        LOGGER.info("Carry Baby Animals initialized");
    }
}
