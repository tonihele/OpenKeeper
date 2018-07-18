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
public interface IPlayerScreenController extends ScreenController {

    /**
     * Select active item on HUD
     *
     * @param iState name of InteractionState#Type
     * @see
     * toniarts.openkeeper.view.PlayerInteractionState.InteractionState.Type
     * @param id id of selected item
     */
    public void select(String iState, String id);

    public void togglePanel();

    public void toggleObjective();

    public void pauseMenu();

    public void onPaused(boolean paused);

    public void pauseMenuNavigate(String menu, String backMenu,
            String confirmationTitle, String confirmMethod);

    public void zoomToDungeon();

    public void zoomToCreature(String creatureId, String uiState);

    public void pickUpCreature(String creatureId, String uiState);

    public void workersAmount(String uiState);

    // TODO move method to own controller or in parameter
    public void zoomToImp(String state);

    public void pickUpImp(String state);

    public void grabGold();

    public String getTooltipText(String bundleId);

    public void quitToMainMenu();

    public void quitToOS();
}
