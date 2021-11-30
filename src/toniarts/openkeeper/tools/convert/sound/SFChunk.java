/*
 * Copyright (C) 2014-2016 OpenKeeper
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
package toniarts.openkeeper.tools.convert.sound;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import toniarts.openkeeper.tools.convert.ConversionUtils;
import toniarts.openkeeper.tools.convert.IResourceChunkReader;
import toniarts.openkeeper.tools.convert.ISeekableResourceReader;
import toniarts.openkeeper.tools.convert.IValueEnum;

/**
 *
 * @author ArchDemon
 */
public class SFChunk {

    public static enum SFSampleLink implements IValueEnum {
        monoSample(1),
        rightSample(2),
        leftSample(4),
        linkedSample(8),
        RomMonoSample(0x8001),
        RomRightSample(0x8002),
        RomLeftSample(0x8004),
        RomLinkedSample(0x8008);

        @Override
        public int getValue() {
            return value;
        }

        private SFSampleLink(int value) {
            this.value = value;
        }

        private final int value;
    };

    public static enum Generators implements IValueEnum {
        startAddrsOffset(0),
        endAddrsOffset(1),
        startloopAddrsOffset(2),
        endloopAddrsOffset(3),
        startAddrsCoarseOffset(4),
        modLfoToPitch(5),
        vibLfoToPitch(6),
        modEnvToPitch(7),
        initialFilterFc(8),
        initialFilterQ(9),
        modLfoToFilterFc(10),
        modEnvToFilterFc(11),
        endAddrsCoarseOffset(12),
        modLfoToVolume(13),
        unused1(14),
        chorusEffectsSend(15),
        reverbEffectsSend(16),
        pan(17),
        unused2(18),
        unused3(19),
        unused4(20),
        delayModLFO(21),
        freqModLFO(22),
        delayVibLFO(23),
        freqVibLFO(24),
        delayModEnv(25),
        attackModEnv(26),
        holdModEnv(27),
        decayModEnv(28),
        sustainModEnv(29),
        releaseModEnv(30),
        keynumToModEnvHold(31),
        keynumToModEnvDecay(32),
        delayVolEnv(33),
        attackVolEnv(34),
        holdVolEnv(35),
        decayVolEnv(36),
        sustainVolEnv(37),
        releaseVolEnv(38),
        keynumToVolEnvHold(39),
        keynumToVolEnvDecay(40),
        instrument(41),
        reserved1(42),
        keyRange(43),
        velRange(44),
        startloopAddrsCoarseOffset(45),
        keynum(46),
        velocity(47),
        initialAttenuation(48),
        reserved2(49),
        endloopAddrsCoarseOffset(50),
        coarseTune(51),
        fineTune(52),
        sampleID(53),
        sampleModes(54),
        reserved3(55),
        scaleTuning(56),
        exclusiveClass(57),
        overridingRootKey(58),
        unused5(59),
        endOper(60);

        @Override
        public int getValue() {
            return value;
        }

        private Generators(int value) {
            this.value = value;
        }

        private final int value;
    };

    public static enum Modulators implements IValueEnum {
        NoController(0),
        NoteOnVelocity(2),
        NoteOnKeyNumber(3),
        PolyPressure(10),
        ChannelPressure(13),
        PitchWheel(14),
        PitchWheelSensitivity(16),
        Link(127);

        @Override
        public int getValue() {
            return value;
        }

        private Modulators(int value) {
            this.value = value;
        }

        private final int value;
    };

    public static enum ModulatorTypes implements IValueEnum {
        Linear(0),
        Concave(1), // output = log(sqrt(value^2)/(max value)^2)
        Convex(2),
        Switch(3);

        @Override
        public int getValue() {
            return value;
        }

        private ModulatorTypes(int value) {
            this.value = value;
        }

        private final int value;
    };

    public static enum Transforms implements IValueEnum {
        Linear(0),
        Absolute(2); // output = square root ((input value)^2) or output = output * sgn(output)

        @Override
        public int getValue() {
            return value;
        }

        private Transforms(int value) {
            this.value = value;
        }

        private final int value;
    };

    public static enum Type {

        RIFF, LIST;
    };

    public static enum SubType {

        sfbk, INFO, ifil, iver, INAM, isng, irom, IPRD, IENG, ISFT, ICRD, ICMT,
        ICOP, sdta, smpl, pdta, phdr, pbag, pmod, pgen, inst, ibag, imod, igen, shdr;
    };

    protected Type type = null;
    protected SubType subType = null;
    protected long size; // in bytes
    protected final List data = new ArrayList<>();

    protected Map<SubType, SFChunk> children = new HashMap<>();

