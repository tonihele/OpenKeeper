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

import com.simsilica.es.Entity;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.EntitySet;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.EffectTrigger;
import toniarts.openkeeper.game.component.Health;
import toniarts.openkeeper.game.component.ObjectComponent;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.controller.ILevelInfo;
import toniarts.openkeeper.game.controller.entity.EntityController;
import toniarts.openkeeper.game.controller.entity.IEntityController;
import toniarts.openkeeper.game.controller.room.FiveByFiveRotatedController;
import toniarts.openkeeper.game.event.GameEventService;
import toniarts.openkeeper.tools.convert.map.DestroyedFixtures;
import toniarts.openkeeper.tools.convert.map.GameObject;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.TrapFiringFixtures;
import toniarts.openkeeper.tools.convert.map.Variable.MiscVariable.MiscType;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.WorldUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A destroyed object blows up with its death effect, like the Dungeon Heart
 * with the Heart Explosion Effect
 */
class ObjectDeathEffectTest {

    private static final short KEEPER = 3;
    private static final Point TILE = new Point(2, 2);

    private final EntityData entityData = new DefaultEntityData();
    private HealthSystem healthSystem;
    private EntityId heart;

    @BeforeEach
    void setUp() {
        heart = entityData.createEntity();
        entityData.setComponents(heart, new ObjectComponent(FiveByFiveRotatedController.OBJECT_HEART_ID, null),
                new Owner(KEEPER, KEEPER), new Position(0, WorldUtils.pointToVector3f(TILE)), new Health(1000, 1000));

        IEntityPositionLookup lookup = (IEntityPositionLookup) Proxy.newProxyInstance(IEntityPositionLookup.class.getClassLoader(),
                new Class<?>[]{IEntityPositionLookup.class}, (proxy, method, args) -> switch (method.getName()) {
            case "getEntityController" -> removing((EntityId) args[0]);
            default -> throw new UnsupportedOperationException(method.getName());
        });
        GameObject heartObject = DestroyedFixtures.object(FiveByFiveRotatedController.OBJECT_HEART_ID, DestroyedFixtures.HEART_EXPLOSION_ID);
        IKwdFile kwdFile = (IKwdFile) Proxy.newProxyInstance(IKwdFile.class.getClassLoader(),
                new Class<?>[]{IKwdFile.class}, (proxy, method, args) -> method.getName().equals("getObject") ? heartObject : null);
        ILevelInfo levelInfo = (ILevelInfo) Proxy.newProxyInstance(ILevelInfo.class.getClassLoader(),
                new Class<?>[]{ILevelInfo.class}, (proxy, method, args) -> switch (method.getName()) {
            case "getLevelData" -> kwdFile;
            default -> throw new UnsupportedOperationException(method.getName());
        });
        healthSystem = new HealthSystem(entityData, lookup, Map.of(
                MiscType.CREATURE_DYING_STATE_DURATION_SECONDS, TrapFiringFixtures.miscVariable(MiscType.CREATURE_DYING_STATE_DURATION_SECONDS, 10),
                MiscType.PRISON_MODIFY_CREATURE_HEALTH_PER_SECOND, TrapFiringFixtures.miscVariable(MiscType.PRISON_MODIFY_CREATURE_HEALTH_PER_SECOND, 0),
                MiscType.MODIFY_HEALTH_OF_CREATURE_IN_LAIR_PER_SECOND, TrapFiringFixtures.miscVariable(MiscType.MODIFY_HEALTH_OF_CREATURE_IN_LAIR_PER_SECOND, 0)),
                null, levelInfo, List.of(), null, (GameEventService) Proxy.newProxyInstance(GameEventService.class.getClassLoader(), new Class<?>[]{GameEventService.class}, (proxy, method, args) -> null));
        healthSystem.processTick(0.25f);
    }

    @Test
    void destroyedHeartBlowsUpWithItsDeathEffect() {
        EntityController.setDamage(entityData, heart, 1000);
        healthSystem.processTick(0.25f);

        EntitySet effects = entityData.getEntities(EffectTrigger.class, Position.class);
        assertEquals(1, effects.size());
        Entity effect = effects.iterator().next();
        assertEquals(DestroyedFixtures.HEART_EXPLOSION_ID, effect.get(EffectTrigger.class).effectId);
        assertEquals(WorldUtils.pointToVector3f(TILE), effect.get(Position.class).position);
        effects.release();
    }

    @Test
    void damagedHeartDoesNotBlowUp() {
        EntityController.setDamage(entityData, heart, 999);
        healthSystem.processTick(0.25f);

        EntitySet effects = entityData.getEntities(EffectTrigger.class);
        assertEquals(0, effects.size());
        effects.release();
    }

    private IEntityController removing(EntityId entityId) {
        return (IEntityController) Proxy.newProxyInstance(IEntityController.class.getClassLoader(),
                new Class<?>[]{IEntityController.class}, (proxy, method, args) -> switch (method.getName()) {
            case "remove" -> {
                entityData.removeEntity(entityId);
                yield null;
            }
            default -> throw new UnsupportedOperationException(method.getName());
        });
    }

}
