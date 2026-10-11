package com.teamaonn.silkspawners.mixin;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Captures the mob ID before vanilla consumes the item, then restores it on placement. */
@Mixin(BlockItem.class)
public abstract class SpawnerPlacementMixin {
    @Unique
    private static final ThreadLocal<String> teamaonn$pendingMobId = new ThreadLocal<>();

    @Inject(method = "place", at = @At("HEAD"))
    private void teamaonn$captureSpawner(BlockPlaceContext context, CallbackInfoReturnable<InteractionResult> cir) {
        teamaonn$pendingMobId.remove();
        if (!context.getItemInHand().is(Items.SPAWNER)) return;
        var data = context.getItemInHand().get(DataComponents.CUSTOM_DATA);
        if (data == null) return;
        String mobId = data.copyTag().getString("mob_id").orElse("");
        if (!mobId.isEmpty()) teamaonn$pendingMobId.set(mobId);
    }

    @Inject(method = "place", at = @At("RETURN"))
    private void teamaonn$restoreSpawner(BlockPlaceContext context, CallbackInfoReturnable<InteractionResult> cir) {
        String mobId = teamaonn$pendingMobId.get();
        teamaonn$pendingMobId.remove();
        if (mobId == null || !cir.getReturnValue().consumesAction()) return;
        var level = context.getLevel();
        if (level.isClientSide()) return;
        var pos = context.getClickedPos();
        if (!level.getBlockState(pos).is(Blocks.SPAWNER)) return;
        if (!(level.getBlockEntity(pos) instanceof SpawnerBlockEntity spawner)) return;
        BuiltInRegistries.ENTITY_TYPE.getOptional(Identifier.parse(mobId)).ifPresent(type -> {
            spawner.getSpawner().setEntityId(type, level, level.getRandom(), pos);
            spawner.setChanged();
        });
    }
}
