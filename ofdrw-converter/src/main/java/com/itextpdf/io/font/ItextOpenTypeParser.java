/**
 * 源码来自于Itext 7.1.13 的OpenTypeParser类</br>
 * 增加的代码使用注释标识了"代码增加" 修改的代码使用注释标识了"代码修改" 删掉的代码使用注释标识了"代码删除"
 */
package com.itextpdf.io.font;

import com.itextpdf.io.IOException;

import java.io.Closeable;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

class ItextOpenTypeParser extends OpenTypeParser implements Serializable, Closeable {

    private static final long serialVersionUID = 3399061674525229738L;

    public ItextOpenTypeParser(byte[] ttf) throws java.io.IOException {
        super(ttf);
    }

    public ItextOpenTypeParser(byte[] ttc, int ttcIndex) throws java.io.IOException {
        super(ttc, ttcIndex);
    }

    public ItextOpenTypeParser(String ttcPath, int ttcIndex) throws java.io.IOException {
        super(ttcPath, ttcIndex);
    }

    public ItextOpenTypeParser(String name) throws java.io.IOException {
        super(name);
    }

    @Override
    public String getPsFontName() {
        try {
            return super.getPsFontName();
        } catch (Exception e) {
            return "Font";
        }
    }

    @Override
    protected void loadTables(boolean all) throws java.io.IOException {
        readNameTable();
        readHeadTable();
        readOs_2Table();
        readPostTable();
        if (all) {
            checkCff();
            readHheaTable();
            readGlyphWidths();
            readCmapTable();
        }
    }

    /**
     * Extracts the names of the font in all the languages available.
     *
     * @throws IOException on error
     * @throws java.io.IOException on error
     */
    private void readNameTable() throws java.io.IOException {
        int[] table_location = tables.get("name");
        if (table_location == null) {
            /*********** 代码删除 ****************/
            /***********************************
             * if (fileName != null) { throw new IOException(IOException.TableDoesNotExistsIn).setMessageParams("name",
             * fileName); } else { throw new IOException(IOException.TableDoesNotExist).setMessageParams("name"); }
             ***********************************/
        }
        allNameEntries = new LinkedHashMap<>();
        /*********** 代码修改 ****************/
        /**********************************/
        if (table_location != null) {
            raf.seek(table_location[0] + 2);
        }
        /**********************************/
        int numRecords = raf.readUnsignedShort();
        int startOfStorage = raf.readUnsignedShort();
        for (int k = 0; k < numRecords; ++k) {
            int platformID = raf.readUnsignedShort();
            int platformEncodingID = raf.readUnsignedShort();
            int languageID = raf.readUnsignedShort();
            int nameID = raf.readUnsignedShort();
            int length = raf.readUnsignedShort();
            int offset = raf.readUnsignedShort();
            List<String[]> names;
            if (allNameEntries.containsKey(nameID)) {
                names = allNameEntries.get(nameID);
            } else {
                allNameEntries.put(nameID, names = new ArrayList<>());
            }
            int pos = (int)raf.getPosition();
            /*********** 代码修改 ****************/
            /**********************************/
            if (table_location != null) {
                raf.seek(table_location[0] + startOfStorage + offset);
            }
            /**********************************/
            String name;
            if (platformID == 0 || platformID == 3 || platformID == 2 && platformEncodingID == 1) {
                name = readUnicodeString(length);
            } else {
                name = readStandardString(length);
            }
            names.add(new String[] {Integer.toString(platformID), Integer.toString(platformEncodingID),
                Integer.toString(languageID), name});
            raf.seek(pos);
        }
    }

    /**
     * Read horizontal header, table 'hhea'.
     *
     * @throws IOException the font is invalid.
     * @throws java.io.IOException the font file could not be read.
     */
    private void readHheaTable() throws java.io.IOException {
        int[] table_location = tables.get("hhea");
        if (table_location == null) {
            if (fileName != null) {
                throw new IOException(IOException.TableDoesNotExistsIn).setMessageParams("hhea", fileName);
            } else {
                throw new IOException(IOException.TableDoesNotExist).setMessageParams("hhea");
            }
        }
        raf.seek(table_location[0] + 4);
        hhea = new HorizontalHeader();
        hhea.Ascender = raf.readShort();
        hhea.Descender = raf.readShort();
        hhea.LineGap = raf.readShort();
        hhea.advanceWidthMax = raf.readUnsignedShort();
        hhea.minLeftSideBearing = raf.readShort();
        hhea.minRightSideBearing = raf.readShort();
        hhea.xMaxExtent = raf.readShort();
        hhea.caretSlopeRise = raf.readShort();
        hhea.caretSlopeRun = raf.readShort();
        raf.skipBytes(12);
        hhea.numberOfHMetrics = raf.readUnsignedShort();
    }

