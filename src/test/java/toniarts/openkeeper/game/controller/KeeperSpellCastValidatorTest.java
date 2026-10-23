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
import toniarts.openkeeper.game.map.FakeMapData;
import toniarts.openkeeper.game.map.FakeMapInformation;
import toniarts.openkeeper.game.map.FakeMapTile;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.KeeperSpell;
import toniarts.openkeeper.tools.convert.map.ShotFixtures;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KeeperSpellCastValidatorTest {

    private static final short PLAYER_ID = 3;
    private static final short ENEMY_ID = 4;

    private final EntityData entityData = new DefaultEntityData();
    private final FakeMapInformation mapInformation = new FakeMapInformation(new FakeMapData(4, 4), Map.of());
    private final IKwdFile kwdFile = (IKwdFile) Proxy.newProxyInstance(IKwdFile.class.getClassLoader(), new Class<?>[]{IKwdFile.class},
            (proxy, method, args) -> method.getName().equals("getCreature") ? new Creature() : null);
    private final KeeperSpell thunderbolt = ShotFixtures.keeperSpell(KeeperSpell.CastRule.OWN_LAND, KeeperSpell.TargetRule.ENEMY_CREATURES, 6000);
    private final Keeper player = new Keeper(false, PLAYER_ID);
    private final FakeMapTile ownTile;

    KeeperSpellCastValidatorTest() {
        player.setMana(100000);
        ownTile = mapInformation.getMapData().getTile(1, 1);
        ownTile.ownerId = PLAYER_ID;
    }

    @Test
    void enemyCreatureCanBeStruck() {
        EntityId enemyCreature = createThing(ENEMY_ID, true);

        assertTrue(isValidCast(enemyCreature));
    }

    @Test
    void enemyThatIsNotACreatureCannotBeStruck() {
        EntityId enemyDoor = createThing(ENEMY_ID, false);

        assertFalse(isValidCast(enemyDoor));
    }

    @Test
    void ownCreatureCannotBeStruck() {
        EntityId ownCreature = createThing(PLAYER_ID, true);

        assertFalse(isValidCast(ownCreature));
    }

    @Test
    void notEnoughManaCannotStrike() {
        EntityId enemyCreature = createThing(ENEMY_ID, true);
        player.setMana(5999);

        assertFalse(isValidCast(enemyCreature));
    }

    private boolean isValidCast(EntityId target) {
        return KeeperSpellCastValidator.isValidCast(thunderbolt, kwdFile, mapInformation, ownTile, player, entityData, target);
    }

    private EntityId createThing(short ownerId, boolean creature) {
        EntityId thing = entityData.createEntity();
        entityData.setComponent(thing, new Owner(ownerId, ownerId));
        if (creature) {
            entityData.setComponent(thing, new CreatureComponent());
        }
        return thing;
    }

}
