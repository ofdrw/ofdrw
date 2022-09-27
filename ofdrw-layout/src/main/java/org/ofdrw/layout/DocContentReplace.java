package org.ofdrw.layout;

import org.ofdrw.core.basicStructure.pageObj.layer.block.TextObject;
import org.ofdrw.core.basicType.ST_Loc;
import org.ofdrw.core.text.CT_CGTransform;
import org.ofdrw.core.text.font.CT_Font;
import org.ofdrw.font.Font;
import org.ofdrw.font.FontName;
import org.ofdrw.layout.element.Paragraph;
import org.ofdrw.reader.ContentExtractor;
import org.ofdrw.reader.OFDReader;
import org.ofdrw.reader.extractor.ExtractorFilter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 文档内容替换
 *
 * @author shanhy
 * @since 2022-09-27 8:38
 */
public class DocContentReplace {

    /**
     * 已有的OFD文档
     */
    private OFDDoc ofdDoc;

    /**
     * 扩展，为对应的文字构造CgTransform
     */
    private ReplaceTextCgTransformHandler replaceTextCgTransformHandler;

    public DocContentReplace(OFDDoc ofdDoc) {
        this.ofdDoc = ofdDoc;
        if (ofdDoc == null) {
            throw new IllegalArgumentException("OFDDoc不能为空");
        }
    }

    public DocContentReplace(OFDDoc ofdDoc, ReplaceTextCgTransformHandler replaceTextCgTransformHandler) {
        this.ofdDoc = ofdDoc;
        this.replaceTextCgTransformHandler = replaceTextCgTransformHandler;
    }

    /**
     * 文档中的内容替换，对于OFD文档来说，替换的内容应为一行，如果是换行的内容，请以行为单位逐行替换 <br/>
     * 替换范围：指定页码
     *
     * @param pageNum 页码，从1开始
     * @param textMap 文本内容替换的映射关系 key为替换钱的文本，value是替换后的文本
     */
    public void replaceText(int pageNum, Map<String, String> textMap) {
        this.replaceText(pageNum, textMap, null);
    }

    /**
     * 文档中的内容替换，对于OFD文档来说，替换的内容应为一行，如果是换行的内容，请以行为单位逐行替换 <br/>
     * 换范围：整个文档
     *
     * @param textMap 文本内容替换的映射关系 key为替换钱的文本，value是替换后的文本
     */
    public void replaceText(Map<String, String> textMap) {
        this.replaceText(textMap, null);
    }

    /**
     * 文档中的内容替换，对于OFD文档来说，替换的内容应为一行，如果是换行的内容，请以行为单位逐行替换 <br/>
     * 替换范围：指定页码
     *
     * @param pageNum 页码，从1开始
     * @param textMap 文本内容替换的映射关系 key为替换钱的文本，value是替换后的文本
     * @param contentExtractorFilter 内容抽取过滤器
     */
    public void replaceText(int pageNum, Map<String, String> textMap, ExtractorFilter contentExtractorFilter) {
        ContentExtractor contentExtractor = new ContentExtractor(this.getReader(), contentExtractorFilter);
        this.replaceTextByTextObject(textMap, contentExtractor.getPageTextObject(pageNum));
    }

    /**
     * @param contentExtractorFilter 文本内容过滤器
     * @param textMap 文本内容替换的映射关系 key为替换钱的文本，value是替换后的文本
     */
    public void replaceText(Map<String, String> textMap, ExtractorFilter contentExtractorFilter) {
        ContentExtractor contentExtractor = new ContentExtractor(this.getReader(), contentExtractorFilter);
        this.replaceTextByTextObject(textMap, contentExtractor.extractAllTextObject());
    }

