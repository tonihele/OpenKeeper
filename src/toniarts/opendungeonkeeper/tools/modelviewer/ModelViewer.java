package toniarts.opendungeonkeeper.tools.modelviewer;

import com.jme3.animation.AnimChannel;
import com.jme3.animation.AnimControl;
import com.jme3.animation.LoopMode;
import com.jme3.app.SimpleApplication;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.niftygui.NiftyJmeDisplay;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.SceneGraphVisitor;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Quad;
import com.jme3.shadow.DirectionalLightShadowRenderer;
import com.jme3.shadow.EdgeFilteringMode;
import com.jme3.util.TangentBinormalGenerator;
import de.lessvoid.nifty.Nifty;
import de.lessvoid.nifty.NiftyEventSubscriber;
import de.lessvoid.nifty.controls.DropDown;
import de.lessvoid.nifty.controls.DropDownSelectionChangedEvent;
import de.lessvoid.nifty.controls.ListBox;
import de.lessvoid.nifty.controls.ListBoxSelectionChangedEvent;
import de.lessvoid.nifty.screen.Screen;
import de.lessvoid.nifty.screen.ScreenController;
import de.lessvoid.nifty.spi.render.RenderFont;
import java.io.File;
import java.io.FilenameFilter;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import toniarts.opendungeonkeeper.gui.CursorFactory;
import toniarts.opendungeonkeeper.tools.convert.AssetsConverter;
import toniarts.opendungeonkeeper.tools.convert.KmfAssetInfo;
import toniarts.opendungeonkeeper.tools.convert.KmfModelLoader;
import toniarts.opendungeonkeeper.tools.convert.kmf.KmfFile;
import toniarts.opendungeonkeeper.tools.convert.map.KwdFile;
import toniarts.opendungeonkeeper.tools.convert.map.Terrain;
import toniarts.opendungeonkeeper.tools.convert.map.loader.TerrainLoader;

