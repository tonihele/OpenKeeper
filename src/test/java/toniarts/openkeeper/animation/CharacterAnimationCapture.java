/* Copyright (C) 2026 OpenKeeper, licensed under GPL-3.0-or-later. */
package toniarts.openkeeper.animation;

import com.jme3.app.SimpleApplication;
import com.jme3.asset.plugins.FileLocator;
import com.jme3.bounding.BoundingBox;
import com.jme3.font.BitmapText;
import com.jme3.light.AmbientLight;
import com.jme3.math.*;
import com.jme3.renderer.RenderManager;
import com.jme3.scene.*;
import com.jme3.system.AppSettings;
import com.jme3.util.BufferUtils;
import com.jme3.util.Screenshots;
import com.simsilica.es.base.DefaultEntityData;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import javax.imageio.ImageIO;
import toniarts.openkeeper.game.component.CreatureAi;
import toniarts.openkeeper.game.component.CreatureViewState;
import toniarts.openkeeper.game.controller.creature.CreatureState;
import toniarts.openkeeper.tools.convert.map.*;
import toniarts.openkeeper.utils.AssetUtils;
import toniarts.openkeeper.view.animation.*;
import toniarts.openkeeper.view.control.CreatureViewControl;

/** Installed-asset GL smoke/sequence capture, not original-DK2 or full-game acceptance. */
public final class CharacterAnimationCapture extends SimpleApplication {
    private final IKwdFile catalog;
    private final String converted;
    private final Path output;
    private final DefaultEntityData entities = new DefaultEntityData();
    private final List<CreatureViewControl> controls = new ArrayList<>();
    private final CountDownLatch closed = new CountDownLatch(1);
    private volatile Throwable failure;
    private ByteBuffer pixels;
    private int frame;
    private long sequence;
    private double gameTime;
    private long pauseHash, hiddenHash;
    private int pausedPixels;
    private boolean shadows;

    private CharacterAnimationCapture(String dk2, String converted, Path output) {
        catalog = KwdFile.load(dk2,Path.of(dk2,"Data","Editor","Maps","Alcatraz.kwd"));
        this.converted = converted; this.output = output;
    }

    public static void main(String[] args) throws Exception {
        if(args.length < 3) throw new IllegalArgumentException("Expected DK2 root, converted root, output directory, optional shadows");
        boolean shadows = args.length > 3 && Boolean.parseBoolean(args[3]);
        var app = new CharacterAnimationCapture(args[0],args[1],shadows ? Path.of(args[2],"shadows") : Path.of(args[2]));
        app.shadows = shadows;
        Files.createDirectories(app.output);
        var settings = new AppSettings(true); settings.setResolution(1280,720);
        settings.setRenderer(AppSettings.LWJGL_OPENGL33); settings.setAudioRenderer(null);
        settings.setFrameRate(60); settings.setVSync(false); settings.setTitle("OpenKeeper animation validation");
        app.setSettings(settings); app.setShowSettings(false); app.setDisplayFps(false); app.setDisplayStatView(false);
        app.start();
        if(!app.closed.await(60,TimeUnit.SECONDS)) { app.stop(); throw new AssertionError("GL capture timeout"); }
        if(app.failure != null) throw new AssertionError("GL capture failed",app.failure);
    }

    @Override public void simpleInitApp() {
        assetManager.registerLocator(converted,FileLocator.class);
        viewPort.setBackgroundColor(new ColorRGBA(.12f,.14f,.18f,1));
        flyCam.setEnabled(false); inputManager.setCursorVisible(true);
        cam.setParallelProjection(true); cam.setFrustum(.1f,100,-10,10,5.625f,-5.625f);
        cam.setLocation(new Vector3f(0,14,18)); cam.lookAt(new Vector3f(0,1,0),Vector3f.UNIT_Y);
        rootNode.addLight(new AmbientLight(ColorRGBA.White));
        if (shadows) {
            var light = new com.jme3.light.DirectionalLight(new Vector3f(-1,-2,-1).normalizeLocal(),ColorRGBA.White);
            rootNode.addLight(light); rootNode.setShadowMode(com.jme3.renderer.queue.RenderQueue.ShadowMode.CastAndReceive);
            var shadowRenderer = new com.jme3.shadow.DirectionalLightShadowRenderer(assetManager,1024,2);
            shadowRenderer.setLight(light); viewPort.addProcessor(shadowRenderer);
            var floor = new Geometry("capture-floor",new com.jme3.scene.shape.Box(10,.05f,6)); floor.setLocalTranslation(0,-.05f,0);
            var material = new com.jme3.material.Material(assetManager,"Common/MatDefs/Misc/Unshaded.j3md");
            material.setColor("Color",new ColorRGBA(.2f,.2f,.22f,1)); floor.setMaterial(material); rootNode.attachChild(floor);
        }
        pixels = BufferUtils.createByteBuffer(1280*720*4);
        List<Creature> examples = new ArrayList<>();
        for(String token : new String[]{"imp","mistress","knight","warlock","bile","firefly","skeleton"}) {
            catalog.getCreatureList().stream().filter(c->c.getName().toLowerCase(Locale.ROOT).contains(token))
                    .findFirst().ifPresent(examples::add);
        }
        catalog.getCreatureList().stream().filter(c->c.getFlags().contains(Creature.CreatureFlag.IS_UNIQUE))
                .filter(c->!examples.contains(c)).findFirst().ifPresent(examples::add);
        if(examples.size() < 6) throw new AssertionError("Representative catalog creatures unavailable");
        for(int i=0;i<examples.size();i++) {
            Creature creature = examples.get(i); var id = entities.createEntity();
            entities.setComponent(id,new CreatureAi(0,CreatureState.IDLE,creature.getCreatureId()));
            var owner = new Node(creature.getName()); owner.addControl(new PreparedAnimationCache(3,256*1024));
            owner.setCullHint(Spatial.CullHint.Always);
            var control = new CreatureViewControl(id,entities,creature,null,assetManager,null);
            control.setGameTime(()->gameTime); owner.addControl(control); controls.add(control);
            control.setViewState(creature,new CreatureViewState(creature.getCreatureId(),0,Creature.AnimationType.WALK,0));
            owner.setCullHint(Spatial.CullHint.Inherit); rootNode.attachChild(owner); owner.updateGeometricState();
            if(owner.getWorldBound() instanceof BoundingBox bound && bound.getYExtent() > 0) owner.setLocalScale(1.05f/bound.getYExtent());
            owner.setLocalTranslation((i%4-1.5f)*4,0,(i/4-.5f)*5.5f);
            AssetUtils.applyFullLight(owner);
            BitmapText label = new BitmapText(assetManager.loadFont("Interface/Fonts/Default.fnt"));
            label.setText(creature.getName()); label.setSize(18);
            Vector3f labelPosition = cam.getScreenCoordinates(owner.getLocalTranslation());
            label.setLocalTranslation(labelPosition.x-label.getLineWidth()/2,labelPosition.y-15,0); guiNode.attachChild(label);
            System.out.println("GL_EXAMPLE "+creature.getCreatureId()+" "+creature.getName());
        }
    }

