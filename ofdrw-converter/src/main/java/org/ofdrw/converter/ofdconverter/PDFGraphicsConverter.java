package org.ofdrw.converter.ofdconverter;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.ofdrw.converter.GeneralConvertException;
import org.ofdrw.graphics2d.OFDPageGraphics2D;
import org.ofdrw.graphics2d.OFDGraphicsDocument;

import java.awt.RenderingHints;
import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 基于 {@code ofdrw-graphics2d} 的图形化 PDF → OFD 转换器（把 PDF 当作图片）
 * <p>
 * 把 PDF 页面当作图片处理：用 PDFBox 的 {@link PDFRenderer#renderPageToGraphics(int, java.awt.Graphics2D, float)}
 * 直接渲染到 {@link OFDPageGraphics2D}，产出物全部是 OFD 的路径（{@code PathObject}）与图片
 * （{@code ImageObject}）图元，<b>不生成文字对象、不嵌入字体</b>。
 *
 * <h2>与 {@link PDFConverter} 的区别</h2>
 * {@link PDFConverter} 采用<b>文字对象化</b>方案：拦截 PDFBox 的字形绘制回调，输出 OFD {@code TextObject}
 * 并嵌入（可按需子集化的）字体，因此转出的 OFD 文本可检索、可复制、体积小；本类则沿用纯
 * {@link java.awt.Graphics2D} 渲染，字形在渲染阶段就展开为轮廓路径并直接写成 {@code PathObject}，
 * 因此<b>文本不可检索</b>、体积大得多，但实现简单、完全不依赖字体嵌入。
 * <p>
 * 两者<b>并行存在、互不影响</b>：本类不调用 {@code OfdPageDrawer} 等文字对象化组件，也不改动
 * {@link PDFConverter} 的任何行为，可以在同一工程中按需并行使用。
 *
 * <h2>体积代价（固有，不做优化）</h2>
 * 文字转路径会显著放大体积，这是本方案的固有代价。以 {@code dense-text-perf} 样本（96 dpi 对照）为例：
 * <table border="1">
 *     <caption>两种实现的实测对比</caption>
 *     <tr><th>指标</th><th>{@link PDFConverter}</th><th>本类</th></tr>
 *     <tr><td>输出体积</td><td>305 KB</td><td>约 9 MB（约 30 倍）</td></tr>
 *     <tr><td>{@code TextObject} 数量</td><td>8,478</td><td>0</td></tr>
 *     <tr><td>内嵌字体</td><td>有</td><td>无</td></tr>
 *     <tr><td>文本可检索</td><td>支持</td><td>不支持</td></tr>
 * </table>
 *
 * <h2>适用场景</h2>
 * <ul>
 *     <li><b>优先使用 {@link PDFConverter}</b>：需要全文检索、复制文本、二次编辑，或追求体积最小的正式文档归档；</li>
 *     <li>使用本类：只要“看起来一样”的可视化归档；字形无法嵌入（非内嵌字体且无可用系统字体）导致
 *       {@link PDFConverter} 回退路径后效果不佳；或需要与旧版“整页路径渲染”输出做像素级对照。</li>
 * </ul>
 *
 * <h2>用法</h2>
 * <pre>{@code
 * try (PDFGraphicsConverter converter = new PDFGraphicsConverter(Paths.get("out.ofd"))) {
 *     converter.convert(Paths.get("in.pdf"));          // 全部页
 *     // 或只转换指定页（从 0 起，支持乱序与重复）：
 *     // converter.convert(Paths.get("in.pdf"), 0, 2);
 * }
 * }</pre>
 *
 * @author ofdrw
 * @see PDFConverter 文字对象化实现（推荐，文本可检索、体积小）
 * @see org.ofdrw.graphics2d.OFDGraphicsDocument
 */
public class PDFGraphicsConverter implements DocConverter, Closeable {

    /**
     * 每毫米对应的 PDF 用户单元数（PT_PER_MM = 25.4 / 72 ≈ 2.8346），单位：user unit/mm
     * <p>
     * 用于把 PDF 页面尺寸（PDF 用户空间，单位 pt）换算成 OFD 页面尺寸（单位 mm），
     * 同时作为渲染缩放系数的倒数（缩放系数 = 1 / uuPmm，单位：mm/pt）。
     */
    private double uuPmm = 2.8346;

    /**
     * 渲染分辨率缩放系数
     * <p>
     * 默认 {@code 1.0} 表示按 OFD 页面的自然尺寸（毫米）渲染；增大该值会让 PDFBox 内部按更高分辨率
     * 栅格化图像类内容（矢量内容不受影响），代价是转换更慢、图片资源更大。
     */
    private double imageScale = 1.0d;

    /**
     * 渲染提示（抗锯齿、插值等），可为 {@code null}（使用 PDFBox 默认值）
     */
    private RenderingHints renderingHints;

    /**
     * 输出 OFD 文档
     */
    private final OFDGraphicsDocument ofdDoc;

    /**
     * 是否已经关闭，避免重复打包
     */
    private boolean closed = false;

    /**
     * 创建图形化 PDF → OFD 转换器
     *
     * @param ofdPath 转换后的 OFD 文件路径（不存在时自动创建）
     * @throws IOException 输出文件创建失败
     */
    public PDFGraphicsConverter(Path ofdPath) throws IOException {
        if (ofdPath == null) {
            throw new IllegalArgumentException("转换后的OFD文件路径为空");
        }
        ofdPath = ofdPath.toAbsolutePath();
        if (!Files.exists(ofdPath)) {
            Path parent = ofdPath.getParent();
            if (parent != null) {
                if (Files.exists(parent)) {
                    if (!Files.isDirectory(parent)) {
                        throw new IllegalArgumentException("已经存在同名文件: " + parent);
                    }
                } else {
                    Files.createDirectories(parent);
                }
            }
            Files.createFile(ofdPath);
        }
        this.ofdDoc = new OFDGraphicsDocument(ofdPath);
        // 渲染参数与 PDFConverter 的图形渲染保持一致，保证两套实现的视觉输出可直接对照
        RenderingHints hints = new RenderingHints(null);
        hints.put(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        hints.put(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
        hints.put(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        this.renderingHints = hints;
    }

    /**
     * 设置每毫米对应的 PDF 用户单元数
     * <p>
     * 默认 {@code 2.8346}（即 25.4/72），通常无需修改。
     *
     * @param uuPmm 每毫米用户单元数，单位：user unit/mm，必须为正数
     */
    public void setUUPMM(double uuPmm) {
        if (uuPmm <= 0) {
            throw new IllegalArgumentException("每毫米用户单元数必须为正数：" + uuPmm);
        }
        this.uuPmm = uuPmm;
    }

    /**
     * 设置图片渲染的额外缩放倍数
     *
     * @param imageScale 缩放倍数，单位：倍，必须为正数；{@code 1.0} 表示按页面毫米尺寸渲染
     */
    public void setImageScale(double imageScale) {
        if (imageScale <= 0) {
            throw new IllegalArgumentException("渲染缩放倍数必须为正数：" + imageScale);
        }
        this.imageScale = imageScale;
    }

    /**
     * 设置渲染提示
     *
     * @param hints 渲染提示，{@code null} 表示使用 PDFBox 默认值
     */
    public void setRenderingHints(RenderingHints hints) {
        this.renderingHints = hints;
    }

    /**
     * 把 PDF 转换为 OFD
     * <p>
     * 页码参数不传或为空时转换全部页；支持乱序与重复。
     *
     * @param filepath 待转换的 PDF 文件路径
     * @param indexes  【可选】【可变】待转换页码（从 0 起）
     * @throws GeneralConvertException 转换异常（文件无法读取、页码越界、渲染失败等）
     */
    @Override
    public void convert(Path filepath, int... indexes) throws GeneralConvertException {
        if (filepath == null) {
            throw new GeneralConvertException("待转换的PDF文件路径为空");
        }
        if (!Files.exists(filepath)) {
            throw new GeneralConvertException("待转换的PDF文件不存在：" + filepath);
        }
        try (PDDocument pdfDoc = Loader.loadPDF(filepath.toFile())) {
            renderPages(pdfDoc, indexes);
        } catch (GeneralConvertException e) {
            throw e;
        } catch (IOException | RuntimeException e) {
            throw new GeneralConvertException("PDF转换为OFD失败：" + filepath, e);
        }
    }

    /**
     * 按页码序列把 PDF 页面渲染为 OFD 页面
     *
     * @param pdfDoc  PDF 文档
     * @param indexes 页码序列（从 0 起）；为空表示全部页
     * @throws IOException 渲染或写包失败
     */
    private void renderPages(PDDocument pdfDoc, int... indexes) throws IOException {
        final int pageCount = pdfDoc.getNumberOfPages();
        List<Integer> targetPages = new ArrayList<>();
        if (indexes == null || indexes.length == 0) {
            for (int i = 0; i < pageCount; i++) {
                targetPages.add(i);
            }
        } else {
            for (int index : indexes) {
                if (index < 0 || index >= pageCount) {
                    throw new GeneralConvertException("页码越界：" + index + "，PDF 共 " + pageCount + " 页");
                }
                targetPages.add(index);
            }
        }

        PDFRenderer pdfRender = new PDFRenderer(pdfDoc);
        if (renderingHints != null) {
            pdfRender.setRenderingHints(renderingHints);
        }
        // 缩放系数：PDF 用户空间（pt）→ OFD 页面坐标（mm）
        float scale = (float) (imageScale / uuPmm);

        for (Integer index : targetPages) {
            PDPage page = pdfDoc.getPage(index);
            /*
             * 页面“旋转角度”为 90/270 时 PDF 实际显示为横向，
             * 而 BBox 给的仍是未旋转的纵向尺寸，因此需要交换宽高，
             * 否则横向页会被压成纵向（旧版本 commit 57c65e32 修复过该问题）。
             */
            int rotation = ((page.getRotation() % 360) + 360) % 360;
            PDRectangle pageBox = page.getBBox();
            float width = pageBox.getWidth();
            float height = pageBox.getHeight();
            if (rotation == 90 || rotation == 270) {
                float tmp = width;
                width = height;
                height = tmp;
            }

            // PDF 用户空间（pt）→ OFD 页面尺寸（mm）
            OFDPageGraphics2D pageGraphics = ofdDoc.newPage(width / uuPmm, height / uuPmm);
            // 纯 Graphics2D 渲染：字形在此全部展开为路径，不产生文字对象
            pdfRender.renderPageToGraphics(index, pageGraphics, scale);
        }
    }

    /**
     * 关闭转换器并打包 OFD 文件
     *
     * @throws IOException 打包失败
     */
    @Override
    public void close() throws IOException {
        if (closed) {
            return;
        }
        closed = true;
        if (ofdDoc != null) {
            ofdDoc.close();
        }
    }
}
