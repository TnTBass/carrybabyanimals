# Fabric 26.1.2 API Surfaces

## Confirmed Build Inputs

- Minecraft: 26.1.2
- Java: 25
- Fabric Loader: 0.19.2
- Fabric API: 0.149.0+26.1.2
- Mappings: no explicit mappings dependency; Loom generated non-obfuscated Minecraft 26.1.2 sources

## Passenger Fallback

Use the vanilla passenger system. The carried baby rides the player while carried. Vanilla clients see the baby above the player's head. Modded clients suppress the vanilla render and render the baby at the player's hand position.

## Growth Detection

Record whether Fabric 26.x exposes a baby-to-adult growth event here during implementation. If no event exists, use the `CarryTicker` tick-check: every server tick, inspect carried animals and drop them when `AgeableMob#isBaby()` becomes false. This can allow a one-tick race window between growth and drop; the race is acceptable because the next tick drops the animal safely.

## Interaction Hooks

Record the final hook names used for:

- Sneak-right-click entity interaction: `net.fabricmc.fabric.api.event.player.UseEntityCallback.EVENT`, signature `(Player player, Level world, InteractionHand hand, Entity entity, EntityHitResult hitResult) -> InteractionResult`.
  - If the player is already carrying, the handler returns `InteractionResult.SUCCESS` for sneaking entity interactions. This is intentional: carrying occupies the interaction and clicking another baby while carrying is ignored until the current baby is dropped.
- Carry cleanup ticker / growth fallback: `net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_SERVER_TICK`, signature `(MinecraftServer server) -> void`.
- Left-click attack interception while carrying: `net.fabricmc.fabric.api.event.player.AttackEntityCallback.EVENT`, signature `(Player player, Level world, InteractionHand hand, Entity entity, EntityHitResult hitResult) -> InteractionResult`.
- Left-click block attack interception while carrying: `net.fabricmc.fabric.api.event.player.AttackBlockCallback.EVENT`, signature `(Player player, Level world, InteractionHand hand, BlockPos pos, Direction direction) -> InteractionResult`.
- Block break prevention fallback while carrying: `net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents.BEFORE`, signature `(Level world, Player player, BlockPos pos, BlockState state, BlockEntity blockEntity) -> boolean`.
- Use-item interaction blocking while carrying: `net.fabricmc.fabric.api.event.player.UseItemCallback.EVENT`, signature `(Player player, Level world, InteractionHand hand) -> InteractionResult`.
- Use-block interaction blocking while carrying: `net.fabricmc.fabric.api.event.player.UseBlockCallback.EVENT`, signature `(Player player, Level world, InteractionHand hand, BlockHitResult hitResult) -> InteractionResult`.
- Logout cleanup: `net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.DISCONNECT`, signature `(ServerGamePacketListenerImpl handler, MinecraftServer server) -> void`.
- Server stop cleanup: `net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPING`, signature `(MinecraftServer server) -> void`.
  - Shutdown cleanup intentionally drops carried babies through the normal safe placement/collision path before world save completes. It skips only the final destination chunk force-load during `SERVER_STOPPING` to avoid extra late chunk work.
- Death cleanup: `net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents.ALLOW_DEATH`, signature `(ServerPlayer player, DamageSource damageSource, float damageAmount) -> boolean`.
- Dimension change cleanup: `net.fabricmc.fabric.api.entity.event.v1.ServerEntityLevelChangeEvents.AFTER_PLAYER_CHANGE_LEVEL`, signature `(ServerPlayer player, ServerLevel origin, ServerLevel destination) -> void`.
  - Dimension cleanup drops in the origin level when the carried baby did not move with the player, then clears the carrier's visual state.

## Renderer Hooks

