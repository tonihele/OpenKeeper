import com.jme3.app.SimpleApplication;
import com.jme3.app.state.ScreenshotAppState;
import com.jme3.asset.plugins.FileLocator;
import com.jme3.font.BitmapText;
import com.jme3.light.AmbientLight;
import com.jme3.math.*;
import com.jme3.material.Material;
import com.jme3.scene.*;
import com.jme3.scene.shape.Box;
import com.jme3.system.AppSettings;
import com.simsilica.es.*;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Method;
import java.util.*;
import toniarts.openkeeper.game.component.*;
import toniarts.openkeeper.tools.convert.map.*;
import toniarts.openkeeper.view.PlayerEntityViewState;
import toniarts.openkeeper.view.control.DoorViewControl;
import toniarts.openkeeper.view.loader.DoorLoader;
import toniarts.openkeeper.utils.PathUtils;

/** Asset-backed rendering check; no full game or original-game assets are bundled. */
public class ShowSecretDoors extends SimpleApplication {
    static String reference, converted, captures;
    DefaultEntityData data = new DefaultEntityData();
    List<EntityId> ids = new ArrayList<>();
    List<Spatial> models = new ArrayList<>();
    PlayerEntityViewState view;
    Method update;
    ScreenshotAppState shots = new ScreenshotAppState(captures + java.io.File.separator, "doors-");
    BitmapText status;
    int phase=-1, captured=-1;
    float elapsed;
    public static void main(String[] args) {
        if(args.length!=3)throw new IllegalArgumentException("Expected reference-installation, converted-assets, capture-directory");
        reference=args[0];converted=args[1];captures=args[2];
        new java.io.File(captures).mkdirs();
        var app=new ShowSecretDoors();
        var settings=new AppSettings(true);
        settings.setResolution(1280,800);settings.setTitle("OpenKeeper #1365: owned, allied and enemy doors");
        settings.setAudioRenderer(null);settings.setFrameRate(30);
        app.setSettings(settings);app.setShowSettings(false);app.setPauseOnLostFocus(false);app.start();
    }
    void label(String text,float x,float y,int size) {
        var t=new BitmapText(assetManager.loadFont("Interface/Fonts/Default.fnt"));
        t.setText(text);t.setSize(size);t.setLocalTranslation(x,y,0);guiNode.attachChild(t);
    }
    public void simpleInitApp() {
        try {
            flyCam.setEnabled(false);setDisplayStatView(false);setDisplayFps(false);
            PathUtils.setDKIIFolder(PathUtils.fixFilePath(reference));
            assetManager.registerLocator(converted,FileLocator.class);
            var level=KwdFile.load(PathUtils.getDKIIFolder(),"Level1");
            var secret=level.getDoors().stream().filter(d->d.getFlags().contains(Door.DoorFlag.IS_SECRET)).findFirst().orElseThrow();
            var normal=level.getDoors().stream().filter(d->!d.getFlags().contains(Door.DoorFlag.IS_SECRET)&&!d.getFlags().contains(Door.DoorFlag.IS_BARRICADE)).findFirst().orElseThrow();
            view=new PlayerEntityViewState(level,assetManager,data,(short)3,null,rootNode);
            update=PlayerEntityViewState.class.getDeclaredMethod("updateDoorModelState",Spatial.class,Entity.class);update.setAccessible(true);
            rootNode.addLight(new AmbientLight(ColorRGBA.White.mult(1.2f)));
            viewPort.setBackgroundColor(new ColorRGBA(.045f,.055f,.07f,1));
            var floor=new Geometry("floor",new Box(6,.025f,4));
            var mat=new Material(assetManager,"Common/MatDefs/Misc/Unshaded.j3md");mat.setColor("Color",new ColorRGBA(.16f,.19f,.22f,1));floor.setMaterial(mat);floor.setLocalTranslation(5,-.05f,3);rootNode.attachChild(floor);
            var loader=new DoorLoader(level);
            for(int row=0;row<2;row++)for(int col=0;col<3;col++) {
                var definition=row==0?normal:secret;
                var id=data.createEntity(); short owner=(short)(3+col);
                var state=new DoorViewState(definition.getDoorId(),false,false,false,row==1);
                var pos=new Vector3f(2+col*3,0,1+row*4);
                data.setComponents(id,state,new Position(0,pos),new Owner(owner,owner),new Interaction(true,false,false,false,false));
                var model=loader.load(assetManager,state);
                if(model==null)throw new AssertionError("Missing actual door asset");
                model.addControl(new DoorViewControl(id,data,definition,state,assetManager,null,level.getObject(definition.getKeyObjectId())));
                model.setLocalTranslation(pos);rootNode.attachChild(model);ids.add(id);models.add(model);
                System.out.println("ASSET "+definition.getName()+" owner="+owner+" model="+model.getName());
            }
            cam.setLocation(new Vector3f(5,10,14));cam.lookAt(new Vector3f(5,0,3),Vector3f.UNIT_Y);
            label("#1365 - actual converted door assets / production visibility",28,770,25);
            label("OWN DOOR                    ALLIED DOOR                  ENEMY DOOR",130,700,23);
            label("Ordinary doors",30,630,19);label("Secret doors",30,380,19);
            label("Viewer: Keeper 3. Owner 4 is allied; owner 5 is an enemy. All tiles explored.",28,90,18);
            status=new BitmapText(assetManager.loadFont("Interface/Fonts/Default.fnt"));status.setSize(21);status.setLocalTranslation(28,55,0);guiNode.attachChild(status);
            stateManager.attach(shots);
        } catch(Exception e) {e.printStackTrace();System.exit(1);}
    }
    void setPhase(int p) throws Exception {
        for(int i=0;i<ids.size();i++) {
            var id=ids.get(i);var old=data.getComponent(id,DoorViewState.class);
            boolean hidden=i>=3&&p<2;
            var state=new DoorViewState(old.doorId,false,false,p==1,hidden);data.setComponent(id,state);
            update.invoke(view,models.get(i),data.getEntity(id,DoorViewState.class,Position.class,Owner.class));
            boolean shown=models.get(i).getLocalCullHint()!=Spatial.CullHint.Always;
            boolean expected=i<3||i==3||p==2;
            if(shown!=expected)throw new AssertionError("Visibility mismatch phase="+p+" door="+i);
            System.out.println("RENDER_ASSERT phase="+p+" door="+i+" hidden="+hidden+" shown="+shown+" PASS");
        }
        status.setText(new String[]{"1/3 CLOSED + UNDISCOVERED: only the owner sees secret doors","2/3 OPEN + UNDISCOVERED: allied and enemy secrets remain hidden","3/3 DISCOVERED + CLOSED: all players see secret doors; discovery persists"}[p]);
    }
    public void simpleUpdate(float tpf) {
        elapsed+=tpf;
        for(var model:models) toniarts.openkeeper.utils.AssetUtils.applyFullLight(model);
        int next=Math.min(2,(int)(elapsed/4));
        try {if(phase!=next){phase=next;setPhase(phase);}
            if(elapsed>phase*4+2&&captured!=phase){shots.takeScreenshot();captured=phase;}
            if(elapsed>12){System.out.println("RENDERED_DOOR_CHECK_PASS 18 visibility assertions; 3 captured phases");stop();}
        }catch(Exception e){e.printStackTrace();System.exit(1);}
    }
}
