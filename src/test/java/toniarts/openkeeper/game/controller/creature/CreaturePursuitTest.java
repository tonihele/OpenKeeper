package toniarts.openkeeper.game.controller.creature;

import com.badlogic.gdx.ai.pfa.DefaultGraphPath;
import com.jme3.math.Vector3f;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.Navigation;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.event.GameEvent;
import toniarts.openkeeper.game.event.GameEventType;
import toniarts.openkeeper.game.event.ServerGameEventService;
import toniarts.openkeeper.game.map.IMapTileInformation;
import toniarts.openkeeper.game.navigation.INavigationService;
import toniarts.openkeeper.game.sound.GameEventSounds;
import toniarts.openkeeper.game.state.session.PlayerService;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.SoundFixtures;
import toniarts.openkeeper.utils.Point;

import static org.junit.jupiter.api.Assertions.*;

class CreaturePursuitTest {
    private final EntityData data = new DefaultEntityData();
    private final EntityId attacker = entity(1, 1);
    private final EntityId target = entity(6, 1);
    private final List<GameEvent> events = new ArrayList<>();
    private final ServerGameEventService eventService;
    private final INavigationService navigation;
    private boolean pathAvailable = true;
    private int pathRequests;

    CreaturePursuitTest() {
        CreatureComponent component = new CreatureComponent();
        component.creatureId = 5;
        data.setComponent(attacker, component);
        PlayerService players = (PlayerService) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{PlayerService.class}, (proxy, method, args) -> {
                    if (method.getName().equals("sendEvent")) {
                        events.add(new GameEvent((GameEventType) args[0], (GameEvent.ThingFlag) args[1],
                                (int) args[2], (GameEvent.ThingFlag) args[3], (int) args[4],
                                (EntityId) args[5], (Point) args[6]));
                    }
                    return null;
                });
        eventService = new ServerGameEventService(data, players);
        navigation = (INavigationService) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{INavigationService.class}, (proxy, method, args) -> {
                    if (!method.getName().equals("findPath")) {
                        throw new UnsupportedOperationException(method.getName());
                    }
                    pathRequests++;
                    if (!pathAvailable) {
                        return null;
                    }
                    DefaultGraphPath<IMapTileInformation> path = new DefaultGraphPath<>();
                    for (Point p : List.of((Point) args[0], (Point) args[1])) {
                        path.add((IMapTileInformation) Proxy.newProxyInstance(getClass().getClassLoader(),
                                new Class<?>[]{IMapTileInformation.class}, (tile, getter, parameters) -> {
                                    if (getter.getName().equals("getLocation")) {
                                        return p;
                                    }
                                    throw new UnsupportedOperationException(getter.getName());
                                }));
                    }
                    return path;
                });
    }

    @AfterEach
    void close() {
        data.close();
    }

    @Test
    void successfulPursuitEmitsPositionalChargeUsingEachCreaturesCategory() {
        controller().navigateToAttackTarget(target);
        assertTrue(data.getComponent(attacker, Navigation.class).pursuingAttackTarget);
        assertEquals(1, events.size());
        GameEvent event = events.getFirst();
        assertEquals(GameEventType.CREATURE_CHARGE, event.type);
        assertEquals(attacker, event.entity);
        assertEquals(5, event.subjectId);
        assertEquals(new Point(1, 1), event.position);
        for (String category : List.of("CREATURE_GOBLIN", "CREATURE_TROLL", "CREATURE_WARLOCK")) {
            IKwdFile kwd = (IKwdFile) Proxy.newProxyInstance(getClass().getClassLoader(),
                    new Class<?>[]{IKwdFile.class}, (proxy, method, args) ->
                            method.getName().equals("getCreature") ? SoundFixtures.creature((short) 5, category) : null);
            assertEquals(List.of(new GameEventSounds.Sound(category, 0xD6, new Point(1, 1))),
                    GameEventSounds.getSounds(event, kwd));
        }
    }

    @Test
    void repeatedTicksAndMovingOrChangedTargetsDoNotRepeatTheCall() {
        CreatureController controller = controller();
        controller.navigateToAttackTarget(target);
        for (int i = 0; i < 10; i++) {
            controller.navigateToAttackTarget(target);
        }
        assertEquals(1, pathRequests);
        data.setComponent(target, new Position(0, new Vector3f(7, 0, 1)));
        controller.navigateToAttackTarget(target);
        controller.navigateToAttackTarget(entity(8, 2));
        assertEquals(3, pathRequests);
        assertEquals(1, events.size());
        assertEquals(new Point(8, 2), data.getComponent(attacker, Navigation.class).target);
    }

    @Test
    void pursuitStateSurvivesControllerRecreation() {
        controller().navigateToAttackTarget(target);
        data.setComponent(target, new Position(0, new Vector3f(7, 0, 1)));
        controller().navigateToAttackTarget(target);
        assertEquals(1, events.size());
    }

    @Test
    void stoppingThenStartingPursuitCallsAgain() {
        CreatureController controller = controller();
        controller.navigateToAttackTarget(target);
        controller.stopCreature();
        controller.navigateToAttackTarget(target);
        assertEquals(2, events.size());
    }

    @Test
    void ordinaryNavigationToSameDestinationStillStartsACharge() {
        data.setComponent(attacker, new Navigation(new Point(6, 1), null, List.of()));
        controller().navigateToAttackTarget(target);
        assertEquals(1, events.size());
        assertTrue(data.getComponent(attacker, Navigation.class).pursuingAttackTarget);
    }

    @Test
    void replacingPursuitWithOrdinaryNavigationAllowsAnotherCharge() {
        controller().navigateToAttackTarget(target);
        data.setComponent(attacker, new Navigation(new Point(3, 1), null, List.of()));
        controller().navigateToAttackTarget(target);
        assertEquals(2, events.size());
    }

    @Test
    void failedPathIsSilentAndALaterSuccessfulRetryCalls() {
        pathAvailable = false;
        controller().navigateToAttackTarget(target);
        assertTrue(events.isEmpty());
        assertNull(data.getComponent(attacker, Navigation.class));
        pathAvailable = true;
        controller().navigateToAttackTarget(target);
        assertEquals(1, events.size());
    }

    @Test
    void missingTargetOrTargetOnSameTileDoesNotCall() {
        CreatureController controller = controller();
        controller.navigateToAttackTarget(data.createEntity());
        controller.navigateToAttackTarget(entity(1, 1));
        assertEquals(0, pathRequests);
        assertTrue(events.isEmpty());
    }

    private CreatureController controller() {
        return new CreatureController(attacker, data, SoundFixtures.creature((short) 5, "CREATURE_GOBLIN"),
                navigation, null, null, null, null, null, null, null, null, null, eventService);
    }

    private EntityId entity(float x, float z) {
        EntityId id = data.createEntity();
        data.setComponent(id, new Position(0, new Vector3f(x, 0, z)));
        return id;
    }
}
