package com.eisiadev.vendingmachine.machine;

import com.eisiadev.vendingmachine.VMConfig;
import com.eisiadev.vendingmachine.blocks.VendingMachineCasingBlock;
import com.eisiadev.vendingmachine.currency.CurrencyAmount;
import com.eisiadev.vendingmachine.currency.CurrencyDefinition;
import com.eisiadev.vendingmachine.currency.CurrencyRegistry;
import com.eisiadev.vendingmachine.gui.VendingGui;
import com.eisiadev.vendingmachine.gui.VendingUIFactory;
import com.eisiadev.vendingmachine.network.TradeStatusPacket;
import com.eisiadev.vendingmachine.network.VMNetwork;
import com.eisiadev.vendingmachine.registry.VMBlocks;
import com.eisiadev.vendingmachine.registry.VMSounds;
import com.eisiadev.vendingmachine.storage.Wallet;
import com.eisiadev.vendingmachine.trade.ItemIngredient;
import com.eisiadev.vendingmachine.trade.Trade;
import com.eisiadev.vendingmachine.trade.TradeDatabase;
import com.eisiadev.vendingmachine.trade.TradeGroup;
import com.eisiadev.vendingmachine.trade.TradeManager;

import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.TickableSubscription;
import com.gregtechceu.gtceu.api.machine.feature.IMachineLife;
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine;

