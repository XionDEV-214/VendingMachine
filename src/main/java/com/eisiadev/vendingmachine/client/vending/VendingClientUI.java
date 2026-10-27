package com.eisiadev.vendingmachine.client.vending;

import com.eisiadev.vendingmachine.VMConfig;
import com.eisiadev.vendingmachine.client.VendingMusic;
import com.eisiadev.vendingmachine.client.vending.widget.CategoryTab;
import com.eisiadev.vendingmachine.client.vending.widget.ChuteWidget;
import com.eisiadev.vendingmachine.client.vending.widget.CoinEntry;
import com.eisiadev.vendingmachine.client.vending.widget.Decor;
import com.eisiadev.vendingmachine.client.vending.widget.IconKey;
import com.eisiadev.vendingmachine.client.vending.widget.PurchaseDisplay;
import com.eisiadev.vendingmachine.client.vending.widget.QuantityField;
import com.eisiadev.vendingmachine.client.vending.widget.TradeButton;
import com.eisiadev.vendingmachine.currency.CurrencyDefinition;
import com.eisiadev.vendingmachine.currency.CurrencyRegistry;
import com.eisiadev.vendingmachine.gui.DisplayType;
import com.eisiadev.vendingmachine.gui.MusicTrack;
import com.eisiadev.vendingmachine.gui.SortMode;
import com.eisiadev.vendingmachine.gui.VMTextures;
import com.eisiadev.vendingmachine.gui.VendingGui;
import com.eisiadev.vendingmachine.gui.widget.ItemTooltips;
import com.eisiadev.vendingmachine.machine.VendingMachineController;
import com.eisiadev.vendingmachine.trade.TradeCategory;
import com.eisiadev.vendingmachine.trade.TradeDatabase;
import com.eisiadev.vendingmachine.trade.TradeGroup;

import brachy.modularui.api.GuiAxis;
import brachy.modularui.api.drawable.IDrawable;
import brachy.modularui.api.widget.IWidget;
import brachy.modularui.screen.ModularPanel;
import brachy.modularui.screen.viewport.ModularGuiContext;
import brachy.modularui.theme.WidgetThemeEntry;
import brachy.modularui.utils.Alignment;
import brachy.modularui.value.StringValue;
import brachy.modularui.widget.ParentWidget;
import brachy.modularui.widgets.ListWidget;
import brachy.modularui.widgets.PagedWidget;
import brachy.modularui.widgets.SlotGroupWidget;
import brachy.modularui.widgets.TransformWidget;
import brachy.modularui.widgets.layout.Flow;
import brachy.modularui.widgets.slot.ItemSlot;
import brachy.modularui.widgets.textfield.TextFieldWidget;

import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.Locale;

/** Client half of the vending machine UI: every widget that is drawn. Layout constants are in panel coordinates. */
public final class VendingClientUI {

    private static final int W = VendingGui.WIDTH;
    private static final int H = VendingGui.HEIGHT;

    private static final int TITLE_H = 15;
    private static final int SEARCH_X = 92;
    private static final int SEARCH_W = W - 4 - SEARCH_X - 3;

    private static final int SHELF_X = 8;
    private static final int SHELF_Y = 22;
    private static final int SHELF_W = 162;
    private static final int SHELF_H = 112;
    private static final int TILES_PER_ROW = 3;
    private static final int TILE_GAP = 2;
    private static final int TILE_ROW_W = TILES_PER_ROW * TradeButton.TILE_WIDTH + (TILES_PER_ROW - 1) * TILE_GAP;

    private static final int LED_Y = SHELF_Y + SHELF_H + 3;
    private static final int LED_H = 24;

    private static final int COIN_Y = LED_Y + LED_H + 3;
    private static final int COIN_ROWS = 5;
    private static final int COIN_H = COIN_ROWS * CoinEntry.H + 4;

    private static final int SIDE_W = 54;
    private static final int SIDE_H = 218;
    private static final int SIDE_Y = 22;
    private static final int OUT_Y = 114;
    private static final int CHUTE_X = (SIDE_W - ChuteWidget.W) / 2;
    private static final int CHUTE_Y = OUT_Y + 12;
    private static final int OPTIONS_Y = CHUTE_Y + ChuteWidget.H + 9;

    private VendingClientUI() {}

