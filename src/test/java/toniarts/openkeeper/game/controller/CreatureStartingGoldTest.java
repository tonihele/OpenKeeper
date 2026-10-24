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
package toniarts.openkeeper.game.controller;

import com.jme3.math.Vector2f;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.Gold;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.CreatureGoldFixtures;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.Player;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The gold a creature carries when it is created. In the original a new
 * creature carries nothing (0x4872A0 clears it); only a creature placed in the
 * editor is given its gold held percentage of its max gold held (0x520180,
 * 0x520460, 0x5208B0). The "initial gold held" attribute of the creature
 * record is never read.
 */
class CreatureStartingGoldTest {

    private static final short IMP_ID = 1;
    private static final short KNIGHT_ID = 22;
    private static final int MAX_GOLD = 1400;
    private static final short INITIAL_GOLD = 500;

    private final EntityData entityData = new DefaultEntityData();
    private final Creature imp = CreatureGoldFixtures.goldCreature(IMP_ID, MAX_GOLD, INITIAL_GOLD);
    private final Creature knight = CreatureGoldFixtures.goldCreature(KNIGHT_ID, MAX_GOLD, INITIAL_GOLD);
    private final CreaturesController controller = new CreaturesController(kwdFile(), entityData,
            Collections.emptyMap(), stub(IGameTimer.class), null, stub(IMapController.class), null);

    @Test
    void placedKeeperCreatureWithoutGoldCarriesNone() {
        EntityId imp = controller.spawnCreature(CreatureGoldFixtures.placedKeeperCreature(IMP_ID, Player.KEEPER1_ID, 0),
                new Vector2f(5, 5));

        assertEquals(0, carriedGold(imp));
    }

    @Test
    void placedHeroCarriesItsPercentageOfMaxGold() {
        EntityId hero = controller.spawnCreature(CreatureGoldFixtures.placedHero(KNIGHT_ID, 50), new Vector2f(5, 5));

        assertEquals(MAX_GOLD / 2, carriedGold(hero));
    }

    @Test
    void creatureCreatedInGameCarriesNone() {
        EntityId imp = controller.spawnCreature(IMP_ID, Player.KEEPER1_ID, 1, new Vector2f(5, 5), ICreaturesController.SpawnType.ENTRANCE);

        assertEquals(0, carriedGold(imp));
    }

    @Test
    void levellingUpKeepsTheCarriedGold() {
        EntityId hero = controller.spawnCreature(CreatureGoldFixtures.placedHero(KNIGHT_ID, 25), new Vector2f(5, 5));

        controller.levelUpCreature(hero, 2, 0);

        assertEquals(MAX_GOLD / 4, carriedGold(hero));
    }

    private int carriedGold(EntityId entityId) {
        return entityData.getComponent(entityId, Gold.class).gold;
    }

    private IKwdFile kwdFile() {
        return (IKwdFile) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{IKwdFile.class},
                (proxy, method, args) -> switch (method.getName()) {
            case "getCreature" ->
                ((Short) args[0]) == IMP_ID ? imp : knight;
            case "getImp" ->
                imp;
            case "getThings" ->
                List.of();
            default ->
                defaultValue(method.getReturnType());
        });
    }

    private static <T> T stub(Class<T> type) {
        return type.cast(Proxy.newProxyInstance(CreatureStartingGoldTest.class.getClassLoader(), new Class<?>[]{type},
                (proxy, method, args) -> defaultValue(method.getReturnType())));
    }

    private static Object defaultValue(Class<?> type) {
        if (type == boolean.class) {
            return false;
        }
        if (type == double.class) {
            return 0d;
        }
        if (type == float.class) {
            return 0f;
        }
        if (type == long.class) {
            return 0L;
        }
        if (type == int.class) {
            return 0;
        }
        if (type == short.class) {
            return (short) 0;
        }
        if (type == byte.class) {
            return (byte) 0;
        }
        return null;
    }
}
