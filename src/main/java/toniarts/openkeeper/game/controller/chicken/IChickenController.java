/*
 * Copyright (C) 2014-2018 OpenKeeper
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
package toniarts.openkeeper.game.controller.chicken;

import com.badlogic.gdx.ai.fsm.StateMachine;
import toniarts.openkeeper.game.controller.entity.IEntityController;
import toniarts.openkeeper.game.logic.IGameLogicUpdatable;
import toniarts.openkeeper.game.navigation.pathfinding.INavigable;

/**
 * Controls chicken entities
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public interface IChickenController extends IGameLogicUpdatable, INavigable, IEntityController {

    public void navigateToRandomPoint();

    public StateMachine<IChickenController, ChickenState> getStateMachine();

    public boolean isStopped();

    public boolean isTimeToReEvaluate();

    public void resetReEvaluationTimer();

    /**
     * Evaluates the time spent in current state and compares it to the
     * creatures target time in a state. The target time would be set by either
     * animation or level variable
     *
     * @return {@code true} if state should be changed
     */
    public boolean isStateTimeExceeded();

    public void growIntoChicken();

    /**
     * Makes a free-range chicken run half a tile straight away from a
     * creature on its own tile or one of the eight around it. Workers and
     * creatures in the air don't scare it (DKII.EXE 0x49CEC0)
     *
     * @return {@code true} if the chicken ran
     */
    public boolean dodgeCreature();

}