    public SFChunk(ISeekableResourceReader file) throws IOException {
        IResourceChunkReader fileReader = file.readChunk(8);
        String code = fileReader.readString(4);
        size = fileReader.readUnsignedIntegerAsLong();
        long pointer = file.getFilePointer();
        //data = new byte[(int)size];
        //file.read(data);
        fileReader = file.readChunk((int) size);
        try {
            type = Type.valueOf(code);
            code = fileReader.readString(4);
            subType = SubType.valueOf(code);

            while (!isEOC(file, pointer)) {
                // this is container for other types
                SFChunk child = new SFChunk(file);
                children.put(child.getSubType(), child);
            }

            if (file.getFilePointer() != pointer + size) {
                throw new RuntimeException("File pointer out of range. Expect "
                        + (pointer + size) + ", take " + file.getFilePointer());
            }

        } catch (IllegalArgumentException e) {
            try {
                subType = SubType.valueOf(code);
                switch (subType) {
                    case isng: // szSoundEngine : e.g. "EMU8000"
                    case irom: // szROM : e.g. "1MGM"
                    case INAM: // szName : e.g. "General MIDI"
                    case ICRD: // szDate : e.g. "July 15, 1997"
                    case IENG: // szName : e.g. "John Q. Sounddesigner"
                    case IPRD: // szProduct : e.g. "SBAWE64 Gold"
                    case ICOP: // szCopyright : e.g. "Copyright (c) 1997 E-mu Systems, Inc."
                    case ICMT: // szComment : e.g. "This is a comment"
                    case ISFT: // szTools : e.g. ":Preditor 2.00a:Vienna SF Studio 2.0:"
                        //data = new ArrayList<>();
                        while (!isEOC(file, pointer)) {
                            data.add(fileReader.readString((int) size));
                        }
                        break;

                    case smpl:
                        while (!isEOC(file, pointer)) {
                            data.add(fileReader.readShort());
                        }
                        //byte d = new byte[(int) size];
                        //file.read((byte[])data);
                        break;

                    case ifil:
                    case iver:
                        while (!isEOC(file, pointer)) {
                            data.add(new sfVersionTag(fileReader));
                        }
                        break;

                    case phdr:
                        while (!isEOC(file, pointer)) {
                            data.add(new sfPresetHeader(fileReader));
                        }
                        break;

                    case pbag:
                        while (!isEOC(file, pointer)) {
                            data.add(new sfPresetBag(fileReader));
                        }
                        break;

                    case pmod:
                        while (!isEOC(file, pointer)) {
                            data.add(new sfModList(fileReader));
                        }
                        break;

                    case pgen:
                        while (!isEOC(file, pointer)) {
                            data.add(new sfGenList(fileReader));
                        }
                        break;

                    case inst:
                        while (!isEOC(file, pointer)) {
                            data.add(new sfInst(fileReader));
                        }
                        break;

                    case ibag:
                        while (!isEOC(file, pointer)) {
                            data.add(new sfInstBag(fileReader));
                        }
                        break;

                    case imod:
                        while (!isEOC(file, pointer)) {
                            data.add(new sfInstModList(fileReader));
                        }
                        break;

                    case igen:
                        while (!isEOC(file, pointer)) {
                            data.add(new sfInstGenList(fileReader));
                        }
                        break;

                    case shdr:
                        while (!isEOC(file, pointer)) {
                            data.add(new sfSample(fileReader));
                        }
                        break;
                }
            } catch (IllegalArgumentException ex) {
                throw new RuntimeException("Chunk code " + code + " not supported");
            }
        }
    }

    public Type getType() {
        return type;
    }

    public SubType getSubType() {
        return subType;
    }

    public Map<SubType, SFChunk> getChilds() {
        return children;
    }

    /**
     * Check end of chunk
     *
     * @param file file to read
     * @param chunkPointer current chunk start pointer
     * @return true if file pointer >= chunk end pointer
     * @throws IOException
     */
    private boolean isEOC(ISeekableResourceReader file, long chunkPointer) throws IOException {
        return file.getFilePointer() >= (size + chunkPointer);
    }

    @Override
    public String toString() {
        String result = "Sf2Chunk{" + "size=" + size;
        if (type != null) {
            result += ", type=" + type;
        }
        if (subType != null) {
            result += ", subType=" + subType;
        }
        if (!data.isEmpty()) {
            result += ", data=" + data;
        }
        if (!children.isEmpty()) {
            result += ", childs=" + children;
        }
        result += '}';

        return result;
    }

    protected class sfVersionTag { // <iver-rec>

        protected int major;
        protected int minor;

        protected sfVersionTag(IResourceChunkReader file) throws IOException {
            major = file.readShort();
            minor = file.readShort();
        }

        @Override
        public String toString() {
            return "sfVersionTag{" + "major=" + major + ", minor=" + minor + '}';
        }
    }

    protected class sfPresetHeader { // <phdr-rec>

