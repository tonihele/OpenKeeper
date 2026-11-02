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
package toniarts.openkeeper.game.logic;

import com.badlogic.gdx.math.Vector2;
import com.jme3.math.Vector2f;
import com.simsilica.es.Entity;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.EntitySet;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.Navigation;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.component.SeparationPush;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.navigation.steering.SteeringUtils;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.WorldUtils;

/**
 * Pushes walking creatures away from the creatures they overlap, the way DK
 * II's separation steering does (0x4D2DF0, 0x4D2B80). Like DK II's pilot
 * update (0x4D4760) it runs once a game tick. The push is stored in
 * {@link SeparationPush} for the movement to add to the path following.
 * <br>
 * Every creature counts, whoever owns it. Two creatures overlap when they are
 * closer than the sum of their radii, half their widths. The push grows as
 * 1/sqrt(d/R) - 1, is shared by mass, and is stronger when moving away from
 * the other one.
 */
public final class SeparationSystem implements IGameLogicUpdatable {

    /**
     * DK II's pilot maximum force (1.5 tiles, 0x4D5B30). A push is capped to
     * it.
     */
    public static final float MAX_FORCE = 1.5f;

    private final EntityData entityData;
    private final IKwdFile kwdFile;
    private final IMapController mapController;
    private final IEntityPositionLookup entityPositionLookup;
    private final EntitySet walkingCreatures;

    public SeparationSystem(EntityData entityData, IKwdFile kwdFile, IMapController mapController,
            IEntityPositionLookup entityPositionLookup) {
        this.entityData = entityData;
        this.kwdFile = kwdFile;
        this.mapController = mapController;
        this.entityPositionLookup = entityPositionLookup;
        walkingCreatures = entityData.getEntities(CreatureComponent.class, Position.class, Navigation.class);
    }

    @Override
    public void processTick(float tpf) {
        if (walkingCreatures.applyChanges()) {
            for (Entity entity : walkingCreatures.getRemovedEntities()) {
                entityData.removeComponent(entity.getId(), SeparationPush.class);
            }
        }

        for (Entity entity : walkingCreatures) {
            Vector2f push = getPush(entity);
            if (push.equals(Vector2f.ZERO)) {
                if (entityData.getComponent(entity.getId(), SeparationPush.class) != null) {
                    entityData.removeComponent(entity.getId(), SeparationPush.class);
                }
            } else {
                entityData.setComponent(entity.getId(), new SeparationPush(push));
            }
        }
    }

    private Vector2f getPush(Entity entity) {
        Vector2f separation = new Vector2f();
        Creature creature = kwdFile.getCreature(entity.get(CreatureComponent.class).creatureId);
        if (creature == null) {
            return separation;
        }

        // A walking creature faces where it goes
        Position position = entity.get(Position.class);
        Vector2 heading = SteeringUtils.calculateAngleToVector(new Vector2(), -position.rotation);
        float radius = creature.getAttributes().getWidth() / 2;
        float mass = Math.max(1, creature.getUnkea8()); // Mass 1-5, 0x4D2A60
        Point tile = WorldUtils.vectorToPoint(position.position);
        Vector2f away = new Vector2f();
        for (int x = Math.max(0, tile.x - 1); x <= Math.min(mapController.getMapData().getWidth() - 1, tile.x + 1); x++) {
            for (int y = Math.max(0, tile.y - 1); y <= Math.min(mapController.getMapData().getHeight() - 1, tile.y + 1); y++) {
                for (EntityId otherId : entityPositionLookup.getEntitiesInLocation(x, y)) {
                    CreatureComponent otherCreature = otherId.equals(entity.getId()) ? null : entityData.getComponent(otherId, CreatureComponent.class);
                    Position otherPosition = entityData.getComponent(otherId, Position.class);
                    Creature other = otherCreature == null ? null : kwdFile.getCreature(otherCreature.creatureId);
                    if (other == null || otherPosition == null) {
                        continue;
                    }

                    float reach = radius + other.getAttributes().getWidth() / 2;
                    away.set(position.position.x - otherPosition.position.x, position.position.z - otherPosition.position.z);
                    float distance = away.length();
                    if (distance > reach) {
                        continue;
                    }
                    if (distance == 0) {

                        // Exactly on top of each other, split them the same way every time
                        away.set(entity.getId().getId() < otherId.getId() ? -1 : 1, 0);
                    }
                    away.normalizeLocal();
                    float ratio = distance / reach;
                    float push = ratio < 0.01f ? MAX_FORCE : (float) (1 / Math.sqrt(ratio)) - 1;
                    float otherMass = Math.max(1, other.getUnkea8());
                    float alignment = (away.x * heading.x + away.y * heading.y) / 2 + 0.7f;
                    separation.addLocal(away.mult(push * otherMass / (mass + otherMass) * alignment));
                }
            }
        }
        if (separation.length() > MAX_FORCE) {
            separation.normalizeLocal().multLocal(MAX_FORCE);
        }

        return separation;
    }

    @Override
    public void start() {

    }

    @Override
    public void stop() {
        walkingCreatures.release();
    }

}
