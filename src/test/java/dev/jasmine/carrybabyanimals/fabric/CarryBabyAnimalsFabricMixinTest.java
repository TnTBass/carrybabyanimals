package dev.jasmine.carrybabyanimals.fabric;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class CarryBabyAnimalsFabricMixinTest {
    @Test
    void fabricCarryAttachmentMixinAvoidsExclusiveRedirects() throws IOException {
        String source = Files.readString(repoRoot().resolve(Path.of(
                "src",
                "fabric",
                "java",
                "dev",
                "jasmine",
                "carrybabyanimals",
                "fabric",
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