    @Override public void simpleUpdate(float tpf) {
        frame++;
        if(frame < 450 || frame > 480) gameTime += 1.0/60;
        if(frame == 120) request(Creature.AnimationType.MELEE_ATTACK);
        if(frame == 240) request(Creature.AnimationType.IDLE_1);
        if(frame == 300) request(Creature.AnimationType.DEATH_POSE);
        if(frame == 360 || frame == 420) request(Creature.AnimationType.WALK);
        if(frame == 450) AnimationLoader.pauseAnimations(rootNode);
        if(frame == 480) AnimationLoader.resumeAnimations(rootNode);
        if(frame == 510) rootNode.setCullHint(Spatial.CullHint.Always);
        if(frame == 540) rootNode.setCullHint(Spatial.CullHint.Dynamic);
        if(frame >= 600) stop();
    }

    private void request(Creature.AnimationType state) {
        sequence++;
        for(var control : controls) {
            Creature creature = control.getDataObject();
            boolean accepted = control.setViewState(creature,new CreatureViewState(creature.getCreatureId(),gameTime,state,sequence));
            AssetUtils.applyFullLight(control.getSpatial());
            System.out.println("GL_REQUEST frame="+frame+" creature="+creature.getName()+" action="+state+" accepted="+accepted);
        }
    }

    @Override public void simpleRender(RenderManager manager) {
        if(frame > 0 && frame % 15 == 0 && frame < 600) {
            try {
                pixels.clear(); renderer.readFrameBufferWithFormat(viewPort.getOutputFrameBuffer(),pixels,com.jme3.texture.Image.Format.BGRA8);
                var image = new BufferedImage(1280,720,BufferedImage.TYPE_4BYTE_ABGR);
                Screenshots.convertScreenShot(pixels,image);
                ImageIO.write(image,"png",output.resolve(String.format(Locale.ROOT,"frame-%03d.png",frame)).toFile());
                for(var control : controls) validateCurrentPose(control.getSpatial());
                long hash = poseHash();
                if(frame == 450) pauseHash = hash;
                if(frame == 465 && pauseHash != hash) throw new AssertionError("Paused pose changed");
                int pixelHash = Arrays.hashCode(((java.awt.image.DataBufferByte)image.getRaster().getDataBuffer()).getData());
                if(frame == 450) pausedPixels = pixelHash;
                if(frame == 465 && pausedPixels != pixelHash) throw new AssertionError("Paused rendered pixels changed");
                if(frame == 510) hiddenHash = hash;
                if(frame == 525 && hiddenHash != hash) throw new AssertionError("Hidden pose was deformed");
                if(frame == 555 && hiddenHash == hash) throw new AssertionError("Visible re-entry did not update pose");
                System.out.println("GL_CAPTURE frame="+frame+" pose_hash="+hash);
            } catch(Exception e) { handleError("Capture failed",e); }
        }
    }

    private long poseHash() {
        long[] hash = {1};
        rootNode.depthFirstTraversal(node -> {
            if(node instanceof Geometry geometry) {
                var buffer = geometry.getMesh().getBuffer(VertexBuffer.Type.Position);
                if(buffer != null && buffer.getData() instanceof java.nio.FloatBuffer data)
                    for(int i=0;i<data.limit();i++) hash[0] = hash[0]*31 + Float.floatToIntBits(data.get(i));
            }
        });
        return hash[0];
    }

    private static void validateCurrentPose(Spatial root) {
        root.depthFirstTraversal(node -> {
            if (!(node instanceof Geometry geometry)) return;
            var buffer = geometry.getMesh().getBuffer(VertexBuffer.Type.Position);
            if (buffer == null || !(buffer.getData() instanceof java.nio.FloatBuffer data)) return;
            for(int i=0;i<data.limit();i++) if(!Float.isFinite(data.get(i))) throw new AssertionError("Non-finite GL pose: "+geometry.getName());
        });
    }

    @Override public void handleError(String message, Throwable error) { failure = error; error.printStackTrace(); stop(); }
    @Override public void destroy() {
        try { for(var control : controls) control.cleanup(); entities.close(); super.destroy(); }
        finally { closed.countDown(); }
    }
}
