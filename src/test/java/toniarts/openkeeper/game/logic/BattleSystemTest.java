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
import java.util.Set;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.AttackTarget;
import toniarts.openkeeper.game.component.Battle;
import toniarts.openkeeper.game.component.CreatureAi;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.DoorComponent;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.controller.creature.CreatureState;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BattleSystemTest {

    private static final short KEEPER_ID = 3;
    private static final short HERO_ID = 1;

    private final EntityData entityData = new DefaultEntityData();
    private double gameTime;
    private final BattleSystem battleSystem = new BattleSystem(entityData, () -> gameTime);

    @Test
    void creaturesFightingEachOtherStartABattle() {
        EntityId imp = addCreature(KEEPER_ID, 10, 10);
        EntityId dwarf = addCreature(HERO_ID, 12, 10);
        fight(imp, dwarf);
        fight(dwarf, imp);

        battleSystem.processTick(0.25f);

        Battle battle = entityData.getComponent(getBattle(), Battle.class);
        assertEquals(Set.of(KEEPER_ID, HERO_ID), battle.playerIds);
        assertEquals(new Vector3f(11, 0, 10), battle.position);
    }

    @Test
    void fightsWithinFiveTilesShareABattle() {
        EntityId hero = addCreature(HERO_ID, 10, 10);
        fight(addCreature(KEEPER_ID, 10, 10), hero);
        fight(addCreature(KEEPER_ID, 15, 10), hero);

        battleSystem.processTick(0.25f);

        assertEquals(1, getBattles().size());
    }

    @Test
    void fightsFurtherAwayStartAnotherBattle() {
        EntityId hero = addCreature(HERO_ID, 10, 10);
        fight(addCreature(KEEPER_ID, 10, 10), hero);
        fight(addCreature(KEEPER_ID, 16, 10), hero);

        battleSystem.processTick(0.25f);

        assertEquals(2, getBattles().size());
    }

    @Test
    void attackingADoorIsNoBattle() {
        EntityId door = entityData.createEntity();
        entityData.setComponent(door, new DoorComponent());
        fight(addCreature(HERO_ID, 10, 10), door);

        battleSystem.processTick(0.25f);

        assertTrue(getBattles().isEmpty());
    }

    @Test
    void battleLastsFourSecondsAfterTheFightStops() {
        EntityId imp = addCreature(KEEPER_ID, 10, 10);
        fight(imp, addCreature(HERO_ID, 10, 10));
        battleSystem.processTick(0.25f);

        stopFighting(imp);
        gameTime = 3.9;
        battleSystem.processTick(0.25f);
        assertEquals(1, getBattles().size());

        gameTime = 4;
        battleSystem.processTick(0.25f);
        assertTrue(getBattles().isEmpty());
    }

    @Test
    void creatureStillFightingStaysInTheBattle() {
        EntityId imp = addCreature(KEEPER_ID, 10, 10);
        fight(imp, addCreature(HERO_ID, 10, 10));
        battleSystem.processTick(0.25f);

        gameTime = 60;
        battleSystem.processTick(0.25f);

        assertEquals(1, getBattles().size());
    }

    @Test
    void pickedUpCreatureLeavesAtOnce() {
        EntityId imp = addCreature(KEEPER_ID, 10, 10);
        EntityId hero = addCreature(HERO_ID, 10, 10);
        fight(imp, hero);
        battleSystem.processTick(0.25f);

        entityData.removeComponent(imp, Position.class);
        entityData.removeComponent(imp, CreatureAi.class);
        battleSystem.processTick(0.25f);

        assertTrue(getBattles().isEmpty());
    }

    @Test
    void battleListsOnlyThePlayersStillInIt() {
        EntityId imp = addCreature(KEEPER_ID, 10, 10);
        EntityId dwarf = addCreature(HERO_ID, 10, 10);
        fight(imp, dwarf);
        fight(dwarf, imp);
        battleSystem.processTick(0.25f);

        entityData.removeComponent(imp, CreatureAi.class);
        battleSystem.processTick(0.25f);

        assertEquals(Set.of(HERO_ID), entityData.getComponent(getBattle(), Battle.class).playerIds);
    }

    private EntityId addCreature(short ownerId, float x, float z) {
        EntityId creature = entityData.createEntity();
        entityData.setComponent(creature, new CreatureComponent());
        entityData.setComponent(creature, new Owner(ownerId, ownerId));
        entityData.setComponent(creature, new Position(0, new Vector3f(x, 0, z)));
        entityData.setComponent(creature, new CreatureAi(gameTime, CreatureState.IDLE, (short) 1));
        return creature;
    }

    private void fight(EntityId creature, EntityId target) {
        entityData.setComponent(creature, new CreatureAi(gameTime, CreatureState.FIGHT, (short) 1));
        entityData.setComponent(creature, new AttackTarget(target));
    }

    private void stopFighting(EntityId creature) {
        entityData.setComponent(creature, new CreatureAi(gameTime, CreatureState.IDLE, (short) 1));
        entityData.removeComponent(creature, AttackTarget.class);
    }

    private Set<EntityId> getBattles() {
        return entityData.findEntities(null, Battle.class);
    }

    private EntityId getBattle() {
        Set<EntityId> battles = getBattles();
        assertEquals(1, battles.size());
        return battles.iterator().next();
    }

}
