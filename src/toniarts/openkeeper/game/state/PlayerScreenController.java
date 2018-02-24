/*
 * Copyright (C) 2014-2016 OpenKeeper
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

import com.jme3.asset.AssetKey;
import com.jme3.asset.AssetManager;
import com.jme3.asset.DesktopAssetManager;
import com.jme3.asset.TextureKey;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;
import com.jme3.texture.plugins.AWTLoader;
import de.lessvoid.nifty.Nifty;
import de.lessvoid.nifty.NiftyEventSubscriber;
import de.lessvoid.nifty.NiftyIdCreator;
import de.lessvoid.nifty.builder.ControlBuilder;
import de.lessvoid.nifty.builder.EffectBuilder;
import de.lessvoid.nifty.builder.ImageBuilder;
import de.lessvoid.nifty.controls.Console;
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
import de.lessvoid.nifty.tools.Color;
import de.lessvoid.nifty.tools.SizeValue;
import de.lessvoid.nifty.tools.SizeValueType;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import toniarts.openkeeper.Main;
import toniarts.openkeeper.game.controller.player.PlayerCreatureControl.CreatureUIState;
import toniarts.openkeeper.game.controller.player.PlayerSpell;
import toniarts.openkeeper.gui.nifty.CreatureCardControl;
import toniarts.openkeeper.gui.nifty.NiftyUtils;
import toniarts.openkeeper.gui.nifty.WorkerAmountControl;
import toniarts.openkeeper.gui.nifty.WorkerEqualControl;
import toniarts.openkeeper.gui.nifty.flowlayout.FlowLayoutControl;
import toniarts.openkeeper.gui.nifty.icontext.IconTextBuilder;
import toniarts.openkeeper.tools.convert.AssetsConverter;
import toniarts.openkeeper.tools.convert.ConversionUtils;
import toniarts.openkeeper.tools.convert.map.ArtResource;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.CreatureSpell;
import toniarts.openkeeper.tools.convert.map.Door;
import toniarts.openkeeper.tools.convert.map.GameLevel;
import toniarts.openkeeper.tools.convert.map.KeeperSpell;
import toniarts.openkeeper.tools.convert.map.Room;
import toniarts.openkeeper.tools.convert.map.Trap;
import toniarts.openkeeper.utils.Utils;
import toniarts.openkeeper.view.PlayerInteractionState;
import toniarts.openkeeper.view.PossessionInteractionState;

/**
 *
 * @author ArchDemon
 */
public class PlayerScreenController implements IPlayerScreenController {

    private enum PauseMenuState {

        MAIN, QUIT, CONFIRMATION;
    }

    public static final String HUD_SCREEN_ID = "hud";
    public static final String POSSESSION_SCREEN_ID = "possession";
    public static final String CINEMATIC_SCREEN_ID = "cinematic";
    public static final String SCREEN_EMPTY_ID = "empty";

    private final PlayerState state;
    private Nifty nifty;
    private Screen screen;

    private Element selectedButton;
    private Label tooltip;
    private boolean initHud = false;
    private String cinematicText;
    private PossessionInteractionState.Action possessionAction = PossessionInteractionState.Action.MELEE;

    private static final Logger logger = Logger.getLogger(PlayerScreenController.class.getName());

    public PlayerScreenController(PlayerState state) {
        this.state = state;
        this.nifty = state.app.getNifty();
        this.screen = nifty.getCurrentScreen();
    }

    @Override
    public void select(String iState, String id) {
        state.interactionState.setInteractionState(PlayerInteractionState.InteractionState.Type.valueOf(iState.toUpperCase()), Integer.valueOf(id));
    }

    @Override
    public void togglePanel() {
        // FIXME work but not properly. Map should not move with other things. Need HUD redesign
        Element element = nifty.getScreen(HUD_SCREEN_ID).findElementById("bottomPanel");

        if (!element.getUserDataKeys().contains("toggle")) {
            element.setUserData("toggle", false);
        }

        boolean toggled = element.getUserData("toggle");
        if (toggled) {
            element.setMarginTop(new SizeValue(0, SizeValueType.Pixel));
        } else {
            element.setMarginTop(new SizeValue(156, SizeValueType.Pixel));
        }
        element.setUserData("toggle", !toggled);
        element.getParent().layoutElements();
    }

