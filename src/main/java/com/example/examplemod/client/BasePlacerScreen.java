package com.example.examplemod.client;

import com.example.examplemod.BaseBuildHandler;
import com.example.examplemod.network.BasePlacerStartPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * 鍩哄湴閾鸿鍣?GUI锛? * - 涓よ 16 鑹叉贩鍑濆湡鑹叉澘閫?杈规/鍐呴儴棰滆壊"
 * - 鍖哄潡杈圭晫鏍峰紡锛氬鏁版牸 / 鍋舵暟鏍? * - 閾鸿澶у皬婊戝潡锛?~21 鍖哄潡锛夈€佸疄蹇冮摵璁惧紑鍏? * - 棰滆壊閮介€夊ソ鍚庡睍绀?3脳3 鍖哄潡鏍峰紡棰勮
 * - "寮€濮嬮摵璁?鎸夐挳锛氶渶棰滆壊閮介€夛紝鍚﹀垯鎻愮ず锛涜繍琛屼腑涓嶅彲鍐嶆寜
 */
public class BasePlacerScreen extends Screen {

    private static final int[] CONCRETE_RGB = {
            0xE9ECEC, 0xF07613, 0xBD44B3, 0x3AAFD9,
            0xF8C627, 0x70B919, 0xED8DAC, 0x3E4447,
            0x8E8E86, 0x158991, 0x792AAC, 0x35399D,
            0x724728, 0x546D1B, 0xB3312C, 0x1D1D21
    };

    private static final int SWATCH = 20;
    private static final int GAP = 4;

    private final BlockPos pos;
    private boolean running;
    private int borderColor = -1;
    private int interiorColor = -1;
    private int style = BaseBuildHandler.STYLE_ODD;
    private int size = 17;
    private boolean solid = false;
    private Button oddButton;
    private Button evenButton;
    private Button solidButton;
    private Button startButton;
    private Component status = Component.empty();

    public BasePlacerScreen(BlockPos pos, boolean running) {
        super(Component.translatable("title.iknow.base_placer"));
        this.pos = pos;
        this.running = running;
    }

    private int rowX() {
        return (this.width - (16 * SWATCH + 15 * GAP)) / 2;
    }

    @Override
    protected void init() {
        int midX = this.width / 2;
        this.oddButton = Button.builder(Component.translatable("button.iknow.style_odd"), b -> setStyle(BaseBuildHandler.STYLE_ODD))
                .bounds(midX - 80, 116, 78, 20).build();
        this.evenButton = Button.builder(Component.translatable("button.iknow.style_even"), b -> setStyle(BaseBuildHandler.STYLE_EVEN))
                .bounds(midX + 2, 116, 78, 20).build();
        this.addRenderableWidget(new SizeSlider(midX - 80, 154, 160, 16));
        this.solidButton = Button.builder(Component.translatable("button.iknow.solid_off"), b -> setSolid(!this.solid))
                .bounds(midX - 80, 174, 160, 20).build();
        this.startButton = Button.builder(Component.translatable("button.iknow.start"), this::onStart)
                .bounds(midX - 80, 200, 160, 20).build();
        this.addRenderableWidget(this.oddButton);
        this.addRenderableWidget(this.evenButton);
        this.addRenderableWidget(this.solidButton);
        this.addRenderableWidget(this.startButton);
        this.updateButtons();
    }

    private class SizeSlider extends AbstractSliderButton {
        SizeSlider(int x, int y, int w, int h) {
            super(x, y, w, h, Component.empty(), (BasePlacerScreen.this.size - 1) / 20.0);
        }

        @Override
        protected void updateMessage() {
            this.setMessage(Component.translatable("label.iknow.size").append(Component.literal(String.valueOf(sizeValue()))));
        }

        @Override
        protected void applyValue() {
            BasePlacerScreen.this.size = sizeValue();
        }

        private int sizeValue() {
            return 1 + (int) Math.round(this.value * 20.0);
        }
    }

    private void setStyle(int s) {
        this.style = s;
        this.updateButtons();
    }

    private void setSolid(boolean s) {
        this.solid = s;
        this.updateSolidButton();
    }

    private void onStart(Button button) {
        if (this.running) {
            this.status = Component.translatable("message.iknow.already_building");
            return;
        }
        if (this.borderColor < 0 || this.interiorColor < 0) {
            this.status = Component.translatable("message.iknow.select_color");
            return;
        }
        PacketDistributor.sendToServer(new BasePlacerStartPayload(this.pos, this.borderColor, this.interiorColor, this.style, this.size, this.solid));
        this.running = true;
        this.status = Component.translatable("message.iknow.base_building");
        this.updateButtons();
    }

