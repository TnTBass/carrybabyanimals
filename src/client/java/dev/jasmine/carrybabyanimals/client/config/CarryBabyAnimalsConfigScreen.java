package dev.jasmine.carrybabyanimals.client.config;

import dev.jasmine.carrybabyanimals.client.render.FirstPersonLargeBabyVisibilityMode;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.io.IOException;
import java.util.Arrays;

public final class CarryBabyAnimalsConfigScreen extends Screen {
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

        addRenderableOnly(new StringWidget(left, layout.titleY(), contentWidth, rowHeight, Component.literal("Carry Baby Animals"), this.font));

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
            this.minecraft.setScreen(parent);
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
}
