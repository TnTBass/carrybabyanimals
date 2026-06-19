package dev.jasmine.carrybabyanimals.client.config;

import dev.jasmine.carrybabyanimals.client.modstatus.ClientModStatusTracker;
import dev.jasmine.carrybabyanimals.client.render.FirstPersonLargeBabyVisibilityMode;
import dev.jasmine.carrybabyanimals.internal.modstatus.ModStatusDisplay;
import dev.jasmine.carrybabyanimals.internal.modstatus.StatusTone;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class CarryBabyAnimalsConfigScreen extends Screen {
    static final int STATUS_SQUARE_SIZE = 8;
    static final int STATUS_SQUARE_BORDER_COLOR = 0xFF222222;

    private final Screen parent;
    private final ClientCarryVisualConfigEditState editState;

    public CarryBabyAnimalsConfigScreen(Screen parent) {
        super(Component.literal("Carry Baby Animals"));
        this.parent = parent;
        this.editState = ClientCarryVisualConfigEditState.from(ClientCarryVisualConfigManager.config());
    }

    @Override
    protected void init() {
        CarryBabyAnimalsConfigScreenLayout layout = CarryBabyAnimalsConfigScreenLayout.create(this.width, this.height);
        int left = layout.left();
        int contentWidth = layout.contentWidth();
        int rowHeight = layout.rowHeight();

        addRenderableOnly(new StringWidget(left, layout.titleY(), layout.titleWidth(), rowHeight, Component.literal("Carry Baby Animals"), this.font));
        addModStatus(layout);

        addRenderableWidget(Checkbox.builder(Component.literal("Carried baby reactions"), this.font)
                .pos(left, layout.reactionsY())
                .selected(editState.carriedBabyReactionsEnabled())
                .onValueChange((checkbox, selected) -> editState.setCarriedBabyReactionsEnabled(selected))
                .build());

        addRenderableWidget(Checkbox.builder(Component.literal(CarryBabyAnimalsConfigScreenLayout.TUCKED_POSE_LABEL), this.font)
                .pos(left, layout.tuckedPoseY())
                .selected(editState.largeBabyTuckedPoseEnabled())
                .onValueChange((checkbox, selected) -> editState.setLargeBabyTuckedPoseEnabled(selected))
                .build());

        addRenderableWidget(CycleButton.builder(CarryBabyAnimalsConfigScreen::modeLabel, editState.firstPersonLargeBabyVisibilityMode())
                .withValues(Arrays.asList(FirstPersonLargeBabyVisibilityMode.values()))
                .create(left, layout.firstPersonModeY(), contentWidth, rowHeight, Component.literal(CarryBabyAnimalsConfigScreenLayout.FIRST_PERSON_MODE_LABEL), (button, mode) ->
                        editState.setFirstPersonLargeBabyVisibilityMode(mode)
                ));

        addRenderableWidget(Checkbox.builder(Component.literal("Sleepy carry visuals"), this.font)
                .pos(left, layout.sleepyVisualsY())
                .selected(editState.sleepyCarryVisualsEnabled())
                .onValueChange((checkbox, selected) -> editState.setSleepyCarryVisualsEnabled(selected))
                .build());

        addRenderableOnly(new StringWidget(left, layout.intensityLabelY(), contentWidth, rowHeight, Component.literal("Animal reaction intensity"), this.font));
        EditBox intensity = new EditBox(this.font, left, layout.intensityInputY(), contentWidth, rowHeight, Component.literal("Animal reaction intensity"));
        intensity.setValue(editState.animalReactionIntensityText());
        intensity.setResponder(editState::setAnimalReactionIntensityText);
        addRenderableWidget(intensity);

        addRenderableOnly(new StringWidget(left, layout.disabledAnimalsLabelY(), contentWidth, rowHeight, Component.literal("Disabled carried reaction animals"), this.font));
        EditBox disabledAnimals = new EditBox(this.font, left, layout.disabledAnimalsInputY(), contentWidth, rowHeight, Component.literal("Disabled carried reaction animals"));
        disabledAnimals.setMaxLength(512);
        disabledAnimals.setValue(editState.disabledCarriedReactionAnimalsText());
        disabledAnimals.setResponder(editState::setDisabledCarriedReactionAnimalsText);
        addRenderableWidget(disabledAnimals);

        addRenderableWidget(Button.builder(Component.literal("Save"), this::saveAndClose)
                .bounds(left, layout.buttonY(), layout.buttonWidth(), rowHeight)
                .build());
        addRenderableWidget(Button.builder(Component.literal("Cancel"), button -> onClose())
                .bounds(layout.cancelButtonX(), layout.buttonY(), layout.buttonWidth(), rowHeight)
                .build());
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreenAndShow(parent);
        }
    }

    private void saveAndClose(Button button) {
        try {
            ClientCarryVisualConfigManager.save(ClientCarryVisualConfigManager.configPath(), editState.toConfig());
            onClose();
        } catch (IOException exception) {
            button.setMessage(Component.literal("Save failed"));
        }
    }

    private static Component modeLabel(FirstPersonLargeBabyVisibilityMode mode) {
        return Component.literal(switch (mode) {
            case TUCKED -> "Tucked";
            case LOWERED -> "Lowered";
            case HIDE_WHEN_OBSTRUCTING -> "Hide when obstructing";
        });
    }

    private void addModStatus(CarryBabyAnimalsConfigScreenLayout layout) {
        addRenderableOnly(new ConnectionStatusDot(layout));
    }

    private static int toneColor(StatusTone tone) {
        return switch (tone) {
            case GREEN -> 0xFF55FF55;
            case TEAL -> 0xFF55FFFF;
            case ORANGE -> 0xFFFFAA00;
            case RED -> 0xFFFF5555;
            case GRAY -> 0xFFAAAAAA;
        };
    }

    private static List<Component> tooltipLines(ModStatusDisplay display) {
        return tooltipText(display).stream()
                .<Component>map(Component::literal)
                .toList();
    }

    static List<String> tooltipText(ModStatusDisplay display) {
        List<String> text = new ArrayList<>();
        text.add(display.displayName());
        text.add("Status: " + display.statusLabel());
        text.add("Client: " + versionWithBuild(display.clientVersion(), display.clientBuild()));
        text.add("Server: " + versionWithBuild(display.serverVersion(), display.serverBuild()));
        if (!display.helpText().isEmpty()) {
            text.add(display.helpText());
        }
        return text;
    }

    private static String versionWithBuild(String version, String build) {
        if (version == null || version.isBlank()) {
            return "Unknown";
        }
        return build == null || build.isBlank() || "dev".equalsIgnoreCase(build) ? version : version + "+" + build;
    }

    private final class ConnectionStatusDot implements Renderable {
        private final int x;
        private final int y;
        private final int size;

        private ConnectionStatusDot(CarryBabyAnimalsConfigScreenLayout layout) {
            this.x = layout.statusX();
            this.y = layout.statusY();
            this.size = layout.statusWidth();
        }

        @Override
        public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
            ModStatusDisplay display = ClientModStatusTracker.display();
            graphics.fill(x, y, x + size, y + size, STATUS_SQUARE_BORDER_COLOR);
            graphics.fill(x + 1, y + 1, x + size - 1, y + size - 1, toneColor(display.tone()));
            if (isHovered(mouseX, mouseY)) {
                graphics.setComponentTooltipForNextFrame(font, tooltipLines(display), mouseX, mouseY);
            }
        }

        private boolean isHovered(int mouseX, int mouseY) {
            return mouseX >= x && mouseX < x + size && mouseY >= y && mouseY < y + size;
        }
    }
}
