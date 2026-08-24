package com.example.iknow.client;

import com.example.iknow.FlightHandler;
import com.example.iknow.ModDataComponents;
import com.example.iknow.PickupMode;
import com.example.iknow.ToolMode;
import com.example.iknow.item.IknowToolItem;
import com.example.iknow.network.ModeChangePayload;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;

/**
 * 多功能工具的功能轮盘界面。
 */
public class ModeWheelScreen extends Screen {

    // ============ 布局：三个等大轮盘横向排布，滑块/按钮在左栏 ============
        private static final int WHEEL_RADIUS_MAX = 78;
    private static final int WHEEL_INNER = 40;
    private static final int CENTER_RADIUS = 28;
    private static final int ROW_LEFT = 130;
    private static final int ROW_RIGHT_MARGIN = 16;

    private static final int[] TOOL_COLORS = {
            0xB0413C, 0x1F7AC4, 0x3E8E4A, 0xA83E9E, 0x8B6F47
    };

    private static final int[] ENCH_OPTIONS = {
            IknowToolItem.ENCHANT_SILK, IknowToolItem.ENCHANT_FORTUNE, IknowToolItem.ENCHANT_OFF
    };
    private static final int[] ENCH_COLORS = {
            0x2F8FDF, 0xD9A428, 0x6E6E6E
    };

    private static final int CONTROL_X = 12;
    private static final int SLIDER_WIDTH = 95;
    private static final int SLIDER_HEIGHT = 16;
    private static final int FLIGHT_BTN_WIDTH = 95;
    private static final int FLIGHT_BTN_HEIGHT = 16;
    private static final int MINING_LABEL_Y = -56;
    private static final int MINING_SLIDER_Y = -40;
    private static final int FLIGHT_LABEL_Y = -18;
    private static final int FLIGHT_SLIDER_Y = -2;
    private static final int FLIGHT_BTN_Y = 22;
    private static final int NIGHTVISION_BTN_Y = 44;
    private static final int BLOCK_REACH_LABEL_Y = 62;
    private static final int BLOCK_REACH_SLIDER_Y = 78;
    private static final int ATTACK_REACH_LABEL_Y = 98;
    private static final int ATTACK_REACH_SLIDER_Y = 114;
    private static final float REACH_MAX = 10.0F;

    private static final PickupMode[] PICKUP_MODES = {
            PickupMode.MAGNET, PickupMode.MAGNET_AE, PickupMode.BREAK_INVENTORY, PickupMode.BREAK_AE
    };
    private static final int[] PICKUP_COLORS = { 0x2E8B57, 0x1F7AC4, 0xD9A428, 0xA83E9E };

    private boolean singleMode = false;
    private int hoveredToolSector = -1;
    private int hoveredEnchSector = -1;
    private int hoveredPickupSector = -1;
    private int tickCount = 0;
    private ValueSlider speedSlider;
    private ValueSlider flightSpeedSlider;
    private FloatSlider blockReachSlider;
    private FloatSlider attackReachSlider;

