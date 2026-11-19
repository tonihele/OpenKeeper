/* Copyright (C) 2026 OpenKeeper, licensed under GPL-3.0-or-later. */
package toniarts.openkeeper.animation;

import com.jme3.app.SimpleApplication;
import com.jme3.asset.plugins.FileLocator;
import com.jme3.bounding.BoundingBox;
import com.jme3.math.*;
import com.jme3.profile.*;
import com.jme3.renderer.*;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.*;
import com.jme3.system.AppSettings;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import toniarts.openkeeper.tools.convert.map.ArtResource;
import toniarts.openkeeper.utils.AssetUtils;
import toniarts.openkeeper.view.animation.AnimationLoader;

/** Rendered CPU-backend attribution fixture. Not a game/AI, VAT, instancing or speedup benchmark. */
public final class RenderedAnimationCrowdBenchmark extends SimpleApplication implements AppProfiler {
    private static final int WARMUP=180, SAMPLES=600, DRAIN=32;
    private static final String[] NAMES={"Imp_Run","Goblin-Walk","Mis_Walk","Knight-Walk","Warlock-Walk","Bile_Demon-Walk","Fir-Walk","Skeleton-Walk"};
    private record Workload(int run, int count, boolean moving) { }
    private final String converted;
    private final Path output;
    private final CountDownLatch closed=new CountDownLatch(1);
    private volatile Throwable failure;
    private final ArrayDeque<Workload> workloads=new ArrayDeque<>();
    private final List<String> report=new ArrayList<>();
    private Spatial[] templates;
    private final AnimationDataCache animationData;
    private Workload workload;
    private int frame, query=-1;
    private double gameTime;
    private long begin, spatialBegin, renderBegin, previousBegin;
    private int previousSample=-1;
    private int[] queryIds, querySamples, statistics;
    private final long[] wall=new long[SAMPLES], spatialCpu=new long[SAMPLES], renderCpu=new long[SAMPLES], gpu=new long[SAMPLES];
    private final int[] draws=new int[SAMPLES];

