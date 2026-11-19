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
import com.badlogic.gdx.math.Vector2;
import com.jme3.math.FastMath;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.filter.FieldFilter;
import toniarts.openkeeper.game.component.Armour;
import toniarts.openkeeper.game.component.Attack;
import toniarts.openkeeper.game.component.AttackTarget;
import toniarts.openkeeper.game.component.BackOff;
import toniarts.openkeeper.game.component.CallToArms;
import toniarts.openkeeper.game.component.CasinoBigWinner;
import toniarts.openkeeper.game.component.CasinoDisco;
import toniarts.openkeeper.game.component.CastShot;
import toniarts.openkeeper.game.component.CombatPitFighter;
import toniarts.openkeeper.game.component.CreatureAi;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.CreatureExperience;
import toniarts.openkeeper.game.component.CreatureFall;
import toniarts.openkeeper.game.component.CreatureHunger;
import toniarts.openkeeper.game.component.CreatureImprisoned;
import toniarts.openkeeper.game.component.CreatureMeleeAttack;
import toniarts.openkeeper.game.component.CreatureMood;
import toniarts.openkeeper.game.component.CreaturePay;
import toniarts.openkeeper.game.component.CreatureRecuperating;
import toniarts.openkeeper.game.component.CreatureSleep;
import toniarts.openkeeper.game.component.CreatureSpell;
import toniarts.openkeeper.game.component.CreatureSpells;
import toniarts.openkeeper.game.component.CreatureTortured;
import toniarts.openkeeper.game.component.CreatureViewState;
import toniarts.openkeeper.game.component.Disguised;
import toniarts.openkeeper.game.component.Fearless;
import toniarts.openkeeper.game.component.FollowTarget;
import toniarts.openkeeper.game.component.Frightened;
import toniarts.openkeeper.game.component.Gold;
import toniarts.openkeeper.game.component.Health;
import toniarts.openkeeper.game.component.Hypnotised;
import toniarts.openkeeper.game.component.InHand;
import toniarts.openkeeper.game.component.Invisible;
import toniarts.openkeeper.game.component.Invulnerable;
import toniarts.openkeeper.game.component.Mana;
import toniarts.openkeeper.game.component.Mobile;
import toniarts.openkeeper.game.component.Navigation;
import toniarts.openkeeper.game.component.Objective;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Party;
import toniarts.openkeeper.game.component.Peaceful;
import toniarts.openkeeper.game.component.PlayerObjective;
import toniarts.openkeeper.game.component.PortalGem;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.component.Possessed;
import toniarts.openkeeper.game.component.PossessedAbility;
import toniarts.openkeeper.game.component.PossessedMovement;
import toniarts.openkeeper.game.component.ReturnToHeroLair;
import toniarts.openkeeper.game.component.Senses;
import toniarts.openkeeper.game.component.Slapped;
import toniarts.openkeeper.game.component.SpeedBlur;
import toniarts.openkeeper.game.component.SpeedChange;
import toniarts.openkeeper.game.component.Stored;
import toniarts.openkeeper.game.component.TaskComponent;
import toniarts.openkeeper.game.component.Teleport;
import toniarts.openkeeper.game.component.Threat;
import toniarts.openkeeper.game.component.Unconscious;
import toniarts.openkeeper.game.controller.GameWorldController;
import toniarts.openkeeper.game.controller.ICreaturesController;
import toniarts.openkeeper.game.controller.IGameTimer;
import toniarts.openkeeper.game.controller.ILevelInfo;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.IObjectsController;
import toniarts.openkeeper.game.controller.IShotsController;
import toniarts.openkeeper.game.controller.entity.EntityController;
import toniarts.openkeeper.game.controller.entity.IEntityController;
import toniarts.openkeeper.game.controller.object.IObjectController;
import toniarts.openkeeper.game.controller.room.AbstractRoomController;
import toniarts.openkeeper.game.controller.room.CombatPitController;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.game.data.ActionPoint;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.data.ObjectiveType;
import toniarts.openkeeper.game.event.GameEvent.ThingFlag;
import toniarts.openkeeper.game.event.GameEventService;
import toniarts.openkeeper.game.event.GameEventType;
import toniarts.openkeeper.game.logic.IEntityPositionLookup;
import toniarts.openkeeper.game.map.IMapTileInformation;
import toniarts.openkeeper.game.navigation.INavigationService;
import toniarts.openkeeper.game.navigation.steering.SteeringUtils;
import toniarts.openkeeper.game.task.ITaskManager;
import toniarts.openkeeper.game.task.Task;
import toniarts.openkeeper.game.task.TaskType;
import toniarts.openkeeper.tools.convert.map.ArtResource;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.CreatureSpell.CreatureSpellFlag;
import toniarts.openkeeper.tools.convert.map.KeeperSpell;
import toniarts.openkeeper.tools.convert.map.Player;
import toniarts.openkeeper.tools.convert.map.Shot;
import toniarts.openkeeper.tools.convert.map.Terrain;
import toniarts.openkeeper.tools.convert.map.Thing;
import toniarts.openkeeper.tools.convert.map.Variable;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.Utils;
import toniarts.openkeeper.utils.WorldUtils;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

