/*
 * Copyright (C) 2014-2018 OpenKeeper
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
package toniarts.openkeeper.view.text;

import com.simsilica.es.Entity;
import com.simsilica.es.EntityComponent;
import java.util.Collection;
import toniarts.openkeeper.utils.TextParameter;
import toniarts.openkeeper.game.component.CreatureAi;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.CreatureEfficiency;
import toniarts.openkeeper.game.component.CreatureMood;
import toniarts.openkeeper.game.component.Objective;
import toniarts.openkeeper.game.component.StolenGold;
import toniarts.openkeeper.game.component.TaskComponent;
import toniarts.openkeeper.game.map.IMapInformation;
import toniarts.openkeeper.game.map.IMapTileInformation;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.GameLevel;
import toniarts.openkeeper.utils.Utils;

/**
 * Parses text where the entity is based on a Creature data object
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public final class CreatureTextParser extends EntityTextParser<Creature> {

    private static final short LORD_OF_THE_LAND = 29;

    private final IMapInformation mapInformation;
    private GameLevel.TextTable level = GameLevel.TextTable.NONE;

    public CreatureTextParser(IMapInformation mapInformation) {
        super();

        this.mapInformation = mapInformation;
    }

    @Override
    protected String getReplacement(TextParameter parameter, Entity entity, Creature creature) {
        switch (parameter) {
            case CREATURE_INSTANCE_NAME:

                // The Lord of the Land is named after the campaign level (0x48D880)
                if (creature.getCreatureId() == LORD_OF_THE_LAND) {
                    return Utils.getMainTextResourceBundle().getString(getLordOfTheLandNameId(level));
                }

                // The unique creatures have a name of their own (0x48D880)
                if (creature.getUniqueNameTextId() != 0) {
                    return Utils.getMainTextResourceBundle().getString(Integer.toString(creature.getUniqueNameTextId()));
                }
                CreatureComponent creatureComponent = entity.get(CreatureComponent.class);
                if (creatureComponent != null) {
                    return creatureComponent.name;
                }
                return "";
            case CREATURE_NAME:
                return Utils.getMainTextResourceBundle().getString(Integer.toString(creature.getNameStringId()));
            case CREATURE_ACTIVITY:
                CreatureAi creatureAi = entity.get(CreatureAi.class);
                if (creatureAi != null) {
                    return getStatusText(entity, creatureAi, mapInformation);
                }
                return "";
            case CREATURE_STATUS:
                // FIXME
                return "";
            case CREATURE_MOOD:
                CreatureMood creatureMood = entity.get(CreatureMood.class);
                if (creatureMood != null) {
                    int unhappyThreshold = CreatureMood.toRuntimeThreshold(
                            creature.getAttributes().getUnhappyThreshold());
                    String textId = switch (creatureMood.getState(unhappyThreshold)) {
                        case 0 -> "2340"; // Content
                        case 1 -> "2342"; // Discontent
                        case 2 -> "2344"; // Angry
                        default -> throw new IllegalStateException("Unknown creature mood state");
                    };
                    return Utils.getMainTextResourceBundle().getString(textId);
                }
                return "";
            case CREATURE_EFFICIENCY:
                CreatureEfficiency creatureEfficiency = entity.get(CreatureEfficiency.class);
                if (creatureEfficiency != null) {
                    return Integer.toString(creatureEfficiency.efficiencyPercentage);
                }
                return "";
            case CREATURE_BLOOD_TYPE:
                CreatureComponent bloodTypeComponent = entity.get(CreatureComponent.class);
                return bloodTypeComponent != null ? bloodTypeComponent.bloodType : "";
            case CREATURE_PAY:

                // The pay of the creature's level, like the next payday pays it (0x48D340)
                CreatureComponent payComponent = entity.get(CreatureComponent.class);
                return payComponent != null ? Integer.toString(payComponent.pay) : "";
            case CREATURE_GOLD:
                return super.getReplacement(TextParameter.ENTITY_GOLD, entity, creature);
            default:
                return super.getReplacement(parameter, entity, creature);
        }
    }

    private static String getStatusText(Entity entity, CreatureAi creatureAi,
            IMapInformation mapInformation) {
        switch (creatureAi.getCreatureState()) {
            case IDLE: {
                return Utils.getMainTextResourceBundle().getString("2599");
            }
            case WORK: {
                TaskComponent taskComponent = entity.get(TaskComponent.class);
                if (taskComponent != null) {
                    return getTaskTooltip(entity, taskComponent, mapInformation);
                }
            }
            case WANDER: {
                return Utils.getMainTextResourceBundle().getString("2628");
            }
            case DEAD: {
                return Utils.getMainTextResourceBundle().getString("2598");
            }
            case FLEE: {
                return Utils.getMainTextResourceBundle().getString("2658");
            }
            case MELEE_ATTACK:
            case CAST_SPELL:
            case FIGHT: {
                return Utils.getMainTextResourceBundle().getString("2787");
            }
            case DRAGGED:
            case UNCONSCIOUS: {
                return Utils.getMainTextResourceBundle().getString("2655");
            }
            case STUNNED: {
                return Utils.getMainTextResourceBundle().getString("2597");
            }
            case FOLLOW: {

                // A hero following its party leader is "advancing" (states 0xDE and 0xDF, 0x461F00, 0x462130)
                return Utils.getMainTextResourceBundle().getString("2651");
            }
            case CALL_TO_ARMS: {
                return Utils.getMainTextResourceBundle().getString("2648");
            }
            case IMPRISONED: {
                return Utils.getMainTextResourceBundle().getString("2674");
            }
            case TORTURED: {
                return Utils.getMainTextResourceBundle().getString("2635");
            }
            case SLEEPING: {
                return Utils.getMainTextResourceBundle().getString("2672");
            }
            case RECUPERATING:
            case HERO_RECUPERATING: {
                return Utils.getMainTextResourceBundle().getString("2667");
            }
            case EATING: {
                return Utils.getMainTextResourceBundle().getString("2630");
            }
        }

        return "";
    }

    private static String getTaskTooltip(Entity entity, TaskComponent taskComponent, IMapInformation mapInformation) {
        switch (taskComponent.taskType) {
            case CLAIM_LAIR -> {
                return Utils.getMainTextResourceBundle().getString("2627");
            }
            case CAPTURE_ENEMY_CREATURE -> {
                return Utils.getMainTextResourceBundle().getString("2620");
            }
            case CARRY_CREATURE_TO_JAIL -> {
                return Utils.getMainTextResourceBundle().getString("2620");
            }
            case CARRY_CREATURE_TO_LAIR -> {
                return Utils.getMainTextResourceBundle().getString("2619");
            }
            case CARRY_GOLD_TO_TREASURY -> {
                return Utils.getMainTextResourceBundle().getString("2786");
            }
            case CARRY_OBJECT_TO_STORAGE -> {
                return Utils.getMainTextResourceBundle().getString("2609"); // TODO: these are really awfully specific, we need to fine grain this
            }
            case CLAIM_ROOM -> {
                return Utils.getMainTextResourceBundle().getString("2602");
            }
            case CLAIM_TILE -> {
                return Utils.getMainTextResourceBundle().getString("2601");
            }
            case CLAIM_WALL -> {
                return Utils.getMainTextResourceBundle().getString("2603");
            }
            case DIG_TILE -> {
                IMapTileInformation tile = mapInformation.getMapData().getTile(taskComponent.targetLocation);
                return Utils.getMainTextResourceBundle().getString(tile.getGold() > 0 ? "2605" : "2600");
            }
            case FETCH_OBJECT -> {
                return Utils.getMainTextResourceBundle().getString("2608");
            }
            case FETCH_CORPSE, CARRY_CORPSE_TO_GRAVEYARD -> {
                return Utils.getMainTextResourceBundle().getString("2618");
            }
            case GO_TO_LOCATION -> {
                return Utils.getMainTextResourceBundle().getString(
                        getEnRouteTextId(entity.get(Objective.class), entity.get(StolenGold.class) != null));
            }
            case GO_TO_SLEEP -> {
                return Utils.getMainTextResourceBundle().getString("2671");
            }
            case KILL_PLAYER -> {
                return Utils.getMainTextResourceBundle().getString("2645");
            }
            case REPAIR_WALL -> {
                return Utils.getMainTextResourceBundle().getString("2604");
            }
            case RESCUE_CREATURE -> {
                return Utils.getMainTextResourceBundle().getString("2617");
            }
            case RESEARCH -> {
                return Utils.getMainTextResourceBundle().getString("2625");
            }
            case MANUFACTURE -> {
                return Utils.getMainTextResourceBundle().getString("2624");
            }
            case FETCH_TRAP_CRATE -> {
                return Utils.getMainTextResourceBundle().getString("2611");
            }
            case DELIVER_TRAP_CRATE -> {
                return Utils.getMainTextResourceBundle().getString("2611");
            }
            case FETCH_DOOR_CRATE -> {
                return Utils.getMainTextResourceBundle().getString("2614");
            }
            case DELIVER_DOOR_CRATE -> {
                return Utils.getMainTextResourceBundle().getString("2614");
            }
            case GO_TO_EAT -> {
                return Utils.getMainTextResourceBundle().getString("2630");
            }
            case TRAIN -> {
                return Utils.getMainTextResourceBundle().getString("2633");
            }
            case PRAY -> {
                return Utils.getMainTextResourceBundle().getString("2632");
            }
            case SULK -> {
                return Utils.getMainTextResourceBundle().getString("2673");
            }
            case LEAVE -> {
                return Utils.getMainTextResourceBundle().getString("2649");
            }
            case REBEL -> {
                return Utils.getMainTextResourceBundle().getString("2599");
            }
            case GUARD -> {
                return Utils.getMainTextResourceBundle().getString("2661");
            }
            case GAMBLE, COLLECT_WINNINGS -> {
                return Utils.getMainTextResourceBundle().getString("2640");
            }
            case TORTURE -> {
                return Utils.getMainTextResourceBundle().getString("2634");
            }
            case COLLECT_PAY -> {
                return Utils.getMainTextResourceBundle().getString("2669");
            }
            default -> {
                return "";
            }
        }
    }

    /**
     * Gets the job text of a creature that is going somewhere. DK II shows the
     * job it is heading to, not "en route": the state table marks the walking
     * states to show the stored destination state (0x478090). Going somewhere
     * here is a hero objective, and DK II heads to "attacking Dungeon Heart"
     * when killing the keeper or the keeper's creatures (states 0xB1 and 0xB2,
     * 0x462310, 0x462490), to "stealing booty" or "escaping with stolen booty"
     * when stealing gold (0x481FA0) and to an "idle" state at an action point
     * (0x471E80)
     *
     * @param objective the objective of the creature, may be {@code null}
     * @param stolenGold whether the creature has the gold it stole
     * @return the text ID
     */
    static String getEnRouteTextId(Objective objective, boolean stolenGold) {
        if (objective == null || objective.objective == null) {
            return "2670";
        }
        return switch (objective.objective) {
            case KILL_PLAYER, KILL_CREATURES ->
                "2645";
            case STEAL_GOLD ->
                stolenGold ? "2643" : "2642";
            case SEND_TO_ACTION_POINT ->
                "2599";
            default ->
                "2670";
        };
    }

    /**
     * Gets the name of the Lord of the Land. DK II gives each campaign level
     * its own lord, and all the others are Michael (0x48D880)
     *
     * @param level the text table of the level, it is the campaign level
     * number DK II uses
     * @return the text ID
     */
    static String getLordOfTheLandNameId(GameLevel.TextTable level) {
        if (level == null) {
            return "3079";
        }
        return switch (level) {
            case LEVEL_1 ->
                "2866";
            case LEVEL_2 ->
                "2867";
            case LEVEL_3 ->
                "2868";
            case LEVEL_4 ->
                "2869";
            case LEVEL_5 ->
                "2870";
            case LEVEL_6A, LEVEL_6B ->
                "2871";
            case LEVEL_7 ->
                "2872";
            case LEVEL_8 ->
                "2873";
            case LEVEL_9 ->
                "2874";
            case LEVEL_10 ->
                "2875";
            case LEVEL_12 ->
                "2876";
            case LEVEL_14 ->
                "2877";
            case LEVEL_15A, LEVEL_15B ->
                "2878";
            case LEVEL_16 ->
                "2879";
            default ->
                "3079";
        };
    }

    /**
     * Sets the level that is played. The Lord of the Land is named after it
     *
     * @param level the text table of the level
     */
    public void setLevel(GameLevel.TextTable level) {
        this.level = level;
    }

    @Override
    public Collection<Class<? extends EntityComponent>> getWatchedComponents() {
        Collection<Class<? extends EntityComponent>> components = super.getWatchedComponents();

        components.add(TaskComponent.class);
        components.add(CreatureComponent.class);
        components.add(CreatureAi.class);
        components.add(CreatureMood.class);
        components.add(CreatureEfficiency.class);
        components.add(Objective.class);
        components.add(StolenGold.class);

        return components;
    }

}
