package org.ofdrw.converter.pdf.textobj;

import org.apache.pdfbox.contentstream.PDFStreamEngine;
import org.apache.pdfbox.contentstream.operator.Operator;
import org.apache.pdfbox.contentstream.operator.markedcontent.BeginMarkedContentSequence;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.documentinterchange.markedcontent.PDPropertyList;

import java.io.IOException;
import java.util.List;

/**
 * 标记内容算子 {@code BDC} 的安全包装
 * <p>
 * 当标记内容属性包含软掩码（SMask）或非 Normal 混合模式（BM）时，暂停文字对象化，
 * 避免文字对象被写入一个在 OFD 中无法表达的透明/混合上下文。
 *
 * @author 权观宇
 * @since 2.5.1
 */
final class SafeMarkedContentProcessor extends SafeOperatorProcessor {

    /**
     * 软掩码键
     */
    private static final COSName SMASK = COSName.getPDFName("SMask");

    /**
     * 混合模式键
     */
    private static final COSName BM = COSName.getPDFName("BM");

    private final BeginMarkedContentSequence delegate;

    /**
     * @param engine 页面绘制器
     */
    SafeMarkedContentProcessor(PDFStreamEngine engine) {
        super(engine, "BDC");
        this.delegate = new BeginMarkedContentSequence(engine);
    }

    @Override
    public void process(Operator operator, List<COSBase> arguments) throws IOException {
        runSafely(operator, arguments, isSafe(arguments), delegate);
    }

    /**
     * 判断标记内容属性是否不含软掩码与非 Normal 混合模式
     *
     * @param arguments 算子参数（属性名、属性字典或属性名引用）
     * @return true - 安全
     */
    private boolean isSafe(List<COSBase> arguments) {
        if (arguments == null || arguments.isEmpty()) {
            return true;
        }
        COSBase last = arguments.get(arguments.size() - 1);
        COSDictionary dict = null;
        if (last instanceof COSDictionary) {
            dict = (COSDictionary) last;
        } else if (last instanceof COSName) {
            OfdPageDrawer drawer = drawer();
            PDResources resources = drawer == null ? null : drawer.getResources();
            PDPropertyList list = resources == null ? null : resources.getProperties((COSName) last);
            if (list != null) {
                dict = list.getCOSObject();
            }
        }
        if (dict == null) {
            return true;
        }
        if (dict.containsKey(SMASK)) {
            return false;
        }
        COSName bm = dict.getCOSName(BM);
        return bm == null || COSName.NORMAL.equals(bm);
    }
}
