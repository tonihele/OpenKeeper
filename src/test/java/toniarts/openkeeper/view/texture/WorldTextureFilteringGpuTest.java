/* Copyright (C) 2026 OpenKeeper. SPDX-License-Identifier: GPL-3.0-or-later */
package toniarts.openkeeper.view.texture;

import static org.junit.jupiter.api.Assertions.*;

import com.jme3.app.SimpleApplication;
import com.jme3.asset.TextureKey;
import com.jme3.material.Material;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Limits;
import com.jme3.scene.Geometry;
import com.jme3.scene.shape.Quad;
import com.jme3.system.AppSettings;
import com.jme3.system.JmeContext;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Image;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;
import com.jme3.texture.image.ColorSpace;
import com.jme3.util.BufferUtils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.lwjgl.opengl.EXTTextureFilterAnisotropic;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import toniarts.openkeeper.game.data.TextureFiltering;

import java.nio.ByteBuffer;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/** Actual mip allocation and live switching; run with -PgpuTests=true. */
@EnabledIfSystemProperty(named = "openkeeper.gpuTests", matches = "true")
class WorldTextureFilteringGpuTest {
    @Test
    void earlyAndLateImagesHaveRealMipLevelsAndLiveSwitchingRestoresTheFramebuffer()
            throws Exception {
        var app = new Probe();
        var settings = new AppSettings(true);
        settings.setRenderer(AppSettings.LWJGL_OPENGL3);
        settings.setResolution(128, 128);
        settings.setAudioRenderer(null);
        settings.setUseJoysticks(false);
        app.setSettings(settings);
        app.setShowSettings(false);
        try {
            app.start(JmeContext.Type.OffscreenSurface);
            app.completed.get(30, TimeUnit.SECONDS);
        } finally {
            app.stop(true);
        }
    }

    private static final class Probe extends SimpleApplication {
        final CompletableFuture<Void> completed = new CompletableFuture<>();

        @Override
        public void simpleInitApp() {
            try {
                verify();
                completed.complete(null);
            } catch (Throwable failure) {
                completed.completeExceptionally(failure);
            }
        }

        @Override
        public void handleError(String message, Throwable failure) {
            completed.completeExceptionally(failure);
            stop(false);
        }

        private void bind(Texture texture) throws Exception {
            renderer.setTexture(0, texture);
        }

        private static int mipWidth() {
            return GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 1, GL11.GL_TEXTURE_WIDTH);
        }