    /**
     * 文档中的内容替换，对于OFD文档来说，替换的内容应为一行，如果是换行的内容，请以行为单位逐行替换
     *
     * @param txtObjectList 要替换内容的 TextObject 集合
     * @param textMap 文本内容替换的映射关系 key为替换钱的文本，value是替换后的文本
     */
    protected void replaceTextByTextObject(Map<String, String> textMap, List<TextObject> txtObjectList) {
        if (txtObjectList == null || txtObjectList.isEmpty())
            return;
        Map<String, Font> fontMapCache = new HashMap<>();
        // 内容替换
        txtObjectList.forEach(txtObj -> txtObj.getTextCodes().forEach(txtCode -> {
            String oldText = txtCode.getContent();
            if (oldText != null && oldText.trim().length() > 0) {
                String newText = textMap.get(oldText);
                if (newText != null) {
                    // 删除 ofd:TextObject 节点中的 ofd:CGTransform，
                    // CGTransform 中的检索码会对应到字体文件中去获取（res中的字体文件中一般只包含文档中需要的字的内容，这样字体文件会比较小），
                    // 如果获取不到才会显示 TextCode 中的内容，这样在使用特殊字体时，能保证在不同操作系统和环境中看到和打印的字体的统一，
                    // 因为我们这里要替换的内容未知，为了保证替换的可靠性，这里删除掉 ofd:CGTransform，让文档显示 textCode 中的内容。
                    List<CT_CGTransform> ctCgTransformList = txtObj.getCGTransforms();
                    if (ctCgTransformList != null && ctCgTransformList.size() > 0)
                        txtObj.removeOFDElemByNames(ctCgTransformList.get(0).getName());

                    // 下面的font处理，是为了后面Paragraph中根据传入的font计算getDeltaX用途，其中Times New Roman是特殊的且仅适用英文
                    String fontRefId = txtObj.getFont().getRefId().toString();
                    Font font = fontMapCache.get(fontRefId);
                    if (font == null) {
                        CT_Font ctFont = this.getReader().getResMgt().getFont(fontRefId);
                        String fontName = ctFont.getFontName();
                        font = new Font(fontName, ctFont.getFamilyName());
                        // 字体 Times New Roman 处理
                        if ("Times New Roman".equals(fontName))
                            font.setPrintableAsciiWidthMap(FontName.TIMES_NEW_ROMAN_PRINTABLE_ASCII_MAP);
                        if (this.replaceTextCgTransformHandler != null) {
                            CT_CGTransform cgTransform =
                                    this.replaceTextCgTransformHandler.createCgTransformHandler(txtObj, newText,
                                            ctFont.getFontFile());
                            if (cgTransform != null)
                                txtObj.addCGTransform(cgTransform);
                        }
                        fontMapCache.put(fontRefId, font);
                    }

                    // 使用Paragraph重新计算新内容的DeltaX数据
                    Paragraph paragraph = new Paragraph()
                            .setFontSize(txtObj.getSize())
                            .setDefaultFont(font)
                            .setXY(txtCode.getX(), txtCode.getY())
                            .add(newText);

                    txtCode.setContent(newText);
                    txtCode.setDeltaX(paragraph.getContents().get(0).getDeltaX());
                }
            }
        }));
    }

    public ReplaceTextCgTransformHandler getReplaceTextCgTransformHandler() {
        return replaceTextCgTransformHandler;
    }

    public void setReplaceTextCgTransformHandler(ReplaceTextCgTransformHandler replaceTextCgTransformHandler) {
        this.replaceTextCgTransformHandler = replaceTextCgTransformHandler;
    }

    public OFDReader getReader() {
        return this.ofdDoc.getReader();
    }

    interface ReplaceTextCgTransformHandler {

        /**
         * 扩展预留，为对应的文字构造CgTransform
         *
         * @param textObject TextObject对象
         * @param newText 替换后的文字
         * @param beforeTextFontFile 元文字内容的字体文件
         */
        CT_CGTransform createCgTransformHandler(TextObject textObject, String newText, ST_Loc beforeTextFontFile);

    }

}