    /** Builds everything that is drawn; returns what to run when the screen closes. */
    public static Runnable build(ModularPanel<?> panel, VendingGui gui) {
        VendingView view = new VendingView(gui);
        view.open();
        gui.sync().onClientTick(view::tick);

        // The panel itself draws nothing: category tabs go first, then the body covers their inner edge, so tabs
        // always look like they are behind the machine.
        panel.disableThemeBackground(true);
        for (int i = 0; i < view.categories().size(); i++) {
            panel.child(new CategoryTab(view, i).pos(-CategoryTab.W + 2, SHELF_Y + i * (CategoryTab.H + 2)));
        }
        panel.child(new Decor(view, (g, w, h, now) -> body(g, w, h)).pos(0, 0).size(W, H));

        // Title strip with the search field set into it
        panel.child(new Decor(view, (g, w, h, now) -> titleStrip(g, w, h, now, view)).pos(4, 4).size(W - 8, TITLE_H));
        panel.child(searchField(view));

        panel.child(shelf(view));

        panel.child(new PurchaseDisplay(view).pos(SHELF_X, LED_Y).size(SHELF_W, LED_H));
        panel.child(new QuantityField(view)
            .pos(SHELF_X + PurchaseDisplay.FIELD_X, LED_Y + PurchaseDisplay.FIELD_Y)
            .size(PurchaseDisplay.FIELD_W, PurchaseDisplay.FIELD_H));

        panel.child(new Decor(view, (g, w, h, now) -> coinWell(g, w, h)).pos(SHELF_X, COIN_Y).size(SHELF_W, COIN_H));
        panel.child(coinStrip(view));

        // Inventory
        panel.child(new Decor(view, (g, w, h, now) -> {
            Gfx.rect(g, 0, 0, w, 1, Gfx.SEAM);
            Gfx.rect(g, 0, 1, w, 1, 0xFF4A535D);
            Gfx.text(g, Component.translatable("vendingmachine.gui.inventory").getString(), 0, 4, 0.5f, Gfx.MUTED, false);
            Gfx.hazard(g, w - 30, 3, 30, 4, Gfx.AMBER, Gfx.HAZARD_DARK, 0);
        }).pos(VendingGui.INVENTORY_X, VendingGui.INVENTORY_Y - 10).size(162, 8));
        panel.child(SlotGroupWidget.playerInventory(false, (index, slot) -> style(slot))
            .pos(VendingGui.INVENTORY_X, VendingGui.INVENTORY_Y));

        panel.child(sidePanel(view));
        return view::close;
    }

    // ---------- Search ----------

    private static IWidget searchField(VendingView view) {
        TextFieldWidget field = new TextFieldWidget();
        field.autoUpdateOnChange(true);
        field.setTextColor(Gfx.TEXT);
        field.hintText(Component.translatable("vendingmachine.gui.search"));
        field.hintColor(Gfx.DIM);
        field.disableThemeBackground(true);
        field.background(SEARCH_BACKGROUND);
        field.value(new StringValue.Dynamic(view::search, view::setSearch));
        return field.pos(SEARCH_X, 6).size(SEARCH_W, TITLE_H - 4);
    }

    private static final IDrawable SEARCH_BACKGROUND = (context, x, y, w, h, theme) -> {
        GuiGraphics g = context.getGraphics();
        Gfx.well(g, x, y, w, h);
        Gfx.rect(g, x + 1, y + 1, w - 2, h - 2, 0xFF0B0E11);
    };

    // ---------- Shelf: one scrolling page per category tab ----------

