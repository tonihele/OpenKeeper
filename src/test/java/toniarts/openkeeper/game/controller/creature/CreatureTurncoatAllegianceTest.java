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

import com.jme3.math.Vector3f;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import java.util.LinkedHashSet;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.AttackTarget;
import toniarts.openkeeper.game.component.Health;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.component.Turncoat;
import toniarts.openkeeper.game.controller.ICreaturesController;
import toniarts.openkeeper.game.controller.ILevelInfo;
import toniarts.openkeeper.game.controller.entity.IEntityController;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.logic.IEntityPositionLookup;
import toniarts.openkeeper.tools.convert.map.SpellTargetFixtures;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * A turncoat creature fights for the player who turned it, and its old
 * friends see it as an enemy
 */
class CreatureTurncoatAllegianceTest {

    private static final short CASTER = 3;
    private static final short VICTIM_OWNER = 4;

    private EntityData entityData;
    private Set<EntityId> sensedEntities;

    @BeforeEach
    void setUp() {
        entityData = new DefaultEntityData();
        sensedEntities = new LinkedHashSet<>();
    }

    @Test
    void turncoatAttacksItsOwnersCreatures() {
        EntityId turncoat = creature(VICTIM_OWNER);
        entityData.setComponent(turncoat, new Turncoat(CASTER, 125));
        EntityId oldFriend = creature(VICTIM_OWNER);
        sensedEntities.add(oldFriend);

        IEntityController attackTarget = controller(turncoat).getAttackTarget();

        assertNotNull(attackTarget);
        assertEquals(oldFriend, attackTarget.getEntityId());
    }

    @Test
    void casterCreaturesLeaveTheTurncoatAlone() {
        EntityId casterCreature = creature(CASTER);
        EntityId turncoat = creature(VICTIM_OWNER);
        entityData.setComponent(turncoat, new Turncoat(CASTER, 125));
        sensedEntities.add(turncoat);

        assertNull(controller(casterCreature).getAttackTarget());
    }

    @Test
    void oldFriendsAttackTheTurncoat() {
        EntityId oldFriend = creature(VICTIM_OWNER);
        EntityId turncoat = creature(VICTIM_OWNER);
        entityData.setComponent(turncoat, new Turncoat(CASTER, 125));
        sensedEntities.add(turncoat);

        IEntityController attackTarget = controller(oldFriend).getAttackTarget();

        assertNotNull(attackTarget);
        assertEquals(turncoat, attackTarget.getEntityId());
    }

    @Test
    void creatureStopsAttackingWhenTheTargetIsNoLongerAnEnemy() {
        EntityId casterCreature = creature(CASTER);
        EntityId enemy = creature(VICTIM_OWNER);
        entityData.setComponent(casterCreature, new AttackTarget(enemy));
        entityData.setComponent(enemy, new Turncoat(CASTER, 125));

        assertNull(controller(casterCreature).getAttackTarget());
        assertNull(entityData.getComponent(casterCreature, AttackTarget.class));
    }

    private EntityId creature(short ownerId) {
        EntityId entity = entityData.createEntity();
        entityData.setComponent(entity, new Owner(ownerId, ownerId));
        entityData.setComponent(entity, new Health(100, 100));
        entityData.setComponent(entity, new Position(0, new Vector3f(1.5f, 0, 1.5f)));
        return entity;
    }

    private CreatureController controller(EntityId entity) {
        ICreaturesController creaturesController = proxy(ICreaturesController.class, (method, args) -> switch (method) {
            case "isValidEntity" -> true;
            case "createController" -> creatureController((EntityId) args[0]);
            default -> null;
        });
        IEntityPositionLookup entityPositionLookup = proxy(IEntityPositionLookup.class, (method, args) -> switch (method) {
            case "getSensedEntities" -> sensedEntities;
            default -> null;
        });
        ILevelInfo levelInfo = proxy(ILevelInfo.class, (method, args) -> switch (method) {
            case "getPlayer" -> new Keeper(false, (short) args[0]);
            default -> null;
        });

        return new CreatureController(entity, entityData, SpellTargetFixtures.creature((short) 1), null, null, null, null,
                creaturesController, entityPositionLookup, null, levelInfo, null, null);
    }

    private static ICreatureController creatureController(EntityId entity) {
        return proxy(ICreatureController.class, (method, args) -> switch (method) {
            case "getEntityId" -> entity;
            default -> null;
        });
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, Handler handler) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type},
                (proxy, method, args) -> switch (method.getName()) {
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    default -> handler.invoke(method.getName(), args);
                });
    }

    @FunctionalInterface
    private interface Handler {

        Object invoke(String method, Object[] args);
    }

}
