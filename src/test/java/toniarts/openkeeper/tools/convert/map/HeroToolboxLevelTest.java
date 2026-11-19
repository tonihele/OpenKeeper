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
package toniarts.openkeeper.tools.convert.map;

import com.simsilica.es.Entity;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntitySet;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.CreatureExperience;
import toniarts.openkeeper.game.component.LevelIndicator;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.controller.CreaturesController;
import toniarts.openkeeper.game.controller.IGameController;
import toniarts.openkeeper.game.controller.IGameTimer;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.event.GameEventService;
import toniarts.openkeeper.game.map.IMapDataInformation;
import toniarts.openkeeper.game.map.IMapTileController;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.WorldUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The My Pet Dungeon level indicator and the Hero Toolbox. Clicking the
 * indicator turns it a level up and slapping a level down, round between 1 and
 * 10 (DKII.EXE 0x514320 and 0x49C2A0). The heroes placed with "I am a tool"
 * that still stand on the Hero Lair are set to its level. Lives in the
 * parser's package for the protected setters.
 */
class HeroToolboxLevelTest {

    private static final short KNIGHT_ID = 22;
    private static final short HERO_LAIR_TERRAIN_ID = 35;
    private static final short PATH_TERRAIN_ID = 13;

    private final EntityData entityData = new DefaultEntityData();
    private final Creature imp = creature((short) 1);
    private final Creature knight = creature(KNIGHT_ID);
    private final List<Thing.GoodCreature> heroes = new ArrayList<>();

    @Test
    void indicatorTurnsRoundBetweenTheFirstAndLastLevel() {
        assertEquals(2, new LevelIndicator(1).turn(1).level);
        assertEquals(1, new LevelIndicator(10).turn(1).level);
        assertEquals(10, new LevelIndicator(1).turn(-1).level);
        assertEquals(4, new LevelIndicator(5).turn(-1).level);
    }

    @Test
    void toolsInTheToolboxGetTheLevel() {
        placeHero(2, 2, true);
        CreaturesController controller = creaturesController();

        controller.levelUpTools(4);
        assertEquals(4, levelAt(2, 2));

        controller.levelUpTools(10);
        controller.levelUpTools(1);
        assertEquals(1, levelAt(2, 2));
    }

    @Test
    void otherHeroesAndToolsOutsideTheToolboxKeepTheirLevel() {
        placeHero(3, 2, false);
        placeHero(8, 8, true);
        CreaturesController controller = creaturesController();

        controller.levelUpTools(4);

        assertEquals(1, levelAt(3, 2));
        assertEquals(1, levelAt(8, 8));
    }

    private void placeHero(int x, int y, boolean tool) {
        Thing.GoodCreature hero = new Thing.GoodCreature();
        hero.setCreatureId(KNIGHT_ID);
        hero.setPosX(x);
        hero.setPosY(y);
        hero.setLevel((short) 1);
        hero.setInitialHealth(100);
        hero.setFlags2(tool ? EnumSet.of(Thing.Creature.CreatureFlag2.I_AM_A_TOOL) : EnumSet.noneOf(Thing.Creature.CreatureFlag2.class));
        heroes.add(hero);
    }

    private int levelAt(int x, int y) {
        EntitySet entities = entityData.getEntities(CreatureExperience.class, Position.class);
        try {
            for (Entity entity : entities) {
                if (WorldUtils.vectorToPoint(entity.get(Position.class).position).equals(new Point(x, y))) {
                    return entity.get(CreatureExperience.class).level;
                }
            }
            throw new AssertionError("No creature at " + x + ", " + y);
        } finally {
            entities.release();
        }
    }

    private static Creature creature(short creatureId) {
        Creature creature = new Creature();
        creature.setCreatureId(creatureId);
        creature.setFlags(EnumSet.noneOf(Creature.CreatureFlag.class));
        creature.setMeleeAttackType(Creature.AttackType.MELEE_BLUNT);
        creature.setSpells(new ArrayList<>());
        creature.getAttributes().setHp(1000);
        return creature;
    }

    private CreaturesController creaturesController() {
        IKwdFile kwdFile = stub(IKwdFile.class, (method, args) -> switch (method) {
            case "getCreature" ->
                ((Short) args[0]) == KNIGHT_ID ? knight : imp;
            case "getImp" ->
                imp;
            case "getThings" ->
                args[0] == Thing.GoodCreature.class ? heroes : Collections.emptyList();
            default ->
                null;
        });
        IGameController gameController = stub(IGameController.class, (method, args)
                -> method.equals("getGameEventService") ? stub(GameEventService.class, (m, a) -> null) : null);
        return new CreaturesController(kwdFile, entityData, Collections.emptyMap(), stub(IGameTimer.class, (method, args) -> null),
                gameController, mapController(), null);
    }

    /**
     * The Hero Lair is the tiles up to x = 4, the rest is path
     */
    private static IMapController mapController() {
        IMapDataInformation<?> mapData = stub(IMapDataInformation.class, (method, args) -> {
            if (!method.equals("getTile")) {
                return null;
            }
            int x = args[0] instanceof Point p ? p.x : (Integer) args[0];
            short terrainId = x <= 4 ? HERO_LAIR_TERRAIN_ID : PATH_TERRAIN_ID;
            return stub(IMapTileController.class, (m, a) -> m.equals("getTerrainId") ? terrainId : null);
        });
        return stub(IMapController.class, (method, args) -> method.equals("getMapData") ? mapData : null);
    }

    private interface Handler {

        Object handle(String method, Object[] args);
    }

    private static <T> T stub(Class<T> type, Handler handler) {
        return type.cast(Proxy.newProxyInstance(HeroToolboxLevelTest.class.getClassLoader(), new Class<?>[]{type},
                (proxy, method, args) -> {
                    Object result = handler.handle(method.getName(), args);
                    return result != null ? result : defaultValue(method.getReturnType());
                }));
    }

    private static Object defaultValue(Class<?> type) {
        if (type == boolean.class) {
            return false;
        }
        if (type == double.class) {
            return 0d;
        }
        if (type == float.class) {
            return 0f;
        }
        if (type == long.class) {
            return 0L;
        }
        if (type == int.class) {
            return 0;
        }
        if (type == short.class) {
            return (short) 0;
        }
        if (type == byte.class) {
            return (byte) 0;
        }
        return null;
    }

}
