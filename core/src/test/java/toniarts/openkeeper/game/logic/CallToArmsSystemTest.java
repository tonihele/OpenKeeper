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
package toniarts.openkeeper.game.logic;

import com.jme3.math.Vector3f;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.CallToArms;
import toniarts.openkeeper.game.component.CreatureAi;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.FollowTarget;
import toniarts.openkeeper.game.component.InHand;
import toniarts.openkeeper.game.component.Mana;
import toniarts.openkeeper.game.component.ObjectComponent;
import toniarts.openkeeper.game.component.ObjectViewState;
import toniarts.openkeeper.game.component.ObjectViewState.GameObjectAnimState;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.component.Slapped;
import toniarts.openkeeper.game.controller.ICreaturesController;
import toniarts.openkeeper.game.controller.IPlayerController;
import toniarts.openkeeper.game.controller.ObjectsController;
import toniarts.openkeeper.game.controller.creature.CreatureState;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.data.PlayerSpell;
import toniarts.openkeeper.game.event.GameEventService;
import toniarts.openkeeper.game.event.GameEventType;
import toniarts.openkeeper.tools.convert.map.ChickenSpawnFixtures;
import toniarts.openkeeper.tools.convert.map.GameObject;
import toniarts.openkeeper.tools.convert.map.GameObjectFixtures;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.KeeperSpellFixtures;
import toniarts.openkeeper.tools.convert.map.Shot;
import toniarts.openkeeper.tools.convert.map.ShotFixtures;
import toniarts.openkeeper.tools.convert.map.Variable.MiscVariable.MiscType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Call to Arms as in the original: who answers the banner and what it costs.
 * The spell values are the original's, 10000 to cast and 90 a second
 */
class CallToArmsSystemTest {

    private static final short PLAYER_ID = 3;
    private static final short ENEMY_ID = 4;
    private static final short SPELL_ID = 3;
    private static final short SHOT_ID = 5;

    private final EntityData entityData = new DefaultEntityData();
    private final Keeper keeper = new Keeper(false, PLAYER_ID);
    private final List<EntityId> answered = new ArrayList<>();
    private final List<Object[]> events = new ArrayList<>();
    private double gameTime;
    private IKwdFile kwdFile;
    private CallToArmsSystem system;

    @BeforeEach
    void setUp() {
        keeper.setMana(100_000);
        PlayerSpell spell = new PlayerSpell(SPELL_ID, true);
        keeper.getAvailableSpells().add(spell);

        kwdFile = stub(IKwdFile.class, name -> switch (name) {
            case "getKeeperSpells" -> List.of(KeeperSpellFixtures.keeperSpell(SPELL_ID, 10000, 90, SHOT_ID, ObjectsController.OBJECT_CALL_TO_ARMS_ID));
            case "getShotById" -> ShotFixtures.shot(SHOT_ID, Shot.ProcessType.CREATE_OBJECT, 1, EnumSet.noneOf(Shot.ShotProcessFlag.class));
            case "getObject" -> bannerObject();
            case "getThings" -> List.of();
            case "getVariables" -> Map.of(MiscType.UPGRADED_CALL_TO_ARMS_COST_PERCENTAGE,
                    ChickenSpawnFixtures.miscVariable(MiscType.UPGRADED_CALL_TO_ARMS_COST_PERCENTAGE, 70));
            default -> throw new UnsupportedOperationException(name);
        });
        IPlayerController playerController = stub(IPlayerController.class, name -> keeper);
        ICreaturesController creaturesController = (ICreaturesController) Proxy.newProxyInstance(ICreaturesController.class.getClassLoader(),
                new Class<?>[]{ICreaturesController.class}, (proxy, method, args) -> {
                    EntityId creature = (EntityId) args[0];
                    return stub(ICreatureController.class, name -> answered.add(creature));
                });
        GameEventService gameEventService = (GameEventService) Proxy.newProxyInstance(GameEventService.class.getClassLoader(),
                new Class<?>[]{GameEventService.class}, (proxy, method, args) -> events.add(args));
        system = new CallToArmsSystem(entityData, kwdFile, List.of(playerController), creaturesController, () -> gameTime,
                gameEventService);
    }

