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
import com.jme3.effect.*;
import com.jme3.math.FastMath;
import com.jme3.scene.Node;
import toniarts.openkeeper.tools.convert.map.Effect;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class EffectPlaybackRandomTest {
    private static List<String> sample(long unrelatedSeed) {
        FastMath.rand.setSeed(unrelatedSeed);
        var emitter = new EffectParticleEmitter("test", ParticleMesh.Type.Triangle, 8, 0, 0, false, 0);
        emitter.setParticlesPerSec(0);
        emitter.setLowLife(1); emitter.setHighLife(2);
        emitter.setRotateSpeed(3); emitter.setRandomAngle(true);
        emitter.setSelectRandomImage(true); emitter.setImagesX(4);
        emitter.setPlaybackRandom(new Random(42));
        emitter.emitParticles(8); emitter.updateFromControl(.05f);
        List<String> result = new ArrayList<>();
        for (Particle p : emitter.getParticles()) {
            assertEquals(Math.round(p.startlife * 20), p.startlife * 20, 1e-5);
            result.add(p.life+":"+p.angle+":"+p.rotateSpeed+":"+p.imageIndex);
        }
        return result;
    }
    @Test void privateJmeBirthRandomnessCannotChangeTheSequence() {
        assertEquals(sample(1), sample(9090));
    }

    private static List<String> formation(boolean waiting) throws Exception {
        var formation = EffectPlaybackTest.effect(225, 10, 0, 0);
        EffectPlaybackTest.set(formation, "flags", EnumSet.of(Effect.EffectFlag.GENERATE_EFFECTS));
        EffectPlaybackTest.set(formation, "generateIds", List.of(902));
        EffectPlaybackTest.set(formation, "outerOriginRange", 4096);
        Node root = new Node();
        try (var playback = new EffectPlayback(EffectPlaybackTest.manager(formation,
                EffectPlaybackTest.effect(223, 1, 0, 0), EffectPlaybackTest.effect(902, 100, 0, 0)),
                root, (short) 3, 42)) {
            if (waiting) playback.spawn(223, null, -1);
            playback.advance(.25);
            playback.spawn(225, null);
            playback.advance(.1);
            var snapshot = new ArrayList<String>();
            root.getChild("effect-225").depthFirstTraversal(spatial -> snapshot.add(spatial.getLocalTransform().toString()));
            return snapshot;
        }
    }

    @Test void omittedWaitingCuesCannotChangeFormationRandomness() throws Exception {
        assertEquals(formation(true), formation(false));
    }
}
