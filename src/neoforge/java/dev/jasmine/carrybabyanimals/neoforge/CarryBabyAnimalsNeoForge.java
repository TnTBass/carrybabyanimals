package dev.jasmine.carrybabyanimals.neoforge;

import dev.jasmine.carrybabyanimals.CarryBabyAnimals;
import dev.jasmine.carrybabyanimals.carry.CarryAiController;
import dev.jasmine.carrybabyanimals.carry.CarryAttachment;
import dev.jasmine.carrybabyanimals.carry.CarryEligibility;
import dev.jasmine.carrybabyanimals.carry.CarryInteractionHandler;
import dev.jasmine.carrybabyanimals.carry.CarryManager;
import dev.jasmine.carrybabyanimals.carry.CarryTicker;
import dev.jasmine.carrybabyanimals.config.AnimalAliasRegistry;
import dev.jasmine.carrybabyanimals.config.CarryConfigManager;
import dev.jasmine.carrybabyanimals.cozy.CozyFeedbackScheduler;
import dev.jasmine.carrybabyanimals.modstatus.CarryBabyAnimalsModStatus;
import dev.jasmine.carrybabyanimals.neoforge.client.CarryBabyAnimalsNeoForgeClient;
import dev.jasmine.carrybabyanimals.neoforge.network.NeoForgeCarryNetworking;
import dev.jasmine.carrybabyanimals.neoforge.permissions.NeoForgeCarryPermissions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.io.IOException;
import java.nio.file.Path;

@Mod(CarryBabyAnimals.MOD_ID)
public final class CarryBabyAnimalsNeoForge {
    public static final CarryConfigManager CONFIG = new CarryConfigManager();
    public static final CarryManager CARRY_MANAGER = new CarryManager();
    public static final CarryAiController AI_CONTROLLER = new CarryAiController();
    public static final CozyFeedbackScheduler COZY_FEEDBACK_SCHEDULER = new CozyFeedbackScheduler();
    public static final CarryInteractionHandler INTERACTIONS = new CarryInteractionHandler(
            CARRY_MANAGER,
            new CarryEligibility(AnimalAliasRegistry.createDefault()),
            CONFIG,
            new CarryAttachment(),
            AI_CONTROLLER,
            COZY_FEEDBACK_SCHEDULER,
            NeoForgeCarryNetworking.SENDER
    );

    public CarryBabyAnimalsNeoForge(IEventBus modBus, ModContainer modContainer) {
        NeoForgeCarryPermissions.install();
        CarryBabyAnimalsModStatus.useCurrentVersion(currentVersion());
        loadConfig();
        modBus.addListener(CarryBabyAnimalsNeoForge::registerPayloads);
        if (FMLEnvironment.getDist() == Dist.CLIENT) {
            CarryBabyAnimalsNeoForgeClient.registerModBus(modBus, modContainer);
        }
        registerEvents();
        CarryBabyAnimals.LOGGER.info("Carry Baby Animals initialized");
    }

    private static void registerPayloads(net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent event) {
        NeoForgeCarryNetworking.registerPayloads(event, INTERACTIONS);
    }

