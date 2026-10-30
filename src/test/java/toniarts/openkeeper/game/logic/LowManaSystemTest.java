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
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import toniarts.openkeeper.game.component.CreatureAi;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.CreatureExperience;
import toniarts.openkeeper.game.component.Damage;
import toniarts.openkeeper.game.component.Health;
import toniarts.openkeeper.game.component.ImpPopCountdown;
import toniarts.openkeeper.game.component.KeeperComponent;
import toniarts.openkeeper.game.component.LowMana;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.controller.IPlayerController;
import toniarts.openkeeper.game.controller.creature.CreatureState;
import toniarts.openkeeper.game.controller.player.PlayerManaControl;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.state.session.PlayerService;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.DestroyRoomsFixtures;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.ShotFixtures;
import toniarts.openkeeper.tools.convert.map.Variable.MiscVariable;
import toniarts.openkeeper.tools.convert.map.Variable.MiscVariable.MiscType;
import toniarts.openkeeper.utils.Point;

/**
 * The low mana warnings and the popping of imps
 */
class LowManaSystemTest {

    private static final short PLAYER_ID = 3;
    private static final short IMP_ID = 1;
    private static final int PERIOD = 10;
    private static final int THRESHOLD = 100;
    private static final int MINIMUM_IMPS = 4;
    private static final int CREATE_IMP_COST = 1500;

    private final DefaultEntityData entityData = new DefaultEntityData();
    private final Keeper keeper = new Keeper(false, PLAYER_ID);
    private final List<Integer> speech = new ArrayList<>();
    private final EntityId playerEntity;
    private final LowManaSystem system;

    LowManaSystemTest() {
        Map<MiscType, MiscVariable> settings = new EnumMap<>(MiscType.class);
        setting(settings, MiscType.MAXIMUM_MANA_THRESHOLD, 200000);
        setting(settings, MiscType.LOW_MANA_WARNING_THRESHOLD, THRESHOLD);
        setting(settings, MiscType.INSUFFICIENT_MANA_IMP_EVALUATION_PERIOD_SECONDS, PERIOD);
        setting(settings, MiscType.MINIMUM_IMP_THRESHOLD, MINIMUM_IMPS);
        PlayerManaControl manaControl = new PlayerManaControl(keeper, settings);
        keeper.setDungeonHeartLocation(new Point(10, 10));

        Creature imp = ShotFixtures.creature(IMP_ID, EnumSet.of(Creature.CreatureFlag.IS_WORKER, Creature.CreatureFlag.IS_EVIL),
                EnumSet.noneOf(Creature.CreatureFlag3.class));
        IKwdFile kwdFile = stub(IKwdFile.class, (method, args) -> switch (method) {
            case "getImp" -> imp;
            case "getKeeperSpells" -> List.of(ShotFixtures.createImp(CREATE_IMP_COST));
            default -> null;
        });
        IPlayerController playerController = stub(IPlayerController.class, (method, args) -> switch (method) {
            case "getKeeper" -> keeper;
            case "getManaControl" -> manaControl;
            default -> null;
        });
        PlayerService playerService = stub(PlayerService.class, (method, args) -> {
            if (method.equals("showMentorMessage")) {
                assertEquals(PLAYER_ID, args[2]);
                speech.add((Integer) args[0]);
            }
            return null;
        });

        playerEntity = entityData.createEntity();
        entityData.setComponent(playerEntity, new KeeperComponent(PLAYER_ID));
        system = new LowManaSystem(entityData, kwdFile, List.of(playerController), settings, playerService);
    }

    @Test
    void warnsFourTimesAndThenPopsAnImp() {
        addImps(6);
        keeper.setMana(50);

        runSeconds(1);
        assertEquals(List.of(), speech, "The first warning comes one period after the mana went low");
        runSeconds(PERIOD - 1);
        assertEquals(List.of(), speech);
        runSeconds(1);
        assertEquals(List.of(447), speech);
        runSeconds(PERIOD * 2);
        assertEquals(List.of(447, 1329, 1330), speech);
        assertEquals(0, countdowns());

        runSeconds(PERIOD);
        assertEquals(List.of(447, 1329, 1330, 1331, 1333), speech, "The fourth warning starts the popping");
        assertEquals(1, countdowns());

        runSeconds(PERIOD);
        assertEquals(List.of(447, 1329, 1330, 1331, 1333), speech, "No more warnings, the next imp is chosen a second after the period");
        runSeconds(1);
        assertEquals(List.of(447, 1329, 1330, 1331, 1333, 1333), speech);
    }

