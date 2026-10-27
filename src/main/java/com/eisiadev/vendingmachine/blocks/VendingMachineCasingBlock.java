package com.eisiadev.vendingmachine.blocks;

import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;

import java.util.Locale;

public class VendingMachineCasingBlock extends Block {

    public enum Part implements StringRepresentable {

        NONE,
        POSTER_TOP,
        POSTER_MIDDLE,
        POSTER_BOTTOM,
        PANEL_TOP,
        PANEL_BOTTOM;

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public static final EnumProperty<Part> PART = EnumProperty.create("part", Part.class);
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty MIRRORED = BooleanProperty.create("mirrored");

    public VendingMachineCasingBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
            .setValue(PART, Part.NONE)
            .setValue(FACING, Direction.NORTH)
            .setValue(MIRRORED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PART, FACING, MIRRORED);
    }

    public BlockState withOverlay(Part part, Direction facing, boolean mirrored) {
        return defaultBlockState().setValue(PART, part).setValue(FACING, facing).setValue(MIRRORED, mirrored);
    }
}