        protected String achPresetName;
        protected int wPreset;
        protected int wBank;
        protected int wPresetBagNdx;
        protected long dwLibrary;
        protected long dwGenre;
        protected long dwMorphology;

        protected sfPresetHeader(IResourceChunkReader file) throws IOException {
            achPresetName = file.readVaryingLengthString(20);
            wPreset = file.readUnsignedShort();
            wBank = file.readUnsignedShort();
            wPresetBagNdx = file.readUnsignedShort();
            dwLibrary = file.readUnsignedIntegerAsLong();
            dwGenre = file.readUnsignedIntegerAsLong();
            dwMorphology = file.readUnsignedIntegerAsLong();
        }

        @Override
        public String toString() {
            return "sfPresetHeader{" + "achPresetName=" + achPresetName
                    + ", wPreset=" + wPreset + ", wBank=" + wBank
                    + ", wPresetBagNdx=" + wPresetBagNdx + ", dwLibrary=" + dwLibrary
                    + ", dwGenre=" + dwGenre + ", dwMorphology=" + dwMorphology + '}';
        }
    }

    protected class sfPresetBag { // <pbag-rec>

        protected int wGenNdx;
        protected int wModNdx;

        protected sfPresetBag(IResourceChunkReader file) throws IOException {
            wGenNdx = file.readUnsignedShort();
            wModNdx = file.readUnsignedShort();
        }

        @Override
        public String toString() {
            return "sfPresetBag{" + "wGenNdx=" + wGenNdx + ", wModNdx=" + wModNdx + '}';
        }
    }

    protected class sfInst { // <inst-rec>

        protected String achInstName;
        protected int wInstBagNdx;

        protected sfInst(IResourceChunkReader file) throws IOException {
            achInstName = file.readVaryingLengthString(20);
            wInstBagNdx = file.readUnsignedShort();
        }

        @Override
        public String toString() {
            return "sfInst{" + "achInstName=" + achInstName + ", wInstBagNdx=" + wInstBagNdx + '}';
        }
    }

    protected class sfInstBag { // <ibag-rec>

        protected int wInstGenNdx;
        protected int wInstModNdx;

        protected sfInstBag(IResourceChunkReader file) throws IOException {
            wInstGenNdx = file.readUnsignedShort();
            wInstModNdx = file.readUnsignedShort();
        }

        @Override
        public String toString() {
            return "sfInstBag{" + "wInstGenNdx=" + wInstGenNdx + ", wInstModNdx=" + wInstModNdx + '}';
        }
    }

    protected class sfSample { // <shdr-rec>

        protected String achSampleName;
        protected long dwStart;
        protected long dwEnd;
        protected long dwStartloop;
        protected long dwEndloop;
        protected long dwSampleRate;
        protected short byOriginalKey;
        protected byte chCorrection; // CHAR
        protected int wSampleLink;
        protected SFSampleLink sfSampleType;

        protected sfSample(IResourceChunkReader file) throws IOException {
            achSampleName = file.readVaryingLengthString(20);
            dwStart = file.readUnsignedIntegerAsLong();
            dwEnd = file.readUnsignedIntegerAsLong();
            dwStartloop = file.readUnsignedIntegerAsLong();
            dwEndloop = file.readUnsignedIntegerAsLong();
            dwSampleRate = file.readUnsignedIntegerAsLong();
            byOriginalKey = file.readUnsignedByte();
            chCorrection = file.readByte();
            wSampleLink = file.readUnsignedShort();
            sfSampleType = file.readShortAsEnum(SFSampleLink.class);
        }

        @Override
        public String toString() {
            return "sfSample{" + "achSampleName=" + achSampleName + ", dwStart=" + dwStart
                    + ", dwEnd=" + dwEnd + ", dwStartloop=" + dwStartloop
                    + ", dwEndloop=" + dwEndloop + ", dwSampleRate=" + dwSampleRate
                    + ", byOriginalKey=" + byOriginalKey + ", chCorrection=" + chCorrection
                    + ", wSampleLink=" + wSampleLink + ", sfSampleType=" + sfSampleType + '}';
        }
    }

    protected class rangesType {

        protected short byLo;
        protected short byHi;

        protected rangesType(IResourceChunkReader file) throws IOException {
            byLo = file.readUnsignedByte();
            byHi = file.readUnsignedByte();
        }
    }

    protected class genAmountType {

        protected rangesType ranges;
        protected short shAmount;
        protected int wAmount;

        protected genAmountType(IResourceChunkReader file) throws IOException {
            ranges = new rangesType(file);
            shAmount = file.readShort();
            wAmount = file.readUnsignedShort();
        }
    }

    protected class SFModulator {

        protected Modulators bIndex = null; // A 7 bit value specifying the controller source
        protected boolean cc; // MIDI Continuous Controller Flag
        protected boolean d; // Direction
        protected boolean p; // Polarity
        protected ModulatorTypes bType = null; // A 6 bit value specifying the continuity of the controller