- Clientbound carry sync payloads:
  - Use `net.minecraft.resources.Identifier` and `new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(...))` for payload ids.
  - Use `PayloadTypeRegistry.clientboundPlay().register(type, StreamCodec<RegistryFriendlyByteBuf, payload>)` in common init before gameplay callbacks.
  - Use `StreamCodec.composite(ByteBufCodecs.VAR_INT, ...)` for the minimal integer entity-id payloads.
  - Use `ServerPlayNetworking.canSend(ServerPlayer, CustomPacketPayload.Type<?>)` before `ServerPlayNetworking.send(...)` so vanilla/unmodded clients keep the passenger fallback without receiving unknown custom payloads.
  - Use `PlayerLookup.tracking(Entity)` plus the carrier as the S2C recipient set.
  - Use `EntityTrackingEvents.START_TRACKING`, signature `(Entity entity, ServerPlayer player) -> void`, to replay `SET_CARRIED` when a modded client starts tracking a baby that is already carried.
  - Use `EntityTrackingEvents.STOP_TRACKING`, signature `(Entity entity, ServerPlayer player) -> void`, to send `CLEAR_CARRIED` when a client stops tracking a carried baby.
  - Use `ServerPlayConnectionEvents.JOIN`, signature `(ServerGamePacketListenerImpl handler, PacketSender sender, MinecraftServer server) -> void`, to replay currently visible carried babies after a modded client finishes joining. The replay is limited to carries already visible through `PlayerLookup.tracking(baby)` or the carrier itself; later visibility is covered by `START_TRACKING`.
- Client packet handlers:
  - Use `ClientPlayNetworking.registerGlobalReceiver(type, (payload, context) -> context.client().execute(...))`.
  - The Fabric 26.1 handler signature is `receive(T payload, ClientPlayNetworking.Context context)`.
  - Use `ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> ...)` to clear client-side render state when leaving a server/world.
- Vanilla render suppression:
  - Use a client mixin on `net.minecraft.client.renderer.entity.LivingEntityRenderer`.
  - The verified extraction descriptor is `extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V`.
  - The verified submit descriptor is `submit(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V`.
  - The mixin stores a Fabric `RenderStateDataKey<Boolean>` on the extracted `LivingEntityRenderState`, then cancels only submit calls marked as carried babies.
- Held replacement render:
  - The older `WorldRenderEvents` API was not present in Fabric API 0.149.0+26.1.2.
  - Use `net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents.COLLECT_SUBMITS` with `LevelRenderContext`.
  - `LevelRenderContext` exposes `poseStack()`, `submitNodeCollector()`, and `levelState().cameraRenderState`; it does not expose the old world-render context names.
  - Use `Minecraft.getInstance().getEntityRenderDispatcher().extractEntity(entity, tickDelta)` to build an `EntityRenderState`, adjust its world-space `x/y/z`, then call `EntityRenderDispatcher.submit(state, cameraRenderState, x - camera.x, y - camera.y, z - camera.z, poseStack, submitNodeCollector)`.
  - Client render state prunes entries during `COLLECT_SUBMITS` when the baby or carrier entity is missing, and clears individual entries when either entity is dead. This keeps missed server clears from hiding future same-id entities indefinitely.
  - The Task 8 held pose is intentionally modest: the baby is moved near the carrier's upper body/main-arm side. Exact hand bone attachment remains a later polish item because the new submit pipeline does not expose a simple per-limb attachment event here.

## Task 4 Carry Eligibility Substitutions

- Use `net.minecraft.resources.Identifier` instead of the plan's `net.minecraft.resources.ResourceLocation`.
- `EntityType.getKey(entity.getType())` returns `Identifier` in this repo's Minecraft 26.1.2 generated sources.
- Use `TamableAnimal#isOwnedBy(LivingEntity)` instead of the older `getOwnerUUID()` owner check.
- Use `ServerPlayer#permissions().hasPermission(net.minecraft.server.permissions.Permissions.COMMANDS_GAMEMASTER)` instead of the older `hasPermissions(2)` command-level check.

## Task 5 Passenger Attachment Substitutions

- Use `Entity#startRiding(Entity, boolean force, boolean sendEventAndTriggers)` instead of the plan's two-argument `startRiding(Entity, boolean)` overload.
- Use `Entity#snapTo(double, double, double, float, float)` instead of the older `moveTo(double, double, double, float, float)` repositioning call.

## Task 7 Petting Substitutions

- Use `ServerPlayer#level()` and cast to `ServerLevel` instead of the plan's `ServerPlayer#serverLevel()` helper, which is not exposed by this repo's Minecraft 26.1.2 mappings.
