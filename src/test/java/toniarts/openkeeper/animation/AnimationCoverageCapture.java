/* Copyright (C) 2026 OpenKeeper, licensed under GPL-3.0-or-later. */
package toniarts.openkeeper.animation;

import com.jme3.app.SimpleApplication;
import com.jme3.material.Material;
import com.jme3.math.*;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.queue.RenderQueue.ShadowMode;
import com.jme3.scene.*;
import com.jme3.scene.shape.Quad;
import com.jme3.shadow.*;
import com.jme3.system.AppSettings;
import com.jme3.texture.*;
import com.jme3.util.*;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.nio.file.*;
import java.util.concurrent.*;
import javax.imageio.ImageIO;
import toniarts.openkeeper.utils.AssetUtils;

/** Synthetic real-GL color/shadow coverage regression, with no DK2 assets. */
public final class AnimationCoverageCapture extends SimpleApplication {
    private final CountDownLatch closed = new CountDownLatch(1);
    private volatile Throwable failure;
    private final Path output;
    private Geometry caster;
    private Texture texture;
    private int frame, example;
    private final ByteBuffer pixels = BufferUtils.createByteBuffer(1000*400*4);
    private AnimationCoverageCapture(Path output) { this.output = output; }
    public static void main(String[] args) throws Exception {
        var app = new AnimationCoverageCapture(Path.of(args[0])); Files.createDirectories(app.output);
        var settings = new AppSettings(true); settings.setResolution(1000,400); settings.setRenderer(AppSettings.LWJGL_OPENGL33);
        settings.setAudioRenderer(null); settings.setFrameRate(60); settings.setTitle("OpenKeeper coverage regression");
        app.setSettings(settings); app.setShowSettings(false); app.setDisplayFps(false); app.setDisplayStatView(false); app.start();
        if (!app.closed.await(60,TimeUnit.SECONDS)) { app.stop(); throw new AssertionError("Coverage capture timeout"); }
        if (app.failure != null) throw new AssertionError("Coverage capture failed",app.failure);
    }
    @Override public void simpleInitApp() {
        flyCam.setEnabled(false); cam.setParallelProjection(true); cam.setFrustum(.1f,50,-5,5,2,-2);
        cam.setLocation(new Vector3f(0,0,10)); cam.lookAt(Vector3f.ZERO,Vector3f.UNIT_Y);
        var floor = new Geometry("receiver",new Quad(10,4)); floor.setLocalTranslation(-5,-2,-1);
        var white = new Material(assetManager,"Common/MatDefs/Misc/Unshaded.j3md"); white.setColor("Color",ColorRGBA.White);
        floor.setMaterial(white); floor.setShadowMode(ShadowMode.Receive); rootNode.attachChild(floor);
        caster = new Geometry("coverage",new Quad(2,2)); caster.setLocalTranslation(0,-1,0); caster.setShadowMode(ShadowMode.Cast); rootNode.attachChild(caster);
        texture = new Texture2D(new Image(Image.Format.RGBA8,2,1,BufferUtils.createByteBuffer(new byte[]{(byte)255,0,0,0, 0,0,0,(byte)255}),com.jme3.texture.image.ColorSpace.Linear));
        texture.setMagFilter(Texture.MagFilter.Nearest); texture.setMinFilter(Texture.MinFilter.NearestNoMipMaps);
        var light = new com.jme3.light.DirectionalLight(new Vector3f(-3,0,-1).normalizeLocal(),ColorRGBA.White); rootNode.addLight(light);
        var shadow = new DirectionalLightShadowRenderer(assetManager,1024,1); shadow.setLight(light); shadow.setShadowIntensity(1);
        shadow.setEdgeFilteringMode(EdgeFilteringMode.Nearest); viewPort.addProcessor(shadow);
        select(0);
    }
    private void select(int index) {
        String definition = switch(index) {
            case 0 -> "MatDefs/DkLighting/Vertex.j3md";
            case 1 -> "MatDefs/DkLighting/Flat.j3md";
            default -> "MatDefs/LightingSprite.j3md";
        };
        var material = new Material(assetManager,definition); material.setTexture("DiffuseMap",texture);
        material.setFloat("AlphaDiscardThreshold",.1f); material.setBoolean("UseAlphaCoverage",index != 3);
        if (index >= 2) material.setInt("Speed",0);
        caster.setMaterial(material); AssetUtils.applyFullLight(caster); example = index;
    }
    @Override public void simpleUpdate(float tpf) {
        frame++;
        if(frame == 31) select(1); if(frame == 61) select(2); if(frame == 91) select(3);
        if(frame == 120) stop();
    }
    private int rgb(BufferedImage image, Vector3f point) {
        var screen = cam.getScreenCoordinates(point);
        return image.getRGB(Math.round(screen.x),image.getHeight()-1-Math.round(screen.y)) & 0xffffff;
    }
    private static float brightness(int rgb) { return (((rgb>>16)&255)+((rgb>>8)&255)+(rgb&255))/(3f*255); }
    @Override public void simpleRender(RenderManager manager) {
        if(frame % 30 != 20) return;
        try {
            pixels.clear(); renderer.readFrameBufferWithFormat(viewPort.getOutputFrameBuffer(),pixels,Image.Format.BGRA8);
            var image = new BufferedImage(1000,400,BufferedImage.TYPE_4BYTE_ABGR); Screenshots.convertScreenShot(pixels,image);
            ImageIO.write(image,"png",output.resolve("coverage-"+example+".png").toFile());
            float holeShadow = brightness(rgb(image,new Vector3f(-2.5f,0,-1)));
            float solidShadow = brightness(rgb(image,new Vector3f(-1.5f,0,-1)));
            int holeColor = rgb(image,new Vector3f(.5f,0,0)), solidColor = rgb(image,new Vector3f(1.5f,0,0));
            boolean alpha = example != 3;
            System.out.printf(java.util.Locale.ROOT,"COVERAGE case=%d alpha=%s holeShadow=%.3f solidShadow=%.3f holeColor=%06x solidColor=%06x%n",example,alpha,holeShadow,solidShadow,holeColor,solidColor);
            if(alpha ? holeShadow < .8f || solidShadow > .6f || brightness(holeColor) < .8f || brightness(solidColor) > .2f
                    : holeShadow > .6f || solidShadow < .8f || ((holeColor>>16)&255) < 200 || brightness(solidColor) < .8f)
                throw new AssertionError("Color/shadow coverage mismatch in case "+example);
        } catch(Throwable error) { handleError("Coverage validation failed",error); }
    }
    @Override public void handleError(String message, Throwable error) { failure = error; error.printStackTrace(); stop(); }
    @Override public void destroy() { try { super.destroy(); } finally { closed.countDown(); } }
}
