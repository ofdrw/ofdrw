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
package org.apache.batik.svggen;

import java.awt.AlphaComposite;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.batik.ext.awt.g2d.GraphicContext;
import org.w3c.dom.Element;

/**
 * Utility class that converts an AlphaComposite object into
 * a set of SVG properties and definitions. Here is
 * how AlphaComposites are mapped to SVG:
 * + AlphaComposite.SRC_OVER with extra alpha is mapped
 *   to the opacity attribute
 * + AlphaComposite's other rules are translated into
 *   predefined filter effects.
 * One of the big differences between AlphaComposite and
 * the SVG feComposite filter is that feComposite does not
 * have the notion of extra alpha applied to the source.
 * The extra alpha equivalent is obtained by setting the
 * opacity property on the nodes to be composited.
 *
 * @author <a href="mailto:vincent.hardy@eng.sun.com">Vincent Hardy</a>
 * @version $Id: SVGAlphaComposite.java 1808888 2017-09-19 14:22:11Z ssteiner $
 * @see                org.apache.batik.svggen.SVGAlphaComposite
 */
public class SVGAlphaComposite extends AbstractSVGConverter {
    /**
     * Map of all possible AlphaComposite filter equivalents
     */
    private Map compositeDefsMap = new HashMap();

    /**
     * By default, access to the background is not required.
     */
    private boolean backgroundAccessRequired = false;

    /**
     * @param generatorContext for use by SVGAlphaComposite to build Elements
     */
    public SVGAlphaComposite(SVGGeneratorContext generatorContext) {
        super(generatorContext);

        //
        // Initialize map of AlphaComposite filter definitions
        //
        compositeDefsMap.put(AlphaComposite.Src,
                compositeToSVG(AlphaComposite.Src));
        compositeDefsMap.put(AlphaComposite.SrcIn,
                compositeToSVG(AlphaComposite.SrcIn));
        compositeDefsMap.put(AlphaComposite.SrcOut,
                compositeToSVG(AlphaComposite.SrcOut));
        compositeDefsMap.put(AlphaComposite.DstIn,
                compositeToSVG(AlphaComposite.DstIn));
        compositeDefsMap.put(AlphaComposite.DstOut,
                compositeToSVG(AlphaComposite.DstOut));
        compositeDefsMap.put(AlphaComposite.DstOver,
                compositeToSVG(AlphaComposite.DstOver));
        compositeDefsMap.put(AlphaComposite.Clear,
                compositeToSVG(AlphaComposite.Clear));
    }

    /**
     * @return set of all AlphaComposite filter definitions
     */
    public List getAlphaCompositeFilterSet() {
        return new LinkedList(compositeDefsMap.values());
    }

    /**
     * @return true if background access is required for any
     *         of the converted AlphaComposite rules
     */
    public boolean requiresBackgroundAccess() {
        return backgroundAccessRequired;
    }

    /**
     * Converts part or all of the input GraphicContext into
     * a set of attribute/value pairs and related definitions
     *
     * @param gc GraphicContext to be converted
     * @return descriptor of the attributes required to represent
     *         some or all of the GraphicContext state, along
     *         with the related definitions
     * @see org.apache.batik.svggen.SVGDescriptor
     */
    public SVGDescriptor toSVG(GraphicContext gc) {
        return toSVG((AlphaComposite)gc.getComposite());
    }

