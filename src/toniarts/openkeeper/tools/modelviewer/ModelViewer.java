/*
 * Copyright (C) 2014-2015 OpenKeeper
 *
 * OpenKeeper is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * OpenKeeper is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with OpenKeeper.  If not, see <http://www.gnu.org/licenses/>.
 */
package toniarts.openkeeper.tools.modelviewer;

import com.jme3.animation.AnimChannel;
import com.jme3.animation.AnimControl;
import com.jme3.animation.LoopMode;
import com.jme3.app.SimpleApplication;
import com.jme3.asset.plugins.FileLocator;
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
import com.jme3.scene.Mesh;
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
import toniarts.openkeeper.gui.CursorFactory;
import toniarts.openkeeper.tools.convert.AssetsConverter;
import toniarts.openkeeper.tools.convert.KmfAssetInfo;
import toniarts.openkeeper.tools.convert.KmfModelLoader;
import toniarts.openkeeper.tools.convert.kmf.KmfFile;
import toniarts.openkeeper.tools.convert.map.KwdFile;
import toniarts.openkeeper.tools.convert.map.Terrain;
import toniarts.openkeeper.utils.AssetUtils;
import toniarts.openkeeper.utils.PathUtils;
import toniarts.openkeeper.world.MapLoader;
import toniarts.openkeeper.world.TerrainLoader;
import toniarts.openkeeper.world.effect.EffectManagerState;

