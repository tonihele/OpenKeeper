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

import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.AttackTarget;
import toniarts.openkeeper.game.component.Damage;
import toniarts.openkeeper.game.component.Health;
import toniarts.openkeeper.game.component.ObjectComponent;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.controller.ICreaturesController;
import toniarts.openkeeper.game.controller.IGameTimer;
import toniarts.openkeeper.game.controller.ILevelInfo;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.RoomAttackMessages;
import toniarts.openkeeper.game.controller.entity.IEntityController;
import toniarts.openkeeper.game.controller.room.FiveByFiveRotatedController;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.event.GameEventService;
import toniarts.openkeeper.game.state.session.PlayerService;
import toniarts.openkeeper.tools.convert.map.DestroyRoomsFixtures;
import toniarts.openkeeper.tools.convert.map.DestroyedFixtures;
import toniarts.openkeeper.tools.convert.map.GameLevel;
import toniarts.openkeeper.tools.convert.map.GameObject;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.KeeperDefeatFixtures;
import toniarts.openkeeper.tools.convert.map.Player;
import toniarts.openkeeper.tools.convert.map.Variable.MiscVariable.MiscType;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.WorldUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The keeper hears of the damage to his Dungeon Heart. DKII.EXE queues message
 * 1 to the owner of the heart, with the heart's tile, whenever the heart object
 * takes damage (0x4DA200)
 */
class HealthSystemTest {

    private static final short KEEPER = Player.KEEPER1_ID;
    private static final Point HEART_TILE = new Point(10, 10);

    private final EntityData entityData = new DefaultEntityData();
    private final List<String> messages = new ArrayList<>();
    private final PlayerService playerService = (PlayerService) Proxy.newProxyInstance(PlayerService.class.getClassLoader(),
            new Class<?>[]{PlayerService.class}, (proxy, method, args) -> {
                if (method.getName().equals("showMentorMessage")) {
                    messages.add(args[0] + "@" + args[1] + ">p" + args[3]);
                }
                return null;
            });
    private GameLevel.LevFlag[] levelFlags = {};
    private final IGameTimer gameTimer = (IGameTimer) Proxy.newProxyInstance(IGameTimer.class.getClassLoader(),
            new Class<?>[]{IGameTimer.class}, (proxy, method, args) -> method.getName().equals("getGameTime") ? 100.0 : null);

    @Test
    void damageToTheHeartIsToldToItsOwner() {
        EntityId heart = createHeart(Player.KEEPER2_ID);
        HealthSystem healthSystem = createHealthSystem();

        entityData.setComponent(heart, new Damage(100));
        healthSystem.processTick(0.25f);

        // "Your Dungeon Heart is under attack!"
        assertEquals(List.of(346 + "@" + WorldUtils.pointToVector3f(HEART_TILE) + ">p" + Player.KEEPER2_ID), messages);
        assertEquals(900, entityData.getComponent(heart, Health.class).health);
    }

    @Test
    void healingTheHeartIsNotTold() {
        EntityId heart = createHeart(KEEPER);
        entityData.setComponent(heart, new Health(500, 1000));
        HealthSystem healthSystem = createHealthSystem();

        entityData.setComponent(heart, new Damage(-100));
        healthSystem.processTick(0.25f);

        assertEquals(List.of(), messages);
    }

    @Test
    void heartAtHalfHealthIsToldToItsOwner() {
        EntityId heart = createHeart(KEEPER);
        HealthSystem healthSystem = createHealthSystem();

        entityData.setComponent(heart, new Damage(500));
        healthSystem.processTick(0.25f);

        // "Your Dungeon Heart is under attack!", then the warning for 50% health
        assertEquals(List.of(346 + "@" + WorldUtils.pointToVector3f(HEART_TILE) + ">p" + KEEPER, "347@null>p" + KEEPER), messages);
    }

    @Test
    void destroyedHeartIsToldToItsOwnerAndTheKeeperAttackingIt() {
        EntityId heart = createHeart(KEEPER);
        EntityId attacker = entityData.createEntity();
        entityData.setComponents(attacker, new AttackTarget(heart), new Owner(Player.KEEPER2_ID, Player.KEEPER2_ID));
        HealthSystem healthSystem = createHealthSystem();

        entityData.setComponent(heart, new Damage(1000));
        healthSystem.processTick(0.25f);

        // The heart is lost, then the one who destroyed it hears of the victory
        assertEquals(List.of(346 + "@" + WorldUtils.pointToVector3f(HEART_TILE) + ">p" + KEEPER, "1291@null>p" + KEEPER,
                "1162@null>p" + Player.KEEPER2_ID), messages);
    }

