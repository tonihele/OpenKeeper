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
import com.badlogic.gdx.math.Vector2;
import com.simsilica.es.Entity;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.EntitySet;
import java.util.HashMap;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import toniarts.openkeeper.game.component.ObjectComponent;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.controller.room.FiveByFiveRotatedController;
import toniarts.openkeeper.tools.convert.map.GameObject;
import toniarts.openkeeper.tools.convert.map.GameObject.ObjectFlag;
import toniarts.openkeeper.tools.convert.map.IKwdFile;

/** Local furniture repulsion and contact steering, independent of visual models. */
public final class FurnitureCollision {

    // Repel_Obstacle's native steering weight (setup at 0x4D580D).
    private static final float REPULSION_WEIGHT = 0.4f;
    // Native enumeration handles this special Heart footprint separately.
    private static final short HEART_OBSTACLE = 100;
    private final EntitySet objects;
    private final IKwdFile level;
    private final Map<EntityId, Obstacle> obstacles = new HashMap<>();
    private final Map<Long, Map<EntityId, Obstacle>> cells = new HashMap<>();
    // Native enumeration visits rows, then columns, then each tile's object
    // chain. IDs provide a stable tie-break until same-tile chain order is known.
    private final Set<Obstacle> nearby = new TreeSet<>(Comparator
            .comparingInt((Obstacle object) -> cell(object.y()))
            .thenComparingInt(object -> cell(object.x()))
            .thenComparing(Obstacle::id));
    private final Vector2 normal = new Vector2();
    private final Vector2 sideways = new Vector2();
    private final Vector2 repulsion = new Vector2();
    private final Vector2 nextVelocity = new Vector2();
    private final Vector2 contactForce = new Vector2();
    private final Vector2[] contacts = new Vector2[FurnitureContactSolver.MAX_CONTACTS];

    private record Obstacle(EntityId id, float x, float y, float radius, boolean solid) {
        int minX() { return cell(x - radius); }
        int maxX() { return cell(x + radius); }
        int minY() { return cell(y - radius); }
        int maxY() { return cell(y + radius); }
    }

    public FurnitureCollision(EntityData entities, IKwdFile level) {
        this.level = level;
        for (int i = 0; i < contacts.length; i++) {
            contacts[i] = new Vector2();
        }
        objects = entities.getEntities(ObjectComponent.class, Position.class);
        for (Entity entity : objects) {
            add(entity);
        }
    }

    public void update() {
        if (!objects.applyChanges()) {
            return;
        }
        for (Entity entity : objects.getRemovedEntities()) {
            remove(entity.getId());
        }
        for (Entity entity : objects.getChangedEntities()) {
            remove(entity.getId());
            add(entity);
        }
        for (Entity entity : objects.getAddedEntities()) {
            add(entity);
        }
    }

    private void add(Entity entity) {
        short id = entity.get(ObjectComponent.class).objectId;
        GameObject definition = level.getObject(id);
        if (definition == null || !eligible(id, definition)) {
            return;
        }
        Position position = entity.get(Position.class);
        Obstacle obstacle = new Obstacle(entity.getId(), position.position.x, position.position.z,
                definition.getWidth() * 0.5f, definition.getFlags().contains(ObjectFlag.SOLID_OBSTACLE));
        obstacles.put(entity.getId(), obstacle);
        for (int x = obstacle.minX(); x <= obstacle.maxX(); x++) {
            for (int y = obstacle.minY(); y <= obstacle.maxY(); y++) {
                cells.computeIfAbsent(key(x, y), ignored -> new TreeMap<>()).put(entity.getId(), obstacle);
            }
        }
    }

    private static boolean eligible(short id, GameObject definition) {
        var flags = definition.getFlags();
        // Heart behavior and loose-item physics are deliberately separate work.
        return id != FiveByFiveRotatedController.OBJECT_HEART_ID && id != HEART_OBSTACLE
                && flags.contains(ObjectFlag.OBSTACLE)
                && !flags.contains(ObjectFlag.OBJECT_TYPE_GOLD)
                && !flags.contains(ObjectFlag.OBJECT_TYPE_FOOD)
                && !flags.contains(ObjectFlag.OBJECT_TYPE_CRATE)
                && !flags.contains(ObjectFlag.OBJECT_TYPE_SPECIAL)
                && !flags.contains(ObjectFlag.OBJECT_TYPE_SPELL_BOOK)
                && !flags.contains(ObjectFlag.OBJECT_TYPE_LEVEL_GEM)
                && Float.isFinite(definition.getWidth()) && definition.getWidth() > 0;
    }

