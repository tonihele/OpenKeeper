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
package toniarts.openkeeper.game.controller.player;

import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.EntitySet;
import com.simsilica.es.base.DefaultEntityData;
import com.simsilica.es.filter.FieldFilter;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.Gold;
import toniarts.openkeeper.game.component.KeeperComponent;
import toniarts.openkeeper.game.data.Keeper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerGoldControlTest {

    private static final short PLAYER_ID = 3;
    private static final short OTHER_PLAYER_ID = 4;

    private final EntityData entityData = new DefaultEntityData();
    private final EntityId playerEntity = createKeeperEntity(PLAYER_ID);
    private final EntityId otherPlayerEntity = createKeeperEntity(OTHER_PLAYER_ID);
    private final Keeper keeper = new Keeper(false, PLAYER_ID);
    private final PlayerGoldControl goldControl = new PlayerGoldControl(keeper, entityData);

    @Test
    void addedGoldIsCappedByTheMaximum() {
        goldControl.setGoldMax(1000);
        goldControl.addGold(1500);

        Gold gold = entityData.getComponent(playerEntity, Gold.class);
        assertEquals(1000, gold.gold);
        assertEquals(1000, gold.maxGold);
        assertEquals(1000, goldControl.getGold());
        assertTrue(goldControl.isFullCapacity());
        assertEquals(1500, keeper.getGoldMined());
    }

    @Test
    void goldIsOnlySubtractedWhenThereIsEnough() {
        goldControl.setGoldMax(1000);
        goldControl.addGold(500);

        assertFalse(goldControl.subGold(600));
        assertEquals(500, entityData.getComponent(playerEntity, Gold.class).gold);

        assertTrue(goldControl.subGold(200));
        assertEquals(300, entityData.getComponent(playerEntity, Gold.class).gold);
        assertFalse(goldControl.isFullCapacity());
    }

    @Test
    void onlyThePlayersOwnEntityChanges() {
        goldControl.setGoldMax(1000);
        goldControl.addGold(500);

        Gold otherGold = entityData.getComponent(otherPlayerEntity, Gold.class);
        assertEquals(0, otherGold.gold);
        assertEquals(0, otherGold.maxGold);
    }

    @Test
    void clientWatchingThePlayerEntitySeesTheGold() {
        EntitySet keeperEntity = entityData.getEntities(new FieldFilter(KeeperComponent.class, "playerId", PLAYER_ID), KeeperComponent.class, Gold.class);
        goldControl.setGoldMax(1000);
        goldControl.addGold(250);

        assertTrue(keeperEntity.applyChanges());
        assertEquals(1, keeperEntity.size());
        assertEquals(250, keeperEntity.iterator().next().get(Gold.class).gold);
        keeperEntity.release();
    }

    private EntityId createKeeperEntity(short playerId) {
        EntityId entityId = entityData.createEntity();
        entityData.setComponents(entityId, new KeeperComponent(playerId), new Gold(0, 0));
        return entityId;
    }

}
