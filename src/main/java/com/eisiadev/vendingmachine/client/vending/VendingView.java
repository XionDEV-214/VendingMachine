package com.eisiadev.vendingmachine.client.vending;

import com.eisiadev.vendingmachine.VMConfig;
import com.eisiadev.vendingmachine.client.ClientTradeState;
import com.eisiadev.vendingmachine.client.FavouritesTracker;
import com.eisiadev.vendingmachine.client.TradeDisplay;
import com.eisiadev.vendingmachine.client.VendingMusic;
import com.eisiadev.vendingmachine.client.vending.widget.TradeButton;
import com.eisiadev.vendingmachine.currency.CurrencyAmount;
import com.eisiadev.vendingmachine.currency.CurrencyDefinition;
import com.eisiadev.vendingmachine.currency.CurrencyRegistry;
import com.eisiadev.vendingmachine.gui.SortMode;
import com.eisiadev.vendingmachine.gui.VendingGui;
import com.eisiadev.vendingmachine.machine.VendingMachineController;
import com.eisiadev.vendingmachine.trade.ItemIngredient;
import com.eisiadev.vendingmachine.trade.TradeCategory;
import com.eisiadev.vendingmachine.trade.TradeDatabase;
import com.eisiadev.vendingmachine.trade.TradeGroup;

import brachy.modularui.screen.RichTooltip;
import brachy.modularui.value.sync.LongSyncValue;
import brachy.modularui.value.sync.PanelSyncManager;
import brachy.modularui.widgets.PagedWidget;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import org.jetbrains.annotations.Nullable;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Client-side model of one open vending screen: which trades are shown per category tab, the search text, the
 * purchase quantity, the hovered trade, and the actions the widgets trigger (buy, favourite, eject, collect).
 */
public final class VendingView {

    public enum Status { READY, SHORT, BLOCKED, OFFLINE }

    // Remembered for the session, like the wallet's tabs.
    private static String lastSearch = "";
    private static int lastPage = 1;
    private static String lastQuantity = "";

    private final VendingGui gui;
    private final List<TradeCategory> categories = new ArrayList<>();
    private final Map<TradeCategory, List<TradeDisplay>> displayed = new EnumMap<>(TradeCategory.class);
    private final Set<TradeCategory> highlightedTabs = new HashSet<>();
    private final List<TradeButton> buttons = new ArrayList<>();
    private final PagedWidget.Controller tabs = new PagedWidget.Controller();

    private String search = lastSearch;
    @Nullable
    private String appliedSearch = null;
    private boolean forceRefresh = true;
    private int pageToRestore = lastPage;

    private String quantityText = lastQuantity;
    private int quantity = parseQuantity(lastQuantity);

    @Nullable
    private TradeDisplay hovered;

    public VendingView(VendingGui gui) {
        this.gui = gui;
        categories.add(TradeCategory.FAVOURITES);
        categories.add(TradeCategory.ALL);
        for (TradeCategory c : TradeDatabase.client().getCategories()) {
            if (c != TradeCategory.FAVOURITES && c != TradeCategory.ALL) categories.add(c);
        }
    }

    // ---------- Structure ----------

    public List<TradeCategory> categories() {
        return categories;
    }

    public PagedWidget.Controller tabs() {
        return tabs;
    }

    public int activeTab() {
        return tabs.isInitialised() ? tabs.getActivePageIndex() : -1;
    }

    public void selectTab(int index) {
        if (!tabs.isInitialised() || index == tabs.getActivePageIndex()) return;
        if (categories.get(index) == TradeCategory.FAVOURITES && list(TradeCategory.FAVOURITES).isEmpty()) {
            deny();
            return;
        }
        tabs.setPage(index);
        click(1.0F);
    }

    public boolean highlighted(TradeCategory category) {
        return highlightedTabs.contains(category);
    }

    public TradeButton register(TradeButton button) {
        buttons.add(button);
        return button;
    }

    public List<TradeDisplay> list(TradeCategory category) {
        return displayed.getOrDefault(category, List.of());
    }

    @Nullable
    public TradeDisplay displayed(TradeCategory category, int index) {
        List<TradeDisplay> list = list(category);
        return index < list.size() ? list.get(index) : null;
    }

