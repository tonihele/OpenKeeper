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

import com.badlogic.gdx.ai.fsm.DefaultStateMachine;
import com.badlogic.gdx.ai.fsm.StateMachine;
import com.badlogic.gdx.ai.pfa.GraphPath;
import com.jme3.math.FastMath;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import java.awt.Point;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import toniarts.openkeeper.game.component.CreatureAi;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.Gold;
import toniarts.openkeeper.game.component.Health;
import toniarts.openkeeper.game.component.Mobile;
import toniarts.openkeeper.game.component.Navigation;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.component.TaskComponent;
import toniarts.openkeeper.game.controller.IGameTimer;
import toniarts.openkeeper.game.map.MapTile;
import toniarts.openkeeper.game.navigation.INavigationService;
import toniarts.openkeeper.game.navigation.steering.SteeringUtils;
import toniarts.openkeeper.game.task.ITaskManager;
import toniarts.openkeeper.game.task.Task;
import toniarts.openkeeper.tools.convert.map.ArtResource;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.utils.WorldUtils;

/**
 * Controls an entity with {@link CreatureAi} component. Basically supports the
 * AI state machine.
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class CreatureController implements ICreatureController {

    private final EntityId entityId;
    private final EntityData entityData;
    private final INavigationService navigationService;
    private final ITaskManager taskManager;
    private final IGameTimer gameTimer;
    // TODO: All the data is not supposed to be on entities as they become too big, but I don't want these here either
    private final Creature creature;
    private final StateMachine<ICreatureController, CreatureState> stateMachine;
    private float taskDuration = 0.0f;
    private boolean taskStarted = false;
    private float motionless = 0;

    public CreatureController(EntityId entityId, EntityData entityData, Creature creature, INavigationService navigationService,
            ITaskManager taskManager, IGameTimer gameTimer) {
        this.entityId = entityId;
        this.entityData = entityData;
        this.navigationService = navigationService;
        this.taskManager = taskManager;
        this.creature = creature;
        this.gameTimer = gameTimer;
        this.stateMachine = new DefaultStateMachine<ICreatureController, CreatureState>(this) {

            @Override
            public void changeState(CreatureState newState) {
                super.changeState(newState);

                // Also change our state component
                entityData.setComponent(entityId, new CreatureAi(gameTimer.getGameTime(), newState, creature.getId()));
            }

        };
    }

    @Override
    public boolean shouldFleeOrAttack() {
        return false;
    }

    @Override
    public void unassingCurrentTask() {
        Task assignedTask = getAssignedTask();
        if (assignedTask != null) {
            assignedTask.unassign(this);
            entityData.removeComponent(entityId, TaskComponent.class);
        }
        taskStarted = false;
    }

    @Override
    public void navigateToRandomPoint() {
        final Position position = entityData.getComponent(entityId, Position.class);
        final Mobile mobile = entityData.getComponent(entityId, Mobile.class);
        final Owner owner = entityData.getComponent(entityId, Owner.class);
        if (position != null && mobile != null && owner != null) {
            Point start = WorldUtils.vectorToPoint(position.position);
            Point destination = navigationService.findRandomAccessibleTile(start, 10, this);
            if (destination != null) {
                GraphPath<MapTile> path = navigationService.findPath(start, destination, this);
                entityData.setComponent(entityId, new Navigation(destination, null, SteeringUtils.pathToList(path)));
            }
        }
    }

    @Override
    public Object getParty() {
        // TODO:
        return null;
    }

    @Override
    public StateMachine<ICreatureController, CreatureState> getStateMachine() {
        return stateMachine;
    }

    @Override
    public boolean hasObjective() {
        // TODO:
        return false;
    }

    @Override
    public boolean followObjective() {
        // TODO:
        return false;
    }

    @Override
    public boolean needsLair() {
        // TODO:
        return false;
    }

    @Override
    public boolean hasLair() {
        // TODO:
        return false;
    }

    @Override
    public boolean findLair() {
        // TODO:
        return false;
    }

    @Override
    public boolean isNeedForSleep() {
        // TODO:
        return false;
    }

    @Override
    public boolean goToSleep() {
        // TODO:
        return false;
    }

    @Override
    public boolean findWork() {

        // See if we have some available work
        if (isWorker()) {
            return (taskManager.assignTask(this, false));
        }

        // See that is there a prefered job for us
        // FIXME: moods
        List<Creature.JobPreference> jobs = new ArrayList<>();
        if (creature.getHappyJobs() != null) {
            for (Creature.JobPreference jobPreference : creature.getHappyJobs()) {
                if (taskManager.isTaskAvailable(this, jobPreference.getJobType())) {
                    jobs.add(jobPreference);
                }
            }
        }

        // Choose
        if (!jobs.isEmpty()) {
            return (taskManager.assignTask(this, chooseOnWeight(jobs).getJobType()));
        }

        return false;
    }

    private static Creature.JobPreference chooseOnWeight(List<Creature.JobPreference> items) {
        double completeWeight = 0.0;
        for (Creature.JobPreference item : items) {
            completeWeight += item.getChance();
        }
        double r = FastMath.rand.nextDouble() * completeWeight;
        double countWeight = 0.0;
        for (Creature.JobPreference item : items) {
            countWeight += item.getChance();
            if (countWeight >= r) {
                return item;
            }
        }
        return null;
    }

    @Override
    public boolean isWorker() {
        return entityData.getComponent(entityId, CreatureComponent.class).worker;
    }

    @Override
    public void executeAssignedTask() {
        taskStarted = true;
        if (isAssignedTaskValid()) {
            getAssignedTask().executeTask(this, taskDuration);
        }
    }

    @Override
    public boolean isTooMuchGold() {
        return getGold() >= getMaxGold() && isWorker();
    }

    @Override
    public boolean dropGoldToTreasury() {
        if (getGold() > 0 && isWorker()) {
            if (taskManager.assignGoldToTreasuryTask(this)) {
                navigateToAssignedTask();
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean isStopped() {
        return entityData.getComponent(entityId, Navigation.class) == null;
    }

    @Override
    public void die() {
        // TODO:
    }

    @Override
    public void navigateToAssignedTask() {
        Task assignedTask = getAssignedTask();
        if (assignedTask != null) {
            Vector2f loc = assignedTask.getTarget(this);
            //workNavigationRequired = false;

            if (loc != null) {
                Point destination = WorldUtils.vectorToPoint(loc);
                GraphPath<MapTile> path = navigationService.findPath(getCreatureCoordinates(), destination, this);
                entityData.setComponent(entityId, new Navigation(destination, assignedTask.isFaceTarget() ? assignedTask.getTaskLocation() : null, SteeringUtils.pathToList(path)));
            }
        }
    }

    @Override
    public boolean isAtAssignedTaskTarget() {
        Task assignedTask = getAssignedTask();
        return (assignedTask != null && assignedTask.getTarget(this) != null
                //&& !workNavigationRequired
                && isStopped()
                && isNear(assignedTask.getTarget(this)));
    }

    private boolean isNear(Vector2f target) {
        Vector3f currentPos = getPosition();
        return (target.distanceSquared(currentPos.x, currentPos.z) < 0.5f);
    }

    @Override
    public void dropGold() {
        // TODO:
    }

    @Override
    public boolean isWorkNavigationRequired() {
        // TODO:
        return false;
    }

    @Override
    public boolean isAssignedTaskValid() {
        Task assignedTask = getAssignedTask();
        return (assignedTask != null && assignedTask.isValid(this));
    }

    @Override
    public ICreatureController getAttackTarget() {
        // TODO:
        return null;
    }

    @Override
    public boolean isWithinAttackDistance(ICreatureController attackTarget) {
        // TODO:
        return false;
    }

    @Override
    public void stop() {
        // TODO:
    }

    @Override
    public void executeAttack(ICreatureController attackTarget) {
        // TODO:
    }

    @Override
    public void navigateToAttackTarget(ICreatureController attackTarget) {
        // TODO:
    }

    @Override
    public ICreatureController getFollowTarget() {
        // TODO:
        return null;
    }

    @Override
    public Task getAssignedTask() {
        TaskComponent taskComponent = entityData.getComponent(entityId, TaskComponent.class);
        if (taskComponent != null) {
            return taskManager.getTaskById(taskComponent.taskId);
        }
        return null;
    }

    @Override
    public float getDistanceToCreature(ICreatureController followTarget) {
        // TODO:
        return 0f;
    }

    @Override
    public void navigateToRandomPointAroundTarget(ICreatureController followTarget, int i) {
        // TODO:
    }

    @Override
    public void resetFollowTarget() {
        // TODO:
    }

    @Override
    public void flee() {
        // TODO:
    }

    @Override
    public boolean isAttacked() {
        // TODO:
        return false;
    }

    @Override
    public boolean isEnoughSleep() {
        // TODO:
        return true;
    }

    @Override
    public boolean isFullHealth() {
        // TODO:
        return true;
    }

    @Override
    public EntityId getEntityId() {
        return entityId;
    }

    @Override
    public boolean isIncapacitated() {
        // TODO:
        return false;
    }

    @Override
    public int compareTo(ICreatureController t) {
        return Long.compare(entityId.getId(), t.getEntityId().getId());
    }

    private void initState() {
        stateMachine.changeState(entityData.getComponent(entityId, CreatureAi.class).getCreatureState());
    }

    @Override
    public Vector3f getPosition() {
        Position position = entityData.getComponent(entityId, Position.class);
        return position.position;
    }

    @Override
    public short getOwnerId() {
        Owner owner = entityData.getComponent(entityId, Owner.class);
        return owner.ownerId;
    }

    @Override
    public boolean canFly() {
        Mobile mobile = entityData.getComponent(entityId, Mobile.class);
        return mobile.canFly;
    }

    @Override
    public boolean canWalkOnWater() {
        Mobile mobile = entityData.getComponent(entityId, Mobile.class);
        return mobile.canWalkOnWater;
    }

    @Override
    public boolean canWalkOnLava() {
        Mobile mobile = entityData.getComponent(entityId, Mobile.class);
        return mobile.canWalkOnLava;
    }

    @Override
    public boolean canMoveDiagonally() {
        return true;
    }

    @Override
    public void start() {

    }

    @Override
    public void processTick(float tpf, double gameTime) {
        if (stateMachine.getCurrentState() == null) {
            initState();
        }

        /**
         * The creatures have these time motionless stuff in different states,
         * they seem to equal to kind of re-evaluate what to do. We should
         * figure out a proper way to utilize these. We could also use the
         * delayed telegram stuff. The hard part is just to kind of figure out
         * the motionless part and not re-send messages always etc. We could
         * probably go with pretty much event driven AI.
         */
        if (isStopped()) {
            motionless += tpf;
        } else {
            motionless = 0;
        }

        // Task timer
        if (taskStarted) {
            taskDuration += tpf;
        }

        stateMachine.update();
    }

    @Override
    public boolean isTimeToReEvaluate() {

        // See that we have been motionless for enough time, per state
        // TODO: now just 5 seconds, it is the default for imps
        return motionless >= 5f;
    }

    @Override
    public void resetReEvaluationTimer() {
        motionless = 0;
    }

    @Override
    public void addGold(int amount) {
        Gold gold = entityData.getComponent(entityId, Gold.class);
        entityData.setComponent(entityId, new Gold(gold.gold + amount, gold.maxGold));
    }

    @Override
    public int getGold() {
        return entityData.getComponent(entityId, Gold.class).gold;
    }

    @Override
    public int getMaxGold() {
        return entityData.getComponent(entityId, Gold.class).maxGold;
    }

    @Override
    public void substractGold(int amount) {
        Gold gold = entityData.getComponent(entityId, Gold.class);
        entityData.setComponent(entityId, new Gold(gold.gold - amount, gold.maxGold));
    }

    @Override
    public Point getLairLocation() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean isDragged() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean isUnconscious() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Point getCreatureCoordinates() {
        return WorldUtils.vectorToPoint(getPosition());
    }

    @Override
    public void setAssignedTask(Task task) {

        // Unassign previous task
        unassingCurrentTask();

        taskDuration = 0.0f;
        //workNavigationRequired = true;
        entityData.setComponent(entityId, new TaskComponent(task.getId(), task.getTaskTarget(), task.getTaskLocation(), task.getTaskType()));
    }

    @Override
    public Creature getCreature() {
        return creature;
    }

    @Override
    public void stopCreature() {
        //TODO:
    }

    @Override
    public Object getObjectiveTargetActionPoint() {
        // TODO
        return null;
    }

    @Override
    public void setObjectiveTargetActionPoint(Object actionPoint) {
        //TODO
    }

    @Override
    public Object getObjective() {
        // TODO
        return null;
    }

    @Override
    public void setObjective(Object objective) {
        // TODO
    }

    @Override
    public boolean isDead() {
        // TODO
        return false;
    }

    @Override
    public boolean isImprisoned() {
        // TODO
        return false;
    }

    @Override
    public boolean isTortured() {
        // TODO
        return false;
    }

    @Override
    public boolean isStunned() {
        // TODO
        return false;
    }

    @Override
    public int getHealth() {
        Health health = entityData.getComponent(entityId, Health.class);
        return health.health;
    }

    @Override
    public int getMaxHealth() {
        Health health = entityData.getComponent(entityId, Health.class);
        return health.maxHealth;
    }

    @Override
    public int getLevel() {
        CreatureComponent creatureComponent = entityData.getComponent(entityId, CreatureComponent.class);
        return creatureComponent.level;
    }

    @Override
    public boolean isPickedUp() {
        // TODO
        return false;
    }

    @Override
    public void attachPortalGem() {
        // TODO
    }

    @Override
    public void setObjectiveTargetPlayerId(short playerId) {
        // TODO
    }

    @Override
    public void setPlayerObjective(Object object) {
        // TODO
    }

    @Override
    public boolean isStateTimeExceeded() {
        double timeSpent = gameTimer.getGameTime() - entityData.getComponent(entityId, CreatureAi.class).stateStartTime;

        switch (stateMachine.getCurrentState()) {
            case STUNNED: {
                // Hmm, this might actually be the level variable, the stun seems to be the time fallen when dropped
                return timeSpent >= entityData.getComponent(entityId, CreatureComponent.class).stunDuration;
            }
            case FALLEN: {
                return timeSpent >= entityData.getComponent(entityId, CreatureComponent.class).stunDuration;
            }
            case GETTING_UP: {
                return timeSpent >= getAnimationTime(creature, Creature.AnimationType.GET_UP);
            }
            case ENTERING_DUNGEON: {
                return timeSpent >= getAnimationTime(creature, Creature.AnimationType.ENTRANCE);
            }
        }
        return false;
    }

    private static double getAnimationTime(Creature creature, Creature.AnimationType animation) {
        // TODO: we could cache and calculate these for all centrally, also include the starting and ending animation
        ArtResource animationResource = creature.getAnimation(animation);
        int frames = animationResource.getData("frames");
        int fps = animationResource.getData("fps");
        return frames / (double) fps;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 37 * hash + Objects.hashCode(this.entityId);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final CreatureController other = (CreatureController) obj;
        if (!Objects.equals(this.entityId, other.entityId)) {
            return false;
        }
        return true;
    }

}