    /**
     * Read font header, table 'head'.
     *
     * @throws IOException the font is invalid.
     * @throws java.io.IOException the font file could not be read.
     */
    private void readHeadTable() throws java.io.IOException {
        int[] table_location = tables.get("head");
        if (table_location == null) {
            if (fileName != null) {
                throw new IOException(IOException.TableDoesNotExistsIn).setMessageParams("head", fileName);
            } else {
                throw new IOException(IOException.TableDoesNotExist).setMessageParams("head");
            }
        }
        raf.seek(table_location[0] + 16);
        head = new HeaderTable();
        head.flags = raf.readUnsignedShort();
        head.unitsPerEm = raf.readUnsignedShort();
        raf.skipBytes(16);
        head.xMin = raf.readShort();
        head.yMin = raf.readShort();
        head.xMax = raf.readShort();
        head.yMax = raf.readShort();
        head.macStyle = raf.readUnsignedShort();
    }

    /**
     * Reads the windows metrics table. The metrics are extracted from the table 'OS/2'. Depends on
     * {@link HeaderTable#unitsPerEm} property.
     *
     * @throws IOException the font is invalid.
     * @throws java.io.IOException the font file could not be read.
     */
    private void readOs_2Table() throws java.io.IOException {
        int[] table_location = tables.get("OS/2");
        /*********** 代码增加 ****************/
        /**********************************/
        if (table_location == null) {
            table_location = tables.get("os/2");
        }
        /**********************************/
        if (table_location == null) {
            if (fileName != null) {
                throw new IOException(IOException.TableDoesNotExistsIn).setMessageParams("os/2", fileName);
            } else {
                throw new IOException(IOException.TableDoesNotExist).setMessageParams("os/2");
            }
        }
        os_2 = new WindowsMetrics();
        raf.seek(table_location[0]);
        int version = raf.readUnsignedShort();
        os_2.xAvgCharWidth = raf.readShort();
        os_2.usWeightClass = raf.readUnsignedShort();
        os_2.usWidthClass = raf.readUnsignedShort();
        os_2.fsType = raf.readShort();
        os_2.ySubscriptXSize = raf.readShort();
        os_2.ySubscriptYSize = raf.readShort();
        os_2.ySubscriptXOffset = raf.readShort();
        os_2.ySubscriptYOffset = raf.readShort();
        os_2.ySuperscriptXSize = raf.readShort();
        os_2.ySuperscriptYSize = raf.readShort();
        os_2.ySuperscriptXOffset = raf.readShort();
        os_2.ySuperscriptYOffset = raf.readShort();
        os_2.yStrikeoutSize = raf.readShort();
        os_2.yStrikeoutPosition = raf.readShort();
        os_2.sFamilyClass = raf.readShort();
        raf.readFully(os_2.panose);
        raf.skipBytes(16);
        raf.readFully(os_2.achVendID);
        os_2.fsSelection = raf.readUnsignedShort();
        os_2.usFirstCharIndex = raf.readUnsignedShort();
        os_2.usLastCharIndex = raf.readUnsignedShort();
        os_2.sTypoAscender = raf.readShort();
        os_2.sTypoDescender = raf.readShort();
        if (os_2.sTypoDescender > 0) {
            os_2.sTypoDescender = (short)-os_2.sTypoDescender;
        }
        os_2.sTypoLineGap = raf.readShort();
        os_2.usWinAscent = raf.readUnsignedShort();
        os_2.usWinDescent = raf.readUnsignedShort();
        if (os_2.usWinDescent > 0) {
            os_2.usWinDescent = (short)-os_2.usWinDescent;
        }
        os_2.ulCodePageRange1 = 0;
        os_2.ulCodePageRange2 = 0;
        if (version > 0) {
            os_2.ulCodePageRange1 = raf.readInt();
            os_2.ulCodePageRange2 = raf.readInt();
        }
        if (version > 1) {
            // todo os_2.sxHeight = raf.readShort();
            raf.skipBytes(2);
            os_2.sCapHeight = raf.readShort();
        } else {
            os_2.sCapHeight = (int)(0.7 * head.unitsPerEm);
        }
    }

    private void readPostTable() throws java.io.IOException {
        int[] table_location = tables.get("post");
        if (table_location != null) {
            raf.seek(table_location[0] + 4);
            short mantissa = raf.readShort();
            int fraction = raf.readUnsignedShort();
            post = new PostTable();
            post.italicAngle = (float)(mantissa + fraction / 16384.0d);
            post.underlinePosition = raf.readShort();
            post.underlineThickness = raf.readShort();
            post.isFixedPitch = raf.readInt() != 0;
        } else {
            post = new PostTable();
            /*********** 代码修改 ****************/
            /**********************************/
            if (null != hhea) {
                post.italicAngle = (float)(-Math.atan2(hhea.caretSlopeRun, hhea.caretSlopeRise) * 180 / Math.PI);
            }
            /**********************************/
        }
    }

