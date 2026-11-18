/* Copyright (C) 2026 OpenKeeper, licensed under GPL-3.0-or-later. */
package toniarts.openkeeper.animation;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import toniarts.openkeeper.tools.convert.map.ArtResource;
import toniarts.openkeeper.tools.convert.map.Creature;

/** Enumerates real animation slots and their authored segments without reflective getters. */
public final class CreatureAnimationCatalog {
    private CreatureAnimationCatalog() {}

    public static Map<String, ArtResource> preloadModels(Collection<Creature> creatures) {
        Map<String,ArtResource> result = new LinkedHashMap<>();
        for (Creature creature : creatures) for (ArtResource resource : creature.getAnimations().values()) {
            if (resource == null || resource.getType() != ArtResource.ArtResourceType.ANIMATING_MESH
                    || !resource.getFlags().contains(ArtResource.ArtResourceFlag.PRELOAD)) continue;
            result.putIfAbsent(resource.getName(),resource);
            if (resource.getFlags().contains(ArtResource.ArtResourceFlag.HAS_START_ANIMATION)) result.putIfAbsent(resource.getName()+"Start",resource);
            if (resource.getFlags().contains(ArtResource.ArtResourceFlag.HAS_END_ANIMATION)) result.putIfAbsent(resource.getName()+"End",resource);
        }
        return result;
    }
}
