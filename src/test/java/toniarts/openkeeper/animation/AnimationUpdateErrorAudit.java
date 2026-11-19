/* Copyright (C) 2026 OpenKeeper, licensed under GPL-3.0-or-later. */
package toniarts.openkeeper.animation;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import toniarts.openkeeper.tools.convert.AssetsConverter;

/** Offline quality gate for held-pose scheduling. Does not infer visibility or throttle gameplay. */
public final class AnimationUpdateErrorAudit {
    record Result(double maxRelativeError, int samples, int writes) { }

    /**
     * Relative error uses the part's 3D envelope diameter, not a guessed creature height.
     * Under an orthographic view, projected error cannot exceed this distance times pixels/unit.
     * Perspective, moving cameras and shadow cameras require their own projection/error tests.
     */
    static Result measure(VertexClip clip, int renderHz, int updateHz, double offset) {
        if (renderHz < 1 || updateHz < 1 || updateHz > renderHz || !Double.isFinite(offset)
                || offset < 0 || offset >= 1) throw new IllegalArgumentException("Invalid schedule");
        int size = clip.vertexCount()*3;
        float[] current = new float[size], held = new float[size];
        double[] min = {Double.POSITIVE_INFINITY,Double.POSITIVE_INFINITY,Double.POSITIVE_INFINITY};
        double[] max = {Double.NEGATIVE_INFINITY,Double.NEGATIVE_INFINITY,Double.NEGATIVE_INFINITY};
        for (int key=0;key<clip.keyCount();key++) {
            clip.copyFrame(key,current);
            for (int i=0;i<size;i++) { min[i%3]=Math.min(min[i%3],current[i]); max[i%3]=Math.max(max[i%3],current[i]); }
        }
        double diameter=Math.sqrt(Math.pow(max[0]-min[0],2)+Math.pow(max[1]-min[1],2)+Math.pow(max[2]-min[2],2));
        double period=clip.keyTime(clip.keyCount()-1);
        if (period <= 0) throw new IllegalArgumentException("Expected positive loop period");
        // Three wraps include the authored terminal dwell and loop seam; model the frame the write occurs.
        int samples=(int)Math.ceil(period*3*renderHz)+1, writes=0;
        long previousBucket=Long.MIN_VALUE; double maximum=0;
        for (int frame=0;frame<samples;frame++) {
            double time=(double)frame/renderHz;
            long bucket=(long)Math.floor((long)frame*updateHz/(double)renderHz+offset);
            if (frame==0 || bucket!=previousBucket) {
                clip.sample(time%period,held); writes++; previousBucket=bucket;
            }
            clip.sample(time%period,current);
            for (int i=0;i<size;i+=3) {
                double dx=(double)current[i]-held[i], dy=(double)current[i+1]-held[i+1], dz=(double)current[i+2]-held[i+2];
                maximum=Math.max(maximum,Math.sqrt(dx*dx+dy*dy+dz*dz));
            }
        }
        return new Result(diameter==0 ? 0 : maximum/diameter,samples,writes);
    }

    public static void main(String[] args) throws Exception {
        if (args.length!=2) throw new IllegalArgumentException("Expected baked clip directory and report path");
        Path root=Path.of(args[0]), report=Path.of(args[1]);
        var rows=new ArrayList<String>();
        rows.add("resource,part,render_hz,update_hz,stagger,part_diameter_pixels,max_error_bound_pixels,samples,writes");
        int tests=0, accepted16=0; double worst16=0;
        // Read only the current manifest, never stale files left by a previous bake.
        var manifest=Files.readAllLines(root.resolve("report.csv"));
        for (String line : manifest.subList(1,manifest.size())) {
            String[] fields=line.split(","); String resource=fields[0]; int part=Integer.parseInt(fields[1]);
            // The isolated bake records one source identity per part file; match it through the fresh manifest.
            String hash=fields[7];
            VertexClip clip;
            try (var input=Files.newInputStream(root.resolve(hash+"-"+part+"-FLOAT32.okva"))) {
                clip=VertexClip.read(input,hash,AssetsConverter.ConvertProcess.MODELS.getVersion(),part,VertexClipBake.BUDGET);
            }
            for (int renderHz : new int[]{60,120,144}) for (int updateHz : new int[]{15,30,60}) for (int stagger=0;stagger<8;stagger++) {
                Result result=measure(clip,renderHz,updateHz,stagger/8.0);
                tests++; worst16=Math.max(worst16,result.maxRelativeError*16);
                if (result.maxRelativeError*16<=.5) accepted16++;
                for (int pixels : new int[]{8,16,32,64}) rows.add(String.format(Locale.ROOT,"%s,%d,%d,%d,%d,%d,%.9g,%d,%d",
                        resource,part,renderHz,updateHz,stagger,pixels,result.maxRelativeError*pixels,result.samples,result.writes));
            }
        }
        Files.createDirectories(report.toAbsolutePath().getParent()); Files.write(report,rows);
        System.out.printf(Locale.ROOT,"UPDATE_ERROR cases=%d accepted_at_16px=%d worst_16px_bound=%.6f threshold_pixels=0.5%n",tests,accepted16,worst16);
    }
}
