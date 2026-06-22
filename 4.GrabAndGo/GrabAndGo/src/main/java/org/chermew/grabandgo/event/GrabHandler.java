package org.chermew.grabandgo.event;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import org.chermew.grabandgo.duck.GrabCarrier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class GrabHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger("GrabAndGo");

    public static void register() {
        // Intercept block interactions (grabbing blocks, and placing down blocks/entities)
        UseBlockCallback.EVENT.register(GrabHandler::onUseBlock);

        // Intercept entity interactions (grabbing passive mobs/villagers)
        UseEntityCallback.EVENT.register(GrabHandler::onUseEntity);
    }

    private static InteractionResult onUseBlock(Player player, Level world, InteractionHand hand, BlockHitResult hitResult) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }

        GrabCarrier carrier = (GrabCarrier) player;
        BlockPos pos = hitResult.getBlockPos();
        BlockState state = world.getBlockState(pos);
        Block block = state.getBlock();

        // 1. PLACE OBJECT LOGIC (If already carrying something)
        if (carrier.grabandgo$isCarrying()) {
            // Only allow placing with empty hand (to prevent weird item interactions)
            if (!player.getItemInHand(hand).isEmpty()) {
                return InteractionResult.PASS;
            }

            BlockPos placePos = pos.relative(hitResult.getDirection());
            // Ensure target block is replaceable (e.g. air, tall grass, water)
            if (!world.getBlockState(placePos).canBeReplaced()) {
                return InteractionResult.FAIL;
            }

            CompoundTag carriedData = carrier.grabandgo$getCarriedData();
            if (carriedData == null || carriedData.isEmpty()) {
                // Ghost state recovery
                carrier.grabandgo$clearCarried();
                return InteractionResult.FAIL;
            }

            String type = carriedData.getStringOr("Type", "");
            if ("block".equals(type)) {
                return placeBlock(player, world, placePos, carriedData);
            } else if ("entity".equals(type)) {
                return placeEntity(player, world, placePos, carriedData);
            }

            return InteractionResult.PASS;
        }

        // 2. GRAB BLOCK LOGIC (If sneaking and holding nothing)
        if (player.isShiftKeyDown() && player.getItemInHand(hand).isEmpty()) {
            boolean isChest = block instanceof ChestBlock;
            boolean isBarrel = block instanceof BarrelBlock;

            if (isChest || isBarrel) {
                // Prevent actions on client side (let server handle it and sync via SynchedEntityData)
                if (world.isClientSide()) {
                    return InteractionResult.SUCCESS;
                }

                BlockEntity blockEntity = world.getBlockEntity(pos);
                if (blockEntity == null) {
                    return InteractionResult.PASS;
                }

                try {
                    // Create NBT of the block entity using registry lookup (returns CompoundTag)
                    CompoundTag blockEntityData = blockEntity.saveWithFullMetadata(world.registryAccess());
                    if (blockEntityData == null) {
                        throw new IllegalStateException("Failed to serialize block entity data");
                    }

                    // Build carried object data structure
                    CompoundTag carriedData = new CompoundTag();
                    carriedData.putString("Type", "block");
                    carriedData.putString("BlockId", BuiltInRegistries.BLOCK.getKey(block).toString());
                    
                    // Serialize block state
                    CompoundTag stateNbt = NbtUtils.writeBlockState(state);
                    carriedData.put("BlockState", stateNbt);
                    carriedData.put("BlockEntityData", blockEntityData);

                    // Safety precaution: clear inventory of container block before removing it
                    // to prevent standard replacement trigger (which drops items in onStateReplaced)
                    if (blockEntity instanceof Container container) {
                        container.clearContent();
                    }

                    // Remove the block from the world
                    world.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);

                    // Save data onto the player
                    carrier.grabandgo$setCarriedData(carriedData);
                    carrier.grabandgo$setCarrying(true);

                    // Sound effect
                    world.playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 1.0F, 0.8F);

                    return InteractionResult.SUCCESS;
                } catch (Exception e) {
                    LOGGER.error("Failed to safely grab block entity at {}", pos, e);
                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§cCannot carry this block! (Data Corrupted)"));
                    return InteractionResult.FAIL;
                }
            }
        }

        return InteractionResult.PASS;
    }

    private static InteractionResult onUseEntity(Player player, Level world, InteractionHand hand, Entity entity, EntityHitResult hitResult) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }

        GrabCarrier carrier = (GrabCarrier) player;

        // GRAB ENTITY LOGIC (If sneaking, empty handed, and not carrying anything)
        if (player.isShiftKeyDown() && player.getItemInHand(hand).isEmpty() && !carrier.grabandgo$isCarrying()) {
            boolean isPassive = entity instanceof AgeableMob;
            boolean isMerchant = entity instanceof AbstractVillager;

            if (isPassive || isMerchant) {
                if (world.isClientSide()) {
                    return InteractionResult.SUCCESS;
                }

                try {
                    // Modern 1.26.2 Entity save uses TagValueOutput
                    ProblemReporter.Collector collector = new ProblemReporter.Collector();
                    TagValueOutput tagOutput = TagValueOutput.createWithContext(collector, world.registryAccess());
                    
                    if (!entity.save(tagOutput)) {
                        throw new IllegalStateException("Entity refused to save itself to NBT");
                    }
                    CompoundTag entityNbt = tagOutput.buildResult();

                    CompoundTag carriedData = new CompoundTag();
                    carriedData.putString("Type", "entity");
                    carriedData.putString("EntityTypeId", BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString());
                    carriedData.put("EntityData", entityNbt);

                    // Discard entity from world
                    entity.discard();

                    // Save data on player
                    carrier.grabandgo$setCarriedData(carriedData);
                    carrier.grabandgo$setCarrying(true);

                    // Sound
                    world.playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 1.0F, 1.2F);

                    return InteractionResult.SUCCESS;
                } catch (Exception e) {
                    LOGGER.error("Failed to safely grab entity: {}", entity, e);
                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§cCannot carry this entity! (Data Corrupted)"));
                    return InteractionResult.FAIL;
                }
            }
        }

        return InteractionResult.PASS;
    }

    private static InteractionResult placeBlock(Player player, Level world, BlockPos placePos, CompoundTag carriedData) {
        if (world.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        GrabCarrier carrier = (GrabCarrier) player;
        String blockIdStr = carriedData.getStringOr("BlockId", "");
        String blockTypeStr = carriedData.getStringOr("BlockType", "");
        
        try {
            BlockState state = null;
            if (carriedData.contains("BlockState")) {
                CompoundTag stateNbt = carriedData.getCompoundOrEmpty("BlockState");
                state = NbtUtils.readBlockState(world.registryAccess().lookupOrThrow(Registries.BLOCK), stateNbt);
            }

            // Fallback if state restoration failed
            if (state == null || state.isAir()) {
                Identifier blockId = Identifier.tryParse(blockIdStr);
                Block block = BuiltInRegistries.BLOCK.getValue(blockId);
                state = block.defaultBlockState();
            }

            // Rotate chest based on player direction if applicable
            if (state.hasProperty(ChestBlock.FACING)) {
                state = state.setValue(ChestBlock.FACING, player.getDirection().getOpposite());
            } else if (state.hasProperty(BarrelBlock.FACING)) {
                state = state.setValue(BarrelBlock.FACING, Direction.UP);
            }

            // Place block in world
            world.setBlock(placePos, state, Block.UPDATE_ALL);

            // Restore BlockEntity NBT data
            BlockEntity newBlockEntity = world.getBlockEntity(placePos);
            if (newBlockEntity != null && carriedData.contains("BlockEntityData")) {
                CompoundTag blockEntityData = carriedData.getCompoundOrEmpty("BlockEntityData");
                // Correct coordinates inside block entity NBT
                blockEntityData.putInt("x", placePos.getX());
                blockEntityData.putInt("y", placePos.getY());
                blockEntityData.putInt("z", placePos.getZ());
                
                // Wrap CompoundTag inside TagValueInput for loadWithComponents
                ValueInput valueInput = TagValueInput.create(new ProblemReporter.Collector(), world.registryAccess(), blockEntityData);
                newBlockEntity.loadWithComponents(valueInput);
                newBlockEntity.setChanged();
            }

            // Sync state to clients
            world.sendBlockUpdated(placePos, state, state, Block.UPDATE_ALL);

            // Clear carrier state
            carrier.grabandgo$clearCarried();

            // Sound
            world.playSound(null, placePos.getX(), placePos.getY(), placePos.getZ(),
                    state.getSoundType().getPlaceSound(), SoundSource.BLOCKS, 1.0F, 1.0F);

            return InteractionResult.SUCCESS;
        } catch (Exception e) {
            LOGGER.error("Failed to safely restore carried block at {}", placePos, e);
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§cFailed to place block safely! Recovery dropped as item."));
            
            // Critical Recovery: drop block item with NBT to avoid content deletion
            try {
                Identifier blockId = Identifier.tryParse(blockIdStr);
                Block block = BuiltInRegistries.BLOCK.getValue(blockId);
                ItemStack stack = new ItemStack(block);
                BlockEntityType<?> blockEntityType = BuiltInRegistries.BLOCK_ENTITY_TYPE.getValue(Identifier.tryParse(blockTypeStr));
                if (blockEntityType != null &&carriedData.contains("BlockEntityData")) {
                    // ใช้ Codec ในการสร้าง TypedEntityData จาก CompoundTag
                    DynamicOps<Tag> ops = NbtOps.INSTANCE;
                    DataResult<TypedEntityData<BlockEntityType<?>>> result = TypedEntityData.codec(BuiltInRegistries.BLOCK_ENTITY_TYPE.byNameCodec())
                            .parse(ops, carriedData.getCompoundOrEmpty("BlockEntityData"));
                    if (result.isSuccess()) {
                        TypedEntityData<BlockEntityType<?>> typedData = result.getOrThrow();
                        stack.set(DataComponents.BLOCK_ENTITY_DATA, typedData);
                    }
                }
                ItemEntity itemEntity = new ItemEntity(world, 
                        placePos.getX() + 0.5, placePos.getY() + 0.5, placePos.getZ() + 0.5, stack);
                world.addFreshEntity(itemEntity);
                carrier.grabandgo$clearCarried();
            } catch (Exception recoveryEx) {
                LOGGER.error("Critical recovery failed!", recoveryEx);
            }
            return InteractionResult.FAIL;
        }
    }

    private static InteractionResult placeEntity(Player player, Level world, BlockPos placePos, CompoundTag carriedData) {
        if (world.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        GrabCarrier carrier = (GrabCarrier) player;
        String entityTypeIdStr = carriedData.getStringOr("EntityTypeId", "");

        try {
            CompoundTag entityNbt = carriedData.getCompoundOrEmpty("EntityData");
            
            // Modern 1.26.2 EntityType.create from NBT utilizes ValueInput and EntitySpawnReason
            ValueInput valueInput = TagValueInput.create(new ProblemReporter.Collector(), world.registryAccess(), entityNbt);
            EntitySpawnRequest request = new EntitySpawnRequest(EntitySpawnReason.LOAD, false);
            Optional<Entity> entityOpt = EntityType.create(valueInput, world, request);

            if (entityOpt.isPresent()) {
                Entity entity = entityOpt.get();
                // Position entity slightly above ground center
                entity.setPos(placePos.getX() + 0.5, placePos.getY() + 0.05, placePos.getZ() + 0.5);
                
                // Spawn back
                world.addFreshEntity(entity);
                carrier.grabandgo$clearCarried();

                // Sound
                world.playSound(null, placePos.getX(), placePos.getY(), placePos.getZ(),
                        SoundEvents.CHICKEN_EGG, SoundSource.NEUTRAL, 1.0F, 0.7F);

                return InteractionResult.SUCCESS;
            } else {
                throw new IllegalStateException("EntityType.create returned empty");
            }
        } catch (Exception e) {
            LOGGER.error("Failed to safely restore entity at {}", placePos, e);
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§cFailed to place entity! Force spawning default variant."));

            // Fallback: spawn default entity type if custom deserialization failed
            try {
                Identifier typeId = Identifier.tryParse(entityTypeIdStr);
                EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getValue(typeId);
                Entity entity = type.create(world, new EntitySpawnRequest(EntitySpawnReason.LOAD, false));
                if (entity != null) {
                    entity.setPos(placePos.getX() + 0.5, placePos.getY() + 0.05, placePos.getZ() + 0.5);
                    world.addFreshEntity(entity);
                }
                carrier.grabandgo$clearCarried();
            } catch (Exception fallbackEx) {
                LOGGER.error("Critical fallback spawning failed!", fallbackEx);
            }
            return InteractionResult.FAIL;
        }
    }

    public static void dropCarriedObject(Player player) {
        GrabCarrier carrier = (GrabCarrier) player;
        if (!carrier.grabandgo$isCarrying()) return;

        Level world = player.level();
        if (world.isClientSide()) return;

        BlockPos placePos = player.blockPosition();
        // Adjust pos if block is not replaceable
        if (!world.getWorldBorder().isWithinBounds(placePos)) {
            return;
        }
        if (!world.getBlockState(placePos).canBeReplaced()) {
            placePos = placePos.above();
        }

        CompoundTag carriedData = carrier.grabandgo$getCarriedData();
        if (carriedData == null || carriedData.isEmpty()) {
            carrier.grabandgo$clearCarried();
            return;
        }

        String type = carriedData.getStringOr("Type", "");
        if ("block".equals(type)) {
            placeBlock(player, world, placePos, carriedData);
        } else if ("entity".equals(type)) {
            placeEntity(player, world, placePos, carriedData);
        }
    }
}
