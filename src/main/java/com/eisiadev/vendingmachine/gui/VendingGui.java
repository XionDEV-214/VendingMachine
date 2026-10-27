package com.eisiadev.vendingmachine.gui;

import com.eisiadev.vendingmachine.VMConfig;
import com.eisiadev.vendingmachine.VendingMachine;
import com.eisiadev.vendingmachine.client.ClientTradeState;
import com.eisiadev.vendingmachine.client.FavouritesTracker;
import com.eisiadev.vendingmachine.client.TradeDisplay;
import com.eisiadev.vendingmachine.client.VendingMusic;
import com.eisiadev.vendingmachine.currency.CurrencyAmount;
import com.eisiadev.vendingmachine.currency.CurrencyDefinition;
import com.eisiadev.vendingmachine.currency.CurrencyRegistry;
import com.eisiadev.vendingmachine.gui.widget.FallingItemSlots;
import com.eisiadev.vendingmachine.gui.widget.ItemTooltips;
import com.eisiadev.vendingmachine.gui.widget.TradeButton;
import com.eisiadev.vendingmachine.machine.VendingMachineController;
import com.eisiadev.vendingmachine.trade.ItemIngredient;
import com.eisiadev.vendingmachine.trade.TradeCategory;
import com.eisiadev.vendingmachine.trade.TradeDatabase;
import com.eisiadev.vendingmachine.trade.TradeGroup;

