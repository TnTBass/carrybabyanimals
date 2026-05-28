package dev.jasmine.carrybabyanimals.cozy;

import dev.jasmine.carrybabyanimals.carry.CarryState;
import dev.jasmine.carrybabyanimals.config.CarryConfig;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.animal.Animal;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class CozyFeedbackScheduler {
    private final CozyFeedbackMessageCatalog messageCatalog;
    private final CozyFeedbackRandom random;
    private final CozyFeedbackEmitter emitter;
    private final Map<UUID, State> statesByCarrier = new HashMap<>();

    public CozyFeedbackScheduler() {
        this(new CozyFeedbackMessageCatalog(), randomSourceBacked(RandomSource.create()), new CozyFeedbackEmitter());
    }

    public CozyFeedbackScheduler(CozyFeedbackMessageCatalog messageCatalog, CozyFeedbackRandom random) {
        this(messageCatalog, random, new CozyFeedbackEmitter());
    }

    public CozyFeedbackScheduler(
            CozyFeedbackMessageCatalog messageCatalog,
            CozyFeedbackRandom random,
            CozyFeedbackEmitter emitter
    ) {
        this.messageCatalog = messageCatalog;
        this.random = random;
        this.emitter = emitter;
    }

    public void tick(ServerPlayer carrier, Animal baby, CarryState carryState, long gameTime, CarryConfig config) {
        CozyFeedbackCarrySnapshot snapshot = new CozyFeedbackCarrySnapshot(
                carrier.getUUID(),
                baby.getId(),
                baby.getDisplayName().getString(),
                baby.hasCustomName(),
                carryState.startedAtTick(),
                gameTime
        );
        CozyFeedbackDecision decision = tickSnapshot(snapshot, config);
        if (decision.playIdleSound()) {
            emitter.playIdleSound(baby);
        }
        decision.sleepyMessage().ifPresent(message -> emitter.showSleepyMessage(carrier, message));
        if (decision.spawnSleepyParticles() && baby.level() instanceof ServerLevel serverLevel) {
            emitter.spawnSleepyParticles(serverLevel, baby);
        }
    }

    CozyFeedbackDecision tickSnapshot(CozyFeedbackCarrySnapshot snapshot, CarryConfig config) {
        if (!config.cozyFeedbackEnabled()) {
            return CozyFeedbackDecision.none();
        }

        State state = statesByCarrier.get(snapshot.carrierId());
        if (state == null || state.carriedEntityId() != snapshot.carriedEntityId()) {
            long firstIdleDueTick = snapshot.startedAtTick() + randomInterval(config);
            state = new State(
                    snapshot.carriedEntityId(),
                    snapshot.gameTime() >= firstIdleDueTick ? firstIdleDueTick : snapshot.gameTime() + randomInterval(config),
                    snapshot.startedAtTick(),
                    snapshot.startedAtTick()
            );
        }

        boolean playIdleSound = false;
        long nextIdleSoundTick = state.nextIdleSoundTick();
        if (config.carriedIdleSoundsEnabled() && snapshot.gameTime() >= state.nextIdleSoundTick()) {
            playIdleSound = true;
            nextIdleSoundTick = snapshot.gameTime() + randomInterval(config);
        }

        boolean sleepyEligible = config.sleepyBabiesEnabled()
                && snapshot.gameTime() - snapshot.startedAtTick() >= config.sleepyAfterTicks();
        Optional<String> sleepyMessage = Optional.empty();
        long lastSleepyMessageTick = state.lastSleepyMessageTick();
        if (sleepyEligible
                && config.pettingMessagesEnabled()
                && snapshot.gameTime() - state.lastSleepyMessageTick() >= config.sleepyMessageCooldownTicks()) {
            sleepyMessage = Optional.of(messageCatalog.sleepyMessage(
                    snapshot.displayName(),
                    snapshot.hasCustomName(),
                    config,
                    0
            ));
            lastSleepyMessageTick = snapshot.gameTime();
        }

        boolean spawnSleepyParticles = false;
        long lastSleepyParticleTick = state.lastSleepyParticleTick();
        if (sleepyEligible
                && config.cozyParticlesEnabled()
                && snapshot.gameTime() - state.lastSleepyParticleTick() >= config.sleepyParticleCooldownTicks()) {
            spawnSleepyParticles = true;
            lastSleepyParticleTick = snapshot.gameTime();
        }

        statesByCarrier.put(snapshot.carrierId(), new State(
                snapshot.carriedEntityId(),
                nextIdleSoundTick,
                lastSleepyMessageTick,
                lastSleepyParticleTick
        ));
        return new CozyFeedbackDecision(playIdleSound, sleepyMessage, spawnSleepyParticles);
    }

    public void clear(UUID carrierId) {
        statesByCarrier.remove(carrierId);
    }

    private int randomInterval(CarryConfig config) {
        return random.nextIntInclusive(config.carriedIdleSoundMinTicks(), config.carriedIdleSoundMaxTicks());
    }

    private static CozyFeedbackRandom randomSourceBacked(RandomSource randomSource) {
        return (inclusiveMin, inclusiveMax) -> inclusiveMin + randomSource.nextInt(inclusiveMax - inclusiveMin + 1);
    }

    private record State(
            int carriedEntityId,
            long nextIdleSoundTick,
            long lastSleepyMessageTick,
            long lastSleepyParticleTick
    ) {
    }
}
