/*
 * Copyright (C) 2014-2017 OpenKeeper
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
package toniarts.openkeeper.game.controller;

import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import toniarts.openkeeper.game.component.CreatureEntity;
import toniarts.openkeeper.game.component.CreatureViewState;
import toniarts.openkeeper.game.component.Gold;
import toniarts.openkeeper.game.component.Health;
import toniarts.openkeeper.game.component.Interaction;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.component.Senses;
import toniarts.openkeeper.game.component.Trigger;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.KwdFile;
import toniarts.openkeeper.tools.convert.map.Player;
import toniarts.openkeeper.tools.convert.map.Thing;
import toniarts.openkeeper.tools.convert.map.Variable;
import toniarts.openkeeper.utils.Utils;
import toniarts.openkeeper.utils.WorldUtils;
import toniarts.openkeeper.world.MapLoader;
import toniarts.openkeeper.world.creature.Party;

/**
 * This is a controller that controls all the game objects in the world TODO:
 * Hmm, should this be more a factory/loader maybe, or if this offers the
 * ability to load / save, then it is fine
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class CreaturesController {

    private KwdFile kwdFile;
    private EntityData entityData;
    private Map<Variable.MiscVariable.MiscType, Variable.MiscVariable> gameSettings;
    private Map<Integer, Party> creatureParties = new HashMap<>();

    private static final Logger logger = Logger.getLogger(CreaturesController.class.getName());

    public CreaturesController() {
        // For serialization
    }

    /**
     * Load creatures from a KWD file straight (new game)
     *
     * @param kwdFile the KWD file
     * @param entityData the entity controller
     * @param gameSettings the game settings
     */
    public CreaturesController(KwdFile kwdFile, EntityData entityData, Map<Variable.MiscVariable.MiscType, Variable.MiscVariable> gameSettings) {
        this.kwdFile = kwdFile;
        this.entityData = entityData;
        this.gameSettings = gameSettings;

        // Load creatures
        loadCreatures();
    }

    private void loadCreatures() {
        for (toniarts.openkeeper.tools.convert.map.Thing obj : kwdFile.getThings()) {
            try {
                if (obj instanceof Thing.Creature) {
                    Thing.Creature creature = (Thing.Creature) obj;
                    spawnCreature(creature, new Vector2f(creature.getPosX(), creature.getPosY()));
                } else if (obj instanceof Thing.HeroParty) {
                    Thing.HeroParty partyThing = (Thing.HeroParty) obj;
                    Party party = new Party(partyThing);
//                    if (partyThing.getTriggerId() != 0) {
//                        partyTriggerState.addParty(partyThing.getTriggerId(), party);
//                    }
                    creatureParties.put(party.getId(), party);
                }
            } catch (Exception ex) {
                logger.log(Level.WARNING, "Could not load Thing.", ex);
            }
        }
    }

    /**
     * Spawn a creature
     *
     * @param creature creature data
     * @param position the position to spawn to, may be {@code null}
     * @return the actual spawned creature
     */
    public EntityId spawnCreature(Thing.Creature creature, Vector2f position) {
        Integer triggerId = null;
        short ownerId = 0;
        Integer healthPercentage = null;
        short level = 1;
        if (creature instanceof Thing.GoodCreature) {
            triggerId = ((Thing.GoodCreature) creature).getTriggerId();
            healthPercentage = ((Thing.GoodCreature) creature).getInitialHealth();
            level = ((Thing.GoodCreature) creature).getLevel();
            ownerId = Player.GOOD_PLAYER_ID;
        } else if (creature instanceof Thing.NeutralCreature) {
            triggerId = ((Thing.NeutralCreature) creature).getTriggerId();
            healthPercentage = ((Thing.NeutralCreature) creature).getInitialHealth();
            level = ((Thing.NeutralCreature) creature).getLevel();
            ownerId = Player.NEUTRAL_PLAYER_ID;
        } else if (creature instanceof Thing.KeeperCreature) {
            triggerId = ((Thing.KeeperCreature) creature).getTriggerId();
            healthPercentage = ((Thing.KeeperCreature) creature).getInitialHealth();
            level = ((Thing.KeeperCreature) creature).getLevel();
            ownerId = ((Thing.KeeperCreature) creature).getPlayerId();
        }
        Vector3f v = WorldUtils.pointToVector3f(creature.getPosX(), creature.getPosY());
        return loadCreature(creature.getCreatureId(), ownerId, level, v.getX(), v.getZ(), 0f, healthPercentage, creature.getGoldHeld(), triggerId, false);
    }

    /**
     * Spawn a creature
     *
     * @param creatureId the creature ID to generate
     * @param playerId the owner
     * @param level the creature level
     * @param position the position to spawn to, may be {@code null}
     * @param entrance whether this an enrance for the creature (coming out of a
     * portal)
     * @return the actual spawned creature
     */
    public EntityId spawnCreature(short creatureId, short playerId, short level, Vector2f position, boolean entrance) {
        return loadCreature(creatureId, playerId, level, position.x, position.y, 0, 100, 0, null, entrance);
    }

    private EntityId loadCreature(short creatureId, short ownerId, short level, float x, float y, float rotation, Integer healthPercentage, int money, Integer triggerId, boolean entrance) {
        Creature creature = kwdFile.getCreature(creatureId);
        EntityId entity = entityData.createEntity();

        // Create health, unless dead body
        Health healthComponent = healthPercentage != null ? new Health(0, healthPercentage, 100) : null;

        Gold goldComponent = new Gold(money, 0);
        Senses sensesComponent = healthComponent != null ? new Senses(creature.getAttributes().getDistanceCanHear(), creature.getAttributes().getDistanceCanSee()) : null;

        // The creature itself
        CreatureEntity creatureComponent = new toniarts.openkeeper.game.component.CreatureEntity();
        creatureComponent.name = Utils.generateCreatureName();
        creatureComponent.bloodType = Utils.generateBloodType();
        creatureComponent.level = level;
        creatureComponent.creatureId = creatureId;

        // Set every attribute by the level of the created creature
        setAttributesByLevel(creatureComponent, healthComponent, goldComponent, sensesComponent);

        entityData.setComponent(entity, creatureComponent);
        entityData.setComponent(entity, healthComponent);
        entityData.setComponent(entity, new Owner(ownerId));

        // Position
        // FIXME: no floor height
        entityData.setComponent(entity, new Position(rotation, new Vector3f(x, MapLoader.FLOOR_HEIGHT, y)));

        // Trigger
        if (triggerId != null) {
            entityData.setComponent(entity, new Trigger(triggerId));
        }

        // Add some interaction properties
        if (creature.getFlags().contains(Creature.CreatureFlag.CAN_BE_SLAPPED) || creature.getFlags().contains(Creature.CreatureFlag.CAN_BE_PICKED_UP)) {
            entityData.setComponent(entity, new Interaction(true, creature.getFlags().contains(Creature.CreatureFlag.CAN_BE_SLAPPED), creature.getFlags().contains(Creature.CreatureFlag.CAN_BE_PICKED_UP), false));
        }

        // Visuals
        entityData.setComponent(entity, new CreatureViewState(creatureId, entrance ? Creature.AnimationType.ENTRANCE : Creature.AnimationType.IDLE_1));

        return entity;
    }

    private void setAttributesByLevel(CreatureEntity creatureComponent, Health healthComponent, Gold goldComponent, Senses sensesComponent) {
        Creature creature = kwdFile.getCreature(creatureComponent.creatureId);
        Map<Variable.CreatureStats.StatType, Variable.CreatureStats> stats = kwdFile.getCreatureStats(creatureComponent.level);
        Creature.Attributes attributes = creature.getAttributes();
        creatureComponent.height = attributes.getHeight() * ((stats != null ? stats.get(Variable.CreatureStats.StatType.HEIGHT_TILES).getValue() : 100) / 100);
        if (healthComponent != null) {
            int prevMaxHealth = healthComponent.maxHealth;
            healthComponent.maxHealth = attributes.getHp() * ((stats != null ? stats.get(Variable.CreatureStats.StatType.HEALTH).getValue() : 100) / 100);
            healthComponent.health += Math.abs(healthComponent.maxHealth - prevMaxHealth); // Abs for some weird level configs :)
            healthComponent.ownLandHealthIncrease = attributes.getOwnLandHealthIncrease() * ((stats != null ? stats.get(Variable.CreatureStats.StatType.OWN_LAND_HEALTH_INCREASE_PER_SECOND).getValue() : 100) / 100);
        }
        creatureComponent.fear = attributes.getFear() * ((stats != null ? stats.get(Variable.CreatureStats.StatType.FEAR).getValue() : 100) / 100);
        creatureComponent.threat = attributes.getThreat() * ((stats != null ? stats.get(Variable.CreatureStats.StatType.THREAT).getValue() : 100) / 100);
        creatureComponent.pay = attributes.getPay() * ((stats != null ? stats.get(Variable.CreatureStats.StatType.PAY).getValue() : 100) / 100);
        goldComponent.maxGold = attributes.getMaxGoldHeld() * ((stats != null ? stats.get(Variable.CreatureStats.StatType.MAX_GOLD_HELD).getValue() : 100) / 100);
        creatureComponent.hungerFill = attributes.getHungerFill() * ((stats != null ? stats.get(Variable.CreatureStats.StatType.HUNGER_FILL_CHICKENS).getValue() : 100) / 100);
        creatureComponent.manaGenPrayer = attributes.getManaGenPrayer() * ((stats != null ? stats.get(Variable.CreatureStats.StatType.MANA_GENERATED_BY_PRAYER_PER_SECOND).getValue() : 100) / 100);
        creatureComponent.experienceToNextLevel = attributes.getExpForNextLevel() * ((stats != null ? stats.get(Variable.CreatureStats.StatType.EXPERIENCE_POINTS_FOR_NEXT_LEVEL).getValue() : 100) / 100);
        creatureComponent.experiencePerSecond = attributes.getExpPerSecond() * ((stats != null ? stats.get(Variable.CreatureStats.StatType.EXPERIENCE_POINTS_PER_SECOND).getValue() : 100) / 100);
        creatureComponent.experiencePerSecondTraining = attributes.getExpPerSecondTraining() * ((stats != null ? stats.get(Variable.CreatureStats.StatType.EXPERIENCE_POINTS_FROM_TRAINING_PER_SECOND).getValue() : 100) / 100);
        creatureComponent.researchPerSecond = attributes.getResearchPerSecond() * ((stats != null ? stats.get(Variable.CreatureStats.StatType.RESEARCH_POINTS_PER_SECOND).getValue() : 100) / 100);
        creatureComponent.manufacturePerSecond = attributes.getManufacturePerSecond() * ((stats != null ? stats.get(Variable.CreatureStats.StatType.MANUFACTURE_POINTS_PER_SECOND).getValue() : 100) / 100);
        creatureComponent.decomposeValue = attributes.getDecomposeValue() * ((stats != null ? stats.get(Variable.CreatureStats.StatType.DECOMPOSE_VALUE).getValue() : 100) / 100);
        creatureComponent.speed = attributes.getSpeed() * ((stats != null ? stats.get(Variable.CreatureStats.StatType.SPEED_TILES_PER_SECOND).getValue() : 100) / 100);
        creatureComponent.runSpeed = attributes.getRunSpeed() * ((stats != null ? stats.get(Variable.CreatureStats.StatType.RUN_SPEED_TILES_PER_SECOND).getValue() : 100) / 100);
        creatureComponent.tortureTimeToConvert = attributes.getTortureTimeToConvert() * ((stats != null ? stats.get(Variable.CreatureStats.StatType.TORTURE_TIME_TO_CONVERT_SECONDS).getValue() : 100) / 100);
        creatureComponent.posessionManaCost = attributes.getPossessionManaCost() * ((stats != null ? stats.get(Variable.CreatureStats.StatType.POSSESSION_MANA_COST_PER_SECOND).getValue() : 100) / 100);
        if (sensesComponent != null) {
            sensesComponent.distanceCanHear = attributes.getDistanceCanHear() * ((stats != null ? stats.get(Variable.CreatureStats.StatType.DISTANCE_CAN_HEAR_TILES).getValue() : 100) / 100);
        }
        // TODO initialGoldHeld = attributes.getInitialGoldHeld() * ((stats != null ? stats.get(Variable.CreatureStats.StatType.INITIAL_GOLD_HELD).getValue() : 100) / 100);
        creatureComponent.meleeDamage = creature.getMeleeDamage() * ((stats != null ? stats.get(Variable.CreatureStats.StatType.MELEE_DAMAGE).getValue() : 100) / 100);
        creatureComponent.meleeRecharge = creature.getMeleeRecharge() * ((stats != null ? stats.get(Variable.CreatureStats.StatType.MELEE_RECHARGE_TIME_SECONDS).getValue() : 100) / 100);

        // FIXME: We should know when we run and when we walk and set the speed
        // Steering
        //setMaxLinearSpeed(speed);
    }

    public EntityData getEntityData() {
        return entityData;
    }

}
