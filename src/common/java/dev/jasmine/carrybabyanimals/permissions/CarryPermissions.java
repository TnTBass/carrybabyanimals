package dev.jasmine.carrybabyanimals.permissions;

import net.minecraft.server.level.ServerPlayer;

import java.util.function.BooleanSupplier;

public final class CarryPermissions {
    public static final String CARRY = "carrybabyanimals.carry";
    public static final String CARRY_TAMED = "carrybabyanimals.carry.tamed";
    public static final String CARRY_OTHERS_TAMED = "carrybabyanimals.carry.others_tamed";
    public static final String RELOAD = "carrybabyanimals.reload";
    public static final String NURSERY_BYPASS = "carrybabyanimals.nursery.bypass";

    private static PermissionResolver resolver = PermissionResolver.DEFAULTS;

    private CarryPermissions() {
    }

    public static void useResolver(PermissionResolver resolver) {
        CarryPermissions.resolver = resolver == null ? PermissionResolver.DEFAULTS : resolver;
    }

    public static boolean canCarry(ServerPlayer player) {
        return resolver.check(player, CARRY, true);
    }

    public static boolean canCarryTamed(ServerPlayer player) {
        return resolver.check(player, CARRY_TAMED, true);
    }

    public static boolean canCarryOthersTamed(ServerPlayer player) {
        return resolver.check(player, CARRY_OTHERS_TAMED, false);
    }

    public static boolean canReload(ServerPlayer player) {
        return resolver.check(player, RELOAD, gameMaster(player));
    }

    public static boolean canBypassNursery(ServerPlayer player) {
        return resolver.checkNurseryBypass(player, () -> gameMaster(player));
    }

    private static boolean gameMaster(ServerPlayer player) {
        return player.permissions().hasPermission(net.minecraft.server.permissions.Permissions.COMMANDS_GAMEMASTER);
    }

    public static boolean resolvedPermission(boolean permissionsApiPresent, BooleanSupplier permissionCheck, boolean fallback) {
        return permissionsApiPresent ? permissionCheck.getAsBoolean() : fallback;
    }

    public static boolean nurseryBypassPermission(
            boolean permissionsApiPresent,
            BooleanSupplier permissionCheck,
            BooleanSupplier gameMasterFallback
    ) {
        return resolvedPermission(permissionsApiPresent, permissionCheck, nurseryBypassFallback(permissionsApiPresent, gameMasterFallback));
    }

    public static boolean nurseryBypassFallback(boolean permissionsApiPresent, BooleanSupplier gameMasterFallback) {
        return permissionsApiPresent ? false : gameMasterFallback.getAsBoolean();
    }

    public interface PermissionResolver {
        PermissionResolver DEFAULTS = new PermissionResolver() {
        };

        default boolean check(ServerPlayer player, String permission, boolean fallback) {
            return fallback;
        }

        default boolean checkNurseryBypass(ServerPlayer player, BooleanSupplier gameMasterFallback) {
            return gameMasterFallback.getAsBoolean();
        }
    }
}
