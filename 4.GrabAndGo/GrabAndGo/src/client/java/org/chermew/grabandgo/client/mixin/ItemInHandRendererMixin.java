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
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.animal.feline.Cat;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.animal.rabbit.Rabbit;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.npc.villager.Villager;
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
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.ProblemReporter;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;

import java.util.UUID;

@Mixin(ItemInHandRenderer.class)
public class ItemInHandRendererMixin {
    @Shadow @Final private EntityRenderDispatcher entityRenderDispatcher;
    @Shadow @Final private Minecraft minecraft;

    @Inject(method = "submitHandsWithItems", at = @At("HEAD"), cancellable = true)
    private void grabandgo$renderCarryingInFirstPerson(float partialTick, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, LocalPlayer player, int lightCoords, CallbackInfo ci) {
        GrabCarrier carrier = (GrabCarrier) player;
        if (carrier.grabandgo$isCarrying()) {
            grabandgo$renderCarriedArmsAndObject(partialTick, poseStack, submitNodeCollector, player, lightCoords);
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
        try {
            // Lower the Y translation to -0.45F and push forward to -0.55F to prevent camera clipping
            poseStack.translate(0.0F, -0.45F, -0.55F);
            
            // Render the carried object using the shared static rendering logic
            if ("block".equals(type)) {
                CarriedObjectFeatureRenderer.renderCarriedObject(poseStack, submitNodeCollector, light, carriedData, player, 0);
            } else if ("entity".equals(type)) {
                // --- แทรกส่วนเรนเดอร์ Mob ตรงนี้ค่ะ ---
                renderEntityInFirstPerson(poseStack, submitNodeCollector, light, carriedData, player, partialTick);
            }
        } finally {
            poseStack.popPose();
        }

        // 2. Render both arms holding the carried object
        grabandgo$renderArmHoldingObject(poseStack, submitNodeCollector, light, HumanoidArm.RIGHT, player);
        grabandgo$renderArmHoldingObject(poseStack, submitNodeCollector, light, HumanoidArm.LEFT, player);
    }

    private void renderEntityInFirstPerson(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int light, CompoundTag carriedData, LocalPlayer player, float partialTick) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) return;

        String typeIdStr = carriedData.getStringOr("EntityTypeId", "");
        if (typeIdStr.isEmpty()) return;

        // Compute and cache dummy entity client-side using CarriedObjectFeatureRenderer's cache
        Entity dummy = CarriedObjectFeatureRenderer.dummyEntityCache.get(typeIdStr);
        if (dummy == null || dummy.level() != client.level) {
            try {
                Identifier id = Identifier.tryParse(typeIdStr);
                EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(id)
                    .map(ref -> ref.value())
                    .orElse(null);
                dummy = type != null ? type.create(client.level, new EntitySpawnRequest(EntitySpawnReason.LOAD, false)) : null;
                if (dummy != null) {
                    CarriedObjectFeatureRenderer.dummyEntityCache.put(typeIdStr, dummy);
                }
            } catch (Exception e) {
                dummy = null;
            }
        }

        if (dummy != null) {
            // DESERIALIZE NBT to transfer wool color, custom name, profession, and other properties
            if (carriedData.contains("EntityData")) {
                try {
                    CompoundTag entityNbt = carriedData.getCompound("EntityData").orElse(null);
                    if (entityNbt != null) {
                        net.minecraft.world.level.storage.ValueInput valueInput = net.minecraft.world.level.storage.TagValueInput.create(new ProblemReporter.Collector(), client.level.registryAccess(), entityNbt);
                        dummy.load(valueInput);
                    }
                } catch (Exception nbtEx) {
                    // Fallback
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
            try {
                // Adjust scaling depending on size of entity
                float scale = 0.3F;
                float heightOffset = 0.0F;

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
                    scale = 0.35F;
                } else if (type == cow || type == sheep || type == pig) {
                    scale = 0.28F;
                } else if (type == villager) {
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

                    // Get the camera render state safely from the client's game render state (F5-safe check)
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
                } catch (Exception e) {
                    // Ignore rendering failures for this frame
                }
            } finally {
                poseStack.popPose();
            }
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
        try {
            float side = arm == HumanoidArm.RIGHT ? 1.0F : -1.0F;
            
            // Position the shoulder joint relative to the camera
            // Translate arm starting position lower and much wider for a relaxed carrying pose:
            // X = side * 0.45F, Y = -0.75F, Z = -0.30F
            poseStack.translate(side * 0.45F, -0.75F, -0.30F);
            
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

            try {
                // Reset positions to 0 so the model part pivots exactly around our translated PoseStack origin
                armPart.x = 0.0F;
                armPart.y = 0.0F;
                armPart.z = 0.0F;
                sleevePart.x = 0.0F;
                sleevePart.y = 0.0F;
                sleevePart.z = 0.0F;

                // Set the carrying pose angles directly on the ModelPart rotations (radians)
                // XP: -50 degrees (reach forward to cradle), YP: side * -15 degrees (turn inward slightly), ZP: side * -10 degrees (elbows relaxed down)
                float targetXRot = (float) Math.toRadians(-50.0);
                float targetYRot = (float) Math.toRadians(side * -15.0);
                float targetZRot = (float) Math.toRadians(side * -10.0);

                armPart.xRot = targetXRot;
                armPart.yRot = targetYRot;
                armPart.zRot = targetZRot;
                
                sleevePart.xRot = targetXRot;
                sleevePart.yRot = targetYRot;
                sleevePart.zRot = targetZRot;

                submitNodeCollector.submitCustomGeometry(poseStack, RenderTypes.entityTranslucent(skinTexture), (pose, consumer) -> {
                    PoseStack tempStack = new PoseStack();
                    tempStack.last().pose().set(pose.pose());
                    tempStack.last().normal().set(pose.normal());
                    armPart.render(tempStack, consumer, light, OverlayTexture.NO_OVERLAY);
                    if (sleeveShown) {
                        sleevePart.render(tempStack, consumer, light, OverlayTexture.NO_OVERLAY);
                    }
                });
            } finally {
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
            }
        } finally {
            poseStack.popPose();
        }
    }
}