    @Override
    public void toggleObjective() {
        Element element = nifty.getScreen(HUD_SCREEN_ID).findElementById("objective");
        if (element.isVisible()) {
            element.hide();
        } else {
            GameLevel gameLevel = state.stateManager.getState(GameState.class).getLevelData().getGameLevel();

            Label mainObjective = element.findNiftyControl("mainObjective", Label.class);
            mainObjective.setText(gameLevel.getMainObjective());

            Label subObjective1 = element.findNiftyControl("subObjective1", Label.class);
            subObjective1.setText(gameLevel.getSubObjective1());
            Label subObjective2 = element.findNiftyControl("subObjective2", Label.class);
            subObjective2.setText(gameLevel.getSubObjective2());
            Label subObjective3 = element.findNiftyControl("subObjective3", Label.class);
            subObjective3.setText(gameLevel.getSubObjective3());

            element.layoutElements();
            element.show();
        }
    }

    @Override
    public void pauseMenu() {
        boolean paused = !state.isPaused();
        // Pause state
        state.setPaused(paused);
        // Set the menuButton
        Element menuButton = nifty.getScreen(HUD_SCREEN_ID).findElementById("menuButton");
        if (paused) {
            menuButton.startEffect(EffectEventId.onCustom, null, "select");
        } else {
            menuButton.stopEffect(EffectEventId.onCustom);
        }

        nifty.getScreen(HUD_SCREEN_ID).findElementById("optionsMenu").setVisible(paused);
        if (paused) {
            pauseMenuNavigate(PauseMenuState.MAIN.name(), null, null, null);
        }
    }

