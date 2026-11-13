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

import com.jme3.math.Vector2f;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import java.util.EnumSet;
import java.util.List;
import java.util.function.BiFunction;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import toniarts.openkeeper.common.RoomInstance;
import toniarts.openkeeper.game.component.CreatureAi;
import toniarts.openkeeper.game.controller.CreaturesController;
import toniarts.openkeeper.game.controller.IGameTimer;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.creature.CreatureState;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.game.controller.room.storage.IRoomObjectControl;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A creature placed in the level editor on a prison of another player is a
 * prisoner only when it is set to act as dropped. On level8 the heroes standing
 * in Keeper Dante's ruined prison are not, the neutral creatures there are.
 */
class ActAsDroppedTest {

    private static final short DWARF = 14;

    @ParameterizedTest
    @CsvSource({"false, IDLE", "true, IMPRISONED"})
    void onlyACreatureActingAsDroppedStartsImprisoned(boolean actAsDropped, CreatureState state) {
        EntityData entityData = new DefaultEntityData();
        Thing.GoodCreature hero = new Thing.GoodCreature();
        hero.setCreatureId(DWARF);
        hero.setLevel((short) 1);
        hero.setInitialHealth(100);
        hero.setFlags(actAsDropped ? EnumSet.of(Thing.Creature.CreatureFlag.ACT_AS_DROPPED)
                : EnumSet.of(Thing.Creature.CreatureFlag.WILL_FIGHT));

        EntityId entityId = creaturesController(entityData).spawnCreature(hero, new Vector2f(1, 1));

        assertEquals(state, entityData.getComponent(entityId, CreatureAi.class).getCreatureState());
    }

    private static CreaturesController creaturesController(EntityData entityData) {
        Creature dwarf = ShotFixtures.creature(DWARF, EnumSet.noneOf(Creature.CreatureFlag.class), EnumSet.noneOf(Creature.CreatureFlag3.class));
        dwarf.setMeleeAttackType(Creature.AttackType.MELEE_BLUNT);
        dwarf.setSpells(List.of());
        IKwdFile kwdFile = stub(IKwdFile.class, (method, args) -> switch (method) {
            case "getThings" -> List.of();
            case "getCreature" -> dwarf;
            case "getImp" -> ShotFixtures.creature((short) 1, EnumSet.noneOf(Creature.CreatureFlag.class), EnumSet.noneOf(Creature.CreatureFlag3.class));
            default -> null;
        });

        // The neutral prison of a ruined dungeon
        RoomInstance prison = new RoomInstance(new Room());
        prison.setOwnerId(Player.NEUTRAL_PLAYER_ID);
        IRoomObjectControl prisoners = stub(IRoomObjectControl.class, (method, args) -> null);
        IRoomController room = stub(IRoomController.class, (method, args) -> switch (method) {
            case "getRoomInstance" -> prison;
            case "hasObjectControl" -> true;
            case "getObjectControl" -> prisoners;
            default -> null;
        });
        IMapController mapController = stub(IMapController.class, (method, args) -> room);
        IGameTimer gameTimer = stub(IGameTimer.class, (method, args) -> 0.0);

        return new CreaturesController(kwdFile, entityData, null, gameTimer, null, mapController, null);
    }

    @SuppressWarnings("unchecked")
    private static <T> T stub(Class<T> type, BiFunction<String, Object[], Object> handler) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type},
                (proxy, method, args) -> handler.apply(method.getName(), args));
    }
}
