/*
 * Copyright (C) 2026 OpenKeeper
 *
 * OpenKeeper is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * OpenKeeper is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with OpenKeeper.  If not, see <http://www.gnu.org/licenses/>.
 */
package toniarts.openkeeper.tools.convert.map;

import com.jme3.asset.DesktopAssetManager;
import com.jme3.asset.plugins.FileLocator;
import com.jme3.scene.Node;
import com.jme3.scene.VertexBuffer;
import java.awt.image.BufferedImage;
import java.nio.FloatBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import toniarts.openkeeper.tools.modelviewer.UniversalArtResourceLoader;
import toniarts.openkeeper.view.effect.EffectGeometry;
import toniarts.openkeeper.view.effect.EffectManagerState;
import toniarts.openkeeper.view.effect.VisualEffect;
import static org.junit.jupiter.api.Assertions.*;

class SpriteEffectPlaybackTest {
    @TempDir Path assets;

    @Test
    void equalElapsedTimeSelectsEqualFramesAtDifferentRenderRates() {
        for (int count : new int[]{8, 20, 32}) {
            for (int seconds = 1; seconds <= 5; seconds++) {
                var frames = new HashSet<Integer>();
                for (int fps : new int[]{30, 60, 144}) {
                    var g = geometry(count);
                    for (int i = 0; i < seconds * fps; i++) {
                        g.updateLogicalState(1f / fps);
                    }
                    frames.add(frame(g));
                }
                assertEquals(1, frames.size(), count + " images after " + seconds + " seconds");
            }
        }
    }

    @Test
    void animationWaitsForAnEffectTickAndWraps() {
        var g = geometry(8);
        assertEquals(0, frame(g)); // The atlas is initialized before the first render.
        g.updateLogicalState(0.025f);
        assertEquals(0, frame(g));
        g.updateLogicalState(0.025f);
        assertEquals(1, frame(g));
        g.updateLogicalState(0.35f);
        assertEquals(0, frame(g));
    }

    @Test
    void nonDivisorFrameCountRetainsIntegerPhaseQuantization() {
        var g = geometry(20);
        g.updateLogicalState(0.05f);
        assertEquals(0, frame(g));
        g.updateLogicalState(0.05f);
        assertEquals(1, frame(g));
        g.updateLogicalState(0.9f);
        assertEquals(19, frame(g));
        g.updateLogicalState(0.05f);
        assertEquals(0, frame(g));
    }

    @Test
    void oneShotHoldsFinalFrameAndLeavesLifetimeToItsOwner() {
        var g = geometry(8);
        var parent = new Node();
        parent.attachChild(g);
        g.setLooping(false);
        g.updateLogicalState(0.35f);
        assertEquals(7, frame(g));
        g.updateLogicalState(100);
        assertEquals(7, frame(g));
        assertSame(parent, g.getParent());
    }

    @Test
    void longUpdateMatchesSmallUpdates() {
        var longFrame = geometry(20);
        var smallFrames = geometry(20);
        longFrame.updateLogicalState(12.5f);
        for (int i = 0; i < 250; i++) smallFrames.updateLogicalState(0.05f);
        assertEquals(frame(smallFrames), frame(longFrame));
    }

    @Test
    void pausedAndInvalidDeltasLeaveTheFrameUnchanged() {
        var g = geometry(8);
        g.updateLogicalState(0.1f);
        int before = frame(g);
        for (float delta : new float[]{0, -1, Float.NaN, Float.POSITIVE_INFINITY}) {
            g.updateLogicalState(delta);
            assertEquals(before, frame(g));
        }
    }

    @Test
    void randomStartAlwaysStaysInsideAtlasIncludingSingleFrame() {
        for (int count : new int[]{1, 8, 20}) {
            var g = geometry(count);
            var selected = new HashSet<Integer>();
            for (int i = 0; i < 200; i++) {
                g.setSelectRandomImage(true);
                selected.add(frame(g));
                assertTrue(frame(g) >= 0 && frame(g) < count);
                g.updateLogicalState(0.05f);
                assertTrue(frame(g) >= 0 && frame(g) < count);
            }
            assertEquals(count == 1, selected.size() == 1);
        }
    }

