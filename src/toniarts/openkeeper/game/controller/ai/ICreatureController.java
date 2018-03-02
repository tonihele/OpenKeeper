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
package toniarts.openkeeper.game.controller.ai;

import com.badlogic.gdx.ai.fsm.StateMachine;
import com.simsilica.es.EntityId;
import toniarts.openkeeper.game.logic.IGameLogicUpdatable;

/**
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public interface ICreatureController extends Comparable<ICreatureController>, IGameLogicUpdatable {

    public boolean shouldFleeOrAttack();

    public void unassingCurrentTask();

    public void navigateToRandomPoint();

    public Object getParty();

    public StateMachine<ICreatureController, CreatureState> getStateMachine();

    public boolean hasObjective();

    public boolean followObjective();

    public boolean needsLair();

    public boolean hasLair();

    public boolean findLair();

    public boolean isNeedForSleep();

    public boolean goToSleep();

    public boolean findWork();

    public boolean isWorker();

    public boolean isTooMuchGold();

    public boolean dropGoldToTreasury();

    public boolean isStopped();

    public void die();

    public void navigateToAssignedTask();

    public boolean isAtAssignedTaskTarget();

    public void dropGold();

    public boolean isWorkNavigationRequired();

    public boolean isAssignedTaskValid();

    public ICreatureController getAttackTarget();

    public boolean isWithinAttackDistance(ICreatureController attackTarget);

    public void stop();

    public void executeAttack(ICreatureController attackTarget);

    public void navigateToAttackTarget(ICreatureController attackTarget);

    public ICreatureController getFollowTarget();

    public Object getAssignedTask();

    public float getDistanceToCreature(ICreatureController followTarget);

    public void navigateToRandomPointAroundTarget(ICreatureController followTarget, int i);

    public void resetFollowTarget();

    public void flee();

    public boolean isAttacked();

    public boolean isEnoughSleep();

    public boolean isFullHealth();

    public EntityId getEntityId();

    public boolean isIncapacitated();

    public boolean isTimeToReEvaluate();

    public void resetReEvaluationTimer();

}
