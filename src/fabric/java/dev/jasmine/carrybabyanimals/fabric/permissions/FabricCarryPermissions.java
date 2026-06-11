package dev.jasmine.carrybabyanimals.fabric.permissions;

import dev.jasmine.carrybabyanimals.CarryBabyAnimals;
import dev.jasmine.carrybabyanimals.permissions.CarryPermissions;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

import java.util.function.BooleanSupplier;

public final class FabricCarryPermissions implements CarryPermissions.PermissionResolver {
    private static final String MODERN_FABRIC_PERMISSIONS_API = "fabric-permission-api-v1";
    private static final String LEGACY_FABRIC_PERMISSIONS_API = "fabric-permissions-api-v0";
    private static final String LEGACY_WARNING =
            "Old deprecated permissions API detected. Please update to Fabric API version 0.149.0+26.1.2 or newer.";

    private final Provider modernProvider;
    private final Provider legacyProvider;
    private final WarningSink warningSink;
    private boolean loggedLegacyWarning;

    private FabricCarryPermissions() {
        this(runtimeModernProvider(), runtimeLegacyProvider(), CarryBabyAnimals.LOGGER::warn);
    }

    FabricCarryPermissions(Provider modernProvider, Provider legacyProvider, WarningSink warningSink) {
        this.modernProvider = modernProvider;
        this.legacyProvider = legacyProvider;
        this.warningSink = warningSink;
    }

    public static void install() {
        CarryPermissions.useResolver(new FabricCarryPermissions());
    }

    @Override
    public boolean check(ServerPlayer player, String permission, boolean fallback) {
        Provider provider = activeProvider();
        if (provider == null) {
            return fallback;
        }
        return CarryPermissions.resolvedPermission(
                true,
                () -> provider.check(player, permission, fallback),
                fallback
        );
    }

    @Override
    public boolean checkNurseryBypass(ServerPlayer player, BooleanSupplier gameMasterFallback) {
        Provider provider = activeProvider();
        if (provider == null) {
            return gameMasterFallback.getAsBoolean();
        }
        boolean permissionsApiPresent = provider != null;
        return CarryPermissions.nurseryBypassPermission(
                permissionsApiPresent,
                () -> provider.check(
                        player,
                        CarryPermissions.NURSERY_BYPASS,
                        CarryPermissions.nurseryBypassFallback(permissionsApiPresent, () -> false)
                ),
                gameMasterFallback
        );
    }

    private Provider activeProvider() {
        if (modernProvider.present()) {
            return modernProvider;
        }
        if (legacyProvider.present()) {
            warnLegacyOnce();
            return legacyProvider;
        }
        return null;
    }

    private void warnLegacyOnce() {
        if (!loggedLegacyWarning) {
            warningSink.warn(LEGACY_WARNING);
            loggedLegacyWarning = true;
        }
    }

    private static Provider runtimeModernProvider() {
        if (FabricLoader.getInstance().isModLoaded(MODERN_FABRIC_PERMISSIONS_API)) {
            return new ModernFabricProvider();
        }
        return Provider.ABSENT;
    }

    private static Provider runtimeLegacyProvider() {
        if (FabricLoader.getInstance().isModLoaded(LEGACY_FABRIC_PERMISSIONS_API)) {
            return new LegacyLuckoProvider();
        }
        return Provider.ABSENT;
    }

    interface Provider {
        Provider ABSENT = new Provider() {
            @Override
            public boolean present() {
                return false;
            }

            @Override
            public boolean check(ServerPlayer player, String permission, boolean fallback) {
                return fallback;
            }
        };

        boolean present();

        boolean check(ServerPlayer player, String permission, boolean fallback);
    }

    interface WarningSink {
        void warn(String message);
    }

    private static final class ModernFabricProvider implements Provider {
        @Override
        public boolean present() {
            return true;
        }

        @Override
        public boolean check(ServerPlayer player, String permission, boolean fallback) {
            return ((net.fabricmc.fabric.api.permission.v1.PermissionContextOwner) player)
                    .checkPermission(toIdentifier(permission), fallback);
        }
    }

    private static final class LegacyLuckoProvider implements Provider {
        @Override
        public boolean present() {
            return true;
        }

        @Override
        public boolean check(ServerPlayer player, String permission, boolean fallback) {
            // Remove this compatibility path after the next Minecraft-version update once the modern
            // Fabric permission API is the only supported Fabric permission surface.
            return me.lucko.fabric.api.permissions.v0.Permissions.check(player, permission, fallback);
        }
    }

    private static Identifier toIdentifier(String permission) {
        String prefix = CarryBabyAnimals.MOD_ID + ".";
        String path = permission.startsWith(prefix) ? permission.substring(prefix.length()) : permission;
        return Identifier.fromNamespaceAndPath(CarryBabyAnimals.MOD_ID, path);
    }
}
