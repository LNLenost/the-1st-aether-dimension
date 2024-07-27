package net.minecraft.data.models;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.gson.JsonElement;
import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.core.Direction;
import net.minecraft.core.FrontAndTop;
import net.minecraft.data.BlockFamilies;
import net.minecraft.data.BlockFamily;
import net.minecraft.data.models.blockstates.BlockStateGenerator;
import net.minecraft.data.models.blockstates.Condition;
import net.minecraft.data.models.blockstates.MultiPartGenerator;
import net.minecraft.data.models.blockstates.MultiVariantGenerator;
import net.minecraft.data.models.blockstates.PropertyDispatch;
import net.minecraft.data.models.blockstates.Variant;
import net.minecraft.data.models.blockstates.VariantProperties;
import net.minecraft.data.models.model.DelegatedModel;
import net.minecraft.data.models.model.ModelLocationUtils;
import net.minecraft.data.models.model.ModelTemplate;
import net.minecraft.data.models.model.ModelTemplates;
import net.minecraft.data.models.model.TextureMapping;
import net.minecraft.data.models.model.TextureSlot;
import net.minecraft.data.models.model.TexturedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CrafterBlock;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.MangrovePropaguleBlock;
import net.minecraft.world.level.block.PitcherCropBlock;
import net.minecraft.world.level.block.SnifferEggBlock;
import net.minecraft.world.level.block.VaultBlock;
import net.minecraft.world.level.block.entity.trialspawner.TrialSpawnerState;
import net.minecraft.world.level.block.entity.vault.VaultState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BambooLeaves;
import net.minecraft.world.level.block.state.properties.BellAttachType;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.ComparatorMode;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.DripstoneThickness;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.PistonType;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.level.block.state.properties.RedstoneSide;
import net.minecraft.world.level.block.state.properties.SculkSensorPhase;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.block.state.properties.StairsShape;
import net.minecraft.world.level.block.state.properties.StructureMode;
import net.minecraft.world.level.block.state.properties.Tilt;
import net.minecraft.world.level.block.state.properties.WallSide;