    @Test
    void theKeepersFreeFightersAnswer() {
        EntityId idle = creature(PLAYER_ID, false, CreatureState.IDLE);
        EntityId working = creature(PLAYER_ID, false, CreatureState.WORK);
        EntityId sleeping = creature(PLAYER_ID, false, CreatureState.SLEEPING);
        creature(PLAYER_ID, true, CreatureState.IDLE); // Imps don't
        creature(ENEMY_ID, false, CreatureState.IDLE);
        creature(PLAYER_ID, false, CreatureState.FIGHT);
        creature(PLAYER_ID, false, CreatureState.ENTERING_DUNGEON);
        creature(PLAYER_ID, false, CreatureState.IMPRISONED);
        creature(PLAYER_ID, false, CreatureState.CALL_TO_ARMS);
        banner();

        system.processTick(0.25f);

        assertEquals(List.of(idle, working, sleeping), answered.stream().sorted().toList());
    }

    @Test
    void theBannerCallsEverySecond() {
        creature(PLAYER_ID, false, CreatureState.IDLE);
        banner();

        system.processTick(0.25f);
        gameTime = 0.5;
        system.processTick(0.25f);
        assertEquals(1, answered.size());

        gameTime = 1;
        system.processTick(0.25f);
        assertEquals(2, answered.size());
    }

    @Test
    void theCastingCostPaysForTheFirst111Seconds() {
        EntityId banner = banner();
        system.processTick(0.25f);

        gameTime = 110.75;
        system.processTick(0.25f);
        assertNull(entityData.getComponent(banner, Mana.class));

        gameTime = 111;
        system.processTick(0.25f);
        assertEquals(-90, entityData.getComponent(banner, Mana.class).manaGeneration);
    }

    @Test
    void theUpgradedBannerCosts70Percent() {
        keeper.getAvailableSpells().get(0).setUpgraded(true);
        EntityId banner = banner();
        system.processTick(0.25f);

        gameTime = 111;
        system.processTick(0.25f);
        assertEquals(-63, entityData.getComponent(banner, Mana.class).manaGeneration);
    }

    @Test
    void theBannerInTheHandCostsNothing() {
        EntityId banner = banner();
        system.processTick(0.25f);
        gameTime = 111;
        system.processTick(0.25f);

        entityData.removeComponent(banner, Position.class);
        system.processTick(0.25f);
        assertNull(entityData.getComponent(banner, Mana.class));
    }

    @Test
    void theBannerFallsWhenTheManaRunsOut() {
        EntityId banner = banner();
        system.processTick(0.25f);

        keeper.setMana(89);
        gameTime = 111;
        system.processTick(0.25f);
        assertEquals(GameObjectAnimState.ADDITIONAL_RESOURCE_3, entityData.getComponent(banner, ObjectViewState.class).animState);
        assertNull(entityData.getComponent(banner, Mana.class));

        gameTime = 112;
        system.processTick(0.25f);
        assertNull(entityData.getComponent(banner, ObjectComponent.class));
    }

    @Test
    void aSlapTakesTheBannerDown() {
        EntityId banner = banner();
        system.processTick(0.25f);

        entityData.setComponent(banner, new Slapped(gameTime));
        system.processTick(0.25f);
        assertEquals(GameObjectAnimState.ADDITIONAL_RESOURCE_3, entityData.getComponent(banner, ObjectViewState.class).animState);

        // Knocked away in a second, the creatures heed it until then
        gameTime = 0.75;
        system.processTick(0.25f);
        assertNotNull(entityData.getComponent(banner, CallToArms.class));
        gameTime = 1;
        system.processTick(0.25f);
        assertNull(entityData.getComponent(banner, ObjectComponent.class));
    }

    @Test
    void castingAgainMovesTheBannerWithoutResettingTheUpkeep() {
        EntityId first = banner();
        EntityId heeding = creature(PLAYER_ID, false, CreatureState.CALL_TO_ARMS);
        entityData.setComponent(heeding, new FollowTarget(first));
        system.processTick(0.25f);

        gameTime = 50;
        EntityId second = banner();
        system.processTick(0.25f);

        // The old banner sinks back into the ground in 2 seconds
        assertEquals(111, entityData.getComponent(second, CallToArms.class).upkeepTime);
        assertEquals(GameObjectAnimState.ADDITIONAL_RESOURCE_2, entityData.getComponent(first, ObjectViewState.class).animState);
        assertEquals(second, entityData.getComponent(heeding, FollowTarget.class).entityId);
        gameTime = 52;
        system.processTick(0.25f);
        assertNull(entityData.getComponent(first, ObjectComponent.class));
    }

    @Test
    void theBannerRisesThenWaves() {
        EntityId banner = banner();
        system.processTick(0.25f);
        assertEquals(GameObjectAnimState.MESH_RESOURCE, entityData.getComponent(banner, ObjectViewState.class).animState);

        gameTime = 2;
        system.processTick(0.25f);
        assertEquals(GameObjectAnimState.ADDITIONAL_RESOURCE_1, entityData.getComponent(banner, ObjectViewState.class).animState);
    }

