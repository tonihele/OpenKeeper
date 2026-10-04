/*
 * Copyright (C) 2014-2025 OpenKeeper
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

import com.jme3.math.Vector3f;
import javax.annotation.Nullable;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.WorldUtils;

/**
 * Sounds of the things that come from the map data (creatures, objects, doors,
 * traps, shots, rooms, terrain and spells): the event ids the game code plays
 * on the sound category of the thing type.
 */
public final class ThingSounds {

    // Hand
    public static final int PICK_UP = 0xC3;
    public static final int DROP = 0xC4;
    public static final int SLAP = 0xC5;

    // Creatures, see the creature instance table
    public static final int CREATURE_HAPPY = 0xCA;
    public static final int CREATURE_ANGRY = 0xCC;
    public static final int CREATURE_EAT = 0xCD;
    public static final int CREATURE_SLEEP = 0xCE;
    public static final int CREATURE_RESEARCH = 0xD1;
    public static final int CREATURE_MANUFACTURE = 0xD2;
    public static final int CREATURE_TRAIN = 0xD3;
    public static final int CREATURE_PRAY = 0xD4;
    public static final int CREATURE_MELEE = 0xD7;
    public static final int CREATURE_CAST = 0xD8;
    public static final int CREATURE_HIT = 0xDB;
    public static final int CREATURE_SULK = 0xDE;
    public static final int CREATURE_ENTRANCE = 0xE0;
    public static final int CREATURE_DRINK = 0xE1;
    public static final int CREATURE_FALL = 0xE6;
    public static final int CREATURE_DIG = 0x334;

    // Doors
    public static final int DOOR_OPEN = 0xE7;
    public static final int DOOR_CLOSE = 0xE8;
    public static final int DOOR_LOCK = 0xE9;
    public static final int DOOR_UNLOCK = 0xEA;

    // Traps
    public static final int TRAP_TRIGGER = 0xEB;

    // Shots, spells
    public static final int SHOT_LAUNCH = 0xEF;
    public static final int SHOT_FLIGHT = 0xF2;

    // Objects, rooms, terrain
    public static final int OBJECT_CREATE = 0xF1;
    public static final int OBJECT_LOOP = 0xF2;
    public static final int DESTROYED = 0xF4;
    public static final int ROOM_EVENT = 0xF5;
    public static final int MANUFACTURED = 0x10F;

    private ThingSounds() {
        // Nope
    }

    /**
     * Plays a sound of a thing type at a world position.
     *
     * @return the sound handle, {@link SoundEngine#NO_HANDLE} if nothing was played
     */
    public static int play(@Nullable String category, int eventId, Vector3f position) {
        return play(category, eventId, WorldUtils.vectorToPoint(position));
    }

    /**
     * Plays a sound of a thing type at a tile.
     *
     * @return the sound handle, {@link SoundEngine#NO_HANDLE} if nothing was played
     */
    public static int play(@Nullable String category, int eventId, Point position) {
        if (category == null || category.isEmpty()) {
            return SoundEngine.NO_HANDLE;
        }
        return SoundManager.playSound(category, eventId, position);
    }

    /**
     * @return the sound event that is played when a creature starts the
     * animation, {@code -1} if the animation has no sound
     */
    public static int getCreatureEvent(@Nullable Creature.AnimationType animation) {
        if (animation == null) {
            return -1;
        }
        return switch (animation) {
            case DIG ->
                CREATURE_DIG;
            case MELEE_ATTACK, SWIPE ->
                CREATURE_MELEE;
            case CAST_SPELL ->
                CREATURE_CAST;
            case SLEEPING ->
                CREATURE_SLEEP;
            case EATING ->
                CREATURE_EAT;
            case PRAYING ->
                CREATURE_PRAY;
            case ENTRANCE ->
                CREATURE_ENTRANCE;
            case RESEARCHING ->
                CREATURE_RESEARCH;
            case MANUFACTURING ->
                CREATURE_MANUFACTURE;
            case TRAINING ->
                CREATURE_TRAIN;
            case HAPPY ->
                CREATURE_HAPPY;
            case ANGRY ->
                CREATURE_ANGRY;
            case DRINKING ->
                CREATURE_DRINK;
            case RECOIL_FORWARDS, RECOIL_BACKWARDS ->
                CREATURE_HIT;
            case FALLBACK ->
                CREATURE_FALL;
            case IN_HAND ->
                PICK_UP;
            default ->
                -1;
        };
    }

    /**
     * Plays the sound of a creature that starts an animation.
     */
    public static int playCreature(Creature creature, Creature.AnimationType animation, Vector3f position) {
        int event = getCreatureEvent(animation);
        if (event < 0) {
            return SoundEngine.NO_HANDLE;
        }
        return play(creature.getSoundCategory(), event, position);
    }
}