    private RenderedAnimationCrowdBenchmark(String converted, Path output, int runs, int packedBytes) {
        this.converted=converted; this.output=output;
        animationData=new AnimationDataCache(packedBytes);
        for (int run=0;run<runs;run++) for (int count : new int[]{50,250,500,1000}) {
            // Alternate order between runs to expose warm/order bias.
            workloads.add(new Workload(run,count,run%2==0)); workloads.add(new Workload(run,count,run%2!=0));
        }
        report.add("run,owners,moving,frame_median_ms,frame_p95_ms,frame_p99_ms,spatial_p95_ms,render_submit_p95_ms,gpu_render_p95_ms,draws_median,gpu_samples");
    }
    public static void main(String[] args) throws Exception {
        if (args.length!=4) throw new IllegalArgumentException("Expected converted root, report directory, run count and packed KiB");
        int runs=Integer.parseInt(args[2]);
        if (runs<1 || runs>20) throw new IllegalArgumentException("Expected 1..20 runs");
        int packedKiB=Integer.parseInt(args[3]);
        var app=new RenderedAnimationCrowdBenchmark(args[0],Path.of(args[1],"packed-"+packedKiB),runs,Math.multiplyExact(packedKiB,1024));
        Files.createDirectories(app.output);
        var settings=new AppSettings(true); settings.setResolution(1280,720); settings.setSamples(0);
        settings.setRenderer(AppSettings.LWJGL_OPENGL33); settings.setAudioRenderer(null);
        settings.setFrameRate(0); settings.setVSync(false); settings.setTitle("OpenKeeper rendered crowd attribution");
        app.setSettings(settings); app.setShowSettings(false); app.setDisplayFps(false); app.setDisplayStatView(false);
        app.setPauseOnLostFocus(false); app.start();
        if (!app.closed.await(15,TimeUnit.MINUTES)) { app.stop(); throw new AssertionError("Rendered crowd benchmark timed out"); }
        app.stop(true);
        if (app.failure!=null) throw new AssertionError("Rendered crowd benchmark failed",app.failure);
    }
    @Override public void simpleInitApp() {
        assetManager.registerLocator(converted,FileLocator.class); flyCam.setEnabled(false);
        cam.setParallelProjection(false); cam.setFrustumPerspective(50,1280f/720,.1f,150);
        cam.setLocation(new Vector3f(0,6,10)); cam.lookAt(new Vector3f(0,.8f,0),Vector3f.UNIT_Y);
        viewPort.setBackgroundColor(new ColorRGBA(.12f,.14f,.18f,1));
        templates=Arrays.stream(NAMES).map(n->assetManager.loadModel("Models/"+n+".j3o")).toArray(Spatial[]::new);
        for (Spatial template : templates) animationData.prepare(template);
        queryIds=renderer.generateProfilingTasks(32); querySamples=new int[queryIds.length]; Arrays.fill(querySamples,-1);
        statistics=new int[renderer.getStatistics().getLabels().length]; renderer.getStatistics().setEnabled(true);
        setAppProfiler(this);
        System.out.println("RENDERED_CROWD CPU backend, 1280x720, no shadows/AI/world lights, close perspective camera (0,6,10), target (0,.8,0), FOV=50, varied phase, uncapped, vsync=false; owners include offscreen actors, draw count is actual submitted parts");
        System.out.println("RENDERED_PACKED primitive_limit="+animationData.getLimit()+" shared_primitive_bytes="+animationData.getResidentPrimitiveBytes());
    }
    @Override public void simpleUpdate(float tpf) {
        if (workload==null || frame>=WARMUP+SAMPLES+DRAIN) {
            if (workload!=null) finishWorkload();
            workload=workloads.poll();
            if (workload==null) { stop(); return; }
            rootNode.detachAllChildren(); Arrays.fill(wall,0); Arrays.fill(spatialCpu,0);
            Arrays.fill(renderCpu,0); Arrays.fill(gpu,0); Arrays.fill(draws,0);
            previousSample=-1; frame=0; gameTime=0;
            int width=(int)Math.ceil(Math.sqrt(workload.count));
            for (int i=0;i<workload.count;i++) {
                int type=i%templates.length;
                var owner=new Node("owner-"+i); Spatial instance=templates[type].clone();
                var result=AnimationLoader.playAnimation(owner,new ArtResource(NAMES[type],ArtResource.ArtResourceType.ANIMATING_MESH),
                        (name,resource)->instance,false);
                if (result!=AnimationLoader.PlaybackResult.STARTED) throw new AssertionError(result);
                double phase=(i*.6180339887498949)%1;
                AnimationLoader.bindPlaybackClock(owner,()->gameTime+phase,0);
                owner.updateGeometricState();
                if (owner.getWorldBound() instanceof BoundingBox box && box.getYExtent()>0) owner.setLocalScale(.8f/box.getYExtent());
                owner.setLocalTranslation((i%width-(width-1)*.5f)*2,0,(i/width-(width-1)*.5f)*2);
                AssetUtils.applyFullLight(owner); rootNode.attachChild(owner);
            }
            System.out.printf(Locale.ROOT,"RENDERED_START run=%d owners=%d moving=%s%n",workload.run,workload.count,workload.moving);
        }
        frame++;
        if (workload.moving) gameTime+=1.0/60;
    }
    private int sample() { return workload!=null && frame>WARMUP && frame<=WARMUP+SAMPLES ? frame-WARMUP-1 : -1; }
    @Override public void simpleRender(RenderManager manager) {
        int sample=sample();
        if (sample>=0) {
            // ResetStatsState clears these during postRender, before AppStep.EndFrame.
            renderer.getStatistics().getData(statistics); draws[sample]=statistics[3];
        }
        if (frame==WARMUP+SAMPLES+3 && workload.run==0 && workload.moving) {
            var pixels=com.jme3.util.BufferUtils.createByteBuffer(1280*720*4);
            renderer.readFrameBufferWithFormat(viewPort.getOutputFrameBuffer(),pixels,com.jme3.texture.Image.Format.BGRA8);
            var image=new java.awt.image.BufferedImage(1280,720,java.awt.image.BufferedImage.TYPE_4BYTE_ABGR);
            com.jme3.util.Screenshots.convertScreenShot(pixels,image);
            try { javax.imageio.ImageIO.write(image,"png",output.resolve("close-"+workload.count+".png").toFile()); }
            catch (java.io.IOException error) { throw new java.io.UncheckedIOException(error); }
        }
    }
    @Override public void appStep(AppStep step) {
        long now=System.nanoTime();
        switch (step) {
            case BeginFrame -> {
                if (previousSample>=0) wall[previousSample]=now-previousBegin;
                previousSample=-1; begin=now;
                if (queryIds!=null) for (int i=0;i<queryIds.length;i++) if (querySamples[i]>=0 && renderer.isTaskResultAvailable(queryIds[i])) {
                    gpu[querySamples[i]]=renderer.getProfilingTime(queryIds[i]); querySamples[i]=-1;
                }
            }
            case SpatialUpdate -> spatialBegin=now;
            case StateManagerRender -> { int sample=sample(); if (sample>=0) spatialCpu[sample]=now-spatialBegin; }
            case RenderFrame -> {
                // StatsAppState initializes after simpleInitApp and disables counters with its hidden overlay.
                renderer.getStatistics().setEnabled(true);
                renderBegin=now;
                if (sample()>=0) {
                    for (int i=0;i<queryIds.length;i++) if (querySamples[i]<0) { query=i; break; }
                    if (query<0) throw new AssertionError("GPU query pool exhausted; no blocking result read is allowed");
                    renderer.startProfiling(queryIds[query]);
                }
            }
            case EndFrame -> {
                if (query>=0) { renderer.stopProfiling(); querySamples[query]=sample(); query=-1; }
                int sample=sample();
                if (sample>=0) {
                    renderCpu[sample]=now-renderBegin; previousBegin=begin; previousSample=sample;
                }
            }
            default -> { }
        }
    }
    private static double percentile(long[] values, double fraction) {
        long[] copy=values.clone(); Arrays.sort(copy); return copy[Math.min(copy.length-1,(int)(fraction*copy.length))]/1e6;
    }
    private void finishWorkload() {
        if (Arrays.stream(querySamples).anyMatch(v->v>=0) || Arrays.stream(gpu).anyMatch(v->v<=0)
                || Arrays.stream(wall).anyMatch(v->v<=0) || Arrays.stream(draws).anyMatch(v->v<=0))
            throw new AssertionError("Incomplete timing/draw data: pending="+Arrays.toString(querySamples)
                    +" GPU zero="+Arrays.stream(gpu).filter(v->v<=0).count()+" wall zero="+Arrays.stream(wall).filter(v->v<=0).count()
                    +" draw zero="+Arrays.stream(draws).filter(v->v<=0).count());
        int[] sorted=draws.clone(); Arrays.sort(sorted);
        String row=String.format(Locale.ROOT,"%d,%d,%s,%.6f,%.6f,%.6f,%.6f,%.6f,%.6f,%d,%d",
                workload.run,workload.count,workload.moving,percentile(wall,.5),percentile(wall,.95),percentile(wall,.99),
                percentile(spatialCpu,.95),percentile(renderCpu,.95),percentile(gpu,.95),sorted[SAMPLES/2],SAMPLES);
        report.add(row); System.out.println("RENDERED_RESULT "+row);
        var samples=new ArrayList<String>(SAMPLES+1);
        samples.add("sample,frame_ns,spatial_cpu_ns,render_submit_cpu_ns,gpu_render_scope_ns,drawn_parts");
        for (int i=0;i<SAMPLES;i++) samples.add(i+","+wall[i]+","+spatialCpu[i]+","+renderCpu[i]+","+gpu[i]+","+draws[i]);
        try { Files.write(output.resolve("run-"+workload.run+"-"+workload.count+"-"+workload.moving+".csv"),samples); }
        catch (java.io.IOException error) { throw new java.io.UncheckedIOException(error); }
    }
    @Override public void appSubStep(String... info) { }
    @Override public void vpStep(VpStep step, ViewPort vp, RenderQueue.Bucket bucket) { }
    @Override public void spStep(SpStep step, String... info) { }
    @Override public void handleError(String message, Throwable error) { failure=error; error.printStackTrace(); stop(); }
    @Override public void destroy() {
        try { try { Files.write(output.resolve("rendered-crowd.csv"),report); } catch (Exception error) { failure=error; }
            super.destroy(); } finally { closed.countDown(); }
    }
}
