package toniarts.openkeeper.game.sound;

import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.event.GameEvent;
import toniarts.openkeeper.game.event.ServerGameEventService;
import toniarts.openkeeper.game.state.session.PlayerService;
import java.util.ArrayList;
import toniarts.openkeeper.game.controller.room.AbstractRoomController;
import toniarts.openkeeper.game.component.ObjectComponent;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.event.GameEventType;
import toniarts.openkeeper.game.event.GameEvent.ThingFlag;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.SoundFixtures;
import toniarts.openkeeper.utils.Point;

import com.jme3.math.Vector3f;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The game logic only creates {@link GameEvent}s, what the client makes of them is the client's matter.
 */
class GameEventTest {

    private EntityData entityData;
    private ServerGameEventService service;
    private final List<Object[]> sent = new ArrayList<>();

    @BeforeEach
    void setUp() {
        entityData = new DefaultEntityData();
        PlayerService playerService = (PlayerService) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{PlayerService.class}, (proxy, method, args) -> {
                    if (method.getName().equals("sendEvent")) {
                        sent.add(args);
                    }
                    return null;
                });
        service = new ServerGameEventService(entityData, playerService);
    }

    @AfterEach
    void tearDown() {
        entityData.close();
    }

    @Test
    void emitCreatesAnEventForEveryone() {
        service.emit(GameEventType.ROOM_SOLD, ThingFlag.ROOM, 7, new Point(3, 4));

        assertEquals(1, sent.size());
        Object[] args = sent.get(0);
        assertEquals(GameEventType.ROOM_SOLD, args[0]);
        assertEquals(ThingFlag.ROOM, args[1]);
        assertEquals(7, args[2]);
        assertEquals(new Point(3, 4), args[6]);
        assertNull(args[7], "No player means everyone");
    }

    @Test
    void emitToPlayerHasNoPlaceAndIsOnlyForThePlayer() {
        service.emitToPlayer(GameEventType.TILES_TAGGED, (short) 2);

        Object[] args = sent.get(0);
        assertNull(args[6]);
        assertNull(args[1]);
        assertEquals((short) 2, args[7]);
    }

    @Test
    void entityEventKnowsTheThingTypeOfTheEntity() {
        EntityId entity = entityData.createEntity();
        entityData.setComponents(entity, new ObjectComponent((short) 5, AbstractRoomController.ObjectType.GOLD),
                new Position(0, new Vector3f(2.4f, 0, 6.2f)));

        service.emit(GameEventType.SLAPPED, entity);

        Object[] args = sent.get(0);
        assertEquals(ThingFlag.OBJECT, args[1]);
        assertEquals(5, args[2]);
        assertEquals(entity, args[5]);
        assertEquals(new Point(2, 6), args[6]);
    }

    @Test
    void entityThatIsNoThingMakesNoEvent() {
        service.emit(GameEventType.SLAPPED, entityData.createEntity(), new Vector3f());

        assertTrue(sent.isEmpty());
    }

    @Test
    void slappedObjectSquawksOnItsOwnCategory() {
        IKwdFile kwdFile = (IKwdFile) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{IKwdFile.class},
                (proxy, method, args) -> method.getName().equals("getObject") ? SoundFixtures.object("OBJECT_CHICKEN") : null);
        GameEvent event = new GameEvent(GameEventType.SLAPPED, ThingFlag.OBJECT, 3, null, 0, null, new Point(1, 2));

        List<GameEventSounds.Sound> sounds = GameEventSounds.getSounds(event, kwdFile);

        assertEquals(List.of(
                new GameEventSounds.Sound("OBJECT_CHICKEN", ThingSounds.SLAP, new Point(1, 2)),
                new GameEventSounds.Sound("OBJECT_CHICKEN", ThingSounds.OBJECT_SLAPPED, new Point(1, 2))), sounds);
    }

    @Test
    void handSlapIsNotPositional() {
        GameEvent event = new GameEvent(GameEventType.HAND_SLAP, null, 0, null, 0, null, null);

        List<GameEventSounds.Sound> sounds = GameEventSounds.getSounds(event, null);

        assertEquals(List.of(new GameEventSounds.Sound(GlobalCategory.HAND, ThingSounds.SLAP, null)), sounds);
    }

    @Test
    void thingWithoutSoundCategoryMakesNoSound() {
        IKwdFile kwdFile = (IKwdFile) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{IKwdFile.class},
                (proxy, method, args) -> method.getName().equals("getRoomById") ? SoundFixtures.room(null) : null);
        GameEvent event = new GameEvent(GameEventType.ROOM_SOLD, ThingFlag.ROOM, 3, null, 0, null, new Point(1, 1));

        assertTrue(GameEventSounds.getSounds(event, kwdFile).isEmpty());
    }

    @Test
    void onlyHurtIsExclusive() {
        assertTrue(GameEventSounds.isExclusive(GameEventType.CREATURE_HURT));
        assertFalse(GameEventSounds.isExclusive(GameEventType.CREATURE_DIE));
    }
}
