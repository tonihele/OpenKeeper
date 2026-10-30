package toniarts.openkeeper.game.sound;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SoundThrottleTest {

    private static final int THROTTLED = 0xE5;
    private static final int NOT_THROTTLED = 0x1C5;

    private SoundThrottle throttle;

    @BeforeEach
    void setUp() {
        throttle = new SoundThrottle();
    }

    @Test
    void knowsItsEvents() {
        assertTrue(SoundThrottle.isThrottled(THROTTLED));
        assertTrue(SoundThrottle.isThrottled(0x30F));
        assertFalse(SoundThrottle.isThrottled(NOT_THROTTLED));
        assertFalse(SoundThrottle.isThrottled(0x1C5));
    }

    @Test
    void isInertAtNormalFrameRates() {
        throttle.tick(0);
        throttle.tick(20);

        for (int i = 0; i < 100; i++) {
            assertTrue(throttle.allow(THROTTLED));
        }
    }

    @Test
    void dropsMostRequestsOfASlowFrame() {
        // a 400 ms tick is slow but not very slow
        throttle.tick(0);
        throttle.tick(400);

        int allowed = 0;
        for (int i = 0; i < 100; i++) {
            if (throttle.allow(THROTTLED)) {
                allowed++;
            }
        }

        assertTrue(allowed > 0, "the first request of a slow frame must pass");
        assertTrue(allowed < 20, "a slow frame must drop most requests, but allowed " + allowed);
    }

    @Test
    void dropsEverythingOfAVerySlowFrame() {
        // a 900 ms tick
        throttle.tick(0);
        throttle.tick(900);

        for (int i = 0; i < 100; i++) {
            assertFalse(throttle.allow(THROTTLED));
        }
    }

    @Test
    void recoversOnTheNextNormalTick() {
        throttle.tick(0);
        throttle.tick(900);
        assertFalse(throttle.allow(THROTTLED));

        throttle.tick(1900);
        throttle.tick(1920);

        assertTrue(throttle.allow(THROTTLED));
    }

    @Test
    void neverThrottlesWhenDisabled() {
        throttle.setEnabled(false);
        throttle.tick(0);
        throttle.tick(900);

        for (int i = 0; i < 100; i++) {
            assertTrue(throttle.allow(THROTTLED));
        }
    }
}