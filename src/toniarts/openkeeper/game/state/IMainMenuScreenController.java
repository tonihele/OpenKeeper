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

import de.lessvoid.nifty.screen.ScreenController;

/**
 *
 * @author ArchDemon
 */
public interface IMainMenuScreenController extends ScreenController {

    public final static String SOUND_BUTTON_ID = "buttonClick";
    public final static String SOUND_MENU_ID = "menuClick";
    public final static String SCREEN_EMPTY_ID = "empty";
    public final static String SCREEN_START_ID = "start";
    public final static String SCREEN_DEBRIEFING_ID = "debriefing";
    public final static String SCREEN_OPTIONS_MAIN_ID = "optionsMain";
    public final static String PLAYER_LIST_ID = "playersTable";

    public void applySoundSettings();
    //// screen id="multiplayerCreate"  ////
    //public void multiplayerSend()
    public void addComputerPlayer();

    //// screen id="skirmish" and id="multiplayerCreate" ////
    public void changeAI();

    public void kickPlayer();

    public void setPlayerReady();

    //public void gameSettings();
    public void selectRandomMap();

    /**
     * Start local multiplayer
     */
    public void startSkirmish();

    //// screen id="myPetDungeon" ////
    /**
     * Select a my pet dungeon level
     *
     * @param number the level number as a string
     */
    public void selectMPDLevel(String number);

    //// screen id="skirmishMapSelect" ////
    public void cancelMapSelection();

    public void mapSelected();

    //// screen id="multiplayerLocal" ////
    public void connectToServer();

    public void multiplayerCreate();

    public void multiplayerConnect();

    public void multiplayerRefresh();

    //// screen id="extras" ////
    public void playMovie(String movieFile);

    //// screen id="optionsGraphics"  ////
    /**
     * Save the graphics settings
     */
    public void applyGraphicsSettings();

    //// screen id="campaign" ////
    public void startLevel(String type);

    /**
     * Cancel level selection and go back to the campaign map selection
     */
    public void cancelLevelSelect();

    //// see CreditsControl.xml ////
    /**
     * TODO name of fuction set to a variable Called by the gui to restart the
     * autoscroll effect
     */
    public void restartCredits();

    //// screen id="quitGame" ////
    public void quitToOS();

    //// screen id="start" and ... ////
    /**
     * Switch to another screen
     *
     * @param screen the screen id as a string
     */
    public void goToScreen(String screen);

    /**
     * Go to screen with cinematic transition
     *
     * @param transition the transition code
     * @param screen the screen to go to
     * @param transitionStatic the transition for the finishing position. Not
     * all the transitions return perfectly so this is a workaround
     */
    public void doTransition(String transition, String screen, String transitionStatic);

    /**
     * Cancel multiplayer lobby
     */
    public void cancelMultiplayer();
}
