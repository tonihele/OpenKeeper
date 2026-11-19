/* Copyright (C) 2026 OpenKeeper, licensed under GPL-3.0-or-later. */
package toniarts.openkeeper.animation;

import com.jme3.anim.AnimComposer;
import com.jme3.asset.DesktopAssetManager;
import com.jme3.asset.plugins.FileLocator;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.VertexBuffer;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import toniarts.openkeeper.tools.convert.KmfModelLoader;
import toniarts.openkeeper.tools.convert.map.ArtResource;
import toniarts.openkeeper.view.animation.AnimationLoader;

/** Repeatable CPU-only workload; deliberately makes no FPS/GPU or whole-game claims. */
public final class AnimationCrowdBenchmark {
    private static final String[] DEFAULT_NAMES = {"Arc-Walk","BIL-Walkback","Electrocute1"};
    private static final int WARMUP = 180, SAMPLES = 600;
    private static final com.sun.management.ThreadMXBean ALLOCATIONS =
            (com.sun.management.ThreadMXBean)ManagementFactory.getThreadMXBean();

    public static void main(String[] args) {
        if (args.length < 1 || args.length > 3) throw new IllegalArgumentException("Expected converted asset root, optional runs and comma-separated model names");
        int runs = args.length > 1 ? Integer.parseInt(args[1]) : 1;
        if (runs < 1 || runs > 20) throw new IllegalArgumentException("Runs must be 1..20");
        String[] names = DEFAULT_NAMES;
        if (args.length > 2) {
            names = args[2].split(",", -1);
            if (names.length == 0 || Arrays.stream(names).anyMatch(String::isBlank)) throw new IllegalArgumentException("Empty model name");
        }
        var assets = new DesktopAssetManager(true);
        assets.registerLocator(args[0],FileLocator.class);
        Spatial[] templates = Arrays.stream(names).map(n->assets.loadModel("Models/"+n+".j3o")).toArray(Spatial[]::new);
        System.out.println("CPU_ONLY java="+System.getProperty("java.version")+" warmup="+WARMUP+" samples="+SAMPLES);
        System.out.println("mode,creatures,tracks,vertices,median_ms,p95_ms,p99_ms,allocated_bytes_per_frame,dirty_bytes_per_frame");
        for (int repeat=0;repeat<runs;repeat++) {
        System.out.println("CPU_RUN index="+repeat+" models="+String.join(";",names));
        for (int count : new int[]{1,50,200,250,500,1000}) {
            for (String mode : new String[]{"visible","hidden","unchanged","owner-visible","owner-hidden","owner-unchanged"}) run(templates,names,count,mode);
        }
        for(int count : new int[]{50,200,500}) for(int capacity : new int[]{0,3}) {
            Node[] owners = new Node[count]; Arrays.setAll(owners,i->new Node());
            if(capacity > 0) for(Node owner : owners) owner.addControl(new toniarts.openkeeper.view.animation.PreparedAnimationCache(capacity,256*1024));
            // Warm path caches, then measure two complete transition bursts.
            for(String name : names) AnimationLoader.playAnimation(new Node(),new ArtResource(name,ArtResource.ArtResourceType.ANIMATING_MESH),assets);
            for(String name : names) for(Node owner : owners)
                AnimationLoader.playAnimation(owner,new ArtResource(name,ArtResource.ArtResourceType.ANIMATING_MESH),assets);
            long allocation = allocated(), start = System.nanoTime(); int replacements = 0;
            for(int burst=0;burst<6;burst++) for(Node owner : owners) {
                var result = AnimationLoader.playAnimation(owner,new ArtResource(names[burst%names.length],ArtResource.ArtResourceType.ANIMATING_MESH),assets);
                if(result != AnimationLoader.PlaybackResult.STARTED) throw new AssertionError(result);
                replacements++;
            }
            long resident = capacity == 0 ? 0 : Arrays.stream(owners).mapToLong(n->n.getControl(toniarts.openkeeper.view.animation.PreparedAnimationCache.class).getResidentPositionBytes()).sum();
            System.out.printf(java.util.Locale.ROOT,"TRANSITIONS creatures=%d cache_clips=%d replacements=%d ms=%.3f allocated_bytes=%d retained_position_bytes=%d%n",
                    count,capacity,replacements,(System.nanoTime()-start)/1e6,allocated()-allocation,resident);
        }
        }
    }

    private static void run(Spatial[] templates, String[] names, int count, String mode) {
        Node crowd = new Node(); List<PoseTrack> tracks = new ArrayList<>();
        for(int i=0;i<count;i++) {
            Spatial instance = templates[i%templates.length].clone(); crowd.attachChild(instance);
            if (mode.startsWith("owner-")) {
                // Include the production owner timeline, separate from the historical raw-composer baseline.
                instance.removeFromParent(); var owner = new Node(); crowd.attachChild(owner);
                Spatial sample = instance;
                var result = AnimationLoader.playAnimation(owner,new ArtResource(names[i%names.length],ArtResource.ArtResourceType.ANIMATING_MESH),(name,art)->sample,false);
                if (result != AnimationLoader.PlaybackResult.STARTED) throw new AssertionError(result);
                instance = owner;
            }
            instance.depthFirstTraversal(node->{
                var composer = node.getControl(AnimComposer.class);
                if(composer != null) {
                    if(!mode.startsWith("owner-")) composer.setCurrentAction(KmfModelLoader.DUMMY_ANIM_CLIP_NAME);
                    for(var clip : composer.getAnimClips()) for(var track : clip.getTracks()) if(track instanceof PoseTrack pose) tracks.add(pose);
                }
            });
        }
        if(mode.endsWith("hidden")) crowd.setCullHint(Spatial.CullHint.Always);
        float tpf = mode.endsWith("unchanged") ? 0 : 1f/60;
        for(int frame=0;frame<WARMUP;frame++) crowd.updateLogicalState(tpf);
        for(PoseTrack track : tracks) track.getTarget().getMesh().getBuffer(VertexBuffer.Type.Position).clearUpdateNeeded();
        long[] times = new long[SAMPLES]; long dirty = 0, allocation = allocated();
        for(int frame=0;frame<SAMPLES;frame++) {
            long start = System.nanoTime(); crowd.updateLogicalState(tpf); times[frame]=System.nanoTime()-start;
            for(PoseTrack track : tracks) {
                VertexBuffer buffer = track.getTarget().getMesh().getBuffer(VertexBuffer.Type.Position);
                if(buffer.isUpdateNeeded()) dirty += buffer.getData().limit()*4L;
                buffer.clearUpdateNeeded();
            }
        }
        long bytes = allocated()-allocation;
        long vertices = tracks.stream().mapToLong(t->t.getTarget().getMesh().getVertexCount()).sum();
        Arrays.sort(times);
        System.out.printf(java.util.Locale.ROOT,"%s,%d,%d,%d,%.6f,%.6f,%.6f,%d,%d%n",mode,count,tracks.size(),vertices,
                times[SAMPLES/2]/1e6,times[(int)(SAMPLES*.95)]/1e6,times[(int)(SAMPLES*.99)]/1e6,bytes/SAMPLES,dirty/SAMPLES);
    }

    private static long allocated() {
        return ALLOCATIONS.isThreadAllocatedMemorySupported() ? ALLOCATIONS.getThreadAllocatedBytes(Thread.currentThread().threadId()) : 0;
    }
}
