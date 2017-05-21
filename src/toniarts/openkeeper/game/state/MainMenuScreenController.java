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
import de.lessvoid.nifty.tools.Color;
import de.lessvoid.nifty.tools.SizeValue;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Logger;
import toniarts.openkeeper.Main;
import toniarts.openkeeper.game.MapSelector;
import toniarts.openkeeper.game.data.CustomMPDLevel;
import toniarts.openkeeper.game.data.GameResult;
import toniarts.openkeeper.game.data.HiScores;
import toniarts.openkeeper.game.data.Level;
import toniarts.openkeeper.game.data.Level.LevelType;
import toniarts.openkeeper.game.data.Settings;
import toniarts.openkeeper.game.data.Settings.LevelStatus;
import toniarts.openkeeper.game.network.chat.ChatClientService;
import toniarts.openkeeper.game.network.chat.ChatSessionListener;
import toniarts.openkeeper.game.state.lobby.ClientInfo;
import toniarts.openkeeper.game.state.lobby.LobbySession;
import toniarts.openkeeper.game.state.lobby.LobbySessionListener;
import toniarts.openkeeper.game.state.lobby.LobbyState;
import toniarts.openkeeper.gui.nifty.NiftyUtils;
import toniarts.openkeeper.gui.nifty.chat.Chat;
import toniarts.openkeeper.gui.nifty.chat.event.ChatTextSendEvent;
import toniarts.openkeeper.gui.nifty.table.TableColumn;
import toniarts.openkeeper.gui.nifty.table.TableControl;
import toniarts.openkeeper.gui.nifty.table.TableRow;
import toniarts.openkeeper.gui.nifty.table.player.PlayerTableBuilder;
import toniarts.openkeeper.gui.nifty.table.player.PlayerTableRow;
import toniarts.openkeeper.tools.convert.ConversionUtils;
import toniarts.openkeeper.tools.convert.map.AI;
import toniarts.openkeeper.tools.convert.map.GameLevel;
import toniarts.openkeeper.tools.convert.map.KwdFile;
import toniarts.openkeeper.utils.PathUtils;
import toniarts.openkeeper.utils.Utils;

/**
 *
 * @author ArchDemon
 */
public class MainMenuScreenController implements IMainMenuScreenController {

    public final static String SCREEN_EMPTY_ID = "empty";
    public final static String SCREEN_START_ID = "start";
    private final static String PLAYER_LIST_ID = "playersTable";
    public final static String SCREEN_DEBRIEFING_ID = "debriefing";

    private final MainMenuState state;
    private Nifty nifty;
    private Screen screen;
    private static final List<Cutscene> CUTSCENES = new ArrayList<>();
    private ChatSessionListener chatSessionListener;
    private LobbySessionListener lobbySessionListener;

