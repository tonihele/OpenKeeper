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
import com.jme3.math.FastMath;
import com.jme3.scene.Spatial;
import toniarts.openkeeper.game.logic.IGameLogicUpdateable;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.world.object.HighlightControl;

/**
 * Handles the moving logic of the visual creature object
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public abstract class AbstractCreatureSteeringControl extends HighlightControl implements Steerable<Vector2>, IGameLogicUpdateable {

    protected final Creature creature;
    protected SteeringBehavior<Vector2> steeringBehavior;
    private final SteeringAcceleration<Vector2> steeringOutput = new SteeringAcceleration<>(new Vector2());
    private final Vector2 position = new Vector2();
    private float orientation = 0;
    private final Vector2 linearVelocity = new Vector2();
    private float angularVelocity;
    private boolean tagged;

    private float maxLinearSpeed = 1f;
    private float maxLinearAcceleration = 8f;
    private float maxAngularSpeed = FastMath.RAD_TO_DEG * FastMath.QUARTER_PI / 2;
    private float maxAngularAcceleration = FastMath.RAD_TO_DEG * FastMath.QUARTER_PI;
    private float speedThreshold = 0.1f;
    private volatile boolean applySteering = false;
    // FIXME: dirty hack. Roll back changes and see what happens :-(
    private volatile boolean stopMoving = false;

    public AbstractCreatureSteeringControl(Creature creature) {
        this.creature = creature;

        maxLinearSpeed = creature.getSpeed();
        maxLinearSpeed = maxLinearSpeed * 10;
    }

    @Override
    public void setSpatial(Spatial spatial) {
        super.setSpatial(spatial);

        // Init the position
        position.set(getSpatial().getLocalTranslation().x, getSpatial().getLocalTranslation().z);
        orientation = getSpatial().getLocalRotation().getY();
    }

    @Override
    protected void controlUpdate(float tpf) {

        // Set the actual location to where we believe it is
        if (applySteering) {
            applySteering = false;
            getSpatial().setLocalTranslation(position.x, 0, position.y);
            getSpatial().setLocalRotation(getSpatial().getLocalRotation().fromAngles(0, -orientation, 0));
        }
    }

    public void processSteeringTick(float tpf, Application app) {
        if (steeringBehavior != null && steeringBehavior.isEnabled()) {

            // Calculate steering acceleration
            steeringBehavior.calculateSteering(steeringOutput);
            // Apply steering acceleration
            applySteering(steeringOutput, tpf);
            applySteering = true;
        }
    }

    protected void applySteering(SteeringAcceleration<Vector2> steering, float tpf) {

        // Update position and linear velocity. Velocity is trimmed to maximum speed
        position.add(linearVelocity.x * tpf, linearVelocity.y * tpf);
        linearVelocity.mulAdd(steering.linear, tpf).limit(getMaxLinearSpeed());

        // We are done
        // TODO: Call function?
        if (steering.isZero()) {
            steeringBehavior = null;
            stopMoving = false;
            return;
        }

        // Update orientation and angular velocity
        if (!stopMoving) {
            position.add(linearVelocity.x * tpf, linearVelocity.y * tpf);
            linearVelocity.mulAdd(steering.linear, tpf).limit(maxLinearSpeed);

            float newOrientation = vectorToAngle(linearVelocity);
            // this is superfluous if independentFacing is always true
            angularVelocity = (newOrientation - getOrientation()) * tpf;
            setOrientation(newOrientation);
        } else {
            // Update orientation and angular velocity
            orientation += angularVelocity * tpf;
            angularVelocity += steering.angular * tpf;
        }

        if (steering.angular != 0 && steering.linear.isZero(speedThreshold)) {
            stopMoving = true;
        }
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
        return speedThreshold;
    }

    @Override
    public void setZeroLinearSpeedThreshold(float value) {
        throw new UnsupportedOperationException();
    }

    public SteeringBehavior<Vector2> getSteeringBehavior() {
        return steeringBehavior;
    }

    public void setSteeringBehavior(SteeringBehavior<Vector2> steeringBehavior) {
        this.steeringBehavior = steeringBehavior;
    }

    public static float calculateVectorToAngle(Vector2 vector) {
        return (float) Math.atan2(-vector.x, vector.y);
    }

    public static Vector2 calculateAngleToVector(Vector2 outVector, float angle) {
        outVector.x = -(float) Math.sin(angle);
        outVector.y = (float) Math.cos(angle);
        return outVector;
    }

}