        protected SFModulator(IResourceChunkReader file) throws IOException {
            int bits = file.readUnsignedShort();

            bIndex = ConversionUtils.parseEnum(ConversionUtils.bits(bits, 0, 7), Modulators.class);
            cc = ConversionUtils.bits(bits, 7, 1) == 1;
            d = ConversionUtils.bits(bits, 8, 1) == 1;
            p = ConversionUtils.bits(bits, 9, 1) == 1;
            bType = ConversionUtils.parseEnum(ConversionUtils.bits(bits, 10, 6), ModulatorTypes.class);
        }
    }

    protected class SFGenerator {

        protected Generators bIndex = null;
        protected boolean cc;
        protected boolean d;
        protected boolean p;
        protected byte bType;

        protected SFGenerator(IResourceChunkReader file) throws IOException {
            int bits = file.readUnsignedShort();

            bIndex = ConversionUtils.parseEnum(ConversionUtils.bits(bits, 0, 7), Generators.class);
            cc = ConversionUtils.bits(bits, 7, 1) == 1;
            d = ConversionUtils.bits(bits, 8, 1) == 1;
            p = ConversionUtils.bits(bits, 9, 1) == 1;
            bType = (byte) ConversionUtils.bits(bits, 10, 6);
        }
    }

    protected class SFTransform {

        protected Transforms bIndex = null;
        protected boolean cc;
        protected boolean d;
        protected boolean p;
        protected byte bType;

        protected SFTransform(IResourceChunkReader file) throws IOException {
            int bits = file.readUnsignedShort();

            bIndex = ConversionUtils.parseEnum(ConversionUtils.bits(bits, 0, 7), Transforms.class);
            cc = ConversionUtils.bits(bits, 7, 1) == 1;
            d = ConversionUtils.bits(bits, 8, 1) == 1;
            p = ConversionUtils.bits(bits, 9, 1) == 1;
            bType = (byte) ConversionUtils.bits(bits, 10, 6);
        }
    }

    protected class sfModList { // <pmod-rec>

        protected SFModulator sfModSrcOper;
        protected SFGenerator sfModDestOper;
        short modAmount;
        protected SFModulator sfModAmtSrcOper;
        protected SFTransform sfModTransOper;

        protected sfModList(IResourceChunkReader file) throws IOException {
            sfModSrcOper = new SFModulator(file);
            sfModDestOper = new SFGenerator(file);
            modAmount = file.readShort();
            sfModAmtSrcOper = new SFModulator(file);
            sfModTransOper = new SFTransform(file);
        }

        @Override
        public String toString() {
            return "sfModList{" + "sfModSrcOper=" + sfModSrcOper
                    + ", sfModDestOper=" + sfModDestOper + ", modAmount=" + modAmount
                    + ", sfModAmtSrcOper=" + sfModAmtSrcOper + ", sfModTransOper=" + sfModTransOper + '}';
        }
    }

    protected class sfGenList { // <pgen-rec>

        protected SFGenerator sfGenOper;
        protected genAmountType genAmount;

        protected sfGenList(IResourceChunkReader file) throws IOException {
            sfGenOper = new SFGenerator(file);
            genAmount = new genAmountType(file);
        }

        @Override
        public String toString() {
            return "sfGenList{" + "sfGenOper=" + sfGenOper + ", genAmount=" + genAmount + '}';
        }
    }

    protected class sfInstModList { // <imod-rec>

        protected SFModulator sfModSrcOper;
        protected SFGenerator sfModDestOper;
        protected short modAmount;
        protected SFModulator sfModAmtSrcOper;
        protected SFTransform sfModTransOper;

        protected sfInstModList(IResourceChunkReader file) throws IOException {
            sfModSrcOper = new SFModulator(file);
            sfModDestOper = new SFGenerator(file);
            modAmount = file.readShort();
            sfModAmtSrcOper = new SFModulator(file);
            sfModTransOper = new SFTransform(file);
        }

        @Override
        public String toString() {
            return "sfInstModList{" + "sfModSrcOper=" + sfModSrcOper
                    + ", sfModDestOper=" + sfModDestOper + ", modAmount=" + modAmount
                    + ", sfModAmtSrcOper=" + sfModAmtSrcOper + ", sfModTransOper=" + sfModTransOper + '}';
        }
    }

    protected class sfInstGenList { // <igen-rec>

        protected SFGenerator sfGenOper;
        protected genAmountType genAmount;

        protected sfInstGenList(IResourceChunkReader file) throws IOException {
            sfGenOper = new SFGenerator(file);
            genAmount = new genAmountType(file);
        }

        @Override
        public String toString() {
            return "sfInstGenList{" + "sfGenOper=" + sfGenOper + ", genAmount=" + genAmount + '}';
        }
    }
}
