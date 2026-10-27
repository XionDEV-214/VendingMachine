package com.eisiadev.vendingmachine.client.vending.widget;

import com.eisiadev.vendingmachine.client.vending.Gfx;
import com.eisiadev.vendingmachine.client.vending.VendingView;
import com.eisiadev.vendingmachine.gui.VMTextures;
import com.eisiadev.vendingmachine.trade.TradeCategory;

import brachy.modularui.api.widget.Interactable;
import brachy.modularui.drawable.UITexture;
import brachy.modularui.screen.viewport.ModularGuiContext;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import org.jetbrains.annotations.NotNull;

/**
 * Vertical tab on the left edge of the panel, one per trade category. Drawn before the panel body so tabs look
 * like they sit behind the machine; the selected one sticks out further. Amber lamp = search matches here.
 */
public class CategoryTab extends VendingWidget<CategoryTab> implements Interactable {

    public static final int W = 30;
    public static final int H = 22;

    private final int index;
    private final TradeCategory category;
    private final UITexture icon;

    public CategoryTab(VendingView view, int index) {
        super(view);
        this.index = index;
        this.category = view.categories().get(index);
        this.icon = VMTextures.categoryIcon(category.getKey());
        size(W, H);
        tooltipDynamic(t -> {
            t.clearText().addLine(Component.translatable(category.getTranslationKey()));
            if (empty()) t.addLine(Component.translatable("vendingmachine.gui.tab.empty").withStyle(ChatFormatting.GRAY));
        });
        tooltipAutoUpdate(true);
    }

    private boolean empty() {
        return category == TradeCategory.FAVOURITES && view.list(TradeCategory.FAVOURITES).isEmpty();
    }

    @Override
    public @NotNull Result onMousePressed(int button) {
        if (button != 0) return Result.IGNORE;
        view.selectTab(index);
        return Result.SUCCESS;
    }

    @Override
    protected void render(ModularGuiContext context, GuiGraphics g, int w, int h, long now) {
        boolean active = view.activeTab() == index;
        boolean hovered = isHovering() && !empty();
        int x0 = active ? 0 : 4;

        Gfx.rect(g, x0, 0, w - x0, h, Gfx.FRAME);
        if (active) {
            Gfx.vgrad(g, x0 + 1, 1, w - x0 - 1, h - 2, Gfx.PLATE_TOP, Gfx.PLATE_BOTTOM);
            Gfx.rect(g, x0 + 1, 1, w - x0 - 1, 1, Gfx.BEVEL_HI);
            Gfx.rect(g, x0 + 1, 1, 2, h - 2, Gfx.CYAN);
            Gfx.glow(g, x0 + 1, 1, 2, h - 2, Gfx.CYAN, 60);
        } else {
            Gfx.vgrad(g, x0 + 1, 1, w - x0 - 1, h - 2, hovered ? 0xFF353C45 : 0xFF262B31,
                hovered ? 0xFF262B31 : 0xFF1A1E22);
            Gfx.rect(g, x0 + 1, 1, w - x0 - 1, 1, 0xFF444C56);
        }

        int iconX = x0 + (w - 3 - x0 - 20) / 2 + 2;
        icon.draw(context, iconX, (h - 16) / 2, 20, 16);
        if (empty()) Gfx.rect(g, x0 + 1, 1, w - x0 - 1, h - 2, 0x99101418);

        if (view.highlighted(category)) {
            float p = Gfx.pulse(now, 1200);
            Gfx.rect(g, x0 + 4, 3, 3, 3, Gfx.lerpColor(Gfx.shade(Gfx.AMBER, 0.5f), Gfx.AMBER, p));
            Gfx.glow(g, x0 + 4, 3, 3, 3, Gfx.AMBER, (int) (40 + 50 * p));
        }
    }
}
