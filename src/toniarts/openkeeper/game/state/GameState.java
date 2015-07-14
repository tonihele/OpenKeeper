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
package toniarts.openkeeper.game.state;

import com.jme3.app.Application;
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.asset.AssetKey;
import com.jme3.asset.AssetManager;
import com.jme3.asset.DesktopAssetManager;
import com.jme3.asset.TextureKey;
import com.jme3.input.InputManager;
import com.jme3.math.Vector3f;
import com.jme3.niftygui.NiftyJmeDisplay;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Node;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;
import com.jme3.texture.plugins.AWTLoader;
import de.lessvoid.nifty.Nifty;
import de.lessvoid.nifty.builder.ControlBuilder;
import de.lessvoid.nifty.builder.ImageBuilder;
import de.lessvoid.nifty.builder.PanelBuilder;
import de.lessvoid.nifty.elements.Element;
import de.lessvoid.nifty.elements.render.ImageRenderer;
import de.lessvoid.nifty.render.NiftyImage;
import de.lessvoid.nifty.render.image.ImageModeFactory;
import de.lessvoid.nifty.render.image.ImageModeHelper;
import de.lessvoid.nifty.screen.Screen;
import de.lessvoid.nifty.screen.ScreenController;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import toniarts.openkeeper.Main;
import toniarts.openkeeper.game.state.loading.SingleBarLoadingState;
import toniarts.openkeeper.tools.convert.AssetsConverter;
import toniarts.openkeeper.tools.convert.Utils;
import toniarts.openkeeper.tools.convert.map.KeeperSpell;
import toniarts.openkeeper.tools.convert.map.KwdFile;
import toniarts.openkeeper.tools.convert.map.Player;
import toniarts.openkeeper.tools.convert.map.Room;
import toniarts.openkeeper.world.MapLoader;

