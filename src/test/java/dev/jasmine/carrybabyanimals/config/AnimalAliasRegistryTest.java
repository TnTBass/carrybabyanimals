package dev.jasmine.carrybabyanimals.config;

import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class AnimalAliasRegistryTest {
    @Test
    void resolvesCommonFriendlyNames() {
        AnimalAliasRegistry aliases = AnimalAliasRegistry.createDefault();

        assertEquals(Identifier.withDefaultNamespace("cow"), aliases.resolve("cow").orElseThrow().id());
        assertEquals(Identifier.withDefaultNamespace("pig"), aliases.resolve("pig").orElseThrow().id());
        assertEquals(Identifier.withDefaultNamespace("cat"), aliases.resolve("cat").orElseThrow().id());
    }

    @Test
    void dogAndWolfResolveToWolfWithDifferentTamedPolicy() {
        AnimalAliasRegistry aliases = AnimalAliasRegistry.createDefault();

        AnimalAliasRegistry.ResolvedAnimal dog = aliases.resolve("dog").orElseThrow();
        AnimalAliasRegistry.ResolvedAnimal wolf = aliases.resolve("wolf").orElseThrow();

        assertEquals(Identifier.withDefaultNamespace("wolf"), dog.id());
        assertEquals(Identifier.withDefaultNamespace("wolf"), wolf.id());
        assertTrue(dog.requiresTamed());
        assertFalse(wolf.requiresTamed());
    }

    @Test
    void ignoresCaseAndWhitespace() {
        AnimalAliasRegistry aliases = AnimalAliasRegistry.createDefault();

        assertEquals(Identifier.withDefaultNamespace("sheep"), aliases.resolve(" Sheep ").orElseThrow().id());
    }

    @Test
    void nullAndBlankNamesDoNotResolve() {
        AnimalAliasRegistry aliases = AnimalAliasRegistry.createDefault();

        assertTrue(aliases.resolve(null).isEmpty());
        assertTrue(aliases.resolve("").isEmpty());
        assertTrue(aliases.resolve("   ").isEmpty());
    }
}
