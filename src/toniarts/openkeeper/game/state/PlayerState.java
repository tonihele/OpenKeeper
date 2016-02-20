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
import com.jme3.font.Rectangle;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;
import com.jme3.texture.plugins.AWTLoader;
import de.lessvoid.nifty.Nifty;
import de.lessvoid.nifty.NiftyEventSubscriber;
import de.lessvoid.nifty.NiftyIdCreator;
import de.lessvoid.nifty.builder.EffectBuilder;
import de.lessvoid.nifty.builder.HoverEffectBuilder;
import de.lessvoid.nifty.builder.ImageBuilder;
import de.lessvoid.nifty.controls.Label;
import de.lessvoid.nifty.controls.TabSelectedEvent;
import de.lessvoid.nifty.controls.label.builder.LabelBuilder;
import de.lessvoid.nifty.effects.EffectEventId;
import de.lessvoid.nifty.elements.Element;
import de.lessvoid.nifty.elements.render.ImageRenderer;
import de.lessvoid.nifty.elements.render.PanelRenderer;
import de.lessvoid.nifty.render.NiftyImage;
import de.lessvoid.nifty.render.image.ImageModeFactory;
import de.lessvoid.nifty.render.image.ImageModeHelper;
import de.lessvoid.nifty.screen.Screen;
import de.lessvoid.nifty.screen.ScreenController;
import de.lessvoid.nifty.tools.Color;
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
import toniarts.openkeeper.game.player.PlayerGoldControl;
import toniarts.openkeeper.game.player.PlayerManaControl;
import toniarts.openkeeper.game.player.PlayerTriggerControl;
import toniarts.openkeeper.gui.nifty.NiftyUtils;
import toniarts.openkeeper.gui.nifty.icontext.IconTextBuilder;
import toniarts.openkeeper.tools.convert.AssetsConverter;
import toniarts.openkeeper.tools.convert.ConversionUtils;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.CreatureSpell;
import toniarts.openkeeper.tools.convert.map.Door;
import toniarts.openkeeper.tools.convert.map.KeeperSpell;
import toniarts.openkeeper.tools.convert.map.Player;
import toniarts.openkeeper.tools.convert.map.Room;
import toniarts.openkeeper.tools.convert.map.Thing;
import toniarts.openkeeper.tools.convert.map.Trap;
import toniarts.openkeeper.tools.convert.map.TriggerAction;
import toniarts.openkeeper.utils.Utils;
import toniarts.openkeeper.view.PlayerCameraState;
import toniarts.openkeeper.view.PlayerInteractionState;
import toniarts.openkeeper.view.PlayerInteractionState.InteractionState;
import toniarts.openkeeper.view.PossessionCameraState;
import toniarts.openkeeper.view.PossessionInteractionState;

