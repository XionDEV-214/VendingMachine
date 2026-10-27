package com.eisiadev.vendingmachine.trade;

import com.eisiadev.vendingmachine.VendingMachine;
import com.eisiadev.vendingmachine.registry.VMItems;

import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class ItemIngredient {

    @Nullable
    private final String itemId;
    @Nullable
    private final String tagId;
    @Nullable
    private final CompoundTag nbt;
    private final long count;

    private Item resolvedItem;
    private TagKey<Item> resolvedTag;
    private boolean resolved;

    private ItemIngredient(@Nullable String itemId, @Nullable String tagId, @Nullable CompoundTag nbt, long count) {
        this.itemId = itemId;
        this.tagId = tagId;
        this.nbt = nbt;
        this.count = count;
    }

    public static ItemIngredient ofItem(String itemId, long count, @Nullable CompoundTag nbt) {
        return new ItemIngredient(itemId, null, nbt, count);
    }

    public static ItemIngredient ofStack(ItemStack stack) {
        ResourceLocation key = ForgeRegistries.ITEMS.getKey(stack.getItem());
        return new ItemIngredient(
            key == null ? "minecraft:air" : key.toString(),
            null,
            stack.hasTag() ? stack.getTag().copy() : null,
            stack.getCount());
    }

    public static ItemIngredient ofTag(String tagId, long count) {
        return new ItemIngredient(null, tagId, null, count);
    }

    public ItemIngredient withCount(long newCount) {
        return new ItemIngredient(itemId, tagId, nbt, newCount);
    }

    private void resolve() {
        if (resolved) return;
        resolved = true;
        if (itemId != null) {
            ResourceLocation rl = ResourceLocation.tryParse(itemId);
            Item item = rl == null ? null : ForgeRegistries.ITEMS.getValue(rl);
            if (item != null && (ForgeRegistries.ITEMS.containsKey(rl))) {
                resolvedItem = item;
            }
        }
        if (tagId != null) {
            ResourceLocation rl = ResourceLocation.tryParse(tagId.startsWith("#") ? tagId.substring(1) : tagId);
            if (rl != null) resolvedTag = TagKey.create(Registries.ITEM, rl);
        }
    }

    public boolean isTag() {
        return tagId != null;
    }

    public boolean isValid() {
        resolve();
        return isTag() ? resolvedTag != null : resolvedItem != null;
    }

    public long getCount() {
        return count;
    }

    @Nullable
    public String getItemId() {
        return itemId;
    }

    @Nullable
    public String getTagId() {
        return tagId;
    }

    @Nullable
    public TagKey<Item> getTag() {
        resolve();
        return resolvedTag;
    }

    public boolean matches(ItemStack candidate) {
        if (candidate.isEmpty()) return false;
        resolve();
        if (isTag()) {
            return resolvedTag != null && candidate.is(resolvedTag);
        }
        if (resolvedItem == null || !candidate.is(resolvedItem)) return false;
        return nbt == null || NbtUtils.compareNbt(nbt, candidate.getTag(), true);
    }

    public ItemStack getDisplayStack() {
        resolve();
        ItemStack stack;
        if (isTag()) {
            stack = resolvedTag == null ? ItemStack.EMPTY : ForgeRegistries.ITEMS.tags() == null ? ItemStack.EMPTY :
                ForgeRegistries.ITEMS.tags().getTag(resolvedTag).stream().findFirst().map(ItemStack::new)
                    .orElse(ItemStack.EMPTY);
        } else {
            stack = resolvedItem == null ? ItemStack.EMPTY : new ItemStack(resolvedItem);
            if (!stack.isEmpty() && nbt != null) stack.setTag(nbt.copy());
        }
        if (stack.isEmpty()) {
            stack = VMItems.placeholderFor(isTag() ? "#" + tagId : itemId);
        }
        return stack;
    }

    public ItemStack getDisplayStackWithCount() {
        ItemStack stack = getDisplayStack().copy();
        stack.setCount((int) Math.min(Integer.MAX_VALUE, count));
        return stack;
    }

    public List<ItemStack> createStacks() {
        List<ItemStack> out = new ArrayList<>();
        ItemStack base = getDisplayStack();
        if (base.isEmpty()) return out;
        long remaining = Math.max(1, count);
        int max = Math.max(1, base.getMaxStackSize());
        while (remaining > 0) {
            int size = (int) Math.min(remaining, max);
            ItemStack s = base.copy();
            s.setCount(size);
            out.add(s);
            remaining -= size;
        }
        return out;
    }

    public static ItemIngredient fromJson(JsonObject json) {
        long count = json.has("count") ? json.get("count").getAsLong() : 1;
        if (count < 0) throw new JsonParseException("count must not be negative");
        if (json.has("tag")) {
            String tag = json.get("tag").getAsString();
            return ofTag(tag.startsWith("#") ? tag.substring(1) : tag, count);
        }
        String id;
        if (json.has("item")) id = json.get("item").getAsString();
        else if (json.has("id")) id = json.get("id").getAsString();
        else throw new JsonParseException("item entry needs \"item\" or \"tag\"");
        if (id.startsWith("#")) return ofTag(id.substring(1), count);
        CompoundTag nbt = null;
        if (json.has("nbt")) {
            try {
                nbt = json.get("nbt").isJsonObject() ? TagParser.parseTag(json.get("nbt").toString()) :
                    TagParser.parseTag(json.get("nbt").getAsString());
            } catch (CommandSyntaxException e) {
                VendingMachine.LOG.error("Invalid nbt for {}: {}", id, e.getMessage());
            }
        }
        return ofItem(id, count, nbt);
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        if (isTag()) {
            json.addProperty("tag", tagId);
        } else {
            json.addProperty("item", itemId);
        }
        json.addProperty("count", count);
        if (nbt != null) json.addProperty("nbt", nbt.toString());
        return json;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ItemIngredient that)) return false;
        return count == that.count && Objects.equals(itemId, that.itemId) && Objects.equals(tagId, that.tagId) &&
            Objects.equals(nbt, that.nbt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(itemId, tagId, nbt, count);
    }

    @Override
    public String toString() {
        return count + "x " + (isTag() ? "#" + tagId : itemId);
    }
}
