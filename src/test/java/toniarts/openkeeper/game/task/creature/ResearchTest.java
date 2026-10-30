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
package toniarts.openkeeper.game.task.creature;

import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.controller.player.PlayerResearchControl;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.game.controller.room.storage.RoomSpellBookControl;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.data.ResearchableEntity;
import toniarts.openkeeper.game.data.ResearchableType;
import toniarts.openkeeper.utils.Point;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResearchTest {

    @Test
    void researchInSoldLibraryIsNoLongerValid() {
        EntityData entityData = new DefaultEntityData();
        EntityId roomEntity = entityData.createEntity();
        boolean[] removed = {false};
        RoomSpellBookControl[] spellBooks = new RoomSpellBookControl[1];
        IRoomController library = (IRoomController) Proxy.newProxyInstance(
                IRoomController.class.getClassLoader(), new Class<?>[]{IRoomController.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "getEntityId" -> roomEntity;
                    case "getOwnerId" -> (short) 1;
                    case "isRemoved" -> removed[0];
                    case "isDestroyed" -> false;
                    case "getObjectControl" -> spellBooks[0];
                    default -> throw new UnsupportedOperationException(method.getName());
                });
        spellBooks[0] = new RoomSpellBookControl(null, library, entityData, () -> 0) {
            @Override
            protected int getNumberOfAccessibleTiles() {
                return 1;
            }
        };
        Keeper keeper = new Keeper();
        keeper.setCurrentResearch(new ResearchableEntity((short) 1, ResearchableType.SPELL));
        Research research = new Research(null, null, new Point(1, 1), (short) 1, library, null,
                new PlayerResearchControl(keeper, null), null);

        assertTrue(research.isValid(null));

        // Selling the library destroys its spell book storage with it
        spellBooks[0].destroy();
        removed[0] = true;

        assertFalse(research.isValid(null));
        assertTrue(research.isRemovable());
    }
}
