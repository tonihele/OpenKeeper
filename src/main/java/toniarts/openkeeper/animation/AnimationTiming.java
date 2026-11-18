/* Copyright (C) 2026 OpenKeeper, licensed under GPL-3.0-or-later. */
package toniarts.openkeeper.animation;

import toniarts.openkeeper.tools.convert.map.ArtResource;

/** Shared metadata interpretation. Simulation never needs to load a mesh. */
public final class AnimationTiming {
    public static final double SOURCE_FPS = 30;
    private AnimationTiming() { }

    public static double framesPerSecond(ArtResource resource) {
        Number fps = resource == null ? null : resource.getData(ArtResource.KEY_FPS);
        return fps != null && Double.isFinite(fps.doubleValue()) && fps.doubleValue() > 0
                ? fps.doubleValue() : SOURCE_FPS;
    }

    public static double duration(ArtResource resource, double fallback) {
        if (resource == null || resource.getType() == ArtResource.ArtResourceType.NONE) return fallback;
        Number frames = resource.getData(ArtResource.KEY_FRAMES);
        return frames != null && Double.isFinite(frames.doubleValue()) && frames.doubleValue() > 0
                ? frames.doubleValue() / framesPerSecond(resource) : fallback;
    }
}