    private static IWidget shelf(VendingView view) {
        PagedWidget<?> paged = new PagedWidget<>().controller(view.tabs());
        paged.disableThemeBackground(true);
        paged.background(WELL);
        paged.pos(SHELF_X, SHELF_Y).size(SHELF_W, SHELF_H);

        TradeDatabase db = TradeDatabase.client();
        int totalTrades = db.getTradeCount();
        for (TradeCategory category : view.categories()) {
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
            list.size(SHELF_W - 4, SHELF_H - 4).pos(2, 2).crossAxisAlignment(Alignment.CrossAxis.START);

            list.child(new Decor(view, (g, w, h, now) -> offline(g, w, h, now))
                .setEnabledIf(w -> !view.formed()).size(TILE_ROW_W, 14).marginBottom(2));

            Flow row = null;
            for (int i = 0; i < count; i++) {
                if (i % TILES_PER_ROW == 0) {
                    row = newRow(TILE_ROW_W, TradeButton.TILE_HEIGHT).marginBottom(TILE_GAP);
                    list.child(row);
                }
                row.child(view.register(new TradeButton(view, category, i, DisplayType.TILE)));
            }
            for (int i = 0; i < count; i++) {
                Flow listRow = newRow(TradeButton.LIST_WIDTH, TradeButton.LIST_HEIGHT).marginBottom(1);
                listRow.child(view.register(new TradeButton(view, category, i, DisplayType.LIST)));
                list.child(listRow);
            }
            paged.addPage(list);
        }
        return paged;
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

    // ---------- Wallet strip ----------

    private static IWidget coinStrip(VendingView view) {
        ListWidget<IWidget, ?> coins = new ListWidget<>();
        coins.scrollDirection(GuiAxis.X);
        coins.pos(SHELF_X + 3, COIN_Y + 2).size(SHELF_W - 6, COIN_H - 4);
        coins.setEnabledIf(w -> VMConfig.SHOW_COINS.get());

        Flow column = null;
        int n = 0;
        for (CurrencyDefinition def : CurrencyRegistry.client().all()) {
            if (n % COIN_ROWS == 0) {
                column = Flow.col().width(CoinEntry.W).height(COIN_H - 4);
                coins.child(column);
            }
            column.child(new CoinEntry(view, def));
            n++;
        }
        coins.child(new IconKey(view, () -> VMTextures.COIN_RETURN, () -> view.ejectCoins(null))
            .accent(Gfx.AMBER)
            .tooltipDynamic(t -> t.clearText().addLine(Component.translatable("vendingmachine.gui.coin_eject")))
            .size(16));
        return coins;
    }

    // ---------- Side cabinet: input, output chute, options ----------

    private static ParentWidget<?> sidePanel(VendingView view) {
        SidePanel side = new SidePanel();
        side.pos(W - 3, SIDE_Y).size(SIDE_W, SIDE_H);
        side.excludeAreaInRecipeViewer();

        side.child(new Decor(view, (g, w, h, now) -> banner(g, w, h, "vendingmachine.gui.in", Gfx.LED_ON))
            .pos(6, 6).size(SIDE_W - 12, 10));
        for (int i = 0; i < VendingMachineController.INPUT_SLOTS; i++) {
            side.child(style(new ItemSlot()).syncHandler(VendingGui.SLOT_INPUT, i)
                .pos(9 + (i % 2) * 18, 18 + (i / 2) * 18));
        }
        side.child(new IconKey(view, () -> VMTextures.EJECT_ITEMS, view::ejectItems)
            .accent(Gfx.LED_ON).iconSize(10)
            .tooltipDynamic(t -> t.clearText().addLine(Component.translatable("vendingmachine.gui.item_eject")))
            .pos(8, 93).size(18, 14));
        side.child(new IconKey(view, () -> VMTextures.COIN_RETURN, () -> view.ejectCoins(null))
            .accent(Gfx.AMBER).iconSize(10)
            .tooltipDynamic(t -> t.clearText().addLine(Component.translatable("vendingmachine.gui.coin_eject")))
            .pos(28, 93).size(18, 14));

        side.child(new Decor(view, (g, w, h, now) -> banner(g, w, h, "vendingmachine.gui.out", Gfx.AMBER))
            .pos(6, OUT_Y).size(SIDE_W - 12, 10));
        ChuteWidget chute = new ChuteWidget(view);
        side.child(chute.pos(CHUTE_X, CHUTE_Y));
        for (int i = 0; i < VendingMachineController.OUTPUT_SLOTS; i++) {
            int slot = i;
            // No hover square under items in the chute: they are loose items, not slots.
            ItemSlot item = style(new ItemSlot() {

                @Override
                public int getSlotHoverColor() {
                    return 0;
                }
            }).syncHandler(VendingGui.SLOT_OUTPUT, i);
            item.disableThemeBackground(true);
            item.background(IDrawable.EMPTY);
            item.setEnabledIf(s -> item.isSynced() && item.getSlot().hasItem());
            item.pos(CHUTE_X + ChuteWidget.slotX(i), CHUTE_Y + ChuteWidget.slotY(i));
            // TransformWidget delegates its area to the slot; it only adds the falling offset.
            side.child(new TransformWidget(item)
                .transform(stack -> stack.translate(0, chute.dropOffset(slot, Util.getMillis()), slot * 0.5f)));
        }
        side.child(new Decor(view, (g, w, h, now) -> ChuteWidget.drawLip(g, w, now))
            .pos(CHUTE_X, CHUTE_Y).size(ChuteWidget.W, ChuteWidget.LIP_H));

        int x = 7;
        side.child(musicKey(view).pos(x, OPTIONS_Y).size(13, 13));
        side.child(displayKey(view).pos(x + 14, OPTIONS_Y).size(13, 13));
        side.child(sortKey(view).pos(x + 28, OPTIONS_Y).size(13, 13));
        return side;
    }

    private static IconKey musicKey(VendingView view) {
        IconKey key = new IconKey(view, () -> VMTextures.musicTrack(VMConfig.MUSIC_TRACK.get()), () -> {
            MusicTrack[] all = MusicTrack.values();
            VMConfig.MUSIC_TRACK.set(all[(VMConfig.MUSIC_TRACK.get().ordinal() + 1) % all.length]);
            VMConfig.CLIENT_SPEC.save();
            VendingMusic.onTrackChanged();
        }).accent(Gfx.CYAN).iconSize(9).onScroll(delta -> {
            double volume = VMConfig.MUSIC_VOLUME.get() + (delta > 0 ? 0.05 : -0.05);
            volume = Math.round(Math.max(0, Math.min(1, volume)) * 20) / 20.0;
            VMConfig.MUSIC_VOLUME.set(volume);
            VMConfig.CLIENT_SPEC.save();
        });
        key.tooltipDynamic(t -> t.clearText()
            .addLine(Component.translatable("vendingmachine.gui.display_track")
                .append(" ").append(Component.translatable(VMConfig.MUSIC_TRACK.get().translationKey)))
            .addLine(Component.translatable("vendingmachine.gui.volume.tooltip_volume_display",
                Math.round(VMConfig.MUSIC_VOLUME.get() * 100)).withStyle(ChatFormatting.GRAY))
            .addLine(Component.translatable("vendingmachine.gui.volume.tooltip_scroll")
                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC)));
        key.tooltipAutoUpdate(true);
        return key;
    }

