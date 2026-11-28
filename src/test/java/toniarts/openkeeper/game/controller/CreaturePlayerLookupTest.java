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
import com.simsilica.util.ReportSystem;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.CreatureMood;
import toniarts.openkeeper.game.component.Health;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Possessed;
import toniarts.openkeeper.tools.convert.map.IKwdFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * The lookups of one player's creatures: they take the creatures of that
 * player only, and they leave no entity set behind in the entity data.
 */
class CreaturePlayerLookupTest {

    private static final short PLAYER = 3;
    private static final short ENEMY = 4;

    private final EntityData entityData = new DefaultEntityData();
    private final CreaturesController controller = new CreaturesController(kwdFile(), entityData,
            Collections.emptyMap(), null, null, null, null);

    @Test
    void makeHappyCalmsOnlyThePlayersCreatures() {
        EntityId own = creature(PLAYER);
        EntityId enemy = creature(ENEMY);

        controller.makeCreaturesHappy(PLAYER);

        assertEquals(0, entityData.getComponent(own, CreatureMood.class).general);
        assertEquals(100, entityData.getComponent(enemy, CreatureMood.class).general);
    }

    @Test
    void makeUnhappyAngersOnlyTheOtherPlayersCreatures() {
        EntityId own = creature(PLAYER);
        EntityId enemy = creature(ENEMY);

        controller.angerEnemyCreatures(PLAYER);

        assertEquals(100, entityData.getComponent(own, CreatureMood.class).general);
        assertEquals(CreatureMood.MAX_ANGER, entityData.getComponent(enemy, CreatureMood.class).general);
    }

    @Test
    void healsOnlyThePlayersCreatures() {
        EntityId own = creature(PLAYER);
        EntityId enemy = creature(ENEMY);

        controller.healCreatures(PLAYER);

        assertEquals(50, entityData.getComponent(own, Health.class).health);
        assertEquals(10, entityData.getComponent(enemy, Health.class).health);
    }

    @Test
    void findsThePlayersPossessedCreature() {
        creature(PLAYER);
        EntityId possessed = creature(PLAYER);
        entityData.setComponent(possessed, new Possessed(1, 0));

        assertEquals(possessed, controller.getPossessedCreature(PLAYER));
        assertNull(controller.getPossessedCreature(ENEMY));
    }

    @Test
    void leavesNoEntitySetBehind() {
        creature(PLAYER);
        creature(ENEMY);
        String entitySets = ReportSystem.getReport(ReportSystem.REPORT_CACHE);

        controller.makeCreaturesHappy(PLAYER);
        controller.angerEnemyCreatures(PLAYER);
        controller.healCreatures(PLAYER);
        controller.getPossessedCreature(PLAYER);

        assertEquals(entitySets, ReportSystem.getReport(ReportSystem.REPORT_CACHE));
    }

    private EntityId creature(short owner) {
        EntityId entityId = entityData.createEntity();
        entityData.setComponents(entityId, new CreatureComponent(), new Health(10, 50), new Owner(owner, owner),
                new CreatureMood(100, 0, 0, 0, 0, 0));
        return entityId;
    }

    private IKwdFile kwdFile() {
        return (IKwdFile) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{IKwdFile.class},
                (proxy, method, args) -> List.of());
    }
}