    public ModeWheelScreen() {
        super(Component.translatable("wheel.iknow.title"));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void resize(Minecraft mc, int width, int height) {
        super.resize(mc, width, height);
        // 缩放窗口时 MC 不重跑 init()，这里手动重建控件，使其跟随新的 width/height 布局
        this.rebuildWidgets();
    }

    @Override
    public void tick() {
        this.tickCount++;
    }

    @Override
    protected void init() {
        super.init();
        int cy = this.height / 2;
        ItemStack stack = heldStack();
        int speed = stack != null ? IknowToolItem.miningSpeed(stack) : 50;
        int flight = stack != null ? IknowToolItem.flightSpeed(stack) : 50;
        float blockReach = stack != null ? IknowToolItem.blockReach(stack) : IknowToolItem.DEFAULT_BLOCK_REACH;
        float attackReach = stack != null ? IknowToolItem.attackReach(stack) : IknowToolItem.DEFAULT_ATTACK_REACH;
        this.speedSlider = new ValueSlider(CONTROL_X, cy + MINING_SLIDER_Y, SLIDER_WIDTH, SLIDER_HEIGHT,
                speed, ModeWheelScreen::onSpeedChanged);
        this.flightSpeedSlider = new ValueSlider(CONTROL_X, cy + FLIGHT_SLIDER_Y, SLIDER_WIDTH, SLIDER_HEIGHT,
                flight, ModeWheelScreen::onFlightSpeedChanged);
        this.blockReachSlider = new FloatSlider(CONTROL_X, cy + BLOCK_REACH_SLIDER_Y, SLIDER_WIDTH, SLIDER_HEIGHT,
                IknowToolItem.DEFAULT_BLOCK_REACH, REACH_MAX, blockReach, ModeWheelScreen::onBlockReachChanged);
        this.attackReachSlider = new FloatSlider(CONTROL_X, cy + ATTACK_REACH_SLIDER_Y, SLIDER_WIDTH, SLIDER_HEIGHT,
                IknowToolItem.DEFAULT_ATTACK_REACH, REACH_MAX, attackReach, ModeWheelScreen::onAttackReachChanged);
        this.addRenderableWidget(this.speedSlider);
        this.addRenderableWidget(this.flightSpeedSlider);
        this.addRenderableWidget(this.blockReachSlider);
        this.addRenderableWidget(this.attackReachSlider);
    }

    // ==================== 动态布局 ====================
        private int rowSpacing() {
        int avail = this.width - ROW_LEFT - ROW_RIGHT_MARGIN;
        return Math.max(88, avail / 3);
    }

    /** 根据可用间隔缩放整个轮盘（中心/内圈/外圈同比例），保证窄窗口下环形仍有厚度、仍可点击 */
    private float wheelScale() {
        int spacing = rowSpacing();
        return Math.max(0.42F, Math.min(1.0F, (spacing - 6) / (2.0F * WHEEL_RADIUS_MAX)));
    }

    private int outerR() {
        return (int) (WHEEL_RADIUS_MAX * wheelScale());
    }

    private int innerR() {
        return (int) (WHEEL_INNER * wheelScale());
    }

    private int centerR() {
        return (int) (CENTER_RADIUS * wheelScale());
    }

    private int toolX() {
        return ROW_LEFT + (int) (rowSpacing() * 0.5);
    }

    private int enchX() {
        return ROW_LEFT + (int) (rowSpacing() * 1.5);
    }

    private int pickupX() {
        return ROW_LEFT + (int) (rowSpacing() * 2.5);
    }

    private int cy() {
        return this.height / 2;
    }

    // ==================== 渲染 ====================

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        int cy = cy();
        int tx = toolX();
        int ex = enchX();
        int px = pickupX();
        int radius = outerR();
        int inner = innerR();

        float scale = Math.min(1.0F, (this.tickCount + partialTick) / 5.0F);

        guiGraphics.drawCenteredString(this.font, Component.translatable("wheel.iknow.title"), this.width / 2, 24, 0xFFFFFFFF);
        guiGraphics.drawCenteredString(this.font, Component.translatable("wheel.iknow.hint"), this.width / 2, 40, 0xFFAAAAAA);
        guiGraphics.drawCenteredString(this.font, Component.translatable("wheel.iknow.tool_title"), tx, cy - radius - 40, 0xFFFFFFFF);
        guiGraphics.drawCenteredString(this.font, Component.translatable("wheel.iknow.enchant_title"), ex, cy - radius - 40, 0xFFFFFFFF);
        guiGraphics.drawCenteredString(this.font, Component.translatable("wheel.iknow.pickup_title"), px, cy - radius - 40, 0xFFDDDDDD);

        int toolModes = currentToolModes();
        boolean[] toolSelected = new boolean[ToolMode.values().length];
        for (int i = 0; i < toolSelected.length; i++) {
            toolSelected[i] = ToolMode.isEnabled(toolModes, ToolMode.values()[i]);
        }
        ItemStack[] toolIcons = new ItemStack[ToolMode.values().length];
        Component[] toolNames = new Component[ToolMode.values().length];
        for (int i = 0; i < ToolMode.values().length; i++) {
            toolIcons[i] = toolIcon(ToolMode.values()[i]);
            toolNames[i] = ToolMode.values()[i].displayName();
        }
        drawWheel(guiGraphics, tx, cy, (int) (inner * scale), (int) (radius * scale),
                toolSelected, hoveredToolSector, toolIcons, toolNames, TOOL_COLORS);

        fillSector(guiGraphics, tx, cy, 0, centerR(), 0.0, Math.PI * 2, 0xE0202438);
        fillSector(guiGraphics, tx, cy, centerR() - 1, centerR(), 0.0, Math.PI * 2, 0xFF3A4060);
        guiGraphics.drawCenteredString(this.font,
                Component.translatable(this.singleMode ? "wheel.iknow.single" : "wheel.iknow.multi"),
                tx, cy - 8, 0xFFFFFFFF);
        guiGraphics.drawCenteredString(this.font, Component.translatable("wheel.iknow.switch"), tx, cy + 6, 0xFF9AA0C0);

        int enchMode = currentEnchantMode();
        boolean[] enchSelected = new boolean[ENCH_OPTIONS.length];
        ItemStack[] enchIcons = new ItemStack[ENCH_OPTIONS.length];
        Component[] enchNames = new Component[ENCH_OPTIONS.length];
        for (int i = 0; i < ENCH_OPTIONS.length; i++) {
            enchSelected[i] = enchMode == ENCH_OPTIONS[i];
            enchIcons[i] = enchIcon(ENCH_OPTIONS[i]);
            enchNames[i] = enchName(ENCH_OPTIONS[i]);
        }
        drawWheel(guiGraphics, ex, cy, (int) (inner * scale), (int) (radius * scale),
                enchSelected, hoveredEnchSector, enchIcons, enchNames, ENCH_COLORS);
        fillSector(guiGraphics, ex, cy, 0, inner - 6, 0.0, Math.PI * 2, 0xC0181828);
        guiGraphics.drawCenteredString(this.font, enchName(enchMode), ex, cy - 4, 0xFFFFFFFF);

        PickupMode curMagnet = currentMagnetMode();
        PickupMode curBreak = currentBreakMode();
        boolean[] pickupSel = new boolean[PICKUP_MODES.length];
        ItemStack[] pickupIcons = new ItemStack[PICKUP_MODES.length];
        Component[] pickupNames = new Component[PICKUP_MODES.length];
        for (int i = 0; i < PICKUP_MODES.length; i++) {
            PickupMode pm = PICKUP_MODES[i];
            boolean sel = switch (pm) {
                case MAGNET, MAGNET_AE -> pm == curMagnet;
                case BREAK_INVENTORY, BREAK_AE -> pm == curBreak;
                default -> false;
            };
            pickupSel[i] = sel;
            pickupIcons[i] = pickupIcon(pm);
            pickupNames[i] = pm.displayName();
        }
        drawWheel(guiGraphics, px, cy, (int) (inner * scale), (int) (radius * scale),
                pickupSel, hoveredPickupSector, pickupIcons, pickupNames, PICKUP_COLORS);
        fillSector(guiGraphics, px, cy, 0, inner - 6, 0.0, Math.PI * 2, 0xC0181828);
        guiGraphics.drawCenteredString(this.font, currentPickupLabel(curMagnet, curBreak), px, cy - 4, 0xFFFFFFFF);

        guiGraphics.drawCenteredString(this.font, currentStatusComponent(), this.width / 2, this.height - 18, 0xFFDDDDDD);

        int sliderValue = this.speedSlider != null ? this.speedSlider.valueInt() : 50;
        guiGraphics.drawString(this.font,
                Component.translatable("wheel.iknow.speed").append(String.valueOf(sliderValue)),
                CONTROL_X, cy + MINING_LABEL_Y, 0xFFDDDDDD);
        int flightValue = this.flightSpeedSlider != null ? this.flightSpeedSlider.valueInt() : 50;
        guiGraphics.drawString(this.font,
                Component.translatable("wheel.iknow.flight_speed").append(String.valueOf(flightValue)),
                CONTROL_X, cy + FLIGHT_LABEL_Y, 0xFFDDDDDD);
        drawFlightButton(guiGraphics, cy);
        drawNightVisionButton(guiGraphics, cy);
        float blockReachValue = this.blockReachSlider != null ? this.blockReachSlider.floatValue() : IknowToolItem.DEFAULT_BLOCK_REACH;
        guiGraphics.drawString(this.font,
                Component.translatable("wheel.iknow.block_reach").append(String.format("%.1f", blockReachValue)),
                CONTROL_X, cy + BLOCK_REACH_LABEL_Y, 0xFFDDDDDD);
        float attackReachValue = this.attackReachSlider != null ? this.attackReachSlider.floatValue() : IknowToolItem.DEFAULT_ATTACK_REACH;
        guiGraphics.drawString(this.font,
                Component.translatable("wheel.iknow.attack_reach").append(String.format("%.1f", attackReachValue)),
                CONTROL_X, cy + ATTACK_REACH_LABEL_Y, 0xFFDDDDDD);
    }

