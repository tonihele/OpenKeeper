/* Copyright (C) 2026 OpenKeeper. Licensed under GPL-3.0-or-later. */
package toniarts.openkeeper.view.filter;

import com.jme3.asset.AssetKey;
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
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import toniarts.openkeeper.tools.convert.map.Creature.GammaEffect;

/** DKII's channel ramps, applied after the world, view mask and HUD are composed. */
public final class PossessionGamma implements SceneProcessor {
    public static final String RAMPS = "Shaders/PossessionGamma/NativeRamps.bin";
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

    /** Decode the original 16-bit tables without applying texture gamma conversion. */
    public static ByteBuffer readRamps(InputStream input) throws IOException {
        byte[] bytes = input.readAllBytes();
        if (bytes.length != 16 * 256 * 3 * Short.BYTES) {
            throw new IOException("Invalid possession gamma table length: " + bytes.length);
        }
        var source = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        var pixels = BufferUtils.createByteBuffer(16 * 256 * 4 * Float.BYTES);
        for (int i = 0; i < 16 * 256; i++) {
            for (int channel = 0; channel < 3; channel++) {
                pixels.putFloat(Short.toUnsignedInt(source.getShort()) / 65535f);
            }
            pixels.putFloat(1f);
        }
        return pixels.flip();
    }

    @Override public void initialize(RenderManager manager, ViewPort viewport) {
        renderManager = manager;
        viewPort = viewport;
        var info = assets.locateAsset(new AssetKey<>(RAMPS));
        if (info == null) throw new IllegalStateException("Missing " + RAMPS);
        try (var input = info.openStream()) {
            ramps = new Texture2D(new Image(Image.Format.RGBA32F, 256, 16, readRamps(input), ColorSpace.Linear));
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read possession gamma tables", e);
        }
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
