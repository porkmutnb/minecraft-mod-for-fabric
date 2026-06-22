package org.chermew.grabandgo.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.animal.feline.Cat;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.animal.rabbit.Rabbit;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.ValueInput;
import org.chermew.grabandgo.duck.GrabCarrier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class CarriedObjectFeatureRenderer extends RenderLayer<AvatarRenderState, PlayerModel> {
    private static final Logger LOGGER = LoggerFactory.getLogger("GrabAndGoRenderer");
    private static final BlockDisplayContext BLOCK_DISPLAY_CONTEXT = BlockDisplayContext.create();
    public static final Map<String, Entity> dummyEntityCache = new HashMap<>();
    private static final Map<Integer, BlockModelRenderState> blockStateCache = new HashMap<>();
    private static BlockModelResolver blockModelResolver;

    private static BlockModelResolver getBlockModelResolver() {
        if (blockModelResolver == null) {
            blockModelResolver = new BlockModelResolver(Minecraft.getInstance().getModelManager());
        }
        return blockModelResolver;
    }

    public CarriedObjectFeatureRenderer(RenderLayerParent<AvatarRenderState, PlayerModel> context) {
        super(context);
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords, AvatarRenderState state, float limbSwing, float limbSwingAmount) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) return;

        // Retrieve player entity on client using state's unique entity ID
        Entity entity = client.level.getEntity(state.id);
        if (!(entity instanceof Player player)) {
            return;
        }

        GrabCarrier carrier = (GrabCarrier) player;
        if (!carrier.grabandgo$isCarrying()) {
            return;
        }

        CompoundTag carriedData = carrier.grabandgo$getCarriedData();
        // GHOST RENDER PREVENTION: Check if NBT data is empty but state is active
        if (carriedData == null || carriedData.isEmpty()) {
            return;
        }

        String type = carriedData.getStringOr("Type", "");
        
        poseStack.pushPose();
        try {
            // Translate and position the carried object in front of the player's chest/arms
            poseStack.translate(0.0F, 0.9F, -0.45F);
            
            if ("block".equals(type)) {
                renderBlock(poseStack, submitNodeCollector, lightCoords, state.id, state.outlineColor, carriedData);
            } else if ("entity".equals(type)) {
                renderEntity(poseStack, submitNodeCollector, lightCoords, carriedData, player);
            }
        } finally {
            poseStack.popPose();
        }
    }

    public static void renderCarriedObject(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords, CompoundTag carriedData, Entity player, int outlineColor) {
        String type = carriedData.getStringOr("Type", "");
        if ("block".equals(type)) {
            renderBlock(poseStack, submitNodeCollector, lightCoords, player.getId(), outlineColor, carriedData);
        } else if ("entity".equals(type)) {
            renderEntity(poseStack, submitNodeCollector, lightCoords, carriedData, player);
        }
    }

    private static void renderBlock(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords, int playerId, int outlineColor, CompoundTag carriedData) {
        try {
            Minecraft client = Minecraft.getInstance();
            if (client.level == null) return;

            BlockState blockState = null;
            if (carriedData.contains("BlockState")) {
                CompoundTag stateNbt = carriedData.getCompound("BlockState").orElse(null);
                if (stateNbt != null) {
                    blockState = NbtUtils.readBlockState(client.level.registryAccess().lookupOrThrow(Registries.BLOCK), stateNbt);
                }
            }

            if (blockState == null || blockState.isAir()) {
                String blockIdStr = carriedData.getStringOr("BlockId", "");
                Identifier blockId = Identifier.tryParse(blockIdStr);
                if (blockId != null) {
                    blockState = BuiltInRegistries.BLOCK.get(blockId)
                        .map(ref -> ref.value().defaultBlockState())
                        .orElse(null);
                }
            }

            if (blockState == null || blockState.isAir()) {
                return;
            }

            poseStack.pushPose();
            try {
                // Scale block so it doesn't completely block player's view
                poseStack.scale(0.65F, 0.65F, 0.65F);
                // Center block pivot point
                poseStack.translate(-0.5F, -0.5F, -0.5F);

                // Populate/Update BlockModelRenderState
                BlockModelRenderState renderState = blockStateCache.computeIfAbsent(playerId, id -> new BlockModelRenderState());
                getBlockModelResolver().update(renderState, blockState, BLOCK_DISPLAY_CONTEXT);

                // Submit block render commands
                renderState.submit(poseStack, submitNodeCollector, lightCoords, OverlayTexture.NO_OVERLAY, outlineColor);
            } finally {
                poseStack.popPose();
            }
        } catch (Exception e) {
            LOGGER.error("Failed to render carried block", e);
        }
    }

    private static void renderEntity(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords, CompoundTag carriedData, Entity player) {
        try {
            Minecraft client = Minecraft.getInstance();
            if (client.level == null) return;

            String typeIdStr = carriedData.getStringOr("EntityTypeId", "");
            if (typeIdStr.isEmpty()) return;

            // Compute and cache dummy entity client-side
            Entity dummy = dummyEntityCache.get(typeIdStr);
            if (dummy == null || dummy.level() != client.level) {
                try {
                    Identifier id = Identifier.tryParse(typeIdStr);
                    EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(id)
                        .map(ref -> ref.value())
                        .orElse(null);
                    dummy = type != null ? type.create(client.level, EntitySpawnReason.LOAD) : null;
                    if (dummy != null) {
                        dummyEntityCache.put(typeIdStr, dummy);
                    }
                } catch (Exception e) {
                    LOGGER.error("Failed to create dummy client entity: {}", typeIdStr, e);
                    dummy = null;
                }
            }

            if (dummy == null) return;

            // DESERIALIZE NBT to transfer wool color, custom name, profession, and other properties
            if (carriedData.contains("EntityData")) {
                try {
                    CompoundTag entityNbt = carriedData.getCompound("EntityData").orElse(null);
                    if (entityNbt != null) {
                        // Wrap CompoundTag into ValueInput for dummy loading in 1.26.2
                        ValueInput valueInput = TagValueInput.create(new ProblemReporter.Collector(), client.level.registryAccess(), entityNbt);
                        dummy.load(valueInput);
                    }
                } catch (Exception nbtEx) {
                    // Fallback to default state if NBT loading fails for this frame
                }
            }

            // Force ensure visibility
            dummy.setInvisible(false);

            // Sync positions with player to avoid culling and light issues
            dummy.tickCount = player.tickCount;
            dummy.setPos(player.getX(), player.getY(), player.getZ());
            dummy.xo = player.xo;
            dummy.yo = player.yo;
            dummy.zo = player.zo;
            dummy.xOld = player.getX();
            dummy.yOld = player.getY();
            dummy.zOld = player.getZ();

            // Sync dummy entity's rotations with the player's view direction
            // In first person, this keeps the mob facing the correct direction relative to the camera as the player rotates.
            // In third person, this makes the mob turn dynamically as the player turns.
            float playerYaw = player.getViewYRot(client.getDeltaTracker().getGameTimeDeltaTicks());
            float playerPitch = player.getViewXRot(client.getDeltaTracker().getGameTimeDeltaTicks());
            dummy.setYRot(playerYaw);
            dummy.setXRot(playerPitch);
            dummy.yRotO = playerYaw;
            dummy.xRotO = playerPitch;
            if (dummy instanceof net.minecraft.world.entity.LivingEntity livingDummy) {
                if (player instanceof net.minecraft.world.entity.LivingEntity livingPlayer) {
                    livingDummy.yBodyRot = livingPlayer.yBodyRot;
                    livingDummy.yBodyRotO = livingPlayer.yBodyRotO;
                } else {
                    livingDummy.yBodyRot = playerYaw;
                    livingDummy.yBodyRotO = playerYaw;
                }
                livingDummy.yHeadRot = playerYaw;
                livingDummy.yHeadRotO = playerYaw;
            }

            poseStack.pushPose();
            try {
                // Adjust scaling depending on size of entity
                float scale = 0.5F;
                float heightOffset = 0.0F;
                
                // Customize dimensions for certain common mobs to make them fit nicely
                EntityType<?> type = dummy.getType();
                EntityType<Chicken> chicken = EntityTypes.CHICKEN;
                EntityType<Rabbit> rabbit = EntityTypes.RABBIT;
                EntityType<Cat> cat = EntityTypes.CAT;
                EntityType<Wolf> wolf = EntityTypes.WOLF;
                EntityType<Cow> cow = EntityTypes.COW;
                EntityType<Sheep> sheep = EntityTypes.SHEEP;
                EntityType<Pig> pig = EntityTypes.PIG;
                EntityType<Villager> villager = EntityTypes.VILLAGER;
                if (type == chicken || type == rabbit || type == cat || type == wolf) {
                    scale = 0.6F;
                } else if (type == cow || type == sheep || type == pig) {
                    scale = 0.45F;
                } else if (type == villager) {
                    scale = 0.4F;
                    heightOffset = -0.5F; // lower villager model slightly
                }

                // Flip Y and Z axes to correct upside-down rendering and face the player
                // Y is flipped because player model space has positive Y pointing down, whereas world space has positive Y pointing up.
                // Z is flipped because the local front of standard entities is negative Z, so flipping Z points them towards the camera.
                poseStack.scale(scale, -scale, -scale);
                poseStack.translate(0.0F, -heightOffset, 0.0F); // Invert heightOffset sign because Y is flipped

                // Extract partialTick / gameTimeDeltaTicks
                float partialTick = client.getDeltaTracker().getGameTimeDeltaTicks();

                // Extract the EntityRenderState for the dummy entity
                EntityRenderState entityState = client.getEntityRenderDispatcher().extractEntity(dummy, partialTick);

                // Get the camera render state from the client's game render state safely (F5-safe check)
                var gameRenderState = client.gameRenderer.gameRenderState();
                if (gameRenderState != null && gameRenderState.levelRenderState != null && gameRenderState.levelRenderState.cameraRenderState != null) {
                    CameraRenderState cameraState = gameRenderState.levelRenderState.cameraRenderState;
                    if (entityState != null) {
                        // Submit entity render commands directly to the entity's renderer to bypass dispatcher culling
                        net.minecraft.client.renderer.entity.EntityRenderer renderer = client.getEntityRenderDispatcher().getRenderer(entityState);
                        if (renderer != null) {
                            renderer.submit(entityState, poseStack, submitNodeCollector, cameraState);
                        }
                    }
                }
            } finally {
                poseStack.popPose();
            }
        } catch (Exception e) {
            LOGGER.error("Failed to render carried entity", e);
        }
    }
}