        private void verify() throws Exception {
            var policy =
                    new WorldTextureFiltering(
                            TextureFiltering.BILINEAR,
                            () -> renderer.getLimits().getOrDefault(Limits.TextureAnisotropy, 1));
            try {
                var late = WorldTextureFilteringTest.texture("Textures/late.png", 128, 128);
                bind(late);
                assertEquals(0, mipWidth());
                assertEquals(
                        0, GL11.glGetTexParameteri(GL11.GL_TEXTURE_2D, GL12.GL_TEXTURE_MAX_LEVEL));
                var uiImage = late.getImage();
                int uiId = uiImage.getId();
                var world = late.clone();
                policy.prepare(world);
                bind(world);
                assertEquals(64, mipWidth());
                assertNotSame(uiImage, world.getImage());
                assertEquals(uiId, uiImage.getId());
                assertFalse(uiImage.isGeneratedMipmapsRequired());
                bind(late);
                assertEquals(0, mipWidth());

                // An already managed Texture may receive a different, base-only uploaded Image.
                var replaced = WorldTextureFilteringTest.texture("Textures/replaced.png", 128, 128);
                policy.prepare(replaced);
                bind(replaced);
                replaced.setImage(uiImage);
                policy.prepare(replaced);
                assertSame(world.getImage(), replaced.getImage());
                bind(replaced);
                assertEquals(64, mipWidth());
                assertTrue(
                        GL11.glGetTexParameteri(GL11.GL_TEXTURE_2D, GL12.GL_TEXTURE_MAX_LEVEL) > 0);

                byte[] pixels = new byte[128 * 128 * 4];
                new Random(1024).nextBytes(pixels);
                var data = BufferUtils.createByteBuffer(pixels.length);
                data.put(pixels).flip();
                var early =
                        new Texture2D(
                                new Image(Image.Format.RGBA8, 128, 128, data, ColorSpace.Linear));
                early.setKey(new TextureKey("Textures/early.png", false));
                early.setWrap(Texture.WrapMode.Repeat);
                policy.prepare(early);
                bind(early);
                assertEquals(64, mipWidth());
                assertTrue(early.getImage().isMipmapsGenerated());
                int id = early.getImage().getId();
                var material = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
                material.setTexture("ColorMap", early);
                var quad = new Quad(2, 2);
                quad.getFloatBuffer(com.jme3.scene.VertexBuffer.Type.TexCoord)
                        .put(new float[] {0, 0, 16, 0, 16, 16, 0, 16})
                        .rewind();
                var geometry = new Geometry("sampling", quad);
                geometry.setMaterial(material);
                geometry.setLocalTranslation(-1, -1, 0);
                rootNode.attachChild(geometry);
                cam.setLocation(new Vector3f(0, 0, 3));
                cam.lookAt(Vector3f.ZERO, Vector3f.UNIT_Y);
                var target = new FrameBuffer(128, 128, 1);
                target.addColorTarget(FrameBuffer.FrameBufferTarget.newTarget(Image.Format.RGBA8));
                viewPort.setOutputFrameBuffer(target);
                rootNode.updateLogicalState(0);
                rootNode.updateGeometricState();
                byte[] originalFrame = renderFrame();
                for (var mode : TextureFiltering.values()) {
                    policy.refresh(mode);
                    assertFalse(
                            early.getImage().isUpdateNeeded(),
                            "Sampler changes must not upload pixels again");
                    bind(early);
                    assertEquals(id, early.getImage().getId());
                    assertEquals(64, mipWidth());
                    assertEquals(
                            mode == TextureFiltering.BILINEAR
                                    ? GL11.GL_LINEAR_MIPMAP_NEAREST
                                    : GL11.GL_LINEAR_MIPMAP_LINEAR,
                            GL11.glGetTexParameteri(
                                    GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER));
                    if (GL.getCapabilities().GL_EXT_texture_filter_anisotropic) {
                        assertEquals(
                                (float) policy.getEffectiveAnisotropy(),
                                GL11.glGetTexParameterf(
                                        GL11.GL_TEXTURE_2D,
                                        EXTTextureFilterAnisotropic.GL_TEXTURE_MAX_ANISOTROPY_EXT),
                                0.001f);
                    }
                    renderFrame();
                }
                policy.refresh(TextureFiltering.BILINEAR);
                assertArrayEquals(originalFrame, renderFrame());
                byte[] retained = new byte[pixels.length];
                early.getImage().getData(0).duplicate().rewind().get(retained);
                assertArrayEquals(pixels, retained);
                var before = BufferUtils.createByteBuffer(64 * 64 * 4);
                bind(early);
                GL11.glGetTexImage(
                        GL11.GL_TEXTURE_2D, 1, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, before);
                var changed = early.getImage().getData(0).duplicate().rewind();
                while (changed.hasRemaining()) {
                    changed.put((byte) 255).put((byte) 0).put((byte) 0).put((byte) 255);
                }
                early.getImage().setUpdateNeeded();
                bind(early);
                assertEquals(id, early.getImage().getId());
                assertEquals(64, mipWidth());
                var after = BufferUtils.createByteBuffer(before.capacity());
                GL11.glGetTexImage(
                        GL11.GL_TEXTURE_2D, 1, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, after);
                assertNotEquals(before, after, "The lower level must contain the updated pixels");
                while (after.hasRemaining()) {
                    assertEquals(255, Byte.toUnsignedInt(after.get()));
                    assertEquals(0, Byte.toUnsignedInt(after.get()));
                    assertEquals(0, Byte.toUnsignedInt(after.get()));
                    assertEquals(255, Byte.toUnsignedInt(after.get()));
                }
                assertEquals(GL11.GL_NO_ERROR, GL11.glGetError());
            } finally {
                policy.close();
            }
        }

        private byte[] renderFrame() {
            renderManager.renderViewPort(viewPort, 0);
            ByteBuffer frame = BufferUtils.createByteBuffer(128 * 128 * 4);
            renderer.readFrameBuffer(viewPort.getOutputFrameBuffer(), frame);
            byte[] bytes = new byte[frame.capacity()];
            frame.rewind().get(bytes);
            return bytes;
        }
    }
}
