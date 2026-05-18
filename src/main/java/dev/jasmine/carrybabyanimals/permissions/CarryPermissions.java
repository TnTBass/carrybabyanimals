package dev.jasmine.carrybabyanimals.permissions;

import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.level.ServerPlayer;

import java.util.function.BooleanSupplier;

public final class CarryPermissions {
    private static final String FABRIC_PERMISSIONS_API = "fabric-permissions-api-v0";
    public static final String CARRY = "carrybabyanimals.carry";
    public static final String CARRY_TAMED = "carrybabyanimals.carry.tamed";
    public static final String CARRY_OTHERS_TAMED = "carrybabyanimals.carry.others_tamed";
    public static final String RELOAD = "carrybabyanimals.reload";

    private CarryPermissions() {
    }

    public static boolean canCarry(ServerPlayer player) {
        return check(player, CARRY, true);
    }

    public static boolean canCarryTamed(ServerPlayer player) {
        return check(player, CARRY_TAMED, true);
    }

    public static boolean canCarryOthersTamed(ServerPlayer player) {
        return check(player, CARRY_OTHERS_TAMED, false);
    }

    public static boolean canReload(ServerPlayer player) {
        return check(
                player,
                RELOAD,
                player.permissions().hasPermission(net.minecraft.server.permissions.Permissions.COMMANDS_GAMEMASTER)
        );
    }

    private static boolean check(ServerPlayer player, String permission, boolean fallback) {
        return resolvedPermission(
                FabricLoader.getInstance().isModLoaded(FABRIC_PERMISSIONS_API),
                () -> Permissions.check(player, permission, fallback),
                fallback
        );
    }

    static boolean resolvedPermission(boolean permissionsApiPresent, BooleanSupplier permissionCheck, boolean fallback) {
        return permissionsApiPresent ? permissionCheck.getAsBoolean() : fallback;
    }
}
