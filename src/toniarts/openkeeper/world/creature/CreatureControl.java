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
package toniarts.openkeeper.world.creature;

import com.badlogic.gdx.ai.fsm.DefaultStateMachine;
import com.badlogic.gdx.ai.fsm.StateMachine;
import com.badlogic.gdx.ai.pfa.GraphPath;
import com.badlogic.gdx.ai.steer.behaviors.FollowPath;
import com.badlogic.gdx.ai.steer.behaviors.PrioritySteering;
import com.badlogic.gdx.ai.steer.behaviors.RaycastObstacleAvoidance;
import com.badlogic.gdx.ai.steer.behaviors.Wander;
import com.badlogic.gdx.ai.steer.utils.paths.LinePath;
import com.badlogic.gdx.ai.steer.utils.paths.LinePath.LinePathParam;
import com.badlogic.gdx.ai.steer.utils.rays.SingleRayConfiguration;
import com.badlogic.gdx.ai.utils.RaycastCollisionDetector;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.jme3.math.FastMath;
import com.jme3.math.Vector2f;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Spatial;
import java.awt.Point;
import java.util.ArrayList;
import java.util.List;
import toniarts.openkeeper.ai.creature.CreatureState;
import toniarts.openkeeper.game.task.AbstractTask;
import toniarts.openkeeper.tools.convert.map.ArtResource;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.Player;
import toniarts.openkeeper.tools.convert.map.Thing;
import toniarts.openkeeper.tools.convert.map.Thing.DeadBody;
import toniarts.openkeeper.tools.convert.map.Thing.GoodCreature;
import toniarts.openkeeper.tools.convert.map.Thing.KeeperCreature;
import toniarts.openkeeper.tools.convert.map.Thing.NeutralCreature;
import toniarts.openkeeper.utils.Utils;
import toniarts.openkeeper.world.TileData;
import toniarts.openkeeper.world.WorldState;
import toniarts.openkeeper.world.control.IInteractiveControl;
import toniarts.openkeeper.world.creature.steering.AbstractCreatureSteeringControl;
import toniarts.openkeeper.world.creature.steering.CreatureRayCastCollisionDetector;
import toniarts.openkeeper.world.listener.CreatureListener;
import toniarts.openkeeper.world.object.ObjectControl;
import toniarts.openkeeper.world.room.GenericRoom;

