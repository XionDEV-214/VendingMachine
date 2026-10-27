package com.eisiadev.vendingmachine.items;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import org.jetbrains.annotations.Nullable;

import java.util.List;

public class PlaceholderItem extends Item {

    public static final String ORIGINAL_ID = "orig_id";

    public PlaceholderItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        if (stack.getTag() == null || !stack.getTag().contains(ORIGINAL_ID)) {
            tooltip.add(Component.translatable("item.vendingmachine.placeholder.missing").withStyle(ChatFormatting.RED));
            return;
        }
        tooltip.add(Component.translatable("item.vendingmachine.placeholder.original",
            stack.getTag().getString(ORIGINAL_ID)).withStyle(ChatFormatting.GRAY));
    }
}
