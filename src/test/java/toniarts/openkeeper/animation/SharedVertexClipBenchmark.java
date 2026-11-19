/* Copyright (C) 2026 OpenKeeper, licensed under GPL-3.0-or-later. */
package toniarts.openkeeper.animation;

import com.jme3.anim.AnimComposer;
import com.jme3.asset.DesktopAssetManager;
import com.jme3.asset.plugins.FileLocator;
import com.jme3.scene.*;
import java.lang.management.ManagementFactory;
import java.nio.FloatBuffer;
import java.util.*;

/** Isolated shared immutable decoded-data experiment, including owner output writes; not full-frame evidence. */
public final class SharedVertexClipBenchmark {
    private static final String[] NAMES={"Imp_Run","Goblin-Walk","Mis_Walk","Knight-Walk","Warlock-Walk","Bile_Demon-Walk","Fir-Walk","Skeleton-Walk"};
    private static final int WARMUP=180, SAMPLES=600;
    private static final com.sun.management.ThreadMXBean ALLOCATIONS=(com.sun.management.ThreadMXBean)ManagementFactory.getThreadMXBean();
    private record Owner(PoseTrack track, VertexClip clip, float[] output, FloatBuffer buffer, double offset) { }

    private static List<PoseTrack> tracks(Spatial model) {
        var result=new ArrayList<PoseTrack>();
        model.depthFirstTraversal(node->{
            var composer=node.getControl(AnimComposer.class);
            if (composer!=null) for (var clip : composer.getAnimClips()) for (var track : clip.getTracks())
                if (track instanceof PoseTrack pose) result.add(pose);
        });
        return result;
    }
    public static void main(String[] args) {
        if (args.length!=2) throw new IllegalArgumentException("Expected converted root and repeat count");
        int runs=Integer.parseInt(args[1]); if (runs<1 || runs>20) throw new IllegalArgumentException("Expected 1..20 runs");
        var assets=new DesktopAssetManager(true); assets.registerLocator(args[0],FileLocator.class);
        Spatial[] templates=Arrays.stream(NAMES).map(name->assets.loadModel("Models/"+name+".j3o")).toArray(Spatial[]::new);
        var clips=new ArrayList<List<VertexClip>>();
        for (Spatial template : templates) clips.add(tracks(template).stream().map(t->VertexClip.bake(t,VertexClipBake.BUDGET)).toList());
        long sharedBytes=clips.stream().flatMap(Collection::stream).mapToLong(VertexClip::decodedBytes).sum();
        System.out.println("SHARED_CLIP_CPU_ONLY shared_packed_primitive_bytes="+sharedBytes+" warmup="+WARMUP+" samples="+SAMPLES);
        System.out.println("run,owners,tracks,packed,median_ms,p95_ms,p99_ms,allocated_bytes_per_update,owner_scratch_primitive_bytes");
        for (int run=0;run<runs;run++) for (int count : new int[]{50,500,1000}) {
            var classic=new ArrayList<Owner>(); var packed=new ArrayList<Owner>();
            for (int owner=0;owner<count;owner++) {
                int type=owner%templates.length;
                var a=tracks(templates[type].clone()); var b=tracks(templates[type].clone());
                for (int part=0;part<a.size();part++) {
                    VertexClip clip=clips.get(type).get(part); double offset=owner*.6180339887498949;
                    classic.add(new Owner(a.get(part),clip,null,position(a.get(part)),offset));
                    packed.add(new Owner(b.get(part),clip,new float[clip.vertexCount()*3],position(b.get(part)),offset));
                }
            }
            // Alternate order, retaining identical source phase and mutable output ownership.
            if (run%2==0) { measure(run,count,classic,false); measure(run,count,packed,true); }
            else { measure(run,count,packed,true); measure(run,count,classic,false); }
            for (int part=0;part<classic.size();part++) {
                FloatBuffer a=classic.get(part).buffer, b=packed.get(part).buffer;
                for (int i=0;i<a.limit();i++) if (Float.floatToIntBits(a.get(i))!=Float.floatToIntBits(b.get(i)))
                    throw new AssertionError("Final packed/reference output differs: part="+part+" component="+i);
            }
        }
    }
    private static FloatBuffer position(PoseTrack track) {
        return (FloatBuffer)track.getTarget().getMesh().getBuffer(VertexBuffer.Type.Position).getData();
    }
    private static void update(List<Owner> owners, boolean packed, int frame) {
        for (Owner owner : owners) {
            double period=owner.clip.keyTime(owner.clip.keyCount()-1);
            double time=(frame/60.0+owner.offset)%period;
            if (packed) {
                owner.clip.sample(time,owner.output);
                owner.buffer.put(0,owner.output,0,owner.output.length);
                Mesh mesh=owner.track.getTarget().getMesh();
                mesh.getBuffer(VertexBuffer.Type.Position).updateData(owner.buffer); mesh.clearCollisionData();
            } else owner.track.getDataAtTime(time,null);
        }
    }
    private static long allocated() { return ALLOCATIONS.isThreadAllocatedMemorySupported() ? ALLOCATIONS.getThreadAllocatedBytes(Thread.currentThread().threadId()) : 0; }
    private static void measure(int run, int count, List<Owner> owners, boolean packed) {
        for (int frame=0;frame<WARMUP;frame++) update(owners,packed,frame);
        long[] times=new long[SAMPLES]; long bytes=allocated();
        for (int frame=0;frame<SAMPLES;frame++) { long start=System.nanoTime(); update(owners,packed,WARMUP+frame); times[frame]=System.nanoTime()-start; }
        bytes=allocated()-bytes; Arrays.sort(times);
        long scratch=owners.stream().mapToLong(o->12L*o.clip.vertexCount()*(packed?1:2)).sum();
        System.out.printf(Locale.ROOT,"%d,%d,%d,%s,%.6f,%.6f,%.6f,%d,%d%n",run,count,owners.size(),packed,
                times[SAMPLES/2]/1e6,times[(int)(SAMPLES*.95)]/1e6,times[(int)(SAMPLES*.99)]/1e6,bytes/SAMPLES,scratch);
    }
}