    @Test
    void inASkirmishEveryOtherKeeperHearsTheKeeperIsDefeated() {
        levelFlags = new GameLevel.LevFlag[]{GameLevel.LevFlag.IS_SKIRMISH_LEVEL};
        EntityId heart = createHeart(KEEPER);
        HealthSystem healthSystem = createHealthSystem();

        entityData.setComponent(heart, new Damage(1000));
        healthSystem.processTick(0.25f);

        // The heart is lost, then the other keeper hears of the defeat (message 0x57, speech 1517 to 1520)
        assertEquals(3, messages.size());
        assertEquals("1291@null>p" + KEEPER, messages.get(1));
        assertTrue(messages.get(2).matches("15(17|18|19|20)@null>p" + Player.KEEPER2_ID));
    }

    @Test
    void damageToAnotherObjectIsNotToldAsAnAttackOnTheHeart() {
        EntityId object = entityData.createEntity();
        entityData.setComponents(object, new ObjectComponent(FiveByFiveRotatedController.OBJECT_PLUG_ID, null),
                new Owner(KEEPER, KEEPER), new Health(1000, 1000), new Position(0, WorldUtils.pointToVector3f(HEART_TILE)));
        HealthSystem healthSystem = createHealthSystem();

        entityData.setComponent(object, new Damage(100));
        healthSystem.processTick(0.25f);

        assertEquals(List.of(), messages);
    }

    private EntityId createHeart(short ownerId) {
        EntityId heart = entityData.createEntity();
        entityData.setComponents(heart, new ObjectComponent(FiveByFiveRotatedController.OBJECT_HEART_ID, null),
                new Owner(ownerId, ownerId), new Health(1000, 1000),
                new Position(0, WorldUtils.pointToVector3f(HEART_TILE)));
        return heart;
    }

    private HealthSystem createHealthSystem() {

        // The destroyed heart looks up its death effect, it has none here
        GameObject heartObject = DestroyedFixtures.object(FiveByFiveRotatedController.OBJECT_HEART_ID, 0);
        GameLevel level = KeeperDefeatFixtures.level(levelFlags);
        IKwdFile kwdFile = (IKwdFile) Proxy.newProxyInstance(IKwdFile.class.getClassLoader(),
                new Class<?>[]{IKwdFile.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "getObject" -> heartObject;
                    case "getGameLevel" -> level;
                    default -> null;
                });
        Map<Short, Keeper> players = Map.of(KEEPER, new Keeper(false, KEEPER), Player.KEEPER2_ID, new Keeper(false, Player.KEEPER2_ID));
        ILevelInfo levelInfo = (ILevelInfo) Proxy.newProxyInstance(ILevelInfo.class.getClassLoader(),
                new Class<?>[]{ILevelInfo.class}, (proxy, method, args) -> method.getName().equals("getPlayers") ? players : kwdFile);
        return new HealthSystem(entityData, stub(IEntityPositionLookup.class, stub(IEntityController.class, null)),
                Map.of(MiscType.CREATURE_DYING_STATE_DURATION_SECONDS,
                        DestroyRoomsFixtures.destroyRoomsVariable(MiscType.CREATURE_DYING_STATE_DURATION_SECONDS, 60),
                        MiscType.PRISON_MODIFY_CREATURE_HEALTH_PER_SECOND,
                        DestroyRoomsFixtures.destroyRoomsVariable(MiscType.PRISON_MODIFY_CREATURE_HEALTH_PER_SECOND, -1),
                        MiscType.MODIFY_HEALTH_OF_CREATURE_IN_LAIR_PER_SECOND,
                        DestroyRoomsFixtures.destroyRoomsVariable(MiscType.MODIFY_HEALTH_OF_CREATURE_IN_LAIR_PER_SECOND, 1)),
                stub(ICreaturesController.class, null), new RoomAttackMessages(entityData, playerService, gameTimer),
                levelInfo, List.of(), stub(IMapController.class, null),
                gameTimer, stub(GameEventService.class, null));
    }

    private static <T> T stub(Class<T> type, Object answer) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, (proxy, method, args) -> answer));
    }

}
