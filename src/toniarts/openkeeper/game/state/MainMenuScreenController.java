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

import com.jme3.audio.AudioData;
import com.jme3.audio.AudioNode;
import com.jme3.input.KeyNames;
import com.jme3.system.AppSettings;
import de.lessvoid.nifty.Nifty;
import de.lessvoid.nifty.NiftyEventSubscriber;
import de.lessvoid.nifty.builder.ControlBuilder;
import de.lessvoid.nifty.controls.Chat;
import de.lessvoid.nifty.controls.ChatTextSendEvent;
import de.lessvoid.nifty.controls.CheckBox;
import de.lessvoid.nifty.controls.CheckBoxStateChangedEvent;
import de.lessvoid.nifty.controls.DropDown;
import de.lessvoid.nifty.controls.DropDownSelectionChangedEvent;
import de.lessvoid.nifty.controls.Label;
import de.lessvoid.nifty.controls.ListBox;
import de.lessvoid.nifty.controls.ListBoxSelectionChangedEvent;
import de.lessvoid.nifty.controls.Slider;
import de.lessvoid.nifty.controls.TextField;
import de.lessvoid.nifty.effects.EffectEventId;
import de.lessvoid.nifty.elements.Element;
import de.lessvoid.nifty.elements.render.ImageRenderer;
import de.lessvoid.nifty.elements.render.TextRenderer;
import de.lessvoid.nifty.render.NiftyImage;
import de.lessvoid.nifty.screen.Screen;
import de.lessvoid.nifty.tools.SizeValue;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import toniarts.openkeeper.Main;
import toniarts.openkeeper.game.data.CustomMPDLevel;
import toniarts.openkeeper.game.data.HiScores;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.data.Level;
import toniarts.openkeeper.game.data.Settings;
import toniarts.openkeeper.game.network.chat.ChatSessionListener;
import toniarts.openkeeper.game.network.lobby.LobbyClientService;
import toniarts.openkeeper.game.network.lobby.LobbySessionListener;
import toniarts.openkeeper.gui.nifty.NiftyUtils;
import toniarts.openkeeper.gui.nifty.table.TableRow;
import toniarts.openkeeper.tools.convert.ConversionUtils;
import toniarts.openkeeper.tools.convert.map.GameLevel;
import toniarts.openkeeper.tools.convert.map.KwdFile;
import toniarts.openkeeper.utils.PathUtils;

/**
 *
 * @author ArchDemon
 */
public class MainMenuScreenController implements IMainMenuScreenController {

    public final static String SCREEN_EMPTY_ID = "empty";
    public final static String SCREEN_START_ID = "start";

    private final MainMenuState state;
    private Nifty nifty;
    private Screen screen;
    private static final List<Cutscene> CUTSCENES = new ArrayList<>();
    private ChatSessionListener chatSessionListener;
    private LobbySessionListener lobbySessionListener;
    private static final Logger logger = Logger.getLogger(MainMenuScreenController.class.getName());

    static {
        CUTSCENES.add(new Cutscene("Intro", "INTRO", "${menu.77}"));
        CUTSCENES.add(new Cutscene("000", "CutSceneLevel1", "${speech.1417}"));
        CUTSCENES.add(new Cutscene("001", "CutSceneLevel2", "${speech.1439}"));
        CUTSCENES.add(new Cutscene("002", "CutSceneLevel3", "${speech.1435}"));
        CUTSCENES.add(new Cutscene("003", "CutSceneLevel4", "${speech.1445}"));
        CUTSCENES.add(new Cutscene("004", "CutSceneLevel5", "${speech.1428}"));
        CUTSCENES.add(new Cutscene("005", "CutSceneLevel6", "${speech.1426}"));
        CUTSCENES.add(new Cutscene("006", "CutSceneLevel7", "${speech.1430}"));
        CUTSCENES.add(new Cutscene("007", "CutSceneLevel8", "${speech.1432}"));
        CUTSCENES.add(new Cutscene("008", "CutSceneLevel9", "${speech.1441}"));
        CUTSCENES.add(new Cutscene("009", "CutSceneLevel10", "${speech.1431}"));
        CUTSCENES.add(new Cutscene("010", "CutSceneLevel11", "${speech.1433}"));
        CUTSCENES.add(new Cutscene("011", "CutSceneLevel12", "${speech.1419}"));
        CUTSCENES.add(new Cutscene("012", "CutSceneLevel13", "${speech.1414}"));
        CUTSCENES.add(new Cutscene("013", "CutSceneLevel14", "${speech.1437}"));
        CUTSCENES.add(new Cutscene("014", "CutSceneLevel15", "${speech.1416}"));
        CUTSCENES.add(new Cutscene("015", "CutSceneLevel16", "${speech.1420}"));
        CUTSCENES.add(new Cutscene("016", "CutSceneLevel17", "${speech.1421}"));
        CUTSCENES.add(new Cutscene("017", "CutSceneLevel18", "${speech.1443}"));
        CUTSCENES.add(new Cutscene("018", "CutSceneLevel19", "${speech.1422}"));
        CUTSCENES.add(new Cutscene("Outro", "Outro", "${menu.2843}"));
    }

