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
import com.simsilica.es.EntityId;
import java.awt.Point;
import toniarts.openkeeper.game.controller.entity.IEntityController;
import toniarts.openkeeper.game.data.ObjectiveType;
import toniarts.openkeeper.game.logic.IGameLogicUpdatable;
import toniarts.openkeeper.game.navigation.pathfinding.INavigable;
import toniarts.openkeeper.game.task.Task;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.Thing;

/**
 * Controls creature entities
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public interface ICreatureController extends IGameLogicUpdatable, INavigable, IEntityController {

    public boolean shouldFleeOrAttack();

    /**
     * Checks what the creature sees and hears
     */
    public void checkSurroundings();

    public void unassingCurrentTask();

    public void navigateToRandomPoint();

    public IPartyController getParty();

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

    public void navigateToAssignedTask();

    public boolean isAtAssignedTaskTarget();

    public void dropGold();

    public boolean isWorkNavigationRequired();

    public boolean isAssignedTaskValid();

    public ICreatureController getAttackTarget();

    public boolean isWithinAttackDistance(EntityId attackTarget);

    public void stopCreature();

    public void executeAttack(EntityId attackTarget);

    public void navigateToAttackTarget(EntityId attackTarget);

    public ICreatureController getFollowTarget();

    public Task getAssignedTask();

    public float getDistanceToCreature(EntityId target);

    public void navigateToRandomPointAroundTarget(EntityId target, int radius);

    public void resetFollowTarget();

    public void flee();

    public boolean isAttacked();

    public boolean isEnoughSleep();

    public boolean isIncapacitated();

    public boolean isTimeToReEvaluate();

    public void resetReEvaluationTimer();

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

    public int getObjectiveTargetActionPointId();

    public void setObjectiveTargetActionPointId(int actionPointId);

    public Thing.HeroParty.Objective getObjective();

    public void setObjective(Thing.HeroParty.Objective objective);

    public boolean isDead();

    public boolean isImprisoned();

    public boolean isTortured();

    public boolean isStunned();

    public int getLevel();

    public void attachPortalGem();

    public void setObjectiveTargetPlayerId(short playerId);

    public short getObjectiveTargetPlayerId();

    public void setPlayerObjective(ObjectiveType objective);

    public void setCreatureLair(EntityId lairId);

    /**
     * Evaluates the time spent in current state and compares it to the
     * creatures target time in a state. The target time would be set by either
     * animation or level variable
     *
     * @return {@code true} if state should be changed
     */
    public boolean isStateTimeExceeded();

    public void sleep();

    /**
     * Assigns the given creature haul us
     *
     * @param creature the creature hauling us
     */
    public void setHaulable(ICreatureController creature);

    /**
     * Set a target for us to follow
     *
     * @param target target to follow
     */
    public void setFollowTarget(EntityId target);

    public boolean shouldNavigateToFollowTarget();

    public boolean isSlapped();

    public boolean isPortalGemInPosession();

    /**
     * When a creature is hauled to prison, call this to properly seal the enemy
     * to the prison
     */
    public void imprison();

    /**
     * Is the (neutral) creature claimed
     *
     * @return returns {@code true} if the creature is owned by a keeper
     */
    public boolean isClaimed();

    public boolean isHungry();

    public boolean goToEat();

    /**
     * Makes the creature eat the target
     *
     * @param target the devouree
     */
    public void eat(IEntityController target);

    /**
     * Marks that we have eaten a single ration of food
     */
    public void sate();

    /**
     * Get research per second attribute
     *
     * @return research per second
     */
    public int getResearchPerSecond();

}