/**
 * Simple model viewer
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class ModelViewer extends SimpleApplication implements ScreenController {

    public enum Types {

        MODELS("Models"), TERRAIN("Terrain"), /*OBJECTS("Objects"),*/ MAPS("Maps");
        private final String name;

        private Types(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }
    private static String dkIIFolder;
    private final Vector3f lightDir = new Vector3f(-1, -1, .5f).normalizeLocal();
    private DirectionalLight dl;
    private Nifty nifty;
    private Screen screen;
    private File kmfModel = null;
    private boolean wireframe = false;
    private boolean rotate = true;
    private boolean showNormals = false;
    private List<String> models;
    private List<String> maps;
    private KwdFile kwdFile;
    private Node floorGeom;
    /**
     * The node name for the model (that is attached to the root)
     */
    private static final String NODE_NAME = "SelectedModel";
    private static final String NODE_NAME_NORMALS = "Normals";
    private static final String KEY_MAPPING_SHOW_NORMALS = "show normals";
    private static final String KEY_MAPPING_TOGGLE_WIREFRAME = "toggle wireframe";
    private static final String KEY_MAPPING_TOGGLE_ROTATION = "toggle rotation";
    private static final Logger logger = Logger.getLogger(ModelViewer.class.getName());

    public static void main(String[] args) {

        //Take Dungeon Keeper 2 root folder as parameter
        if (args.length != 1 || !new File(args[0]).exists()) {
            dkIIFolder = PathUtils.getDKIIFolder();
            if (dkIIFolder == null) {
                throw new RuntimeException("Please provide Dungeon Keeper II main folder as a first parameter!");
            }
        } else {
            dkIIFolder = PathUtils.fixFilePath(args[0]);
        }

        ModelViewer app = new ModelViewer();
        app.start();
    }

    public ModelViewer(File kmfModel, String dkFolder) {
        this();
        this.kmfModel = kmfModel;
        dkIIFolder = dkFolder;
    }

    public ModelViewer() {
        super();
    }

    private void setupLighting() {

        // To make shadows, sun
        dl = new DirectionalLight();
        dl.setDirection(lightDir);
        dl.setColor(ColorRGBA.White);
        rootNode.addLight(dl);

        // Add ambient light
        AmbientLight al = new AmbientLight();
        al.setColor(ColorRGBA.White.multLocal(0.4f));
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

    private void setupFloor() {
        Material mat = assetManager.loadMaterial("Materials/ModelViewer/FloorMarble.j3m");

        floorGeom = new Node("floorGeom");
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
            if (KEY_MAPPING_TOGGLE_WIREFRAME.equals(name) && !pressed) {
                wireframe = !wireframe;
                toggleWireframe();
            } // Toggle rotation
            else if (KEY_MAPPING_TOGGLE_ROTATION.equals(name) && !pressed) {
                rotate = !rotate;
                toggleRotate();
            } // Normals
            else if (KEY_MAPPING_SHOW_NORMALS.equals(name) && !pressed) {
                showNormals = !showNormals;
                toggleShowNormals();
            }
        }
    };

    private void toggleWireframe() {
        Spatial spat = rootNode.getChild(ModelViewer.NODE_NAME);
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
        Spatial spat = rootNode.getChild(ModelViewer.NODE_NAME);
        if (spat != null) {
            RotatorControl rotator = spat.getControl(RotatorControl.class);
            if (rotator != null) {
                rotator.setEnabled(rotate);
            }
        }
    }

    private void toggleShowNormals() {
        Spatial spat = rootNode.getChild(ModelViewer.NODE_NAME);

        if (spat != null && spat instanceof Node) {

            // See if it already has the normal meshes generated
            Node normals = (Node) ((Node) spat).getChild(ModelViewer.NODE_NAME_NORMALS);
            if (normals != null) {
                normals.setCullHint(showNormals ? Spatial.CullHint.Never : Spatial.CullHint.Always);
            } else if (showNormals) {

                // Generate
                final Node nodeNormals = new Node(ModelViewer.NODE_NAME_NORMALS);

                spat.depthFirstTraversal(new SceneGraphVisitor() {
                    @Override
                    public void visit(Spatial spatial) {
                        if (spatial instanceof Geometry) {
                            Geometry g = (Geometry) spatial;
                            Mesh normalMesh = TangentBinormalGenerator.genNormalLines(g.getMesh(), 0.1f);
                            Geometry normalGeometry = new Geometry(g.getName() + "Normal", normalMesh);
                            Material mat = new Material(assetManager,
                                    "Common/MatDefs/Misc/Unshaded.j3md");
                            mat.setColor("Color", ColorRGBA.Red);
                            normalGeometry.setMaterial(mat);
                            nodeNormals.attachChild(normalGeometry);
                        }
                    }
                });
                nodeNormals.setCullHint(Spatial.CullHint.Never);
                ((Node) spat).attachChild(nodeNormals);
            }
        }
    }

    @Override
    public void simpleInitApp() {

        // Distribution locator
        getAssetManager().registerLocator(AssetsConverter.getAssetsFolder(), FileLocator.class);

        // The GUI
        NiftyJmeDisplay niftyDisplay = new NiftyJmeDisplay(assetManager,
                inputManager,
                audioRenderer,
                guiViewPort);
        nifty = niftyDisplay.getNifty();
        guiViewPort.addProcessor(niftyDisplay);
        nifty.fromXml("Interface/ModelViewer/ModelViewer.xml", "start", this);

        // Set default font
        RenderFont font = nifty.createFont("Interface/Fonts/Frontend14.fnt");
        nifty.getRenderEngine().setFont(font);
        nifty.registerMouseCursor("pointer", "Interface/Cursors/Idle.png", 4, 4);

        cam.setLocation(new Vector3f(-15.445636f, 30.162927f, 60.252777f));
        cam.setRotation(new Quaternion(0.05173137f, 0.92363626f, -0.13454558f, 0.35513034f));
        flyCam.setMoveSpeed(30);
        flyCam.setDragToRotate(true);

        // Mouse cursor
        inputManager.setCursorVisible(true);
        inputManager.setMouseCursor(CursorFactory.getCursor(CursorFactory.CursorType.POINTER, assetManager));

        // Wireframe
        inputManager.addMapping(KEY_MAPPING_TOGGLE_WIREFRAME, new KeyTrigger(KeyInput.KEY_T));
        inputManager.addListener(actionListener, KEY_MAPPING_TOGGLE_WIREFRAME);

        // Rotation
        inputManager.addMapping(KEY_MAPPING_TOGGLE_ROTATION, new KeyTrigger(KeyInput.KEY_R));
        inputManager.addListener(actionListener, KEY_MAPPING_TOGGLE_ROTATION);

        // Normals
        inputManager.addMapping(KEY_MAPPING_SHOW_NORMALS, new KeyTrigger(KeyInput.KEY_N));
        inputManager.addListener(actionListener, KEY_MAPPING_SHOW_NORMALS);

        setupLighting();
        setupFloor();

        // Open a KMF model if set
        if (kmfModel != null) {
            try {
                KmfFile kmf = new KmfFile(kmfModel);
                KmfModelLoader loader = new KmfModelLoader();
                KmfAssetInfo asset = new KmfAssetInfo(assetManager, null, kmf, AssetsConverter.getEngineTexturesFile(dkIIFolder), false);
                Node node = (Node) loader.load(asset);
                setupModel(node, false);
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Failed to handle: " + kmfModel, e);
            }
        }
    }

    /**
     * Fill the listbox with items
     *
     * @param object list of objects (cached)
     * @param rootDirectory the root directory (i.e. DK II dir or the dev dir),
     * must be relative to the actual directory of where the objects are
     * gathered
     * @param directory the actual directory where the objects are get
     * @param extension the file extension of the objects wanted
     */
    public void fillWithFiles(List<String> object, final String rootDirectory, final String directory, final String extension) {
        ListBox<String> listBox = getModelListBox();

        if (object == null) {

            //Find all the models
            object = new ArrayList<>();
            File f = new File(directory);
            File[] files = f.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return name.toLowerCase().endsWith(".".concat(extension));
                }
            });
            Path path = new File(rootDirectory).toPath();
            for (File file : files) {
                String key = path.relativize(file.toPath()).toString();
                object.add(key.substring(0, key.length() - 4));
            }
        }

        // Add & sort
        listBox.addAllItems(object);
        listBox.sortAllItems(String.CASE_INSENSITIVE_ORDER);
    }

    private void fillTerrain() {
        KwdFile kwfFile = getKwdFile();
        Collection<Terrain> terrains = kwfFile.getTerrainList();
        getModelListBox().addAllItems(Arrays.asList(terrains.toArray()));
    }

    private void fillObjects() {
        KwdFile kwfFile = getKwdFile();
        Collection<toniarts.openkeeper.tools.convert.map.Object> objects = kwfFile.getObjectList();
        getModelListBox().addAllItems(Arrays.asList(objects.toArray()));
    }

    @NiftyEventSubscriber(id = "modelListBox")
    public void onListBoxSelectionChanged(final String id, final ListBoxSelectionChangedEvent<Object> event) {
        List<Object> selection = event.getSelection();
        if (selection.size() == 1) {

            switch (getTypeDropDown().getSelection()) {
                case MODELS: {

                    // Load the selected model
                    Node spat = (Node) AssetUtils.loadModel(assetManager, ((String) selection.get(0)).concat(".j3o").replaceAll(Matcher.quoteReplacement(File.separator), "/"), false);
                    setupModel(spat, false);
                    break;
                }
                case TERRAIN: {

                    // Load the selected terrain
                    Node spat = (Node) new TerrainLoader().load(this.getAssetManager(), (Terrain) selection.get(0));
                    setupModel(spat, false);
                    break;
                }
                case MAPS: {

                    // Load the selected map
                    String file = ((String) selection.get(0)).concat(".kwd").replaceAll(Matcher.quoteReplacement(File.separator), "/");
                    KwdFile kwd = new KwdFile(dkIIFolder, new File(dkIIFolder.concat(file)));
                    Node spat = (Node) new MapLoader(this.getAssetManager(), kwd, new EffectManagerState(kwdFile, this.getAssetManager()), null) {
                        @Override
                        protected void updateProgress(int progress, int max) {
                            // Do nothing
                        }
                    }.load(this.getAssetManager(), kwd);
                    setupModel(spat, true);
                    break;
                }
//                case OBJECTS: {
//
//                    // Load the selected object
//                    Node spat = (Node) new ObjectLoader().load(this.getAssetManager(), (toniarts.openkeeper.tools.convert.map.Object) selection.get(0));
//                    setupModel(spat, false);
//                    break;
//                }
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

    private void setupModel(final Node spat, boolean isMap) {
        spat.setName(NODE_NAME);

        if (!isMap) {

            // Reset the game translation and scale
            for (Spatial subSpat : spat.getChildren()) {
                subSpat.setLocalScale(1);
                subSpat.setLocalTranslation(0, 0, 0);
            }

            // Make it bigger and move
            spat.scale(10);
            spat.setLocalTranslation(10, 25, 30);

            // Make it rotate
            RotatorControl rotator = new RotatorControl();
            rotator.setEnabled(rotate);
            spat.addControl(rotator);
        }

        // Hide the floor on maps
        floorGeom.setCullHint(!isMap ? Spatial.CullHint.Never : Spatial.CullHint.Always);

        // Shadows
        spat.setShadowMode(RenderQueue.ShadowMode.CastAndReceive);

        // Remove the old model
        rootNode.detachChildNamed(NODE_NAME);

        // Attach the new model
        rootNode.attachChild(spat);

        // Wireframe status
        toggleWireframe();

        // Normals status
        toggleShowNormals();

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
                fillWithFiles(models, AssetsConverter.getAssetsFolder(), AssetsConverter.getAssetsFolder().concat(AssetsConverter.MODELS_FOLDER).concat(File.separator), "j3o");
                break;
            }
            case TERRAIN: {
                fillTerrain();
                break;
            }
            case MAPS: {
                fillWithFiles(maps, dkIIFolder, dkIIFolder.concat(AssetsConverter.MAPS_FOLDER), "kwd");
                break;
            }
//            case OBJECTS: {
//                fillObjects();
//                break;
//            }
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
            kwdFile = new KwdFile(dkIIFolder, new File(dkIIFolder.concat(PathUtils.DKII_DATA_FOLDER).concat(File.separator).concat(PathUtils.DKII_EDITOR_FOLDER).concat(File.separator).concat(PathUtils.DKII_MAPS_FOLDER).concat(File.separator).concat("Alcatraz.kwd")));
        }
        return kwdFile;
    }
}
