/* Copyright (C) 2026 OpenKeeper, licensed under GPL-3.0-or-later. */
package toniarts.openkeeper.animation;

import com.jme3.asset.DesktopAssetManager;
import com.jme3.asset.plugins.FileLocator;
import java.lang.management.ManagementFactory;
import java.nio.file.Path;
import java.util.*;
import toniarts.openkeeper.tools.convert.map.*;
import toniarts.openkeeper.utils.AssetUtils;

/** Paired warmed model-clone/setup workload; does not measure GPU preloading or full startup. */
public final class CreaturePreloadBenchmark {
    public static void main(String[] args) throws Exception {
        var catalog = KwdFile.load(args[0],Path.of(args[0],"Data","Editor","Maps","Alcatraz.kwd"));
        var assets = new DesktopAssetManager(true); assets.registerLocator(args[1],FileLocator.class);
        var before = new ArrayList<CreatureAnimationCatalog.ModelPreload>();
        var oldTyped = new LinkedHashMap<String,ArtResource>();
        for (var creature : catalog.getCreatureList()) {
            for (var method : Creature.class.getMethods()) if(method.getReturnType() == ArtResource.class && method.getParameterCount() == 0) {
                var art = (ArtResource)method.invoke(creature);
                if(model(art)) before.add(new CreatureAnimationCatalog.ModelPreload(art.getName(),art));
            }
            for (var art : creature.getAnimations().values()) if(model(art) && art.getType() == ArtResource.ArtResourceType.ANIMATING_MESH) {
                oldTyped.putIfAbsent(art.getName(),art);
                if(art.getFlags().contains(ArtResource.ArtResourceFlag.HAS_START_ANIMATION)) oldTyped.putIfAbsent(art.getName()+"Start",art);
                if(art.getFlags().contains(ArtResource.ArtResourceFlag.HAS_END_ANIMATION)) oldTyped.putIfAbsent(art.getName()+"End",art);
            }
        }
        oldTyped.forEach((name,art)->before.add(new CreatureAnimationCatalog.ModelPreload(name,art)));
        var after = CreatureAnimationCatalog.preloadRequests(catalog.getCreatureList());
        for (var requests : List.of(before,after)) for (var request : requests) AssetUtils.loadModel(assets,request.name(),request.resource());
        var allocation = (com.sun.management.ThreadMXBean)ManagementFactory.getThreadMXBean();
        for (int mode=0;mode<2;mode++) {
            var requests = mode == 0 ? before : after; long[] times = new long[7]; long bytes = 0;
            for (int run=0;run<7;run++) {
                long initialBytes = allocation.getThreadAllocatedBytes(Thread.currentThread().threadId()), start = System.nanoTime();
                for (var request : requests) AssetUtils.loadModel(assets,request.name(),request.resource());
                times[run] = System.nanoTime()-start;
                bytes += allocation.getThreadAllocatedBytes(Thread.currentThread().threadId())-initialBytes;
            }
            Arrays.sort(times);
            System.out.printf(Locale.ROOT,"PRELOAD mode=%s requests=%d median_ms=%.3f allocated_bytes_per_pass=%d GPU_EXCLUDED%n",mode == 0 ? "old-double-pass" : "policy-aware",requests.size(),times[3]/1e6,bytes/7);
        }
    }
    private static boolean model(ArtResource art) {
        return art != null && art.getFlags().contains(ArtResource.ArtResourceFlag.PRELOAD) && switch(art.getType()) {
            case MESH, MESH_COLLECTION, ANIMATING_MESH, PROCEDURAL_MESH -> true;
            default -> false;
        };
    }
}
