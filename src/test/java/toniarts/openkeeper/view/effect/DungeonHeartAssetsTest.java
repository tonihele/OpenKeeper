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
package toniarts.openkeeper.view.effect;

import com.jme3.anim.AnimComposer;
import com.jme3.asset.*;
import com.jme3.asset.plugins.FileLocator;
import com.jme3.effect.*;
import com.jme3.math.Vector3f;
import com.jme3.scene.*;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import com.simsilica.es.base.DefaultEntityData;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import toniarts.openkeeper.tools.convert.KmfModelLoader;
import toniarts.openkeeper.tools.convert.map.*;
import toniarts.openkeeper.game.component.*;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.view.DungeonHeartIntroductionState;
import toniarts.openkeeper.animation.PoseTrack;
import toniarts.openkeeper.utils.AssetUtils;
import java.nio.FloatBuffer;
import static org.junit.jupiter.api.Assertions.*;

/** Optional integration checks: proprietary data is never required by the normal test suite. */
@EnabledIfEnvironmentVariable(named = "OPENKEEPER_DK2", matches = ".+")
class DungeonHeartAssetsTest {
    private static IKwdFile kwd;
    private static AssetManager assets;
    @BeforeAll static void load() {
        String root = Path.of(System.getenv("OPENKEEPER_DK2")).toAbsolutePath().toString() + "/";
        kwd = KwdFile.load(root, Path.of(root, "Data/editor/maps/level1.kwd"));
        assets = new DesktopAssetManager(true);
        assets.registerLocator(System.getenv().getOrDefault("OPENKEEPER_CONVERTED_ASSETS", "assets/Converted"), FileLocator.class);
    }
    @Test void nativeGraphAndHeartMetadataRemainCompatible() {
        assertEquals("Dungeon centre", kwd.getObject(13).getMeshResource().getName());
        assertFalse(kwd.getObject(13).getMeshResource().getFlags().contains(ArtResource.ArtResourceFlag.HAS_START_ANIMATION));
        assertEquals(224, kwd.getEffect(225).getDeathEffectId());
        assertEquals(223, kwd.getEffect(224).getNextEffectId());
        assertEquals(208, kwd.getEffect(224).getDeathEffectId());
        assertEquals(215, kwd.getEffect(210).getNextEffectId());
        Spatial heart = assets.loadModel("Models/Dungeon centre.j3o");
        List<AnimComposer> composers = new ArrayList<>();
        heart.depthFirstTraversal(s -> { var c = s.getControl(AnimComposer.class); if (c != null) composers.add(c); });
        assertEquals(1, composers.size());
        assertEquals(70.0 / 30, composers.getFirst().getAnimClip(KmfModelLoader.DUMMY_ANIM_CLIP_NAME).getLength(), 1e-5);
    }
    private static List<String> snapshot(double duration, boolean coarse) {
        Node root = new Node("test");
        try (var playback = new EffectPlayback(new EffectManagerState(kwd, assets), root, (short) 3, 42)) {
            playback.spawn(225, new Vector3f(0, 1, 0));
            int count = coarse ? 1 : (int) Math.round(duration / .01);
            for (int i = 0; i < count; i++) {
                double dt = duration / count;
                playback.advance(dt);
                root.updateLogicalState((float) dt); // must not advance managed controls again
                root.updateGeometricState();
            }
            List<String> state = new ArrayList<>();
            root.depthFirstTraversal(s -> {
                state.add(s.getName() + ":" + s.getLocalTransform());
                if (s instanceof ParticleEmitter e) {
                    for (Particle p : e.getParticles()) if (p.life > 0) {
                        state.add(p.position + ":" + p.velocity + ":" + p.life + ":" + p.angle
                                + ":" + p.rotateSpeed + ":" + p.imageIndex + ":" + p.color + ":" + p.size);
                    }
                }
            });
            if (duration == 8.5) {
                for (int id = 1; id <= 9; id++) {
                    String name = "DHeartPlug" + id;
                    assertTrue(state.stream().anyMatch(s -> s.startsWith(name + ":")), name);
                }
            }
            if (duration == 20) assertTrue(playback.isEmpty());
            return state;
        }
    }
    @Test void actualParticlesAndDebrisAreIndependentOfFramePartitioning() {
        for (double time : new double[]{1, 3, 6, 8.5, 20}) {
            assertEquals(snapshot(time, false), snapshot(time, true), "at " + time);
        }
    }

