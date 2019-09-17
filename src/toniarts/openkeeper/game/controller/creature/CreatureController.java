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
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import toniarts.openkeeper.game.component.Attack;
import toniarts.openkeeper.game.component.AttackTarget;
import toniarts.openkeeper.game.component.CreatureAi;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.CreatureExperience;
import toniarts.openkeeper.game.component.CreatureFall;
import toniarts.openkeeper.game.component.CreatureHunger;
import toniarts.openkeeper.game.component.CreatureImprisoned;
import toniarts.openkeeper.game.component.CreatureMeleeAttack;
import toniarts.openkeeper.game.component.CreatureRecuperating;
import toniarts.openkeeper.game.component.CreatureSleep;
import toniarts.openkeeper.game.component.CreatureTortured;
import toniarts.openkeeper.game.component.Fearless;
import toniarts.openkeeper.game.component.FollowTarget;
import toniarts.openkeeper.game.component.Gold;
import toniarts.openkeeper.game.component.HauledBy;
import toniarts.openkeeper.game.component.Health;
import toniarts.openkeeper.game.component.InHand;
import toniarts.openkeeper.game.component.Mobile;
import toniarts.openkeeper.game.component.Navigation;
import toniarts.openkeeper.game.component.Objective;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Party;
import toniarts.openkeeper.game.component.PlayerObjective;
import toniarts.openkeeper.game.component.PortalGem;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.component.RoomStorage;
import toniarts.openkeeper.game.component.Slapped;
import toniarts.openkeeper.game.component.TaskComponent;
import toniarts.openkeeper.game.component.Threat;
import toniarts.openkeeper.game.controller.ICreaturesController;
import toniarts.openkeeper.game.controller.IGameTimer;
import toniarts.openkeeper.game.controller.ILevelInfo;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.IObjectsController;
import toniarts.openkeeper.game.controller.entity.EntityController;
import toniarts.openkeeper.game.controller.entity.IEntityController;
import toniarts.openkeeper.game.controller.room.AbstractRoomController;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.data.ObjectiveType;
import toniarts.openkeeper.game.logic.IEntityPositionLookup;
import toniarts.openkeeper.game.map.MapTile;
import toniarts.openkeeper.game.navigation.INavigationService;
import toniarts.openkeeper.game.navigation.steering.SteeringUtils;
import toniarts.openkeeper.game.task.ITaskManager;
import toniarts.openkeeper.game.task.Task;
import toniarts.openkeeper.tools.convert.map.ArtResource;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.Player;
import toniarts.openkeeper.tools.convert.map.Thing;
import toniarts.openkeeper.tools.convert.map.Variable;
import toniarts.openkeeper.utils.WorldUtils;