    /**
     * @param composite the AlphaComposite object to convert to SVG
     * @return an SVGCompositeDescriptor that defines how to map the
     *         input composite in SVG
     */
    public SVGCompositeDescriptor toSVG(AlphaComposite composite) {
        SVGCompositeDescriptor compositeDesc =
                (SVGCompositeDescriptor)descMap.get(composite);

        if(compositeDesc == null){
            // Process the composite opacity
            String opacityValue = doubleString(composite.getAlpha());

            // For all rules different than SRC_OVER, a filter is
            // needed to represent the composition rule.
            String filterValue = null;
            Element filterDef = null;
            if(composite.getRule() != AlphaComposite.SRC_OVER && composite.getRule() != AlphaComposite.SRC_ATOP){
                // Note that the extra alpha is ignored by using the
                // majorComposite. The extra alpha is already represented
                // by the SVG_OPACITY_ATTRIBUTE value.
                AlphaComposite majorComposite =
                        AlphaComposite.getInstance(composite.getRule());
                filterDef = (Element)compositeDefsMap.get(majorComposite);
                defSet.add(filterDef);

                System.out.println(composite.getRule());
                // Process the filter value
                StringBuffer filterAttrBuf = new StringBuffer(URL_PREFIX);
                filterAttrBuf.append(SIGN_POUND);
                filterAttrBuf.append(filterDef.getAttributeNS(null, SVG_ID_ATTRIBUTE));
                filterAttrBuf.append(URL_SUFFIX);

                filterValue = filterAttrBuf.toString();
            } else
                filterValue = SVG_NONE_VALUE;

            compositeDesc = new SVGCompositeDescriptor(opacityValue, filterValue,
                    filterDef);

            descMap.put(composite, compositeDesc);
        }

        if (composite.getRule() != AlphaComposite.SRC_OVER)
            backgroundAccessRequired = true;

        return compositeDesc;
    }

