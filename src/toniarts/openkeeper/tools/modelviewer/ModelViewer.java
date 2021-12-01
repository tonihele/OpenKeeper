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
import com.jme3.app.SimpleApplication;
import com.jme3.asset.plugins.FileLocator;
import com.jme3.audio.AudioNode;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.light.LightProbe;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
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
import com.jme3.util.mikktspace.MikktspaceTangentGenerator;
import de.lessvoid.nifty.Nifty;
import de.lessvoid.nifty.controls.DropDown;
import de.lessvoid.nifty.controls.ListBox;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import toniarts.openkeeper.Main;
import toniarts.openkeeper.audio.plugins.MP2Loader;
import toniarts.openkeeper.game.MapSelector;
import toniarts.openkeeper.game.data.ISoundable;
import toniarts.openkeeper.game.sound.SoundCategory;
import toniarts.openkeeper.game.sound.SoundFile;
import toniarts.openkeeper.game.sound.SoundGroup;
import toniarts.openkeeper.gui.CursorFactory;
import toniarts.openkeeper.tools.convert.AssetsConverter;
import toniarts.openkeeper.tools.convert.KmfAssetInfo;
import toniarts.openkeeper.tools.convert.KmfModelLoader;
import toniarts.openkeeper.tools.convert.kmf.KmfFile;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.Door;
import toniarts.openkeeper.tools.convert.map.Effect;
import toniarts.openkeeper.tools.convert.map.GameLevel;
import toniarts.openkeeper.tools.convert.map.GameObject;
import toniarts.openkeeper.tools.convert.map.KwdFile;
import toniarts.openkeeper.tools.convert.map.Room;
import toniarts.openkeeper.tools.convert.map.Shot;
import toniarts.openkeeper.tools.convert.map.Terrain;
import toniarts.openkeeper.tools.convert.map.Trap;
import toniarts.openkeeper.utils.AssetUtils;
import toniarts.openkeeper.utils.PathUtils;
import toniarts.openkeeper.world.animation.AnimationLoader;
import toniarts.openkeeper.world.effect.EffectManagerState;

