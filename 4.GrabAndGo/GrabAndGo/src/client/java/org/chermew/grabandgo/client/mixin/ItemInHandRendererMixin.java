package org.chermew.grabandgo.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.nbt.CompoundTag;
import org.chermew.grabandgo.duck.GrabCarrier;
import org.chermew.grabandgo.client.render.CarriedObjectFeatureRenderer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.ProblemReporter;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;

@Mixin(ItemInHandRenderer.class)
public class ItemInHandRendererMixin {
    @Shadow @Final private EntityRenderDispatcher entityRenderDispatcher;
    @Shadow @Final private Minecraft minecraft;

    @Inject(method = "renderHandsWithItems", at = @At("HEAD"), cancellable = true)
    private void grabandgo$renderCarryingInFirstPerson(float partialTick, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, LocalPlayer player, int light, CallbackInfo ci) {
        GrabCarrier carrier = (GrabCarrier) player;
        if (carrier.grabandgo$isCarrying()) {
            grabandgo$renderCarriedArmsAndObject(partialTick, poseStack, submitNodeCollector, player, light);
            this.minecraft.gameRenderer.getFeatureRenderDispatcher().renderAllFeatures();
            this.minecraft.renderBuffers().bufferSource().endBatch();
            ci.cancel();
        }
    }

    private void grabandgo$renderCarriedArmsAndObject(float partialTick, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, LocalPlayer player, int light) {
        GrabCarrier carrier = (GrabCarrier) player;
        CompoundTag carriedData = carrier.grabandgo$getCarriedData();
        if (carriedData == null || carriedData.isEmpty()) return;

        String type = carriedData.getStringOr("Type", "");

        // 1. Render the carried object in the lower center of the first-person view
        poseStack.pushPose();
        // Lower the Y translation to -0.45F and push forward to -0.55F to prevent camera clipping
        poseStack.translate(0.0F, -0.45F, -0.55F);
        
        // Render the carried object using the shared static rendering logic
        if ("block".equals(type)) {
            CarriedObjectFeatureRenderer.renderCarriedObject(poseStack, submitNodeCollector, light, carriedData, player, 0);
        } else if ("entity".equals(type)) {
            // --- แทรกส่วนเรนเดอร์ Mob ตรงนี้ค่ะ ---
            renderEntityInFirstPerson(poseStack, submitNodeCollector, light, carriedData, partialTick);
        }
        poseStack.popPose();

        // 2. Render both arms holding the carried object
        grabandgo$renderArmHoldingObject(poseStack, submitNodeCollector, light, HumanoidArm.RIGHT, player);
        grabandgo$renderArmHoldingObject(poseStack, submitNodeCollector, light, HumanoidArm.LEFT, player);
    }