    public MainMenuScreenController(MainMenuState state) {
        this.state = state;
        nifty = state.app.getNifty();
        screen = nifty.getCurrentScreen();

        // Set some Nifty stuff
        nifty.addResourceBundle("menu", Main.getResourceBundle("Interface/Texts/Text"));
        nifty.addResourceBundle("speech", Main.getResourceBundle("Interface/Texts/Speech"));
        nifty.addResourceBundle("mpd1", Main.getResourceBundle("Interface/Texts/LEVELMPD1_BRIEFING"));
        nifty.addResourceBundle("mpd2", Main.getResourceBundle("Interface/Texts/LEVELMPD2_BRIEFING"));
        nifty.addResourceBundle("mpd3", Main.getResourceBundle("Interface/Texts/LEVELMPD3_BRIEFING"));
        nifty.addResourceBundle("mpd4", Main.getResourceBundle("Interface/Texts/LEVELMPD4_BRIEFING"));
        nifty.addResourceBundle("mpd5", Main.getResourceBundle("Interface/Texts/LEVELMPD5_BRIEFING"));
        nifty.addResourceBundle("mpd6", Main.getResourceBundle("Interface/Texts/LEVELMPD6_BRIEFING"));
    }

    @Override
    public void selectRandomMap() {
        state.mapSelector.random();
        populateSelectedMap(state.mapSelector.getMap());
    }

    @Override
    public void selectMPDLevel(String number) {
        state.selectedLevel = new Level(Level.LevelType.MPD, Integer.parseInt(number), null);
        goToScreen("briefing");
    }

    @Override
    public void cancelMapSelection() {
        if (state.mapSelector.isSkirmish()) {
            nifty.gotoScreen("skirmish");
        } else {
            nifty.gotoScreen("multiplayerCreate");
        }
    }

    @Override
    public void mapSelected() {
        ListBox<TableRow> listBox = screen.findNiftyControl("mapsTable", ListBox.class);
        int selectedMapIndex = listBox.getSelectedIndices().get(0);

        state.mapSelector.selectMap(selectedMapIndex);
        if (state.mapSelector.isSkirmish()) {
            nifty.gotoScreen("skirmish");
        } else {
            nifty.gotoScreen("multiplayerCreate");
        }
    }

    @Override
    public void connectToServer() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void multiplayerCreate() {

        TextField player = screen.findNiftyControl("playerName", TextField.class);
        TextField game = screen.findNiftyControl("gameName", TextField.class);
        TextField port = screen.findNiftyControl("gamePort", TextField.class);

        state.multiplayerCreate(game.getRealText(),
                Integer.valueOf(port.getRealText()),
                player.getRealText());

        // TODO: Some overlay that says connecting?
    }

    @Override
    public void multiplayerConnect() {
        /*
         ListBox<TableRow> games = screen.findNiftyControl("multiplayerGamesTable", ListBox.class);
         if (games == null) {
         logger.warning("Element multiplayerGamesTable not found");
         return;
         }
         TableRow row = games.getFocusItem();
         String host = row.getData().get(1);
         String port = row.getData().get(2);
         */
        TextField player = screen.findNiftyControl("playerName", TextField.class);
        TextField hostAddress = screen.findNiftyControl("hostAddress", TextField.class);
        if (player == null || player.getRealText().isEmpty()
                || hostAddress == null || hostAddress.getRealText().isEmpty()) {
            return;
        }

        state.multiplayerConnect(hostAddress.getRealText(), player.getRealText());

        // TODO: Some overlay that says connecting?
    }