    public boolean formed() {
        return gui.machine().isFormed();
    }

    // ---------- Search ----------

    public String search() {
        return search;
    }

    public void setSearch(String text) {
        search = text == null ? "" : text;
    }

    public void forceRefresh() {
        forceRefresh = true;
    }

    // ---------- Quantity ----------

    /** Exactly what the player typed (digits only, at most 6, enforced by the field). Empty means 1. */
    public String quantityText() {
        return quantityText;
    }

    public void setQuantityText(String text) {
        quantityText = text == null ? "" : text;
        quantity = parseQuantity(quantityText);
    }

    public int quantity() {
        return quantity;
    }

    private static int parseQuantity(String text) {
        long value = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c < '0' || c > '9') continue;
            value = value * 10 + (c - '0');
            if (value > VendingMachineController.MAX_PURCHASE_QUANTITY) break;
        }
        return VendingMachineController.clampQuantity(value);
    }

    /** Whether the trade can be bought right now in the selected quantity. */
    public boolean canBuy(TradeDisplay d) {
        return d.tradeableNow && d.maxQuantity >= quantity;
    }

    public boolean blocked(TradeDisplay d) {
        return !formed() || d.hasCooldown || !d.enabled;
    }

    // ---------- Hover ----------

    @Nullable
    public TradeDisplay hovered() {
        return hovered;
    }

    @Nullable
    public UUID hoveredGroup() {
        return hovered == null ? null : hovered.tgID;
    }

    public Status status() {
        if (!formed()) return Status.OFFLINE;
        if (hovered == null) return Status.READY;
        if (blocked(hovered)) return Status.BLOCKED;
        return canBuy(hovered) ? Status.READY : Status.SHORT;
    }

    // ---------- Wallet ----------

    public long balance(CurrencyDefinition def) {
        LongSyncValue value = gui.sync().findSyncHandler(VendingGui.SYNC_WALLET + def.getId(), LongSyncValue.class);
        return value == null ? 0 : value.getLongValue();
    }

    // ---------- Actions ----------

    public void attemptPurchase(TradeDisplay d) {
        if (!d.tradeableNow || blocked(d)) {
            deny();
            return;
        }
        int qty = quantity;
        if (qty > d.maxQuantity) {
            gui.player().displayClientMessage(Component.translatable("vendingmachine.gui.error.quantity_too_high",
                full(qty), full(d.maxQuantity)).withStyle(ChatFormatting.RED), true);
            deny();
            return;
        }
        d.cdTradeCount += qty;
        d.maxQuantity -= qty; // optimistic, corrected by the next status sync
        gui.sync().callSyncedAction(VendingGui.ACTION_TRADE, buf -> {
            buf.writeUUID(d.tgID);
            buf.writeVarInt(d.tradeGroupOrder);
            buf.writeVarInt(qty);
        });
        click(1.6F);
    }

    public void toggleFavourite(TradeDisplay d) {
        FavouritesTracker.toggle(d.tgID, d.tradeGroupOrder);
        forceRefresh = true;
        click(d.isFavourite ? 0.8F : 1.3F);
    }

    public void ejectItems() {
        gui.sync().callSyncedAction(VendingGui.ACTION_EJECT_ITEMS);
    }

    /** {@code currency} null = every currency in the wallet. */
    public void ejectCoins(@Nullable CurrencyDefinition currency) {
        gui.sync().callSyncedAction(VendingGui.ACTION_EJECT_COINS,
            buf -> buf.writeUtf(currency == null ? "" : currency.getId()));
    }

    public void collectOutputs() {
        gui.sync().callSyncedAction(VendingGui.ACTION_COLLECT);
    }

    public long outputDropTime(int slot) {
        return gui.outputDropTime(slot);
    }

    private void deny() {
        sound(SoundEvents.DISPENSER_FAIL, 1.6F, 0.4F);
    }

    // ---------- Lifecycle ----------

    public void open() {
        VendingMusic.start();
    }

    public void tick() {
        boolean dirty = ClientTradeState.consumeDirty();
        if (forceRefresh || dirty || !search.equals(appliedSearch)) {
            refresh(search);
            forceRefresh = false;
            appliedSearch = search;
        }
        if (tabs.isInitialised()) {
            if (pageToRestore >= 0) {
                if (pageToRestore < categories.size()) tabs.setPage(pageToRestore);
                pageToRestore = -1;
            }
            // Favourites emptied while open: fall back to "all".
            int active = tabs.getActivePageIndex();
            if (active >= 0 && active < categories.size() && categories.get(active) == TradeCategory.FAVOURITES &&
                list(TradeCategory.FAVOURITES).isEmpty()) {
                tabs.setPage(1);
            }
        }
        hovered = null;
        for (TradeButton b : buttons) {
            if (b.isEnabled() && b.isHovering() && b.display() != null) {
                hovered = b.display();
                break;
            }
        }
    }

    public void close() {
        VendingMusic.stop();
        lastSearch = search;
        lastQuantity = quantityText;
        if (tabs.isInitialised()) lastPage = tabs.getActivePageIndex();
        FavouritesTracker.save();
    }

    private void refresh(String text) {
        String lower = text.toLowerCase(Locale.ROOT);
        displayed.clear();
        List<TradeDisplay> all = new ArrayList<>();
        for (TradeDisplay d : ClientTradeState.getTrades()) {
            d.isFavourite = FavouritesTracker.isFavourite(d.tgID, d.tradeGroupOrder);
            if (!d.matchesSearch(lower)) continue;
            all.add(d);
            displayed.computeIfAbsent(d.group.getCategory(), k -> new ArrayList<>()).add(d);
        }
        displayed.put(TradeCategory.ALL, all);
        List<TradeDisplay> favs = new ArrayList<>();
        for (TradeDisplay d : all) if (d.isFavourite) favs.add(d);
        displayed.put(TradeCategory.FAVOURITES, favs);

        Comparator<TradeDisplay> cmp = comparator(VMConfig.SORT_MODE.get());
        displayed.values().forEach(list -> list.sort(cmp));

        highlightedTabs.clear();
        if (!lower.isEmpty()) {
            displayed.forEach((k, v) -> {
                if (!v.isEmpty()) highlightedTabs.add(k);
            });
        }
    }

    private static Comparator<TradeDisplay> comparator(SortMode mode) {
        Comparator<TradeDisplay> favFirst = Comparator.comparing(d -> !d.isFavourite);
        if (mode == SortMode.ALPHABET) {
            return favFirst.thenComparing(d -> d.display.getHoverName().getString());
        }
        return favFirst
            .thenComparingInt(VendingView::rank)
            .thenComparing(Comparator.comparingLong((TradeDisplay d) -> d.cooldown).reversed())
            .thenComparing(d -> String.valueOf(ForgeRegistries.ITEMS.getKey(d.display.getItem())))
            .thenComparingInt(d -> d.tradeGroupOrder);
    }

    private static int rank(TradeDisplay d) {
        if (!d.enabled) return 5;
        if (d.tradeableNow) return d.hasCooldown ? 2 : 1;
        return d.hasCooldown ? 4 : 3;
    }

    // ---------- Tooltip ----------

    public void buildTradeTooltip(RichTooltip t, TradeDisplay d) {
        int qty = quantity;
        for (ItemIngredient out : d.trade.toItems) {
            ItemStack stack = out.getDisplayStack();
            MutableComponent line = Component.empty();
            long perUnit = Math.max(1, out.getCount());
            if (qty > 1 || perUnit > 1) {
                line.append(Component.literal(times(perUnit, qty) + " ").withStyle(ChatFormatting.AQUA));
            }
            line.append(stack.getHoverName().copy().withStyle(ChatFormatting.AQUA));
            t.addLine(line);
        }
        t.emptyLine();

        t.addLine(Component.translatable("vendingmachine.gui.quantity.tooltip", full(qty), full(d.maxQuantity))
            .withStyle(canBuy(d) ? ChatFormatting.GREEN : ChatFormatting.RED));
        t.emptyLine();

        if (!d.trade.fromCurrency.isEmpty() || !d.trade.fromItems.isEmpty()) {
            t.addLine(Component.translatable("vendingmachine.gui.required_inputs")
                .withStyle(ChatFormatting.DARK_GREEN, ChatFormatting.ITALIC));
            for (CurrencyAmount price : d.trade.fromCurrency) {
                CurrencyDefinition def = CurrencyRegistry.client().get(price.type());
                Component name = def == null ? Component.literal(price.type()) : def.getDisplayName();
                t.addLine(Component.literal(times(price.value(), qty) + " ").append(name)
                    .withStyle(ChatFormatting.DARK_GREEN));
            }
            for (ItemIngredient in : d.trade.fromItems) t.addLine(ingredientLine(in, qty));
            t.emptyLine();
        }
        if (!d.trade.nonConsumedItems.isEmpty()) {
            t.addLine(Component.translatable("vendingmachine.gui.nc_inputs")
                .withStyle(ChatFormatting.DARK_GREEN, ChatFormatting.ITALIC));
            for (ItemIngredient in : d.trade.nonConsumedItems) t.addLine(ingredientLine(in, 1));
            t.emptyLine();
        }

        TradeGroup tg = d.group;
        if (tg.getTrades().size() > 1) {
            t.addLine(Component.translatable("vendingmachine.gui.shared_trades_tooltip", tg.getTrades().size() - 1)
                .withStyle(ChatFormatting.AQUA, ChatFormatting.ITALIC));
            t.emptyLine();
        }
        if (tg.hasCooldown()) {
            int per = ClientTradeState.getTradesPerCooldown();
            t.addLine(Component.translatable("vendingmachine.gui.cooldown_remaining_tooltip",
                Math.max(0, per - d.cdTradeCount), per).withStyle(ChatFormatting.DARK_AQUA));
            t.addLine(Component.translatable(d.cooldown > 0 ? "vendingmachine.gui.cooldown_started_tooltip" :
                "vendingmachine.gui.cooldown_idle_tooltip", d.cooldownText).withStyle(ChatFormatting.DARK_AQUA));
            t.emptyLine();
        }
        if (tg.maxTrades != -1 && !d.enabled) {
            t.addLine(Component.translatable("vendingmachine.gui.trade_limit_reached").withStyle(ChatFormatting.RED));
            t.emptyLine();
        }
        t.addLine(Component.translatable("vendingmachine.gui.trade_hint").withStyle(ChatFormatting.GRAY));
        t.addLine(Component.translatable("vendingmachine.gui.favourite_hint").withStyle(ChatFormatting.GRAY));
    }

    private static Component ingredientLine(ItemIngredient in, int qty) {
        MutableComponent line = Component.literal(times(in.getCount(), qty) + " ");
        if (in.isTag()) {
            line.append(Component.translatable("vendingmachine.gui.any_of_tag", "#" + in.getTagId()));
        } else {
            line.append(in.getDisplayStack().getHoverName());
        }
        return line.withStyle(ChatFormatting.DARK_GREEN);
    }

    // ---------- Formatting ----------

    public static String full(long value) {
        return String.format(Locale.ROOT, "%,d", value);
    }

    /** {@code value × qty} with thousands separators, without overflow. */
    public static String times(long value, int qty) {
        return String.format(Locale.ROOT, "%,d", BigInteger.valueOf(value).multiply(BigInteger.valueOf(qty)));
    }

    /** 999 / 12.3K / 4.56M ... */
    public static String compact(long value) {
        if (value < 10_000) return Long.toString(value);
        String[] units = { "K", "M", "B", "T", "Q" };
        double v = value;
        int u = -1;
        while (v >= 1000 && u < units.length - 1) {
            v /= 1000;
            u++;
        }
        String number = v >= 100 ? String.format(Locale.ROOT, "%.0f", v) :
            v >= 10 ? String.format(Locale.ROOT, "%.1f", v) : String.format(Locale.ROOT, "%.2f", v);
        return number + units[u];
    }

    // ---------- Sounds ----------

    public static void click(float pitch) {
        sound(SoundEvents.UI_BUTTON_CLICK.value(), pitch, 0.25F);
    }

    public static void sound(SoundEvent event, float pitch, float volume) {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(event, pitch, volume));
    }
}