    private void drawWheel(GuiGraphics guiGraphics, int cx, int cy, int rIn, int rOut,
                           boolean[] selected, int hoveredSector, ItemStack[] icons, Component[] names, int[] colors) {
        int count = icons.length;
        double step = 360.0 / count;
        int midR = (rIn + rOut) / 2;
        fillSector(guiGraphics, cx, cy, rOut - 2, rOut, 0.0, Math.PI * 2, 0xAA181828);
        for (int i = 0; i < count; i++) {
            double mid = Math.toRadians(-90 + i * step);
            double a0 = mid - Math.toRadians(step / 2);
            double a1 = mid + Math.toRadians(step / 2);
            boolean sel = selected[i];
            boolean hover = i == hoveredSector;
            int alpha = (sel || hover) ? 0xD9 : 0x8C;
            fillSector(guiGraphics, cx, cy, rIn, rOut, a0, a1, (alpha << 24) | colors[i]);
            if (sel) {
                fillSector(guiGraphics, cx, cy, rOut, rOut + 4, a0, a1, 0x30FFFFFF);
            }
            fillSector(guiGraphics, cx, cy, rIn, rOut, a0 - 0.02, a0, 0x66181828);
            int ix = cx + (int) (Math.cos(mid) * midR);
            int iy = cy + (int) (Math.sin(mid) * midR);
            guiGraphics.renderItem(icons[i], ix - 8, iy - 8);
            guiGraphics.drawCenteredString(this.font, names[i], ix, iy + 11, sel ? 0xFFFFFFFF : 0xFFCCCCCC);
            if (sel) {
                fillSector(guiGraphics, ix + 9, iy - 9, 0, 3, 0.0, Math.PI * 2, 0xFF4ADE80);
            }
        }
    }