    private void remove(EntityId id) {
        Obstacle obstacle = obstacles.remove(id);
        if (obstacle == null) {
            return;
        }
        for (int x = obstacle.minX(); x <= obstacle.maxX(); x++) {
            for (int y = obstacle.minY(); y <= obstacle.maxY(); y++) {
                long key = key(x, y);
                Map<EntityId, Obstacle> bucket = cells.get(key);
                bucket.remove(id);
                if (bucket.isEmpty()) {
                    cells.remove(key);
                }
            }
        }
    }

    private void findNearby(ISteerableEntity mover) {
        nearby.clear();
        float radius = mover.getBoundingRadius();
        for (int x = cell(mover.getPosition().x - radius); x <= cell(mover.getPosition().x + radius); x++) {
            for (int y = cell(mover.getPosition().y - radius); y <= cell(mover.getPosition().y + radius); y++) {
                Map<EntityId, Obstacle> bucket = cells.get(key(x, y));
                if (bucket != null) {
                    nearby.addAll(bucket.values());
                }
            }
        }
    }

    private static int cell(float position) {
        // OpenKeeper's tile centres are integer coordinates.
        return (int) Math.floor(position + 0.5f);
    }

    private static long key(int x, int y) {
        return ((long) x << 32) | (y & 0xffffffffL);
    }

    public void constrain(ISteerableEntity mover, SteeringAcceleration<Vector2> steering, Vector2 destination, float tpf) {
        // A destination is supplied only for walks allowed to end inside an
        // obstacle (native pilot flag 4), and only on their final segment.
        if (destination != null) {
            float dx = Math.abs(destination.x - mover.getPosition().x);
            float dy = Math.abs(destination.y - mover.getPosition().y);
            if (Math.max(dx, dy) + Math.min(dx, dy) * 0.5f <= 1f + mover.getBoundingRadius()) {
                return;
            }
        }
        steer(mover, steering, tpf);
    }

    public void constrain(ISteerableEntity mover, SteeringAcceleration<Vector2> steering, float tpf) {
        steer(mover, steering, tpf);
    }

    private void steer(ISteerableEntity mover, SteeringAcceleration<Vector2> steering, float tpf) {
        if (!Float.isFinite(tpf) || tpf <= 0) {
            throw new IllegalArgumentException("Contact update must be positive and finite");
        }
        findNearby(mover);
        repulsion.setZero();
        int contactCount = 0;
        for (Obstacle object : nearby) {
            normal.set(mover.getPosition()).sub(object.x(), object.y());
            float clearance = object.radius() + mover.getBoundingRadius();
            float distanceSquared = normal.len2();
            if (distanceSquared == 0 || distanceSquared > clearance * clearance) {
                continue;
            }
            normal.nor();
            if (!object.solid()) {
                // Native soft contacts add outward acceleration, not a constraint.
                repulsion.mulAdd(normal, mover.getMaxLinearAcceleration());
                continue;
            }
            if (contactCount < contacts.length) {
                contacts[contactCount++].set(normal);
            }
        }
        // Each native behavior is capped before its weight is applied. Capping
        // only the final sum makes several soft objects excessively repulsive.
        repulsion.limit(mover.getMaxLinearAcceleration()).scl(REPULSION_WEIGHT);
        steering.linear.add(repulsion);
        nextVelocity.set(mover.getLinearVelocity()).scl(tpf);
        contactForce.set(steering.linear).scl(tpf * tpf);
        FurnitureContactSolver.solve(contacts, contactCount, nextVelocity, contactForce,
                mover.getMaxLinearAcceleration() * tpf * tpf, sideways);
        mover.getLinearVelocity().set(nextVelocity).scl(1 / tpf);
        steering.linear.set(contactForce).scl(1 / (tpf * tpf))
                .limit(mover.getMaxLinearAcceleration());
    }

    public void release() {
        objects.release();
        obstacles.clear();
        cells.clear();
        nearby.clear();
    }
}
