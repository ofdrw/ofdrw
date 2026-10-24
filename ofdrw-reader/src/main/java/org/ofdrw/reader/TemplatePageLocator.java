package org.ofdrw.reader;

import org.dom4j.DocumentException;
import org.ofdrw.core.basicType.ST_Loc;
import org.ofdrw.core.basicStructure.pageObj.Page;

import java.io.FileNotFoundException;
import java.util.function.Function;

/**
 * 模板页定位器
 * <p>
 * 负责以文档目录（Doc_N）为基准加载模板页内容。
 * 文档中的 TemplatePage/@BaseLoc 既可以写成相对文档目录的路径（规范推荐的 Doc_0/Tpls/...），
 * 也有部分文档写成相对上一级目录的路径；本定位器始终以文档目录为基准解析，
 * 避免受资源定位器当前工作目录的影响。
 *
 * @author ofdrw
 */
final class TemplatePageLocator {

    /**
     * 工具类不允许实例化
     */
    private TemplatePageLocator() {
    }

    /**
     * 以文档目录为基准加载模板页
     *
     * @param reader  OFD 解析器
     * @param docRoot 文档根路径（Document.xml 的路径，例如 Doc_0/Document.xml）
     * @param baseLoc 模板页路径
     * @param mapper  模板页对象构造器
     * @param <R>     模板页对象类型
     * @return 模板页对象
     * @throws FileNotFoundException 模板页文件不存在
     * @throws DocumentException     模板页解析异常
     */
    static <R> R load(OFDReader reader, ST_Loc docRoot, ST_Loc baseLoc, Function<org.dom4j.Element, R> mapper)
            throws FileNotFoundException, DocumentException {
        final ResourceLocator rl = reader.getResourceLocator();
        final ST_Loc docDir = new ST_Loc(docRoot.parent());
        final String loc = baseLoc.getLoc();
        // 相对路径以文档目录为基准拼接为绝对路径；绝对路径保持不变
        final ST_Loc absLoc = loc.startsWith("/")
                ? new ST_Loc(loc)
                : new ST_Loc(docDir.getLoc() + "/" + loc);
        rl.save();
        try {
            rl.cd("/");
            return rl.get(absLoc, mapper);
        } finally {
            rl.restore();
        }
    }
}
