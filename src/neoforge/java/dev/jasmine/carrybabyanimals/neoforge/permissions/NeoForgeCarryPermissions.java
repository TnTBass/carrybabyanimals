package dev.jasmine.carrybabyanimals.neoforge.permissions;

import dev.jasmine.carrybabyanimals.CarryBabyAnimals;
import dev.jasmine.carrybabyanimals.permissions.CarryPermissions;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.neoforged.neoforge.server.permission.PermissionAPI;
import net.neoforged.neoforge.server.permission.events.PermissionGatherEvent;
import net.neoforged.neoforge.server.permission.nodes.PermissionNode;
import net.neoforged.neoforge.server.permission.nodes.PermissionTypes;

import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.stream.Collectors;

public final class NeoForgeCarryPermissions implements CarryPermissions.PermissionResolver {
    private static final PermissionNode<Boolean> CARRY = node(CarryPermissions.CARRY, true);
    private static final PermissionNode<Boolean> CARRY_TAMED = node(CarryPermissions.CARRY_TAMED, true);
    private static final PermissionNode<Boolean> CARRY_OTHERS_TAMED = node(CarryPermissions.CARRY_OTHERS_TAMED, false);
    private static final PermissionNode<Boolean> NURSERY_BYPASS = node(CarryPermissions.NURSERY_BYPASS, false);
    private static final PermissionNode<Boolean> RELOAD = node(
            CarryPermissions.RELOAD,
            (player, uuid, contexts) -> player == null
                    || player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)
    );
    private static final List<PermissionNode<Boolean>> NODES = List.of(
            CARRY,
            CARRY_TAMED,
            CARRY_OTHERS_TAMED,
            NURSERY_BYPASS,
            RELOAD
    );
    private static final Map<String, PermissionNode<Boolean>> NODES_BY_NAME = NODES.stream()
            .collect(Collectors.toUnmodifiableMap(PermissionNode::getNodeName, permissionNode -> permissionNode));

    private final Backend backend;

    public NeoForgeCarryPermissions() {
        this(PermissionAPI::getPermission);
    }

    NeoForgeCarryPermissions(Backend backend) {
        this.backend = backend;
    }

    public static void install() {
        CarryPermissions.useResolver(new NeoForgeCarryPermissions());
    }

    public static void registerNodes(PermissionGatherEvent.Nodes event) {
        event.addNodes(NODES.toArray(PermissionNode[]::new));
    }

    @Override
    public boolean check(ServerPlayer player, String permission, boolean fallback) {
        PermissionNode<Boolean> node = NODES_BY_NAME.get(permission);
        return node == null ? fallback : backend.getPermission(player, node);
    }

    @Override
    public boolean checkNurseryBypass(ServerPlayer player, BooleanSupplier gameMasterFallback) {
        return backend.getPermission(player, NURSERY_BYPASS);
    }

    static List<PermissionNode<Boolean>> nodes() {
        return NODES;
    }

    static PermissionNode<Boolean> node(String permission) {
        return NODES_BY_NAME.get(permission);
    }

    interface Backend {
        boolean getPermission(ServerPlayer player, PermissionNode<Boolean> node);
    }

    private static PermissionNode<Boolean> node(String permission, boolean fallback) {
        return node(permission, (player, uuid, contexts) -> fallback);
    }

    private static PermissionNode<Boolean> node(
            String permission,
            PermissionNode.PermissionResolver<Boolean> resolver
    ) {
        String prefix = CarryBabyAnimals.MOD_ID + ".";
        String path = permission.startsWith(prefix) ? permission.substring(prefix.length()) : permission;
        return new PermissionNode<>(CarryBabyAnimals.MOD_ID, path, PermissionTypes.BOOLEAN, resolver);
    }
}