    private static IconKey displayKey(VendingView view) {
        IconKey key = new IconKey(view, () -> VMTextures.displayType(VMConfig.DISPLAY_TYPE.get()), () -> {
            DisplayType[] all = DisplayType.values();
            VMConfig.DISPLAY_TYPE.set(all[(VMConfig.DISPLAY_TYPE.get().ordinal() + 1) % all.length]);
            VMConfig.CLIENT_SPEC.save();
        }).accent(Gfx.CYAN).iconSize(9);
        key.tooltipDynamic(t -> t.clearText()
            .addLine(Component.translatable("vendingmachine.gui.display_mode")
                .append(" ").append(Component.translatable(VMConfig.DISPLAY_TYPE.get().translationKey))));
        key.tooltipAutoUpdate(true);
        return key;
    }

    private static IconKey sortKey(VendingView view) {
        IconKey key = new IconKey(view, () -> VMTextures.sortMode(VMConfig.SORT_MODE.get()), () -> {
            SortMode[] all = SortMode.values();
            VMConfig.SORT_MODE.set(all[(VMConfig.SORT_MODE.get().ordinal() + 1) % all.length]);
            VMConfig.CLIENT_SPEC.save();
            view.forceRefresh();
        }).accent(Gfx.CYAN).iconSize(9);
        key.tooltipDynamic(t -> t.clearText()
            .addLine(Component.translatable("vendingmachine.gui.display_sort")
                .append(" ").append(Component.translatable(VMConfig.SORT_MODE.get().translationKey))));
        key.tooltipAutoUpdate(true);
        return key;
    }

    /** Fixed side cabinet bolted to the right of the panel. */
    private static final class SidePanel extends ParentWidget<SidePanel> {

        @Override
        public void drawBackground(ModularGuiContext context, WidgetThemeEntry<?> widgetTheme) {
            GuiGraphics g = context.getGraphics();
            int w = getArea().width;
            int h = getArea().height;
            Gfx.plate(g, 0, 0, w, h);
            Gfx.rivets(g, 0, 0, w, h, 3);
            for (int seam : new int[] { OUT_Y - 4, OPTIONS_Y - 4 }) {
                Gfx.rect(g, 3, seam, w - 6, 1, Gfx.SEAM);
                Gfx.rect(g, 3, seam + 1, w - 6, 1, 0xFF4F5862);
            }
        }
    }

