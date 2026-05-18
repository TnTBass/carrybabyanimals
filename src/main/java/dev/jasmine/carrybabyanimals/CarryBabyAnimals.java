package dev.jasmine.carrybabyanimals;

import dev.jasmine.carrybabyanimals.carry.CarryAiController;
import dev.jasmine.carrybabyanimals.carry.CarryAttachment;
import dev.jasmine.carrybabyanimals.carry.CarryEligibility;
import dev.jasmine.carrybabyanimals.carry.CarryInteractionHandler;
import dev.jasmine.carrybabyanimals.carry.CarryManager;
import dev.jasmine.carrybabyanimals.carry.CarryTicker;
import dev.jasmine.carrybabyanimals.config.AnimalAliasRegistry;
import dev.jasmine.carrybabyanimals.config.CarryConfigManager;
import dev.jasmine.carrybabyanimals.network.CarryNetworking;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityLevelChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.networking.v1.EntityTrackingEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;

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
        loadConfig();
        LOGGER.info("Carry Baby Animals initialized");
        CarryNetworking.registerS2CPayloads();
        CarryTicker ticker = new CarryTicker(CARRY_MANAGER, INTERACTIONS);

        ServerTickEvents.END_SERVER_TICK.register(ticker::tick);
        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClientSide() || !(player instanceof ServerPlayer serverPlayer)) {
                return InteractionResult.PASS;
            }
            return INTERACTIONS.onEntityInteract(serverPlayer, entity);
        });
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClientSide() || !(player instanceof ServerPlayer serverPlayer)) {
                return InteractionResult.PASS;
            }
            return INTERACTIONS.onAttack(serverPlayer);
        });
        AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> {
            if (world.isClientSide() || !(player instanceof ServerPlayer serverPlayer)) {
                return InteractionResult.PASS;
            }
            return INTERACTIONS.onAttack(serverPlayer);
        });
        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) ->
                !(player instanceof ServerPlayer serverPlayer) || !INTERACTIONS.isCarrying(serverPlayer)
        );
        UseItemCallback.EVENT.register((player, world, hand) -> {
            if (world.isClientSide() || !(player instanceof ServerPlayer serverPlayer)) {
                return InteractionResult.PASS;
            }
            return INTERACTIONS.onUseWhileCarrying(serverPlayer);
        });
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (world.isClientSide() || !(player instanceof ServerPlayer serverPlayer)) {
                return InteractionResult.PASS;
            }
            return INTERACTIONS.onUseWhileCarrying(serverPlayer);
        });
        EntityTrackingEvents.START_TRACKING.register((entity, player) ->
                CarryNetworking.replayTrackedCarry(CARRY_MANAGER, entity, player)
        );
        EntityTrackingEvents.STOP_TRACKING.register((entity, player) -> {
            if (CARRY_MANAGER.carrierIdFor(entity.getId()).isPresent()) {
                CarryNetworking.sendClearCarriedToPlayer(player, entity.getId());
            }
        });
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                CarryNetworking.replayVisibleCarries(CARRY_MANAGER, handler.getPlayer())
        );
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
                INTERACTIONS.dropCurrent(handler.getPlayer())
        );
        ServerPlayerEvents.ALLOW_DEATH.register((player, damageSource, damageAmount) -> {
            INTERACTIONS.dropCurrent(player);
            return true;
        });
        ServerEntityLevelChangeEvents.AFTER_PLAYER_CHANGE_LEVEL.register((player, origin, destination) ->
                INTERACTIONS.dropCurrentInLevel(player, origin, false)
        );
        ServerLifecycleEvents.SERVER_STOPPING.register(server ->
                server.getPlayerList().getPlayers().forEach(player ->
                        // Shutdown cleanup still uses normal safe placement, but avoids late chunk forcing.
                        INTERACTIONS.dropCurrent(player, false)
                )
        );
    }

    private static void loadConfig() {
        Path configPath = FabricLoader.getInstance()
                .getConfigDir()
                .resolve(MOD_ID + ".json");
        try {
            CONFIG.load(configPath);
            CONFIG.logUnknownAnimalNames(AnimalAliasRegistry.createDefault(), LOGGER);
        } catch (IOException exception) {
            LOGGER.error("Failed to load Carry Baby Animals config from {}", configPath, exception);
        }
    }
}
