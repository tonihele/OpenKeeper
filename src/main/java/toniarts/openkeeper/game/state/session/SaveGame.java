/*
 * Copyright (C) 2014-2026 OpenKeeper
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
package toniarts.openkeeper.game.state.session;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.InstanceCreator;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.simsilica.es.EntityComponent;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import com.simsilica.es.base.DefaultEntityIdGenerator;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.BiFunction;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import javax.annotation.Nullable;
import toniarts.openkeeper.game.component.ChickenAi;
import toniarts.openkeeper.game.component.CreatureAi;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.controller.ILevelInfo;
import toniarts.openkeeper.game.controller.IPlayerController;
import toniarts.openkeeper.game.controller.chicken.ChickenState;
import toniarts.openkeeper.game.controller.creature.CreatureState;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.controller.creature.IPartyController;
import toniarts.openkeeper.game.controller.creature.PartyController;
import toniarts.openkeeper.game.controller.creature.PartyType;
import toniarts.openkeeper.game.controller.player.PlayerStatsControl;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.game.data.CampaignLevel;
import toniarts.openkeeper.game.data.GameTimer;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.data.PlayerSpell;
import toniarts.openkeeper.game.data.ResearchableEntity;
import toniarts.openkeeper.game.trigger.TriggerControl;
import toniarts.openkeeper.tools.convert.map.Thing;

/**
 * A saved game: the level, the game time, the players and their statistics,
 * the level script variables and triggers, the hero parties, the furniture of
 * the rooms and every entity component, written as gzipped JSON. The entity
 * ids are kept as they are, so the ids inside the components stay valid.
 * Rooms, indexes, tasks and the state machines are not saved, they are built
 * again from the components and the level on load.
 */
public final class SaveGame {

    /**
     * Version of the file layout, increase it when the layout changes
     */
    public static final int VERSION = 1;

    private static final String COMPONENT_PACKAGE = Position.class.getPackageName();
    private static final Map<String, Class<? extends EntityComponent>> COMPONENT_TYPES = findComponentTypes();
    private static final Type PLAYERS_TYPE = new TypeToken<List<Keeper>>() {
    }.getType();
    private static final Type ENTITY_IDS_TYPE = new TypeToken<List<EntityId>>() {
    }.getType();
    private static final String[] SECTIONS = {"players", "playerStats", "levelInfo", "triggers", "parties", "rooms", "components"};
    private static final Gson GSON = new GsonBuilder()
            .serializeNulls()
            .serializeSpecialFloatingPointValues()
            .registerTypeAdapterFactory(new EnumNames<>(CreatureAi.class, CreatureState.class, "creatureState", "targetState"))
            .registerTypeAdapterFactory(new EnumNames<>(ChickenAi.class, ChickenState.class, "chickenState"))
            .create();

    private final JsonObject json;

