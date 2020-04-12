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
package toniarts.openkeeper.world.creature.steering;

import com.badlogic.gdx.ai.steer.Steerable;
import com.badlogic.gdx.ai.steer.SteeringAcceleration;
import com.badlogic.gdx.ai.steer.SteeringBehavior;
import com.badlogic.gdx.ai.utils.Location;
import com.badlogic.gdx.math.Vector2;
import com.jme3.app.Application;
import com.jme3.bounding.BoundingBox;
import com.jme3.scene.Spatial;
import toniarts.openkeeper.game.logic.IGameLogicUpdatable;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.world.MapLoader;
import toniarts.openkeeper.world.object.HighlightControl;

/**
 * Handles the moving logic of the visual creature object
 *
 * @see toniarts.openkeeper.game.logic.MovementSystem
 * @see toniarts.openkeeper.game.navigation.steering.SteerableEntity
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
@Deprecated
public abstract class AbstractCreatureSteeringControl extends HighlightControl implements Steerable<Vector2>, IGameLogicUpdatable {

    protected final Creature creature;
    protected SteeringBehavior<Vector2> steeringBehavior;
    private static final SteeringAcceleration<Vector2> steeringOutput = new SteeringAcceleration<>(new Vector2());
    private final Vector2 position = new Vector2();
    private float orientation = 0;
    private final Vector2 linearVelocity = new Vector2();
    private float angularVelocity;
    private boolean tagged;
    private boolean independentFacing = false;
    private float maxLinearSpeed = 1;
    private float maxLinearAcceleration = 4;
    private float maxAngularSpeed = 20.0f;
    private float maxAngularAcceleration = 20.0f;
    private float zeroLinearSpeedThreshold = 0.01f;
    private volatile boolean steeringReady = false;

    public AbstractCreatureSteeringControl(Creature creature) {
        this.creature = creature;

        maxLinearSpeed = creature.getAttributes().getSpeed();
        // FIXME how calculate acceleration? mass & maxLinearSpeed?
        maxLinearAcceleration = maxLinearSpeed * 4;
        // FIXME how calculate zero linear speed threshold?
        //zeroLinearSpeedThreshold = maxLinearSpeed / 3;
    }

    @Override
    public void setSpatial(Spatial spatial) {
        super.setSpatial(spatial);

        // Init the position
        setPositionFromSpatial();
    }

    @Override
    protected void controlUpdate(float tpf) {

        // Set the actual location to where we believe it is
        if (steeringReady) {
            steeringReady = false;
            getSpatial().setLocalTranslation(position.x, MapLoader.FLOOR_HEIGHT, position.y);
            getSpatial().setLocalRotation(getSpatial().getLocalRotation().fromAngles(0, -orientation, 0));
        }
    }

    public void processSteeringTick(float tpf, Application app) {
        if (steeringBehavior != null && steeringBehavior.isEnabled() && !steeringReady) {

            // Calculate steering acceleration
            steeringBehavior.calculateSteering(steeringOutput);

            // Apply steering acceleration
            steeringReady = applySteering(steeringOutput, tpf);
        }
    }

    @Deprecated
    protected boolean applySteering(SteeringAcceleration<Vector2> steering, float tpf) {
        // We are done
        // TODO: Call function?
        if (steering.isZero() && linearVelocity.isZero(zeroLinearSpeedThreshold)
                && isZeroAngular(angularVelocity, zeroLinearSpeedThreshold)) {
            steeringBehavior = null;
            return false;
        }
        // trim speed if have no forces
        if (steering.linear.isZero() && !linearVelocity.isZero()) {
            linearVelocity.scl(0.8f);
        }
        // Update position and linear velocity. Velocity is trimmed to maximum speed
        linearVelocity.mulAdd(steering.linear, tpf).limit(maxLinearSpeed);
        if (!linearVelocity.isZero(zeroLinearSpeedThreshold)) {
            position.add(linearVelocity.x * tpf, linearVelocity.y * tpf);
        }
        // If we haven't got any velocity, then we can do nothing.
        if (!independentFacing && !linearVelocity.isZero(zeroLinearSpeedThreshold)) {
            float newOrientation = vectorToAngle(linearVelocity);
            angularVelocity = (newOrientation - orientation);
            orientation = newOrientation;
            return true;
        }

        if (steering.angular == 0 && angularVelocity != 0) {
            angularVelocity *= 0.9f;
        }
        // Update angular velocity. Velocity is trimmed to maximum speed
        angularVelocity += limitAngular(steering.angular, maxAngularAcceleration) * tpf;
        angularVelocity = limitAngular(angularVelocity, maxAngularSpeed);
        if (!isZeroAngular(angularVelocity, zeroLinearSpeedThreshold)) {
            orientation += angularVelocity * tpf;
        }

        return true;
    }

    /**
     * Limit angular speed or acceleration
     *
     * @param angular
     * @param limit
     * @return
     */
    public float limitAngular(float angular, float limit) {
        return (Math.abs(angular) > limit) ? limit : angular;
    }

    public boolean isZeroAngular(float angular, float threshold) {
        return Math.abs(angular) < threshold;
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

    public void setAngularVelocity(float angularVelocity) {
        this.angularVelocity = angularVelocity;
    }

    @Override
    public float getBoundingRadius() {
        BoundingBox worldBound = (BoundingBox) getSpatial().getWorldBound();
        return Math.max(worldBound.getXExtent(), worldBound.getZExtent());
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
        return new CreatureLocation();
    }

    @Override
    public float vectorToAngle(Vector2 vector) {
        return calculateVectorToAngle(vector);
    }

    @Override
    public Vector2 angleToVector(Vector2 outVector, float angle) {
        return calculateAngleToVector(outVector, angle);
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
        zeroLinearSpeedThreshold = value;
    }

    public boolean isIndependentFacing() {
        return independentFacing;
    }

    public void setIndependentFacing(boolean independentFacing) {
        this.independentFacing = independentFacing;
    }

    public SteeringBehavior<Vector2> getSteeringBehavior() {
        return steeringBehavior;
    }

    public void setSteeringBehavior(SteeringBehavior<Vector2> steeringBehavior) {
        this.steeringBehavior = steeringBehavior;

        if (this.steeringBehavior != null) {

            // Init the position
            setPositionFromSpatial();
        }
    }

    public static float calculateVectorToAngle(Vector2 vector) {
        return (float) Math.atan2(-vector.x, vector.y);
    }

    public static Vector2 calculateAngleToVector(Vector2 outVector, float angle) {
        outVector.x = -(float) Math.sin(angle);
        outVector.y = (float) Math.cos(angle);
        return outVector;
    }

    protected void setPositionFromSpatial() {
        position.set(getSpatial().getLocalTranslation().x, getSpatial().getLocalTranslation().z);
        orientation = getSpatial().getLocalRotation().getY();
    }

}