    @Test
    void singleFrameAndReconfigurationRemainValid() {
        var g = geometry(8);
        g.updateLogicalState(0.1f);
        g.setFrames(1);
        g.updateLogicalState(100);
        assertEquals(0, frame(g));
        var uv = (FloatBuffer) g.getMesh().getBuffer(VertexBuffer.Type.TexCoord).getData();
        assertEquals(1f, uv.get(2));
        g.setFrames(8);
        assertEquals(0, frame(g));
        g.updateLogicalState(0.05f);
        assertEquals(1, frame(g));
    }

    @Test
    void effectAndModelViewerApplyPlaybackFlagsForAllSpriteTypes() throws Exception {
        var manager = new DesktopAssetManager(true);
        manager.registerLocator(assets.toString(), FileLocator.class);
        Files.createDirectories(assets.resolve("Textures"));
        for (var type : List.of(ArtResource.ArtResourceType.SPRITE,
                ArtResource.ArtResourceType.ALPHA, ArtResource.ArtResourceType.ADDITIVE_ALPHA)) {
            String name = "syntheticPlayback" + type;
            for (int i = 0; i < 8; i++) {
                ImageIO.write(new BufferedImage(2, 2, BufferedImage.TYPE_INT_ARGB), "png",
                        assets.resolve("Textures/" + name + i + ".png").toFile());
            }
            var art = new ArtResource(name, type);
            art.setFlags(EnumSet.of(ArtResource.ArtResourceFlag.ANIMATING_TEXTURE,
                    ArtResource.ArtResourceFlag.DOESNT_LOOP));
            art.setData(ArtResource.KEY_FRAMES, 8);
            art.setData(ArtResource.KEY_WIDTH, 1);
            var definition = new Effect();
            definition.setName("Synthetic sprite");
            definition.setArtResource(art);
            definition.setFlags(EnumSet.noneOf(Effect.EffectFlag.class));
            definition.setGenerateIds(List.of());
            definition.setMinHp(200);
            definition.setMaxHp(200);
            var root = new Node();
            var effect = new VisualEffect(new EffectManagerState(null, manager), root, definition);
            var g = (EffectGeometry) ((Node) ((Node) root.getChild(0)).getChild(0)).getChild(0);
            var preview = (EffectGeometry) UniversalArtResourceLoader.load(manager, art);
            for (var sprite : List.of(g, preview)) {
                sprite.updateLogicalState(1);
                assertEquals(7, frame(sprite), type.toString());
            }
            effect.removeEffect();
            assertEquals(0, root.getQuantity());

            art.getFlags().add(ArtResource.ArtResourceFlag.RANDOM_START_FRAME);
            var effectStarts = new HashSet<Integer>();
            var previewStarts = new HashSet<Integer>();
            for (int i = 0; i < 64; i++) {
                var randomized = new VisualEffect(new EffectManagerState(null, manager), root, definition);
                var sprite = (EffectGeometry) ((Node) ((Node) root.getChild(0)).getChild(0)).getChild(0);
                effectStarts.add(frame(sprite));
                previewStarts.add(frame((EffectGeometry) UniversalArtResourceLoader.load(manager, art)));
                randomized.removeEffect();
            }
            assertTrue(effectStarts.size() > 1, type + " effect ignored random start");
            assertTrue(previewStarts.size() > 1, type + " preview ignored random start");
        }
    }

    private static EffectGeometry geometry(int frames) {
        var g = new EffectGeometry("synthetic");
        g.setFrames(frames);
        return g;
    }

    private static int frame(EffectGeometry g) {
        var uv = (FloatBuffer) g.getMesh().getBuffer(VertexBuffer.Type.TexCoord).getData();
        return Math.round(uv.get(0) * g.getFrames());
    }
}
