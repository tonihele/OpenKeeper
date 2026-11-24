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
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.Damage;
import toniarts.openkeeper.game.component.Health;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Regeneration;
import toniarts.openkeeper.game.controller.ILevelInfo;
import toniarts.openkeeper.game.event.GameEventService;
import toniarts.openkeeper.game.map.IMapTileInformation;
import toniarts.openkeeper.tools.convert.map.CreatureSleepFixtures;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.Variable.MiscVariable;
import toniarts.openkeeper.tools.convert.map.Variable.MiscVariable.MiscType;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OwnLandRegenerationTest {

    private static final short OWNER = 3;

    /**
     * An Imp fighting in its own Dungeon Heart keeps regaining its own land
     * health on every tick while it is hit
     */
    @Test
    void ownLandRegenerationGoesOnWhileBeingHit() {
        EntityData entityData = new DefaultEntityData();
        EntityId imp = entityData.createEntity();
        entityData.setComponents(imp, new Health(300, 1000), new Regeneration(67, null), new Owner(OWNER, OWNER),
                new CreatureComponent());

        // 67 a second is 16 a tick at four ticks a second, as DK II rounds it down (0x492320)
        assertEquals(300 - 40 * 5 + 40 * 16, healthAfterTenSecondsOfHits(entityData, imp));
    }

    /**
     * Something that isn't a creature, such as the Dungeon Heart, regains
     * its health once a second, also while it is hit
     */
    @Test
    void otherRegenerationGoesOnWhileBeingHit() {
        EntityData entityData = new DefaultEntityData();
        EntityId heart = entityData.createEntity();
        entityData.setComponents(heart, new Health(300, 1000), new Regeneration(67, null), new Owner(OWNER, OWNER));

        // The first second only starts the count, then 67 every second
        assertEquals(300 - 40 * 5 + 9 * 67, healthAfterTenSecondsOfHits(entityData, heart));
    }

    private static int healthAfterTenSecondsOfHits(EntityData entityData, EntityId entity) {
        HealthSystem healthSystem = healthSystem(entityData);

        // Ten seconds at four ticks a second, hit for 5 on every tick
        for (int tick = 0; tick < 40; tick++) {
            entityData.setComponent(entity, new Damage(5));
            healthSystem.processTick(0.25f);
        }
        return entityData.getComponent(entity, Health.class).health;
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

        IKwdFile kwd = proxy(IKwdFile.class, method -> switch (method) {
            case "getGameLevel" -> CreatureSleepFixtures.level(4);
            case "getCreature" -> CreatureSleepFixtures.sleeper(1);
            default -> throw new UnsupportedOperationException(method);
        });
        ILevelInfo levelInfo = proxy(ILevelInfo.class, method -> switch (method) {
            case "getLevelData" -> kwd;
            default -> throw new UnsupportedOperationException(method);
        });

        return new HealthSystem(entityData, lookup, settings, null, levelInfo, List.of(), null, () -> 0.0, events);
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, Function<String, Object> answer) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type},
                (proxy, method, args) -> answer.apply(method.getName()));
    }

}
