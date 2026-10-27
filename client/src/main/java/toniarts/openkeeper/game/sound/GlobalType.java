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
 *
 * @author archdemon
 */
public enum GlobalType implements IIndexable {

    OBJECT_RESEARCH_1(49), // LIBRARY_BOOK
    OBJECT_RESEARCH_2(50), // LIBRARY_BOOK
    OBJECT_RESEARCH_3(51), // LIBRARY_BOOK
    OBJECT_RESEARCH_4(52), // LIBRARY_BOOK

    OBJECT_SPECIAL_1(127), // Global\SpecialsHD\Box_wob5.mp2
    OBJECT_SPECIAL_2(128), // Global\SpecialsHD\Box_wob5.mp2
    OBJECT_SPECIAL_3(129), // Global\SpecialsHD\Box_wob5.mp2
    OBJECT_SPECIAL_4(130), // Global\SpecialsHD\Rescreat_spec2.mp2
    OBJECT_SPECIAL_5(131), // Global\SpecialsHD\Box_wob5.mp2
    OBJECT_SPECIAL_6(132), // Global\SpecialsHD\Box_wob5.mp2
    OBJECT_SPECIAL_7(133), // Global\SpecialsHD\Box_wob5.mp2
    OBJECT_SPECIAL_8(134), // Global\SpecialsHD\Box_wob5.mp2
    OBJECT_SPECIAL_9(135), // Global\SpecialsHD\Box_wob5.mp2
    OBJECT_SPECIAL_10(136), // Global\SpecialsHD\Box_wob5.mp2
    OBJECT_SPECIAL_11(137), // Global\SpecialsHD\Box_wob5.mp2
    OBJECT_SPECIAL_12(138), // Global\SpecialsHD\Box_wob5.mp2
    OBJECT_SPECIAL_13(139), // Global\SpecialsHD\Box_wob5.mp2
    OBJECT_SPECIAL_14(140), // Global\SpecialsHD\Box_wob5.mp2
    OBJECT_SPECIAL_15(141), // Global\SpecialsHD\Box_wob5.mp2
    OBJECT_SPECIAL_16(142), // Global\SpecialsHD\Killcreat_spec.mp2
    OBJECT_SPECIAL_17(143), // Global\SpecialsHD\Box_wob5.mp2

    OBJECT_PICKUP(195),
    OBJECT_DROP(196),
    OBJECT_CREATE(241), // chicken create, room create
    OBJECT_LOOP(242), // or bat fly or rat
    OBJECT_DESTROY(244),
    OBJECT_TORTURE_WHEEL(251), // OBJECT_ELECTRIC_CHAIR Global\TortureRoomHD\elec_chair_1.mp2
    OBJECT_CHICKEN_FLAP_1(272),
    OBJECT_CHICKEN_FLAP_2(273),
    OBJECT_FORGE_HIT(274),
    OBJECT_AMBIENCE_LOOP(746), // OBJECT_FE_WINDOW Global\FrontEndHD\gemfe1.mp2
    OBJECT_TRAIN_MECH(785),
    OBJECT_TORTURE_FIRE(795),
    OBJECT_ROULETTE_WHEEL(804), // ???

    DOOR_OPEN(231), // or prison door
    DOOR_CLOSE(232), // or prison door
    DOOR_LOCK(233),
    DOOR_UNLOCK(234),
    DOOR_MANUFACTURED(271), // door or trap

    TRAP_DESTROY(244), // Global\TrapsHD\Bldr_hitwall2.mp2
    TRAP_MANUFACTURED(271), // door or trap
    TRAP_HIT(276), // maybe wall
    TRAP_HIT_WALL(329), // boulder
    TRAP_ATTACK(235),
    TRAP_HIT_WOOD_DOOR(330), // boulder
    TRAP_INTO_WATER(331), // boulder
    TRAP_INTO_LAVA(332), // boulder
    TRAP_HIT_CREATURE(333), // boulder
    TRAP_KILL_CREATURE(334), // boulder
    TRAP_ROLL(335), // boulder
    TRAP_HIT_DOOR(346), // boulder
    TRAP_HIT_2(443), // maybe wall

