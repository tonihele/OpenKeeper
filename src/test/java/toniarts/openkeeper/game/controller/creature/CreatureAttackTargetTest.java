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
package toniarts.openkeeper.game.controller.creature;

import com.badlogic.gdx.ai.pfa.DefaultGraphPath;
import com.jme3.math.Vector3f;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.AttackTarget;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.CreatureMeleeAttack;
import toniarts.openkeeper.game.component.Health;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.controller.ICreaturesController;
import toniarts.openkeeper.game.logic.IEntityPositionLookup;
import toniarts.openkeeper.game.navigation.INavigationService;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.ShotFixtures;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * A fighting creature drops a target it can neither reach nor walk to, like
 * DK II's fight state picks its target again whenever it is out of reach,
 * only from those it can walk to (0x45F030, 0x45CB00)
 */
class CreatureAttackTargetTest {

    private final EntityData entityData = new DefaultEntityData();

    @Test
    void aTargetWithNoWayToItIsDropped() {
        EntityId target = createCreature((short) 3, 14, 23);
        EntityId attacker = createCreature((short) 1, 13, 22);
        entityData.setComponent(attacker, new AttackTarget(target));

        // Diagonally past two walls: out of reach, and there is no path
        assertNull(createController(attacker, false).getAttackTarget());
        assertNull(entityData.getComponent(attacker, AttackTarget.class));
    }

    @Test
    void aTargetOutOfReachWithAWayToItIsKept() {
        EntityId target = createCreature((short) 3, 14, 23);
        EntityId attacker = createCreature((short) 1, 13, 22);
        entityData.setComponent(attacker, new AttackTarget(target));

        createController(attacker, true).getAttackTarget();

        assertEquals(target, entityData.getComponent(attacker, AttackTarget.class).entityId);
    }

    private EntityId createCreature(short owner, float x, float y) {
        EntityId entity = entityData.createEntity();
        CreatureComponent creatureComponent = new CreatureComponent();
        creatureComponent.creatureId = 14;
        entityData.setComponents(entity, creatureComponent, new Owner(owner, owner), new Health(100, 100),
                new Position(0, new Vector3f(x, 0, y)), new CreatureMeleeAttack(0, 10, 1, 0.6f));
        return entity;
    }

    private CreatureController createController(EntityId entity, boolean pathExists) {
        Creature creature = ShotFixtures.creature((short) 14, EnumSet.noneOf(Creature.CreatureFlag.class), EnumSet.noneOf(Creature.CreatureFlag3.class));
        INavigationService navigationService = stub(INavigationService.class,
                (name, args) -> "findPath".equals(name) && pathExists ? new DefaultGraphPath<>() : null);
        ICreaturesController creaturesController = stub(ICreaturesController.class, (name, args) -> switch (name) {
            case "isValidEntity" -> true;
            case "createController" -> stub(ICreatureController.class, (method, params) -> null);
            default -> null;
        });
        return new CreatureController(entity, entityData, creature, navigationService, null, () -> 0, Map.of(),
                creaturesController, stub(IEntityPositionLookup.class,
                        (name, args) -> "getSensedEntities".equals(name) ? Set.of() : null),
                null, null, null, null, null);
    }

    @FunctionalInterface
    private interface Answer {

        Object answer(String name, Object[] args);
    }

    @SuppressWarnings("unchecked")
    private static <T> T stub(Class<T> type, Answer answer) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, (proxy, method, args) -> {
            Object result = answer.answer(method.getName(), args);
            if (result == null && method.getReturnType() == boolean.class) {
                return false;
            }
            return result;
        });
    }

}
