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
import java.util.EnumSet;
import toniarts.openkeeper.game.component.CreatureAi;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.Mobile;
import toniarts.openkeeper.game.component.Navigation;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.component.SeparationPush;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.creature.CreatureState;
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
 * 1/sqrt(d/R) - 1, or as 1/(d/R) - 1 from a creature that stands its ground (a
 * blocker), is shared by mass, and is stronger when moving away from the other
 * one. A blocker is never pushed itself.
 * <br>
 * DK II adds the weighted push to a velocity that is capped at the walking
 * speed of a tick, so the push is measured against that speed: it can hold a
 * walker back or turn it round a blocker, but it doesn't push into walls.
 */
public final class SeparationSystem implements IGameLogicUpdatable {

    /**
     * DK II's pilot maximum force (1.5 tiles, 0x4D5B30). A push is capped to
     * it.
     */
    private static final float MAX_FORCE = 1.5f;

    /**
     * DK II weighs the separation 0.13 and the path following 0.35 (0x4D56E0)
     */
    private static final float SEPARATION_WEIGHT = 0x2147 / (float) 0x5999;

    /**
     * A creature stands its ground while it fights a target in reach (the
     * fight state 0x45F030 sets the blocker flag +0x232 0x80)
     */
    private static final EnumSet<CreatureState> FIGHTING = EnumSet.of(CreatureState.FIGHT, CreatureState.MELEE_ATTACK, CreatureState.CAST_SPELL);

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
        walkingCreatures = entityData.getEntities(CreatureComponent.class, Position.class, Navigation.class, Mobile.class);
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
        if (creature == null || isBlocker(entity.getId(), creature)) {
            return separation;
        }

        // A walking creature faces where it goes
        Position position = entity.get(Position.class);
        Vector2 heading = SteeringUtils.calculateAngleToVector(new Vector2(), -position.rotation);
        Point tile = WorldUtils.vectorToPoint(position.position);
        for (int x = Math.max(0, tile.x - 1); x <= Math.min(mapController.getMapData().getWidth() - 1, tile.x + 1); x++) {
            for (int y = Math.max(0, tile.y - 1); y <= Math.min(mapController.getMapData().getHeight() - 1, tile.y + 1); y++) {
                for (EntityId otherId : entityPositionLookup.getEntitiesInLocation(x, y)) {
                    if (!otherId.equals(entity.getId())) {
                        addPairPush(separation, entity.getId(), position, heading, creature, otherId);
                    }
                }
            }
        }
        if (separation.length() > MAX_FORCE) {
            separation.normalizeLocal().multLocal(MAX_FORCE);
        }
        slideAlongWalls(separation, position, creature.getAttributes().getWidth() / 2);

        // As a share of the path following, that turns the velocity towards the walking speed of a tick
        float speedPerTick = entity.get(Mobile.class).maxSpeed / kwdFile.getGameLevel().getTicksPerSec();
        return separation.multLocal(SEPARATION_WEIGHT / speedPerTick);
    }

    private void addPairPush(Vector2f separation, EntityId entityId, Position position, Vector2 heading,
            Creature creature, EntityId otherId) {
        CreatureComponent otherCreature = entityData.getComponent(otherId, CreatureComponent.class);
        Position otherPosition = entityData.getComponent(otherId, Position.class);
        Creature other = otherCreature == null ? null : kwdFile.getCreature(otherCreature.creatureId);
        if (other == null || otherPosition == null) {
            return;
        }

        float reach = creature.getAttributes().getWidth() / 2 + other.getAttributes().getWidth() / 2;
        Vector2f away = new Vector2f(position.position.x - otherPosition.position.x, position.position.z - otherPosition.position.z);
        float distance = away.length();
        if (distance > reach) {
            return;
        }
        if (distance == 0) {

            // Exactly on top of each other, split them the same way every time
            away.set(entityId.getId() < otherId.getId() ? -1 : 1, 0);
        }
        away.normalizeLocal();
        float push = getPushStrength(distance / reach, otherId, other);
        float mass = Math.max(1, creature.getUnkea8()); // Mass 1-5, 0x4D2A60
        float otherMass = Math.max(1, other.getUnkea8());
        float alignment = (away.x * heading.x + away.y * heading.y) / 2 + 0.7f;
        separation.addLocal(away.multLocal(push * otherMass / (mass + otherMass) * alignment));
    }

    private float getPushStrength(float ratio, EntityId otherId, Creature other) {
        if (ratio < 0.01f) {
            return MAX_FORCE;
        }
        if (isBlocker(otherId, other)) {
            return 1 / ratio - 1;
        }
        return (float) (1 / Math.sqrt(ratio)) - 1;
    }

    /**
     * DK II moves a creature per axis when the step is blocked (0x4D4760), so
     * the push along an axis that would take the creature's edge into a wall
     * is dropped
     */
    private void slideAlongWalls(Vector2f separation, Position position, float radius) {
        if (separation.x != 0 && mapController.isSolid(WorldUtils.vectorToPoint(position.position.x + Math.signum(separation.x) * radius, position.position.z))) {
            separation.x = 0;
        }
        if (separation.y != 0 && mapController.isSolid(WorldUtils.vectorToPoint(position.position.x, position.position.z + Math.signum(separation.y) * radius))) {
            separation.y = 0;
        }
    }

    /**
     * Does the creature stand its ground against the separation of others. DK
     * II marks a creature that fights a target in reach (0x45F030) and the
     * Horned Reaper always (0x488930).
     *
     * @param entityId the creature
     * @param creature its type
     * @return true if walkers have to go round it
     */
    private boolean isBlocker(EntityId entityId, Creature creature) {
        if (creature.getFlags().contains(Creature.CreatureFlag.IS_HORNY)) {
            return true;
        }
        CreatureAi creatureAi = entityData.getComponent(entityId, CreatureAi.class);
        if (creatureAi == null || !FIGHTING.contains(creatureAi.getCreatureState())) {
            return false;
        }

        // Not while it still walks to its target
        Navigation navigation = entityData.getComponent(entityId, Navigation.class);
        return navigation == null || navigation.navigationPath.isEmpty();
    }

    @Override
    public void start() {

    }

    @Override
    public void stop() {
        walkingCreatures.release();
    }

}
