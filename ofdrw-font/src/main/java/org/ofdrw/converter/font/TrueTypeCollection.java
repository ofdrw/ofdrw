/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ofdrw.converter.font;


import java.io.IOException;
import java.util.function.Consumer;

/**
 * A TrueType Collection, now more properly known as a "Font Collection" as it may contain either
 * TrueType or OpenType fonts.
 *
 * @author John Hewson
 */
public class TrueTypeCollection {
    /**
     * Stream 由外部关闭
     */
    private TTFDataStream stream;


    private int numFonts;
    private long[] fontOffsets;

    public TrueTypeCollection() {
    }

    /**
     * Creates a new TrueTypeCollection from a TTC stream.
     *
     * @param stream The TTF file.
     * @throws IOException If the font could not be parsed.
     * @return this
     */
    public TrueTypeCollection parse(TTFDataStream stream) throws IOException {
        this.stream = stream;
        // TTC header
        String tag = stream.readTag();
        if (!tag.equals("ttcf")) {
            throw new IOException("Missing TTC header");
        }
        float version = stream.read32Fixed();
        numFonts = (int) stream.readUnsignedInt();
        fontOffsets = new long[numFonts];
        for (int i = 0; i < numFonts; i++) {
            fontOffsets[i] = stream.readUnsignedInt();
        }
        if (version >= 2) {
            // not used at this time
            int ulDsigTag = stream.readUnsignedShort();
            int ulDsigLength = stream.readUnsignedShort();
            int ulDsigOffset = stream.readUnsignedShort();
        }
        return this;
    }

    /**
     * 通过字体索引获取字体
     *
     * @param idx 索引号
     * @return 字体
     * @throws IOException IOE
     */
    public TrueTypeFont getFontAtIndex(int idx) throws IOException {
        if (idx < 0 || idx >= numFonts) {
            throw new IllegalArgumentException("索引号 不存在"+ idx);
        }
        stream.seek(fontOffsets[idx]);
        return new TrueTypeFont().parse(stream);
    }

    /**
     * 遍历字体集合中的所有字体
     *
     * @param cn 字体集合
     * @throws IOException IOE
     */
    public void foreach(Consumer<TrueTypeFont> cn) throws IOException {
        for (int i = 0; i < numFonts; i++) {
            TrueTypeFont font = getFontAtIndex(i);
            cn.accept(font);
        }
    }

    /**
     * 字体总数
     *
     * @return 总数
     */
    public int getNumFonts() {
        return numFonts;
    }
}