/**
 * Simple model viewer
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class ModelViewer extends SimpleApplication implements ScreenController {

    public enum Types {

        MODELS("Models"), TERRAIN("Terrain");
        private String name;

        private Types(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }
    private final String dkIIFolder;
    private static boolean convertAssets = false;
    private Vector3f lightDir = new Vector3f(-1, -1, .5f).normalizeLocal();
    private DirectionalLight dl;
    private Nifty nifty;
    private Screen screen;
    private final File kmfModel;
    private final String name = "SelectedModel";
    private boolean wireframe = false;
    private boolean rotate = true;
    private List<String> models;
    private KwdFile kwdFile;
    private static final Logger logger = Logger.getLogger(ModelViewer.class.getName());

    public static void main(String[] args) {

        //Take Dungeon Keeper 2 root folder as parameter
        if (convertAssets && args.length != 1 && !new File(args[0]).exists()) {
            throw new RuntimeException("Please provide Dungeon Keeper II main folder as a first parameter! Second parameter is the extraction target folder!");
        }

        ModelViewer app = new ModelViewer(null, args[0]);
        app.start();
    }

    public ModelViewer(File kmfModel, String dkIIFolder) {
        super();

        this.kmfModel = kmfModel;
        this.dkIIFolder = dkIIFolder;
    }

    public void setupLighting() {

        // To make shadows, sun
        dl = new DirectionalLight();
        dl.setDirection(lightDir);
        dl.setColor(ColorRGBA.White);
        rootNode.addLight(dl);

        // Add ambient light
        AmbientLight al = new AmbientLight();
        al.setColor(ColorRGBA.White);
        rootNode.addLight(al);

        /* Drop shadows */
        final int SHADOWMAP_SIZE = 1024;
        DirectionalLightShadowRenderer dlsr = new DirectionalLightShadowRenderer(getAssetManager(), SHADOWMAP_SIZE, 3);
        dlsr.setLight(dl);
        dlsr.setLambda(0.55f);
        dlsr.setShadowIntensity(0.6f);
        dlsr.setEdgeFilteringMode(EdgeFilteringMode.PCF8);
        getViewPort().addProcessor(dlsr);
    }

    public void setupFloor() {
        Material mat = assetManager.loadMaterial("Materials/ModelViewer/FloorMarble.j3m");

        Node floorGeom = new Node("floorGeom");
        Quad q = new Quad(100, 100);
        q.scaleTextureCoordinates(new Vector2f(10, 10));
        Geometry g = new Geometry("geom", q);
        g.setLocalRotation(new Quaternion().fromAngleAxis(-FastMath.HALF_PI, Vector3f.UNIT_X));
        g.setShadowMode(RenderQueue.ShadowMode.Receive);
        floorGeom.attachChild(g);

        TangentBinormalGenerator.generate(floorGeom);
        floorGeom.setLocalTranslation(-50, 22, 60);

        floorGeom.setMaterial(mat);
        rootNode.attachChild(floorGeom);
    }
    private ActionListener actionListener = new ActionListener() {
        @Override
        public void onAction(String name, boolean pressed, float tpf) {

            // Toggle wireframe
            if (name.equals("toggle wireframe") && !pressed) {
                wireframe = !wireframe;
                toggleWireframe();
            } // Toggle rotation
            else if (name.equals("toggle rotation") && !pressed) {
                rotate = !rotate;
                toggleRotate();
            }
        }
    };

    private void toggleWireframe() {
        Spatial spat = rootNode.getChild(ModelViewer.this.name);
        if (spat != null) {
            spat.depthFirstTraversal(new SceneGraphVisitor() {
                @Override
                public void visit(Spatial spatial) {
                    if (spatial instanceof Geometry) {
                        ((Geometry) spatial).getMaterial().getAdditionalRenderState().setWireframe(wireframe);
                    }
                }
            });
        }
    }

    private void toggleRotate() {
        Spatial spat = rootNode.getChild(ModelViewer.this.name);
        if (spat != null) {
            RotatorControl rotator = spat.getControl(RotatorControl.class);
            if (rotator != null) {
                rotator.setEnabled(rotate);
            }
        }
    }

    @Override
    public void simpleInitApp() {

        // Convert the assets
        if (convertAssets) {
            AssetsConverter assetsConverter = new AssetsConverter(dkIIFolder, assetManager) {
                @Override
                protected void updateStatus(Integer currentProgress, Integer totalProgress, AssetsConverter.ConvertProcess process) {
                    //
                }
            };
            assetsConverter.convertAssets();
        }

        // The GUI
        NiftyJmeDisplay niftyDisplay = new NiftyJmeDisplay(assetManager,
                inputManager,
                audioRenderer,
                guiViewPort);
        nifty = niftyDisplay.getNifty();
        guiViewPort.addProcessor(niftyDisplay);
        nifty.fromXml("Interface/ModelViewer/ModelViewer.xml", "start", this);

        // Set default font
        RenderFont font = nifty.createFont("Interface/Fonts/DungeonKeeperII.fnt");
        nifty.getRenderEngine().setFont(font);
        nifty.registerMouseCursor("pointer", "Interface/Cursors/Idle.png", 4, 4);

        cam.setLocation(new Vector3f(-15.445636f, 30.162927f, 60.252777f));
        cam.setRotation(new Quaternion(0.05173137f, 0.92363626f, -0.13454558f, 0.35513034f));
        flyCam.setMoveSpeed(30);
        flyCam.setDragToRotate(true);

        // Mouse cursor
        inputManager.setCursorVisible(true);
        inputManager.setMouseCursor(CursorFactory.getCursor(CursorFactory.Cursor.POINTER, assetManager));

        // Wireframe
        inputManager.addMapping("toggle wireframe", new KeyTrigger(KeyInput.KEY_T));
        inputManager.addListener(actionListener, "toggle wireframe");

        // Rotation
        inputManager.addMapping("toggle rotation", new KeyTrigger(KeyInput.KEY_R));
        inputManager.addListener(actionListener, "toggle rotation");

        setupLighting();
        setupFloor();

        // Open a KMF model if set
        if (kmfModel != null) {
            try {
                KmfFile kmf = new KmfFile(kmfModel);
                KmfModelLoader loader = new KmfModelLoader();
                KmfAssetInfo asset = new KmfAssetInfo(assetManager, null, kmf, AssetsConverter.getEngineTexturesFile(dkIIFolder));
                Node node = (Node) loader.load(asset);
                setupModel(node);
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Failed to handle: " + kmfModel, e);
            }
        }
    }

    /**
     * Fill the listbox with items. In this case with JustAnExampleModelClass.
     */
    public void fillModels() {
        ListBox listBox = getModelListBox();

        if (models == null) {

            //Find all the models
            models = new ArrayList<>();
            File f = new File(AssetsConverter.getAssetsFolder().concat(AssetsConverter.MODELS_FOLDER).concat(File.separator));
            File[] files = f.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return name.toLowerCase().endsWith(".j3o");
                }
            });
            Path path = new File(AssetsConverter.getAssetsFolder()).toPath();
            for (File file : files) {
                String key = path.relativize(file.toPath()).toString();
                models.add(key.substring(0, key.length() - 4));
            }
        }

        // Add & sort
        listBox.addAllItems(models);
        listBox.sortAllItems();
    }

    private void fillTerrain() {
        KwdFile kwfFile = getKwdFile();
        Collection<Terrain> terrains = kwfFile.getTerrainList();
        getModelListBox().addAllItems(Arrays.asList(terrains.toArray()));
    }

    @NiftyEventSubscriber(id = "modelListBox")
    public void onListBoxSelectionChanged(final String id, final ListBoxSelectionChangedEvent<Object> event) {
        List<Object> selection = event.getSelection();
        if (selection.size() == 1) {

            switch (getTypeDropDown().getSelection()) {
                case MODELS: {

                    // Load the selected model
                    Node spat = (Node) this.getAssetManager().loadModel(((String) selection.get(0)).concat(".j3o").replaceAll(Matcher.quoteReplacement(File.separator), "/"));
                    setupModel(spat);
                    break;
                }
                case TERRAIN: {

                    // Load the selected terrain
                    Node spat = (Node) new TerrainLoader().load(this.getAssetManager(), (Terrain) selection.get(0));
                    setupModel(spat);
                    break;
                }
            }
        }
    }

    @NiftyEventSubscriber(id = "typeCombo")
    public void onTypeChanged(final String id, final DropDownSelectionChangedEvent<Types> event) {
        fillList(event.getSelection());
    }

    @Override
    public void simpleUpdate(float tpf) {
    }

    @Override
    public void simpleRender(RenderManager rm) {
        //TODO: add render code
    }

    @Override
    public void bind(Nifty nifty, Screen screen) {
        this.screen = screen;

        // Fill types
        fillTypes();
    }

    @Override
    public void onStartScreen() {
    }

    @Override
    public void onEndScreen() {
    }

    private void setupModel(Node spat) {
        spat.setName(name);

        // Reset the game translation and scale
        for (Spatial subSpat : spat.getChildren()) {
            subSpat.setLocalScale(1);
            subSpat.setLocalTranslation(0, 0, 0);
        }

        // Make it bigger and move
        spat.scale(10);
        spat.setLocalTranslation(10, 25, 30);

        // Rotate it
        Quaternion quat = new Quaternion();
        quat.fromAngleAxis(FastMath.PI / 2, new Vector3f(1, 0, 0));
        spat.rotate(quat);

        // Shadows
        spat.setShadowMode(RenderQueue.ShadowMode.CastAndReceive);

        // Make it rotate
        RotatorControl rotator = new RotatorControl();
        rotator.setEnabled(rotate);
        spat.addControl(rotator);

        // Remove the old model
        rootNode.detachChildNamed(name);

        // Attach the new model
        rootNode.attachChild(spat);

        // Wireframe status
        toggleWireframe();

        // Animate!
        AnimControl animControl = (AnimControl) spat.getChild(0).getControl(AnimControl.class);
        if (animControl != null) {
            AnimChannel channel = animControl.createChannel();
            channel.setAnim("anim");
            channel.setLoopMode(LoopMode.Loop);
        }
    }

    /**
     * Fills in the different types of objects we have
     */
    private void fillTypes() {
        DropDown<Types> dropDown = getTypeDropDown();
        if (dkIIFolder != null) {
            dropDown.addAllItems(Arrays.asList(Types.values()));
        } else {
            dropDown.addItem(Types.MODELS);
        }

        // Select the first one
        dropDown.selectItemByIndex(0);
        fillList(dropDown.getSelection());
    }

    /**
     * Fill the list box with the objects of currently selected type
     *
     * @param type the selected type
     */
    private void fillList(Types type) {
        getModelListBox().clear();
        switch (type) {
            case MODELS: {
                fillModels();
                break;
            }
            case TERRAIN: {
                fillTerrain();
                break;
            }
        }
    }

    /**
     * Get the listbox holding the models
     *
     * @return the listbox
     */
    private ListBox getModelListBox() {
        ListBox<Object> listBox = (ListBox<Object>) screen.findNiftyControl("modelListBox", ListBox.class);
        return listBox;
    }

    /**
     * Get the dropdown selection for type
     *
     * @return the dropdown
     */
    private DropDown<Types> getTypeDropDown() {
        DropDown<Types> dropDown = (DropDown<Types>) screen.findNiftyControl("typeCombo", DropDown.class);
        return dropDown;
    }

    private synchronized KwdFile getKwdFile() {
        if (kwdFile == null) {

            // Read Alcatraz.kwd by default
            kwdFile = new KwdFile(dkIIFolder, new File(dkIIFolder.concat("Data").concat(File.separator).concat("editor").concat(File.separator).concat("maps").concat(File.separator).concat("Alcatraz.kwd")));
        }
        return kwdFile;
    }
}