    KEEPER_SPELL_OUT(232), // end spell action
    KEEPER_SPELL_IN(239),
    KEEPER_SPELL_LOOP(251),
    KEEPER_SPELL_RUBBLE(748),
    ROOM_DUNGEON_HEART_ATTACK(218),
    ROOM_PRISON_OPEN(231),
    ROOM_PRISON_CLOSE(232),
    ROOM_PRISON_LOCK(233),
    ROOM_PRISON_UNLOCK(234),
    ROOM_CREATE(241), // Global\SetPiecesHD\Heart form Alph.mp2
    ROOM_DUNGEON_HEART_DESTROY(244),
    ROOM_SELL(245),
    ROOM_DUNGEON_HEART_HEART(336),
    ROOM_WORKSHOP_BIGAIR(744),
    ROOM_WORKSHOP_STEAMY(745),
    ROOM_AMBIENCE_LOOP(746), // Global\LibraryHD\nunstart3.mp2, libnun3.mp2, nunend3.mp2
    ROOM_AMBIENCE_LOOP_2(747), // Global\LibraryHD\book12.mp2, libbw6.mp2, Global\WorkshopHD\boiler1.mp2
    ROOM_LAY_TILE(760),
    ROOM_DUNGEON_HEART_HEARTSTORM(761),
    ROOM_LIBRARY_PAPER(765),
    ROOM_DUNGEON_HEART_HEART_2(767),
    ROOM_DUNGEON_HEART_HEART_3(768),
    ROOM_CASINO_DISCO_INFERNO(790),
    ROOM_CASINO_NEEDLE_ON_RECOR(830),
    ROOM_DUNGEON_HEART_DAMAGE(809), // when dungeon heart damage?
    ROOM_PORTAL_UNIQUE(843), // ???

    SPELL_LOOP(242),
    SPELL_DESTROY(244),
    SPELL_IN(239),
    SPELL_HIT(276),
    SPELL_FIREBALL(277),
    SPELL_SKELETON_ARMY(298),
    SPELL_GRENADE_BOUNCE(340),
    SPELL_HIT_2(443), // ???

    TERRAIN_DESTROY(244), // terrain and object // Global\TerrainHD\keep70.mp2
    TERRAIN_REINFORCE(268), // Global\GuiHD\dk1reinf.mp2
    TERRAIN_AMBIENCE_LOOP(746), // Global\AmbienceHD\water_lp1.mp2  // terrain or room. E.g. water wave sound
    TERRAIN_GENSPELL(793), // Global\CharacterSpellsHD\dk1_genspell_1.mp2

