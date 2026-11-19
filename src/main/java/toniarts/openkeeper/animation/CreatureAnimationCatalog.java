/* Copyright (C) 2026 OpenKeeper, licensed under GPL-3.0-or-later. */
package toniarts.openkeeper.animation;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.List;
import java.util.Set;
import java.util.Locale;
import toniarts.openkeeper.tools.convert.map.ArtResource;
import toniarts.openkeeper.tools.convert.map.Creature;

/** Enumerates creature model requests once, preserving distinct resource policies. */
public final class CreatureAnimationCatalog {
    private CreatureAnimationCatalog() {}

    public record ModelPreload(String name, ArtResource resource) { }
    private record Key(String name, ArtResource.ArtResourceType type, Set<ArtResource.ArtResourceFlag> flags, Map<String,Number> data) { }

    public static List<ModelPreload> preloadRequests(Collection<Creature> creatures) {
        Map<Key,ModelPreload> result = new LinkedHashMap<>();
        for (Creature creature : creatures) {
            for (ArtResource resource : creature.getAnimations().values()) add(result,resource);
            // Preserve the non-animation resources found by the old reflective pass.
            for (ArtResource resource : new ArtResource[]{creature.getIcon1Resource(),creature.getIcon2Resource(),
                    creature.getPortraitResource(),creature.getFirstPersonFilterResource(),creature.getFirstPersonMeleeResource(),creature.getUniqueResource()})
                add(result,resource);
        }
        return List.copyOf(result.values());
    }

    private static void add(Map<Key,ModelPreload> result, ArtResource resource) {
        if (resource == null || !resource.getFlags().contains(ArtResource.ArtResourceFlag.PRELOAD)) return;
        switch(resource.getType()) {
            case ANIMATING_MESH, MESH, MESH_COLLECTION, PROCEDURAL_MESH -> { }
            default -> { return; }
        }
        var data = new java.util.HashMap<String,Number>();
        for(String key : resource.getDataKeys()) data.put(key,resource.getData(key));
        var flags = Set.copyOf(resource.getFlags()); var metadata = Map.copyOf(data);
        for (String suffix : new String[]{"","Start","End"}) {
            if (suffix.equals("Start") && !flags.contains(ArtResource.ArtResourceFlag.HAS_START_ANIMATION)
                    || suffix.equals("End") && !flags.contains(ArtResource.ArtResourceFlag.HAS_END_ANIMATION)) continue;
            String name = resource.getName()+suffix;
            result.putIfAbsent(new Key(name.toLowerCase(Locale.ROOT),resource.getType(),flags,metadata),new ModelPreload(name,resource));
        }
    }

    /** Names-only compatibility/inventory view; rendering uses policy-aware requests. */
    public static Map<String, ArtResource> preloadModels(Collection<Creature> creatures) {
        Map<String,ArtResource> result = new LinkedHashMap<>();
        for (var request : preloadRequests(creatures)) result.putIfAbsent(request.name(),request.resource());
        return result;
    }
}