/**
 * Controls an entity with {@link CreatureAi} component. Basically supports the
 * AI state machine.
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
// Controllers inherit entity-ID equality and hashing; mutable sleep state is not identity.
// skipcq: JAVA-W0100
public final class CreatureController extends EntityController implements ICreatureController {

    /**
     * Fight turns in a row with the threat over our fear that scare us
     */
    private static final int SCARED_OVER_THREAT_CHECKS = 3;
    /**
     * Within this many tiles of our own Dungeon Heart, the threat to us is
     * taken lighter the nearer we are
     */
    private static final int HEART_THREAT_DISTANCE = 10;
    /**
     * The least distance, in tiles, a creature backs off from its target
     */
    private static final float BACK_OFF_RANGE = 2f;

    private static final Logger logger = System.getLogger(CreatureController.class.getName());

    /**
     * How far from the threat, in tiles, a fleeing creature aims. The distance
     * DKII.EXE adds to the effect radius is 2, the radius is guessed at 5
     */
    private static final int FLEE_DISTANCE = 7;
    /**
     * How far, in tiles, a recuperating hero wanders on its own land (DK II
     * 0x461B50 asks 0x461BB0 for a tile this far)
     */
    private static final int OWN_LAND_WANDER_DISTANCE = 10;

    /**
     * Possessed movement speed relative to walking forward
     */
    private static final float POSSESSED_BACKWARD_SPEED_FACTOR = 0.5f;
    private static final float POSSESSED_STRAFE_SPEED_FACTOR = 0.667f;
    private static final float SPEED_MULTIPLIER = 2f;
    /**
     * How long the big winner of the Casino celebrates its jackpot, as in DK II (DKII.EXE 0x456EC0)
     */
    private static final float BIG_WINNER_CELEBRATION_SECONDS = 10f;
    private static final int EAT_ATTEMPT_INTERVAL_TICKS = 4;
    /**
     * How long an idle hero does nothing before it tries its objective again, as in DK II (DKII.EXE 0x477850)
     */
    private static final int HERO_IDLE_TICKS = 12;

    /**
     * An Imp teleports to a job more than this many tiles away on either axis
     */
    private static final int IMP_TELEPORT_DISTANCE = 2;
    private static final int IMP_TELEPORT_OUT_EFFECT = 321;
    private static final int IMP_TELEPORT_IN_EFFECT = 322;
    private static final int TELEPORT_OUT_TICKS = 4;
    private static final int TELEPORT_IN_TICKS = 2;
    /**
     * The jobs that take something somewhere, an Imp walks these (0x4651A0)
     */
    private static final Set<TaskType> CARRY_TASKS = EnumSet.of(TaskType.CARRY_CREATURE_TO_LAIR, TaskType.CARRY_CREATURE_TO_JAIL,
            TaskType.CARRY_OBJECT_TO_STORAGE, TaskType.CARRY_CORPSE_TO_GRAVEYARD, TaskType.DELIVER_TRAP_CRATE, TaskType.DELIVER_DOOR_CRATE);

    /**
     * The possessed melee attack hits what is at most 341/2048 of a full
     * circle (about 60 degrees) off the creature's facing, as the cosine of
     * that angle
     */
    private static final float POSSESSED_ATTACK_MIN_FACING = FastMath.cos(341 / 2048f * FastMath.TWO_PI);

    /**
     * The states the Chicken spell interrupts, the creature is going about its own business in these
     */
    private static final Set<CreatureState> CHICKEN_INTERRUPTIBLE_STATES = EnumSet.of(CreatureState.IDLE,
            CreatureState.WANDER, CreatureState.WORK, CreatureState.FIGHT, CreatureState.FOLLOW, CreatureState.FLEE,
            CreatureState.SLEEPING, CreatureState.MELEE_ATTACK, CreatureState.CAST_SPELL, CreatureState.EATING,
            CreatureState.CHICKEN);

    /**
     * The states a meal interrupts. Like in DK II, a creature busy with
     * something else, say fighting, sleeping or held in prison, gets the food
     * of a chicken it is given without stopping to eat it
     */
    private static final Set<CreatureState> EAT_INTERRUPTIBLE_STATES = EnumSet.of(CreatureState.IDLE,
            CreatureState.WANDER, CreatureState.WORK, CreatureState.DANCING);

    /**
     * A possessed creature's spell aims at creatures closer than this, 91
     * square tiles (0x5B000 in DK II's fixed point)
     */
    private static final float POSSESSED_SPELL_REACH_SQUARED = 91;

    /**
     * A possessed Bile Demon picks up a worker closer than a tile (0x516EC0)
     * and holds it half a tile in front of it (0x4831D0). It throws the worker
     * forward two tiles a turn and up a quarter of a tile a turn (0x4831D0).
     */
    private static final float POSSESSED_PICK_UP_REACH = WorldUtils.TILE_WIDTH;
    private static final float POSSESSED_CARRY_DISTANCE = WorldUtils.TILE_WIDTH / 2;
    private static final float THROWN_WORKER_SPEED = 2 * WorldUtils.TILE_WIDTH;
    private static final float THROWN_WORKER_LIFT = WorldUtils.TILE_WIDTH / 4;

    /**
     * A thrown or dropped worker takes this much damage as it lands (0x4833C0)
     */
    private static final int THROWN_WORKER_DAMAGE = 50;

    /**
     * The possessed Maiden's fear attack frightens its target for 20 game
     * ticks with this severity (0x40FA90, 0x514470)
     */
    private static final int FEAR_ATTACK_TICKS = 20;
    private static final int FEAR_ATTACK_SEVERITY = 2000;

    /**
     * A creature joins the group of the possessed creature from closer than
     * this, in tiles
     */
    private static final int POSSESSED_GROUP_RANGE = 7;

    private final INavigationService navigationService;
    private final ITaskManager taskManager;
    private final IGameTimer gameTimer;
    private final Map<Variable.MiscVariable.MiscType, Variable.MiscVariable> gameSettings;
    private final ICreaturesController creaturesController;
    private final IEntityPositionLookup entityPositionLookup;
    private final ILevelInfo levelInfo;
    private final IShotsController shotsController;
    private final GameEventService gameEventService;
    // TODO: All the data is not supposed to be on entities as they become too big, but I don't want these here either
    private final Creature creature;
    private final StateMachine<ICreatureController, CreatureState> stateMachine;
    private float motionless = 0;

    public CreatureController(EntityId entityId, EntityData entityData, Creature creature, INavigationService navigationService,
            ITaskManager taskManager, IGameTimer gameTimer, Map<Variable.MiscVariable.MiscType, Variable.MiscVariable> gameSettings,
            ICreaturesController creaturesController, IEntityPositionLookup entityPositionLookup, IMapController mapController,
            ILevelInfo levelInfo, IObjectsController objectsController, IShotsController shotsController, GameEventService gameEventService) {
        super(entityId, entityData, objectsController, mapController);
        this.navigationService = navigationService;
        this.taskManager = taskManager;
        this.creature = creature;
        this.gameTimer = gameTimer;
        this.gameSettings = Collections.unmodifiableMap(gameSettings);
        this.creaturesController = creaturesController;
        this.entityPositionLookup = entityPositionLookup;
        this.levelInfo = levelInfo;
        this.shotsController = shotsController;
        this.gameEventService = gameEventService;
        this.stateMachine = new DefaultStateMachine<>(this) {
            @Override
            public void changeState(CreatureState newState) {
                CreatureAi ai = entityData.getComponent(entityId, CreatureAi.class);
                CreatureState target = ai == null ? null : ai.getTargetState();
                if (newState == CreatureState.IDLE && target != null) {
                    newState = target;
                }
                if (newState != null && newState == target) {
                    // Consume before enter(): a failed target may immediately fall back to IDLE.
                    entityData.setComponent(entityId, new CreatureAi(ai.stateStartTime,
                            ai.getCreatureState(), ai.creatureId));
                }
                super.changeState(newState);
            }
        };
    }

    @Override
    public void completeDungeonEntrance() {
        Position position = entityData.getComponent(entityId, Position.class);
        var offset = creature.getAnimationOffsets(Creature.OffsetType.PORTAL_ENTRANCE);
        if (position == null || offset == null) {
            return;
        }
        // DKII 0x459c91..0x459d2d adds offset slot 0 in world XY (no rotation),
        // then replaces Z with floor height before switching to idle.
        entityData.setComponent(entityId, new Position(position.rotation,
                new Vector3f(position.position.x + offset.x, WorldUtils.FLOOR_HEIGHT,
                        position.position.z + offset.y)));
    }

    @Override
    public boolean shouldFleeOrAttack() {
        checkSurroundings();

        // Check fleeing, TODO: Always flee?
        boolean inDHeart = false;
        if (entityData.getComponent(entityId, Fearless.class) == null && !isCombatPitFighter()
                && (isThreatTooHigh() || isFrightened(entityData.getComponent(entityId, CreatureComponent.class).fear))) {

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

        // Should we attack, try to avoid i.e. imps engaging in a fight
        if ((creature.getFightStyle()
                != Creature.FightStyle.NON_FIGHTER || inDHeart) && getAttackTarget() != null
                && (stateMachine.isInState(CreatureState.FIGHT) || isNearCallToArms())) {
            if (!stateMachine.isInState(CreatureState.FIGHT)) {
                stateMachine.changeState(CreatureState.FIGHT);
            }
            return true;
        }

        return false;
    }

    /**
     * Tells whether the threat to us, less the threat we cause, is over our
     * fear while no fellow fighters are with us. A creature that always flees
     * doesn't count on fellow fighters
     *
     * @return true if we should flee
     */
    private boolean isThreatTooHigh() {
        Threat threatComponent = entityData.getComponent(entityId, Threat.class);
        int threat = threatComponent != null ? threatComponent.threat : 0;
        int fear = entityData.getComponent(entityId, CreatureComponent.class).fear;
        int threatToUs = getEnemyThreat();
        int threatCaused = creature.getFlags().contains(Creature.CreatureFlag.ALWAYS_FLEE)
                || isHealthAtCriticalLevel() ? threat : getOurThreat();
        return threatToUs - threatCaused > fear && (getFellowFighters() == 0
                || creature.getFlags().contains(Creature.CreatureFlag.ALWAYS_FLEE));
    }

    @Override
    public void checkSurroundings() {

        // Scan for neutral creatures to claim
        short ownerId = getOwnerId();
        if (ownerId == Player.NEUTRAL_PLAYER_ID || ownerId == Player.GOOD_PLAYER_ID) {
            return;
        }

        for (EntityId entity : entityPositionLookup.getSensedEntities(entityId)) {
            Owner owner = entityData.getComponent(entity, Owner.class);

            // We sense through doors, but the creature behind an enemy or a locked door
            // is not ours before we get there
            if (owner != null && owner.ownerId == Player.NEUTRAL_PLAYER_ID
                    && !entityPositionLookup.isBehindBlockingDoor(entityId, entity)) {
                convertCreature(entity, ownerId);
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
            if (isEnemy(entity) && isThreat(entity) && ThreatSource.isThreateningUs(entityData, entity, entityId)) {
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
        return !isIncapacitated(entity) && !isCaptive(entity) && !isChicken(entity);
    }

    /**
     * Checks if the given entity is a captive. Unable to join a fight and also
     * posses no threat to anyone
     *
     * @param entity the entity
     * @return is entity a captive
     */
    private boolean isCaptive(EntityId entity) {
        // Fighting in the Combat Pit is kind of captivity too
        return isImprisoned(entityData, entity) || isTortured(entityData, entity)
                || entityData.getComponent(entity, CombatPitFighter.class) != null;
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

    /**
     * Has a possessed Maiden frightened us more than we can take. DK II adds
     * the severity to our fear on each tick of the fear (0x488930), the fear
     * is gone when its time is up.
     *
     * @param fear how much fear we can take
     * @return true if we are frightened and should run away
     */
    private boolean isFrightened(int fear) {
        Frightened frightened = entityData.getComponent(entityId, Frightened.class);
        if (frightened != null && frightened.endTime <= gameTimer.getGameTime()) {
            entityData.removeComponent(entityId, Frightened.class);
            return false;
        }
        return frightened != null && frightened.severity > fear;
    }

    @Override
    public void unassingCurrentTask() {
        Task assignedTask = getAssignedTask();
        if (assignedTask != null) {
            assignedTask.unassign(this);
            entityData.removeComponent(entityId, TaskComponent.class);
        }
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
    public void navigateToRandomPointInCombatPit() {
        CombatPitFighter pitFighter = entityData.getComponent(entityId, CombatPitFighter.class);
        if (pitFighter != null && mapController.getRoomController(pitFighter.pit) instanceof CombatPitController pit) {
            createNavigation(getCreatureCoordinates(), pit.getRandomFightingTile(), null);
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
        // Creatures without a lair object (the princes, King Reginald...) never make a home
        return getOwnerId() >= Player.KEEPER1_ID && creature.getLairObjectId() != 0
                && entityData.getComponent(entityId, CreatureSleep.class) != null;
    }

    @Override
    public boolean hasLair() {
        CreatureSleep creatureSleep = entityData.getComponent(entityId, CreatureSleep.class);
        return creatureSleep != null && creatureSleep.lairObjectId != null && entityData.getComponent(creatureSleep.lairObjectId, Position.class) != null;
    }

    @Override
    public boolean findLair() {
        return taskManager.assignClosestRoomTask(this, AbstractRoomController.ObjectType.LAIR, null);
    }

    @Override
    public boolean isNeedForSleep() {
        CreatureSleep creatureSleep = entityData.getComponent(entityId, CreatureSleep.class);
        // CREATURE_SLEEPS_WHEN_BELOW_PERCENT_HEALTH triggers the native attempt path,
        // but its assigned-bed gate (0x46f47b) still requires positive debt. The
        // 250/1000 HP, zero-debt probe stayed awake; manual Lair placement bypasses it.
        return needsLair() && isNeedForSleep(creatureSleep,
                creature.getAttributes().getTimeSleep());
    }

    static boolean isNeedForSleep(CreatureSleep sleep, int timeSleep) {
        return sleep != null && timeSleep > 0 && sleep.sleepNeed > 0;
    }

    @Override
    public boolean goToSleepAfterLairDrop() {
        if (shouldFleeOrAttack()) {
            return false;
        }
        if (!needsLair() || creature.getAttributes().getTimeSleep() <= 0 || !hasLair()) {
            return false;
        }
        IRoomController room = mapController.getRoomControllerByCoordinates(getCreatureCoordinates());
        CreatureSleep sleep = entityData.getComponent(entityId, CreatureSleep.class);
        Stored bed = entityData.getComponent(sleep.lairObjectId, Stored.class);
        if (room == null || room.isDestroyed() || room.isRemoved()
                || room.getOwnerId() != getOwnerId()
                || !room.hasObjectControl(AbstractRoomController.ObjectType.LAIR)
                || bed == null || bed.objectType != AbstractRoomController.ObjectType.LAIR
                || !room.getEntityId().equals(bed.room)) {
            return false;
        }
        // Native manual placement bypasses the autonomous debt/retry gates.
        return taskManager.assignSleepTask(this);
    }

    @Override
    public boolean goToSleep() {
        double gameTime = gameTimer.getGameTime();
        CreatureSleep sleep = entityData.getComponent(entityId, CreatureSleep.class);
        if (sleep == null || gameTime - sleep.lastSleepAttemptTime < 1) {
            return false;
        }
        // Native autonomous attempts are throttled to one per four native ticks.
        CreatureSleep updated = new CreatureSleep(sleep);
        updated.lastSleepAttemptTime = gameTime;
        entityData.setComponent(entityId, updated);
        return taskManager.assignSleepTask(this);
    }

    @Override
    public void findWork(Consumer<Boolean> workResult) {

        // See if we have some available work
        if (isWorker()) {
            taskManager.addToUnemployedWorkerQueue(this, workResult);
            return;
        }

        short owner = getOwnerId();
        if (owner == Player.NEUTRAL_PLAYER_ID || owner == Player.GOOD_PLAYER_ID) {

            // Currently no creature specific jobs offered for neutral or good players
            workResult.accept(false);
            return;
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
        if (!jobs.isEmpty() && taskManager.assignTask(this, chooseOnWeight(jobs).getJobType())) {
            workResult.accept(true);
            return;
        }

        // With none of its own jobs to start, the creature trains, like in DK II. The Casino comes after this
        if (!creature.getFlags().contains(Creature.CreatureFlag.NOT_TRAIN_WHEN_IDLE)
                && taskManager.isTaskAvailable(this, Creature.JobType.TRAIN)
                && taskManager.assignTask(this, Creature.JobType.TRAIN)) {
            workResult.accept(true);
            return;
        }

        workResult.accept(false);
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
        TaskComponent taskComponent = entityData.getComponent(entityId, TaskComponent.class);
        if (!taskComponent.taskStarted) {
            entityData.setComponent(entityId, new TaskComponent(taskComponent.taskId, taskComponent.targetEntity, taskComponent.targetLocation, taskComponent.taskType, taskComponent.taskDuration, true));
        }
        if (isAssignedTaskValid()) {
            getAssignedTask().executeTask(this, taskComponent.taskDuration);
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
            castImpHaste();

            Vector2f loc = assignedTask.getTarget(this);
            Vector2f workPosition = assignedTask.getWorkPosition(this);
            if (workPosition != null && workPosition.equals(loc)) {
                workPosition = null;
            }

            // The middle of the target tile is near enough, unless the task wants us somewhere else on the tile
            boolean there = workPosition != null ? isAt(workPosition) : loc != null && isNear(loc);
            if (loc != null && !there) {
                //workNavigationRequired = false;

                Point destination = WorldUtils.vectorToPoint(loc);
                if (!teleport(assignedTask.getTaskType(), destination, workPosition != null ? workPosition : loc)) {
                    createNavigation(getCreatureCoordinates(), destination, assignedTask.isFaceTarget() ? assignedTask.getTaskLocation() : null, workPosition);
                }
            }
        }
    }

    /**
     * An Imp setting off to a job speeds itself up with its own speed spell
     * whenever the spell is recharged, without a cast (0x4651A0). The spell
     * does what its shot does, so a speed change that is still running is left
     * as it is, but the spell recharges all the same. A speed-up shows the
     * speed blur for as many game ticks as the spell's first value, the
     * percentage
     */
    private void castImpHaste() {
        EntityId spellEntity = getRechargedSpell(CreatureSpellFlag.IMP_HASTE);
        if (spellEntity != null) {
            CreatureSpell spell = entityData.getComponent(spellEntity, CreatureSpell.class);
            toniarts.openkeeper.tools.convert.map.CreatureSpell creatureSpell = levelInfo.getLevelData().getCreatureSpellById(spell.creatureSpellId);
            entityData.setComponent(spellEntity, new CreatureSpell(spell, gameTimer.getGameTime()));
            shotsController.createShot(creatureSpell.getShotTypeId(), creatureSpell.getShotData1(), creatureSpell.getShotData2(), getOwnerId(),
                    getPosition(), entityId, entityId);
            if (creatureSpell.getShotData1() > 100) {
                entityData.setComponent(entityId, new SpeedBlur(gameTimer.getGameTime()
                        + creatureSpell.getShotData1() / (double) levelInfo.getLevelData().getGameLevel().getTicksPerSec()));
            }
        }
    }

    /**
     * An Imp with its teleport spell recharged jumps to a job more than 2 tiles
     * away on either axis instead of walking there, if it can walk there. Not
     * when it takes something somewhere, but gold to the Treasury is no such
     * trip (0x4651A0). The spell has no shot and costs no mana
     *
     * @param taskType the job
     * @param destination the tile the Imp would walk to
     * @param target the spot it lands on, the one it would walk to
     * @return true if the Imp teleports
     */
    private boolean teleport(TaskType taskType, Point destination, Vector2f target) {
        Point location = getCreatureCoordinates();
        if (CARRY_TASKS.contains(taskType)
                || (Math.abs(destination.x - location.x) <= IMP_TELEPORT_DISTANCE && Math.abs(destination.y - location.y) <= IMP_TELEPORT_DISTANCE)) {
            return false;
        }
        EntityId spellEntity = getRechargedSpell(CreatureSpellFlag.IMP_TELEPORT);
        if (spellEntity == null || navigationService.findPath(location, destination, this) == null) {
            return false;
        }

        double now = gameTimer.getGameTime();
        Position position = entityData.getComponent(entityId, Position.class);
        entityData.setComponent(spellEntity, new CreatureSpell(entityData.getComponent(spellEntity, CreatureSpell.class), now));
        entityData.removeComponent(entityId, Navigation.class);
        entityData.setComponent(entityId, new Teleport(now, new Vector3f(target.x, position.position.y, target.y)));
        gameEventService.emitEffect(IMP_TELEPORT_OUT_EFFECT, position.position, position.rotation);
        gameEventService.emit(GameEventType.CREATURE_TELEPORT_OUT, ThingFlag.CREATURE, creature.getCreatureId(), position.position);
        return true;
    }

    /**
     * The Imp jumps out, then lands on the job and jumps in. Each half lasts as
     * long as the jump animation, but at least 4 ticks out and 2 ticks in
     * (0x499B80 and 0x499BE0)
     */
    private void continueTeleport(Teleport teleport) {
        double now = gameTimer.getGameTime();
        int ticks = teleport.destination != null ? TELEPORT_OUT_TICKS : TELEPORT_IN_TICKS;
        if (now - teleport.startTime < Math.max(ticks / (double) levelInfo.getLevelData().getGameLevel().getTicksPerSec(),
                getAnimationTime(creature, Creature.AnimationType.SPECIAL_1))) {
            return;
        }
        if (teleport.destination == null) {
            entityData.removeComponent(entityId, Teleport.class);
            return;
        }

        Position position = entityData.getComponent(entityId, Position.class);
        entityData.setComponents(entityId, new Position(position.rotation, teleport.destination), new Teleport(now, null));
        gameEventService.emitEffect(IMP_TELEPORT_IN_EFFECT, teleport.destination, position.rotation);
        gameEventService.emit(GameEventType.CREATURE_TELEPORT_IN, ThingFlag.CREATURE, creature.getCreatureId(), teleport.destination);
    }

    /**
     * @param flag the flag of the spell
     * @return our spell with the flag if it is recharged, null if there is none
     */
    private EntityId getRechargedSpell(CreatureSpellFlag flag) {
        CreatureSpells creatureSpells = entityData.getComponent(entityId, CreatureSpells.class);
        if (creatureSpells != null) {
            for (EntityId spellEntity : creatureSpells.creatureSpells) {
                CreatureSpell spell = entityData.getComponent(spellEntity, CreatureSpell.class);
                if (levelInfo.getLevelData().getCreatureSpellById(spell.creatureSpellId).getFlags().contains(flag) && isAttackRecharged(spell)) {
                    return spellEntity;
                }
            }
        }
        return null;
    }

    private boolean createNavigation(Point currentLocation, Point destination, Point faceTarget) {
        return createNavigation(currentLocation, destination, faceTarget, null);
    }

    private boolean createNavigation(Point currentLocation, Point destination, Point faceTarget, Vector2f workPosition) {
        GraphPath<IMapTileInformation> path = navigationService.findPath(currentLocation, destination, this);
        if (path == null) {
            logger.log(Level.WARNING, "No path from {0} to {1}", getCreatureCoordinates(), destination);
            return true;
        }

        // An enemy door in the way is not walked through, it is broken down
        List<Point> tiles = new ArrayList<>(path.getCount());
        path.forEach(tile -> tiles.add(tile.getLocation()));
        int doorIndex = DoorAttackTarget.getDoorIndex(tiles, this::isDoorToBreak);
        if (doorIndex == DoorAttackTarget.ADJACENT_DOOR_INDEX) {
            attackDoor(getBlockingDoor(tiles.get(doorIndex)));
            return false;
        } else if (doorIndex > DoorAttackTarget.ADJACENT_DOOR_INDEX) {
            path = DoorAttackTarget.stopBefore(path, doorIndex);
            destination = tiles.get(doorIndex - 1);
            faceTarget = tiles.get(doorIndex);
            workPosition = null;
        }

        List<Vector2> navigationPath = SteeringUtils.pathToList(path);
        if (workPosition != null) {
            Vector3f position = getPosition();
            SteeringUtils.pathToPosition(navigationPath, new Vector2(position.x, position.z), new Vector2(workPosition.x, workPosition.y));
        }
        entityData.setComponent(entityId, new Navigation(destination, faceTarget, navigationPath));
        return false;
    }

    @Override
    public boolean attackVisibleTrap() {
        if (!TrapAttackTarget.attacksTraps(creature.getFightStyle(), getOwnerId())) {
            return false;
        }
        EntityId trap = TrapAttackTarget.findNearest(entityPositionLookup.getSensedEntities(entityId), this::isTrapToAttack,
                this::getDistanceToCreature, creature.getAttributes().getDistanceCanSee(), isWorker(),
                target -> isWithinAttackDistance(target) || hasPathToEntity(target));
        if (trap == null) {
            return false;
        }

        if (isWithinAttackDistance(trap)) {
            stopCreature();
            executeAttack(trap);
        } else {
            navigateToAttackTarget(trap);
        }
        return true;
    }

    private boolean isTrapToAttack(EntityId entity) {
        return TrapAttackTarget.isAttackable(entityData, entity) && isEnemy(entity);
    }

    public boolean attackAdjacentDoor() {
        if (!DoorAttackTarget.attacksAdjacentWhenIdle(getOwnerId())) {
            return false;
        }
        Position position = entityData.getComponent(entityId, Position.class);
        Point tile = position != null ? WorldUtils.vectorToPoint(position.position) : null;
        EntityId door = DoorAttackTarget.findAdjacent(tile, this::getBlockingDoor, this::canBreakDoor);
        if (door == null) {
            return false;
        }
        attackDoor(door);
        return true;
    }

    @Override
    public boolean canBreakDoor(EntityId door) {
        // A follower keeps behind its leader, and the leader breaks the door (DKII.EXE 0x462130 walks with
        // the own-doors-only flag, see 0x4C8BD0)
        IPartyController party = getParty();
        if (party != null && !party.isPartyLeader(this)) {
            return false;
        }
        return DoorAttackTarget.canBreak(entityData, door, creature.getFightStyle(), isEnemy(door));
    }

    private boolean isDoorToBreak(Point tile) {
        EntityId door = getBlockingDoor(tile);
        return door != null && canBreakDoor(door);
    }

    private EntityId getBlockingDoor(Point tile) {
        return entityPositionLookup.getBlockingDoor(tile.x, tile.y, getOwnerId());
    }

    /**
     * Hits a door that is next to us with the melee attack, as often as the
     * attack recharges
     *
     * @param door the door
     */
    private void attackDoor(EntityId door) {
        CreatureMeleeAttack creatureMeleeAttack = entityData.getComponent(entityId, CreatureMeleeAttack.class);
        if (creatureMeleeAttack != null && isAttackRecharged(creatureMeleeAttack)) {
            entityData.setComponent(entityId, new CreatureMeleeAttack(creatureMeleeAttack, gameTimer.getGameTime()));
            stateMachine.changeState(CreatureState.MELEE_ATTACK);
            setDamage(door, creatureMeleeAttack.damage);
        }
    }

    @Override
    public boolean isAtAssignedTaskTarget() {
        Task assignedTask = getAssignedTask();

        // Stopped comes first and the target is asked once, a task may search a path for its target
        if (assignedTask == null || !isStopped()) {
            return false;
        }
        Vector2f target = assignedTask.getTarget(this);
        return target != null && isNear(target);
    }

    private boolean isNear(Vector2f target) {
        Vector3f currentPos = getPosition();
        return (target.distanceSquared(currentPos.x, currentPos.z) < 0.5f);
    }

    /**
     * Standing on the spot, not just on the tile. Well above the arrival
     * tolerance of the steering (0.1), or the creature keeps stepping around
     * the spot
     */
    private boolean isAt(Vector2f target) {
        Vector3f currentPos = getPosition();
        return (target.distanceSquared(currentPos.x, currentPos.z) < 0.25f * 0.25f);
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
    public IEntityController getAttackTarget() {
        CombatPitFighter pitFighter = entityData.getComponent(entityId, CombatPitFighter.class);
        if (pitFighter != null) {
            return getCombatPitOpponent(pitFighter.pit);
        }

        // One that won't fight neither picks a target nor keeps the one it has (DK II 0x45A6D0, 0x45F030)
        if (entityData.getComponent(entityId, Peaceful.class) != null) {
            setAttackTarget(null);
            return null;
        }
        AttackTarget attackTarget = entityData.getComponent(entityId, AttackTarget.class);
        if (attackTarget != null && DungeonHeartTarget.isDungeonHeart(entityData, attackTarget.entityId)) {
            if (isHeartToAttack(attackTarget.entityId)) {
                return getTargetWhileAttackingHeart(attackTarget.entityId);
            }
            attackTarget = null;
        }
        IEntityController attackTargetController = null;
        if (attackTarget == null || isIncapacitated(attackTarget.entityId)
                || isFleeing(attackTarget.entityId) || isCaptive(attackTarget.entityId) || isInvisible(attackTarget.entityId)
                || isUnreachable(attackTarget.entityId)) {

            // Pick a new target, the nearest enemy already attacking us comes first
            // TODO: creatures only now
            EntityId nearestEnemy = getNearestEnemy(false);

            // With no one but workers to fight, go for an enemy heart in sight
            EntityId heart = null;
            if (getNearestEnemy(true) == null) {
                heart = getNearestEnemyHeart(nearestEnemy != null ? getDistanceToCreature(nearestEnemy) : Float.MAX_VALUE);
            }
            if (heart != null) {
                attackTargetController = objectsController.createController(heart);
                setAttackTarget(heart);
            } else if (nearestEnemy != null) {
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

    /**
     * Gets the creature we fight in the Combat Pit. An enemy fighter comes
     * first, then the nearest one of our own side.
     *
     * @param pit the Combat Pit we fight in
     * @return the opponent, or {@code null} if there is none
     */
    private ICreatureController getCombatPitOpponent(EntityId pit) {
        AttackTarget attackTarget = entityData.getComponent(entityId, AttackTarget.class);
        if (attackTarget != null && isCombatPitOpponent(attackTarget.entityId, pit)) {
            return creaturesController.createController(attackTarget.entityId);
        }

        // We beat our opponent, winning cheers us up. Picking the next opponent clears the beaten one, so this is once a win
        CombatPitFighter beaten = attackTarget != null ? entityData.getComponent(attackTarget.entityId, CombatPitFighter.class) : null;
        CreatureMood mood = entityData.getComponent(entityId, CreatureMood.class);
        if (beaten != null && pit.equals(beaten.pit) && isIncapacitated(attackTarget.entityId) && mood != null) {
            entityData.setComponent(entityId, mood.add(CreatureMood.REASON_OTHER,
                    (int) gameSettings.get(Variable.MiscVariable.MiscType.MODIFY_ANGER_OF_COMBAT_PIT_VICTOR).getValue()));
        }

        EntityId nearestEnemy = null;
        float nearestEnemyDistance = Float.MAX_VALUE;
        EntityId nearestOther = null;
        float nearestOtherDistance = Float.MAX_VALUE;
        if (!isAtTopCombatPitLevel(entityId)) {
            for (EntityId entity : entityData.findEntities(new FieldFilter<>(CombatPitFighter.class, "pit", pit), CombatPitFighter.class)) {
                if (!isCombatPitOpponent(entity, pit)) {
                    continue;
                }
                float distance = getDistanceToCreature(entity);
                if (isEnemy(entity)) {
                    if (distance < nearestEnemyDistance) {
                        nearestEnemyDistance = distance;
                        nearestEnemy = entity;
                    }
                } else if (distance < nearestOtherDistance) {
                    nearestOtherDistance = distance;
                    nearestOther = entity;
                }
            }
        }

        EntityId opponent = nearestEnemy != null ? nearestEnemy : nearestOther;
        setAttackTarget(opponent);
        return opponent != null ? creaturesController.createController(opponent) : null;
    }

    private boolean isCombatPitOpponent(EntityId entity, EntityId pit) {
        CombatPitFighter pitFighter = entityData.getComponent(entity, CombatPitFighter.class);
        return !entity.equals(entityId) && pitFighter != null && pit.equals(pitFighter.pit)
                && !isIncapacitated(entity) && !isAtTopCombatPitLevel(entity);
    }

    /**
     * Creatures at the top level the Combat Pit teaches neither fight nor are
     * fought there
     */
    private boolean isAtTopCombatPitLevel(EntityId entity) {
        CreatureExperience creatureExperience = entityData.getComponent(entity, CreatureExperience.class);
        return creatureExperience != null && creatureExperience.level >= (int) gameSettings.get(Variable.MiscVariable.MiscType.COMBAT_PIT_MAX_EXPERIENCE_LEVEL).getValue();
    }

    /**
     * Picks a target while we are hitting a heart
     *
     * @param heart the heart we are hitting
     * @return the new target, or the heart if we go on hitting it
     */
    private IEntityController getTargetWhileAttackingHeart(EntityId heart) {

        // The original picks a target again while hitting a heart, leave it for any fighter that shows up,
        // or for a worker nearer than the heart
        EntityId enemy = getNearestEnemy(true);
        if (enemy == null) {
            enemy = getNearestEnemy(false);
            if (enemy != null && getDistanceToCreature(enemy) >= getDistanceToCreature(heart)) {
                enemy = null;
            }
        }
        if (enemy == null) {
            return objectsController.createController(heart);
        }
        setAttackTarget(enemy);
        return creaturesController.createController(enemy);
    }

    /**
     * Finds the enemy creature we sense and can fight, the nearest enemy
     * already attacking us first
     *
     * @param fightersOnly leave the workers out
     * @return the creature, or {@code null} if there is none
     */
    private EntityId getNearestEnemy(boolean fightersOnly) {
        EntityId nearestEnemy = null;
        float nearestDistance = Float.MAX_VALUE;
        EntityId nearestAttacker = null;
        float nearestAttackerDistance = Float.MAX_VALUE;
        for (EntityId entity : entityPositionLookup.getSensedEntities(entityId)) {
            if (creaturesController.isValidEntity(entity)
                    && isEnemy(entity)
                    && !(isIncapacitated(entity) || isFleeing(entity) || isCaptive(entity) || isInvisible(entity))
                    && !(fightersOnly && creaturesController.createController(entity).isWorker())
                    && hasRoomForAttacker(entity)
                    && (hasPathToEntity(entity) || isWithinAttackDistance(entity))) {
                float distance = getDistanceToCreature(entity);
                if (distance < nearestDistance) {
                    nearestDistance = distance;
                    nearestEnemy = entity;
                }
                if (distance < nearestAttackerDistance && isAttacking(entity, entityId)) {
                    nearestAttackerDistance = distance;
                    nearestAttacker = entity;
                }
            }
        }

        return nearestAttacker != null ? nearestAttacker : nearestEnemy;
    }

    /**
     * Finds the nearest enemy Dungeon Heart we sense and can get next to
     *
     * @param maxDistance the heart needs to be nearer than this
     * @return the heart, or {@code null} if there is none
     */
    private EntityId getNearestEnemyHeart(float maxDistance) {
        EntityId nearestHeart = null;
        float nearestDistance = maxDistance;
        for (EntityId entity : entityPositionLookup.getSensedEntities(entityId)) {
            if (DungeonHeartTarget.isDungeonHeart(entityData, entity) && isHeartToAttack(entity)
                    && getHeartAttackTile(entity) != null) {
                float distance = getDistanceToCreature(entity);
                if (distance < nearestDistance) {
                    nearestDistance = distance;
                    nearestHeart = entity;
                }
            }
        }

        return nearestHeart;
    }

    private float getMeleeRange() {
        return entityData.getComponent(entityId, CreatureMeleeAttack.class).range;
    }

    private boolean isHeartToAttack(EntityId heart) {
        Health health = entityData.getComponent(heart, Health.class);
        return health != null && health.health > 0 && isEnemy(heart)
                && creature.getFightStyle() != Creature.FightStyle.NON_FIGHTER;
    }

    private Point getHeartTile(EntityId heart) {
        return WorldUtils.vectorToPoint(getPosition(entityData, heart));
    }

    private Point getHeartAttackTile(EntityId heart) {
        Point ourPosition = getCreatureCoordinates();
        return DungeonHeartTarget.getAttackTile(ourPosition, getHeartTile(heart), getMeleeRange(),
                tile -> tile.equals(ourPosition) || isWalkable(tile) && navigationService.findPath(ourPosition, tile, this) != null);
    }

    /**
     * A cheap check to save a path search to a wall
     */
    private boolean isWalkable(Point tile) {
        IMapTileInformation mapTile = mapController.getMapData().getTile(tile);
        return mapTile != null && !mapController.getTerrain(mapTile).getFlags().contains(Terrain.TerrainFlag.SOLID);
    }

    /**
     * Tells if a target is out of reach with no way to it. DK II's fight
     * state picks a target again whenever its target is out of reach, and only
     * one it can walk to (0x45F030, 0x45CB00), so such a target is dropped.
     * One already walked to has a way to it.
     *
     * @param entity the target
     * @return true if it can't be hit nor walked to
     */
    private boolean isUnreachable(EntityId entity) {
        return entityData.getComponent(entityId, Navigation.class) == null && !isWithinAttackDistance(entity)
                && !hasPathToEntity(entity);
    }

    private boolean isAttacking(EntityId attacker, EntityId target) {
        AttackTarget attackTarget = entityData.getComponent(attacker, AttackTarget.class);
        return attackTarget != null && target.equals(attackTarget.entityId);
    }

    /**
     * Checks that the target can take one more attacker, we may already be one
     * of them. Like DK II (0x45A9A0), the melee attackers and the ones with
     * attack spells have room for 8 each, and one with spells that finds no
     * room among them fights in melee (0x45A550)
     *
     * @param target the creature we would like to attack
     * @return true if we can join the fight against the target
     */
    private boolean hasRoomForAttacker(EntityId target) {
        int meleeAttackers = 0;
        int rangedAttackers = 0;
        for (EntityId attacker : entityData.findEntities(new FieldFilter<>(AttackTarget.class, "entityId", target), AttackTarget.class)) {
            if (attacker.equals(entityId)) {
                return true;
            }
            if (hasAttackSpell(attacker)) {
                rangedAttackers++;
            } else {
                meleeAttackers++;
            }
        }
        meleeAttackers += Math.max(0, rangedAttackers - CombatRules.MAX_RANGED_ATTACKERS);

        return meleeAttackers < CombatRules.MAX_MELEE_ATTACKERS
                || rangedAttackers < CombatRules.MAX_RANGED_ATTACKERS && hasAttackSpell(entityId);
    }

    private boolean hasAttackSpell(EntityId creature) {
        CreatureSpells creatureSpells = entityData.getComponent(creature, CreatureSpells.class);
        if (creatureSpells != null) {
            for (EntityId creatureSpellEntity : creatureSpells.creatureSpells) {
                CreatureSpell spell = entityData.getComponent(creatureSpellEntity, CreatureSpell.class);
                if (spell != null && levelInfo.getLevelData().getCreatureSpellById(spell.creatureSpellId).getFlags().contains(CreatureSpellFlag.IS_ATTACKING)) {
                    return true;
                }
            }
        }
        return false;
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
        if (DungeonHeartTarget.isDungeonHeart(entityData, attackTarget)) {
            return DungeonHeartTarget.isInReach(WorldUtils.vector3fToVector2f(getPosition()), getHeartTile(attackTarget), getMeleeRange());
        }
        float distanceNeeded = Math.max(getPreferredAttackDistance(), getPossibleAttackDistance());

        // TODO: currently we move only with a tile precision, so accept attack if on the same tile already
        return isInRange(distanceNeeded, attackTarget);
    }

    /**
     * Gets currently possible maximum attack distance for this creature,
     * includes the cooldowns
     *
     * @return maximum distance can attack at the moment
     */
    private float getPossibleAttackDistance() {
        float distanceNeeded = entityData.getComponent(entityId, CreatureMeleeAttack.class).range;
        for (CreatureSpell attack : getCreatureSpells()) {
            toniarts.openkeeper.tools.convert.map.CreatureSpell spell = levelInfo.getLevelData().getCreatureSpellById(attack.creatureSpellId);
            if (spell.getFlags().contains(CreatureSpellFlag.IS_ATTACKING) && isAttackRecharged(attack)) {
                distanceNeeded = Math.max(distanceNeeded, attack.range);
            }
        }

        return distanceNeeded;
    }

    /**
     * Gets the preferred attack distance from target for this creature's
     * figting style
     *
     * @return preferred attack distance
     */
    private float getPreferredAttackDistance() {
        float distanceNeeded = entityData.getComponent(entityId, CreatureMeleeAttack.class).range; // The melee range, the shortest range
        if (creature.getFightStyle() == Creature.FightStyle.SUPPORT) {

            // Get max distance we can cast all spells, and hopefully stay safe
            Optional<Float> shortestDistance = getShortestAttackSpellDistanceNeeded();
            if (shortestDistance.isPresent()) {
                distanceNeeded = shortestDistance.get();
            }
        }

        return distanceNeeded;
    }

    private Optional<Float> getShortestAttackSpellDistanceNeeded() {
        Float shortestDistance = null;
        for (CreatureSpell attack : getCreatureSpells()) {
            toniarts.openkeeper.tools.convert.map.CreatureSpell spell = levelInfo.getLevelData().getCreatureSpellById(attack.creatureSpellId);
            if (spell.getFlags().contains(CreatureSpellFlag.IS_ATTACKING)) {
                if (shortestDistance == null) {
                    shortestDistance = attack.range;
                } else {
                    shortestDistance = Math.min(shortestDistance, attack.range);
                }
            }
        }

        return Optional.ofNullable(shortestDistance);
    }

    private boolean isInRange(float range, EntityId target) {
        return range >= getDistanceToCreature(target) || (isStopped() && isAtSameTile(target));
    }

    private List<CreatureSpell> getCreatureSpells() {
        CreatureSpells creatureSpells = entityData.getComponent(entityId, CreatureSpells.class);
        if (creatureSpells == null) {
            return Collections.emptyList();
        }

        List<CreatureSpell> spells = new ArrayList<>(creatureSpells.creatureSpells.size());
        for (EntityId entity : creatureSpells.creatureSpells) {
            spells.add(entityData.getComponent(entity, CreatureSpell.class));
        }

        return spells;
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

        // TODO: how to apply the damage? Create a component for THIS creature that adds the damage to enemy after the countdown is finished?
        // Always prefer melee if in range
        CreatureMeleeAttack creatureMeleeAttack = entityData.getComponent(entityId, CreatureMeleeAttack.class);
        if (DungeonHeartTarget.isDungeonHeart(entityData, attackTarget)
                ? isWithinAttackDistance(attackTarget) : isInRange(creatureMeleeAttack.range, attackTarget)) {
            if (isAttackRecharged(creatureMeleeAttack)) {
                stateMachine.changeState(CreatureState.MELEE_ATTACK);
                hitWithMelee(attackTarget, creatureMeleeAttack, getMeleeDamageTaken(attackTarget,
                        getCombatPitDamage(attackTarget, creatureMeleeAttack.damage)));
            }
        } else {
            Optional<CreatureSpell> attack = getPreferredAttackSpell(attackTarget);
            if (attack.isPresent()) {
                CreatureSpells creatureSpells = entityData.getComponent(entityId, CreatureSpells.class);
                for (EntityId creatureSpellEntity : creatureSpells.creatureSpells) {
                    CreatureSpell spell = entityData.getComponent(creatureSpellEntity, CreatureSpell.class);
                    if (spell.creatureSpellId == attack.get().creatureSpellId) {
                        entityData.setComponent(creatureSpellEntity, new CreatureSpell(attack.get(), gameTimer.getGameTime()));
                        break;
                    }
                }
                stateMachine.changeState(CreatureState.CAST_SPELL);

                // The shot leaves during the cast, after the spell's shot delay (0x4984D0)
                entityData.setComponent(entityId, new CastShot(attack.get().creatureSpellId, attackTarget, attack.get().range,
                        gameTimer.getGameTime() + getShotDelay(attack.get().creatureSpellId)));
            }
        }
    }

    @Override
    public boolean castSupportSpell() {
        CreatureSpells creatureSpells = entityData.getComponent(entityId, CreatureSpells.class);
        if (creatureSpells == null) {
            return false;
        }

        // Of the spells for our own creatures, the last one that is ready gets its go
        EntityId spellEntity = null;
        for (EntityId creatureSpellEntity : creatureSpells.creatureSpells) {
            CreatureSpell spell = entityData.getComponent(creatureSpellEntity, CreatureSpell.class);
            if (isAttackRecharged(spell) && levelInfo.getLevelData().getCreatureSpellById(spell.creatureSpellId).getFlags().contains(CreatureSpellFlag.CREATURE)) {
                spellEntity = creatureSpellEntity;
            }
        }
        if (spellEntity == null) {
            return false;
        }

        CreatureSpell spell = entityData.getComponent(spellEntity, CreatureSpell.class);
        toniarts.openkeeper.tools.convert.map.CreatureSpell creatureSpell = levelInfo.getLevelData().getCreatureSpellById(spell.creatureSpellId);
        EntityId target = getSupportSpellTarget(spell, creatureSpell);
        if (target == null) {
            return false;
        }

        entityData.setComponent(spellEntity, new CreatureSpell(spell, gameTimer.getGameTime()));
        stateMachine.changeState(CreatureState.CAST_SPELL);

        // Cast like an attack spell, the shot leaves after the spell's shot delay (0x45F030, 0x498870)
        entityData.setComponent(entityId, new CastShot(spell.creatureSpellId, target, spell.range,
                gameTimer.getGameTime() + getShotDelay(spell.creatureSpellId)));

        return true;
    }

    /**
     * Gets the creature to cast a spell for our own creatures on. A spell for
     * the caster itself needs the caster to fight. The others go to the nearest
     * of our fighters in the spell's range that has less than a quarter of its
     * health left, and that can be the caster too
     *
     * @param spell the spell we have
     * @param creatureSpell the spell's data
     * @return the creature to cast on, or {@code null} if nobody needs it
     */
    private EntityId getSupportSpellTarget(CreatureSpell spell, toniarts.openkeeper.tools.convert.map.CreatureSpell creatureSpell) {
        if (creatureSpell.getFlags().contains(CreatureSpellFlag.CAST_ON_SELF)) {
            return isFighting(entityId) && isSupportSpellNeeded(entityId, creatureSpell) ? entityId : null;
        }
        if (isSupportSpellTarget(entityId, creatureSpell)) {
            return entityId;
        }

        EntityId nearestTarget = null;
        float nearestDistance = Float.MAX_VALUE;
        for (EntityId entity : entityPositionLookup.getSensedEntities(entityId)) {
            float distance = getDistanceToCreature(entity);
            if (distance < nearestDistance && distance <= spell.range && isSupportSpellTarget(entity, creatureSpell)) {
                nearestDistance = distance;
                nearestTarget = entity;
            }
        }

        return nearestTarget;
    }

    private boolean isSupportSpellTarget(EntityId entity, toniarts.openkeeper.tools.convert.map.CreatureSpell creatureSpell) {
        Health health = entityData.getComponent(entity, Health.class);
        Owner owner = entityData.getComponent(entity, Owner.class);

        return health != null && owner != null && owner.ownerId == getOwnerId() && health.health < health.maxHealth / 4
                && isFighting(entity) && !isIncapacitated(entity) && !isCaptive(entity) && isSupportSpellNeeded(entity, creatureSpell);
    }

    private boolean isFighting(EntityId entity) {
        CreatureAi creatureAi = entityData.getComponent(entity, CreatureAi.class);
        if (creatureAi == null) {
            return false;
        }
        CreatureState state = creatureAi.getCreatureState();

        return state == CreatureState.FIGHT || state == CreatureState.MELEE_ATTACK || state == CreatureState.CAST_SPELL;
    }

    private boolean isInvisible(EntityId entity) {
        return entityData.getComponent(entity, Invisible.class) != null;
    }

    /**
     * The spell's own condition on the creature it would be cast on
     *
     * @param entity the creature to cast on
     * @param creatureSpell the spell's data
     * @return true if the spell is of use to the creature
     */
    private boolean isSupportSpellNeeded(EntityId entity, toniarts.openkeeper.tools.convert.map.CreatureSpell creatureSpell) {
        Health health = entityData.getComponent(entity, Health.class);

        return switch (creatureSpell.getAlternativeShot()) {
            case HEAL ->
                health != null && health.health < health.maxHealth / 2;
            case ARMOUR ->
                isFighting(entity) && entityData.getComponent(entity, Armour.class) == null;
            case INVULNERABLE ->
                isFighting(entity) && entityData.getComponent(entity, Invulnerable.class) == null;
            case INVISIBLE ->
                isFighting(entity) && !isInvisible(entity);
            case HASTE ->
                entityData.getComponent(entity, SpeedChange.class) == null;
            default ->
                false;
        };
    }

    @Override
    public void releaseCastShot() {
        CastShot castShot = entityData.getComponent(entityId, CastShot.class);
        if (castShot == null || gameTimer.getGameTime() < castShot.releaseTime) {
            return;
        }

        entityData.removeComponent(entityId, CastShot.class);

        // TODO: What is the alternative shot all about?
        toniarts.openkeeper.tools.convert.map.CreatureSpell creatureSpell = levelInfo.getLevelData().getCreatureSpellById(castShot.creatureSpellId);
        int shotData1 = getShotData1(creatureSpell);

        // A shot that flies, cast on its own caster, hits it at once: it starts on the caster, the only thing such a
        // shot hits (0x451570). An instant shot, like Heal, is fired like any other
        boolean flyingOnCaster = entityId.equals(castShot.target)
                && levelInfo.getLevelData().getShotById(creatureSpell.getShotTypeId()).getSpeed() > 0;
        if (flyingOnCaster || !shotsController.fireShot(creatureSpell.getShotTypeId(), shotData1, creatureSpell.getShotData2(),
                getOwnerId(), getShotPosition(castShot.creatureSpellId, getPosition(entityData, castShot.target)), castShot.target, entityId, castShot.range)) {
            shotsController.createShot(creatureSpell.getShotTypeId(), shotData1, creatureSpell.getShotData2(), getOwnerId(), getPosition(), castShot.target, entityId);

            // TODO: Of course not like that, the shot is created and it does what it does
            // Set the damage, a spell for our own creatures does them none
            if (creatureSpell.getFlags().contains(CreatureSpellFlag.IS_ATTACKING)) {
                setDamage(castShot.target, shotData1);
            }
        }
    }

    @Override
    public void cancelCastShot() {
        entityData.removeComponent(entityId, CastShot.class);
    }

    /**
     * How long into the cast the shot leaves. It leaves at the end of the cast
     * at the latest, so that a cast always fires. A possessed creature's shot
     * leaves at once (0x4984D0).
     *
     * @param creatureSpellId the spell cast
     * @return the time from the start of the cast
     */
    private double getShotDelay(short creatureSpellId) {
        if (entityData.getComponent(entityId, Possessed.class) != null) {
            return 0;
        }

        double castTime = getAnimationTime(creature, Creature.AnimationType.CAST_SPELL);
        for (Creature.Spell spell : creature.getSpells()) {
            if (spell.getCreatureSpellId() == creatureSpellId) {
                return Math.min(spell.getShotDelay(), castTime);
            }
        }

        return 0;
    }

    /**
     * Gets the damage our melee hit does to the target
     *
     * @param attackTarget the target we hit
     * @param damage the damage of our melee attack
     * @return the damage the target takes
     */
    private int getMeleeDamageTaken(EntityId attackTarget, int damage) {
        CreatureComponent targetCreature = entityData.getComponent(attackTarget, CreatureComponent.class);
        if (targetCreature != null && !CombatRules.canBeHurtBy(creature.getFlags(),
                levelInfo.getLevelData().getCreature(targetCreature.creatureId).getFlags())) {
            return 0;
        }

        // Only creatures take the bonus for a hit from behind, doors and objects are hit as they are (0x498E30)
        Position targetPosition = entityData.getComponent(attackTarget, Position.class);
        Vector3f ourPosition = getPosition();
        if (targetCreature != null && targetPosition != null && ourPosition != null) {
            damage = CombatRules.addRearAttackBonus(damage, ourPosition, targetPosition.position, targetPosition.rotation);
        }

        return damage;
    }

    /**
     * Hits the target with our melee attack, which then needs to recharge
     *
     * @param attackTarget the target we hit
     * @param creatureMeleeAttack our melee attack
     * @param damage the damage to deal
     */
    private void hitWithMelee(EntityId attackTarget, CreatureMeleeAttack creatureMeleeAttack, int damage) {
        entityData.setComponent(entityId, new CreatureMeleeAttack(creatureMeleeAttack, gameTimer.getGameTime()));
        if (damage > 0) {
            setDamage(attackTarget, damage);
        }
        gameEventService.emitAt(GameEventType.MELEE_HIT, ThingFlag.CREATURE, creature.getCreatureId(), attackTarget, getPosition());
    }

    private float getFirstPersonRechargeModifyPercentage() {
        Variable.MiscVariable modify = gameSettings.get(Variable.MiscVariable.MiscType.FIRST_PERSON_RECHARGE_MODIFY_PERCENTAGE);
        return modify != null ? modify.getValue() : 100;
    }

    /**
     * Gets what a stat is in first person, as a percentage of the normal
     *
     * @param statType the stat
     * @return 100 plus the stat increase, 100 if there is none
     */
    private int getFirstPersonPercentage(Variable.CreatureStats.StatType statType) {
        Variable.CreatureFirstPerson stat = levelInfo.getLevelData().getCreatureFirstPersonStats().get(statType);
        return 100 + (stat != null ? stat.getValue() : 0);
    }

    /**
     * Where the creature's shot starts. The spell's shot offset is in front of
     * the creature and above its feet (0x4984D0), the creature faces the
     * target when it fires.
     *
     * @param creatureSpellId the spell fired
     * @param targetPosition where the shot is aimed at
     * @return the start of the shot
     */
    private Vector3f getShotPosition(short creatureSpellId, Vector3f targetPosition) {
        Vector3f position = getPosition();
        Vector3f result = new Vector3f(position);
        for (Creature.Spell spell : creature.getSpells()) {
            if (spell.getCreatureSpellId() != creatureSpellId) {
                continue;
            }

            Vector3f forward = targetPosition != null ? targetPosition.subtract(position).setY(0) : Vector3f.ZERO;
            if (forward.lengthSquared() > 0) {
                result.addLocal(forward.normalizeLocal().multLocal(-spell.getShotOffset().y));
            }
            result.y += spell.getShotOffset().z;
            break;
        }

        return result;
    }

    /**
     * Gets preferred attack creature spell that is available to cast to the
     * target
     *
     * @param attackTarget target to attack
     * @return creature spell to cast on the unsuspecting victim
     */
    private Optional<CreatureSpell> getPreferredAttackSpell(EntityId attackTarget) {
        List<CreatureSpell> creatureSpells = getCreatureSpells();
        if (creatureSpells.isEmpty()) {
            return Optional.empty();
        }

        // Sort in order of preference
        return creatureSpells
                .stream()
                .filter(creatureSpell -> isAttackRecharged(creatureSpell) && isInRange(creatureSpell.range, attackTarget) && levelInfo.getLevelData().getCreatureSpellById(creatureSpell.creatureSpellId).getFlags().contains(CreatureSpellFlag.IS_ATTACKING))
                .sorted(Comparator.<CreatureSpell>comparingInt(creatureSpell -> levelInfo.getLevelData().getCreatureSpellById(creatureSpell.creatureSpellId).getCombatPoints()).reversed())
                .findFirst();
    }

    /**
     * Fighters in the Combat Pit hit by the melee damage modifier percentage
     * (normal damage at 100) and take only a part of the damage
     *
     * @param attackTarget the target we hit
     * @param damage the damage of our hit
     * @return the damage the target takes
     */
    private int getCombatPitDamage(EntityId attackTarget, int damage) {
        if (isCombatPitFighter()) {
            damage = damage * (int) gameSettings.get(Variable.MiscVariable.MiscType.COMBAT_PIT_MELEE_DAMAGE_MODIFIER_PERCENTAGE).getValue() / 100;
        }
        if (entityData.getComponent(attackTarget, CombatPitFighter.class) != null) {
            damage = damage * (int) gameSettings.get(Variable.MiscVariable.MiscType.PIT_PERCENTAGE_DAMAGE_TAKEN_OF_NORMAL_COMBAT).getValue() / 100;
        }

        return damage;
    }

    private boolean isAttackRecharged(Attack attack) {
        return attack.attactStartTime == null || attack.attactStartTime + attack.rechargeTime <= gameTimer.getGameTime();
    }

    @Override
    public void navigateToAttackTarget(EntityId attackTarget) {
        if (DungeonHeartTarget.isDungeonHeart(entityData, attackTarget)) {
            navigateToHeart(attackTarget);
            return;
        }
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

    private void navigateToHeart(EntityId heart) {
        Point heartTile = getHeartTile(heart);
        Navigation currentNavigation = entityData.getComponent(entityId, Navigation.class);
        if (currentNavigation != null
                && DungeonHeartTarget.isInReach(WorldUtils.pointToVector2f(currentNavigation.target), heartTile, getMeleeRange())) {
            return;
        }

        Point attackTile = getHeartAttackTile(heart);
        if (attackTile == null) {

            // Walled off, pick something else to do
            setAttackTarget(null);
        } else if (!attackTile.equals(getCreatureCoordinates())) {
            createNavigation(getCreatureCoordinates(), attackTile, heartTile);
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
    public boolean answerCallToArms(EntityId banner) {
        Position bannerPosition = entityData.getComponent(banner, Position.class);
        if (bannerPosition == null) {
            return false;
        }
        Point destination = WorldUtils.vectorToPoint(bannerPosition.position);
        GraphPath<IMapTileInformation> path = navigationService.findPath(getCreatureCoordinates(), destination, this);
        if (path == null) {
            return false;
        }

        stateMachine.changeState(CreatureState.CALL_TO_ARMS);
        setFollowTarget(banner);
        entityData.setComponent(entityId, new Navigation(destination, null, SteeringUtils.pathToList(path)));
        return true;
    }

    @Override
    public boolean heedCallToArms() {
        FollowTarget followTarget = entityData.getComponent(entityId, FollowTarget.class);
        if (followTarget == null || entityData.getComponent(followTarget.entityId, CallToArms.class) == null) {
            return false;
        }

        // Wait while the keeper carries the banner
        Position bannerPosition = entityData.getComponent(followTarget.entityId, Position.class);
        if (bannerPosition == null || shouldFleeOrAttack()) {
            return true;
        }
        if (!isStopped()) {
            return true;
        }

        // Go to the banner, or mill about it when there
        Point banner = WorldUtils.vectorToPoint(bannerPosition.position);
        Point ourPosition = getCreatureCoordinates();
        if (Math.max(Math.abs(ourPosition.x - banner.x), Math.abs(ourPosition.y - banner.y)) > 2) {
            if (createNavigation(ourPosition, banner, null)) {
                gameEventService.emitToPlayer(GameEventType.CALL_TO_ARMS_UNREACHABLE, getOwnerId());
                return false;
            }
        } else {
            navigateToRandomPointAroundTarget(followTarget.entityId, 2);
        }
        return true;
    }

    /**
     * While its keeper has a Call to Arms banner up, a creature only starts a
     * fight near the banner
     *
     * @return true if we may start a fight here
     */
    private boolean isNearCallToArms() {
        Vector3f ourPosition = getPosition();
        if (ourPosition == null) {
            return true;
        }
        for (EntityId banner : entityData.findEntities(new FieldFilter<>(Owner.class, "ownerId", getOwnerId()), CallToArms.class, Owner.class)) {
            CallToArms callToArms = entityData.getComponent(banner, CallToArms.class);
            Position bannerPosition = entityData.getComponent(banner, Position.class);
            if (callToArms != null && callToArms.isUp() && bannerPosition != null
                    && bannerPosition.position.distance(ourPosition) > gameSettings.get(Variable.MiscVariable.MiscType.CALL_TO_ARMS_FIGHT_DISTANCE_TILES).getValue() * WorldUtils.TILE_WIDTH) {
                return false;
            }
        }
        return true;
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
        Point threat = findThreatLocation();
        if (threat != null) {
            fleeFrom(threat);
            return;
        }

        // Nothing to run from, just go to the dungeon heart or random tiles
        Keeper keeper = levelInfo.getPlayer(getOwnerId());
        if (keeper != null && keeper.getDungeonHeartLocation() != null) {
            Point p = keeper.getDungeonHeartLocation();
            createNavigation(getCreatureCoordinates(), new Point(p.x - 2, p.y - 2), null);
        } else {
            navigateToRandomPoint();
        }
    }

    /**
     * Finds what we are running from. When we are frightened it is the
     * creature that frightened us, otherwise the nearest enemy we sense that
     * threatens us. A trap is one after it has fired, see {@link Threat}.
     *
     * @return the tile of the threat, null if there is none
     */
    private Point findThreatLocation() {
        Frightened frightened = entityData.getComponent(entityId, Frightened.class);
        if (frightened != null && frightened.endTime > gameTimer.getGameTime()) {
            IMapTileInformation tile = entityPositionLookup.getEntityLocation(frightened.entity);
            if (tile != null) {
                return tile.getLocation();
            }
        }

        Point ourLocation = getCreatureCoordinates();
        Point nearest = null;
        for (EntityId entity : entityPositionLookup.getSensedEntities(entityId)) {
            IMapTileInformation tile = isEnemy(entity) && isThreat(entity) && getThreat(entity) > 0
                    && ThreatSource.isThreateningUs(entityData, entity, entityId)
                    ? entityPositionLookup.getEntityLocation(entity) : null;
            if (tile != null && (nearest == null || tile.getLocation().distance(ourLocation) < nearest.distance(ourLocation))) {
                nearest = tile.getLocation();
            }
        }
        return nearest;
    }

    /**
     * Runs away from the threat the way DKII.EXE does, see
     * {@link FleeDestinationFinder}
     */
    private void fleeFrom(Point threat) {
        Point ourLocation = getCreatureCoordinates();
        FleeDestinationFinder finder = new FleeDestinationFinder(mapController.getMapData().getWidth(),
                mapController.getMapData().getHeight(),
                (x, y) -> navigationService.findPath(ourLocation, new Point(x, y), this) != null,
                (x, y) -> mapController.getMapData().getTile(x, y).getOwnerId() == getOwnerId() && !mapController.isSolid(new Point(x, y)),
                ThreadLocalRandom.current());
        finder.findAwayFrom(ourLocation, threat, FLEE_DISTANCE)
                .or(() -> finder.findAround(ourLocation))
                .ifPresent(destination -> createNavigation(ourLocation, destination, null));
    }

    @Override
    public boolean isAttacked() {
        // TODO:
        return false;
    }

    @Override
    public boolean isEnoughSleep() {
        CreatureSleep creatureSleep = entityData.getComponent(entityId, CreatureSleep.class);
        return creatureSleep.sleepNeed == 0 && isFullHealth();
    }

    @Override
    public boolean isCombatPitFighter() {
        return entityData.getComponent(entityId, CombatPitFighter.class) != null;
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
        if (health == null || entityData.getComponent(entityId, Unconscious.class) != null) {
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
                return mapController.isWater(WorldUtils.vectorToPoint(pos));
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
                return mapController.isLava(WorldUtils.vectorToPoint(pos));
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
    public void processTick(float tpf) {
        Armour armour = entityData.getComponent(entityId, Armour.class);
        if (armour != null && armour.endTime <= gameTimer.getGameTime()) {
            entityData.removeComponent(entityId, Armour.class);
        }
        Invulnerable invulnerable = entityData.getComponent(entityId, Invulnerable.class);
        if (invulnerable != null && invulnerable.endTime <= gameTimer.getGameTime()) {
            entityData.removeComponent(entityId, Invulnerable.class);
        }
        Invisible invisible = entityData.getComponent(entityId, Invisible.class);
        if (invisible != null && invisible.endTime <= gameTimer.getGameTime()) {
            entityData.removeComponent(entityId, Invisible.class);
        }
        SpeedBlur speedBlur = entityData.getComponent(entityId, SpeedBlur.class);
        if (speedBlur != null && speedBlur.endTime <= gameTimer.getGameTime()) {
            entityData.removeComponent(entityId, SpeedBlur.class);
        }

        // A manual drop can already carry its target in AI, but no state may
        // enter or advance until the fall system has finished landing.
        if (entityData.getComponent(entityId, CreatureFall.class) != null) {
            return;
        }

        // A teleporting Imp does nothing else until it has landed (states 51 and 52)
        Teleport teleport = entityData.getComponent(entityId, Teleport.class);
        if (teleport != null) {
            continueTeleport(teleport);
            return;
        }

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
        TaskComponent taskComponent = entityData.getComponent(entityId, TaskComponent.class);
        if (taskComponent != null && taskComponent.taskStarted) {
            entityData.setComponent(entityId, new TaskComponent(taskComponent.taskId, taskComponent.targetEntity, taskComponent.targetLocation, taskComponent.taskType, taskComponent.taskDuration + tpf, taskComponent.taskStarted));
        }

        // A slap interrupts the work, once per slap
        Slapped slapped = entityData.getComponent(entityId, Slapped.class);
        if (slapped != null && !slapped.reacted) {
            entityData.setComponent(entityId, new Slapped(slapped.startTime, true));
            interruptWork();
        }

        // Hypnotised on the render thread, we start following here. Outside FOLLOW only a creature that was
        // just hypnotised has its hypnotiser as the follow target, FOLLOW drops it on the way out
        FollowTarget followTarget = entityData.getComponent(entityId, FollowTarget.class);
        if (followTarget != null && followTarget.entityId.equals(getHypnotiser())
                && stateMachine.getCurrentState() != CreatureState.FOLLOW) {
            unassingCurrentTask();
            stateMachine.changeState(CreatureState.FOLLOW);
        }

        stateMachine.update();

        // Also change our state component
        CreatureAi creatureAi = entityData.getComponent(entityId, CreatureAi.class);
        if (creatureAi == null || stateMachine.getCurrentState() != creatureAi.getCreatureState()) {
            entityData.setComponent(entityId, new CreatureAi(gameTimer.getGameTime(), stateMachine.getCurrentState(), creature.getId(),
                    creatureAi == null ? null : creatureAi.getTargetState()));
        }
    }

    @Override
    public boolean isTimeToReEvaluate() {

        // See that we have been motionless for enough time, per state
        // TODO: now just 5 seconds, it is the default for imps
        // DK II's pit fighter waits four seconds' worth of ticks on each spot (0x457CF0)
        if (getOwnerId() == Player.GOOD_PLAYER_ID && stateMachine.getCurrentState() == CreatureState.IDLE) {
            return motionless >= HERO_IDLE_TICKS / (float) levelInfo.getLevelData().getGameLevel().getTicksPerSec();
        }
        return motionless >= (stateMachine.getCurrentState() == CreatureState.COMBAT_PIT ? 4f : 5f);
    }

    @Override
    public void resetReEvaluationTimer() {
        motionless = 0;
    }

    @Override
    public void addGold(int amount) {
        Gold gold = entityData.getComponent(entityId, Gold.class);

        // Drop excess gold, we can only carry so much
        int maxGoldCanAdd = gold.maxGold - gold.gold;
        int goldToAdd = Math.min(amount, maxGoldCanAdd);
        int looseGold = amount - goldToAdd;

        if (goldToAdd > 0) {
            entityData.setComponent(entityId, new Gold(gold.gold + goldToAdd, gold.maxGold));
        }

        if (looseGold > 0) {
            Point coordinates = getCreatureCoordinates();

            // Like in DKII (0x4510C0), the gold goes to the pile at our feet if there is one
            synchronized (GameWorldController.GOLD_LOCK) {
                EntityId goldPile = GameWorldController.findLooseGoldPile(entityData, entityPositionLookup, coordinates);
                if (goldPile != null) {
                    Gold pile = entityData.getComponent(goldPile, Gold.class);
                    entityData.setComponent(goldPile, new Gold(pile.gold + looseGold, pile.maxGold));
                    return;
                }
            }
            objectsController.addLooseGold(coordinates.x, coordinates.y, looseGold, (int) gameSettings.get(Variable.MiscVariable.MiscType.MAX_GOLD_PILE_OUTSIDE_TREASURY).getValue());
        }
    }

    @Override
    public void takePay(int amount) {
        Gold gold = entityData.getComponent(entityId, Gold.class);
        entityData.setComponent(entityId, new Gold(Math.min(gold.gold + amount / 2, gold.maxGold), gold.maxGold));

        // Whatever the amount, the creature is owed nothing more and is no longer angry about its pay
        CreaturePay pay = entityData.getComponent(entityId, CreaturePay.class);
        if (pay != null && pay.unpaid > 0) {
            entityData.removeComponent(entityId, CreaturePay.class);
        }
        CreatureMood mood = entityData.getComponent(entityId, CreatureMood.class);
        if (mood != null) {
            entityData.setComponent(entityId, mood.clear(CreatureMood.REASON_NO_PAY));
        }
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
    public boolean isUnconscious() {
        return entityData.getComponent(entityId, Unconscious.class) != null;
    }

    @Override
    public void wakeUp() {
        Unconscious unconscious = entityData.getComponent(entityId, Unconscious.class);
        Health health = entityData.getComponent(entityId, Health.class);
        if (unconscious == null || health == null) {
            return;
        }

        // In DK II the health of a knocked out creature is its dying time in ticks, one is lost every tick
        // (DKII.EXE 0x4B2CE0, 0x4599D0). It wakes up with what is left (0x468420)
        double timeLeft = gameSettings.get(Variable.MiscVariable.MiscType.CREATURE_DYING_STATE_DURATION_SECONDS).getValue()
                - (gameTimer.getGameTime() - unconscious.startTime);
        int healthLeft = (int) (timeLeft * levelInfo.getLevelData().getGameLevel().getTicksPerSec());
        entityData.setComponent(entityId, new Health(Math.clamp(healthLeft, 1, health.maxHealth), health.maxHealth));
        entityData.removeComponent(entityId, Unconscious.class);
        stateMachine.changeState(CreatureState.IDLE);
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

        //workNavigationRequired = true;
        entityData.setComponent(entityId, new TaskComponent(task.getId(), task.getTaskTarget(), task.getTaskLocation(), task.getTaskType(), 0.0f, false));
    }

    @Override
    public Creature getCreature() {
        return creature;
    }

    @Override
    public void fireEvent(GameEventType type, Point location) {
        gameEventService.emit(type, ThingFlag.CREATURE, creature.getCreatureId(), location);
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

    /**
     * A slapped worker drops whatever it is working on and looks for work
     * anew
     */
    private void interruptWork() {
        if (!isWorker() || stateMachine.getCurrentState() != CreatureState.WORK) {
            return;
        }

        stateMachine.changeState(CreatureState.IDLE);
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
        CreatureSleep updated = new CreatureSleep(creatureSleep);
        updated.lairObjectId = lairId;
        entityData.setComponent(entityId, updated);
    }

    @Override
    public void sleep() {
        double gameTime = gameTimer.getGameTime();
        // Native lair recovery uses global second boundaries, not a full second after entry.
        double recoveryTime = Math.floor(gameTime);
        entityData.setComponent(entityId, new CreatureRecuperating(gameTime, recoveryTime, recoveryTime));
        CreatureSleep creatureSleep = entityData.getComponent(entityId, CreatureSleep.class);
        CreatureSleep updated = new CreatureSleep(creatureSleep);
        updated.lastSleepTime = gameTime;
        updated.sleepStartTime = gameTime;
        entityData.setComponent(entityId, updated);
        stateMachine.changeState(CreatureState.SLEEPING);
    }

    @Override
    public void knockDown() {

        // Without the AI component the creature is in hand, falling or possessed, nothing for us to knock down
        if (entityData.getComponent(entityId, CreatureAi.class) == null || isIncapacitated()) {
            return;
        }

        unassingCurrentTask();
        stopCreature();
        stateMachine.changeState(CreatureState.FALLEN);

        // Written here and not at the end of the tick, so that the state time restarts also when the creature
        // was down already. That keeps a creature that is struck over and over down
        entityData.setComponent(entityId, new CreatureAi(gameTimer.getGameTime(), CreatureState.FALLEN, creature.getId(),
                entityData.getComponent(entityId, CreatureAi.class).getTargetState()));
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

            // We take a creature disguised as one of ours for one of ours (0x45B140)
            Disguised disguised = entityData.getComponent(entity, Disguised.class);
            if (disguised != null && disguised.ownerId == getOwnerId()) {
                return false;
            }
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

    @Override
    public boolean isScared() {

        // It takes three fight turns in a row with the threat over our fear (DK II 0x45EC80)
        BackOff backOff = entityData.getComponent(entityId, BackOff.class);
        int overThreatChecks = 0;
        if (entityData.getComponent(entityId, Fearless.class) == null && !isCombatPitFighter()
                && getFightThreat() > entityData.getComponent(entityId, CreatureComponent.class).fear) {
            overThreatChecks = Math.min(backOff != null ? backOff.overThreatChecks + 1 : 1, SCARED_OVER_THREAT_CHECKS);
        }
        if (backOff == null || backOff.overThreatChecks != overThreatChecks) {
            entityData.setComponent(entityId, new BackOff(overThreatChecks, backOff != null ? backOff.endTime : 0));
        }

        return overThreatChecks >= SCARED_OVER_THREAT_CHECKS;
    }

    /**
     * Gets the threat of the enemies we fight. An enemy shares its threat
     * evenly between its target and the creatures attacking it. Near our own
     * Dungeon Heart we take it more lightly (DK II 0x45E4F0)
     *
     * @return the threat to us
     */
    private int getFightThreat() {
        AttackTarget ourTarget = entityData.getComponent(entityId, AttackTarget.class);
        int threat = 0;

        // The enemies we fight count however far away they are, not only the ones we sense, so a spell caster
        // fighting from a distance feels their threat too
        Set<EntityId> enemies = new HashSet<>(entityData.findEntities(new FieldFilter<>(AttackTarget.class, "entityId", entityId), AttackTarget.class));
        if (ourTarget != null) {
            enemies.add(ourTarget.entityId);
        }
        for (EntityId enemy : enemies) {
            AttackTarget enemyTarget = entityData.getComponent(enemy, AttackTarget.class);
            int shares = (enemyTarget != null && entityId.equals(enemyTarget.entityId) ? 1 : 0)
                    + (ourTarget != null && enemy.equals(ourTarget.entityId) ? 1 : 0);
            if (shares > 0 && isEnemy(enemy) && isThreat(enemy)) {
                int opponents = entityData.findEntities(new FieldFilter<>(AttackTarget.class, "entityId", enemy), AttackTarget.class).size()
                        + (enemyTarget != null ? 1 : 0);
                threat += shares * (getThreat(enemy) / opponents);
            }
        }

        Keeper keeper = levelInfo.getPlayer(getOwnerId());
        if (keeper != null && keeper.getDungeonHeartLocation() != null) {
            Point heart = keeper.getDungeonHeartLocation();
            Point tile = getCreatureCoordinates();
            int distance = Math.max(Math.abs(tile.x - heart.x), Math.abs(tile.y - heart.y));
            threat = threat * Math.min(distance, HEART_THREAT_DISTANCE) / HEART_THREAT_DISTANCE;
        }

        return threat;
    }

    @Override
    public void startBackOff() {

        // The more we are scared, the sooner we withdraw. We don't back off from one that would catch us (DK II 0x45F030)
        int fear = entityData.getComponent(entityId, CreatureComponent.class).fear;
        AttackTarget attackTarget = entityData.getComponent(entityId, AttackTarget.class);
        CreatureComponent targetCreature = attackTarget != null ? entityData.getComponent(attackTarget.entityId, CreatureComponent.class) : null;
        int turns = 0;
        if (fear > 0 && attackTarget != null && creature.getFightStyle() != Creature.FightStyle.NON_FIGHTER
                && (targetCreature == null || targetCreature.runSpeed <= entityData.getComponent(entityId, CreatureComponent.class).runSpeed)) {
            int minimum = (int) gameSettings.get(Variable.MiscVariable.MiscType.BACK_OFF_MINIMUM_DURATION_SECONDS).getValue();
            int maximum = (int) gameSettings.get(Variable.MiscVariable.MiscType.BACK_OFF_MAXIMUM_DURATION_SECONDS).getValue();
            turns = minimum + (maximum - minimum) * (4 - Math.min(getFightThreat() / fear, 4));
        }

        // The durations are in turns, despite the names
        BackOff backOff = entityData.getComponent(entityId, BackOff.class);
        entityData.setComponent(entityId, new BackOff(backOff != null ? backOff.overThreatChecks : 0,
                gameTimer.getGameTime() + turns / (double) levelInfo.getLevelData().getGameLevel().getTicksPerSec()));
    }

    @Override
    public boolean isBackOffOver() {
        BackOff backOff = entityData.getComponent(entityId, BackOff.class);
        return backOff == null || backOff.endTime <= gameTimer.getGameTime();
    }

    @Override
    public boolean backOff() {
        AttackTarget attackTarget = entityData.getComponent(entityId, AttackTarget.class);
        Vector3f targetPosition = attackTarget != null ? getPosition(entityData, attackTarget.entityId) : null;
        Vector3f position = getPosition();
        if (targetPosition == null || position == null) {
            return false;
        }

        // Far enough from the target's reach, we just stand (DK II 0x4602E0)
        CreatureMeleeAttack targetMeleeAttack = entityData.getComponent(attackTarget.entityId, CreatureMeleeAttack.class);
        float range = Math.max(BACK_OFF_RANGE, targetMeleeAttack != null ? targetMeleeAttack.range : 0);
        float dx = position.x - targetPosition.x;
        float dz = position.z - targetPosition.z;
        if (dx * dx + dz * dz > range * range * 5 / 4) {
            stopCreature();
            return true;
        }
        if (!isStopped()) {
            return true;
        }

        // Step back a tile and face the target again. Straight away from it, or else along the wall behind us.
        // DK II backs off a little at a time, so it gets stuck only against a wall right behind it
        Point tile = getCreatureCoordinates();
        int direction = Math.round(FastMath.atan2(dz, dx) / FastMath.QUARTER_PI);
        for (int turn : new int[]{0, 1, -1, 2, -2}) {
            float angle = (direction + turn) * FastMath.QUARTER_PI;
            Point step = new Point(tile.x + Math.round(FastMath.cos(angle)), tile.y + Math.round(FastMath.sin(angle)));
            GraphPath<IMapTileInformation> path = navigationService.findPath(tile, step, this);
            if (path != null) {
                entityData.setComponent(entityId, new Navigation(step, WorldUtils.vectorToPoint(targetPosition), SteeringUtils.pathToList(path)));
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean withdrawToHeroLair() {

        // Below a sixth of its health, unless it is fearless (DK II 0x45EDA0)
        Health health = entityData.getComponent(entityId, Health.class);
        if (getOwnerId() != Player.GOOD_PLAYER_ID || entityData.getComponent(entityId, ReturnToHeroLair.class) == null
                || entityData.getComponent(entityId, Fearless.class) != null || isCombatPitFighter()
                || health == null || health.health >= health.maxHealth / 6) {
            return false;
        }

        // To the first hero lair action point we can get to, else we fight on (DK II 0x4611D0)
        for (ActionPoint actionPoint : levelInfo.getActionPoints()) {
            if (actionPoint.getFlags().contains(Thing.ActionPoint.ActionPointFlag.HERO_LAIR) && !createNavigation(getCreatureCoordinates(), actionPoint.getStart(), null)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public void wanderOnOwnLand() {

        // A tile of our own on the square 10 tiles round us. DK II goes round the square from a random start
        // to its end only, so it may find nothing this time (0x461BB0)
        Point tile = getCreatureCoordinates();
        int side = OWN_LAND_WANDER_DISTANCE * 2;
        for (int i = ThreadLocalRandom.current().nextInt(side * 4); i < side * 4; i++) {
            int offset = i % side - OWN_LAND_WANDER_DISTANCE;
            Point destination = switch (i / side) {
                case 0 -> new Point(tile.x + offset, tile.y - OWN_LAND_WANDER_DISTANCE);
                case 1 -> new Point(tile.x + OWN_LAND_WANDER_DISTANCE, tile.y + offset);
                case 2 -> new Point(tile.x + offset, tile.y + OWN_LAND_WANDER_DISTANCE);
                default -> new Point(tile.x - OWN_LAND_WANDER_DISTANCE, tile.y + offset);
            };
            IMapTileInformation mapTile = mapController.getMapData().getTile(destination);
            if (mapTile != null && mapTile.getOwnerId() == getOwnerId() && !mapController.isSolid(destination)) {
                createNavigation(tile, destination, null);
                return;
            }
        }
    }

    private boolean isFleeing(EntityId entity) {
        CreatureAi creatureAi = entityData.getComponent(entity, CreatureAi.class);
        if (creatureAi != null) {
            return creatureAi.getCreatureState() == CreatureState.FLEE;
        }

        return false;
    }

    @Override
    public void imprison(short playerId, EntityId roomId) {
        Health health = entityData.getComponent(entityId, Health.class);
        if (health == null) {
            logger.log(Level.INFO, "Tried to imprison me but I don't have any health left!");
            return;
        }

        // In DK II the health of a knocked out creature is its dying time in ticks
        // (DKII.EXE 0x4B2CE0, 0x4599D0). The Prison takes it in with a tenth of its
        // maximum health on top (0x4E2C70)
        int healthLeft = health.health;
        Unconscious unconscious = entityData.getComponent(entityId, Unconscious.class);
        if (unconscious != null) {
            double timeLeft = gameSettings.get(Variable.MiscVariable.MiscType.CREATURE_DYING_STATE_DURATION_SECONDS).getValue()
                    - (gameTimer.getGameTime() - unconscious.startTime);
            healthLeft = (int) (timeLeft * levelInfo.getLevelData().getGameLevel().getTicksPerSec());
        }
        entityData.setComponent(entityId, new Health(Math.clamp(healthLeft + health.maxHealth / 10, 1, health.maxHealth), health.maxHealth));
        entityData.removeComponent(entityId, Unconscious.class);
        entityData.setComponent(entityId, new CreatureImprisoned(gameTimer.getGameTime(), gameTimer.getGameTime()));
        entityData.setComponent(entityId, new Stored(roomId, AbstractRoomController.ObjectType.PRISONER));

        // Switch the control to the imprisoning player
        Owner owner = entityData.getComponent(entityId, Owner.class);
        entityData.setComponent(entityId, new Owner(owner.ownerId, playerId));

        stateMachine.changeState(CreatureState.IMPRISONED);
    }

    @Override
    public boolean isStateTimeExceeded() {
        double timeSpent = gameTimer.getGameTime() - entityData.getComponent(entityId, CreatureAi.class).stateStartTime;
        double stateTargetTime;

        switch (stateMachine.getCurrentState()) {
            case STUNNED -> {
                // Hmm, this might actually be the level variable, the stun seems to be the time fallen when dropped
                stateTargetTime = entityData.getComponent(entityId, CreatureComponent.class).stunDuration;
            }
            case FALLEN -> {

                // Never shorter than the fall itself, imps for example have no stun duration at all
                stateTargetTime = Math.max(entityData.getComponent(entityId, CreatureComponent.class).stunDuration,
                        getAnimationTime(creature, Creature.AnimationType.FALLBACK));
            }
            case GETTING_UP -> {
                stateTargetTime = getAnimationTime(creature, Creature.AnimationType.GET_UP);
            }
            case ENTERING_DUNGEON -> {
                stateTargetTime = getAnimationTime(creature, Creature.AnimationType.ENTRANCE);
            }
            case MELEE_ATTACK -> {
                stateTargetTime = getAnimationTime(creature, Creature.AnimationType.MELEE_ATTACK);
            }
            case CAST_SPELL -> {
                stateTargetTime = getAnimationTime(creature, Creature.AnimationType.CAST_SPELL);
            }
            case EATING -> {
                stateTargetTime = getAnimationTime(creature, Creature.AnimationType.EATING);
            }
            case CELEBRATING -> {
                stateTargetTime = BIG_WINNER_CELEBRATION_SECONDS;
            }
            case CHICKEN -> {
                stateTargetTime = entityData.getComponent(entityId, CreatureAi.class).stateDuration;
            }
            case FLEE -> {
                // I couldn't find a variable for this, so lets use this for now
                stateTargetTime = gameSettings.get(Variable.MiscVariable.MiscType.IMP_IDLE_DELAY_BEFORE_REEVALUATION_SECONDS).getValue();
            }
            default -> {
                stateTargetTime = Double.MAX_VALUE;
            }
        }

        return stateTargetTime < timeSpent;
    }

    private static double getAnimationTime(Creature creature, Creature.AnimationType animation) {
        // TODO: we could cache and calculate these for all centrally, also include the starting and ending animation
        ArtResource animationResource = creature.getAnimation(animation);
        if (animationResource == null) {
            return 0;
        }

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
                makeHungry(creatureHunger);

                return true;
            }
        }

        return false;
    }

    @Override
    public boolean goToEat() {
        double gameTime = gameTimer.getGameTime();
        CreatureHunger hunger = entityData.getComponent(entityId, CreatureHunger.class);
        if (hunger == null || gameTime - hunger.lastEatAttemptTime
                < EAT_ATTEMPT_INTERVAL_TICKS / (double) levelInfo.getLevelData().getGameLevel().getTicksPerSec()) {
            return false;
        }
        // Native attempts are throttled to one per four ticks
        CreatureHunger updated = new CreatureHunger(hunger.lastEatTime, hunger.amountNeeded);
        updated.lastEatAttemptTime = gameTime;
        entityData.setComponent(entityId, updated);
        return taskManager.assignEatTask(this);
    }

    @Override
    public boolean goGambling() {

        // As in the original, the wish only depends on the gold held, the stake is the money mode one.
        // The big winner has stopped gambling (the Gamble task is not valid for it), so it doesn't wish for it
        return !isWorker() && getOwnerId() >= Player.KEEPER1_ID
                && entityData.getComponent(entityId, CasinoBigWinner.class) == null && getGold() > Math.abs(
                gameSettings.get(Variable.MiscVariable.MiscType.GAMBLING_MODIFY_CREATURE_MONEY_MONEY_PER_SECOND).getValue())
                && taskManager.assignTask(this, Creature.JobType.DRINK);
    }

    @Override
    public boolean goToCollectPay() {
        CreaturePay pay = entityData.getComponent(entityId, CreaturePay.class);
        double gameTime = gameTimer.getGameTime();
        if (pay == null || pay.unpaid <= 0 || pay.attempts >= CreaturePay.MAX_COLLECTION_ATTEMPTS || gameTime < pay.nextAttemptTime) {
            return false;
        }
        if (taskManager.assignCollectPayTask(this)) {
            return true;
        }

        // No gold room with gold in it that we can reach, that is a try that got nothing
        entityData.setComponent(entityId, new CreaturePay(pay.unpaid, pay.attempts + 1, gameTime
                + gameSettings.get(Variable.MiscVariable.MiscType.DELAY_BEFORE_PAY_REEVALUATION_SECONDS).getValue(), false));
        return false;
    }

    @Override
    public void eat(IEntityController target) {

        // Ok, first we destroy the entity we are eating so that nobody else gets it
        Health health = entityData.getComponent(target.getEntityId(), Health.class);
        if (health != null) {
            target.remove();

            if (EAT_INTERRUPTIBLE_STATES.contains(stateMachine.getCurrentState())) {
                stateMachine.changeState(CreatureState.EATING);
            } else if (!isIncapacitated()) {
                sate();
            }
        }
    }

    @Override
    public void sate() {

        // Like in DK II, a creature that isn't hungry gets nothing from a chicken
        if (!isHungry()) {
            return;
        }
        CreatureHunger creatureHunger = entityData.getComponent(entityId, CreatureHunger.class);
        if (creatureHunger.amountNeeded == 1) {

            // We are full
            entityData.setComponent(entityId, new CreatureHunger(gameTimer.getGameTime(), 0));
        } else if (creatureHunger.amountNeeded > 0) {
            entityData.setComponent(entityId, new CreatureHunger(creatureHunger.lastEatTime, creatureHunger.amountNeeded - 1));
        }

        // Increase health
        setDamage(-creature.getAttributes().getHpFromChicken());
    }

    @Override
    public boolean collectWinnings() {
        return entityData.getComponent(entityId, CasinoBigWinner.class) != null && taskManager.assignCollectWinningsTask(this);
    }

    @Override
    public boolean goToDisco() {
        if (isWorker() || getOwnerId() < Player.KEEPER1_ID || creature.getAnimation(Creature.AnimationType.DANCE) == null) {
            return false;
        }

        // Dance where we stand if we are in the disco already
        if (isAtDisco()) {
            return true;
        }
        for (EntityId casino : entityData.findEntities(new FieldFilter<>(Owner.class, "ownerId", getOwnerId()), CasinoDisco.class, Owner.class)) {
            IRoomController room = mapController.getRoomController(casino);
            if (room != null) {
                List<Point> coordinates = new ArrayList<>(room.getRoomInstance().getCoordinates());
                coordinates.removeIf(p -> !room.isTileAccessible(null, p));
                Point destination = Utils.getRandomItem(coordinates);
                return destination != null && !createNavigation(getCreatureCoordinates(), destination, null);
            }
        }

        return false;
    }

    @Override
    public boolean isAtDisco() {
        IRoomController room = mapController.getRoomControllerByCoordinates(getCreatureCoordinates());
        return room != null && room.getOwnerId() == getOwnerId()
                && entityData.getComponent(room.getEntityId(), CasinoDisco.class) != null;
    }

    @Override
    public void makeHungry() {
        CreatureHunger creatureHunger = entityData.getComponent(entityId, CreatureHunger.class);
        makeHungry(creatureHunger);
    }

    private void makeHungry(CreatureHunger creatureHunger) {
        CreatureComponent creatureComponent = entityData.getComponent(entityId, CreatureComponent.class);
        entityData.setComponent(entityId, new CreatureHunger(creatureHunger.lastEatTime, creatureComponent.hungerFill));
    }

    @Override
    public void setWillFight(boolean willFight) {
        if (willFight) {
            entityData.removeComponent(entityId, Peaceful.class);
        } else {
            entityData.setComponent(entityId, new Peaceful());
        }
    }

    @Override
    public int getResearchPerTick() {
        CreatureComponent creatureComponent = entityData.getComponent(entityId, CreatureComponent.class);
        int pointsPerTick = creatureComponent.researchPerTick;

        // A slapped creature researches faster, DK II adds the bonus on the
        // first tick of every second (DKII.EXE 0x4C2DC0)
        int ticksPerSecond = levelInfo.getLevelData().getGameLevel().getTicksPerSec();
        if (isSlapped() && Math.round(gameTimer.getGameTime() * ticksPerSecond) % ticksPerSecond == 0) {
            int bonusPercentage = (int) gameSettings.get(Variable.MiscVariable.MiscType.INCREASED_WORK_RATE_PECENTAGE_FROM_SLAPPING).getValue();
            pointsPerTick += ticksPerSecond * ticksPerSecond * bonusPercentage * pointsPerTick / 100;
        }
        return pointsPerTick;
    }

    @Override
    public int getManufacturePerTick() {
        CreatureComponent creatureComponent = entityData.getComponent(entityId, CreatureComponent.class);
        return creatureComponent.manufacturePerTick;
    }

    @Override
    public void giveObject(IObjectController object) {
        if (object.getType() == AbstractRoomController.ObjectType.GOLD) {

            // Take only what we can carry, the rest stays in the pile
            Gold pile = entityData.getComponent(object.getEntityId(), Gold.class);
            int amount = Math.min(pile.gold, getMaxGold() - getGold());
            addGold(amount);
            if (amount < pile.gold) {
                entityData.setComponent(object.getEntityId(), new Gold(pile.gold - amount, pile.maxGold));
            } else {
                entityData.removeComponent(object.getEntityId(), Gold.class);
                object.remove();
            }
        } else {
            logger.log(Level.WARNING, "Object {0} receiving not specified!", object.getType());
        }
    }

    @Override
    public void stopRecuperating() {
        entityData.removeComponent(entityId, CreatureRecuperating.class);
    }

    @Override
    public boolean isRecuperating() {
        return entityData.getComponent(entityId, CreatureRecuperating.class) != null;
    }

    @Override
    public void turnIntoChicken(double duration) {
        if (creature.getFlags().contains(Creature.CreatureFlag.IS_IMMUNE_TO_CHICKEN)) {
            return;
        }

        // Drop whatever we were doing, unless we are not in control of ourselves
        if (entityData.getComponent(entityId, Possessed.class) == null
                && CHICKEN_INTERRUPTIBLE_STATES.contains(stateMachine.getCurrentState())) {
            stateMachine.changeState(CreatureState.CHICKEN);
            entityData.setComponent(entityId, new CreatureAi(gameTimer.getGameTime(), CreatureState.CHICKEN,
                    creature.getId(), null, duration));
        }
    }

    private boolean isChicken(EntityId entity) {
        CreatureAi creatureAi = entityData.getComponent(entity, CreatureAi.class);
        return creatureAi != null && creatureAi.getCreatureState() == CreatureState.CHICKEN;
    }

    @Override
    public void setPossession(boolean possessed) {
        if (possessed) {
            startPossession();
        } else {
            endPossession();
        }
    }

    @Override
    public void setPossessedMovement(Vector2f direction, float rotation, float pitch, byte speedMode) {
        // Use the level scaled speeds, same as the AI movement does
        Creature.Attributes attributes = creature.getAttributes();
        CreatureComponent creatureComponent = entityData.getComponent(entityId, CreatureComponent.class);
        float walkSpeed = creatureComponent != null ? creatureComponent.speed : attributes.getSpeed();
        float runSpeed = creatureComponent != null ? creatureComponent.runSpeed : attributes.getRunSpeed();
        float speed = switch (speedMode) {
            case PossessedMovement.SPEED_RUN -> runSpeed;
            case PossessedMovement.SPEED_CREEP -> attributes.getShuffleSpeed();
            default -> walkSpeed;
        };
        // A praying creature doesn't move, but still turns
        PossessedAbility possessedAbility = entityData.getComponent(entityId, PossessedAbility.class);
        boolean praying = possessedAbility != null && possessedAbility.ability == Creature.SpecialAbility.PRAY;
        Vector2f normalizedDirection = direction.lengthSquared() > 0 && !praying ? direction.normalize() : new Vector2f();
        if (normalizedDirection.lengthSquared() > 0) {

            // Backing up and strafing are slower than walking forward, relative to the facing
            Vector2f facing = new Vector2f(FastMath.sin(rotation), FastMath.cos(rotation));
            float forward = normalizedDirection.dot(facing);
            float sideways = normalizedDirection.x * facing.y - normalizedDirection.y * facing.x;
            speed *= new Vector2f(forward * (forward < 0 ? POSSESSED_BACKWARD_SPEED_FACTOR : 1f),
                    sideways * POSSESSED_STRAFE_SPEED_FACTOR).length();
        }
        entityData.setComponent(entityId, new PossessedMovement(normalizedDirection, rotation, pitch, speed * SPEED_MULTIPLIER));
    }

    @Override
    public void setPossessedAttack(boolean attacking, int spell) {
        Possessed possessed = entityData.getComponent(entityId, Possessed.class);
        if (possessed == null) {
            return;
        }
        if (attacking) {
            entityData.setComponent(entityId, new Possessed(possessed.manaDrain, possessed.manaCheckTime, true, gameTimer.getGameTime(), spell));
        } else if (possessed.isAttackPending() && !possessed.attackGroup) {

            // A click shorter than a logic tick still gets its one swing
            double pressTime = hasSwungSince(possessed) ? Double.NaN : possessed.attackPressTime;
            entityData.setComponent(entityId, new Possessed(possessed.manaDrain, possessed.manaCheckTime, false, pressTime, possessed.attackSpell));
        }
    }

    private boolean hasSwungSince(Possessed possessed) {
        Attack attack;
        if (possessed.attackSpell > 0) {
            EntityId spellEntity = getPossessedSpell(possessed.attackSpell);
            attack = spellEntity != null ? entityData.getComponent(spellEntity, CreatureSpell.class) : null;
        } else {
            attack = entityData.getComponent(entityId, CreatureMeleeAttack.class);
        }
        return attack == null || (attack.attactStartTime != null && attack.attactStartTime >= possessed.attackPressTime);
    }

    @Override
    public void addPossessedFollower() {
        Possessed possessed = entityData.getComponent(entityId, Possessed.class);
        if (possessed != null) {
            entityData.setComponent(entityId, new Possessed(possessed.manaDrain, possessed.manaCheckTime, false, gameTimer.getGameTime(), true));
        }
    }

    @Override
    public void executePossessedAttack() {

        // A bat can't attack or cast (DK II stops it at the keys, 0x4100F0)
        PossessedAbility possessedAbility = entityData.getComponent(entityId, PossessedAbility.class);
        if (possessedAbility != null && possessedAbility.ability == Creature.SpecialAbility.TURN_TO_BAT) {
            return;
        }

        Possessed possessed = entityData.getComponent(entityId, Possessed.class);
        if (possessed != null && possessed.attackSpell > 0) {
            castPossessedSpell(possessed.attackSpell);
            return;
        }

        CreatureMeleeAttack meleeAttack = entityData.getComponent(entityId, CreatureMeleeAttack.class);
        Position position = entityData.getComponent(entityId, Position.class);
        if (position != null && possessed != null && possessed.attackGroup) {
            addFollowerInView(position);
            return;
        }
        if (meleeAttack == null || position == null) {
            return;
        }

        // The creature fights differently in first person, see the first person stats
        CreatureMeleeAttack firstPersonAttack = new CreatureMeleeAttack(meleeAttack);
        firstPersonAttack.damage = firstPersonAttack.damage * getFirstPersonPercentage(Variable.CreatureStats.StatType.MELEE_DAMAGE) / 100;
        firstPersonAttack.rechargeTime = firstPersonAttack.rechargeTime * getFirstPersonPercentage(Variable.CreatureStats.StatType.MELEE_RECHARGE_TIME_SECONDS) / 100
                * getFirstPersonRechargeModifyPercentage() / 100;
        if (!isAttackRecharged(firstPersonAttack)) {
            return;
        }

        // Nothing to hit, a worker digs but others don't swing at all
        EntityId target = getPossessedAttackTarget(position);
        if (target != null) {
            hitWithMelee(target, meleeAttack, firstPersonAttack.damage);
            revealTo(target);

            // The creature we have hypnotised attacks what we hit (0x514830)
            EntityId hypnotised = getHypnotised();
            if (hypnotised != null) {
                entityData.setComponent(hypnotised, new AttackTarget(target));
            }
        } else if (isWorker() && digTileInFront(position)) {
            entityData.setComponent(entityId, new CreatureMeleeAttack(meleeAttack, gameTimer.getGameTime()));
        } else {
            return;
        }
        entityData.setComponent(entityId, new CreatureViewState(creature.getCreatureId(), gameTimer.getGameTime(), Creature.AnimationType.MELEE_ATTACK));
    }

    @Override
    public void usePossessedAbility(int ability) {
        Creature.SpecialAbility specialAbility = switch (ability) {
            case 1 -> creature.getFirstPersonSpecialAbility1();
            case 2 -> creature.getFirstPersonSpecialAbility2();
            case POSSESSED_SNIPER_MODE_KEY -> hasFirstPersonAbility(Creature.SpecialAbility.SNIPER_MODE)
                    ? Creature.SpecialAbility.SNIPER_MODE : Creature.SpecialAbility.NONE;
            default -> Creature.SpecialAbility.NONE;
        };
        if (specialAbility == Creature.SpecialAbility.TURN_TO_BAT || specialAbility == Creature.SpecialAbility.PRAY
                || specialAbility == Creature.SpecialAbility.SNIPER_MODE) {
            togglePossessedAbility(specialAbility);
        } else if (specialAbility == Creature.SpecialAbility.PICK_UP_WORKERS) {
            pickUpOrThrowWorker();
        } else if (specialAbility == Creature.SpecialAbility.HYPNOTISE) {
            hypnotiseOrRelease();
        } else if (specialAbility == Creature.SpecialAbility.DISGUISE_N_STEAL) {
            disguiseOrReveal();
        } else if (specialAbility == Creature.SpecialAbility.FEAR_ATTACK) {
            frightenTarget();
        }
    }

    private boolean hasFirstPersonAbility(Creature.SpecialAbility ability) {
        return creature.getFirstPersonSpecialAbility1() == ability || creature.getFirstPersonSpecialAbility2() == ability;
    }

    /**
     * The damage of a shot the creature fires. While the possessed creature is
     * in sniper mode its shots do double damage (0x4AC420).
     *
     * @param creatureSpell the spell that makes the shot
     * @return the shot's data 1, its damage if it hits things
     */
    private int getShotData1(toniarts.openkeeper.tools.convert.map.CreatureSpell creatureSpell) {
        PossessedAbility possessedAbility = entityData.getComponent(entityId, PossessedAbility.class);
        if (possessedAbility != null && possessedAbility.ability == Creature.SpecialAbility.SNIPER_MODE
                && levelInfo.getLevelData().getShotById(creatureSpell.getShotTypeId()).getDamageType() == Shot.DamageType.HIT_THING) {
            return creatureSpell.getShotData1() * 2;
        }

        return creatureSpell.getShotData1();
    }

    /**
     * Frightens what our melee attack would hit for 20 game ticks (0x40FA90,
     * 0x514470). Using it again starts the time over.
     */
    private void frightenTarget() {
        EntityId target = getPossessedAttackTarget(entityData.getComponent(entityId, Position.class));
        if (target != null) {
            double duration = FEAR_ATTACK_TICKS / (double) levelInfo.getLevelData().getGameLevel().getTicksPerSec();
            entityData.setComponent(target, new Frightened(entityId, FEAR_ATTACK_SEVERITY, gameTimer.getGameTime() + duration));
        }
    }

    /**
     * Disguises us as a creature of the keeper whose land we stand on, or
     * drops the disguise (0x517270, 0x48EE90). It can't be done on our own or
     * neutral land, or while a creature of that keeper or of their allies sees
     * us: we are within its sight distance and no solid tile is in between.
     */
    private void disguiseOrReveal() {
        if (entityData.getComponent(entityId, Disguised.class) != null) {
            entityData.removeComponent(entityId, Disguised.class);
            return;
        }

        Vector3f position = getPosition();
        IMapTileInformation tile = mapController.getMapData().getTile(WorldUtils.vectorToPoint(position));
        Keeper keeper = tile != null ? levelInfo.getPlayer(tile.getOwnerId()) : null;
        if (keeper == null || keeper.getId() == getOwnerId() || keeper.getId() == Player.NEUTRAL_PLAYER_ID) {
            return;
        }
        for (EntityId entity : entityData.findEntities(null, CreatureComponent.class, Owner.class, Senses.class)) {
            if (keeper.isAlly(entityData.getComponent(entity, Owner.class).ownerId)
                    && getDistanceToCreature(entity) < entityData.getComponent(entity, Senses.class).distanceCanSee
                    && isInSight(position, entity)) {
                return;
            }
        }

        entityData.setComponent(entityId, new Disguised(keeper.getId()));
    }

    /**
     * Drops our disguise when we hit or cast at a creature of the keeper we
     * look like, or of their allies (0x514830)
     *
     * @param target what we hit or cast at
     */
    private void revealTo(EntityId target) {
        Disguised disguised = entityData.getComponent(entityId, Disguised.class);
        Owner owner = entityData.getComponent(target, Owner.class);
        Keeper keeper = owner != null ? levelInfo.getPlayer(owner.ownerId) : null;
        if (disguised != null && keeper != null && keeper.isAlly(disguised.ownerId)) {
            entityData.removeComponent(entityId, Disguised.class);
        }
    }

    /**
     * Hypnotises the nearest enemy creature in sight within 60 degrees of the
     * view, or lets go of the one we have hypnotised (0x5163C0). It joins our
     * keeper and follows us (0x461980), our keeper pays
     * HYPNOTISM_MANA_DRAIN_PER_SECOND for it (0x4C10E0). If the nearest
     * creature is immune to turncoat, nothing happens.
     */
    private void hypnotiseOrRelease() {
        EntityId hypnotised = getHypnotised();
        if (hypnotised != null) {
            releaseHypnotised(hypnotised);
            return;
        }

        Position position = entityData.getComponent(entityId, Position.class);
        EntityId nearestCreature = null;
        float nearestDistance = Float.MAX_VALUE;
        for (EntityId entity : entityData.findEntities(null, CreatureComponent.class, Position.class)) {
            float distance = getDistanceToCreature(entity);
            if (distance < nearestDistance && canBeHypnotised(entity) && isInFront(position, entity) && isInSight(position.position, entity)) {
                nearestDistance = distance;
                nearestCreature = entity;
            }
        }
        if (nearestCreature == null || getCreature(nearestCreature).getFlags().contains(Creature.CreatureFlag.IS_IMMUNE_TO_TURNCOAT)) {
            return;
        }

        entityData.setComponent(nearestCreature, new Hypnotised(entityId, entityData.getComponent(nearestCreature, Owner.class).ownerId));
        entityData.setComponent(nearestCreature, new Owner(getOwnerId(), getOwnerId()));
        entityData.removeComponent(nearestCreature, AttackTarget.class);
        Variable.MiscVariable manaDrain = gameSettings.get(Variable.MiscVariable.MiscType.HYPNOTISM_MANA_DRAIN_PER_SECOND);
        if (manaDrain != null && manaDrain.getValue() > 0) {
            entityData.setComponent(nearestCreature, new Mana(-(int) manaDrain.getValue()));
        }

        // This runs on the render thread, the creature starts following us on its own tick
        entityData.setComponent(nearestCreature, new FollowTarget(entityId));
    }

    private boolean canBeHypnotised(EntityId entity) {
        return !entityId.equals(entity) && creaturesController.isValidEntity(entity) && isEnemy(entity) && !isIncapacitated(entity)
                && entityData.getComponent(entity, Possessed.class) == null
                && getCreature(entity).getFlags().contains(Creature.CreatureFlag.CAN_BE_HYPNOTIZED);
    }

    private Creature getCreature(EntityId entity) {
        return levelInfo.getLevelData().getCreature(entityData.getComponent(entity, CreatureComponent.class).creatureId);
    }

    /**
     * Is there no solid tile on the line from us to the entity (0x4512F0)
     */
    private boolean isInSight(Vector3f position, EntityId entity) {
        Vector3f target = getPosition(entityData, entity);
        int steps = (int) FastMath.ceil(position.distance(target) / (WorldUtils.TILE_WIDTH / 8));
        for (int i = 1; i <= steps; i++) {
            Point tile = WorldUtils.vectorToPoint(position.clone().interpolateLocal(target, i / (float) steps));
            if (mapController.getMapData().getTile(tile) == null || mapController.isSolid(tile)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Gets the creature we have hypnotised
     *
     * @return the creature, {@code null} if there is none
     */
    private EntityId getHypnotised() {
        return entityData.findEntity(new FieldFilter<>(Hypnotised.class, "entity", entityId), Hypnotised.class);
    }

    /**
     * Lets go of the creature we have hypnotised (0x5163C0, 0x514590). It goes
     * back to its owner and stops fighting (0x45EF50).
     *
     * @param hypnotised the creature we have hypnotised
     */
    private void releaseHypnotised(EntityId hypnotised) {
        short ownerId = entityData.getComponent(hypnotised, Hypnotised.class).ownerId;
        entityData.removeComponent(hypnotised, Hypnotised.class);
        entityData.removeComponent(hypnotised, Mana.class);
        entityData.removeComponent(hypnotised, AttackTarget.class);
        entityData.setComponent(hypnotised, new Owner(ownerId, ownerId));

        // Without the follow target the creature goes back to idle on its own tick
        FollowTarget followTarget = entityData.getComponent(hypnotised, FollowTarget.class);
        if (followTarget != null && followTarget.entityId.equals(entityId)) {
            entityData.removeComponent(hypnotised, FollowTarget.class);
        }
    }

    @Override
    public EntityId getHypnotiser() {
        Hypnotised hypnotised = entityData.getComponent(entityId, Hypnotised.class);
        return hypnotised != null ? hypnotised.entity : null;
    }

    /**
     * Picks up the nearest worker of our keeper in front of us, or throws the
     * one we carry (0x516EC0). The worker has to be within a tile and 60
     * degrees of the view. It leaves its work and its AI while it is carried.
     */
    private void pickUpOrThrowWorker() {
        PossessedAbility possessedAbility = entityData.getComponent(entityId, PossessedAbility.class);
        if (possessedAbility != null && possessedAbility.entity != null) {
            entityData.removeComponent(entityId, PossessedAbility.class);
            releaseWorker(possessedAbility.entity, true);
            return;
        }

        Position position = entityData.getComponent(entityId, Position.class);
        EntityId nearestWorker = null;
        float nearestDistance = POSSESSED_PICK_UP_REACH;
        for (Point tile : WorldUtils.getSurroundingTiles(mapController.getMapData(), WorldUtils.vectorToPoint(position.position), true)) {
            for (EntityId entity : entityPositionLookup.getEntitiesInLocation(tile)) {
                float distance = getDistanceToCreature(entity);
                if (distance < nearestDistance && isOwnWorker(entity) && isInFront(position, entity)) {
                    nearestDistance = distance;
                    nearestWorker = entity;
                }
            }
        }
        if (nearestWorker == null) {
            return;
        }

        creaturesController.createController(nearestWorker).unassingCurrentTask();
        entityData.removeComponent(nearestWorker, CreatureAi.class);
        entityData.removeComponent(nearestWorker, Navigation.class);
        CreatureViewState viewState = entityData.getComponent(nearestWorker, CreatureViewState.class);
        if (viewState != null) {
            entityData.setComponent(nearestWorker, new CreatureViewState(viewState.creatureId, gameTimer.getGameTime(), Creature.AnimationType.IN_HAND));
        }
        entityData.setComponent(entityId, new PossessedAbility(Creature.SpecialAbility.PICK_UP_WORKERS, gameTimer.getGameTime(), nearestWorker));
    }

    private boolean isOwnWorker(EntityId entity) {
        Owner owner = entityData.getComponent(entity, Owner.class);
        return !entityId.equals(entity) && creaturesController.isValidEntity(entity) && owner != null
                && owner.ownerId == getOwnerId() && !isIncapacitated(entity) && creaturesController.createController(entity).isWorker();
    }

    /**
     * Lets go of the worker we carry, from where we hold it. It flies or falls
     * down and takes damage as it lands (0x4833C0).
     *
     * @param worker the worker we carry
     * @param thrown whether we throw it forward or just drop it
     */
    private void releaseWorker(EntityId worker, boolean thrown) {

        // The keeper's hand may have taken it, or it has died
        Position position = entityData.getComponent(entityId, Position.class);
        if (position == null || entityData.getComponent(worker, Position.class) == null) {
            return;
        }

        Vector3f direction = new Vector3f(FastMath.sin(position.rotation), 0, FastMath.cos(position.rotation));
        Vector3f start = position.position.add(direction.mult(POSSESSED_CARRY_DISTANCE));
        start.y += creature.getAttributes().getHeight();
        Vector3f velocity = thrown ? direction.multLocal(THROWN_WORKER_SPEED).setY(THROWN_WORKER_LIFT) : new Vector3f();

        entityData.setComponent(worker, new Position(position.rotation, start));
        entityData.setComponent(worker, new CreatureFall(velocity, THROWN_WORKER_DAMAGE));
    }

    /**
     * Turns an ability that stays on, such as the Vampire's bat form, on or
     * off. A bat flies (0x517190, 0x48ED40). A praying creature stays still
     * and heals (0x517220). Sniper mode is a flag on the creature (0x5171D0).
     *
     * @param ability the ability to turn on or off
     */
    private void togglePossessedAbility(Creature.SpecialAbility ability) {
        PossessedAbility possessedAbility = entityData.getComponent(entityId, PossessedAbility.class);
        boolean on = possessedAbility == null || possessedAbility.ability != ability;
        if (on) {
            entityData.setComponent(entityId, new PossessedAbility(ability, gameTimer.getGameTime()));
        } else {
            entityData.removeComponent(entityId, PossessedAbility.class);
        }

        Mobile mobile = entityData.getComponent(entityId, Mobile.class);
        if (ability == Creature.SpecialAbility.TURN_TO_BAT && mobile != null) {
            entityData.setComponent(entityId, new Mobile(on || creature.getFlags().contains(Creature.CreatureFlag.CAN_FLY),
                    mobile.canWalkOnWater, mobile.canWalkOnLava, mobile.maxSpeed));
        }
    }

    /**
     * Casts a spell of the possessed creature, if it has recharged
     * (0x514830). It is aimed at the creature closest to where the possessed
     * creature looks, or straight ahead if there is none.
     *
     * @param spell 1 to 3, the creature's spell in the order the creature has
     * them
     */
    private void castPossessedSpell(int spell) {
        EntityId spellEntity = getPossessedSpell(spell);
        Position position = entityData.getComponent(entityId, Position.class);
        if (spellEntity == null || position == null) {
            return;
        }

        // In first person the recharge time is scaled as the melee attack's is (0x4F7AB0)
        CreatureSpell attack = entityData.getComponent(spellEntity, CreatureSpell.class);
        CreatureSpell firstPersonAttack = new CreatureSpell(attack.creatureSpellId, entityId, attack.rechargeTime
                * getFirstPersonPercentage(Variable.CreatureStats.StatType.MELEE_RECHARGE_TIME_SECONDS) / 100
                * getFirstPersonRechargeModifyPercentage() / 100, attack.range);
        firstPersonAttack.attactStartTime = attack.attactStartTime;
        toniarts.openkeeper.tools.convert.map.CreatureSpell creatureSpell = levelInfo.getLevelData().getCreatureSpellById(attack.creatureSpellId);
        Set<CreatureSpellFlag> flags = creatureSpell.getFlags();
        if (!isAttackRecharged(firstPersonAttack)) {
            return;
        }

        // The Imp's teleport is no cast, and it recharges even with no heart to go to
        if (flags.contains(CreatureSpellFlag.IMP_TELEPORT)) {
            teleportToDungeonHeart(position);
            entityData.setComponent(spellEntity, new CreatureSpell(attack, gameTimer.getGameTime()));
            return;
        }

        // Nor is the Imp's haste. It changes the speed with the spell's values and the same speed timer as a slap,
        // so a speed change that is still running is left as it is, but the spell recharges all the same (0x48CAD0).
        // A speed-up shows the speed blur for as many game ticks as the percentage (0x514830)
        if (flags.contains(CreatureSpellFlag.IMP_HASTE)) {
            shotsController.createShot(creatureSpell.getShotTypeId(), creatureSpell.getShotData1(), creatureSpell.getShotData2(), getOwnerId(),
                    getPosition(), entityId, entityId);
            entityData.setComponent(spellEntity, new CreatureSpell(attack, gameTimer.getGameTime()));
            if (creatureSpell.getShotData1() > 100) {
                entityData.setComponent(entityId, new SpeedBlur(gameTimer.getGameTime()
                        + creatureSpell.getShotData1() / (double) levelInfo.getLevelData().getGameLevel().getTicksPerSec()));
            }
            return;
        }

        EntityId target = flags.contains(CreatureSpellFlag.CAST_ON_SELF) ? entityId
                : getPossessedSpellTarget(position, flags.contains(CreatureSpellFlag.CREATURE));
        if (target != null) {
            entityData.setComponent(entityId, new CastShot(attack.creatureSpellId, target, attack.range,
                    gameTimer.getGameTime() + getShotDelay(attack.creatureSpellId)));
            releaseCastShot();
            revealTo(target);
        } else {
            PossessedMovement movement = entityData.getComponent(entityId, PossessedMovement.class);
            float pitch = movement != null ? movement.pitch : 0;
            Vector3f ahead = getViewDirection(position.rotation, pitch).multLocal(attack.range);
            Vector3f start = getShotPosition(attack.creatureSpellId, position.position.add(ahead));
            if (!shotsController.fireShot(creatureSpell.getShotTypeId(), getShotData1(creatureSpell), creatureSpell.getShotData2(),
                    getOwnerId(), start, start.add(ahead), entityId, attack.range)) {
                return;
            }
        }
        entityData.setComponent(spellEntity, new CreatureSpell(attack, gameTimer.getGameTime()));
        entityData.setComponent(entityId, new CreatureViewState(creature.getCreatureId(), gameTimer.getGameTime(), Creature.AnimationType.CAST_SPELL));
    }

    /**
     * Teleports the possessed Imp to the middle of its keeper's dungeon heart,
     * the one closest to it in DK II (0x514830, 0x4C61C0). Where it looks
     * doesn't matter.
     *
     * @param position the position of the Imp
     */
    private void teleportToDungeonHeart(Position position) {
        Keeper keeper = levelInfo.getPlayer(getOwnerId());
        Point heart = keeper != null ? keeper.getDungeonHeartLocation() : null;
        if (heart != null) {
            Vector3f destination = WorldUtils.pointToVector3f(heart);
            gameEventService.emitEffect(IMP_TELEPORT_OUT_EFFECT, position.position, 0);
            gameEventService.emitEffect(IMP_TELEPORT_IN_EFFECT, destination, 0);
            entityData.setComponent(entityId, new Position(position.rotation, destination));
        }
    }

    /**
     * The direction a possessed creature looks in
     *
     * @param rotation the facing of the creature
     * @param pitch the pitch of the view, positive looking up, in radians
     * @return the unit vector of the view
     */
    private static Vector3f getViewDirection(float rotation, float pitch) {
        return new Vector3f(FastMath.sin(rotation) * FastMath.cos(pitch), FastMath.sin(pitch),
                FastMath.cos(rotation) * FastMath.cos(pitch));
    }

    /**
     * Finds the possessed creature's spell, the creature only has the spells
     * of its level
     *
     * @param spell 1 to 3, the creature's spell in the order the creature has
     * them
     * @return the creature spell entity, {@code null} if the creature doesn't
     * have the spell
     */
    private EntityId getPossessedSpell(int spell) {
        if (spell < 1 || spell > creature.getSpells().size()) {
            return null;
        }

        short creatureSpellId = creature.getSpells().get(spell - 1).getCreatureSpellId();
        CreatureSpells creatureSpells = entityData.getComponent(entityId, CreatureSpells.class);
        if (creatureSpells == null) {
            return null;
        }
        for (EntityId spellEntity : creatureSpells.creatureSpells) {
            if (entityData.getComponent(spellEntity, CreatureSpell.class).creatureSpellId == creatureSpellId) {
                return spellEntity;
            }
        }

        return null;
    }

    /**
     * Finds what a possessed creature's spell aims at (0x515520): the creature
     * closest to where the possessed creature looks. It has to be within the
     * first person accuracy, which DK II compares to one minus the cosine of
     * the angle off the view as FIRST_PERSON_ACCURACY_ANGLE_DEGREES / 720. So
     * the 45 of the levels lets through about 20 degrees. The view includes
     * its pitch, and DK II also wants a line of sight to the creature
     * (0x4512F0), here that is no solid tile in between. Doors are not
     * checked.
     *
     * @param position the position of the possessed creature
     * @param ownCreatures whether the spell is for our own creatures rather
     * than the enemy's
     * @return the target, {@code null} if there is nothing to aim at
     */
    private EntityId getPossessedSpellTarget(Position position, boolean ownCreatures) {
        Variable.MiscVariable accuracy = gameSettings.get(Variable.MiscVariable.MiscType.FIRST_PERSON_ACCURACY_ANGLE_DEGREES);
        float bestOffView = (accuracy != null ? accuracy.getValue() : 45) / 720f;
        EntityId bestTarget = null;
        PossessedMovement movement = entityData.getComponent(entityId, PossessedMovement.class);
        Vector3f view = getViewDirection(position.rotation, movement != null ? movement.pitch : 0);
        Point center = WorldUtils.vectorToPoint(position.position);
        int reach = (int) FastMath.ceil(FastMath.sqrt(POSSESSED_SPELL_REACH_SQUARED));
        for (int x = center.x - reach; x <= center.x + reach; x++) {
            for (int y = center.y - reach; y <= center.y + reach; y++) {
                IMapTileInformation tile = mapController.getMapData().getTile(x, y);
                if (tile == null) {
                    continue;
                }

                for (EntityId entity : entityPositionLookup.getEntitiesInLocation(tile)) {
                    Owner owner = entityData.getComponent(entity, Owner.class);
                    Vector3f targetPosition = getPosition(entityData, entity);
                    if (owner == null || targetPosition == null || entityId.equals(entity) || !creaturesController.isValidEntity(entity)
                            || isIncapacitated(entity) || (owner.ownerId == getOwnerId()) != ownCreatures) {
                        continue;
                    }

                    float dx = targetPosition.x - position.position.x;
                    float dz = targetPosition.z - position.position.z;
                    float distanceSquared = dx * dx + dz * dz;
                    if (distanceSquared >= POSSESSED_SPELL_REACH_SQUARED || distanceSquared == 0) {
                        continue;
                    }

                    float dy = targetPosition.y - position.position.y;
                    float offView = 1 - (view.x * dx + view.y * dy + view.z * dz) / FastMath.sqrt(distanceSquared + dy * dy);
                    if (offView < bestOffView && hasLineOfSight(position.position, targetPosition)) {
                        bestOffView = offView;
                        bestTarget = entity;
                    }
                }
            }
        }

        return bestTarget;
    }

    /**
     * Whether there are no solid tiles between two points on the map. The
     * tiles the points are on don't count.
     */
    private boolean hasLineOfSight(Vector3f from, Vector3f to) {
        Point startTile = WorldUtils.vectorToPoint(from);
        Point endTile = WorldUtils.vectorToPoint(to);
        float dx = to.x - from.x;
        float dz = to.z - from.z;
        int steps = (int) FastMath.ceil(Math.max(Math.abs(dx), Math.abs(dz)) / 0.25f);
        for (int i = 1; i < steps; i++) {
            Point tile = WorldUtils.vectorToPoint(from.x + dx * i / steps, from.z + dz * i / steps);
            if (!tile.equals(startTile) && !tile.equals(endTile) && mapController.isSolid(tile)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Finds what the possessed creature hits: the nearest target on the tiles
     * around it that is within the cone in front of it. The melee range of the
     * creature doesn't matter here.
     *
     * @param position the position of the possessed creature
     * @return the target to hit, or {@code null} if there is nothing to hit
     */
    private EntityId getPossessedAttackTarget(Position position) {
        EntityId nearestTarget = null;
        float nearestDistance = Float.MAX_VALUE;
        for (Point tile : WorldUtils.getSurroundingTiles(mapController.getMapData(), WorldUtils.vectorToPoint(position.position), true)) {
            for (EntityId entity : entityPositionLookup.getEntitiesInLocation(tile)) {
                float distance = getDistanceToCreature(entity);
                if (distance < nearestDistance && isPossessedAttackTarget(entity) && isInFront(position, entity)) {
                    nearestDistance = distance;
                    nearestTarget = entity;
                }
            }
        }

        return nearestTarget;
    }

    /**
     * Can the possessed creature hit the entity with its melee attack. Any
     * creature but our own, also neutral and allied ones, and the doors and
     * the traps that are not ours. A hidden trap is not hit until it shows.
     * The Dungeon Heart can't be hit, it is not among the objects the original
     * game lets a possessed creature hit.
     *
     * @param entity the entity in reach
     * @return true if the entity is something to hit
     */
    private boolean isPossessedAttackTarget(EntityId entity) {
        Owner owner = entityData.getComponent(entity, Owner.class);
        if (entityId.equals(entity) || owner == null || owner.ownerId == getOwnerId()) {
            return false;
        }
        return DoorAttackTarget.isBreakable(entityData, entity)
                || TrapAttackTarget.isAttackable(entityData, entity)
                || (creaturesController.isValidEntity(entity) && !isIncapacitated(entity));
    }

    private boolean isInFront(Position position, EntityId entity) {
        return getFacing(position, entity) >= POSSESSED_ATTACK_MIN_FACING;
    }

    /**
     * How straight the creature looks at the entity
     *
     * @return the cosine of the angle between where the creature looks and
     * where the entity is, 1 if it is right on the entity
     */
    private float getFacing(Position position, EntityId entity) {
        Vector3f targetPosition = getPosition(entityData, entity);
        float dx = targetPosition.x - position.position.x;
        float dz = targetPosition.z - position.position.z;
        float length = FastMath.sqrt(dx * dx + dz * dz);
        if (length == 0) {
            return 1;
        }

        return (FastMath.sin(position.rotation) * dx + FastMath.cos(position.rotation) * dz) / length;
    }

    /**
     * A possessed worker digs the tile next to it in the direction it is
     * facing. Dirt, gold, gems and walls can be dug, also our own walls.
     *
     * @param position the position of the possessed worker
     * @return true if there was something to dig
     */
    private boolean digTileInFront(Position position) {
        float x = FastMath.sin(position.rotation);
        float z = FastMath.cos(position.rotation);
        Point tile = WorldUtils.vectorToPoint(position.position);
        if (Math.abs(x) >= Math.abs(z)) {
            tile = new Point(tile.x + (x > 0 ? 1 : -1), tile.y);
        } else {
            tile = new Point(tile.x, tile.y + (z > 0 ? 1 : -1));
        }

        IMapTileInformation mapTile = mapController.getMapData().getTile(tile);
        if (mapTile == null) {
            return false;
        }
        Set<Terrain.TerrainFlag> flags = mapController.getTerrain(mapTile).getFlags();
        if (!flags.contains(Terrain.TerrainFlag.SOLID) || (flags.contains(Terrain.TerrainFlag.IMPENETRABLE) && mapTile.getGold() <= 0)) {
            return false;
        }

        addGold(mapController.damageTile(tile, getOwnerId(), this));
        return true;
    }

    /**
     * Our own creature that the possessed creature looks at joins its group
     * and follows it. It is the one closest to where the creature looks, at
     * most FIRST_PERSON_ACCURACY_ANGLE_DEGREES off in the way the original game
     * measures it, and closer than 7 tiles. The group has room for
     * MAXIMUM_GROUP_NUMBER_OF_FOLLOWERS creatures.
     *
     * @param position the position of the possessed creature
     */
    private void addFollowerInView(Position position) {
        if (getPossessedFollowers().size() >= getMiscVariable(Variable.MiscVariable.MiscType.MAXIMUM_GROUP_NUMBER_OF_FOLLOWERS, 8)) {
            return;
        }

        // The original compares the angle, in 2048ths of a circle, to one minus the cosine, in 4096ths
        float bestFacing = 1 - getMiscVariable(Variable.MiscVariable.MiscType.FIRST_PERSON_ACCURACY_ANGLE_DEGREES, 45) / 720f;
        EntityId follower = null;
        Point tile = WorldUtils.vectorToPoint(position.position);
        for (int x = tile.x - POSSESSED_GROUP_RANGE; x <= tile.x + POSSESSED_GROUP_RANGE; x++) {
            for (int y = tile.y - POSSESSED_GROUP_RANGE; y <= tile.y + POSSESSED_GROUP_RANGE; y++) {
                if (mapController.getMapData().getTile(x, y) == null) {
                    continue;
                }
                for (EntityId entity : entityPositionLookup.getEntitiesInLocation(new Point(x, y))) {
                    float facing = getFacing(position, entity);
                    if (facing > bestFacing && canJoinPossessedGroup(entity) && getDistanceToCreature(entity) < POSSESSED_GROUP_RANGE) {
                        bestFacing = facing;
                        follower = entity;
                    }
                }
            }
        }
        if (follower == null) {
            return;
        }

        ICreatureController creatureController = creaturesController.createController(follower);
        creatureController.unassingCurrentTask();
        creatureController.setFollowTarget(entityId);
        creatureController.getStateMachine().changeState(CreatureState.FOLLOW);
    }

    /**
     * Can the creature join the group of the possessed creature. Our own
     * creatures can, but not the ones already in a group, the imprisoned or
     * tortured ones, the incapacitated ones or the Horny Reaper.
     */
    private boolean canJoinPossessedGroup(EntityId entity) {
        Owner owner = entityData.getComponent(entity, Owner.class);
        CreatureComponent creatureComponent = entityData.getComponent(entity, CreatureComponent.class);
        return !entityId.equals(entity) && owner != null && owner.ownerId == getOwnerId() && creatureComponent != null
                && creaturesController.isValidEntity(entity) && !isIncapacitated(entity)
                && entityData.getComponent(entity, FollowTarget.class) == null
                && entityData.getComponent(entity, CreatureImprisoned.class) == null
                && entityData.getComponent(entity, CreatureTortured.class) == null
                && !levelInfo.getLevelData().getCreature(creatureComponent.creatureId).getFlags().contains(Creature.CreatureFlag.IS_HORNY);
    }

    /**
     * The group of the possessed creature. The creature it has hypnotised is
     * not in it, that one has its own slot (0x5163C0, 0x461980).
     */
    private Set<EntityId> getPossessedFollowers() {
        Set<EntityId> followers = new HashSet<>(entityData.findEntities(new FieldFilter<>(FollowTarget.class, "entityId", entityId), FollowTarget.class));
        followers.removeIf(follower -> entityData.getComponent(follower, Hypnotised.class) != null);
        return followers;
    }

    private int getMiscVariable(Variable.MiscVariable.MiscType type, int defaultValue) {
        Variable.MiscVariable variable = gameSettings.get(type);
        return variable != null ? (int) variable.getValue() : defaultValue;
    }

    @Override
    public boolean isInPossessedGroup() {
        FollowTarget followTarget = entityData.getComponent(entityId, FollowTarget.class);
        return followTarget != null && entityData.getComponent(followTarget.entityId, Possessed.class) != null
                && entityData.getComponent(entityId, Hypnotised.class) == null;
    }

    private void startPossession() {
        if (entityData.getComponent(entityId, Possessed.class) != null) {
            return;
        }

        CreatureComponent creatureComponent = entityData.getComponent(entityId, CreatureComponent.class);
        int manaDrain = creatureComponent != null ? creatureComponent.posessionManaCost : 0;
        if (isPossessionUpgraded()) {
            manaDrain = manaDrain * (int) gameSettings.get(Variable.MiscVariable.MiscType.UPGRADED_POSSESSION_COST_PERCENTAGE).getValue() / 100;
        }
        Mana mana = entityData.getComponent(entityId, Mana.class);
        entityData.setComponent(entityId, new Mana(mana != null ? mana.manaGeneration - manaDrain : -manaDrain));
        entityData.setComponent(entityId, new Possessed(manaDrain, gameTimer.getGameTime()));
        entityData.removeComponent(entityId, CreatureAi.class);
        entityData.removeComponent(entityId, Navigation.class);
        unassingCurrentTask();
    }

    /**
     * DK II charges less for possessing once the keeper has upgraded the
     * Possession spell (DKII.EXE 0x4C17A0 and 0x4C10E0)
     *
     * @return whether the owner has upgraded the Possession spell
     */
    private boolean isPossessionUpgraded() {
        Keeper keeper = levelInfo.getPlayer(getOwnerId());
        return keeper != null && keeper.getAvailableSpells().stream()
                .anyMatch(spell -> spell.isUpgraded()
                && levelInfo.getLevelData().getKeeperSpellById(spell.getId()).getTargetRule() == KeeperSpell.TargetRule.POSESSION);
    }

    private void endPossession() {
        Possessed possessed = entityData.getComponent(entityId, Possessed.class);
        if (possessed == null) {
            return;
        }

        // Return the mana flow
        Mana mana = entityData.getComponent(entityId, Mana.class);
        int manaGeneration = (mana != null ? mana.manaGeneration : 0) + possessed.manaDrain;
        if (manaGeneration == 0) {
            entityData.removeComponent(entityId, Mana.class);
        } else {
            entityData.setComponent(entityId, new Mana(manaGeneration));
        }

        // The abilities only last the possession, a carried worker falls down (0x4831D0)
        PossessedAbility possessedAbility = entityData.getComponent(entityId, PossessedAbility.class);
        if (possessedAbility != null) {
            togglePossessedAbility(possessedAbility.ability);
        }
        if (possessedAbility != null && possessedAbility.entity != null) {
            releaseWorker(possessedAbility.entity, false);
        }

        // The hypnotised creature is let go (0x514590)
        EntityId hypnotised = getHypnotised();
        if (hypnotised != null) {
            releaseHypnotised(hypnotised);
        }

        // The disguise drops (0x4BE090)
        entityData.removeComponent(entityId, Disguised.class);
        entityData.removeComponent(entityId, PossessedMovement.class);
        entityData.removeComponent(entityId, Possessed.class);

        // The group follows us only while we are possessed
        for (EntityId follower : getPossessedFollowers()) {
            entityData.removeComponent(follower, FollowTarget.class);
        }
        entityData.setComponent(entityId, new CreatureAi(gameTimer.getGameTime(), CreatureState.IDLE, getCreature().getCreatureId()));
    }

    @Override
    public void convertCreature(short playerId) {
        convertCreature(entityId, playerId);
    }

    private void convertCreature(EntityId entity, short playerId) {
        removeFromRoomStrorage();

        // Remove our lair from the last player
        removeLair();

        // Set new owner
        entityData.setComponent(entity, new Owner(playerId, playerId));

        // Remove good player stuff
        entityData.removeComponent(entity, Objective.class);

        // Reset AI
        getStateMachine().changeState(CreatureState.IDLE);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof CreatureController that)) {
            return false;
        }

        return Objects.equals(entityId, that.entityId);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hashCode(entityId);
    }
}
