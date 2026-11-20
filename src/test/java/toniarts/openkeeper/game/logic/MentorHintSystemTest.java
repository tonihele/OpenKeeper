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

import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.AttackTarget;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.Health;
import toniarts.openkeeper.game.component.KeeperComponent;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Unconscious;
import toniarts.openkeeper.game.controller.IGameTimer;
import toniarts.openkeeper.game.controller.IPlayerController;
import toniarts.openkeeper.game.controller.RoomAttackMessages;
import toniarts.openkeeper.game.controller.player.PlayerManaControl;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.state.session.PlayerService;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.ShotFixtures;
import toniarts.openkeeper.tools.convert.map.Variable.MiscVariable;
import toniarts.openkeeper.tools.convert.map.Variable.MiscVariable.MiscType;
import toniarts.openkeeper.tools.convert.map.VariableFixtures;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The mentor's advice from the keeper's own tick (DKII.EXE 0x4B9740). The
 * speech numbers are those of DK II's message table (0x6A0B00)
 */
class MentorHintSystemTest {

    private static final short PLAYER_ID = 3;
    private static final short ENEMY_ID = 4;
    private static final short REAPER_ID = 22;
    private static final short GOBLIN_ID = 3;
    private static final short IMP_ID = 1;
    private static final int TICKS_PER_SECOND = 4;
    private static final int REAPER_ADVICE = 1350;

    private final DefaultEntityData entityData = new DefaultEntityData();
    private final Keeper keeper = new Keeper(false, PLAYER_ID);
    private final List<Integer> speech = new ArrayList<>();
    private double gameTime;
    private final MentorHintSystem system = createSystem();

    @Test
    void keeperWithAReaperAndLittleManaIsAdvisedOnce() {
        keeper.setMana(25100);
        keeper.setManaUpkeep(50);
        addCreature(REAPER_ID, PLAYER_ID);

        tick();
        assertEquals(List.of(), speech, "25050 left after the upkeep is enough");

        keeper.setManaUpkeep(150);
        tick();
        assertEquals(List.of(REAPER_ADVICE), speech);

        // Not again, even after the message is no longer held
        gameTime += 1000;
        tick();
        assertEquals(List.of(REAPER_ADVICE), speech);
    }

    @Test
    void keeperWithoutAReaperOrOutOfTheGameIsNotAdvised() {
        keeper.setMana(100);
        addCreature(GOBLIN_ID, PLAYER_ID);
        addCreature(REAPER_ID, ENEMY_ID);

        tick();
        assertEquals(List.of(), speech);

        addCreature(REAPER_ID, PLAYER_ID);
        keeper.setDestroyed(ENEMY_ID);
        tick();
        assertEquals(List.of(), speech);
    }

    @Test
    void keeperWithFewerThanFiveFightersIsToldItsDungeonIsEmptyAtFiftyMinutes() {
        for (int i = 0; i < 4; i++) {
            addCreature(GOBLIN_ID, PLAYER_ID);
        }
        for (int i = 0; i < 6; i++) {
            entityData.getComponent(addCreature(IMP_ID, PLAYER_ID), CreatureComponent.class).worker = true;
        }
        addCreature(GOBLIN_ID, ENEMY_ID);

        tickAt(3000 - 0.25);
        tickAt(3000 + 0.25);
        assertEquals(List.of(), speech);
        // A quiet dungeon's hint follows on the same tick
        tickAt(3000);
        assertEquals(1168, speech.getFirst());
        assertEquals(2, speech.size());
    }

    @Test
    void keeperWithFiveFightersIsNotToldItsDungeonIsEmpty() {
        for (int i = 0; i < 5; i++) {
            addCreature(GOBLIN_ID, PLAYER_ID);
        }

        tickAt(3000);

        assertEquals(1, speech.size());
        assertTrue(speech.getFirst() >= 509 && speech.getFirst() <= 516, "Only the quiet dungeon's hint: " + speech);
    }

    @Test
    void everyKeeperIsToldItShouldHaveWonByNowAtTwoHours() {

        // The 40 minute hint comes on the same tick
        tickAt(7200);

        assertEquals(2, speech.size());
        assertEquals(1197, speech.getFirst());
        assertTrue(speech.getLast() >= 1182 && speech.getLast() <= 1195 && speech.getLast() != 1186, "One of the 13 general hints: " + speech);
    }