    private void updateButtons() {
        this.oddButton.setMessage(Component.translatable(this.style == BaseBuildHandler.STYLE_ODD
                ? "button.iknow.style_odd_sel" : "button.iknow.style_odd"));
        this.evenButton.setMessage(Component.translatable(this.style == BaseBuildHandler.STYLE_EVEN
                ? "button.iknow.style_even_sel" : "button.iknow.style_even"));
        this.updateSolidButton();
        this.startButton.setMessage(Component.translatable(this.running
                ? "button.iknow.building" : "button.iknow.start"));
    }

    private void updateSolidButton() {
        this.solidButton.setMessage(Component.translatable(this.solid
                ? "button.iknow.solid_on" : "button.iknow.solid_off"));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (trySwatchClick(mouseX, mouseY, 40, true) || trySwatchClick(mouseX, mouseY, 80, false)) {
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean trySwatchClick(double mx, double my, int rowY, boolean border) {
        int x0 = rowX();
        for (int i = 0; i < 16; i++) {
            int x = x0 + i * (SWATCH + GAP);
            if (mx >= x && mx <= x + SWATCH && my >= rowY && my <= rowY + SWATCH) {
                if (border) {
                    this.borderColor = i;
                } else {
                    this.interiorColor = i;
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        int cx = this.width / 2;
        guiGraphics.drawCenteredString(this.font, this.title, cx, 14, 0xFFFFFFFF);
        this.drawRow(guiGraphics, 40, true);
        this.drawRow(guiGraphics, 80, false);
        guiGraphics.drawCenteredString(this.font, Component.translatable("label.iknow.style"), cx, 104, 0xFFAAAAAA);
        guiGraphics.drawCenteredString(this.font, this.status, cx, 226, 0xFFFFDD55);
        if (this.borderColor >= 0 && this.interiorColor >= 0) {
            this.drawPreview(guiGraphics, 240);
        }
    }

    private void drawRow(GuiGraphics guiGraphics, int rowY, boolean border) {
        int x0 = rowX();
        int selected = border ? this.borderColor : this.interiorColor;
        String labelKey = border ? "label.iknow.border_color" : "label.iknow.interior_color";
        guiGraphics.drawString(this.font, Component.translatable(labelKey), x0, rowY - 12, 0xFFAAAAAA);
        for (int i = 0; i < 16; i++) {
            int x = x0 + i * (SWATCH + GAP);
            int rgb = CONCRETE_RGB[i];
            guiGraphics.fill(x, rowY, x + SWATCH, rowY + SWATCH, 0xFF000000 | rgb);
            if (selected == i) {
                guiGraphics.fill(x - 1, rowY - 1, x + SWATCH + 1, rowY, 0xFFFFFFFF);
                guiGraphics.fill(x - 1, rowY + SWATCH, x + SWATCH + 1, rowY + SWATCH + 1, 0xFFFFFFFF);
                guiGraphics.fill(x - 1, rowY, x, rowY + SWATCH, 0xFFFFFFFF);
                guiGraphics.fill(x + SWATCH, rowY, x + SWATCH + 1, rowY + SWATCH, 0xFFFFFFFF);
            }
        }
    }

    private void drawPreview(GuiGraphics guiGraphics, int top) {
        int cell = 3;
        int size = 48 * cell;
        int left = (this.width - size) / 2;
        int borderRgb = 0xFF000000 | CONCRETE_RGB[this.borderColor];
        int interiorRgb = 0xFF000000 | CONCRETE_RGB[this.interiorColor];
        guiGraphics.drawCenteredString(this.font, Component.translatable("label.iknow.preview"), this.width / 2, top - 10, 0xFFAAAAAA);
        for (int gx = 0; gx < 48; gx++) {
            for (int gz = 0; gz < 48; gz++) {
                boolean boundary = isBoundary(gx, gz);
                guiGraphics.fill(left + gx * cell, top + gz * cell,
                        left + gx * cell + cell, top + gz * cell + cell,
                        boundary ? borderRgb : interiorRgb);
            }
        }
    }

    private boolean isBoundary(int x, int z) {
        if (this.style == BaseBuildHandler.STYLE_EVEN) {
            return (x & 15) == 0 || (x & 15) == 15 || (z & 15) == 0 || (z & 15) == 15;
        }
        return (x & 15) == 0 || (z & 15) == 0 || x == 0 || x == 47 || z == 0 || z == 47;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}

