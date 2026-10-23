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
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.CreatureAi;
import toniarts.openkeeper.game.component.Mana;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Slapped;
import toniarts.openkeeper.game.component.SummonedReaper;
import toniarts.openkeeper.game.controller.ICreaturesController;
import toniarts.openkeeper.game.controller.IPlayerController;
import toniarts.openkeeper.game.controller.creature.CreatureState;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.data.Keeper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SummonedReaperSystemTest {

    private static final short PLAYER_ID = 3;
    private static final int MANA_DRAIN = 2000;

    private final EntityData entityData = new DefaultEntityData();
    private final Keeper keeper = new Keeper(false, PLAYER_ID);
    private final List<EntityId> removedCreatures = new ArrayList<>();
    private final List<EntityId> thinkingReapers = new ArrayList<>();
    private boolean somethingToDo = true;
    private double now = 0;

    @Test
    void keeperPaysNothingForTheFirstFiftySeconds() {
        EntityId reaper = addReaper(0, MANA_DRAIN);
        SummonedReaperSystem system = system();

        tickAt(system, 49.9);

        assertNull(entityData.getComponent(reaper, Mana.class));
    }

    @Test
    void reaperDrainsManaOnceTheFiftySecondsAreUp() {
        keeper.setMana(100000);
        EntityId reaper = addReaper(0, MANA_DRAIN);
        SummonedReaperSystem system = system();

        tickAt(system, 50);

        assertEquals(-MANA_DRAIN, entityData.getComponent(reaper, Mana.class).manaGeneration);
    }

    @Test
    void reaperStaysWhileTheKeeperCanPay() {
        keeper.setMana(MANA_DRAIN);
        EntityId reaper = addReaper(0, MANA_DRAIN);
        SummonedReaperSystem system = system();

        tickAt(system, 50);
        tickAt(system, 51);
        tickAt(system, 52);

        assertFalse(isLeaving(reaper));
    }

    @Test
    void reaperLeavesWhenTheKeeperRunsOutOfMana() {
        keeper.setMana(MANA_DRAIN - 1);
        EntityId reaper = addReaper(0, MANA_DRAIN);
        SummonedReaperSystem system = system();

        tickAt(system, 50);
        assertFalse(isLeaving(reaper));
        tickAt(system, 51);

        assertTrue(isLeaving(reaper));

        // He doesn't cost anything on his way out, and he doesn't do anything anymore
        assertNull(entityData.getComponent(reaper, Mana.class));
        assertNull(entityData.getComponent(reaper, CreatureAi.class));
        assertTrue(removedCreatures.isEmpty());
    }

    @Test
    void leavingReaperIsGoneAfterSixSeconds() {
        keeper.setMana(0);
        EntityId reaper = addReaper(0, MANA_DRAIN);
        SummonedReaperSystem system = system();
        tickAt(system, 50);
        tickAt(system, 51);

        tickAt(system, 56.9);
        assertTrue(removedCreatures.isEmpty());
        tickAt(system, 57);

        assertEquals(List.of(reaper), removedCreatures);
    }

    @Test
    void leavingReaperGivesBackTheOtherManaFlow() {
        keeper.setMana(0);
        EntityId reaper = addReaper(0, MANA_DRAIN);
        entityData.setComponent(reaper, new Mana(5));
        SummonedReaperSystem system = system();

        tickAt(system, 50);
        assertEquals(5 - MANA_DRAIN, entityData.getComponent(reaper, Mana.class).manaGeneration);
        tickAt(system, 51);

        assertEquals(5, entityData.getComponent(reaper, Mana.class).manaGeneration);
    }

    @Test
    void oneShotReaperLeavesWhenItsTimeIsUp() {
        EntityId reaper = addReaper(99, 0);
        SummonedReaperSystem system = system();

        tickAt(system, 98.9);
        assertFalse(isLeaving(reaper));
        tickAt(system, 99);

        assertTrue(isLeaving(reaper));
        assertNull(entityData.getComponent(reaper, Mana.class));
    }

    @Test
    void reaperThinksWhileHeIsAround() {
        EntityId reaper = addReaper(0, MANA_DRAIN);

        tickAt(system(), 1);

        assertEquals(List.of(reaper), thinkingReapers);
    }

    @Test
    void reaperLeavesWhenThereIsNothingLeftForHim() {
        somethingToDo = false;
        EntityId reaper = addReaper(0, MANA_DRAIN);

        tickAt(system(), 1);

        assertTrue(isLeaving(reaper));
    }

    @Test
    void slappingSendsTheReaperBack() {
        EntityId reaper = addReaper(0, MANA_DRAIN);
        SummonedReaperSystem system = system();
        tickAt(system, 5);
        entityData.setComponent(reaper, new Slapped(5));

        tickAt(system, 5.25);

        assertTrue(isLeaving(reaper));
        assertNull(entityData.getComponent(reaper, CreatureAi.class));
    }

    @Test
    void fallingReaperDoesNotThinkYet() {
        EntityId reaper = addReaper(0, MANA_DRAIN);
        entityData.removeComponent(reaper, CreatureAi.class);

        tickAt(system(), 1);

        assertTrue(thinkingReapers.isEmpty());
        assertFalse(isLeaving(reaper));
    }

    @Test
    void leavingReaperDoesNotThink() {
        keeper.setMana(0);
        addReaper(0, MANA_DRAIN);
        SummonedReaperSystem system = system();
        tickAt(system, 50);
        tickAt(system, 51);
        thinkingReapers.clear();

        tickAt(system, 52);

        assertTrue(thinkingReapers.isEmpty());
    }

    private boolean isLeaving(EntityId reaper) {
        SummonedReaper summonedReaper = entityData.getComponent(reaper, SummonedReaper.class);
        assertNotNull(summonedReaper);
        return summonedReaper.isLeaving();
    }

    private EntityId addReaper(double lifetime, int manaDrain) {
        EntityId reaper = entityData.createEntity();
        entityData.setComponent(reaper, new Owner(PLAYER_ID, PLAYER_ID));
        entityData.setComponent(reaper, new CreatureAi(now, CreatureState.IDLE, (short) 27));
        entityData.setComponent(reaper, new SummonedReaper(now, lifetime, manaDrain));
        return reaper;
    }

    private void tickAt(SummonedReaperSystem system, double time) {
        now = time;
        system.processTick(0.25f);
    }

    private SummonedReaperSystem system() {
        IPlayerController playerController = (IPlayerController) Proxy.newProxyInstance(IPlayerController.class.getClassLoader(),
                new Class<?>[]{IPlayerController.class}, (proxy, method, args) -> {
                    if (!method.getName().equals("getKeeper")) {
                        throw new UnsupportedOperationException(method.getName());
                    }
                    return keeper;
                });
        return new SummonedReaperSystem(entityData, List.of(playerController), creaturesController(), () -> now,
                (reaper, ownerId) -> {
                    thinkingReapers.add(reaper);
                    return somethingToDo;
                });
    }

    private ICreaturesController creaturesController() {
        return (ICreaturesController) Proxy.newProxyInstance(ICreaturesController.class.getClassLoader(),
                new Class<?>[]{ICreaturesController.class}, (proxy, method, args) -> {
                    if (!method.getName().equals("createController")) {
                        throw new UnsupportedOperationException(method.getName());
                    }
                    EntityId entityId = (EntityId) args[0];
                    return creatureController(entityId);
                });
    }

    private ICreatureController creatureController(EntityId entityId) {
        return (ICreatureController) Proxy.newProxyInstance(ICreatureController.class.getClassLoader(),
                new Class<?>[]{ICreatureController.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "remove" -> {
                        removedCreatures.add(entityId);
                        entityData.removeEntity(entityId);
                        yield null;
                    }
                    case "unassingCurrentTask", "stopCreature" -> null;
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }

}