    private void renderEntityInFirstPerson(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int light, CompoundTag carriedData, float partialTick) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) return;

        String typeIdStr = carriedData.getStringOr("EntityTypeId", "");
        if (typeIdStr.isEmpty()) return;

        // Compute and cache dummy entity client-side using CarriedObjectFeatureRenderer's cache
        Entity dummy = CarriedObjectFeatureRenderer.dummyEntityCache.computeIfAbsent(typeIdStr, idStr -> {
            try {
                Identifier id = Identifier.tryParse(idStr);
                EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(id)
                    .map(ref -> ref.value())
                    .orElse(null);
                return type != null ? type.create(client.level, EntitySpawnReason.LOAD) : null;
            } catch (Exception e) {
                return null;
            }
        });

        if (dummy != null) {
            // DESERIALIZE NBT to transfer wool color, custom name, profession, and other properties
            if (carriedData.contains("EntityData")) {
                try {
                    CompoundTag entityNbt = carriedData.getCompound("EntityData").orElse(null);
                    if (entityNbt != null) {
                        net.minecraft.world.level.storage.ValueInput valueInput = net.minecraft.world.level.storage.TagValueInput.create(new ProblemReporter.Collector(), client.level.registryAccess(), entityNbt);
                        dummy.load(valueInput);
                    }
                    dummy.tickCount = 0;
                    dummy.setPos(dummy.getX(), dummy.getY(), dummy.getZ());
                } catch (Exception nbtEx) {
                    // Fallback
                }
            } else {
                dummy.tickCount = 0;
            }

            // Set rotations to 0 so it aligns straight relative to the camera viewport
            dummy.setYRot(0.0F);
            dummy.setXRot(0.0F);
            dummy.yRotO = 0.0F;
            dummy.xRotO = 0.0F;
            if (dummy instanceof net.minecraft.world.entity.LivingEntity livingDummy) {
                livingDummy.yBodyRot = 0.0F;
                livingDummy.yBodyRotO = 0.0F;
                livingDummy.yHeadRot = 0.0F;
                livingDummy.yHeadRotO = 0.0F;
            }

            poseStack.pushPose();

            // Adjust scaling depending on size of entity
            float scale = 0.3F;
            float heightOffset = 0.0F;
            
            EntityType<?> type = dummy.getType();
            if (type == EntityType.CHICKEN || type == EntityType.RABBIT || type == EntityType.CAT || type == EntityType.WOLF) {
                scale = 0.35F;
            } else if (type == EntityType.COW || type == EntityType.SHEEP || type == EntityType.PIG) {
                scale = 0.28F;
            } else if (type == EntityType.VILLAGER) {
                scale = 0.25F;
                heightOffset = -0.3F;
            }

            // Apply scaling and translations (Y is positive up in first-person rendering)
            poseStack.scale(scale, scale, scale);
            poseStack.translate(0.0F, heightOffset, 0.0F);

            // Rotate the dummy entity 180 degrees so it faces the player camera
            poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));

            try {
                // Extract the EntityRenderState for the dummy entity
                EntityRenderState entityState = client.getEntityRenderDispatcher().extractEntity(dummy, partialTick);

                // Get the camera render state from the client's game render state
                CameraRenderState cameraState = client.gameRenderer.getGameRenderState().levelRenderState.cameraRenderState;

                // Submit entity render commands
                client.getEntityRenderDispatcher().submit(entityState, cameraState, 0.0, 0.0, 0.0, poseStack, submitNodeCollector);
            } catch (Exception e) {
                // Ignore rendering failures for this frame
            }

            poseStack.popPose();
        }
    }

    private void grabandgo$renderArmHoldingObject(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int light, HumanoidArm arm, LocalPlayer player) {
        AvatarRenderer avatarRenderer = this.entityRenderDispatcher.getPlayerRenderer(player);
        PlayerModel model = (PlayerModel) avatarRenderer.getModel();
        
        ModelPart armPart = arm == HumanoidArm.RIGHT ? model.rightArm : model.leftArm;
        ModelPart sleevePart = arm == HumanoidArm.RIGHT ? model.rightSleeve : model.leftSleeve;
        
        Identifier skinTexture = player.getSkin().body().texturePath();
        boolean sleeveShown = player.isModelPartShown(arm == HumanoidArm.RIGHT ? PlayerModelPart.RIGHT_SLEEVE : PlayerModelPart.LEFT_SLEEVE);

        poseStack.pushPose();
        
        float side = arm == HumanoidArm.RIGHT ? 1.0F : -1.0F;
        
        // Position the shoulder joint relative to the camera
        // Translate arm starting position to side * 0.30F, Y = -0.55F, Z = -0.4F to hug the entity closely
        poseStack.translate(side * 0.30F, -0.55F, -0.4F);
        
        // Save original position and rotations to restore them after rendering
        float origX = armPart.x;
        float origY = armPart.y;
        float origZ = armPart.z;
        float origXRot = armPart.xRot;
        float origYRot = armPart.yRot;
        float origZRot = armPart.zRot;

        float origSleeveX = sleevePart.x;
        float origSleeveY = sleevePart.y;
        float origSleeveZ = sleevePart.z;
        float origSleeveXRot = sleevePart.xRot;
        float origSleeveYRot = sleevePart.yRot;
        float origSleeveZRot = sleevePart.zRot;

        // Reset positions to 0 so the model part pivots exactly around our translated PoseStack origin
        armPart.x = 0.0F;
        armPart.y = 0.0F;
        armPart.z = 0.0F;
        sleevePart.x = 0.0F;
        sleevePart.y = 0.0F;
        sleevePart.z = 0.0F;

        // Set the carrying pose angles directly on the ModelPart rotations (radians)
        // XP: -35 degrees, YP: side * -55 degrees (hug inward), ZP: side * -35 degrees (elbows downward and inward)
        float targetXRot = (float) Math.toRadians(-35.0);
        float targetYRot = (float) Math.toRadians(side * -55.0);
        float targetZRot = (float) Math.toRadians(side * -35.0);

        armPart.xRot = targetXRot;
        armPart.yRot = targetYRot;
        armPart.zRot = targetZRot;
        
        sleevePart.xRot = targetXRot;
        sleevePart.yRot = targetYRot;
        sleevePart.zRot = targetZRot;

        // Render the arm and sleeve directly using ModelPart.render
        Minecraft client = Minecraft.getInstance();
        VertexConsumer vertexConsumer = client.renderBuffers().bufferSource().getBuffer(
            RenderTypes.entityTranslucent(skinTexture)
        );

        armPart.render(poseStack, vertexConsumer, light, OverlayTexture.NO_OVERLAY);
        if (sleeveShown) {
            sleevePart.render(poseStack, vertexConsumer, light, OverlayTexture.NO_OVERLAY);
        }

        // Restore original position and rotations
        armPart.x = origX;
        armPart.y = origY;
        armPart.z = origZ;
        armPart.xRot = origXRot;
        armPart.yRot = origYRot;
        armPart.zRot = origZRot;

        sleevePart.x = origSleeveX;
        sleevePart.y = origSleeveY;
        sleevePart.z = origSleeveZ;
        sleevePart.xRot = origSleeveXRot;
        sleevePart.yRot = origSleeveYRot;
        sleevePart.z = origSleeveZRot;
        
        poseStack.popPose();
    }
}
