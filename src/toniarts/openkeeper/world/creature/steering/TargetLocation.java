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

import com.badlogic.gdx.ai.utils.Location;
import com.badlogic.gdx.math.Vector2;

/**
 *
 * @author ArchDemon
 */
@Deprecated
public final class TargetLocation implements Location<Vector2> {

    private final Vector2 position;
    private float orientation;

    public TargetLocation(Vector2 from, Vector2 to) {
        this.position = new Vector2();
        orientation = vectorToAngle(from.sub(to));
    }

    public TargetLocation(float orientation) {
        this.position = new Vector2();
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
    public void setOrientation(float f) {
        this.orientation = f;
    }

    @Override
    public float vectorToAngle(Vector2 t) {
        return AbstractCreatureSteeringControl.calculateVectorToAngle(t);
    }

    @Override
    public Vector2 angleToVector(Vector2 t, float f) {
        return AbstractCreatureSteeringControl.calculateAngleToVector(t, f);
    }

    @Override
    public Location<Vector2> newLocation() {
        return new TargetLocation(orientation);
    }
}
