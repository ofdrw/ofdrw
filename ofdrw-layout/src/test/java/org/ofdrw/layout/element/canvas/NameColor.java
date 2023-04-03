package org.ofdrw.layout.element.canvas;

import java.awt.*;

public class NameColor {

    public static void main(String[] args) {
        String str = "AliceBlue #F0F8FF \n" +
                "AntiqueWhite #FAEBD7 \n" +
                "Aqua #00FFFF \n" +
                "Aquamarine #7FFFD4 \n" +
                "Azure #F0FFFF \n" +
                "Beige #F5F5DC \n" +
                "Bisque #FFE4C4 \n" +
                "Black #000000 \n" +
                "BlanchedAlmond #FFEBCD \n" +
                "Blue #0000FF \n" +
                "BlueViolet #8A2BE2 \n" +
                "Brown #A52A2A \n" +
                "BurlyWood #DEB887 \n" +
                "CadetBlue #5F9EA0 \n" +
                "Chartreuse #7FFF00 \n" +
                "Chocolate #D2691E \n" +
                "Coral #FF7F50 \n" +
                "CornflowerBlue #6495ED \n" +
                "Cornsilk #FFF8DC \n" +
                "Crimson #DC143C \n" +
                "Cyan #00FFFF \n" +
                "DarkBlue #00008B \n" +
                "DarkCyan #008B8B \n" +
                "DarkGoldenRod #B8860B \n" +
                "DarkGray #A9A9A9 \n" +
                "DarkGreen #006400 \n" +
                "DarkKhaki #BDB76B \n" +
                "DarkMagenta #8B008B \n" +
                "DarkOliveGreen #556B2F \n" +
                "Darkorange #FF8C00 \n" +
                "DarkOrchid #9932CC \n" +
                "DarkRed #8B0000 \n" +
                "DarkSalmon #E9967A \n" +
                "DarkSeaGreen #8FBC8F \n" +
                "DarkSlateBlue #483D8B \n" +
                "DarkSlateGray #2F4F4F \n" +
                "DarkTurquoise #00CED1 \n" +
                "DarkViolet #9400D3 \n" +
                "DeepPink #FF1493 \n" +
                "DeepSkyBlue #00BFFF \n" +
                "DimGray #696969 \n" +
                "DodgerBlue #1E90FF \n" +
                "Feldspar #D19275 \n" +
                "FireBrick #B22222 \n" +
                "FloralWhite #FFFAF0 \n" +
                "ForestGreen #228B22 \n" +
                "Fuchsia #FF00FF \n" +
                "Gainsboro #DCDCDC \n" +
                "GhostWhite #F8F8FF \n" +
                "Gold #FFD700 \n" +
                "GoldenRod #DAA520 \n" +
                "Gray #808080 \n" +
                "Green #008000 \n" +
                "GreenYellow #ADFF2F \n" +
                "HoneyDew #F0FFF0 \n" +
                "HotPink #FF69B4 \n" +
                "IndianRed  #CD5C5C \n" +
                "Indigo  #4B0082 \n" +
                "Ivory #FFFFF0 \n" +
                "Khaki #F0E68C \n" +
                "Lavender #E6E6FA \n" +
                "LavenderBlush #FFF0F5 \n" +
                "LawnGreen #7CFC00 \n" +
                "LemonChiffon #FFFACD \n" +
                "LightBlue #ADD8E6 \n" +
                "LightCoral #F08080 \n" +
                "LightCyan #E0FFFF \n" +
                "LightGoldenRodYellow #FAFAD2 \n" +
                "LightGrey #D3D3D3 \n" +
                "LightGreen #90EE90 \n" +
                "LightPink #FFB6C1 \n" +
                "LightSalmon #FFA07A \n" +
                "LightSeaGreen #20B2AA \n" +
                "LightSkyBlue #87CEFA \n" +
                "LightSlateBlue #8470FF \n" +
                "LightSlateGray #778899 \n" +
                "LightSteelBlue #B0C4DE \n" +
                "LightYellow #FFFFE0 \n" +
                "Lime #00FF00 \n" +
                "LimeGreen #32CD32 \n" +
                "Linen #FAF0E6 \n" +
                "Magenta #FF00FF \n" +
                "Maroon #800000 \n" +
                "MediumAquaMarine #66CDAA \n" +
                "MediumBlue #0000CD \n" +
                "MediumOrchid #BA55D3 \n" +
                "MediumPurple #9370D8 \n" +
                "MediumSeaGreen #3CB371 \n" +
                "MediumSlateBlue #7B68EE \n" +
                "MediumSpringGreen #00FA9A \n" +
                "MediumTurquoise #48D1CC \n" +
                "MediumVioletRed #C71585 \n" +
                "MidnightBlue #191970 \n" +
                "MintCream #F5FFFA \n" +
                "MistyRose #FFE4E1 \n" +
                "Moccasin #FFE4B5 \n" +
                "NavajoWhite #FFDEAD \n" +
                "Navy #000080 \n" +
                "OldLace #FDF5E6 \n" +
                "Olive #808000 \n" +
                "OliveDrab #6B8E23 \n" +
                "Orange #FFA500 \n" +
                "OrangeRed #FF4500 \n" +
                "Orchid #DA70D6 \n" +
                "PaleGoldenRod #EEE8AA \n" +
                "PaleGreen #98FB98 \n" +
                "PaleTurquoise #AFEEEE \n" +
                "PaleVioletRed #D87093 \n" +
                "PapayaWhip #FFEFD5 \n" +
                "PeachPuff #FFDAB9 \n" +
                "Peru #CD853F \n" +
                "Pink #FFC0CB \n" +
                "Plum #DDA0DD \n" +
                "PowderBlue #B0E0E6 \n" +
                "Purple #800080 \n" +
                "Red #FF0000 \n" +
                "RosyBrown #BC8F8F \n" +
                "RoyalBlue #4169E1 \n" +
                "SaddleBrown #8B4513 \n" +
                "Salmon #FA8072 \n" +
                "SandyBrown #F4A460 \n" +
                "SeaGreen #2E8B57 \n" +
                "SeaShell #FFF5EE \n" +
                "Sienna #A0522D \n" +
                "Silver #C0C0C0 \n" +
                "SkyBlue #87CEEB \n" +
                "SlateBlue #6A5ACD \n" +
                "SlateGray #708090 \n" +
                "Snow #FFFAFA \n" +
                "SpringGreen #00FF7F \n" +
                "SteelBlue #4682B4 \n" +
                "Tan #D2B48C \n" +
                "Teal #008080 \n" +
                "Thistle #D8BFD8 \n" +
                "Tomato #FF6347 \n" +
                "Turquoise #40E0D0 \n" +
                "Violet #EE82EE \n" +
                "VioletRed #D02090 \n" +
                "Wheat #F5DEB3 \n" +
                "White #FFFFFF \n" +
                "WhiteSmoke #F5F5F5 \n" +
                "Yellow #FFFF00 \n" +
                "YellowGreen #9ACD32\n";
        String[] lines = str.split("\n");
        for (String line : lines) {
            line = line.trim();
            int off = line.indexOf('#');
            String[] arr = new String[]{
                    line.substring(0, off).trim(), line.substring(off).trim()
            };
            arr[0] = arr[0].toLowerCase();
            Color c  = Color.decode(arr[1]);
            System.out.printf("case \"%s\": return new int[]{ %d, %d, %d};\n", arr[0], c.getRed(), c.getGreen(), c.getBlue());
        }
    }
}