    private static void registerEvents() {
        CarryTicker ticker = new CarryTicker(CARRY_MANAGER, INTERACTIONS, CONFIG, COZY_FEEDBACK_SCHEDULER);

        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post event) -> ticker.tick(event.getServer()));
        NeoForge.EVENT_BUS.addListener(CarryBabyAnimalsNeoForge::onEntityInteractSpecific);
        NeoForge.EVENT_BUS.addListener(CarryBabyAnimalsNeoForge::onEntityInteract);
        NeoForge.EVENT_BUS.addListener(CarryBabyAnimalsNeoForge::onAttackEntity);
        NeoForge.EVENT_BUS.addListener(CarryBabyAnimalsNeoForge::onLeftClickBlock);
        NeoForge.EVENT_BUS.addListener(CarryBabyAnimalsNeoForge::onBreakBlock);
        NeoForge.EVENT_BUS.addListener(CarryBabyAnimalsNeoForge::onRightClickItem);
        NeoForge.EVENT_BUS.addListener(CarryBabyAnimalsNeoForge::onRightClickBlock);
        NeoForge.EVENT_BUS.addListener(CarryBabyAnimalsNeoForge::onStartTracking);
        NeoForge.EVENT_BUS.addListener(CarryBabyAnimalsNeoForge::onStopTracking);
        NeoForge.EVENT_BUS.addListener(CarryBabyAnimalsNeoForge::onPlayerLoggedIn);
        NeoForge.EVENT_BUS.addListener(CarryBabyAnimalsNeoForge::onPlayerLoggedOut);
        NeoForge.EVENT_BUS.addListener(CarryBabyAnimalsNeoForge::onPlayerDeath);
        NeoForge.EVENT_BUS.addListener(CarryBabyAnimalsNeoForge::onPlayerChangedDimension);
        NeoForge.EVENT_BUS.addListener(CarryBabyAnimalsNeoForge::onServerStopping);
    }

    private static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        applyInteractionResult(event, INTERACTIONS.onEntityInteract(player, event.getTarget(), event.getHand()));
    }

    private static void onEntityInteractSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        applyInteractionResult(event, INTERACTIONS.onEntityInteract(player, event.getTarget(), event.getHand()));
    }

    private static void onAttackEntity(AttackEntityEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        applyCancelableResult(event, INTERACTIONS.onAttack(player));
    }

    private static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (event.getAction() != PlayerInteractEvent.LeftClickBlock.Action.START
                || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        applyCancelableResult(event, INTERACTIONS.onAttack(player));
    }

    private static void onBreakBlock(BreakBlockEvent event) {
        if (event.getPlayer() instanceof ServerPlayer player && INTERACTIONS.isCarrying(player)) {
            event.setCanceled(true);
            event.setNotifyClient(true);
        }
    }

    private static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        applyInteractionResult(event, INTERACTIONS.onUseWhileCarrying(player));
    }

    private static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        applyInteractionResult(
                event,
                INTERACTIONS.onUseBlockWhileCarrying(player, event.getLevel().getBlockState(event.getPos()))
        );
    }

    private static void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            NeoForgeCarryNetworking.replayTrackedCarry(CARRY_MANAGER, event.getTarget(), player);
        }
    }

    private static void onStopTracking(PlayerEvent.StopTracking event) {
        if (event.getEntity() instanceof ServerPlayer player
                && CARRY_MANAGER.carrierIdFor(event.getTarget().getId()).isPresent()) {
            NeoForgeCarryNetworking.sendClearCarriedToPlayer(player, event.getTarget().getId());
        }
    }

    private static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            NeoForgeCarryNetworking.sendServerVersionIfSupported(player);
            NeoForgeCarryNetworking.replayVisibleCarries(CARRY_MANAGER, player);
        }
    }

    private static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            INTERACTIONS.dropCurrent(player);
        }
    }

    private static void onPlayerDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            INTERACTIONS.dropCurrent(player);
        }
    }

    private static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        ServerLevel origin = player.level().getServer().getLevel(event.getFrom());
        if (origin == null) {
            INTERACTIONS.dropCurrent(player, false);
            return;
        }
        INTERACTIONS.dropCurrentInLevel(player, origin, false);
    }

    private static void onServerStopping(ServerStoppingEvent event) {
        event.getServer().getPlayerList().getPlayers().forEach(player ->
                // Shutdown cleanup still uses normal safe placement, but avoids late chunk forcing.
                INTERACTIONS.dropCurrent(player, false)
        );
    }

    private static void applyInteractionResult(PlayerInteractEvent.EntityInteract event, InteractionResult result) {
        if (result != InteractionResult.PASS) {
            event.setCancellationResult(result);
            event.setCanceled(true);
        }
    }

    private static void applyInteractionResult(PlayerInteractEvent.EntityInteractSpecific event, InteractionResult result) {
        if (result != InteractionResult.PASS) {
            event.setCancellationResult(result);
            event.setCanceled(true);
        }
    }

    private static void applyInteractionResult(PlayerInteractEvent.RightClickItem event, InteractionResult result) {
        if (result != InteractionResult.PASS) {
            event.setCancellationResult(result);
            event.setCanceled(true);
        }
    }

    private static void applyInteractionResult(PlayerInteractEvent.RightClickBlock event, InteractionResult result) {
        if (result != InteractionResult.PASS) {
            event.setCancellationResult(result);
            event.setCanceled(true);
        }
    }

    private static void applyCancelableResult(net.neoforged.bus.api.ICancellableEvent event, InteractionResult result) {
        if (result != InteractionResult.PASS) {
            event.setCanceled(true);
        }
    }

    private static void loadConfig() {
        Path configPath = NeoForgePaths.configPath(FMLPaths.CONFIGDIR.get());
        try {
            CONFIG.load(configPath);
            CONFIG.filterAndLogUnknownAnimalNames(AnimalAliasRegistry.createDefault(), CarryBabyAnimals.LOGGER);
        } catch (IOException exception) {
            CarryBabyAnimals.LOGGER.error("Failed to load Carry Baby Animals config from {}", configPath, exception);
        }
    }

    private static String currentVersion() {
        return ModList.get()
                .getModContainerById(CarryBabyAnimals.MOD_ID)
                .map(container -> container.getModInfo().getVersion().toString())
                .orElse("unknown");
    }
}
