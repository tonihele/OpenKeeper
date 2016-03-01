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
package toniarts.openkeeper.tools.convert.material;

import com.jme3.export.JmeExporter;
import com.jme3.export.OutputCapsule;
import com.jme3.export.Savable;
import com.jme3.material.MatParam;
import com.jme3.material.Material;
import com.jme3.material.MaterialDef;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * An exporter for JME materials<br>
 * Maybe not the smartest idea implement the JmeExporter, maybe a standalone
 * util class would have been better and type safe. I mean, we can't save
 * anything else than a Material really...<br>
 * Format and code is heavily copy + pasted from
 * com.jme3.gde.materials.EditableMaterialFile.java
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class MaterialExporter implements JmeExporter {

    private String name;
    private String matDefName;
    private MaterialDef matDef;
//    private MaterialDef materialDef;
    private OutputStream outputStream;
    private Map<String, MaterialProperty> materialParameters = new HashMap<>();
    private Map<String, MaterialProperty> additionalRenderStates = new HashMap<>();
    private List<String> matDefEntries = new ArrayList<>();

    @Override
    public void save(Savable svbl, OutputStream out) throws IOException {

        // We only save Materials
        if (!(svbl instanceof Material)) {
            throw new UnsupportedOperationException("We only can save JME Material files!");
        }
        Material mat = (Material) svbl;

        // Set the file
        outputStream = out;
        setAsMaterial(mat);
    }

    @Override
    public void save(Savable svbl, File file) throws IOException {

        // Copied from BinaryExporter JME 3
        File parentDirectory = file.getParentFile();
        if (parentDirectory != null && !parentDirectory.exists()) {
            parentDirectory.mkdirs();
        }
        boolean rVal;
        try (FileOutputStream fos = new FileOutputStream(file)) {
            save(svbl, fos);
        }
    }

    @Override
    public OutputCapsule getCapsule(Savable object) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    /**
     * Creates the data from a material
     *
     * @param mat
     */
    public void setAsMaterial(Material mat) throws IOException {
        assert (mat.getMaterialDef().getAssetName() != null);
        setName(mat.getName());
        setMatDefName(mat.getMaterialDef().getAssetName());

        matDef = mat.getMaterialDef();

//        createBaseMaterialFile();
        materialParameters.clear();
        //checkPackedTextureProps(mat);

        addMaterialParameters(mat);

        additionalRenderStates.put("Wireframe", new MaterialProperty("OnOff", "Wireframe", mat.getAdditionalRenderState().isWireframe() ? "On" : "Off"));
        additionalRenderStates.put("DepthWrite", new MaterialProperty("OnOff", "DepthWrite", mat.getAdditionalRenderState().isDepthWrite() ? "On" : "Off"));
        additionalRenderStates.put("DepthTest", new MaterialProperty("OnOff", "DepthTest", mat.getAdditionalRenderState().isDepthTest() ? "On" : "Off"));
        additionalRenderStates.put("ColorWrite", new MaterialProperty("OnOff", "ColorWrite", mat.getAdditionalRenderState().isColorWrite() ? "On" : "Off"));
        additionalRenderStates.put("PointSprite", new MaterialProperty("OnOff", "PointSprite", mat.getAdditionalRenderState().isPointSprite() ? "On" : "Off"));
        additionalRenderStates.put("FaceCull", new MaterialProperty("FaceCullMode", "FaceCull", mat.getAdditionalRenderState().getFaceCullMode().name()));
        additionalRenderStates.put("Blend", new MaterialProperty("BlendMode", "Blend", mat.getAdditionalRenderState().getBlendMode().name()));
        additionalRenderStates.put("AlphaTestFalloff", new MaterialProperty("Float", "AlphaTestFalloff", mat.getAdditionalRenderState().getAlphaFallOff() + ""));
        additionalRenderStates.put("PolyOffset", new MaterialProperty("Float,Float", "PolyOffset", mat.getAdditionalRenderState().getPolyOffsetUnits() + " " + mat.getAdditionalRenderState().getPolyOffsetFactor()));
        checkWithMatDef();
        setAsText(getUpdatedContent());
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the matDefName
     */
    public String getMatDefName() {
        return matDefName;
    }

    /**
     * @param matDefName the matDefName to set
     */
    public void setMatDefName(String matDefName) {
        this.matDefName = matDefName;
        assert (matDefName != null);
//        initMatDef();
//        checkWithMatDef();
    }

    private List<String> createBaseMaterialFile() {
        List<String> lines = new ArrayList<>();
        lines.add("Material MyMaterial : " + matDefName + " {\n");
        lines.add(" MaterialParameters {\n");
        lines.add(" }\n");
        lines.add("}\n");
        return lines;
    }

//    private void initMatDef() {
//        materialDef = (MaterialDef) manager.loadAsset(new AssetKey(matDefName));
//    }
    /**
     * Finds and loads the matdef file either from project or from base jme,
     * then applies the parameter values to the material parameter entries.
     *
     * @param line
     */
    private void checkWithMatDef() {
//load matdef
        matDefEntries.clear();

        for (MatParam matParam : matDef.getMaterialParams()) {
            matDefEntries.add(matParam.getName());
            MaterialProperty prop = materialParameters.get(matParam.getName());
            if (prop == null) {
                prop = new MaterialProperty();
                prop.setName(matParam.getName());
                prop.setValue("");
                materialParameters.put(prop.getName(), prop);
            }
            prop.setType(matParam.getVarType().toString());
        }

        for (Iterator<Map.Entry<String, MaterialProperty>> it = materialParameters.entrySet().iterator(); it.hasNext();) {
            Map.Entry<String, MaterialProperty> entry = it.next();
            if (!matDefEntries.contains(entry.getKey())) {
                it.remove();
            }
        }
    }

    /**
     * returns the new content of the material file, filled with the new
     * parameters
     *
     * @return
     */
    public String getUpdatedContent() {
        boolean params = false;
        boolean states = false;
        boolean addedstates = false;
        int level = 0;
        try {
//            List<String> matLines = material.asLines();
            List<String> matLines = createBaseMaterialFile();
            StringWriter out = new StringWriter();
            List<String> setValues = new LinkedList<>();
            List<String> setStates = new LinkedList<>();
//goes through the lines of the material file and replaces the values it finds
            for (String line : matLines) {
                String newLine = line;
//                line = MaterialUtils.trimLine(line);
                line = trimLine(line);
//write material header
                if (line.startsWith("Material ") || line.startsWith("Material\t") && level == 0) {
                    String suffix = "";
                    if (line.indexOf("{") > -1) {
                        suffix = "{";
                    }
                    newLine = "Material " + getName() + " : " + matDefName + " " + suffix;
                }
//start parameters
                if (line.startsWith("MaterialParameters ") || line.startsWith("MaterialParameters\t") || line.startsWith("MaterialParameters{") && level == 1) {
                    params = true;
                }
//start states
                if (line.startsWith("AdditionalRenderState ") || line.startsWith("AdditionalRenderState\t") || line.startsWith("AdditionalRenderState{") && level == 1) {
                    states = true;
                    addedstates = true;
                }
//up a level
                if (line.indexOf("{") != -1) {
                    level++;
                }
//down a level, stop processing states and check if all parameters and states have been written
                if (line.indexOf("}") != -1) {
                    level--;
//find and write parameters we did not replace yet at end of parameters section
                    if (params) {
                        for (Iterator<Map.Entry<String, MaterialProperty>> it = materialParameters.entrySet().iterator(); it.hasNext();) {
                            Map.Entry<String, MaterialProperty> entry = it.next();
                            if (!setValues.contains(entry.getKey()) && matDefEntries.contains(entry.getKey())) {
                                MaterialProperty prop = entry.getValue();
                                if (prop.getValue() != null && prop.getValue().length() > 0) {
                                    String myLine = " " + prop.getName() + " : " + prop.getValue() + "\n";
                                    out.write(myLine, 0, myLine.length());
                                }
                            }
                        }
                        params = false;
                    }
//find and write states we did not replace yet at end of states section
                    if (states) {
                        for (Iterator<Map.Entry<String, MaterialProperty>> it = additionalRenderStates.entrySet().iterator(); it.hasNext();) {
                            Map.Entry<String, MaterialProperty> entry = it.next();
                            if (!setStates.contains(entry.getKey())) {
                                MaterialProperty prop = entry.getValue();
                                if (prop.getValue() != null && prop.getValue().length() > 0) {
                                    String myLine = " " + prop.getName() + " " + prop.getValue() + "\n";
                                    out.write(myLine, 0, myLine.length());
                                }
                            }
                        }
                        states = false;
                    }
//add renderstates if they havent been in the file yet
                    if (level == 0) {
                        if (!addedstates) {
                            String myLine = " AdditionalRenderState {\n";
                            out.write(myLine, 0, myLine.length());
                            for (Iterator<Map.Entry<String, MaterialProperty>> it = additionalRenderStates.entrySet().iterator(); it.hasNext();) {
                                Map.Entry<String, MaterialProperty> entry = it.next();
                                if (!setStates.contains(entry.getKey())) {
                                    MaterialProperty prop = entry.getValue();
                                    if (prop.getValue() != null && prop.getValue().length() > 0) {
                                        myLine = " " + prop.getName() + " " + prop.getValue() + "\n";
                                        out.write(myLine, 0, myLine.length());
                                    }
                                }
                            }
                            myLine = " }\n";
                            out.write(myLine, 0, myLine.length());
                        }
                    }
                }
//try replacing value of parameter line with new value
                if (level == 2 && params) {
                    int colonIdx = newLine.indexOf(":");
                    if (colonIdx != -1) {
                        String[] lines = newLine.split(":");
                        String myName = lines[0].trim();
                        if (materialParameters.containsKey(myName)) {
                            setValues.add(myName);
                            MaterialProperty prop = materialParameters.get(myName);
                            if (prop.getValue() != null && prop.getValue().length() > 0 && prop.getType() != null) {
                                newLine = lines[0] + ": " + prop.getValue();
                            } else {
                                newLine = null;
                            }
                        } else if (!matDefEntries.contains(myName)) {
                            newLine = null;
                        }
                    }
                }
//try replacing value of state line with new value
                if (level == 2 && states) {
                    String cutLine = newLine.trim();
                    String[] lines = null;
                    int colonIdx = cutLine.indexOf(" ");
                    if (colonIdx != -1) {
                        lines = cutLine.split(" ");
                    }
                    colonIdx = cutLine.indexOf("\t");
                    if (colonIdx != -1) {
                        lines = cutLine.split("\t");
                    }
                    if (lines != null) {
                        String myName = lines[0].trim();
                        if (additionalRenderStates.containsKey(myName)) {
                            setStates.add(myName);
                            MaterialProperty prop = additionalRenderStates.get(myName);
                            if (prop.getValue() != null && prop.getValue().length() > 0 && prop.getType() != null) {
                                newLine = " " + lines[0] + " " + prop.getValue();
                            } else {
                                newLine = null;
                            }
                        }
                    }
                }
                if (newLine != null) {
                    out.write(newLine + "\n", 0, newLine.length() + 1);
                }
            }
            out.close();
            return out.toString();
        } catch (IOException ex) {
            // Exceptions.printStackTrace(ex);
        }
        return "";
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * trims a line and removes comments
     *
     * @param line
     * @return
     */
    public static String trimLine(String line) {
        int idx = line.indexOf("//");
        if (idx != -1) {
            line = line.substring(0, idx);
        }
        return line.trim();
    }

    public void setAsText(String text) throws IOException {
        try (OutputStreamWriter out = new OutputStreamWriter(getOutputStream())) {
            out.write(text, 0, text.length());
        }
    }

    private OutputStream getOutputStream() throws FileNotFoundException {
        return outputStream;
    }

    private void addMaterialParameters(Material mat) {
        for (MatParam matParam : mat.getParams()) {
            MaterialProperty prop = new MaterialProperty(matParam);
            materialParameters.put(matParam.getName(), prop);
        }
    }
}
