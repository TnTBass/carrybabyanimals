package dev.jasmine.carrybabyanimals.permissions;

import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.server.level.ServerPlayer;

public final class CarryPermissions {
    public static final String CARRY = "carrybabyanimals.carry";
    public static final String CARRY_TAMED = "carrybabyanimals.carry.tamed";
    public static final String CARRY_OTHERS_TAMED = "carrybabyanimals.carry.others_tamed";
    public static final String RELOAD = "carrybabyanimals.reload";

    private CarryPermissions() {
    }

    public static boolean canCarry(ServerPlayer player) {
        return Permissions.check(player, CARRY, true);
    }

    public static boolean canCarryTamed(ServerPlayer player) {
        return Permissions.check(player, CARRY_TAMED, true);
    }

    public static boolean canCarryOthersTamed(ServerPlayer player) {
        return Permissions.check(player, CARRY_OTHERS_TAMED, false);
    }

    public static boolean canReload(ServerPlayer player) {
        return Permissions.check(
                player,
                RELOAD,
                player.permissions().hasPermission(net.minecraft.server.permissions.Permissions.COMMANDS_GAMEMASTER)
        );
    }
}