    /**
     * Reads the several maps from the table 'cmap'. The maps of interest are 1.0 for symbolic fonts and 3.1 for all
     * others. A symbolic font is defined as having the map 3.0. Depends from {@code readGlyphWidths()}.
     *
     * @throws java.io.IOException the font file could not be read
     */
    private void readCmapTable() throws java.io.IOException {
        int[] table_location = tables.get("cmap");
        /*********** 代码修改 ****************/
        if (table_location == null) {
            // if (fileName != null) {
            // throw new IOException(IOException.TableDoesNotExistsIn).setMessageParams("cmap", fileName);
            // } else {
            // throw new IOException(IOException.TableDoesNotExist).setMessageParams("cmap");
            // }
        } else {
            raf.seek(table_location[0]);
        }

        raf.skipBytes(2);
        int num_tables = raf.readUnsignedShort();
        int map10 = 0;
        int map31 = 0;
        int map30 = 0;
        int mapExt = 0;
        cmaps = new CmapTable();
        for (int k = 0; k < num_tables; ++k) {
            int platId = raf.readUnsignedShort();
            int platSpecId = raf.readUnsignedShort();
            int offset = raf.readInt();
            if (platId == 3 && platSpecId == 0) {
                cmaps.fontSpecific = true;
                map30 = offset;
            } else if (platId == 3 && platSpecId == 1) {
                map31 = offset;
            } else if (platId == 3 && platSpecId == 10) {
                mapExt = offset;
            } else if (platId == 1 && platSpecId == 0) {
                map10 = offset;
            }
        }
        if (map10 > 0) {
            raf.seek(table_location[0] + map10);
            int format = raf.readUnsignedShort();
            switch (format) {
                case 0:
                    cmaps.cmap10 = readFormat0();
                    break;
                case 4:
                    cmaps.cmap10 = readFormat4(false);
                    break;
                case 6:
                    cmaps.cmap10 = readFormat6();
                    break;
            }
        }
        if (map31 > 0) {
            raf.seek(table_location[0] + map31);
            int format = raf.readUnsignedShort();
            if (format == 4) {
                cmaps.cmap31 = readFormat4(false);
            }
        }
        if (map30 > 0) {
            raf.seek(table_location[0] + map30);
            int format = raf.readUnsignedShort();
            if (format == 4) {
                cmaps.cmap10 = readFormat4(cmaps.fontSpecific);
            } else {
                cmaps.fontSpecific = false;
            }
        }
        if (mapExt > 0) {
            raf.seek(table_location[0] + mapExt);
            int format = raf.readUnsignedShort();
            switch (format) {
                case 0:
                    cmaps.cmapExt = readFormat0();
                    break;
                case 4:
                    cmaps.cmapExt = readFormat4(false);
                    break;
                case 6:
                    cmaps.cmapExt = readFormat6();
                    break;
                case 12:
                    cmaps.cmapExt = readFormat12();
                    break;
            }
        }
        /*********** 代码增加 ****************/
        /**********************************/
        if (null == cmaps.cmapExt && null == cmaps.cmap31 && null == cmaps.cmap10) {
            cmaps.cmap10 = new HashMap<Integer, int[]>();
        }
        /***********************************/
    }

    /**
     * Reads a <CODE>String</CODE> from the font file as bytes using the Cp1252 encoding.
     *
     * @param length the length of bytes to read
     * @return the <CODE>String</CODE> read
     * @throws java.io.IOException the font file could not be read
     */
    private String readStandardString(int length) throws java.io.IOException {
        return raf.readString(length, PdfEncodings.WINANSI);
    }

    /**
     * Reads a Unicode <CODE>String</CODE> from the font file. Each character is represented by two bytes.
     *
     * @param length the length of bytes to read. The <CODE>String</CODE> will have <CODE>length</CODE>/2 characters.
     * @return the <CODE>String</CODE> read.
     * @throws java.io.IOException the font file could not be read.
     */
    private String readUnicodeString(int length) throws java.io.IOException {
        StringBuilder buf = new StringBuilder();
        length /= 2;
        for (int k = 0; k < length; ++k) {
            buf.append(raf.readChar());
        }
        return buf.toString();
    }

