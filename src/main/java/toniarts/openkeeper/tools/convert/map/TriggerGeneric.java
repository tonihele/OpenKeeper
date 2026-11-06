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
        /**
         * Event: this creature receives an accepted hand slap. Each accepted
         * request is separate, even if several requests refresh the same slap
         * effect before a simulation tick. It is not a persistent test for the
         * creature's temporary slap animation or work-efficiency bonus.
         * <p>
         * DK II emits creature event 5 on its normal slap paths before the
         * companion player event 60. OpenKeeper queues accepted creature hand
         * slaps and dispatches them on the server simulation thread. Conditions
         * observe delivery-time state rather than synchronous native call state.
         * Object slaps and rejected hand interactions do not enqueue this event.
         * Existing hand admission and
         * slap mechanics are retained; this does not reproduce every native
         * special-state or Horny slap path.
         * <p>
         * Matching creature-root branches run at delivery time, with nested
         * conditions checked then. Ordinary polling stays false; nested events
         * are not independently dispatched. Existing repeat counters still apply.
         * <p>
         * Example: a scripted creature reacts with a message on its first slap.
         * A repeating branch can react to later slaps too, without firing every
         * tick while the efficiency bonus remains active.
         */
        CREATURE_SLAPPED(5),
        CREATURE_ATTACKED(6), // Event, This creature is
        /**
         * Event: this creature is admitted to prison. Remaining imprisoned is
         * not a repeated event, and manually setting a prison-state component
         * does not count as an admission. Merely carrying a prisoner also does
         * not emit: the transfer must reach its admission step.
         * <p>
         * DK II emits after room admission succeeds, while subsequent captive
         * owner/state bookkeeping can still be pending. OpenKeeper instead queues
         * completed admissions and checks nested conditions at simulation delivery
         * time, after its admission state has been stored. These timings differ.
         * <p>
         * Producers cover carried-prisoner admission, hand drops accepted into
         * prison, and living creatures placed in prison at spawn/load. The queue
         * exists before world loading so initial admissions survive until their
         * script controls exist. Existing admission/capacity rules are retained.
         * <p>
         * Matching creature-root branches run at delivery time; ordinary polling
         * stays false and nested events are not independently dispatched. Existing
         * repeat limits apply, so a later admission can trigger a repeating branch.
         * <p>
         * Example: an Imp finishes delivering a scripted hero to prison, allowing
         * a message or objective branch to run once. That branch does not fire
         * every tick while the hero remains locked up.
         */
        CREATURE_IMPRISONED(7),
        CREATURE_TORTURED(8), // Event, This creature is
        CREATURE_CONVERTED(9), // Event, This creature is
        CREATURE_CLAIMED(10), // Event, This creature is
        /**
         * Fires when a positive mood change moves this creature into the fully
         * angry state. Merely becoming unhappy does not fire it, and staying
         * angry does not repeatedly fire it. After recovery, becoming angry
         * again can fire another event, subject to the script's repeat limits.
         * <p>
         * This event runs its child actions and conditions immediately; it is
         * not a persistent test of the creature's current mood. Emotionless
         * creatures do not generate it. For example, it can warn the player
         * when a particular Warlock becomes fully angry for the first time.
         */
        CREATURE_ANGRY(11),
        /**
         * Fires when this creature passes a qualifying fear evaluation, before
         * the game attempts an escape. It is an immediate creature-script
         * event, not a poll of the current fleeing state. The event can occur
         * even when escape is subsequently suppressed or unavailable.
         * <p>
         * For example, a weak creature confronted by an overwhelming enemy
         * can trigger a warning or reinforcement script. Level4 contains
         * two uses of this event. An active escape suppresses further events;
         * later eligible fear checks can deliver it again, subject to the
         * branch's repeat limit.
         * <p>
         * DKII dispatches matching active root branches synchronously and
         * tests their child conditions at event time. The ordinary script
         * walk does not match this enum, and nested event conditions are not
         * searched recursively. OpenKeeper uses its existing fear evaluation;
         * this condition does not replace fear scoring or flee navigation.
         */
        CREATURE_AFRAID(12),
        CREATURE_STEALS(13), // Event, This creature is
        CREATURE_LEAVES(14), // Event, This creature is
        /**
         * Event: an explicit stun/knockdown command is accepted for this creature.
         * DK II emits after accepting the command, including repeated accepted
         * knockdowns while already down. A rejected command emits nothing.
         * This is not a persistent test for an AI state named "stunned".
         * <p>
         * OpenKeeper emits from its accepted knockDown() path, used by Lightning,
         * after storing the FALLEN state and restarting its timer. Rejected
         * knockdowns (including no AI, dead, unconscious, resting, falling or
         * in-hand creatures) do not enqueue the event.
         * Initial states, hand-drop landings and ordinary get-up transitions do
         * not emit merely because their stored state is FALLEN or STUNNED.
         * <p>
         * Server simulation drains the event queue and runs matching creature-root
         * branches, checking nested conditions against delivery-time state.
         * Ordinary polling stays false; nested events are not independently
         * dispatched. Existing knockdown eligibility, recovery and repeat limits
         * are retained; queued delivery is not DK II's synchronous call timing.
         * <p>
         * Example: a scripted hero struck by Lightning can advance a branch when
         * knocked down; another accepted hit can emit again while it is down.
         * <p>
         * TODO: validate additional stun causes and their native callers before
         * wiring them, including whether the separate stun-imps special should
         * emit. Its passive STUNNED state is not currently an event producer.
         */
        CREATURE_STUNNED(15),
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
        PLAYER_CREATURE_PICKED_UP(58), // Event, This player's...
        PLAYER_CREATURE_DROPPED(59), // Event, This player's...
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
