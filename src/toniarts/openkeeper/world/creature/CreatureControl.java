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
import toniarts.openkeeper.ai.creature.CreatureState;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.Thing;
import toniarts.openkeeper.world.WorldHandler;
import toniarts.openkeeper.world.creature.steering.AbstractCreatureSteeringControl;
import toniarts.openkeeper.world.creature.steering.CreatureRayCastCollisionDetector;

/**
 * Controller for creature. Bridge between the visual object and AI.
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class CreatureControl extends AbstractCreatureSteeringControl {

    protected final StateMachine<CreatureControl, CreatureState> stateMachine;
    private final WorldHandler worldHandler;

    public CreatureControl(Thing.Creature creatureInstance, Creature creature, WorldHandler worldHandler) {
        super(creature);
        stateMachine = new DefaultStateMachine<>(this);
        this.worldHandler = worldHandler;
    }

    @Override
    protected void controlUpdate(float tpf) {
        super.controlUpdate(tpf);
        if (stateMachine.getCurrentState() == null) {
            stateMachine.changeState(CreatureState.WANDER);
        }

        stateMachine.update();
    }

    @Override
    protected void controlRender(RenderManager rm, ViewPort vp) {

    }

    public void wander() {
        PrioritySteering<Vector2> prioritySteering = new PrioritySteering(this);
        RaycastCollisionDetector<Vector2> raycastCollisionDetector = new CreatureRayCastCollisionDetector(worldHandler);
        RaycastObstacleAvoidance<Vector2> raycastObstacleAvoidanceSB = new RaycastObstacleAvoidance<>(this, new SingleRayConfiguration<Vector2>(this, 0.05f),
                raycastCollisionDetector, 0.01f);
//        prioritySteering.add(raycastObstacleAvoidanceSB);
//        prioritySteering.add(new LookWhereYouAreGoing<>(this).setTimeToTarget(0.1f) //
//                .setAlignTolerance(0.001f) //
//                .setDecelerationRadius(FastMath.PI));
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

}
