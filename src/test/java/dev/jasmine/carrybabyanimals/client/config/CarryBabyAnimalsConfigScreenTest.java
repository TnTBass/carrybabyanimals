package dev.jasmine.carrybabyanimals.client.config;

import dev.jasmine.carrybabyanimals.internal.modstatus.ModStatusDisplay;
import dev.jasmine.carrybabyanimals.internal.modstatus.StatusTone;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class CarryBabyAnimalsConfigScreenTest {
    @Test
    void statusIndicatorUsesReferenceSquareStyling() {
        CarryBabyAnimalsConfigScreenLayout layout = CarryBabyAnimalsConfigScreenLayout.create(480, 320);

        assertEquals(8, CarryBabyAnimalsConfigScreen.STATUS_SQUARE_SIZE);
        assertEquals(CarryBabyAnimalsConfigScreen.STATUS_SQUARE_SIZE, layout.statusWidth());
        assertEquals(0xFF222222, CarryBabyAnimalsConfigScreen.STATUS_SQUARE_BORDER_COLOR);
    }

    @Test
    void statusTooltipIncludesBuildMetadataWhenPresent() {
        ModStatusDisplay display = new ModStatusDisplay(
                "Carry Baby Animals",
                "0.1.3",
                "client123",
                "0.1.3",
                "server456",
                "Matched",
                "Client and server versions match.",
                StatusTone.GREEN,
                "https://modrinth.com/mod/carrybabyanimals"
        );

        assertEquals(
                List.of(
                        "Carry Baby Animals",
                        "Status: Matched",
                        "Client: 0.1.3+client123",
                        "Server: 0.1.3+server456",
                        "Client and server versions match."
                ),
                CarryBabyAnimalsConfigScreen.tooltipText(display)
        );
    }

    @Test
    void statusTooltipHidesLocalDevBuildFallback() {
        ModStatusDisplay display = new ModStatusDisplay(
                "Carry Baby Animals",
                "0.1.3",
                "dev",
                "0.1.3",
                "dev",
                "Matched",
                "Client and server versions match.",
                StatusTone.GREEN,
                "https://modrinth.com/mod/carrybabyanimals"
        );

        assertEquals(
                List.of(
                        "Carry Baby Animals",
                        "Status: Matched",
                        "Client: 0.1.3",
                        "Server: 0.1.3",
                        "Client and server versions match."
                ),
                CarryBabyAnimalsConfigScreen.tooltipText(display)
        );
    }
}