/**
 * Simple model viewer
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class ModelViewer extends SimpleApplication {

    public enum Types {

        MODELS("Models"),
        TERRAIN("Terrain"),
        OBJECTS("Objects"),
        MAPS("Maps"),
        CREATURES("Creatures"),
        ROOMS("Rooms"),
        DOORS("Doors"),
        TRAPS("Traps"),
        SHOTS("Shots"),
        EFFECTS("Effects");

        private final String name;

        private Types(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }
    //private final static float SCALE = 2;
    private static String dkIIFolder;
    private final Vector3f lightDir = new Vector3f(-1, -1, .5f).normalizeLocal();
    private DirectionalLight dl;
    private NiftyJmeDisplay niftyDisplay;
    private ModelViewerScreenController screen;
    private Path kmfModel = null;
    private boolean wireframe = false;
    private boolean rotate = true;
    private boolean showNormals = false;
    private List<String> models;
    private List<String> maps;
    private KwdFile kwdFile;
    private Node floorGeom;

    //private SoundsLoader soundLoader;
    /**
     * The node name for the model (that is attached to the root)
     */
    private static final String NODE_NAME = "SelectedModel";
    private static final String NODE_NAME_NORMALS = "Normals";
    private static final String KEY_MAPPING_SHOW_NORMALS = "show normals";
    private static final String KEY_MAPPING_TOGGLE_WIREFRAME = "toggle wireframe";
    private static final String KEY_MAPPING_TOGGLE_ROTATION = "toggle rotation";
    private static final Logger LOGGER = Logger.getLogger(ModelViewer.class.getName());

    private EffectManagerState effectManagerState;
    private MapLoaderAppState mapLoaderAppState;

    private final ActionListener actionListener = new ActionListener() {
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

    public static void main(String[] args) {

        // Take Dungeon Keeper 2 root folder as parameter
        if (args.length != 1 || !Files.exists(Paths.get(args[0]))) {
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

    public ModelViewer(Path kmfModel, String dkFolder) {
        this();
        this.kmfModel = kmfModel;
        dkIIFolder = dkFolder;
    }

    public ModelViewer() {
        super();
    }


    @Override
    public void simpleInitApp() {

        // Distribution locator
        assetManager.registerLocator(AssetsConverter.getAssetsFolder(), FileLocator.class);
        assetManager.registerLoader(MP2Loader.class, "mp2");

        //Effects manager
        this.effectManagerState = new EffectManagerState(getKwdFile(), assetManager);
        stateManager.attach(effectManagerState);
        // init sound loader
        //soundLoader = new SoundsLoader(assetManager);

        // Map loader
        mapLoaderAppState = new MapLoaderAppState();
        stateManager.attach(mapLoaderAppState);

        Nifty nifty = getNifty();
        screen = new ModelViewerScreenController(this);
        nifty.registerScreenController(screen);

        try {
            byte[] xml = PathUtils.readInputStream(Main.class.getResourceAsStream("/Interface/ModelViewer/ModelViewer.xml"));
            nifty.validateXml(new ByteArrayInputStream(xml));
            nifty.addXml(new ByteArrayInputStream(xml));
        } catch (Exception ex) {
            throw new RuntimeException("Failed to validate GUI file!", ex);
        }

        screen.goToScreen(ModelViewerScreenController.ID_SCREEN);

        cam.setLocation(new Vector3f(0, 10, 10));
        cam.lookAt(Vector3f.ZERO, Vector3f.UNIT_Y);
        flyCam.setMoveSpeed(10);
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
        setupDebug();

        // Open a KMF model if set
        if (kmfModel != null) {
            try {
                KmfFile kmf = new KmfFile(kmfModel);
                KmfModelLoader loader = new KmfModelLoader();
                KmfAssetInfo asset = new KmfAssetInfo(assetManager, null, kmf, false);
                Node node = (Node) loader.load(asset);
                setupModel(node, false);
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Failed to handle: " + kmfModel, e);
            }
        }
    }

    @Override
    public void simpleUpdate(float tpf) {
    }

    @Override
    public void simpleRender(RenderManager rm) {
        //TODO: add render code
    }

    private NiftyJmeDisplay getNiftyDisplay() {
        if (niftyDisplay == null) {
            niftyDisplay = new NiftyJmeDisplay(assetManager,
                inputManager,
                audioRenderer,
                guiViewPort);

            guiViewPort.addProcessor(niftyDisplay);
        }

        return niftyDisplay;
    }

    protected Nifty getNifty() {
        return getNiftyDisplay().getNifty();
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

        // Default light probe
        Spatial probeHolder = assetManager.loadModel("Models/ModelViewer/studio.j3o");
        LightProbe probe = (LightProbe) probeHolder.getLocalLightList().get(0);
        probe.setPosition(Vector3f.ZERO);
        probeHolder.removeLight(probe);
        rootNode.addLight(probe);

        // Light debug
        //LightsDebugState debugState = new LightsDebugState();
        //stateManager.attach(debugState);
    }

    private void setupFloor() {

        Material floorMaterial = new Material(assetManager, "Common/MatDefs/Light/PBRLighting.j3md");
        floorMaterial.setTexture("BaseColorMap", assetManager.loadTexture("Textures/ModelViewer/1K-marble_tiles_2-texture.jpg"));
        floorMaterial.setTexture("NormalMap", assetManager.loadTexture("Textures/ModelViewer/1K-marble_tiles_2-normal.jpg"));
        floorMaterial.setTexture("SpecularMap", assetManager.loadTexture("Textures/ModelViewer/1K-marble_tiles_2-specular.jpg"));
        floorMaterial.setTexture("LightMap", assetManager.loadTexture("Textures/ModelViewer/1K-marble_tiles_2-ao.jpg"));
        floorMaterial.setBoolean("LightMapAsAOMap", true);
        floorMaterial.setTexture("ParallaxMap", assetManager.loadTexture("Textures/ModelViewer/1K-marble_tiles_2-displacement.jpg"));
        floorMaterial.setBoolean("SteepParallax", true);
        floorMaterial.setFloat("Roughness", 0.1f);
        floorMaterial.setFloat("Metallic", 0.04f);

        floorGeom = new Node("floorGeom");
        Quad q = new Quad(20, 20);
        Geometry g = new Geometry("geom", q);
        g.setLocalRotation(new Quaternion().fromAngleAxis(-FastMath.HALF_PI, Vector3f.UNIT_X));
        g.setShadowMode(RenderQueue.ShadowMode.Receive);
        floorGeom.attachChild(g);

        MikktspaceTangentGenerator.generate(g);
        floorGeom.setLocalTranslation(-10, -1f, 10);

        floorGeom.setMaterial(floorMaterial);
        rootNode.attachChild(floorGeom);
    }

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
                            Mesh normalMesh = TangentBinormalGenerator.genTbnLines(g.getMesh(), 0.1f);
                            Geometry normalGeometry = new Geometry(g.getName() + "Normal", normalMesh);
                            Material mat = new Material(assetManager,
                                    "Common/MatDefs/Misc/Unshaded.j3md");
                            mat.setColor("Color", ColorRGBA.Red);
                            normalGeometry.setMaterial(mat);
                            nodeNormals.attachChild(normalGeometry);
                            
                            g.setMaterial(new Material(assetManager,
                                    "Common/MatDefs/Misc/ShowNormals.j3md"));
                        }
                    }
                });
                nodeNormals.setCullHint(Spatial.CullHint.Never);
                nodeNormals.setLocalTranslation(((Node) spat).getChild(0).getLocalTranslation());
                ((Node) spat).attachChild(nodeNormals);
            }
        }
    }

    public void onSelectionChanged(Object selection) {
        effectManagerState.setEnabled(false);

        switch (screen.getTypeControl().getSelection()) {
            case MODELS: {
                // Load the selected model
                Node spat = (Node) AssetUtils.loadAsset(assetManager, (String) selection);

                screen.setupItem(null, null);
                setupModel(spat, false);
                break;
            }
            case TERRAIN: {
                // Load the selected terrain
                Terrain terrain = (Terrain) selection;
                effectManagerState.setEnabled(true);
                Node spat = (Node) new TerrainsLoader().load(this.getAssetManager(),
                        effectManagerState, terrain);
                setupModel(spat, false);

                screen.setupItem(terrain, loadSoundCategory(terrain));
                break;
            }
            case MAPS: {
                // Load the selected map
                String file = (String) selection + ".kwd";
                KwdFile kwd = new KwdFile(dkIIFolder, Paths.get(dkIIFolder, PathUtils.DKII_MAPS_FOLDER, file));
                Node spat = mapLoaderAppState.loadMap(kwd);

                GameLevel gameLevel = kwd.getGameLevel();
                screen.setupItem(gameLevel, loadSoundCategory(gameLevel, false));
                setupModel(spat, true);
                break;
            }
            case OBJECTS: {
                // Load the selected object
                GameObject object = (GameObject) selection;
                effectManagerState.setEnabled(true);
                Node spat = (Node) new ObjectsLoader().load(this.getAssetManager(),
                        effectManagerState, object);
                setupModel(spat, false);

                screen.setupItem(object, loadSoundCategory(object));
                break;
            }
            case CREATURES: {
                // Load the selected creature
                Creature creature = (Creature) selection;
                effectManagerState.setEnabled(true);
                Node spat = (Node) new CreaturesLoader().load(this.getAssetManager(),
                        effectManagerState, creature);
                setupModel(spat, false);

                screen.setupItem(creature, loadSoundCategory(creature));
                break;
            }
            case TRAPS: {
                // Load the selected trap
                Trap trap = (Trap) selection;
                effectManagerState.setEnabled(true);
                Node spat = (Node) new TrapsLoader().load(this.getAssetManager(),
                        effectManagerState, trap);
                setupModel(spat, false);

                screen.setupItem(trap, loadSoundCategory(trap));
                break;
            }
            case DOORS: {
                // Load the selected door
                Door door = (Door) selection;
                effectManagerState.setEnabled(true);
                Node spat = (Node) new DoorsLoader().load(this.getAssetManager(),
                        effectManagerState, door);
                setupModel(spat, false);

                screen.setupItem(door, loadSoundCategory(door));
                break;
            }
            case ROOMS: {
                // Load the selected room
                Room room = (Room) selection;
                effectManagerState.setEnabled(true);
                Node spat = (Node) new RoomsLoader().load(this.getAssetManager(),
                        effectManagerState, room);
                setupModel(spat, false);

                screen.setupItem(room, loadSoundCategory(room));
                break;
            }
            case SHOTS: {
                // Load the selected shot
                Shot shot = (Shot) selection;
                effectManagerState.setEnabled(true);
                Node spat = (Node) new ShotsLoader().load(this.getAssetManager(),
                        effectManagerState, shot);
                setupModel(spat, false);

                screen.setupItem(shot, loadSoundCategory(shot));
                break;
            }
            case EFFECTS: {
                // Load the selected effect
                Node spat = new Node();
                Effect effect = (Effect) selection;
                effectManagerState.setEnabled(true);
                // Load the selected effect
                effectManagerState.loadSingleEffect(spat, new Vector3f(0, 0, 0),
                        effect.getEffectId(), true);
                setupModel(spat, false);

                screen.setupItem(effect, null);
                break;
            }
        }

    }

    private void setupDebug() {
        Debug.showNodeAxes(assetManager, rootNode, 10);
        Debug.attachWireFrameDebugGrid(assetManager, rootNode, Vector3f.ZERO, 20, ColorRGBA.DarkGray);
    }

    private void setupModel(final Node spat, boolean isMap) {
        spat.setName(NODE_NAME);

        if (!isMap) {

            // Reset the game translation and scale
//            for (Spatial subSpat : spat.getChildren()) {
//                subSpat.setLocalScale(1);
//                subSpat.setLocalTranslation(0, 0, 0);
//            }

            // Make it bigger and move
//            spat.scale(10);
//            spat.setLocalTranslation(10, 25, 30);

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
        spat.depthFirstTraversal(new SceneGraphVisitor() {
            @Override
            public void visit(Spatial spatial) {
                AnimControl animControl = (AnimControl) spatial.getControl(AnimControl.class);
                if (animControl != null) {
                    AnimChannel channel = animControl.createChannel();
                    channel.setAnim("anim");
                    AnimationLoader.setLoopModeOnChannel(spatial, channel);
                }
            }
        });
    }

    /**
     * Fill the listbox with items
     *
     * @param object list of objects (cached)
     * @param directory the actual directory where the objects are get
     * @param extension the file extension of the objects wanted
     */
    private void fillWithFiles(List<String> object,
            final String directory, final String extension) {

        ListBox<String> listBox = screen.getItemsControl();

        if (object == null) {

            // Find all the files
            object = new ArrayList<>();
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get(directory), PathUtils.getFilterForFilesEndingWith(extension))) {
                for (Path file : stream) {
                    String key = file.getFileName().toString();
                    object.add(key.substring(0, key.length() - 4));
                }
            } catch (IOException ex) {
                Logger.getLogger(MapSelector.class.getName()).log(Level.SEVERE, "Failed to load the maps!", ex);
            }
        }

        // Add & sort
        listBox.addAllItems(object);
        listBox.sortAllItems(String.CASE_INSENSITIVE_ORDER);
    }

    /**
     * Fills in the different types of objects we have
     */
    protected void fillTypes() {
        DropDown<Types> dropDown = screen.getTypeControl();
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
    protected void fillList(Types type) {
        screen.getItemsControl().clear();
        switch (type) {
            case MODELS: {
                fillWithFiles(models, AssetsConverter.getAssetsFolder()
                        + AssetsConverter.MODELS_FOLDER + File.separator, ".j3o");
                break;
            }
            case MAPS: {
                fillWithFiles(maps, dkIIFolder + PathUtils.DKII_MAPS_FOLDER, ".kwd");
                break;
            }
            case CREATURES: {
                KwdFile kwfFile = getKwdFile();
                Collection<Creature> creatures = kwfFile.getCreatureList();
                screen.getItemsControl().addAllItems(creatures);
                break;
            }
            case TERRAIN: {
                KwdFile kwfFile = getKwdFile();
                Collection<Terrain> terrains = kwfFile.getTerrainList();
                screen.getItemsControl().addAllItems(terrains);
                break;
            }
            case OBJECTS: {
                KwdFile kwfFile = getKwdFile();
                Collection<GameObject> objects = kwfFile.getObjectList();
                screen.getItemsControl().addAllItems(objects);
                break;
            }
            case ROOMS: {
                KwdFile kwfFile = getKwdFile();
                Collection<Room> rooms = kwfFile.getRooms();
                screen.getItemsControl().addAllItems(rooms);
                break;
            }
            case DOORS: {
                KwdFile kwfFile = getKwdFile();
                Collection<Door> doors = kwfFile.getDoors();
                screen.getItemsControl().addAllItems(doors);
                break;
            }
            case TRAPS: {
                KwdFile kwfFile = getKwdFile();
                Collection<Trap> traps = kwfFile.getTraps();
                screen.getItemsControl().addAllItems(traps);
                break;
            }
            case SHOTS: {
                KwdFile kwfFile = getKwdFile();
                Collection<Shot> shots = kwfFile.getShots();
                screen.getItemsControl().addAllItems(shots);
                break;
            }
            case EFFECTS: {
                KwdFile kwfFile = getKwdFile();
                Collection<Effect> effects = kwfFile.getEffects().values();
                screen.getItemsControl().addAllItems(effects);
                break;
            }
        }
    }

    private KwdFile getKwdFile() {
        if (kwdFile == null) {

            // Read Alcatraz.kwd by default
            kwdFile = new KwdFile(dkIIFolder,
                    Paths.get(dkIIFolder, PathUtils.DKII_MAPS_FOLDER, "Alcatraz.kwd"));
        }

        return kwdFile;
    }

    public void onSoundChanged(SoundFile soundFile) {
        AudioNode node = SoundsLoader.getAudioNode(assetManager, soundFile);
        node.setLooping(false);
        node.setPositional(false);
        node.play();
    }

    public List<SoundFile> loadSoundCategory(ISoundable item) {
        return loadSoundCategory(item, true);
    }

    public List<SoundFile> loadSoundCategory(ISoundable item, boolean useGlobal) {
        List<SoundFile> result = new ArrayList<>();

        String soundCategory = item.getSoundCategory();
        SoundCategory sc = SoundsLoader.load(soundCategory, useGlobal);
        if (sc != null) {
            for (SoundGroup sa : sc.getGroups().values()) {
                result.addAll(sa.getFiles());
            }
        }
        Collections.sort(result);

        return result;
    }
}
