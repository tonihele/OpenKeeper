/* SPDX-License-Identifier: GPL-3.0-or-later */
package toniarts.openkeeper.tools.convert.map;

import com.simsilica.es.base.DefaultEntityData;
import java.util.EnumSet;
import java.util.Map;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.common.RoomInstance;
import toniarts.openkeeper.game.component.ImpGenerator;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.controller.room.FiveByFiveRotatedController;
import toniarts.openkeeper.utils.Point;
import static org.junit.jupiter.api.Assertions.*;

class DestroyedHeartCaptureTest {
    @Test
    void capturingBrokenHeartTransfersOwnershipWithoutRestartingImpSpawning() {
        var data = new DefaultEntityData();
        try {
            var type = new Room();
            type.setFlags(EnumSet.noneOf(Room.RoomFlag.class));
            var instance = new RoomInstance(type);
            for (int x = 0; x < 5; x++) {
                for (int y = 0; y < 5; y++) {
                    instance.addCoordinate(new Point(x, y));
                }
            }
            instance.setDestroyed(true);
            var entity = data.createEntity();
            var gold = Variable.MiscVariable.MiscType.MAX_GOLD_PER_DUNGEON_HEART_TILE;
            var controller = new FiveByFiveRotatedController(entity, data, null, instance, null,
                    Map.of(gold, CreatureSleepFixtures.variable(gold, 100)), null);
            assertNull(data.getComponent(entity, ImpGenerator.class));
            controller.captured(Player.KEEPER1_ID);
            assertEquals(Player.KEEPER1_ID, data.getComponent(entity, Owner.class).ownerId);
            assertTrue(controller.isDestroyed());
            assertNull(data.getComponent(entity, ImpGenerator.class));
        } finally {
            data.close();
        }
    }
}
