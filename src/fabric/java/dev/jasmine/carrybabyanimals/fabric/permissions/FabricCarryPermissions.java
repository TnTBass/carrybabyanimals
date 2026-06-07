package dev.jasmine.carrybabyanimals.fabric.permissions;

import dev.jasmine.carrybabyanimals.permissions.CarryPermissions;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.level.ServerPlayer;

import java.util.function.BooleanSupplier;

public final class FabricCarryPermissions implements CarryPermissions.PermissionResolver {
    private static final String FABRIC_PERMISSIONS_API = "fabric-permissions-api-v0";

    private FabricCarryPermissions() {
    }

    public static void install() {
        CarryPermissions.useResolver(new FabricCarryPermissions());
    }

    @Override
    public boolean check(ServerPlayer player, String permission, boolean fallback) {
        return CarryPermissions.resolvedPermission(
                permissionsApiPresent(),
                () -> Permissions.check(player, permission, fallback),
                fallback
        );
    }

    @Override
    public boolean checkNurseryBypass(ServerPlayer player, BooleanSupplier gameMasterFallback) {
        boolean permissionsApiPresent = permissionsApiPresent();
        return CarryPermissions.nurseryBypassPermission(
                permissionsApiPresent,
                () -> Permissions.check(
                        player,
                        CarryPermissions.NURSERY_BYPASS,
                        CarryPermissions.nurseryBypassFallback(permissionsApiPresent, () -> false)
                ),
                gameMasterFallback
        );
    }

    private static boolean permissionsApiPresent() {
        return FabricLoader.getInstance().isModLoaded(FABRIC_PERMISSIONS_API);
    }
}