    /**
     * Takes a snapshot of the game. Nothing may change the entities or the
     * players while this runs. Some systems change component values in place
     * (the position vector), so the snapshot is turned into JSON right away,
     * and the file can then be written on another thread. One entity id is
     * used up to find the next free id.
     *
     * @param level the level name
     * @param campaignLevel the campaign level, or {@code null} for
     * non-campaign
     * @param gameTime the game time in seconds
     * @param levelInfo the players and the level script variables
     * @param playerControllers the player controllers, for the player
     * statistics
     * @param triggerControls the level script triggers
     * @param parties the hero parties that have been created
     * @param rooms the rooms, for their furniture
     * @param entityData the entities
     */
    public SaveGame(String level, @Nullable CampaignLevel campaignLevel, double gameTime, ILevelInfo levelInfo,
            Collection<IPlayerController> playerControllers, Collection<TriggerControl> triggerControls,
            Collection<IPartyController> parties, Collection<IRoomController> rooms, EntityData entityData) {
        json = new JsonObject();
        json.addProperty("version", VERSION);
        json.addProperty("level", level);
        if (campaignLevel != null) {
            JsonObject campaign = new JsonObject();
            campaign.addProperty("type", campaignLevel.getType().name());
            campaign.addProperty("level", campaignLevel.getLevel());
            campaign.addProperty("variation", campaignLevel.getVariation());
            json.add("campaignLevel", campaign);
        }
        json.addProperty("gameTime", gameTime);
        json.addProperty("nextEntityId", entityData.createEntity().getId());
        json.add("players", GSON.toJsonTree(levelInfo.getPlayers().values(), PLAYERS_TYPE));

        JsonObject playerStats = new JsonObject();
        for (IPlayerController playerController : playerControllers) {
            playerStats.add(Short.toString(playerController.getKeeper().getId()), GSON.toJsonTree(playerController.getStatsControl()));
        }
        json.add("playerStats", playerStats);

        JsonObject script = new JsonObject();
        JsonArray flags = new JsonArray();
        for (int i = 0; i < ILevelInfo.FLAG_COUNT; i++) {
            flags.add(levelInfo.getFlag(i));
        }
        script.add("flags", flags);
        JsonArray timers = new JsonArray();
        for (int i = 0; i < ILevelInfo.TIMER_COUNT; i++) {
            timers.add(GSON.toJsonTree(levelInfo.getTimer(i)));
        }
        script.add("timers", timers);
        script.addProperty("timeLimit", levelInfo.getTimeLimit());
        script.addProperty("levelScore", levelInfo.getLevelScore());
        json.add("levelInfo", script);

        JsonObject triggers = new JsonObject();
        for (TriggerControl triggerControl : triggerControls) {
            triggers.add(Integer.toString(triggerControl.getTriggerId()), GSON.toJsonTree(triggerControl.getState()));
        }
        json.add("triggers", triggers);

        JsonArray savedParties = new JsonArray();
        for (IPartyController party : parties) {
            JsonObject savedParty = new JsonObject();
            savedParty.addProperty("id", party.getId());
            savedParty.addProperty("partyId", party.getPartyId());
            savedParty.add("type", GSON.toJsonTree(party.getType()));
            savedParty.add("leader", GSON.toJsonTree(party.getLeader() != null ? party.getLeader().getEntityId() : null));
            JsonArray members = new JsonArray();
            for (ICreatureController member : party.getActualMembers()) {
                members.add(GSON.toJsonTree(member != null ? member.getEntityId() : null));
            }
            savedParty.add("members", members);
            savedParties.add(savedParty);
        }
        json.add("parties", savedParties);

        // Nothing on the furniture tells which room it belongs to
        JsonObject savedRooms = new JsonObject();
        for (IRoomController room : rooms) {
            JsonObject savedRoom = new JsonObject();
            savedRoom.add("floorFurniture", GSON.toJsonTree(room.getFloorFurniture()));
            savedRoom.add("wallFurniture", GSON.toJsonTree(room.getWallFurniture()));
            savedRoom.add("pillars", GSON.toJsonTree(room.getPillars()));
            savedRooms.add(Long.toString(room.getEntityId().getId()), savedRoom);
        }
        json.add("rooms", savedRooms);

        JsonObject components = new JsonObject();
        for (Map.Entry<String, Class<? extends EntityComponent>> type : COMPONENT_TYPES.entrySet()) {
            JsonObject entities = new JsonObject();
            entityData.findEntities(null, type.getValue()).stream().sorted(Comparator.comparingLong(EntityId::getId)).forEach(entityId
                    -> entities.add(Long.toString(entityId.getId()), GSON.toJsonTree(entityData.getComponent(entityId, type.getValue()), type.getValue())));
            if (entities.size() > 0) {
                components.add(type.getKey(), entities);
            }
        }
        json.add("components", components);
    }

    private SaveGame(JsonObject json) {
        this.json = json;
    }

