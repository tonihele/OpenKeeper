package toniarts.openkeeper.view.text;

import com.simsilica.es.EntityId;
import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.map.IRoomInformation;
import toniarts.openkeeper.game.map.IRoomsInformation;
import static org.junit.jupiter.api.Assertions.assertEquals;

class RoomTextParserTest {
    @Test
    void hidesTooltipWhileASoldRoomsTileReferenceIsStillVisible() {
        var rooms = new IRoomsInformation<IRoomInformation>() {
            public IRoomInformation getRoomInformation(EntityId id) { return null; }
            public int getRoomCount(short ownerId, short roomId) { return 0; }
        };
        assertEquals("", new RoomTextParser(rooms).parseText("Health %37, capacity %38 / %39", new EntityId(1), null));
    }

    @Test
    void resolvesRoomOnceForAllTooltipParameters() {
        AtomicInteger lookups = new AtomicInteger();
        IRoomInformation room = (IRoomInformation) Proxy.newProxyInstance(IRoomInformation.class.getClassLoader(),
                new Class<?>[]{IRoomInformation.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "getHealthPercent" -> 100;
                    case "getUsedCapacity" -> 2;
                    case "getMaxCapacity" -> 9;
                    default -> throw new UnsupportedOperationException(method.getName());
                });
        var rooms = new IRoomsInformation<IRoomInformation>() {
            public IRoomInformation getRoomInformation(EntityId id) { return lookups.incrementAndGet() == 1 ? room : null; }
            public int getRoomCount(short ownerId, short roomId) { return 0; }
        };
        assertEquals("Health 100, capacity 2 / 9", new RoomTextParser(rooms).parseText(
                "Health %37, capacity %38 / %39", new EntityId(1), null));
        assertEquals(1, lookups.get());
    }
}