/**
 * Controls an entity with {@link CreatureAi} component. Basically supports the
 * AI state machine.
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class CreatureController extends EntityController implements ICreatureController {

    private final INavigationService navigationService;
    private final ITaskManager taskManager;
    private final IGameTimer gameTimer;
    private final Map<Variable.MiscVariable.MiscType, Variable.MiscVariable> gameSettings;
    private final ICreaturesController creaturesController;
    private final IEntityPositionLookup entityPositionLookup;
    private final ILevelInfo levelInfo;
    // TODO: All the data is not supposed to be on entities as they become too big, but I don't want these here either
    private final Creature creature;
    private final StateMachine<ICreatureController, CreatureState> stateMachine;
    private float taskDuration = 0.0f;
    private boolean taskStarted = false;
    private float motionless = 0;

    private static final Logger LOGGER = Logger.getLogger(CreatureController.class.getName());

    public CreatureController(EntityId entityId, EntityData entityData, Creature creature, INavigationService navigationService,
            ITaskManager taskManager, IGameTimer gameTimer, Map<Variable.MiscVariable.MiscType, Variable.MiscVariable> gameSettings,
            ICreaturesController creaturesController, IEntityPositionLookup entityPositionLookup, IMapController mapController,
            ILevelInfo levelInfo, IObjectsController objectsController) {
        super(entityId, entityData, objectsController, mapController);
        this.navigationService = navigationService;
        this.taskManager = taskManager;
        this.creature = creature;
        this.gameTimer = gameTimer;
        this.gameSettings = gameSettings;
        this.creaturesController = creaturesController;
        this.entityPositionLookup = entityPositionLookup;
        this.levelInfo = levelInfo;
        this.stateMachine = new DefaultStateMachine<>(this);
    }

    @Override
    public boolean shouldFleeOrAttack() {
        checkSurroundings();

        // Check fleeing, TODO: Always flee?
        boolean inDHeart = false;
        if (entityData.getComponent(entityId, Fearless.class) == null) {
            Threat threatComponent = entityData.getComponent(entityId, Threat.class);
            int threat = threatComponent != null ? threatComponent.threat : 0;
            int fear = entityData.getComponent(entityId, CreatureComponent.class).fear;
            int threatToUs = getEnemyThreat();
            int threatCaused = creature.getFlags().contains(Creature.CreatureFlag.ALWAYS_FLEE)
                    || isHealthAtCriticalLevel() ? threat : getOurThreat();
            if (threatToUs - threatCaused > fear && (getFellowFighters() == 0
                    || creature.getFlags().contains(Creature.CreatureFlag.ALWAYS_FLEE))) {

                // No longer flee from DHeart
                IRoomController roomController = mapController.getRoomControllerByCoordinates(getCreatureCoordinates());
                if (roomController == null || !roomController.isDungeonHeart()) {
                    if (!stateMachine.isInState(CreatureState.FLEE)) {

                        stateMachine.changeState(CreatureState.FLEE);
                    }
                    return true;
                } else {
                    inDHeart = true;
                }
            }
        }

        // Should we attack, try to avoid i.e. imps engaging in a fight
        if ((creature.getFightStyle()
                != Creature.FightStyle.NON_FIGHTER || inDHeart) && getAttackTarget() != null) {
            if (!stateMachine.isInState(CreatureState.FIGHT)) {
                stateMachine.changeState(CreatureState.FIGHT);
            }
            return true;
        }

        return false;
    }

    @Override
    public void checkSurroundings() {

        // Scan for neutral creatures to claim
        short ownerId = getOwnerId();
        if (ownerId != Player.NEUTRAL_PLAYER_ID && ownerId != Player.GOOD_PLAYER_ID) {
            for (EntityId entity : entityPositionLookup.getSensedEntities(entityId)) {
                Owner owner = entityData.getComponent(entity, Owner.class);
                if (owner != null && owner.ownerId == Player.NEUTRAL_PLAYER_ID) {
                    entityData.setComponent(entity, new Owner(ownerId));
                }
            }
        }
    }

    /**
     * Checks if we should fear death
     *
     * @return true if we have critically low health level
     */
    private boolean isHealthAtCriticalLevel() {
        return gameSettings.get(Variable.MiscVariable.MiscType.CREATURE_CRITICAL_HEALTH_PERCENTAGE_OF_MAX).getValue() > getHealthPercentage();
    }

    private int getFellowFighters() {
        int fellowFighters = 0;
        for (EntityId entity : entityPositionLookup.getSensedEntities(entityId)) {
            if (isAlly(entity) && !isIncapacitated(entity) && getEntityFightingStyle(entity) != Creature.FightStyle.NON_FIGHTER) {
                fellowFighters++;
            }
        }

        return fellowFighters;
    }

    /**
     * Gets the total threat caused by the enemies visible
     *
     * @return total enemy threat
     */
    private int getEnemyThreat() {
        int enemyThreat = 0;
        for (EntityId entity : entityPositionLookup.getSensedEntities(entityId)) {
            if (isEnemy(entity) && isThreat(entity)) {
                enemyThreat += getThreat(entity);
            }
        }

        return enemyThreat;
    }

    /**
     * Checks if the given entity posseses a threat to anyone at all
     *
     * @param entity the entity to check
     * @return is the entity a threat to anyone
     */
    private boolean isThreat(EntityId entity) {
        return !isIncapacitated(entity) && !isCaptive(entity);
    }

    /**
     * Checks if the given entity is a captive. Unable to join a fight and also
     * posses no threat to anyone
     *
     * @param entity the entity
     * @return is entity a captive
     */
    private boolean isCaptive(EntityId entity) {
        // TODO: Fighting in arena is kind of captivity
        return isImprisoned(entityData, entity) || isTortured(entityData, entity);
    }

    /**
     * Gets the total threat caused by us. Meaning the band of brothers visible
     * to us
     *
     * @return total threat caused by us
     */
    private int getOurThreat() {
        int ourThreat = 0;
        for (EntityId entity : entityPositionLookup.getSensedEntities(entityId)) {
            if (isAlly(entity) && isThreat(entity)) {
                ourThreat += getThreat(entity);
            }
        }

        return ourThreat;
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
                createNavigation(start, destination, null);
            }
        }
    }

    @Override
    public IPartyController getParty() {
        Party party = entityData.getComponent(entityId, Party.class);
        if (party != null) {
            return creaturesController.getPartyById(party.partyId);
        }
        return null;
    }

    @Override
    public StateMachine<ICreatureController, CreatureState> getStateMachine() {
        return stateMachine;
    }

    @Override
    public boolean hasObjective() {
        Objective creatureObjective = entityData.getComponent(entityId, Objective.class);
        return (creatureObjective != null && creatureObjective.objective != null);
    }

    @Override
    public boolean followObjective() {
        return taskManager.assignObjectiveTask(this, entityData.getComponent(entityId, Objective.class).objective);
    }

    @Override
    public boolean needsLair() {
        return getOwnerId() >= Player.KEEPER1_ID && entityData.getComponent(entityId, CreatureSleep.class) != null;
    }

    @Override
    public boolean hasLair() {
        CreatureSleep creatureSleep = entityData.getComponent(entityId, CreatureSleep.class);
        return creatureSleep != null && creatureSleep.lairObjectId != null && entityData.getEntity(creatureSleep.lairObjectId, Position.class) != null;
    }

    @Override
    public boolean findLair() {
        return taskManager.assignClosestRoomTask(this, AbstractRoomController.ObjectType.LAIR, null);
    }

    @Override
    public boolean isNeedForSleep() {
        CreatureSleep creatureSleep = entityData.getComponent(entityId, CreatureSleep.class);
        return creatureSleep != null && needsLair() && (gameTimer.getGameTime() - creatureSleep.lastSleepTime >= creature.getAttributes().getTimeAwake()
                || isNeedForRecuperating());
    }

    private boolean isNeedForRecuperating() {
        return gameSettings.get(Variable.MiscVariable.MiscType.CREATURE_SLEEPS_WHEN_BELOW_PERCENT_HEALTH).getValue() >= getHealthPercentage();
    }

    @Override
    public boolean goToSleep() {
        return taskManager.assignSleepTask(this);
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
        if (getGold() > 0 && getOwnerId() >= Player.KEEPER1_ID && isWorker()) {
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
    public void navigateToAssignedTask() {
        Task assignedTask = getAssignedTask();
        if (assignedTask != null) {
            Vector2f loc = assignedTask.getTarget(this);
            if (!isNear(loc)) {
                //workNavigationRequired = false;

                if (loc != null) {
                    Point destination = WorldUtils.vectorToPoint(loc);
                    createNavigation(getCreatureCoordinates(), destination, assignedTask.isFaceTarget() ? assignedTask.getTaskLocation() : null);
                }
            }
        }
    }

    private boolean createNavigation(Point currentLocation, Point destination, Point faceTarget) {
        GraphPath<MapTile> path = navigationService.findPath(currentLocation, destination, this);
        if (path == null) {
            LOGGER.log(Level.WARNING, "No path from {0} to {1}", new Object[]{getCreatureCoordinates(), destination});
            return true;
        }
        entityData.setComponent(entityId, new Navigation(destination, faceTarget, SteeringUtils.pathToList(path)));
        return false;
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
        AttackTarget attackTarget = entityData.getComponent(entityId, AttackTarget.class);
        ICreatureController attackTargetController = null;
        if (attackTarget == null || isIncapacitated(attackTarget.entityId)
                || isFleeing(attackTarget.entityId) || isCaptive(attackTarget.entityId)) {

            // Pick a new target
            // TODO: is there any preference? Now just take the nearest
            // TODO: creatures only now
            EntityId nearestEnemy = null;
            float nearestDistance = Float.MAX_VALUE;
            for (EntityId entity : entityPositionLookup.getSensedEntities(entityId)) {
                if (creaturesController.isValidEntity(entity)
                        && isEnemy(entity)
                        && !(isIncapacitated(entity) || isFleeing(entity) || isCaptive(entity))
                        && (hasPathToEntity(entity) || isWithinAttackDistance(entity))) {
                    float distance = getDistanceToCreature(entity);
                    if (distance < nearestDistance) {
                        nearestDistance = distance;
                        nearestEnemy = entity;
                    }
                }
            }
            if (nearestEnemy != null) {
                attackTargetController = creaturesController.createController(nearestEnemy);
                setAttackTarget(nearestEnemy);
            } else {
                setAttackTarget(null);
            }
        } else {
            attackTargetController = creaturesController.createController(attackTarget.entityId);
        }
        return attackTargetController;
    }

    private boolean hasPathToEntity(EntityId entity) {
        Point ourPos = WorldUtils.vectorToPoint(getPosition());
        Point theirPos = WorldUtils.vectorToPoint(getPosition(entityData, entity));

        return ourPos.equals(theirPos) || navigationService.findPath(ourPos, theirPos, this) != null;
    }

    private void setAttackTarget(EntityId entity) {
        if (entity == null) {
            entityData.removeComponent(entityId, AttackTarget.class);
        } else {
            entityData.setComponent(entityId, new AttackTarget(entity));
        }
    }

    @Override
    public boolean isWithinAttackDistance(EntityId attackTarget) {
        float distanceNeeded = entityData.getComponent(entityId, CreatureMeleeAttack.class).range; // The melee range, the shortest range
        if (creature.getFightStyle() == Creature.FightStyle.SUPPORT) {

            // TODO: Creature spells
            // Get max distance we can cast all spells, and hopefully stay safe
            Float shortestDistance = null;
//            for (CreatureAttack attack : attacks) {
//                if (!attack.isMelee() && attack.isAvailable() && attack.isAttacking()) {
//                    if (shortestDistance == null) {
//                        shortestDistance = attack.getRange();
//                    } else {
//                        shortestDistance = Math.min(shortestDistance, attack.getRange());
//                    }
//                }
//            }
            if (shortestDistance != null) {
                distanceNeeded = shortestDistance;
            }
        }

        // TODO: currently we move only with a tile precision, so accept attack if on the same tile already
        return distanceNeeded >= getDistanceToCreature(attackTarget) || (isStopped() && isAtSameTile(attackTarget));
    }

    private boolean isAtSameTile(EntityId attackTarget) {
        return getCreatureCoordinates().equals(getCreatureCoordinates(entityData, attackTarget));
    }

    @Override
    public void stop() {
        // Note that this is the updatable stop, not the creature stop...
    }

    @Override
    public void executeAttack(EntityId attackTarget) {

        // Now just the melee attack
        // TODO: spells
        // TODO: how to apply the damage? Create a component for THIS creature that adds the damage to enemy after the countdown is finished?
        CreatureMeleeAttack creatureMeleeAttack = entityData.getComponent(entityId, CreatureMeleeAttack.class);
        if (isAttackRecharged(creatureMeleeAttack)) {
            entityData.setComponent(entityId, new CreatureMeleeAttack(creatureMeleeAttack, gameTimer.getGameTime()));
            stateMachine.changeState(CreatureState.MELEE_ATTACK);

            // TODO: now, instant action, substract the health
            Health enemyHealth = entityData.getComponent(attackTarget, Health.class);
            entityData.setComponent(attackTarget, new Health(enemyHealth.ownLandHealthIncrease, enemyHealth.health - creatureMeleeAttack.damage, enemyHealth.maxHealth, enemyHealth.unconscious));
        }
    }

    private boolean isAttackRecharged(Attack attack) {
        return attack.attactStartTime == null || attack.attactStartTime + attack.rechargeTime <= gameTimer.getGameTime();
    }

    @Override
    public void navigateToAttackTarget(EntityId attackTarget) {
        Vector3f targetPosition = getPosition(entityData, attackTarget);
        if (targetPosition != null) {

            // Check if we are already going there
            Point destination = WorldUtils.vectorToPoint(targetPosition);
            Navigation currentNavigation = entityData.getComponent(entityId, Navigation.class);
            if (currentNavigation != null && currentNavigation.target.equals(destination)) {
                return;
            }

            // Just now simply go where the target currently is
            Point ourPosition = getCreatureCoordinates();
            if (destination != null && !destination.equals(ourPosition)) {
                createNavigation(ourPosition, destination, null);
            }
        }
    }

    @Override
    public ICreatureController getFollowTarget() {
        FollowTarget followTarget = entityData.getComponent(entityId, FollowTarget.class);
        if (followTarget != null && entityData.getComponent(followTarget.entityId, Position.class) != null) {
            return creaturesController.createController(followTarget.entityId);
        }
        return null;
    }

    @Override
    public boolean shouldNavigateToFollowTarget() {
        return (isStopped() && getDistanceToCreature(getFollowTarget().getEntityId()) > 1.5f) || (getDistanceToCreature(getFollowTarget().getEntityId()) > 2.5f);
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
    public float getDistanceToCreature(EntityId target) {

        // FIXME: now just direct distance, should be perhaps real distance that the creature needs to traverse to reach the target
        Position targetPosition = entityData.getComponent(target, Position.class);
        Vector3f ourPosition = getPosition();
        if (targetPosition == null || ourPosition == null) {
            return Float.MAX_VALUE;
        }
        return ourPosition.distance(targetPosition.position);
    }

    @Override
    public void navigateToRandomPointAroundTarget(EntityId target, int radius) {
        Position targetPosition = entityData.getComponent(target, Position.class);
        if (targetPosition != null) {

            // To keep up with the target, see if it has a target it is navigating to
            Point destination;
            Navigation targetNavigation = entityData.getComponent(target, Navigation.class);
            if (targetNavigation != null) {
                destination = targetNavigation.target;
            } else {
                destination = WorldUtils.vectorToPoint(targetPosition.position);
            }

            Point p = navigationService.findRandomAccessibleTile(destination, radius, this);
            Point ourPosition = getCreatureCoordinates();
            if (p != null && p != ourPosition) {
                createNavigation(ourPosition, p, null);
            }
        }
    }

    @Override
    public void setFollowTarget(EntityId target) {
        entityData.setComponent(entityId, new FollowTarget(target));
    }

    @Override
    public void resetFollowTarget() {
        entityData.removeComponent(entityId, FollowTarget.class);
    }

    @Override
    public void flee() {
        //PrioritySteering<Vector2> prioritySteering = new PrioritySteering(this, 0.0001f);

        // Get the nearest enemy
        // FIXME: method naming if truly nearest enemy, and perhaps we should flee from our assailant
//        CreatureControl target = getAttackTarget();
//
//        // Flee from the enemy
//        if (target != null) {
//            Flee<Vector2> flee = new Flee<>(this, target);
//            prioritySteering.add(flee);
//        }
        // FIXME: For now just flee towards the dungeon heart or random tiles
        Keeper keeper = levelInfo.getPlayer(getOwnerId());
        if (keeper != null && keeper.getDungeonHeartLocation() != null) {
            Point p = keeper.getDungeonHeartLocation();
            createNavigation(getCreatureCoordinates(), new Point(p.x - 2, p.y - 2), null);
        } else {
            navigateToRandomPoint();
        }

        // Try to find our dungeon heart etc. safety haven
        //setSteeringBehavior(prioritySteering);
    }

    @Override
    public boolean isAttacked() {
        // TODO:
        return false;
    }

    @Override
    public boolean isEnoughSleep() {
        double timeSpent = gameTimer.getGameTime() - entityData.getComponent(entityId, CreatureAi.class).stateStartTime;
        if (timeSpent >= creature.getAttributes().getTimeSleep()) {

            // Hmm, I don't know if this is the right place to do this, but works for now
            CreatureSleep creatureSleep = entityData.getComponent(entityId, CreatureSleep.class);
            entityData.setComponent(entityId, new CreatureSleep(creatureSleep.lairObjectId, gameTimer.getGameTime(), creatureSleep.sleepStartTime));
            return true;
        }
        return false;
    }

    @Override
    public boolean isIncapacitated() {
        return isIncapacitated(entityData, entityId);
    }

    private boolean isIncapacitated(EntityId entityId) {
        return isIncapacitated(entityData, entityId);
    }

    private static boolean isIncapacitated(EntityData entityData, EntityId entityId) {
        Health health = entityData.getComponent(entityId, Health.class);
        if (health == null || health.unconscious) {
            return true;
        }

        CreatureRecuperating recuperating = entityData.getComponent(entityId, CreatureRecuperating.class);
        if (recuperating != null) {
            return true;
        }

        CreatureFall creatureFall = entityData.getComponent(entityId, CreatureFall.class);
        if (creatureFall != null) {
            return true;
        }

        InHand inHand = entityData.getComponent(entityId, InHand.class);
        if (inHand != null) {
            return true;
        }

        return false;
    }

    private void initState() {
        stateMachine.changeState(entityData.getComponent(entityId, CreatureAi.class).getCreatureState());
    }

    @Override
    public boolean canFly() {
        Mobile mobile = entityData.getComponent(entityId, Mobile.class);
        return mobile.canFly;
    }

    @Override
    public boolean canWalkOnWater() {
        Mobile mobile = entityData.getComponent(entityId, Mobile.class);
        if (!mobile.canWalkOnWater) {

            // We need anyway to get out from water if we are tossed in such
            Vector3f pos = getPosition();
            if (pos != null) {
                Point p = WorldUtils.vectorToPoint(pos);
                return mapController.isWater(p.x, p.y);
            }
        }
        return mobile.canWalkOnWater;
    }

    @Override
    public boolean canWalkOnLava() {
        Mobile mobile = entityData.getComponent(entityId, Mobile.class);
        if (!mobile.canWalkOnLava) {

            // We need anyway to get out from lava if we are tossed in such
            Vector3f pos = getPosition();
            if (pos != null) {
                Point p = WorldUtils.vectorToPoint(pos);
                return mapController.isLava(p.x, p.y);
            }
        }
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

        /**
         * Hmm, I'm not sure how to do this, this is not ideal either, how to
         * control the state machine outside the controller. Should it be
         * allowed and should we just check that the current state matches the
         * state in the entity component
         */
        //CreatureAi creatureAi = entityData.getComponent(entityId, CreatureAi.class);
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

        // Also change our state component
        CreatureAi creatureAi = entityData.getComponent(entityId, CreatureAi.class);
        if (creatureAi == null || stateMachine.getCurrentState() != creatureAi.getCreatureState()) {
            entityData.setComponent(entityId, new CreatureAi(gameTimer.getGameTime(), stateMachine.getCurrentState(), creature.getId()));
        }
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
        CreatureSleep creatureSleep = entityData.getComponent(entityId, CreatureSleep.class);
        if (creatureSleep != null && creatureSleep.lairObjectId != null) {
            Position position = entityData.getComponent(creatureSleep.lairObjectId, Position.class);
            if (position != null) {
                return WorldUtils.vectorToPoint(position.position);
            }
        }
        return null;
    }

    @Override
    public boolean isDragged() {
        return entityData.getComponent(entityId, HauledBy.class) != null;
    }

    @Override
    public boolean isUnconscious() {
        Health health = entityData.getComponent(entityId, Health.class);
        if (health != null) {
            return health.unconscious;
        }
        return false;
    }

    @Override
    public Point getCreatureCoordinates() {
        return getCreatureCoordinates(entityData, entityId);
    }

    private static Point getCreatureCoordinates(EntityData entityData, EntityId entity) {
        return WorldUtils.vectorToPoint(getPosition(entityData, entity));
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
        entityData.removeComponent(entityId, Navigation.class);
    }

    @Override
    public int getObjectiveTargetActionPointId() {
        Objective creatureObjective = entityData.getComponent(entityId, Objective.class);
        if (creatureObjective != null) {
            return creatureObjective.actionPointId;
        }
        return 0;
    }

    @Override
    public void setObjectiveTargetActionPointId(int actionPointId) {
        Objective creatureObjective = entityData.getComponent(entityId, Objective.class);
        entityData.setComponent(entityId, new Objective((creatureObjective != null ? creatureObjective.objective : null), (creatureObjective != null ? creatureObjective.objectiveTargetPlayerId : 0), actionPointId));
    }

    @Override
    public Thing.HeroParty.Objective getObjective() {
        Objective creatureObjective = entityData.getComponent(entityId, Objective.class);
        if (creatureObjective != null) {
            return creatureObjective.objective;
        }
        return null;
    }

    @Override
    public void setObjective(Thing.HeroParty.Objective objective) {
        Objective creatureObjective = entityData.getComponent(entityId, Objective.class);
        entityData.setComponent(entityId, new Objective(objective, (creatureObjective != null ? creatureObjective.objectiveTargetPlayerId : 0), (creatureObjective != null ? creatureObjective.actionPointId : 0)));
    }

    @Override
    public boolean isDead() {
        Health health = entityData.getComponent(entityId, Health.class);
        return health == null;
    }

    @Override
    public boolean isImprisoned() {
        return isImprisoned(entityData, entityId);
    }

    @Override
    public boolean isTortured() {
        return isTortured(entityData, entityId);
    }

    private static boolean isImprisoned(EntityData entityData, EntityId entity) {
        CreatureImprisoned imprisoned = entityData.getComponent(entity, CreatureImprisoned.class);
        return imprisoned != null;
    }

    private static boolean isTortured(EntityData entityData, EntityId entity) {
        CreatureTortured tortured = entityData.getComponent(entity, CreatureTortured.class);
        return tortured != null;
    }

    @Override
    public boolean isStunned() {
        return stateMachine.isInState(CreatureState.STUNNED);
    }

    @Override
    public int getLevel() {
        CreatureExperience creatureExperience = entityData.getComponent(entityId, CreatureExperience.class);
        return creatureExperience.level;
    }

    @Override
    public boolean isSlapped() {
        Slapped creatureSlapped = entityData.getComponent(entityId, Slapped.class);
        return creatureSlapped != null;
    }

    @Override
    public boolean isPortalGemInPosession() {
        PortalGem portalGem = entityData.getComponent(entityId, PortalGem.class);
        return portalGem != null;
    }
    @Override
    public void attachPortalGem() {
        entityData.setComponent(entityId, new PortalGem());
    }

    @Override
    public void setObjectiveTargetPlayerId(short playerId) {
        Objective creatureObjective = entityData.getComponent(entityId, Objective.class);
        entityData.setComponent(entityId, new Objective((creatureObjective != null ? creatureObjective.objective : null), playerId, (creatureObjective != null ? creatureObjective.actionPointId : 0)));
    }

    @Override
    public short getObjectiveTargetPlayerId() {
        Objective creatureObjective = entityData.getComponent(entityId, Objective.class);
        if (creatureObjective != null) {
            return creatureObjective.objectiveTargetPlayerId;
        }

        return -1;
    }

    @Override
    public void setPlayerObjective(ObjectiveType objective) {
        if (objective == null) {
            entityData.removeComponent(entityId, PlayerObjective.class);
        } else {
            entityData.setComponent(entityId, new PlayerObjective(objective));
        }
    }

    @Override
    public void setCreatureLair(EntityId lairId) {
        CreatureSleep creatureSleep = entityData.getComponent(entityId, CreatureSleep.class);
        entityData.setComponent(entityId, new CreatureSleep(lairId, creatureSleep.lastSleepTime, creatureSleep.sleepStartTime));
    }

    @Override
    public void sleep() {
        entityData.setComponent(entityId, new CreatureRecuperating(gameTimer.getGameTime(), gameTimer.getGameTime()));
        if (isNeedForRecuperating()) {
            // entityData.setComponent(entityId, new CreatureAi(gameTimer.getGameTime(), CreatureState.RECUPERATING, creature.getCreatureId()));
            stateMachine.changeState(CreatureState.RECUPERATING);
        } else {
            CreatureSleep creatureSleep = entityData.getComponent(entityId, CreatureSleep.class);
            entityData.setComponent(entityId, new CreatureSleep(creatureSleep.lairObjectId, creatureSleep.lastSleepTime, gameTimer.getGameTime()));
            // entityData.setComponent(entityId, new CreatureAi(gameTimer.getGameTime(), CreatureState.SLEEPING, creature.getCreatureId()));
            stateMachine.changeState(CreatureState.SLEEPING);
        }
    }

    @Override
    public void setHaulable(ICreatureController creature) {
        if (creature != null) {
            entityData.setComponent(entityId, new HauledBy(creature.getEntityId()));
        } else {
            entityData.removeComponent(entityId, HauledBy.class);
        }
    }

    private boolean isAlly(EntityId entity) {
        Owner otherOwner = entityData.getComponent(entity, Owner.class);
        if (otherOwner != null) {
            Keeper keeper = levelInfo.getPlayer(getOwnerId());
            if (keeper != null) {
                return keeper.isAlly(otherOwner.ownerId);
            }
        }

        return false;
    }

    private Creature.FightStyle getEntityFightingStyle(EntityId entity) {
        CreatureComponent otherCreature = entityData.getComponent(entity, CreatureComponent.class);
        if (otherCreature != null) {
            return levelInfo.getLevelData().getCreature(otherCreature.creatureId).getFightStyle();
        }

        return null;
    }

    private boolean isEnemy(EntityId entity) {
        Owner otherOwner = entityData.getComponent(entity, Owner.class);
        if (otherOwner != null) {
            Keeper keeper = levelInfo.getPlayer(getOwnerId());
            if (keeper != null) {
                return keeper.isEnemy(otherOwner.ownerId);
            }
        }

        return false;
    }

    private int getThreat(EntityId entity) {
        Threat threat = entityData.getComponent(entity, Threat.class);
        if (threat != null) {
            return threat.threat;
        }

        return 0;
    }

    private boolean isFleeing(EntityId entity) {
        CreatureAi creatureAi = entityData.getComponent(entity, CreatureAi.class);
        if (creatureAi != null) {
            return creatureAi.getCreatureState() == CreatureState.FLEE;
        }

        return false;
    }

    @Override
    public void imprison() {

        // Return health to 20%
        Health health = entityData.getComponent(entityId, Health.class);
        entityData.setComponent(entityId, new Health(health.ownLandHealthIncrease, (int) Math.floor(health.maxHealth * 0.2f), health.maxHealth, false));
        entityData.setComponent(entityId, new CreatureImprisoned(gameTimer.getGameTime(), gameTimer.getGameTime()));
        entityData.setComponent(entityId, new RoomStorage(AbstractRoomController.ObjectType.PRISONER));
        stateMachine.changeState(CreatureState.IMPRISONED);
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
            case MELEE_ATTACK: {
                return timeSpent >= getAnimationTime(creature, Creature.AnimationType.MELEE_ATTACK);
            }
            case EATING: {
                return timeSpent >= getAnimationTime(creature, Creature.AnimationType.EATING);
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
    public boolean isClaimed() {
        return Player.NEUTRAL_PLAYER_ID != getOwnerId();
    }

    @Override
    public boolean isHungry() {
        CreatureHunger creatureHunger = entityData.getComponent(entityId, CreatureHunger.class);
        if (creatureHunger != null) {
            if (creatureHunger.amountNeeded > 0) {
                return true;
            } else if (gameTimer.getGameTime() - creatureHunger.lastEatTime >= creature.getAttributes().getHungerRate()) {

                // We are hungry now, mark the amount of food we need
                CreatureComponent creatureComponent = entityData.getComponent(entityId, CreatureComponent.class);
                entityData.setComponent(entityId, new CreatureHunger(creatureHunger.lastEatTime, creatureComponent.hungerFill));
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean goToEat() {
        return taskManager.assignEatTask(this);
    }

    @Override
    public void eat(IEntityController target) {

        // Ok, first we destroy the entity we are eating so that nobody else gets it
        Health health = entityData.getComponent(target.getEntityId(), Health.class);
        if (health != null) {
            target.remove();

            stateMachine.changeState(CreatureState.EATING);
        }
    }

    @Override
    public void sate() {
        CreatureHunger creatureHunger = entityData.getComponent(entityId, CreatureHunger.class);
        if (creatureHunger.amountNeeded == 1) {

            // We are full
            entityData.setComponent(entityId, new CreatureHunger(gameTimer.getGameTime(), 0));
        } else if (creatureHunger.amountNeeded > 0) {
            entityData.setComponent(entityId, new CreatureHunger(creatureHunger.lastEatTime, creatureHunger.amountNeeded - 1));
        }

        // Increase health
        if (!isFullHealth()) {
            Health health = entityData.getComponent(entityId, Health.class);
            entityData.setComponent(entityId, new Health(health.ownLandHealthIncrease, Math.max(health.health + creature.getAttributes().getHpFromChicken(), health.maxHealth), health.maxHealth, health.unconscious));
        }
    }

    @Override
    public int getResearchPerSecond() {
        CreatureComponent creatureComponent = entityData.getComponent(entityId, CreatureComponent.class);
        return creatureComponent.researchPerSecond;
    }

}
