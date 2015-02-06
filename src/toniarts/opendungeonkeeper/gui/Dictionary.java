/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package toniarts.opendungeonkeeper.gui;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A simple dictionary, to hold our own simple dictionary format<br>
 * Our format is simply id=string
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class Dictionary extends LinkedHashMap<Integer, String> {

    /**
     * For the asset converter, loads up the dictionary with ready map
     *
     * @param m existing map
     */
    public Dictionary(Map<Integer, String> m) {
        super(m);
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
