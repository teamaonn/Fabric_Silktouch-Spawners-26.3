package com.teamaonn.silkspawners.mixin;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionResult;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Restores only the stored mob ID after vanilla spawner placement. */
@Mixin(BlockItem.class)
public abstract class SpawnerPlacementMixin {
    @Inject(method = "place", at = @At("RETURN"))
    private void teamaonn$restoreSpawner(BlockPlaceContext context, CallbackInfoReturnable<InteractionResult> cir) {
        if (!cir.getReturnValue().consumesAction()) return;
        if (!context.getItemInHand().is(net.minecraft.world.item.Items.SPAWNER)) return;
        if (context.getLevel().isClientSide()) return;
        var data = context.getItemInHand().get(DataComponents.CUSTOM_DATA);
        if (data == null) return;
        String mobId = data.copyTag().getString("mob_id").orElse("");
        if (mobId.isEmpty()) return;
        var pos = context.getClickedPos();
        var level = context.getLevel();
        if (!level.getBlockState(pos).is(Blocks.SPAWNER)) return;
        if (!(level.getBlockEntity(pos) instanceof SpawnerBlockEntity spawner)) return;
        BuiltInRegistries.ENTITY_TYPE.getOptional(ResourceLocation.parse(mobId)).ifPresent(type -> {
            spawner.getSpawner().setEntityId(type, level, level.getRandom(), pos);
            spawner.setChanged();
        });
    }
}
