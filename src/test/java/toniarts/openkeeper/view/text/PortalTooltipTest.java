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
package toniarts.openkeeper.view.text;

import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.PortalAttracted;
import toniarts.openkeeper.game.map.IRoomInformation;
import toniarts.openkeeper.game.map.IRoomsInformation;
import toniarts.openkeeper.utils.TextParameter;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The Portal tooltip: "Creatures attracted: %42 / %43"
 */
class PortalTooltipTest {

    private static final short KEEPER = 3;
    private static final short PORTAL = 3;
    private static final short HERO_PORTAL = 25;

    private final EntityData entityData = new DefaultEntityData();

    @Test
    void showsTheAttractedCreaturesAndWhatThePortalsSupport() {
        for (int i = 0; i < 4; i++) {
            EntityId creature = entityData.createEntity();
            entityData.setComponent(creature, new CreatureComponent());
            entityData.setComponent(creature, new Owner(KEEPER, KEEPER));
            entityData.setComponent(creature, new PortalAttracted());
        }
        IRoomsInformation<IRoomInformation> rooms = stub(IRoomsInformation.class, (name, args) -> switch (name) {
            case "getRoomCount" -> {
                short roomId = (short) args[1];
                yield roomId == PORTAL ? 2 : roomId == HERO_PORTAL ? 1 : 0;
            }
            default -> null;
        });
        IRoomInformation portal = stub(IRoomInformation.class, (name, args) -> switch (name) {
            case "getOwnerId" -> KEEPER;
            default -> null;
        });

        RoomTextParser parser = new RoomTextParser(rooms, entityData, 15, 5);

        assertEquals("4", parser.getReplacement(TextParameter.ATTRACTED_CREATURE_COUNT, portal, null));
        assertEquals("25", parser.getReplacement(TextParameter.PORTAL_CREATURE_CAPACITY, portal, null));
        assertEquals("2", parser.getReplacement(TextParameter.PORTAL_COUNT, portal, null));
    }

    private interface Answer {

        Object answer(String methodName, Object[] args);
    }

    @SuppressWarnings("unchecked")
    private static <T> T stub(Class<?> type, Answer answer) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type},
                (proxy, method, args) -> answer.answer(method.getName(), args));
    }

}
