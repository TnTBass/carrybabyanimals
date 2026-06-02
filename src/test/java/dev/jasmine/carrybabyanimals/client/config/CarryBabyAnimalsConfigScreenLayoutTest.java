package dev.jasmine.carrybabyanimals.client.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class CarryBabyAnimalsConfigScreenLayoutTest {
    @Test
    void compactScreenKeepsDisabledAnimalsInputAboveFooterButtons() {
        CarryBabyAnimalsConfigScreenLayout layout = CarryBabyAnimalsConfigScreenLayout.create(480, 320);

        assertTrue(layout.contentFitsAboveButtons());
        assertTrue(layout.disabledAnimalsInputY() + layout.rowHeight() <= layout.buttonY());
    }

    @Test
    void labelsDistinguishThirdPersonTuckedPoseFromFirstPersonPlacement() {
        assertEquals("Third-person large baby side tuck", CarryBabyAnimalsConfigScreenLayout.TUCKED_POSE_LABEL);
        assertEquals("First-person large baby placement", CarryBabyAnimalsConfigScreenLayout.FIRST_PERSON_MODE_LABEL);
    }
}
