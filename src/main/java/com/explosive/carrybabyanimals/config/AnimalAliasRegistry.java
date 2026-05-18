package com.explosive.carrybabyanimals.config;

import net.minecraft.resources.Identifier;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class AnimalAliasRegistry {
    private final Map<String, ResolvedAnimal> aliases;

    private AnimalAliasRegistry(Map<String, ResolvedAnimal> aliases) {
        this.aliases = Map.copyOf(aliases);
    }

    public static AnimalAliasRegistry createDefault() {
        Map<String, ResolvedAnimal> aliases = new LinkedHashMap<>();
        add(aliases, "cow", "cow", false);
        add(aliases, "pig", "pig", false);
        add(aliases, "sheep", "sheep", false);
        add(aliases, "chicken", "chicken", false);
        add(aliases, "goat", "goat", false);
        add(aliases, "rabbit", "rabbit", false);
        add(aliases, "cat", "cat", false);
        add(aliases, "fox", "fox", false);
        add(aliases, "horse", "horse", false);
        add(aliases, "donkey", "donkey", false);
        add(aliases, "mule", "mule", false);
        add(aliases, "llama", "llama", false);
        add(aliases, "trader_llama", "trader_llama", false);
        add(aliases, "camel", "camel", false);
        add(aliases, "panda", "panda", false);
        add(aliases, "turtle", "turtle", false);
        add(aliases, "wolf", "wolf", false);
        add(aliases, "dog", "wolf", true);
        return new AnimalAliasRegistry(aliases);
    }

    private static void add(Map<String, ResolvedAnimal> aliases, String alias, String vanillaId, boolean requiresTamed) {
        aliases.put(normalize(alias), new ResolvedAnimal(Identifier.withDefaultNamespace(vanillaId), requiresTamed));
    }

    public Optional<ResolvedAnimal> resolve(String name) {
        if (name == null) {
            return Optional.empty();
        }
        String normalized = normalize(name);
        if (normalized.isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable(aliases.get(normalized));
    }

    public Map<String, ResolvedAnimal> aliases() {
        return aliases;
    }

    private static String normalize(String name) {
        return name.trim().toLowerCase(Locale.ROOT);
    }

    public record ResolvedAnimal(Identifier id, boolean requiresTamed) {
    }
}
