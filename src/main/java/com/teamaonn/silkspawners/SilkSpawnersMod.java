package com.teamaonn.silkspawners;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.TypedEntityData;
import net.minecraft.world.level.block.entity.BlockEntityTypes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.server.level.ServerLevel;

/**
 * Only feature: Netherite + Silk Touch converts mined normal/trial spawners
 * into a normal spawner item with the same entity type.
 */
public final class SilkSpawnersMod implements ModInitializer {
    @Override
    public void onInitialize() {
        PlayerBlockBreakEvents.AFTER.register((level, player, pos, state, blockEntity) -> {
            if (!(level instanceof ServerLevel serverLevel)) return;
            if (!state.is(Blocks.SPAWNER) && !state.is(Blocks.TRIAL_SPAWNER)) return;
            ItemStack tool = player.getMainHandItem();
            if (!tool.is(Items.NETHERITE_PICKAXE)) return;

            var silkTouch = serverLevel.registryAccess()
                .lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SILK_TOUCH);
            if (EnchantmentHelper.getItemEnchantmentLevel(silkTouch, tool) < 1) return;
            if (blockEntity == null) return;

            CompoundTag original = blockEntity.saveWithoutMetadata(serverLevel.registryAccess());
            // Ordinary spawners use SpawnData; trial spawners use spawn_data.
            CompoundTag spawnData = state.is(Blocks.TRIAL_SPAWNER)
                ? original.getCompound("spawn_data").orElse(new CompoundTag())
                : original.getCompound("SpawnData").orElse(new CompoundTag());
            CompoundTag entity = spawnData.getCompound("entity").orElse(new CompoundTag());
            String entityId = entity.getString("id").orElse("");
            if (entityId.isEmpty()) return;

            // Deliberately omit trial-only configuration, rewards and cooldowns.
            CompoundTag newSpawnData = new CompoundTag();
            CompoundTag newEntity = new CompoundTag();
            newEntity.putString("id", entityId);
            newSpawnData.put("entity", newEntity);
            CompoundTag spawnerData = new CompoundTag();
            spawnerData.putString("id", "minecraft:mob_spawner");
            spawnerData.put("SpawnData", newSpawnData);

            ItemStack drop = new ItemStack(Items.SPAWNER);
            drop.set(DataComponents.BLOCK_ENTITY_DATA, TypedEntityData.of(BlockEntityTypes.MOB_SPAWNER, spawnerData));
            Block.popResource(serverLevel, pos, drop);
        });
    }
}
