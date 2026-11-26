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
package toniarts.openkeeper.game.task.worker;

import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.Gold;
import toniarts.openkeeper.game.component.ObjectComponent;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.TileWork;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.controller.room.AbstractRoomController;
import toniarts.openkeeper.game.logic.IEntityPositionLookup;
import toniarts.openkeeper.game.map.IMapDataInformation;
import toniarts.openkeeper.game.map.IMapTileInformation;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.CreatureSleepFixtures;
import toniarts.openkeeper.tools.convert.map.Player;
import toniarts.openkeeper.tools.convert.map.Terrain;
import toniarts.openkeeper.tools.convert.map.TerrainFixtures;
import toniarts.openkeeper.utils.Point;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * DK II's dig swing (0x496AA0, 0x496E00): one hit at the start of each swing,
 * and the digger stays until the swing that brought the tile down has played
 * out. Swings are the creatures' melee animations in the shipped data, the
 * damage the level 20 variables at 4 ticks a second.
 */
class DigTileTaskTest {

    private static final int TICKS_PER_SECOND = 4;
    private static final int DIRT_HEALTH = 165;
    private static final int FRESH = 80;
    private static final short PLAYER = 3;

    @Test
    void dwarfTakesTwoSwingsForDirt() {

        // Dwarf-Melee: 30 frames at 30 fps, 9 start and 10 end frames. Six times the own wall rate, 154 a swing
        Creature dwarf = CreatureSleepFixtures.digger("Dwarf", 30, 30, 9, 10);
        int damage = TileWork.perSwing(6 * (-130 / TICKS_PER_SECOND), FRESH);

        // The second swing starts once the first, with the start of the animation, is over (1.3 s), and the
        // dwarf leaves after the last swing and the end of the animation (1.33 s)
        assertEquals(new Dig(List.of(0, 6), 12), dig(dwarf, DIRT_HEALTH, damage, 40));
    }

    @Test
    void impTakesOneSwingForDirt() {

        // Imp_Dig: 15 frames at 20 fps, 8 end frames. The rock rate, 1000 a swing
        Creature imp = CreatureSleepFixtures.digger("Imp", 15, 20, 0, 8);
        int damage = TileWork.perSwing(-5000 / TICKS_PER_SECOND, FRESH);

        assertEquals(new Dig(List.of(0), 5), dig(imp, DIRT_HEALTH, damage, 40));
    }

    @Test
    void impHitsAWallOnceASwing() {
        Creature imp = CreatureSleepFixtures.digger("Imp", 15, 20, 0, 8);
        int damage = TileWork.perSwing(-130 / TICKS_PER_SECOND, FRESH);

        // A claimed wall of 1000 health, 26 a swing every 3 ticks: 39 swings, then the end of the animation
        Dig dig = dig(imp, 1000, damage, 200);

        assertEquals(39, dig.hits().size());
        assertEquals(List.of(0, 3, 6, 9), dig.hits().subList(0, 4));
        assertEquals(38 * 3 + 5, dig.leaves());
    }

    @Test
    void diggerStaysUntilItsSwingIsOver() {
        Creature dwarf = CreatureSleepFixtures.digger("Dwarf", 30, 30, 9, 10);
        Digging digging = new Digging(DIRT_HEALTH, DIRT_HEALTH);
        DigTileTask task = new DigTileTask(null, digging.mapController(), new Point(1, 1), PLAYER);
        ICreatureController creature = creature(dwarf);
        task.assign(creature, false);

        task.executeTask(creature, 0);

        // The tile fell to the first hit, the dwarf swings on through the whole animation, start and end
        // included (49 frames, 1.63 s)
        assertEquals(0, digging.health);
        for (int tick = 1; tick <= 6; tick++) {
            task.executeTask(creature, (float) tick / TICKS_PER_SECOND);
            assertEquals(true, task.isValid(creature), "tick " + tick);
        }
        task.executeTask(creature, 7f / TICKS_PER_SECOND);
        assertEquals(false, task.isValid(creature));

        // Others can't take a fallen tile
        assertEquals(false, task.isValid(creature(dwarf)));
    }

    @Test
    void impMinesOnUntilEverySpotOfItsSideHoldsAFullGoldPile() {

        // DK II (0x465870, 0x469AF0): a spot over MAX_GOLD_PILE_OUTSIDE_TREASURY (2000) sends the imp to another of the
        // side's three spots. The one pile on the tile stands for all three, so the imp gives up the side only over 6000
        Creature imp = CreatureSleepFixtures.digger("Imp", 15, 20, 0, 8);
        for (int pile : new int[]{2001, 6000, 6001}) {
            Digging digging = new Digging(DIRT_HEALTH, 1);
            EntityData entityData = new DefaultEntityData();
            IEntityPositionLookup lookup = goldPile(entityData, pile, new Point(1, 2)::equals);
            DigTileTask task = new DigTileTask(null, digging.mapController(), new Point(1, 1), PLAYER, entityData, lookup, 2000);
            List<String> calls = new ArrayList<>();
            ICreatureController digger = (ICreatureController) Proxy.newProxyInstance(ICreatureController.class.getClassLoader(),
                    new Class<?>[]{ICreatureController.class}, (proxy, method, args) -> switch (method.getName()) {
                case "getCreature" ->
                    imp;
                case "getCreatureCoordinates" ->
                    new Point(1, 2);
                case "addGold", "unassingCurrentTask" -> {
                    calls.add(method.getName());
                    yield null;
                }
                case "hashCode" ->
                    System.identityHashCode(proxy);
                case "equals" ->
                    proxy == args[0];
                default ->
                    throw new UnsupportedOperationException(method.getName());
            });
            task.assign(digger, false);

            task.executeTask(digger, 0);

            assertEquals(pile > 6000 ? List.of("unassingCurrentTask") : List.of("addGold"), calls, "pile " + pile);
            assertEquals(pile > 6000 ? List.of() : List.of(0), digging.hits, "pile " + pile);

            // And DK II's search for a spot to dig from skips it (0x469AF0)
            assertEquals(pile <= 6000, task.isWorkSpot(new Point(1, 2)), "pile " + pile);
            assertTrue(task.isWorkSpot(new Point(0, 1)));
        }
    }