    @Override
    public void multiplayerRefresh() {
        /*
         TextField port = screen.findNiftyControl("gamePort", TextField.class);
         int serverPort = Integer.valueOf(port.getRealText());

         ListBox<TableRow> games = screen.findNiftyControl("multiplayerGamesTable", ListBox.class);
         if (games == null) {
         logger.warning("Element multiplayerGamesTable not found");
         return;
         }
         games.clear();

         LocalServerSearch searcher = new LocalServerSearch(serverPort) {
         int i = 0;

         @Override
         public void onFound(NetworkServer server) {
         TableRow row = new TableRow(i++, server.getName(), server.getHost(), server.getPort() + "");
         games.addItem(row);
         }
         };
         searcher.start();
         */
    }

    @Override
    public void playMovie(String movieFile) {
        state.playMovie(movieFile);
    }

    @Override
    public void applyGraphicsSettings() {
        // Get the controls settings
        boolean needToRestart = true;
        Settings settings = Main.getUserSettings();
        DropDown res = screen.findNiftyControl("resolution", DropDown.class);
        DropDown refresh = screen.findNiftyControl("refreshRate", DropDown.class);
        CheckBox fullscreen = screen.findNiftyControl("fullscreen", CheckBox.class);
        CheckBox vsync = screen.findNiftyControl("verticalSync", CheckBox.class);
        DropDown ogl = screen.findNiftyControl("openGl", DropDown.class);
        DropDown aa = screen.findNiftyControl("antialiasing", DropDown.class);
        DropDown af = screen.findNiftyControl("anisotropicFiltering", DropDown.class);
        CheckBox ssao = screen.findNiftyControl("ssao", CheckBox.class);
        MyDisplayMode mdm = (MyDisplayMode) res.getSelection();

        // TODO: See if we need a restart, but keep in mind that the settings are saved in the restart
        // Set the settings
        settings.getAppSettings().setResolution(mdm.width, mdm.height);
        settings.getAppSettings().setBitsPerPixel(mdm.bitDepth);
        settings.getAppSettings().setFrequency((Integer) refresh.getSelection());
        settings.getAppSettings().setFullscreen(fullscreen.isChecked());
        settings.getAppSettings().setVSync(vsync.isChecked());
        settings.getAppSettings().setRenderer((String) ogl.getSelection());
        settings.getAppSettings().setSamples((Integer) aa.getSelection());
        settings.setSetting(Settings.Setting.ANISOTROPY, af.getSelection());
        settings.setSetting(Settings.Setting.SSAO, ssao.isChecked());

        // This fails and crashes on invalid settings
        if (needToRestart) {
            state.restart();
            nifty.resolutionChanged();
        }
        state.app.setViewProcessors();
    }

    @Override
    public void startLevel(String type) {
        state.startLevel(type);
    }

    @Override
    public void cancelLevelSelect() {
        if (state.selectedLevel instanceof CustomMPDLevel) {
            // go to the custom selection, needs to be checked before because of mpd7
            goToScreen("myPetDungeonMapSelect");
        } else if (state.selectedLevel instanceof Level && ((Level) state.selectedLevel).getType().equals(Level.LevelType.MPD)) {
            goToScreen("myPetDungeon");
        } else {
            doTransition("254", "selectCampaignLevel", null);
        }
        state.selectedLevel = null;
    }

    @Override
    public void restartCredits() {
        Element credits = screen.findElementById("creditList");
        if (credits != null) {
            credits.resetEffects();
            if (!credits.isEffectActive(EffectEventId.onActive)) {
                credits.startEffect(EffectEventId.onActive);
            } else {
                // Screen got changed
                credits.stopEffect(EffectEventId.onActive);
            }
        }
    }

    @Override
    public void quitToOS() {
        state.quitToOS();
    }

    @Override
    public void goToScreen(String screen) {
        nifty.gotoScreen(screen);
    }