    @Override
    public void pauseMenuNavigate(String menu, String backMenu, String confirmationTitle, String confirmMethod) {
        Element optionsMenu = nifty.getScreen(HUD_SCREEN_ID).findElementById("optionsMenu");
        Label optionsMenuTitle = optionsMenu.findNiftyControl("#title", Label.class);
        Element optionsColumnOne = optionsMenu.findElementById("#columnOne");
        for (Element element : optionsColumnOne.getChildren()) {
            element.markForRemoval();
        }
        Element optionsColumnTwo = optionsMenu.findElementById("#columnTwo");
        for (Element element : optionsColumnTwo.getChildren()) {
            element.markForRemoval();
        }
        Element optionsNavigationColumnOne = optionsMenu.findElementById("#navigationColumnOne");
        for (Element element : optionsNavigationColumnOne.getChildren()) {
            element.markForRemoval();
        }
        Element optionsNavigationColumnTwo = optionsMenu.findElementById("#navigationColumnTwo");
        for (Element element : optionsNavigationColumnTwo.getChildren()) {
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
                }.build(nifty, nifty.getScreen(HUD_SCREEN_ID), optionsColumnOne);

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
            }.build(nifty, nifty.getScreen(HUD_SCREEN_ID), item.parent);
        }

        // Fix layout
        NiftyUtils.resetContraints(optionsMenuTitle);
        optionsMenu.layoutElements();
    }

    @Override
    public void grabGold() {
        state.grabGold(100);
    }

    @Override
    public void zoomToDungeon() {
        state.zoomToDungeon();
    }

    @Override
    public String getTooltipText(String bundleId) {
        String result = Utils.getMainTextResourceBundle().getString(bundleId);

        return state.getTooltipText(result);
    }

    @Override
    public void zoomToCreature(String creatureId, String uiState) {
        state.zoomToCreature(Short.parseShort(creatureId),
                "null".equals(uiState) ? null : CreatureUIState.valueOf(uiState.toUpperCase()));
    }

    @Override
    public void zoomToImp(String uiState) {
        short creatureId = state.getCreatureControl().getImp().getCreatureId();
        zoomToCreature(String.valueOf(creatureId), uiState);
    }

    @Override
    public void pickUpCreature(String creatureId, String uiState) {
        state.pickUpCreature(Short.parseShort(creatureId),
                "null".equals(uiState) ? null : CreatureUIState.valueOf(uiState.toUpperCase()));
    }

    @Override
    public void pickUpImp(String uiState) {
        short creatureId = state.getCreatureControl().getImp().getCreatureId();
        pickUpCreature(String.valueOf(creatureId), uiState);
    }

    @Override
    public void workersAmount(String uiState) {
        WorkerAmountControl.State controlState = WorkerAmountControl.State.valueOf(uiState.toUpperCase());
        if (controlState != null) {
            Screen s = nifty.getScreen(HUD_SCREEN_ID);
            WorkerAmountControl cAmount = s.findNiftyControl("tab-workers", WorkerAmountControl.class);
            cAmount.setState(controlState);

            WorkerEqualControl cEqual = s.findNiftyControl("tab-workers-equal", WorkerEqualControl.class);
            cEqual.setState(controlState);

            Element e = s.findElementById("tab-creature-content");
            for (Element element : e.getChildren()) {
                element.getControl(CreatureCardControl.class).setState(controlState);
            }
        }
    }

    @Override
    public void quitToMainMenu() {
        state.quitToMainMenu();
    }

    @Override
    public void quitToOS() {
        state.quitToOS();
    }

    @Override
    public void bind(Nifty nifty, Screen screen) {
        this.nifty = nifty;
        this.screen = screen;
    }

    @Override
    public void onStartScreen() {
        setScreen(nifty.getCurrentScreen());

        switch (screen.getScreenId()) {
            case HUD_SCREEN_ID: {

                if (initHud) {

                    // Init the HUD items
                    initHud = false;
                    initHudItems(state.assetManager);
                }

                break;
            }
            case POSSESSION_SCREEN_ID:
                // what we need? abitities and spells, also melee
                //final Creature creature = gameState.getLevelData().getCreature((short)13);
                final Creature creature = state.getPossessionCreature();

                Element contentPanel = screen.findElementById("creature-icon");
                if (contentPanel != null) {
                    createCreatureIcon(creature.getIcon1Resource().getName()).build(nifty, screen, contentPanel);
                }

                contentPanel = screen.findElementById("creature-filter");
                if (contentPanel != null) {
                    if (creature.getFirstPersonFilterResource() != null) {
                        new ImageBuilder() {
                            {
                                filename(creature.getFirstPersonFilterResource().getName());
                            }
                        }.build(nifty, screen, contentPanel);
                    } else if (getFilterResourceName(creature.getCreatureId()) != null) {
                        new ImageBuilder() {
                            {
                                filename(getFilterResourceName(creature.getCreatureId()));
                            }
                        }.build(nifty, screen, contentPanel);
                    }

                    if (creature.getFirstPersonGammaEffect() != null) {
                        contentPanel.getRenderer(PanelRenderer.class).setBackgroundColor(getGammaEffectColor(creature.getFirstPersonGammaEffect()));
                    }
                }

                contentPanel = screen.findElementById("creature-abilities");
                if (contentPanel != null) {

                    String ability = getAbilityResourceName(creature.getFirstPersonSpecialAbility1());
                    if (ability != null) {
                        createCreatureAbilityIcon(ability, 1).build(nifty, screen, contentPanel);
                    }

                    ability = getAbilityResourceName(creature.getFirstPersonSpecialAbility2());
                    if (ability != null) {
                        createCreatureAbilityIcon(ability, 2).build(nifty, screen, contentPanel);
                    }
                }

                contentPanel = screen.findElementById("creature-attacks");
                if (contentPanel != null) {
                    for (Element element : contentPanel.getChildren()) {
                        element.markForRemoval();
                    }

                    createCreatureMeleeIcon(creature.getFirstPersonMeleeResource().getName()).build(nifty, screen, contentPanel);

                    int index = 1;
                    for (Creature.Spell s : creature.getSpells()) {
                        // TODO: check creature level availiablity
                        if (s.getCreatureSpellId() == 0) {
                            continue;
                        }
                        //CreatureSpell cs = state.stateManager.getState(GameState.class).getLevelData().getCreatureSpellById(s.getCreatureSpellId());
                        //createCreatureSpellIcon(cs, index++).build(nifty, screen, contentPanel);
                    }
                }
                updatePossessionSelectedItem(possessionAction);
                break;

            case CINEMATIC_SCREEN_ID:
                Label text = screen.findNiftyControl("speechText", Label.class);
                text.setText(cinematicText);
                break;
        }
    }

    @Override
    public void onEndScreen() {
    }

    @NiftyEventSubscriber(id = "tabs-hud")
    public void onTabChange(String id, TabSelectedEvent event) {
        updateSelectedItem(state.getInteractionState());
    }

    protected void initHud(String resource) {
        Screen hud = nifty.getScreen(HUD_SCREEN_ID);

        // Load the level dictionary
        if (resource != null) {
            String levelResource = "Interface/Texts/".concat(resource);
            try {
                nifty.addResourceBundle("level", Main.getResourceBundle(levelResource));
            } catch (Exception ex) {
                logger.log(Level.WARNING, "Failed to load the level dictionary!", ex);
            }
        }

        // Load the HUD
        initHud = true;

        nifty.gotoScreen(PlayerScreenController.HUD_SCREEN_ID);

        hud.layoutLayers();
    }

    public void setPause(boolean paused) {
        nifty.getScreen(HUD_SCREEN_ID).findElementById("optionsMenu").setVisible(paused);
    }

    public Element getGuiConstraint() {
        Element middle = nifty.getScreen(HUD_SCREEN_ID).findElementById("middle");
        return middle;
    }

    public Label getTooltip() {
        if (tooltip == null) {
            tooltip = nifty.getScreen(HUD_SCREEN_ID).findNiftyControl("tooltip", Label.class);
        }

        return tooltip;
    }

    public Console getConsole() {
        return nifty.getScreen(HUD_SCREEN_ID).findNiftyControl("console", Console.class);
    }

    public Element getMessages() {
        return nifty.getScreen(HUD_SCREEN_ID).findElementById("systemMessages");
    }

    /**
     * Sets a level based text as cinematic text (subtitles)
     *
     * @param textId the text id from the dict
     * @param introduction is introduction
     * @param pathId
     */
    public void setCinematicText(int textId, boolean introduction, int pathId) {
        setCinematicText(String.format("${level.%d}", textId - 1));
    }

    /**
     * Sets a general text as cinematic text (subtitles)
     *
     * @param textId id taken from the main bundle
     */
    public void setCinematicText(int textId) {
        setCinematicText(Utils.getMainTextResourceBundle().getString(Integer.toString(textId)));
    }

    private void setCinematicText(String text) {
        Label label = nifty.getScreen(CINEMATIC_SCREEN_ID).findNiftyControl("speechText", Label.class);
        cinematicText = text;
        label.setText(cinematicText);
    }

    private void setScreen(Screen screen) {
        this.screen = screen;
    }

    public void goToScreen(String screen) {
        nifty.gotoScreen(screen);
    }

    /**
     * FIXME not applied when enter possession twice Focus active Action element
     * on GUI
     *
     * @param action
     */
    protected void updatePossessionSelectedItem(PossessionInteractionState.Action action) {
        possessionAction = action;
        Element element = nifty.getScreen(POSSESSION_SCREEN_ID).findElementById("creature-" + possessionAction.toString().toLowerCase());
        if (element != null) {
            element.setFocus();
        }
    }

    /**
     * Initializes the HUD items, such as buildings, spells etc. to level
     * initials
     */
    private void initHudItems(AssetManager assetManager) {
        Screen hud = nifty.getScreen(HUD_SCREEN_ID);

        // Stretch the background image (height-wise) on the background image panel
        try {
            BufferedImage img = ImageIO.read(assetManager.locateAsset(new AssetKey("Textures/GUI/Windows/Panel-BG.png")).openStream());

            // Scale the backgroung image to the panel height, keeping the aspect ratio
            Element panel = nifty.getCurrentScreen().findElementById("bottomBackgroundPanel");
            BufferedImage newImage = new BufferedImage(panel.getHeight() * img.getWidth() / img.getHeight(), panel.getHeight(), img.getType());
            Graphics2D g = newImage.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.drawImage(img, 0, 0, newImage.getWidth(), newImage.getHeight(), 0, 0, img.getWidth(), img.getHeight(), null);
            g.dispose();

            // Convert the new image to a texture, and add a dummy cached entry to the asset manager
            AWTLoader loader = new AWTLoader();
            Texture tex = new Texture2D(loader.load(newImage, false));
            ((DesktopAssetManager) assetManager).addToCache(new TextureKey("HUDBackground", false), tex);

            // Add the scaled one
            NiftyImage niftyImage = nifty.createImage("HUDBackground", true);
            String resizeString = "repeat:0,0," + newImage.getWidth() + "," + newImage.getHeight();
            String areaProviderProperty = ImageModeHelper.getAreaProviderProperty(resizeString);
            String renderStrategyProperty = ImageModeHelper.getRenderStrategyProperty(resizeString);
            niftyImage.setImageMode(ImageModeFactory.getSharedInstance().createImageMode(areaProviderProperty, renderStrategyProperty));
            ImageRenderer renderer = panel.getRenderer(ImageRenderer.class);
            renderer.setImage(niftyImage);
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Failed to open the background image!", ex);
        }

//        PlayerManaControl manaControl = state.getPlayer().getManaControl();
//        if (manaControl != null) {
        //manaControl.addListener(hud.findNiftyControl("mana", Label.class), PlayerManaControl.Type.CURRENT);
        // manaControl.addListener(hud.findNiftyControl("manaGet", Label.class), PlayerManaControl.Type.GAIN);
        //manaControl.addListener(hud.findNiftyControl("manaLose", Label.class), PlayerManaControl.Type.LOSE);
//        }
        hud.findNiftyControl("mana", Label.class).setText(Integer.toString(state.getPlayer().getMana()));
        hud.findNiftyControl("manaGet", Label.class).setText(Integer.toString(state.getPlayer().getManaGain()));
        hud.findNiftyControl("manaLose", Label.class).setText(Integer.toString(state.getPlayer().getManaLoose()));

        if (state.getGoldControl() != null) {
            // state.getGoldControl().addListener(hud.findNiftyControl("gold", Label.class));
        }
        hud.findNiftyControl("gold", Label.class).setText(Integer.toString(state.getPlayer().getGold()));

        // Player creatures
        Element creatureTab = hud.findElementById("tab-creature");
        final Element creaturePanel = creatureTab.findElementById("tab-creature-content");
        removeAllChildElements(creaturePanel);
//        for (final Map.Entry<Creature, Set<CreatureControl>> entry : state.getCreatureControl().getCreatures().entrySet()) {
//            createPlayerCreatureIcon(entry.getKey(), hud, creaturePanel);
//        }
//        state.getCreatureControl().addCreatureListener(new CreatureListener() {
//
//            @Override
//            public void onSpawn(CreatureControl creature) {
//                int total = state.getCreatureControl().getCreatures().get(creature.getCreature()).size();
//                CreatureCardControl card = creaturePanel.findControl("creature_" + creature.getCreature().getCreatureId(),
//                        CreatureCardControl.class);
//                if (card == null) {
//                    card = createPlayerCreatureIcon(creature.getCreature(), hud, creaturePanel);// Create
//                }
//                card.setTotal(total);
//            }
//
//            @Override
//            public void onStateChange(CreatureControl creature, CreatureState newState, CreatureState oldState) {
//                if (newState == CreatureState.PICKED_UP || oldState == CreatureState.PICKED_UP) {
//                    int total = 0;
//                    Set<CreatureControl> c = state.getCreatureControl().getCreatures().get(creature.getCreature());
//                    for (CreatureControl creatureControl : c) {
//                        if (creatureControl.getStateMachine().getCurrentState() != CreatureState.PICKED_UP) {
//                            total++;
//                        }
//                    }
//                    CreatureCardControl card = creaturePanel.findControl("creature_" + creature.getCreature().getCreatureId(),
//                            CreatureCardControl.class);
//                    card.setTotal(total);
//                }
//            }
//
//            @Override
//            public void onDie(CreatureControl creature) {
//                int total = state.getCreatureControl().getCreatures().get(creature.getCreature()).size();
//                CreatureCardControl card = creaturePanel.findControl("creature_" + creature.getCreature().getCreatureId(),
//                        CreatureCardControl.class);
//                card.setTotal(total);
//                if (total == 0) {
//                    card.getElement().markForRemoval(); // Remove
//                }
//            }
//        });
//        state.getCreatureControl().addWorkerListener(creatureTab.findControl("tab-workers",
//                WorkerAmountControl.class));

        // Rooms
//        state.getRoomControl().addRoomAvailabilityListener(new PlayerRoomControl.IRoomAvailabilityListener() {
//
//            @Override
//            public void onChange() {
//                populateRoomTab();
//            }
//        });
        populateRoomTab();
        // Spells
//        state.getSpellControl().addPlayerSpellListener(new PlayerSpellListener() {
//            @Override
//            public void onAdded(PlayerSpell spell) {
//                populateSpellTab();
//            }
//
//            @Override
//            public void onRemoved(PlayerSpell spell) {
//                populateSpellTab();
//            }
//
//            @Override
//            public void onResearchStatusChanged(PlayerSpell spell) {
//                // FIXME: implement properly
//                populateSpellTab();
//            }
//        });
//        populateSpellTab();
//        FlowLayoutControl contentPanel = hud.findElementById("tab-workshop-content").getControl(FlowLayoutControl.class);
//        contentPanel.removeAll();
//        for (final Door door : state.getAvailableDoors()) {
//            contentPanel.addElement(createDoorIcon(door));
//        }
//        for (final Trap trap : state.getAvailableTraps()) {
//            contentPanel.addElement(createTrapIcon(trap));
//        }
    }

    public void setGold(int gold) {
        Screen hud = nifty.getScreen(HUD_SCREEN_ID);
        hud.findNiftyControl("gold", Label.class).setText(Integer.toString(gold));
    }

    public void setMana(int mana, int manaLoose, int manaGain) {
        Screen hud = nifty.getScreen(HUD_SCREEN_ID);
        hud.findNiftyControl("mana", Label.class).setText(Integer.toString(mana));
        hud.findNiftyControl("manaGet", Label.class).setText(Integer.toString(manaGain));
        hud.findNiftyControl("manaLose", Label.class).setText(Integer.toString(manaLoose));
    }

    /**
     * Populates the player spells tab
     */
    public void populateSpellTab() {
        Screen hud = nifty.getScreen(HUD_SCREEN_ID);
        FlowLayoutControl contentPanel = hud.findElementById("tab-spell-content").getControl(FlowLayoutControl.class);
        contentPanel.removeAll();
        for (final Entry<KeeperSpell, PlayerSpell> entry : state.getSpellControl().getTypes().entrySet()) {
            contentPanel.addElement(createSpellIcon(entry.getValue()));
        }
    }

    /**
     * Populates the player rooms tab
     */
    public void populateRoomTab() {
        Screen hud = nifty.getScreen(HUD_SCREEN_ID);
        FlowLayoutControl contentPanel = hud.findElementById("tab-room-content").getControl(FlowLayoutControl.class);
        contentPanel.removeAll();
        for (final Room room : state.getAvailableRoomsToBuild()) {
            contentPanel.addElement(createRoomIcon(room));
        }
    }

    /**
     * Deletes all children from element
     *
     * @param element parent
     */
    private void removeAllChildElements(Element element) {
        for (Element e : element.getChildren()) {
            e.markForRemoval();
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

    protected void updateSelectedItem(PlayerInteractionState.InteractionState state) {
        if (selectedButton != null) {
            selectedButton.stopEffect(EffectEventId.onCustom);
        }

        String itemId = state.getType().toString().toLowerCase() + "_" + state.getItemId();
        selectedButton = nifty.getScreen(HUD_SCREEN_ID).findElementById(itemId);
        if (selectedButton != null) {
            selectedButton.startEffect(EffectEventId.onCustom, null, "select");
        }
    }

    private ImageBuilder createCreatureAbilityIcon(final String name, final int index) {

        return new ImageBuilder() {
            {
                valignCenter();
                marginRight("6px");
                focusable(true);
                id("creature-ability_" + index);
                filename(ConversionUtils.getCanonicalAssetKey(AssetsConverter.TEXTURES_FOLDER
                        + File.separator + name + ".png"));
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
                filename(ConversionUtils.getCanonicalAssetKey(AssetsConverter.TEXTURES_FOLDER
                        + File.separator + name + ".png"));
                valignCenter();
            }
        };
    }

    private ImageBuilder createCreatureMeleeIcon(final String name) {
        return new ImageBuilder() {
            {
                filename(ConversionUtils.getCanonicalAssetKey(AssetsConverter.TEXTURES_FOLDER
                        + File.separator + name + ".png"));
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
                filename(ConversionUtils.getCanonicalAssetKey(AssetsConverter.TEXTURES_FOLDER
                        + File.separator + cs.getGuiIcon().getName() + ".png"));
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

    private CreatureCardControl createPlayerCreatureIcon(Creature creature, Screen hud, Element parent) {
        ControlBuilder cb = new ControlBuilder("creature") {
            {
                filename(ConversionUtils.getCanonicalAssetKey(AssetsConverter.TEXTURES_FOLDER
                        + File.separator + creature.getPortraitResource().getName() + ".png"));
                parameter("creatureId", Integer.toString(creature.getCreatureId()));
                id("creature_" + creature.getCreatureId());
            }
        };
        Element element = cb.build(nifty, hud, parent);

        return element.getControl(CreatureCardControl.class);
    }

    private ControlBuilder createRoomIcon(final Room room) {
        String name = Utils.getMainTextResourceBundle().getString(Integer.toString(room.getNameStringId()));
        final String hint = Utils.getMainTextResourceBundle().getString("1783")
                .replace("%1", name)
                .replace("%2", room.getCost() + "");
        return createIcon(room.getRoomId(), "room", room.getGuiIcon(), room.getGeneralDescriptionStringId(),
                hint.replace("%21", room.getCost() + ""), true);
    }

    private ControlBuilder createSpellIcon(final PlayerSpell spell) {
        if (spell.isDiscovered()) {
            String name = Utils.getMainTextResourceBundle().getString(Integer.toString(spell.getKeeperSpell().getNameStringId()));
            String hint = Utils.getMainTextResourceBundle().getString("1785")
                    .replace("%1", name)
                    .replace("%2", spell.getKeeperSpell().getManaCost() + "")
                    .replace("%3", spell.isUpgraded() ? "2" : "1");
            return createIcon(spell.getKeeperSpell().getKeeperSpellId(), "spell", spell.isUpgraded() ? spell.getKeeperSpell().getGuiIcon().getName() + "-2" : spell.getKeeperSpell().getGuiIcon().getName(), spell.getKeeperSpell().getGeneralDescriptionStringId(), hint, true);
        }
        return createIcon(spell.getKeeperSpell().getKeeperSpellId(),
                "spell", "gui\\spells\\s-tba", null, null, false);
    }

    private ControlBuilder createDoorIcon(final Door door) {
        String name = Utils.getMainTextResourceBundle().getString(Integer.toString(door.getNameStringId()));
        final String hint = Utils.getMainTextResourceBundle().getString("1783")
                .replace("%1", name)
                .replace("%2", door.getGoldCost() + "");
        return createIcon(door.getDoorId(), "door", door.getGuiIcon(), door.getGeneralDescriptionStringId(), hint, true);
    }

    private ControlBuilder createTrapIcon(final Trap trap) {
        String name = Utils.getMainTextResourceBundle().getString(Integer.toString(trap.getNameStringId()));
        final String hint = Utils.getMainTextResourceBundle().getString("1784")
                .replace("%1", name)
                .replace("%2", trap.getManaCost() + "");
        return createIcon(trap.getTrapId(), "trap", trap.getGuiIcon(), trap.getGeneralDescriptionStringId(), hint.replace("%17", trap.getManaCost() + ""), true);
    }

    public ControlBuilder createIcon(final int id, final String type, final ArtResource guiIcon, final int generalDescriptionId, final String hint, final boolean allowSelect) {
        return createIcon(id, type, guiIcon.getName(), generalDescriptionId, hint, allowSelect);
    }

    public ControlBuilder createIcon(final int id, final String type, final String guiIcon, final Integer generalDescriptionId, final String hint, final boolean allowSelect) {
        ControlBuilder cb = new ControlBuilder(type + "_" + id, "guiIcon") {
            {
                parameter("image", ConversionUtils.getCanonicalAssetKey(AssetsConverter.TEXTURES_FOLDER + File.separator + guiIcon + ".png"));
                if (generalDescriptionId != null) {
                    parameter("tooltip", "${menu." + generalDescriptionId + "}");
                }
                if (hint != null) {
                    parameter("hint", hint);
                }
//                if (allowSelect) {
                parameter("click", "select(" + type + ", " + id + ")");
                parameter("hoverImage", ConversionUtils.getCanonicalAssetKey(AssetsConverter.TEXTURES_FOLDER + File.separator + "GUI/Icons/frame.png"));
                parameter("activeImage", ConversionUtils.getCanonicalAssetKey(AssetsConverter.TEXTURES_FOLDER + File.separator + "GUI/Icons/selected-" + type + ".png"));
//                }
            }
        };
        return cb;
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
