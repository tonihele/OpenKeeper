/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package toniarts.opendungeonkeeper.gui;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A simple dictionary, to hold our own simple dictionary format<br>
 * Our format is simply id=string
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class Dictionary extends LinkedHashMap<Integer, String> {

    private static final Logger logger = Logger.getLogger(Dictionary.class.getName());

    /**
     * For the asset converter, loads up the dictionary with ready map
     *
     * @param m existing map
     */
    public Dictionary(Map<Integer, String> m) {
        super(m);
    }

    /**
     * Loads up the dictionary from a file and checks duplicates
     *
     * @param inputStream the file to read the dictionaries from
     */
    public Dictionary(InputStream inputStream) throws UnsupportedEncodingException, IOException {
        super(load(inputStream));
    }

    /**
     * Loads up the dictionary from a file, it is sorted and checked for
     * duplicates
     *
     * @param inputStream the source file
     * @return sorted dictionary map
     * @throws UnsupportedEncodingException What?
     * @throws IOException Reading may fail
     */
    private static Map<Integer, String> load(InputStream inputStream) throws UnsupportedEncodingException, IOException {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream, "UTF-16"))) {
            String line;
            TreeMap<Integer, String> map = new TreeMap<>(); // Keep them in order
            Pattern pattern = Pattern.compile("(?<id>\\d+)=(?<text>.*+)");
            while ((line = br.readLine()) != null) {

                // Parse
                Matcher m = pattern.matcher(line);
                if (!m.matches()) {
                    throw new RuntimeException("Failed to parse the dictionary! The line was " + line + "!");
                }
                Integer id = Integer.parseInt(m.group("id"));
                String text = m.group("text");

                // Check for duplicates
                if (map.containsKey(id)) {
                    logger.log(Level.WARNING, "Duplicate entry found with ID {0}!", id);
                } else { // Add
                    map.put(id, text);
                }
            }

            // Return the map
            return map;
        }
    }

    /**
     * Saves the dictionary in our format
     *
     * @param file file to save the dictionary to
     */
    public void save(File file) throws IOException {
        try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file, false), "UTF-16"))) {
            for (Map.Entry<Integer, String> entry : entrySet()) {
                pw.println(entry.getKey() + "=" + entry.getValue());
            }
        }
    }
}