    CREATURE_PICKUP(195), // creature or object
    CREATURE_DROP(196), // creature or object
    CREATURE_SLAPPED(197),
    CREATURE_STUNNED_DROPPED(199),
    CREATURE_TICKLED(200),
    CREATURE_IN_HAND(201),
    CREATURE_HAPPY(202),
    CREATURE_UNHAPPY(203),
    CREATURE_ANGRY(204),
    CREATURE_EAT(205),
    CREATURE_SLEEP(206),
    CREATURE_TORTURE_GENERAL(207), // FIXME maybe not. CREATURE_TORTURE_WHEEL
    CREATURE_IMPRISON(208),
    CREATURE_RESEARCH(209),
    CREATURE_TRAIN(211),
    CREATURE_PRAY(212),
    CREATURE_GAMBLE(213),
    CREATURE_CHARGE(214), // also Global\DarkAngelHD\da_rune2.mp2
    CREATURE_SPELL_ATTACK(216),
    CREATURE_MAGIC_ATTACK(217),
    CREATURE_DIE(218),
    CREATURE_HIT_OR_MELEE(219),
    CREATURE_FLEE(220),
    CREATURE_MELEE(221),
    CREATURE_SULK(222),
    CREATURE_INTRO(224),
    CREATURE_DRINK(225),
    CREATURE_SACRIFICE(226),
    CREATURE_HELLO(228),
    CREATURE_FALL(230),
    CREATURE_FALL_DOOR(246), // sound like door is locked. heavylump, land
    CREATURE_KEEP_1(265),
    CREATURE_CLAIM_1(266),
    CREATURE_CLAIM_2(267),
    CREATURE_REPAIR(268), // creature or terrain // Global\TerrainHD\Repair_1.mp2
    CREATURE_DRAG(269),
    CREATURE_TORTURE_WHEEL(342), // FIXME maybe not. CREATURE_TORTURE_GENERAL
    CREATURE_REPORT(762),
    CREATURE_CHAT(786),
    CREATURE_FT1(788),
    CREATURE_FT2(789),
    CREATURE_TORTURE_FIRE(795), // creature or object
    CREATURE_TORTURE_WATER(796),
    CREATURE_TRAPS_OR_CRATES_1(806),
    CREATURE_DOORS_WOOD(807),
    CREATURE_TRAPS_OR_CRATES_2(808),
    CREATURE_FT_3(818), // genfoot | maifoot
    CREATURE_FT_4(819), // genfoot | maifoot
    CREATURE_KEEP_2(820),
    CREATURE_IMPENETRABLE(821),
    CREATURE_STINKY(822),
    CREATURE_WATER_SWIM(823), // WADING
    CREATURE_FT_IN_WATER_SMA(824),
    CREATURE_REINFORCE(829), // Global\GuiHD\dk1reinf.mp2
    CREATURE_TORCH_LOOP(832),
    CREATURE_TELEPORT_IN(834),
    CREATURE_TELEPORT_OUT(835),
    CREATURE_LEVEL_UP(841),
    CREATURE_JACKINBOX_FIREWORKS(844),
    CREATURE_JACKINBOX(845), // ???

    // for category HAND
    HAND_SLAP(197),
    HAND_TAG(236),
    HAND_NICE_WAHBONGDIGI(840),
    // for category AMBIENCE
    AMBIENCE(341),
    /* for category:
        GUI_BUTTON_DEFAULT
        GUI_BUTTON_ICON_LARGE
        GUI_BUTTON_INFO
        GUI_BUTTON_OPTIONS
        GUI_BUTTON_REAPER_TALISMAN
        GUI_BUTTON_ZOOM
        GUI_BUTTON_ICON_LARGE
        GUI_BUTTON_ICON
        GUI_BUTTON_SIZE
        GUI_BUTTON_TAB_CREATURE
        GUI_BUTTON_TAB_NEW_COMBAT
        GUI_BUTTON_TAB_ROOMS
        GUI_BUTTON_TAB_SPELLS
        GUI_BUTTON_TAB_WORKSHOP
        GUI_MINIMAP
        GUI_SELL
     */
    GUI_BUTTON_CKICK(260),
    // for category GUI_BUTTON_SIZE
    GUI_BUTTON_SIZE_MAX(307),
    GUI_BUTTON_SIZE_NORMIN(308),
    GUI_BUTTON_SIZE_HIDE(309),
    /*
        GUI_BUTTON_ICON_LARGE
        GUI_BUTTON_ICON
        GUI_BUTTON_TAB_WORKSHOP
        GUI_BUTTON_TAB_SPELLS
        GUI_BUTTON_TAB_ROOMS
        GUI_BUTTON_TAB_CREATURE
     */
    GUI_BUTTON_FLASH(826),
    /*
        GUI_BUTTON_TAB_WORKSHOP
        GUI_BUTTON_TAB_SPELLS
        GUI_BUTTON_TAB_ROOMS
        GUI_BUTTON_TAB_CREATURE
        GUI_BUTTON_TAB_NEW_COMBAT
     */
    GUI_BUTTON_SPINNING(812),
    /*
        GUI_BUTTON_TAB_WORKSHOP // FIXME exception because point on null file in sdt archive
        GUI_BUTTON_TAB_SPELLS
        GUI_BUTTON_TAB_ROOMS
        GUI_BUTTON_TAB_CREATURE
        GUI_BUTTON_TAB_NEW_COMBAT
     */
    GUI_BUTTON_TABSLAB(811),
    /*
        GUI_BUTTON_TAB_WORKSHOP
        GUI_BUTTON_TAB_SPELLS
        GUI_BUTTON_TAB_ROOMS
        GUI_BUTTON_TAB_CREATURE
        GUI_BUTTON_TAB_NEW_COMBAT
     */
    GUI_BUTTON_REVEAL(312),
    // for category GUI_BUTTON_TAB_NEW_COMBAT
    GUI_BUTTON_COMBATALARM(783),
    // for category FRONT_END
    FRONT_END_CKICK(778), // Global\GuiHD\FE3D CLICK ON T.mp2,
    FRONT_END_GLOW(783), // Global\FrontEndHD\GLOW 3 FX.mp2,