    @Test
    void theBannerShowsItsEffectEveryThirdCall() {
        EntityId banner = banner();
        for (int second = 0; second < 6; second++) {
            gameTime = second;
            system.processTick(0.25f);
        }

        assertEquals(2, events.size());
        assertEquals(GameEventType.CALL_TO_ARMS, events.get(0)[0]);
        assertEquals(banner, events.get(0)[1]);
    }

    @Test
    void theBannerInTheHandStaysWhenCastingAgain() {
        EntityId first = banner();
        system.processTick(0.25f);
        entityData.removeComponent(first, Position.class);
        entityData.setComponent(first, new InHand());

        EntityId second = banner();
        system.processTick(0.25f);

        assertNotNull(entityData.getComponent(first, CallToArms.class));
        assertNull(entityData.getComponent(second, ObjectComponent.class));
    }

    @Test
    void clickingTheIndicatorKnocksAwayTheWavingBanner() {
        ObjectsController objectsController = new ObjectsController(kwdFile, entityData, Map.of(), () -> gameTime, null, null);
        EntityId banner = banner();
        system.processTick(0.25f);

        // Not while it rises, and not someone else's
        objectsController.cancelCallToArms(PLAYER_ID);
        objectsController.cancelCallToArms(ENEMY_ID);
        system.processTick(0.25f);
        assertEquals(GameObjectAnimState.MESH_RESOURCE, entityData.getComponent(banner, ObjectViewState.class).animState);

        gameTime = 2;
        system.processTick(0.25f);
        objectsController.cancelCallToArms(ENEMY_ID);
        system.processTick(0.25f);
        assertEquals(GameObjectAnimState.ADDITIONAL_RESOURCE_1, entityData.getComponent(banner, ObjectViewState.class).animState);

        objectsController.cancelCallToArms(PLAYER_ID);
        system.processTick(0.25f);
        assertEquals(GameObjectAnimState.ADDITIONAL_RESOURCE_3, entityData.getComponent(banner, ObjectViewState.class).animState);
        gameTime = 3;
        system.processTick(0.25f);
        assertNull(entityData.getComponent(banner, ObjectComponent.class));
    }

    @Test
    void theBannerInTheHandCantBeTakenDownFromTheIndicator() {
        ObjectsController objectsController = new ObjectsController(kwdFile, entityData, Map.of(), () -> gameTime, null, null);
        EntityId banner = banner();
        system.processTick(0.25f);
        gameTime = 2;
        system.processTick(0.25f);
        entityData.removeComponent(banner, Position.class);

        objectsController.cancelCallToArms(PLAYER_ID);
        system.processTick(0.25f);
        assertNull(entityData.getComponent(banner, Slapped.class));
        assertNotNull(entityData.getComponent(banner, CallToArms.class));
    }

    private EntityId banner() {
        EntityId banner = entityData.createEntity();
        entityData.setComponents(banner, new ObjectComponent(ObjectsController.OBJECT_CALL_TO_ARMS_ID, null),
                new Owner(PLAYER_ID, PLAYER_ID), new Position(0, new Vector3f(5, 0, 5)),
                new ObjectViewState(ObjectsController.OBJECT_CALL_TO_ARMS_ID, GameObject.State.CREATE_CALL_TO_ARMS,
                        GameObjectAnimState.MESH_RESOURCE, true));
        return banner;
    }

    /**
     * The banner rises in 2 seconds, sinks in 2 and is knocked away in 1
     */
    private static GameObject bannerObject() {
        return GameObjectFixtures.gameObject(ObjectsController.OBJECT_CALL_TO_ARMS_ID, GameObjectFixtures.animation(30, 15),
                GameObjectFixtures.animation(30, 30), GameObjectFixtures.animation(20, 10), GameObjectFixtures.animation(10, 10));
    }

    private EntityId creature(short ownerId, boolean worker, CreatureState state) {
        EntityId creature = entityData.createEntity();
        CreatureComponent creatureComponent = new CreatureComponent();
        creatureComponent.worker = worker;
        entityData.setComponents(creature, creatureComponent, new Owner(ownerId, ownerId),
                new CreatureAi(0, state, (short) 1));
        return creature;
    }

    private static <T> T stub(Class<T> type, Function<String, Object> answer) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type},
                (proxy, method, args) -> answer.apply(method.getName())));
    }

}
