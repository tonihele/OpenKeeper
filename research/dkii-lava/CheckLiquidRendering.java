import com.jme3.app.SimpleApplication;
import com.jme3.asset.plugins.FileLocator;
import com.jme3.math.*;
import com.jme3.scene.*;
import com.jme3.system.*;
import com.jme3.texture.*;
import com.jme3.texture.image.ColorSpace;
import com.jme3.util.BufferUtils;
import java.nio.*;
import java.nio.file.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.util.*;
import toniarts.openkeeper.Main;
import toniarts.openkeeper.common.EntityInstance;
import toniarts.openkeeper.game.data.Settings;
import toniarts.openkeeper.tools.convert.map.*;
import toniarts.openkeeper.utils.*;
import toniarts.openkeeper.view.map.Water;

public class CheckLiquidRendering extends SimpleApplication {
    int frame=0,phase=0;byte[] classic,enhanced;int failure=0;
    final java.util.List<Geometry> surfaces=new ArrayList<>();
    Texture2D visible,hidden;
    static final int WIDTH=640,HEIGHT=480;
    static final Path OUTPUT=Path.of("/tmp/openkeeper-liquid-render");
    public static void main(String[] args) {
        PathUtils.setDKIIFolder("/home/wietse/Downloads/Data/Bullfrog/Dungeon Keeper 2/");
        var app=new CheckLiquidRendering();var settings=new AppSettings(true);
        settings.setResolution(WIDTH,HEIGHT);settings.setRenderer(AppSettings.LWJGL_OPENGL32);
        settings.setAudioRenderer(null);settings.setGammaCorrection(false);settings.setSamples(0);
        app.setSettings(settings);app.setShowSettings(false);app.setPauseOnLostFocus(false);
        app.start(JmeContext.Type.OffscreenSurface);
    }
    static Texture2D fog(boolean explored) {
        var data=BufferUtils.createByteBuffer(8*8*4);
        for(int i=0;i<64;i++)data.put((byte)80).put((byte)80).put((byte)80).put((byte)(explored?255:0));
        data.flip();return new Texture2D(new Image(Image.Format.RGBA8,8,8,data,ColorSpace.Linear));
    }
    @Override public void simpleInitApp() {
        assetManager.registerLocator("assets",FileLocator.class);
        assetManager.registerLocator("assets/Converted",FileLocator.class);
        flyCam.setEnabled(false);setDisplayStatView(false);setDisplayFps(false);
        cam.setLocation(new Vector3f(2.5f,7,7));cam.lookAt(new Vector3f(2.5f,.85f,1),Vector3f.UNIT_Y);
        visible=fog(true);hidden=fog(false);
        var k=KwdFile.load(PathUtils.getDKIIFolder(),"Level1");
        Main.getUserSettings().setSetting(Settings.Setting.ENHANCED_LIQUIDS,false);
        for(int kind=0;kind<2;kind++) {
            var instance=new EntityInstance<>(k.getTerrain((short)(kind==0?4:5)));
            for(int z=0;z<3;z++)for(int x=0;x<3;x++)instance.addCoordinate(new toniarts.openkeeper.utils.Point(x+kind*4,z));
            var geometry=(Geometry)Water.construct(assetManager,java.util.List.of(instance));
            geometry.getMaterial().setTexture("AmbientMap",visible);
            geometry.getMaterial().setVector2("AmbientMapSize",new Vector2f(8,8));
            // Exercise the LIGHT_DATA shader variant without introducing moving sources.
            var data=BufferUtils.createByteBuffer(16);data.asFloatBuffer().put(new float[]{0,.2f,.2f,.2f});
            geometry.getMaterial().setTexture("LightData",new Texture2D(new Image(Image.Format.RGBA32F,1,1,data,ColorSpace.Linear)));
            surfaces.add(geometry);rootNode.attachChild(geometry);
        }
    }
    @Override public void simpleUpdate(float tpf) {
        frame++;
        if(frame==10) {
            classic=capture("classic");
            Main.getUserSettings().setSetting(Settings.Setting.ENHANCED_LIQUIDS,true);
        }
        if(frame==20) {
            for(var g:surfaces)if(g.getMaterial().getTextureParam("BumpMap")==null)throw new AssertionError("Enhanced assets not loaded for "+g.getName());
            enhanced=capture("enhanced");
            int changed=0;for(int i=0;i<classic.length;i+=4)if(Math.abs((classic[i]&255)-(enhanced[i]&255))+Math.abs((classic[i+1]&255)-(enhanced[i+1]&255))+Math.abs((classic[i+2]&255)-(enhanced[i+2]&255))>10)changed++;
            if(changed<100)throw new AssertionError("Enhanced rendering did not change the image: "+changed);
            System.out.println("Enhanced changed "+changed+" pixels");
            for(var g:surfaces)g.getMaterial().setTexture("AmbientMap",hidden);
        }
        if(frame==30) {
            byte[] black=capture("unexplored");int max=0;
            for(int i=0;i<black.length;i++)if(i%4!=3)max=Math.max(max,black[i]&255);
            if(max>1)throw new AssertionError("Enhanced environment map revealed unexplored terrain: "+max);
            for(var g:surfaces)g.getMaterial().setTexture("AmbientMap",visible);
            Main.getUserSettings().setSetting(Settings.Setting.ENHANCED_LIQUIDS,false);
        }
        if(frame==40) {
            capture("restored");
            for(var g:surfaces)if(g.getMaterial().getTextureParam("BumpMap")!=null)throw new AssertionError("Bump state retained in Classic");
            // Also compile the unmodified terrain technique through the shared library.
            var terrain=new Geometry("Standard terrain shader check",surfaces.get(1).getMesh());
            var material=new com.jme3.material.Material(assetManager,"MatDefs/DkLighting/Vertex.j3md");
            material.setTexture("DiffuseMap",surfaces.get(1).getMaterial().getTextureParam("DiffuseMap").getTextureValue());
            material.setColor("Ambient",ColorRGBA.White);
            terrain.setMaterial(material);renderManager.preloadScene(terrain);
            for(var g:surfaces) {
                g.getMaterial().clearParam("AmbientMap");g.getMaterial().clearParam("LightData");
                g.getMaterial().setColor("Ambient",ColorRGBA.White);
            }
        }
        if(frame==45) Main.getUserSettings().setSetting(Settings.Setting.ENHANCED_LIQUIDS,true);
        if(frame==50) {
            capture("enhanced-without-map-bindings");
            System.out.println("LIQUID_RENDER_CHECK_PASSED");stop();
        }
    }
    byte[] capture(String name) {
        ByteBuffer pixels=BufferUtils.createByteBuffer(WIDTH*HEIGHT*4);
        renderer.readFrameBuffer(viewPort.getOutputFrameBuffer(),pixels);
        byte[] bytes=new byte[pixels.capacity()];pixels.get(bytes);
        BufferedImage image=new BufferedImage(WIDTH,HEIGHT,BufferedImage.TYPE_INT_ARGB);
        for(int y=0;y<HEIGHT;y++)for(int x=0;x<WIDTH;x++) {
            int p=(y*WIDTH+x)*4;image.setRGB(x,HEIGHT-1-y,0xff000000|((bytes[p]&255)<<16)|((bytes[p+1]&255)<<8)|(bytes[p+2]&255));
        }
        try {Files.createDirectories(OUTPUT);ImageIO.write(image,"png",OUTPUT.resolve(name+".png").toFile());}
        catch(Exception e){throw new RuntimeException(e);}
        return bytes;
    }
    @Override public void handleError(String message,Throwable error) {
        System.err.println(message);if(error!=null)error.printStackTrace();failure=1;stop();
    }
    @Override public void destroy(){super.destroy();if(failure!=0)System.exit(failure);}
}
