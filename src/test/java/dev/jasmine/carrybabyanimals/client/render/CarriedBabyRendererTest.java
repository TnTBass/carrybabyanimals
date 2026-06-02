package dev.jasmine.carrybabyanimals.client.render;

import dev.jasmine.carrybabyanimals.client.config.ClientCarryVisualConfig;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class CarriedBabyRendererTest {
    @Test
    void firstPersonLargeBabyModeStillUsesLargeBucketWhenThirdPersonTuckIsDisabled() {
        ClientCarryVisualConfig visualConfig = new ClientCarryVisualConfig(
                true,
                false,
                FirstPersonLargeBabyVisibilityMode.TUCKED,
                true,
                0.75D,
                List.of()
        );

        assertEquals(
                CarriedBabySizeBucket.TALL,
                CarriedBabyRenderer.effectiveSizeBucket(CarriedBabySizeBucket.TALL, true, visualConfig)
        );
        assertEquals(
                CarriedBabySizeBucket.MEDIUM,
                CarriedBabyRenderer.effectiveSizeBucket(CarriedBabySizeBucket.TALL, false, visualConfig)
        );
    }
}
