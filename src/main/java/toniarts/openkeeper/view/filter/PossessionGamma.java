/* Copyright (C) 2026 OpenKeeper. Licensed under GPL-3.0-or-later. */
package toniarts.openkeeper.view.filter;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.post.SceneProcessor;
import com.jme3.profile.AppProfiler;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.shape.Quad;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Image;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;
import com.jme3.texture.image.ColorSpace;
import com.jme3.util.BufferUtils;
import java.nio.ByteBuffer;
import toniarts.openkeeper.tools.convert.map.Creature.GammaEffect;

/** DKII's channel ramps, applied after the world, view mask and HUD are composed. */
public final class PossessionGamma implements SceneProcessor {
    private final AssetManager assets;
    private GammaEffect effect = GammaEffect.NORMAL;
    private RenderManager renderManager;
    private ViewPort viewPort;
    private Material material;
    private Texture2D captured;
    private Texture2D ramps;
    private FrameBuffer buffer;
    private Geometry quad;

    public PossessionGamma(AssetManager assets) { this.assets = assets; }

    /** The original builder has 16 cases; Ghost (16) has no defined ramp. */
    public static boolean isSupported(GammaEffect effect) {
        return effect != null && effect.getValue() >= 0 && effect.getValue() < 16;
    }

    public void setEffect(GammaEffect effect) {
        this.effect = isSupported(effect) ? effect : GammaEffect.NORMAL;
    }

    /** Build gamma-1 colour curves without shipping data extracted from the game. */
    static ByteBuffer createRamps() {
        var pixels = BufferUtils.createByteBuffer(16 * 256 * 4 * Float.BYTES);
        for (GammaEffect effect : GammaEffect.values()) {
            if (!isSupported(effect)) continue;
            for (int i = 0; i < 256; i++) {
                // Hardware ramps use i/256, so their last entry is below unity.
                double q = i / 256.0;
                switch (effect) {
                    case VAMPIRE_RED -> put(pixels, q, q * (1f / 3f), q * (1f / 3f));
                    case DARK_ELF_PURPLE -> put(pixels, q, q * (1f / 3f), q);
                    case SKELETON_BLACK_N_WHITE -> put(pixels, q / 2, q / 2, q * 1.5);
                    case SALAMANDER_INFRARED, MARTINS_TEST_2 -> put(pixels, q * 2, q, q);
                    case DARK_ANGER_BRIGHT_BLUE -> put(pixels, q, q, q * 4);
                    case WHITEOUT -> put(pixels, 1, 1, 1);
                    case BLACKOUT -> put(pixels, 0, 0, 0);
                    case REDOUT -> put(pixels, 1, 0, 0);
                    case DEATH_VIEW_HALF_RED -> put(pixels, q / 2, 0, 0);
                    case MARTINS_TEST_1 -> putSpectrum(pixels, q, 3, false);
                    case MARTINS_TEST_3 -> putSpectrum(pixels, q, 4, true);
                    case MARTINS_TEST_4 -> putSpectrum(pixels, q, 4, false);
                    case FREEZE_VIEW_ONLY_BLUE -> put(pixels, 0, 0, q * 1.5);
                    case MAIDEN -> put(pixels, q, q + 0.2, 0);
                    default -> put(pixels, q, q, q);
                }
            }
        }
        return pixels.flip();
    }

    /** Blue, green, then red/grey sine lobes for the experimental presets. */
    private static void putSpectrum(ByteBuffer pixels, double q, int bands, boolean swapRedGreen) {
        if (q < 1.0 / bands) {
            put(pixels, 0, 0, sine(q * bands));
        } else if (q < 2.0 / bands) {
            double middle = sine((q - 1.0 / bands) * bands);
            put(pixels, swapRedGreen ? middle : 0, swapRedGreen ? 0 : middle, 0);
        } else if (bands == 4 && q < 3.0 / bands) {
            double middle = sine((q - 2.0 / bands) * bands);
            put(pixels, swapRedGreen ? 0 : middle, swapRedGreen ? middle : 0, 0);
        } else {
            double grey = sine((q - (bands - 1.0) / bands) * bands / 2);
            put(pixels, grey, grey, grey);
        }
    }

    private static float sine(double phase) {
        return (float) Math.sin((float) Math.PI * phase);
    }

    private static void put(ByteBuffer pixels, double red, double green, double blue) {
        for (double channel : new double[]{red, green, blue}) {
            // Preserve the 16-bit truncation of the display ramps.
            pixels.putFloat((int) (Math.clamp(channel, 0, 1) * 65535) / 65535f);
        }
        pixels.putFloat(1f);
    }

    @Override public void initialize(RenderManager manager, ViewPort viewport) {
        renderManager = manager;
        viewPort = viewport;
        ramps = new Texture2D(new Image(Image.Format.RGBA32F, 256, 16, createRamps(), ColorSpace.Linear));
        nearest(ramps);
        material = new Material(assets, "Shaders/PossessionGamma/Gamma.j3md");
        material.setTexture("Ramps", ramps);
        reshape(viewport, viewport.getCamera().getWidth(), viewport.getCamera().getHeight());
    }

    private static void nearest(Texture texture) {
        texture.setMinFilter(Texture.MinFilter.NearestNoMipMaps);
        texture.setMagFilter(Texture.MagFilter.Nearest);
    }

    @Override public boolean isInitialized() { return renderManager != null; }

    @Override public void reshape(ViewPort viewport, int width, int height) {
        if (width <= 0 || height <= 0 || material == null) return;
        releaseCapture();
        // A linear half-float target preserves sRGB blit decoding without 8-bit
        // quantization. The shader recovers display values before the lookup.
        captured = new Texture2D(width, height, Image.Format.RGBA16F);
        nearest(captured);
        buffer = new FrameBuffer(width, height, 1);
        buffer.setColorTexture(captured);
        buffer.setSrgb(false);
        material.setTexture("Frame", captured);
        quad = new Geometry("Possession display gamma", new Quad(width, height));
        quad.setMaterial(material);
        quad.updateGeometricState();
    }

    @Override public void postFrame(FrameBuffer output) {
        if (effect == GammaEffect.NORMAL || material == null || buffer == null) return;
        var renderer = renderManager.getRenderer();
        // Copy the completed frame instead of changing viewport targets; existing
        // post-processors (including SSAO) retain their own framebuffer lifecycle.
        renderer.copyFrameBuffer(output, buffer, true, false);
        renderer.setFrameBuffer(output);
        material.setInt("Effect", effect.getValue());
        material.setBoolean("OutputSrgb", output == null ? renderer.isMainFrameBufferSrgb() : output.isSrgb());
        renderManager.setCamera(viewPort.getCamera(), true);
        renderManager.renderGeometry(quad);
        renderManager.setCamera(viewPort.getCamera(), false);
    }

    private void releaseCapture() {
        if (renderManager == null) return;
        if (buffer != null) renderManager.getRenderer().deleteFrameBuffer(buffer);
        if (captured != null) renderManager.getRenderer().deleteImage(captured.getImage());
        buffer = null;
        captured = null;
    }

    @Override public void cleanup() {
        releaseCapture();
        if (renderManager != null && ramps != null) renderManager.getRenderer().deleteImage(ramps.getImage());
        ramps = null;
        material = null;
        quad = null;
        renderManager = null;
        viewPort = null;
    }
    @Override public void preFrame(float tpf) { }
    @Override public void postQueue(RenderQueue queue) { }
    @Override public void setProfiler(AppProfiler profiler) { }
}