    private static List<String> presentationSnapshot(double delay, double age, boolean late) {
        var data = new DefaultEntityData();
        var room = data.createEntity();
        var intro = new DungeonHeartIntroduction(null, data.createEntity(), data.createEntity(),
                data.createEntity(), data.createEntity(), data.createEntity());
        intro.waitingStartTime = 1_000_000_000L;
        intro.startTime = intro.waitingStartTime + Math.round((delay + .25) * 1e9);
        intro.ticksPerSecond = 4;
        data.setComponents(room, intro, new Owner((short) 3, (short) 3),
                new RoomComponent((short) 5, false, new Point(10, 10)));
        var clock = new AtomicLong();
        Node root = new Node("presentation");
        var state = new DungeonHeartIntroductionState(data, (short) 3, root,
                new EffectManagerState(kwd, assets), id -> null, point -> true, clock::get);
        try {
            state.start();
            int steps = (int) Math.round((delay + .25 + age) * 100);
            for (int i = late ? steps : 0; i <= steps; i++) {
                clock.set(intro.waitingStartTime + i * 10_000_000L);
                state.update(late ? steps / 100f : .01f);
                root.updateLogicalState(late ? steps / 100f : .01f);
                root.updateGeometricState();
            }
            List<String> snapshot = new ArrayList<>();
            root.depthFirstTraversal(spatial -> {
                // Signed zero is not a visual difference; preserve all nonzero values.
                snapshot.add((spatial.getName() + ":" + spatial.getLocalTransform())
                        .replaceAll("-0\\.0(?=[,\\] )}:]|$)", "0.0"));
                if (spatial instanceof ParticleEmitter emitter) {
                    for (Particle p : emitter.getParticles()) if (p.life > 0) {
                        snapshot.add(p.position + ":" + p.velocity + ":" + p.life + ":" + p.angle
                                + ":" + p.rotateSpeed + ":" + p.imageIndex + ":" + p.color + ":" + p.size);
                    }
                }
            });
            return snapshot;
        } finally {
            state.cleanup();
            data.close();
        }
    }

    @Test void completePresentationIsIndependentOfTheFirstRenderedFrame() {
        for (double delay : new double[]{0, 15, 16}) {
            for (double age : new double[]{-.1, 0, 3, 8.5, 20}) {
                assertEquals(presentationSnapshot(delay, age, false), presentationSnapshot(delay, age, true),
                        "delay=" + delay + ", age=" + age);
            }
        }
    }

    @Test void representativeCreatureTracksRemainSeekableAcrossEndpointsAndRewinds() {
        for (var animation : new Creature.AnimationType[]{Creature.AnimationType.WALK,
                Creature.AnimationType.DIE}) {
            var art = kwd.getImp().getAnimation(animation);
            assertNotNull(art, animation.toString());
            Spatial model = AssetUtils.loadModel(assets, art.getName(), art);
            int[] tracks = {0};
            model.depthFirstTraversal(spatial -> {
                AnimComposer composer = spatial.getControl(AnimComposer.class);
                if (composer == null) return;
                for (var track : composer.getAnimClip(KmfModelLoader.DUMMY_ANIM_CLIP_NAME).getTracks()) {
                    if (!(track instanceof PoseTrack pose)) continue;
                    tracks[0]++;
                    double quarter = pose.getLength() / 4;
                    pose.getDataAtTime(quarter, null);
                    FloatBuffer positions = (FloatBuffer) pose.getTarget().getMesh().getBuffer(VertexBuffer.Type.Position).getData();
                    float[] expected = new float[positions.limit()];
                    for (int i = 0; i < expected.length; i++) expected[i] = positions.get(i);
                    pose.getDataAtTime(pose.getLength(), null);
                    pose.getDataAtTime(0, null);
                    pose.getDataAtTime(quarter, null);
                    for (int i = 0; i < expected.length; i++) {
                        assertTrue(Float.isFinite(positions.get(i)));
                        assertEquals(expected[i], positions.get(i), 1e-6, animation + " vertex " + i);
                    }
                }
            });
            assertTrue(tracks[0] > 0, animation.toString());
        }
    }
}
