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
        // 根据视角调整：让权杖在物品栏/手里呈舒适 3D 角度
        if (ctx == ItemDisplayContext.GUI || ctx == ItemDisplayContext.GROUND || ctx == ItemDisplayContext.FIXED) {
            pose.scale(0.5f, 0.5f, 0.5f);
            pose.translate(0.0f, -0.55f, 0.0f);
            pose.mulPose(Axis.XP.rotationDegrees(25));
            pose.mulPose(Axis.YP.rotationDegrees(-40));
        } else {
            pose.scale(0.9f, 0.9f, 0.9f);
            pose.translate(0.0f, -0.5f, 0.0f);
        }
        PoseStack.Pose poseEntry = pose.last();
        VertexConsumer vc = buffers.getBuffer(RenderType.entityTranslucentEmissive(WHITE));
        drawStaff(vc, poseEntry, overlay);
        pose.popPose();
    }

    private static void drawStaff(VertexConsumer vc, PoseStack.Pose p, int overlay) {
        // ---- 柄（八棱渐细圆柱）----
        float hb = -0.5f, ht = 0.15f, rb = 0.055f, rt = 0.04f;
        int dark = color(60, 62, 66, 255);
        for (int i = 0; i < N; i++) {
            float[] a = ring(i, hb, rb), b = ring((i + 1) % N, hb, rb),
                    c = ring((i + 1) % N, ht, rt), d = ring(i, ht, rt);
            addQuad(vc, p, a, b, c, d, dark, overlay);
        }
        // ---- 护环（青）----
        float g0 = 0.15f, g1 = 0.2f, gr = 0.072f;
        int cyan = color(44, 208, 222, 255);
        for (int i = 0; i < N; i++) {
            float[] a = ring(i, g0, gr), b = ring((i + 1) % N, g0, gr),
                    c = ring((i + 1) % N, g1, gr), d = ring(i, g1, gr);
            addQuad(vc, p, a, b, c, d, cyan, overlay);
        }
        // ---- 能量水晶头（下锥 青 → 上锥 亮青）----
        float cBase = 0.2f, cWaist = 0.3f, cApexY = 0.5f, rBase = 0.075f, rWaist = 0.1f;
        int glow = color(120, 228, 240, 255);
        int bright = color(225, 255, 255, 255);
        for (int i = 0; i < N; i++) {
            float[] a = ring(i, cBase, rBase), b = ring((i + 1) % N, cBase, rBase),
                    c = ring((i + 1) % N, cWaist, rWaist), d = ring(i, cWaist, rWaist);
            addQuad(vc, p, a, b, c, d, glow, overlay);
        }
        float[] apex = { 0f, cApexY, 0f };
        for (int i = 0; i < N; i++) {
            float[] a = ring(i, cWaist, rWaist), b = ring((i + 1) % N, cWaist, rWaist);
            addTri(vc, p, a, b, apex, bright, overlay);
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
