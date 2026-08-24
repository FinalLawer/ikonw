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

package com.example.iknow.client;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import appeng.api.config.InscriberInputCapacity;
import appeng.api.config.Settings;
import appeng.api.config.YesNo;
import appeng.api.util.IConfigManager;
import com.example.iknow.block.InfinityOutputterBlockEntity;
import appeng.blockentity.misc.InscriberRecipes;
import appeng.client.gui.Icon;
import appeng.core.definitions.AEItems;
import appeng.core.definitions.ItemDefinition;
import appeng.core.localization.Side;
import appeng.core.localization.Tooltips;
import appeng.menu.SlotSemantics;
import appeng.menu.guisync.GuiSync;
import appeng.menu.implementations.MenuTypeBuilder;
import appeng.menu.implementations.UpgradeableMenu;
import appeng.menu.interfaces.IProgressProvider;
import appeng.menu.slot.AppEngSlot;
import appeng.menu.slot.OutputSlot;

/**
 * @see appeng.client.gui.implementations.InscriberScreen
 */
public class InfinityOutputterMenu extends UpgradeableMenu<InfinityOutputterBlockEntity> implements IProgressProvider {

    public static final MenuType<InfinityOutputterMenu> TYPE = MenuTypeBuilder
            .create(InfinityOutputterMenu::new, InfinityOutputterBlockEntity.class)
            .withMenuTitle(be -> net.minecraft.network.chat.Component
                    .translatable("block.iknow.infinite_item_outputter"))
            .buildUnregistered(net.minecraft.resources.ResourceLocation
                    .fromNamespaceAndPath(com.example.iknow.IknowMod.MODID, "infinite_item_outputter"));

    private final Slot top;
    private final Slot middle;
    private final Slot bottom;

    @GuiSync(2)
    public int maxProcessingTime = -1;

    @GuiSync(3)
    public int processingTime = -1;

    @GuiSync(7)
    public YesNo separateSides = YesNo.NO;
    @GuiSync(8)
    public YesNo autoExport = YesNo.NO;
    @GuiSync(9)
    public InscriberInputCapacity bufferSize = InscriberInputCapacity.SIXTY_FOUR;

    @GuiSync(10)
    public int outputFacingValue = 0;

    public InfinityOutputterMenu(int id, Inventory ip, InfinityOutputterBlockEntity host) {
        super(TYPE, id, ip, host);

        var inv = host.getInternalInventory();

        var top = new AppEngSlot(inv, 0);
        top.setIcon(Icon.BACKGROUND_PLATE);
        top.setEmptyTooltip(
                () -> separateSides == YesNo.YES ? Tooltips.inputSlot(Side.TOP) : Tooltips.inputSlot(Side.ANY));
        this.top = this.addSlot(top, SlotSemantics.INSCRIBER_PLATE_TOP);

        var bottom = new AppEngSlot(inv, 1);
        bottom.setIcon(Icon.BACKGROUND_PLATE);
        bottom.setEmptyTooltip(
                () -> separateSides == YesNo.YES ? Tooltips.inputSlot(Side.BOTTOM) : Tooltips.inputSlot(Side.ANY));
        this.bottom = this.addSlot(bottom, SlotSemantics.INSCRIBER_PLATE_BOTTOM);

        var middle = new AppEngSlot(inv, 2);
        middle.setIcon(Icon.BACKGROUND_INGOT);
        middle.setEmptyTooltip(
                () -> separateSides == YesNo.YES ? Tooltips.inputSlot(Side.LEFT, Side.RIGHT, Side.BACK, Side.FRONT)
                        : Tooltips.inputSlot(Side.ANY));
        this.middle = this.addSlot(middle, SlotSemantics.MACHINE_INPUT);

        var output = new OutputSlot(inv, 3, null);
        output.setEmptyTooltip(
                () -> separateSides == YesNo.YES ? Tooltips.outputSlot(Side.LEFT, Side.RIGHT, Side.BACK, Side.FRONT)
                        : Tooltips.outputSlot(Side.ANY));
        this.addSlot(output, SlotSemantics.MACHINE_OUTPUT);
    }

    @Override
    protected void loadSettingsFromHost(IConfigManager cm) {
        this.separateSides = getHost().getConfigManager().getSetting(Settings.INSCRIBER_SEPARATE_SIDES);
        this.autoExport = getHost().getConfigManager().getSetting(Settings.AUTO_EXPORT);
        this.bufferSize = getHost().getConfigManager().getSetting(Settings.INSCRIBER_INPUT_CAPACITY);
    }

    @Override
    protected void standardDetectAndSendChanges() {
        if (isServerSide()) {
            this.maxProcessingTime = getHost().getMaxProcessingTime();
            this.processingTime = getHost().getProcessingTime();
            this.outputFacingValue = getHost().getOutputFacing().get3DDataValue();
        }
        super.standardDetectAndSendChanges();
    }

    /** 客户端点击：请求服务端循环切换输出面 */
    public void cycleOutputFacing() {
        com.example.iknow.network.ModNetwork.sendInfinityOutputterAction(
                com.example.iknow.network.InfinityOutputterActionPayload.CYCLE_FACING);
    }

    public net.minecraft.core.Direction getOutputFacing() {
        return net.minecraft.core.Direction.from3DDataValue(outputFacingValue);
    }

    public int getOutputFacingValue() {
        return outputFacingValue;
    }

    @Override
    public boolean isValidForSlot(Slot s, ItemStack is) {
        // 无限元件可放入中间（输入）槽，即使没有对应压印配方
        if (s == this.middle && isInfinityCell(is)) {
            return true;
        }

        final ItemStack top = getHost().getInternalInventory().getStackInSlot(0);
        final ItemStack bot = getHost().getInternalInventory().getStackInSlot(1);

        if (s == this.middle) {
            ItemDefinition<?> press = AEItems.NAME_PRESS;
            if (press.is(top) || press.is(bot)) {
                return !press.is(is);
            }

            return InscriberRecipes.findRecipe(getHost().getLevel(), is, top, bot, false) != null;
        } else if (s == this.top && !bot.isEmpty() || s == this.bottom && !top.isEmpty()) {
            ItemStack otherSlot;
            if (s == this.top) {
                otherSlot = this.bottom.getItem();
            } else {
                otherSlot = this.top.getItem();
            }

            // name presses
            ItemDefinition<?> namePress = AEItems.NAME_PRESS;
            if (namePress.is(otherSlot)) {
                return namePress.is(is);
            }

            // everything else
            // test for a partial recipe match (ignoring the middle slot)
            return InscriberRecipes.isValidOptionalIngredientCombination(getHost().getLevel(), is, otherSlot);
        }
        return true;
    }

    /** 软依赖判断是否为 ExtendedAE 无限元件（反射），失败返回 false */
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

    @Override
    public int getCurrentProgress() {
        return this.processingTime;
    }

    @Override
    public int getMaxProgress() {
        return this.maxProcessingTime;
    }

    public YesNo getSeparateSides() {
        return separateSides;
    }

    public YesNo getAutoExport() {
        return autoExport;
    }

    public InscriberInputCapacity getBufferSize() {
        return bufferSize;
    }
}
