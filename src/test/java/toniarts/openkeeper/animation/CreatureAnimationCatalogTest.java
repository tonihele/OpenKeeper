/* Copyright (C) 2026 OpenKeeper, licensed under GPL-3.0-or-later. */
package toniarts.openkeeper.animation;

import java.util.List;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.tools.convert.map.ArtResource;
import toniarts.openkeeper.tools.convert.map.Creature;
import static org.junit.jupiter.api.Assertions.*;

class CreatureAnimationCatalogTest {
    @Test void includesMapOnlySlotsAndSegmentsOnceAndHonorsPreload() {
        var creature = new Creature();
        var run = new ArtResource("run",ArtResource.ArtResourceType.ANIMATING_MESH);
        run.getFlags().addAll(java.util.EnumSet.of(ArtResource.ArtResourceFlag.PRELOAD,
                ArtResource.ArtResourceFlag.HAS_START_ANIMATION,ArtResource.ArtResourceFlag.HAS_END_ANIMATION));
        creature.getAnimations().put(Creature.AnimationType.RUN,run);
        creature.getAnimations().put(Creature.AnimationType.WALK,run);
        creature.getAnimations().put(Creature.AnimationType.IDLE_4,new ArtResource("idle",ArtResource.ArtResourceType.ANIMATING_MESH));
        assertEquals(java.util.Set.of("run","runStart","runEnd"),CreatureAnimationCatalog.preloadModels(List.of(creature)).keySet());
        assertTrue(CreatureAnimationCatalog.preloadModels(List.of()).isEmpty());
    }
}
