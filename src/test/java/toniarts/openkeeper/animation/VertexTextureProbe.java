/* Copyright (C) 2026 OpenKeeper, licensed under GPL-3.0-or-later. */
package toniarts.openkeeper.animation;

import com.jme3.app.SimpleApplication;
import com.jme3.material.Material;
import com.jme3.math.*;
import com.jme3.renderer.*;
import com.jme3.scene.*;
import com.jme3.system.AppSettings;
import com.jme3.texture.*;
import com.jme3.texture.image.ColorSpace;
import com.jme3.util.BufferUtils;
import java.nio.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import toniarts.openkeeper.tools.convert.AssetsConverter;

/** Numerical GL transport PoC, deliberately separate from world rendering/performance acceptance. */
public final class VertexTextureProbe extends SimpleApplication {
    private static final int WIDTH=1024, CASES=18;
    private record Clip(VertexClip data, Texture2D texture) { }
    private final List<Clip> clips=new ArrayList<>();
    private final Path input;
    private final CountDownLatch closed=new CountDownLatch(1);
    private volatile Throwable failure;
    private ViewPort probeView;
    private FrameBuffer framebuffer;
    private Geometry points;
    private Material material;
    private int frame, probe;
    private long components;
    private double maxError;
    private final ByteBuffer pixels=BufferUtils.createByteBuffer(WIDTH*16);
    private VertexTextureProbe(Path input) { this.input=input; }