import brachy.modularui.api.IPanelHandler;
import brachy.modularui.api.drawable.IDrawable;
import brachy.modularui.api.drawable.Text;
import brachy.modularui.api.widget.IWidget;
import brachy.modularui.api.widget.Interactable;
import brachy.modularui.drawable.DynamicDrawable;
import brachy.modularui.drawable.GuiTextures;
import brachy.modularui.drawable.ItemDrawable;
import brachy.modularui.factory.PosGuiData;
import brachy.modularui.screen.ModularPanel;
import brachy.modularui.screen.ModularScreen;
import brachy.modularui.screen.RichTooltip;
import brachy.modularui.screen.UISettings;
import brachy.modularui.utils.Alignment;
import brachy.modularui.value.IntValue;
import brachy.modularui.value.sync.LongSyncValue;
import brachy.modularui.value.sync.PanelSyncManager;
import brachy.modularui.widget.ParentWidget;
import brachy.modularui.widgets.ButtonWidget;
import brachy.modularui.widgets.CycleButtonWidget;
import brachy.modularui.widgets.ListWidget;
import brachy.modularui.widgets.PageButton;
import brachy.modularui.widgets.PagedWidget;
import brachy.modularui.widgets.SlotGroupWidget;
import brachy.modularui.widgets.TextWidget;
import brachy.modularui.widgets.layout.Flow;
import brachy.modularui.widgets.slot.ItemSlot;
import brachy.modularui.widgets.slot.ModularSlot;
import brachy.modularui.widget.scroll.VerticalScrollData;
import brachy.modularui.widgets.textfield.TextFieldWidget;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.registries.ForgeRegistries;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class VendingGui {

    private static final int PANEL_WIDTH = 178;
    private static final int TRADE_AREA_TOP = 33;
    private static final int TRADE_AREA_WIDTH = 162;
    private static final int TRADE_AREA_HEIGHT = 112;
    private static final int TILES_PER_ROW = 3;
    private static final int TILE_GAP = 2;
    private static final int TILE_ROW_WIDTH = TILES_PER_ROW * TradeButton.TILE_WIDTH + (TILES_PER_ROW - 1) * TILE_GAP;
    private static final int TAB_VISIBLE_WIDTH = 28;
    private static final int TAB_SCROLLBAR_WIDTH = 3;
    private static final int COIN_AREA_TOP = 148;
    private static final int COIN_ROWS = 5;
    private static final int COIN_ENTRY_HEIGHT = 13;
    private static final int COIN_AREA_HEIGHT = COIN_ROWS * COIN_ENTRY_HEIGHT + 6;
    private static final int PANEL_HEIGHT = COIN_AREA_TOP + COIN_AREA_HEIGHT + 88;
    private static final int COIN_ENTRY_WIDTH = 40;
    private static final int SIDE_WIDTH = 50;
    private static final int SIDE_HEIGHT = 214;
    private static final int CHUTE_WIDTH = 40;
    private static final int CHUTE_HEIGHT = 72;
    private static final int CHUTE_X = 5;
    private static final int CHUTE_Y = 138;

    private static final String ACTION_TRADE = "trade";
    private static final String ACTION_EJECT_ITEMS = "eject_items";
    private static final String ACTION_EJECT_COINS = "eject_coins";
    private static final String ACTION_COLLECT = "collect_outputs";

    private static boolean openOnClient = false;
    private static String lastSearch = "";
    private static int lastPage = 1;

    private final VendingMachineController machine;
    private final PosGuiData data;
    private final PanelSyncManager syncManager;
    private final boolean client;

    private final List<TradeCategory> categories = new ArrayList<>();
    private final Map<TradeCategory, List<TradeDisplay>> displayed = new EnumMap<>(TradeCategory.class);
    private final Set<TradeCategory> highlightedTabs = new HashSet<>();
    private PagedWidget.Controller tabController;
    private TextFieldWidget searchField;
    private String appliedSearch = null;
    private boolean forceRefresh = true;
    @Nullable
    private UUID hoveredGroup;
    private final List<TradeButton> allButtons = new ArrayList<>();

    public VendingGui(VendingMachineController machine, PosGuiData data, PanelSyncManager syncManager) {
        this.machine = machine;
        this.data = data;
        this.syncManager = syncManager;
        this.client = syncManager.isClient();
    }

    @OnlyIn(Dist.CLIENT)
    public static ModularScreen createScreen(ModularPanel<?> panel) {
        return new ModularScreen(VendingMachine.MODID, panel);
    }

    public static boolean isOpen() {
        return openOnClient;
    }

    public ModularPanel<?> build(UISettings settings) {
        registerSync();

        ModularPanel<?> panel = new ModularPanel<>("vending_machine");
        panel.size(PANEL_WIDTH, PANEL_HEIGHT);

        panel.child(Text.lang("block.vendingmachine.vending_machine").style(ChatFormatting.DARK_GRAY)
            .asWidget().pos(8, 6));

        if (client) {
            openOnClient = true;
            VendingMusic.start();
            categories.add(TradeCategory.FAVOURITES);
            categories.add(TradeCategory.ALL);
            for (TradeCategory c : TradeDatabase.client().getCategories()) {
                if (c != TradeCategory.FAVOURITES && c != TradeCategory.ALL) categories.add(c);
            }
            tabController = new PagedWidget.Controller();

            panel.child(createSearchField());
            panel.child(createOptionButtons());
            panel.child(createCategoryTabs());
            panel.child(createTradeArea());
            panel.child(createCoinArea());
            syncManager.onClientTick(this::clientTick);
        }

        panel.child(SlotGroupWidget.playerInventory(false, (i, slot) -> ItemTooltips.fix(slot)).pos(8, PANEL_HEIGHT - 82));
        panel.child(createSidePanel());
        return panel;
    }

    private void registerSync() {
        Player player = data.getPlayer();
        syncManager.bindPlayerInventory(player);
        syncManager.registerSlotGroup("vm_input", 2, true);
        syncManager.registerSlotGroup("vm_output", 2, false);

        for (CurrencyDefinition def : CurrencyRegistry.get(client).all()) {
            String id = def.getId();
            syncManager.syncValue("wallet_" + id, new LongSyncValue(() -> {
                if (client) return 0L;
                var wallet = com.eisiadev.vendingmachine.trade.TradeManager.getWallet(player.getUUID());
                return wallet == null ? 0L : wallet.get(id);
            }));
        }

        syncManager.registerServerSyncedAction(ACTION_TRADE, buf -> {
            UUID group = buf.readUUID();
            int order = buf.readVarInt();
            if (player instanceof ServerPlayer sp) machine.queueTrade(sp, group, order);
        });
        syncManager.registerServerSyncedAction(ACTION_EJECT_ITEMS, buf -> machine.ejectInputItems());
        syncManager.registerServerSyncedAction(ACTION_EJECT_COINS, buf -> {
            String currency = buf.readUtf();
            if (player instanceof ServerPlayer sp) machine.ejectCoins(sp, currency.isEmpty() ? null : currency);
        });
        syncManager.registerServerSyncedAction(ACTION_COLLECT, buf -> machine.collectOutputs(player));

        syncManager.addCloseListener(p -> {
            if (client) {
                openOnClient = false;
                VendingMusic.stop();
                if (searchField != null) lastSearch = searchField.getText();
                if (tabController != null && tabController.isInitialised()) {
                    lastPage = tabController.getActivePageIndex();
                }
                FavouritesTracker.save();
            } else {
                machine.resetCurrentUser(p);
            }
        });
    }

    private IWidget createSearchField() {
        searchField = new TextFieldWidget();
        searchField.hintText(Component.translatable("vendingmachine.gui.search"));
        searchField.setText(lastSearch);
        return searchField.pos(8, 17).size(114, 12);
    }

    private IWidget createOptionButtons() {
        CycleButtonWidget displayButton = new CycleButtonWidget()
            .stateCount(DisplayType.values().length)
            .value(new IntValue.Dynamic(() -> VMConfig.DISPLAY_TYPE.get().ordinal(), v -> {
                VMConfig.DISPLAY_TYPE.set(DisplayType.values()[v]);
                VMConfig.CLIENT_SPEC.save();
            }))
            .overlay(new DynamicDrawable(() -> VMTextures.displayType(VMConfig.DISPLAY_TYPE.get()).asIcon().size(12)));
        displayButton.tooltipDynamic(t -> t.clearText()
            .addLine(Component.translatable("vendingmachine.gui.display_mode")
                .append(" ").append(Component.translatable(VMConfig.DISPLAY_TYPE.get().translationKey))));
        displayButton.tooltipAutoUpdate(true);

        CycleButtonWidget sortButton = new CycleButtonWidget()
            .stateCount(SortMode.values().length)
            .value(new IntValue.Dynamic(() -> VMConfig.SORT_MODE.get().ordinal(), v -> {
                VMConfig.SORT_MODE.set(SortMode.values()[v]);
                VMConfig.CLIENT_SPEC.save();
                forceRefresh = true;
            }))
            .overlay(new DynamicDrawable(() -> VMTextures.sortMode(VMConfig.SORT_MODE.get()).asIcon().size(12)));
        sortButton.tooltipDynamic(t -> t.clearText()
            .addLine(Component.translatable("vendingmachine.gui.display_sort")
                .append(" ").append(Component.translatable(VMConfig.SORT_MODE.get().translationKey))));
        sortButton.tooltipAutoUpdate(true);

        CycleButtonWidget musicButton = new CycleButtonWidget() {
            @Override
            public boolean onMouseScrolled(double delta) {
                double volume = VMConfig.MUSIC_VOLUME.get() + (delta > 0 ? 0.05 : -0.05);
                volume = Math.round(Math.max(0, Math.min(1, volume)) * 20) / 20.0;
                VMConfig.MUSIC_VOLUME.set(volume);
                VMConfig.CLIENT_SPEC.save();
                return true;
            }
        }
            .stateCount(MusicTrack.values().length)
            .value(new IntValue.Dynamic(() -> VMConfig.MUSIC_TRACK.get().ordinal(), v -> {
                VMConfig.MUSIC_TRACK.set(MusicTrack.values()[v]);
                VMConfig.CLIENT_SPEC.save();
                VendingMusic.onTrackChanged();
            }))
            .overlay(new DynamicDrawable(() -> VMTextures.musicTrack(VMConfig.MUSIC_TRACK.get()).asIcon().size(12)));
        musicButton.tooltipDynamic(t -> t.clearText()
            .addLine(Component.translatable("vendingmachine.gui.display_track")
                .append(" ").append(Component.translatable(VMConfig.MUSIC_TRACK.get().translationKey)))
            .addLine(Component.translatable("vendingmachine.gui.volume.tooltip_volume_display",
                Math.round(VMConfig.MUSIC_VOLUME.get() * 100)).withStyle(ChatFormatting.GRAY))
            .addLine(Component.translatable("vendingmachine.gui.volume.tooltip_scroll")
                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC)));
        musicButton.tooltipAutoUpdate(true);

        return Flow.row()
            .coverChildren()
            .childPadding(2)
            .pos(126, 16)
            .child(musicButton.size(14))
            .child(displayButton.size(14))
            .child(sortButton.size(14));
    }

    private IWidget createCategoryTabs() {
        ListWidget<IWidget, ?> tabs = new ListWidget<>();
        tabs.scrollDirection(new VerticalScrollData(true, TAB_SCROLLBAR_WIDTH));
        tabs.pos(-TAB_VISIBLE_WIDTH - TAB_SCROLLBAR_WIDTH, TRADE_AREA_TOP)
            .width(32 + TAB_SCROLLBAR_WIDTH)
            .height(PANEL_HEIGHT - TRADE_AREA_TOP - 4)
            .crossAxisAlignment(Alignment.CrossAxis.END);
        for (int i = 0; i < categories.size(); i++) {
            TradeCategory category = categories.get(i);
            int index = i;
            PageButton tab = new PageButton(i, tabController)
                .tab(GuiTextures.TAB_LEFT, 0);
            tab.overlay(
                new DynamicDrawable(() -> highlightedTabs.contains(category) ?
                    VMTextures.TAB_HIGHLIGHT.asIcon().size(20, 16) : IDrawable.EMPTY),
                VMTextures.categoryIcon(category.getKey()).asIcon().size(20, 16));
            tab.tooltipDynamic(t -> t.clearText().addLine(Component.translatable(category.getTranslationKey())));
            if (category == TradeCategory.FAVOURITES) {
                tab.setEnabledIf(w -> !getList(TradeCategory.FAVOURITES).isEmpty());
            }
            tab.onUpdateListener(w -> {
                if (tabController.isInitialised() && tabController.getActivePageIndex() == index &&
                    category == TradeCategory.FAVOURITES && getList(TradeCategory.FAVOURITES).isEmpty()) {
                    tabController.setPage(1);
                }
            }, true);
            tabs.child(tab);
        }
        return tabs;
    }

    private IWidget createTradeArea() {
        PagedWidget<?> paged = new PagedWidget<>()
            .controller(tabController)
            .background(VMTextures.TRADE_BACKGROUND);
        paged.pos(8, TRADE_AREA_TOP).size(TRADE_AREA_WIDTH, TRADE_AREA_HEIGHT);

        TradeDatabase db = TradeDatabase.client();
        int totalTrades = db.getTradeCount();
        for (TradeCategory category : categories) {
            int count;
            if (category == TradeCategory.ALL || category == TradeCategory.FAVOURITES) {
                count = totalTrades;
            } else {
                count = 0;
                for (TradeGroup tg : db.getTradeGroups()) {
                    if (tg.getCategory() == category) count += tg.getTrades().size();
                }
            }

            ListWidget<IWidget, ?> list = new ListWidget<>();
            list.size(TRADE_AREA_WIDTH - 2, TRADE_AREA_HEIGHT - 4).pos(2, 2)
                .crossAxisAlignment(Alignment.CrossAxis.START);

            Flow status = Flow.row().height(12).width(TRADE_AREA_WIDTH - 8)
                .child(Text.lang("vendingmachine.gui.error.incomplete_structure").style(ChatFormatting.RED)
                    .asWidget());
            status.setEnabledIf(w -> !isMachineFormed());
            list.child(status);

            Flow row = null;
            for (int i = 0; i < count; i++) {
                if (i % TILES_PER_ROW == 0) {
                    row = newRow(TILE_ROW_WIDTH, TradeButton.TILE_HEIGHT).marginBottom(2);
                    list.child(row);
                }
                row.child(register(new TradeButton(this, category, i, DisplayType.TILE)));
            }
            for (int i = 0; i < count; i++) {
                Flow listRow = newRow(TradeButton.LIST_WIDTH, TradeButton.LIST_HEIGHT).marginBottom(1);
                listRow.child(register(new TradeButton(this, category, i, DisplayType.LIST)));
                list.child(listRow);
            }
            paged.addPage(list);
        }
        return paged;
    }

    private TradeButton register(TradeButton button) {
        allButtons.add(button);
        return button;
    }

    private static Flow newRow(int width, int height) {
        Flow row = Flow.row()
            .size(width, height)
            .childPadding(TILE_GAP)
            .mainAxisAlignment(Alignment.MainAxis.START)
            .collapseDisabledChildren(true);
        row.setEnabledIf(r -> {
            for (IWidget child : r.getChildren()) if (child.isEnabled()) return true;
            return false;
        });
        return row;
    }

    private IWidget createCoinArea() {
        ListWidget<IWidget, ?> coins = new ListWidget<>();
        coins.scrollDirection(brachy.modularui.api.GuiAxis.X);
        coins.pos(8, COIN_AREA_TOP).size(TRADE_AREA_WIDTH, COIN_AREA_HEIGHT);
        coins.setEnabledIf(w -> VMConfig.SHOW_COINS.get());

        Flow column = null;
        int n = 0;
        for (CurrencyDefinition def : CurrencyRegistry.client().all()) {
            if (n % COIN_ROWS == 0) {
                column = Flow.col().width(COIN_ENTRY_WIDTH).height(COIN_AREA_HEIGHT - 4);
                coins.child(column);
            }
            column.child(createCoinEntry(def));
            n++;
        }
        coins.child(new ButtonWidget<>()
            .size(16)
            .overlay(VMTextures.COIN_RETURN.asIcon().size(14))
            .tooltipDynamic(t -> t.clearText().addLine(Component.translatable("vendingmachine.gui.coin_eject")))
            .onMousePressed((ctx, button) -> {
                syncManager.callSyncedAction(ACTION_EJECT_COINS, buf -> buf.writeUtf(""));
                return true;
            }));
        return coins;
    }

    private IWidget createCoinEntry(CurrencyDefinition def) {
        LongSyncValue amount = syncManager.findSyncHandler("wallet_" + def.getId(), LongSyncValue.class);
        ButtonWidget<?> icon = new ButtonWidget<>()
            .size(12)
            .background(IDrawable.EMPTY)
            .hoverBackground(IDrawable.EMPTY)
            .overlay(new DynamicDrawable(() -> new ItemDrawable(def.getIcon(amount.getLongValue())).asIcon().size(12)))
            .onMousePressed((ctx, button) -> {
                if (!Interactable.hasShiftDown()) return false;
                syncManager.callSyncedAction(ACTION_EJECT_COINS, buf -> buf.writeUtf(def.getId()));
                return true;
            });
        icon.tooltipDynamic(t -> t.clearText()
            .addLine(Component.literal(String.format(Locale.ROOT, "%,d ", amount.getLongValue()))
                .append(def.getDisplayName()))
            .emptyLine()
            .addLine(Component.translatable("vendingmachine.gui.single_coin_type_eject_hint")
                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC)));
        icon.tooltipAutoUpdate(true);

        return Flow.row()
            .height(COIN_ENTRY_HEIGHT)
            .width(COIN_ENTRY_WIDTH)
            .child(icon)
            .child(new TextWidget<>(() -> Component.literal(shortAmount(amount.getLongValue())))
                .scale(0.8f)
                .marginLeft(2));
    }

    private IWidget createSidePanel() {
        ParentWidget<?> side = new ParentWidget<>();
        side.pos(PANEL_WIDTH - 2, 30).size(SIDE_WIDTH, SIDE_HEIGHT).background(VMTextures.SIDE_PANEL_BACKGROUND)
            .excludeAreaInRecipeViewer();

        addBanner(side, "vendingmachine.gui.in", 10, 4);
        side.child(SlotGroupWidget.builder()
            .matrix("II", "II", "II", "II")
            .key('I', i -> ItemTooltips.fix(new ItemSlot().slot(new ModularSlot(machine.inputItems, i).slotGroup("vm_input"))))
            .build()
            .pos(7, 24));

        side.child(new ButtonWidget<>()
            .size(16)
            .pos(8, 99)
            .overlay(VMTextures.EJECT_ITEMS.asIcon().size(14))
            .tooltipDynamic(t -> t.clearText().addLine(Component.translatable("vendingmachine.gui.item_eject")))
            .onMousePressed((ctx, button) -> {
                syncManager.callSyncedAction(ACTION_EJECT_ITEMS);
                return true;
            }));
        side.child(new ButtonWidget<>()
            .size(16)
            .pos(26, 99)
            .overlay(VMTextures.COIN_RETURN.asIcon().size(14))
            .tooltipDynamic(t -> t.clearText().addLine(Component.translatable("vendingmachine.gui.coin_eject")))
            .onMousePressed((ctx, button) -> {
                syncManager.callSyncedAction(ACTION_EJECT_COINS, buf -> buf.writeUtf(""));
                return true;
            }));

        addBanner(side, "vendingmachine.gui.out", 10, 118);
        addChute(side, CHUTE_X, CHUTE_Y);
        return side;
    }

    private static void addBanner(ParentWidget<?> parent, String langKey, int x, int y) {
        parent.child(VMTextures.IO_BANNER.asWidget().pos(x, y).size(30, 20));
        parent.child(new TextWidget<>(Component.translatable(langKey))
            .color(0xFFFFFFFF)
            .shadow(true)
            .alignment(Alignment.Center)
            .pos(x, y + 1)
            .size(30, 12));
    }

    private void addChute(ParentWidget<?> parent, int x, int y) {
        parent.child(VMTextures.DISPENSER_BACKGROUND.asWidget().pos(x, y).size(CHUTE_WIDTH, CHUTE_HEIGHT));
        ButtonWidget<?> collect = new ButtonWidget<>()
            .pos(x, y)
            .size(CHUTE_WIDTH, CHUTE_HEIGHT)
            .background(IDrawable.EMPTY)
            .hoverBackground(IDrawable.EMPTY)
            .playClickSound(false)
            .onMousePressed((ctx, button) -> {
                if (button != 0 || !Interactable.hasShiftDown()) return false;
                syncManager.callSyncedAction(ACTION_COLLECT);
                return true;
            });
        collect.tooltipDynamic(t -> t.clearText()
            .addLine(Component.translatable("vendingmachine.gui.collect_outputs_hint").withStyle(ChatFormatting.GRAY)));
        parent.child(collect);

        FallingItemSlots falling = new FallingItemSlots(machine.outputItems, "vm_output",
            x + 1, y, CHUTE_WIDTH - 18, CHUTE_HEIGHT - 20);
        for (int i = 0; i < VendingMachineController.OUTPUT_SLOTS; i++) {
            parent.child(falling.create(i));
        }
        parent.child(VMTextures.DISPENSER_OVERHANG.asWidget().pos(x, y).size(CHUTE_WIDTH, 19));
    }

    public boolean isMachineFormed() {
        return machine.isFormed();
    }

    @Nullable
    public UUID getHoveredGroup() {
        return hoveredGroup;
    }

    private List<TradeDisplay> getList(TradeCategory category) {
        return displayed.getOrDefault(category, List.of());
    }

    @Nullable
    public TradeDisplay getDisplayed(TradeCategory category, int index) {
        List<TradeDisplay> list = getList(category);
        return index < list.size() ? list.get(index) : null;
    }

    private void clientTick() {
        String search = searchField == null ? "" : searchField.getText();
        boolean dirty = ClientTradeState.consumeDirty();
        if (forceRefresh || dirty || !search.equals(appliedSearch)) {
            refresh(search);
            forceRefresh = false;
            appliedSearch = search;
        }
        if (tabController != null && tabController.isInitialised() && lastPage >= 0) {
            if (lastPage < categories.size()) tabController.setPage(lastPage);
            lastPage = -1;
        }
        hoveredGroup = null;
        for (TradeButton b : allButtons) {
            if (b.isEnabled() && b.isHovering() && b.getDisplay() != null) {
                hoveredGroup = b.getDisplay().tgID;
                break;
            }
        }
    }

    private void refresh(String search) {
        String lower = search.toLowerCase(Locale.ROOT);
        SortMode sortMode = VMConfig.SORT_MODE.get();
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

        Comparator<TradeDisplay> cmp = comparator(sortMode);
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
            .thenComparingInt(VendingGui::rank)
            .thenComparing(Comparator.comparingLong((TradeDisplay d) -> d.cooldown).reversed())
            .thenComparing(d -> String.valueOf(ForgeRegistries.ITEMS.getKey(d.display.getItem())))
            .thenComparingInt(d -> d.tradeGroupOrder);
    }

    private static int rank(TradeDisplay d) {
        if (!d.enabled) return 5;
        if (d.tradeableNow) return d.hasCooldown ? 2 : 1;
        return d.hasCooldown ? 4 : 3;
    }

    public void attemptPurchase(TradeDisplay d) {
        if (!d.tradeableNow || !d.enabled || d.hasCooldown || !isMachineFormed()) return;
        d.cdTradeCount++;
        syncManager.callSyncedAction(ACTION_TRADE, buf -> {
            buf.writeUUID(d.tgID);
            buf.writeVarInt(d.tradeGroupOrder);
        });
    }

    public void toggleFavourite(TradeDisplay d) {
        FavouritesTracker.toggle(d.tgID, d.tradeGroupOrder);
        forceRefresh = true;
    }

    public void buildTradeTooltip(RichTooltip t, TradeDisplay d) {
        for (ItemIngredient out : d.trade.toItems) {
            ItemStack stack = out.getDisplayStack();
            MutableComponent line = Component.empty();
            if (out.getCount() > 1) line.append(Component.literal(out.getCount() + " ").withStyle(ChatFormatting.AQUA));
            line.append(stack.getHoverName().copy().withStyle(ChatFormatting.AQUA));
            t.addLine(line);
        }
        t.emptyLine();

        if (!d.trade.fromCurrency.isEmpty() || !d.trade.fromItems.isEmpty()) {
            t.addLine(Component.translatable("vendingmachine.gui.required_inputs")
                .withStyle(ChatFormatting.DARK_GREEN, ChatFormatting.ITALIC));
            for (CurrencyAmount price : d.trade.fromCurrency) {
                CurrencyDefinition def = CurrencyRegistry.client().get(price.type());
                Component name = def == null ? Component.literal(price.type()) : def.getDisplayName();
                t.addLine(Component.literal(String.format(Locale.ROOT, "%,d ", price.value())).append(name)
                    .withStyle(ChatFormatting.DARK_GREEN));
            }
            for (ItemIngredient in : d.trade.fromItems) t.addLine(ingredientLine(in));
            t.emptyLine();
        }
        if (!d.trade.nonConsumedItems.isEmpty()) {
            t.addLine(Component.translatable("vendingmachine.gui.nc_inputs")
                .withStyle(ChatFormatting.DARK_GREEN, ChatFormatting.ITALIC));
            for (ItemIngredient in : d.trade.nonConsumedItems) t.addLine(ingredientLine(in));
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

    private static Component ingredientLine(ItemIngredient in) {
        MutableComponent line = Component.literal(in.getCount() + " ");
        if (in.isTag()) {
            line.append(Component.translatable("vendingmachine.gui.any_of_tag", "#" + in.getTagId()));
        } else {
            line.append(in.getDisplayStack().getHoverName());
        }
        return line.withStyle(ChatFormatting.DARK_GREEN);
    }

    private static String shortAmount(long amount) {
        if (amount < 10_000) return String.valueOf(amount);
        if (amount < 1_000_000) return amount / 1_000 + "K";
        if (amount < 1_000_000_000) return amount / 1_000_000 + "M";
        return amount / 1_000_000_000 + "B";
    }
}
