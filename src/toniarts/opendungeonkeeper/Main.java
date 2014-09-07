package toniarts.opendungeonkeeper;

import com.jme3.app.SimpleApplication;
import com.jme3.asset.ModelKey;
import com.jme3.audio.AudioNode;
import com.jme3.renderer.RenderManager;
import com.jme3.scene.Spatial;
import java.io.File;
import toniarts.opendungeonkeeper.audio.plugins.MP2Loader;
import toniarts.opendungeonkeeper.tools.convert.AssetsConverter;

/**
 * test
 *
 * @author normenhansen
 */
public class Main extends SimpleApplication {

    private AudioNode audioSource;
    private float time = 0;
    private static String dkIIFolder;

    public static void main(String[] args) {

        //Take Dungeon Keeper 2 root folder as parameter
        if (args.length != 1 && !new File(args[0]).exists()) {
            throw new RuntimeException("Please provide Dungeon Keeper II main folder as a first parameter! Second parameter is the extraction target folder!");
        }
        dkIIFolder = args[0];

        Main app = new Main();
        app.start();
    }

    @Override
    public void simpleInitApp() {
//        Box b = new Box(1, 1, 1);
//        Geometry geom = new Geometry("Box", b);
//
//        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
//        mat.setColor("Color", ColorRGBA.Blue);
//        geom.setMaterial(mat);


//        rootNode.attachChild(geom);

        // Convert the assets
        AssetsConverter.convertAssets(dkIIFolder, assetManager);

        String key = "Models/Impkmf/Imp.kmf.j3o";

        //Create an imp on the map
        Spatial dg = this.getAssetManager().loadModel(new ModelKey(key));
        rootNode.attachChild(dg);

        //Sound
        this.getAssetManager().registerLoader(MP2Loader.class, "mp2");
        audioSource = new AudioNode(assetManager, "Sounds/horng014.mp2", false);
        audioSource.setLooping(false);
    }

    @Override
    public void simpleUpdate(float tpf) {
        time += tpf;
        if (time > 1f) {
            audioSource.playInstance();
            time = 0;
        }
    }

    @Override
    public void simpleRender(RenderManager rm) {
        //TODO: add render code
    }
}
