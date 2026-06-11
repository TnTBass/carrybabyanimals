package dev.jasmine.carrybabyanimals.neoforge.permissions;

import dev.jasmine.carrybabyanimals.permissions.CarryPermissions;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class NeoForgeCarryPermissionsTest {
    @Test
    void permissionChecksUseNeoForgePermissionApiNodes() {
        RecordingBackend backend = new RecordingBackend();
        backend.results.add(true);
        backend.results.add(false);
        NeoForgeCarryPermissions permissions = new NeoForgeCarryPermissions(backend);

        assertTrue(permissions.check(null, CarryPermissions.CARRY, true));
        assertFalse(permissions.check(null, CarryPermissions.CARRY_TAMED, true));

        assertEquals(List.of(CarryPermissions.CARRY, CarryPermissions.CARRY_TAMED), backend.queries);
    }

    @Test
    void nurseryBypassCanBeGrantedThroughNeoForgePermissionApi() {
        RecordingBackend backend = new RecordingBackend();
        backend.results.add(true);
        NeoForgeCarryPermissions permissions = new NeoForgeCarryPermissions(backend);

        assertTrue(permissions.checkNurseryBypass(null, () -> false));

        assertEquals(List.of(CarryPermissions.NURSERY_BYPASS), backend.queries);
    }

    @Test
    void unknownPermissionUsesFallbackWithoutQueryingNeoForgePermissionApi() {
        RecordingBackend backend = new RecordingBackend();
        NeoForgeCarryPermissions permissions = new NeoForgeCarryPermissions(backend);

        assertFalse(permissions.check(null, "carrybabyanimals.unknown", false));
        assertTrue(permissions.check(null, "carrybabyanimals.unknown", true));

        assertTrue(backend.queries.isEmpty());
    }

    @Test
    void registeredNodesKeepStableNamesAndDefaults() {
        assertEquals(
                List.of(
                        CarryPermissions.CARRY,
                        CarryPermissions.CARRY_TAMED,
                        CarryPermissions.CARRY_OTHERS_TAMED,
                        CarryPermissions.NURSERY_BYPASS,
                        CarryPermissions.RELOAD
                ),
                NeoForgeCarryPermissions.nodes().stream()
                        .map(net.neoforged.neoforge.server.permission.nodes.PermissionNode::getNodeName)
                        .toList()
        );

        assertTrue(NeoForgeCarryPermissions.node(CarryPermissions.CARRY).getDefaultResolver().resolve(null, null));
        assertTrue(NeoForgeCarryPermissions.node(CarryPermissions.CARRY_TAMED).getDefaultResolver().resolve(null, null));
        assertFalse(NeoForgeCarryPermissions.node(CarryPermissions.CARRY_OTHERS_TAMED).getDefaultResolver().resolve(null, null));
        assertFalse(NeoForgeCarryPermissions.node(CarryPermissions.NURSERY_BYPASS).getDefaultResolver().resolve(null, null));
        assertTrue(NeoForgeCarryPermissions.node(CarryPermissions.RELOAD).getDefaultResolver().resolve(null, null));
    }

    private static final class RecordingBackend implements NeoForgeCarryPermissions.Backend {
        private final List<String> queries = new ArrayList<>();
        private final Queue<Boolean> results = new ArrayDeque<>();

        @Override
        public boolean getPermission(
                net.minecraft.server.level.ServerPlayer player,
                net.neoforged.neoforge.server.permission.nodes.PermissionNode<Boolean> node
        ) {
            queries.add(node.getNodeName());
            return results.remove();
        }
    }
}
