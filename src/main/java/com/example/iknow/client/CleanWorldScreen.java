package com.example.iknow.client;

import com.example.iknow.network.CleanNowPayload;
import com.example.iknow.network.CleanPausePayload;
import com.example.iknow.network.CleanStartPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * 浣犳兂瑕佸共鍑€鐨勪笘鐣?GUI锛? * - "娓呯悊鎺夎惤鐗?鎸夐挳 鈫?寮圭獥纭锛堜綘鐭ラ亾浣犲湪骞蹭粈涔堝悧锛熸垜鐭ラ亾/涓嶇煡閬擄級锛岄€?鎴戠煡閬?绔嬪嵆娓呯悊
 * - 瀹氭椂娓呯悊锛氳緭鍏ョ鏁帮紙鐜板疄鏃堕棿锛夆啋 纭閿佸畾 鈫?鍚姩 鈫?鏄剧ず鍊掕鏃讹紝鍙殢鏃舵殏鍋?缁х画
 */
public class CleanWorldScreen extends Screen {

    private static final long MS_PER_SEC = 1000L;

    private boolean running;
    private boolean paused;
    private long remainingMs;
    private long durationMs;
    private long lastTickMs;
    private boolean showingConfirm;
    private int lockedSeconds;

    private Button clearNowButton;
    private Button confirmButton;
    private Button startButton;
    private Button pauseButton;
    private Button knowButton;
    private Button dontKnowButton;
    private EditBox timeField;

    public CleanWorldScreen(int remainingSeconds, int durationSeconds, boolean running, boolean paused) {
        super(Component.translatable("title.iknow.clean_world"));
        this.running = running;
        this.paused = paused;
        this.remainingMs = remainingSeconds * MS_PER_SEC;
        this.durationMs = (durationSeconds > 0 ? durationSeconds : remainingSeconds) * MS_PER_SEC;
        this.lastTickMs = System.currentTimeMillis();
    }

    @Override
    public void resize(Minecraft mc, int width, int height) {
        super.resize(mc, width, height);
        // 缩放窗口时 MC 不重跑 init()，这里手动重建控件，使其跟随新的 width/height 布局
        this.rebuildWidgets();
    }

    @Override
    protected void init() {
        int midX = this.width / 2;
        this.clearNowButton = Button.builder(Component.translatable("button.iknow.clean_now"), b -> setConfirm(true))
                .bounds(midX - 80, 40, 160, 20).build();
        this.confirmButton = Button.builder(Component.translatable("button.iknow.confirm_time"), b -> {
            try {
                this.lockedSeconds = Math.max(1, Integer.parseInt(this.timeField.getValue().trim()));
                this.timeField.setValue(String.valueOf(this.lockedSeconds));
            } catch (NumberFormatException e) {
                this.lockedSeconds = 0;
            }
        }).bounds(midX - 80, 112, 160, 20).build();
        this.startButton = Button.builder(Component.translatable("button.iknow.start_clean"), b -> startTimer())
                .bounds(midX - 80, 140, 160, 20).build();
        this.pauseButton = Button.builder(Component.translatable("button.iknow.pause"), b -> togglePause())
                .bounds(midX - 80, 168, 160, 20).build();
        this.timeField = new EditBox(this.font, midX - 80, 84, 160, 20, Component.literal(""));
        this.timeField.setMaxLength(6);
        this.timeField.setValue("60");
        this.addRenderableWidget(this.clearNowButton);
        this.addRenderableWidget(this.confirmButton);
        this.addRenderableWidget(this.startButton);
        this.addRenderableWidget(this.pauseButton);
        this.addRenderableWidget(this.timeField);

        // 寮圭獥纭鎸夐挳锛堜粎寮圭獥鏃跺彲瑙侊級
        int cx = this.width / 2;
        int cy = this.height / 2;
        this.knowButton = Button.builder(Component.translatable("button.iknow.know"), b -> { setConfirm(false); sendCleanNow(); })
                .bounds(cx - 84, cy + 10, 78, 20).build();
        this.dontKnowButton = Button.builder(Component.translatable("button.iknow.dont_know"), b -> setConfirm(false))
                .bounds(cx + 6, cy + 10, 78, 20).build();
        this.addRenderableWidget(this.knowButton);
        this.addRenderableWidget(this.dontKnowButton);
        this.updateVisibility();
    }

