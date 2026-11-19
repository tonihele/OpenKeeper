/* Copyright (C) 2026 OpenKeeper, licensed under GPL-3.0-or-later. */
package toniarts.openkeeper.animation;

import java.util.List;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.tools.convert.map.ArtResource;
import toniarts.openkeeper.tools.convert.map.Creature;
import static org.junit.jupiter.api.Assertions.*;

class CreatureAnimationCatalogTest {
    @Test void deduplicatesAliasesPreservesRatesAndIncludesStaticAndExtraModels() throws Exception {
        var creature = new Creature();
        var slow = toniarts.openkeeper.tools.convert.map.AnimationFixtures.resource("shared",3,15);
        var alias = toniarts.openkeeper.tools.convert.map.AnimationFixtures.resource("SHARED",3,15);
        var fast = toniarts.openkeeper.tools.convert.map.AnimationFixtures.resource("shared",3,50);
        for (var art : List.of(slow,alias,fast)) art.getFlags().add(ArtResource.ArtResourceFlag.PRELOAD);
        creature.getAnimations().put(Creature.AnimationType.WALK,slow);
        creature.getAnimations().put(Creature.AnimationType.RUN,alias);
        creature.getAnimations().put(Creature.AnimationType.IDLE_1,fast);
        var pose = new ArtResource("pose",ArtResource.ArtResourceType.MESH); pose.getFlags().add(ArtResource.ArtResourceFlag.PRELOAD);
        creature.getAnimations().put(Creature.AnimationType.STAND_STILL,pose);
        var extra = new ArtResource("extra",ArtResource.ArtResourceType.MESH_COLLECTION); extra.getFlags().add(ArtResource.ArtResourceFlag.PRELOAD);
        var field = Creature.class.getDeclaredField("uniqueResource"); field.setAccessible(true); field.set(creature,extra);
        var requests = CreatureAnimationCatalog.preloadRequests(List.of(creature,creature));
        assertEquals(4,requests.size());
        assertEquals(2,requests.stream().filter(r->r.name().equalsIgnoreCase("shared")).count());
        assertTrue(requests.stream().anyMatch(r->r.resource() == pose));
        assertTrue(requests.stream().anyMatch(r->r.resource() == extra));
    }
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
