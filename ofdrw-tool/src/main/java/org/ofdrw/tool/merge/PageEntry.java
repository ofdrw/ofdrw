package org.ofdrw.tool.merge;

import java.nio.file.Path;
import java.util.ArrayList;

/**
 * 页面项目
 *
 * @author 权观宇
 * @since 2021-11-08 20:52:49
 */
public class PageEntry {

    /**
     * OFD路径
     */
    Path ofdPath;

    /**
     * 待合并的页面
     */
    ArrayList<Integer> pageIndexes;
}