public class BlockModelGenerators {
    final Consumer<BlockStateGenerator> blockStateOutput;
    final BiConsumer<ResourceLocation, Supplier<JsonElement>> modelOutput;
    private final Consumer<Item> skippedAutoModelsOutput;
    final List<Block> nonOrientableTrapdoor = ImmutableList.of(Blocks.OAK_TRAPDOOR, Blocks.DARK_OAK_TRAPDOOR, Blocks.IRON_TRAPDOOR);
    final Map<Block, BlockModelGenerators.BlockStateGeneratorSupplier> fullBlockModelCustomGenerators = ImmutableMap.<Block, BlockModelGenerators.BlockStateGeneratorSupplier>builder()
        .put(Blocks.STONE, BlockModelGenerators::createMirroredCubeGenerator)
        .put(Blocks.DEEPSLATE, BlockModelGenerators::createMirroredColumnGenerator)
        .put(Blocks.MUD_BRICKS, BlockModelGenerators::createNorthWestMirroredCubeGenerator)
        .build();
    final Map<Block, TexturedModel> texturedModels = ImmutableMap.<Block, TexturedModel>builder()
        .put(Blocks.SANDSTONE, TexturedModel.TOP_BOTTOM_WITH_WALL.get(Blocks.SANDSTONE))
        .put(Blocks.RED_SANDSTONE, TexturedModel.TOP_BOTTOM_WITH_WALL.get(Blocks.RED_SANDSTONE))
        .put(Blocks.SMOOTH_SANDSTONE, TexturedModel.createAllSame(TextureMapping.getBlockTexture(Blocks.SANDSTONE, "_top")))
        .put(Blocks.SMOOTH_RED_SANDSTONE, TexturedModel.createAllSame(TextureMapping.getBlockTexture(Blocks.RED_SANDSTONE, "_top")))
        .put(
            Blocks.CUT_SANDSTONE,
            TexturedModel.COLUMN
                .get(Blocks.SANDSTONE)
                .updateTextures(p_176223_ -> p_176223_.put(TextureSlot.SIDE, TextureMapping.getBlockTexture(Blocks.CUT_SANDSTONE)))
        )
        .put(
            Blocks.CUT_RED_SANDSTONE,
            TexturedModel.COLUMN
                .get(Blocks.RED_SANDSTONE)
                .updateTextures(p_176211_ -> p_176211_.put(TextureSlot.SIDE, TextureMapping.getBlockTexture(Blocks.CUT_RED_SANDSTONE)))
        )
        .put(Blocks.QUARTZ_BLOCK, TexturedModel.COLUMN.get(Blocks.QUARTZ_BLOCK))
        .put(Blocks.SMOOTH_QUARTZ, TexturedModel.createAllSame(TextureMapping.getBlockTexture(Blocks.QUARTZ_BLOCK, "_bottom")))
        .put(Blocks.BLACKSTONE, TexturedModel.COLUMN_WITH_WALL.get(Blocks.BLACKSTONE))
        .put(Blocks.DEEPSLATE, TexturedModel.COLUMN_WITH_WALL.get(Blocks.DEEPSLATE))
        .put(
            Blocks.CHISELED_QUARTZ_BLOCK,
            TexturedModel.COLUMN
                .get(Blocks.CHISELED_QUARTZ_BLOCK)
                .updateTextures(p_176202_ -> p_176202_.put(TextureSlot.SIDE, TextureMapping.getBlockTexture(Blocks.CHISELED_QUARTZ_BLOCK)))
        )
        .put(Blocks.CHISELED_SANDSTONE, TexturedModel.COLUMN.get(Blocks.CHISELED_SANDSTONE).updateTextures(p_176190_ -> {
            p_176190_.put(TextureSlot.END, TextureMapping.getBlockTexture(Blocks.SANDSTONE, "_top"));
            p_176190_.put(TextureSlot.SIDE, TextureMapping.getBlockTexture(Blocks.CHISELED_SANDSTONE));
        }))
        .put(Blocks.CHISELED_RED_SANDSTONE, TexturedModel.COLUMN.get(Blocks.CHISELED_RED_SANDSTONE).updateTextures(p_176145_ -> {
            p_176145_.put(TextureSlot.END, TextureMapping.getBlockTexture(Blocks.RED_SANDSTONE, "_top"));
            p_176145_.put(TextureSlot.SIDE, TextureMapping.getBlockTexture(Blocks.CHISELED_RED_SANDSTONE));
        }))
        .put(Blocks.CHISELED_TUFF_BRICKS, TexturedModel.COLUMN_WITH_WALL.get(Blocks.CHISELED_TUFF_BRICKS))
        .put(Blocks.CHISELED_TUFF, TexturedModel.COLUMN_WITH_WALL.get(Blocks.CHISELED_TUFF))
        .build();
    static final Map<BlockFamily.Variant, BiConsumer<BlockModelGenerators.BlockFamilyProvider, Block>> SHAPE_CONSUMERS = ImmutableMap.<BlockFamily.Variant, BiConsumer<BlockModelGenerators.BlockFamilyProvider, Block>>builder()
        .put(BlockFamily.Variant.BUTTON, BlockModelGenerators.BlockFamilyProvider::button)
        .put(BlockFamily.Variant.DOOR, BlockModelGenerators.BlockFamilyProvider::door)
        .put(BlockFamily.Variant.CHISELED, BlockModelGenerators.BlockFamilyProvider::fullBlockVariant)
        .put(BlockFamily.Variant.CRACKED, BlockModelGenerators.BlockFamilyProvider::fullBlockVariant)
        .put(BlockFamily.Variant.CUSTOM_FENCE, BlockModelGenerators.BlockFamilyProvider::customFence)
        .put(BlockFamily.Variant.FENCE, BlockModelGenerators.BlockFamilyProvider::fence)
        .put(BlockFamily.Variant.CUSTOM_FENCE_GATE, BlockModelGenerators.BlockFamilyProvider::customFenceGate)
        .put(BlockFamily.Variant.FENCE_GATE, BlockModelGenerators.BlockFamilyProvider::fenceGate)
        .put(BlockFamily.Variant.SIGN, BlockModelGenerators.BlockFamilyProvider::sign)
        .put(BlockFamily.Variant.SLAB, BlockModelGenerators.BlockFamilyProvider::slab)
        .put(BlockFamily.Variant.STAIRS, BlockModelGenerators.BlockFamilyProvider::stairs)
        .put(BlockFamily.Variant.PRESSURE_PLATE, BlockModelGenerators.BlockFamilyProvider::pressurePlate)
        .put(BlockFamily.Variant.TRAPDOOR, BlockModelGenerators.BlockFamilyProvider::trapdoor)
        .put(BlockFamily.Variant.WALL, BlockModelGenerators.BlockFamilyProvider::wall)
        .build();
    public static final List<Pair<BooleanProperty, Function<ResourceLocation, Variant>>> MULTIFACE_GENERATOR = List.of(
        Pair.of(BlockStateProperties.NORTH, p_176234_ -> Variant.variant().with(VariantProperties.MODEL, p_176234_)),
        Pair.of(
            BlockStateProperties.EAST,
            p_176229_ -> Variant.variant()
                    .with(VariantProperties.MODEL, p_176229_)
                    .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90)
                    .with(VariantProperties.UV_LOCK, true)
        ),
        Pair.of(
            BlockStateProperties.SOUTH,
            p_176225_ -> Variant.variant()
                    .with(VariantProperties.MODEL, p_176225_)
                    .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180)
                    .with(VariantProperties.UV_LOCK, true)
        ),
        Pair.of(
            BlockStateProperties.WEST,
            p_176213_ -> Variant.variant()
                    .with(VariantProperties.MODEL, p_176213_)
                    .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270)
                    .with(VariantProperties.UV_LOCK, true)
        ),
        Pair.of(
            BlockStateProperties.UP,
            p_176204_ -> Variant.variant()
                    .with(VariantProperties.MODEL, p_176204_)
                    .with(VariantProperties.X_ROT, VariantProperties.Rotation.R270)
                    .with(VariantProperties.UV_LOCK, true)
        ),
        Pair.of(
            BlockStateProperties.DOWN,
            p_176195_ -> Variant.variant()
                    .with(VariantProperties.MODEL, p_176195_)
                    .with(VariantProperties.X_ROT, VariantProperties.Rotation.R90)
                    .with(VariantProperties.UV_LOCK, true)
        )
    );
    private static final Map<BlockModelGenerators.BookSlotModelCacheKey, ResourceLocation> CHISELED_BOOKSHELF_SLOT_MODEL_CACHE = new HashMap<>();

    private static BlockStateGenerator createMirroredCubeGenerator(
        Block p_176110_, ResourceLocation p_176111_, TextureMapping p_176112_, BiConsumer<ResourceLocation, Supplier<JsonElement>> p_176113_
    ) {
        ResourceLocation resourcelocation = ModelTemplates.CUBE_MIRRORED_ALL.create(p_176110_, p_176112_, p_176113_);
        return createRotatedVariant(p_176110_, p_176111_, resourcelocation);
    }

    private static BlockStateGenerator createNorthWestMirroredCubeGenerator(
        Block p_236317_, ResourceLocation p_236318_, TextureMapping p_236319_, BiConsumer<ResourceLocation, Supplier<JsonElement>> p_236320_
    ) {
        ResourceLocation resourcelocation = ModelTemplates.CUBE_NORTH_WEST_MIRRORED_ALL.create(p_236317_, p_236319_, p_236320_);
        return createSimpleBlock(p_236317_, resourcelocation);
    }

    private static BlockStateGenerator createMirroredColumnGenerator(
        Block p_176180_, ResourceLocation p_176181_, TextureMapping p_176182_, BiConsumer<ResourceLocation, Supplier<JsonElement>> p_176183_
    ) {
        ResourceLocation resourcelocation = ModelTemplates.CUBE_COLUMN_MIRRORED.create(p_176180_, p_176182_, p_176183_);
        return createRotatedVariant(p_176180_, p_176181_, resourcelocation).with(createRotatedPillar());
    }

    public BlockModelGenerators(
        Consumer<BlockStateGenerator> p_124481_, BiConsumer<ResourceLocation, Supplier<JsonElement>> p_124482_, Consumer<Item> p_124483_
    ) {
        this.blockStateOutput = p_124481_;
        this.modelOutput = p_124482_;
        this.skippedAutoModelsOutput = p_124483_;
    }

    void skipAutoItemBlock(Block p_124525_) {
        this.skippedAutoModelsOutput.accept(p_124525_.asItem());
    }

    void delegateItemModel(Block p_124798_, ResourceLocation p_124799_) {
        this.modelOutput.accept(ModelLocationUtils.getModelLocation(p_124798_.asItem()), new DelegatedModel(p_124799_));
    }

    private void delegateItemModel(Item p_124520_, ResourceLocation p_124521_) {
        this.modelOutput.accept(ModelLocationUtils.getModelLocation(p_124520_), new DelegatedModel(p_124521_));
    }

    void createSimpleFlatItemModel(Item p_124518_) {
        ModelTemplates.FLAT_ITEM.create(ModelLocationUtils.getModelLocation(p_124518_), TextureMapping.layer0(p_124518_), this.modelOutput);
    }

    private void createSimpleFlatItemModel(Block p_124729_) {
        Item item = p_124729_.asItem();
        if (item != Items.AIR) {
            ModelTemplates.FLAT_ITEM.create(ModelLocationUtils.getModelLocation(item), TextureMapping.layer0(p_124729_), this.modelOutput);
        }
    }

    private void createSimpleFlatItemModel(Block p_124576_, String p_124577_) {
        Item item = p_124576_.asItem();
        ModelTemplates.FLAT_ITEM
            .create(ModelLocationUtils.getModelLocation(item), TextureMapping.layer0(TextureMapping.getBlockTexture(p_124576_, p_124577_)), this.modelOutput);
    }

    private static PropertyDispatch createHorizontalFacingDispatch() {
        return PropertyDispatch.property(BlockStateProperties.HORIZONTAL_FACING)
            .select(Direction.EAST, Variant.variant().with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90))
            .select(Direction.SOUTH, Variant.variant().with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180))
            .select(Direction.WEST, Variant.variant().with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270))
            .select(Direction.NORTH, Variant.variant());
    }

    private static PropertyDispatch createHorizontalFacingDispatchAlt() {
        return PropertyDispatch.property(BlockStateProperties.HORIZONTAL_FACING)
            .select(Direction.SOUTH, Variant.variant())
            .select(Direction.WEST, Variant.variant().with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90))
            .select(Direction.NORTH, Variant.variant().with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180))
            .select(Direction.EAST, Variant.variant().with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270));
    }

    private static PropertyDispatch createTorchHorizontalDispatch() {
        return PropertyDispatch.property(BlockStateProperties.HORIZONTAL_FACING)
            .select(Direction.EAST, Variant.variant())
            .select(Direction.SOUTH, Variant.variant().with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90))
            .select(Direction.WEST, Variant.variant().with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180))
            .select(Direction.NORTH, Variant.variant().with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270));
    }

    private static PropertyDispatch createFacingDispatch() {
        return PropertyDispatch.property(BlockStateProperties.FACING)
            .select(Direction.DOWN, Variant.variant().with(VariantProperties.X_ROT, VariantProperties.Rotation.R90))
            .select(Direction.UP, Variant.variant().with(VariantProperties.X_ROT, VariantProperties.Rotation.R270))
            .select(Direction.NORTH, Variant.variant())
            .select(Direction.SOUTH, Variant.variant().with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180))
            .select(Direction.WEST, Variant.variant().with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270))
            .select(Direction.EAST, Variant.variant().with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90));
    }

    private static MultiVariantGenerator createRotatedVariant(Block p_124832_, ResourceLocation p_124833_) {
        return MultiVariantGenerator.multiVariant(p_124832_, createRotatedVariants(p_124833_));
    }

    private static Variant[] createRotatedVariants(ResourceLocation p_124689_) {
        return new Variant[]{
            Variant.variant().with(VariantProperties.MODEL, p_124689_),
            Variant.variant().with(VariantProperties.MODEL, p_124689_).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90),
            Variant.variant().with(VariantProperties.MODEL, p_124689_).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180),
            Variant.variant().with(VariantProperties.MODEL, p_124689_).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270)
        };
    }

    private static MultiVariantGenerator createRotatedVariant(Block p_124863_, ResourceLocation p_124864_, ResourceLocation p_124865_) {
        return MultiVariantGenerator.multiVariant(
            p_124863_,
            Variant.variant().with(VariantProperties.MODEL, p_124864_),
            Variant.variant().with(VariantProperties.MODEL, p_124865_),
            Variant.variant().with(VariantProperties.MODEL, p_124864_).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180),
            Variant.variant().with(VariantProperties.MODEL, p_124865_).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180)
        );
    }

    private static PropertyDispatch createBooleanModelDispatch(BooleanProperty p_124623_, ResourceLocation p_124624_, ResourceLocation p_124625_) {
        return PropertyDispatch.property(p_124623_)
            .select(true, Variant.variant().with(VariantProperties.MODEL, p_124624_))
            .select(false, Variant.variant().with(VariantProperties.MODEL, p_124625_));
    }

    private void createRotatedMirroredVariantBlock(Block p_124787_) {
        ResourceLocation resourcelocation = TexturedModel.CUBE.create(p_124787_, this.modelOutput);
        ResourceLocation resourcelocation1 = TexturedModel.CUBE_MIRRORED.create(p_124787_, this.modelOutput);
        this.blockStateOutput.accept(createRotatedVariant(p_124787_, resourcelocation, resourcelocation1));
    }

    private void createRotatedVariantBlock(Block p_124824_) {
        ResourceLocation resourcelocation = TexturedModel.CUBE.create(p_124824_, this.modelOutput);
        this.blockStateOutput.accept(createRotatedVariant(p_124824_, resourcelocation));
    }

    private void createBrushableBlock(Block p_277651_) {
        this.blockStateOutput
            .accept(
                MultiVariantGenerator.multiVariant(p_277651_)
                    .with(
                        PropertyDispatch.property(BlockStateProperties.DUSTED)
                            .generate(
                                p_277253_ -> {
                                    String s = "_" + p_277253_;
                                    ResourceLocation resourcelocation = TextureMapping.getBlockTexture(p_277651_, s);
                                    return Variant.variant()
                                        .with(
                                            VariantProperties.MODEL,
                                            ModelTemplates.CUBE_ALL
                                                .createWithSuffix(p_277651_, s, new TextureMapping().put(TextureSlot.ALL, resourcelocation), this.modelOutput)
                                        );
                                }
                            )
                    )
            );
        this.delegateItemModel(p_277651_, TextureMapping.getBlockTexture(p_277651_, "_0"));
    }

    static BlockStateGenerator createButton(Block p_124885_, ResourceLocation p_124886_, ResourceLocation p_124887_) {
        return MultiVariantGenerator.multiVariant(p_124885_)
            .with(
                PropertyDispatch.property(BlockStateProperties.POWERED)
                    .select(false, Variant.variant().with(VariantProperties.MODEL, p_124886_))
                    .select(true, Variant.variant().with(VariantProperties.MODEL, p_124887_))
            )
            .with(
                PropertyDispatch.properties(BlockStateProperties.ATTACH_FACE, BlockStateProperties.HORIZONTAL_FACING)
                    .select(AttachFace.FLOOR, Direction.EAST, Variant.variant().with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90))
                    .select(AttachFace.FLOOR, Direction.WEST, Variant.variant().with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270))
                    .select(AttachFace.FLOOR, Direction.SOUTH, Variant.variant().with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180))
                    .select(AttachFace.FLOOR, Direction.NORTH, Variant.variant())
                    .select(
                        AttachFace.WALL,
                        Direction.EAST,
                        Variant.variant()
                            .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90)
                            .with(VariantProperties.X_ROT, VariantProperties.Rotation.R90)
                            .with(VariantProperties.UV_LOCK, true)
                    )
                    .select(
                        AttachFace.WALL,
                        Direction.WEST,
                        Variant.variant()
                            .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270)
                            .with(VariantProperties.X_ROT, VariantProperties.Rotation.R90)
                            .with(VariantProperties.UV_LOCK, true)
                    )
                    .select(
                        AttachFace.WALL,
                        Direction.SOUTH,
                        Variant.variant()
                            .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180)
                            .with(VariantProperties.X_ROT, VariantProperties.Rotation.R90)
                            .with(VariantProperties.UV_LOCK, true)
                    )
                    .select(
                        AttachFace.WALL,
                        Direction.NORTH,
                        Variant.variant().with(VariantProperties.X_ROT, VariantProperties.Rotation.R90).with(VariantProperties.UV_LOCK, true)
                    )
                    .select(
                        AttachFace.CEILING,
                        Direction.EAST,
                        Variant.variant()
                            .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270)
                            .with(VariantProperties.X_ROT, VariantProperties.Rotation.R180)
                    )
                    .select(
                        AttachFace.CEILING,
                        Direction.WEST,
                        Variant.variant()
                            .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90)
                            .with(VariantProperties.X_ROT, VariantProperties.Rotation.R180)
                    )
                    .select(AttachFace.CEILING, Direction.SOUTH, Variant.variant().with(VariantProperties.X_ROT, VariantProperties.Rotation.R180))
                    .select(
                        AttachFace.CEILING,
                        Direction.NORTH,
                        Variant.variant()
                            .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180)
                            .with(VariantProperties.X_ROT, VariantProperties.Rotation.R180)
                    )
            );
    }

    private static PropertyDispatch.C4<Direction, DoubleBlockHalf, DoorHingeSide, Boolean> configureDoorHalf(
        PropertyDispatch.C4<Direction, DoubleBlockHalf, DoorHingeSide, Boolean> p_236305_,
        DoubleBlockHalf p_236306_,
        ResourceLocation p_236307_,
        ResourceLocation p_236308_,
        ResourceLocation p_236309_,
        ResourceLocation p_236310_
    ) {
        return p_236305_.select(Direction.EAST, p_236306_, DoorHingeSide.LEFT, false, Variant.variant().with(VariantProperties.MODEL, p_236307_))
            .select(
                Direction.SOUTH,
                p_236306_,
                DoorHingeSide.LEFT,
                false,
                Variant.variant().with(VariantProperties.MODEL, p_236307_).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90)
            )
            .select(
                Direction.WEST,
                p_236306_,
                DoorHingeSide.LEFT,
                false,
                Variant.variant().with(VariantProperties.MODEL, p_236307_).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180)
            )
            .select(
                Direction.NORTH,
                p_236306_,
                DoorHingeSide.LEFT,
                false,
                Variant.variant().with(VariantProperties.MODEL, p_236307_).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270)
            )
            .select(Direction.EAST, p_236306_, DoorHingeSide.RIGHT, false, Variant.variant().with(VariantProperties.MODEL, p_236309_))
            .select(
                Direction.SOUTH,
                p_236306_,
                DoorHingeSide.RIGHT,
                false,
                Variant.variant().with(VariantProperties.MODEL, p_236309_).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90)
            )
            .select(
                Direction.WEST,
                p_236306_,
                DoorHingeSide.RIGHT,
                false,
                Variant.variant().with(VariantProperties.MODEL, p_236309_).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180)
            )
            .select(
                Direction.NORTH,
                p_236306_,
                DoorHingeSide.RIGHT,
                false,
                Variant.variant().with(VariantProperties.MODEL, p_236309_).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270)
            )
            .select(
                Direction.EAST,
                p_236306_,
                DoorHingeSide.LEFT,
                true,
                Variant.variant().with(VariantProperties.MODEL, p_236308_).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90)
            )
            .select(
                Direction.SOUTH,
                p_236306_,
                DoorHingeSide.LEFT,
                true,
                Variant.variant().with(VariantProperties.MODEL, p_236308_).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180)
            )
            .select(
                Direction.WEST,
                p_236306_,
                DoorHingeSide.LEFT,
                true,
                Variant.variant().with(VariantProperties.MODEL, p_236308_).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270)
            )
            .select(Direction.NORTH, p_236306_, DoorHingeSide.LEFT, true, Variant.variant().with(VariantProperties.MODEL, p_236308_))
            .select(
                Direction.EAST,
                p_236306_,
                DoorHingeSide.RIGHT,
                true,
                Variant.variant().with(VariantProperties.MODEL, p_236310_).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270)
            )
            .select(Direction.SOUTH, p_236306_, DoorHingeSide.RIGHT, true, Variant.variant().with(VariantProperties.MODEL, p_236310_))
            .select(
                Direction.WEST,
                p_236306_,
                DoorHingeSide.RIGHT,
                true,
                Variant.variant().with(VariantProperties.MODEL, p_236310_).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90)
            )
            .select(
                Direction.NORTH,
                p_236306_,
                DoorHingeSide.RIGHT,
                true,
                Variant.variant().with(VariantProperties.MODEL, p_236310_).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180)
            );
    }

    private static BlockStateGenerator createDoor(
        Block p_236284_,
        ResourceLocation p_236285_,
        ResourceLocation p_236286_,
        ResourceLocation p_236287_,
        ResourceLocation p_236288_,
        ResourceLocation p_236289_,
        ResourceLocation p_236290_,
        ResourceLocation p_236291_,
        ResourceLocation p_236292_
    ) {
        return MultiVariantGenerator.multiVariant(p_236284_)
            .with(
                configureDoorHalf(
                    configureDoorHalf(
                        PropertyDispatch.properties(
                            BlockStateProperties.HORIZONTAL_FACING,
                            BlockStateProperties.DOUBLE_BLOCK_HALF,
                            BlockStateProperties.DOOR_HINGE,
                            BlockStateProperties.OPEN
                        ),
                        DoubleBlockHalf.LOWER,
                        p_236285_,
                        p_236286_,
                        p_236287_,
                        p_236288_
                    ),
                    DoubleBlockHalf.UPPER,
                    p_236289_,
                    p_236290_,
                    p_236291_,
                    p_236292_
                )
            );
    }

    static BlockStateGenerator createCustomFence(
        Block p_248625_,
        ResourceLocation p_248654_,
        ResourceLocation p_249827_,
        ResourceLocation p_248819_,
        ResourceLocation p_251062_,
        ResourceLocation p_249076_
    ) {
        return MultiPartGenerator.multiPart(p_248625_)
            .with(Variant.variant().with(VariantProperties.MODEL, p_248654_))
            .with(
                Condition.condition().term(BlockStateProperties.NORTH, true),
                Variant.variant().with(VariantProperties.MODEL, p_249827_).with(VariantProperties.UV_LOCK, false)
            )
            .with(
                Condition.condition().term(BlockStateProperties.EAST, true),
                Variant.variant().with(VariantProperties.MODEL, p_248819_).with(VariantProperties.UV_LOCK, false)
            )
            .with(
                Condition.condition().term(BlockStateProperties.SOUTH, true),
                Variant.variant().with(VariantProperties.MODEL, p_251062_).with(VariantProperties.UV_LOCK, false)
            )
            .with(
                Condition.condition().term(BlockStateProperties.WEST, true),
                Variant.variant().with(VariantProperties.MODEL, p_249076_).with(VariantProperties.UV_LOCK, false)
            );
    }

    static BlockStateGenerator createFence(Block p_124905_, ResourceLocation p_124906_, ResourceLocation p_124907_) {
        return MultiPartGenerator.multiPart(p_124905_)
            .with(Variant.variant().with(VariantProperties.MODEL, p_124906_))
            .with(
                Condition.condition().term(BlockStateProperties.NORTH, true),
                Variant.variant().with(VariantProperties.MODEL, p_124907_).with(VariantProperties.UV_LOCK, true)
            )
            .with(
                Condition.condition().term(BlockStateProperties.EAST, true),
                Variant.variant()
                    .with(VariantProperties.MODEL, p_124907_)
                    .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90)
                    .with(VariantProperties.UV_LOCK, true)
            )
            .with(
                Condition.condition().term(BlockStateProperties.SOUTH, true),
                Variant.variant()
                    .with(VariantProperties.MODEL, p_124907_)
                    .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180)
                    .with(VariantProperties.UV_LOCK, true)
            )
            .with(
                Condition.condition().term(BlockStateProperties.WEST, true),
                Variant.variant()
                    .with(VariantProperties.MODEL, p_124907_)
                    .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270)
                    .with(VariantProperties.UV_LOCK, true)
            );
    }

    static BlockStateGenerator createWall(Block p_124839_, ResourceLocation p_124840_, ResourceLocation p_124841_, ResourceLocation p_124842_) {
        return MultiPartGenerator.multiPart(p_124839_)
            .with(Condition.condition().term(BlockStateProperties.UP, true), Variant.variant().with(VariantProperties.MODEL, p_124840_))
            .with(
                Condition.condition().term(BlockStateProperties.NORTH_WALL, WallSide.LOW),
                Variant.variant().with(VariantProperties.MODEL, p_124841_).with(VariantProperties.UV_LOCK, true)
            )
            .with(
                Condition.condition().term(BlockStateProperties.EAST_WALL, WallSide.LOW),
                Variant.variant()
                    .with(VariantProperties.MODEL, p_124841_)
                    .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90)
                    .with(VariantProperties.UV_LOCK, true)
            )
            .with(
                Condition.condition().term(BlockStateProperties.SOUTH_WALL, WallSide.LOW),
                Variant.variant()
                    .with(VariantProperties.MODEL, p_124841_)
                    .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180)
                    .with(VariantProperties.UV_LOCK, true)
            )
            .with(
                Condition.condition().term(BlockStateProperties.WEST_WALL, WallSide.LOW),
                Variant.variant()
                    .with(VariantProperties.MODEL, p_124841_)
                    .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270)
                    .with(VariantProperties.UV_LOCK, true)
            )
            .with(
                Condition.condition().term(BlockStateProperties.NORTH_WALL, WallSide.TALL),
                Variant.variant().with(VariantProperties.MODEL, p_124842_).with(VariantProperties.UV_LOCK, true)
            )
            .with(
                Condition.condition().term(BlockStateProperties.EAST_WALL, WallSide.TALL),
                Variant.variant()
                    .with(VariantProperties.MODEL, p_124842_)
                    .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90)
                    .with(VariantProperties.UV_LOCK, true)
            )
            .with(
                Condition.condition().term(BlockStateProperties.SOUTH_WALL, WallSide.TALL),
                Variant.variant()
                    .with(VariantProperties.MODEL, p_124842_)
                    .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180)
                    .with(VariantProperties.UV_LOCK, true)
            )
            .with(
                Condition.condition().term(BlockStateProperties.WEST_WALL, WallSide.TALL),
                Variant.variant()
                    .with(VariantProperties.MODEL, p_124842_)
                    .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270)
                    .with(VariantProperties.UV_LOCK, true)
            );
    }

    static BlockStateGenerator createFenceGate(
        Block p_124810_, ResourceLocation p_124811_, ResourceLocation p_124812_, ResourceLocation p_124813_, ResourceLocation p_124814_, boolean p_251730_
    ) {
        return MultiVariantGenerator.multiVariant(p_124810_, Variant.variant().with(VariantProperties.UV_LOCK, p_251730_))
            .with(createHorizontalFacingDispatchAlt())
            .with(
                PropertyDispatch.properties(BlockStateProperties.IN_WALL, BlockStateProperties.OPEN)
                    .select(false, false, Variant.variant().with(VariantProperties.MODEL, p_124812_))
                    .select(true, false, Variant.variant().with(VariantProperties.MODEL, p_124814_))
                    .select(false, true, Variant.variant().with(VariantProperties.MODEL, p_124811_))
                    .select(true, true, Variant.variant().with(VariantProperties.MODEL, p_124813_))
            );
    }

    static BlockStateGenerator createStairs(Block p_124867_, ResourceLocation p_124868_, ResourceLocation p_124869_, ResourceLocation p_124870_) {
        return MultiVariantGenerator.multiVariant(p_124867_)
            .with(
                PropertyDispatch.properties(BlockStateProperties.HORIZONTAL_FACING, BlockStateProperties.HALF, BlockStateProperties.STAIRS_SHAPE)
                    .select(Direction.EAST, Half.BOTTOM, StairsShape.STRAIGHT, Variant.variant().with(VariantProperties.MODEL, p_124869_))
                    .select(
                        Direction.WEST,
                        Half.BOTTOM,
                        StairsShape.STRAIGHT,
                        Variant.variant()
                            .with(VariantProperties.MODEL, p_124869_)
                            .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180)
                            .with(VariantProperties.UV_LOCK, true)
                    )
                    .select(
                        Direction.SOUTH,
                        Half.BOTTOM,
                        StairsShape.STRAIGHT,
                        Variant.variant()
                            .with(VariantProperties.MODEL, p_124869_)
                            .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90)
                            .with(VariantProperties.UV_LOCK, true)
                    )
                    .select(
                        Direction.NORTH,
                        Half.BOTTOM,
                        StairsShape.STRAIGHT,
                        Variant.variant()
                            .with(VariantProperties.MODEL, p_124869_)
                            .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270)
                            .with(VariantProperties.UV_LOCK, true)
                    )
                    .select(Direction.EAST, Half.BOTTOM, StairsShape.OUTER_RIGHT, Variant.variant().with(VariantProperties.MODEL, p_124870_))
                    .select(
                        Direction.WEST,
                        Half.BOTTOM,
                        StairsShape.OUTER_RIGHT,
                        Variant.variant()
                            .with(VariantProperties.MODEL, p_124870_)
                            .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180)
                            .with(VariantProperties.UV_LOCK, true)
                    )
                    .select(
                        Direction.SOUTH,
                        Half.BOTTOM,
                        StairsShape.OUTER_RIGHT,
                        Variant.variant()
                            .with(VariantProperties.MODEL, p_124870_)
                            .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90)
                            .with(VariantProperties.UV_LOCK, true)
                    )
                    .select(
                        Direction.NORTH,
                        Half.BOTTOM,
                        StairsShape.OUTER_RIGHT,
                        Variant.variant()
                            .with(VariantProperties.MODEL, p_124870_)
                            .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270)
                            .with(VariantProperties.UV_LOCK, true)
                    )
                    .select(
                        Direction.EAST,
                        Half.BOTTOM,
                        StairsShape.OUTER_LEFT,
                        Variant.variant()
                            .with(VariantProperties.MODEL, p_124870_)
                            .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270)
                            .with(VariantProperties.UV_LOCK, true)
                    )
                    .select(
                        Direction.WEST,
                        Half.BOTTOM,
                        StairsShape.OUTER_LEFT,
                        Variant.variant()
                            .with(VariantProperties.MODEL, p_124870_)
                            .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90)
                            .with(VariantProperties.UV_LOCK, true)
                    )
                    .select(Direction.SOUTH, Half.BOTTOM, StairsShape.OUTER_LEFT, Variant.variant().with(VariantProperties.MODEL, p_124870_))
                    .select(
                        Direction.NORTH,
                        Half.BOTTOM,
                        StairsShape.OUTER_LEFT,
                        Variant.variant()
                            .with(VariantProperties.MODEL, p_124870_)
                            .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180)
                            .with(VariantProperties.UV_LOCK, true)
                    )
                    .select(Direction.EAST, Half.BOTTOM, StairsShape.INNER_RIGHT, Variant.variant().with(VariantProperties.MODEL, p_124868_))
                    .select(
                        Direction.WEST,
                        Half.BOTTOM,
                        StairsShape.INNER_RIGHT,
                        Variant.variant()
                            .with(VariantProperties.MODEL, p_124868_)
                            .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180)
                            .with(VariantProperties.UV_LOCK, true)
                    )
                    .select(
                        Direction.SOUTH,
                        Half.BOTTOM,
                        StairsShape.INNER_RIGHT,
                        Variant.variant()
                            .with(VariantProperties.MODEL, p_124868_)
                            .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90)
                            .with(VariantProperties.UV_LOCK, true)
                    )
                    .select(
                        Direction.NORTH,
                        Half.BOTTOM,
                        StairsShape.INNER_RIGHT,
                        Variant.variant()
                            .with(VariantProperties.MODEL, p_124868_)
                            .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270)
                            .with(VariantProperties.UV_LOCK, true)
                    )
                    .select(
                        Direction.EAST,
                        Half.BOTTOM,
                        StairsShape.INNER_LEFT,
                        Variant.variant()
                            .with(VariantProperties.MODEL, p_124868_)
                            .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270)
                            .with(VariantProperties.UV_LOCK, true)
                    )
                    .select(
                        Direction.WEST,
                        Half.BOTTOM,
                        StairsShape.INNER_LEFT,
                        Variant.variant()
                            .with(VariantProperties.MODEL, p_124868_)
                            .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90)
                            .with(VariantProperties.UV_LOCK, true)
                    )
                    .select(Direction.SOUTH, Half.BOTTOM, StairsShape.INNER_LEFT, Variant.variant().with(VariantProperties.MODEL, p_124868_))
                    .select(
                        Direction.NORTH,
                        Half.BOTTOM,
                        StairsShape.INNER_LEFT,
                        Variant.variant()
                            .with(VariantProperties.MODEL, p_124868_)
                            .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180)
                            .with(VariantProperties.UV_LOCK, true)
                    )
                    .select(
                        Direction.EAST,
                        Half.TOP,
                        StairsShape.STRAIGHT,
                        Variant.variant()
                            .with(VariantProperties.MODEL, p_124869_)
                            .with(VariantProperties.X_ROT, VariantProperties.Rotation.R180)
                            .with(VariantProperties.UV_LOCK, true)
                    )
                    .select(
                        Direction.WEST,
                        Half.TOP,
                        StairsShape.STRAIGHT,
                        Variant.variant()
                            .with(VariantProperties.MODEL, p_124869_)
                            .with(VariantProperties.X_ROT, VariantProperties.Rotation.R180)
                            .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180)
                            .with(VariantProperties.UV_LOCK, true)
                    )
                    .select(
                        Direction.SOUTH,
                        Half.TOP,
                        StairsShape.STRAIGHT,
                        Variant.variant()
                            .with(VariantProperties.MODEL, p_124869_)
                            .with(VariantProperties.X_ROT, VariantProperties.Rotation.R180)
                            .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90)
                            .with(VariantProperties.UV_LOCK, true)
                    )
                    .select(
                        Direction.NORTH,
                        Half.TOP,
                        StairsShape.STRAIGHT,
                        Variant.variant()
                            .with(VariantProperties.MODEL, p_124869_)
                            .with(VariantProperties.X_ROT, VariantProperties.Rotation.R180)
                            .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270)
                            .with(VariantProperties.UV_LOCK, true)
                    )
                    .select(
                        Direction.EAST,
                        Half.TOP,
                        StairsShape.OUTER_RIGHT,
                        Variant.variant()
                            .with(VariantProperties.MODEL, p_124870_)
                            .with(VariantProperties.X_ROT, VariantProperties.Rotation.R180)
                            .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90)
                            .with(VariantProperties.UV_LOCK, true)
                    )
                    .select(
                        Direction.WEST,
                        Half.TOP,
                        StairsShape.OUTER_RIGHT,
                        Variant.variant()
                            .with(VariantProperties.MODEL, p_124870_)
                            .with(VariantProperties.X_ROT, VariantProperties.Rotation.R180)
                            .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270)
                            .with(VariantProperties.UV_LOCK, true)
                    )
                    .select(
                        Direction.SOUTH,
                        Half.TOP,
                        StairsShape.OUTER_RIGHT,
                        Variant.variant()
                            .with(VariantProperties.MODEL, p_124870_)
                            .with(VariantProperties.X_ROT, VariantProperties.Rotation.R180)
                            .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180)
                            .with(VariantProperties.UV_LOCK, true)
                    )
                    .select(
                        Direction.NORTH,
                        Half.TOP,
                        StairsShape.OUTER_RIGHT,
                        Variant.variant()
                            .with(VariantProperties.MODEL, p_124870_)
                            .with(VariantProperties.X_ROT, VariantProperties.Rotation.R180)
                            .with(VariantProperties.UV_LOCK, true)
                    )
                    .select(
                        Direction.EAST,
                        Half.TOP,
                        StairsShape.OUTER_LEFT,
                        Variant.variant()
                            .with(VariantProperties.MODEL, p_124870_)
                            .with(VariantProperties.X_ROT, VariantProperties.Rotation.R180)
                            .with(VariantProperties.UV_LOCK, true)
                    )
                    .select(
                        Direction.WEST,
                        Half.TOP,
                        StairsShape.OUTER_LEFT,
                        Variant.variant()
                            .with(VariantProperties.MODEL, p_124870_)
                            .with(VariantProperties.X_ROT, VariantProperties.Rotation.R180)
                            .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180)
                            .with(VariantProperties.UV_LOCK, true)
                    )
                    .select(
                        Direction.SOUTH,
                        Half.TOP,
                        StairsShape.OUTER_LEFT,
                        Variant.variant()
                            .with(VariantProperties.MODEL, p_124870_)
                            .with(VariantProperties.X_ROT, VariantProperties.Rotation.R180)
                            .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90)
                            .with(VariantProperties.UV_LOCK, true)
                    )
                    .select(
                        Direction.NORTH,
                        Half.TOP,
                        StairsShape.OUTER_LEFT,
                        Variant.variant()
                            .with(VariantProperties.MODEL, p_124870_)
                            .with(VariantProperties.X_ROT, VariantProperties.Rotation.R180)
                            .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270)
                            .with(VariantProperties.UV_LOCK, true)
                    )
                    .select(
                        Direction.EAST,
                        Half.TOP,
                        StairsShape.INNER_RIGHT,
                        Variant.variant()
                            .with(VariantProperties.MODEL, p_124868_)
                            .with(VariantProperties.X_ROT, VariantProperties.Rotation.R180)
                            .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90)
                            .with(VariantProperties.UV_LOCK, true)
                    )
                    .select(
                        Direction.WEST,
                        Half.TOP,
                        StairsShape.INNER_RIGHT,
                        Variant.variant()
                            .with(VariantProperties.MODEL, p_124868_)
                            .with(VariantProperties.X_ROT, VariantProperties.Rotation.R180)
                            .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270)
                            .with(VariantProperties.UV_LOCK, true)
                    )
                    .select(
                        Direction.SOUTH,
                        Half.TOP,
                        StairsShape.INNER_RIGHT,
                        Variant.variant()
                            .with(VariantProperties.MODEL, p_124868_)
                            .with(VariantProperties.X_ROT, VariantProperties.Rotation.R180)
                            .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180)
                            .with(VariantProperties.UV_LOCK, true)
                    )
                    .select(
                        Direction.NORTH,
                        Half.TOP,
                        StairsShape.INNER_RIGHT,
                        Variant.variant()
                            .with(VariantProperties.MODEL, p_124868_)
                            .with(VariantProperties.X_ROT, VariantProperties.Rotation.R180)
                            .with(VariantProperties.UV_LOCK, true)
                    )
                    .select(
                        Direction.EAST,
                        Half.TOP,
                        StairsShape.INNER_LEFT,
                        Variant.variant()
                            .with(VariantProperties.MODEL, p_124868_)
                            .with(VariantProperties.X_ROT, VariantProperties.Rotation.R180)
                            .with(VariantProperties.UV_LOCK, true)
                    )
                    .select(
                        Direction.WEST,
                        Half.TOP,
                        StairsShape.INNER_LEFT,
                        Variant.variant()
                            .with(VariantProperties.MODEL, p_124868_)
                            .with(VariantProperties.X_ROT, VariantProperties.Rotation.R180)
                            .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180)
                            .with(VariantProperties.UV_LOCK, true)
                    )
                    .select(
                        Direction.SOUTH,
                        Half.TOP,
                        StairsShape.INNER_LEFT,
                        Variant.variant()
                            .with(VariantProperties.MODEL, p_124868_)
                            .with(VariantProperties.X_ROT, VariantProperties.Rotation.R180)
                            .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90)
                            .with(VariantProperties.UV_LOCK, true)
                    )
                    .select(
                        Direction.NORTH,
                        Half.TOP,
                        StairsShape.INNER_LEFT,
                        Variant.variant()
                            .with(VariantProperties.MODEL, p_124868_)
                            .with(VariantProperties.X_ROT, VariantProperties.Rotation.R180)
                            .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270)
                            .with(VariantProperties.UV_LOCK, true)
                    )
            );
    }

    private static BlockStateGenerator createOrientableTrapdoor(
        Block p_124889_, ResourceLocation p_124890_, ResourceLocation p_124891_, ResourceLocation p_124892_
    ) {
        return MultiVariantGenerator.multiVariant(p_124889_)
            .with(
                PropertyDispatch.properties(BlockStateProperties.HORIZONTAL_FACING, BlockStateProperties.HALF, BlockStateProperties.OPEN)
                    .select(Direction.NORTH, Half.BOTTOM, false, Variant.variant().with(VariantProperties.MODEL, p_124891_))
                    .select(
                        Direction.SOUTH,
                        Half.BOTTOM,
                        false,
                        Variant.variant().with(VariantProperties.MODEL, p_124891_).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180)
                    )
                    .select(
                        Direction.EAST,
                        Half.BOTTOM,
                        false,
                        Variant.variant().with(VariantProperties.MODEL, p_124891_).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90)
                    )
                    .select(
                        Direction.WEST,
                        Half.BOTTOM,
                        false,
                        Variant.variant().with(VariantProperties.MODEL, p_124891_).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270)
                    )
                    .select(Direction.NORTH, Half.TOP, false, Variant.variant().with(VariantProperties.MODEL, p_124890_))
                    .select(
                        Direction.SOUTH,
                        Half.TOP,
                        false,
                        Variant.variant().with(VariantProperties.MODEL, p_124890_).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180)
                    )
                    .select(
                        Direction.EAST,
                        Half.TOP,
                        false,
                        Variant.variant().with(VariantProperties.MODEL, p_124890_).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90)
                    )
                    .select(
                        Direction.WEST,
                        Half.TOP,
                        false,
                        Variant.variant().with(VariantProperties.MODEL, p_124890_).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270)
                    )
                    .select(Direction.NORTH, Half.BOTTOM, true, Variant.variant().with(VariantProperties.MODEL, p_124892_))
                    .select(
                        Direction.SOUTH,
                        Half.BOTTOM,
                        true,
                        Variant.variant().with(VariantProperties.MODEL, p_124892_).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180)
                    )
                    .select(
                        Direction.EAST,
                        Half.BOTTOM,
                        true,
                        Variant.variant().with(VariantProperties.MODEL, p_124892_).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90)
                    )
                    .select(
                        Direction.WEST,
                        Half.BOTTOM,
                        true,
                        Variant.variant().with(VariantProperties.MODEL, p_124892_).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270)
                    )
                    .select(
                        Direction.NORTH,
                        Half.TOP,
                        true,
                        Variant.variant()
                            .with(VariantProperties.MODEL, p_124892_)
                            .with(VariantProperties.X_ROT, VariantProperties.Rotation.R180)
                            .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180)
                    )
                    .select(
                        Direction.SOUTH,
                        Half.TOP,
                        true,
                        Variant.variant()
                            .with(VariantProperties.MODEL, p_124892_)
                            .with(VariantProperties.X_ROT, VariantProperties.Rotation.R180)
                            .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R0)
                    )
                    .select(
                        Direction.EAST,
                        Half.TOP,
                        true,
                        Variant.variant()
                            .with(VariantProperties.MODEL, p_124892_)
                            .with(VariantProperties.X_ROT, VariantProperties.Rotation.R180)
                            .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270)
                    )
                    .select(
                        Direction.WEST,
                        Half.TOP,
                        true,
                        Variant.variant()
                            .with(VariantProperties.MODEL, p_124892_)
                            .with(VariantProperties.X_ROT, VariantProperties.Rotation.R180)
                            .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90)
                    )
            );
    }

    private static BlockStateGenerator createTrapdoor(Block p_124909_, ResourceLocation p_124910_, ResourceLocation p_124911_, ResourceLocation p_124912_) {
        return MultiVariantGenerator.multiVariant(p_124909_)
            .with(
                PropertyDispatch.properties(BlockStateProperties.HORIZONTAL_FACING, BlockStateProperties.HALF, BlockStateProperties.OPEN)
                    .select(Direction.NORTH, Half.BOTTOM, false, Variant.variant().with(VariantProperties.MODEL, p_124911_))
                    .select(Direction.SOUTH, Half.BOTTOM, false, Variant.variant().with(VariantProperties.MODEL, p_124911_))
                    .select(Direction.EAST, Half.BOTTOM, false, Variant.variant().with(VariantProperties.MODEL, p_124911_))
                    .select(Direction.WEST, Half.BOTTOM, false, Variant.variant().with(VariantProperties.MODEL, p_124911_))
                    .select(Direction.NORTH, Half.TOP, false, Variant.variant().with(VariantProperties.MODEL, p_124910_))
                    .select(Direction.SOUTH, Half.TOP, false, Variant.variant().with(VariantProperties.MODEL, p_124910_))
                    .select(Direction.EAST, Half.TOP, false, Variant.variant().with(VariantProperties.MODEL, p_124910_))
                    .select(Direction.WEST, Half.TOP, false, Variant.variant().with(VariantProperties.MODEL, p_124910_))
                    .select(Direction.NORTH, Half.BOTTOM, true, Variant.variant().with(VariantProperties.MODEL, p_124912_))
                    .select(
                        Direction.SOUTH,
                        Half.BOTTOM,
                        true,
                        Variant.variant().with(VariantProperties.MODEL, p_124912_).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180)
                    )
                    .select(
                        Direction.EAST,
                        Half.BOTTOM,
                        true,
                        Variant.variant().with(VariantProperties.MODEL, p_124912_).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90)
                    )
                    .select(
                        Direction.WEST,
                        Half.BOTTOM,
                        true,
                        Variant.variant().with(VariantProperties.MODEL, p_124912_).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270)
                    )
                    .select(Direction.NORTH, Half.TOP, true, Variant.variant().with(VariantProperties.MODEL, p_124912_))
                    .select(
                        Direction.SOUTH,
                        Half.TOP,
                        true,
                        Variant.variant().with(VariantProperties.MODEL, p_124912_).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180)
                    )
                    .select(
                        Direction.EAST,
                        Half.TOP,
                        true,
                        Variant.variant().with(VariantProperties.MODEL, p_124912_).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90)
                    )
                    .select(
                        Direction.WEST,
                        Half.TOP,
                        true,
                        Variant.variant().with(VariantProperties.MODEL, p_124912_).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270)
                    )
            );
    }

    static MultiVariantGenerator createSimpleBlock(Block p_124860_, ResourceLocation p_124861_) {
        return MultiVariantGenerator.multiVariant(p_124860_, Variant.variant().with(VariantProperties.MODEL, p_124861_));
    }

    private static PropertyDispatch createRotatedPillar() {
        return PropertyDispatch.property(BlockStateProperties.AXIS)
            .select(Direction.Axis.Y, Variant.variant())
            .select(Direction.Axis.Z, Variant.variant().with(VariantProperties.X_ROT, VariantProperties.Rotation.R90))
            .select(
                Direction.Axis.X,
                Variant.variant().with(VariantProperties.X_ROT, VariantProperties.Rotation.R90).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90)
            );
    }

    static BlockStateGenerator createPillarBlockUVLocked(
        Block p_259670_, TextureMapping p_259852_, BiConsumer<ResourceLocation, Supplier<JsonElement>> p_259181_
    ) {
        ResourceLocation resourcelocation = ModelTemplates.CUBE_COLUMN_UV_LOCKED_X.create(p_259670_, p_259852_, p_259181_);
        ResourceLocation resourcelocation1 = ModelTemplates.CUBE_COLUMN_UV_LOCKED_Y.create(p_259670_, p_259852_, p_259181_);
        ResourceLocation resourcelocation2 = ModelTemplates.CUBE_COLUMN_UV_LOCKED_Z.create(p_259670_, p_259852_, p_259181_);
        ResourceLocation resourcelocation3 = ModelTemplates.CUBE_COLUMN.create(p_259670_, p_259852_, p_259181_);
        return MultiVariantGenerator.multiVariant(p_259670_, Variant.variant().with(VariantProperties.MODEL, resourcelocation3))
            .with(
                PropertyDispatch.property(BlockStateProperties.AXIS)
                    .select(Direction.Axis.X, Variant.variant().with(VariantProperties.MODEL, resourcelocation))
                    .select(Direction.Axis.Y, Variant.variant().with(VariantProperties.MODEL, resourcelocation1))
                    .select(Direction.Axis.Z, Variant.variant().with(VariantProperties.MODEL, resourcelocation2))
            );
    }

    static BlockStateGenerator createAxisAlignedPillarBlock(Block p_124882_, ResourceLocation p_124883_) {
        return MultiVariantGenerator.multiVariant(p_124882_, Variant.variant().with(VariantProperties.MODEL, p_124883_)).with(createRotatedPillar());
    }

    private void createAxisAlignedPillarBlockCustomModel(Block p_124902_, ResourceLocation p_124903_) {
        this.blockStateOutput.accept(createAxisAlignedPillarBlock(p_124902_, p_124903_));
    }

    public void createAxisAlignedPillarBlock(Block p_124587_, TexturedModel.Provider p_124588_) {
        ResourceLocation resourcelocation = p_124588_.create(p_124587_, this.modelOutput);
        this.blockStateOutput.accept(createAxisAlignedPillarBlock(p_124587_, resourcelocation));
    }

    private void createHorizontallyRotatedBlock(Block p_124745_, TexturedModel.Provider p_124746_) {
        ResourceLocation resourcelocation = p_124746_.create(p_124745_, this.modelOutput);
        this.blockStateOutput
            .accept(
                MultiVariantGenerator.multiVariant(p_124745_, Variant.variant().with(VariantProperties.MODEL, resourcelocation))
                    .with(createHorizontalFacingDispatch())
            );
    }

    static BlockStateGenerator createRotatedPillarWithHorizontalVariant(Block p_124925_, ResourceLocation p_124926_, ResourceLocation p_124927_) {
        return MultiVariantGenerator.multiVariant(p_124925_)
            .with(
                PropertyDispatch.property(BlockStateProperties.AXIS)
                    .select(Direction.Axis.Y, Variant.variant().with(VariantProperties.MODEL, p_124926_))
                    .select(
                        Direction.Axis.Z,
                        Variant.variant().with(VariantProperties.MODEL, p_124927_).with(VariantProperties.X_ROT, VariantProperties.Rotation.R90)
                    )
                    .select(
                        Direction.Axis.X,
                        Variant.variant()
                            .with(VariantProperties.MODEL, p_124927_)
                            .with(VariantProperties.X_ROT, VariantProperties.Rotation.R90)
                            .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90)
                    )
            );
    }

    private void createRotatedPillarWithHorizontalVariant(Block p_124590_, TexturedModel.Provider p_124591_, TexturedModel.Provider p_124592_) {
        ResourceLocation resourcelocation = p_124591_.create(p_124590_, this.modelOutput);
        ResourceLocation resourcelocation1 = p_124592_.create(p_124590_, this.modelOutput);
        this.blockStateOutput.accept(createRotatedPillarWithHorizontalVariant(p_124590_, resourcelocation, resourcelocation1));
    }

    private ResourceLocation createSuffixedVariant(
        Block p_124579_, String p_124580_, ModelTemplate p_124581_, Function<ResourceLocation, TextureMapping> p_124582_
    ) {
        return p_124581_.createWithSuffix(p_124579_, p_124580_, p_124582_.apply(TextureMapping.getBlockTexture(p_124579_, p_124580_)), this.modelOutput);
    }

    static BlockStateGenerator createPressurePlate(Block p_124942_, ResourceLocation p_124943_, ResourceLocation p_124944_) {
        return MultiVariantGenerator.multiVariant(p_124942_).with(createBooleanModelDispatch(BlockStateProperties.POWERED, p_124944_, p_124943_));
    }

    static BlockStateGenerator createSlab(Block p_124929_, ResourceLocation p_124930_, ResourceLocation p_124931_, ResourceLocation p_124932_) {
        return MultiVariantGenerator.multiVariant(p_124929_)
            .with(
                PropertyDispatch.property(BlockStateProperties.SLAB_TYPE)
                    .select(SlabType.BOTTOM, Variant.variant().with(VariantProperties.MODEL, p_124930_))
                    .select(SlabType.TOP, Variant.variant().with(VariantProperties.MODEL, p_124931_))
                    .select(SlabType.DOUBLE, Variant.variant().with(VariantProperties.MODEL, p_124932_))
            );
    }

    public void createTrivialCube(Block p_124852_) {
        this.createTrivialBlock(p_124852_, TexturedModel.CUBE);
    }

    public void createTrivialBlock(Block p_124795_, TexturedModel.Provider p_124796_) {
        this.blockStateOutput.accept(createSimpleBlock(p_124795_, p_124796_.create(p_124795_, this.modelOutput)));
    }

    private void createTrivialBlock(Block p_124568_, TextureMapping p_124569_, ModelTemplate p_124570_) {
        ResourceLocation resourcelocation = p_124570_.create(p_124568_, p_124569_, this.modelOutput);
        this.blockStateOutput.accept(createSimpleBlock(p_124568_, resourcelocation));
    }

    private BlockModelGenerators.BlockFamilyProvider family(Block p_124877_) {
        TexturedModel texturedmodel = this.texturedModels.getOrDefault(p_124877_, TexturedModel.CUBE.get(p_124877_));
        return new BlockModelGenerators.BlockFamilyProvider(texturedmodel.getMapping()).fullBlock(p_124877_, texturedmodel.getTemplate());
    }

    public void createHangingSign(Block p_249023_, Block p_250861_, Block p_250943_) {
        TextureMapping texturemapping = TextureMapping.particle(p_249023_);
        ResourceLocation resourcelocation = ModelTemplates.PARTICLE_ONLY.create(p_250861_, texturemapping, this.modelOutput);
        this.blockStateOutput.accept(createSimpleBlock(p_250861_, resourcelocation));
        this.blockStateOutput.accept(createSimpleBlock(p_250943_, resourcelocation));
        this.createSimpleFlatItemModel(p_250861_.asItem());
        this.skipAutoItemBlock(p_250943_);
    }

    void createDoor(Block p_124897_) {
        TextureMapping texturemapping = TextureMapping.door(p_124897_);
        ResourceLocation resourcelocation = ModelTemplates.DOOR_BOTTOM_LEFT.create(p_124897_, texturemapping, this.modelOutput);
        ResourceLocation resourcelocation1 = ModelTemplates.DOOR_BOTTOM_LEFT_OPEN.create(p_124897_, texturemapping, this.modelOutput);
        ResourceLocation resourcelocation2 = ModelTemplates.DOOR_BOTTOM_RIGHT.create(p_124897_, texturemapping, this.modelOutput);
        ResourceLocation resourcelocation3 = ModelTemplates.DOOR_BOTTOM_RIGHT_OPEN.create(p_124897_, texturemapping, this.modelOutput);
        ResourceLocation resourcelocation4 = ModelTemplates.DOOR_TOP_LEFT.create(p_124897_, texturemapping, this.modelOutput);
        ResourceLocation resourcelocation5 = ModelTemplates.DOOR_TOP_LEFT_OPEN.create(p_124897_, texturemapping, this.modelOutput);
        ResourceLocation resourcelocation6 = ModelTemplates.DOOR_TOP_RIGHT.create(p_124897_, texturemapping, this.modelOutput);
        ResourceLocation resourcelocation7 = ModelTemplates.DOOR_TOP_RIGHT_OPEN.create(p_124897_, texturemapping, this.modelOutput);
        this.createSimpleFlatItemModel(p_124897_.asItem());
        this.blockStateOutput
            .accept(
                createDoor(
                    p_124897_,
                    resourcelocation,
                    resourcelocation1,
                    resourcelocation2,
                    resourcelocation3,
                    resourcelocation4,
                    resourcelocation5,
                    resourcelocation6,
                    resourcelocation7
                )
            );
    }

    private void copyDoorModel(Block p_308919_, Block p_308994_) {
        ResourceLocation resourcelocation = ModelTemplates.DOOR_BOTTOM_LEFT.getDefaultModelLocation(p_308919_);
        ResourceLocation resourcelocation1 = ModelTemplates.DOOR_BOTTOM_LEFT_OPEN.getDefaultModelLocation(p_308919_);
        ResourceLocation resourcelocation2 = ModelTemplates.DOOR_BOTTOM_RIGHT.getDefaultModelLocation(p_308919_);
        ResourceLocation resourcelocation3 = ModelTemplates.DOOR_BOTTOM_RIGHT_OPEN.getDefaultModelLocation(p_308919_);
        ResourceLocation resourcelocation4 = ModelTemplates.DOOR_TOP_LEFT.getDefaultModelLocation(p_308919_);
        ResourceLocation resourcelocation5 = ModelTemplates.DOOR_TOP_LEFT_OPEN.getDefaultModelLocation(p_308919_);
        ResourceLocation resourcelocation6 = ModelTemplates.DOOR_TOP_RIGHT.getDefaultModelLocation(p_308919_);
        ResourceLocation resourcelocation7 = ModelTemplates.DOOR_TOP_RIGHT_OPEN.getDefaultModelLocation(p_308919_);
        this.delegateItemModel(p_308994_, ModelLocationUtils.getModelLocation(p_308919_.asItem()));
        this.blockStateOutput
            .accept(
                createDoor(
                    p_308994_,
                    resourcelocation,
                    resourcelocation1,
                    resourcelocation2,
                    resourcelocation3,
                    resourcelocation4,
                    resourcelocation5,
                    resourcelocation6,
                    resourcelocation7
                )
            );
    }

    void createOrientableTrapdoor(Block p_124917_) {
        TextureMapping texturemapping = TextureMapping.defaultTexture(p_124917_);
        ResourceLocation resourcelocation = ModelTemplates.ORIENTABLE_TRAPDOOR_TOP.create(p_124917_, texturemapping, this.modelOutput);
        ResourceLocation resourcelocation1 = ModelTemplates.ORIENTABLE_TRAPDOOR_BOTTOM.create(p_124917_, texturemapping, this.modelOutput);
        ResourceLocation resourcelocation2 = ModelTemplates.ORIENTABLE_TRAPDOOR_OPEN.create(p_124917_, texturemapping, this.modelOutput);
        this.blockStateOutput.accept(createOrientableTrapdoor(p_124917_, resourcelocation, resourcelocation1, resourcelocation2));
        this.delegateItemModel(p_124917_, resourcelocation1);
    }

    void createTrapdoor(Block p_124937_) {
        TextureMapping texturemapping = TextureMapping.defaultTexture(p_124937_);
        ResourceLocation resourcelocation = ModelTemplates.TRAPDOOR_TOP.create(p_124937_, texturemapping, this.modelOutput);
        ResourceLocation resourcelocation1 = ModelTemplates.TRAPDOOR_BOTTOM.create(p_124937_, texturemapping, this.modelOutput);
        ResourceLocation resourcelocation2 = ModelTemplates.TRAPDOOR_OPEN.create(p_124937_, texturemapping, this.modelOutput);
        this.blockStateOutput.accept(createTrapdoor(p_124937_, resourcelocation, resourcelocation1, resourcelocation2));
        this.delegateItemModel(p_124937_, resourcelocation1);
    }

    private void copyTrapdoorModel(Block p_309079_, Block p_309124_) {
        ResourceLocation resourcelocation = ModelTemplates.TRAPDOOR_TOP.getDefaultModelLocation(p_309079_);
        ResourceLocation resourcelocation1 = ModelTemplates.TRAPDOOR_BOTTOM.getDefaultModelLocation(p_309079_);
        ResourceLocation resourcelocation2 = ModelTemplates.TRAPDOOR_OPEN.getDefaultModelLocation(p_309079_);
        this.delegateItemModel(p_309124_, ModelLocationUtils.getModelLocation(p_309079_.asItem()));
        this.blockStateOutput.accept(createTrapdoor(p_309124_, resourcelocation, resourcelocation1, resourcelocation2));
    }

    private void createBigDripLeafBlock() {
        this.skipAutoItemBlock(Blocks.BIG_DRIPLEAF);
        ResourceLocation resourcelocation = ModelLocationUtils.getModelLocation(Blocks.BIG_DRIPLEAF);
        ResourceLocation resourcelocation1 = ModelLocationUtils.getModelLocation(Blocks.BIG_DRIPLEAF, "_partial_tilt");
        ResourceLocation resourcelocation2 = ModelLocationUtils.getModelLocation(Blocks.BIG_DRIPLEAF, "_full_tilt");
        this.blockStateOutput
            .accept(
                MultiVariantGenerator.multiVariant(Blocks.BIG_DRIPLEAF)
                    .with(createHorizontalFacingDispatch())
                    .with(
                        PropertyDispatch.property(BlockStateProperties.TILT)
                            .select(Tilt.NONE, Variant.variant().with(VariantProperties.MODEL, resourcelocation))
                            .select(Tilt.UNSTABLE, Variant.variant().with(VariantProperties.MODEL, resourcelocation))
                            .select(Tilt.PARTIAL, Variant.variant().with(VariantProperties.MODEL, resourcelocation1))
                            .select(Tilt.FULL, Variant.variant().with(VariantProperties.MODEL, resourcelocation2))
                    )
            );
    }

    private BlockModelGenerators.WoodProvider woodProvider(Block p_124949_) {
        return new BlockModelGenerators.WoodProvider(TextureMapping.logColumn(p_124949_));
    }

    private void createNonTemplateModelBlock(Block p_124961_) {
        this.createNonTemplateModelBlock(p_124961_, p_124961_);
    }

    private void createNonTemplateModelBlock(Block p_124534_, Block p_124535_) {
        this.blockStateOutput.accept(createSimpleBlock(p_124534_, ModelLocationUtils.getModelLocation(p_124535_)));
    }

    private void createCrossBlockWithDefaultItem(Block p_124558_, BlockModelGenerators.TintState p_124559_) {
        this.createSimpleFlatItemModel(p_124558_);
        this.createCrossBlock(p_124558_, p_124559_);
    }

    private void createCrossBlockWithDefaultItem(Block p_124561_, BlockModelGenerators.TintState p_124562_, TextureMapping p_124563_) {
        this.createSimpleFlatItemModel(p_124561_);
        this.createCrossBlock(p_124561_, p_124562_, p_124563_);
    }

    private void createCrossBlock(Block p_124738_, BlockModelGenerators.TintState p_124739_) {
        TextureMapping texturemapping = TextureMapping.cross(p_124738_);
        this.createCrossBlock(p_124738_, p_124739_, texturemapping);
    }

    private void createCrossBlock(Block p_124741_, BlockModelGenerators.TintState p_124742_, TextureMapping p_124743_) {
        ResourceLocation resourcelocation = p_124742_.getCross().create(p_124741_, p_124743_, this.modelOutput);
        this.blockStateOutput.accept(createSimpleBlock(p_124741_, resourcelocation));
    }

    private void createCrossBlock(Block p_273533_, BlockModelGenerators.TintState p_273521_, Property<Integer> p_273430_, int... p_273001_) {
        if (p_273430_.getPossibleValues().size() != p_273001_.length) {
            throw new IllegalArgumentException("missing values for property: " + p_273430_);
        } else {
            PropertyDispatch propertydispatch = PropertyDispatch.property(p_273430_).generate(p_272381_ -> {
                String s = "_stage" + p_273001_[p_272381_];
                TextureMapping texturemapping = TextureMapping.cross(TextureMapping.getBlockTexture(p_273533_, s));
                ResourceLocation resourcelocation = p_273521_.getCross().createWithSuffix(p_273533_, s, texturemapping, this.modelOutput);
                return Variant.variant().with(VariantProperties.MODEL, resourcelocation);
            });
            this.createSimpleFlatItemModel(p_273533_.asItem());
            this.blockStateOutput.accept(MultiVariantGenerator.multiVariant(p_273533_).with(propertydispatch));
        }
    }

    private void createPlant(Block p_124546_, Block p_124547_, BlockModelGenerators.TintState p_124548_) {
        this.createCrossBlockWithDefaultItem(p_124546_, p_124548_);
        TextureMapping texturemapping = TextureMapping.plant(p_124546_);
        ResourceLocation resourcelocation = p_124548_.getCrossPot().create(p_124547_, texturemapping, this.modelOutput);
        this.blockStateOutput.accept(createSimpleBlock(p_124547_, resourcelocation));
    }

    private void createCoralFans(Block p_124731_, Block p_124732_) {
        TexturedModel texturedmodel = TexturedModel.CORAL_FAN.get(p_124731_);
        ResourceLocation resourcelocation = texturedmodel.create(p_124731_, this.modelOutput);
        this.blockStateOutput.accept(createSimpleBlock(p_124731_, resourcelocation));
        ResourceLocation resourcelocation1 = ModelTemplates.CORAL_WALL_FAN.create(p_124732_, texturedmodel.getMapping(), this.modelOutput);
        this.blockStateOutput
            .accept(
                MultiVariantGenerator.multiVariant(p_124732_, Variant.variant().with(VariantProperties.MODEL, resourcelocation1))
                    .with(createHorizontalFacingDispatch())
            );
        this.createSimpleFlatItemModel(p_124731_);
    }

    private void createStems(Block p_124789_, Block p_124790_) {
        this.createSimpleFlatItemModel(p_124789_.asItem());
        TextureMapping texturemapping = TextureMapping.stem(p_124789_);
        TextureMapping texturemapping1 = TextureMapping.attachedStem(p_124789_, p_124790_);
        ResourceLocation resourcelocation = ModelTemplates.ATTACHED_STEM.create(p_124790_, texturemapping1, this.modelOutput);
        this.blockStateOutput
            .accept(
                MultiVariantGenerator.multiVariant(p_124790_, Variant.variant().with(VariantProperties.MODEL, resourcelocation))
                    .with(
                        PropertyDispatch.property(BlockStateProperties.HORIZONTAL_FACING)
                            .select(Direction.WEST, Variant.variant())
                            .select(Direction.SOUTH, Variant.variant().with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270))
                            .select(Direction.NORTH, Variant.variant().with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90))
                            .select(Direction.EAST, Variant.variant().with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180))
                    )
            );
        this.blockStateOutput
            .accept(
                MultiVariantGenerator.multiVariant(p_124789_)
                    .with(
                        PropertyDispatch.property(BlockStateProperties.AGE_7)
                            .generate(
                                p_176108_ -> Variant.variant()
                                        .with(VariantProperties.MODEL, ModelTemplates.STEMS[p_176108_].create(p_124789_, texturemapping, this.modelOutput))
                            )
                    )
            );
    }

    private void createPitcherPlant() {
        Block block = Blocks.PITCHER_PLANT;
        this.createSimpleFlatItemModel(block.asItem());
        ResourceLocation resourcelocation = ModelLocationUtils.getModelLocation(block, "_top");
        ResourceLocation resourcelocation1 = ModelLocationUtils.getModelLocation(block, "_bottom");
        this.createDoubleBlock(block, resourcelocation, resourcelocation1);
    }

    private void createPitcherCrop() {
        Block block = Blocks.PITCHER_CROP;
        this.createSimpleFlatItemModel(block.asItem());
        PropertyDispatch propertydispatch = PropertyDispatch.properties(PitcherCropBlock.AGE, BlockStateProperties.DOUBLE_BLOCK_HALF)
            .generate((p_339371_, p_339372_) -> {
                return switch (p_339372_) {
                    case UPPER -> Variant.variant().with(VariantProperties.MODEL, ModelLocationUtils.getModelLocation(block, "_top_stage_" + p_339371_));
                    case LOWER -> Variant.variant().with(VariantProperties.MODEL, ModelLocationUtils.getModelLocation(block, "_bottom_stage_" + p_339371_));
                };
            });
        this.blockStateOutput.accept(MultiVariantGenerator.multiVariant(block).with(propertydispatch));
    }

    private void createCoral(
        Block p_124537_, Block p_124538_, Block p_124539_, Block p_124540_, Block p_124541_, Block p_124542_, Block p_124543_, Block p_124544_
    ) {
        this.createCrossBlockWithDefaultItem(p_124537_, BlockModelGenerators.TintState.NOT_TINTED);
        this.createCrossBlockWithDefaultItem(p_124538_, BlockModelGenerators.TintState.NOT_TINTED);
        this.createTrivialCube(p_124539_);
        this.createTrivialCube(p_124540_);
        this.createCoralFans(p_124541_, p_124543_);
        this.createCoralFans(p_124542_, p_124544_);
    }

    private void createDoublePlant(Block p_124792_, BlockModelGenerators.TintState p_124793_) {
        this.createSimpleFlatItemModel(p_124792_, "_top");
        ResourceLocation resourcelocation = this.createSuffixedVariant(p_124792_, "_top", p_124793_.getCross(), TextureMapping::cross);
        ResourceLocation resourcelocation1 = this.createSuffixedVariant(p_124792_, "_bottom", p_124793_.getCross(), TextureMapping::cross);
        this.createDoubleBlock(p_124792_, resourcelocation, resourcelocation1);
    }

    private void createSunflower() {
        this.createSimpleFlatItemModel(Blocks.SUNFLOWER, "_front");
        ResourceLocation resourcelocation = ModelLocationUtils.getModelLocation(Blocks.SUNFLOWER, "_top");
        ResourceLocation resourcelocation1 = this.createSuffixedVariant(
            Blocks.SUNFLOWER, "_bottom", BlockModelGenerators.TintState.NOT_TINTED.getCross(), TextureMapping::cross
        );
        this.createDoubleBlock(Blocks.SUNFLOWER, resourcelocation, resourcelocation1);
    }

    private void createTallSeagrass() {
        ResourceLocation resourcelocation = this.createSuffixedVariant(Blocks.TALL_SEAGRASS, "_top", ModelTemplates.SEAGRASS, TextureMapping::defaultTexture);
        ResourceLocation resourcelocation1 = this.createSuffixedVariant(
            Blocks.TALL_SEAGRASS, "_bottom", ModelTemplates.SEAGRASS, TextureMapping::defaultTexture
        );
        this.createDoubleBlock(Blocks.TALL_SEAGRASS, resourcelocation, resourcelocation1);
    }

    private void createSmallDripleaf() {
        this.skipAutoItemBlock(Blocks.SMALL_DRIPLEAF);
        ResourceLocation resourcelocation = ModelLocationUtils.getModelLocation(Blocks.SMALL_DRIPLEAF, "_top");
        ResourceLocation resourcelocation1 = ModelLocationUtils.getModelLocation(Blocks.SMALL_DRIPLEAF, "_bottom");
        this.blockStateOutput
            .accept(
                MultiVariantGenerator.multiVariant(Blocks.SMALL_DRIPLEAF)
                    .with(createHorizontalFacingDispatch())
                    .with(
                        PropertyDispatch.property(BlockStateProperties.DOUBLE_BLOCK_HALF)
                            .select(DoubleBlockHalf.LOWER, Variant.variant().with(VariantProperties.MODEL, resourcelocation1))
                            .select(DoubleBlockHalf.UPPER, Variant.variant().with(VariantProperties.MODEL, resourcelocation))
                    )
            );
    }

    private void createDoubleBlock(Block p_124954_, ResourceLocation p_124955_, ResourceLocation p_124956_) {
        this.blockStateOutput
            .accept(
                MultiVariantGenerator.multiVariant(p_124954_)
                    .with(
                        PropertyDispatch.property(BlockStateProperties.DOUBLE_BLOCK_HALF)
                            .select(DoubleBlockHalf.LOWER, Variant.variant().with(VariantProperties.MODEL, p_124956_))
                            .select(DoubleBlockHalf.UPPER, Variant.variant().with(VariantProperties.MODEL, p_124955_))
                    )
            );
    }

    private void createPassiveRail(Block p_124969_) {
        TextureMapping texturemapping = TextureMapping.rail(p_124969_);
        TextureMapping texturemapping1 = TextureMapping.rail(TextureMapping.getBlockTexture(p_124969_, "_corner"));
        ResourceLocation resourcelocation = ModelTemplates.RAIL_FLAT.create(p_124969_, texturemapping, this.modelOutput);
        ResourceLocation resourcelocation1 = ModelTemplates.RAIL_CURVED.create(p_124969_, texturemapping1, this.modelOutput);
        ResourceLocation resourcelocation2 = ModelTemplates.RAIL_RAISED_NE.create(p_124969_, texturemapping, this.modelOutput);
        ResourceLocation resourcelocation3 = ModelTemplates.RAIL_RAISED_SW.create(p_124969_, texturemapping, this.modelOutput);
        this.createSimpleFlatItemModel(p_124969_);
        this.blockStateOutput
            .accept(
                MultiVariantGenerator.multiVariant(p_124969_)
                    .with(
                        PropertyDispatch.property(BlockStateProperties.RAIL_SHAPE)
                            .select(RailShape.NORTH_SOUTH, Variant.variant().with(VariantProperties.MODEL, resourcelocation))
                            .select(
                                RailShape.EAST_WEST,
                                Variant.variant().with(VariantProperties.MODEL, resourcelocation).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90)
                            )
                            .select(
                                RailShape.ASCENDING_EAST,
                                Variant.variant()
                                    .with(VariantProperties.MODEL, resourcelocation2)
                                    .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90)
                            )
                            .select(
                                RailShape.ASCENDING_WEST,
                                Variant.variant()
                                    .with(VariantProperties.MODEL, resourcelocation3)
                                    .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90)
                            )
                            .select(RailShape.ASCENDING_NORTH, Variant.variant().with(VariantProperties.MODEL, resourcelocation2))
                            .select(RailShape.ASCENDING_SOUTH, Variant.variant().with(VariantProperties.MODEL, resourcelocation3))
                            .select(RailShape.SOUTH_EAST, Variant.variant().with(VariantProperties.MODEL, resourcelocation1))
                            .select(
                                RailShape.SOUTH_WEST,
                                Variant.variant()
                                    .with(VariantProperties.MODEL, resourcelocation1)
                                    .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90)
                            )
                            .select(
                                RailShape.NORTH_WEST,
                                Variant.variant()
                                    .with(VariantProperties.MODEL, resourcelocation1)
                                    .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180)
                            )
                            .select(
                                RailShape.NORTH_EAST,
                                Variant.variant()
                                    .with(VariantProperties.MODEL, resourcelocation1)
                                    .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270)
                            )
                    )
            );
    }

    private void createActiveRail(Block p_124975_) {
        ResourceLocation resourcelocation = this.createSuffixedVariant(p_124975_, "", ModelTemplates.RAIL_FLAT, TextureMapping::rail);
        ResourceLocation resourcelocation1 = this.createSuffixedVariant(p_124975_, "", ModelTemplates.RAIL_RAISED_NE, TextureMapping::rail);
        ResourceLocation resourcelocation2 = this.createSuffixedVariant(p_124975_, "", ModelTemplates.RAIL_RAISED_SW, TextureMapping::rail);
        ResourceLocation resourcelocation3 = this.createSuffixedVariant(p_124975_, "_on", ModelTemplates.RAIL_FLAT, TextureMapping::rail);
        ResourceLocation resourcelocation4 = this.createSuffixedVariant(p_124975_, "_on", ModelTemplates.RAIL_RAISED_NE, TextureMapping::rail);
        ResourceLocation resourcelocation5 = this.createSuffixedVariant(p_124975_, "_on", ModelTemplates.RAIL_RAISED_SW, TextureMapping::rail);
        PropertyDispatch propertydispatch = PropertyDispatch.properties(BlockStateProperties.POWERED, BlockStateProperties.RAIL_SHAPE_STRAIGHT)
            .generate(
                (p_176166_, p_176167_) -> {
                    switch (p_176167_) {
                        case NORTH_SOUTH:
                            return Variant.variant().with(VariantProperties.MODEL, p_176166_ ? resourcelocation3 : resourcelocation);
                        case EAST_WEST:
                            return Variant.variant()
                                .with(VariantProperties.MODEL, p_176166_ ? resourcelocation3 : resourcelocation)
                                .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90);
                        case ASCENDING_EAST:
                            return Variant.variant()
                                .with(VariantProperties.MODEL, p_176166_ ? resourcelocation4 : resourcelocation1)
                                .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90);
                        case ASCENDING_WEST:
                            return Variant.variant()
                                .with(VariantProperties.MODEL, p_176166_ ? resourcelocation5 : resourcelocation2)
                                .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90);
                        case ASCENDING_NORTH:
                            return Variant.variant().with(VariantProperties.MODEL, p_176166_ ? resourcelocation4 : resourcelocation1);
                        case ASCENDING_SOUTH:
                            return Variant.variant().with(VariantProperties.MODEL, p_176166_ ? resourcelocation5 : resourcelocation2);
                        default:
                            throw new UnsupportedOperationException("Fix you generator!");
                    }
                }
            );
        this.createSimpleFlatItemModel(p_124975_);
        this.blockStateOutput.accept(MultiVariantGenerator.multiVariant(p_124975_).with(propertydispatch));
    }

    private BlockModelGenerators.BlockEntityModelGenerator blockEntityModels(ResourceLocation p_124691_, Block p_124692_) {
        return new BlockModelGenerators.BlockEntityModelGenerator(p_124691_, p_124692_);
    }

    private BlockModelGenerators.BlockEntityModelGenerator blockEntityModels(Block p_124826_, Block p_124827_) {
        return new BlockModelGenerators.BlockEntityModelGenerator(ModelLocationUtils.getModelLocation(p_124826_), p_124827_);
    }

    private void createAirLikeBlock(Block p_124531_, Item p_124532_) {
        ResourceLocation resourcelocation = ModelTemplates.PARTICLE_ONLY.create(p_124531_, TextureMapping.particleFromItem(p_124532_), this.modelOutput);
        this.blockStateOutput.accept(createSimpleBlock(p_124531_, resourcelocation));
    }

    private void createAirLikeBlock(Block p_124922_, ResourceLocation p_124923_) {
        ResourceLocation resourcelocation = ModelTemplates.PARTICLE_ONLY.create(p_124922_, TextureMapping.particle(p_124923_), this.modelOutput);
        this.blockStateOutput.accept(createSimpleBlock(p_124922_, resourcelocation));
    }

    private void createFullAndCarpetBlocks(Block p_176218_, Block p_176219_) {
        this.createTrivialCube(p_176218_);
        ResourceLocation resourcelocation = TexturedModel.CARPET.get(p_176218_).create(p_176219_, this.modelOutput);
        this.blockStateOutput.accept(createSimpleBlock(p_176219_, resourcelocation));
    }

    private void createFlowerBed(Block p_273441_) {
        this.createSimpleFlatItemModel(p_273441_.asItem());
        ResourceLocation resourcelocation = TexturedModel.FLOWERBED_1.create(p_273441_, this.modelOutput);
        ResourceLocation resourcelocation1 = TexturedModel.FLOWERBED_2.create(p_273441_, this.modelOutput);
        ResourceLocation resourcelocation2 = TexturedModel.FLOWERBED_3.create(p_273441_, this.modelOutput);
        ResourceLocation resourcelocation3 = TexturedModel.FLOWERBED_4.create(p_273441_, this.modelOutput);
        this.blockStateOutput
            .accept(
                MultiPartGenerator.multiPart(p_273441_)
                    .with(
                        Condition.condition()
                            .term(BlockStateProperties.FLOWER_AMOUNT, 1, 2, 3, 4)
                            .term(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH),
                        Variant.variant().with(VariantProperties.MODEL, resourcelocation)
                    )
                    .with(
                        Condition.condition().term(BlockStateProperties.FLOWER_AMOUNT, 1, 2, 3, 4).term(BlockStateProperties.HORIZONTAL_FACING, Direction.EAST),
                        Variant.variant().with(VariantProperties.MODEL, resourcelocation).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90)
                    )
                    .with(
                        Condition.condition()
                            .term(BlockStateProperties.FLOWER_AMOUNT, 1, 2, 3, 4)
                            .term(BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH),
                        Variant.variant().with(VariantProperties.MODEL, resourcelocation).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180)
                    )
                    .with(
                        Condition.condition().term(BlockStateProperties.FLOWER_AMOUNT, 1, 2, 3, 4).term(BlockStateProperties.HORIZONTAL_FACING, Direction.WEST),
                        Variant.variant().with(VariantProperties.MODEL, resourcelocation).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270)
                    )
                    .with(
                        Condition.condition().term(BlockStateProperties.FLOWER_AMOUNT, 2, 3, 4).term(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH),
                        Variant.variant().with(VariantProperties.MODEL, resourcelocation1)
                    )
                    .with(
                        Condition.condition().term(BlockStateProperties.FLOWER_AMOUNT, 2, 3, 4).term(BlockStateProperties.HORIZONTAL_FACING, Direction.EAST),
                        Variant.variant().with(VariantProperties.MODEL, resourcelocation1).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90)
                    )
                    .with(
                        Condition.condition().term(BlockStateProperties.FLOWER_AMOUNT, 2, 3, 4).term(BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH),
                        Variant.variant().with(VariantProperties.MODEL, resourcelocation1).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180)
                    )
                    .with(
                        Condition.condition().term(BlockStateProperties.FLOWER_AMOUNT, 2, 3, 4).term(BlockStateProperties.HORIZONTAL_FACING, Direction.WEST),
                        Variant.variant().with(VariantProperties.MODEL, resourcelocation1).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270)
                    )
                    .with(
                        Condition.condition().term(BlockStateProperties.FLOWER_AMOUNT, 3, 4).term(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH),
                        Variant.variant().with(VariantProperties.MODEL, resourcelocation2)
                    )
                    .with(
                        Condition.condition().term(BlockStateProperties.FLOWER_AMOUNT, 3, 4).term(BlockStateProperties.HORIZONTAL_FACING, Direction.EAST),
                        Variant.variant().with(VariantProperties.MODEL, resourcelocation2).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90)
                    )
                    .with(
                        Condition.condition().term(BlockStateProperties.FLOWER_AMOUNT, 3, 4).term(BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH),
                        Variant.variant().with(VariantProperties.MODEL, resourcelocation2).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180)
                    )
                    .with(
                        Condition.condition().term(BlockStateProperties.FLOWER_AMOUNT, 3, 4).term(BlockStateProperties.HORIZONTAL_FACING, Direction.WEST),
                        Variant.variant().with(VariantProperties.MODEL, resourcelocation2).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270)
                    )
                    .with(
                        Condition.condition().term(BlockStateProperties.FLOWER_AMOUNT, 4).term(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH),
                        Variant.variant().with(VariantProperties.MODEL, resourcelocation3)
                    )
                    .with(
                        Condition.condition().term(BlockStateProperties.FLOWER_AMOUNT, 4).term(BlockStateProperties.HORIZONTAL_FACING, Direction.EAST),
                        Variant.variant().with(VariantProperties.MODEL, resourcelocation3).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90)
                    )
                    .with(
                        Condition.condition().term(BlockStateProperties.FLOWER_AMOUNT, 4).term(BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH),
                        Variant.variant().with(VariantProperties.MODEL, resourcelocation3).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180)
                    )
                    .with(
                        Condition.condition().term(BlockStateProperties.FLOWER_AMOUNT, 4).term(BlockStateProperties.HORIZONTAL_FACING, Direction.WEST),
                        Variant.variant().with(VariantProperties.MODEL, resourcelocation3).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270)
                    )
            );
    }

    private void createColoredBlockWithRandomRotations(TexturedModel.Provider p_124686_, Block... p_124687_) {
        for (Block block : p_124687_) {
            ResourceLocation resourcelocation = p_124686_.create(block, this.modelOutput);
            this.blockStateOutput.accept(createRotatedVariant(block, resourcelocation));
        }
    }

    private void createColoredBlockWithStateRotations(TexturedModel.Provider p_124778_, Block... p_124779_) {
        for (Block block : p_124779_) {
            ResourceLocation resourcelocation = p_124778_.create(block, this.modelOutput);
            this.blockStateOutput
                .accept(
                    MultiVariantGenerator.multiVariant(block, Variant.variant().with(VariantProperties.MODEL, resourcelocation))
                        .with(createHorizontalFacingDispatchAlt())
                );
        }
    }

    private void createGlassBlocks(Block p_124879_, Block p_124880_) {
        this.createTrivialCube(p_124879_);
        TextureMapping texturemapping = TextureMapping.pane(p_124879_, p_124880_);
        ResourceLocation resourcelocation = ModelTemplates.STAINED_GLASS_PANE_POST.create(p_124880_, texturemapping, this.modelOutput);
        ResourceLocation resourcelocation1 = ModelTemplates.STAINED_GLASS_PANE_SIDE.create(p_124880_, texturemapping, this.modelOutput);
        ResourceLocation resourcelocation2 = ModelTemplates.STAINED_GLASS_PANE_SIDE_ALT.create(p_124880_, texturemapping, this.modelOutput);
        ResourceLocation resourcelocation3 = ModelTemplates.STAINED_GLASS_PANE_NOSIDE.create(p_124880_, texturemapping, this.modelOutput);
        ResourceLocation resourcelocation4 = ModelTemplates.STAINED_GLASS_PANE_NOSIDE_ALT.create(p_124880_, texturemapping, this.modelOutput);
        Item item = p_124880_.asItem();
        ModelTemplates.FLAT_ITEM.create(ModelLocationUtils.getModelLocation(item), TextureMapping.layer0(p_124879_), this.modelOutput);
        this.blockStateOutput
            .accept(
                MultiPartGenerator.multiPart(p_124880_)
                    .with(Variant.variant().with(VariantProperties.MODEL, resourcelocation))
                    .with(Condition.condition().term(BlockStateProperties.NORTH, true), Variant.variant().with(VariantProperties.MODEL, resourcelocation1))
                    .with(
                        Condition.condition().term(BlockStateProperties.EAST, true),
                        Variant.variant().with(VariantProperties.MODEL, resourcelocation1).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90)
                    )
                    .with(Condition.condition().term(BlockStateProperties.SOUTH, true), Variant.variant().with(VariantProperties.MODEL, resourcelocation2))
                    .with(
                        Condition.condition().term(BlockStateProperties.WEST, true),
                        Variant.variant().with(VariantProperties.MODEL, resourcelocation2).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90)
                    )
                    .with(Condition.condition().term(BlockStateProperties.NORTH, false), Variant.variant().with(VariantProperties.MODEL, resourcelocation3))
                    .with(Condition.condition().term(BlockStateProperties.EAST, false), Variant.variant().with(VariantProperties.MODEL, resourcelocation4))
                    .with(
                        Condition.condition().term(BlockStateProperties.SOUTH, false),
                        Variant.variant().with(VariantProperties.MODEL, resourcelocation4).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90)
                    )
                    .with(
                        Condition.condition().term(BlockStateProperties.WEST, false),
                        Variant.variant().with(VariantProperties.MODEL, resourcelocation3).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270)
                    )
            );
    }

    private void createCommandBlock(Block p_124978_) {
        TextureMapping texturemapping = TextureMapping.commandBlock(p_124978_);
        ResourceLocation resourcelocation = ModelTemplates.COMMAND_BLOCK.create(p_124978_, texturemapping, this.modelOutput);
        ResourceLocation resourcelocation1 = this.createSuffixedVariant(
            p_124978_, "_conditional", ModelTemplates.COMMAND_BLOCK, p_176193_ -> texturemapping.copyAndUpdate(TextureSlot.SIDE, p_176193_)
        );
        this.blockStateOutput
            .accept(
                MultiVariantGenerator.multiVariant(p_124978_)
                    .with(createBooleanModelDispatch(BlockStateProperties.CONDITIONAL, resourcelocation1, resourcelocation))
                    .with(createFacingDispatch())
            );
    }

    private void createAnvil(Block p_124981_) {
        ResourceLocation resourcelocation = TexturedModel.ANVIL.create(p_124981_, this.modelOutput);
        this.blockStateOutput.accept(createSimpleBlock(p_124981_, resourcelocation).with(createHorizontalFacingDispatchAlt()));
    }

    private List<Variant> createBambooModels(int p_124512_) {
        String s = "_age" + p_124512_;
        return IntStream.range(1, 5)
            .mapToObj(p_176139_ -> Variant.variant().with(VariantProperties.MODEL, ModelLocationUtils.getModelLocation(Blocks.BAMBOO, p_176139_ + s)))
            .collect(Collectors.toList());
    }

    private void createBamboo() {
        this.skipAutoItemBlock(Blocks.BAMBOO);
        this.blockStateOutput
            .accept(
                MultiPartGenerator.multiPart(Blocks.BAMBOO)
                    .with(Condition.condition().term(BlockStateProperties.AGE_1, 0), this.createBambooModels(0))
                    .with(Condition.condition().term(BlockStateProperties.AGE_1, 1), this.createBambooModels(1))
                    .with(
                        Condition.condition().term(BlockStateProperties.BAMBOO_LEAVES, BambooLeaves.SMALL),
                        Variant.variant().with(VariantProperties.MODEL, ModelLocationUtils.getModelLocation(Blocks.BAMBOO, "_small_leaves"))
                    )
                    .with(
                        Condition.condition().term(BlockStateProperties.BAMBOO_LEAVES, BambooLeaves.LARGE),
                        Variant.variant().with(VariantProperties.MODEL, ModelLocationUtils.getModelLocation(Blocks.BAMBOO, "_large_leaves"))
                    )
            );
    }

    private PropertyDispatch createColumnWithFacing() {
        return PropertyDispatch.property(BlockStateProperties.FACING)
            .select(Direction.DOWN, Variant.variant().with(VariantProperties.X_ROT, VariantProperties.Rotation.R180))
            .select(Direction.UP, Variant.variant())
            .select(Direction.NORTH, Variant.variant().with(VariantProperties.X_ROT, VariantProperties.Rotation.R90))
            .select(
                Direction.SOUTH,
                Variant.variant().with(VariantProperties.X_ROT, VariantProperties.Rotation.R90).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180)
            )
            .select(
                Direction.WEST,
                Variant.variant().with(VariantProperties.X_ROT, VariantProperties.Rotation.R90).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270)
            )
            .select(
                Direction.EAST,
                Variant.variant().with(VariantProperties.X_ROT, VariantProperties.Rotation.R90).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90)
            );
    }

    private void createBarrel() {
        ResourceLocation resourcelocation = TextureMapping.getBlockTexture(Blocks.BARREL, "_top_open");
        this.blockStateOutput
            .accept(
                MultiVariantGenerator.multiVariant(Blocks.BARREL)
                    .with(this.createColumnWithFacing())
                    .with(
                        PropertyDispatch.property(BlockStateProperties.OPEN)
                            .select(
                                false, Variant.variant().with(VariantProperties.MODEL, TexturedModel.CUBE_TOP_BOTTOM.create(Blocks.BARREL, this.modelOutput))
                            )
                            .select(
                                true,
                                Variant.variant()
                                    .with(
                                        VariantProperties.MODEL,
                                        TexturedModel.CUBE_TOP_BOTTOM
                                            .get(Blocks.BARREL)
                                            .updateTextures(p_176216_ -> p_176216_.put(TextureSlot.TOP, resourcelocation))
                                            .createWithSuffix(Blocks.BARREL, "_open", this.modelOutput)
                                    )
                            )
                    )
            );
    }

    private static <T extends Comparable<T>> PropertyDispatch createEmptyOrFullDispatch(
        Property<T> p_124627_, T p_124628_, ResourceLocation p_124629_, ResourceLocation p_124630_
    ) {
        Variant variant = Variant.variant().with(VariantProperties.MODEL, p_124629_);
        Variant variant1 = Variant.variant().with(VariantProperties.MODEL, p_124630_);
        return PropertyDispatch.property(p_124627_).generate(p_176130_ -> {
            boolean flag = p_176130_.compareTo(p_124628_) >= 0;
            return flag ? variant : variant1;
        });
    }

    private void createBeeNest(Block p_124584_, Function<Block, TextureMapping> p_124585_) {
        TextureMapping texturemapping = p_124585_.apply(p_124584_).copyForced(TextureSlot.SIDE, TextureSlot.PARTICLE);
        TextureMapping texturemapping1 = texturemapping.copyAndUpdate(TextureSlot.FRONT, TextureMapping.getBlockTexture(p_124584_, "_front_honey"));
        ResourceLocation resourcelocation = ModelTemplates.CUBE_ORIENTABLE_TOP_BOTTOM.create(p_124584_, texturemapping, this.modelOutput);
        ResourceLocation resourcelocation1 = ModelTemplates.CUBE_ORIENTABLE_TOP_BOTTOM.createWithSuffix(p_124584_, "_honey", texturemapping1, this.modelOutput);
        this.blockStateOutput
            .accept(
                MultiVariantGenerator.multiVariant(p_124584_)
                    .with(createHorizontalFacingDispatch())
                    .with(createEmptyOrFullDispatch(BlockStateProperties.LEVEL_HONEY, 5, resourcelocation1, resourcelocation))
            );
    }

    private void createCropBlock(Block p_124554_, Property<Integer> p_124555_, int... p_124556_) {
        if (p_124555_.getPossibleValues().size() != p_124556_.length) {
            throw new IllegalArgumentException();
        } else {
            Int2ObjectMap<ResourceLocation> int2objectmap = new Int2ObjectOpenHashMap<>();
            PropertyDispatch propertydispatch = PropertyDispatch.property(p_124555_)
                .generate(
                    p_176172_ -> {
                        int i = p_124556_[p_176172_];
                        ResourceLocation resourcelocation = int2objectmap.computeIfAbsent(
                            i, p_176098_ -> this.createSuffixedVariant(p_124554_, "_stage" + i, ModelTemplates.CROP, TextureMapping::crop)
                        );
                        return Variant.variant().with(VariantProperties.MODEL, resourcelocation);
                    }
                );
            this.createSimpleFlatItemModel(p_124554_.asItem());
            this.blockStateOutput.accept(MultiVariantGenerator.multiVariant(p_124554_).with(propertydispatch));
        }
    }

    private void createBell() {
        ResourceLocation resourcelocation = ModelLocationUtils.getModelLocation(Blocks.BELL, "_floor");
        ResourceLocation resourcelocation1 = ModelLocationUtils.getModelLocation(Blocks.BELL, "_ceiling");
        ResourceLocation resourcelocation2 = ModelLocationUtils.getModelLocation(Blocks.BELL, "_wall");
        ResourceLocation resourcelocation3 = ModelLocationUtils.getModelLocation(Blocks.BELL, "_between_walls");
        this.createSimpleFlatItemModel(Items.BELL);
        this.blockStateOutput
            .accept(
                MultiVariantGenerator.multiVariant(Blocks.BELL)
                    .with(
                        PropertyDispatch.properties(BlockStateProperties.HORIZONTAL_FACING, BlockStateProperties.BELL_ATTACHMENT)
                            .select(Direction.NORTH, BellAttachType.FLOOR, Variant.variant().with(VariantProperties.MODEL, resourcelocation))
                            .select(
                                Direction.SOUTH,
                                BellAttachType.FLOOR,
                                Variant.variant()
                                    .with(VariantProperties.MODEL, resourcelocation)
                                    .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180)
                            )
                            .select(
                                Direction.EAST,
                                BellAttachType.FLOOR,
                                Variant.variant().with(VariantProperties.MODEL, resourcelocation).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90)
                            )
                            .select(
                                Direction.WEST,
                                BellAttachType.FLOOR,
                                Variant.variant()
                                    .with(VariantProperties.MODEL, resourcelocation)
                                    .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270)
                            )
                            .select(Direction.NORTH, BellAttachType.CEILING, Variant.variant().with(VariantProperties.MODEL, resourcelocation1))
                            .select(
                                Direction.SOUTH,
                                BellAttachType.CEILING,
                                Variant.variant()
                                    .with(VariantProperties.MODEL, resourcelocation1)
                                    .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180)
                            )
                            .select(
                                Direction.EAST,
                                BellAttachType.CEILING,
                                Variant.variant()
                                    .with(VariantProperties.MODEL, resourcelocation1)
                                    .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90)
                            )
                            .select(
                                Direction.WEST,
                                BellAttachType.CEILING,
                                Variant.variant()
                                    .with(VariantProperties.MODEL, resourcelocation1)
                                    .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270)
                            )
                            .select(
                                Direction.NORTH,
                                BellAttachType.SINGLE_WALL,
                                Variant.variant()
                                    .with(VariantProperties.MODEL, resourcelocation2)
                                    .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270)
                            )
                            .select(
                                Direction.SOUTH,
                                BellAttachType.SINGLE_WALL,
                                Variant.variant()
                                    .with(VariantProperties.MODEL, resourcelocation2)
                                    .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90)
                            )
                            .select(Direction.EAST, BellAttachType.SINGLE_WALL, Variant.variant().with(VariantProperties.MODEL, resourcelocation2))
                            .select(
                                Direction.WEST,
                                BellAttachType.SINGLE_WALL,
                                Variant.variant()
                                    .with(VariantProperties.MODEL, resourcelocation2)
                                    .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180)
                            )
                            .select(
                                Direction.SOUTH,
                                BellAttachType.DOUBLE_WALL,
                                Variant.variant()
                                    .with(VariantProperties.MODEL, resourcelocation3)
                                    .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90)
                            )
                            .select(
                                Direction.NORTH,
                                BellAttachType.DOUBLE_WALL,
                                Variant.variant()
                                    .with(VariantProperties.MODEL, resourcelocation3)
                                    .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270)
                            )
                            .select(Direction.EAST, BellAttachType.DOUBLE_WALL, Variant.variant().with(VariantProperties.MODEL, resourcelocation3))
                            .select(
                                Direction.WEST,
                                BellAttachType.DOUBLE_WALL,
                                Variant.variant()
                                    .with(VariantProperties.MODEL, resourcelocation3)
                                    .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180)
                            )
                    )
            );
    }

    private void createGrindstone() {
        this.blockStateOutput
            .accept(
                MultiVariantGenerator.multiVariant(
                        Blocks.GRINDSTONE, Variant.variant().with(VariantProperties.MODEL, ModelLocationUtils.getModelLocation(Blocks.GRINDSTONE))
                    )
                    .with(
                        PropertyDispatch.properties(BlockStateProperties.ATTACH_FACE, BlockStateProperties.HORIZONTAL_FACING)
                            .select(AttachFace.FLOOR, Direction.NORTH, Variant.variant())
                            .select(AttachFace.FLOOR, Direction.EAST, Variant.variant().with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90))
                            .select(AttachFace.FLOOR, Direction.SOUTH, Variant.variant().with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180))
                            .select(AttachFace.FLOOR, Direction.WEST, Variant.variant().with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270))
                            .select(AttachFace.WALL, Direction.NORTH, Variant.variant().with(VariantProperties.X_ROT, VariantProperties.Rotation.R90))
                            .select(
                                AttachFace.WALL,
                                Direction.EAST,
                                Variant.variant()
                                    .with(VariantProperties.X_ROT, VariantProperties.Rotation.R90)
                                    .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90)
                            )
                            .select(
                                AttachFace.WALL,
                                Direction.SOUTH,
                                Variant.variant()
                                    .with(VariantProperties.X_ROT, VariantProperties.Rotation.R90)
                                    .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180)
                            )
                            .select(
                                AttachFace.WALL,
                                Direction.WEST,
                                Variant.variant()
                                    .with(VariantProperties.X_ROT, VariantProperties.Rotation.R90)
                                    .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270)
                            )
                            .select(AttachFace.CEILING, Direction.SOUTH, Variant.variant().with(VariantProperties.X_ROT, VariantProperties.Rotation.R180))
                            .select(
                                AttachFace.CEILING,
                                Direction.WEST,
                                Variant.variant()
                                    .with(VariantProperties.X_ROT, VariantProperties.Rotation.R180)
                                    .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90)
                            )
                            .select(
                                AttachFace.CEILING,
                                Direction.NORTH,
                                Variant.variant()
                                    .with(VariantProperties.X_ROT, VariantProperties.Rotation.R180)
                                    .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180)
                            )
                            .select(
                                AttachFace.CEILING,
                                Direction.EAST,
                                Variant.variant()
                                    .with(VariantProperties.X_ROT, VariantProperties.Rotation.R180)
                                    .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270)
                            )
                    )
            );
    }

    private void createFurnace(Block p_124857_, TexturedModel.Provider p_124858_) {
        ResourceLocation resourcelocation = p_124858_.create(p_124857_, this.modelOutput);
        ResourceLocation resourcelocation1 = TextureMapping.getBlockTexture(p_124857_, "_front_on");
        ResourceLocation resourcelocation2 = p_124858_.get(p_124857_)
            .updateTextures(p_176207_ -> p_176207_.put(TextureSlot.FRONT, resourcelocation1))
            .createWithSuffix(p_124857_, "_on", this.modelOutput);
        this.blockStateOutput
            .accept(
                MultiVariantGenerator.multiVariant(p_124857_)
                    .with(createBooleanModelDispatch(BlockStateProperties.LIT, resourcelocation2, resourcelocation))
                    .with(createHorizontalFacingDispatch())
            );
    }

    private void createCampfires(Block... p_124714_) {
        ResourceLocation resourcelocation = ModelLocationUtils.decorateBlockModelLocation("campfire_off");

        for (Block block : p_124714_) {
            ResourceLocation resourcelocation1 = ModelTemplates.CAMPFIRE.create(block, TextureMapping.campfire(block), this.modelOutput);
            this.createSimpleFlatItemModel(block.asItem());
            this.blockStateOutput
                .accept(
                    MultiVariantGenerator.multiVariant(block)
                        .with(createBooleanModelDispatch(BlockStateProperties.LIT, resourcelocation1, resourcelocation))
                        .with(createHorizontalFacingDispatchAlt())
                );
        }
    }

    private void createAzalea(Block p_176248_) {
        ResourceLocation resourcelocation = ModelTemplates.AZALEA.create(p_176248_, TextureMapping.cubeTop(p_176248_), this.modelOutput);
        this.blockStateOutput.accept(createSimpleBlock(p_176248_, resourcelocation));
    }

    private void createPottedAzalea(Block p_176250_) {
        ResourceLocation resourcelocation;
        if (p_176250_ == Blocks.POTTED_FLOWERING_AZALEA) {
            resourcelocation = ModelTemplates.POTTED_FLOWERING_AZALEA.create(p_176250_, TextureMapping.pottedAzalea(p_176250_), this.modelOutput);
        } else {
            resourcelocation = ModelTemplates.POTTED_AZALEA.create(p_176250_, TextureMapping.pottedAzalea(p_176250_), this.modelOutput);
        }

        this.blockStateOutput.accept(createSimpleBlock(p_176250_, resourcelocation));
    }

    private void createBookshelf() {
        TextureMapping texturemapping = TextureMapping.column(
            TextureMapping.getBlockTexture(Blocks.BOOKSHELF), TextureMapping.getBlockTexture(Blocks.OAK_PLANKS)
        );
        ResourceLocation resourcelocation = ModelTemplates.CUBE_COLUMN.create(Blocks.BOOKSHELF, texturemapping, this.modelOutput);
        this.blockStateOutput.accept(createSimpleBlock(Blocks.BOOKSHELF, resourcelocation));
    }

    private void createRedstoneWire() {
        this.createSimpleFlatItemModel(Items.REDSTONE);
        this.blockStateOutput
            .accept(
                MultiPartGenerator.multiPart(Blocks.REDSTONE_WIRE)
                    .with(
                        Condition.or(
                            Condition.condition()
                                .term(BlockStateProperties.NORTH_REDSTONE, RedstoneSide.NONE)
                                .term(BlockStateProperties.EAST_REDSTONE, RedstoneSide.NONE)
                                .term(BlockStateProperties.SOUTH_REDSTONE, RedstoneSide.NONE)
                                .term(BlockStateProperties.WEST_REDSTONE, RedstoneSide.NONE),
                            Condition.condition()
                                .term(BlockStateProperties.NORTH_REDSTONE, RedstoneSide.SIDE, RedstoneSide.UP)
                                .term(BlockStateProperties.EAST_REDSTONE, RedstoneSide.SIDE, RedstoneSide.UP),
                            Condition.condition()
                                .term(BlockStateProperties.EAST_REDSTONE, RedstoneSide.SIDE, RedstoneSide.UP)
                                .term(BlockStateProperties.SOUTH_REDSTONE, RedstoneSide.SIDE, RedstoneSide.UP),
                            Condition.condition()
                                .term(BlockStateProperties.SOUTH_REDSTONE, RedstoneSide.SIDE, RedstoneSide.UP)
                                .term(BlockStateProperties.WEST_REDSTONE, RedstoneSide.SIDE, RedstoneSide.UP),
                            Condition.condition()
                                .term(BlockStateProperties.WEST_REDSTONE, RedstoneSide.SIDE, RedstoneSide.UP)
                                .term(BlockStateProperties.NORTH_REDSTONE, RedstoneSide.SIDE, RedstoneSide.UP)
                        ),
                        Variant.variant().with(VariantProperties.MODEL, ModelLocationUtils.decorateBlockModelLocation("redstone_dust_dot"))
                    )
                    .with(
                        Condition.condition().term(BlockStateProperties.NORTH_REDSTONE, RedstoneSide.SIDE, RedstoneSide.UP),
                        Variant.variant().with(VariantProperties.MODEL, ModelLocationUtils.decorateBlockModelLocation("redstone_dust_side0"))
                    )
                    .with(
                        Condition.condition().term(BlockStateProperties.SOUTH_REDSTONE, RedstoneSide.SIDE, RedstoneSide.UP),
                        Variant.variant().with(VariantProperties.MODEL, ModelLocationUtils.decorateBlockModelLocation("redstone_dust_side_alt0"))
                    )
                    .with(
                        Condition.condition().term(BlockStateProperties.EAST_REDSTONE, RedstoneSide.SIDE, RedstoneSide.UP),
                        Variant.variant()
                            .with(VariantProperties.MODEL, ModelLocationUtils.decorateBlockModelLocation("redstone_dust_side_alt1"))
                            .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270)
                    )
                    .with(
                        Condition.condition().term(BlockStateProperties.WEST_REDSTONE, RedstoneSide.SIDE, RedstoneSide.UP),
                        Variant.variant()
                            .with(VariantProperties.MODEL, ModelLocationUtils.decorateBlockModelLocation("redstone_dust_side1"))
                            .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270)
                    )
                    .with(
                        Condition.condition().term(BlockStateProperties.NORTH_REDSTONE, RedstoneSide.UP),
                        Variant.variant().with(VariantProperties.MODEL, ModelLocationUtils.decorateBlockModelLocation("redstone_dust_up"))
                    )
                    .with(
                        Condition.condition().term(BlockStateProperties.EAST_REDSTONE, RedstoneSide.UP),
                        Variant.variant()
                            .with(VariantProperties.MODEL, ModelLocationUtils.decorateBlockModelLocation("redstone_dust_up"))
                            .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90)
                    )
                    .with(
                        Condition.condition().term(BlockStateProperties.SOUTH_REDSTONE, RedstoneSide.UP),
                        Variant.variant()
                            .with(VariantProperties.MODEL, ModelLocationUtils.decorateBlockModelLocation("redstone_dust_up"))
                            .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180)
                    )
                    .with(
                        Condition.condition().term(BlockStateProperties.WEST_REDSTONE, RedstoneSide.UP),
                        Variant.variant()
                            .with(VariantProperties.MODEL, ModelLocationUtils.decorateBlockModelLocation("redstone_dust_up"))
                            .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270)
                    )
            );
    }

    private void createComparator() {
        this.createSimpleFlatItemModel(Items.COMPARATOR);
        this.blockStateOutput
            .accept(
                MultiVariantGenerator.multiVariant(Blocks.COMPARATOR)
                    .with(createHorizontalFacingDispatchAlt())
                    .with(
                        PropertyDispatch.properties(BlockStateProperties.MODE_COMPARATOR, BlockStateProperties.POWERED)
                            .select(
                                ComparatorMode.COMPARE,
                                false,
                                Variant.variant().with(VariantProperties.MODEL, ModelLocationUtils.getModelLocation(Blocks.COMPARATOR))
                            )
                            .select(
                                ComparatorMode.COMPARE,
                                true,
                                Variant.variant().with(VariantProperties.MODEL, ModelLocationUtils.getModelLocation(Blocks.COMPARATOR, "_on"))
                            )
                            .select(
                                ComparatorMode.SUBTRACT,
                                false,
                                Variant.variant().with(VariantProperties.MODEL, ModelLocationUtils.getModelLocation(Blocks.COMPARATOR, "_subtract"))
                            )
                            .select(
                                ComparatorMode.SUBTRACT,
                                true,
                                Variant.variant().with(VariantProperties.MODEL, ModelLocationUtils.getModelLocation(Blocks.COMPARATOR, "_on_subtract"))
                            )
                    )
            );
    }

    private void createSmoothStoneSlab() {
        TextureMapping texturemapping = TextureMapping.cube(Blocks.SMOOTH_STONE);
        TextureMapping texturemapping1 = TextureMapping.column(
            TextureMapping.getBlockTexture(Blocks.SMOOTH_STONE_SLAB, "_side"), texturemapping.get(TextureSlot.TOP)
        );
        ResourceLocation resourcelocation = ModelTemplates.SLAB_BOTTOM.create(Blocks.SMOOTH_STONE_SLAB, texturemapping1, this.modelOutput);
        ResourceLocation resourcelocation1 = ModelTemplates.SLAB_TOP.create(Blocks.SMOOTH_STONE_SLAB, texturemapping1, this.modelOutput);
        ResourceLocation resourcelocation2 = ModelTemplates.CUBE_COLUMN
            .createWithOverride(Blocks.SMOOTH_STONE_SLAB, "_double", texturemapping1, this.modelOutput);
        this.blockStateOutput.accept(createSlab(Blocks.SMOOTH_STONE_SLAB, resourcelocation, resourcelocation1, resourcelocation2));
        this.blockStateOutput
            .accept(createSimpleBlock(Blocks.SMOOTH_STONE, ModelTemplates.CUBE_ALL.create(Blocks.SMOOTH_STONE, texturemapping, this.modelOutput)));
    }

    private void createBrewingStand() {
        this.createSimpleFlatItemModel(Items.BREWING_STAND);
        this.blockStateOutput
            .accept(
                MultiPartGenerator.multiPart(Blocks.BREWING_STAND)
                    .with(Variant.variant().with(VariantProperties.MODEL, TextureMapping.getBlockTexture(Blocks.BREWING_STAND)))
                    .with(
                        Condition.condition().term(BlockStateProperties.HAS_BOTTLE_0, true),
                        Variant.variant().with(VariantProperties.MODEL, TextureMapping.getBlockTexture(Blocks.BREWING_STAND, "_bottle0"))
                    )
                    .with(
                        Condition.condition().term(BlockStateProperties.HAS_BOTTLE_1, true),
                        Variant.variant().with(VariantProperties.MODEL, TextureMapping.getBlockTexture(Blocks.BREWING_STAND, "_bottle1"))
                    )
                    .with(
                        Condition.condition().term(BlockStateProperties.HAS_BOTTLE_2, true),
                        Variant.variant().with(VariantProperties.MODEL, TextureMapping.getBlockTexture(Blocks.BREWING_STAND, "_bottle2"))
                    )
                    .with(
                        Condition.condition().term(BlockStateProperties.HAS_BOTTLE_0, false),
                        Variant.variant().with(VariantProperties.MODEL, TextureMapping.getBlockTexture(Blocks.BREWING_STAND, "_empty0"))
                    )
                    .with(
                        Condition.condition().term(BlockStateProperties.HAS_BOTTLE_1, false),
                        Variant.variant().with(VariantProperties.MODEL, TextureMapping.getBlockTexture(Blocks.BREWING_STAND, "_empty1"))
                    )
                    .with(
                        Condition.condition().term(BlockStateProperties.HAS_BOTTLE_2, false),
                        Variant.variant().with(VariantProperties.MODEL, TextureMapping.getBlockTexture(Blocks.BREWING_STAND, "_empty2"))
                    )
            );
    }

    private void createMushroomBlock(Block p_124984_) {
        ResourceLocation resourcelocation = ModelTemplates.SINGLE_FACE.create(p_124984_, TextureMapping.defaultTexture(p_124984_), this.modelOutput);
        ResourceLocation resourcelocation1 = ModelLocationUtils.decorateBlockModelLocation("mushroom_block_inside");
        this.blockStateOutput
            .accept(
                MultiPartGenerator.multiPart(p_124984_)
                    .with(Condition.condition().term(BlockStateProperties.NORTH, true), Variant.variant().with(VariantProperties.MODEL, resourcelocation))
                    .with(
                        Condition.condition().term(BlockStateProperties.EAST, true),
                        Variant.variant()
                            .with(VariantProperties.MODEL, resourcelocation)
                            .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90)
                            .with(VariantProperties.UV_LOCK, true)
                    )
                    .with(
                        Condition.condition().term(BlockStateProperties.SOUTH, true),
                        Variant.variant()
                            .with(VariantProperties.MODEL, resourcelocation)
                            .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180)
                            .with(VariantProperties.UV_LOCK, true)
                    )
                    .with(
                        Condition.condition().term(BlockStateProperties.WEST, true),
                        Variant.variant()
                            .with(VariantProperties.MODEL, resourcelocation)
                            .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270)
                            .with(VariantProperties.UV_LOCK, true)
                    )
                    .with(
                        Condition.condition().term(BlockStateProperties.UP, true),
                        Variant.variant()
                            .with(VariantProperties.MODEL, resourcelocation)
                            .with(VariantProperties.X_ROT, VariantProperties.Rotation.R270)
                            .with(VariantProperties.UV_LOCK, true)
                    )
                    .with(
                        Condition.condition().term(BlockStateProperties.DOWN, true),
                        Variant.variant()
                            .with(VariantProperties.MODEL, resourcelocation)
                            .with(VariantProperties.X_ROT, VariantProperties.Rotation.R90)
                            .with(VariantProperties.UV_LOCK, true)
                    )
                    .with(Condition.condition().term(BlockStateProperties.NORTH, false), Variant.variant().with(VariantProperties.MODEL, resourcelocation1))
                    .with(
                        Condition.condition().term(BlockStateProperties.EAST, false),
                        Variant.variant()
                            .with(VariantProperties.MODEL, resourcelocation1)
                            .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90)
                            .with(VariantProperties.UV_LOCK, false)
                    )
                    .with(
                        Condition.condition().term(BlockStateProperties.SOUTH, false),
                        Variant.variant()
                            .with(VariantProperties.MODEL, resourcelocation1)
                            .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180)
                            .with(VariantProperties.UV_LOCK, false)
                    )
                    .with(
                        Condition.condition().term(BlockStateProperties.WEST, false),
                        Variant.variant()
                            .with(VariantProperties.MODEL, resourcelocation1)
                            .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270)
                            .with(VariantProperties.UV_LOCK, false)
                    )
                    .with(
                        Condition.condition().term(BlockStateProperties.UP, false),
                        Variant.variant()
                            .with(VariantProperties.MODEL, resourcelocation1)
                            .with(VariantProperties.X_ROT, VariantProperties.Rotation.R270)
                            .with(VariantProperties.UV_LOCK, false)
                    )
                    .with(
                        Condition.condition().term(BlockStateProperties.DOWN, false),
                        Variant.variant()
                            .with(VariantProperties.MODEL, resourcelocation1)
                            .with(VariantProperties.X_ROT, VariantProperties.Rotation.R90)
                            .with(VariantProperties.UV_LOCK, false)
                    )
            );
        this.delegateItemModel(p_124984_, TexturedModel.CUBE.createWithSuffix(p_124984_, "_inventory", this.modelOutput));
    }

    private void createCakeBlock() {
        this.createSimpleFlatItemModel(Items.CAKE);
        this.blockStateOutput
            .accept(
                MultiVariantGenerator.multiVariant(Blocks.CAKE)
                    .with(
                        PropertyDispatch.property(BlockStateProperties.BITES)
                            .select(0, Variant.variant().with(VariantProperties.MODEL, ModelLocationUtils.getModelLocation(Blocks.CAKE)))
                            .select(1, Variant.variant().with(VariantProperties.MODEL, ModelLocationUtils.getModelLocation(Blocks.CAKE, "_slice1")))
                            .select(2, Variant.variant().with(VariantProperties.MODEL, ModelLocationUtils.getModelLocation(Blocks.CAKE, "_slice2")))
                            .select(3, Variant.variant().with(VariantProperties.MODEL, ModelLocationUtils.getModelLocation(Blocks.CAKE, "_slice3")))
                            .select(4, Variant.variant().with(VariantProperties.MODEL, ModelLocationUtils.getModelLocation(Blocks.CAKE, "_slice4")))
                            .select(5, Variant.variant().with(VariantProperties.MODEL, ModelLocationUtils.getModelLocation(Blocks.CAKE, "_slice5")))
                            .select(6, Variant.variant().with(VariantProperties.MODEL, ModelLocationUtils.getModelLocation(Blocks.CAKE, "_slice6")))
                    )
            );
    }

    private void createCartographyTable() {
        TextureMapping texturemapping = new TextureMapping()
            .put(TextureSlot.PARTICLE, TextureMapping.getBlockTexture(Blocks.CARTOGRAPHY_TABLE, "_side3"))
            .put(TextureSlot.DOWN, TextureMapping.getBlockTexture(Blocks.DARK_OAK_PLANKS))
            .put(TextureSlot.UP, TextureMapping.getBlockTexture(Blocks.CARTOGRAPHY_TABLE, "_top"))
            .put(TextureSlot.NORTH, TextureMapping.getBlockTexture(Blocks.CARTOGRAPHY_TABLE, "_side3"))
            .put(TextureSlot.EAST, TextureMapping.getBlockTexture(Blocks.CARTOGRAPHY_TABLE, "_side3"))
            .put(TextureSlot.SOUTH, TextureMapping.getBlockTexture(Blocks.CARTOGRAPHY_TABLE, "_side1"))
            .put(TextureSlot.WEST, TextureMapping.getBlockTexture(Blocks.CARTOGRAPHY_TABLE, "_side2"));
        this.blockStateOutput
            .accept(createSimpleBlock(Blocks.CARTOGRAPHY_TABLE, ModelTemplates.CUBE.create(Blocks.CARTOGRAPHY_TABLE, texturemapping, this.modelOutput)));
    }

    private void createSmithingTable() {
        TextureMapping texturemapping = new TextureMapping()
            .put(TextureSlot.PARTICLE, TextureMapping.getBlockTexture(Blocks.SMITHING_TABLE, "_front"))
            .put(TextureSlot.DOWN, TextureMapping.getBlockTexture(Blocks.SMITHING_TABLE, "_bottom"))
            .put(TextureSlot.UP, TextureMapping.getBlockTexture(Blocks.SMITHING_TABLE, "_top"))
            .put(TextureSlot.NORTH, TextureMapping.getBlockTexture(Blocks.SMITHING_TABLE, "_front"))
            .put(TextureSlot.SOUTH, TextureMapping.getBlockTexture(Blocks.SMITHING_TABLE, "_front"))
            .put(TextureSlot.EAST, TextureMapping.getBlockTexture(Blocks.SMITHING_TABLE, "_side"))
            .put(TextureSlot.WEST, TextureMapping.getBlockTexture(Blocks.SMITHING_TABLE, "_side"));
        this.blockStateOutput
            .accept(createSimpleBlock(Blocks.SMITHING_TABLE, ModelTemplates.CUBE.create(Blocks.SMITHING_TABLE, texturemapping, this.modelOutput)));
    }

    private void createCraftingTableLike(Block p_124550_, Block p_124551_, BiFunction<Block, Block, TextureMapping> p_124552_) {
        TextureMapping texturemapping = p_124552_.apply(p_124550_, p_124551_);
        this.blockStateOutput.accept(createSimpleBlock(p_124550_, ModelTemplates.CUBE.create(p_124550_, texturemapping, this.modelOutput)));
    }

    public void createGenericCube(Block p_282830_) {
        TextureMapping texturemapping = new TextureMapping()
            .put(TextureSlot.PARTICLE, TextureMapping.getBlockTexture(p_282830_, "_particle"))
            .put(TextureSlot.DOWN, TextureMapping.getBlockTexture(p_282830_, "_down"))
            .put(TextureSlot.UP, TextureMapping.getBlockTexture(p_282830_, "_up"))
            .put(TextureSlot.NORTH, TextureMapping.getBlockTexture(p_282830_, "_north"))
            .put(TextureSlot.SOUTH, TextureMapping.getBlockTexture(p_282830_, "_south"))
            .put(TextureSlot.EAST, TextureMapping.getBlockTexture(p_282830_, "_east"))
            .put(TextureSlot.WEST, TextureMapping.getBlockTexture(p_282830_, "_west"));
        this.blockStateOutput.accept(createSimpleBlock(p_282830_, ModelTemplates.CUBE.create(p_282830_, texturemapping, this.modelOutput)));
    }

    private void createPumpkins() {
        TextureMapping texturemapping = TextureMapping.column(Blocks.PUMPKIN);
        this.blockStateOutput.accept(createSimpleBlock(Blocks.PUMPKIN, ModelLocationUtils.getModelLocation(Blocks.PUMPKIN)));
        this.createPumpkinVariant(Blocks.CARVED_PUMPKIN, texturemapping);
        this.createPumpkinVariant(Blocks.JACK_O_LANTERN, texturemapping);
    }

    private void createPumpkinVariant(Block p_124565_, TextureMapping p_124566_) {
        ResourceLocation resourcelocation = ModelTemplates.CUBE_ORIENTABLE
            .create(p_124565_, p_124566_.copyAndUpdate(TextureSlot.FRONT, TextureMapping.getBlockTexture(p_124565_)), this.modelOutput);
        this.blockStateOutput
            .accept(
                MultiVariantGenerator.multiVariant(p_124565_, Variant.variant().with(VariantProperties.MODEL, resourcelocation))
                    .with(createHorizontalFacingDispatch())
            );
    }

    private void createCauldrons() {
        this.createSimpleFlatItemModel(Items.CAULDRON);
        this.createNonTemplateModelBlock(Blocks.CAULDRON);
        this.blockStateOutput
            .accept(
                createSimpleBlock(
                    Blocks.LAVA_CAULDRON,
                    ModelTemplates.CAULDRON_FULL
                        .create(Blocks.LAVA_CAULDRON, TextureMapping.cauldron(TextureMapping.getBlockTexture(Blocks.LAVA, "_still")), this.modelOutput)
                )
            );
        this.blockStateOutput
            .accept(
                MultiVariantGenerator.multiVariant(Blocks.WATER_CAULDRON)
                    .with(
                        PropertyDispatch.property(LayeredCauldronBlock.LEVEL)
                            .select(
                                1,
                                Variant.variant()
                                    .with(
                                        VariantProperties.MODEL,
                                        ModelTemplates.CAULDRON_LEVEL1
                                            .createWithSuffix(
                                                Blocks.WATER_CAULDRON,
                                                "_level1",
                                                TextureMapping.cauldron(TextureMapping.getBlockTexture(Blocks.WATER, "_still")),
                                                this.modelOutput
                                            )
                                    )
                            )
                            .select(
                                2,
                                Variant.variant()
                                    .with(
                                        VariantProperties.MODEL,
                                        ModelTemplates.CAULDRON_LEVEL2
                                            .createWithSuffix(
                                                Blocks.WATER_CAULDRON,
                                                "_level2",
                                                TextureMapping.cauldron(TextureMapping.getBlockTexture(Blocks.WATER, "_still")),
                                                this.modelOutput
                                            )
                                    )
                            )
                            .select(
                                3,
                                Variant.variant()
                                    .with(
                                        VariantProperties.MODEL,
                                        ModelTemplates.CAULDRON_FULL
                                            .createWithSuffix(
                                                Blocks.WATER_CAULDRON,
                                                "_full",
                                                TextureMapping.cauldron(TextureMapping.getBlockTexture(Blocks.WATER, "_still")),
                                                this.modelOutput
                                            )
                                    )
                            )
                    )
            );
        this.blockStateOutput
            .accept(
                MultiVariantGenerator.multiVariant(Blocks.POWDER_SNOW_CAULDRON)
                    .with(
                        PropertyDispatch.property(LayeredCauldronBlock.LEVEL)
                            .select(
                                1,
                                Variant.variant()
                                    .with(
                                        VariantProperties.MODEL,
                                        ModelTemplates.CAULDRON_LEVEL1
                                            .createWithSuffix(
                                                Blocks.POWDER_SNOW_CAULDRON,
                                                "_level1",
                                                TextureMapping.cauldron(TextureMapping.getBlockTexture(Blocks.POWDER_SNOW)),
                                                this.modelOutput
                                            )
                                    )
                            )
                            .select(
                                2,
                                Variant.variant()
                                    .with(
                                        VariantProperties.MODEL,
                                        ModelTemplates.CAULDRON_LEVEL2
                                            .createWithSuffix(
                                                Blocks.POWDER_SNOW_CAULDRON,
                                                "_level2",
                                                TextureMapping.cauldron(TextureMapping.getBlockTexture(Blocks.POWDER_SNOW)),
                                                this.modelOutput
                                            )
                                    )
                            )
                            .select(
                                3,
                                Variant.variant()
                                    .with(
                                        VariantProperties.MODEL,
                                        ModelTemplates.CAULDRON_FULL
                                            .createWithSuffix(
                                                Blocks.POWDER_SNOW_CAULDRON,
                                                "_full",
                                                TextureMapping.cauldron(TextureMapping.getBlockTexture(Blocks.POWDER_SNOW)),
                                                this.modelOutput
                                            )
                                    )
                            )
                    )
            );
    }

    private void createChorusFlower() {
        TextureMapping texturemapping = TextureMapping.defaultTexture(Blocks.CHORUS_FLOWER);
        ResourceLocation resourcelocation = ModelTemplates.CHORUS_FLOWER.create(Blocks.CHORUS_FLOWER, texturemapping, this.modelOutput);
        ResourceLocation resourcelocation1 = this.createSuffixedVariant(
            Blocks.CHORUS_FLOWER, "_dead", ModelTemplates.CHORUS_FLOWER, p_176148_ -> texturemapping.copyAndUpdate(TextureSlot.TEXTURE, p_176148_)
        );
        this.blockStateOutput
            .accept(
                MultiVariantGenerator.multiVariant(Blocks.CHORUS_FLOWER)
                    .with(createEmptyOrFullDispatch(BlockStateProperties.AGE_5, 5, resourcelocation1, resourcelocation))
            );
    }

    private void createCrafterBlock() {
        ResourceLocation resourcelocation = ModelLocationUtils.getModelLocation(Blocks.CRAFTER);
        ResourceLocation resourcelocation1 = ModelLocationUtils.getModelLocation(Blocks.CRAFTER, "_triggered");
        ResourceLocation resourcelocation2 = ModelLocationUtils.getModelLocation(Blocks.CRAFTER, "_crafting");
        ResourceLocation resourcelocation3 = ModelLocationUtils.getModelLocation(Blocks.CRAFTER, "_crafting_triggered");
        this.blockStateOutput
            .accept(
                MultiVariantGenerator.multiVariant(Blocks.CRAFTER)
                    .with(PropertyDispatch.property(BlockStateProperties.ORIENTATION).generate(p_236301_ -> this.applyRotation(p_236301_, Variant.variant())))
                    .with(
                        PropertyDispatch.properties(BlockStateProperties.TRIGGERED, CrafterBlock.CRAFTING)
                            .select(false, false, Variant.variant().with(VariantProperties.MODEL, resourcelocation))
                            .select(true, true, Variant.variant().with(VariantProperties.MODEL, resourcelocation3))
                            .select(true, false, Variant.variant().with(VariantProperties.MODEL, resourcelocation1))
                            .select(false, true, Variant.variant().with(VariantProperties.MODEL, resourcelocation2))
                    )
            );
    }

    private void createDispenserBlock(Block p_124987_) {
        TextureMapping texturemapping = new TextureMapping()
            .put(TextureSlot.TOP, TextureMapping.getBlockTexture(Blocks.FURNACE, "_top"))
            .put(TextureSlot.SIDE, TextureMapping.getBlockTexture(Blocks.FURNACE, "_side"))
            .put(TextureSlot.FRONT, TextureMapping.getBlockTexture(p_124987_, "_front"));
        TextureMapping texturemapping1 = new TextureMapping()
            .put(TextureSlot.SIDE, TextureMapping.getBlockTexture(Blocks.FURNACE, "_top"))
            .put(TextureSlot.FRONT, TextureMapping.getBlockTexture(p_124987_, "_front_vertical"));
        ResourceLocation resourcelocation = ModelTemplates.CUBE_ORIENTABLE.create(p_124987_, texturemapping, this.modelOutput);
        ResourceLocation resourcelocation1 = ModelTemplates.CUBE_ORIENTABLE_VERTICAL.create(p_124987_, texturemapping1, this.modelOutput);
        this.blockStateOutput
            .accept(
                MultiVariantGenerator.multiVariant(p_124987_)
                    .with(
                        PropertyDispatch.property(BlockStateProperties.FACING)
                            .select(
                                Direction.DOWN,
                                Variant.variant()
                                    .with(VariantProperties.MODEL, resourcelocation1)
                                    .with(VariantProperties.X_ROT, VariantProperties.Rotation.R180)
                            )
                            .select(Direction.UP, Variant.variant().with(VariantProperties.MODEL, resourcelocation1))
                            .select(Direction.NORTH, Variant.variant().with(VariantProperties.MODEL, resourcelocation))
                            .select(
                                Direction.EAST,
                                Variant.variant().with(VariantProperties.MODEL, resourcelocation).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90)
                            )
                            .select(
                                Direction.SOUTH,
                                Variant.variant()
                                    .with(VariantProperties.MODEL, resourcelocation)
                                    .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180)
                            )
                            .select(
                                Direction.WEST,
                                Variant.variant()
                                    .with(VariantProperties.MODEL, resourcelocation)
                                    .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270)
                            )
                    )
            );
    }

    private void createEndPortalFrame() {
        ResourceLocation resourcelocation = ModelLocationUtils.getModelLocation(Blocks.END_PORTAL_FRAME);
        ResourceLocation resourcelocation1 = ModelLocationUtils.getModelLocation(Blocks.END_PORTAL_FRAME, "_filled");
        this.blockStateOutput
            .accept(
                MultiVariantGenerator.multiVariant(Blocks.END_PORTAL_FRAME)
                    .with(
                        PropertyDispatch.property(BlockStateProperties.EYE)
                            .select(false, Variant.variant().with(VariantProperties.MODEL, resourcelocation))
                            .select(true, Variant.variant().with(VariantProperties.MODEL, resourcelocation1))
                    )
                    .with(createHorizontalFacingDispatchAlt())
            );
    }

    private void createChorusPlant() {
        ResourceLocation resourcelocation = ModelLocationUtils.getModelLocation(Blocks.CHORUS_PLANT, "_side");
        ResourceLocation resourcelocation1 = ModelLocationUtils.getModelLocation(Blocks.CHORUS_PLANT, "_noside");
        ResourceLocation resourcelocation2 = ModelLocationUtils.getModelLocation(Blocks.CHORUS_PLANT, "_noside1");
        ResourceLocation resourcelocation3 = ModelLocationUtils.getModelLocation(Blocks.CHORUS_PLANT, "_noside2");
        ResourceLocation resourcelocation4 = ModelLocationUtils.getModelLocation(Blocks.CHORUS_PLANT, "_noside3");
        this.blockStateOutput
            .accept(
                MultiPartGenerator.multiPart(Blocks.CHORUS_PLANT)
                    .with(Condition.condition().term(BlockStateProperties.NORTH, true), Variant.variant().with(VariantProperties.MODEL, resourcelocation))
                    .with(
                        Condition.condition().term(BlockStateProperties.EAST, true),
                        Variant.variant()
                            .with(VariantProperties.MODEL, resourcelocation)
                            .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90)
                            .with(VariantProperties.UV_LOCK, true)
                    )
                    .with(
                        Condition.condition().term(BlockStateProperties.SOUTH, true),
                        Variant.variant()
                            .with(VariantProperties.MODEL, resourcelocation)
                            .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180)
                            .with(VariantProperties.UV_LOCK, true)
                    )
                    .with(
                        Condition.condition().term(BlockStateProperties.WEST, true),
                        Variant.variant()
                            .with(VariantProperties.MODEL, resourcelocation)
                            .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270)
                            .with(VariantProperties.UV_LOCK, true)
                    )
                    .with(
                        Condition.condition().term(BlockStateProperties.UP, true),
                        Variant.variant()
                            .with(VariantProperties.MODEL, resourcelocation)
                            .with(VariantProperties.X_ROT, VariantProperties.Rotation.R270)
                            .with(VariantProperties.UV_LOCK, true)
                    )
                    .with(
                        Condition.condition().term(BlockStateProperties.DOWN, true),
                        Variant.variant()
                            .with(VariantProperties.MODEL, resourcelocation)
                            .with(VariantProperties.X_ROT, VariantProperties.Rotation.R90)
                            .with(VariantProperties.UV_LOCK, true)
                    )
                    .with(
                        Condition.condition().term(BlockStateProperties.NORTH, false),
                        Variant.variant().with(VariantProperties.MODEL, resourcelocation1).with(VariantProperties.WEIGHT, 2),
                        Variant.variant().with(VariantProperties.MODEL, resourcelocation2),
                        Variant.variant().with(VariantProperties.MODEL, resourcelocation3),
                        Variant.variant().with(VariantProperties.MODEL, resourcelocation4)
                    )
                    .with(
                        Condition.condition().term(BlockStateProperties.EAST, false),
                        Variant.variant()
                            .with(VariantProperties.MODEL, resourcelocation2)
                            .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90)
                            .with(VariantProperties.UV_LOCK, true),
                        Variant.variant()
                            .with(VariantProperties.MODEL, resourcelocation3)
                            .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90)
                            .with(VariantProperties.UV_LOCK, true),
                        Variant.variant()
                            .with(VariantProperties.MODEL, resourcelocation4)
                            .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90)
                            .with(VariantProperties.UV_LOCK, true),
                        Variant.variant()
                            .with(VariantProperties.MODEL, resourcelocation1)
                            .with(VariantProperties.WEIGHT, 2)
                            .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90)
                            .with(VariantProperties.UV_LOCK, true)
                    )
                    .with(
                        Condition.condition().term(BlockStateProperties.SOUTH, false),
                        Variant.variant()
                            .with(VariantProperties.MODEL, resourcelocation3)
                            .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180)
                            .with(VariantProperties.UV_LOCK, true),
                        Variant.variant()
                            .with(VariantProperties.MODEL, resourcelocation4)
                            .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180)
                            .with(VariantProperties.UV_LOCK, true),
                        Variant.variant()
                            .with(VariantProperties.MODEL, resourcelocation1)
                            .with(VariantProperties.WEIGHT, 2)
                            .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180)
                            .with(VariantProperties.UV_LOCK, true),
                        Variant.variant()
                            .with(VariantProperties.MODEL, resourcelocation2)
                            .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180)
                            .with(VariantProperties.UV_LOCK, true)
                    )
                    .with(
                        Condition.condition().term(BlockStateProperties.WEST, false),
                        Variant.variant()
                            .with(VariantProperties.MODEL, resourcelocation4)
                            .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270)
                            .with(VariantProperties.UV_LOCK, true),
                        Variant.variant()
                            .with(VariantProperties.MODEL, resourcelocation1)
                            .with(VariantProperties.WEIGHT, 2)
                            .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270)
                            .with(VariantProperties.UV_LOCK, true),
                        Variant.variant()
                            .with(VariantProperties.MODEL, resourcelocation2)
                            .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270)
                            .with(VariantProperties.UV_LOCK, true),
                        Variant.variant()
                            .with(VariantProperties.MODEL, resourcelocation3)
                            .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270)
                            .with(VariantProperties.UV_LOCK, true)
                    )
                    .with(
                        Condition.condition().term(BlockStateProperties.UP, false),
                        Variant.variant()
                            .with(VariantProperties.MODEL, resourcelocation1)
                            .with(VariantProperties.WEIGHT, 2)
                            .with(VariantProperties.X_ROT, VariantProperties.Rotation.R270)
                            .with(VariantProperties.UV_LOCK, true),
                        Variant.variant()
                            .with(VariantProperties.MODEL, resourcelocation4)
                            .with(VariantProperties.X_ROT, VariantProperties.Rotation.R270)
                            .with(VariantProperties.UV_LOCK, true),
                        Variant.variant()
                            .with(VariantProperties.MODEL, resourcelocation2)
                            .with(VariantProperties.X_ROT, VariantProperties.Rotation.R270)
                            .with(VariantProperties.UV_LOCK, true),
                        Variant.variant()
                            .with(VariantProperties.MODEL, resourcelocation3)
                            .with(VariantProperties.X_ROT, VariantProperties.Rotation.R270)
                            .with(VariantProperties.UV_LOCK, true)
                    )
                    .with(
                        Condition.condition().term(BlockStateProperties.DOWN, false),
                        Variant.variant()
                            .with(VariantProperties.MODEL, resourcelocation4)
                            .with(VariantProperties.X_ROT, VariantProperties.Rotation.R90)
                            .with(VariantProperties.UV_LOCK, true),
                        Variant.variant()
                            .with(VariantProperties.MODEL, resourcelocation3)
                            .with(VariantProperties.X_ROT, VariantProperties.Rotation.R90)
                            .with(VariantProperties.UV_LOCK, true),
                        Variant.variant()
                            .with(VariantProperties.MODEL, resourcelocation2)
                            .with(VariantProperties.X_ROT, VariantProperties.Rotation.R90)
                            .with(VariantProperties.UV_LOCK, true),
                        Variant.variant()
                            .with(VariantProperties.MODEL, resourcelocation1)
                            .with(VariantProperties.WEIGHT, 2)
                            .with(VariantProperties.X_ROT, VariantProperties.Rotation.R90)
                            .with(VariantProperties.UV_LOCK, true)
                    )
            );
    }

    private void createComposter() {
        this.blockStateOutput
            .accept(
                MultiPartGenerator.multiPart(Blocks.COMPOSTER)
                    .with(Variant.variant().with(VariantProperties.MODEL, TextureMapping.getBlockTexture(Blocks.COMPOSTER)))
                    .with(
                        Condition.condition().term(BlockStateProperties.LEVEL_COMPOSTER, 1),
                        Variant.variant().with(VariantProperties.MODEL, TextureMapping.getBlockTexture(Blocks.COMPOSTER, "_contents1"))
                    )
                    .with(
                        Condition.condition().term(BlockStateProperties.LEVEL_COMPOSTER, 2),
                        Variant.variant().with(VariantProperties.MODEL, TextureMapping.getBlockTexture(Blocks.COMPOSTER, "_contents2"))
                    )
                    .with(
                        Condition.condition().term(BlockStateProperties.LEVEL_COMPOSTER, 3),
                        Variant.variant().with(VariantProperties.MODEL, TextureMapping.getBlockTexture(Blocks.COMPOSTER, "_contents3"))
                    )
                    .with(
                        Condition.condition().term(BlockStateProperties.LEVEL_COMPOSTER, 4),
                        Variant.variant().with(VariantProperties.MODEL, TextureMapping.getBlockTexture(Blocks.COMPOSTER, "_contents4"))
                    )
                    .with(
                        Condition.condition().term(BlockStateProperties.LEVEL_COMPOSTER, 5),
                        Variant.variant().with(VariantProperties.MODEL, TextureMapping.getBlockTexture(Blocks.COMPOSTER, "_contents5"))
                    )
                    .with(
                        Condition.condition().term(BlockStateProperties.LEVEL_COMPOSTER, 6),
                        Variant.variant().with(VariantProperties.MODEL, TextureMapping.getBlockTexture(Blocks.COMPOSTER, "_contents6"))
                    )
                    .with(
                        Condition.condition().term(BlockStateProperties.LEVEL_COMPOSTER, 7),
                        Variant.variant().with(VariantProperties.MODEL, TextureMapping.getBlockTexture(Blocks.COMPOSTER, "_contents7"))
                    )
                    .with(
                        Condition.condition().term(BlockStateProperties.LEVEL_COMPOSTER, 8),
                        Variant.variant().with(VariantProperties.MODEL, TextureMapping.getBlockTexture(Blocks.COMPOSTER, "_contents_ready"))
                    )
            );
    }

    private void createCopperBulb(Block p_308914_) {
        ResourceLocation resourcelocation = ModelTemplates.CUBE_ALL.create(p_308914_, TextureMapping.cube(p_308914_), this.modelOutput);
        ResourceLocation resourcelocation1 = this.createSuffixedVariant(p_308914_, "_powered", ModelTemplates.CUBE_ALL, TextureMapping::cube);
        ResourceLocation resourcelocation2 = this.createSuffixedVariant(p_308914_, "_lit", ModelTemplates.CUBE_ALL, TextureMapping::cube);
        ResourceLocation resourcelocation3 = this.createSuffixedVariant(p_308914_, "_lit_powered", ModelTemplates.CUBE_ALL, TextureMapping::cube);
        this.blockStateOutput.accept(this.createCopperBulb(p_308914_, resourcelocation, resourcelocation2, resourcelocation1, resourcelocation3));
    }

    private BlockStateGenerator createCopperBulb(
        Block p_309175_, ResourceLocation p_309189_, ResourceLocation p_308957_, ResourceLocation p_308948_, ResourceLocation p_309019_
    ) {
        return MultiVariantGenerator.multiVariant(p_309175_)
            .with(
                PropertyDispatch.properties(BlockStateProperties.LIT, BlockStateProperties.POWERED)
                    .generate(
                        (p_308471_, p_308472_) -> p_308471_
                                ? Variant.variant().with(VariantProperties.MODEL, p_308472_ ? p_309019_ : p_308957_)
                                : Variant.variant().with(VariantProperties.MODEL, p_308472_ ? p_308948_ : p_309189_)
                    )
            );
    }

    private void copyCopperBulbModel(Block p_309045_, Block p_309092_) {
        ResourceLocation resourcelocation = ModelLocationUtils.getModelLocation(p_309045_);
        ResourceLocation resourcelocation1 = ModelLocationUtils.getModelLocation(p_309045_, "_powered");
        ResourceLocation resourcelocation2 = ModelLocationUtils.getModelLocation(p_309045_, "_lit");
        ResourceLocation resourcelocation3 = ModelLocationUtils.getModelLocation(p_309045_, "_lit_powered");
        this.delegateItemModel(p_309092_, ModelLocationUtils.getModelLocation(p_309045_.asItem()));
        this.blockStateOutput.accept(this.createCopperBulb(p_309092_, resourcelocation, resourcelocation2, resourcelocation1, resourcelocation3));
    }

    private void createAmethystCluster(Block p_176252_) {
        this.skipAutoItemBlock(p_176252_);
        this.blockStateOutput
            .accept(
                MultiVariantGenerator.multiVariant(
                        p_176252_,
                        Variant.variant()
                            .with(VariantProperties.MODEL, ModelTemplates.CROSS.create(p_176252_, TextureMapping.cross(p_176252_), this.modelOutput))
                    )
                    .with(this.createColumnWithFacing())
            );
    }

    private void createAmethystClusters() {
        this.createAmethystCluster(Blocks.SMALL_AMETHYST_BUD);
        this.createAmethystCluster(Blocks.MEDIUM_AMETHYST_BUD);
        this.createAmethystCluster(Blocks.LARGE_AMETHYST_BUD);
        this.createAmethystCluster(Blocks.AMETHYST_CLUSTER);
    }

    private void createPointedDripstone() {
        this.skipAutoItemBlock(Blocks.POINTED_DRIPSTONE);
        PropertyDispatch.C2<Direction, DripstoneThickness> c2 = PropertyDispatch.properties(
            BlockStateProperties.VERTICAL_DIRECTION, BlockStateProperties.DRIPSTONE_THICKNESS
        );

        for (DripstoneThickness dripstonethickness : DripstoneThickness.values()) {
            c2.select(Direction.UP, dripstonethickness, this.createPointedDripstoneVariant(Direction.UP, dripstonethickness));
        }

        for (DripstoneThickness dripstonethickness1 : DripstoneThickness.values()) {
            c2.select(Direction.DOWN, dripstonethickness1, this.createPointedDripstoneVariant(Direction.DOWN, dripstonethickness1));
        }

        this.blockStateOutput.accept(MultiVariantGenerator.multiVariant(Blocks.POINTED_DRIPSTONE).with(c2));
    }

    private Variant createPointedDripstoneVariant(Direction p_176117_, DripstoneThickness p_176118_) {
        String s = "_" + p_176117_.getSerializedName() + "_" + p_176118_.getSerializedName();
        TextureMapping texturemapping = TextureMapping.cross(TextureMapping.getBlockTexture(Blocks.POINTED_DRIPSTONE, s));
        return Variant.variant()
            .with(VariantProperties.MODEL, ModelTemplates.POINTED_DRIPSTONE.createWithSuffix(Blocks.POINTED_DRIPSTONE, s, texturemapping, this.modelOutput));
    }

    private void createNyliumBlock(Block p_124990_) {
        TextureMapping texturemapping = new TextureMapping()
            .put(TextureSlot.BOTTOM, TextureMapping.getBlockTexture(Blocks.NETHERRACK))
            .put(TextureSlot.TOP, TextureMapping.getBlockTexture(p_124990_))
            .put(TextureSlot.SIDE, TextureMapping.getBlockTexture(p_124990_, "_side"));
        this.blockStateOutput.accept(createSimpleBlock(p_124990_, ModelTemplates.CUBE_BOTTOM_TOP.create(p_124990_, texturemapping, this.modelOutput)));
    }

    private void createDaylightDetector() {
        ResourceLocation resourcelocation = TextureMapping.getBlockTexture(Blocks.DAYLIGHT_DETECTOR, "_side");
        TextureMapping texturemapping = new TextureMapping()
            .put(TextureSlot.TOP, TextureMapping.getBlockTexture(Blocks.DAYLIGHT_DETECTOR, "_top"))
            .put(TextureSlot.SIDE, resourcelocation);
        TextureMapping texturemapping1 = new TextureMapping()
            .put(TextureSlot.TOP, TextureMapping.getBlockTexture(Blocks.DAYLIGHT_DETECTOR, "_inverted_top"))
            .put(TextureSlot.SIDE, resourcelocation);
        this.blockStateOutput
            .accept(
                MultiVariantGenerator.multiVariant(Blocks.DAYLIGHT_DETECTOR)
                    .with(
                        PropertyDispatch.property(BlockStateProperties.INVERTED)
                            .select(
                                false,
                                Variant.variant()
                                    .with(
                                        VariantProperties.MODEL,
                                        ModelTemplates.DAYLIGHT_DETECTOR.create(Blocks.DAYLIGHT_DETECTOR, texturemapping, this.modelOutput)
                                    )
                            )
                            .select(
                                true,
                                Variant.variant()
                                    .with(
                                        VariantProperties.MODEL,
                                        ModelTemplates.DAYLIGHT_DETECTOR
                                            .create(
                                                ModelLocationUtils.getModelLocation(Blocks.DAYLIGHT_DETECTOR, "_inverted"), texturemapping1, this.modelOutput
                                            )
                                    )
                            )
                    )
            );
    }

    private void createRotatableColumn(Block p_124993_) {
        this.blockStateOutput
            .accept(
                MultiVariantGenerator.multiVariant(p_124993_, Variant.variant().with(VariantProperties.MODEL, ModelLocationUtils.getModelLocation(p_124993_)))
                    .with(this.createColumnWithFacing())
            );
    }

    private void createLightningRod() {
        Block block = Blocks.LIGHTNING_ROD;
        ResourceLocation resourcelocation = ModelLocationUtils.getModelLocation(block, "_on");
        ResourceLocation resourcelocation1 = ModelLocationUtils.getModelLocation(block);
        this.blockStateOutput
            .accept(
                MultiVariantGenerator.multiVariant(block, Variant.variant().with(VariantProperties.MODEL, ModelLocationUtils.getModelLocation(block)))
                    .with(this.createColumnWithFacing())
                    .with(createBooleanModelDispatch(BlockStateProperties.POWERED, resourcelocation, resourcelocation1))
            );
    }

    private void createFarmland() {
        TextureMapping texturemapping = new TextureMapping()
            .put(TextureSlot.DIRT, TextureMapping.getBlockTexture(Blocks.DIRT))
            .put(TextureSlot.TOP, TextureMapping.getBlockTexture(Blocks.FARMLAND));
        TextureMapping texturemapping1 = new TextureMapping()
            .put(TextureSlot.DIRT, TextureMapping.getBlockTexture(Blocks.DIRT))
            .put(TextureSlot.TOP, TextureMapping.getBlockTexture(Blocks.FARMLAND, "_moist"));
        ResourceLocation resourcelocation = ModelTemplates.FARMLAND.create(Blocks.FARMLAND, texturemapping, this.modelOutput);
        ResourceLocation resourcelocation1 = ModelTemplates.FARMLAND
            .create(TextureMapping.getBlockTexture(Blocks.FARMLAND, "_moist"), texturemapping1, this.modelOutput);
        this.blockStateOutput
            .accept(
                MultiVariantGenerator.multiVariant(Blocks.FARMLAND)
                    .with(createEmptyOrFullDispatch(BlockStateProperties.MOISTURE, 7, resourcelocation1, resourcelocation))
            );
    }

    private List<ResourceLocation> createFloorFireModels(Block p_124996_) {
        ResourceLocation resourcelocation = ModelTemplates.FIRE_FLOOR
            .create(ModelLocationUtils.getModelLocation(p_124996_, "_floor0"), TextureMapping.fire0(p_124996_), this.modelOutput);
        ResourceLocation resourcelocation1 = ModelTemplates.FIRE_FLOOR
            .create(ModelLocationUtils.getModelLocation(p_124996_, "_floor1"), TextureMapping.fire1(p_124996_), this.modelOutput);
        return ImmutableList.of(resourcelocation, resourcelocation1);
    }

    private List<ResourceLocation> createSideFireModels(Block p_124999_) {
        ResourceLocation resourcelocation = ModelTemplates.FIRE_SIDE
            .create(ModelLocationUtils.getModelLocation(p_124999_, "_side0"), TextureMapping.fire0(p_124999_), this.modelOutput);
        ResourceLocation resourcelocation1 = ModelTemplates.FIRE_SIDE
            .create(ModelLocationUtils.getModelLocation(p_124999_, "_side1"), TextureMapping.fire1(p_124999_), this.modelOutput);
        ResourceLocation resourcelocation2 = ModelTemplates.FIRE_SIDE_ALT
            .create(ModelLocationUtils.getModelLocation(p_124999_, "_side_alt0"), TextureMapping.fire0(p_124999_), this.modelOutput);
        ResourceLocation resourcelocation3 = ModelTemplates.FIRE_SIDE_ALT
            .create(ModelLocationUtils.getModelLocation(p_124999_, "_side_alt1"), TextureMapping.fire1(p_124999_), this.modelOutput);
        return ImmutableList.of(resourcelocation, resourcelocation1, resourcelocation2, resourcelocation3);
    }

    private List<ResourceLocation> createTopFireModels(Block p_125002_) {
        ResourceLocation resourcelocation = ModelTemplates.FIRE_UP
            .create(ModelLocationUtils.getModelLocation(p_125002_, "_up0"), TextureMapping.fire0(p_125002_), this.modelOutput);
        ResourceLocation resourcelocation1 = ModelTemplates.FIRE_UP
            .create(ModelLocationUtils.getModelLocation(p_125002_, "_up1"), TextureMapping.fire1(p_125002_), this.modelOutput);
        ResourceLocation resourcelocation2 = ModelTemplates.FIRE_UP_ALT
            .create(ModelLocationUtils.getModelLocation(p_125002_, "_up_alt0"), TextureMapping.fire0(p_125002_), this.modelOutput);
        ResourceLocation resourcelocation3 = ModelTemplates.FIRE_UP_ALT
            .create(ModelLocationUtils.getModelLocation(p_125002_, "_up_alt1"), TextureMapping.fire1(p_125002_), this.modelOutput);
        return ImmutableList.of(resourcelocation, resourcelocation1, resourcelocation2, resourcelocation3);
    }

    private static List<Variant> wrapModels(List<ResourceLocation> p_124683_, UnaryOperator<Variant> p_124684_) {
        return p_124683_.stream().map(p_176238_ -> Variant.variant().with(VariantProperties.MODEL, p_176238_)).map(p_124684_).collect(Collectors.toList());
    }

    private void createFire() {
        Condition condition = Condition.condition()
            .term(BlockStateProperties.NORTH, false)
            .term(BlockStateProperties.EAST, false)
            .term(BlockStateProperties.SOUTH, false)
            .term(BlockStateProperties.WEST, false)
            .term(BlockStateProperties.UP, false);
        List<ResourceLocation> list = this.createFloorFireModels(Blocks.FIRE);
        List<ResourceLocation> list1 = this.createSideFireModels(Blocks.FIRE);
        List<ResourceLocation> list2 = this.createTopFireModels(Blocks.FIRE);
        this.blockStateOutput
            .accept(
                MultiPartGenerator.multiPart(Blocks.FIRE)
                    .with(condition, wrapModels(list, p_124894_ -> p_124894_))
                    .with(Condition.or(Condition.condition().term(BlockStateProperties.NORTH, true), condition), wrapModels(list1, p_176243_ -> p_176243_))
                    .with(
                        Condition.or(Condition.condition().term(BlockStateProperties.EAST, true), condition),
                        wrapModels(list1, p_176240_ -> p_176240_.with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90))
                    )
                    .with(
                        Condition.or(Condition.condition().term(BlockStateProperties.SOUTH, true), condition),
                        wrapModels(list1, p_176236_ -> p_176236_.with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180))
                    )
                    .with(
                        Condition.or(Condition.condition().term(BlockStateProperties.WEST, true), condition),
                        wrapModels(list1, p_176232_ -> p_176232_.with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270))
                    )
                    .with(Condition.condition().term(BlockStateProperties.UP, true), wrapModels(list2, p_176227_ -> p_176227_))
            );
    }

    private void createSoulFire() {
        List<ResourceLocation> list = this.createFloorFireModels(Blocks.SOUL_FIRE);
        List<ResourceLocation> list1 = this.createSideFireModels(Blocks.SOUL_FIRE);
        this.blockStateOutput
            .accept(
                MultiPartGenerator.multiPart(Blocks.SOUL_FIRE)
                    .with(wrapModels(list, p_176221_ -> p_176221_))
                    .with(wrapModels(list1, p_176209_ -> p_176209_))
                    .with(wrapModels(list1, p_176200_ -> p_176200_.with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90)))
                    .with(wrapModels(list1, p_176188_ -> p_176188_.with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180)))
                    .with(wrapModels(list1, p_176143_ -> p_176143_.with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270)))
            );
    }

    private void createLantern(Block p_125005_) {
        ResourceLocation resourcelocation = TexturedModel.LANTERN.create(p_125005_, this.modelOutput);
        ResourceLocation resourcelocation1 = TexturedModel.HANGING_LANTERN.create(p_125005_, this.modelOutput);
        this.createSimpleFlatItemModel(p_125005_.asItem());
        this.blockStateOutput
            .accept(
                MultiVariantGenerator.multiVariant(p_125005_)
                    .with(createBooleanModelDispatch(BlockStateProperties.HANGING, resourcelocation1, resourcelocation))
            );
    }

    private void createMuddyMangroveRoots() {
        TextureMapping texturemapping = TextureMapping.column(
            TextureMapping.getBlockTexture(Blocks.MUDDY_MANGROVE_ROOTS, "_side"), TextureMapping.getBlockTexture(Blocks.MUDDY_MANGROVE_ROOTS, "_top")
        );
        ResourceLocation resourcelocation = ModelTemplates.CUBE_COLUMN.create(Blocks.MUDDY_MANGROVE_ROOTS, texturemapping, this.modelOutput);
        this.blockStateOutput.accept(createAxisAlignedPillarBlock(Blocks.MUDDY_MANGROVE_ROOTS, resourcelocation));
    }

    private void createMangrovePropagule() {
        this.createSimpleFlatItemModel(Items.MANGROVE_PROPAGULE);
        Block block = Blocks.MANGROVE_PROPAGULE;
        PropertyDispatch.C2<Boolean, Integer> c2 = PropertyDispatch.properties(MangrovePropaguleBlock.HANGING, MangrovePropaguleBlock.AGE);
        ResourceLocation resourcelocation = ModelLocationUtils.getModelLocation(block);

        for (int i = 0; i <= 4; i++) {
            ResourceLocation resourcelocation1 = ModelLocationUtils.getModelLocation(block, "_hanging_" + i);
            c2.select(true, i, Variant.variant().with(VariantProperties.MODEL, resourcelocation1));
            c2.select(false, i, Variant.variant().with(VariantProperties.MODEL, resourcelocation));
        }

        this.blockStateOutput.accept(MultiVariantGenerator.multiVariant(Blocks.MANGROVE_PROPAGULE).with(c2));
    }

    private void createFrostedIce() {
        this.blockStateOutput
            .accept(
                MultiVariantGenerator.multiVariant(Blocks.FROSTED_ICE)
                    .with(
                        PropertyDispatch.property(BlockStateProperties.AGE_3)
                            .select(
                                0,
                                Variant.variant()
                                    .with(
                                        VariantProperties.MODEL,
                                        this.createSuffixedVariant(Blocks.FROSTED_ICE, "_0", ModelTemplates.CUBE_ALL, TextureMapping::cube)
                                    )
                            )
                            .select(
                                1,
                                Variant.variant()
                                    .with(
                                        VariantProperties.MODEL,
                                        this.createSuffixedVariant(Blocks.FROSTED_ICE, "_1", ModelTemplates.CUBE_ALL, TextureMapping::cube)
                                    )
                            )
                            .select(
                                2,
                                Variant.variant()
                                    .with(
                                        VariantProperties.MODEL,
                                        this.createSuffixedVariant(Blocks.FROSTED_ICE, "_2", ModelTemplates.CUBE_ALL, TextureMapping::cube)
                                    )
                            )
                            .select(
                                3,
                                Variant.variant()
                                    .with(
                                        VariantProperties.MODEL,
                                        this.createSuffixedVariant(Blocks.FROSTED_ICE, "_3", ModelTemplates.CUBE_ALL, TextureMapping::cube)
                                    )
                            )
                    )
            );
    }

    private void createGrassBlocks() {
        ResourceLocation resourcelocation = TextureMapping.getBlockTexture(Blocks.DIRT);
        TextureMapping texturemapping = new TextureMapping()
            .put(TextureSlot.BOTTOM, resourcelocation)
            .copyForced(TextureSlot.BOTTOM, TextureSlot.PARTICLE)
            .put(TextureSlot.TOP, TextureMapping.getBlockTexture(Blocks.GRASS_BLOCK, "_top"))
            .put(TextureSlot.SIDE, TextureMapping.getBlockTexture(Blocks.GRASS_BLOCK, "_snow"));
        Variant variant = Variant.variant()
            .with(VariantProperties.MODEL, ModelTemplates.CUBE_BOTTOM_TOP.createWithSuffix(Blocks.GRASS_BLOCK, "_snow", texturemapping, this.modelOutput));
        this.createGrassLikeBlock(Blocks.GRASS_BLOCK, ModelLocationUtils.getModelLocation(Blocks.GRASS_BLOCK), variant);
        ResourceLocation resourcelocation1 = TexturedModel.CUBE_TOP_BOTTOM
            .get(Blocks.MYCELIUM)
            .updateTextures(p_176198_ -> p_176198_.put(TextureSlot.BOTTOM, resourcelocation))
            .create(Blocks.MYCELIUM, this.modelOutput);
        this.createGrassLikeBlock(Blocks.MYCELIUM, resourcelocation1, variant);
        ResourceLocation resourcelocation2 = TexturedModel.CUBE_TOP_BOTTOM
            .get(Blocks.PODZOL)
            .updateTextures(p_176154_ -> p_176154_.put(TextureSlot.BOTTOM, resourcelocation))
            .create(Blocks.PODZOL, this.modelOutput);
        this.createGrassLikeBlock(Blocks.PODZOL, resourcelocation2, variant);
    }

    private void createGrassLikeBlock(Block p_124600_, ResourceLocation p_124601_, Variant p_124602_) {
        List<Variant> list = Arrays.asList(createRotatedVariants(p_124601_));
        this.blockStateOutput
            .accept(
                MultiVariantGenerator.multiVariant(p_124600_)
                    .with(PropertyDispatch.property(BlockStateProperties.SNOWY).select(true, p_124602_).select(false, list))
            );
    }

    private void createCocoa() {
        this.createSimpleFlatItemModel(Items.COCOA_BEANS);
        this.blockStateOutput
            .accept(
                MultiVariantGenerator.multiVariant(Blocks.COCOA)
                    .with(
                        PropertyDispatch.property(BlockStateProperties.AGE_2)
                            .select(0, Variant.variant().with(VariantProperties.MODEL, ModelLocationUtils.getModelLocation(Blocks.COCOA, "_stage0")))
                            .select(1, Variant.variant().with(VariantProperties.MODEL, ModelLocationUtils.getModelLocation(Blocks.COCOA, "_stage1")))
                            .select(2, Variant.variant().with(VariantProperties.MODEL, ModelLocationUtils.getModelLocation(Blocks.COCOA, "_stage2")))
                    )
                    .with(createHorizontalFacingDispatchAlt())
            );
    }

    private void createDirtPath() {
        this.blockStateOutput.accept(createRotatedVariant(Blocks.DIRT_PATH, ModelLocationUtils.getModelLocation(Blocks.DIRT_PATH)));
    }

    private void createWeightedPressurePlate(Block p_124919_, Block p_124920_) {
        TextureMapping texturemapping = TextureMapping.defaultTexture(p_124920_);
        ResourceLocation resourcelocation = ModelTemplates.PRESSURE_PLATE_UP.create(p_124919_, texturemapping, this.modelOutput);
        ResourceLocation resourcelocation1 = ModelTemplates.PRESSURE_PLATE_DOWN.create(p_124919_, texturemapping, this.modelOutput);
        this.blockStateOutput
            .accept(
                MultiVariantGenerator.multiVariant(p_124919_)
                    .with(createEmptyOrFullDispatch(BlockStateProperties.POWER, 1, resourcelocation1, resourcelocation))
            );
    }

    private void createHopper() {
        ResourceLocation resourcelocation = ModelLocationUtils.getModelLocation(Blocks.HOPPER);
        ResourceLocation resourcelocation1 = ModelLocationUtils.getModelLocation(Blocks.HOPPER, "_side");
        this.createSimpleFlatItemModel(Items.HOPPER);
        this.blockStateOutput
            .accept(
                MultiVariantGenerator.multiVariant(Blocks.HOPPER)
                    .with(
                        PropertyDispatch.property(BlockStateProperties.FACING_HOPPER)
                            .select(Direction.DOWN, Variant.variant().with(VariantProperties.MODEL, resourcelocation))
                            .select(Direction.NORTH, Variant.variant().with(VariantProperties.MODEL, resourcelocation1))
                            .select(
                                Direction.EAST,
                                Variant.variant()
                                    .with(VariantProperties.MODEL, resourcelocation1)
                                    .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90)
                            )
                            .select(
                                Direction.SOUTH,
                                Variant.variant()
                                    .with(VariantProperties.MODEL, resourcelocation1)
                                    .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180)
                            )
                            .select(
                                Direction.WEST,
                                Variant.variant()
                                    .with(VariantProperties.MODEL, resourcelocation1)
                                    .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270)
                            )
                    )
            );
    }

    private void copyModel(Block p_124939_, Block p_124940_) {
        ResourceLocation resourcelocation = ModelLocationUtils.getModelLocation(p_124939_);
        this.blockStateOutput.accept(MultiVariantGenerator.multiVariant(p_124940_, Variant.variant().with(VariantProperties.MODEL, resourcelocation)));
        this.delegateItemModel(p_124940_, resourcelocation);
    }

    private void createIronBars() {
        ResourceLocation resourcelocation = ModelLocationUtils.getModelLocation(Blocks.IRON_BARS, "_post_ends");
        ResourceLocation resourcelocation1 = ModelLocationUtils.getModelLocation(Blocks.IRON_BARS, "_post");
        ResourceLocation resourcelocation2 = ModelLocationUtils.getModelLocation(Blocks.IRON_BARS, "_cap");
        ResourceLocation resourcelocation3 = ModelLocationUtils.getModelLocation(Blocks.IRON_BARS, "_cap_alt");
        ResourceLocation resourcelocation4 = ModelLocationUtils.getModelLocation(Blocks.IRON_BARS, "_side");
        ResourceLocation resourcelocation5 = ModelLocationUtils.getModelLocation(Blocks.IRON_BARS, "_side_alt");
        this.blockStateOutput
            .accept(
                MultiPartGenerator.multiPart(Blocks.IRON_BARS)
                    .with(Variant.variant().with(VariantProperties.MODEL, resourcelocation))
                    .with(
                        Condition.condition()
                            .term(BlockStateProperties.NORTH, false)
                            .term(BlockStateProperties.EAST, false)
                            .term(BlockStateProperties.SOUTH, false)
                            .term(BlockStateProperties.WEST, false),
                        Variant.variant().with(VariantProperties.MODEL, resourcelocation1)
                    )
                    .with(
                        Condition.condition()
                            .term(BlockStateProperties.NORTH, true)
                            .term(BlockStateProperties.EAST, false)
                            .term(BlockStateProperties.SOUTH, false)
                            .term(BlockStateProperties.WEST, false),
                        Variant.variant().with(VariantProperties.MODEL, resourcelocation2)
                    )
                    .with(
                        Condition.condition()
                            .term(BlockStateProperties.NORTH, false)
                            .term(BlockStateProperties.EAST, true)
                            .term(BlockStateProperties.SOUTH, false)
                            .term(BlockStateProperties.WEST, false),
                        Variant.variant().with(VariantProperties.MODEL, resourcelocation2).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90)
                    )
                    .with(
                        Condition.condition()
                            .term(BlockStateProperties.NORTH, false)
                            .term(BlockStateProperties.EAST, false)
                            .term(BlockStateProperties.SOUTH, true)
                            .term(BlockStateProperties.WEST, false),
                        Variant.variant().with(VariantProperties.MODEL, resourcelocation3)
                    )
                    .with(
                        Condition.condition()
                            .term(BlockStateProperties.NORTH, false)
                            .term(BlockStateProperties.EAST, false)
                            .term(BlockStateProperties.SOUTH, false)
                            .term(BlockStateProperties.WEST, true),
                        Variant.variant().with(VariantProperties.MODEL, resourcelocation3).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90)
                    )
                    .with(Condition.condition().term(BlockStateProperties.NORTH, true), Variant.variant().with(VariantProperties.MODEL, resourcelocation4))
                    .with(
                        Condition.condition().term(BlockStateProperties.EAST, true),
                        Variant.variant().with(VariantProperties.MODEL, resourcelocation4).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90)
                    )
                    .with(Condition.condition().term(BlockStateProperties.SOUTH, true), Variant.variant().with(VariantProperties.MODEL, resourcelocation5))
                    .with(
                        Condition.condition().term(BlockStateProperties.WEST, true),
                        Variant.variant().with(VariantProperties.MODEL, resourcelocation5).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90)
                    )
            );
        this.createSimpleFlatItemModel(Blocks.IRON_BARS);
    }

    private void createNonTemplateHorizontalBlock(Block p_125008_) {
        this.blockStateOutput
            .accept(
                MultiVariantGenerator.multiVariant(p_125008_, Variant.variant().with(VariantProperties.MODEL, ModelLocationUtils.getModelLocation(p_125008_)))
                    .with(createHorizontalFacingDispatch())
            );
    }

    private void createLever() {
        ResourceLocation resourcelocation = ModelLocationUtils.getModelLocation(Blocks.LEVER);
        ResourceLocation resourcelocation1 = ModelLocationUtils.getModelLocation(Blocks.LEVER, "_on");
        this.createSimpleFlatItemModel(Blocks.LEVER);
        this.blockStateOutput
            .accept(
                MultiVariantGenerator.multiVariant(Blocks.LEVER)
                    .with(createBooleanModelDispatch(BlockStateProperties.POWERED, resourcelocation, resourcelocation1))
                    .with(
                        PropertyDispatch.properties(BlockStateProperties.ATTACH_FACE, BlockStateProperties.HORIZONTAL_FACING)
                            .select(
                                AttachFace.CEILING,
                                Direction.NORTH,
                                Variant.variant()
                                    .with(VariantProperties.X_ROT, VariantProperties.Rotation.R180)
                                    .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180)
                            )
                            .select(
                                AttachFace.CEILING,
                                Direction.EAST,
                                Variant.variant()
                                    .with(VariantProperties.X_ROT, VariantProperties.Rotation.R180)
                                    .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270)
                            )
                            .select(AttachFace.CEILING, Direction.SOUTH, Variant.variant().with(VariantProperties.X_ROT, VariantProperties.Rotation.R180))
                            .select(
                                AttachFace.CEILING,
                                Direction.WEST,
                                Variant.variant()
                                    .with(VariantProperties.X_ROT, VariantProperties.Rotation.R180)
                                    .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90)
                            )
                            .select(AttachFace.FLOOR, Direction.NORTH, Variant.variant())
                            .select(AttachFace.FLOOR, Direction.EAST, Variant.variant().with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90))
                            .select(AttachFace.FLOOR, Direction.SOUTH, Variant.variant().with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180))
                            .select(AttachFace.FLOOR, Direction.WEST, Variant.variant().with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270))
                            .select(AttachFace.WALL, Direction.NORTH, Variant.variant().with(VariantProperties.X_ROT, VariantProperties.Rotation.R90))
                            .select(
                                AttachFace.WALL,
                                Direction.EAST,
                                Variant.variant()
                                    .with(VariantProperties.X_ROT, VariantProperties.Rotation.R90)
                                    .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90)
                            )
                            .select(
                                AttachFace.WALL,
                                Direction.SOUTH,
                                Variant.variant()
                                    .with(VariantProperties.X_ROT, VariantProperties.Rotation.R90)
                                    .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180)
                            )
                            .select(
                                AttachFace.WALL,
                                Direction.WEST,
                                Variant.variant()
                                    .with(VariantProperties.X_ROT, VariantProperties.Rotation.R90)
                                    .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270)
                            )
                    )
            );
    }

    private void createLilyPad() {
        this.createSimpleFlatItemModel(Blocks.LILY_PAD);
        this.blockStateOutput.accept(createRotatedVariant(Blocks.LILY_PAD, ModelLocationUtils.getModelLocation(Blocks.LILY_PAD)));
    }

    private void createFrogspawnBlock() {
        this.createSimpleFlatItemModel(Blocks.FROGSPAWN);
        this.blockStateOutput.accept(createSimpleBlock(Blocks.FROGSPAWN, ModelLocationUtils.getModelLocation(Blocks.FROGSPAWN)));
    }

    private void createNetherPortalBlock() {
        this.blockStateOutput
            .accept(
                MultiVariantGenerator.multiVariant(Blocks.NETHER_PORTAL)
                    .with(
                        PropertyDispatch.property(BlockStateProperties.HORIZONTAL_AXIS)
                            .select(
                                Direction.Axis.X,
                                Variant.variant().with(VariantProperties.MODEL, ModelLocationUtils.getModelLocation(Blocks.NETHER_PORTAL, "_ns"))
                            )
                            .select(
                                Direction.Axis.Z,
                                Variant.variant().with(VariantProperties.MODEL, ModelLocationUtils.getModelLocation(Blocks.NETHER_PORTAL, "_ew"))
                            )
                    )
            );
    }

    private void createNetherrack() {
        ResourceLocation resourcelocation = TexturedModel.CUBE.create(Blocks.NETHERRACK, this.modelOutput);
        this.blockStateOutput
            .accept(
                MultiVariantGenerator.multiVariant(
                    Blocks.NETHERRACK,
                    Variant.variant().with(VariantProperties.MODEL, resourcelocation),
                    Variant.variant().with(VariantProperties.MODEL, resourcelocation).with(VariantProperties.X_ROT, VariantProperties.Rotation.R90),
                    Variant.variant().with(VariantProperties.MODEL, resourcelocation).with(VariantProperties.X_ROT, VariantProperties.Rotation.R180),
                    Variant.variant().with(VariantProperties.MODEL, resourcelocation).with(VariantProperties.X_ROT, VariantProperties.Rotation.R270),
                    Variant.variant().with(VariantProperties.MODEL, resourcelocation).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90),
                    Variant.variant()
                        .with(VariantProperties.MODEL, resourcelocation)
                        .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90)
                        .with(VariantProperties.X_ROT, VariantProperties.Rotation.R90),
                    Variant.variant()
                        .with(VariantProperties.MODEL, resourcelocation)
                        .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90)
                        .with(VariantProperties.X_ROT, VariantProperties.Rotation.R180),
                    Variant.variant()
                        .with(VariantProperties.MODEL, resourcelocation)
                        .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90)
                        .with(VariantProperties.X_ROT, VariantProperties.Rotation.R270),
                    Variant.variant().with(VariantProperties.MODEL, resourcelocation).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180),
                    Variant.variant()
                        .with(VariantProperties.MODEL, resourcelocation)
                        .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180)
                        .with(VariantProperties.X_ROT, VariantProperties.Rotation.R90),
                    Variant.variant()
                        .with(VariantProperties.MODEL, resourcelocation)
                        .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180)
                        .with(VariantProperties.X_ROT, VariantProperties.Rotation.R180),
                    Variant.variant()
                        .with(VariantProperties.MODEL, resourcelocation)
                        .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180)
                        .with(VariantProperties.X_ROT, VariantProperties.Rotation.R270),
                    Variant.variant().with(VariantProperties.MODEL, resourcelocation).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270),
                    Variant.variant()
                        .with(VariantProperties.MODEL, resourcelocation)
                        .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270)
                        .with(VariantProperties.X_ROT, VariantProperties.Rotation.R90),
                    Variant.variant()
                        .with(VariantProperties.MODEL, resourcelocation)
                        .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270)
                        .with(VariantProperties.X_ROT, VariantProperties.Rotation.R180),
                    Variant.variant()
                        .with(VariantProperties.MODEL, resourcelocation)
                        .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270)
                        .with(VariantProperties.X_ROT, VariantProperties.Rotation.R270)
                )
            );
    }

    private void createObserver() {
        ResourceLocation resourcelocation = ModelLocationUtils.getModelLocation(Blocks.OBSERVER);
        ResourceLocation resourcelocation1 = ModelLocationUtils.getModelLocation(Blocks.OBSERVER, "_on");
        this.blockStateOutput
            .accept(
                MultiVariantGenerator.multiVariant(Blocks.OBSERVER)
                    .with(createBooleanModelDispatch(BlockStateProperties.POWERED, resourcelocation1, resourcelocation))
                    .with(createFacingDispatch())
            );
    }

    private void createPistons() {
        TextureMapping texturemapping = new TextureMapping()
            .put(TextureSlot.BOTTOM, TextureMapping.getBlockTexture(Blocks.PISTON, "_bottom"))
            .put(TextureSlot.SIDE, TextureMapping.getBlockTexture(Blocks.PISTON, "_side"));
        ResourceLocation resourcelocation = TextureMapping.getBlockTexture(Blocks.PISTON, "_top_sticky");
        ResourceLocation resourcelocation1 = TextureMapping.getBlockTexture(Blocks.PISTON, "_top");
        TextureMapping texturemapping1 = texturemapping.copyAndUpdate(TextureSlot.PLATFORM, resourcelocation);
        TextureMapping texturemapping2 = texturemapping.copyAndUpdate(TextureSlot.PLATFORM, resourcelocation1);
        ResourceLocation resourcelocation2 = ModelLocationUtils.getModelLocation(Blocks.PISTON, "_base");
        this.createPistonVariant(Blocks.PISTON, resourcelocation2, texturemapping2);
        this.createPistonVariant(Blocks.STICKY_PISTON, resourcelocation2, texturemapping1);
        ResourceLocation resourcelocation3 = ModelTemplates.CUBE_BOTTOM_TOP
            .createWithSuffix(Blocks.PISTON, "_inventory", texturemapping.copyAndUpdate(TextureSlot.TOP, resourcelocation1), this.modelOutput);
        ResourceLocation resourcelocation4 = ModelTemplates.CUBE_BOTTOM_TOP
            .createWithSuffix(Blocks.STICKY_PISTON, "_inventory", texturemapping.copyAndUpdate(TextureSlot.TOP, resourcelocation), this.modelOutput);
        this.delegateItemModel(Blocks.PISTON, resourcelocation3);
        this.delegateItemModel(Blocks.STICKY_PISTON, resourcelocation4);
    }

    private void createPistonVariant(Block p_124604_, ResourceLocation p_124605_, TextureMapping p_124606_) {
        ResourceLocation resourcelocation = ModelTemplates.PISTON.create(p_124604_, p_124606_, this.modelOutput);
        this.blockStateOutput
            .accept(
                MultiVariantGenerator.multiVariant(p_124604_)
                    .with(createBooleanModelDispatch(BlockStateProperties.EXTENDED, p_124605_, resourcelocation))
                    .with(createFacingDispatch())
            );
    }

    private void createPistonHeads() {
        TextureMapping texturemapping = new TextureMapping()
            .put(TextureSlot.UNSTICKY, TextureMapping.getBlockTexture(Blocks.PISTON, "_top"))
            .put(TextureSlot.SIDE, TextureMapping.getBlockTexture(Blocks.PISTON, "_side"));
        TextureMapping texturemapping1 = texturemapping.copyAndUpdate(TextureSlot.PLATFORM, TextureMapping.getBlockTexture(Blocks.PISTON, "_top_sticky"));
        TextureMapping texturemapping2 = texturemapping.copyAndUpdate(TextureSlot.PLATFORM, TextureMapping.getBlockTexture(Blocks.PISTON, "_top"));
        this.blockStateOutput
            .accept(
                MultiVariantGenerator.multiVariant(Blocks.PISTON_HEAD)
                    .with(
                        PropertyDispatch.properties(BlockStateProperties.SHORT, BlockStateProperties.PISTON_TYPE)
                            .select(
                                false,
                                PistonType.DEFAULT,
                                Variant.variant()
                                    .with(
                                        VariantProperties.MODEL,
                                        ModelTemplates.PISTON_HEAD.createWithSuffix(Blocks.PISTON, "_head", texturemapping2, this.modelOutput)
                                    )
                            )
                            .select(
                                false,
                                PistonType.STICKY,
                                Variant.variant()
                                    .with(
                                        VariantProperties.MODEL,
                                        ModelTemplates.PISTON_HEAD.createWithSuffix(Blocks.PISTON, "_head_sticky", texturemapping1, this.modelOutput)
                                    )
                            )
                            .select(
                                true,
                                PistonType.DEFAULT,
                                Variant.variant()
                                    .with(
                                        VariantProperties.MODEL,
                                        ModelTemplates.PISTON_HEAD_SHORT.createWithSuffix(Blocks.PISTON, "_head_short", texturemapping2, this.modelOutput)
                                    )
                            )
                            .select(
                                true,
                                PistonType.STICKY,
                                Variant.variant()
                                    .with(
                                        VariantProperties.MODEL,
                                        ModelTemplates.PISTON_HEAD_SHORT
                                            .createWithSuffix(Blocks.PISTON, "_head_short_sticky", texturemapping1, this.modelOutput)
                                    )
                            )
                    )
                    .with(createFacingDispatch())
            );
    }

    private void createTrialSpawner() {
        Block block = Blocks.TRIAL_SPAWNER;
        TextureMapping texturemapping = TextureMapping.trialSpawner(block, "_side_inactive", "_top_inactive");
        TextureMapping texturemapping1 = TextureMapping.trialSpawner(block, "_side_active", "_top_active");
        TextureMapping texturemapping2 = TextureMapping.trialSpawner(block, "_side_active", "_top_ejecting_reward");
        TextureMapping texturemapping3 = TextureMapping.trialSpawner(block, "_side_inactive_ominous", "_top_inactive_ominous");
        TextureMapping texturemapping4 = TextureMapping.trialSpawner(block, "_side_active_ominous", "_top_active_ominous");
        TextureMapping texturemapping5 = TextureMapping.trialSpawner(block, "_side_active_ominous", "_top_ejecting_reward_ominous");
        ResourceLocation resourcelocation = ModelTemplates.CUBE_BOTTOM_TOP_INNER_FACES.create(block, texturemapping, this.modelOutput);
        ResourceLocation resourcelocation1 = ModelTemplates.CUBE_BOTTOM_TOP_INNER_FACES.createWithSuffix(block, "_active", texturemapping1, this.modelOutput);
        ResourceLocation resourcelocation2 = ModelTemplates.CUBE_BOTTOM_TOP_INNER_FACES
            .createWithSuffix(block, "_ejecting_reward", texturemapping2, this.modelOutput);
        ResourceLocation resourcelocation3 = ModelTemplates.CUBE_BOTTOM_TOP_INNER_FACES
            .createWithSuffix(block, "_inactive_ominous", texturemapping3, this.modelOutput);
        ResourceLocation resourcelocation4 = ModelTemplates.CUBE_BOTTOM_TOP_INNER_FACES
            .createWithSuffix(block, "_active_ominous", texturemapping4, this.modelOutput);
        ResourceLocation resourcelocation5 = ModelTemplates.CUBE_BOTTOM_TOP_INNER_FACES
            .createWithSuffix(block, "_ejecting_reward_ominous", texturemapping5, this.modelOutput);
        this.delegateItemModel(block, resourcelocation);
        this.blockStateOutput
            .accept(
                MultiVariantGenerator.multiVariant(block)
                    .with(
                        PropertyDispatch.properties(BlockStateProperties.TRIAL_SPAWNER_STATE, BlockStateProperties.OMINOUS)
                            .generate(
                                (p_337483_, p_337484_) -> {
                                    return switch (p_337483_) {
                                        case INACTIVE, COOLDOWN -> Variant.variant()
                                        .with(VariantProperties.MODEL, p_337484_ ? resourcelocation3 : resourcelocation);
                                        case WAITING_FOR_PLAYERS, ACTIVE, WAITING_FOR_REWARD_EJECTION -> Variant.variant()
                                        .with(VariantProperties.MODEL, p_337484_ ? resourcelocation4 : resourcelocation1);
                                        case EJECTING_REWARD -> Variant.variant()
                                        .with(VariantProperties.MODEL, p_337484_ ? resourcelocation5 : resourcelocation2);
                                    };
                                }
                            )
                    )
            );
    }

    private void createVault() {
        Block block = Blocks.VAULT;
        TextureMapping texturemapping = TextureMapping.vault(block, "_front_off", "_side_off", "_top", "_bottom");
        TextureMapping texturemapping1 = TextureMapping.vault(block, "_front_on", "_side_on", "_top", "_bottom");
        TextureMapping texturemapping2 = TextureMapping.vault(block, "_front_ejecting", "_side_on", "_top", "_bottom");
        TextureMapping texturemapping3 = TextureMapping.vault(block, "_front_ejecting", "_side_on", "_top_ejecting", "_bottom");
        ResourceLocation resourcelocation = ModelTemplates.VAULT.create(block, texturemapping, this.modelOutput);
        ResourceLocation resourcelocation1 = ModelTemplates.VAULT.createWithSuffix(block, "_active", texturemapping1, this.modelOutput);
        ResourceLocation resourcelocation2 = ModelTemplates.VAULT.createWithSuffix(block, "_unlocking", texturemapping2, this.modelOutput);
        ResourceLocation resourcelocation3 = ModelTemplates.VAULT.createWithSuffix(block, "_ejecting_reward", texturemapping3, this.modelOutput);
        TextureMapping texturemapping4 = TextureMapping.vault(block, "_front_off_ominous", "_side_off_ominous", "_top_ominous", "_bottom_ominous");
        TextureMapping texturemapping5 = TextureMapping.vault(block, "_front_on_ominous", "_side_on_ominous", "_top_ominous", "_bottom_ominous");
        TextureMapping texturemapping6 = TextureMapping.vault(block, "_front_ejecting_ominous", "_side_on_ominous", "_top_ominous", "_bottom_ominous");
        TextureMapping texturemapping7 = TextureMapping.vault(block, "_front_ejecting_ominous", "_side_on_ominous", "_top_ejecting_ominous", "_bottom_ominous");
        ResourceLocation resourcelocation4 = ModelTemplates.VAULT.createWithSuffix(block, "_ominous", texturemapping4, this.modelOutput);
        ResourceLocation resourcelocation5 = ModelTemplates.VAULT.createWithSuffix(block, "_active_ominous", texturemapping5, this.modelOutput);
        ResourceLocation resourcelocation6 = ModelTemplates.VAULT.createWithSuffix(block, "_unlocking_ominous", texturemapping6, this.modelOutput);
        ResourceLocation resourcelocation7 = ModelTemplates.VAULT.createWithSuffix(block, "_ejecting_reward_ominous", texturemapping7, this.modelOutput);
        this.delegateItemModel(block, resourcelocation);
        this.blockStateOutput
            .accept(
                MultiVariantGenerator.multiVariant(block)
                    .with(createHorizontalFacingDispatch())
                    .with(PropertyDispatch.properties(VaultBlock.STATE, VaultBlock.OMINOUS).generate((p_337475_, p_337476_) -> {
                        return switch (p_337475_) {
                            case INACTIVE -> Variant.variant().with(VariantProperties.MODEL, p_337476_ ? resourcelocation4 : resourcelocation);
                            case ACTIVE -> Variant.variant().with(VariantProperties.MODEL, p_337476_ ? resourcelocation5 : resourcelocation1);
                            case UNLOCKING -> Variant.variant().with(VariantProperties.MODEL, p_337476_ ? resourcelocation6 : resourcelocation2);
                            case EJECTING -> Variant.variant().with(VariantProperties.MODEL, p_337476_ ? resourcelocation7 : resourcelocation3);
                        };
                    }))
            );
    }

    private void createSculkSensor() {
        ResourceLocation resourcelocation = ModelLocationUtils.getModelLocation(Blocks.SCULK_SENSOR, "_inactive");
        ResourceLocation resourcelocation1 = ModelLocationUtils.getModelLocation(Blocks.SCULK_SENSOR, "_active");
        this.delegateItemModel(Blocks.SCULK_SENSOR, resourcelocation);
        this.blockStateOutput
            .accept(
                MultiVariantGenerator.multiVariant(Blocks.SCULK_SENSOR)
                    .with(
                        PropertyDispatch.property(BlockStateProperties.SCULK_SENSOR_PHASE)
                            .generate(
                                p_284650_ -> Variant.variant()
                                        .with(
                                            VariantProperties.MODEL,
                                            p_284650_ != SculkSensorPhase.ACTIVE && p_284650_ != SculkSensorPhase.COOLDOWN
                                                ? resourcelocation
                                                : resourcelocation1
                                        )
                            )
                    )
            );
    }

    private void createCalibratedSculkSensor() {
        ResourceLocation resourcelocation = ModelLocationUtils.getModelLocation(Blocks.CALIBRATED_SCULK_SENSOR, "_inactive");
        ResourceLocation resourcelocation1 = ModelLocationUtils.getModelLocation(Blocks.CALIBRATED_SCULK_SENSOR, "_active");
        this.delegateItemModel(Blocks.CALIBRATED_SCULK_SENSOR, resourcelocation);
        this.blockStateOutput
            .accept(
                MultiVariantGenerator.multiVariant(Blocks.CALIBRATED_SCULK_SENSOR)
                    .with(
                        PropertyDispatch.property(BlockStateProperties.SCULK_SENSOR_PHASE)
                            .generate(
                                p_284647_ -> Variant.variant()
                                        .with(
                                            VariantProperties.MODEL,
                                            p_284647_ != SculkSensorPhase.ACTIVE && p_284647_ != SculkSensorPhase.COOLDOWN
                                                ? resourcelocation
                                                : resourcelocation1
                                        )
                            )
                    )
                    .with(createHorizontalFacingDispatch())
            );
    }

    private void createSculkShrieker() {
        ResourceLocation resourcelocation = ModelTemplates.SCULK_SHRIEKER.create(Blocks.SCULK_SHRIEKER, TextureMapping.sculkShrieker(false), this.modelOutput);
        ResourceLocation resourcelocation1 = ModelTemplates.SCULK_SHRIEKER
            .createWithSuffix(Blocks.SCULK_SHRIEKER, "_can_summon", TextureMapping.sculkShrieker(true), this.modelOutput);
        this.delegateItemModel(Blocks.SCULK_SHRIEKER, resourcelocation);
        this.blockStateOutput
            .accept(
                MultiVariantGenerator.multiVariant(Blocks.SCULK_SHRIEKER)
                    .with(createBooleanModelDispatch(BlockStateProperties.CAN_SUMMON, resourcelocation1, resourcelocation))
            );
    }

    private void createScaffolding() {
        ResourceLocation resourcelocation = ModelLocationUtils.getModelLocation(Blocks.SCAFFOLDING, "_stable");
        ResourceLocation resourcelocation1 = ModelLocationUtils.getModelLocation(Blocks.SCAFFOLDING, "_unstable");
        this.delegateItemModel(Blocks.SCAFFOLDING, resourcelocation);
        this.blockStateOutput
            .accept(
                MultiVariantGenerator.multiVariant(Blocks.SCAFFOLDING)
                    .with(createBooleanModelDispatch(BlockStateProperties.BOTTOM, resourcelocation1, resourcelocation))
            );
    }

    private void createCaveVines() {
        ResourceLocation resourcelocation = this.createSuffixedVariant(Blocks.CAVE_VINES, "", ModelTemplates.CROSS, TextureMapping::cross);
        ResourceLocation resourcelocation1 = this.createSuffixedVariant(Blocks.CAVE_VINES, "_lit", ModelTemplates.CROSS, TextureMapping::cross);
        this.blockStateOutput
            .accept(
                MultiVariantGenerator.multiVariant(Blocks.CAVE_VINES)
                    .with(createBooleanModelDispatch(BlockStateProperties.BERRIES, resourcelocation1, resourcelocation))
            );
        ResourceLocation resourcelocation2 = this.createSuffixedVariant(Blocks.CAVE_VINES_PLANT, "", ModelTemplates.CROSS, TextureMapping::cross);
        ResourceLocation resourcelocation3 = this.createSuffixedVariant(Blocks.CAVE_VINES_PLANT, "_lit", ModelTemplates.CROSS, TextureMapping::cross);
        this.blockStateOutput
            .accept(
                MultiVariantGenerator.multiVariant(Blocks.CAVE_VINES_PLANT)
                    .with(createBooleanModelDispatch(BlockStateProperties.BERRIES, resourcelocation3, resourcelocation2))
            );
    }

    private void createRedstoneLamp() {
        ResourceLocation resourcelocation = TexturedModel.CUBE.create(Blocks.REDSTONE_LAMP, this.modelOutput);
        ResourceLocation resourcelocation1 = this.createSuffixedVariant(Blocks.REDSTONE_LAMP, "_on", ModelTemplates.CUBE_ALL, TextureMapping::cube);
        this.blockStateOutput
            .accept(
                MultiVariantGenerator.multiVariant(Blocks.REDSTONE_LAMP)
                    .with(createBooleanModelDispatch(BlockStateProperties.LIT, resourcelocation1, resourcelocation))
            );
    }

    private void createNormalTorch(Block p_124951_, Block p_124952_) {
        TextureMapping texturemapping = TextureMapping.torch(p_124951_);
        this.blockStateOutput.accept(createSimpleBlock(p_124951_, ModelTemplates.TORCH.create(p_124951_, texturemapping, this.modelOutput)));
        this.blockStateOutput
            .accept(
                MultiVariantGenerator.multiVariant(
                        p_124952_,
                        Variant.variant().with(VariantProperties.MODEL, ModelTemplates.WALL_TORCH.create(p_124952_, texturemapping, this.modelOutput))
                    )
                    .with(createTorchHorizontalDispatch())
            );
        this.createSimpleFlatItemModel(p_124951_);
        this.skipAutoItemBlock(p_124952_);
    }

    private void createRedstoneTorch() {
        TextureMapping texturemapping = TextureMapping.torch(Blocks.REDSTONE_TORCH);
        TextureMapping texturemapping1 = TextureMapping.torch(TextureMapping.getBlockTexture(Blocks.REDSTONE_TORCH, "_off"));
        ResourceLocation resourcelocation = ModelTemplates.TORCH.create(Blocks.REDSTONE_TORCH, texturemapping, this.modelOutput);
        ResourceLocation resourcelocation1 = ModelTemplates.TORCH.createWithSuffix(Blocks.REDSTONE_TORCH, "_off", texturemapping1, this.modelOutput);
        this.blockStateOutput
            .accept(
                MultiVariantGenerator.multiVariant(Blocks.REDSTONE_TORCH)
                    .with(createBooleanModelDispatch(BlockStateProperties.LIT, resourcelocation, resourcelocation1))
            );
        ResourceLocation resourcelocation2 = ModelTemplates.WALL_TORCH.create(Blocks.REDSTONE_WALL_TORCH, texturemapping, this.modelOutput);
        ResourceLocation resourcelocation3 = ModelTemplates.WALL_TORCH.createWithSuffix(Blocks.REDSTONE_WALL_TORCH, "_off", texturemapping1, this.modelOutput);
        this.blockStateOutput
            .accept(
                MultiVariantGenerator.multiVariant(Blocks.REDSTONE_WALL_TORCH)
                    .with(createBooleanModelDispatch(BlockStateProperties.LIT, resourcelocation2, resourcelocation3))
                    .with(createTorchHorizontalDispatch())
            );
        this.createSimpleFlatItemModel(Blocks.REDSTONE_TORCH);
        this.skipAutoItemBlock(Blocks.REDSTONE_WALL_TORCH);
    }

    private void createRepeater() {
        this.createSimpleFlatItemModel(Items.REPEATER);
        this.blockStateOutput
            .accept(
                MultiVariantGenerator.multiVariant(Blocks.REPEATER)
                    .with(
                        PropertyDispatch.properties(BlockStateProperties.DELAY, BlockStateProperties.LOCKED, BlockStateProperties.POWERED)
                            .generate((p_337485_, p_337486_, p_337487_) -> {
                                StringBuilder stringbuilder = new StringBuilder();
                                stringbuilder.append('_').append(p_337485_).append("tick");
                                if (p_337487_) {
                                    stringbuilder.append("_on");
                                }

                                if (p_337486_) {
                                    stringbuilder.append("_locked");
                                }

                                return Variant.variant()
                                    .with(VariantProperties.MODEL, TextureMapping.getBlockTexture(Blocks.REPEATER, stringbuilder.toString()));
                            })
                    )
                    .with(createHorizontalFacingDispatchAlt())
            );
    }

    private void createSeaPickle() {
        this.createSimpleFlatItemModel(Items.SEA_PICKLE);
        this.blockStateOutput
            .accept(
                MultiVariantGenerator.multiVariant(Blocks.SEA_PICKLE)
                    .with(
                        PropertyDispatch.properties(BlockStateProperties.PICKLES, BlockStateProperties.WATERLOGGED)
                            .select(1, false, Arrays.asList(createRotatedVariants(ModelLocationUtils.decorateBlockModelLocation("dead_sea_pickle"))))
                            .select(2, false, Arrays.asList(createRotatedVariants(ModelLocationUtils.decorateBlockModelLocation("two_dead_sea_pickles"))))
                            .select(3, false, Arrays.asList(createRotatedVariants(ModelLocationUtils.decorateBlockModelLocation("three_dead_sea_pickles"))))
                            .select(4, false, Arrays.asList(createRotatedVariants(ModelLocationUtils.decorateBlockModelLocation("four_dead_sea_pickles"))))
                            .select(1, true, Arrays.asList(createRotatedVariants(ModelLocationUtils.decorateBlockModelLocation("sea_pickle"))))
                            .select(2, true, Arrays.asList(createRotatedVariants(ModelLocationUtils.decorateBlockModelLocation("two_sea_pickles"))))
                            .select(3, true, Arrays.asList(createRotatedVariants(ModelLocationUtils.decorateBlockModelLocation("three_sea_pickles"))))
                            .select(4, true, Arrays.asList(createRotatedVariants(ModelLocationUtils.decorateBlockModelLocation("four_sea_pickles"))))
                    )
            );
    }

    private void createSnowBlocks() {
        TextureMapping texturemapping = TextureMapping.cube(Blocks.SNOW);
        ResourceLocation resourcelocation = ModelTemplates.CUBE_ALL.create(Blocks.SNOW_BLOCK, texturemapping, this.modelOutput);
        this.blockStateOutput
            .accept(
                MultiVariantGenerator.multiVariant(Blocks.SNOW)
                    .with(
                        PropertyDispatch.property(BlockStateProperties.LAYERS)
                            .generate(
                                p_176151_ -> Variant.variant()
                                        .with(
                                            VariantProperties.MODEL,
                                            p_176151_ < 8 ? ModelLocationUtils.getModelLocation(Blocks.SNOW, "_height" + p_176151_ * 2) : resourcelocation
                                        )
                            )
                    )
            );
        this.delegateItemModel(Blocks.SNOW, ModelLocationUtils.getModelLocation(Blocks.SNOW, "_height2"));
        this.blockStateOutput.accept(createSimpleBlock(Blocks.SNOW_BLOCK, resourcelocation));
    }

    private void createStonecutter() {
        this.blockStateOutput
            .accept(
                MultiVariantGenerator.multiVariant(
                        Blocks.STONECUTTER, Variant.variant().with(VariantProperties.MODEL, ModelLocationUtils.getModelLocation(Blocks.STONECUTTER))
                    )
                    .with(createHorizontalFacingDispatch())
            );
    }

    private void createStructureBlock() {
        ResourceLocation resourcelocation = TexturedModel.CUBE.create(Blocks.STRUCTURE_BLOCK, this.modelOutput);
        this.delegateItemModel(Blocks.STRUCTURE_BLOCK, resourcelocation);
        this.blockStateOutput
            .accept(
                MultiVariantGenerator.multiVariant(Blocks.STRUCTURE_BLOCK)
                    .with(
                        PropertyDispatch.property(BlockStateProperties.STRUCTUREBLOCK_MODE)
                            .generate(
                                p_176115_ -> Variant.variant()
                                        .with(
                                            VariantProperties.MODEL,
                                            this.createSuffixedVariant(
                                                Blocks.STRUCTURE_BLOCK, "_" + p_176115_.getSerializedName(), ModelTemplates.CUBE_ALL, TextureMapping::cube
                                            )
                                        )
                            )
                    )
            );
    }

    private void createSweetBerryBush() {
        this.createSimpleFlatItemModel(Items.SWEET_BERRIES);
        this.blockStateOutput
            .accept(
                MultiVariantGenerator.multiVariant(Blocks.SWEET_BERRY_BUSH)
                    .with(
                        PropertyDispatch.property(BlockStateProperties.AGE_3)
                            .generate(
                                p_176132_ -> Variant.variant()
                                        .with(
                                            VariantProperties.MODEL,
                                            this.createSuffixedVariant(
                                                Blocks.SWEET_BERRY_BUSH, "_stage" + p_176132_, ModelTemplates.CROSS, TextureMapping::cross
                                            )
                                        )
                            )
                    )
            );
    }

    private void createTripwire() {
        this.createSimpleFlatItemModel(Items.STRING);
        this.blockStateOutput
            .accept(
                MultiVariantGenerator.multiVariant(Blocks.TRIPWIRE)
                    .with(
                        PropertyDispatch.properties(
                                BlockStateProperties.ATTACHED,
                                BlockStateProperties.EAST,
                                BlockStateProperties.NORTH,
                                BlockStateProperties.SOUTH,
                                BlockStateProperties.WEST
                            )
                            .select(
                                false,
                                false,
                                false,
                                false,
                                false,
                                Variant.variant().with(VariantProperties.MODEL, ModelLocationUtils.getModelLocation(Blocks.TRIPWIRE, "_ns"))
                            )
                            .select(
                                false,
                                true,
                                false,
                                false,
                                false,
                                Variant.variant()
                                    .with(VariantProperties.MODEL, ModelLocationUtils.getModelLocation(Blocks.TRIPWIRE, "_n"))
                                    .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90)
                            )
                            .select(
                                false,
                                false,
                                true,
                                false,
                                false,
                                Variant.variant().with(VariantProperties.MODEL, ModelLocationUtils.getModelLocation(Blocks.TRIPWIRE, "_n"))
                            )
                            .select(
                                false,
                                false,
                                false,
                                true,
                                false,
                                Variant.variant()
                                    .with(VariantProperties.MODEL, ModelLocationUtils.getModelLocation(Blocks.TRIPWIRE, "_n"))
                                    .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180)
                            )
                            .select(
                                false,
                                false,
                                false,
                                false,
                                true,
                                Variant.variant()
                                    .with(VariantProperties.MODEL, ModelLocationUtils.getModelLocation(Blocks.TRIPWIRE, "_n"))
                                    .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270)
                            )
                            .select(
                                false,
                                true,
                                true,
                                false,
                                false,
                                Variant.variant().with(VariantProperties.MODEL, ModelLocationUtils.getModelLocation(Blocks.TRIPWIRE, "_ne"))
                            )
                            .select(
                                false,
                                true,
                                false,
                                true,
                                false,
                                Variant.variant()
                                    .with(VariantProperties.MODEL, ModelLocationUtils.getModelLocation(Blocks.TRIPWIRE, "_ne"))
                                    .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90)
                            )
                            .select(
                                false,
                                false,
                                false,
                                true,
                                true,
                                Variant.variant()
                                    .with(VariantProperties.MODEL, ModelLocationUtils.getModelLocation(Blocks.TRIPWIRE, "_ne"))
                                    .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180)
                            )
                            .select(
                                false,
                                false,
                                true,
                                false,
                                true,
                                Variant.variant()
                                    .with(VariantProperties.MODEL, ModelLocationUtils.getModelLocation(Blocks.TRIPWIRE, "_ne"))
                                    .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270)
                            )
                            .select(
                                false,
                                false,
                                true,
                                true,
                                false,
                                Variant.variant().with(VariantProperties.MODEL, ModelLocationUtils.getModelLocation(Blocks.TRIPWIRE, "_ns"))
                            )
                            .select(
                                false,
                                true,
                                false,
                                false,
                                true,
                                Variant.variant()
                                    .with(VariantProperties.MODEL, ModelLocationUtils.getModelLocation(Blocks.TRIPWIRE, "_ns"))
                                    .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90)
                            )
                            .select(
                                false,
                                true,
                                true,
                                true,
                                false,
                                Variant.variant().with(VariantProperties.MODEL, ModelLocationUtils.getModelLocation(Blocks.TRIPWIRE, "_nse"))
                            )
                            .select(
                                false,
                                true,
                                false,
                                true,
                                true,
                                Variant.variant()
                                    .with(VariantProperties.MODEL, ModelLocationUtils.getModelLocation(Blocks.TRIPWIRE, "_nse"))
                                    .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90)
                            )
                            .select(
                                false,
                                false,
                                true,
                                true,
                                true,
                                Variant.variant()
                                    .with(VariantProperties.MODEL, ModelLocationUtils.getModelLocation(Blocks.TRIPWIRE, "_nse"))
                                    .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180)
                            )
                            .select(
                                false,
                                true,
                                true,
                                false,
                                true,
                                Variant.variant()
                                    .with(VariantProperties.MODEL, ModelLocationUtils.getModelLocation(Blocks.TRIPWIRE, "_nse"))
                                    .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270)
                            )
                            .select(
                                false,
                                true,
                                true,
                                true,
                                true,
                                Variant.variant().with(VariantProperties.MODEL, ModelLocationUtils.getModelLocation(Blocks.TRIPWIRE, "_nsew"))
                            )
                            .select(
                                true,
                                false,
                                false,
                                false,
                                false,
                                Variant.variant().with(VariantProperties.MODEL, ModelLocationUtils.getModelLocation(Blocks.TRIPWIRE, "_attached_ns"))
                            )
                            .select(
                                true,
                                false,
                                true,
                                false,
                                false,
                                Variant.variant().with(VariantProperties.MODEL, ModelLocationUtils.getModelLocation(Blocks.TRIPWIRE, "_attached_n"))
                            )
                            .select(
                                true,
                                false,
                                false,
                                true,
                                false,
                                Variant.variant()
                                    .with(VariantProperties.MODEL, ModelLocationUtils.getModelLocation(Blocks.TRIPWIRE, "_attached_n"))
                                    .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180)
                            )
                            .select(
                                true,
                                true,
                                false,
                                false,
                                false,
                                Variant.variant()
                                    .with(VariantProperties.MODEL, ModelLocationUtils.getModelLocation(Blocks.TRIPWIRE, "_attached_n"))
                                    .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90)
                            )
                            .select(
                                true,
                                false,
                                false,
                                false,
                                true,
                                Variant.variant()
                                    .with(VariantProperties.MODEL, ModelLocationUtils.getModelLocation(Blocks.TRIPWIRE, "_attached_n"))
                                    .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270)
                            )
                            .select(
                                true,
                                true,
                                true,
                                false,
                                false,
                                Variant.variant().with(VariantProperties.MODEL, ModelLocationUtils.getModelLocation(Blocks.TRIPWIRE, "_attached_ne"))
                            )
                            .select(
                                true,
                                true,
                                false,
                                true,
                                false,
                                Variant.variant()
                                    .with(VariantProperties.MODEL, ModelLocationUtils.getModelLocation(Blocks.TRIPWIRE, "_attached_ne"))
                                    .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90)
                            )
                            .select(
                                true,
                                false,
                                false,
                                true,
                                true,
                                Variant.variant()
                                    .with(VariantProperties.MODEL, ModelLocationUtils.getModelLocation(Blocks.TRIPWIRE, "_attached_ne"))
                                    .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180)
                            )
                            .select(
                                true,
                                false,
                                true,
                                false,
                                true,
                                Variant.variant()
                                    .with(VariantProperties.MODEL, ModelLocationUtils.getModelLocation(Blocks.TRIPWIRE, "_attached_ne"))
                                    .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270)
                            )
                            .select(
                                true,
                                false,
                                true,
                                true,
                                false,
                                Variant.variant().with(VariantProperties.MODEL, ModelLocationUtils.getModelLocation(Blocks.TRIPWIRE, "_attached_ns"))
                            )
                            .select(
                                true,
                                true,
                                false,
                                false,
                                true,
                                Variant.variant()
                                    .with(VariantProperties.MODEL, ModelLocationUtils.getModelLocation(Blocks.TRIPWIRE, "_attached_ns"))
                                    .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90)
                            )
                            .select(
                                true,
                                true,
                                true,
                                true,
                                false,
                                Variant.variant().with(VariantProperties.MODEL, ModelLocationUtils.getModelLocation(Blocks.TRIPWIRE, "_attached_nse"))
                            )
                            .select(
                                true,
                                true,
                                false,
                                true,
                                true,
                                Variant.variant()
                                    .with(VariantProperties.MODEL, ModelLocationUtils.getModelLocation(Blocks.TRIPWIRE, "_attached_nse"))
                                    .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90)
                            )
                            .select(
                                true,
                                false,
                                true,
                                true,
                                true,
                                Variant.variant()
                                    .with(VariantProperties.MODEL, ModelLocationUtils.getModelLocation(Blocks.TRIPWIRE, "_attached_nse"))
                                    .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180)
                            )
                            .select(
                                true,
                                true,
                                true,
                                false,
                                true,
                                Variant.variant()
                                    .with(VariantProperties.MODEL, ModelLocationUtils.getModelLocation(Blocks.TRIPWIRE, "_attached_nse"))
                                    .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270)
                            )
                            .select(
                                true,
                                true,
                                true,
                                true,
                                true,
                                Variant.variant().with(VariantProperties.MODEL, ModelLocationUtils.getModelLocation(Blocks.TRIPWIRE, "_attached_nsew"))
                            )
                    )
            );
    }

    private void createTripwireHook() {
        this.createSimpleFlatItemModel(Blocks.TRIPWIRE_HOOK);
        this.blockStateOutput
            .accept(
                MultiVariantGenerator.multiVariant(Blocks.TRIPWIRE_HOOK)
                    .with(
                        PropertyDispatch.properties(BlockStateProperties.ATTACHED, BlockStateProperties.POWERED)
                            .generate(
                                (p_176124_, p_176125_) -> Variant.variant()
                                        .with(
                                            VariantProperties.MODEL,
                                            TextureMapping.getBlockTexture(Blocks.TRIPWIRE_HOOK, (p_176124_ ? "_attached" : "") + (p_176125_ ? "_on" : ""))
                                        )
                            )
                    )
                    .with(createHorizontalFacingDispatch())
            );
    }

    private ResourceLocation createTurtleEggModel(int p_124514_, String p_124515_, TextureMapping p_124516_) {
        switch (p_124514_) {
            case 1:
                return ModelTemplates.TURTLE_EGG.create(ModelLocationUtils.decorateBlockModelLocation(p_124515_ + "turtle_egg"), p_124516_, this.modelOutput);
            case 2:
                return ModelTemplates.TWO_TURTLE_EGGS
                    .create(ModelLocationUtils.decorateBlockModelLocation("two_" + p_124515_ + "turtle_eggs"), p_124516_, this.modelOutput);
            case 3:
                return ModelTemplates.THREE_TURTLE_EGGS
                    .create(ModelLocationUtils.decorateBlockModelLocation("three_" + p_124515_ + "turtle_eggs"), p_124516_, this.modelOutput);
            case 4:
                return ModelTemplates.FOUR_TURTLE_EGGS
                    .create(ModelLocationUtils.decorateBlockModelLocation("four_" + p_124515_ + "turtle_eggs"), p_124516_, this.modelOutput);
            default:
                throw new UnsupportedOperationException();
        }
    }

    private ResourceLocation createTurtleEggModel(Integer p_124677_, Integer p_124678_) {
        switch (p_124678_) {
            case 0:
                return this.createTurtleEggModel(p_124677_, "", TextureMapping.cube(TextureMapping.getBlockTexture(Blocks.TURTLE_EGG)));
            case 1:
                return this.createTurtleEggModel(
                    p_124677_, "slightly_cracked_", TextureMapping.cube(TextureMapping.getBlockTexture(Blocks.TURTLE_EGG, "_slightly_cracked"))
                );
            case 2:
                return this.createTurtleEggModel(
                    p_124677_, "very_cracked_", TextureMapping.cube(TextureMapping.getBlockTexture(Blocks.TURTLE_EGG, "_very_cracked"))
                );
            default:
                throw new UnsupportedOperationException();
        }
    }

    private void createTurtleEgg() {
        this.createSimpleFlatItemModel(Items.TURTLE_EGG);
        this.blockStateOutput
            .accept(
                MultiVariantGenerator.multiVariant(Blocks.TURTLE_EGG)
                    .with(
                        PropertyDispatch.properties(BlockStateProperties.EGGS, BlockStateProperties.HATCH)
                            .generateList((p_176185_, p_176186_) -> Arrays.asList(createRotatedVariants(this.createTurtleEggModel(p_176185_, p_176186_))))
                    )
            );
    }

    private void createSnifferEgg() {
        this.createSimpleFlatItemModel(Items.SNIFFER_EGG);
        Function<Integer, ResourceLocation> function = p_278206_ -> {
            String s = switch (p_278206_) {
                case 1 -> "_slightly_cracked";
                case 2 -> "_very_cracked";
                default -> "_not_cracked";
            };
            TextureMapping texturemapping = TextureMapping.snifferEgg(s);
            return ModelTemplates.SNIFFER_EGG.createWithSuffix(Blocks.SNIFFER_EGG, s, texturemapping, this.modelOutput);
        };
        this.blockStateOutput
            .accept(
                MultiVariantGenerator.multiVariant(Blocks.SNIFFER_EGG)
                    .with(
                        PropertyDispatch.property(SnifferEggBlock.HATCH)
                            .generate(p_277261_ -> Variant.variant().with(VariantProperties.MODEL, function.apply(p_277261_)))
                    )
            );
    }

    private void createMultiface(Block p_176086_) {
        this.createSimpleFlatItemModel(p_176086_);
        ResourceLocation resourcelocation = ModelLocationUtils.getModelLocation(p_176086_);
        MultiPartGenerator multipartgenerator = MultiPartGenerator.multiPart(p_176086_);
        Condition.TerminalCondition condition$terminalcondition = Util.make(
            Condition.condition(), p_236295_ -> MULTIFACE_GENERATOR.stream().map(Pair::getFirst).forEach(p_236299_ -> {
                    if (p_176086_.defaultBlockState().hasProperty(p_236299_)) {
                        p_236295_.term(p_236299_, false);
                    }
                })
        );

        for (Pair<BooleanProperty, Function<ResourceLocation, Variant>> pair : MU