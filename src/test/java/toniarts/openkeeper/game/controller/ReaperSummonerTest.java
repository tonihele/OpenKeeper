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
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.SummonedReaper;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.KeeperSpell;
import toniarts.openkeeper.tools.convert.map.Shot;
import toniarts.openkeeper.tools.convert.map.SummonReaperFixtures;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class ReaperSummonerTest {

    private static final short PLAYER_ID = 3;
    private static final short REAPER_ID = 27;
    private static final short REAPER_SHOT_ID = 46;
    private static final double NOW = 120;

    private final EntityData entityData = new DefaultEntityData();
    private final List<Object[]> spawns = new ArrayList<>();
    private final List<Creature> creatures = new ArrayList<>(List.of(
            SummonReaperFixtures.creature((short) 1, Creature.CreatureFlag.IS_WORKER),
            SummonReaperFixtures.creature(REAPER_ID, Creature.CreatureFlag.IS_HORNY)));
    private final List<KeeperSpell> keeperSpells = List.of(
            SummonReaperFixtures.keeperSpell((short) 4, (short) 2, 1, 1500, 7),
            SummonReaperFixtures.keeperSpell((short) 5, REAPER_SHOT_ID, 0, 100000, 2000),
            SummonReaperFixtures.keeperSpell((short) 14, REAPER_SHOT_ID, 99, 0, 125));

    @Test
    void summonsTheHornedReaperAtTheCastPoint() {
        Vector2f position = new Vector2f(10.5f, 4.5f);

        EntityId reaper = summoner().summon(REAPER_SHOT_ID, 0, PLAYER_ID, position);

        assertEquals(1, spawns.size());
        Object[] spawn = spawns.get(0);
        assertEquals(REAPER_ID, spawn[0]);
        assertEquals(PLAYER_ID, spawn[1]);
        assertEquals(position, spawn[3]);
        assertEquals(ICreaturesController.SpawnType.CONJURE, spawn[4]);
        assertNotNull(entityData.getComponent(reaper, SummonedReaper.class));
    }

    @Test
    void reaperSummonedBySpellIsLevelTenAndPaidWithMana() {
        EntityId reaper = summoner().summon(REAPER_SHOT_ID, 0, PLAYER_ID, new Vector2f(1, 1));

        assertEquals(10, spawns.get(0)[2]);
        SummonedReaper summonedReaper = entityData.getComponent(reaper, SummonedReaper.class);
        assertEquals(NOW, summonedReaper.summonTime);
        assertEquals(0, summonedReaper.lifetime);

        // The drain is the one of the spell that summons the Reaper for good
        assertEquals(2000, summonedReaper.manaDrain);
    }

    @Test
    void oneShotReaperUsesTheLevelOfTheMapAndStaysForTheShotTime() {
        EntityId reaper = summoner().summon(REAPER_SHOT_ID, 99, PLAYER_ID, new Vector2f(1, 1));

        assertEquals(6, spawns.get(0)[2]);
        SummonedReaper summonedReaper = entityData.getComponent(reaper, SummonedReaper.class);
        assertEquals(99, summonedReaper.lifetime);
        assertEquals(0, summonedReaper.manaDrain);
    }

    @Test
    void nothingIsSummonedWithoutReaperCreature() {
        creatures.remove(1);

        assertNull(summoner().summon(REAPER_SHOT_ID, 0, PLAYER_ID, new Vector2f(1, 1)));
        assertEquals(0, spawns.size());
    }

    private ReaperSummoner summoner() {
        return new ReaperSummoner(kwdFile(), entityData, () -> NOW, creaturesController());
    }

    private IKwdFile kwdFile() {
        return (IKwdFile) Proxy.newProxyInstance(IKwdFile.class.getClassLoader(), new Class<?>[]{IKwdFile.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getCreatureList" -> creatures;
                    case "getKeeperSpells" -> keeperSpells;
                    case "getShotById" -> SummonReaperFixtures.shot((short) args[0], Shot.ProcessType.SUMMON_REAPER);
                    case "getGameLevel" -> SummonReaperFixtures.gameLevel((short) 6);
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }

    private ICreaturesController creaturesController() {
        return (ICreaturesController) Proxy.newProxyInstance(ICreaturesController.class.getClassLoader(),
                new Class<?>[]{ICreaturesController.class}, (proxy, method, args) -> {
                    if (!method.getName().equals("spawnCreature") || args.length != 5) {
                        throw new UnsupportedOperationException(method.getName());
                    }
                    spawns.add(args);
                    return entityData.createEntity();
                });
    }

}