    public static void main(String[] args) throws Exception {
        if (args.length!=1) throw new IllegalArgumentException("Expected isolated derived clip directory");
        var app=new VertexTextureProbe(Path.of(args[0]));
        var settings=new AppSettings(true); settings.setResolution(640,120); settings.setRenderer(AppSettings.LWJGL_OPENGL33);
        settings.setAudioRenderer(null); settings.setFrameRate(120); settings.setVSync(false); settings.setTitle("OpenKeeper vertex texture numerical probe");
        app.setSettings(settings); app.setShowSettings(false); app.setDisplayFps(false); app.setDisplayStatView(false); app.start();
        if (!app.closed.await(60,TimeUnit.SECONDS)) { app.stop(); throw new AssertionError("Vertex texture probe timed out"); }
        if (app.failure!=null) throw new AssertionError("Vertex texture probe failed",app.failure);
        if (app.components==0) throw new AssertionError("No GPU samples validated");
    }
    @Override public void simpleInitApp() {
        try {
            if (!renderer.getCaps().containsAll(List.of(Caps.VertexTextureFetch,Caps.FloatTexture,Caps.FloatColorBuffer)))
                throw new UnsupportedOperationException("Probe requires vertex fetch and float texture/color support");
            try (var files=Files.list(input)) {
                for (Path path : files.filter(p->p.getFileName().toString().endsWith("-FLOAT32.okva")).sorted().toList()) {
                    String[] identity=path.getFileName().toString().split("-");
                    VertexClip clip;
                    try (var source=Files.newInputStream(path)) { clip=VertexClip.read(source,identity[0],AssetsConverter.ConvertProcess.MODELS.getVersion(),Integer.parseInt(identity[1]),VertexClipBake.BUDGET); }
                    if (clip.vertexCount()>WIDTH) throw new IllegalArgumentException("Probe width exceeded");
                    var bytes=BufferUtils.createByteBuffer(Math.multiplyExact(clip.vertexCount()*clip.keyCount(),16));
                    var floats=bytes.asFloatBuffer(); float[] xyz=new float[clip.vertexCount()*3];
                    for (int key=0;key<clip.keyCount();key++) {
                        clip.copyFrame(key,xyz);
                        for (int i=0;i<xyz.length;i+=3) floats.put(xyz[i]).put(xyz[i+1]).put(xyz[i+2]).put(1);
                    }
                    var texture=new Texture2D(new Image(Image.Format.RGBA32F,clip.vertexCount(),clip.keyCount(),bytes,ColorSpace.Linear));
                    texture.setMinFilter(Texture.MinFilter.NearestNoMipMaps); texture.setMagFilter(Texture.MagFilter.Nearest);
                    clips.add(new Clip(clip,texture));
                }
            }
            if (clips.isEmpty()) throw new IllegalArgumentException("No derived clips");
            material=new Material(assetManager,"AnimationProbe/VertexTexture.j3md"); material.setFloat("Width",WIDTH);
            points=new Geometry("sample positions",new Mesh()); points.setMaterial(material); points.setCullHint(Spatial.CullHint.Never);
            var scene=new Node(); scene.setCullHint(Spatial.CullHint.Never); scene.attachChild(points);
            framebuffer=new FrameBuffer(WIDTH,1,1);
            framebuffer.addColorTarget(FrameBuffer.FrameBufferTarget.newTarget(new Texture2D(WIDTH,1,Image.Format.RGBA32F)));
            probeView=renderManager.createPreView("vertex numerics",new Camera(WIDTH,1));
            probeView.setClearFlags(true,true,true); probeView.setOutputFrameBuffer(framebuffer); probeView.attachScene(scene);
            scene.updateGeometricState(); select();
        } catch (Throwable error) { handleError("Probe initialization failed",error); }
    }
    private double age(VertexClip clip) {
        double duration=clip.keyTime(clip.keyCount()-1)-clip.keyTime(0);
        int timeCase=(probe%CASES)/2;
        return timeCase==0 ? -1 : timeCase==8 ? duration+1 : duration*(timeCase-1)/6.0;
    }
    private void select() {
        Clip item=clips.get(probe/CASES); VertexClip clip=item.data;
        Mesh mesh=new Mesh(); mesh.setMode(Mesh.Mode.Points);
        mesh.setBuffer(VertexBuffer.Type.Position,3,new float[clip.vertexCount()*3]);
        if ((probe&1)!=0) {
            int[] reverse=new int[clip.vertexCount()]; for (int i=0;i<reverse.length;i++) reverse[i]=reverse.length-1-i;
            mesh.setBuffer(VertexBuffer.Type.Index,1,reverse);
        }
        mesh.updateBound(); points.setMesh(mesh);
        float time=(float)Math.clamp(age(clip)+clip.keyTime(0),clip.keyTime(0),clip.keyTime(clip.keyCount()-1));
        int a=0; while (a+1<clip.keyCount() && clip.keyTime(a+1)<=time) a++;
        int b=Math.min(clip.keyCount()-1,a+1);
        float weight=a==b ? 0 : (time-clip.keyTime(a))/(clip.keyTime(b)-clip.keyTime(a));
        material.setTexture("PoseData",item.texture); material.setVector3("Frames",new Vector3f(a,b,weight));
        points.getParent().updateGeometricState();
    }
    @Override public void simpleUpdate(float tpf) { frame++; }
    @Override public void simpleRender(RenderManager manager) {
        if (frame<10 || clips.isEmpty() || failure!=null) return;
        try {
            VertexClip clip=clips.get(probe/CASES).data;
            pixels.clear(); renderer.readFrameBufferWithFormat(framebuffer,pixels,Image.Format.RGBA32F);
            var read=pixels.asFloatBuffer(); float[] reference=new float[clip.vertexCount()*3]; clip.sample(age(clip),reference);
            for (int i=0;i<clip.vertexCount();i++) {
                if (read.get(i*4+3)!=1) throw new AssertionError("Unwritten vertex sample: "+i);
                for (int axis=0;axis<3;axis++) {
                    double error=Math.abs((double)read.get(i*4+axis)-reference[i*3+axis]);
                    if (!Double.isFinite(error) || error>2e-6) throw new AssertionError("GPU interpolation mismatch: "+error);
                    maxError=Math.max(maxError,error); components++;
                }
            }
            probe++;
            if (probe==clips.size()*CASES) {
                System.out.printf(Locale.ROOT,"VERTEX_TEXTURE_PASS clips=%d cases=%d components=%d max_component_error=%.9g indexed_and_nonindexed=true%n",clips.size(),probe,components,maxError);
                stop();
            } else select();
        } catch (Throwable error) { handleError("GPU numeric validation failed",error); }
    }
    @Override public void handleError(String message,Throwable error) { failure=error; error.printStackTrace(); stop(); }
    @Override public void destroy() {
        try { if (probeView!=null) renderManager.removePreView(probeView); super.destroy(); }
        finally { closed.countDown(); }
    }
}
