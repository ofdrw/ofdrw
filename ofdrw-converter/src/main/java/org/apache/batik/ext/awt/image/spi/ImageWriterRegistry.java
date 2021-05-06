/*

   Licensed to the Apache Software Foundation (ASF) under one or more
   contributor license agreements.  See the NOTICE file distributed with
   this work for additional information regarding copyright ownership.
   The ASF licenses this file to You under the Apache License, Version 2.0
   (the "License"); you may not use this file except in compliance with
   the License.  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.

 */
package org.apache.batik.ext.awt.image.spi;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.batik.util.Service;

/**
 *
 * @version $Id: ImageWriterRegistry.java 1808977 2017-09-20 09:06:07Z ssteiner $
 */
public final class ImageWriterRegistry {

    private static ImageWriterRegistry instance;

    private final Map imageWriterMap = new HashMap();

    private ImageWriterRegistry() {
        setup();
    }

    public static ImageWriterRegistry getInstance() {
        synchronized( ImageWriterRegistry.class ){
            if ( instance == null ){
                instance = new ImageWriterRegistry();
            }
            return instance;
        }
    }

    private void setup() {
        Iterator iter = Service.providers(org.apache.xmlgraphics.image.writer.ImageWriter.class);
        while (iter.hasNext()) {
            ImageWriter writer = (ImageWriter)iter.next();
            // System.out.println("RE: " + writer);
            register(writer);
        }
    }

    public void register(ImageWriter writer) {
        imageWriterMap.put(writer.getMIMEType(), writer);
    }

    /**
     * get the ImageWriter registered for mime, or null.
     * @param mime used for lookup
     * @return the registered ImageWriter (maybe null)
     */
    public ImageWriter getWriterFor(String mime) {
        return (ImageWriter)imageWriterMap.get(mime);
    }

}
