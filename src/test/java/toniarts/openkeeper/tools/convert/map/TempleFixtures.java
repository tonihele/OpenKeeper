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

import java.util.Arrays;

/**
 * Builds the {@link Variable} fixtures the Temple tests need. Lives in
 * {@code Variable}'s own package because its setters are {@code protected}.
 */
public final class TempleFixtures {

    private TempleFixtures() {
    }

    /**
     * A sacrifice recipe of up to three creatures
     *
     * @param rewardType the reward type
     * @param rewardValue the reward value, e.g. the creature or object ID
     * @param creatureIds the creatures to sacrifice, in order
     * @return the recipe
     */
    public static Variable.Sacrifice creatureSacrifice(Variable.SacrificeRewardType rewardType, int rewardValue,
            int... creatureIds) {
        Variable.SacrificeType[] types = new Variable.SacrificeType[creatureIds.length];
        for (int i = 0; i < types.length; i++) {
            types[i] = Variable.SacrificeType.CREATURE;
        }
        return sacrifice(rewardType, rewardValue, types, creatureIds);
    }

    /**
     * A sacrifice recipe
     *
     * @param rewardType the reward type
     * @param rewardValue the reward value
     * @param types the item types, in order
     * @param ids the item IDs, in order
     * @return the recipe
     */
    public static Variable.Sacrifice sacrifice(Variable.SacrificeRewardType rewardType, int rewardValue,
            Variable.SacrificeType[] types, int[] ids) {
        Variable.Sacrifice sacrifice = new Variable.Sacrifice();
        sacrifice.setType1(types.length > 0 ? types[0] : Variable.SacrificeType.NONE);
        sacrifice.setId1((short) (ids.length > 0 ? ids[0] : 0));
        sacrifice.setType2(types.length > 1 ? types[1] : Variable.SacrificeType.NONE);
        sacrifice.setId2((short) (ids.length > 1 ? ids[1] : 0));
        sacrifice.setType3(types.length > 2 ? types[2] : Variable.SacrificeType.NONE);
        sacrifice.setId3((short) (ids.length > 2 ? ids[2] : 0));
        sacrifice.setRewardType(rewardType);
        sacrifice.setRewardValue(rewardValue);
        return sacrifice;
    }

    /**
     * The Temple room, without any floor or wall objects
     *
     * @return the room
     */
    public static Room temple() {
        Room room = new Room();
        room.setRoomId((short) 13);
        room.setName("Temple");
        room.setObjects(Arrays.asList((short) 0, (short) 0, (short) 0, (short) 0, (short) 0, (short) 0));
        return room;
    }

    /**
     * A level variable
     *
     * @param type the variable
     * @param value its value
     * @return the variable
     */
    public static Variable.MiscVariable miscVariable(Variable.MiscVariable.MiscType type, int value) {
        Variable.MiscVariable variable = new Variable.MiscVariable();
        variable.setVariableId(type);
        variable.setValue(value);
        return variable;
    }

}
