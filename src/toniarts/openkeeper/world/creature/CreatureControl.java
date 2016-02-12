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
import com.badlogic.gdx.ai.steer.behaviors.PrioritySteering;
import com.badlogic.gdx.ai.steer.behaviors.RaycastObstacleAvoidance;
import com.badlogic.gdx.ai.steer.behaviors.Wander;
import com.badlogic.gdx.ai.steer.utils.rays.SingleRayConfiguration;
import com.badlogic.gdx.ai.utils.RaycastCollisionDetector;
import com.badlogic.gdx.math.Vector2;
import com.jme3.math.FastMath;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import toniarts.openkeeper.ai.creature.CreatureState;
import toniarts.openkeeper.tools.convert.map.ArtResource;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.Thing;
import toniarts.openkeeper.utils.Utils;
import toniarts.openkeeper.world.WorldHandler;
import toniarts.openkeeper.world.creature.steering.AbstractCreatureSteeringControl;
import toniarts.openkeeper.world.creature.steering.CreatureRayCastCollisionDetector;

/**
 * Controller for creature. Bridge between the visual object and AI.
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class CreatureControl extends AbstractCreatureSteeringControl {

    // Attributes
    private final String name;
    private final String bloodType;
    private int gold;
    private int level;
    private int health;
    private int experience;
    //

    protected final StateMachine<CreatureControl, CreatureState> stateMachine;
    private final WorldHandler worldHandler;
    private float timeInState;
    private CreatureState state;
    private boolean animationPlaying = false;

    public CreatureControl(Thing.Creature creatureInstance, Creature creature, WorldHandler worldHandler) {
        super(creature);
        stateMachine = new DefaultStateMachine<>(this);
        this.worldHandler = worldHandler;

        // Attributes
        name = Utils.generateCreatureName();
        bloodType = Utils.generateBloodType();
        gold = creatureInstance.getGoldHeld();
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

        // Set the appropriate animation
        playStateAnimation();
    }

    @Override
    protected void controlRender(RenderManager rm, ViewPort vp) {

    }

    public void wander() {

        // Set wandering
        PrioritySteering<Vector2> prioritySteering = new PrioritySteering(this, 0.0001f);
        RaycastCollisionDetector<Vector2> raycastCollisionDetector = new CreatureRayCastCollisionDetector(worldHandler);
        RaycastObstacleAvoidance<Vector2> raycastObstacleAvoidanceSB = new RaycastObstacleAvoidance<>(this, new SingleRayConfiguration<Vector2>(this, 1.5f),
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

        // Stop
        setSteeringBehavior(null);
    }

    public boolean idleTimeExceeded() {
        return ((creature.getIdleDuration() < 0.1f ? 1f : creature.getIdleDuration()) < timeInState);
    }

    public StateMachine<CreatureControl, CreatureState> getStateMachine() {
        return stateMachine;
    }

    private void playAnimation(ArtResource anim) {
        animationPlaying = true;
        CreatureLoader.playAnimation(getSpatial(), anim);
    }

    /**
     * Should the current animation stop?
     *
     * @return stop or not
     */
    boolean isStopAnimation() {
        return true;
    }

    /**
     * Current animation has stopped
     */
    void onAnimationStop() {
        animationPlaying = false;
        playStateAnimation();
    }

    /**
     * An animation cycle is finished
     */
    void onAnimationCycleDone() {

    }

    private void playStateAnimation() {
        if (!animationPlaying) {
            switch (stateMachine.getCurrentState()) {
                case IDLE: {
                    List<ArtResource> idleAnimations = new ArrayList<>(3);
                    if (creature.getAnimIdle1Resource() != null) {
                        idleAnimations.add(creature.getAnimIdle1Resource());
                    }
                    if (creature.getAnimIdle2Resource() != null) {
                        idleAnimations.add(creature.getAnimIdle2Resource());
                    }
                    ArtResource idleAnim = idleAnimations.get(0);
                    if (idleAnimations.size() > 1) {
                        Random random = new Random();
                        idleAnim = idleAnimations.get(random.nextInt(idleAnimations.size()));
                    }
                    playAnimation(idleAnim);
                    break;
                }
                case WANDER: {
                    playAnimation(creature.getAnimWalkResource());
                    break;
                }
            }
        }
    }

}