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

import org.apache.fontbox.ttf.HorizontalHeaderTable;
import org.apache.fontbox.ttf.TTFTable;
import org.apache.fontbox.ttf.TrueTypeFont;

import java.io.IOException;

/**
 * A table in a true type font.
 *
 * @author Ben Litchfield
 */
public class HorizontalMetricsTable {

    private int[] advanceWidth;
    private short[] leftSideBearing;
    private short[] nonHorizontalLeftSideBearing;
    private int numHMetrics;
    private long[] offset;

    public HorizontalMetricsTable(long[] offset) {
        this.offset = offset;
    }


    public HorizontalMetricsTable parse(int numHMetrics, int numGlyphs, TTFDataStream data) throws IOException {
        data.seek(offset[0]);
        this.numHMetrics = numHMetrics;

        int bytesRead = 0;
        advanceWidth = new int[numHMetrics];
        leftSideBearing = new short[numHMetrics];
        for (int i = 0; i < numHMetrics; i++) {
            advanceWidth[i] = data.readUnsignedShort();
            leftSideBearing[i] = data.readSignedShort();
            bytesRead += 4;
        }

        int numberNonHorizontal = numGlyphs - numHMetrics;

        // handle bad fonts with too many hmetrics
        if (numberNonHorizontal < 0) {
            numberNonHorizontal = numGlyphs;
        }

        // make sure that table is never null and correct size, even with bad fonts that have no
        // "leftSideBearing" table although they should
        nonHorizontalLeftSideBearing = new short[numberNonHorizontal];

        if (bytesRead < getLength()) {
            for (int i = 0; i < numberNonHorizontal; i++) {
                if (bytesRead < getLength()) {
                    nonHorizontalLeftSideBearing[i] = data.readSignedShort();
                    bytesRead += 2;
                }
            }
        }
        return this;
    }

    private long getLength() {
        return offset[1];
    }

    /**
     * Returns the advance width for the given GID.
     *
     * @param gid GID
     * @return  advance width for the given GID.
     */
    public int getAdvanceWidth(int gid) {
        if (gid < numHMetrics) {
            return advanceWidth[gid];
        } else {
            // monospaced fonts may not have a width for every glyph
            // the last one is for subsequent glyphs
            return advanceWidth[advanceWidth.length - 1];
        }
    }

    /**
     * Returns the left side bearing for the given GID.
     *
     * @param gid GID
     * @return left side bearing for the given GID.
     */
    public int getLeftSideBearing(int gid) {
        if (gid < numHMetrics) {
            return leftSideBearing[gid];
        } else {
            return nonHorizontalLeftSideBearing[gid - numHMetrics];
        }
    }
}
