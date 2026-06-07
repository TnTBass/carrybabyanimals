package dev.jasmine.carrybabyanimals.cozy;

@FunctionalInterface
public interface CozyFeedbackRandom {
    int nextIntInclusive(int inclusiveMin, int inclusiveMax);
}
