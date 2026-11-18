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
    private static final String[] NAMES = {"Arc-Walk","BIL-Walkback","Electrocute1"};
    private static final int WARMUP = 180, SAMPLES = 600;
    private static final com.sun.management.ThreadMXBean ALLOCATIONS =
            (com.sun.management.ThreadMXBean)ManagementFactory.getThreadMXBean();

    public static void main(String[] args) {
        if (args.length != 1) throw new IllegalArgumentException("Expected converted asset root");
        var assets = new DesktopAssetManager(true);
        assets.registerLocator(args[0],FileLocator.class);
        Spatial[] templates = Arrays.stream(NAMES).map(n->assets.loadModel("Models/"+n+".j3o")).toArray(Spatial[]::new);
        System.out.println("CPU_ONLY java="+System.getProperty("java.version")+" warmup="+WARMUP+" samples="+SAMPLES);
        System.out.println("mode,creatures,tracks,vertices,median_ms,p95_ms,p99_ms,allocated_bytes_per_frame,dirty_bytes_per_frame");
        for (int count : new int[]{1,50,200,500,1000}) {
            for (String mode : new String[]{"visible","hidden","unchanged"}) run(templates,count,mode);
        }
        for(int count : new int[]{50,200,500}) {
            Node[] owners = new Node[count]; Arrays.setAll(owners,i->new Node());
            // Warm path caches, then measure two complete transition bursts.
            for(String name : NAMES) AnimationLoader.playAnimation(new Node(),new ArtResource(name,ArtResource.ArtResourceType.ANIMATING_MESH),assets);
            long allocation = allocated(), start = System.nanoTime(); int replacements = 0;
            for(int burst=0;burst<6;burst++) for(Node owner : owners) {
                var result = AnimationLoader.playAnimation(owner,new ArtResource(NAMES[burst%3],ArtResource.ArtResourceType.ANIMATING_MESH),assets);
                if(result != AnimationLoader.PlaybackResult.STARTED) throw new AssertionError(result);
                replacements++;
            }
            System.out.printf(java.util.Locale.ROOT,"TRANSITIONS creatures=%d replacements=%d ms=%.3f allocated_bytes=%d%n",
                    count,replacements,(System.nanoTime()-start)/1e6,allocated()-allocation);
        }
    }

    private static void run(Spatial[] templates, int count, String mode) {
        Node crowd = new Node(); List<PoseTrack> tracks = new ArrayList<>();
        for(int i=0;i<count;i++) {
            Spatial instance = templates[i%templates.length].clone(); crowd.attachChild(instance);
            instance.depthFirstTraversal(node->{
                var composer = node.getControl(AnimComposer.class);
                if(composer != null) {
                    composer.setCurrentAction(KmfModelLoader.DUMMY_ANIM_CLIP_NAME);
                    for(var clip : composer.getAnimClips()) for(var track : clip.getTracks()) if(track instanceof PoseTrack pose) tracks.add(pose);
                }
            });
        }
        if(mode.equals("hidden")) crowd.setCullHint(Spatial.CullHint.Always);
        float tpf = mode.equals("unchanged") ? 0 : 1f/60;
        for(int frame=0;frame<WARMUP;frame++) crowd.updateLogicalState(tpf);
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