    @Test
    void diggerGivesUpTheTileWhenEverySideHoldsAFullGoldPile() {

        // With no spot left to dig from, the digger has nowhere to go. It drops the task and looks for other work
        // rather than wait for a spot forever, in the middle of a swing too
        Creature imp = CreatureSleepFixtures.digger("Imp", 15, 20, 0, 8);
        Digging digging = new Digging(DIRT_HEALTH, 1);
        EntityData entityData = new DefaultEntityData();
        AtomicBoolean piledUp = new AtomicBoolean();
        DigTileTask task = new DigTileTask(null, digging.mapController(), new Point(1, 1), PLAYER, entityData,
                goldPile(entityData, 6001, p -> piledUp.get()), 2000);
        ICreatureController digger = creature(imp);
        task.assign(digger, false);
        task.executeTask(digger, 0);
        assertTrue(task.isValid(digger));

        piledUp.set(true);

        assertFalse(task.isValid(digger), "Still swinging");
        assertFalse(task.isValid(creature(imp)), "Nobody else takes it");
        assertTrue(task.isValid(null), "The tile stays tagged for when the gold has been taken away");
    }

    /**
     * Ticks of the hits, and the tick the digger is done
     */
    private record Dig(List<Integer> hits, int leaves) {

    }

    private static Dig dig(Creature creature, int health, int damage, int maxTicks) {
        Digging digging = new Digging(health, damage);
        DigTileTask task = new DigTileTask(null, digging.mapController(), new Point(1, 1), PLAYER);
        ICreatureController digger = creature(creature);
        task.assign(digger, false);
        for (int tick = 0; tick < maxTicks; tick++) {
            digging.tick = tick;
            task.executeTask(digger, (float) tick / TICKS_PER_SECOND);
            if (!task.isValid(digger)) {
                return new Dig(digging.hits, tick);
            }
        }
        throw new AssertionError("Still digging after " + maxTicks + " ticks");
    }

    /**
     * A loose gold pile that lies on the given tiles
     */
    private static IEntityPositionLookup goldPile(EntityData entityData, int gold, Predicate<Point> where) {
        EntityId pile = entityData.createEntity();
        entityData.setComponents(pile, new ObjectComponent((short) 1, AbstractRoomController.ObjectType.GOLD), new Gold(gold, 2000));
        return (IEntityPositionLookup) Proxy.newProxyInstance(IEntityPositionLookup.class.getClassLoader(),
                new Class<?>[]{IEntityPositionLookup.class}, (proxy, method, args) -> switch (method.getName()) {
            case "getEntitiesInLocation" ->
                where.test((Point) args[0]) ? List.of(pile) : List.of();
            default ->
                throw new UnsupportedOperationException(method.getName());
        });
    }

    private static ICreatureController creature(Creature creature) {
        return (ICreatureController) Proxy.newProxyInstance(ICreatureController.class.getClassLoader(),
                new Class<?>[]{ICreatureController.class}, (proxy, method, args) -> switch (method.getName()) {
            case "getCreature" ->
                creature;
            case "getCreatureCoordinates" ->
                new Point(1, 2);
            case "addGold" ->
                0;
            case "hashCode" ->
                System.identityHashCode(proxy);
            case "equals" ->
                proxy == args[0];
            default ->
                throw new UnsupportedOperationException(method.getName());
        });
    }

    /**
     * One tile being dug, it stays selected until its health is gone
     */
    private static final class Digging {

        private final int damage;
        private final List<Integer> hits = new ArrayList<>();
        private int health;
        private int tick = 0;

        private Digging(int health, int damage) {
            this.health = health;
            this.damage = damage;
        }

        private IMapController mapController() {
            IMapTileInformation tile = (IMapTileInformation) Proxy.newProxyInstance(IMapTileInformation.class.getClassLoader(),
                    new Class<?>[]{IMapTileInformation.class}, (proxy, method, args) -> switch (method.getName()) {
                case "isSelected" ->
                    health > 0;
                case "getLocation" ->
                    new Point(-1, -1);
                case "getOwnerId" ->
                    Player.NEUTRAL_PLAYER_ID;
                default ->
                    throw new UnsupportedOperationException(method.getName());
            });
            IMapDataInformation<?> mapData = (IMapDataInformation<?>) Proxy.newProxyInstance(IMapDataInformation.class.getClassLoader(),
                    new Class<?>[]{IMapDataInformation.class}, (proxy, method, args) -> switch (method.getName()) {
                case "getTile" ->
                    tile;
                default ->
                    throw new UnsupportedOperationException(method.getName());
            });
            return (IMapController) Proxy.newProxyInstance(IMapController.class.getClassLoader(),
                    new Class<?>[]{IMapController.class}, (proxy, method, args) -> switch (method.getName()) {
                case "getMapData" ->
                    mapData;
                case "isSolid" ->
                    false;
                case "getTerrain" ->
                    TerrainFixtures.terrain((short) 1, TerrainFixtures.flags(Terrain.TerrainFlag.SOLID), 0, DIRT_HEALTH, DIRT_HEALTH, (short) 1);
                case "damageTile" -> {
                    hits.add(tick);
                    health = Math.max(0, health - damage);
                    yield 0;
                }
                default ->
                    throw new UnsupportedOperationException(method.getName());
            });
        }

    }

}
