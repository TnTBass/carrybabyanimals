package dev.jasmine.carrybabyanimals.carry;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class CarryAiControllerTest {
    @Test
    void restoreStateReturnsPriorNoAiValue() {
        CarryAiController controller = new CarryAiController();
        UUID mobId = UUID.randomUUID();

        assertTrue(controller.rememberSuppressedState(mobId, true));

        assertEquals(true, controller.restoreSuppressedState(mobId).orElseThrow());
    }

    @Test
    void duplicateSuppressionKeepsOriginalNoAiValue() {
        CarryAiController controller = new CarryAiController();
        UUID mobId = UUID.randomUUID();

        assertTrue(controller.rememberSuppressedState(mobId, true));
        assertFalse(controller.rememberSuppressedState(mobId, false));

        assertEquals(true, controller.restoreSuppressedState(mobId).orElseThrow());
    }
}
