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
package toniarts.openkeeper.game.navigation.steering;

import com.badlogic.gdx.ai.utils.Location;
import com.badlogic.gdx.math.Vector2;
import com.simsilica.es.EntityId;

/**
 * Simple steerable entity. For saving games etc. I would perhaps store this
 * data in an entity component still. This would manage the updating still.
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public final class SteerableEntity implements ISteerableEntity {

    private final EntityId entityId;
    private final Vector2 position = new Vector2();
    private float orientation = 0;
    private final Vector2 linearVelocity = new Vector2();
    private float angularVelocity;
    private boolean tagged;
    private float maxLinearSpeed = 1;
    private float maxLinearAcceleration = 4;
    private float maxAngularSpeed = 20.0f;
    private float maxAngularAcceleration = 20.0f;
    private float zeroLinearSpeedThreshold = 0.01f;
    private final float boundingRadius;

    public SteerableEntity(EntityId entityId, float maxSpeed, float boundingRadius, float xPos, float yPos, float orientation) {
        this.entityId = entityId;
        this.boundingRadius = boundingRadius;
        this.maxLinearSpeed = maxSpeed;
        // FIXME how calculate acceleration? mass & maxLinearSpeed?
        this.maxLinearAcceleration = maxLinearSpeed * 4;
        // FIXME how calculate zero linear speed threshold?
        //zeroLinearSpeedThreshold = maxLinearSpeed / 3;
        this.position.x = xPos;
        this.position.y = yPos;
        this.orientation = orientation;
    }

    @Override
    public Vector2 getPosition() {
        return position;
    }

    @Override
    public float getOrientation() {
        return orientation;
    }

    @Override
    public void setOrientation(float orientation) {
        this.orientation = orientation;
    }

    @Override
    public Vector2 getLinearVelocity() {
        return linearVelocity;
    }

    @Override
    public float getAngularVelocity() {
        return angularVelocity;
    }

    @Override
    public void setAngularVelocity(float angularVelocity) {
        this.angularVelocity = angularVelocity;
    }

    @Override
    public float getBoundingRadius() {
        return boundingRadius;
    }

    @Override
    public boolean isTagged() {
        return tagged;
    }

    @Override
    public void setTagged(boolean tagged) {
        this.tagged = tagged;
    }

    @Override
    public Location<Vector2> newLocation() {
        return new EntityLocation();
    }

    @Override
    public float vectorToAngle(Vector2 vector) {
        return SteeringUtils.calculateVectorToAngle(vector);
    }

    @Override
    public Vector2 angleToVector(Vector2 outVector, float angle) {
        return SteeringUtils.calculateAngleToVector(outVector, angle);
    }

    @Override
    public float getMaxLinearSpeed() {
        return maxLinearSpeed;
    }

    @Override
    public void setMaxLinearSpeed(float maxLinearSpeed) {
        this.maxLinearSpeed = maxLinearSpeed;
    }

    @Override
    public float getMaxLinearAcceleration() {
        return maxLinearAcceleration;
    }

    @Override
    public void setMaxLinearAcceleration(float maxLinearAcceleration) {
        this.maxLinearAcceleration = maxLinearAcceleration;
    }

    @Override
    public float getMaxAngularSpeed() {
        return maxAngularSpeed;
    }

    @Override
    public void setMaxAngularSpeed(float maxAngularSpeed) {
        this.maxAngularSpeed = maxAngularSpeed;
    }

    @Override
    public float getMaxAngularAcceleration() {
        return maxAngularAcceleration;
    }

    @Override
    public void setMaxAngularAcceleration(float maxAngularAcceleration) {
        this.maxAngularAcceleration = maxAngularAcceleration;
    }

    @Override
    public float getZeroLinearSpeedThreshold() {
        return zeroLinearSpeedThreshold;
    }

    @Override
    public void setZeroLinearSpeedThreshold(float value) {
        this.zeroLinearSpeedThreshold = value;
    }

    @Override
    public EntityId getEntityId() {
        return entityId;
    }

    @Override
    public int compareTo(ISteerableEntity o) {
        return entityId.compareTo(o.getEntityId());
    }

}
