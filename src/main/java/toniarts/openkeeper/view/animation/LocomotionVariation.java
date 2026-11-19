/* Copyright (C) 2026 OpenKeeper, licensed under GPL-3.0-or-later. */
package toniarts.openkeeper.view.animation;

import toniarts.openkeeper.tools.convert.map.ArtResource;
import toniarts.openkeeper.tools.convert.map.Creature.AnimationType;

/** Stateless presentation-only phase seed; never consumes gameplay RNG or changes clip speed. */
public final class LocomotionVariation {
    private LocomotionVariation() { }
    public static double offset(boolean enabled, long entity, long request, short creature, AnimationType action,
                                ArtResource art, AnimationControl.PlaybackPolicy policy, double wrapPeriod) {
        if (!enabled || policy != AnimationControl.PlaybackPolicy.LOOP || art == null
                || action != AnimationType.WALK && action != AnimationType.RUN && action != AnimationType.STEALTH_WALK
                || art.getFlags().contains(ArtResource.ArtResourceFlag.HAS_START_ANIMATION)
                || art.getFlags().contains(ArtResource.ArtResourceFlag.HAS_END_ANIMATION)
                || art.getFlags().contains(ArtResource.ArtResourceFlag.ANIMATING_TEXTURE)
                || !Double.isFinite(wrapPeriod) || wrapPeriod <= 0) return 0;
        long value = entity ^ Long.rotateLeft(request, 23) ^ ((long)creature << 48);
        value = (value ^ (value >>> 30)) * 0xbf58476d1ce4e5b9L;
        value = (value ^ (value >>> 27)) * 0x94d049bb133111ebL;
        value ^= value >>> 31;
        return (value >>> 11) * 0x1.0p-53 * wrapPeriod;
    }
}
