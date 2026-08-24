/*
 * This file is part of Applied Energistics 2.
 * Copyright (c) 2013 - 2014, AlgorithmX2, All rights reserved.
 *
 * Applied Energistics 2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Applied Energistics 2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Applied Energistics 2.  If not, see <http://www.gnu.org/licenses/lgpl>.
 */

package com.example.iknow.block;

import java.util.EnumSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import appeng.api.config.Actionable;
import appeng.api.config.InscriberInputCapacity;
import appeng.api.config.PowerMultiplier;
import appeng.api.config.Setting;
import appeng.api.config.Settings;
import appeng.api.config.YesNo;
import appeng.api.implementations.blockentities.ICrankable;
import appeng.api.inventories.ISegmentedInventory;
import appeng.api.inventories.InternalInventory;
import appeng.api.networking.IGridNode;
import appeng.api.networking.energy.IEnergyService;
import appeng.api.networking.energy.IEnergySource;
import appeng.api.networking.ticking.IGridTickable;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.networking.ticking.TickingRequest;
import appeng.api.orientation.BlockOrientation;
import appeng.api.orientation.RelativeSide;
import appeng.api.upgrades.IUpgradeInventory;
import appeng.api.upgrades.IUpgradeableObject;
import appeng.api.upgrades.UpgradeInventories;
import appeng.api.util.AECableType;
import appeng.api.util.IConfigManager;
import appeng.api.util.IConfigurableObject;
import appeng.blockentity.grid.AENetworkedPoweredBlockEntity;
import appeng.blockentity.misc.InscriberRecipes;
import appeng.core.definitions.AEBlocks;
import appeng.core.definitions.AEItems;
import appeng.core.settings.TickRates;
import appeng.recipes.handlers.InscriberProcessType;
import appeng.recipes.handlers.InscriberRecipe;
import appeng.util.inv.AppEngInternalInventory;
import appeng.util.inv.CombinedInternalInventory;
import appeng.util.inv.FilteredInternalInventory;
import appeng.util.inv.filter.IAEItemFilter;

/**
 * @author AlgorithmX2
 * @author thatsIch
 * @version rv2
 * @since rv0
 */
