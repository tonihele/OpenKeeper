/* Copyright (C) 2026 OpenKeeper. SPDX-License-Identifier: GPL-3.0-or-later */
package toniarts.openkeeper.game.logic;

import com.jme3.math.Vector3f;
import com.simsilica.es.Entity;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.EntitySet;
import com.simsilica.es.filter.FieldFilter;
import toniarts.openkeeper.game.component.CreatureAi;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.CreatureFall;
import toniarts.openkeeper.game.component.CreatureImprisoned;
import toniarts.openkeeper.game.component.CreatureRecuperating;
import toniarts.openkeeper.game.component.CreatureTortured;
import toniarts.openkeeper.game.component.Death;
import toniarts.openkeeper.game.component.DoorComponent;
import toniarts.openkeeper.game.component.DoorViewState;
import toniarts.openkeeper.game.component.Health;
import toniarts.openkeeper.game.component.InHand;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Peaceful;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.component.Possessed;
import toniarts.openkeeper.game.component.Unconscious;
import toniarts.openkeeper.game.controller.ILevelInfo;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.WorldUtils;

/**
 * Shared discovery of open secret doors (native door tick 0x4A64A3).
 * Exploration, permission to open a door and physical collision do not reveal it.
 */
public final class DoorDiscoverySystem implements IGameLogicUpdatable {

    private static final int DISCOVERY_RANGE = 4;
    private final EntityData entityData;
    private final IEntityPositionLookup positions;
    private final ILevelInfo levelInfo;
    private final EntitySet hiddenDoors;

    public DoorDiscoverySystem(EntityData entityData, IEntityPositionLookup positions, ILevelInfo levelInfo) {
        this.entityData = entityData;
        this.positions = positions;
        this.levelInfo = levelInfo;
        hiddenDoors = entityData.getEntities(new FieldFilter<>(DoorComponent.class, "hidden", true),
                DoorComponent.class, DoorViewState.class, Owner.class, Position.class);
    }

    @Override
    public void processTick(float tpf) {
        hiddenDoors.applyChanges();
        for (Entity door : hiddenDoors) {
            DoorComponent component = door.get(DoorComponent.class);
            DoorViewState view = door.get(DoorViewState.class);
            if (!component.blueprint && view.open && isObserved(door)) {
                entityData.setComponents(door.getId(),
                        new DoorComponent(component.doorId, component.locked, false, false),
                        new DoorViewState(view.doorId, view.locked, view.blueprint, view.open, false));
            }
        }
    }

    private boolean isObserved(Entity door) {
        Vector3f position = door.get(Position.class).position;
        short ownerId = door.get(Owner.class).ownerId;
        Point tile = WorldUtils.vectorToPoint(position);
        for (int x = tile.x - DISCOVERY_RANGE; x <= tile.x + DISCOVERY_RANGE; x++) {
            for (int y = tile.y - DISCOVERY_RANGE; y <= tile.y + DISCOVERY_RANGE; y++) {
                for (EntityId creature : positions.getEntitiesInLocation(x, y)) {
                    Owner owner = entityData.getComponent(creature, Owner.class);
                    Position observer = entityData.getComponent(creature, Position.class);
                    if (owner != null && observer != null
                            // Native asks whether the observer's keeper considers the door's owner an enemy.
                            && !DoorAccessRules.isFriendlyOwner(owner.ownerId, ownerId, levelInfo)
                            && isEligibleObserver(creature)
                            && isInRange(observer.position, position)
                            && positions.hasLineOfSight(observer.position, position)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /** Native fixed-point horizontal squared-distance test; the four-tile boundary is excluded. */
    static boolean isInRange(Vector3f observer, Vector3f door) {
        double dx = Math.floor(Math.abs((double) observer.x - door.x) * 4096);
        double dz = Math.floor(Math.abs((double) observer.z - door.z) * 4096);
        return Math.floor(dx * dx / 4096) + Math.floor(dz * dz / 4096) < 16 * 4096;
    }

    private boolean isEligibleObserver(EntityId creature) {
        Health health = entityData.getComponent(creature, Health.class);
        if (entityData.getComponent(creature, CreatureComponent.class) == null
                || health == null || health.health <= 0
                || entityData.getComponent(creature, Peaceful.class) != null
                || entityData.getComponent(creature, Unconscious.class) != null
                || entityData.getComponent(creature, Death.class) != null
                || entityData.getComponent(creature, InHand.class) != null
                || entityData.getComponent(creature, CreatureFall.class) != null
                || entityData.getComponent(creature, CreatureRecuperating.class) != null
                || entityData.getComponent(creature, CreatureImprisoned.class) != null
                || entityData.getComponent(creature, CreatureTortured.class) != null) {
            return false;
        }
        CreatureAi ai = entityData.getComponent(creature, CreatureAi.class);
        // Possession removes CreatureAi. It does not remove the creature's ability to observe.
        if (ai == null) {
            return entityData.getComponent(creature, Possessed.class) != null;
        }
        return switch (ai.getCreatureState()) {
            case DEAD, SLAPPED, ENTERING_DUNGEON, PICKED_UP, FIGHT, FLEE, BACK_OFF,
                    UNCONSCIOUS, STUNNED, IMPRISONED, TORTURED, SLEEPING, RECUPERATING,
                    DRAGGED, FALLEN, GETTING_UP, MELEE_ATTACK, CAST_SPELL, CELEBRATING,
                    CHICKEN, HERO_RECUPERATING -> false;
            default -> true;
        };
    }

    @Override
    public void start() {
    }

    @Override
    public void stop() {
        hiddenDoors.release();
    }
}