    // for category EFFECTS
    EFFECTS_PUFF(827),
    EFFECTS_FIREWORKS(836),
    // for category MUSIC
    MUSIC_TRACK_1(343),
    MUSIC_TRACK_3(345),
    // for category GUI_MESSAGE_BAR
    GUI_MESSAGE_BAR_END(800), // Global\GuiHD\FE3D TAB END 1.mp2
    GUI_MESSAGE_BAR_SLIDE(801), // Global\GuiHD\FE3D TAB SLIDE.mp2
    GUI_MESSAGE_BAR_END_2(802), // Global\GuiHD\FE3D TAB END 3.mp2

    // for category MULTI_PLAYER
    MULTI_PLAYER(842), // Global\Patch_1HD\QUIT DUNGEON FX.mp2

    // for category ONE_SHOT_ATMOS
    ONE_SHOT_ATMOS(746),
    // for category OPTIONS_MUSIC
    OPTIONS_MUSIC(838),
    // for category OPTIONS_SPEECH
    OPTIONS_SPEECH(837),
    // for category OPTIONS
    OPTIONS_CLICK_1(772), // Global\GuiHD\OPTION CLICK NL.mp2
    OPTIONS_CLICK_2(773), // Global\GuiHD\OPTION CLICK NL.mp2
    OPTIONS_CLICK_3(774), // Global\GuiHD\OPTION CLICK NL.mp2
    OPTIONS_CLICK_4(775), // Global\GuiHD\OPTION CLICK NL.mp2
    OPTIONS_CLICK_5(776), // Global\GuiHD\OPTION CLICK NL.mp2
    OPTIONS_CLICK_6(777), // Global\GuiHD\OPTION CLICK NL.mp2
    OPTIONS_CLICK_7(778), // Global\GuiHD\OPTION CLICK NL.mp2
    OPTIONS_CLICK_8(779), // Global\GuiHD\OPTION CLICK NL.mp2
    OPTIONS_CLICK_9(780), // Global\GuiHD\OPTION CLICK NL.mp2
    OPTIONS_CLICK_10(781), // Global\GuiHD\OPTION CLICK NL.mp2
    OPTIONS_CLICK_11(782), // Global\GuiHD\OPTION CLICK NL.mp2
    OPTIONS_CLICK_12(783), // Global\GuiHD\OPTION CLICK NL.mp2
    OPTIONS_NICEWAHBONGDIGI(828), // Global\GuiHD\nicewahbongdigi.mp2
    OPTIONS_NICEWAHBONGDIGI_2(833); // Global\GuiHD\nicewahbongdigi.mp2

    private GlobalType(int value) {
        this.value = (short) value;
    }

    private final short value;

    @Override
    public short getId() {
        return value;
    }
}