    /**
     * Reads a save game
     *
     * @param file the save file
     * @return the save game
     * @throws IOException if the file can't be read, is from a newer version
     * or has an unknown component
     */
    public static SaveGame read(Path file) throws IOException {
        try (Reader reader = new InputStreamReader(new GZIPInputStream(Files.newInputStream(file)), StandardCharsets.UTF_8)) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            int version = json.get("version").getAsInt();
            if (version > VERSION) {
                throw new IOException("Save game version " + version + " is newer than " + VERSION + "!");
            }
            for (String section : SECTIONS) {
                if (!json.has(section)) {
                    throw new IOException("Save game has no " + section + ", it was saved before games could be loaded!");
                }
            }
            for (String type : json.getAsJsonObject("components").keySet()) {
                if (!COMPONENT_TYPES.containsKey(type)) {
                    throw new IOException("Unknown component " + type + "!");
                }
            }

            return new SaveGame(json);
        }
    }

    /**
     * Writes the save game. The file is first written beside the target and
     * then moved over it, so a crash while saving doesn't break an earlier
     * save.
     *
     * @param file the save file
     * @throws IOException if the file can't be written
     */
    public void write(Path file) throws IOException {
        Path folder = file.toAbsolutePath().getParent();
        Files.createDirectories(folder);
        Path tempFile = folder.resolve(file.getFileName() + ".tmp");
        try (JsonWriter out = GSON.newJsonWriter(new OutputStreamWriter(new GZIPOutputStream(Files.newOutputStream(tempFile)), StandardCharsets.UTF_8))) {
            GSON.toJson(json, out);
        }
        Files.move(tempFile, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }

    /**
     * Creates new entity data with the saved entities. New entities get ids
     * after the saved ones.
     *
     * @return the entity data
     */
    public EntityData createEntityData() {
        EntityData entityData = new DefaultEntityData(new DefaultEntityIdGenerator(json.get("nextEntityId").getAsLong()));
        for (Map.Entry<String, JsonElement> type : json.getAsJsonObject("components").entrySet()) {
            Class<? extends EntityComponent> componentType = COMPONENT_TYPES.get(type.getKey());
            for (Map.Entry<String, JsonElement> entity : type.getValue().getAsJsonObject().entrySet()) {
                entityData.setComponent(new EntityId(Long.parseLong(entity.getKey())), GSON.fromJson(entity.getValue(), componentType));
            }
        }
        return entityData;
    }

    /**
     * Gets the level name
     *
     * @return the level name
     */
    public String getLevel() {
        return json.get("level").getAsString();
    }

    /**
     * Reads only the level name from the start of a save file, for the list of
     * saves
     *
     * @param file the save file
     * @return the level name
     * @throws IOException if the file can't be read or has no level
     */
    public static String readLevel(Path file) throws IOException {
        try (JsonReader reader = new JsonReader(new InputStreamReader(new GZIPInputStream(Files.newInputStream(file)), StandardCharsets.UTF_8))) {
            reader.beginObject();
            while (reader.hasNext()) {
                if ("level".equals(reader.nextName())) {
                    return reader.nextString();
                }
                reader.skipValue();
            }
        }
        throw new IOException("Save game has no level!");
    }

    /**
     * Gets the campaign level the game was saved on
     *
     * @return the campaign level, or {@code null} if it wasn't a campaign
     * level
     */
    @Nullable
    public CampaignLevel getCampaignLevel() {
        JsonObject campaign = json.getAsJsonObject("campaignLevel");
        if (campaign == null) {
            return null;
        }
        String variation = campaign.get("variation").getAsString();
        return new CampaignLevel(CampaignLevel.LevelType.valueOf(campaign.get("type").getAsString()),
                campaign.get("level").getAsInt(), variation.isEmpty() ? null : variation);
    }

    /**
     * Gets the game time
     *
     * @return the game time in seconds
     */
    public double getGameTime() {
        return json.get("gameTime").getAsDouble();
    }

    /**
     * Gets the players
     *
     * @return new copies of the players
     */
    public List<Keeper> getPlayers() {
        List<Keeper> players = GSON.fromJson(json.get("players"), PLAYERS_TYPE);
        for (Keeper player : players) {

            // The spells can be upgraded, the JSON only knows they are researchable
            player.getAvailableSpells().replaceAll(spell -> {
                PlayerSpell playerSpell = new PlayerSpell(spell.getId(), spell.isDiscovered());
                playerSpell.setUpgraded(spell.isUpgraded());
                playerSpell.setResearch(spell.getResearch());
                return playerSpell;
            });

            // The research goes on in the available entity itself
            ResearchableEntity research = player.getCurrentResearch();
            if (research != null) {
                Stream.of(player.getAvailableSpells(), player.getAvailableDoors(), player.getAvailableTraps(), player.getAvailableRooms())
                        .flatMap(List::stream)
                        .filter(entity -> entity.getResearchableType() == research.getResearchableType() && entity.getId() == research.getId())
                        .findFirst()
                        .ifPresent(player::setCurrentResearch);
            }
        }
        return players;
    }

    /**
     * Sets the player statistics (the creatures the players have slapped,
     * picked up and dropped) of newly loaded players to the saved ones
     *
     * @param playerControllers the player controllers to restore
     */
    public void restorePlayerStats(Collection<IPlayerController> playerControllers) {
        JsonObject playerStats = json.getAsJsonObject("playerStats");
        for (IPlayerController playerController : playerControllers) {
            JsonElement stats = playerStats.get(Short.toString(playerController.getKeeper().getId()));
            if (stats != null) {
                GSON.newBuilder()
                        .registerTypeAdapter(PlayerStatsControl.class, (InstanceCreator<PlayerStatsControl>) type -> playerController.getStatsControl())
                        .create()
                        .fromJson(stats, PlayerStatsControl.class);
            }
        }
    }

    /**
     * Gets the hero parties that had been created, oldest first
     *
     * @param heroParties the hero parties of the level by party ID
     * @param creatures gives the creature controller of a saved member, also
     * of one that has died and is gone
     * @return new party controllers
     */
    public List<PartyController> getParties(Map<Short, Thing.HeroParty> heroParties, BiFunction<EntityId, Thing.GoodCreature, ICreatureController> creatures) {
        List<PartyController> parties = new ArrayList<>();
        for (JsonElement element : json.getAsJsonArray("parties")) {
            JsonObject savedParty = element.getAsJsonObject();
            Thing.HeroParty heroParty = heroParties.get(savedParty.get("partyId").getAsShort());
            JsonArray savedMembers = savedParty.getAsJsonArray("members");
            List<ICreatureController> members = new ArrayList<>(savedMembers.size());
            for (int i = 0; i < savedMembers.size(); i++) {
                members.add(savedMembers.get(i).isJsonNull() ? null
                        : creatures.apply(GSON.fromJson(savedMembers.get(i), EntityId.class), heroParty.getHeroPartyMembers().get(i)));
            }
            EntityId leader = GSON.fromJson(savedParty.get("leader"), EntityId.class);

            PartyController party = new PartyController(heroParty);
            party.restore(savedParty.get("id").getAsLong(), GSON.fromJson(savedParty.get("type"), PartyType.class), members,
                    members.stream().filter(member -> member != null && member.getEntityId().equals(leader)).findFirst().orElse(null));
            parties.add(party);
        }
        parties.sort(Comparator.comparingLong(PartyController::getId));
        return parties;
    }

    /**
     * Constructs a room of the loaded game. Its furniture is already in the
     * entities, so the room takes it over instead of making new.
     *
     * @param room the room
     */
    public void restoreRoom(IRoomController room) {
        JsonObject savedRoom = json.getAsJsonObject("rooms").getAsJsonObject(Long.toString(room.getEntityId().getId()));
        if (savedRoom == null) {
            room.construct();
            return;
        }
        room.construct(GSON.fromJson(savedRoom.get("floorFurniture"), ENTITY_IDS_TYPE),
                GSON.fromJson(savedRoom.get("wallFurniture"), ENTITY_IDS_TYPE),
                GSON.fromJson(savedRoom.get("pillars"), ENTITY_IDS_TYPE));
    }

    /**
     * Sets the level script variables (flags, timers, time limit and level
     * score) of a newly loaded level to the saved values
     *
     * @param levelInfo the level info to restore
     */
    public void restoreLevelInfo(ILevelInfo levelInfo) {
        JsonObject script = json.getAsJsonObject("levelInfo");
        JsonArray flags = script.getAsJsonArray("flags");
        for (int i = 0; i < flags.size(); i++) {
            levelInfo.setFlag(i, flags.get(i).getAsInt());
        }
        JsonArray timers = script.getAsJsonArray("timers");
        for (int i = 0; i < timers.size(); i++) {
            GameTimer timer = GSON.fromJson(timers.get(i), GameTimer.class);
            levelInfo.getTimer(i).set(timer.getTime(), timer.isActive());
        }
        if (!script.get("timeLimit").isJsonNull()) {
            levelInfo.setTimeLimit(script.get("timeLimit").getAsFloat());
        }
        levelInfo.setLevelScore(script.get("levelScore").getAsInt());
    }

    /**
     * Sets the trigger trees of a newly loaded level to the saved state. The
     * trees are matched by their first trigger
     *
     * @param triggerControls the trigger controls to restore
     */
    public void restoreTriggers(Collection<TriggerControl> triggerControls) {
        JsonObject triggers = json.getAsJsonObject("triggers");
        for (TriggerControl triggerControl : triggerControls) {
            JsonElement state = triggers.get(Integer.toString(triggerControl.getTriggerId()));
            if (state != null) {
                triggerControl.setState(GSON.fromJson(state, TriggerControl.State.class));
            }
        }
    }

    /**
     * Finds the entity components, every class in the component package
     *
     * @return the component types by their simple names
     */
    private static Map<String, Class<? extends EntityComponent>> findComponentTypes() {
        try {
            Path classes = Paths.get(Position.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            try (FileSystem jar = Files.isDirectory(classes) ? null : FileSystems.newFileSystem(classes);
                    Stream<Path> files = Files.list((jar != null ? jar.getPath("/") : classes).resolve(COMPONENT_PACKAGE.replace('.', '/')))) {
                Map<String, Class<? extends EntityComponent>> types = new TreeMap<>();
                for (Path file : files.toList()) {
                    String fileName = file.getFileName().toString();
                    if (fileName.endsWith(".class") && !fileName.contains("$")) {
                        Class<?> type = Class.forName(COMPONENT_PACKAGE + "." + fileName.substring(0, fileName.length() - ".class".length()));
                        if (EntityComponent.class.isAssignableFrom(type) && !type.isInterface() && !Modifier.isAbstract(type.getModifiers())) {
                            types.put(type.getSimpleName(), type.asSubclass(EntityComponent.class));
                        }
                    }
                }
                return types;
            }
        } catch (IOException | URISyntaxException | ClassNotFoundException ex) {
            throw new IllegalStateException("Failed to find the entity components!", ex);
        }
    }

    /**
     * Writes enum ordinals of a component by name, so the save survives
     * changes to the enum. The components keep the ordinals for the network
     * serializer.
     */
    private static final class EnumNames<C, E extends Enum<E>> implements TypeAdapterFactory {

        private final Class<C> componentType;
        private final Class<E> enumType;
        private final String[] fields;

        public EnumNames(Class<C> componentType, Class<E> enumType, String... fields) {
            this.componentType = componentType;
            this.enumType = enumType;
            this.fields = fields;
        }

        @Override
        public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
            if (type.getRawType() != componentType) {
                return null;
            }

            TypeAdapter<T> delegate = gson.getDelegateAdapter(this, type);
            TypeAdapter<JsonElement> elements = gson.getAdapter(JsonElement.class);
            E[] constants = enumType.getEnumConstants();
            return new TypeAdapter<T>() {

                @Override
                public void write(JsonWriter out, T value) throws IOException {
                    JsonObject json = delegate.toJsonTree(value).getAsJsonObject();
                    for (String field : fields) {
                        int ordinal = json.get(field).getAsInt();
                        if (ordinal >= 0 && ordinal < constants.length) {
                            json.addProperty(field, constants[ordinal].name());
                        }
                    }
                    elements.write(out, json);
                }

                @Override
                public T read(JsonReader in) throws IOException {
                    JsonObject json = elements.read(in).getAsJsonObject();
                    for (String field : fields) {
                        JsonElement value = json.get(field);
                        if (value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()) {
                            json.addProperty(field, Enum.valueOf(enumType, value.getAsString()).ordinal());
                        }
                    }
                    return delegate.fromJsonTree(json);
                }
            }.nullSafe();
        }
    }

}
