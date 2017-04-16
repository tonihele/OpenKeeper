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

import java.lang.management.ManagementFactory;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.List;
import java.util.Random;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import toniarts.openkeeper.Main;

/**
 * Some utility methods
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class Utils {

    private static final Logger logger = Logger.getLogger(Utils.class.getName());
    private static final ResourceBundle bundle = Main.getResourceBundle("Interface/Texts/Text");
    private static final Random random = new Random();
    private static Boolean windows;
    private static final String[] BLOOD_TYPES = {"IkI", "BO", "PoE", "BA", "MoO", "O", "ARh"};
    private static final String[] CREATURE_NAMES = {
        "Telos", "Murmyr", "Petripher", "Thanos", "Poge", "Drotte", "Ghast", "Snitch", "Samsar",
        "Botulic", "Agaric", "Mordechai", "Aorta", "Slay", "Carotida", "Flint", "Madloc", "Saad",
        "Gristle", "Callus", "Ako", "Kraken", "Stendhal", "Phung", "Scurvide", "Caries", "Carnivos",
        "Schismus", "Baphet", "Lector", "Prole", "Cataractus", "Chthon", "Pawnch", "Kragen", "Ithaque",
        "Polidori", "Cyclop", "Gregor", "Monculus", "Fugue", "Penumbra", "Rorty", "Phlebol", "Bhutt",
        "Tussoc", "Umbra", "Jugula", "Prowl", "Gigos", "Philtre", "Magister", "Thanatar", "Animus",
        "Shelag", "Deodand", "Armandaz", "Shadrach", "Nefarius", "Hurse", "Goad", "Medael", "Masoch",
        "Ozymandias", "Frood", "Tungue", "Abrax", "Pestilus", "Cromlech", "Cacus", "Lazuli", "Letharge",
        "Furor", "Pallor", "Crom", "Tenticol", "Gnatspur", "Turnspit", "Skullsunder", "Grelchfork",
        "Flybore", "Meatnik", "Stingpin", "Ratsack", "Catlash", "Ratfretter", "Haglash", "Flymeat",
        "Myrkin", "Gougewheal", "Gallscrape", "Wormish", "Uddergripe", "Flynchknot", "Stonegore",
        "Wyrmcast", "Molespit", "Cacodemus", "Nicodemus", "Grume", "Cruor", "Ichor", "Sorrow", "Strafe",
        "Jubal", "Gnoll", "Cruach", "Keppel", "Sequestor", "Toxicor", "Nadrattle", "Fordor", "Gnaw",
        "Pynch", "Maelstrom", "Rage", "Khir", "Uhlan", "Whumpus", "Rumpus", "Scrum", "Bez", "Saltus",
        "Funes", "Perfidius", "Pulchre", "Barrow", "Spay", "Khanal", "Slugg", "Braze", "Nebulus", "Autarc",
        "Odir", "Shroud", "Lictor", "Fleck", "Sorku", "Pyre", "Dolmen", "Zelus", "Turjan", "Fervor",
        "Acanthus", "Avernus", "Ultan", "Pecksniff", "Cant", "Palter", "Bert", "Carnek", "Mantus", "Swill",
        "Drow", "Bospha", "Kevin", "Sloph", "Vulgor", "Rankle", "Bonemeal", "Leon", "Marune", "Geddon",
        "Veigle", "Corax", "Locus", "Caliban", "Krite", "Miro", "Micturus", "Vurne", "Typhon", "Coleric",
        "Kronos", "Mesmyr", "Thabala", "Corvus", "Tartarus", "Glyph", "Zoa", "Brudegrim", "Mignon",
        "Sephir", "Arkum", "Melchior", "Gurloes", "Balphegor", "Belloc", "Grimoire", "Ultare", "Squatch",
        "Dourif", "Garlech", "Hermann", "Cloot", "Glaucus", "Garm", "Phleg", "Gorgo", "Belisar", "Sulcus",
        "Valdegrin", "Tyram", "Spawn", "Myrmid", "Sapir", "Whorl", "Spoor", "Clave", "Nubble", "Stego",
        "Vlek", "Gatcheman", "Shintaro", "Kreep", "Gorge", "Dorfen", "Phegg", "Gizzard", "Quell", "Thwart",
        "Flog", "Mote", "Flay", "Quiver", "Curdel", "Gangren", "Saleph", "Gudgeon", "Primus", "Blain",
        "Eblis", "Grule", "Chaldis", "Grailbait", "Cardolan", "Nidus", "Sputum", "Scutum", "Phage",
        "Prestiger", "Khalech", "Scrote", "Hellot", "Thrall", "Grote", "Bob", "Maff", "Orghul", "Hakim",
        "Tritoch", "Ominus", "Mopik", "Bogus", "Obol", "Magog", "Malrubius", "Cheop", "Torc", "Jorg",
        "Clench", "Vilish", "Phog", "Azrael", "Gort", "Gorse", "Boderik", "Roach", "Hellion", "Shadrac",
        "Leech", "Glock", "Bubo", "Pumice", "Droog", "Gibbus", "Scullion", "Nute", "Sturge", "Throtter",
        "Orphyr", "Goitre", "Coops", "Dirge", "Alizarin", "Madderose", "Ghule", "Vulpus", "Lupus", "Morg",
        "Ossifer", "Kade", "Swelter", "Carmine", "Fuskus", "Vespertine", "Udo", "Vlad", "Gorkimir",
        "Phistuler", "Mephit", "Cotter", "Thorgen", "Goloch", "Pulsipher", "Azarin", "Nenuphar", "Wrake",
        "Kord", "Yaphet", "Jael", "Noctivagant", "Caligin", "Tumulus", "Spume", "Niph", "Vermilion",
        "Kardec", "Alzabo", "Pelerin", "Vegan", "Naga", "Skuld", "Mutager", "Talos", "Fidus", "Vulcan",
        "Gantor", "Nimrod", "Megistus", "Ermite", "Rumen", "Swellpit", "Crank", "Molder", "Munch", "Rench",
        "Thule", "Fagor", "Vogus", "Ochre", "Prolix", "Botch", "Noctur", "Megalo", "Quirkus", "Bron", "Sulis",
        "Bede", "Mike", "Mawl", "Grombold", "Klammer", "Mantric", "Smeeg", "Quebus", "Corlac", "Mane",
        "Purge", "Fengor", "Scrofule", "Trego", "Krabbs", "Rickets", "DeGrue", "Verdemis", "Nenuphrin",
        "Palaemon", "Ankus", "Mordicant", "Phirus", "Boremite", "Fuligin", "Weevil", "Syrinx", "Vidor",
        "Moraine", "Quagmire", "Tallow", "Dragan", "Blyte", "DeGrise", "Malefric", "Scrawn", "Fust", "Putre",
        "Mudlust", "Snipe", "Drudge", "Tartaric", "Oolon", "Grinder", "Sapper", "Mithras", "Magmas", "Ferral",
        "Cloy", "Andante", "Myrk", "Tycho", "Bludgeon", "Lardsac", "Nodos", "Edema", "Polyp", "Lothar", "Kurt",
        "Whelt", "Croak", "Rasmus", "Mirk", "Oriander", "Urmeric", "Pock", "Furis", "Cerebus", "Ikari",
        "Cranioch", "Julian", "Tolchok", "Shade", "Skimpy", "Pinion", "Merle", "Occator", "Notch", "Wort",
        "Molok", "Thoog", "Kane", "Quilk", "Gibbet", "Rancor", "Verrucus", "Boon", "Susan", "Caro", "Dave",
        "Dural", "Pogrom", "Mungo", "Osric", "Igor", "Brand", "Orcus", "Gill", "Fengus", "Bugpus", "Casca",
        "Pugh", "Gaspar", "Ingro", "Scabmeat", "Flaire", "Vellum", "Nihil", "Bolus", "Fickel", "Berk", "Torus",
        "Osmo", "Kasper", "Beedle", "Mook", "Spudd", "Caleb", "Seath", "Brian", "Boris", "Ralpartha", "Bel",
        "Kludo", "Mantissa", "Sotto", "Ludo", "Mysticor", "Priap", "Whipp", "Grelch", "Mulch", "Mucor",
        "Loam", "Prang", "Bane", "Odo", "Smirch", "Orzac", "Tome", "Phestre", "Scurge", "Rictus",
        "Hatchett", "Gewgog", "Slake", "Ratchett", "Threck", "Galen", "Mortis", "Delver"};

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
     * Gets up a random creature name
     *
     * @return name for your creature
     */
    public static String generateCreatureName() {
        return CREATURE_NAMES[random.nextInt(CREATURE_NAMES.length)];
    }

    /**
     * Gets up a random blood type
     *
     * @return blood type for your creature
     */
    public static String generateBloodType() {
        return BLOOD_TYPES[random.nextInt(BLOOD_TYPES.length)];
    }

    /**
     * Get a random item from a list
     *
     * @param <T> item type
     * @param list list
     * @return random item from the list
     */
    public static <T> T getRandomItem(List<T> list) {
        if (list.size() == 1) {
            return list.get(0);
        }
        return list.get(random.nextInt(list.size()));
    }

    /**
     * Get the game main text resource bundle
     *
     * @return the main text resource bundle
     */
    public static ResourceBundle getMainTextResourceBundle() {
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
}
