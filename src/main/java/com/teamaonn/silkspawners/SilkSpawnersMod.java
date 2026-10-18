package com.teamaonn.silkspawners;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;

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
    private static final net.minecraft.resources.Identifier TRIALS_ID = net.minecraft.resources.Identifier.parse("teamaonn_silkspawners:the_trials");

    private static ItemStack makeSpawnerDrop(ServerLevel level, BlockState state, BlockEntity blockEntity) {
        if (blockEntity == null) return ItemStack.EMPTY;
        CompoundTag original = blockEntity.saveWithoutMetadata(level.registryAccess());
        CompoundTag spawnData = state.is(Blocks.TRIAL_SPAWNER)
            ? original.getCompound("spawn_data").orElse(new CompoundTag())
            : original.getCompound("SpawnData").orElse(new CompoundTag());
        CompoundTag entity = spawnData.getCompound("entity").orElse(new CompoundTag());
        String entityId = entity.getString("id").orElse("");
        if (entityId.isEmpty()) return ItemStack.EMPTY;
        CompoundTag spawnerData = new CompoundTag();
        spawnerData.putString("mob_id", entityId);
        ItemStack drop = new ItemStack(Items.SPAWNER);
        drop.set(DataComponents.CUSTOM_DATA, CustomData.of(spawnerData));
        String mobName = entityId.substring(entityId.indexOf(':') + 1).replace('_', ' ');
        StringBuilder label = new StringBuilder();
        for (String word : mobName.split(" ")) {
            if (!word.isEmpty()) label.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1)).append(' ');
        }
        drop.set(DataComponents.CUSTOM_NAME, Component.literal(label.toString().trim() + " Spawner"));
        return drop;
    }

    private static boolean hasSilkTouch(ServerLevel level, ItemStack tool) {
        var silk = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SILK_TOUCH);
        return EnchantmentHelper.getItemEnchantmentLevel(silk, tool) > 0;
    }

    private static boolean hasTrials(ServerLevel level, ItemStack tool) {
        var entry = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).get(net.minecraft.resources.ResourceKey.create(Registries.ENCHANTMENT, TRIALS_ID));
        return entry.isPresent() && EnchantmentHelper.getItemEnchantmentLevel(entry.get(), tool) > 0;
    }

    @Override
    public void onInitialize() {
        UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
            if (hand != InteractionHand.MAIN_HAND || !(level instanceof ServerLevel serverLevel)) return InteractionResult.PASS;
            BlockPos pos = hit.getBlockPos();
            BlockState state = level.getBlockState(pos);
            if (!state.is(Blocks.SPAWNER) && !state.is(Blocks.TRIAL_SPAWNER)) return InteractionResult.PASS;
            if (player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > 4.0) return InteractionResult.PASS;
            ItemStack tool = player.getMainHandItem();
            if (!tool.is(Items.NETHERITE_PICKAXE) || !hasSilkTouch(serverLevel, tool) || !hasTrials(serverLevel, tool)) return InteractionResult.PASS;
            ItemStack drop = makeSpawnerDrop(serverLevel, state, level.getBlockEntity(pos));
            if (drop.isEmpty()) return InteractionResult.PASS;
            if (!level.destroyBlock(pos, false, player)) return InteractionResult.PASS;
            Block.popResource(serverLevel, pos, drop);
            return InteractionResult.SUCCESS;
        });
        PlayerBlockBreakEvents.AFTER.register((level, player, pos, state, blockEntity) -> {
            if (!(level instanceof ServerLevel serverLevel)) return;
            if (!state.is(Blocks.SPAWNER) && !state.is(Blocks.TRIAL_SPAWNER)) return;
            ItemStack tool = player.getMainHandItem();
            if (!tool.is(Items.NETHERITE_PICKAXE)) return;

            if (!hasSilkTouch(serverLevel, tool)) return;
            ItemStack drop = makeSpawnerDrop(serverLevel, state, blockEntity);
            if (drop.isEmpty()) return;
            Block.popResource(serverLevel, pos, drop);
        });
    }
}
