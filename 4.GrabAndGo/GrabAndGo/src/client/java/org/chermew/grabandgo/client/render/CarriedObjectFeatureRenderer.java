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
        
        // Translate and position the carried object in front of the player's chest/arms
        poseStack.translate(0.0F, 0.9F, -0.45F);
        
        if ("block".equals(type)) {
            renderBlock(poseStack, submitNodeCollector, lightCoords, state.id, state.outlineColor, carriedData);
        } else if ("entity".equals(type)) {
            renderEntity(poseStack, submitNodeCollector, lightCoords, carriedData, player);
        }
        
        poseStack.popPose();
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
            // Scale block so it doesn't completely block player's view
            poseStack.scale(0.65F, 0.65F, 0.65F);
            // Center block pivot point
            poseStack.translate(-0.5F, -0.5F, -0.5F);

            // Populate/Update BlockModelRenderState
            BlockModelRenderState renderState = blockStateCache.computeIfAbsent(playerId, id -> new BlockModelRenderState());
            getBlockModelResolver().update(renderState, blockState, BLOCK_DISPLAY_CONTEXT);

            // Submit block render commands
            renderState.submit(poseStack, submitNodeCollector, lightCoords, OverlayTexture.NO_OVERLAY, outlineColor);

            poseStack.popPose();
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
            Entity dummy = dummyEntityCache.computeIfAbsent(typeIdStr, idStr -> {
                try {
                    Identifier id = Identifier.tryParse(idStr);
                    EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(id)
                        .map(ref -> ref.value())
                        .orElse(null);
                    return type != null ? type.create(client.level, EntitySpawnReason.LOAD) : null;
                } catch (Exception e) {
                    LOGGER.error("Failed to create dummy client entity: {}", idStr, e);
                    return null;
                }
            });

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
                    
                    // Force disable entity ticking values for rendering stability
                    dummy.tickCount = 0;
                    dummy.setPos(dummy.getX(), dummy.getY(), dummy.getZ());
                } catch (Exception nbtEx) {
                    // Fallback to default state if NBT loading fails for this frame
                }
            }

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
            
            // Adjust scaling depending on size of entity
            float scale = 0.5F;
            float heightOffset = 0.0F;
            
            // Customize dimensions for certain common mobs to make them fit nicely
            EntityType<?> type = dummy.getType();
            if (type == EntityType.CHICKEN || type == EntityType.RABBIT || type == EntityType.CAT || type == EntityType.WOLF) {
                scale = 0.6F;
            } else if (type == EntityType.COW || type == EntityType.SHEEP || type == EntityType.PIG) {
                scale = 0.45F;
            } else if (type == EntityType.VILLAGER) {
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

            // Get the camera render state from the client's game render state
            CameraRenderState cameraState = client.gameRenderer.getGameRenderState().levelRenderState.cameraRenderState;

            // Submit entity render commands
            client.getEntityRenderDispatcher().submit(entityState, cameraState, 0.0, 0.0, 0.0, poseStack, submitNodeCollector);

            poseStack.popPose();
        } catch (Exception e) {
            LOGGER.error("Failed to render carried entity", e);
        }
    }
}
