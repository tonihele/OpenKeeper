/*
 * Copyright (C) 2014-2019 OpenKeeper
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
package toniarts.openkeeper.game.sound;

import toniarts.openkeeper.game.data.IIndexable;

/**
 * @see Converted/Interface/Texts/MULTIPLAYER.properties file, but 1-based index
 * @deprecated temporary file. Maybe we should delete it
 * @author ArchDemon
 */
@Deprecated
public enum MultiplayerType implements IIndexable {

    LVLSPE01(1),
    LVLSPE02(2),
    LVLSPE03(3),
    LVLSPE04(4),
    LVLSPE05(5),
    LVLSPE06(6),
    LVLSPE07(7),
    LVLSPE08(8),
    LVLSPE09(9),
    LVLSPE10(10),
    LVLSPE11(11),
    LVLSPE12(12),
    LVLSPE13(13),
    LVLSPE14(14),
    LVLSPE15(15),
    LVLSPE16(16),
    LVLSPE17(17),
    LVLSPE18(18),
    LVLSPE19(19),
    LVLSPE20(20),
    LVLSPE21(21),
    LVLSPE22(22),
    LVLSPE23(23),
    LVLSPE24(24),
    LVLSPE25(25),
    LVLSPE26(26),
    LVLSPE27(27),
    LVLSPE28(28),
    LVLSPE29(29),
    LVLSPE30(30),
    LVLSPE31(31),
    LVLSPE32(32),
    LVLSPE33(33),
    LVLSPE34(34),
    LVLSPE35(35),
    LVLSPE36(36),
    LVLSPE37(37),
    LVLSPE38(38),
    LVLSPE39(39),
    LVLSPE40(40),
    LVLSPE41(41),
    LVLSPE42(42),
    LVLSPE43(43),
    LVLSPE44(44),
    LVLSPE45(45),
    LVLSPE46(46),
    LVLSPE47(47),
    LVLSPE48(48),
    LVLSPE49(49),
    LVLSPE50(50),
    LVLSPE51(51),
    LVLSPE52(52),
    LVLSPE53(53),
    LVLSPE54(54),
    LVLSPE55(55),
    LVLSPE56(56),
    LVLSPE57(57),
    LVLSPE58(58),
    LVLSPE59(59),
    LVLSPE60(60);

    /**
     *
     * @param value
     */
    private MultiplayerType(int value) {
        this.value = (short) value;
    }

    private final short value;

    @Override
    public short getId() {
        return value;
    }
}
