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
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Node;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;
import com.jme3.texture.plugins.AWTLoader;
import de.lessvoid.nifty.Nifty;
import de.lessvoid.nifty.builder.ControlBuilder;
import de.lessvoid.nifty.builder.EffectBuilder;
import de.lessvoid.nifty.builder.HoverEffectBuilder;
import de.lessvoid.nifty.builder.ImageBuilder;
import de.lessvoid.nifty.builder.PanelBuilder;
import de.lessvoid.nifty.controls.Label;
import de.lessvoid.nifty.controls.label.builder.LabelBuilder;
import de.lessvoid.nifty.effects.EffectEventId;
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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import toniarts.openkeeper.Main;
import toniarts.openkeeper.gui.nifty.NiftyUtils;
import toniarts.openkeeper.tools.convert.AssetsConverter;
import toniarts.openkeeper.tools.convert.ConversionUtils;
import toniarts.openkeeper.tools.convert.map.KeeperSpell;
import toniarts.openkeeper.tools.convert.map.Player;
import toniarts.openkeeper.tools.convert.map.Room;
import toniarts.openkeeper.utils.Utils;
import toniarts.openkeeper.view.PlayerCameraState;
import toniarts.openkeeper.view.PlayerInteractionState;

/**
 * The player state! GUI, camera, etc. Player interactions
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class PlayerState extends AbstractAppState implements ScreenController {

    private enum PauseMenuState {

        MAIN, QUIT, CONFIRMATION;
    }

    private enum TabCategory {

        CREATURES, ROOMS, SPELLS, WORKSHOP_ITEMS;
    }
    private Main app;
    private Node rootNode;
    private AssetManager assetManager;
    private AppStateManager stateManager;
    private InputManager inputManager;
    private ViewPort viewPort;
    private GameState gameState;
    private Nifty nifty;
    private Screen screen;
    private boolean paused = false;
    private boolean backgroundSet = false;
    private static final String HUD_SCREEN_ID = "hud";
    private List<AbstractPauseAwareState> appStates = new ArrayList<>();
    private PlayerInteractionState interactionState;
    private static final Logger logger = Logger.getLogger(PlayerState.class.getName());

    @Override
    public void initialize(final AppStateManager stateManager, final Application app) {
        super.initialize(stateManager, app);
        this.app = (Main) app;
        rootNode = this.app.getRootNode();
        assetManager = this.app.getAssetManager();
        this.stateManager = this.app.getStateManager();
        inputManager = this.app.getInputManager();
        viewPort = this.app.getViewPort();
    }

    @Override
    public void cleanup() {

        // Detach states
        for (AbstractAppState state : appStates) {
            stateManager.detach(state);
        }

        super.cleanup();
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        if (enabled) {

            // Get the game state
            gameState = stateManager.getState(GameState.class);

            // Load the HUD
            app.getNifty().getNifty().gotoScreen(HUD_SCREEN_ID);

            // Cursor
            app.getInputManager().setCursorVisible(true);

            // Set the pause state
            if (nifty != null) {
                paused = false;
                nifty.getScreen(HUD_SCREEN_ID).findElementByName("optionsMenu").setVisible(paused);
            }

            // Create app states
            Player player = gameState.getLevelData().getPlayer((short) 3); // Keeper 1
            appStates.add(new PlayerCameraState(player));
            interactionState = new PlayerInteractionState(player, gameState) {
                @Override
                protected void onInteractionStateChange(PlayerInteractionState.InteractionState interactionState, int id) {
                    PlayerState.this.updateGUISelectedStatus(interactionState, id);
                }
            };
            appStates.add(interactionState);

            // Load the state
            for (AbstractAppState state : appStates) {
                stateManager.attach(state);
            }
        } else {

            // Detach states
            for (AbstractAppState state : appStates) {
                stateManager.detach(state);
            }
            gameState = null;
            appStates.clear();
            if (nifty != null) {
                nifty.gotoScreen("empty");
            }
        }
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
                                    filename(ConversionUtils.getCanonicalAssetKey(AssetsConverter.TEXTURES_FOLDER.concat(File.separator).concat(room.getGuiIcon().getName()).concat(".png")));
                                    valignCenter();
                                    marginRight("3px");
                                    interactOnClick("buildMode(" + room.getRoomId() + ")");
                                    id("room" + room.getRoomId());
                                    onHoverEffect(new HoverEffectBuilder("imageOverlay") {
                                        {
                                            effectParameter("filename", ConversionUtils.getCanonicalAssetKey(AssetsConverter.TEXTURES_FOLDER.concat(File.separator).concat("GUI/Icons/frame.png")));
                                            post(true);
                                        }
                                    });
                                    onCustomEffect(new EffectBuilder("imageOverlay") {
                                        {
                                            effectParameter("filename", ConversionUtils.getCanonicalAssetKey(AssetsConverter.TEXTURES_FOLDER.concat(File.separator).concat("GUI/Icons/selected-spell.png")));
                                            effectParameter("customKey", "select");
                                            post(true);
                                            neverStopRendering(true);
                                        }
                                    });
                                }
                            });
                        }
                        break;
                    }
                    case SPELLS: {
                        for (final KeeperSpell spell : getAvailableKeeperSpells()) {
                            image(new ImageBuilder() {
                                {
                                    filename(ConversionUtils.getCanonicalAssetKey(AssetsConverter.TEXTURES_FOLDER.concat(File.separator).concat(spell.getGuiIcon().getName()).concat(".png")));
                                    valignCenter();
                                    marginRight("3px");
                                    id("spell" + spell.getKeeperSpellId());
                                    onHoverEffect(new HoverEffectBuilder("imageOverlay") {
                                        {
                                            effectParameter("filename", ConversionUtils.getCanonicalAssetKey(AssetsConverter.TEXTURES_FOLDER.concat(File.separator).concat("GUI/Icons/frame.png")));
                                            post(true);
                                        }
                                    });
                                    onCustomEffect(new EffectBuilder("imageOverlay") {
                                        {
                                            effectParameter("filename", ConversionUtils.getCanonicalAssetKey(AssetsConverter.TEXTURES_FOLDER.concat(File.separator).concat("GUI/Icons/selected-room.png")));
                                            effectParameter("customKey", "select");
                                            post(true);
                                            neverStopRendering(true);
                                        }
                                    });
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

        // Set the selected status
        updateGUISelectedStatus(interactionState.getInteractionState(), interactionState.getInteractionStateItemId());

        // Reset the layout
        contentPanel.resetLayout();
    }

    private List<Room> getAvailableRoomsToBuild() {
        List<Room> rooms = gameState.getLevelData().getRooms();
        for (Iterator<Room> iter = rooms.iterator(); iter.hasNext();) {
            Room room = iter.next();
            if (!room.getFlags().contains(Room.RoomFlag.BUILDABLE)) {
                iter.remove();
            }
        }
        return rooms;
    }

    private List<KeeperSpell> getAvailableKeeperSpells() {
        List<KeeperSpell> spells = gameState.getLevelData().getKeeperSpells();
        return spells;
    }

    public void pauseMenu() {

        // Pause state
        paused = !paused;

        // Pause / unpause
        gameState.setEnabled(!paused);
        for (AbstractPauseAwareState state : appStates) {
            if (state.isPauseable()) {
                state.setEnabled(!paused);
            }
        }

        // Set the menuButton
        Element menuButton = nifty.getCurrentScreen().findElementByName("menuButton");
        if (paused) {
            menuButton.startEffect(EffectEventId.onCustom, null, "select");
        } else {
            menuButton.stopEffect(EffectEventId.onCustom);
        }

        nifty.getCurrentScreen().findElementByName("optionsMenu").setVisible(paused);
        if (paused) {
            pauseMenuNavigate(PauseMenuState.MAIN.name(), null, null, null);
        }
    }

    public void pauseMenuNavigate(String menu, String backMenu, String confirmationTitle, String confirmMethod) {
        Element optionsMenu = nifty.getCurrentScreen().findElementByName("optionsMenu");
        Label optionsMenuTitle = optionsMenu.findNiftyControl("optionsMenuTitle", Label.class);
        Element optionsColumnOne = optionsMenu.findElementByName("optionsColumnOne");
        for (Element element : optionsColumnOne.getElements()) {
            element.markForRemoval();
        }
        Element optionsColumnTwo = optionsMenu.findElementByName("optionsColumnTwo");
        for (Element element : optionsColumnTwo.getElements()) {
            element.markForRemoval();
        }
        Element optionsNavigationColumnOne = optionsMenu.findElementByName("optionsNavigationColumnOne");
        for (Element element : optionsNavigationColumnOne.getElements()) {
            element.markForRemoval();
        }
        Element optionsNavigationColumnTwo = optionsMenu.findElementByName("optionsNavigationColumnTwo");
        for (Element element : optionsNavigationColumnTwo.getElements()) {
            element.markForRemoval();
        }
        // TODO: Maybe you can just put some lists to the enum, and just loop each column and construct
        switch (PauseMenuState.valueOf(menu)) {
            case MAIN: {
                optionsMenuTitle.setText("${menu.94}");

                // Column one
                new InGameMenuSelectionControl("Textures/GUI/Options/i-objective.png", "${menu.537}", "pauseMenu()") {
                    {
                    }
                }.build(nifty, screen, optionsColumnOne);
                new InGameMenuSelectionControl("Textures/GUI/Options/i-game.png", "${menu.97}", "pauseMenu()") {
                    {
                    }
                }.build(nifty, screen, optionsColumnOne);
                new InGameMenuSelectionControl("Textures/GUI/Options/i-load.png", "${menu.143}", "pauseMenu()") {
                    {
                    }
                }.build(nifty, screen, optionsColumnOne);
                new InGameMenuSelectionControl("Textures/GUI/Options/i-save.png", "${menu.201}", "pauseMenu()") {
                    {
                    }
                }.build(nifty, screen, optionsColumnOne);

                // Column two
                new InGameMenuSelectionControl("Textures/GUI/Options/i-quit.png", "${menu.1266}", "pauseMenuNavigate(" + PauseMenuState.QUIT.name() + "," + PauseMenuState.MAIN.name() + ",null,null)") {
                    {
                    }
                }.build(nifty, screen, optionsColumnTwo);
                new InGameMenuSelectionControl("Textures/GUI/Options/i-restart.png", "${menu.1269}", "pauseMenu()") {
                    {
                    }
                }.build(nifty, screen, optionsColumnTwo);

                // Navigation one
                new InGameMenuSelectionControl("Textures/GUI/Options/i-accept.png", "${menu.142}", "pauseMenu()") {
                    {
                    }
                }.build(nifty, screen, optionsNavigationColumnOne);
                break;
            }
            case QUIT: {
                optionsMenuTitle.setText("${menu.1266}");

                // Column one
                new InGameMenuSelectionControl("Textures/GUI/Options/i-quit.png", "${menu.12}", "pauseMenuNavigate(" + PauseMenuState.CONFIRMATION.name() + "," + PauseMenuState.QUIT.name() + ",${menu.12},quitToMainMenu())") {
                    {
                    }
                }.build(nifty, screen, optionsColumnOne);
                new InGameMenuSelectionControl(Utils.isWindows() ? "Textures/GUI/Options/i-exit_to_windows.png" : "Textures/GUI/Options/i-quit.png", Utils.isWindows() ? "${menu.13}" : "${menu.14}", "pauseMenuNavigate(" + PauseMenuState.CONFIRMATION.name() + "," + PauseMenuState.QUIT.name() + "," + (Utils.isWindows() ? "${menu.13}" : "${menu.14}") + ",quitToOS())") {
                    {
                    }
                }.build(nifty, screen, optionsColumnOne);

                // Navigation one
                new InGameMenuSelectionControl("Textures/GUI/Options/i-accept.png", "${menu.142}", "pauseMenu()") {
                    {
                    }
                }.build(nifty, screen, optionsNavigationColumnOne);

                // Navigation two
                new InGameMenuSelectionControl("Textures/GUI/Options/i-back.png", "${menu.20}", "pauseMenuNavigate(" + PauseMenuState.MAIN + ",null,null,null)") {
                    {
                    }
                }.build(nifty, screen, optionsNavigationColumnTwo);
                break;
            }
            case CONFIRMATION: {
                optionsMenuTitle.setText(confirmationTitle);

                // Column one
                new LabelBuilder("confirmLabel", "${menu.15}") {
                    {
                        style("textNormal");
                    }
                }.build(nifty, screen, optionsColumnOne);
                new InGameMenuSelectionControl("Textures/GUI/Options/i-accept.png", "${menu.21}", confirmMethod) {
                    {
                    }
                }.build(nifty, screen, optionsColumnOne);

                // Navigation one
                new InGameMenuSelectionControl("Textures/GUI/Options/i-accept.png", "${menu.142}", "pauseMenu()") {
                    {
                    }
                }.build(nifty, screen, optionsNavigationColumnOne);

                // Navigation two
                new InGameMenuSelectionControl("Textures/GUI/Options/i-back.png", "${menu.20}", "pauseMenuNavigate(" + backMenu + ",null,null,null)") {
                    {
                    }
                }.build(nifty, screen, optionsNavigationColumnTwo);
            }
        }

        // Fix layout
        NiftyUtils.resetContraints(optionsMenuTitle);
        optionsMenu.layoutElements();
    }

    public void quitToMainMenu() {

        // Disable us, detach game and enable start
        stateManager.detach(gameState);
        setEnabled(false);
        stateManager.getState(MainMenuState.class).setEnabled(true);
    }

    public void quitToOS() {
        app.stop();
    }

    /**
     * Called from the GUI, toggles build mode
     *
     * @param roomId the room id to construct
     *
     */
    public void buildMode(String roomId) {
        interactionState.setInteractionState(PlayerInteractionState.InteractionState.BUILD, Integer.parseInt(roomId));
    }

    /**
     * Called from the GUI, toggles sell mode
     */
    public void sellMode() {
        interactionState.setInteractionState(PlayerInteractionState.InteractionState.SELL, 0);
    }

    private void updateGUISelectedStatus(PlayerInteractionState.InteractionState interactionState, int id) {

        // Update the GUI
        Element contentPanel = nifty.getCurrentScreen().findElementByName("tab-content");

        // End the selected effect on others, and set the wanted as selected
        for (Element e : contentPanel.getElements()) {
            if (interactionState == PlayerInteractionState.InteractionState.BUILD && e.getId().equals("room" + id)) {
                e.startEffect(EffectEventId.onCustom, null, "select");
            } else if (interactionState == PlayerInteractionState.InteractionState.CAST && e.getId().equals("spell" + id)) {
                e.startEffect(EffectEventId.onCustom, null, "select");
            } else {

                // Stop the effect
                e.stopEffect(EffectEventId.onCustom);
            }
        }
        if (interactionState == PlayerInteractionState.InteractionState.BUILD) {
            Element e = contentPanel.findElementByName("room" + id);
            if (e != null) {
                e.startEffect(EffectEventId.onCustom, null, "select");
            }
        }

        // The sell button
        Element sellButton = nifty.getCurrentScreen().findElementByName("sellButton");
        if (interactionState == PlayerInteractionState.InteractionState.SELL) {
            sellButton.startEffect(EffectEventId.onCustom, null, "select");
        } else {
            sellButton.stopEffect(EffectEventId.onCustom);
        }
    }

    /**
     * In-game menu items
     *
     * @author Toni Helenius <helenius.toni@gmail.com>
     */
    private class InGameMenuSelectionControl extends ControlBuilder {

        private InGameMenuSelectionControl(String glyph, String text, String onClick) {
            super("inGameMenuSelection");
            parameter("glyph", glyph);
            parameter("text", text);
            parameter("click", onClick);
        }
    }
}
