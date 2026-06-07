package dev.jasmine.carrybabyanimals.client.config;

import dev.jasmine.carrybabyanimals.client.render.FirstPersonLargeBabyVisibilityMode;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

final class ClientCarryVisualConfigEditState {
    private boolean carriedBabyReactionsEnabled;
    private boolean largeBabyTuckedPoseEnabled;
    private FirstPersonLargeBabyVisibilityMode firstPersonLargeBabyVisibilityMode;
    private boolean sleepyCarryVisualsEnabled;
    private final double originalAnimalReactionIntensity;
    private String animalReactionIntensityText;
    private String disabledCarriedReactionAnimalsText;

    private ClientCarryVisualConfigEditState(ClientCarryVisualConfig config) {
        this.carriedBabyReactionsEnabled = config.carriedBabyReactionsEnabled();
        this.largeBabyTuckedPoseEnabled = config.largeBabyTuckedPoseEnabled();
        this.firstPersonLargeBabyVisibilityMode = config.firstPersonLargeBabyVisibilityMode();
        this.sleepyCarryVisualsEnabled = config.sleepyCarryVisualsEnabled();
        this.originalAnimalReactionIntensity = config.animalReactionIntensity();
        this.animalReactionIntensityText = Double.toString(config.animalReactionIntensity());
        this.disabledCarriedReactionAnimalsText = String.join(", ", config.disabledCarriedReactionAnimals());
    }

    static ClientCarryVisualConfigEditState from(ClientCarryVisualConfig config) {
        return new ClientCarryVisualConfigEditState(config);
    }

    boolean carriedBabyReactionsEnabled() {
        return carriedBabyReactionsEnabled;
    }

    void setCarriedBabyReactionsEnabled(boolean carriedBabyReactionsEnabled) {
        this.carriedBabyReactionsEnabled = carriedBabyReactionsEnabled;
    }

    boolean largeBabyTuckedPoseEnabled() {
        return largeBabyTuckedPoseEnabled;
    }

    void setLargeBabyTuckedPoseEnabled(boolean largeBabyTuckedPoseEnabled) {
        this.largeBabyTuckedPoseEnabled = largeBabyTuckedPoseEnabled;
    }

    FirstPersonLargeBabyVisibilityMode firstPersonLargeBabyVisibilityMode() {
        return firstPersonLargeBabyVisibilityMode;
    }

    void setFirstPersonLargeBabyVisibilityMode(FirstPersonLargeBabyVisibilityMode firstPersonLargeBabyVisibilityMode) {
        this.firstPersonLargeBabyVisibilityMode = firstPersonLargeBabyVisibilityMode;
    }

    boolean sleepyCarryVisualsEnabled() {
        return sleepyCarryVisualsEnabled;
    }

    void setSleepyCarryVisualsEnabled(boolean sleepyCarryVisualsEnabled) {
        this.sleepyCarryVisualsEnabled = sleepyCarryVisualsEnabled;
    }

    String animalReactionIntensityText() {
        return animalReactionIntensityText;
    }

    void setAnimalReactionIntensityText(String animalReactionIntensityText) {
        this.animalReactionIntensityText = animalReactionIntensityText == null ? "" : animalReactionIntensityText;
    }

    String disabledCarriedReactionAnimalsText() {
        return disabledCarriedReactionAnimalsText;
    }

    void setDisabledCarriedReactionAnimalsText(String disabledCarriedReactionAnimalsText) {
        this.disabledCarriedReactionAnimalsText = disabledCarriedReactionAnimalsText == null ? "" : disabledCarriedReactionAnimalsText;
    }

    ClientCarryVisualConfig toConfig() {
        return new ClientCarryVisualConfig(
                carriedBabyReactionsEnabled,
                largeBabyTuckedPoseEnabled,
                firstPersonLargeBabyVisibilityMode,
                sleepyCarryVisualsEnabled,
                animalReactionIntensity(),
                disabledCarriedReactionAnimals()
        );
    }

    private double animalReactionIntensity() {
        try {
            return Double.parseDouble(animalReactionIntensityText.trim());
        } catch (NumberFormatException exception) {
            return originalAnimalReactionIntensity;
        }
    }

    private List<String> disabledCarriedReactionAnimals() {
        if (disabledCarriedReactionAnimalsText.isBlank()) {
            return List.of();
        }
        return Arrays.stream(disabledCarriedReactionAnimalsText.split("[,\\s]+"))
                .map(value -> value.trim().toLowerCase(Locale.ROOT))
                .filter(value -> !value.isBlank())
                .toList();
    }
}
