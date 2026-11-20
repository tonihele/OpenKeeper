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

import com.simsilica.es.EntityId;
import java.lang.reflect.Array;
import java.lang.reflect.Proxy;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import com.simsilica.es.EntityData;
import com.simsilica.es.base.DefaultEntityData;
import toniarts.openkeeper.game.component.DoorComponent;
import toniarts.openkeeper.game.component.Health;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Party;
import toniarts.openkeeper.game.controller.ILevelInfo;
import toniarts.openkeeper.game.controller.creature.CreatureController;
import toniarts.openkeeper.game.controller.ICreaturesController;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.controller.creature.PartyController;
import toniarts.openkeeper.game.logic.IEntityPositionLookup;
import toniarts.openkeeper.game.map.IMapTileInformation;
import toniarts.openkeeper.game.navigation.pathfinding.INavigable;
import toniarts.openkeeper.utils.Point;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A hero party takes its leader's way, and in DK II the leader's way goes
 * through enemy doors like any creature's (DKII.EXE 0x462560, 0x4C8BD0).
 */
class HeroPartyDoorTest {

    private static final EntityId DOOR = new EntityId(7);

    @Test
    void thePartyBreaksTheDoorsItsLeaderBreaks() {
        PartyController party = party(true, false);

        assertEquals(INavigable.DOOR_COST, party.getCost(null, doorTile(), mapController(), positionLookup()));
    }

    @Test
    void aDoorItsLeaderDoesNotBreakStopsTheParty() {
        PartyController party = party(false, true);

        assertNull(party.getCost(null, doorTile(), mapController(), positionLookup()));
    }

    @Test
    void aFollowerDoesNotBreakTheDoorItsLeaderBreaks() {
        EntityData data = new DefaultEntityData();
        EntityId door = data.createEntity();
        data.setComponents(door, new DoorComponent((short) 1, false, false), new Health(2000, 2000),
                new Owner(Player.KEEPER1_ID, Player.KEEPER1_ID));
        EntityId leaderId = hero(data);
        EntityId followerId = hero(data);
        Thing.GoodCreature leaderThing = member(Thing.Creature.CreatureFlag.LEADER);
        Thing.GoodCreature followerThing = member(Thing.Creature.CreatureFlag.FOLLOWER);
        Thing.HeroParty heroParty = new Thing.HeroParty();
        heroParty.setHeroPartyMembers(List.of(leaderThing, followerThing));
        PartyController party = new PartyController(heroParty);
        CreatureController leader = heroController(data, leaderId, party);
        CreatureController follower = heroController(data, followerId, party);
        party.addMemberInstance(leaderThing, leader);
        party.addMemberInstance(followerThing, follower);

        assertTrue(leader.canBreakDoor(door));
        assertFalse(follower.canBreakDoor(door));
        assertTrue(party.canBreakDoor(door));
    }

    private static EntityId hero(EntityData data) {
        EntityId id = data.createEntity();
        data.setComponents(id, new Owner(Player.GOOD_PLAYER_ID, Player.GOOD_PLAYER_ID), new Health(100, 100),
                new Party(1));
        return id;
    }

    private static CreatureController heroController(EntityData data, EntityId id, PartyController party) {
        Creature hero = CombatCreatureFixtures.fighter((short) 1, Creature.FightStyle.BLITZER);
        Keeper heroes = new Keeper(false, Player.GOOD_PLAYER_ID);
        ILevelInfo levelInfo = stub(ILevelInfo.class,
                (method, args) -> method.equals("getPlayer") ? heroes : null);
        ICreaturesController creatures = stub(ICreaturesController.class,
                (method, args) -> method.equals("getPartyById") ? party : null);
        return new CreatureController(id, data, hero, null, null, () -> 0, Map.of(), creatures, null, null,
                levelInfo, null, null, null);
    }

    private static PartyController party(boolean leaderBreaksDoors, boolean followerBreaksDoors) {
        Thing.GoodCreature leader = member(Thing.Creature.CreatureFlag.LEADER);
        Thing.GoodCreature follower = member(Thing.Creature.CreatureFlag.FOLLOWER);
        Thing.HeroParty heroParty = new Thing.HeroParty();
        heroParty.setHeroPartyMembers(List.of(leader, follower));
        PartyController party = new PartyController(heroParty);
        party.addMemberInstance(leader, creature(leaderBreaksDoors));
        party.addMemberInstance(follower, creature(followerBreaksDoors));
        return party;
    }

    private static Thing.GoodCreature member(Thing.Creature.CreatureFlag role) {
        Thing.GoodCreature member = new Thing.GoodCreature();
        member.setFlags(EnumSet.of(role));
        return member;
    }

    private static ICreatureController creature(boolean breaksDoors) {
        return stub(ICreatureController.class, (method, args) -> switch (method) {
            case "canBreakDoor" -> breaksDoors && DOOR.equals(args[0]);
            default -> null;
        });
    }

    private static IMapController mapController() {
        Terrain floor = TerrainFixtures.terrain((short) 1, TerrainFixtures.flags(), 0, 0, 0, (short) 0);
        return stub(IMapController.class, (method, args) -> method.equals("getTerrain") ? floor : null);
    }

    private static IEntityPositionLookup positionLookup() {
        return stub(IEntityPositionLookup.class, (method, args) -> method.equals("getBlockingDoor") ? DOOR : null);
    }

    private static IMapTileInformation doorTile() {
        return stub(IMapTileInformation.class, (method, args) -> switch (method) {
            case "getLocation" -> new Point(2, 2);
            case "getX", "getY" -> 2;
            default -> null;
        });
    }

    private interface Calls {
        Object call(String method, Object[] args);
    }

    private static <T> T stub(Class<T> type, Calls calls) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type},
                (proxy, method, args) -> {
                    Object result = calls.call(method.getName(), args);
                    Class<?> returnType = method.getReturnType();

                    // Default primitives, e.g. a creature that isn't incapacitated
                    return result != null || !returnType.isPrimitive() || returnType == void.class
                            ? result : Array.get(Array.newInstance(returnType, 1), 0);
                }));
    }
}
