package org.ofdrw.converter.pdf.textobj;

import org.apache.pdfbox.contentstream.PDFStreamEngine;
import org.apache.pdfbox.contentstream.operator.Operator;
import org.apache.pdfbox.contentstream.operator.state.SetGraphicsStateParameters;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState;

import java.io.IOException;
import java.util.List;

/**
 * 扩展图形状态算子 {@code gs} 的安全包装
 * <p>
 * 当扩展图形状态包含软掩码（SMask）或非 Normal 混合模式（BM）时，暂停文字对象化，
 * 使该状态作用范围内的文字一律回退为字形路径（OFD 无等价表达）。
 *
 * @author 权观宇
 * @since 2.5.1
 */
final class SafeGraphicsStateProcessor extends SafeOperatorProcessor {

    /**
     * 软掩码键
     */
    private static final COSName SMASK = COSName.getPDFName("SMask");

    /**
     * 混合模式键
     */
    private static final COSName BM = COSName.getPDFName("BM");

    private final SetGraphicsStateParameters delegate;

    /**
     * @param engine 页面绘制器
     */
    SafeGraphicsStateProcessor(PDFStreamEngine engine) {
        super(engine, "gs");
        this.delegate = new SetGraphicsStateParameters(engine);
    }

    @Override
    public void process(Operator operator, List<COSBase> arguments) throws IOException {
        runSafely(operator, arguments, isSafe(arguments), delegate);
    }

    /**
     * 判断扩展图形状态是否不含软掩码与非 Normal 混合模式
     *
     * @param arguments 算子参数（扩展图形状态名称）
     * @return true - 安全
     */
    private boolean isSafe(List<COSBase> arguments) {
        if (arguments == null || arguments.isEmpty() || !(arguments.get(0) instanceof COSName)) {
            return true;
        }
        OfdPageDrawer drawer = drawer();
        PDResources resources = drawer == null ? null : drawer.getResources();
        if (resources == null) {
            return true;
        }
        PDExtendedGraphicsState extState = resources.getExtGState((COSName) arguments.get(0));
        if (extState == null) {
            return true;
        }
        COSDictionary dict = extState.getCOSObject();
        if (dict.containsKey(SMASK)) {
            return false;
        }
        COSName bm = dict.getCOSName(BM);
        return bm == null || COSName.NORMAL.equals(bm);
    }
}
