package dev.jasmine.carrybabyanimals.client.config;

import dev.jasmine.carrybabyanimals.client.render.FirstPersonLargeBabyVisibilityMode;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class ClientCarryVisualConfigEditStateTest {
    @Test
    void createsConfigFromEditedValuesAndNormalizesDisabledAnimalText() {
        ClientCarryVisualConfigEditState state = ClientCarryVisualConfigEditState.from(ClientCarryVisualConfig.defaultConfig());

        state.setCarriedBabyReactionsEnabled(false);
        state.setLargeBabyTuckedPoseEnabled(false);
        state.setFirstPersonLargeBabyVisibilityMode(FirstPersonLargeBabyVisibilityMode.HIDE_WHEN_OBSTRUCTING);
        state.setSleepyCarryVisualsEnabled(false);
        state.setAnimalReactionIntensityText("1.6");
        state.setDisabledCarriedReactionAnimalsText(" Minecraft:Panda, examplemod:Duck\nminecraft:rabbit ");

        ClientCarryVisualConfig config = state.toConfig();

        assertEquals(false, config.carriedBabyReactionsEnabled());
        assertEquals(false, config.largeBabyTuckedPoseEnabled());
        assertEquals(FirstPersonLargeBabyVisibilityMode.HIDE_WHEN_OBSTRUCTING, config.firstPersonLargeBabyVisibilityMode());
        assertEquals(false, config.sleepyCarryVisualsEnabled());
        assertEquals(1.0D, config.animalReactionIntensity(), 1.0E-6D);
        assertEquals(List.of("minecraft:panda", "examplemod:duck", "minecraft:rabbit"), config.disabledCarriedReactionAnimals());
    }

    @Test
    void invalidIntensityTextKeepsOriginalIntensity() {
        ClientCarryVisualConfigEditState state = ClientCarryVisualConfigEditState.from(new ClientCarryVisualConfig(
                true,
                true,
                FirstPersonLargeBabyVisibilityMode.TUCKED,
                true,
                0.35D,
                List.of()
        ));

        state.setAnimalReactionIntensityText("not a number");

        assertEquals(0.35D, state.toConfig().animalReactionIntensity(), 1.0E-6D);
    }
}
