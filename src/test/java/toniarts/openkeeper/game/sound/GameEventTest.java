package toniarts.openkeeper.game.sound;

import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.EntitySet;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.GameEvent;
import toniarts.openkeeper.game.controller.room.AbstractRoomController;
import toniarts.openkeeper.game.component.ObjectComponent;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.event.GameEventType;
import toniarts.openkeeper.game.event.GameEvents;
import toniarts.openkeeper.game.component.GameEvent.ThingFlag;
import toniarts.openkeeper.game.logic.GameEventSystem;
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
    private EntitySet events;

    @BeforeEach
    void setUp() {
        entityData = new DefaultEntityData();
        events = entityData.getEntities(GameEvent.class);
    }

    @AfterEach
    void tearDown() {
        events.release();
        entityData.close();
    }

    @Test
    void emitCreatesAnEventForEveryone() {
        GameEvents.emit(entityData, GameEventType.ROOM_SOLD, ThingFlag.ROOM, 7, new Point(3, 4));

        assertTrue(events.applyChanges());
        GameEvent event = events.iterator().next().get(GameEvent.class);
        assertEquals(GameEventType.ROOM_SOLD, event.type);
        assertEquals(ThingFlag.ROOM, event.subjectKind);
        assertEquals(7, event.subjectId);
        assertEquals(new Point(3, 4), event.position);
        assertEquals(GameEvent.ALL_PLAYERS, event.playerId);
    }

    @Test
    void emitToPlayerHasNoPlaceAndIsOnlyForThePlayer() {
        GameEvents.emitToPlayer(entityData, GameEventType.TILES_TAGGED, (short) 2);

        events.applyChanges();
        GameEvent event = events.iterator().next().get(GameEvent.class);
        assertNull(event.position);
        assertNull(event.subjectKind);
        assertEquals(2, event.playerId);
    }

    @Test
    void entityEventKnowsTheThingTypeOfTheEntity() {
        EntityId entity = entityData.createEntity();
        entityData.setComponents(entity, new ObjectComponent((short) 5, AbstractRoomController.ObjectType.GOLD),
                new Position(0, new Vector3f(2.4f, 0, 6.2f)));

        GameEvents.emit(entityData, GameEventType.SLAPPED, entity);

        events.applyChanges();
        GameEvent event = events.iterator().next().get(GameEvent.class);
        assertEquals(ThingFlag.OBJECT, event.subjectKind);
        assertEquals(5, event.subjectId);
        assertEquals(entity, event.entity);
        assertEquals(new Point(2, 6), event.position);
    }

    @Test
    void entityThatIsNoThingMakesNoEvent() {
        GameEvents.emit(entityData, GameEventType.SLAPPED, entityData.createEntity(), new Vector3f());

        assertFalse(events.applyChanges());
    }

    @Test
    void eventsAreRemovedAfterTheyHadTimeToReachTheClients() {
        GameEventSystem system = new GameEventSystem(entityData);
        GameEvents.emit(entityData, GameEventType.ROOM_SOLD, ThingFlag.ROOM, 1, new Point(1, 1));

        system.processTick(0.1f);
        system.processTick(0.1f);
        events.applyChanges();
        assertEquals(1, events.size(), "Too early, a client might not have seen it yet");

        system.processTick(5f);
        events.applyChanges();
        assertTrue(events.isEmpty());
        system.stop();
    }

    @Test
    void slappedObjectSquawksOnItsOwnCategory() {
        IKwdFile kwdFile = (IKwdFile) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{IKwdFile.class},
                (proxy, method, args) -> method.getName().equals("getObject") ? SoundFixtures.object("OBJECT_CHICKEN") : null);
        GameEvent event = new GameEvent(GameEventType.SLAPPED, ThingFlag.OBJECT, 3, null, 0, null, new Point(1, 2),
                GameEvent.ALL_PLAYERS);

        List<GameEventSounds.Sound> sounds = GameEventSounds.getSounds(event, kwdFile);

        assertEquals(List.of(
                new GameEventSounds.Sound("OBJECT_CHICKEN", ThingSounds.SLAP, new Point(1, 2)),
                new GameEventSounds.Sound("OBJECT_CHICKEN", ThingSounds.OBJECT_SLAPPED, new Point(1, 2))), sounds);
    }

    @Test
    void handSlapIsNotPositional() {
        GameEvent event = new GameEvent(GameEventType.HAND_SLAP, null, 0, null, 0, null, null, (short) 1);

        List<GameEventSounds.Sound> sounds = GameEventSounds.getSounds(event, null);

        assertEquals(List.of(new GameEventSounds.Sound(GlobalCategory.HAND, ThingSounds.SLAP, null)), sounds);
    }

    @Test
    void thingWithoutSoundCategoryMakesNoSound() {
        IKwdFile kwdFile = (IKwdFile) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{IKwdFile.class},
                (proxy, method, args) -> method.getName().equals("getRoomById") ? SoundFixtures.room(null) : null);
        GameEvent event = new GameEvent(GameEventType.ROOM_SOLD, ThingFlag.ROOM, 3, null, 0, null, new Point(1, 1),
                GameEvent.ALL_PLAYERS);

        assertTrue(GameEventSounds.getSounds(event, kwdFile).isEmpty());
    }

    @Test
    void onlyHurtIsExclusive() {
        assertTrue(GameEventSounds.isExclusive(GameEventType.CREATURE_HURT));
        assertFalse(GameEventSounds.isExclusive(GameEventType.CREATURE_DIE));
    }
}