    /**
     * @param composite AlphaComposite to convert to a filter effect
     * @exception Error if an AlphaComposite with SRC_OVER rule in passed to
     *            this method.
     */
    private Element compositeToSVG(AlphaComposite composite) {
        // operator is equivalent to rule
        String operator = null;

        // input1 is equivalent to Src
        String input1 = null;

        // input2 is equivalent to Dst
        String input2 = null;

        // k2 is used only for arithmetic
        // to obtain the equivalent of SRC
        String k2 = "0";

        // ID used to identify the composite
        String id = null;

        switch(composite.getRule()){
            case AlphaComposite.CLEAR:
                operator = SVG_ARITHMETIC_VALUE;
                input1 = SVG_SOURCE_GRAPHIC_VALUE;
                input2 = SVG_BACKGROUND_IMAGE_VALUE;
                id = ID_PREFIX_ALPHA_COMPOSITE_CLEAR;
                break;
            case AlphaComposite.SRC:
                operator = SVG_ARITHMETIC_VALUE;
                input1 = SVG_SOURCE_GRAPHIC_VALUE;
                input2 = SVG_BACKGROUND_IMAGE_VALUE;
                id = ID_PREFIX_ALPHA_COMPOSITE_SRC;
                k2 = SVG_DIGIT_ONE_VALUE;
                break;
            case AlphaComposite.SRC_IN:
                operator = SVG_IN_VALUE;
                input1 = SVG_SOURCE_GRAPHIC_VALUE;
                input2 = SVG_BACKGROUND_IMAGE_VALUE;
                id = ID_PREFIX_ALPHA_COMPOSITE_SRC_IN;
                break;
            case AlphaComposite.SRC_OUT:
                operator = SVG_OUT_VALUE;
                input1 = SVG_SOURCE_GRAPHIC_VALUE;
                input2 = SVG_BACKGROUND_IMAGE_VALUE;
                id = ID_PREFIX_ALPHA_COMPOSITE_SRC_OUT;
                break;
            case AlphaComposite.DST_IN:
                operator = SVG_IN_VALUE;
                input2 = SVG_SOURCE_GRAPHIC_VALUE;
                input1 = SVG_BACKGROUND_IMAGE_VALUE;
                id = ID_PREFIX_ALPHA_COMPOSITE_DST_IN;
                break;
            case AlphaComposite.DST_OUT:
                operator = SVG_OUT_VALUE;
                input2 = SVG_SOURCE_GRAPHIC_VALUE;
                input1 = SVG_BACKGROUND_IMAGE_VALUE;
                id = ID_PREFIX_ALPHA_COMPOSITE_DST_OUT;
                break;
            case AlphaComposite.DST_OVER:
                operator = SVG_OVER_VALUE;
                input2 = SVG_SOURCE_GRAPHIC_VALUE;
                input1 = SVG_BACKGROUND_IMAGE_VALUE;
                id = ID_PREFIX_ALPHA_COMPOSITE_DST_OVER;
                break;
            default:
                throw new RuntimeException("invalid rule:" + composite.getRule() );
        }

        Element compositeFilter =
                generatorContext.domFactory.createElementNS(SVG_NAMESPACE_URI,
                        SVG_FILTER_TAG);
        compositeFilter.setAttributeNS(null, SVG_ID_ATTRIBUTE, id);
        compositeFilter.setAttributeNS(null, SVG_FILTER_UNITS_ATTRIBUTE,
                SVG_OBJECT_BOUNDING_BOX_VALUE);
        compositeFilter.setAttributeNS(null, SVG_X_ATTRIBUTE, SVG_ZERO_PERCENT_VALUE);
        compositeFilter.setAttributeNS(null, SVG_Y_ATTRIBUTE, SVG_ZERO_PERCENT_VALUE);
        compositeFilter.setAttributeNS(null, SVG_WIDTH_ATTRIBUTE,
                SVG_HUNDRED_PERCENT_VALUE);
        compositeFilter.setAttributeNS(null, SVG_HEIGHT_ATTRIBUTE,
                SVG_HUNDRED_PERCENT_VALUE);

        Element feComposite =
                generatorContext.domFactory.createElementNS(SVG_NAMESPACE_URI,
                        SVG_FE_COMPOSITE_TAG);
        feComposite.setAttributeNS(null, SVG_OPERATOR_ATTRIBUTE, operator);
        feComposite.setAttributeNS(null, SVG_IN_ATTRIBUTE, input1);
        feComposite.setAttributeNS(null, SVG_IN2_ATTRIBUTE, input2);
        feComposite.setAttributeNS(null, SVG_K2_ATTRIBUTE, k2);
        feComposite.setAttributeNS(null, SVG_RESULT_ATTRIBUTE, SVG_COMPOSITE_VALUE);

        Element feFlood =
                generatorContext.domFactory.createElementNS(SVG_NAMESPACE_URI,
                        SVG_FE_FLOOD_TAG);
        feFlood.setAttributeNS(null, SVG_FLOOD_COLOR_ATTRIBUTE, "white");
        feFlood.setAttributeNS(null, SVG_FLOOD_OPACITY_ATTRIBUTE, "1");
        feFlood.setAttributeNS(null, SVG_RESULT_ATTRIBUTE, SVG_FLOOD_VALUE);


        Element feMerge =
                generatorContext.domFactory.createElementNS(SVG_NAMESPACE_URI,
                        SVG_FE_MERGE_TAG);
        Element feMergeNodeFlood =
                generatorContext.domFactory.createElementNS(SVG_NAMESPACE_URI,
                        SVG_FE_MERGE_NODE_TAG);
        feMergeNodeFlood.setAttributeNS(null, SVG_IN_ATTRIBUTE, SVG_FLOOD_VALUE);
        Element feMergeNodeComposite =
                generatorContext.domFactory.createElementNS(SVG_NAMESPACE_URI,
                        SVG_FE_MERGE_NODE_TAG);
        feMergeNodeComposite.setAttributeNS(null, SVG_IN_ATTRIBUTE,
                SVG_COMPOSITE_VALUE);

        feMerge.appendChild(feMergeNodeFlood);
        feMerge.appendChild(feMergeNodeComposite);

        compositeFilter.appendChild(feFlood);
        compositeFilter.appendChild(feComposite);
        compositeFilter.appendChild(feMerge);

        return compositeFilter;
    }
}