    // ---------- Painters ----------

    private static final IDrawable WELL = (context, x, y, w, h, theme) -> Gfx.well(context.getGraphics(), x, y, w, h);

    private static void body(GuiGraphics g, int w, int h) {
        Gfx.plate(g, 0, 0, w, h);
        Gfx.rivets(g, 0, 0, w, h, 3);
        for (int seam : new int[] { SHELF_Y - 2, LED_Y - 2, COIN_Y - 2 }) Gfx.rect(g, 6, seam, w - 12, 1, 0x2A000000);
        for (int i = 0; i < 4; i++) Gfx.rect(g, w - 26 + i * 5, h - 6, 3, 1, 0xFF14181C);
    }

    private static void titleStrip(GuiGraphics g, int w, int h, long now, VendingView view) {
        Gfx.rect(g, 0, 0, w, h, 0xFF15191E);
        Gfx.rect(g, 0, h - 1, w, 1, 0xFF4A535D);
        Gfx.hazard(g, 1, 1, 12, h - 2, Gfx.AMBER, Gfx.HAZARD_DARK, (int) (now / 160));
        float p = Gfx.pulse(now, 2200);
        int lamp = view.formed() ? Gfx.CYAN : Gfx.RED;
        Gfx.rect(g, 16, 5, 4, 4, Gfx.lerpColor(Gfx.shade(lamp, 0.3f), lamp, p));
        Gfx.glow(g, 16, 5, 4, 4, lamp, (int) (60 * p));
        String title = Component.translatable("block.vendingmachine.vending_machine").getString().toUpperCase(Locale.ROOT);
        Gfx.text(g, Gfx.fit(title, SEARCH_X - 4 - 4 - 24, 1f), 24, 4, 1f, Gfx.TEXT, true);
    }

    private static void banner(GuiGraphics g, int w, int h, String key, int color) {
        Gfx.rect(g, 0, 0, w, h, Gfx.FRAME);
        Gfx.rect(g, 1, 1, w - 2, h - 2, 0xFF15191E);
        Gfx.rect(g, 1, 1, 3, h - 2, color);
        Gfx.rect(g, w - 4, 1, 3, h - 2, color);
        Gfx.textCentered(g, Component.translatable(key).getString(), w / 2f, 3, 0.5f, color, false);
    }

    private static void coinWell(GuiGraphics g, int w, int h) {
        Gfx.well(g, 0, 0, w, h);
        if (!VMConfig.SHOW_COINS.get()) {
            Gfx.textCentered(g, Component.translatable("vendingmachine.gui.wallet_hidden").getString(), w / 2f,
                h / 2f - 2, 0.5f, Gfx.DIM, false);
        }
    }

    /** Hazard banner shown at the top of the shelf while the multiblock is not formed. */
    private static void offline(GuiGraphics g, int w, int h, long now) {
        Gfx.hazard(g, 0, 0, w, h, 0xFF4A1410, 0xFF15100F, (int) (now / 200));
        Gfx.outline(g, 0, 0, w, h, Gfx.RED);
        String text = Component.translatable("vendingmachine.gui.error.incomplete_structure").getString();
        Gfx.textCentered(g, Gfx.fit(text, w - 6, 1f), w / 2f, (h - 8) / 2f + 1, 1f, Gfx.RED, true);
    }

    private static final IDrawable SLOT_BACKGROUND = (context, x, y, w, h, theme) -> {
        GuiGraphics g = context.getGraphics();
        Gfx.rect(g, x, y, w, h, 0xFF0B0E11);
        Gfx.rect(g, x, y, w, 1, 0xFF050608);
        Gfx.rect(g, x, y, 1, h, 0xFF050608);
        Gfx.rect(g, x + 1, y + h - 1, w - 1, 1, 0xFF3F4750);
        Gfx.rect(g, x + w - 1, y + 1, 1, h - 1, 0xFF3F4750);
        Gfx.rect(g, x + 1, y + 1, w - 2, h - 2, 0xFF181D22);
    };

    /** Dark recessed slot, with the item tooltip workaround. */
    private static ItemSlot style(ItemSlot slot) {
        slot.disableThemeBackground(true);
        slot.background(SLOT_BACKGROUND);
        return ItemTooltips.fix(slot);
    }
}