    // ==================== 输入 ====================

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) {
            return super.mouseClicked(mouseX, mouseY, button);
        }
        int cy = cy();
        int tx = toolX();
        int ex = enchX();
        int px = pickupX();
        int radius = outerR();
        int inner = innerR();

        double eDist = Math.hypot(mouseX - ex, mouseY - cy);
        if (eDist <= radius) {
            if (eDist >= inner) {
                int idx = sectorAt(ex, cy, ENCH_OPTIONS.length, mouseX, mouseY);
                if (idx >= 0) {
                    setEnchantMode(ENCH_OPTIONS[idx]);
                    return true;
                }
            } else {
                setEnchantMode(IknowToolItem.ENCHANT_OFF);
                return true;
            }
        }

        double pDist = Math.hypot(mouseX - px, mouseY - cy);
        if (pDist <= radius) {
            if (pDist >= inner) {
                int idx = sectorAt(px, cy, PICKUP_MODES.length, mouseX, mouseY);
                if (idx >= 0) {
                    togglePickupSector(PICKUP_MODES[idx]);
                    return true;
                }
            } else {
                clearPickupModes();
                return true;
            }
        }

        if ((this.speedSlider != null && this.speedSlider.isMouseOver(mouseX, mouseY))
                || (this.flightSpeedSlider != null && this.flightSpeedSlider.isMouseOver(mouseX, mouseY))
                || (this.blockReachSlider != null && this.blockReachSlider.isMouseOver(mouseX, mouseY))
                || (this.attackReachSlider != null && this.attackReachSlider.isMouseOver(mouseX, mouseY))) {
            return super.mouseClicked(mouseX, mouseY, button);
        }
        if (isFlightButtonAt(mouseX, mouseY)) {
            Player p = Minecraft.getInstance().player;
            if (p != null) {
                FlightHandler.setNoInertia(p, !FlightHandler.noInertia(p));
                ItemStack stack = heldStack();
                if (stack != null) {
                    sendUpdate(stack);
                }
            }
            playClickSound();
            return true;
        }
        if (isNightVisionButtonAt(mouseX, mouseY)) {
            Player p = Minecraft.getInstance().player;
            if (p != null) {
                FlightHandler.setNightVision(p, !FlightHandler.nightVisionEnabled(p));
                ItemStack stack = heldStack();
                if (stack != null) {
                    sendUpdate(stack);
                }
            }
            playClickSound();
            return true;
        }

        double tDist = Math.hypot(mouseX - tx, mouseY - cy);
        if (tDist <= centerR()) {
            this.singleMode = !this.singleMode;
            playClickSound();
            return true;
        }
        if (tDist >= inner && tDist <= radius) {
            int idx = sectorAt(tx, cy, ToolMode.values().length, mouseX, mouseY);
            if (idx >= 0) {
                applyToolSelection(idx);
                return true;
            }
        }
        return true;
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        int cy = cy();
        int tx = toolX();
        int ex = enchX();
        int px = pickupX();
        int radius = outerR();
        int inner = innerR();
        double tDist = Math.hypot(mouseX - tx, mouseY - cy);
        this.hoveredToolSector = (tDist >= inner && tDist <= radius)
                ? sectorAt(tx, cy, ToolMode.values().length, mouseX, mouseY) : -1;
        double eDist = Math.hypot(mouseX - ex, mouseY - cy);
        this.hoveredEnchSector = (eDist >= inner && eDist <= radius)
                ? sectorAt(ex, cy, ENCH_OPTIONS.length, mouseX, mouseY) : -1;
        double pDist = Math.hypot(mouseX - px, mouseY - cy);
        this.hoveredPickupSector = (pDist >= inner && pDist <= radius)
                ? sectorAt(px, cy, PICKUP_MODES.length, mouseX, mouseY) : -1;
    }

    @Override
    public void onClose() {
        ClientEvents.onWheelClosed();
        super.onClose();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            this.onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        if (keyCode == ClientEvents.OPEN_WHEEL.getKey().getValue()) {
            this.onClose();
            return true;
        }
        return super.keyReleased(keyCode, scanCode, modifiers);
    }

    // ==================== 逻辑 ====================
        private void applyToolSelection(int idx) {
        ToolMode mode = ToolMode.values()[idx];
        ItemStack stack = heldStack();
        if (stack == null) {
            this.onClose();
            return;
        }
        int modes = IknowToolItem.modes(stack);
        int next = this.singleMode ? ToolMode.only(mode) : ToolMode.toggle(modes, mode);
        stack.set(ModDataComponents.TOOL_MODES.get(), next);
        sendUpdate(stack);
        playClickSound();
        if (this.singleMode) {
            this.onClose();
        }
    }

    private void setEnchantMode(int mode) {
        ItemStack stack = heldStack();
        if (stack == null) {
            return;
        }
        stack.set(ModDataComponents.ENCHANT_MODE.get(), mode);
        if (Minecraft.getInstance().level != null) {
            IknowToolItem.applyEnchantments(stack, mode, Minecraft.getInstance().level.registryAccess());
        }
        sendUpdate(stack);
        playClickSound();
    }

    private static void sendUpdate(ItemStack stack) {
        Player player = Minecraft.getInstance().player;
        boolean noInertia = player != null && FlightHandler.noInertia(player);
        boolean nightVision = player != null && FlightHandler.nightVisionEnabled(player);
        int flags = (noInertia ? ModeChangePayload.FLAG_NO_INERTIA : 0)
                | (nightVision ? ModeChangePayload.FLAG_NIGHT_VISION : 0);
        PacketDistributor.sendToServer(new ModeChangePayload(
                IknowToolItem.modes(stack),
                IknowToolItem.enchantMode(stack),
                IknowToolItem.miningSpeed(stack),
                IknowToolItem.flightSpeed(stack),
                stack.getOrDefault(ModDataComponents.PICKUP_MODE.get(), 0),
                flags,
                IknowToolItem.blockReach(stack),
                IknowToolItem.attackReach(stack)));
    }

    static void onSpeedChanged(int speed) {
        ItemStack stack = heldStack();
        if (stack == null) {
            return;
        }
        stack.set(ModDataComponents.MINING_SPEED.get(), speed);
        sendUpdate(stack);
    }

    static void onFlightSpeedChanged(int speed) {
        ItemStack stack = heldStack();
        if (stack == null) {
            return;
        }
        stack.set(ModDataComponents.FLIGHT_SPEED.get(), speed);
        sendUpdate(stack);
    }

    static void onBlockReachChanged(float reach) {
        ItemStack stack = heldStack();
        if (stack == null) {
            return;
        }
        stack.set(ModDataComponents.BLOCK_REACH.get(), reach);
        sendUpdate(stack);
    }

    static void onAttackReachChanged(float reach) {
        ItemStack stack = heldStack();
        if (stack == null) {
            return;
        }
        stack.set(ModDataComponents.ATTACK_REACH.get(), reach);
        sendUpdate(stack);
    }

    private boolean isFlightButtonAt(double mouseX, double mouseY) {
        int cy = this.height / 2;
        int y = cy + FLIGHT_BTN_Y;
        return mouseX >= CONTROL_X && mouseX <= CONTROL_X + FLIGHT_BTN_WIDTH
                && mouseY >= y && mouseY <= y + FLIGHT_BTN_HEIGHT;
    }

    private void drawFlightButton(GuiGraphics guiGraphics, int cy) {
        int y = cy + FLIGHT_BTN_Y;
        Player player = Minecraft.getInstance().player;
        boolean noInertia = player != null && FlightHandler.noInertia(player);
        guiGraphics.fill(CONTROL_X, y, CONTROL_X + FLIGHT_BTN_WIDTH, y + FLIGHT_BTN_HEIGHT, noInertia ? 0xE03A9F4F : 0xD03A3A3A);
        guiGraphics.drawCenteredString(this.font,
                Component.translatable(noInertia ? "wheel.iknow.inertia_off" : "wheel.iknow.inertia_on"),
                CONTROL_X + FLIGHT_BTN_WIDTH / 2, y + 4, 0xFFFFFFFF);
    }

    private boolean isNightVisionButtonAt(double mouseX, double mouseY) {
        int cy = this.height / 2;
        int y = cy + NIGHTVISION_BTN_Y;
        return mouseX >= CONTROL_X && mouseX <= CONTROL_X + FLIGHT_BTN_WIDTH
                && mouseY >= y && mouseY <= y + FLIGHT_BTN_HEIGHT;
    }

    private void drawNightVisionButton(GuiGraphics guiGraphics, int cy) {
        int y = cy + NIGHTVISION_BTN_Y;
        Player player = Minecraft.getInstance().player;
        boolean on = player != null && FlightHandler.nightVisionEnabled(player);
        guiGraphics.fill(CONTROL_X, y, CONTROL_X + FLIGHT_BTN_WIDTH, y + FLIGHT_BTN_HEIGHT, on ? 0xE03A9F4F : 0xD03A3A3A);
        guiGraphics.drawCenteredString(this.font,
                Component.translatable(on ? "wheel.iknow.nightvision_on" : "wheel.iknow.nightvision_off"),
                CONTROL_X + FLIGHT_BTN_WIDTH / 2, y + 4, 0xFFFFFFFF);
    }

    private int currentToolModes() {
        ItemStack stack = heldStack();
        return stack != null ? IknowToolItem.modes(stack) : ToolMode.DEFAULT_MASK;
    }

    private int currentEnchantMode() {
        ItemStack stack = heldStack();
        return stack != null ? IknowToolItem.enchantMode(stack) : IknowToolItem.ENCHANT_OFF;
    }

    private PickupMode currentMagnetMode() {
        ItemStack stack = heldStack();
        return stack != null ? IknowToolItem.magnetMode(stack) : PickupMode.NONE;
    }

    private PickupMode currentBreakMode() {
        ItemStack stack = heldStack();
        return stack != null ? IknowToolItem.breakMode(stack) : PickupMode.NONE;
    }

    private Component currentPickupLabel(PickupMode magnet, PickupMode brk) {
        if (magnet == PickupMode.NONE && brk == PickupMode.NONE) {
            return Component.translatable("wheel.iknow.none");
        }
        String mName = magnet != PickupMode.NONE ? magnet.displayName().getString() : "";
        String bName = brk != PickupMode.NONE ? brk.displayName().getString() : "";
        String join = (magnet != PickupMode.NONE && brk != PickupMode.NONE) ? " + " : "";
        return Component.literal(mName + join + bName);
    }

    private void togglePickupSector(PickupMode mode) {
        ItemStack stack = heldStack();
        if (stack == null) {
            return;
        }
        PickupMode mag = currentMagnetMode();
        PickupMode brk = currentBreakMode();
        if (mode == PickupMode.MAGNET || mode == PickupMode.MAGNET_AE) {
            mag = (mag == mode) ? PickupMode.NONE : mode;
        } else if (mode == PickupMode.BREAK_INVENTORY || mode == PickupMode.BREAK_AE) {
            brk = (brk == mode) ? PickupMode.NONE : mode;
        }
        IknowToolItem.setPickupModes(stack, mag, brk);
        sendUpdate(stack);
        playClickSound();
    }

    private void clearPickupModes() {
        ItemStack stack = heldStack();
        if (stack == null) {
            return;
        }
        IknowToolItem.setPickupModes(stack, PickupMode.NONE, PickupMode.NONE);
        sendUpdate(stack);
        playClickSound();
    }

    private static ItemStack pickupIcon(PickupMode mode) {
        return switch (mode) {
            case MAGNET -> new ItemStack(Items.SLIME_BALL);
            case MAGNET_AE -> new ItemStack(Items.ENDER_PEARL);
            case BREAK_INVENTORY -> new ItemStack(Items.CHEST);
            case BREAK_AE -> new ItemStack(Items.ENDER_CHEST);
            default -> new ItemStack(Items.BARRIER);
        };
    }

    private static ItemStack heldStack() {
        Player player = Minecraft.getInstance().player;
        if (player == null) {
            return null;
        }
        ItemStack main = player.getMainHandItem();
        if (main.getItem() instanceof IknowToolItem) {
            return main;
        }
        ItemStack off = player.getOffhandItem();
        return off.getItem() instanceof IknowToolItem ? off : null;
    }

    private static int sectorAt(int cx, int cy, int count, double mouseX, double mouseY) {
        double n = (Math.toDegrees(Math.atan2(mouseY - cy, mouseX - cx)) + 360) % 360;
        double step = 360.0 / count;
        for (int i = 0; i < count; i++) {
            double start = (-90 + i * step - step / 2 + 360) % 360;
            double end = (-90 + i * step + step / 2 + 360) % 360;
            if (start < end ? (n >= start && n < end) : (n >= start || n < end)) {
                return i;
            }
        }
        return -1;
    }

    private Component currentStatusComponent() {
        ItemStack stack = heldStack();
        int modes = stack != null ? IknowToolItem.modes(stack) : ToolMode.DEFAULT_MASK;
        List<Component> names = new ArrayList<>();
        for (ToolMode mode : ToolMode.values()) {
            if (ToolMode.isEnabled(modes, mode)) {
                names.add(mode.displayName());
            }
        }
        String joined = names.stream().map(c -> c.getString()).collect(Collectors.joining("、"));
        if (joined.isEmpty()) {
            joined = Component.translatable("wheel.iknow.none").getString();
        }
        return Component.translatable("wheel.iknow.current").append(joined)
                .append(" · ").append(Component.translatable("wheel.iknow.enchant_current"))
                .append(enchName(currentEnchantMode()));
    }

    private static ItemStack toolIcon(ToolMode mode) {
        return switch (mode) {
            case AXE -> new ItemStack(Items.DIAMOND_AXE);
            case PICKAXE -> new ItemStack(Items.DIAMOND_PICKAXE);
            case HOE -> new ItemStack(Items.DIAMOND_HOE);
            case SHEARS -> new ItemStack(Items.SHEARS);
            case SHOVEL -> new ItemStack(Items.DIAMOND_SHOVEL);
        };
    }

    private static ItemStack enchIcon(int mode) {
        return switch (mode) {
            case IknowToolItem.ENCHANT_SILK -> new ItemStack(Items.ENCHANTED_BOOK);
            case IknowToolItem.ENCHANT_FORTUNE -> new ItemStack(Items.GOLD_NUGGET);
            default -> new ItemStack(Items.BARRIER);
        };
    }

    private static Component enchName(int mode) {
        return switch (mode) {
            case IknowToolItem.ENCHANT_SILK -> Component.translatable("enchantmode.iknow.silk");
            case IknowToolItem.ENCHANT_FORTUNE -> Component.translatable("enchantmode.iknow.fortune");
            default -> Component.translatable("enchantmode.iknow.off");
        };
    }

    private static void playClickSound() {
        Player player = Minecraft.getInstance().player;
        if (player != null) {
            player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 1.0F, 1.0F);
        }
    }

    // ==================== 滑块 ====================

    private static class ValueSlider extends AbstractSliderButton {
        private final Consumer<Integer> onChanged;

        ValueSlider(int x, int y, int width, int height, int value, Consumer<Integer> onChanged) {
            super(x, y, width, height, Component.literal(String.valueOf(value)), value / 100.0);
            this.onChanged = onChanged;
        }

        int valueInt() {
            return (int) Math.round(this.value * 100.0);
        }

        @Override
        protected void updateMessage() {
            this.setMessage(Component.literal(String.valueOf(valueInt())));
        }

        @Override
        protected void applyValue() {
            this.onChanged.accept(valueInt());
        }
    }

    private static class FloatSlider extends AbstractSliderButton {
        private final float min;
        private final float max;
        private final Consumer<Float> onChanged;

        FloatSlider(int x, int y, int width, int height, float min, float max, float value, Consumer<Float> onChanged) {
            super(x, y, width, height, Component.empty(), (value - min) / (max - min));
            this.min = min;
            this.max = max;
            this.onChanged = onChanged;
            this.updateMessage();
        }

        float floatValue() {
            return this.min + (float) (this.value * (this.max - this.min));
        }

        @Override
        protected void updateMessage() {
            this.setMessage(Component.literal(String.format("%.1f", floatValue())));
        }

        @Override
        protected void applyValue() {
            this.onChanged.accept(floatValue());
        }
    }

    // ==================== 扇形填充 ====================
        private static final RenderType WHEEL_STRIP = RenderType.create(
            "iknow_wheel_strip",
            DefaultVertexFormat.POSITION_COLOR,
            VertexFormat.Mode.TRIANGLE_STRIP,
            8192, false, false,
            RenderType.CompositeState.builder()
                    .setShaderState(RenderStateShard.POSITION_COLOR_SHADER)
                    .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
                    .createCompositeState(false));

    private static void fillSector(GuiGraphics guiGraphics, int cx, int cy, double rIn, double rOut, double a0, double a1, int argb) {
        if (rOut <= 0 || rIn > rOut) {
            return;
        }
        int segments = Math.max(16, (int) Math.ceil((a1 - a0) / Math.PI * 2 * 64));
        VertexConsumer vc = guiGraphics.bufferSource().getBuffer(WHEEL_STRIP);
        Matrix4f pose = guiGraphics.pose().last().pose();
        int r = (argb >> 16) & 0xFF;
        int g = (argb >> 8) & 0xFF;
        int b = argb & 0xFF;
        int a = (argb >> 24) & 0xFF;
        for (int i = 0; i <= segments; i++) {
            double t = a0 + (a1 - a0) * i / segments;
            double cos = Math.cos(t);
            double sin = Math.sin(t);
            vc.addVertex(pose, (float) (cx + cos * rOut), (float) (cy + sin * rOut), 0.0F).setColor(r, g, b, a);
            vc.addVertex(pose, (float) (cx + cos * rIn), (float) (cy + sin * rIn), 0.0F).setColor(r, g, b, a);
        }
    }
}