    private void setConfirm(boolean show) {
        this.showingConfirm = show;
        this.updateVisibility();
    }

    private void updateVisibility() {
        this.clearNowButton.visible = !this.showingConfirm;
        this.confirmButton.visible = !this.showingConfirm;
        this.startButton.visible = !this.showingConfirm;
        this.pauseButton.visible = !this.showingConfirm;
        this.timeField.visible = !this.showingConfirm;
        this.knowButton.visible = this.showingConfirm;
        this.dontKnowButton.visible = this.showingConfirm;
    }

    private void startTimer() {
        int seconds = this.lockedSeconds > 0 ? this.lockedSeconds : parseField();
        if (seconds <= 0) {
            return;
        }
        PacketDistributor.sendToServer(new CleanStartPayload(seconds));
        this.lockedSeconds = seconds;
        this.running = true;
        this.paused = false;
        this.remainingMs = seconds * MS_PER_SEC;
        this.durationMs = seconds * MS_PER_SEC;
        this.lastTickMs = System.currentTimeMillis();
    }

    private int parseField() {
        try {
            return Math.max(0, Integer.parseInt(this.timeField.getValue().trim()));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void togglePause() {
        this.paused = !this.paused;
        this.lastTickMs = System.currentTimeMillis();
        PacketDistributor.sendToServer(new CleanPausePayload(this.paused));
        if (this.pauseButton != null) {
            this.pauseButton.setMessage(Component.translatable(this.paused ? "button.iknow.resume" : "button.iknow.pause"));
        }
    }

    private void sendCleanNow() {
        PacketDistributor.sendToServer(new CleanNowPayload());
    }

    @Override
    public void tick() {
        if (this.running && !this.paused) {
            long now = System.currentTimeMillis();
            long dt = now - this.lastTickMs;
            this.lastTickMs = now;
            this.remainingMs -= dt;
            if (this.remainingMs <= 0) {
                // 寰幆杩愯锛氶噸鏂板紑濮嬩竴杞€掕鏃?
        this.remainingMs = this.durationMs;
                if (this.remainingMs <= 0) {
                    this.running = false;
                }
            }
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        int cx = this.width / 2;
        guiGraphics.drawCenteredString(this.font, this.title, cx, 20, 0xFFFFFFFF);
        if (this.showingConfirm) {
            // 寮圭獥鏃堕殣钘忚儗鍚庣殑鐘舵€?鍊掕鏃?
        this.drawConfirmPopup(guiGraphics, cx);
        } else {
            guiGraphics.drawCenteredString(this.font, Component.translatable("label.iknow.clean_timer"), cx, 64, 0xFFAAAAAA);
            guiGraphics.drawCenteredString(this.font, Component.translatable(this.running ? "label.iknow.running" : "label.iknow.idle"),
                    cx, 196, 0xFFDDDDDD);
            if (this.running) {
                String time = formatTime(this.remainingMs);
                guiGraphics.drawCenteredString(this.font, Component.translatable("label.iknow.countdown").append(time),
                        cx, 216, this.remainingMs <= 30_000L ? 0xFFFF5555 : 0xFF66FF66);
            }
        }
    }

    private void drawConfirmPopup(GuiGraphics guiGraphics, int cx) {
        int w = 220;
        int h = 90;
        int y0 = this.height / 2 - h / 2;
        int x0 = cx - w / 2;
        guiGraphics.fill(x0, y0, x0 + w, y0 + h, 0xE0202028);
        guiGraphics.fill(x0, y0, x0 + w, y0 + 1, 0xFFFFFFFF);
        guiGraphics.fill(x0, y0, x0 + 1, y0 + h, 0xFFFFFFFF);
        guiGraphics.fill(x0 + w - 1, y0, x0 + w, y0 + h, 0xFFFFFFFF);
        guiGraphics.fill(x0, y0 + h - 1, x0 + w, y0 + h, 0xFFFFFFFF);
        guiGraphics.drawCenteredString(this.font, Component.translatable("dialog.iknow.confirm"), cx, y0 + 16, 0xFFFFFF55);
    }

    private static String formatTime(long ms) {
        long totalSec = Math.max(0, (ms + 999) / 1000);
        long mm = totalSec / 60;
        long ss = totalSec % 60;
        return String.format("%02d:%02d", mm, ss);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}