    /**
     * A popup instance if some screen should need one
     */
    private Element popupElement;
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
        state.getLobbyState().setRandomMap();
    }

    @Override
    public void selectMPDLevel(String number) {
        state.selectedLevel = new Level(Level.LevelType.MPD, Integer.parseInt(number));
        goToScreen("briefing");
    }

    @Override
    public void cancelMapSelection() {
        nifty.gotoScreen("skirmishLobby");
    }

    @Override
    public void mapSelected() {
        ListBox<TableRow> listBox = screen.findNiftyControl("mapsTable", ListBox.class);
        int selectedMapIndex = listBox.getSelectedIndices().get(0);
        state.getLobbyState().setMap(selectedMapIndex);
        nifty.gotoScreen("skirmishLobby");
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

        // Overlay
        popupElement = nifty.createPopup("connectingLayer");
        nifty.showPopup(nifty.getCurrentScreen(), popupElement.getId(), null);
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

        // Overlay
        popupElement = nifty.createPopup("connectingLayer");
        nifty.showPopup(nifty.getCurrentScreen(), popupElement.getId(), null);
    }

    protected void showError(String title, String message) {
        closePopup();

        // Open message
        popupElement = nifty.createPopup("errorMessage");
        nifty.showPopup(nifty.getCurrentScreen(), popupElement.getId(), null);

        // Set message text
        Label titleLabel = popupElement.findNiftyControl("title", Label.class);
        titleLabel.setText(title);
        Label messageLabel = popupElement.findNiftyControl("message", Label.class);
        messageLabel.setText(message);
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

            case "multiplayer":
                state.mapSelector.reset();
                break;

            case "multiplayerWatch":
                TextField player = screen.findNiftyControl("playerName", TextField.class);
                TextField hostAddress = screen.findNiftyControl("hostAddress", TextField.class);
                player.setText(Main.getUserSettings().getSetting(Settings.Setting.PLAYER_NAME).toString());
                hostAddress.setText(Main.getUserSettings().getSetting(Settings.Setting.MULTIPLAYER_LAST_IP).toString());
                break;

            case "skirmishLobby":

                LobbyState lobbyState = state.getLobbyState();

                // Set up the players table
                setupPlayersTable(lobbyState);

                // Add chat listener
                if (lobbyState.isOnline() && chatSessionListener == null) {
                    Chat chat = screen.findNiftyControl("multiplayerChat", Chat.class);
                    chat.clear();
                    state.getChatService().addChatSessionListener(getChatSessionListener());
                }
                screen.findElementById("chatPanel").setVisible(lobbyState.isOnline());

                // Add player listener
                lobbyState.addLobbySessionListener(getLobbySessionListener());

                // Ask for players and map
                refreshPlayerList(lobbyState.getLobbySession().getPlayers());
                populateSelectedMap(state.mapSelector.getMap(lobbyState.getLobbySession().getMap()).getMap());

                Label title = screen.findNiftyControl("multiplayerTitle", Label.class);
                if (title != null) {
                    title.setText(lobbyState.getGameName());
                }
                Element element = screen.findElementById("multiplayerMapControl");
                if (element != null) {
                    if (!lobbyState.isHosting()) {
                        element.hide();
                    } else {
                        element.show();
                    }
                }
                element = screen.findElementById("multiplayerPlayerControl");
                if (element != null) {
                    if (!lobbyState.isHosting()) {
                        element.hide();
                    } else {
                        element.show();
                    }
                }

                // Set the IP, is is really always our IP, not the servers?
                Label ip = screen.findNiftyControl("ip", Label.class);
                if (lobbyState.isOnline()) {
                    ip.setText("IP: " + Utils.getLocalIPAddress());
                } else {
                    ip.setText(null);
                }
                TextRenderer renderer = ip.getElement().getRenderer(TextRenderer.class);
                ip.setWidth(new SizeValue(renderer.getTextWidth() + "px"));

                screen.layoutLayers();

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

        // Close any possible popups, otherwise they stay on the screen they were opened on...
        closePopup();

        switch (nifty.getCurrentScreen().getScreenId()) {
            case "selectCampaignLevel":
                state.inputManager.removeRawInputListener(state.listener);
                break;

            case "briefing":
                state.clearLevelBriefingNarration();
                break;

            case "skirmishLobby":

                // Remove the old players table, a bit of a hax
                TableControl playersTable = screen.findControl(PLAYER_LIST_ID, TableControl.class);
                if (playersTable != null) {
                    playersTable.getElement().markForRemoval();
                }
                break;
        }
    }

    private void closePopup() {
        if (popupElement != null) {
            nifty.closePopup(popupElement.getId());
            popupElement = null;
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

        KwdFile map = state.mapSelector.getMaps().get(event.getSelectionIndices().get(0)).getMap();
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
     * @param caption the text
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
        }

        // Re-populate
        screen.layoutLayers();
    }

    /**
     * Populate the map selection with given maps
     */
    private void populateMapSelection(final boolean selectMap) {
        ListBox<TableRow> listBox = screen.findNiftyControl("mapsTable", ListBox.class);
        int i = 0;
        listBox.clear();
        for (MapSelector.GameMapContainer mapContainer : state.mapSelector.getMaps()) {

            String name = mapContainer.getMapName();
            KwdFile kwd = mapContainer.getMap();
            if (kwd.getGameLevel().getLvlFlags().contains(GameLevel.LevFlag.IS_MY_PET_DUNGEON_LEVEL)) {
                // the resource tables in all the other levels are completely wrong, so we just use it for custom mpd maps
                name = kwd.getGameLevel().getLevelName().isEmpty() ? kwd.getGameLevel().getName() : kwd.getGameLevel().getLevelName();
            }
            listBox.addItem(new TableRow(i, name,
                    String.valueOf(kwd.getGameLevel().getPlayerCount()),
                    String.format("%s x %s", kwd.getMap().getWidth(), kwd.getMap().getHeight())));

            if (selectMap && kwd.equals(state.mapSelector.getMap().getMap())) {
                listBox.selectItemByIndex(i);
            }
            i++;
        }
    }

    private LobbySessionListener getLobbySessionListener() {
        if (lobbySessionListener == null) {
            lobbySessionListener = new LobbySessionListener() {

                @Override
                public void onPlayerListChanged(List<ClientInfo> players) {
                    refreshPlayerList(players);
                }

                @Override
                public void onMapChanged(String mapName) {
                    populateSelectedMap(state.mapSelector.getMap(mapName).getMap());
                }
            };
        }
        return lobbySessionListener;
    }

    private void refreshPlayerList(List<ClientInfo> players) {
        TableControl<PlayerTableRow> playersList = screen.findNiftyControl(PLAYER_LIST_ID, TableControl.class);
        if (playersList != null) {

            // Get the selection, so that we can restore it possibly
            ClientInfo selectedClient = null;
            if (!playersList.getSelection().isEmpty()) {
                selectedClient = playersList.getSelection().get(0).getClientInfo();
            }

            playersList.clear();
            LobbyState lobbyState = state.getLobbyState();

            // Populate the players list
            int index = -1;
            int i = 0;
            for (ClientInfo clientInfo : players) {
                if (clientInfo.equals(selectedClient)) {
                    index = i;
                }
                playersList.addItem(new PlayerTableRow(clientInfo, playersList.itemCount(), clientInfo.getKeeper().isAi() ? Utils.getMainTextResourceBundle().getString(clientInfo.getKeeper().getAiType().getTranslationKey()) : clientInfo.getName(),
                        "", lobbyState.isOnline() ? Long.toString(clientInfo.getPing()) : "", lobbyState.isOnline() ? Integer.toString(clientInfo.getSystemMemory()) : "", clientInfo.isReady()
                ));
                i++;
            }

            // Restore selection
            if (index > -1) {
                playersList.selectItemByIndex(index);
            }
        }
    }

    @NiftyEventSubscriber(id = PLAYER_LIST_ID)
    public void onPlayerListSelectionChanged(final String id, final ListBoxSelectionChangedEvent<PlayerTableRow> event) {
        List<PlayerTableRow> selection = event.getSelection();
        Element element = screen.findElementById("changeAi");
        if (selection.isEmpty() || !selection.get(0).getClientInfo().getKeeper().isAi()) {
            element.hide();
        } else {
            TextRenderer textRenderer = element.getRenderer(TextRenderer.class);
            ResourceBundle rb = Utils.getMainTextResourceBundle();
            String text = rb.getString("2121") + ": " + rb.getString(selection.get(0).getClientInfo().getKeeper().getAiType().getTranslationKey());
            textRenderer.setText(text);
            element.setConstraintWidth(new SizeValue(textRenderer.getFont().getWidth(text) + "px"));
            element.show();

            // Recalculate
            element.getParent().layoutElements();
        }
    }

    @Override
    public void cancelMultiplayer() {

        // Disconnect and dismantle
        if (chatSessionListener != null) {
            ChatClientService ccs = state.getChatService();
            if (ccs != null) {
                ccs.removeChatSessionListener(getChatSessionListener());
            }
        }
        LobbyState ls = state.getLobbyState();
        if (lobbySessionListener != null) {
            ls.removeLobbySessionListener(getLobbySessionListener());
        }
        chatSessionListener = null;
        lobbySessionListener = null;
        state.shutdownMultiplayer();

        // Go back to where we were, if we ever left...
        if (ls != null) {
            if (ls.isOnline()) {
                if (ls.isHosting()) {
                    goToScreen("multiplayerLocal");
                } else {
                    goToScreen("multiplayerWatch");
                }
            } else {
                doTransition("272", "singlePlayer", "274");
            }
        }
    }

    public void closeErrorMessage() {
        closePopup();

        // This is really now just used for MP error messages
        cancelMultiplayer();
    }

    public ChatSessionListener getChatSessionListener() {
        if (chatSessionListener == null) {
            chatSessionListener = new ChatSessionListener() {

                private final Chat chat = screen.findNiftyControl("multiplayerChat", Chat.class);

                @Override
                public void playerJoined(int playerId, String playerName) {
                    state.app.enqueue(() -> {
                        chat.receivedChatLine(playerName + " joined...", playerId, (short) 0);
                    });
                }

                @Override
                public void newMessage(int playerId, Short keeperId, String playerName, String message) {
                    state.app.enqueue(() -> {
                        chat.receivedChatLine(playerName + ": " + message, playerId, (keeperId != null ? keeperId : 0));
                    });
                }

                @Override
                public void playerLeft(int playerId, String playerName) {
                    state.app.enqueue(() -> {
                        chat.receivedChatLine(playerName + " left...", playerId, (short) 0);
                    });
                }
            };
        }
        return chatSessionListener;
    }

    @Override
    public void startSkirmish() {

        // Start a local lobby
        state.createLocalLobby();

        // Go to screen
        doTransition("271", "skirmishLobby", "273");
    }

    private void setupPlayersTable(LobbyState lobbyState) {
        Element playersPanel = screen.findElementById("playersPanel");

        // Build a new one
        PlayerTableBuilder cb = new PlayerTableBuilder(PLAYER_LIST_ID,
                new TableColumn("${menu.1688}", 34, String.class, new Color("#32050c30")),
                new TableColumn(lobbyState.isOnline() ? "${menu.263}" : "", 33, String.class, new Color("#32050c30")),
                new TableColumn(lobbyState.isOnline() ? "${menu.195}" : "", 11, String.class, new Color("#00752430")),
                new TableColumn(lobbyState.isOnline() ? "${menu.1499}" : "", 11, String.class, new Color("#00779e30")),
                new TableColumn(null, 11, Boolean.class, new Color("#00752430"))
        ) {
            {
                selectionModeSingle();
                displayItems(4);
                optionalVerticalScrollbar();
            }
        };
        cb.build(nifty, screen, playersPanel);
    }

    @Override
    public void addComputerPlayer() {
        state.getLobbyState().getLobbyService().addPlayer();
    }

    @Override
    public void kickPlayer() {
        TableControl<PlayerTableRow> playersList = screen.findNiftyControl(PLAYER_LIST_ID, TableControl.class);
        if (playersList != null && !playersList.getSelection().isEmpty()) {
            ClientInfo clientInfo = playersList.getSelection().get(0).getClientInfo();

            // See that we wont kick ourselves out
            if (state.getLobbyState().getLobbySession().getPlayerId() != clientInfo.getId()) {
                state.getLobbyState().getLobbyService().removePlayer(clientInfo);
            }
        }
    }

    @Override
    public void setPlayerReady() {
        LobbySession lobbySession = state.getLobbyState().getLobbySession();
        lobbySession.setReady(!lobbySession.isReady());
    }

    @Override
    public void changeAI() {
        TableControl<PlayerTableRow> playersList = screen.findNiftyControl(PLAYER_LIST_ID, TableControl.class);
        if (playersList != null && !playersList.getSelection().isEmpty()) {
            ClientInfo clientInfo = playersList.getSelection().get(0).getClientInfo();
            if (clientInfo.getKeeper() != null && clientInfo.getKeeper().isAi()) {

                // Get "next"
                List<AI.AIType> types = Arrays.asList(AI.AIType.values());
                int index = types.indexOf(clientInfo.getKeeper().getAiType());
                state.getLobbyState().getLobbyService().changeAIType(clientInfo, index + 1 == types.size() ? types.get(0) : types.get(index + 1));
            }
        }
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
         * Check if a cutscene is unlocked
         *
         * @return true if movie is viewable by the user
         */
        public boolean isViewable() {
            boolean status = false;
            if (this.click.startsWith("CutSceneLevel")) {
                final int number = Integer.parseInt(this.image) + 1;

                Level levela = null;
                Level levelb = null;

                switch (number) {
                    case 11:
                        levela = new Level(LevelType.Level, number, "a");
                        levelb = new Level(LevelType.Level, number, "b");
                        Level levelc = new Level(LevelType.Level, number, "c");
                        status = isLevelCompleted(levela) || isLevelCompleted(levelb) || isLevelCompleted(levelc);
                        break;
                    case 6:
                    case 15:
                        levela = new Level(LevelType.Level, number, "a");
                        levelb = new Level(LevelType.Level, number, "b");
                        status = isLevelCompleted(levela) || isLevelCompleted(levelb);
                        break;
                    default:
                        status = isLevelCompleted(new Level(LevelType.Level, number));
                }
            } else if (this.image.equals("Outro")) {
                status = isLevelCompleted(new Level(LevelType.Level, 20));
            } else if (this.image.equals("Intro")) {
                // Intro is always visible
                status = true;
            }

            return status;
        }
        
        private boolean isLevelCompleted(Level level) {
            return Settings.getInstance().getLevelStatus(level).equals(LevelStatus.COMPLETED);
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
            logger.warning("Can't find image " + objectiveImage.replace("$index", "0"));
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
                String speech = String.format("Sounds/speech_mentor/speech_mentorHD/lev%02d001.mp2",
                        ((Level) state.selectedLevel).getLevel());
                state.levelBriefing = new AudioNode(state.assetManager,
                        ConversionUtils.getCanonicalAssetKey(speech),
                        AudioData.DataType.Buffer);
                state.levelBriefing.setLooping(false);
                state.levelBriefing.setDirectional(false);
                state.levelBriefing.setPositional(false);
                state.levelBriefing.play();
            }
        }
    }

    public void showDebriefing(GameResult result) {
        Screen deScreen = nifty.getScreen(SCREEN_DEBRIEFING_ID);

        Label levelTitle = deScreen.findNiftyControl("dLevelTitle", Label.class);

        Element mainObjectiveImage = deScreen.findElementById("dMainObjectiveImage");
        Element subObjectiveImage = deScreen.findElementById("dSubObjectiveImage");

        String objectiveImage = String.format("Textures/Obj_Shots/%s-$index.png", state.selectedLevel.getFileName());
        NiftyImage img = null;
        GameLevel gameLevel = state.selectedLevel.getKwdFile().getGameLevel();
        levelTitle.setText(gameLevel.getTitle());

        try {
            img = nifty.createImage(objectiveImage.replace("$index", "0"), false);
            mainObjectiveImage.getRenderer(ImageRenderer.class).setImage(img);
            mainObjectiveImage.setWidth(img.getWidth());
            mainObjectiveImage.setHeight(img.getHeight());
            mainObjectiveImage.show();
        } catch (Exception e) {
            logger.warning("Can't find image " + objectiveImage.replace("$index", "0"));
            mainObjectiveImage.hide();
        }

        Label totalEvilRating = deScreen.findNiftyControl("totalEvilRating", Label.class);
        Label overallTotalEvilRating = deScreen.findNiftyControl("overallTotalEvilRating", Label.class);
        Label specialsFound = deScreen.findNiftyControl("specialsFound", Label.class);

        subObjectiveImage.hide();
        if (state.selectedLevel instanceof Level
                && ((Level) state.selectedLevel).getType().equals(Level.LevelType.Level)) {
            try {
                img = nifty.createImage(objectiveImage.replace("$index", "1"), false);
                subObjectiveImage.getRenderer(ImageRenderer.class).setImage(img);
                subObjectiveImage.setWidth(img.getWidth());
                subObjectiveImage.setHeight(img.getHeight());
                subObjectiveImage.show();
            } catch (Exception e) {
                logger.warning("Can't find image " + objectiveImage.replace("$index", "1"));
                subObjectiveImage.hide();
            }
        }

        boolean levelWon = result.getData(GameResult.ResultType.LEVEL_WON);
        deScreen.findNiftyControl("levelWon", Label.class).setText(levelWon ? "${menu.21}" : "${menu.22}");
        int timeTaken = Math.round(result.getData(GameResult.ResultType.TIME_TAKEN));
        deScreen.findNiftyControl("timeTaken", Label.class).setText(timeToString(timeTaken));

        goToScreen(SCREEN_DEBRIEFING_ID);
    }

    private String timeToString(int time) {
        String result = "";
        int days = time / 86400;
        if (days != 0) {
            time -= days * 86400;
            result += days;
        }
        int hours = time / 3600;
        if (days != 0 || hours != 0) {
            time -= hours * 3600;
            result += String.format(" %02d", hours);
        }
        int minutes = time / 60;
        if (days != 0 || hours != 0 || minutes != 0) {
            time -= minutes * 60;
            result += String.format(":%02d", minutes);
        }
        int seconds = time;
        result += String.format(":%02d", seconds);

        return result.trim();
    }
}