/**
 * The player state! GUI, camera, etc. Player interactions
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class PlayerState extends AbstractAppState implements ScreenController {

    private enum PauseMenuState {

        MAIN, QUIT, CONFIRMATION;
    }
    private Main app;

    private AssetManager assetManager;
    private AppStateManager stateManager;

    private short playerId;
    private Nifty nifty;

    private boolean paused = false;
    private boolean backgroundSet = false;
    public static final String HUD_SCREEN_ID = "hud";
    public static final String POSSESSION_SCREEN_ID = "possession";
    public static final String CINEMATIC_SCREEN_ID = "cinematic";
    private List<AbstractPauseAwareState> appStates = new ArrayList<>();
    private List<AbstractPauseAwareState> storedAppStates;
    private PlayerInteractionState interactionState;
    private PossessionInteractionState possessionState;
    private PlayerCameraState cameraState;
    private PossessionCameraState possessionCameraState;
    private Label goldCurrent;
    private Label tooltip;
    private PlayerManaControl manaControl = null;
    private PlayerTriggerControl triggerControl = null;
    private PlayerGoldControl goldControl = null;
    private int score = 0;
    private boolean transitionEnd = true;
    private Integer textId = null;
    private static final Logger logger = Logger.getLogger(PlayerState.class.getName());

    public PlayerState(int playerId) {
        this.playerId = (short) playerId;
    }

    public PlayerState(int playerId, boolean enabled) {
        this(playerId);
        super.setEnabled(enabled);
    }

    @Override
    public void initialize(final AppStateManager stateManager, final Application app) {
        super.initialize(stateManager, app);

        this.app = (Main) app;
        assetManager = this.app.getAssetManager();
        this.stateManager = stateManager;

        manaControl = new PlayerManaControl(playerId, stateManager);
        goldControl = new PlayerGoldControl(playerId, stateManager);
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
            final GameState gameState = stateManager.getState(GameState.class);

            String levelResource = "Interface/Texts/" + gameState.getLevel().toUpperCase();
            this.app.getNifty().getNifty().getResourceBundles().put("level", Main.getResourceBundle(levelResource));

            int triggerId = gameState.getLevelData().getPlayer(playerId).getTriggerId();
            if (triggerId != 0) {
                triggerControl = new PlayerTriggerControl(stateManager, triggerId);
                triggerControl.setPlayerState(this);
            }

            // Load the HUD
            nifty = app.getNifty().getNifty();
            nifty.gotoScreen(HUD_SCREEN_ID);

            // Init the HUD items
            initHudItems();

            // Cursor
            app.getInputManager().setCursorVisible(true);

            // Get GUI area constraints
            Element middle = app.getNifty().getNifty().getScreen(HUD_SCREEN_ID).findElementByName("middle");
            Rectangle guiConstraint = new Rectangle(middle.getX(), middle.getY(), middle.getWidth(), middle.getHeight());

            // Set the pause state
            if (nifty != null) {
                paused = false;
                nifty.getScreen(HUD_SCREEN_ID).findElementByName("optionsMenu").setVisible(paused);
            }

            // Create app states
            Player player = gameState.getLevelData().getPlayer(playerId); // Keeper 1

            possessionState = new PossessionInteractionState(false) {

                @Override
                protected void onExit() {
                    super.onExit();
                    // Detach states
                    for (AbstractAppState state : storedAppStates) {
                        stateManager.detach(state);
                    }

                    // Load the state
                    for (AbstractAppState state : appStates) {
                        state.setEnabled(true);
                    }

                    nifty.gotoScreen(HUD_SCREEN_ID);
                }

                @Override
                protected void onActionChange(PossessionInteractionState.Action action) {
                    PlayerState.this.updatePossessionSelectedItem(action);
                }
            };

            cameraState = new PlayerCameraState(player);
            interactionState = new PlayerInteractionState(player, gameState, guiConstraint, tooltip) {
                @Override
                protected void onInteractionStateChange(InteractionState interactionState, int id) {
                    PlayerState.this.updateSelectedItem(interactionState, id);
                }

                @Override
                protected void onPossession(Thing.KeeperCreature creature) {
                    // Detach states
                    for (AbstractAppState state : appStates) {
                        state.setEnabled(false);
                    }
                    storedAppStates = new ArrayList<>();
                    storedAppStates.add(possessionState);
                    // TODO not Thing.KeeperCreature need wrapper around KeeperCreature
                    possessionState.setTarget(creature);
                    possessionCameraState = new PossessionCameraState(creature, gameState.getLevelData());
                    storedAppStates.add(possessionCameraState);
                    // Load the state
                    for (AbstractAppState state : storedAppStates) {
                        stateManager.attach(state);
                    }
                    nifty.gotoScreen(POSSESSION_SCREEN_ID);
                }
            };
            appStates.add(interactionState);
            appStates.add(cameraState);

            // Load the state
            for (AbstractAppState state : appStates) {
                stateManager.attach(state);
            }
            // Load the HUD
            // app.getNifty().getNifty().gotoScreen(HUD_SCREEN_ID);
        } else {

            // Detach states
            for (AbstractAppState state : appStates) {
                stateManager.detach(state);
            }

            appStates.clear();
            if (nifty != null) {
                nifty.gotoScreen("empty");
            }
        }
    }

    public PlayerGoldControl getGoldControl() {
        return goldControl;
    }

    public PlayerManaControl getManaControl() {
        return manaControl;
    }

    public void setTransitionEnd(boolean value) {
        transitionEnd = value;
    }

    public boolean isTransitionEnd() {
        return transitionEnd;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public void setWideScreen(boolean enable) {
        if (enable) {
            app.getNifty().getNifty().gotoScreen(PlayerState.CINEMATIC_SCREEN_ID);
        } else {
            app.getNifty().getNifty().gotoScreen(PlayerState.HUD_SCREEN_ID);
        }
    }

    public void setText(int textId, boolean introduction, int pathId) {
        this.textId = textId;
    }

    /**
     * Initializes the HUD items, such as buildings, spells etc. to level
     * initials
     */
    private void initHudItems() {
        nifty = app.getNifty().getNifty();

        Screen hud = nifty.getScreen(HUD_SCREEN_ID);

        if (manaControl != null) {
            manaControl.addListener(hud.findNiftyControl("mana", Label.class), PlayerManaControl.Type.CURRENT);
            manaControl.addListener(hud.findNiftyControl("manaGet", Label.class), PlayerManaControl.Type.GET);
            manaControl.addListener(hud.findNiftyControl("manaLose", Label.class), PlayerManaControl.Type.LOSE);
        }

        if (goldControl != null) {
            goldControl.addListener(hud.findNiftyControl("gold", Label.class));
        }

        Element contentPanel = hud.findElementByName("tab-room-content");
        removeAllChildElements(contentPanel);
        for (final Room room : getAvailableRoomsToBuild()) {
            createRoomIcon(room).build(nifty, hud, contentPanel);
        }

        contentPanel = hud.findElementByName("tab-spell-content");
        removeAllChildElements(contentPanel);
        for (final KeeperSpell spell : getAvailableKeeperSpells()) {
            createSpellIcon(spell).build(nifty, hud, contentPanel);
        }

        contentPanel = hud.findElementByName("tab-door-content");
        removeAllChildElements(contentPanel);
        for (final Door door : getAvailableDoors()) {
            createDoorIcon(door).build(nifty, hud, contentPanel);
        }

        contentPanel = hud.findElementByName("tab-trap-content");
        removeAllChildElements(contentPanel);
        for (final Trap trap : getAvailableTraps()) {
            createTrapIcon(trap).build(nifty, hud, contentPanel);
        }

        if (tooltip == null) {
            tooltip = hud.findNiftyControl("tooltip", Label.class);
        }
    }

    public void flashButton(int id, TriggerAction.MakeType type, boolean enabled, int time) {
        // TODO make flash button
    }

    /**
     * Deletes all children from element
     *
     * @param element parent
     */
    private void removeAllChildElements(Element element) {
        for (Element e : element.getElements()) {
            e.markForRemoval();
        }
    }

    @Override
    public void update(float tpf) {
        if (!isInitialized() || !isEnabled()) {
            return;
        }

        if (manaControl != null) {
            manaControl.update(tpf);
        }

        if (triggerControl != null) {
            triggerControl.update(tpf);
        }

        super.update(tpf);
    }

    @Override
    public void bind(Nifty nifty, Screen screen) {
        this.nifty = nifty;
        //this.screen = screen;
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
                        NiftyImage niftyImage = nifty.createImage("HUDBackground", true);
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
            case POSSESSION_SCREEN_ID:
                // what we need? abitities and spells, also melee
                //final Creature creature = gameState.getLevelData().getCreature((short)13);
                final Creature creature = possessionState.getTargetCreature();

                Element contentPanel = nifty.getCurrentScreen().findElementByName("creature-icon");
                if (contentPanel != null) {
                    createCreatureIcon(creature.getIcon1Resource().getName()).build(nifty, nifty.getCurrentScreen(), contentPanel);
                }

                contentPanel = nifty.getCurrentScreen().findElementByName("creature-filter");
                if (contentPanel != null) {
                    if (creature.getFirstPersonFilterResource() != null) {
                        new ImageBuilder() {
                            {
                                filename(creature.getFirstPersonFilterResource().getName());
                            }
                        }.build(nifty, nifty.getCurrentScreen(), contentPanel);
                    } else if (getFilterResourceName(creature.getCreatureId()) != null) {
                        new ImageBuilder() {
                            {
                                filename(getFilterResourceName(creature.getCreatureId()));
                            }
                        }.build(nifty, nifty.getCurrentScreen(), contentPanel);
                    }

                    if (creature.getFirstPersonGammaEffect() != null) {
                        contentPanel.getRenderer(PanelRenderer.class).setBackgroundColor(getGammaEffectColor(creature.getFirstPersonGammaEffect()));
                    }
                }

                contentPanel = nifty.getCurrentScreen().findElementByName("creature-abilities");
                if (contentPanel != null) {

                    String ability = getAbilityResourceName(creature.getFirstPersonSpecialAbility1());
                    if (ability != null) {
                        createCreatureAbilityIcon(ability, 1).build(nifty, nifty.getCurrentScreen(), contentPanel);
                    }

                    ability = getAbilityResourceName(creature.getFirstPersonSpecialAbility2());
                    if (ability != null) {
                        createCreatureAbilityIcon(ability, 2).build(nifty, nifty.getCurrentScreen(), contentPanel);
                    }
                }

                contentPanel = nifty.getCurrentScreen().findElementByName("creature-attacks");
                if (contentPanel != null) {
                    for (Element element : contentPanel.getElements()) {
                        element.markForRemoval();
                    }

                    createCreatureMeleeIcon(creature.getFirstPersonMeleeResource().getName()).build(nifty, nifty.getCurrentScreen(), contentPanel);

                    int index = 1;
                    for (Creature.Spell s : creature.getSpells()) {
                        // TODO: check creature level availiablity
                        if (s.getCreatureSpellId() == 0) {
                            continue;
                        }
                        CreatureSpell cs = stateManager.getState(GameState.class).getLevelData().getCreatureSpellById(s.getCreatureSpellId());
                        createCreatureSpellIcon(cs, index++).build(nifty, nifty.getCurrentScreen(), contentPanel);
                    }
                }

                break;

            case CINEMATIC_SCREEN_ID:
                if (textId != null) {
                    Label text = nifty.getCurrentScreen().findNiftyControl("speechText", Label.class);
                    if (text != null) {
                        // FIXME why text.getElement() is null ?
                        text.setText(String.format("${level.%d}", textId - 1));
                    }
                }
                break;
        }
    }

    // FIXME where filter resource?
    private String getFilterResourceName(int id) {
        String resource = null;
        switch (id) {
            case 12:
                resource = "Textures/GUI/Filters/F-FireFly.png";
                break;
            case 13:
                resource = "Textures/GUI/Filters/F-Knight.png";
                break;
            case 22:
                resource = "Textures/GUI/Filters/F-Black_Knight.png";
                break;
            case 25:
                resource = "Textures/GUI/Filters/F-Guard.png";
                break;
        }
        return resource;
    }

    // FIXME Gamma Effect not a Color. It`s post effect of render.
    private Color getGammaEffectColor(Creature.GammaEffect type) {
        Color c = new Color(0, 0, 0, 0);

        switch (type) {
            case NORMAL:
                c = new Color(1f, 1f, 1f, 0);
                break;

            case VAMPIRE_RED:
                c = new Color(0.8f, 0, 0, 0.2f);
                break;

            case DARK_ELF_PURPLE:
                c = new Color(0.9f, 0, 0.9f, 0.2f);
                break;

            case SKELETON_BLACK_N_WHITE:
                c = new Color(0, 0, 1f, 0.1f);
                break;

            case SALAMANDER_INFRARED:
                c = new Color(1f, 0, 0, 0.1f);
                break;

            case DARK_ANGER_BRIGHT_BLUE:
            case DEATH_VIEW_HALF_RED:
            case FREEZE_VIEW_ONLY_BLUE:
            case BLACKOUT:
            case GHOST:
            case MAIDEN:
            case REDOUT:
            case WHITEOUT:
                break;

        }

        return c;
    }

    // FIXME I doesn`t find resources to abilities
    private String getAbilityResourceName(Creature.SpecialAbility ability) {
        String name = null;
        switch (ability) {
            case HYPNOTISE:
                name = "GUI/Icons/1st-person/hypnotise-mode";
                break;
            case TURN_TO_BAT:
                name = "GUI/Icons/1st-person/bat-mode";
                break;
            case PICK_UP_WORKERS:
                // Throw_Imp-Mode
                // throw_book-mode
                name = "GUI/Icons/1st-person/throw-mode";
                break;
            case PRAY:
                name = "GUI/Icons/1st-person/pray-mode";
                break;
            case SNIPER_MODE:
                name = "GUI/Icons/1st-person/sniper-mode";
                break;
            case DISGUISE_N_STEAL:
                name = "GUI/Icons/1st-person/sneak-mode";
                break;
        }
        return name;
    }

    private ImageBuilder createCreatureAbilityIcon(final String name, final int index) {

        return new ImageBuilder() {
            {
                valignCenter();
                marginRight("6px");
                focusable(true);
                id("creature-ability_" + index);
                filename(ConversionUtils.getCanonicalAssetKey(AssetsConverter.TEXTURES_FOLDER + File.separator + name + ".png"));
                valignCenter();
                onFocusEffect(new EffectBuilder("imageOverlay") {
                    {
                        effectParameter("filename", ConversionUtils.getCanonicalAssetKey(AssetsConverter.TEXTURES_FOLDER
                                + File.separator + "GUI/Icons/selected-creature.png"));
                        post(true);
                    }
                });
            }
        };
    }

    private ImageBuilder createCreatureIcon(final String name) {
        return new ImageBuilder() {
            {
                filename(ConversionUtils.getCanonicalAssetKey(AssetsConverter.TEXTURES_FOLDER + File.separator + name + ".png"));
                valignCenter();
            }
        };
    }

    private ImageBuilder createCreatureMeleeIcon(final String name) {
        return new ImageBuilder() {
            {
                filename(ConversionUtils.getCanonicalAssetKey(AssetsConverter.TEXTURES_FOLDER + File.separator + name + ".png"));
                valignCenter();
                marginLeft("6px");
                focusable(true);
                id("creature-melee");
                onFocusEffect(new EffectBuilder("imageOverlay") {
                    {
                        effectParameter("filename", ConversionUtils.getCanonicalAssetKey(AssetsConverter.TEXTURES_FOLDER
                                + File.separator + "GUI/Icons/selected-creature.png"));
                        post(true);
                    }
                });
            }
        };
    }

    private ImageBuilder createCreatureSpellIcon(final CreatureSpell cs, final int index) {
        return new ImageBuilder() {
            {
                filename(ConversionUtils.getCanonicalAssetKey(AssetsConverter.TEXTURES_FOLDER + File.separator + cs.getGuiIcon().getName() + ".png"));
                valignCenter();
                marginLeft("6px");
                focusable(true);
                id("creature-spell_" + index);
                onFocusEffect(new EffectBuilder("imageOverlay") {
                    {
                        effectParameter("filename", ConversionUtils.getCanonicalAssetKey(AssetsConverter.TEXTURES_FOLDER
                                + File.separator + "GUI/Icons/selected-spell.png"));
                        post(true);
                    }
                });
            }
        };
    }

    private ImageBuilder createRoomIcon(final Room room) {
        return new ImageBuilder() {
            {
                filename(ConversionUtils.getCanonicalAssetKey(AssetsConverter.TEXTURES_FOLDER.concat(File.separator).concat(room.getGuiIcon().getName()).concat(".png")));
                valignCenter();
                marginRight("3px");
                interactOnClick("select(room, " + room.getRoomId() + ")");
                id("room_" + room.getRoomId());
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
        };
    }

    private ImageBuilder createSpellIcon(final KeeperSpell spell) {
        return new ImageBuilder() {
            {
                filename(ConversionUtils.getCanonicalAssetKey(AssetsConverter.TEXTURES_FOLDER + File.separator + spell.getGuiIcon().getName() + ".png"));
                valignCenter();
                marginRight("3px");
                interactOnClick("select(spell, " + spell.getKeeperSpellId() + ")");
                id("spell_" + spell.getKeeperSpellId());
                onHoverEffect(new HoverEffectBuilder("imageOverlay") {
                    {
                        effectParameter("filename", ConversionUtils.getCanonicalAssetKey(AssetsConverter.TEXTURES_FOLDER + File.separator + "GUI/Icons/frame.png"));
                        post(true);
                    }
                });
                onCustomEffect(new EffectBuilder("imageOverlay") {
                    {
                        effectParameter("filename", ConversionUtils.getCanonicalAssetKey(AssetsConverter.TEXTURES_FOLDER + File.separator + "GUI/Icons/selected-spell.png"));
                        effectParameter("customKey", "select");
                        post(true);
                        neverStopRendering(true);
                    }
                });
            }
        };
    }

    private ImageBuilder createDoorIcon(final Door door) {
        return new ImageBuilder() {
            {
                filename(ConversionUtils.getCanonicalAssetKey(AssetsConverter.TEXTURES_FOLDER + File.separator + door.getGuiIcon().getName() + ".png"));
                valignCenter();
                marginRight("3px");
                interactOnClick("select(door, " + door.getDoorId() + ")");
                id("door_" + door.getDoorId());
                onHoverEffect(new HoverEffectBuilder("imageOverlay") {
                    {
                        effectParameter("filename", ConversionUtils.getCanonicalAssetKey(AssetsConverter.TEXTURES_FOLDER + File.separator + "GUI/Icons/frame.png"));
                        post(true);
                    }
                });
                onCustomEffect(new EffectBuilder("imageOverlay") {
                    {
                        effectParameter("filename", ConversionUtils.getCanonicalAssetKey(AssetsConverter.TEXTURES_FOLDER + File.separator + "GUI/Icons/selected-door.png"));
                        effectParameter("customKey", "select");
                        post(true);
                        neverStopRendering(true);
                    }
                });
            }
        };
    }

    private ImageBuilder createTrapIcon(final Trap trap) {
        return new ImageBuilder() {
            {
                filename(ConversionUtils.getCanonicalAssetKey(AssetsConverter.TEXTURES_FOLDER + File.separator + trap.getGuiIcon().getName() + ".png"));
                valignCenter();
                marginRight("3px");
                interactOnClick("select(trap, " + trap.getTrapId() + ")");
                id("trap_" + trap.getTrapId());
                onHoverEffect(new HoverEffectBuilder("imageOverlay") {
                    {
                        effectParameter("filename", ConversionUtils.getCanonicalAssetKey(AssetsConverter.TEXTURES_FOLDER + File.separator + "GUI/Icons/frame.png"));
                        post(true);
                    }
                });
                onCustomEffect(new EffectBuilder("imageOverlay") {
                    {
                        effectParameter("filename", ConversionUtils.getCanonicalAssetKey(AssetsConverter.TEXTURES_FOLDER + File.separator + "GUI/Icons/selected-trap.png"));
                        effectParameter("customKey", "select");
                        post(true);
                        neverStopRendering(true);
                    }
                });
            }
        };
    }

    @Override
    public void onEndScreen() {
    }

    private List<Room> getAvailableRoomsToBuild() {
        GameState gameState = stateManager.getState(GameState.class);
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
        GameState gameState = stateManager.getState(GameState.class);
        List<KeeperSpell> spells = gameState.getLevelData().getKeeperSpells();
        return spells;
    }

    private List<Door> getAvailableDoors() {
        GameState gameState = stateManager.getState(GameState.class);
        List<Door> doors = gameState.getLevelData().getDoors();
        return doors;
    }

    private List<Trap> getAvailableTraps() {
        GameState gameState = stateManager.getState(GameState.class);
        List<Trap> traps = new ArrayList<>();
        for (Trap trap : gameState.getLevelData().getTraps()) {
            if (trap.getGuiIcon() == null) {
                continue;
            }
            traps.add(trap);
        }
        return traps;
    }

    public void pauseMenu() {

        // Pause state
        paused = !paused;

        // Pause / unpause
        stateManager.getState(GameState.class).setEnabled(!paused);

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

        // TODO: @ArchDemon: I think we need to do modern (not original) game menu
        List<GameMenu> items = new ArrayList<>();
        switch (PauseMenuState.valueOf(menu)) {
            case MAIN:
                optionsMenuTitle.setText("${menu.94}");

                items.add(new GameMenu("i-objective", "${menu.537}", "pauseMenu()", optionsColumnOne));
                items.add(new GameMenu("i-game", "${menu.97}", "pauseMenu()", optionsColumnOne));
                items.add(new GameMenu("i-load", "${menu.143}", "pauseMenu()", optionsColumnOne));
                items.add(new GameMenu("i-save", "${menu.201}", "pauseMenu()", optionsColumnOne));
                items.add(new GameMenu("i-quit", "${menu.1266}", String.format("pauseMenuNavigate(%s,%s,null,null)",
                        PauseMenuState.QUIT.name(), PauseMenuState.MAIN.name()), optionsColumnTwo));
                items.add(new GameMenu("i-restart", "${menu.1269}", "pauseMenu()", optionsColumnTwo));
                items.add(new GameMenu("i-accept", "${menu.142}", "pauseMenu()", optionsNavigationColumnOne));

                break;

            case QUIT:
                optionsMenuTitle.setText("${menu.1266}");

                items.add(new GameMenu("i-quit", "${menu.12}", String.format("pauseMenuNavigate(%s,%s,${menu.12},quitToMainMenu())",
                        PauseMenuState.CONFIRMATION.name(), PauseMenuState.QUIT.name()), optionsColumnOne));
                items.add(new GameMenu(Utils.isWindows() ? "i-exit_to_windows" : "i-quit", Utils.isWindows() ? "${menu.13}" : "${menu.14}",
                        String.format("pauseMenuNavigate(%s,%s,%s,quitToOS())", PauseMenuState.CONFIRMATION.name(),
                                PauseMenuState.QUIT.name(), (Utils.isWindows() ? "${menu.13}" : "${menu.14}")), optionsColumnOne));
                items.add(new GameMenu("i-accept", "${menu.142}", "pauseMenu()", optionsNavigationColumnOne));
                items.add(new GameMenu("i-back", "${menu.20}", String.format("pauseMenuNavigate(%s,null,null,null)", PauseMenuState.MAIN),
                        optionsNavigationColumnTwo));
                break;

            case CONFIRMATION:
                optionsMenuTitle.setText(confirmationTitle);
                // Column one
                new LabelBuilder("confirmLabel", "${menu.15}") {
                    {
                        style("textNormal");
                    }
                }.build(nifty, nifty.getCurrentScreen(), optionsColumnOne);

                items.add(new GameMenu("i-accept", "${menu.21}", confirmMethod, optionsColumnOne));
                items.add(new GameMenu("i-accept", "${menu.142}", "pauseMenu()", optionsNavigationColumnOne));
                items.add(new GameMenu("i-back", "${menu.20}", String.format("pauseMenuNavigate(%s,null,null,null)", backMenu),
                        optionsNavigationColumnTwo));

                break;
        }

        // build menu items
        // FIXME id="#image" and "#text" already exist
        for (GameMenu item : items) {
            new IconTextBuilder("menu-" + NiftyIdCreator.generate(), String.format("Textures/GUI/Options/%s.png", item.id), item.title, item.action) {
                {
                }
            }.build(nifty, nifty.getCurrentScreen(), item.parent);
        }

        // Fix layout
        NiftyUtils.resetContraints(optionsMenuTitle);
        optionsMenu.layoutElements();
    }

    public void quitToMainMenu() {

        // Disable us, detach game and enable start
        stateManager.detach(stateManager.getState(GameState.class));
        setEnabled(false);
        stateManager.getState(MainMenuState.class).setEnabled(true);
    }

    public void quitToOS() {
        app.stop();
    }

    public void select(String state, String id) {
        interactionState.setInteractionState(InteractionState.valueOf(state.toUpperCase()), Integer.valueOf(id));
    }

    @NiftyEventSubscriber(id = "tabs-hud")
    public void onTabChange(String id, TabSelectedEvent event) {
        // TODO: maybe change selected item state when tab change
    }

    private void updateSelectedItem(InteractionState state, int id) {

        for (InteractionState interaction : InteractionState.values()) {
            Element content = nifty.getCurrentScreen().findElementByName("tab-" + interaction.toString().toLowerCase() + "-content");
            if (content == null) {
                continue;
            }

            for (Element e : content.getElements()) {
                boolean visible = e.isVisible();
                if (!visible) { // FIXME: do not remove this. Nifty hack
                    e.show();
                }
                e.stopEffect(EffectEventId.onCustom);
                if (!visible) { // FIXME: do not remove this. Nifty hack
                    e.hide();
                }
            }
        }

        String itemId = state.toString().toLowerCase() + "_" + id;
        Element item = nifty.getCurrentScreen().findElementByName(itemId);
        if (item == null) {
            System.err.println(itemId + " not found"); // FIXME remove this line after debug
            return;
        }
        item.startEffect(EffectEventId.onCustom, null, "select");
    }

    public void action(String action) {
        updatePossessionSelectedItem(PossessionInteractionState.Action.valueOf(action.toUpperCase()));
    }

    private void updatePossessionSelectedItem(PossessionInteractionState.Action action) {
        Element element = nifty.getCurrentScreen().findElementByName("creature-" + action.toString().toLowerCase());
        if (element != null) {
            element.setFocus();
        }
    }

    private class GameMenu {

        protected String title;
        protected String action;
        protected String id;
        protected Element parent;

        public GameMenu() {
        }

        public GameMenu(String id, String title, String action, Element parent) {
            this.id = id;
            this.title = title;
            this.action = action;
            this.parent = parent;
        }
    }
}
