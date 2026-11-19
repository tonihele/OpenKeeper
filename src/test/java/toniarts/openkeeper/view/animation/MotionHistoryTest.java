/* Copyright (C) 2026 OpenKeeper, licensed under GPL-3.0-or-later. */
package toniarts.openkeeper.view.animation;

import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.*;
import toniarts.openkeeper.tools.convert.map.Creature.AnimationType;
import static org.junit.jupiter.api.Assertions.*;

class MotionHistoryTest {
    private CreatureMotion point(double time, long sequence, long epoch, float x, float yaw, AnimationType state) {
        var p = new Position(yaw, new Vector3f(x, 0, 0)); p.motionEpoch = epoch;
        return new CreatureMotion(time, .1, sequence, p, new CreatureViewState((short)1, 0, state, sequence), false);
    }
    @Test void rootsYawAndHistoricalRequestUseOneTimeWithoutExtrapolation() {
        var history = new MotionHistory(4); var store = new Transform();
        history.add(point(1, 1, 5, 1, (float)Math.toRadians(179), AnimationType.WALK));
        history.add(point(2, 2, 5, 2, (float)Math.toRadians(-179), AnimationType.STAND_STILL));
        assertTrue(history.sample(1.5, store)); assertEquals(1.5, store.getTranslation().x, 1e-6);
        assertEquals(-1, store.getRotation().mult(Vector3f.UNIT_Z).z, 1e-4);
        assertEquals(AnimationType.WALK, history.getSampledRequest().state);
        history.sample(9, store); assertEquals(2, store.getTranslation().x); assertEquals(2, history.getSampledTime());
        assertEquals(AnimationType.STAND_STILL, history.getSampledRequest().state);
        history.sample(0, store); assertEquals(2, history.getSampledTime());
    }
    @Test void cutsFlushOldRootsAndCallerMutationCannotChangeHistory() {
        var history = new MotionHistory(2); var store = new Transform();
        history.add(point(1, 1, 5, 1, 0, AnimationType.WALK));
        var next = point(2, 2, 6, 20, 0, AnimationType.WALK); history.add(next);
        next.x = -99; next.request.state = AnimationType.DIE;
        history.sample(1.5, store); assertEquals(20, store.getTranslation().x);
        assertEquals(AnimationType.WALK, history.getSampledRequest().state);
        assertFalse(history.add(point(1.5, 1, 5, 10, 0, AnimationType.WALK)));
        history.clear(); assertFalse(history.sample(2, store));
        assertTrue(history.add(point(0, 0, 1, 0, 0, AnimationType.WALK)));
    }
    @Test void ringEvictsAndInvalidOrOutOfOrderPacketsCannotPoisonIt() {
        var history = new MotionHistory(2); var store = new Transform();
        for (int i=0;i<10;i++) assertTrue(history.add(point(i, i, 1, i, 0, AnimationType.WALK)));
        history.sample(0, store); assertEquals(8, store.getTranslation().x);
        assertFalse(history.add(point(7, 20, 1, 7, 0, AnimationType.WALK)));
        var invalid = point(10, 10, 1, Float.NaN, 0, AnimationType.WALK);
        assertFalse(history.add(invalid)); assertFalse(history.add(null));
        history.sample(10, store); assertEquals(9, store.getTranslation().x);
    }
    @Test void constantPathVelocityErrorFallsAtHighRefreshWithoutMoreThanOneTickLatency() {
        for (int hz : new int[]{60, 120, 144}) {
            var history = new MotionHistory(8); var store = new Transform();
            double previous = 0, previousStepped = 0, smoothError = 0, steppedError = 0;
            int nextTick = 0; double interval = 1.0 / 20;
            for (int frame=0;frame<hz*3;frame++) {
                double now = (double)frame/hz;
                while (nextTick*interval <= now) {
                    double time = nextTick*interval;
                    var sample = point(time, nextTick, 1, (float)time, 0, AnimationType.WALK); sample.interval = interval;
                    history.add(sample); nextTick++;
                }
                history.sample(now-interval, store);
                double current = store.getTranslation().x, stepped = (nextTick-1)*interval;
                if (frame > hz) {
                    smoothError += Math.pow((current-previous)*hz-1, 2);
                    steppedError += Math.pow((stepped-previousStepped)*hz-1, 2);
                    assertEquals(interval, now-history.getSampledTime(), 1e-8);
                }
                previous=current; previousStepped=stepped;
            }
            assertTrue(smoothError < steppedError*.5, "Hz="+hz+" errors="+smoothError+"/"+steppedError);
        }
    }
    @Test void integrationPreservesEpochAndPlacementsCutContinuity() {
        Position a = new Position(0, new Vector3f());
        Position b = Position.integrated(a, 0, 1, 0, 0);
        Position placement = new Position(0, new Vector3f());
        assertEquals(a.motionEpoch, b.motionEpoch); assertNotEquals(a.motionEpoch, placement.motionEpoch);
        assertNotSame(a.position, b.position);
    }
}
