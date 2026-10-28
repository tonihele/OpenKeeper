/* SPDX-License-Identifier: GPL-3.0-or-later */
package toniarts.openkeeper.view.effect;

import com.jme3.effect.Particle;
import com.jme3.math.ColorRGBA;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AdditiveParticleColorTest {
    private static final ColorRGBA RED = new ColorRGBA(220 / 255f, 70 / 255f, 40 / 255f, 1f);

    @Test
    void redUsesNativeIntegerRgbFadeAndRetainsFullAlpha() {
        var color = new AdditiveParticleColor(RED, true);
        var particle = particle(32);
        color.update(particle, true);
        assertRgb(particle, 220, 70, 40);
        particle.life = 31 / 20f;
        color.update(particle, false);
        assertRgb(particle, 212, 67, 38); // channel * (remaining - 1) / remaining
        particle.life = 30 / 20f;
        color.update(particle, false);
        assertRgb(particle, 204, 64, 36);
    }

    @Test
    void substepFramesDoNotFadeAndDelayedFrameFadesOnlyOnce() {
        var color = new AdditiveParticleColor(RED, true);
        var particle = particle(32);
        color.update(particle, true);
        particle.life -= 0.025f;
        color.update(particle, false);
        assertRgb(particle, 220, 70, 40);
        particle.life = 24 / 20f; // Eight native steps in one update.
        color.update(particle, false);
        assertRgb(particle, 210, 67, 38);
        color.update(particle, false);
        assertRgb(particle, 210, 67, 38);
    }

    @Test
    void whiteDiscWithoutFadeStaysWhiteForItsWholeLife() {
        var color = new AdditiveParticleColor(ColorRGBA.White, false);
        var particle = particle(122);
        color.update(particle, true);
        particle.life = 1 / 20f;
        color.update(particle, false);
        assertRgb(particle, 255, 255, 255);
    }

    @Test
    void dimChannelsWaitUntilRemainingLifeReachesTheLargestChannel() {
        var color = new AdditiveParticleColor(new ColorRGBA(2 / 255f, 1 / 255f, 0, 1), true);
        var particle = particle(10);
        particle.life = 3 / 20f;
        color.update(particle, true);
        assertRgb(particle, 2, 1, 0);
        particle.life = 2 / 20f;
        color.update(particle, false);
        assertRgb(particle, 1, 0, 0);
    }

    @Test
    void reusedParticleStartsWithTheAuthoredColourAgain() {
        var color = new AdditiveParticleColor(RED, true);
        var particle = particle(32);
        particle.life = 1 / 20f;
        color.update(particle, true);
        assertRgb(particle, 0, 0, 0);
        particle.life = particle.startlife;
        color.update(particle, true);
        assertRgb(particle, 220, 70, 40);
    }

    private static Particle particle(int steps) {
        var particle = new Particle();
        particle.startlife = particle.life = steps / 20f;
        return particle;
    }

    private static void assertRgb(Particle particle, int r, int g, int b) {
        assertEquals(r / 255f, particle.color.r, 0.000001f);
        assertEquals(g / 255f, particle.color.g, 0.000001f);
        assertEquals(b / 255f, particle.color.b, 0.000001f);
        assertEquals(1f, particle.color.a);
    }
}