/**
 * The GAME state!
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class GameState extends AbstractAppState implements ScreenController {

    private enum TabCategory {

        CREATURES, ROOMS, SPELLS, WORKSHOP_ITEMS;
    }
    private Main app;
    private Node rootNode;
    private AssetManager assetManager;
    private AppStateManager stateManager;
    private InputManager inputManager;
    private ViewPort viewPort;
    private Nifty nifty;
    private Screen screen;
    private Node worldNode;
    private NiftyJmeDisplay niftyDisplay;
    private final String level;
    private KwdFile kwdFile;
    private boolean backgroundSet = false;
    private Vector3f startLocation;
    private static final String HUD_SCREEN_ID = "hud";
    private static final Logger logger = Logger.getLogger(GameState.class.getName());

    public GameState(String level) {
        this.level = level;
    }

    @Override
    public void initialize(final AppStateManager stateManager, final Application app) {
        super.initialize(stateManager, app);
        this.app = (Main) app;
        rootNode = this.app.getRootNode();
        assetManager = this.app.getAssetManager();
        this.stateManager = this.app.getStateManager();
        inputManager = this.app.getInputManager();
        viewPort = this.app.getViewPort();

        // Set up the loading screen
        SingleBarLoadingState loader = new SingleBarLoadingState() {
            @Override
            public Void onLoad() {

                // Load the level data
                kwdFile = new KwdFile(Main.getDkIIFolder(), new File(Main.getDkIIFolder().concat("Data".concat(File.separator).concat("editor").concat(File.separator).concat("maps").concat(File.separator).concat(level).concat(".kwd"))));
                setProgress(0.25f);

                // Create the actual level
                worldNode = new Node("World");
                worldNode.attachChild(new MapLoader() {
                    @Override
                    protected void updateProgress(int progress, int max) {
                        setProgress(0.25f + ((float) progress / max * 0.75f));
                    }
                }.load(assetManager, kwdFile));
                setProgress(1.0f);

                return null;
            }

            @Override
            public void onLoadComplete() {

                // Set the processors
                GameState.this.app.setViewProcessors();

                // Enable the fly cam
                GameState.this.app.getFlyByCamera().setEnabled(true);
                GameState.this.app.getFlyByCamera().setDragToRotate(true);
                GameState.this.app.getFlyByCamera().setMoveSpeed(10);

                rootNode.attachChild(worldNode);

                // Init Nifty
                niftyDisplay = GameState.this.app.getNifty();

                // Load the HUD
                niftyDisplay.getNifty().fromXml("Interface/GameHUD.xml", "hud", GameState.this);

                // Set the camera position
                loadCameraStartLocation();
            }
        };
        stateManager.attach(loader);
    }

    /**
     * Load the initial main menu camera position
     */
    private void loadCameraStartLocation() {
        Player player = kwdFile.getPlayer((short) 3); // Keeper 1
        startLocation = new Vector3f(MapLoader.getCameraPositionOnMapPoint(player.getStartingCameraX(), player.getStartingCameraY()));

        // Set the actual camera location
        this.app.getCamera().setLocation(startLocation);
    }

    @Override
    public void cleanup() {

        // Detach our map
        rootNode.detachChild(worldNode);

        super.cleanup();
    }

    @Override
    public void bind(Nifty nifty, Screen screen) {
        this.nifty = nifty;
        this.screen = screen;
    }

    @Override
    public void onStartScreen() {
        switch (nifty.getCurrentScreen().getScreenId()) {
            case HUD_SCREEN_ID: {

                if (!backgroundSet) {

                    // Stretch the background image (height-wise) on the background image panel
                    try {
                        BufferedImage img = ImageIO.read(assetManager.locateAsset(new AssetKey("Textures/GUI/Windows/Panel-BG.png")).openStream());

                        // Scale the backgroung image to the panel height, keeping the aspect ratio
                        Element panel = nifty.getCurrentScreen().findElementByName("bottomBackgroundPanel");
                        BufferedImage newImage = new BufferedImage(panel.getHeight() * img.getWidth() / img.getHeight(), panel.getHeight(), img.getType());
                        Graphics2D g = newImage.createGraphics();
                        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                        g.drawImage(img, 0, 0, newImage.getWidth(), newImage.getHeight(), 0, 0, img.getWidth(), img.getHeight(), null);
                        g.dispose();

                        // Convert the new image to a texture, and add a dummy cached entry to the asset manager
                        AWTLoader loader = new AWTLoader();
                        Texture tex = new Texture2D(loader.load(newImage, false));
                        ((DesktopAssetManager) assetManager).addToCache(new TextureKey("HUDBackground"), tex);

                        // Add the scaled one
                        NiftyImage niftyImage = nifty.createImage("HUDBackground", false);
                        String resizeString = "repeat:0,0," + newImage.getWidth() + "," + newImage.getHeight();
                        String areaProviderProperty = ImageModeHelper.getAreaProviderProperty(resizeString);
                        String renderStrategyProperty = ImageModeHelper.getRenderStrategyProperty(resizeString);
                        niftyImage.setImageMode(ImageModeFactory.getSharedInstance().createImageMode(areaProviderProperty, renderStrategyProperty));
                        ImageRenderer renderer = panel.getRenderer(ImageRenderer.class);
                        renderer.setImage(niftyImage);

                        backgroundSet = true;
                    } catch (IOException ex) {
                        logger.log(Level.SEVERE, "Failed to open the background image!", ex);
                    }
                }

                break;
            }
        }
    }

    @Override
    public void onEndScreen() {
    }

    public void guiTabClick(String tab) {
        final TabCategory category = TabCategory.valueOf(tab.toUpperCase());

        // Get the contents & remove all
        Element contentPanel = nifty.getCurrentScreen().findElementByName("tab-content");
        for (Element element : contentPanel.getElements()) {
            element.markForRemoval();
        }

        // Rebuild it
        if (category == TabCategory.CREATURES) {
            new ControlBuilder("tab-workers", "workerAmount") {
                {
                }
            }.build(nifty, screen, contentPanel);
        }
        new ControlBuilder("tab-scroll", "tabScroll") {
            {
            }
        }.build(nifty, screen, contentPanel);
        if (category == TabCategory.CREATURES) {
            new ControlBuilder("tab-workers-equal", "workerEqual") {
                {
                }
            }.build(nifty, screen, contentPanel);
        }
        new PanelBuilder("tab-content") {
            {
                width("*");
                marginLeft("3px");
                valignCenter();
                alignLeft();
                childLayoutHorizontal();

                // Fill the actual content
                // FIXME: Somekind of wrapper here for these
                switch (category) {
                    case ROOMS: {
                        for (final Room room : getAvailableRoomsToBuild()) {
                            image(new ImageBuilder() {
                                {
                                    filename(Utils.getCanonicalAssetKey(AssetsConverter.TEXTURES_FOLDER.concat(File.separator).concat(room.getGuiIcon().getName()).concat(".png")));
                                    valignCenter();
                                    marginRight("3px");
                                }
                            });
                        }
                        break;
                    }
                    case SPELLS: {
                        for (final KeeperSpell spell : getAvailableKeeperSpells()) {
                            image(new ImageBuilder() {
                                {
                                    filename(Utils.getCanonicalAssetKey(AssetsConverter.TEXTURES_FOLDER.concat(File.separator).concat(spell.getGuiIcon().getName()).concat(".png")));
                                    valignCenter();
                                    marginRight("3px");
                                }
                            });
                        }
                        break;
                    }
                }
            }
        }.build(nifty, screen, contentPanel);
        new ControlBuilder("tab-reaper", "reaperTalisman") {
            {
            }
        }.build(nifty, screen, contentPanel);

        // Reset the layout
        contentPanel.resetLayout();
    }

    private List<Room> getAvailableRoomsToBuild() {
        List<Room> rooms = kwdFile.getRooms();
        for (Iterator<Room> iter = rooms.iterator(); iter.hasNext();) {
            Room room = iter.next();
            if (!room.getFlags().contains(Room.RoomFlag.BUILDABLE)) {
                iter.remove();
            }
        }
        return rooms;
    }

    private List<KeeperSpell> getAvailableKeeperSpells() {
        List<KeeperSpell> spells = kwdFile.getKeeperSpells();
        return spells;
    }
}