    /**
     * The information in the maps of the table 'cmap' is coded in several formats. Format 0 is the Apple standard
     * character to glyph index mapping table.
     *
     * @return a <CODE>HashMap</CODE> representing this map
     * @throws java.io.IOException the font file could not be read
     */
    private Map<Integer, int[]> readFormat0() throws java.io.IOException {
        Map<Integer, int[]> h = new LinkedHashMap<>();
        raf.skipBytes(4);
        for (int k = 0; k < 256; ++k) {
            int[] r = new int[2];
            r[0] = raf.readUnsignedByte();
            r[1] = getGlyphWidth(r[0]);
            h.put(k, r);
        }
        return h;
    }

    /**
     * The information in the maps of the table 'cmap' is coded in several formats. Format 4 is the Microsoft standard
     * character to glyph index mapping table.
     *
     * @return a <CODE>HashMap</CODE> representing this map
     * @throws java.io.IOException the font file could not be read
     */
    private Map<Integer, int[]> readFormat4(boolean fontSpecific) throws java.io.IOException {
        Map<Integer, int[]> h = new LinkedHashMap<>();
        int table_lenght = raf.readUnsignedShort();
        raf.skipBytes(2);
        int segCount = raf.readUnsignedShort() / 2;
        raf.skipBytes(6);
        int[] endCount = new int[segCount];
        for (int k = 0; k < segCount; ++k) {
            endCount[k] = raf.readUnsignedShort();
        }
        raf.skipBytes(2);
        int[] startCount = new int[segCount];
        for (int k = 0; k < segCount; ++k) {
            startCount[k] = raf.readUnsignedShort();
        }
        int[] idDelta = new int[segCount];
        for (int k = 0; k < segCount; ++k) {
            idDelta[k] = raf.readUnsignedShort();
        }
        int[] idRO = new int[segCount];
        for (int k = 0; k < segCount; ++k) {
            idRO[k] = raf.readUnsignedShort();
        }
        int[] glyphId = new int[table_lenght / 2 - 8 - segCount * 4];
        for (int k = 0; k < glyphId.length; ++k) {
            glyphId[k] = raf.readUnsignedShort();
        }
        for (int k = 0; k < segCount; ++k) {
            int glyph;
            for (int j = startCount[k]; j <= endCount[k] && j != 0xFFFF; ++j) {
                if (idRO[k] == 0) {
                    glyph = j + idDelta[k] & 0xFFFF;
                } else {
                    int idx = k + idRO[k] / 2 - segCount + j - startCount[k];
                    if (idx >= glyphId.length)
                        continue;
                    glyph = glyphId[idx] + idDelta[k] & 0xFFFF;
                }
                int[] r = new int[2];
                r[0] = glyph;
                r[1] = getGlyphWidth(r[0]);

                // (j & 0xff00) == 0xf000) means, that it is private area of unicode
                // So, in case symbol font (cmap 3/0) we add both char codes:
                // j & 0xff and j. It will simplify unicode conversion in TrueTypeFont
                if (fontSpecific && ((j & 0xff00) == 0xf000)) {
                    h.put(j & 0xff, r);
                }
                h.put(j, r);
            }
        }
        return h;
    }

    /**
     * The information in the maps of the table 'cmap' is coded in several formats. Format 6 is a trimmed table mapping.
     * It is similar to format 0 but can have less than 256 entries.
     *
     * @return a <CODE>HashMap</CODE> representing this map
     * @throws java.io.IOException the font file could not be read
     */
    private Map<Integer, int[]> readFormat6() throws java.io.IOException {
        Map<Integer, int[]> h = new LinkedHashMap<>();
        raf.skipBytes(4);
        int start_code = raf.readUnsignedShort();
        int code_count = raf.readUnsignedShort();
        for (int k = 0; k < code_count; ++k) {
            int[] r = new int[2];
            r[0] = raf.readUnsignedShort();
            r[1] = getGlyphWidth(r[0]);
            h.put(k + start_code, r);
        }
        return h;
    }

    private Map<Integer, int[]> readFormat12() throws java.io.IOException {
        Map<Integer, int[]> h = new LinkedHashMap<>();
        raf.skipBytes(2);
        @SuppressWarnings("unused")
        int table_length = raf.readInt();
        raf.skipBytes(4);
        int nGroups = raf.readInt();
        for (int k = 0; k < nGroups; k++) {
            int startCharCode = raf.readInt();
            int endCharCode = raf.readInt();
            int startGlyphID = raf.readInt();
            for (int i = startCharCode; i <= endCharCode; i++) {
                int[] r = new int[2];
                r[0] = startGlyphID;
                r[1] = getGlyphWidth(r[0]);
                h.put(i, r);
                startGlyphID++;
            }
        }
        return h;
    }
}
