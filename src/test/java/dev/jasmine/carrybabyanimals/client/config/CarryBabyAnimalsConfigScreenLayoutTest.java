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
    void veryCompactScreenKeepsTitleStatusAndFooterRowsOrdered() {
        CarryBabyAnimalsConfigScreenLayout layout = CarryBabyAnimalsConfigScreenLayout.create(480, 240);

        assertTrue(layout.statusY() >= layout.titleY());
        assertTrue(layout.statusY() + layout.statusWidth() <= layout.titleY() + layout.rowHeight());
        assertTrue(layout.left() + layout.titleWidth() < layout.statusX());
        assertTrue(layout.disabledAnimalsInputY() + layout.rowHeight() <= layout.buttonY());
    }

    @Test
    void statusDoesNotPushControlsBelowTitleSpacing() {
        CarryBabyAnimalsConfigScreenLayout layout = CarryBabyAnimalsConfigScreenLayout.create(480, 320);

        assertTrue(layout.reactionsY() <= layout.titleY() + 28);
    }

    @Test
    void labelsDistinguishThirdPersonTuckedPoseFromFirstPersonPlacement() {
        assertEquals("Third-person large baby side tuck", CarryBabyAnimalsConfigScreenLayout.TUCKED_POSE_LABEL);
        assertEquals("First-person large baby placement", CarryBabyAnimalsConfigScreenLayout.FIRST_PERSON_MODE_LABEL);
    }

}
