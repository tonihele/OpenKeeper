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
package toniarts.openkeeper.game.controller.creature;

import com.badlogic.gdx.ai.fsm.StateMachine;
import com.jme3.math.Vector3f;
import com.simsilica.es.EntityId;
import java.awt.Point;
import toniarts.openkeeper.game.logic.IGameLogicUpdatable;
import toniarts.openkeeper.game.navigation.pathfinding.INavigable;
import toniarts.openkeeper.game.task.Task;
import toniarts.openkeeper.tools.convert.map.Creature;

/**
 * Controls creature entities
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public interface ICreatureController extends Comparable<ICreatureController>, IGameLogicUpdatable, INavigable {

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

    public void stopCreature();

    public void executeAttack(ICreatureController attackTarget);

    public void navigateToAttackTarget(ICreatureController attackTarget);

    public ICreatureController getFollowTarget();

    public Task getAssignedTask();

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

    public Vector3f getPosition();

    public int getGold();

    public int getMaxGold();

    public void substractGold(int amount);

    public Point getLairLocation();

    public boolean isDragged();

    public boolean isUnconscious();

    public Point getCreatureCoordinates();

    public void setAssignedTask(Task task);

    public void executeAssignedTask();

    public Creature getCreature();

    public void addGold(int amount);

    public Object getObjectiveTargetActionPoint();

    public void setObjectiveTargetActionPoint(Object actionPoint);

    public Object getObjective();

    public void setObjective(Object objective);

    public boolean isDead();

    public boolean isImprisoned();

    public boolean isTortured();

    public boolean isStunned();

    public int getHealth();

    public int getMaxHealth();

    public int getLevel();

    public boolean isPickedUp();

    public void attachPortalGem();

    public void setObjectiveTargetPlayerId(short playerId);

    public void setPlayerObjective(Object object);

    /**
     * Evaluates the time spent in current state and compares it to the
     * creatures target time in a state. The target time would be set by either
     * animation or level variable
     *
     * @return {@code true} if state should be changed
     */
    public boolean isStateTimeExceeded();

}
