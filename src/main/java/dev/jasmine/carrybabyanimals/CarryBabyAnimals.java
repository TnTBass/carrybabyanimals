package dev.jasmine.carrybabyanimals;

import dev.jasmine.carrybabyanimals.carry.CarryAiController;
import dev.jasmine.carrybabyanimals.carry.CarryAttachment;
import dev.jasmine.carrybabyanimals.carry.CarryEligibility;
import dev.jasmine.carrybabyanimals.carry.CarryInteractionHandler;
import dev.jasmine.carrybabyanimals.carry.CarryManager;
import dev.jasmine.carrybabyanimals.carry.CarryTicker;
import dev.jasmine.carrybabyanimals.config.AnimalAliasRegistry;
import dev.jasmine.carrybabyanimals.config.CarryConfigManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
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
        CarryTicker ticker = new CarryTicker(CARRY_MANAGER, INTERACTIONS);

        ServerTickEvents.END_SERVER_TICK.register(ticker::tick);
        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClientSide() || !(player instanceof ServerPlayer serverPlayer)) {
                return InteractionResult.PASS;
            }
            return INTERACTIONS.onEntityInteract(serverPlayer, entity);
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
                INTERACTIONS.dropCurrent(handler.getPlayer())
        );
        ServerLifecycleEvents.SERVER_STOPPING.register(server ->
                server.getPlayerList().getPlayers().forEach(player ->
                        // Shutdown cleanup still uses normal safe placement, but avoids late chunk forcing.
                        INTERACTIONS.dropCurrent(player, false)
                )
        );
    }
}
