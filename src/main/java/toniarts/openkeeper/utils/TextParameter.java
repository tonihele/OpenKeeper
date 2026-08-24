/*
 * Copyright (C) 2014-2026 OpenKeeper
 *
 * OpenKeeper is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package toniarts.openkeeper.utils;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Parameters embedded in Dungeon Keeper text resources.
 * <p>
 * The numeric IDs are part of the original text-resource format and must not
 * be replaced with enum ordinals.
 */
public enum TextParameter {

    NAME(1),
    COST(2),
    SPELL_LEVEL(3),
    CLAIMED_LAND_MANA(4),
    MANA_SOURCES(5),
    TEMPLE_PRAYERS(6),
    IMP_UPKEEP(7),
    TRAP_UPKEEP(8),
    SPELL_MANA_USAGE(9),
    DUNGEON_HEART_HEALTH(10),
    PAYDAY_COST(11),
    IDLE_CREATURE_COUNT(12),
    CLAIMED_LAND_MANA_GAIN(13),
    MANA_SOURCE_GAIN(14),
    TEMPLE_PRAYER_GAIN(15),
    IMP_UPKEEP_LOSS(16),
    TRAP_OR_DOOR_COST(17),
    SPELL_MANA_COST(18),
    CURRENT_DUNGEON_HEART_HEALTH(19),
    CURRENT_PAYDAY_COST(20),
    ROOM_COST(21),
    ROOM_SIZE_HINT(22),
    HORNY_TALISMAN_MANA_COST(23),
    SPECIAL_COUNT(24),
    MANA_USAGE(25),
    MANA_COST_TO_FIRE(26),
    CREATURE_DESCRIPTION(27),
    CREATURE_FIGHT_STYLE(28),
    CREATURE_INSTANCE_NAME(29),
    CREATURE_NAME(30),
    CREATURE_ACTIVITY(31),
    CREATURE_STATUS(32),
    CREATURE_MOOD(33),
    CREATURE_BLOOD_TYPE(34),
    CREATURE_PAY(35),
    CREATURE_GOLD(36),
    HEALTH_PERCENTAGE(37),
    ROOM_USED_CAPACITY(38),
    ROOM_MAX_CAPACITY(39),
    DUNGEON_HEART_MANA(40),
    DUNGEON_HEART_MAX_MANA(41),
    ATTRACTED_CREATURE_COUNT(42),
    PORTAL_CREATURE_CAPACITY(43),
    PORTAL_COUNT(44),
    LAIR_COUNT(45),
    HATCHERY_COUNT(46),
    TREASURY_COUNT(47),
    LIBRARY_COUNT(48),
    TRAINING_ROOM_COUNT(49),
    WORKSHOP_COUNT(50),
    GUARD_ROOM_COUNT(51),
    PATROL_ROUTE_COUNT(52),
    COMBAT_PIT_COUNT(53),
    TORTURE_CHAMBER_COUNT(54),
    SKELETON_COUNT(55),
    MAX_SKELETON_COUNT(56),
    PRISON_STATUS(57),
    PRISON_COUNT(58),
    VAMPIRE_COUNT(59),
    MAX_VAMPIRE_COUNT(60),
    GRAVEYARD_COUNT(61),
    TEMPLE_COUNT(62),
    CASINO_COUNT(63),
    ROOM_NAME(64),
    ROOM_USER_COUNT(65),
    MANA_GAIN(66),
    GOLD(67),
    ENTITY_NAME(68),
    WORKSHOP_ITEM_NUMBER(69),
    WORKSHOP_QUEUE_SIZE(70),
    TRAP_TRIGGER_STATE(71),
    STATE(72),
    ENTITY_GOLD(73),
    CREATURE_EFFICIENCY(74),
    TREASURY_GOLD(75),
    TREASURY_MAX_GOLD(76),
    LIBRARY_RESEARCHER_COUNT(77),
    LIBRARY_MAX_RESEARCHER_COUNT(78),
    WORKSHOP_USED_CAPACITY(79),
    WORKSHOP_MAX_CAPACITY(80),
    GUARD_ROOM_GUARD_COUNT(81),
    GUARD_ROOM_MAX_GUARD_COUNT(82),
    PRISONER_COUNT(83),
    MAX_PRISONER_COUNT(84),
    PRISON_COUNT_ALTERNATIVE(85),
    GRAVEYARD_MAX_VAMPIRE_COUNT(86),
    CASINO_PAYOUT(87),
    CREATURE_LEVEL(88),
    BONUS_PACK_NUMBER(89),
    CRYPT_STATUS(90);

    private static final Map<Integer, TextParameter> BY_ID;

    static {
        Map<Integer, TextParameter> parameters = new HashMap<>();
        for (TextParameter parameter : values()) {
            TextParameter previous = parameters.put(parameter.id, parameter);
            if (previous != null) {
                throw new IllegalStateException("Duplicate text parameter ID " + parameter.id);
            }
        }
        BY_ID = Collections.unmodifiableMap(parameters);
    }

    private final int id;

    TextParameter(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public static Optional<TextParameter> fromId(int id) {
        return Optional.ofNullable(BY_ID.get(id));
    }
}
