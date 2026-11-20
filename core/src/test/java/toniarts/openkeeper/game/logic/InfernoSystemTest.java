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
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.Damage;
import toniarts.openkeeper.game.component.Health;
import toniarts.openkeeper.game.component.InfernoArea;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.CreatureFlagFixtures;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.WorldUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class InfernoSystemTest {

    private static final int DAMAGE = 100;
    private static final float TICK = 0.25f;
    private static final short TROLL = 1;
    private static final short SALAMANDER = 2;
    private static final short STONE_KNIGHT = 3;
    private static final short HORNY = 4;
    private static final short FIREFLY = 5;

    private final EntityData entityData = new DefaultEntityData();
    private final Map<Point, List<EntityId>> entitiesByTile = new HashMap<>();
    private final Map<Short, Creature> creatures = Map.of(
            TROLL, CreatureFlagFixtures.creature(TROLL, EnumSet.of(Creature.CreatureFlag.CAN_WALK_ON_WATER)),
            SALAMANDER, CreatureFlagFixtures.creature(SALAMANDER, EnumSet.of(Creature.CreatureFlag.CAN_WALK_ON_LAVA)),
            STONE_KNIGHT, CreatureFlagFixtures.creature(STONE_KNIGHT, EnumSet.of(Creature.CreatureFlag.ONLY_ATTACKABLE_BY_HORNY)),
            HORNY, CreatureFlagFixtures.creature(HORNY, EnumSet.of(Creature.CreatureFlag.CAN_WALK_ON_LAVA, Creature.CreatureFlag.IS_HORNY)),
            FIREFLY, CreatureFlagFixtures.creature(FIREFLY, EnumSet.of(Creature.CreatureFlag.CAN_WALK_ON_LAVA, Creature.CreatureFlag.CAN_FLY)));
    private final InfernoSystem system = new InfernoSystem(entityData, positionLookup(), kwdFile());

    @Test
    void burnsEveryTick() {
        EntityId fire = fire(new Point(5, 5));
        EntityId inTheFire = creature(new Point(5, 5));

        system.processTick(TICK);
        assertEquals(DAMAGE, damage(inTheFire));
        system.processTick(TICK);
        assertEquals(2 * DAMAGE, damage(inTheFire));
        assertEquals(2, entityData.getComponent(fire, InfernoArea.class).burns);
    }

    @Test
    void firstBurnReachesHalfTheArea() {
        fire(new Point(5, 5));

        // 0.8 tiles away: outside the first burn's reach of 0.71 tiles, inside the 1 tile after it
        EntityId creature = creature(new Vector3f(5.8f, WorldUtils.FLOOR_HEIGHT, 5f), TROLL);

        system.processTick(TICK);
        assertEquals(0, damage(creature));
        system.processTick(TICK);
        assertEquals(DAMAGE, damage(creature));
    }

    @Test
    void creaturesOutOfReachAndObjectsAreNotBurnt() {
        fire(new Point(5, 5));

        // The reach is less than a tile, the middle of the next tile is just out of it
        EntityId nextTile = creature(new Point(6, 5));
        EntityId farAway = creature(new Point(7, 5));
        EntityId chest = entityData.createEntity();
        place(chest, new Point(5, 5));
        entityData.setComponent(chest, new Health(100, 100));

        system.processTick(TICK);
        system.processTick(TICK);

        assertNull(entityData.getComponent(nextTile, Damage.class));
        assertNull(entityData.getComponent(farAway, Damage.class));
        assertNull(entityData.getComponent(chest, Damage.class));
    }

    @Test
    void fireproofCreaturesAreNotBurnt() {
        fire(new Point(5, 5));
        EntityId salamander = creature(new Point(5, 5), SALAMANDER);
        EntityId stoneKnight = creature(new Point(5, 5), STONE_KNIGHT);
        EntityId horny = creature(new Point(5, 5), HORNY);

        system.processTick(TICK);

        assertNull(entityData.getComponent(salamander, Damage.class));
        assertNull(entityData.getComponent(stoneKnight, Damage.class));
        assertNull(entityData.getComponent(horny, Damage.class));
    }

    @Test
    void flyingCreaturesBurnEvenIfTheyWalkOnLava() {
        fire(new Point(5, 5));
        EntityId firefly = creature(new Point(5, 5), FIREFLY);

        system.processTick(TICK);

        assertEquals(DAMAGE, damage(firefly));
    }

    @Test
    void fireThatIsOutBurnsNoMore() {
        EntityId fire = fire(new Point(5, 5));
        EntityId creature = creature(new Point(5, 5));
        system.processTick(TICK);

        entityData.removeEntity(fire);
        system.processTick(TICK);
        system.processTick(TICK);

        assertEquals(DAMAGE, damage(creature));
    }

    private EntityId fire(Point tile) {
        EntityId fire = entityData.createEntity();
        entityData.setComponents(fire, new InfernoArea(DAMAGE, 1, 0), at(tile));
        return fire;
    }

    private EntityId creature(Vector3f position, short creatureId) {
        EntityId creature = entityData.createEntity();
        CreatureComponent creatureComponent = new CreatureComponent();
        creatureComponent.creatureId = creatureId;
        entityData.setComponents(creature, creatureComponent, new Health(1000, 1000), new Position(0, position));
        entitiesByTile.computeIfAbsent(WorldUtils.vectorToPoint(position), p -> new ArrayList<>()).add(creature);
        return creature;
    }

    private EntityId creature(Point tile) {
        return creature(tile, TROLL);
    }

    private EntityId creature(Point tile, short creatureId) {
        EntityId creature = entityData.createEntity();
        CreatureComponent creatureComponent = new CreatureComponent();
        creatureComponent.creatureId = creatureId;
        entityData.setComponents(creature, creatureComponent, new Health(1000, 1000));
        place(creature, tile);
        return creature;
    }

    private void place(EntityId entity, Point tile) {
        entityData.setComponent(entity, at(tile));
        entitiesByTile.computeIfAbsent(tile, p -> new ArrayList<>()).add(entity);
    }

    private static Position at(Point tile) {
        Vector3f position = WorldUtils.pointToVector3f(tile);
        position.y = WorldUtils.FLOOR_HEIGHT;
        return new Position(0, position);
    }

    private int damage(EntityId entity) {
        Damage damage = entityData.getComponent(entity, Damage.class);
        return damage != null ? damage.damage : 0;
    }

    private IKwdFile kwdFile() {
        return (IKwdFile) Proxy.newProxyInstance(IKwdFile.class.getClassLoader(), new Class<?>[]{IKwdFile.class},
                (proxy, method, args) -> switch (method.getName()) {
            case "getCreature" -> creatures.get((short) args[0]);
            default -> throw new UnsupportedOperationException(method.getName());
        });
    }

    private IEntityPositionLookup positionLookup() {
        return (IEntityPositionLookup) Proxy.newProxyInstance(IEntityPositionLookup.class.getClassLoader(), new Class<?>[]{IEntityPositionLookup.class},
                (proxy, method, args) -> switch (method.getName()) {
            case "getEntitiesInLocation" -> args.length == 2
                    ? entitiesByTile.getOrDefault(new Point((int) args[0], (int) args[1]), List.of())
                    : entitiesByTile.getOrDefault((Point) args[0], List.of());
            default -> throw new UnsupportedOperationException(method.getName());
        });
    }

}
