package dev.jasmine.carrybabyanimals.neoforge.client;

import dev.jasmine.carrybabyanimals.CarryBabyAnimals;
import dev.jasmine.carrybabyanimals.client.config.CarryBabyAnimalsConfigScreen;
import dev.jasmine.carrybabyanimals.client.config.ClientCarryVisualConfigManager;
import dev.jasmine.carrybabyanimals.client.modstatus.ClientModStatusTracker;
import dev.jasmine.carrybabyanimals.neoforge.client.network.NeoForgeClientCarryNetworking;
import dev.jasmine.carrybabyanimals.neoforge.client.render.NeoForgeCarriedBabyRenderState;
import dev.jasmine.carrybabyanimals.neoforge.client.render.NeoForgeCarriedBabyRenderer;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
import net.neoforged.neoforge.client.renderstate.RegisterRenderStateModifiersEvent;

import java.io.IOException;
import java.nio.file.Path;

public final class CarryBabyAnimalsNeoForgeClient {
    private CarryBabyAnimalsNeoForgeClient() {
    }

    public static void registerModBus(IEventBus modBus, ModContainer modContainer) {
        registerConfigScreen(modContainer);
        modBus.addListener(CarryBabyAnimalsNeoForgeClient::registerClientPayloadHandlers);
        modBus.addListener(NeoForgeCarriedBabyRenderer::registerRenderStateModifiers);
    }

    private static void registerConfigScreen(ModContainer modContainer) {
        modContainer.registerExtensionPoint(
                IConfigScreenFactory.class,
                (container, parent) -> new CarryBabyAnimalsConfigScreen(parent)
        );
    }

    private static void registerClientPayloadHandlers(RegisterClientPayloadHandlersEvent event) {
        initializeClientConfig();
        NeoForgeClientCarryNetworking.registerClientPayloadHandlers(event);
    }

    @EventBusSubscriber(modid = CarryBabyAnimals.MOD_ID, value = Dist.CLIENT)
    public static final class GameBusEvents {
        private GameBusEvents() {
        }

        @SubscribeEvent
        public static void onClientJoined(ClientPlayerNetworkEvent.LoggingIn event) {
            ClientModStatusTracker.onJoin();
        }

        @SubscribeEvent
        public static void onClientDisconnect(ClientPlayerNetworkEvent.LoggingOut event) {
            NeoForgeCarriedBabyRenderState.clearAll();
            ClientModStatusTracker.onDisconnect();
        }

        @SubscribeEvent
        public static void onClientTick(ClientTickEvent.Post event) {
            ClientModStatusTracker.tick();
        }

        @SubscribeEvent
        public static void onAttackInput(InputEvent.InteractionKeyMappingTriggered event) {
            Minecraft client = Minecraft.getInstance();
            if (!event.isAttack() || client.player == null) {
                return;
            }
            if (ClientCarryInteractionHandler.onPreAttack(client, client.player)) {
                event.setCanceled(true);
                event.setSwingHand(false);
            }
        }

        @SubscribeEvent
        public static void onRenderLivingPre(RenderLivingEvent.Pre<?, ?, ?> event) {
            NeoForgeCarriedBabyRenderer.suppressVanillaCarriedBabyRender(event);
        }

        @SubscribeEvent
        public static void onSubmitCustomGeometry(SubmitCustomGeometryEvent event) {
            NeoForgeCarriedBabyRenderer.submitCarriedBabies(event);
        }
    }

    private static void initializeClientConfig() {
        try {
            ClientCarryVisualConfigManager.useConfigPath(configPath());
            ClientCarryVisualConfigManager.load();
        } catch (IOException exception) {
            CarryBabyAnimals.LOGGER.warn("Could not load Carry Baby Animals client visual config; using defaults.", exception);
        }
    }

    private static Path configPath() {
        return FMLPaths.CONFIGDIR.get().resolve("carrybabyanimals-client.json");
    }
}
