package dev.jasmine.carrybabyanimals.neoforge.permissions;

import dev.jasmine.carrybabyanimals.permissions.CarryPermissions;
import net.minecraft.server.level.ServerPlayer;

import java.util.function.BooleanSupplier;

public final class NeoForgeCarryPermissions implements CarryPermissions.PermissionResolver {
    public NeoForgeCarryPermissions() {
    }

    public static void install() {
        CarryPermissions.useResolver(new NeoForgeCarryPermissions());
    }

    @Override
    public boolean check(ServerPlayer player, String permission, boolean fallback) {
        return fallback;
    }

    @Override
    public boolean checkNurseryBypass(ServerPlayer player, BooleanSupplier gameMasterFallback) {
        return false;
    }
}
