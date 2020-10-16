package org.ofdrw.reader;

import org.ofdrw.core.basicStructure.pageObj.Content;
import org.ofdrw.core.basicStructure.pageObj.layer.CT_Layer;
import org.ofdrw.core.basicStructure.pageObj.layer.PageBlockType;
import org.ofdrw.core.basicStructure.pageObj.layer.block.CT_PageBlock;
import org.ofdrw.core.basicStructure.pageObj.layer.block.TextObject;
import org.ofdrw.core.text.TextCode;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * 内容抽取器
 *
 * @author 权观宇, minghu-zhang
 * @since 2020-09-21 22:53:09
 */
public class ContentExtractor {
    /**
     * 解析结果接收器
     */
    @FunctionalInterface
    static public interface Receiver {
        /**
         * 处理解析完成的文本
         *
         * @param pageNum  页码
         * @param contents 抽取得到的页面文字内容
         */
        void process(int pageNum, List<String> contents);
    }

    private OFDReader reader;

    private ContentExtractor() {
    }

    /**
     * 构造文字抽取器
     *
     * @param reader OFD解析器
     */
    public ContentExtractor(OFDReader reader) {
        this.reader = reader;
    }

    /**
     * 抽取指定页面内的所有文字
     *
     * @param pageNum 页码，从1开始
     * @return 页面内容的所有文本内容序列
     */
    public List<String> getPageContent(int pageNum) {
        Content ofdContentObj = reader.getPage(pageNum).getContent();
        if (ofdContentObj == null) {
            return Collections.emptyList();
        }

        List<String> txtContentList = new LinkedList<>();
        List<CT_Layer> layers = ofdContentObj.getLayers();
        // 如果页面含有多个层那么分层遍历
        for (CT_Layer layer : layers) {
            // 遍历所有页块
            pageBlockHandle(txtContentList, layer.getPageBlocks());
        }
        return txtContentList;
    }

    /**
     * 页块处理
     *
     * @param txtContentList 文本列表
     * @param pageBlocks     页块列表
     */
    private void pageBlockHandle(List<String> txtContentList, List<PageBlockType> pageBlocks) {
        for (PageBlockType block : pageBlocks) {
            // 找出所有的文字对象
            if (block instanceof TextObject) {
                TextObject text = (TextObject) block;
                List<TextCode> textCodes = text.getTextCodes();
                for (TextCode code : textCodes) {
                    txtContentList.add(code.getContent());
                }
            } else if (block instanceof CT_PageBlock) {
                CT_PageBlock ctPageBlock = (CT_PageBlock) block;
                pageBlockHandle(txtContentList, ctPageBlock.getPageBlocks());
            }
        }
    }

    /**
     * 获取OFD内的所有文本内容
     *
     * @return OFD中所有文本内容
     */
    public List<String> extractAll() {
        int numberOfPages = reader.getNumberOfPages();
        List<String> txtContentList = new LinkedList<>();
        for (int pageNum = 1; pageNum <= numberOfPages; pageNum++) {
            List<String> pageContent = getPageContent(pageNum);
            if (pageContent != null && !pageContent.isEmpty()) {
                txtContentList.addAll(pageContent);
            }
        }
        return txtContentList;
    }

    /**
     * 遍历所有页面
     *
     * @param e 接受
     */
    public void traverse(Receiver e) {
        int numberOfPages = reader.getNumberOfPages();
        for (int pageNum = 1; pageNum <= numberOfPages; pageNum++) {
            List<String> pageContent = getPageContent(pageNum);
            if (pageContent != null && !pageContent.isEmpty() && e != null) {
                e.process(pageNum, pageContent);
            }
        }
    }
}
