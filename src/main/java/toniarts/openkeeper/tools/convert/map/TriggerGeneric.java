/*
 * Copyright (C) 2014-2015 OpenKeeper
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

import java.util.Map.Entry;
import toniarts.openkeeper.tools.convert.IValueEnum;

/**
 * This the actual trigger, all TriggerActions need to be owned (triggered) by
 * these
 *
 * @see TriggerAction
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public final class TriggerGeneric extends Trigger {

    public enum ComparisonType implements IValueEnum {

        NONE(0, null),
        LESS_THAN(1, "<"),
        LESS_OR_EQUAL_TO(2, "\u2264"),
        EQUAL_TO(3, "="),
        GREATER_THAN(4, ">"),
        GREATER_OR_EQUAL_TO(5, "\u2265"),
        NOT_EQUAL_TO(6, "!=");

        private ComparisonType(int id, String description) {
            this.id = id;
            this.description = description;
        }

        @Override
        public int getValue() {
            return id;
        }

        @Override
        public String toString() {
            return description;
        }
        private final int id;
        private final String description;
    }

    public enum TargetType implements IValueEnum {

        NONE(0),
        FLAG(1),
        TIMER(2),
        CREATURE_CREATED(3), // Event, This creature is
        CREATURE_KILLED(4), // Event, This creature is
        CREATURE_SLAPPED(5), // Event, This creature is
        CREATURE_ATTACKED(6), // Event, This creature is
        CREATURE_IMPRISONED(7), // Event, This creature is
        CREATURE_TORTURED(8), // Event, This creature is
        CREATURE_CONVERTED(9), // Event, This creature is
        CREATURE_CLAIMED(10), // Event, This creature is
        CREATURE_ANGRY(11), // Event, This creature is
        CREATURE_AFRAID(12), // Event, This creature is
        CREATURE_STEALS(13), // Event, This creature is
        CREATURE_LEAVES(14), // Event, This creature is
        CREATURE_STUNNED(15), // Event, This creature is
        CREATURE_DYING(16), // Event, This creature is
        DOOR_DESTROYED(17), // Event, This door is
        OBJECT_CLAIMED(18), // Event, This object is
        PLAYER_CREATURES(19), // Player
        PLAYER_HAPPY_CREATURES(20), // Player
        PLAYER_ANGRY_CREATURES(21), // Player
        PLAYER_CREATURES_KILLED(22), // Player
        PLAYER_KILLS_CREATURES(23), // Player
        PLAYER_ROOM_SLABS(24), // Player
        PLAYER_ROOMS(25), // Player
        PLAYER_ROOM_SIZE(26), // Player
        PLAYER_DOORS(27), // Player
        PLAYER_TRAPS(28), // Player
        PLAYER_KEEPER_SPELL(29), // Player
        PLAYER_GOLD(30), // Player
        PLAYER_GOLD_MINED(31), // Player
        PLAYER_MANA(32), // Player
        PLAYER_DESTROYS(33), // Player
        LEVEL_TIME(34), // Attribute, This level's...
        LEVEL_CREATURES(35), // Attribute, This level's...
        CREATURE_HEALTH(36), // Attribute, This creature's...
        CREATURE_GOLD_HELD(37), // Attribute, This creature's...
        AP_CONGREGATE_IN(39), // Action point
        AP_CLAIM_PART_OF(40), // Action point
        AP_CLAIM_ALL_OF(41), // Action point
        AP_SLAB_TYPES(42), // Action point
        PARTY_CREATED(43), // Party
        PARTY_MEMBERS_KILLED(44), // Party
        PARTY_MEMBERS_CAPTURED(45), // Party
        LEVEL_PAY_DAY(46), // Event, This level's...
        PLAYER_KILLED(47), // Event, This player's...
        CREATURE_EXPERIENCE_LEVEL(49), // Attribute, This creature's...
        PLAYER_CREATURES_AT_LEVEL(52), // Player
        GUI_BUTTON_PRESSED(53), // GUI
        CREATURE_HUNGER_SATED(54), // Event, This creature is
        CREATURE_PICKS_UP_PORTAL_GEM(55), // Event, This creature is
        PLAYER_DUNGEON_BREACHED(56), // Event, This player's...
        PLAYER_ENEMY_BREACHED(57), // Event, This player's...
        /**
         * Event: this player completes a keeper-hand creature pickup. The picking
         * player receives it, including when the creature has a different original
         * owner. Objects and activated specials are excluded. Current full-hand,
         * ownership, interaction and incapacitation guards still decide admission;
         * rejected requests and already-held clicks do not emit.
         * <p>
         * DK II emits player event 58 after its pickup transition, before the
         * creature's separate pickup event. Its player dispatcher ignores authored
         * creature/room operands for this event, even nonzero or mismatching IDs.
         * It is not a persistent historical test and is not generic creature hauling.
         * <p>
         * OpenKeeper queues the picking player ID after hand insertion and world
         * cleanup. Only simulation ticks dispatch matching player-root branches;
         * nested conditions observe delivery-time state, polling stays false and
         * existing repeats apply. Pickup/drop arrival order is preserved. Native
         * delivery is synchronous; exact timing, upstream native eligibility and
         * finite-repeat parity are not established by this implementation.
         * <p>
         * Example: picking up a captured hero runs the captor's tutorial/objective
         * branch once. Keeping the hero in hand does not repeat it; dropping and
         * successfully picking up the hero again can produce another event.
         */
        PLAYER_CREATURE_PICKED_UP(58),
        /**
         * Fires when this player releases a creature from the hand or when one
         * of this player's carriers releases a hauled creature. The dropping
         * player receives the event, even when the creature belongs to an enemy.
         * Objects do not generate this creature event.
         * <p>
         * This is an event, not a persistent test that any drop has happened.
         * Matching root branches run their children together at delivery time;
         * nested conditions and repeat limits still apply. DKII ignores the
         * authored creature-type and room-type filters for this event.
         * For example, a one-shot branch can explain creature drops after the
         * player's first drop, without firing again on every following tick.
         */
        PLAYER_CREATURE_DROPPED(59),
        PLAYER_CREATURE_SLAPPED(60), // Event, This player's...
        PLAYER_CREATURE_SACKED(61), // Event, This player's...
        AP_TAG_PART_OF(62), // Action point
        CREATURE_SACKED(63), // Event, This creature is
        PARTY_MEMBERS_INCAPACITATED(64), // Party
        CREATURE_PICKED_UP(65), // Event, This creature is
        LEVEL_PLAYED(67), // Attribute, This level's...
        PLAYER_ROOM_FURNITURE(68), // Player
        AP_TAG_ALL_OF(69), // Action point
        AP_POSESSED_CREATURE_ENTERS(70), // Action point
        PLAYER_SLAPS(71), // Player
        GUI_TRANSITION_ENDS(72), // GUI
        PLAYER_CREATURES_GROUPED(73), // Player
        PLAYER_CREATURES_DYING(74);

        // Player
        private TargetType(int id) {
            this.id = id;
        }

        @Override
        public int getValue() {
            return id;
        }

        @Override
        public String toString() {
            String[] splitted = name().split("_");
            String result = "";
            for (String s : splitted) {
                result = result.concat(" ").concat(s.substring(0, 1).toUpperCase()).concat(s.substring(1).toLowerCase());
            }
            return result.trim();
        }
        private final int id;
    }

    public enum TargetValueType {

        VALUE,
        TERRAIN, // Slab types point to terrain ID
        DOOR,
        TRAP,
        KEEPER_SPELL,
        ROOM,
        CREATURE,
        BUTTON,
        FLAG,
        TIMER;
    }
    //    struct TriggerBlock {
    //        int x00;
    //        int x04;
    //        uint16_t x08;
    //        uint16_t x0a;
    //        uint16_t x0c;
    //        uint8_t x0e;
    //        uint8_t x0f;
    //        };
    private ComparisonType targetValueComparison; // Target comparison type
    private int targetValue; // Target value
    private TargetType target;

    public TriggerGeneric(IKwdFile kwdFile) {
        super(kwdFile);
    }

    public ComparisonType getTargetValueComparison() {
        return targetValueComparison;
    }

    protected void setTargetValueComparison(ComparisonType targetValueComparison) {
        this.targetValueComparison = targetValueComparison;
    }

    public int getTargetValue() {
        return targetValue;
    }

    protected void setTargetValue(int targetValue) {
        this.targetValue = targetValue;
    }

    public TargetType getType() {
        return target;
    }

    protected void setType(TargetType target) {
        this.target = target;
    }

    @Override
    public String toString() {
        String result = "When " + target;
        switch (target) {
            case FLAG:
            case TIMER:
                result += " " + ((Short) getUserData("targetId") + 1);
                break;
        }

        if (targetValueComparison != null) {
            result += " " + targetValueComparison;
        }

        result += " " + getUserData("value");

        if (data != null) {
            for (Entry<String, Number> entry : data.entrySet()) {
                String key = entry.getKey();
                Number value = entry.getValue();

                switch (key) {
                    case "playerId":
                        result += " [ " + (((Short) value == 0) ? "Any" : (kwdFile.getPlayer((short) ((Short) value)).getName())) + " ]";
                        break;

                    case "creatureId":
                        result += " [ " + (((Short) value == 0) ? "Any" : (kwdFile.getCreature((short) ((Short) value)).getName())) + " ]";
                        break;

                    case "roomId":
                        result += " [ " + (((Short) value == 0) ? "Any" : (kwdFile.getRoomById((short) ((Short) value)).getName())) + " ]";
                        break;

                    case "terrainId":
                        result += " [ " + (((Short) value == 0) ? "Any" : (kwdFile.getTerrain((short) ((Short) value)).getName())) + " ]";
                        //if (targetFlagId > 0 && terrain != null && terrain.getFlags().contains(Terrain.TerrainFlag.OWNABLE)) {
                        //    result += " [ " + kwdFile.getPlayer(targetFlagId) + " ]";
                        //}
                        break;
                }
            }
        }

        return result;
    }
}
