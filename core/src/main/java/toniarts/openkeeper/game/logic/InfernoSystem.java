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

import com.jme3.math.Vector3f;
import com.simsilica.es.Entity;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.EntitySet;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.Health;
import toniarts.openkeeper.game.component.InfernoArea;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.controller.entity.EntityController;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.WorldUtils;

/**
 * Burns the creatures around a burning Inferno, friend and foe alike, except
 * the ones fire can't harm. As in the original (DKII.EXE 0x4AF660) it burns
 * every game tick. The fire itself goes out on its own (it decays).
 */
public final class InfernoSystem implements IGameLogicUpdatable {

    private final EntityData entityData;
    private final IEntityPositionLookup entityPositionLookup;
    private final IKwdFile kwdFile;
    private final EntitySet fires;

    public InfernoSystem(EntityData entityData, IEntityPositionLookup entityPositionLookup, IKwdFile kwdFile) {
        this.entityData = entityData;
        this.entityPositionLookup = entityPositionLookup;
        this.kwdFile = kwdFile;

        fires = entityData.getEntities(InfernoArea.class, Position.class);
    }

    @Override
    public void processTick(float tpf) {
        fires.applyChanges();

        for (Entity fire : fires) {
            InfernoArea area = fire.get(InfernoArea.class);
            burn(area, fire.get(Position.class).position);
            entityData.setComponent(fire.getId(), new InfernoArea(area.damage, area.radius, area.burns + 1));
        }
    }

    private void burn(InfernoArea area, Vector3f centre) {

        // The creatures on the tiles in reach that are closer than the radius. The first burn reaches only half the
        // area, as in the original
        float reachSquared = area.radius * area.radius * WorldUtils.TILE_WIDTH * WorldUtils.TILE_WIDTH;
        if (area.burns == 0) {
            reachSquared /= 2;
        }
        Point tile = WorldUtils.vectorToPoint(centre);
        for (int x = tile.x - area.radius; x <= tile.x + area.radius; x++) {
            for (int y = tile.y - area.radius; y <= tile.y + area.radius; y++) {
                for (EntityId entityId : entityPositionLookup.getEntitiesInLocation(x, y)) {
                    Position position = entityData.getComponent(entityId, Position.class);
                    CreatureComponent creature = entityData.getComponent(entityId, CreatureComponent.class);
                    if (position == null || creature == null
                            || entityData.getComponent(entityId, Health.class) == null
                            || isFireproof(kwdFile.getCreature(creature.creatureId))) {
                        continue;
                    }
                    float dx = position.position.x - centre.x;
                    float dz = position.position.z - centre.z;
                    if (dx * dx + dz * dz < reachSquared) {
                        EntityController.setDamage(entityData, entityId, area.damage);
                    }
                }
            }
        }
    }

    /**
     * Creatures walking on lava don't burn unless they fly (Salamander, Giant,
     * Horny, but not Firefly or Fairy), that is the Inferno's own check in the
     * original. Horny and the ones only Horny can harm (Stone Knight) take no
     * damage at all there
     *
     * @param creature the creature
     * @return true if the Inferno can't harm it
     */
    static boolean isFireproof(Creature creature) {
        return (creature.getFlags().contains(Creature.CreatureFlag.CAN_WALK_ON_LAVA)
                && !creature.getFlags().contains(Creature.CreatureFlag.CAN_FLY))
                || creature.getFlags().contains(Creature.CreatureFlag.IS_HORNY)
                || creature.getFlags().contains(Creature.CreatureFlag.ONLY_ATTACKABLE_BY_HORNY);
    }

    @Override
    public void start() {

    }

    @Override
    public void stop() {
        fires.release();
    }

}
