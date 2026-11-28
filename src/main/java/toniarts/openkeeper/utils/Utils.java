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
package toniarts.openkeeper.utils;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.lang.management.ManagementFactory;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.ResourceBundle;
import java.util.random.RandomGenerator;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import toniarts.openkeeper.Main;
import toniarts.openkeeper.tools.convert.map.Player;

/**
 * Some utility methods
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public final class Utils {

    private static final Logger logger = System.getLogger(Utils.class.getName());

    private static final Random RANDOM = new Random();
    // The pools are in DK II's own order (the tables at 0x69A8E8 and 0x69A118 of DKII.EXE)
    private static final String[] BLOOD_TYPES = {"ARh+", "O", "MoO+", "BA", "PoE", "BO", "IkI"};
    private static final String[] CREATURE_NAMES = {
        "Delver", "Mortis", "Galen", "Threck", "Ratchett", "Slake", "Gewgog", "Hatchett", "Rictus",
        "Scurge", "Phestre", "Tome", "Orzac", "Smirch", "Odo", "Bane", "Prang", "Loam", "Mucor", "Mulch",
        "Grelch", "Whipp", "Priap", "Mysticor", "Ludo", "Sotto", "Mantissa", "Kludo", "Bel", "Ralpartha",
        "Boris", "Brian", "Seath", "Caleb", "Spudd", "Mook", "Beedle", "Kasper", "Osmo", "Torus", "Berk",
        "Fickel", "Bolus", "Nihil", "Vellum", "Flaire", "Scabmeat", "Ingro", "Gaspar", "Pugh", "Casca",
        "Bugpus", "Fengus", "Gill", "Orcus", "Brand", "Igor", "Osric", "Mungo", "Pogrom", "Dural", "Dave",
        "Caro", "Susan", "Boon", "Verrucus", "Rancor", "Gibbet", "Quilk", "Kane", "Thoog", "Molok", "Wort",
        "Notch", "Occator", "Merle", "Pinion", "Skimpy", "Shade", "Tolchok", "Julian", "Cranioch", "Ikari",
        "Cerebus", "Furis", "Pock", "Urmeric", "Oriander", "Mirk", "Rasmus", "Croak", "Whelt", "Kurt",
        "Lothar", "Polyp", "Edema", "Nodos", "Lardsac", "Bludgeon", "Tycho", "Myrk", "Andante", "Cloy",
        "Ferral", "Magmas", "Mithras", "Sapper", "Grinder", "Oolon", "Tartaric", "Drudge", "Snipe",
        "Mudlust", "Putre", "Fust", "Scrawn", "Malefric", "DeGrise", "Blyte", "Dragan", "Tallow",
        "Quagmire", "Moraine", "Vidor", "Syrinx", "Weevil", "Fuligin", "Boremite", "Phirus", "Mordicant",
        "Ankus", "Palaemon", "Nenuphrin", "Verdemis", "DeGrue", "Rickets", "Krabbs", "Trego", "Scrofule",
        "Fengor", "Purge", "Mane", "Corlac", "Quebus", "Smeeg", "Mantric", "Klammer", "Grombold", "Mawl",
        "Mike", "Bede", "Sulis", "Bron", "Quirkus", "Megalo", "Noctur", "Spike", "Botch", "Prolix", "Ochre",
        "Vogus", "Fagor", "Thule", "Rench", "Munch", "Molder", "Crank", "Swellpit", "Rumen", "Ermite",
        "Megistus", "Nimrod", "Gantor", "Vulcan", "Fidus", "Talos", "Mutager", "Skuld", "Naga", "Vegan",
        "Pelerin", "Alzabo", "Kardec", "Vermilion", "Niph", "Spume", "Tumulus", "Caligin", "Noctivagant",
        "Jael", "Yaphet", "Kord", "Wrake", "Nenuphar", "Azarin", "Pulsipher", "Goloch", "Thorgen", "Cotter",
        "Mephit", "Phistuler", "Gorkimir", "Vlad", "Udo", "Vespertine", "Fuskus", "Carmine", "Swelter",
        "Kade", "Ossifer", "Morg", "Lupus", "Vulpus", "Ghule", "Madderose", "Alizarin", "Dirge", "Coops",
        "Goitre", "Orphyr", "Throtter", "Sturge", "Nute", "Scullion", "Gibbus", "Droog", "Pumice", "Bubo",
        "Glock", "Leech", "Shadrac", "Hellion", "Roach", "Boderik", "Gorse", "Gort", "Azrael", "Phog",
        "Vilish", "Clench", "Jorg", "Torc", "Cheop", "Malrubius", "Magog", "Obol", "Bogus", "Mopik",
        "Ominus", "Tritoch", "Hakim", "Orghul", "Maff", "Bob", "Grote", "Thrall", "Hellot", "Scrote",
        "Khalech", "Prestiger", "Phage", "Scutum", "Sputum", "Nidus", "Cardolan", "Grailbait", "Chaldis",
        "Grule", "Eblis", "Blain", "Primus", "Gudgeon", "Saleph", "Gangren", "Curdel", "Quiver", "Flay",
        "Mote", "Flog", "Thwart", "Quell", "Gizzard", "Phegg", "Dorfen", "Gorge", "Kreep", "Shintaro",
        "Gatcheman", "Vlek", "Stego", "Nubble", "Clave", "Spoor", "Whorl", "Sapir", "Myrmid", "Spawn",
        "Tyram", "Valdegrin", "Sulcus", "Belisar", "Gorgo", "Phleg", "Garm", "Glaucus", "Cloot", "Hermann",
        "Garlech", "Dourif", "Squatch", "Ultare", "Grimoire", "Belloc", "Balphegor", "Gurloes", "Melchior",
        "Arkum", "Sephir", "Mignon", "Brudegrim", "Zoa", "Glyph", "Tartarus", "Corvus", "Thabala", "Mesmyr",
        "Kronos", "Coleric", "Typhon", "Vurne", "Micturus", "Miro", "Krite", "Caliban", "Locus", "Corax",
        "Veigle", "Geddon", "Marune", "Leon", "Bonemeal", "Rankle", "Vulgor", "Sloph", "Kevin", "Bospha",
        "Drow", "Swill", "Mantus", "Carnek", "Bert", "Palter", "Cant", "Pecksniff", "Ultan", "Avernus",
        "Acanthus", "Fervor", "Turjan", "Zelus", "Dolmen", "Pyre", "Sorku", "Fleck", "Lictor", "Shroud",
        "Odir", "Autarc", "Nebulus", "Braze", "Slugg", "Khanal", "Spay", "Barrow", "Pulchre", "Perfidius",
        "Funes", "Saltus", "Bez", "Scrum", "Rumpus", "Whumpus", "Uhlan", "Khir", "Rage", "Maelstrom",
        "Pynch", "Gnaw", "Fordor", "Nadrattle", "Toxicor", "Sequestor", "Keppel", "Cruach", "Gnoll",
        "Jubal", "Strafe", "Sorrow", "Ichor", "Cruor", "Grume", "Nicodemus", "Cacodemus", "Molespit",
        "Wyrmcast", "Stonegore", "Flynchknot", "Uddergripe", "Wormish", "Gallscrape", "Gougewheal",
        "Myrkin", "Flymeat", "Haglash", "Ratfretter", "Catlash", "Ratsack", "Stingpin", "Meatnik",
        "Flybore", "Grelchfork", "Skullsunder", "Turnspit", "Gnatspur", "Tenticol", "Crom", "Pallor",
        "Furor", "Letharge", "Lazuli", "Cacus", "Cromlech", "Pestilus", "Abrax", "Tungue", "Frood",
        "Ozymandias", "Masoch", "Medael", "Goad", "Hurse", "Nefarius", "Shadrach", "Armandaz", "Deodand",
        "Shelag", "Animus", "Thanatar", "Magister", "Philtre", "Gigos", "Prowl", "Jugula", "Umbra",
        "Tussoc", "Bhutt", "Phlebol", "Rorty", "Penumbra", "Fugue", "Monculus", "Gregor", "Cyclop",
        "Polidori", "Ithaque", "Kragen", "Pawnch", "Chthon", "Cataractus", "Prole", "Lector", "Baphet",
        "Schismus", "Carnivos", "Caries", "Scurvide", "Phung", "Stendhal", "Kraken", "Ako", "Callus",
        "Gristle", "Saad", "Madloc", "Flint", "Carotida", "Slay", "Aorta", "Mordechai", "Agaric", "Botulic",
        "Samsar", "Snitch", "Ghast", "Drotte", "Poge", "Thanos", "Petripher", "Murmyr", "Telos"};

    public static final int MAX_THREADS = Runtime.getRuntime().availableProcessors();
    public static final int MAX_CREATURE_LEVEL = 10;
    public static final short[] PLAYER_IDS = new short[]{Player.GOOD_PLAYER_ID, Player.NEUTRAL_PLAYER_ID, Player.KEEPER1_ID, Player.KEEPER2_ID, Player.KEEPER3_ID, Player.KEEPER4_ID, Player.KEEPER5_ID};

    private static ResourceBundle bundle;
    private static Boolean windows;

    private Utils() {
        // Nope
    }

    private static String getOsName() {
        return System.getProperty("os.name");
    }

    /**
     * Is this OS MS Windows
     *
     * @return is Windows
     */
    public static boolean isWindows() {
        if (windows == null) {
            windows = getOsName().toLowerCase().startsWith("windows");
        }
        return windows;
    }

    /**
     * Gets up a random creature name. Like DK II (0x491900), the name is one
     * that no other creature of the owner has, as long as there is one left
     *
     * @param taken the names the owner's creatures already have
     * @return name for your creature
     */
    public static String generateCreatureName(Collection<String> taken) {
        return pickUnused(CREATURE_NAMES, RANDOM.nextInt(CREATURE_NAMES.length), taken);
    }

    /**
     * Gets up a random blood type. Like DK II (0x4919C0), the blood type is
     * one that no other creature of the owner has, as long as there is one
     * left. DK II rolls only the first six, the last one is reached by the
     * search
     *
     * @param taken the blood types the owner's creatures already have
     * @return blood type for your creature
     */
    public static String generateBloodType(Collection<String> taken) {
        return pickUnused(BLOOD_TYPES, RANDOM.nextInt(BLOOD_TYPES.length - 1), taken);
    }

    private static String pickUnused(String[] pool, int start, Collection<String> taken) {
        for (int i = 0; i < pool.length; i++) {
            String candidate = pool[(start + i) % pool.length];
            if (!taken.contains(candidate)) {
                return candidate;
            }
        }

        return pool[start];
    }

    /**
     * Get a random item from a list
     *
     * @param <T> item type
     * @param list list
     * @return random item from the list or {@code null} if the list is empty
     */
    public static <T> T getRandomItem(List<T> list) {
        if (list.isEmpty()) {
            return null;
        }
        if (list.size() == 1) {
            return list.get(0);
        }
        return list.get(RANDOM.nextInt(list.size()));
    }

    /**
     * Get a random item from a collection
     *
     * @param <T> item type
     * @param collection list
     * @return random item from the collection or an empty optional if the collection is empty
     */
    public static <T> Optional<T> getRandomItem(Collection<T> collection) {
        if (collection.isEmpty()) {
            return Optional.empty();
        }
        return collection.stream().skip(RANDOM.nextInt(collection.size())).findFirst();
    }

    /**
     * Returns the random generator used by the game
     *
     * @return random generator
     */
    public static RandomGenerator getRandom() {
        return RANDOM;
    }

    /**
     * Get the game main text resource bundle
     *
     * @return the main text resource bundle
     */
    public static ResourceBundle getMainTextResourceBundle() {
        if (bundle == null) {
            bundle = Main.getResourceBundle("Interface/Texts/Text");
        }
        return bundle;
    }

    /**
     * Get the system memory in GB
     *
     * @return system memory
     */
    public static int getSystemMemory() {
        try {
            MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
            Long bytes = (Long) mBeanServer.getAttribute(new ObjectName("java.lang", "type", "OperatingSystem"), "TotalPhysicalMemorySize");
            return (int) Math.round(bytes / 1024d / 1024d / 1024d);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to get system memory!", e);
        }
        return 0;
    }

    /**
     * Tries to get the local public IP address
     *
     * @return the IP address
     */
    public static String getLocalIPAddress() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();
                if (!networkInterface.isLoopback() && networkInterface.isUp()) {
                    Enumeration<InetAddress> inetAddresses = networkInterface.getInetAddresses();
                    while (inetAddresses.hasMoreElements()) {
                        InetAddress inetAddress = inetAddresses.nextElement();
                        if (inetAddress instanceof Inet4Address) {
                            return inetAddress.getHostAddress();
                        }
                    }
                }
            }
        } catch (SocketException e) {
            logger.log(Level.WARNING, "Failed to get local IP address!", e);
        }
        return null;
    }

    /**
     * Formats enum name to human readable format
     *
     * @param <E> enum type
     * @param enumMember enum value
     * @return good loking enum name
     */
    public static <E extends Enum> String prettyPrintEnum(E enumMember) {
        String[] splitted = enumMember.name().split("_");
        String result = "";
        for (String s : splitted) {
            result = result.concat(" ").concat(s.substring(0, 1).toUpperCase()).concat(s.substring(1).toLowerCase());
        }
        return result.trim();
    }

    /**
     * Get all the player IDs available in the game
     *
     * @return array of player IDs
     */
    public static short[] getPlayerIds() {
        return PLAYER_IDS;
    }

    public static String timeToString(int time) {
        String result = "";
        int days = time / 86400;
        if (days != 0) {
            time -= days * 86400;
            result += days;
        }
        int hours = time / 3600;
        if (hours != 0) {
            time -= hours * 3600;
            result += String.format(" %02d:", hours);
        }
        int minutes = time / 60;
        time -= minutes * 60;
        int seconds = time;
        result += String.format("%02d:%02d", minutes, seconds);

        return result.trim();
    }
}