/**
 * Controller for creature. Bridge between the visual object and AI.
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public abstract class CreatureControl extends AbstractCreatureSteeringControl implements IInteractiveControl, CreatureListener {

    public enum AnimationType {

        MOVE, WORK, IDLE;
    }

    // Attributes
    private final String name;
    private final String bloodType;
    private int gold = 0;
    private int level = 1;
    private int health = 1;
    private int experience = 0;
    private short ownerId;
    //

    protected final StateMachine<CreatureControl, CreatureState> stateMachine;
    private final WorldState worldState;
    private float timeInState;
    private CreatureState state;
    private boolean animationPlaying = false;
    private int idleAnimationPlayCount = 1;
    private float lastAttributeUpdateTime = 0;
    private AbstractTask assignedTask;
    private AnimationType playingAnimationType = AnimationType.IDLE;
    private ObjectControl creatureLair;

    public CreatureControl(Thing.Creature creatureInstance, Creature creature, WorldState worldState) {
        super(creature);
        stateMachine = new DefaultStateMachine<CreatureControl, CreatureState>(this) {

            @Override
            public void changeState(CreatureState newState) {
                super.changeState(newState);

                // Notify
                onStateChange(CreatureControl.this, newState, getPreviousState());

            }

        };
        this.worldState = worldState;

        // Attributes
        name = Utils.generateCreatureName();
        bloodType = Utils.generateBloodType();
        gold = creatureInstance.getGoldHeld();
        health = creature.getHp();
        if (creatureInstance instanceof KeeperCreature) {
            health = (int) (((KeeperCreature) creatureInstance).getInitialHealth() / 100f * health);
            level = ((KeeperCreature) creatureInstance).getLevel();
            ownerId = ((KeeperCreature) creatureInstance).getPlayerId();
        } else if (creatureInstance instanceof GoodCreature) {
            health = (int) (((GoodCreature) creatureInstance).getInitialHealth() / 100f * health);
            level = ((GoodCreature) creatureInstance).getLevel();
            ownerId = Player.GOOD_PLAYER_ID;
        } else if (creatureInstance instanceof NeutralCreature) {
            health = (int) (((NeutralCreature) creatureInstance).getInitialHealth() / 100f * health);
            level = ((NeutralCreature) creatureInstance).getLevel();
            ownerId = Player.NEUTRAL_PLAYER_ID;
        } else if (creatureInstance instanceof DeadBody) {
            ownerId = ((DeadBody) creatureInstance).getPlayerId();
        }
    }

    @Override
    protected void controlUpdate(float tpf) {
        super.controlUpdate(tpf);
        if (stateMachine.getCurrentState() == null) {
            stateMachine.changeState(CreatureState.IDLE);
        }

        // Set the time in state
        if (stateMachine.getCurrentState() != null) {
            if (stateMachine.getCurrentState().equals(state)) {
                timeInState += tpf;
            } else {
                state = stateMachine.getCurrentState();
                timeInState = 0f;
            }
        }

        stateMachine.update();

        // Update attributes
        updateAttributes(tpf);

        // Set the appropriate animation
        playStateAnimation();
    }

    @Override
    protected void controlRender(RenderManager rm, ViewPort vp) {

    }

    public void wander() {

        // Set wandering
        PrioritySteering<Vector2> prioritySteering = new PrioritySteering(this, 0.0001f);
        RaycastCollisionDetector<Vector2> raycastCollisionDetector = new CreatureRayCastCollisionDetector(worldState);
        RaycastObstacleAvoidance<Vector2> raycastObstacleAvoidanceSB = new RaycastObstacleAvoidance<>(this, new SingleRayConfiguration<>(this, 1.5f),
                raycastCollisionDetector, 0.5f);
        prioritySteering.add(raycastObstacleAvoidanceSB);
        prioritySteering.add(new Wander<>(this).setFaceEnabled(false) // We want to use Face internally (independent facing is on)
                .setAlignTolerance(0.001f) // Used by Face
                .setDecelerationRadius(5) // Used by Face
                .setTimeToTarget(0.1f) // Used by Face
                .setWanderOffset(10) //
                .setWanderOrientation(10) //
                .setWanderRadius(10) //
                .setWanderRate(FastMath.TWO_PI * 4));
        setSteeringBehavior(prioritySteering);
    }

    public void idle() {
        unassingCurrentTask();

        // Find a random accessible tile nearby and do some idling there
        if (idleAnimationPlayCount > 0) {
            navigateToRandomPoint();
            idleAnimationPlayCount = 0;
        }
    }

    private void navigateToRandomPoint() {
        Point p = worldState.findRandomAccessibleTile(worldState.getTileCoordinates(getSpatial().getWorldTranslation()), 10, creature);
        if (p != null) {
            GraphPath<TileData> outPath = worldState.findPath(worldState.getTileCoordinates(getSpatial().getWorldTranslation()), p, creature);

            if (outPath != null && outPath.getCount() > 1) {

                // Debug
//                worldHandler.drawPath(new LinePath<>(pathToArray(outPath)));
                PrioritySteering<Vector2> prioritySteering = new PrioritySteering(this, 0.0001f);
                FollowPath<Vector2, LinePathParam> followPath = new FollowPath(this, new LinePath<>(pathToArray(outPath), true), 2);
                followPath.setDecelerationRadius(1f);
                followPath.setArrivalTolerance(0.2f);
                prioritySteering.add(followPath);

                setSteeringBehavior(prioritySteering);
            }
        }
    }

    public boolean idleTimeExceeded() {
        return ((creature.getIdleDuration() < 0.1f ? 1f : creature.getIdleDuration()) < timeInState);
    }

    public StateMachine<CreatureControl, CreatureState> getStateMachine() {
        return stateMachine;
    }

    private void playAnimation(ArtResource anim) {
        animationPlaying = true;
        CreatureLoader.playAnimation(getSpatial(), anim, worldState.getAssetManager());
    }

    /**
     * Should the current animation stop?
     *
     * @return stop or not
     */
    boolean isStopAnimation() {
        // FIXME: not very elegant to check this way
        return (steeringBehavior == null || (stateMachine.getCurrentState() == CreatureState.WORK && isAssignedTaskValid()));
    }

    /**
     * Current animation has stopped
     */
    void onAnimationStop() {
        animationPlaying = false;
        if (stateMachine.getCurrentState() == CreatureState.IDLE && idleAnimationPlayCount > 0) {

            // Find a new target
            idle();
        } else if (stateMachine.getCurrentState() == CreatureState.SLAPPED) {

            // Return to previous state
            stateMachine.revertToPreviousState();
        } else if (stateMachine.getCurrentState() == CreatureState.DEAD) {

            // TODO: should show the pose for awhile I guess
            removeCreature();
        } else {
            playStateAnimation();
        }
    }

    /**
     * An animation cycle is finished
     */
    void onAnimationCycleDone() {

        if (stateMachine.getCurrentState() == CreatureState.WORK && playingAnimationType == AnimationType.WORK && isAssignedTaskValid()) {

            // Different work based reactions
            assignedTask.executeTask(this);
        }
    }

    private void playStateAnimation() {
        if (!animationPlaying) {
            if (steeringBehavior != null) {
                playAnimation(creature.getAnimWalkResource());
                playingAnimationType = AnimationType.MOVE;
            } else if (stateMachine.getCurrentState() == CreatureState.WORK) {

                // Different work animations
                // TODO: The tasks should have the animation
                playingAnimationType = AnimationType.WORK;
                ArtResource anim = assignedTask.getTaskAnimation(this);
                if (anim != null) {
                    playAnimation(anim);
                } else {
                    onAnimationCycleDone();
                }
            } else {
                List<ArtResource> idleAnimations = new ArrayList<>(3);
                if (creature.getAnimIdle1Resource() != null) {
                    idleAnimations.add(creature.getAnimIdle1Resource());
                }
                if (creature.getAnimIdle2Resource() != null) {
                    idleAnimations.add(creature.getAnimIdle2Resource());
                }
                ArtResource idleAnim = idleAnimations.get(0);
                if (idleAnimations.size() > 1) {
                    idleAnim = Utils.getRandomItem(idleAnimations);
                }
                idleAnimationPlayCount++;
                playAnimation(idleAnim);
                playingAnimationType = AnimationType.IDLE;
            }
        }
    }

    private Array<Vector2> pathToArray(GraphPath<TileData> outPath) {
        Array<Vector2> path = new Array<>(outPath.getCount());
        for (TileData tile : outPath) {
            path.add(new Vector2(tile.getX() - 0.5f, tile.getY() - 0.5f));
        }
        return path;
    }

    @Override
    public String getTooltip(short playerId) {
        String tooltip;
        if (ownerId == playerId) {
            tooltip = Utils.getMainTextResourceBundle().getString("2841");
        } else {
            tooltip = Utils.getMainTextResourceBundle().getString(Integer.toString(creature.getTooltipStringId()));
        }
        return formatString(tooltip);
    }

    private String formatString(String string) {
        return string.replaceAll("%29", name).replaceAll("%30", creature.getName()).replaceAll("%31", getStatusText());
    }

    private String getStatusText() {
        switch (state) {
            case IDLE: {
                return Utils.getMainTextResourceBundle().getString("2599");
            }
            case WORK: {
                return assignedTask.getTooltip();
            }
            case WANDER: {
                return Utils.getMainTextResourceBundle().getString("2628");
            }
            case DEAD: {
                return Utils.getMainTextResourceBundle().getString("2598");
            }
        }
        return "";
    }

    private boolean slap(short playerId) {
        // TODO: Direction & sound
        if (isSlappable(playerId)) {
            stateMachine.changeState(CreatureState.SLAPPED);
            steeringBehavior = null;
            idleAnimationPlayCount = 0;
            health -= creature.getSlapDamage();
            if (health < 1) {

                // Die :(
                stateMachine.changeState(CreatureState.DEAD);
            } else {
                playAnimation(creature.getAnimFallbackResource());
            }

            return true;
        }
        return false;
    }

    public void die() {
        //TODO: Dying direction
        playAnimation(creature.getAnimDieResource());

        // Notify
        onDie(CreatureControl.this);
    }

    private void removeCreature() {

        // Unassing any tasks
        unassingCurrentTask();

        Spatial us = getSpatial();
        us.removeFromParent();
    }

    private void updateAttributes(float tpf) {
        lastAttributeUpdateTime += tpf;
        if (lastAttributeUpdateTime >= 1) {
            lastAttributeUpdateTime -= 1;

            experience += creature.getExpPerSecond();
            health += creature.getOwnLandHealthIncrease(); // FIXME, need to detect prev & current pos
            health = Math.min(health, creature.getHp());
            if (experience >= creature.getExpForNextLevel()) { // Probably multiply the value per level?
                experience -= creature.getExpForNextLevel();
                level++;
                //TODO: other leveling stuff, max levels etc.
            }
        }
    }

    public short getOwnerId() {
        return ownerId;
    }

    public void navigateToAssignedTask() {

        Vector2f loc = assignedTask.getTarget(this);
        if (loc != null) {
            GraphPath<TileData> outPath = worldState.findPath(worldState.getTileCoordinates(getSpatial().getWorldTranslation()), new Point((int) Math.floor(loc.x), (int) Math.floor(loc.y)), creature);

            if (outPath != null && outPath.getCount() > 1) {

                // Debug
//                worldHandler.drawPath(new LinePath<>(pathToArray(outPath)));
                PrioritySteering<Vector2> prioritySteering = new PrioritySteering(this, 0.0001f);
                FollowPath<Vector2, LinePathParam> followPath = new FollowPath(this, new LinePath<>(pathToArray(outPath), true), 2);
                followPath.setDecelerationRadius(1f);
                followPath.setArrivalTolerance(0.2f);
                prioritySteering.add(followPath);

                setSteeringBehavior(prioritySteering);
            }
        }
    }

    public void setAssignedTask(AbstractTask task) {

        // Unassign previous task
        unassingCurrentTask();

        assignedTask = task;
    }

    private void unassingCurrentTask() {
        if (assignedTask != null) {
            assignedTask.unassign(this);
        }
        assignedTask = null;
    }

    public Creature getCreature() {
        return creature;
    }

    public boolean isAtAssignedTaskTarget() {
        // FIXME: not like this, universal solution
        return (assignedTask != null && assignedTask.getTarget(this) != null && steeringBehavior == null && isNear(assignedTask.getTarget(this)));
    }

    public boolean isAssignedTaskValid() {
        // FIXME: yep
        return (assignedTask != null && assignedTask.isValid());
    }

    private boolean isNear(Vector2f target) {
        return (target.distanceSquared(getSpatial().getWorldTranslation().x, getSpatial().getWorldTranslation().z) < 0.5f);
    }

    private boolean isSlappable(short playerId) {
        return playerId == ownerId && creature.getFlags().contains(Creature.CreatureFlag.CAN_BE_SLAPPED) && !stateMachine.isInState(CreatureState.DEAD);
    }

    public boolean isTooMuchGold() {
        return gold >= creature.getMaxGoldHeld();
    }

    public boolean dropGoldToTreasury() {
        if (gold > 0) {
            if (worldState.getTaskManager().assignGoldToTreasuryTask(this)) {
                navigateToAssignedTask();
                return true;
            }
        }

        return false;
    }

    public void dropGold() {
//        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    /**
     * Get the creature coordinates, in tile coordinates
     *
     * @return the tile coordinates
     */
    public Point getCreatureCoordinates() {
        return worldState.getTileCoordinates(getSpatial().getWorldTranslation());
    }

    /**
     * Get current creature gold amount
     *
     * @return the posessed gold
     */
    public int getGold() {
        return gold;
    }

    /**
     * Add gold to creature
     *
     * @param gold the amount of gold to add
     */
    public void addGold(int gold) {
        this.gold += gold;
    }

    /**
     * Remove gold from the creature
     *
     * @param gold the amount of gold to remove
     */
    public void substractGold(int gold) {
        this.gold -= gold;
    }

    public boolean isWorker() {
        return creature.getFlags().contains(Creature.CreatureFlag.IS_WORKER);
    }

    public boolean findWork() {

        // See if we have some available work
        return (worldState.getTaskManager().assignTask(this, false));
    }

    /**
     * Finds a space for a lair, a task really
     *
     * @return true if a lair task is found
     */
    public boolean findLair() {
        return (worldState.getTaskManager().assignClosestRoomTask(this, GenericRoom.ObjectType.LAIR));
    }

    /**
     * Does the creature need a lair
     *
     * @return needs a lair
     */
    public boolean needsLair() {
        return creature.getTimeSleep() > 0;
    }

    /**
     * Does the creature have a lair
     *
     * @return has a lair
     */
    public boolean hasLair() {
        return creatureLair != null;
    }

    public void setCreatureLair(ObjectControl creatureLair) {
        this.creatureLair = creatureLair;
    }

    @Override
    public boolean isPickable(short playerId) {
        return playerId == ownerId && creature.getFlags().contains(Creature.CreatureFlag.CAN_BE_PICKED_UP) && !stateMachine.isInState(CreatureState.DEAD);
    }

    @Override
    public boolean isInteractable(short playerId) {
        return isSlappable(playerId);
    }

    @Override
    public boolean pickUp(short playerId) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean interact(short playerId) {
        return slap(playerId);
    }

}