public class InfinityOutputterBlockEntity extends AENetworkedPoweredBlockEntity
        implements IGridTickable, IUpgradeableObject, IConfigurableObject {
    private static final int MAX_PROCESSING_STEPS = 200;

    private final IUpgradeInventory upgrades;
    private final IConfigManager configManager;
    private int processingTime = 0;
    // cycles from 0 - 16, at 8 it preforms the action, at 16 it re-enables the
    // normal routine.
    private boolean smash;
    /**
     * Purely visual on the client-side.
     */
    private boolean repeatSmash;
    private int finalStep;
    private long clientStart;

    // Internally visible inventories
    private final IAEItemFilter baseFilter = new BaseFilter();
    private final AppEngInternalInventory topItemHandler = new AppEngInternalInventory(this, 1, 64, baseFilter);
    private final AppEngInternalInventory bottomItemHandler = new AppEngInternalInventory(this, 1, 64, baseFilter);
    private final AppEngInternalInventory sideItemHandler = new AppEngInternalInventory(this, 2,
            Integer.MAX_VALUE, baseFilter);
    // Combined internally visible inventories
    private final InternalInventory inv = new CombinedInternalInventory(this.topItemHandler,
            this.bottomItemHandler, this.sideItemHandler);

    // "Hack" to see if active recipe changed.
    private final Map<InternalInventory, ItemStack> lastStacks = new IdentityHashMap<>(Map.of(
            topItemHandler, ItemStack.EMPTY, bottomItemHandler, ItemStack.EMPTY,
            sideItemHandler, ItemStack.EMPTY));

    // The externally visible inventories (with filters applied)
    private final InternalInventory topItemHandlerExtern;
    private final InternalInventory bottomItemHandlerExtern;
    private final InternalInventory sideItemHandlerExtern;
    // Combined externally visible inventories
    private final InternalInventory combinedItemHandlerExtern;

    private InscriberRecipe cachedTask = null;

    /** 输出面（泵送方向），默认向下 */
    private Direction outputFacing = Direction.DOWN;

    public InfinityOutputterBlockEntity(BlockEntityType<?> blockEntityType, BlockPos pos, BlockState blockState) {
        super(blockEntityType, pos, blockState);

        this.getMainNode()
                .setIdlePowerUsage(0)
                .addService(IGridTickable.class, this);
        this.setInternalMaxPower(1600);

        this.upgrades = UpgradeInventories.forMachine(AEBlocks.INSCRIBER, 4, this::saveChanges);
        this.configManager = IConfigManager.builder(this::onConfigChanged)
                .registerSetting(Settings.INSCRIBER_SEPARATE_SIDES, YesNo.NO)
                .registerSetting(Settings.AUTO_EXPORT, YesNo.NO)
                .registerSetting(Settings.INSCRIBER_INPUT_CAPACITY, InscriberInputCapacity.SIXTY_FOUR)
                .build();

        var automationFilter = new AutomationFilter();
        this.topItemHandlerExtern = new FilteredInternalInventory(this.topItemHandler, automationFilter);
        this.bottomItemHandlerExtern = new FilteredInternalInventory(this.bottomItemHandler, automationFilter);
        this.sideItemHandlerExtern = new FilteredInternalInventory(this.sideItemHandler, automationFilter);

        this.combinedItemHandlerExtern = new CombinedInternalInventory(topItemHandlerExtern, bottomItemHandlerExtern,
                sideItemHandlerExtern);

        this.setPowerSides(getGridConnectableSides(getOrientation()));
    }

    @Override
    public AECableType getCableConnectionType(Direction dir) {
        return AECableType.COVERED;
    }

    @Override
    public void onReady() {
        super.onReady();
        com.example.iknow.IknowMod.LOGGER.info("[InfinityOutputter] onReady at {} sides={}",
                worldPosition, getGridConnectableSides(getOrientation()));
    }

    @Override
    public void saveAdditional(CompoundTag data, HolderLookup.Provider registries) {
        super.saveAdditional(data, registries);
        this.upgrades.writeToNBT(data, "upgrades", registries);
        this.configManager.writeToNBT(data, registries);
        data.putInt("output_facing", outputFacing.get3DDataValue());
    }

    @Override
    public void loadTag(CompoundTag data, HolderLookup.Provider registries) {
        super.loadTag(data, registries);
        this.upgrades.readFromNBT(data, "upgrades", registries);
        this.configManager.readFromNBT(data, registries);
        // TODO 1.22: Remove compat with old format.
        if ("NO".equals(data.getString("inscriber_buffer_size"))) {
            this.configManager.putSetting(Settings.INSCRIBER_INPUT_CAPACITY, InscriberInputCapacity.FOUR);
        }

        // Update stack tracker
        lastStacks.put(topItemHandler, topItemHandler.getStackInSlot(0));
        lastStacks.put(bottomItemHandler, bottomItemHandler.getStackInSlot(0));
        lastStacks.put(sideItemHandler, sideItemHandler.getStackInSlot(0));

        if (data.contains("output_facing")) {
            outputFacing = Direction.from3DDataValue(data.getInt("output_facing"));
        }
    }

    @Override
    protected boolean readFromStream(RegistryFriendlyByteBuf data) {
        var c = super.readFromStream(data);

        var oldSmash = isSmash();
        var newSmash = data.readBoolean();

        if (oldSmash != newSmash && newSmash) {
            setSmash(true);
        }

        for (int i = 0; i < this.inv.size(); i++) {
            this.inv.setItemDirect(i, ItemStack.OPTIONAL_STREAM_CODEC.decode(data));
        }
        this.cachedTask = null;

        return c;
    }

    @Override
    protected void writeToStream(RegistryFriendlyByteBuf data) {
        super.writeToStream(data);

        data.writeBoolean(isSmash());
        for (int i = 0; i < this.inv.size(); i++) {
            ItemStack.OPTIONAL_STREAM_CODEC.encode(data, inv.getStackInSlot(i));
        }
    }

    @Override
    protected void saveVisualState(CompoundTag data) {
        super.saveVisualState(data);

        data.putBoolean("smash", isSmash());
    }

    @Override
    protected void loadVisualState(CompoundTag data) {
        super.loadVisualState(data);

        setSmash(data.getBoolean("smash"));
    }

    @Override
    public Set<Direction> getGridConnectableSides(BlockOrientation orientation) {
        // 全部 6 面均可连接 AE 线缆（原压印器除外前沿，这里放宽以便任意面接线）
        return EnumSet.allOf(Direction.class);
    }

    @Override
    protected void onOrientationChanged(BlockOrientation orientation) {
        super.onOrientationChanged(orientation);

        this.setPowerSides(getGridConnectableSides(orientation));
    }

    @Override
    public void addAdditionalDrops(Level level, BlockPos pos, List<ItemStack> drops) {
        super.addAdditionalDrops(level, pos, drops);

        for (var upgrade : upgrades) {
            drops.add(upgrade);
        }
    }

    @Override
    public void clearContent() {
        super.clearContent();
        upgrades.clear();
    }

    @Override
    public InternalInventory getInternalInventory() {
        return this.inv;
    }

    @Override
    public void onChangeInventory(AppEngInternalInventory inv, int slot) {
        if (slot == 0) {
            boolean sameItemSameTags = ItemStack.isSameItemSameComponents(inv.getStackInSlot(0), lastStacks.get(inv));
            lastStacks.put(inv, inv.getStackInSlot(0).copy());
            if (sameItemSameTags) {
                return; // Don't care if it's just a count change
            }

            // Reset recipe
            this.setProcessingTime(0);
            this.cachedTask = null;
        }

        // Update displayed stacks on the client
        if (!this.isSmash()) {
            this.markForUpdate();
        }

        getMainNode().ifPresent((grid, node) -> grid.getTickManager().wakeDevice(node));
    }

    //
    // @Override
    @Override
    public TickingRequest getTickingRequest(IGridNode node) {
        return new TickingRequest(TickRates.Inscriber, !hasAutoExportWork() && !this.hasCraftWork());
    }

    private boolean hasAutoExportWork() {
        return !this.sideItemHandler.getStackInSlot(1).isEmpty()
                && configManager.getSetting(Settings.AUTO_EXPORT) == YesNo.YES;
    }

    private boolean hasCraftWork() {
        var task = this.getTask();
        if (task != null) {
            // Only process if the result would fit.
            return sideItemHandler.insertItem(1, task.getResultItem().copy(), true).isEmpty();
        }

        this.setProcessingTime(0);
        return this.isSmash();
    }

    @Nullable
    public InscriberRecipe getTask() {
        if (this.cachedTask == null && level != null) {
            ItemStack input = this.sideItemHandler.getStackInSlot(0);
            ItemStack plateA = this.topItemHandler.getStackInSlot(0);
            ItemStack plateB = this.bottomItemHandler.getStackInSlot(0);
            if (input.isEmpty()) {
                return null; // No input to handle
            }
            // 无限元件特判：读其代表的物品，动态合成配方（无上下印板，不耗元件，产出超大数量）
            ItemStack cellItem = readCellOutput(input);
            if (!cellItem.isEmpty()) {
                ItemStack huge = cellItem.copy();
                huge.setCount(Integer.MAX_VALUE);
                this.cachedTask = new InscriberRecipe(
                        Ingredient.of(cellItem),
                        huge,
                        Ingredient.EMPTY, Ingredient.EMPTY,
                        InscriberProcessType.INSCRIBE);
                return this.cachedTask;
            }
            this.cachedTask = InscriberRecipes.findRecipe(level, input, plateA, plateB, true);
        }
        return this.cachedTask;
    }

    /** 软依赖读取 ExtendedAE 无限元件代表物品（反射 getRecord().wrapForDisplayOrFilter()），失败返回空 */
    private static ItemStack readCellOutput(ItemStack cell) {
        if (cell.isEmpty()) {
            return ItemStack.EMPTY;
        }
        try {
            Class<?> cls = Class.forName("com.glodblock.github.extendedae.common.items.ItemInfinityCell");
            if (cls.isInstance(cell.getItem())) {
                Object record = cls.getMethod("getRecord").invoke(cell.getItem());
                if (record != null) {
                    Object stack = record.getClass().getMethod("wrapForDisplayOrFilter").invoke(record);
                    if (stack instanceof ItemStack is && !is.isEmpty()) {
                        return is.copy();
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return ItemStack.EMPTY;
    }

    /** 当前原料槽是否为无限元件（代表物品非空即视为无限元件） */
    private boolean isInfinityCellInput() {
        return !readCellOutput(this.sideItemHandler.getStackInSlot(0)).isEmpty();
    }

    /** 软依赖判断指定物品是否为 ExtendedAE 无限元件（反射），失败返回 false */
    private static boolean isInfinityCell(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        try {
            Class<?> cls = Class.forName("com.glodblock.github.extendedae.common.items.ItemInfinityCell");
            return cls.isInstance(stack.getItem());
        } catch (Exception ignored) {
            return false;
        }
    }

    /** 无限元件配方约 20 tick 完成一次（默认速度卡速度因子 2，8/2 + 16 ≈ 20） */
    public int getMaxProcessingTime() {
        if (isInfinityCellInput()) {
            return 8;
        }
        return this.MAX_PROCESSING_STEPS;
    }

    /** 无限元件：直接泵送代表物品到邻近容器（填满每个槽位），不消耗元件。依赖方块服务端 ticker 调用，不依赖网格 */
    private boolean pumpInfinityCell() {
        ItemStack cell = this.sideItemHandler.getStackInSlot(0);
        if (cell.isEmpty()) {
            return false;
        }
        ItemStack out = readCellOutput(cell);
        if (out.isEmpty()) {
            return false;
        }
        if (level.getGameTime() % 100 == 0) {
            com.example.iknow.IknowMod.LOGGER.info(
                    "[InfinityOutputter] pump: cell={} out={} nbt={}", cell.getHoverName(), out.getHoverName(), out);
        }
        boolean any = false;
        // 只向配置的输出面（outputFacing）泵送
        var target = InternalInventory.wrapExternal(level, worldPosition.relative(outputFacing),
                outputFacing.getOpposite());
        if (target != null) {
            // 一次尽可能多塞（填满目标容器所有可容纳槽位），addItems 会自动分配到各槽
            ItemStack toInsert = out.copy();
            toInsert.setCount(Integer.MAX_VALUE);
            ItemStack leftover = target.addItems(toInsert);
            if (leftover.getCount() < toInsert.getCount()) {
                any = true;
            }
        }
        this.saveChanges();
        return any;
    }

    /** 方块服务端 tick：有无限元件就泵送（不需要网格激活/供电） */
    public static void serverTick(net.minecraft.world.level.Level level, BlockPos pos, BlockState state,
            InfinityOutputterBlockEntity be) {
        if (level.isClientSide()) {
            return;
        }
        be.pumpInfinityCell();
    }

    public Direction getOutputFacing() {
        return outputFacing;
    }

    public void setOutputFacing(Direction dir) {
        this.outputFacing = dir;
    }

    /** 循环切换输出面（覆盖上下及四水平方向，因为 getClockWise 仅对水平有效） */
    public void cycleOutputFacing() {
        int next = (outputFacing.ordinal() + 1) % Direction.values().length;
        outputFacing = Direction.values()[next];
    }

    @Override
    public TickRateModulation tickingRequest(IGridNode node, int ticksSinceLastCall) {
        // 节流日志：确认是否被网格 tick、中间槽是什么
        if (level.getGameTime() % 100 == 0) {
            com.example.iknow.IknowMod.LOGGER.info(
                    "[InfinityOutputter] tick: slot0={} isInf={} out={} power={}",
                    sideItemHandler.getStackInSlot(0), isInfinityCellInput(),
                    isInfinityCellInput() ? readCellOutput(sideItemHandler.getStackInSlot(0)) : ItemStack.EMPTY,
                    getInternalCurrentPower());
        }
        // 无限元件：由方块服务端 ticker 泵送（不依赖网格 tick），这里跳过压印逻辑
        if (isInfinityCellInput()) {
            return TickRateModulation.SLEEP;
        }
        if (this.isSmash()) {
            this.finalStep++;
            if (this.finalStep == 8) {
                final InscriberRecipe out = this.getTask();
                if (out != null) {
                    final ItemStack outputCopy = out.getResultItem().copy();

                    if (this.sideItemHandler.insertItem(1, outputCopy, false).isEmpty()) {
                        this.setProcessingTime(0);
                        if (out.getProcessType() == InscriberProcessType.PRESS) {
                            this.topItemHandler.extractItem(0, 1, false);
                            this.bottomItemHandler.extractItem(0, 1, false);
                        }
                        // 无限元件不消耗：原料始终保留，反复产出其代表物品
                        if (!isInfinityCellInput()) {
                            this.sideItemHandler.extractItem(0, 1, false);
                        }
                    }
                }
                this.saveChanges();
            } else if (this.finalStep == 16) {
                this.finalStep = 0;
                this.setSmash(false);
                this.markForUpdate();
            }
        } else if (this.hasCraftWork()) {
            getMainNode().ifPresent(grid -> {
                IEnergyService eg = grid.getEnergyService();
                IEnergySource src = this;

                // Note: required ticks = 16 + ceil(MAX_PROCESSING_STEPS / speedFactor)
                final int speedFactor = switch (this.upgrades.getInstalledUpgrades(AEItems.SPEED_CARD)) {
                    default -> 2; // 116 ticks
                    case 1 -> 3; // 83 ticks
                    case 2 -> 5; // 56 ticks
                    case 3 -> 10; // 36 ticks
                    case 4 -> 50; // 20 ticks
                };
                final int powerConsumption = 10 * speedFactor;
                final double powerThreshold = powerConsumption - 0.01;
                double powerReq = this.extractAEPower(powerConsumption, Actionable.SIMULATE, PowerMultiplier.CONFIG);

                if (powerReq <= powerThreshold) {
                    src = eg;
                    powerReq = eg.extractAEPower(powerConsumption, Actionable.SIMULATE, PowerMultiplier.CONFIG);
                }

                if (powerReq > powerThreshold) {
                    src.extractAEPower(powerConsumption, Actionable.MODULATE, PowerMultiplier.CONFIG);
                    this.setProcessingTime(this.getProcessingTime() + speedFactor);
                }
            });

            if (this.getProcessingTime() > this.getMaxProcessingTime()) {
                this.setProcessingTime(this.getMaxProcessingTime());
                final InscriberRecipe out = this.getTask();
                if (out != null) {
                    final ItemStack outputCopy = out.getResultItem().copy();
                    if (this.sideItemHandler.insertItem(1, outputCopy, true).isEmpty()) {
                        this.setSmash(true);
                        this.finalStep = 0;
                        this.markForUpdate();
                    }
                }
            }
        }

        if (this.pushOutResult()) {
            return TickRateModulation.URGENT;
        }

        return this.hasCraftWork() ? TickRateModulation.URGENT
                : this.hasAutoExportWork() ? TickRateModulation.SLOWER : TickRateModulation.SLEEP;
    }

    /**
     * @return true if something was pushed, false otherwise
     */
    private boolean pushOutResult() {
        if (!this.hasAutoExportWork()) {
            return false;
        }

        var pushSides = EnumSet.allOf(Direction.class);
        if (isSeparateSides()) {
            pushSides.remove(this.getTop());
            pushSides.remove(this.getTop().getOpposite());
        }

        for (var dir : pushSides) {
            var target = InternalInventory.wrapExternal(level, getBlockPos().relative(dir), dir.getOpposite());

            if (target != null) {
                int startItems = this.sideItemHandler.getStackInSlot(1).getCount();
                this.sideItemHandler.insertItem(1, target.addItems(this.sideItemHandler.extractItem(1, 64, false)),
                        false);
                int endItems = this.sideItemHandler.getStackInSlot(1).getCount();

                if (startItems != endItems) {
                    return true;
                }
            }
        }

        return false;
    }

    @Nullable
    @Override
    public InternalInventory getSubInventory(ResourceLocation id) {
        if (id.equals(ISegmentedInventory.STORAGE)) {
            return this.getInternalInventory();
        } else if (id.equals(ISegmentedInventory.UPGRADES)) {
            return this.upgrades;
        }

        return super.getSubInventory(id);
    }

    private boolean isSeparateSides() {
        return this.configManager.getSetting(Settings.INSCRIBER_SEPARATE_SIDES) == YesNo.YES;
    }

    @Override
    protected InternalInventory getExposedInventoryForSide(Direction facing) {
        if (isSeparateSides()) {
            if (facing == this.getTop()) {
                return this.topItemHandlerExtern;
            } else if (facing == this.getTop().getOpposite()) {
                return this.bottomItemHandlerExtern;
            } else {
                return this.sideItemHandlerExtern;
            }
        } else {
            return this.combinedItemHandlerExtern;
        }
    }

    @Override
    public IUpgradeInventory getUpgrades() {
        return upgrades;
    }

    @Override
    public IConfigManager getConfigManager() {
        return configManager;
    }

    private void onConfigChanged(IConfigManager manager, Setting<?> setting) {
        if (setting == Settings.AUTO_EXPORT) {
            getMainNode().ifPresent((grid, node) -> grid.getTickManager().wakeDevice(node));
        }

        if (setting == Settings.INSCRIBER_SEPARATE_SIDES) {
            // Our exposed inventory changed, invalidate caps!
            invalidateCapabilities();
        }

        if (setting == Settings.INSCRIBER_INPUT_CAPACITY) {
            var capacity = configManager.getSetting(Settings.INSCRIBER_INPUT_CAPACITY).capacity;
            topItemHandler.setMaxStackSize(0, capacity);
            sideItemHandler.setMaxStackSize(0, capacity);
            bottomItemHandler.setMaxStackSize(0, capacity);
        }

        saveChanges();
    }

    public long getClientStart() {
        return this.clientStart;
    }

    private void setClientStart(long clientStart) {
        this.clientStart = clientStart;
    }

    public boolean isSmash() {
        return this.smash;
    }

    public void setSmash(boolean smash) {
        if (smash && !this.smash) {
            setClientStart(System.currentTimeMillis());
        }
        this.smash = smash;
    }

    public boolean isRepeatSmash() {
        return repeatSmash;
    }

    public void setRepeatSmash(boolean repeatSmash) {
        this.repeatSmash = repeatSmash;
    }

    public int getProcessingTime() {
        return this.processingTime;
    }

    private void setProcessingTime(int processingTime) {
        this.processingTime = processingTime;
    }

    /**
     * Allow cranking from any side other than the front.
     */
    @Nullable
    public ICrankable getCrankable(Direction direction) {
        if (direction != getFront()) {
            return new Crankable();
        }
        return null;
    }

    public class BaseFilter implements IAEItemFilter {
        @Override
        public boolean allowInsert(InternalInventory inv, int slot, ItemStack stack) {
            // output slot
            if (slot == 1) {
                // slots and automation prevent insertion into the output,
                // we need it here for the inscriber's own internal logic
                return true;
            }

            // 无限元件可直接放入中间（输入）槽
            if (inv == sideItemHandler && slot == 0 && isInfinityCell(stack)) {
                return true;
            }

            // always allow name press
            if (inv == topItemHandler || inv == bottomItemHandler) {
                if (AEItems.NAME_PRESS.is(stack)) {
                    return true;
                }
            }

            if (inv == sideItemHandler && (AEItems.NAME_PRESS.is(topItemHandler.getStackInSlot(0))
                    || AEItems.NAME_PRESS.is(bottomItemHandler.getStackInSlot(0)))) {
                // can always rename anything
                return true;
            }

            // only allow if is a proper recipe match
            ItemStack bot = bottomItemHandler.getStackInSlot(0);
            ItemStack middle = sideItemHandler.getStackInSlot(0);
            ItemStack top = topItemHandler.getStackInSlot(0);

            if (inv == bottomItemHandler)
                bot = stack;
            if (inv == sideItemHandler)
                middle = stack;
            if (inv == topItemHandler)
                top = stack;

            for (var holder : InscriberRecipes.getRecipes(level)) {
                var recipe = holder.value();
                if (!middle.isEmpty() && !recipe.getMiddleInput().test(middle)) {
                    continue;
                }

                if (bot.isEmpty() && top.isEmpty()) {
                    return true;
                } else if (bot.isEmpty()) {
                    if (recipe.getTopOptional().test(top) || recipe.getBottomOptional().test(top)) {
                        return true;
                    }
                } else if (top.isEmpty()) {
                    if (recipe.getBottomOptional().test(bot) || recipe.getTopOptional().test(bot)) {
                        return true;
                    }
                } else {
                    if ((recipe.getTopOptional().test(top) && recipe.getBottomOptional().test(bot))
                            || (recipe.getBottomOptional().test(top) && recipe.getTopOptional().test(bot))) {
                        return true;
                    }
                }
            }
            return false;
        }
    }

    public class AutomationFilter implements IAEItemFilter {
        @Override
        public boolean allowExtract(InternalInventory inv, int slot, int amount) {
            if (slot == 1) {
                return true; // Can always extract from output slot
            }

            if (isSmash()) {
                return false;
            }

            // Can only extract from top and bottom in separated sides mode
            return isSeparateSides() && (inv == InfinityOutputterBlockEntity.this.topItemHandler
                    || inv == InfinityOutputterBlockEntity.this.bottomItemHandler);
        }

        @Override
        public boolean allowInsert(InternalInventory inv, int slot, ItemStack stack) {
            if (slot == 1) {
                return false; // No inserting into the output slot
            }
            return !isSmash();
        }
    }
}
