package com.teamaonn.silkspawners.mixin;

import java.util.stream.Stream;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EnchantmentMenu.class)
public abstract class EnchantmentMenuMixin {
    @Unique private boolean teamaonn$allowTrials;
    @Inject(method = "getEnchantmentList", at = @At("HEAD"))
    private void teamaonn$checkOption(RegistryAccess registries, ItemStack stack, int option, int cost,
                                    CallbackInfoReturnable<java.util.List<EnchantmentInstance>> cir) {
        teamaonn$allowTrials = option == 2 && cost == 30 && (stack.is(ItemTags.PICKAXES) || stack.is(Items.BOOK));
    }
    @Redirect(method = "getEnchantmentList", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/enchantment/EnchantmentHelper;selectEnchantment(Lnet/minecraft/util/RandomSource;Lnet/minecraft/world/item/ItemStack;ILjava/util/stream/Stream;)Ljava/util/List;"))
    private java.util.List<EnchantmentInstance> teamaonn$filterBeforeSelection(RandomSource random, ItemStack stack, int power, Stream<Holder<Enchantment>> choices) {
        Identifier trials = Identifier.parse("teamaonn_silkspawners:the_trials");
        return EnchantmentHelper.selectEnchantment(random, stack, power,
                choices.filter(holder -> teamaonn$allowTrials || !holder.unwrapKey().map(key -> key.identifier().equals(trials)).orElse(false)));
    }
}
