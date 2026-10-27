package com.eisiadev.vendingmachine.gui.widget;

import brachy.modularui.animation.Animator;
import brachy.modularui.animation.IAnimatable;
import brachy.modularui.animation.MutableObjectAnimator;
import brachy.modularui.api.widget.IWidget;
import brachy.modularui.api.drawable.IDrawable;
import brachy.modularui.utils.Interpolation;
import brachy.modularui.utils.Interpolations;
import brachy.modularui.widgets.TransformWidget;
import brachy.modularui.widgets.slot.ItemSlot;
import brachy.modularui.widgets.slot.ModularSlot;

import net.minecraftforge.items.IItemHandler;

public final class FallingItemSlots {

    private static final int FALL_ANIMATION_DURATION = 1000;

    private final IItemHandler handler;
    private final String slotGroup;
    private final int chuteX;
    private final int chuteY;
    private final int maxX;
    private final int fallDistance;

    public FallingItemSlots(IItemHandler handler, String slotGroup, int chuteX, int chuteY, int maxX,
                            int fallDistance) {
        this.handler = handler;
        this.slotGroup = slotGroup;
        this.chuteX = chuteX;
        this.chuteY = chuteY;
        this.maxX = maxX;
        this.fallDistance = fallDistance;
    }

    private int restingX(int index) {
        final double phi = (Math.sqrt(5) - 1) / 2.0;
        return (int) ((index * phi % 1.0) * maxX);
    }

    public IWidget create(int index) {
        Pos offset = new Pos(0, 0);
        Animator fall = new MutableObjectAnimator<>(offset, new Pos(0, -(fallDistance + 1)), new Pos(0, 0))
            .bounds(0, 1)
            .curve(Interpolation.BOUNCE_OUT)
            .duration(FALL_ANIMATION_DURATION);

        ItemSlot slot = new ItemSlot().slot(new ModularSlot(handler, index)
            .accessibility(false, true)
            .slotGroup(slotGroup)
            .changeListener((newItem, onlyAmountChanged, client, init) -> {
                if (client && !init && !newItem.isEmpty() && !onlyAmountChanged) {
                    fall.reset();
                    fall.animate();
                }
            }));
        ItemTooltips.fix(slot);
        slot.pos(chuteX + restingX(index), chuteY + fallDistance);
        slot.background(IDrawable.EMPTY).disableHoverBackground();
        slot.setEnabledIf(s -> s.getSlot().hasItem());

        float depth = index * 0.5f;
        return new TransformWidget(slot)
            .transform(stack -> stack.translate(offset.x, offset.y, depth));
    }

    private static final class Pos implements IAnimatable<Pos> {

        private int x;
        private int y;

        Pos(int x, int y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public Pos interpolate(Pos start, Pos end, float t) {
            this.x = Interpolations.lerp(start.x, end.x, t);
            this.y = Interpolations.lerp(start.y, end.y, t);
            return this;
        }

        @Override
        public Pos copyOrImmutable() {
            return new Pos(x, y);
        }
    }
}
