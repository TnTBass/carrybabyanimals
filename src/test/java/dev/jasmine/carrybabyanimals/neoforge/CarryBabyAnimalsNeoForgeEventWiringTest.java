package dev.jasmine.carrybabyanimals.neoforge;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class CarryBabyAnimalsNeoForgeEventWiringTest {
    @Test
    void entitySpecificInteractEventsReachCarryInteractionHandler() throws IOException {
        String source = Files.readString(repoRoot().resolve(Path.of(
                "src",
                "neoforge",
                "java",
                "dev",
                "jasmine",
                "carrybabyanimals",
                "neoforge",
                "CarryBabyAnimalsNeoForge.java"
        )));

        assertTrue(source.contains("PlayerInteractEvent.EntityInteractSpecific"));
        assertTrue(source.contains("onEntityInteractSpecific"));
        assertTrue(source.contains("HANDLED_ENTITY_INTERACTIONS"));
        assertTrue(source.contains("EntityInteractKey"));
    }

    @Test
    void clientRegistersNeoForgeConfigScreenFactory() throws IOException {
        String source = Files.readString(repoRoot().resolve(Path.of(
                "src",
                "neoforgeClient",
                "java",
                "dev",
                "jasmine",
                "carrybabyanimals",
                "neoforge",
                "client",
                "CarryBabyAnimalsNeoForgeClient.java"
        )));

        assertTrue(source.contains("IConfigScreenFactory"));
        assertTrue(source.contains("registerExtensionPoint"));
        assertTrue(source.contains("CarryBabyAnimalsConfigScreen"));
    }

    @Test
    void neoforgeRegistersServerCarryAttachmentMixin() throws IOException {
        Path root = repoRoot();
        String modMetadata = Files.readString(root.resolve(Path.of(
                "src",
                "neoforge",
                "resources",
                "META-INF",
                "neoforge.mods.toml"
        )));
        Path mixinClass = root.resolve(Path.of(
                "src",
                "neoforge",
                "java",
                "dev",
                "jasmine",
                "carrybabyanimals",
                "neoforge",
                "mixin",
                "EntityStartRidingMixin.java"
        ));
        Path mixinConfig = root.resolve(Path.of(
                "src",
                "neoforge",
                "resources",
                "carrybabyanimals.neoforge.mixins.json"
        ));

        assertTrue(modMetadata.contains("carrybabyanimals.neoforge.mixins.json"));
        assertTrue(Files.exists(mixinClass));
        assertTrue(Files.exists(mixinConfig));
    }

    @Test
    void neoforgeCarryAttachmentMixinAvoidsExclusiveRedirects() throws IOException {
        String source = Files.readString(repoRoot().resolve(Path.of(
                "src",
                "neoforge",
                "java",
                "dev",
                "jasmine",
                "carrybabyanimals",
                "neoforge",
                "mixin",
                "EntityStartRidingMixin.java"
        )));

        assertTrue(source.contains("@Inject"));
        assertTrue(source.contains("CallbackInfoReturnable<Boolean>"));
        assertFalse(source.contains("@Redirect"));
        assertFalse(source.contains("EntityType;canSerialize()Z"));
    }

    private static Path repoRoot() {
        Path current = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        while (current != null) {
            if (Files.exists(current.resolve("settings.gradle"))) {
                return current;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("Could not locate repository root from test working directory");
    }
}
