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

import toniarts.openkeeper.game.event.GameEvent;
import toniarts.openkeeper.game.event.GameEvent.ThingFlag;
import toniarts.openkeeper.game.event.GameEventType;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.Door;
import toniarts.openkeeper.tools.convert.map.GameObject;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.KeeperSpell;
import toniarts.openkeeper.tools.convert.map.Material;
import toniarts.openkeeper.tools.convert.map.Shot;
import toniarts.openkeeper.tools.convert.map.Trap;
import toniarts.openkeeper.utils.Point;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * What a client hears of a {@link GameEvent}: the sound category and the sound event are picked here, from the
 * thing types of the event, the game logic knows nothing about them.
 */
public final class GameEventSounds {

    private GameEventSounds() {
        // Static helper
    }

    /**
     * A sound to play
     *
     * @param category the sound category
     * @param eventId the sound event
     * @param position the tile the sound comes from, {@code null} if the sound is not positional
     */
    public record Sound(String category, int eventId, @Nullable Point position) {

    }

    /**
     * The event is not restarted while the previous one of the same entity and type still plays
     */
    public static boolean isExclusive(GameEventType type) {
        return type == GameEventType.CREATURE_HURT;
    }

    /**
     * @return the sounds the event makes, in the order they are played, empty if it makes none
     */
    public static List<Sound> getSounds(GameEvent event, IKwdFile kwdFile) {
        List<Sound> sounds = new ArrayList<>(2);
        String subject = getCategory(kwdFile, event.subjectKind, event.subjectId);
        switch (event.type) {
            case CREATURE_LEVEL_UP ->
                add(sounds, subject, ThingSounds.CREATURE_LEVEL_UP, event.position);
            case CREATURE_LAND ->
                add(sounds, subject, ThingSounds.CREATURE_LAND, event.position);
            case CREATURE_HURT ->
                add(sounds, subject, ThingSounds.CREATURE_HIT, event.position);
            case CREATURE_DIE ->
                add(sounds, subject, ThingSounds.CREATURE_DIE, event.position);
            case CREATURE_CLAIM_TILE ->
                add(sounds, subject, ThingSounds.CREATURE_CLAIM, event.position);
            case CREATURE_REINFORCE_WALL ->
                add(sounds, subject, ThingSounds.CREATURE_REINFORCE, event.position);
            case MELEE_HIT ->
                addMeleeHit(sounds, event, kwdFile);
            case PICKED_UP ->
                add(sounds, subject, ThingSounds.PICK_UP, event.position);
            case DROPPED ->
                add(sounds, subject, ThingSounds.DROP, event.position);
            case SLAPPED -> {
                add(sounds, subject, ThingSounds.SLAP, event.position);

                // The slapped object makes a sound of its own, e.g. a chicken squawks
                if (event.subjectKind == ThingFlag.OBJECT) {
                    add(sounds, subject, ThingSounds.OBJECT_SLAPPED, event.position);
                }
            }
            case HAND_SLAP ->
                add(sounds, GlobalCategory.HAND, ThingSounds.SLAP, null);
            case TILES_TAGGED ->
                add(sounds, GlobalCategory.HAND, ThingSounds.TAG, null);
            case SHOT_LAUNCH ->
                add(sounds, subject, ThingSounds.SHOT_LAUNCH, event.position);
            case SHOT_HIT ->
                addShotHit(sounds, event, kwdFile, subject);
            case IMPS_CREATED ->
                add(sounds, ThingSounds.getCreateImpCategory(kwdFile), ThingSounds.SHOT_LAUNCH, event.position);
            case ROOM_TILES_LAID ->
                add(sounds, subject, ThingSounds.ROOM_LAY_TILE, event.position);
            case ROOM_TILE_BUILT ->
                add(sounds, subject, ThingSounds.OBJECT_CREATE, event.position);
            case ROOM_SOLD ->
                add(sounds, subject, ThingSounds.ROOM_SELL, event.position);
            case TERRAIN_DESTROYED ->
                add(sounds, subject, ThingSounds.DESTROYED, event.position);
        }
        return sounds;
    }

