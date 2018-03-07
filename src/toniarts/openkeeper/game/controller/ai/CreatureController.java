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

import com.badlogic.gdx.ai.fsm.DefaultStateMachine;
import com.badlogic.gdx.ai.fsm.StateMachine;
import com.badlogic.gdx.ai.pfa.GraphPath;
import com.jme3.math.Vector3f;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import java.awt.Point;
import java.util.Objects;
import toniarts.openkeeper.game.component.CreatureAi;
import toniarts.openkeeper.game.component.Mobile;
import toniarts.openkeeper.game.component.Navigation;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.controller.IGameWorldController;
import toniarts.openkeeper.game.map.MapTile;
import toniarts.openkeeper.game.navigation.steering.SteeringUtils;
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
    private final IGameWorldController gameWorldController;
    private final StateMachine<ICreatureController, CreatureState> stateMachine;
    private float motionless = 0;

    public CreatureController(EntityId entityId, EntityData entityData, IGameWorldController gameWorldController) {
        this.entityId = entityId;
        this.entityData = entityData;
        this.gameWorldController = gameWorldController;
        this.stateMachine = new DefaultStateMachine<ICreatureController, CreatureState>(this) {

            public void setCurrentState(CreatureState currentState) {
                this.currentState = currentState;

                // Also change our state component
                entityData.setComponent(entityId, new CreatureAi(currentState));
            }

        };
    }

    @Override
    public boolean shouldFleeOrAttack() {
        return false;
    }

    @Override
    public void unassingCurrentTask() {
        // TODO:
    }

    @Override
    public void navigateToRandomPoint() {
        final Position position = entityData.getComponent(entityId, Position.class);
        final Mobile mobile = entityData.getComponent(entityId, Mobile.class);
        final Owner owner = entityData.getComponent(entityId, Owner.class);
        if (position != null && mobile != null && owner != null) {
            Point start = WorldUtils.vectorToPoint(position.position);
            Point destination = gameWorldController.findRandomAccessibleTile(start, 10, this);
            if (destination != null) {
                GraphPath<MapTile> path = gameWorldController.findPath(start, destination, this);
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
        // TODO:
        return false;
    }

    @Override
    public boolean isWorker() {
        // TODO:
        return false;
    }

    @Override
    public boolean isTooMuchGold() {
        // TODO:
        return false;
    }

    @Override
    public boolean dropGoldToTreasury() {
        // TODO:
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
        // TODO:
    }

    @Override
    public boolean isAtAssignedTaskTarget() {
        // TODO:
        return false;
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
        // TODO:
        return true;
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
    public Object getAssignedTask() {
        // TODO:
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
        stateMachine.changeState(CreatureState.IDLE);
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

    @Override
    public int getGold() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void substractGold(int i) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
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

}
