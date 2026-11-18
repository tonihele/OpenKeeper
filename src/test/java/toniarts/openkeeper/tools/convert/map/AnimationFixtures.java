/* Copyright (C) 2026 OpenKeeper, licensed under GPL-3.0-or-later. */
package toniarts.openkeeper.tools.convert.map;

public final class AnimationFixtures {
    private AnimationFixtures() { }
    public static ArtResource resource(String name, int frames, int fps) {
        var resource = new ArtResource(name, ArtResource.ArtResourceType.ANIMATING_MESH);
        resource.setData(ArtResource.KEY_FRAMES, frames); resource.setData(ArtResource.KEY_FPS, fps);
        return resource;
    }
}