    /**
     * The impact is heard at the attacker, followed by the attacker's grunt.
     */
    private static void addMeleeHit(List<Sound> sounds, GameEvent event, IKwdFile kwdFile) {
        Creature attacker = event.subjectKind == ThingFlag.CREATURE ? kwdFile.getCreature((short) event.subjectId) : null;
        if (attacker == null) {
            return;
        }
        String weapon = CombatSounds.getWeaponCategory(attacker.getMeleeAttackType());
        if (weapon == null) {
            return; // No sound at all, not even the grunt
        }
        int impact = CombatSounds.getMeleeImpactEvent(getMaterial(kwdFile, event.targetKind, event.targetId));
        if (impact >= 0) {
            add(sounds, weapon, impact, event.position);
        }
        add(sounds, attacker.getSoundCategory(), ThingSounds.CREATURE_HIT, event.position);
    }

    /**
     * The impact of a shot on a creature, only for the shots that recoil the victim.
     */
    private static void addShotHit(List<Sound> sounds, GameEvent event, IKwdFile kwdFile, @Nullable String category) {
        Shot shot = event.subjectKind == ThingFlag.SHOT ? kwdFile.getShotById((short) event.subjectId) : null;
        if (shot == null || !shot.getFlags().contains(Shot.ShotFlag.RECOIL) || event.targetKind != ThingFlag.CREATURE) {
            return;
        }
        add(sounds, category, CombatSounds.getShotImpactEvent(getMaterial(kwdFile, event.targetKind, event.targetId)),
                event.position);
    }

    private static void add(List<Sound> sounds, @Nullable String category, int eventId, @Nullable Point position) {
        if (category != null && !category.isEmpty()) {
            sounds.add(new Sound(category, eventId, position));
        }
    }

    /**
     * @return the sound category of the thing type, {@code null} if it has none
     */
    @Nullable
    private static String getCategory(IKwdFile kwdFile, @Nullable ThingFlag kind, int id) {
        if (kind == null) {
            return null;
        }
        return switch (kind) {
            case CREATURE -> {
                Creature creature = kwdFile.getCreature((short) id);
                yield creature != null ? creature.getSoundCategory() : null;
            }
            case OBJECT -> {
                GameObject object = kwdFile.getObject(id);
                yield object != null ? object.getSoundCategory() : null;
            }
            case ROOM -> {
                var room = kwdFile.getRoomById(id);
                yield room != null ? room.getSoundCategory() : null;
            }
            case TERRAIN -> {
                var terrain = kwdFile.getTerrain((short) id);
                yield terrain != null ? terrain.getSoundCategory() : null;
            }
            case SHOT -> {
                Shot shot = kwdFile.getShotById((short) id);
                yield shot != null ? shot.getSoundCategory() : null;
            }
            case KEEPER_SPELL -> {
                KeeperSpell keeperSpell = kwdFile.getKeeperSpellById(id);
                yield keeperSpell != null ? keeperSpell.getSoundCategory() : null;
            }
            default ->
                null;
        };
    }

    /**
     * @return the material of a creature, door, trap or object type, {@code null} if there is none
     */
    @Nullable
    private static Material getMaterial(IKwdFile kwdFile, @Nullable ThingFlag kind, int id) {
        if (kind == null) {
            return null;
        }
        return switch (kind) {
            case CREATURE -> {
                Creature creature = kwdFile.getCreature((short) id);
                yield creature != null ? creature.getMaterial() : null;
            }
            case DOOR -> {
                Door door = kwdFile.getDoorById(id);
                yield door != null ? door.getMaterial() : null;
            }
            case TRAP -> {
                Trap trap = kwdFile.getTrapById(id);
                yield trap != null ? trap.getMaterial() : null;
            }
            case OBJECT -> {
                GameObject object = kwdFile.getObject(id);
                yield object != null ? object.getMaterial() : null;
            }
            default ->
                null;
        };
    }
}