import brachy.modularui.api.IUIHolder;
import brachy.modularui.factory.PosGuiData;
import brachy.modularui.screen.ModularPanel;
import brachy.modularui.screen.ModularScreen;
import brachy.modularui.screen.UISettings;
import brachy.modularui.value.sync.PanelSyncManager;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.items.ItemStackHandler;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class VendingMachineController extends MultiblockControllerMachine
                                      implements IUIHolder<PosGuiData>, IMachineLife {

    public static final int INPUT_SLOTS = 8;
    public static final int OUTPUT_SLOTS = 100;

    public static final int MAX_PURCHASE_QUANTITY = 999_999;

    private static final int BASE_DISPENSE_DELAY = 10;
    private static final String DATA_KEY = "vendingmachine";

    public final ItemStackHandler inputItems = new ItemStackHandler(INPUT_SLOTS) {

        @Override
        protected void onContentsChanged(int slot) {
            onInputChanged();
        }
    };
    public final ItemStackHandler outputItems = new ItemStackHandler(OUTPUT_SLOTS) {

        @Override
        protected void onContentsChanged(int slot) {
            markDirty();
        }
    };

    private final Deque<PendingOutput> outputBuffer = new ArrayDeque<>();
    private final Queue<TradeRequest> pendingTrades = new ConcurrentLinkedQueue<>();

    @Nullable
    private TickableSubscription tickSubscription;
    @Nullable
    private ServerPlayer currentUser;

    private boolean newBufferedOutputs = false;
    private boolean updatingArtwork = false;
    private boolean inputsChanged = false;
    private boolean syncTrades = false;
    private int ticksSinceOutput = 0;
    private int ticksSinceTradeUpdate = 0;

    public VendingMachineController(IMachineBlockEntity holder) {
        super(holder);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (!isRemote()) {
            tickSubscription = subscribeServerTick(this::tickVending);
        }
    }

    @Override
    public void onUnload() {
        super.onUnload();
        unsubscribe(tickSubscription);
        tickSubscription = null;
    }

    @Override
    public void onStructureFormed() {
        super.onStructureFormed();
        updateFrontArtwork(true);
    }

    @Override
    public void onStructureInvalid() {
        super.onStructureInvalid();
        updateFrontArtwork(false);
    }

    private void updateFrontArtwork(boolean formed) {
        if (updatingArtwork || !(getLevel() instanceof ServerLevel level)) return;
        updatingArtwork = true;
        try {
            if (!getBlockState().hasProperty(BlockStateProperties.HORIZONTAL_FACING)) return;
            Direction front = getFrontFacing();
            BlockPos pos = getPos();
            Direction viewerLeft = front.getClockWise();
            Direction viewerRight = front.getCounterClockWise();

            if (!formed) {
                for (Direction side : new Direction[] { viewerLeft, viewerRight }) {
                    for (int dy = -1; dy <= 1; dy++) clearArtwork(level, pos.relative(side).above(dy), front);
                }
                clearArtwork(level, pos.above(), front);
                clearArtwork(level, pos.below(), front);
                return;
            }

            Direction posterSide;
            if (isCasing(level, pos.relative(viewerLeft))) posterSide = viewerLeft;
            else if (isCasing(level, pos.relative(viewerRight))) posterSide = viewerRight;
            else return;
            boolean mirrored = posterSide == viewerRight;
            BlockPos poster = pos.relative(posterSide);
            setArtwork(level, poster.above(), VendingMachineCasingBlock.Part.POSTER_TOP, front, mirrored);
            setArtwork(level, poster, VendingMachineCasingBlock.Part.POSTER_MIDDLE, front, mirrored);
            setArtwork(level, poster.below(), VendingMachineCasingBlock.Part.POSTER_BOTTOM, front, mirrored);
            setArtwork(level, pos.above(), VendingMachineCasingBlock.Part.PANEL_TOP, front, mirrored);
            setArtwork(level, pos.below(), VendingMachineCasingBlock.Part.PANEL_BOTTOM, front, mirrored);
        } finally {
            updatingArtwork = false;
        }
    }

    private static boolean isCasing(Level level, BlockPos pos) {
        return level.getBlockState(pos).is(VMBlocks.CASING.get());
    }

    private static void setArtwork(Level level, BlockPos pos, VendingMachineCasingBlock.Part part, Direction front,
                                   boolean mirrored) {
        BlockState state = level.getBlockState(pos);
        if (!state.is(VMBlocks.CASING.get())) return;
        BlockState target = VMBlocks.CASING.get().withOverlay(part, front, mirrored);
        if (state != target) level.setBlock(pos, target, Block.UPDATE_CLIENTS);
    }

    private static void clearArtwork(Level level, BlockPos pos, Direction front) {
        BlockState state = level.getBlockState(pos);
        if (!state.is(VMBlocks.CASING.get())) return;
        if (state.getValue(VendingMachineCasingBlock.PART) == VendingMachineCasingBlock.Part.NONE) return;
        if (state.getValue(VendingMachineCasingBlock.FACING) != front) return;
        level.setBlock(pos, VMBlocks.CASING.get().defaultBlockState(), Block.UPDATE_CLIENTS);
    }

    @Override
    public void onMachineRemoved() {
        updateFrontArtwork(false);
        clearInventory(inputItems);
        clearInventory(outputItems);
        Level level = getLevel();
        if (level != null) {
            for (PendingOutput out : outputBuffer) {
                long remaining = out.remaining;
                int max = out.maxStackSize();
                while (remaining > 0) {
                    int size = (int) Math.min(remaining, max);
                    Block.popResource(level, getPos(), out.template.copyWithCount(size));
                    remaining -= size;
                }
            }
        }
        outputBuffer.clear();
    }

    @Override
    public InteractionResult onUse(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand,
                                   BlockHitResult hit) {
        InteractionResult superResult = super.onUse(state, world, pos, player, hand, hit);
        if (superResult != InteractionResult.PASS) return superResult;

        if (world.isClientSide) return InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.PASS;

        if (currentUser != null && currentUser != serverPlayer && !currentUser.hasDisconnected() &&
            currentUser.containerMenu != currentUser.inventoryMenu) {
            serverPlayer.displayClientMessage(Component.translatable("vendingmachine.gui.error.player_using"), true);
            return InteractionResult.CONSUME;
        }
        currentUser = serverPlayer;
        ticksSinceTradeUpdate = VMConfig.GUI_REFRESH_INTERVAL.get();
        VendingUIFactory.INSTANCE.open(serverPlayer, this);
        return InteractionResult.CONSUME;
    }

    @Nullable
    public ServerPlayer getCurrentUser() {
        return currentUser;
    }

    public void resetCurrentUser(Player player) {
        if (currentUser == player) currentUser = null;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public ModularScreen createScreen(PosGuiData data, ModularPanel<?> mainPanel) {
        return VendingGui.createScreen(mainPanel);
    }

    @Override
    public ModularPanel<?> buildUI(PosGuiData data, PanelSyncManager syncManager, UISettings settings) {
        return new VendingGui(this, data, syncManager).build(settings);
    }

    private void tickVending() {
        if (currentUser != null && (currentUser.hasDisconnected() || currentUser.isRemoved())) {
            currentUser = null;
        }
        if (inputsChanged) {
            inputsChanged = false;
            depositCoinsFromInputs();
            syncTrades = true;
        }
        if (!isFormed()) return;

        TradeRequest request = pendingTrades.poll();
        if (request != null) processTrade(request);

        if (newBufferedOutputs || (!outputBuffer.isEmpty() && ticksSinceOutput % getDispenseDelay() == 0)) {
            dispenseNext();
            ticksSinceOutput = 0;
        }
        ticksSinceOutput = newBufferedOutputs ? 0 : ticksSinceOutput + 1;
        newBufferedOutputs = false;

        if (currentUser != null &&
            (syncTrades || ++ticksSinceTradeUpdate >= VMConfig.GUI_REFRESH_INTERVAL.get())) {
            sendTradeStatus();
            syncTrades = false;
        }
    }

    private void onInputChanged() {
        markDirty();
        if (!isRemote()) inputsChanged = true;
    }

    public void requestTradeSync() {
        syncTrades = true;
    }

    private void depositCoinsFromInputs() {
        if (currentUser == null) return;
        Wallet wallet = TradeManager.getWallet(currentUser.getUUID());
        if (wallet == null) return;
        boolean deposited = false;
        for (int i = 0; i < INPUT_SLOTS; i++) {
            ItemStack stack = inputItems.getStackInSlot(i);
            CurrencyRegistry.Match match = CurrencyRegistry.server().match(stack);
            if (match == null) continue;
            long value = Wallet.checkedMultiply(match.tier().getValue(), stack.getCount());
            if (value < 0 || !wallet.tryDeposit(match.currency().getId(), value)) continue;
            inputItems.setStackInSlot(i, ItemStack.EMPTY);
            deposited = true;
        }
        if (deposited) {
            TradeManager.markDirty();
            playSound(VMSounds.COIN_INSERT.get());
        }
    }

    public void depositHeldCoins(ServerPlayer player) {
        ItemStack held = player.getMainHandItem();
        CurrencyRegistry.Match match = CurrencyRegistry.server().match(held);
        if (match == null) return;
        Wallet wallet = TradeManager.getWallet(player.getUUID());
        if (wallet == null) return;
        long value = Wallet.checkedMultiply(match.tier().getValue(), held.getCount());
        if (value < 0 || !wallet.tryDeposit(match.currency().getId(), value)) return;
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        TradeManager.markDirty();
        playSound(VMSounds.COIN_INSERT.get());
        syncTrades = true;
    }

    public void ejectCoins(ServerPlayer player, @Nullable String currencyId) {
        if (!isFormed()) return;
        Wallet wallet = TradeManager.getWallet(player.getUUID());
        if (wallet == null) return;
        List<ItemStack> coins = new ArrayList<>();
        int room = (int) Math.max(0, OUTPUT_SLOTS - bufferedStackCount());
        for (CurrencyDefinition def : CurrencyRegistry.server().all()) {
            if (room <= 0) break;
            if (currencyId != null && !currencyId.equals(def.getId())) continue;
            long amount = wallet.get(def.getId());
            if (amount <= 0) continue;
            CurrencyDefinition.Itemized out = def.itemize(amount, room);
            if (out.stacks().isEmpty()) continue;
            coins.addAll(out.stacks());
            room -= out.stacks().size();
            wallet.set(def.getId(), amount - out.value());
        }
        if (coins.isEmpty()) return;
        TradeManager.markDirty();
        dispense(coins);
        syncTrades = true;
    }

    public void ejectInputItems() {
        if (!isFormed()) return;
        List<ItemStack> items = new ArrayList<>();
        for (int i = 0; i < INPUT_SLOTS; i++) {
            ItemStack stack = inputItems.getStackInSlot(i);
            if (stack.isEmpty()) continue;
            items.add(stack.copy());
            inputItems.setStackInSlot(i, ItemStack.EMPTY);
        }
        dispense(items);
    }

    public void collectOutputs(Player player) {
        for (int i = 0; i < OUTPUT_SLOTS; i++) {
            ItemStack stack = outputItems.getStackInSlot(i);
            if (stack.isEmpty()) continue;
            ItemStack toAdd = stack.copy();
            boolean fully = player.getInventory().add(toAdd);
            outputItems.setStackInSlot(i, toAdd.isEmpty() ? ItemStack.EMPTY : toAdd);
            if (!fully) break;
        }
    }

    public void queueTrade(ServerPlayer player, UUID tradeGroup, int order) {
        queueTrade(player, tradeGroup, order, 1);
    }

    public void queueTrade(ServerPlayer player, UUID tradeGroup, int order, int quantity) {
        pendingTrades.add(new TradeRequest(player, tradeGroup, order, clampQuantity(quantity)));
    }

    public static int clampQuantity(long quantity) {
        return (int) Math.max(1, Math.min(MAX_PURCHASE_QUANTITY, quantity));
    }

    private void processTrade(TradeRequest request) {
        ServerPlayer player = request.player();
        UUID playerId = player.getUUID();
        TradeGroup tg = TradeDatabase.server().getTradeGroup(request.tradeGroup());
        if (tg == null || request.order() < 0 || request.order() >= tg.getTrades().size()) return;
        int quantity = clampQuantity(request.quantity());
        if (!TradeManager.canExecuteTrade(playerId, tg) ||
            quantity > TradeManager.getRemainingExecutions(playerId, tg)) {
            rejectPurchase(player, quantity);
            return;
        }
        Trade trade = tg.getTrades().get(request.order());
        if (!checkTrade(trade, playerId, quantity, false)) {
            rejectPurchase(player, quantity);
            return;
        }

        for (ItemIngredient out : trade.toItems) {
            ItemStack base = out.getDisplayStack();

            long perUnit = Math.max(1, out.getCount());
            long total = Wallet.checkedMultiply(perUnit, quantity);
            enqueueOutput(base, total < 0 ? Long.MAX_VALUE : total);
        }
        newBufferedOutputs = true;
        TradeManager.executeTrade(playerId, tg, quantity);
        syncTrades = true;
        markDirty();
    }

    private void rejectPurchase(ServerPlayer player, int quantity) {
        syncTrades = true;
        if (quantity > 1) {
            player.displayClientMessage(
                Component.translatable("vendingmachine.gui.error.bulk_purchase_failed", quantity), true);
        }
    }

    public boolean checkTrade(Trade trade, UUID player, boolean simulate) {
        return checkTrade(trade, player, 1, simulate);
    }

    public boolean checkTrade(Trade trade, UUID player, int quantity, boolean simulate) {
        if (quantity < 1) return false;
        List<CurrencyAmount> price = scalePrice(trade.fromCurrency, quantity);
        if (price == null) return false;

        ItemStack[] slots = new ItemStack[INPUT_SLOTS];
        for (int i = 0; i < INPUT_SLOTS; i++) slots[i] = inputItems.getStackInSlot(i).copy();

        List<ItemIngredient> missingNc = removeItems(slots, trade.nonConsumedItems, 1, true);
        List<ItemIngredient> missing = removeItems(slots, trade.fromItems, quantity, false);

        Wallet wallet = TradeManager.getWallet(player);
        List<CurrencyAmount> unpaid;
        Wallet after = null;
        if (wallet == null) {
            unpaid = price;
        } else {
            after = Wallet.copyOf(wallet);
            unpaid = after.pay(price);
        }

        boolean success = missingNc.isEmpty() && missing.isEmpty() && unpaid.stream().allMatch(c -> c.value() <= 0);
        if (success && !simulate) {
            if (wallet != null) {
                wallet.resetAll();
                after.getBalances().forEach(wallet::set);
                TradeManager.markDirty();
            }
            for (int i = 0; i < INPUT_SLOTS; i++) inputItems.setStackInSlot(i, slots[i]);
        }
        return success;
    }

    @Nullable
    private static List<CurrencyAmount> scalePrice(List<CurrencyAmount> price, int quantity) {
        if (quantity == 1) return price;
        List<CurrencyAmount> scaled = new ArrayList<>(price.size());
        for (CurrencyAmount ca : price) {
            long total = Wallet.checkedMultiply(ca.value(), quantity);
            if (total < 0) return null;
            scaled.add(new CurrencyAmount(ca.type(), total));
        }
        return scaled;
    }

    public int getMaxPurchasable(Trade trade, TradeGroup tg, UUID player) {
        if (!isFormed()) return 0;
        long cap = Math.min(MAX_PURCHASE_QUANTITY, TradeManager.getRemainingExecutions(player, tg));
        if (cap < 1) return 0;

        Wallet wallet = TradeManager.getWallet(player);
        for (CurrencyAmount ca : trade.fromCurrency) {
            if (ca.value() <= 0) continue;
            long have = wallet == null ? 0 : wallet.get(ca.type());
            cap = Math.min(cap, have / ca.value());
        }
        for (ItemIngredient ing : trade.fromItems) {
            if (ing.getCount() <= 0) continue;
            long have = 0;
            for (int i = 0; i < INPUT_SLOTS; i++) {
                ItemStack s = inputItems.getStackInSlot(i);
                if (!s.isEmpty() && ing.matches(s)) have += s.getCount();
            }
            cap = Math.min(cap, have / ing.getCount());
        }
        if (cap < 1 || !checkTrade(trade, player, 1, true)) return 0;

        int lo = 1;
        int hi = (int) cap;
        while (lo < hi) {
            int mid = lo + (hi - lo + 1) / 2;
            if (checkTrade(trade, player, mid, true)) lo = mid;
            else hi = mid - 1;
        }
        return lo;
    }

    private static List<ItemIngredient> removeItems(ItemStack[] slots, List<ItemIngredient> required,
                                                    int quantity, boolean simulate) {
        List<ItemIngredient> missing = new ArrayList<>();
        for (ItemIngredient ing : required) {
            long need = Wallet.checkedMultiply(ing.getCount(), quantity);
            if (need < 0) {
                missing.add(ing);
                continue;
            }
            for (int i = slots.length - 1; i >= 0 && need > 0; i--) {
                ItemStack s = slots[i];
                if (s.isEmpty() || !ing.matches(s)) continue;
                int take = (int) Math.min(need, s.getCount());
                need -= take;
                if (!simulate) s.shrink(take);
            }
            if (need > 0) missing.add(ing.withCount(need));
        }
        return missing;
    }

    private void sendTradeStatus() {
        ticksSinceTradeUpdate = 0;
        ServerPlayer player = currentUser;
        if (player == null) return;
        UUID id = player.getUUID();
        List<TradeStatusPacket.Entry> entries = new ArrayList<>();
        boolean formed = isFormed();
        for (TradeGroup tg : TradeDatabase.server().getTradeGroups()) {
            long cooldown = TradeManager.getCooldownRemaining(id, tg);
            boolean enabled = TradeManager.isEnabled(id, tg);
            int cdCount = TradeManager.getHistory(id, tg).cooldownTradeCount;
            for (int i = 0; i < tg.getTrades().size(); i++) {
                Trade trade = tg.getTrades().get(i);
                boolean tradeable = formed && checkTrade(trade, id, true);
                int maxQuantity = tradeable ? getMaxPurchasable(trade, tg, id) : 0;
                entries.add(new TradeStatusPacket.Entry(tg.getId(), i, cooldown, enabled, tradeable, cdCount,
                    maxQuantity));
            }
        }
        VMNetwork.sendTo(player, new TradeStatusPacket(entries, TradeManager.getTradesPerCooldown()));
    }

    public void dispense(List<ItemStack> stacks) {
        for (ItemStack s : stacks) {
            if (!s.isEmpty()) enqueueOutput(s, s.getCount());
        }
        newBufferedOutputs = true;
        markDirty();
    }

    private void enqueueOutput(ItemStack stack, long count) {
        if (stack.isEmpty() || count <= 0) return;
        PendingOutput last = outputBuffer.peekLast();
        if (last != null && ItemStack.isSameItemSameTags(last.template, stack)) {
            last.remaining = Wallet.saturatedAdd(last.remaining, count);
        } else {
            outputBuffer.add(new PendingOutput(stack.copyWithCount(1), count));
        }
    }

    private long bufferedStackCount() {
        long stacks = 0;
        for (PendingOutput out : outputBuffer) {
            int max = out.maxStackSize();
            stacks = Wallet.saturatedAdd(stacks, out.remaining / max + (out.remaining % max == 0 ? 0 : 1));
        }
        return stacks;
    }

    private int getDispenseDelay() {
        double acceleration = Math.log(bufferedStackCount());
        if (acceleration < 1) return BASE_DISPENSE_DELAY;
        return Math.max(1, (int) (BASE_DISPENSE_DELAY / acceleration));
    }

    private void dispenseNext() {
        PendingOutput next = outputBuffer.peek();
        if (next == null) return;
        if (next.remaining <= 0) {
            outputBuffer.poll();
            return;
        }
        for (int i = 0; i < OUTPUT_SLOTS; i++) {
            if (outputItems.getStackInSlot(i).isEmpty()) {
                int size = (int) Math.min(next.remaining, next.maxStackSize());
                ItemStack stack = next.template.copyWithCount(size);
                outputItems.setStackInSlot(i, stack);
                next.remaining -= size;
                if (next.remaining <= 0) outputBuffer.poll();
                boolean isCoin = CurrencyRegistry.server().match(stack) != null;
                playSound(isCoin ? VMSounds.COIN_DROP.get() : VMSounds.ITEM_DROP.get());
                markDirty();
                return;
            }
        }
    }

    private void playSound(SoundEvent sound) {
        if (getLevel() instanceof ServerLevel level) {
            float volume = (float) (0.6 + 0.4 * Math.random());
            float pitch = (float) (0.95 + 0.1 * Math.random());
            level.playSound(null, getPos(), sound, SoundSource.BLOCKS, volume, pitch);
        }
    }

    @Override
    public void saveCustomPersistedData(@NotNull CompoundTag tag, boolean forDrop) {
        super.saveCustomPersistedData(tag, forDrop);
        if (forDrop) return;
        CompoundTag data = new CompoundTag();
        data.put("inputs", inputItems.serializeNBT());
        data.put("outputs", outputItems.serializeNBT());
        ListTag queue = new ListTag();
        for (PendingOutput out : outputBuffer) {
            if (out.remaining <= 0) continue;
            CompoundTag entry = new CompoundTag();
            entry.put("item", out.template.save(new CompoundTag()));
            entry.putLong("count", out.remaining);
            queue.add(entry);
        }
        data.put("outputQueue", queue);
        tag.put(DATA_KEY, data);
    }

    @Override
    public void loadCustomPersistedData(@NotNull CompoundTag tag) {
        super.loadCustomPersistedData(tag);
        if (!tag.contains(DATA_KEY)) return;
        tag = tag.getCompound(DATA_KEY);
        if (tag.contains("inputs")) loadHandler(inputItems, tag.getCompound("inputs"));
        if (tag.contains("outputs")) loadHandler(outputItems, tag.getCompound("outputs"));
        outputBuffer.clear();

        ListTag legacy = tag.getList("outputBuffer", Tag.TAG_COMPOUND);
        for (int i = 0; i < legacy.size(); i++) {
            ItemStack s = ItemStack.of(legacy.getCompound(i));
            if (!s.isEmpty()) enqueueOutput(s, s.getCount());
        }
        ListTag queue = tag.getList("outputQueue", Tag.TAG_COMPOUND);
        for (int i = 0; i < queue.size(); i++) {
            CompoundTag entry = queue.getCompound(i);
            ItemStack template = ItemStack.of(entry.getCompound("item"));
            enqueueOutput(template, entry.getLong("count"));
        }
    }

    private void loadHandler(ItemStackHandler handler, CompoundTag tag) {
        ItemStackHandler tmp = new ItemStackHandler();
        tmp.deserializeNBT(tag);
        for (int i = 0; i < handler.getSlots(); i++) handler.setStackInSlot(i, ItemStack.EMPTY);
        for (int i = 0; i < tmp.getSlots(); i++) {
            ItemStack s = tmp.getStackInSlot(i);
            if (s.isEmpty()) continue;
            if (i < handler.getSlots()) handler.setStackInSlot(i, s);
            else enqueueOutput(s, s.getCount());
        }
    }

    public record TradeRequest(ServerPlayer player, UUID tradeGroup, int order, int quantity) {}

    private static final class PendingOutput {

        final ItemStack template;
        long remaining;

        PendingOutput(ItemStack template, long remaining) {
            this.template = template;
            this.remaining = remaining;
        }

        int maxStackSize() {
            return Math.max(1, template.getMaxStackSize());
        }
    }
}
