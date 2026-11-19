/*
 * Copyright (C) 2014-2026 OpenKeeper
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

import com.badlogic.gdx.ai.steer.SteeringAcceleration;
import com.jme3.math.Vector3f;
import com.badlogic.gdx.math.Vector2;
import com.simsilica.es.*;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.*;
import toniarts.openkeeper.tools.convert.map.*;
import static org.junit.jupiter.api.Assertions.*;

class FurnitureCollisionTest {
    private final EntityData data  =  new DefaultEntityData();
    private final Map<Short, GameObject> definitions  =  new HashMap<>();
    private final IKwdFile level  =  (IKwdFile) Proxy.newProxyInstance(IKwdFile.class.getClassLoader(), new Class<?>[]{IKwdFile.class},
            (p, m, args) -> definitions.get(((Number)args[0]).shortValue()));

    @Test
    void differentTilesUseRowOrderInsteadOfCreationOrder() throws Exception {
        EntityId first = object((short) 7, .6f, .4f, GameObject.ObjectFlag.OBSTACLE, GameObject.ObjectFlag.SOLID_OBSTACLE);
        EntityId second = object((short) 8, .4f, .6f, GameObject.ObjectFlag.OBSTACLE, GameObject.ObjectFlag.SOLID_OBSTACLE);
        Vector2 expected;
        try {
            FurnitureCollision collision = new FurnitureCollision(data, level);
            var mover = mover(.4f, .4f);
            mover.getLinearVelocity().set(1, 1);
            var steering = steering();
            steering.linear.set(2, 1);
            collision.constrain(mover, steering);
            expected = steering.linear.cpy();
            collision.release();
        } finally {
            data.removeEntity(first);
            data.removeEntity(second);
        }
        object((short) 8, .4f, .6f, GameObject.ObjectFlag.OBSTACLE, GameObject.ObjectFlag.SOLID_OBSTACLE);
        object((short) 7, .6f, .4f, GameObject.ObjectFlag.OBSTACLE, GameObject.ObjectFlag.SOLID_OBSTACLE);
        FurnitureCollision collision = new FurnitureCollision(data, level);
        var mover = mover(.4f, .4f);
        mover.getLinearVelocity().set(1, 1);
        var steering = steering();
        steering.linear.set(2, 1);
        collision.constrain(mover, steering);
        assertEquals(expected.x, steering.linear.x, 1e-6f);
        assertEquals(expected.y, steering.linear.y, 1e-6f);
        collision.release();
    }

    @Test
    void onlyObstacleFlagEnablesNormalFurnitureContacts() throws Exception {
        for (GameObject.ObjectFlag[] flags : List.of(new GameObject.ObjectFlag[]{},
                new GameObject.ObjectFlag[]{GameObject.ObjectFlag.SOLID_OBSTACLE},
                new GameObject.ObjectFlag[]{GameObject.ObjectFlag.FIRST_PERSON_OBSTACLE})) {
            EntityId object  =  object((short) 7, 0, 0, flags);
            FurnitureCollision collision  =  new FurnitureCollision(data, level);
            var mover  =  mover(.2f, 0);
            var steering  =  steering();
            collision.constrain(mover, steering);
            assertTrue(steering.isZero());
            collision.release();
            data.removeEntity(object);
        }
    }

    @Test
    void softFurnitureRepelsWithoutClippingVelocity() throws Exception {
        object((short) 7, 0, 0, GameObject.ObjectFlag.OBSTACLE);
        FurnitureCollision collision  =  new FurnitureCollision(data, level);
        var mover = mover(.2f, 0);
        mover.getLinearVelocity().set(-1, 0);
        var steering = steering();
        collision.constrain(mover, steering);
        assertTrue(steering.linear.x > 0);
        assertEquals(0, steering.linear.y);
        assertEquals(-1, mover.getLinearVelocity().x);
        collision.release();
    }

    @Test
    void severalSoftObjectsAreCappedTogetherBeforeWeighting() throws Exception {
        object((short) 7, 0, 0, GameObject.ObjectFlag.OBSTACLE);
        object((short) 8, .01f, 0, GameObject.ObjectFlag.OBSTACLE);
        FurnitureCollision collision = new FurnitureCollision(data, level);
        var mover = mover(.2f, 0);
        var steering = steering();
        collision.constrain(mover, steering);
        assertEquals(mover.getMaxLinearAcceleration() * .4f, steering.linear.x, 1e-6f);
        collision.release();
    }

    @Test
    void solidFurnitureProjectsInwardMotionAndAddsSidewaysSteering() throws Exception {
        object((short) 7, 0, 0, GameObject.ObjectFlag.OBSTACLE, GameObject.ObjectFlag.SOLID_OBSTACLE);
        FurnitureCollision collision = new FurnitureCollision(data, level);
        var mover = mover(.2f, 0);
        mover.getLinearVelocity().set(-1, 0);
        var steering = steering();
        steering.linear.set(-2, 0);
        collision.constrain(mover, steering);
        assertEquals(0, mover.getLinearVelocity().x, 1e-6f);
        assertEquals(0, steering.linear.x, 1e-6f);
        assertTrue(Math.abs(steering.linear.y) > 0);
        assertEquals(.2f, mover.getPosition().x);
        collision.release();
    }

    @Test
    void indexTracksCreationMovementReplacementAndRemoval() throws Exception {
        FurnitureCollision collision = new FurnitureCollision(data, level);
        EntityId id = object((short) 7, 0, 0, GameObject.ObjectFlag.OBSTACLE);
        collision.update();
        assertRepels(collision, .2f, 0, true);
        data.setComponent(id, new Position(0, new Vector3f(8, 0, 8)));
        collision.update();
        assertRepels(collision, .2f, 0, false);
        assertRepels(collision, 8.2f, 8, true);
        object((short) 8, 20, 20);
        data.setComponent(id, new ObjectComponent((short) 8, null));
        collision.update();
        assertRepels(collision, 8.2f, 8, false);
        data.setComponent(id, new ObjectComponent((short) 7, null));
        collision.update();
        assertRepels(collision, 8.2f, 8, true);
        data.removeComponent(id, Position.class);
        collision.update();
        assertRepels(collision, 8.2f, 8, false);
        data.setComponent(id, new Position(0, new Vector3f(8, 0, 8)));
        collision.update();
        assertRepels(collision, 8.2f, 8, true);
        data.removeEntity(id);
        collision.update();
        assertRepels(collision, 8.2f, 8, false);
        collision.release();
    }

    @Test
    void largeFootprintsInMultipleCellsAreProcessedOnlyOnce() throws Exception {
        object((short) 7, 0, 0, GameObject.ObjectFlag.OBSTACLE);
        var definition = definitions.get((short) 7);
        Field width = GameObject.class.getDeclaredField("width");
        width.setAccessible(true);
        width.set(definition, 4f);
        FurnitureCollision collision = new FurnitureCollision(data, level);
        var mover = new SteerableEntity(new EntityId(99), 1, 1f, 1.6f, 0, 0);
        var steering = steering();
        collision.constrain(mover, steering);
        assertEquals(mover.getMaxLinearAcceleration()*.4f, steering.linear.len(), 1e-6f);
        collision.release();
    }

    @Test
    void finalApproachAndCoincidentCentresDoNotPushOrProduceNaN() throws Exception {
        object((short) 7, 0, 0, GameObject.ObjectFlag.OBSTACLE);
        FurnitureCollision collision = new FurnitureCollision(data, level);
        var steering = steering();
        collision.constrain(mover(.2f, 0), steering, new Vector2());
        assertTrue(steering.isZero());
        collision.constrain(mover(0, 0), steering);
        assertTrue(steering.isZero());
        collision.release();
    }

    @Test
    void looseGoldIsOutsideTheFurniturePolicy() throws Exception {
        object((short) 7, 0, 0, GameObject.ObjectFlag.OBSTACLE, GameObject.ObjectFlag.OBJECT_TYPE_GOLD);
        FurnitureCollision collision = new FurnitureCollision(data, level);
        assertRepels(collision, .2f, 0, false);
        collision.release();
    }

    @Test
    void finalApproachRequiresTheFinalPathLegNotJustANearbyDestination() {
        var mover  =  mover(0, 0);
        var behavior  =  EntitySteeringFactory.navigateToPoint(List.of(new Vector2(), new Vector2(0, 10),
                new Vector2(1, 10), new Vector2(1, 0)), null, mover,
                new toniarts.openkeeper.utils.Point(1, 0), waypoint -> 0, waypoint -> false, false, false);
        behavior.setUpdateInterval(.25f);
        behavior.calculateSteering(steering());
        mover.getPosition().set(1, 2);
        behavior.calculateSteering(steering());
        assertFalse(behavior.isOnFinalPathSegment());
        mover.getPosition().set(0, 10);
        behavior.calculateSteering(steering());
        mover.getPosition().set(1, 10);
        behavior.calculateSteering(steering());
        assertTrue(behavior.isOnFinalPathSegment());
    }

    @Test
    void headOnContactUsesTheNativePositiveTangentTieBreak() throws Exception {
        object((short) 7, 0, 0, GameObject.ObjectFlag.OBSTACLE, GameObject.ObjectFlag.SOLID_OBSTACLE);
        FurnitureCollision collision  =  new FurnitureCollision(data, level);
        var mover  =  mover(.2f, 0);
        mover.getLinearVelocity().set(-1, 0);
        var steering  =  steering();
        collision.constrain(mover, steering);
        assertTrue(steering.linear.y > 0);
        assertEquals(0, mover.getLinearVelocity().x, 1e-6f);
        collision.release();
    }

    private void assertRepels(FurnitureCollision collision, float x, float y, boolean expected) {
        var s = steering();
        collision.constrain(mover(x, y), s);
        assertEquals(expected, !s.linear.isZero());
    }
    private EntityId object(short id, float x, float y, GameObject.ObjectFlag...flags) throws Exception {
        GameObject definition = GameObjectFixtures.gameObject("Synthetic furniture", flags);
        Field width = GameObject.class.getDeclaredField("width");
        width.setAccessible(true);
        width.set(definition, .6f);
        definitions.put(id, definition);
        EntityId entity = data.createEntity();
        data.setComponents(entity, new ObjectComponent(id, null), new Position(0, new Vector3f(x, 0, y)));
        return entity;
    }
    private SteerableEntity mover(float x, float y) {
        return new SteerableEntity(new EntityId(99), 1, .1f, x, y, 0);
    }
    private SteeringAcceleration<Vector2> steering() {
        return new SteeringAcceleration<>(new Vector2());
    }
}