    @Test
    void repeatsTheThirdWarningWithoutSpareImps() {
        addImps(MINIMUM_IMPS);
        keeper.setMana(50);

        runSeconds(1 + PERIOD * 6);

        assertEquals(List.of(447, 1329, 1330, 1330, 1330, 1330), speech);
        assertEquals(0, countdowns());
    }

    @Test
    void doesNotWarnWhileTheManaGrows() {
        addImps(6);
        keeper.setMana(50);
        keeper.setManaGain(10);

        runSeconds(PERIOD * 5);

        assertEquals(List.of(), speech);
    }

    @Test
    void enoughManaEndsTheWarnings() {
        addImps(6);
        keeper.setMana(50);
        runSeconds(1 + PERIOD);
        assertNotNull(entityData.getComponent(playerEntity, LowMana.class));

        keeper.setMana(THRESHOLD);
        runSeconds(1);

        assertNull(entityData.getComponent(playerEntity, LowMana.class));
    }

    @Test
    void popsTheImpWithTheLowestLevel() {
        List<EntityId> imps = addImps(6);
        entityData.setComponent(imps.get(3), experience(1));
        for (EntityId imp : imps) {
            if (!imp.equals(imps.get(3))) {
                entityData.setComponent(imp, experience(2));
            }
        }

        assertEquals(imps.get(3), startPopping());
    }

    @Test
    void popsAnIdleImpWhenTheLevelsAreTheSame() {
        List<EntityId> imps = addImps(6);
        entityData.setComponent(imps.get(4), new CreatureAi(0, CreatureState.IDLE, IMP_ID));

        assertEquals(imps.get(4), startPopping());
    }

    @Test
    void popsTheImpFarthestFromTheHeart() {
        List<EntityId> imps = addImps(6);
        entityData.setComponent(imps.get(2), new Position(0, new Vector3f(30, 0, 12)));

        assertEquals(imps.get(2), startPopping());
    }

    @Test
    void popsTheImpAfterFiveSeconds() {
        addImps(6);
        EntityId imp = startPopping();

        runSeconds(4);
        assertNull(entityData.getComponent(imp, Damage.class));
        runSeconds(1);

        assertEquals(20, entityData.getComponent(imp, Damage.class).damage, "All of the imp's health");
        assertNull(entityData.getComponent(imp, ImpPopCountdown.class));
    }

    @Test
    void keepsTheImpIfTheKeeperCanAffordANewOne() {
        addImps(6);
        EntityId imp = startPopping();

        keeper.setMana(CREATE_IMP_COST);
        runSeconds(1);

        assertNull(entityData.getComponent(imp, ImpPopCountdown.class));
        runSeconds(5);
        assertNull(entityData.getComponent(imp, Damage.class));
    }

    /**
     * Runs the warnings until the first imp is chosen
     *
     * @return the imp chosen
     */
    private EntityId startPopping() {
        keeper.setMana(50);
        runSeconds(1 + PERIOD * 4);
        assertEquals(1, countdowns());
        return entityData.findEntity(null, ImpPopCountdown.class);
    }

    private List<EntityId> addImps(int count) {
        List<EntityId> imps = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            EntityId imp = entityData.createEntity();
            CreatureComponent creatureComponent = new CreatureComponent();
            creatureComponent.creatureId = IMP_ID;
            creatureComponent.worker = true;
            entityData.setComponents(imp, creatureComponent, experience(1), new Owner(PLAYER_ID, PLAYER_ID),
                    new Position(0, new Vector3f(11 + i, 0, 10)), new Health(20, 20),
                    new CreatureAi(0, CreatureState.WORK, IMP_ID));
            imps.add(imp);
        }
        return imps;
    }

    private static CreatureExperience experience(int level) {
        return new CreatureExperience(level, 0, 100, 1, 1);
    }

    private int countdowns() {
        return entityData.findEntities(null, ImpPopCountdown.class).size();
    }

    private void runSeconds(int seconds) {
        for (int i = 0; i < seconds; i++) {
            system.processTick(1f);
        }
    }

    private static void setting(Map<MiscType, MiscVariable> settings, MiscType type, int value) {
        settings.put(type, DestroyRoomsFixtures.destroyRoomsVariable(type, value));
    }

    private interface Calls {

        Object call(String method, Object[] args);
    }

    private static <T> T stub(Class<T> type, Calls calls) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type},
                (proxy, method, args) -> calls.call(method.getName(), args)));
    }
}
