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

import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import java.util.Map;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.KeeperSpell;
import toniarts.openkeeper.tools.convert.map.Shot;
import toniarts.openkeeper.tools.convert.map.SummonReaperFixtures;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Summon Reaper spell's own cast rule: one Horned Reaper at a time. Checked
 * on its own so the test doesn't depend on the rest of the cast validation.
 */
class SummonReaperCastTest {

    private static final short PLAYER_ID = 3;
    private static final short OTHER_PLAYER_ID = 4;
    private static final short REAPER_ID = 27;
    private static final short IMP_ID = 1;
    private static final short REAPER_SHOT_ID = 46;
    private static final short IMP_SHOT_ID = 2;

    private final EntityData entityData = new DefaultEntityData();
    private final Keeper keeper = new Keeper(false, PLAYER_ID);
    private final Map<Short, Creature> creatures = Map.of(
            IMP_ID, SummonReaperFixtures.creature(IMP_ID, Creature.CreatureFlag.IS_WORKER),
            REAPER_ID, SummonReaperFixtures.creature(REAPER_ID, Creature.CreatureFlag.IS_HORNY));
    private final KeeperSpell summonReaper = SummonReaperFixtures.keeperSpell((short) 5, REAPER_SHOT_ID, 0, 100000, 2000);

    @Test
    void reaperCanBeSummoned() {
        addCreature(IMP_ID, PLAYER_ID);

        assertTrue(allowedByReaperRule(summonReaper));
    }

    @Test
    void onlyOneReaperAtATime() {
        addCreature(REAPER_ID, PLAYER_ID);

        assertFalse(allowedByReaperRule(summonReaper));
    }

    @Test
    void onlyOneReaperAtATimeWithTheTalisman() {
        addCreature(REAPER_ID, PLAYER_ID);

        assertFalse(allowedByReaperRule(SummonReaperFixtures.keeperSpell((short) 14, REAPER_SHOT_ID, 99, 0, 125)));
    }

    @Test
    void reaperOfAnotherKeeperDoesNotMatter() {
        addCreature(REAPER_ID, OTHER_PLAYER_ID);

        assertTrue(allowedByReaperRule(summonReaper));
    }

    @Test
    void otherSpellsCanBeCastWithReaper() {
        addCreature(REAPER_ID, PLAYER_ID);

        assertTrue(allowedByReaperRule(SummonReaperFixtures.keeperSpell((short) 4, IMP_SHOT_ID, 1, 1500, 7)));
    }

    private boolean allowedByReaperRule(KeeperSpell keeperSpell) {
        return KeeperSpellCastValidator.checkReaperRule(keeperSpell, kwdFile(), keeper, entityData);
    }

    private void addCreature(short creatureId, short ownerId) {
        EntityId entityId = entityData.createEntity();
        CreatureComponent creatureComponent = new CreatureComponent();
        creatureComponent.creatureId = creatureId;
        entityData.setComponent(entityId, creatureComponent);
        entityData.setComponent(entityId, new Owner(ownerId, ownerId));
    }

    private IKwdFile kwdFile() {
        return (IKwdFile) Proxy.newProxyInstance(IKwdFile.class.getClassLoader(), new Class<?>[]{IKwdFile.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getCreature" -> creatures.get((short) args[0]);
                    case "getShotById" -> SummonReaperFixtures.shot((short) args[0],
                            (short) args[0] == REAPER_SHOT_ID ? Shot.ProcessType.SUMMON_REAPER : Shot.ProcessType.CREATE_CREATURE);
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }

}
