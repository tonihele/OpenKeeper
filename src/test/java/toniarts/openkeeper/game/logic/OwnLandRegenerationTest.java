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

import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.Damage;
import toniarts.openkeeper.game.component.Health;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Regeneration;
import toniarts.openkeeper.game.event.GameEventService;
import toniarts.openkeeper.game.map.IMapTileInformation;
import toniarts.openkeeper.tools.convert.map.CreatureSleepFixtures;
import toniarts.openkeeper.tools.convert.map.Variable.MiscVariable;
import toniarts.openkeeper.tools.convert.map.Variable.MiscVariable.MiscType;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OwnLandRegenerationTest {

    private static final short OWNER = 3;

    /**
     * An Imp fighting in its own Dungeon Heart keeps regaining its own land
     * health every second while it is hit
     */
    @Test
    void ownLandRegenerationGoesOnWhileBeingHit() {
        EntityData entityData = new DefaultEntityData();
        EntityId imp = entityData.createEntity();
        entityData.setComponents(imp, new Health(300, 1000), new Regeneration(67, null), new Owner(OWNER, OWNER));
        HealthSystem healthSystem = healthSystem(entityData);

        // Ten seconds at four ticks a second, hit for 5 on every tick
        for (int tick = 0; tick < 40; tick++) {
            entityData.setComponent(imp, new Damage(5));
            healthSystem.processTick(0.25f);
        }

        // The first second only starts the count, then 67 every second
        assertEquals(300 - 40 * 5 + 9 * 67, entityData.getComponent(imp, Health.class).health);
    }

    private static HealthSystem healthSystem(EntityData entityData) {
        IMapTileInformation ownLand = proxy(IMapTileInformation.class, method -> switch (method) {
            case "getOwnerId" -> OWNER;
            default -> throw new UnsupportedOperationException(method);
        });
        IEntityPositionLookup lookup = proxy(IEntityPositionLookup.class, method -> switch (method) {
            case "getEntityLocation" -> ownLand;
            default -> throw new UnsupportedOperationException(method);
        });
        GameEventService events = proxy(GameEventService.class, method -> null);
        Map<MiscType, MiscVariable> settings = new EnumMap<>(MiscType.class);
        for (MiscType type : List.of(MiscType.CREATURE_DYING_STATE_DURATION_SECONDS,
                MiscType.PRISON_MODIFY_CREATURE_HEALTH_PER_SECOND,
                MiscType.MODIFY_HEALTH_OF_CREATURE_IN_LAIR_PER_SECOND)) {
            settings.put(type, CreatureSleepFixtures.variable(type, 0));
        }

        return new HealthSystem(entityData, lookup, settings, null, null, List.of(), null, () -> 0.0, events);
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, Function<String, Object> answer) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type},
                (proxy, method, args) -> answer.apply(method.getName()));
    }

}