    @Override
    public void doTransition(String transition, String screen, String transitionStatic) {
        transition = "EnginePath" + transition;
        transitionStatic = (transitionStatic == null || "null".equals(transitionStatic) ? null : "EnginePath" + transitionStatic);
        state.doTransitionAndGoToScreen(transition, screen, transitionStatic);
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
            case "singlePlayer":
                state.mapSelector.reset();
                state.initSkirmishPlayers();
                break;

            case "selectCampaignLevel":
                state.inputManager.addRawInputListener(state.listener);
                break;

            case "briefing":
                showBriefing();
                break;

            case "hiscores":
                generateHiscoreList();
                break;

            case "optionsGraphics":
                // Populate settings screen
                setGraphicsSettingsToGUI();
                break;

            case "optionsControl":
                setControlSettingsToGUI();
                break;

            case "movies":
                generateMovieList();
                break;

            case "skirmish":
                // Init the screen
                state.mapSelector.setSkirmish(true);
                populateSelectedMap(state.mapSelector.getMap());
                populateSkirmishPlayerTable();
                break;

            case "multiplayer":
                state.mapSelector.reset();
                break;

            case "multiplayerWatch":
                TextField player = screen.findNiftyControl("playerName", TextField.class);
                TextField hostAddress = screen.findNiftyControl("hostAddress", TextField.class);
                player.setText(Main.getUserSettings().getSetting(Settings.Setting.PLAYER_NAME).toString());
                hostAddress.setText(Main.getUserSettings().getSetting(Settings.Setting.MULTIPLAYER_LAST_IP).toString());
                break;

            case "multiplayerCreate":
                state.mapSelector.setSkirmish(false);
                populateSelectedMap(state.mapSelector.getMap());

                ConnectionState connectionState = state.getConnectionState();
                if (connectionState != null) {
                    refreshPlayerList(connectionState.getService(LobbyClientService.class).getPlayers());

                    // Add chat listener
                    state.getChatService().addChatSessionListener(getChatSessionListener());

                    // Add player listener
                    connectionState.getService(LobbyClientService.class).addLobbySessionListener(getLobbySessionListener());

                    Label title = screen.findNiftyControl("multiplayerTitle", Label.class);
                    if (title != null) {
                        title.setText(connectionState.getServerInfo());
                    }
                    if (!connectionState.isGameHost()) {
                        Element element = screen.findElementById("multiplayerMapControl");
                        if (element != null) {
                            element.hide();
                        }
                        element = screen.findElementById("multiplayerPlayerControl");
                        if (element != null) {
                            element.hide();
                        }
                    }
                }
                break;

            case "multiplayerLocal":
                state.mapSelector.reset();
                // Set the game & user name
                player = screen.findNiftyControl("playerName", TextField.class);
                TextField game = screen.findNiftyControl("gameName", TextField.class);
                player.setText(Main.getUserSettings().getSetting(Settings.Setting.PLAYER_NAME).toString());
                game.setText(Main.getUserSettings().getSetting(Settings.Setting.GAME_NAME).toString());
                // multiplayerRefresh();
                break;

            case "skirmishMapSelect":
                // Populate the maps
                populateMapSelection(true);
                break;

            case "myPetDungeonMapSelect":
                // Populate the maps
                state.mapSelector.setMPD(true);
                populateMapSelection(false);
                break;
        }
    }

    @Override
    public void onEndScreen() {
        switch (nifty.getCurrentScreen().getScreenId()) {
            case "selectCampaignLevel":
                state.inputManager.removeRawInputListener(state.listener);
                break;

            case "briefing":
                state.clearLevelBriefingNarration();
                break;

            case "multiplayerCreate":
//                state.client.removeChatSessionListener(getChatSessionListener());
                chatSessionListener = null;
                lobbySessionListener = null;
                state.shutdownMultiplayer(); // TODO: TEMP!
                break;
        }
    }

    private void setScreen(Screen screen) {
        this.screen = screen;
    }

    @NiftyEventSubscriber(id = "resolution")
    public void onResolutionChanged(final String id, final DropDownSelectionChangedEvent<MyDisplayMode> event) {

        //Set the refresh dropdown
        DropDown refresh = screen.findNiftyControl("refreshRate", DropDown.class);
        refresh.clear();
        refresh.addAllItems(event.getSelection().refreshRate);
        refresh.selectItemByIndex(refresh.itemCount() - 1);
    }

    @NiftyEventSubscriber(id = "fullscreen")
    public void onFullscreenChanged(final String id, final CheckBoxStateChangedEvent event) {

        //Set the refresh dropdown
        DropDown refresh = screen.findNiftyControl("refreshRate", DropDown.class);
        if (event.isChecked()) {
            refresh.enable();
        } else {
            refresh.disable();
        }
    }

    @NiftyEventSubscriber(id = "multiplayerChat")
    public void onChatTextSend(final String id, final ChatTextSendEvent event) {
        state.chatTextSend(event.getText());
    }

    @NiftyEventSubscriber(id = "mapsTable")
    public void onListBoxSelectionChanged(final String id, final ListBoxSelectionChangedEvent<TableRow> event) {

        if (event.getSelectionIndices().isEmpty()) {
            return;
        }

        KwdFile map = state.mapSelector.getMaps().get(event.getSelectionIndices().get(0));
        if (state.mapSelector.isMPD()) {
            // on mpd we show the briefing
            state.selectedLevel = new CustomMPDLevel(map);
            goToScreen("briefing");
        } else {
            // The map title
            populateSelectedMap(map);
        }
    }

    private void generateHiscoreList() {
        Element hiscoreList = screen.findElementById("hiscoreList");

        if (hiscoreList != null) {
            for (Element oldElement : hiscoreList.getChildren()) {
                nifty.removeElement(screen, oldElement);
            }

            ControlBuilder hiscoreDesc = new ControlBuilder("hiscoreHead", "hiscoreRow");
            hiscoreDesc.parameter("rank", "${menu.80}");
            hiscoreDesc.parameter("score", "${menu.82}");
            hiscoreDesc.parameter("level", "${menu.2042}");
            hiscoreDesc.parameter("user", "${menu.83}");
            hiscoreDesc.style("nifty-hiscore-head");
            hiscoreDesc.build(nifty, screen, hiscoreList);

            int i = 0;
            for (HiScores.HiScoresEntry hiscore : HiScores.load().getEntries()) {
                ControlBuilder hiscoreControl = new ControlBuilder("hiscore" + i++, "hiscoreRow");
                hiscoreControl.parameter("rank", i + "");
                hiscoreControl.parameter("score", hiscore.getScore() + "");
                hiscoreControl.parameter("level", hiscore.getLevel());
                hiscoreControl.parameter("user", hiscore.getName());
                hiscoreControl.build(nifty, screen, hiscoreList);
            }
        }
    }

    /**
     * Generates the movie list
     */
    private void generateMovieList() {
        // TODO: We should only do that if the progress has changed and at the start of the game
        Element movies = screen.findElementById("movieList");
        if (movies == null) {
            return;
        }

        for (Element oldElement : movies.getChildren()) {
            oldElement.markForRemoval();
        }

        int index = 0;
        String image;
        String action;
        for (Cutscene cutscene : CUTSCENES) {
            if (cutscene.isViewable()) {
                image = cutscene.image;
                action = "playMovie(" + cutscene.click + ")";
            } else {
                image = "Unavailable";
                action = "goToScreen(cutsceneLocked)";
            }

            ControlBuilder control = new ControlBuilder("movie" + index++, "movieButton");
            control.parameter("image", "Textures/Mov_Shots/M-" + image + "-0.png");
            control.parameter("click", action);
            control.parameter("moviename", cutscene.moviename);
            control.build(nifty, screen, movies);
        }
    }

    /**
     * Set ups a sub objective text
     *
     * @param id the element ID
     * @param textId the text ID in the resource bundle
     * @return returns the element
     */
    private Label setupSubObjectiveLabel(String id, String caption) {

        // Get the actual label and set the text
        Label label = screen.findNiftyControl(id, Label.class);
        label.setText(caption.isEmpty() ? "" : "- ".concat(caption));

        // Measure the text height so that the element can be arranged to the the screen without overlapping the othe sub objectives
        TextRenderer renderer = label.getElement().getRenderer(TextRenderer.class);
        label.setHeight(new SizeValue(renderer.getTextHeight() + "px"));

        return label;
    }

    /**
     * Sets the current setting values to the GUI
     */
    private void setGraphicsSettingsToGUI() {

        // Application settings
        AppSettings settings = Main.getUserSettings().getAppSettings();

        GraphicsDevice device = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        MyDisplayMode mdm = new MyDisplayMode(settings);
        List<MyDisplayMode> resolutions = state.getResolutions(device);
        Collections.sort(resolutions);

        // Get values to the settings screen
        // Resolutions
        DropDown res = screen.findNiftyControl("resolution", DropDown.class);
        res.addAllItems(resolutions);
        res.selectItem(mdm);

        //Refresh rates
        DropDown refresh = screen.findNiftyControl("refreshRate", DropDown.class);
        int index = Collections.binarySearch(resolutions, mdm);
        if (index >= 0) {
            refresh.addAllItems(resolutions.get(index).refreshRate);
            refresh.selectItem(settings.getFrequency());
        } else {
            refresh.addItem(mdm.refreshRate.get(0));
        }
        if (!settings.isFullscreen()) {
            refresh.disable();
        }

        //Fullscreen
        CheckBox fullscreen = screen.findNiftyControl("fullscreen", CheckBox.class);
        fullscreen.setChecked(settings.isFullscreen());
        fullscreen.setEnabled(device.isFullScreenSupported());

        //VSync
        CheckBox vsync = screen.findNiftyControl("verticalSync", CheckBox.class);
        vsync.setChecked(settings.isVSync());

        //Antialiasing
        DropDown aa = screen.findNiftyControl("antialiasing", DropDown.class);
        aa.addAllItems(Settings.samples);
        if (Settings.samples.contains(settings.getSamples())) {
            aa.selectItem(settings.getSamples());
        } else {
            aa.addItem(settings.getSamples());
            aa.selectItem(settings.getSamples());
        }

        //Anisotropic filtering
        DropDown af = screen.findNiftyControl("anisotropicFiltering", DropDown.class);
        af.addAllItems(Settings.anisotrophies);
        if (Main.getUserSettings().containsSetting(Settings.Setting.ANISOTROPY)
                && Settings.anisotrophies.contains(Main.getUserSettings().getSettingInteger(Settings.Setting.ANISOTROPY))) {
            af.selectItem(Main.getUserSettings().getSettingInteger(Settings.Setting.ANISOTROPY));
        } else if (Main.getUserSettings().containsSetting(Settings.Setting.ANISOTROPY)) {
            af.addItem(Main.getUserSettings().getSettingInteger(Settings.Setting.ANISOTROPY));
            af.selectItem(Main.getUserSettings().getSettingInteger(Settings.Setting.ANISOTROPY));
        }

        //OpenGL
        DropDown ogl = screen.findNiftyControl("openGl", DropDown.class);
        ogl.addAllItems(Settings.opengl);
        ogl.selectItem(settings.getRenderer());

        //SSAO
        CheckBox ssao = screen.findNiftyControl("ssao", CheckBox.class);
        ssao.setChecked(Main.getUserSettings().getSettingBoolean(Settings.Setting.SSAO));
    }

    private void setControlSettingsToGUI() {
        ListBox<TableRow> listBox = screen.findNiftyControl("keyboardSetup", ListBox.class);
        int i = 0;
        int selected = 0;
        KeyNames kNames = new KeyNames();
        listBox.clear();
        List<Settings.Setting> settings = Settings.Setting.getSettings(Settings.SettingCategory.CONTROLS);

        for (Settings.Setting setting : settings) {
            String keys = "";
            if (setting.getSpecialKey() != null) {
                keys = (kNames.getName(setting.getSpecialKey()) + " + ").replace("Left ", "").replace("Right ", "");
            }
            keys += kNames.getName((int) setting.getDefaultValue()).replace("Left ", "").replace("Right ", "");
            TableRow row = new TableRow(i++, String.format("${menu.%s}", setting.getTranslationKey()), keys);
            listBox.addItem(row);
        }
        listBox.selectItemByIndex(selected);

        screen.findNiftyControl("mouseSensitivity", Slider.class).setValue((float) Settings.Setting.MOUSE_SENSITIVITY.getDefaultValue());
        screen.findNiftyControl("gameSpeed", Slider.class).setValue((float) Settings.Setting.GAME_SPEED.getDefaultValue());
        screen.findNiftyControl("scrollSpeed", Slider.class).setValue((float) Settings.Setting.SCROLL_SPEED.getDefaultValue());
        screen.findNiftyControl("invertMouse", CheckBox.class).setChecked((boolean) Settings.Setting.MOUSE_INVERT.getDefaultValue());
    }

    private void populateSelectedMap(KwdFile map) {
        // The map title
        Label label = screen.findNiftyControl("mapNameTitle", Label.class);
        label.setText(map == null ? "No maps found from " + PathUtils.DKII_MAPS_FOLDER : map.getGameLevel().getName());
        NiftyUtils.resetContraints(label);

        if (map != null) {

            // Player count
            label = screen.findNiftyControl("playerCount", Label.class);
            label.setText(": " + map.getGameLevel().getPlayerCount());
            NiftyUtils.resetContraints(label);

            // Map image
            Element mapImage = screen.findElementById("mapImage");
            NiftyImage img = nifty.createImage(state.getMapThumbnail(map), true);
            mapImage.getRenderer(ImageRenderer.class).setImage(img);
            mapImage.setConstraintWidth(new SizeValue(img.getWidth() + "px"));
            mapImage.setConstraintHeight(new SizeValue(img.getHeight() + "px"));

            // We can't have more players than the map supports
            if (state.skirmishPlayers.size() > map.getGameLevel().getPlayerCount()) {
                state.skirmishPlayers.subList(map.getGameLevel().getPlayerCount(), state.skirmishPlayers.size()).clear();
            }
        }

        // Re-populate
        screen.layoutLayers();
    }

    private void populateSkirmishPlayerTable() {
        ListBox<TableRow> listBox = screen.findNiftyControl("playersTable", ListBox.class);
        listBox.clear();
        int i = 0;
        for (Keeper keeper : state.skirmishPlayers) {
            listBox.addItem(new TableRow(i, keeper.toString(), "", "", "", keeper.isReady() + ""));
            i++;
        }
    }

    /**
     * Populate the map selection with given maps
     */
    private void populateMapSelection(final boolean selectMap) {
        ListBox<TableRow> listBox = screen.findNiftyControl("mapsTable", ListBox.class);
        int i = 0;
        listBox.clear();
        for (KwdFile kwd : state.mapSelector.getMaps()) {

            String name = kwd.getGameLevel().getName();
            if (kwd.getGameLevel().getLvlFlags().contains(GameLevel.LevFlag.IS_MY_PET_DUNGEON_LEVEL)) {
                // the resource tables in all the other levels are completely wrong, so we just use it for custom mpd maps
                name = kwd.getGameLevel().getLevelName().isEmpty() ? kwd.getGameLevel().getName() : kwd.getGameLevel().getLevelName();
            }
            listBox.addItem(new TableRow(i, name,
                    String.valueOf(kwd.getGameLevel().getPlayerCount()),
                    String.format("%s x %s", kwd.getMap().getWidth(), kwd.getMap().getHeight())));

            if (selectMap && kwd.equals(state.mapSelector.getMap())) {
                listBox.selectItemByIndex(i);
            }
            i++;
        }
    }

    private LobbySessionListener getLobbySessionListener() {
        if (lobbySessionListener == null) {
            lobbySessionListener = new LobbySessionListener() {

                @Override
                public void onPlayerListChanged(List<Keeper> players) {
                    state.app.enqueue(() -> {
                        refreshPlayerList(players);
                    });
                }

                @Override
                public void onMapChanged(String mapName) {
                    //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
                }
            };
        }
        return lobbySessionListener;
    }

    private void refreshPlayerList(List<Keeper> players) {
        ListBox<TableRow> playersList = screen.findNiftyControl("playersTable", ListBox.class);
        if (playersList != null) {
            playersList.clear();

            // Get players may take some time on the network...
            for (Keeper keeper : players) {
                playersList.addItem(new TableRow(playersList.itemCount(), keeper.getName(),
                        "", "", Integer.toString(0)
                ));
            }
        }
    }

    public ChatSessionListener getChatSessionListener() {
        if (chatSessionListener == null) {
            chatSessionListener = new ChatSessionListener() {

                private final Chat chat = screen.findNiftyControl("multiplayerChat", Chat.class);

                @Override
                public void playerJoined(int clientId, String playerName) {
                    state.app.enqueue(() -> {
                        chat.addPlayer(playerName, null);
                    });
                }

                @Override
                public void newMessage(int clientId, String playerName, String message) {
                    state.app.enqueue(() -> {
                        chat.receivedChatLine(message, null);
                    });
                }

                @Override
                public void playerLeft(int clientId, String playerName) {
                    state.app.enqueue(() -> {
                        chat.removePlayer(playerName);
                    });
                }
            };
        }
        return chatSessionListener;
    }

    public static class Cutscene {

        protected String image;
        protected String click;
        protected String moviename;

        public Cutscene(String image, String click, String moviename) {
            this.image = image;
            this.click = click;
            this.moviename = moviename;
        }

        /**
         * TODO get real viewable Stub for checking if a cutscene is unlocked
         *
         * @return
         */
        public boolean isViewable() {
            return true;
        }
    }

    private void showBriefing() {
        // Set the dynamic values
        Label levelTitle = screen.findNiftyControl("levelTitle", Label.class);
        // In the level data there are the text IDs for these, but they don't make sense
        Label mainObjective = screen.findNiftyControl("mainObjective", Label.class);
        Element mainObjectiveImage = screen.findElementById("mainObjectiveImage");
        Element mainObjectivePanel = screen.findElementById("mainObjectivePanel");
        Element subObjectivePanel = screen.findElementById("subObjectivePanel");
        String objectiveImage = String.format("Textures/Obj_Shots/%s-$index.png", state.selectedLevel.getFileName());
        NiftyImage img = null;
        GameLevel gameLevel = state.selectedLevel.getKwdFile().getGameLevel();

        if (!gameLevel.hasBriefing()) {
            levelTitle.setText("No Briefing available");
            mainObjectivePanel.hide();
            subObjectivePanel.hide();
            return;
        }

        mainObjectivePanel.show();
        levelTitle.setText(gameLevel.getTitle());
        mainObjective.setText(gameLevel.getMainObjective());

        try {
            img = nifty.createImage(objectiveImage.replace("$index", "0"), false);
            mainObjectiveImage.getRenderer(ImageRenderer.class).setImage(img);
            mainObjectiveImage.setWidth(img.getWidth());
            mainObjectiveImage.setHeight(img.getHeight());
            mainObjectiveImage.show();
        } catch (Exception e) {
            logger.log(java.util.logging.Level.WARNING, "Can''t find image {0}", objectiveImage.replace("$index", "1"));
            mainObjectiveImage.hide();
        }

        String subText1 = gameLevel.getSubObjective1();
        String subText2 = gameLevel.getSubObjective2();
        String subText3 = gameLevel.getSubObjective3();

        subObjectivePanel.hide();
        if (!(subText1.isEmpty() && subText2.isEmpty() && subText3.isEmpty())) {
            // We have subobjectives
            subObjectivePanel.show();
            setupSubObjectiveLabel("subObjective1", subText1);
            setupSubObjectiveLabel("subObjective2", subText2);
            Label subObjective = setupSubObjectiveLabel("subObjective3", subText3);
            // Fix the layout
            subObjective.getElement().getParent().layoutElements();
            Element subObjectiveImage = screen.findElementById("subObjectiveImage");
            subObjectiveImage.hide();

            if (state.selectedLevel instanceof Level && ((Level) state.selectedLevel).getType().equals(Level.LevelType.Level)) {
                try {
                    img = nifty.createImage(objectiveImage.replace("$index", "1"), false);
                    subObjectiveImage.getRenderer(ImageRenderer.class).setImage(img);
                    subObjectiveImage.setWidth(img.getWidth());
                    subObjectiveImage.setHeight(img.getHeight());
                    subObjectiveImage.show();
                } catch (Exception e) {
                    logger.log(java.util.logging.Level.WARNING, "Can't find image {0}", objectiveImage.replace("$index", "1"));
                    subObjectiveImage.hide();
                }

                // Play some tunes!!
                state.levelBriefing = new AudioNode(state.assetManager, ConversionUtils.getCanonicalAssetKey("Sounds/speech_mentor/lev" + String.format("%02d", ((Level) state.selectedLevel).getLevel()) + "001.mp2"), AudioData.DataType.Buffer);
                state.levelBriefing.setLooping(false);
                state.levelBriefing.setDirectional(false);
                state.levelBriefing.setPositional(false);
                state.levelBriefing.play();
            }
        }
    }

}
