package com.example.iknow.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * 多功能工具的自定义 3D 渲染器（BEWLR）：
 * 参数化绘制一把「科技能量权杖」——八棱渐细柄 + 护环 + 双锥能量水晶头。
 * 顶点按位置染色（柄=深灰、头=青→亮青渐变），全亮自发光。
 */
public class ToolRenderer extends BlockEntityWithoutLevelRenderer {

    private static final ResourceLocation WHITE = ResourceLocation.fromNamespaceAndPath("iknow", "textures/item/white.png");
    private static final int FULL_BRIGHT = 0xF000F0;
    private static final int N = 8; // 棱数（越大越平滑）

    public ToolRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext ctx, PoseStack pose,
                             MultiBufferSource buffers, int light, int overlay) {
        pose.pushPose();
        // 不再额外 scale/rotate：ItemRenderer 已按显示上下文定位（内置实体模型）。
        // 几何以原点为中心、约 1 单位高，让手握住权杖中部。
        PoseStack.Pose poseEntry = pose.last();
        VertexConsumer vc = buffers.getBuffer(RenderType.entityTranslucentEmissive(WHITE));
        drawStaff(vc, poseEntry, overlay);
        pose.popPose();
    }

    private static void drawStaff(VertexConsumer vc, PoseStack.Pose p, int overlay) {
        int N = 8;
        float yoff = 0.35f; // 手握点(原点)对准手柄下段，水晶朝上
        // ---- 柄（深色，较短的八棱柱）----
        float hb = -0.5f + yoff, ht = -0.06f + yoff, rb = 0.05f, rt = 0.045f;
        int dark = color(58, 60, 64, 255);
        for (int i = 0; i < N; i++) {
            float[] a = ring(i, hb, rb), b = ring((i + 1) % N, hb, rb),
                    c = ring((i + 1) % N, ht, rt), d = ring(i, ht, rt);
            addQuad(vc, p, a, b, c, d, dark, overlay);
        }
        // ---- 柄上两道发光能量环 ----
        int ringCyan = color(80, 226, 240, 255);
        for (int ri = 0; ri < 2; ri++) {
            float r0 = -0.40f + yoff + ri * 0.16f, r1 = r0 + 0.03f, rr = 0.055f;
            for (int i = 0; i < N; i++) {
                float[] a = ring(i, r0, rr), b = ring((i + 1) % N, r0, rr),
                        c = ring((i + 1) % N, r1, rr), d = ring(i, r1, rr);
                addQuad(vc, p, a, b, c, d, ringCyan, overlay);
            }
        }
        // ---- 护手（青）----
        float g0 = -0.06f + yoff, g1 = 0.03f + yoff, gr = 0.08f;
        int cyan = color(50, 210, 224, 255);
        for (int i = 0; i < N; i++) {
            float[] a = ring(i, g0, gr), b = ring((i + 1) % N, g0, gr),
                    c = ring((i + 1) % N, g1, gr), d = ring(i, g1, gr);
            addQuad(vc, p, a, b, c, d, cyan, overlay);
        }
        // ---- 能量水晶头（大而亮）----
        float cBase = 0.03f + yoff, cWaist = 0.25f + yoff, cApex = 0.5f + yoff, rBase = 0.09f, rWaist = 0.14f;
        int glow = color(90, 222, 240, 255);
        int bright = color(225, 255, 255, 255);
        for (int i = 0; i < N; i++) {
            float[] a = ring(i, cBase, rBase), b = ring((i + 1) % N, cBase, rBase),
                    c = ring((i + 1) % N, cWaist, rWaist), d = ring(i, cWaist, rWaist);
            addQuad(vc, p, a, b, c, d, glow, overlay);
        }
        for (int i = 0; i < N; i++) {
            float[] a = ring(i, cWaist, rWaist), b = ring((i + 1) % N, cWaist, rWaist);
            addTri(vc, p, a, b, new float[] { 0f, cApex, 0f }, bright, overlay);
        }
    }

    /** 生成圆环上某一点（绕 Y 轴） */
    private static float[] ring(int i, float y, float r) {
        float ang = (float) (2 * Math.PI * i / N - Math.PI / 2);
        return new float[] { (float) Math.cos(ang) * r, y, (float) Math.sin(ang) * r };
    }

    private static int color(int r, int g, int b, int a) {
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static void addQuad(VertexConsumer vc, PoseStack.Pose p,
            float[] a, float[] b, float[] c, float[] d, int col, int overlay) {
        vert(vc, p, a[0], a[1], a[2], col, overlay, 0, 0);
        vert(vc, p, b[0], b[1], b[2], col, overlay, 0, 1);
        vert(vc, p, c[0], c[1], c[2], col, overlay, 1, 1);
        vert(vc, p, d[0], d[1], d[2], col, overlay, 1, 0);
    }

    private static void addTri(VertexConsumer vc, PoseStack.Pose p,
            float[] a, float[] b, float[] c, int col, int overlay) {
        vert(vc, p, a[0], a[1], a[2], col, overlay, 0, 0);
        vert(vc, p, b[0], b[1], b[2], col, overlay, 1, 0);
        vert(vc, p, c[0], c[1], c[2], col, overlay, 1, 1);
    }

    private static void vert(VertexConsumer vc, PoseStack.Pose p,
            float x, float y, float z, int col, int overlay, float u, float v) {
        vc.addVertex(p, x, y, z)
                .setColor(col)
                .setUv(u, v)
                .setOverlay(overlay)
                .setLight(FULL_BRIGHT)
                .setNormal(p, 0, 1, 0);
    }
}
