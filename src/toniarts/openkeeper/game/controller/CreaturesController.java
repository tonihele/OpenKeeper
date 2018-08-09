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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import toniarts.openkeeper.game.component.CreatureAi;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.CreatureViewState;
import toniarts.openkeeper.game.component.Gold;
import toniarts.openkeeper.game.component.Health;
import toniarts.openkeeper.game.component.Interaction;
import toniarts.openkeeper.game.component.Mobile;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Party;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.component.Senses;
import toniarts.openkeeper.game.component.Trigger;
import toniarts.openkeeper.game.controller.creature.CreatureController;
import toniarts.openkeeper.game.controller.creature.CreatureState;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.controller.creature.IPartyController;
import toniarts.openkeeper.game.controller.creature.PartyController;
import toniarts.openkeeper.game.controller.creature.PartyType;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.KwdFile;
import toniarts.openkeeper.tools.convert.map.Player;
import toniarts.openkeeper.tools.convert.map.Thing;
import toniarts.openkeeper.tools.convert.map.Variable;
import toniarts.openkeeper.utils.Utils;
import toniarts.openkeeper.world.MapLoader;

/**
 * This is a controller that controls all the game objects in the world TODO:
 * Hmm, should this be more a factory/loader maybe, or if this offers the
 * ability to load / save, then it is fine
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class CreaturesController implements ICreaturesController {

    private final KwdFile kwdFile;
    private final EntityData entityData;
    private final Map<Variable.MiscVariable.MiscType, Variable.MiscVariable> gameSettings;
    private final Map<Short, IPartyController> creaturePartiesByPartyId = new HashMap<>();
    /**
     * These are the actual living parties on the map by the generated party ID
     */
    private final Map<Long, IPartyController> creaturePartiesById = new HashMap<>();
    /**
     * These are the map defined parties
     */
    private final Map<Short, Thing.HeroParty> heroParties = new HashMap<>();
    private final IGameTimer gameTimer;
    private final IGameController gameController;

    private static final Logger LOGGER = Logger.getLogger(CreaturesController.class.getName());

    /**
     * Load creatures from a KWD file straight (new game)
     *
     * @param kwdFile the KWD file
     * @param entityData the entity controller
     * @param gameSettings the game settings
     * @param gameTimer
     * @param gameController
     */
    public CreaturesController(KwdFile kwdFile, EntityData entityData, Map<Variable.MiscVariable.MiscType, Variable.MiscVariable> gameSettings, IGameTimer gameTimer,
            IGameController gameController) {
        this.kwdFile = kwdFile;
        this.entityData = entityData;
        this.gameSettings = gameSettings;
        this.gameTimer = gameTimer;
        this.gameController = gameController;

        // Load creatures
        loadCreatures();
    }

    private void loadCreatures() {
        for (Thing.GoodCreature creature : kwdFile.getThings(Thing.GoodCreature.class)) {
            try {
                spawnCreature(creature, new Vector2f(creature.getPosX(), creature.getPosY()));
            } catch (Exception ex) {
                LOGGER.log(Level.WARNING, "Could not load Thing " + creature + "!", ex);
            }
        }
        for (Thing.NeutralCreature creature : kwdFile.getThings(Thing.NeutralCreature.class)) {
            try {
                spawnCreature(creature, new Vector2f(creature.getPosX(), creature.getPosY()));
            } catch (Exception ex) {
                LOGGER.log(Level.WARNING, "Could not load Thing " + creature + "!", ex);
            }
        }
        for (Thing.KeeperCreature creature : kwdFile.getThings(Thing.KeeperCreature.class)) {
            try {
                spawnCreature(creature, new Vector2f(creature.getPosX(), creature.getPosY()));
            } catch (Exception ex) {
                LOGGER.log(Level.WARNING, "Could not load Thing " + creature + "!", ex);
            }
        }
        for (Thing.DeadBody creature : kwdFile.getThings(Thing.DeadBody.class)) {
            try {
                spawnCreature(creature, new Vector2f(creature.getPosX(), creature.getPosY()));
            } catch (Exception ex) {
                LOGGER.log(Level.WARNING, "Could not load Thing " + creature + "!", ex);
            }
        }
        for (Thing.HeroParty heroParty : kwdFile.getThings(Thing.HeroParty.class)) {
            try {
                heroParties.put(heroParty.getId(), heroParty);
                creaturePartiesByPartyId.put(heroParty.getId(), new PartyController(heroParty));
            } catch (Exception ex) {
                LOGGER.log(Level.WARNING, "Could not load Thing " + heroParty + "!", ex);
            }
        }
    }

    @Override
    public EntityId spawnCreature(Thing.Creature creature, Vector2f position) {
        Integer triggerId = null;
        short ownerId = 0;
        Integer healthPercentage = null;
        short level = 1;
        if (creature instanceof Thing.GoodCreature) {
            Thing.GoodCreature goodCreature = (Thing.GoodCreature) creature;
            triggerId = goodCreature.getTriggerId();
            healthPercentage = goodCreature.getInitialHealth();
            level = goodCreature.getLevel();
            ownerId = Player.GOOD_PLAYER_ID;
        } else if (creature instanceof Thing.NeutralCreature) {
            Thing.NeutralCreature neutralCreature = (Thing.NeutralCreature) creature;
            triggerId = neutralCreature.getTriggerId();
            healthPercentage = neutralCreature.getInitialHealth();
            level = neutralCreature.getLevel();
            ownerId = Player.NEUTRAL_PLAYER_ID;
        } else if (creature instanceof Thing.KeeperCreature) {
            Thing.KeeperCreature keeperCreature = (Thing.KeeperCreature) creature;
            triggerId = keeperCreature.getTriggerId();
            healthPercentage = keeperCreature.getInitialHealth();
            level = keeperCreature.getLevel();
            ownerId = keeperCreature.getPlayerId();
        } else if (creature instanceof Thing.DeadBody) {
            Thing.DeadBody deadBody = (Thing.DeadBody) creature;
            ownerId = deadBody.getPlayerId();
        }
        return loadCreature(creature.getCreatureId(), ownerId, level, position.getX(), position.getY(), 0f, healthPercentage, creature.getGoldHeld(), triggerId != null && triggerId != 0 ? triggerId : null, false);
    }

    @Override
    public EntityId spawnCreature(short creatureId, short playerId, int level, Vector2f position, boolean entrance) {
        return loadCreature(creatureId, playerId, level, position.x, position.y, 0, 100, 0, null, entrance);
    }

    private EntityId loadCreature(short creatureId, short ownerId, int level, float x, float y, float rotation, Integer healthPercentage, int money, Integer triggerId, boolean entrance) {
        Creature creature = kwdFile.getCreature(creatureId);
        EntityId entity = entityData.createEntity();

        // Create health, unless dead body
        Health healthComponent = healthPercentage != null ? new Health(0, healthPercentage, 100) : null;

        Gold goldComponent = new Gold(money, 0);
        Senses sensesComponent = healthComponent != null ? new Senses(creature.getAttributes().getDistanceCanHear(), creature.getAttributes().getDistanceCanSee()) : null;

        // The creature itself
        CreatureComponent creatureComponent = new CreatureComponent();
        creatureComponent.name = Utils.generateCreatureName();
        creatureComponent.bloodType = Utils.generateBloodType();
        creatureComponent.level = level;
        creatureComponent.creatureId = creatureId;
        creatureComponent.worker = creature.getFlags().contains(Creature.CreatureFlag.IS_WORKER);
        creatureComponent.stunDuration = creature.getAttributes().getStunDuration();

        entityData.setComponent(entity, new CreatureAi(gameTimer.getGameTime(), entrance ? CreatureState.ENTERING_DUNGEON : CreatureState.IDLE, creatureId));

        // Set every attribute by the level of the created creature
        setAttributesByLevel(creatureComponent, healthComponent, goldComponent, sensesComponent);

        entityData.setComponent(entity, creatureComponent);
        if (healthComponent != null) {
            entityData.setComponent(entity, healthComponent);
        }
        if (sensesComponent != null) {
            entityData.setComponent(entity, sensesComponent);
        }
        entityData.setComponent(entity, new Owner(ownerId));
        entityData.setComponent(entity, goldComponent);

        // Position
        // FIXME: no floor height
        entityData.setComponent(entity, new Position(rotation, new Vector3f(x, MapLoader.FLOOR_HEIGHT, y)));

        // Mobility
        entityData.setComponent(entity, new Mobile(creature.getFlags().contains(Creature.CreatureFlag.CAN_FLY),
                creature.getFlags().contains(Creature.CreatureFlag.CAN_WALK_ON_WATER),
                creature.getFlags().contains(Creature.CreatureFlag.CAN_WALK_ON_LAVA), creatureComponent.speed));

        // Trigger
        if (triggerId != null) {
            entityData.setComponent(entity, new Trigger(triggerId));
        }

        // Add some interaction properties
        if (creature.getFlags().contains(Creature.CreatureFlag.CAN_BE_SLAPPED) || creature.getFlags().contains(Creature.CreatureFlag.CAN_BE_PICKED_UP)) {
            entityData.setComponent(entity, new Interaction(true, creature.getFlags().contains(Creature.CreatureFlag.CAN_BE_SLAPPED), creature.getFlags().contains(Creature.CreatureFlag.CAN_BE_PICKED_UP), false));
        }

        // Visuals
        entityData.setComponent(entity, new CreatureViewState(creatureId, gameTimer.getGameTime(), entrance ? Creature.AnimationType.ENTRANCE : healthComponent != null ? Creature.AnimationType.IDLE_1 : Creature.AnimationType.DEATH_POSE));

        return entity;
    }

    private void setAttributesByLevel(CreatureComponent creatureComponent, Health healthComponent, Gold goldComponent, Senses sensesComponent) {
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
        goldComponent.gold = attributes.getInitialGoldHeld() * ((stats != null ? stats.get(Variable.CreatureStats.StatType.INITIAL_GOLD_HELD).getValue() : 100) / 100);
        creatureComponent.meleeDamage = creature.getMeleeDamage() * ((stats != null ? stats.get(Variable.CreatureStats.StatType.MELEE_DAMAGE).getValue() : 100) / 100);
        creatureComponent.meleeRecharge = creature.getMeleeRecharge() * ((stats != null ? stats.get(Variable.CreatureStats.StatType.MELEE_RECHARGE_TIME_SECONDS).getValue() : 100) / 100);

        // FIXME: We should know when we run and when we walk and set the speed
        // Steering
        //setMaxLinearSpeed(speed);
    }

    @Override
    public void spawnHeroParty(short partyId, PartyType partyType, Vector2f position) {
        /**
         * In the game manual it is said that it is entirely possible to have
         * multiple parties with the same party ID, the old party just isn't
         * controlled anymore
         */
        IPartyController partyController = creaturePartiesByPartyId.get(partyId);
        if (partyController.isCreated()) {
            partyController = new PartyController(heroParties.get(partyId));
            LOGGER.log(Level.FINE, "Re-spawning party {0}!", partyId);
        }
        partyController.setType(partyType);
        for (Thing.GoodCreature creature : partyController.getMembers()) {
            EntityId entityId = spawnCreature(creature, position);
            entityData.setComponent(entityId, new Party(partyController.getId()));

            partyController.addMemberInstance(creature, createController(entityId));
        }
        partyController.create();

        creaturePartiesByPartyId.put(partyId, partyController);

        // TODO: Hmm, should we clean these up...
        creaturePartiesById.put(partyController.getId(), partyController);

        // TODO: listener, mainly for the PartyTrigger, to replace the party with new instance just in case
    }

    @Override
    public IPartyController getParty(short partyId) {
        return creaturePartiesByPartyId.get(partyId);
    }

    @Override
    public IPartyController getPartyById(long id) {
        return creaturePartiesById.get(id);
    }

    @Override
    public List<IPartyController> getParties() {
        return new ArrayList<>(creaturePartiesByPartyId.values());
    }

    @Override
    public ICreatureController createController(EntityId entityId) {
        CreatureComponent creatureComponent = entityData.getComponent(entityId, CreatureComponent.class);
        if (creatureComponent == null) {
            throw new RuntimeException("Entity " + entityId + " doesn't represent a creature!");
        }
        return new CreatureController(entityId, entityData, kwdFile.getCreature(creatureComponent.creatureId), gameController.getNavigationService(), gameController.getTaskManager(), gameTimer);
    }

    @Override
    public boolean isValidEntity(EntityId entityId) {
        return entityData.getComponent(entityId, CreatureComponent.class) != null;
    }

}
