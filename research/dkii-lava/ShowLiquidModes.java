import com.jme3.app.SimpleApplication;
import com.jme3.app.state.ScreenshotAppState;
import com.jme3.asset.plugins.FileLocator;
import com.jme3.font.BitmapText;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.*;
import com.jme3.math.*;
import com.jme3.renderer.Camera;
import com.jme3.scene.*;
import com.jme3.system.AppSettings;
import com.jme3.texture.*;
import com.jme3.texture.image.ColorSpace;
import com.jme3.util.BufferUtils;
import java.nio.file.*;
import java.util.*;
import toniarts.openkeeper.common.EntityInstance;
import toniarts.openkeeper.tools.convert.map.KwdFile;
import toniarts.openkeeper.utils.PathUtils;
import toniarts.openkeeper.view.control.LiquidSurfaceControl;
import toniarts.openkeeper.view.map.Water;

/** Interactive four-view comparison using the real OpenKeeper liquid implementation. */
public class ShowLiquidModes extends SimpleApplication {
    private final List<Node> scenes = new ArrayList<>();
    private final List<Camera> cameras = new ArrayList<>();
    private final ScreenshotAppState screenshots = new ScreenshotAppState("/tmp/openkeeper-liquid-comparison/");
    private float angle = .35f, distance = 7f;
    private int frames;
    private final boolean captureOnly;

    ShowLiquidModes(boolean captureOnly) { this.captureOnly = captureOnly; }
    public static void main(String[] args) throws Exception {
        String game = args.length == 0 || args[0].equals("--capture")
                ? "/home/wietse/Downloads/Data/Bullfrog/Dungeon Keeper 2/" : args[0];
        PathUtils.setDKIIFolder(PathUtils.fixFilePath(game));
        Files.createDirectories(Path.of("/tmp/openkeeper-liquid-comparison"));
        var app = new ShowLiquidModes(Arrays.asList(args).contains("--capture"));
        var settings = new AppSettings(true);
        settings.setResolution(1280,800);
        settings.setRenderer(AppSettings.LWJGL_OPENGL32);
        settings.setAudioRenderer(null);
        settings.setGammaCorrection(false);
        settings.setSamples(0);
        settings.setTitle("OpenKeeper — Classic / Enhanced Water and Lava (#851)");
        app.setSettings(settings);
        app.setShowSettings(false);
        app.setPauseOnLostFocus(false);
        app.start();
    }

    @Override public void simpleInitApp() {
        assetManager.registerLocator("assets", FileLocator.class);
        assetManager.registerLocator("assets/Converted", FileLocator.class);
        flyCam.setEnabled(false);
        setDisplayFps(false);setDisplayStatView(false);
        viewPort.setEnabled(false);
        stateManager.attach(screenshots);
        var level = KwdFile.load(PathUtils.getDKIIFolder(),"Level1");
        for(int index=0;index<4;index++) {
            boolean water = index<2, enhanced = index%2==1;
            Node scene = new Node();scenes.add(scene);
            var instance = new EntityInstance<>(level.getTerrain((short)(water?4:5)));
            for(int y=0;y<4;y++)for(int x=0;x<4;x++)instance.addCoordinate(new toniarts.openkeeper.utils.Point(x,y));
            Geometry surface = (Geometry) Water.construct(assetManager,List.of(instance));
            surface.removeControl(LiquidSurfaceControl.class);
            surface.addControl(new LiquidSurfaceControl(water,assetManager,()->enhanced));
            // Identical neutral light, texture-space coordinates and camera in every pane.
            var data=BufferUtils.createByteBuffer(16*Float.BYTES);
            data.asFloatBuffer().put(new float[]{1,.2f,.2f,.2f, 1.5f,2.5f,1.5f,4.5f, .6f,.6f,.6f,.4f, 0,0,0,0});
            surface.getMaterial().setTexture("LightData",new Texture2D(new Image(Image.Format.RGBA32F,4,1,data,ColorSpace.Linear)));
            surface.getMaterial().setColor("Ambient",new ColorRGBA(.3f,.3f,.3f,1));
            scene.attachChild(surface);
            Camera camera=cam.clone();
            float left=enhanced?.5f:0, bottom=water?.5f:0;
            camera.setViewPort(left,left+.5f,bottom,bottom+.5f);
            camera.setFrustumPerspective(40,1.6f,.1f,100);
            cameras.add(camera);
            var viewport=renderManager.createMainView("liquid"+index,camera);
            viewport.setClearFlags(true,true,true);
            viewport.setBackgroundColor(new ColorRGBA(.025f,.03f,.04f,1));
            viewport.attachScene(scene);
            label((water?"Water":"Lava")+" — "+(enhanced?"Enhanced":"Classic"),
                    enhanced?665:25,water?770:370,24);
        }
        label("Arrows: orbit / zoom    F12: screenshot    Esc: close",25,32,16);
        updateCameras();
        for(var entry:Map.of("left",KeyInput.KEY_LEFT,"right",KeyInput.KEY_RIGHT,
                "in",KeyInput.KEY_UP,"out",KeyInput.KEY_DOWN,"capture",KeyInput.KEY_F12).entrySet())
            inputManager.addMapping(entry.getKey(),new KeyTrigger(entry.getValue()));
        inputManager.addListener((ActionListener)(name,pressed,tpf)->{
            if(!pressed)return;
            switch(name) {
                case "left" -> angle-=.15f;
                case "right" -> angle+=.15f;
                case "in" -> distance=Math.max(3,distance-.5f);
                case "out" -> distance=Math.min(14,distance+.5f);
                case "capture" -> screenshots.takeScreenshot();
            }
            updateCameras();
        },"left","right","in","out","capture");
        System.out.println("LIQUID_COMPARISON_READY: top water / bottom lava; left Classic / right Enhanced");
    }
    private void label(String text,float x,float y,int size) {
        BitmapText label=new BitmapText(guiFont);label.setText(text);label.setSize(size);
        label.setLocalTranslation(x,y,0);guiNode.attachChild(label);
    }
    private void updateCameras() {
        for(Camera camera:cameras) {
            camera.setLocation(new Vector3f(1.5f+(float)Math.sin(angle)*distance,distance*.9f,
                    1.5f+(float)Math.cos(angle)*distance));
            camera.lookAt(new Vector3f(1.5f,.85f,1.5f),Vector3f.UNIT_Y);
        }
    }
    @Override public void simpleUpdate(float tpf) {
        for(Node scene:scenes) { scene.updateLogicalState(tpf);scene.updateGeometricState(); }
        frames++;
        if(frames==40)screenshots.takeScreenshot();
        if(captureOnly && frames==60)stop();
    }
    @Override public void handleError(String message,Throwable error) {
        System.err.println(message);if(error!=null)error.printStackTrace();stop();
    }
}