    @Test
    void aQuietDungeonGetsAHintEveryTwentyFiveMinutes() {
        addCreature(GOBLIN_ID, PLAYER_ID);

        tickAt(1500);
        tickAt(3000);

        assertEquals(3, speech.size(), "Two quiet hints and the empty dungeon: " + speech);
        assertTrue(speech.get(0) >= 509 && speech.get(0) <= 516);
        assertEquals(1168, speech.get(1));
        assertTrue(speech.get(2) >= 509 && speech.get(2) <= 516);
    }

    @Test
    void aFightOrAKnockedOutCreatureHoldsTheQuietHintBack() {
        EntityId fighter = addCreature(GOBLIN_ID, PLAYER_ID);
        entityData.setComponent(fighter, new AttackTarget(addCreature(GOBLIN_ID, ENEMY_ID)));

        tickAt(1500);
        assertEquals(List.of(), speech);

        entityData.removeComponent(fighter, AttackTarget.class);
        entityData.setComponent(fighter, new Unconscious(0));
        tickAt(4500);
        assertEquals(List.of(), speech);
    }

    @Test
    void noHintsAsTheLevelStarts() {
        tickAt(0);

        assertEquals(List.of(), speech);
    }

    private void tickAt(double time) {
        gameTime = time;
        system.processTick(1f / TICKS_PER_SECOND);
    }

    private void tick() {
        system.processTick(1f / TICKS_PER_SECOND);
        gameTime += 1.0 / TICKS_PER_SECOND;
    }

    private EntityId addCreature(short creatureId, short ownerId) {
        EntityId creature = entityData.createEntity();
        CreatureComponent creatureComponent = new CreatureComponent();
        creatureComponent.creatureId = creatureId;
        entityData.setComponents(creature, creatureComponent, new Owner(ownerId, ownerId), new Health(100, 100));
        return creature;
    }

    private MentorHintSystem createSystem() {
        Creature reaper = ShotFixtures.creature(REAPER_ID, EnumSet.of(Creature.CreatureFlag.IS_HORNY), EnumSet.noneOf(Creature.CreatureFlag3.class));
        Creature goblin = ShotFixtures.creature(GOBLIN_ID, EnumSet.noneOf(Creature.CreatureFlag.class), EnumSet.noneOf(Creature.CreatureFlag3.class));
        IKwdFile kwdFile = stub(IKwdFile.class, (method, args) -> switch (method) {
            case "getCreatureList" -> List.of(goblin, reaper);
            case "getGameLevel" -> ShotFixtures.gameLevel(TICKS_PER_SECOND);
            default -> null;
        });
        EnumMap<MiscType, MiscVariable> settings = new EnumMap<>(MiscType.class);
        settings.put(MiscType.MAXIMUM_MANA_THRESHOLD, VariableFixtures.miscVariable(MiscType.MAXIMUM_MANA_THRESHOLD, 200000));
        settings.put(MiscType.MAXIMUM_MANA_GAIN_PER_SECOND, VariableFixtures.miscVariable(MiscType.MAXIMUM_MANA_GAIN_PER_SECOND, 1000));
        PlayerManaControl manaControl = new PlayerManaControl(keeper, settings);
        IPlayerController playerController = stub(IPlayerController.class, (method, args) -> switch (method) {
            case "getKeeper" -> keeper;
            case "getManaControl" -> manaControl;
            default -> null;
        });
        PlayerService playerService = stub(PlayerService.class, (method, args) -> {
            if (method.equals("showMentorMessage")) {
                assertEquals(PLAYER_ID, args[3]);
                speech.add((Integer) args[0]);
            }
            return null;
        });
        entityData.setComponent(entityData.createEntity(), new KeeperComponent(PLAYER_ID));
        IGameTimer gameTimer = stub(IGameTimer.class, (method, args) -> gameTime);

        return new MentorHintSystem(entityData, kwdFile, List.of(playerController),
                new RoomAttackMessages(entityData, playerService, gameTimer, TICKS_PER_SECOND), gameTimer);
    }

    private static <T> T stub(Class<T> type, Calls calls) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type},
                (proxy, method, args) -> calls.call(method.getName(), args)));
    }

    private interface Calls {

        Object call(String method, Object[] args);
    }
}
