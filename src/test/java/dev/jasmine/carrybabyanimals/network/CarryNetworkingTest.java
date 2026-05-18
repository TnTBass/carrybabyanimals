package dev.jasmine.carrybabyanimals.network;

import dev.jasmine.carrybabyanimals.CarryBabyAnimals;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class CarryNetworkingTest {
    @Test
    void payloadIdsUseCarryBabyAnimalsNamespace() {
        assertEquals(
                Identifier.fromNamespaceAndPath(CarryBabyAnimals.MOD_ID, "set_carried"),
                CarryNetworking.SetCarriedPayload.TYPE.id()
        );
        assertEquals(
                Identifier.fromNamespaceAndPath(CarryBabyAnimals.MOD_ID, "clear_carried"),
                CarryNetworking.ClearCarriedPayload.TYPE.id()
        );
    }
}
