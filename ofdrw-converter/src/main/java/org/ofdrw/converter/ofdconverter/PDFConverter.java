package org.ofdrw.converter.ofdconverter;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSInputStream;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentNameDictionary;
import org.apache.pdfbox.pdmodel.PDEmbeddedFilesNameTreeNode;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDNameTreeNode;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.common.filespecification.PDComplexFileSpecification;
import org.apache.pdfbox.pdmodel.common.filespecification.PDEmbeddedFile;
import org.apache.pdfbox.pdmodel.common.filespecification.PDFileSpecification;
import org.apache.pdfbox.pdmodel.interactive.action.PDActionGoTo;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationFileAttachment;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.*;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDDocumentOutline;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineNode;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.ofdrw.converter.GeneralConvertException;
import org.ofdrw.converter.pdf.textobj.OfdTextObjectConverter;
import org.ofdrw.converter.pdf.textobj.PdfFontRegistry;
import org.ofdrw.core.action.actionType.actionGoto.CT_Dest;
import org.ofdrw.core.action.actionType.actionGoto.DestType;
import org.ofdrw.core.attachment.CT_Attachment;
import org.ofdrw.core.basicStructure.doc.bookmark.Bookmark;
import org.ofdrw.core.basicStructure.doc.bookmark.Bookmarks;
import org.ofdrw.core.basicType.ST_ID;
import org.ofdrw.graphics2d.OFDGraphicsDocument;
import org.ofdrw.graphics2d.OFDPageGraphics2D;

import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.*;

/**
 * PDF转换为OFD转换器
 *
 * @author 权观宇
 * @since 2023-3-14 23:09:08
 */
public class PDFConverter implements DocConverter {

    /**
     * 是否已经关闭
     */
    private boolean closed = false;


    /**
     * OFD图形文档对象
     */
    final OFDGraphicsDocument ofdDoc;

    /**
     * 书签列表
     */
    private Bookmarks bookmarks;

    /**
     * PDF坐标系 转换 OFD坐标系 缩放比例
     * PDF user unit / OFD mm
     * user unit per millimeter
     */
    double uuPmm = 2.8346;

    /**
     * 已经完成附件复制的文件绝对路径
     * <p>
     * 防止重复拷贝同一个文件中的附件
     */
    private final Set<String> copied;


    /**
     * 是否允许复制PDF的附件到OFD中
     */
    private boolean enableCopyAttachFiles;

    /**
     * 是否允许复制书签
     */
    private boolean enableCopyBookmarks;

    /**
     * PDF 字体资源登记器（文字对象化时使用）
     * <p>
     * 每个 PDFConverter 实例保留一个，保证同一字体在多次 convert 调用中只登记一次、字体资源 ID 稳定。
     */
    private PdfFontRegistry fontRegistry;


    /**
     * 非内嵌字体查找目录：{@code null}（默认）表示不查系统字体，直接回退路径绘制
     */
    private Path fontDir;

    /**
     * 是否开启字体子集化（默认 {@code false}：PDF 内嵌字体原样嵌入）
     */
    private boolean subsetFont = false;

    /**
     * 字体程序摘要 → OFD 字体资源 ID（同一字体程序的多个 PDFont 对象复用同一资源）
     */
    private final Map<String, ST_ID> fontIdCache = new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * 创建PDF转换OFD转换器
     *
     * @param ofdPath 转换后的OFD文件路径
     * @throws IOException 文件解析异常
     */
    public PDFConverter(Path ofdPath) throws IOException {
        if (ofdPath == null) {
            throw new IllegalArgumentException("转换后的OFD文件路径为空");
        }

        ofdPath = ofdPath.toAbsolutePath();
        if (!Files.exists(ofdPath)) {
            Path parent = ofdPath.getParent();
            if (Files.exists(parent)) {
                if (!Files.isDirectory(parent)) {
                    throw new IllegalArgumentException("已经存在同名文件: " + parent);
                }
            } else {
                Files.createDirectories(parent);
            }
            Files.createFile(ofdPath);
        }
        ofdDoc = new OFDGraphicsDocument(ofdPath);
        copied = new HashSet<>();
        enableCopyAttachFiles = true;
        enableCopyBookmarks = true;
        bookmarks = null;
    }


    /**
     * PDF转换为OFD页面
     *
     * @param filepath 待转换文件路径
     * @param indexes  【可选】【可变】待转换页码（从0起），该参数仅在转换源文件类型为类文档文件时有效，当该参数不传或为空时表示转换全部内容到OFD。
     * @throws GeneralConvertException 转换异常
     */
    @Override
    public void convert(Path filepath, int... indexes) throws GeneralConvertException {
        if (filepath == null || !Files.exists(filepath) || Files.isDirectory(filepath)) {
            return;
        }

        // PDFBox 3.x 不再提供 PDDocument.load，统一使用 Loader 加载
        try (PDDocument pdfDoc = Loader.loadPDF(filepath.toFile())) {
            int total = pdfDoc.getNumberOfPages();
            List<Integer> targetPages = new LinkedList<>();
            if (indexes == null || indexes.length == 0) {
                for (int i = 0; i < total; i++) {
                    targetPages.add(i);
                }
            } else {
                // 获取指定页面信息
                for (int index : indexes) {
                    if (index < 0 || index >= total) {
                        continue;
                    }
                    targetPages.add(index);
                }
            }

            // 字体注册表必须在渲染前创建，以便把子集化开关（GID 重排）传给文字对象化转换器
            if (fontRegistry == null) {
                fontRegistry = new PdfFontRegistry(ofdDoc);
                if (fontDir != null) {
                    fontRegistry.setFontDir(fontDir);
                }
                if (subsetFont) {
                    fontRegistry.setSubsetFont(true);
                }
            }
            // 文字对象化：在 PDFBox 字形级入口拦截，输出 OFD 文字对象并内嵌字体
            OfdTextObjectConverter textConverter = new OfdTextObjectConverter(pdfDoc, ofdDoc,
                    this::registerFont, this::flushFonts, this::resolveGid, this::registerGlyphUsage,
                    // 子集化时 GID 会重排，需要登记 CGTransform 节点以便打包前改写（默认 null，不登记）
                    fontRegistry != null && fontRegistry.isSubsetMode(),
                    // 注释/底纹单元允许系统字体兜底（仅在配置 fontDir 时生效；正文仍走严格策略）
                    fontRegistry == null ? null : fontRegistry::registerAnnotationFont);

            RenderingHints r = new RenderingHints(null);
            r.put(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            // 设置渲染模式为快速，关闭PDFBox对图片的压缩
            r.put(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
            r.put(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            // 图片插值/抗锯齿：文字对象化渲染器与普通渲染器使用同一组参数
            textConverter.setRenderingHints(r);

            PDDocumentOutline pdfOutline = pdfDoc.getDocumentCatalog().getDocumentOutline();

            for (Integer index : targetPages) {
                // 获取PDF页面（page）对象
                PDPage page = pdfDoc.getPage(index);
                // 获取当前页面“旋转角度”的值
                int rotation = page.getRotation();
                // 获取页面尺寸对象
                PDRectangle pdfPageSize = page.getBBox();
                // 获取页面的宽度、高度
                float width = pdfPageSize.getWidth();
                float height = pdfPageSize.getHeight();
                // 如果页面“旋转角度”值为90度或270度，则页面显示为“横向”，需要将获取到的宽度、高度值互换。
                if (rotation == 90 || rotation == 270){
                    float tmp = width;
                    width = height;
                    height = tmp;
                }

                // 将PDF页面尺寸缩放至OFD尺寸
                OFDPageGraphics2D ofdPageG2d = ofdDoc.newPage(width / uuPmm, height/ uuPmm);
                textConverter.renderPage(index, ofdPageG2d, (float) (1d / uuPmm));
                if (enableCopyBookmarks) {
                    exportBookmark(pdfDoc, pdfOutline, ofdPageG2d.pageID, index);
                }
            }

            textConverter.finish();

            if (!enableCopyAttachFiles) {
                return;
            }
            // 检查文件中的附件是否已经复制过
            String srcFilePath = filepath.toAbsolutePath().toString();
            if (copied.contains(srcFilePath)) {
                return;
            }
            copied.add(srcFilePath);
            // 复制附件到OFD
            copyAttachFiles(pdfDoc, targetPages);
        } catch (IOException e) {
            throw new GeneralConvertException("PDF转换OFD异常", e);
        }
    }

    /**
     * 登记 PDF 字体到 OFD 字体资源
     *
     * @param font PDF 字体
     * @return OFD 字体资源 ID；无法嵌入时返回 null（该字体文字回退路径）
     */
    private ST_ID registerFont(PDFont font) {
        if (fontRegistry == null) {
            fontRegistry = new PdfFontRegistry(ofdDoc);
            // 字体注册表统一在渲染前创建（见 convert()），这里只兜底
        }
        try {
            /* 文字对象化时 PDFBox 的 PageDrawer 会为同一字体程序创建多个 PDFont 对象，
             * 而字体登记按“字体程序 SHA-256”去重；这里按字体程序哈希缓存资源 ID，
             * 命中缓存直接返回，避免每个字形都重新读取、解析字体程序。 */
            String fontKey = fontProgramKey(font);
            ST_ID cached = fontKey == null ? null : fontIdCache.get(fontKey);
            if (cached != null) {
                return cached;
            }
            ST_ID id = fontRegistry.registerFont(font);
            if (id != null && fontKey != null) {
                fontIdCache.put(fontKey, id);
            }
            return id;
        } catch (RuntimeException e) {
            // 字体登记失败不阻断转换，该字体文字回退路径绘制
            return null;
        }
    }

    /**
     * 把 PDF 原始 GID 换算为最终写入 OFD 字体文件的 GID（CGTransform 使用）
     * <p>
     * 默认（原样嵌入）为恒等；开启字体子集化后，子集字体的 GID 会重排，必须在 flush 之后用
     * {@link PdfFontRegistry#subsetGid(PDFont, int)} 取新 GID，否则 CGTransform 指向旧字形。
     *
     * @param font   PDF 字体
     * @param pdfGid PDF 原始 GID
     * @return 最终 GID
     */
    private int resolveGid(PDFont font, int pdfGid) {
        return fontRegistry == null ? pdfGid : fontRegistry.subsetGid(font, pdfGid);
    }

    /**
     * 记录字形使用（PDF 原始 GID 与 Unicode），供字体子集化重建 cmap
     * <p>
     * 默认（原样嵌入）只做统计；开启子集化时作为子集与 cmap 重建的依据。
     *
     * @param font    PDF 字体
     * @param code    PDF 字符编码
     * @param gid     PDF 原始字形索引
     * @param unicode 该字形对应的 Unicode 码位，未知时为 -1
     */
    private void registerGlyphUsage(PDFont font, int code, int gid, int unicode) {
        if (fontRegistry == null) {
            registerFont(font);
        }
        if (fontRegistry != null) {
            fontRegistry.addUsedGlyph(font, code, gid, unicode);
        }
    }

    /**
     * 计算字体程序缓存键（字体程序的 SHA-256；无字体程序时返回 null）
     * <p>
     * 只读取字体程序的字节摘要，不做完整解析；结果用于把同一字体程序的多个 PDFont 对象
     * 映射到同一个 OFD 字体资源。
     *
     * @param font PDF 字体
     * @return 字体程序摘要（64 位十六进制），无字体程序或读取失败时返回 null
     */
    private String fontProgramKey(PDFont font) {
        org.apache.pdfbox.pdmodel.font.PDFontLike owner = font instanceof org.apache.pdfbox.pdmodel.font.PDType0Font
                ? ((org.apache.pdfbox.pdmodel.font.PDType0Font) font).getDescendantFont() : font;
        if (owner == null || owner.getFontDescriptor() == null) {
            return null;
        }
        org.apache.pdfbox.pdmodel.common.PDStream stream = owner.getFontDescriptor().getFontFile2();
        if (stream == null) {
            stream = owner.getFontDescriptor().getFontFile3();
        }
        if (stream == null) {
            stream = owner.getFontDescriptor().getFontFile();
        }
        if (stream == null) {
            return null;
        }
        try (org.apache.pdfbox.cos.COSInputStream in = stream.createInputStream()) {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) != -1) {
                digest.update(buf, 0, n);
            }
            StringBuilder sb = new StringBuilder(64);
            for (byte b : digest.digest()) {
                sb.append(Character.forDigit((b >> 4) & 0xF, 16)).append(Character.forDigit(b & 0xF, 16));
            }
            return sb.toString();
        } catch (IOException | java.security.NoSuchAlgorithmException | RuntimeException e) {
            return null;
        }
    }

    /**
     * 在 OFD 打包前写入字体文件并回填字体资源
     *
     * @param doc      OFD 文档
     * @param fontIds  已登记的 PDF 字体 -> OFD 字体资源 ID
     */
    private void flushFonts(OFDGraphicsDocument doc, Map<PDFont, ST_ID> fontIds) {
        if (fontRegistry != null) {
            fontRegistry.flush();
        }
    }

    /**
     * 设置非内嵌字体的查找目录（精确匹配，默认关闭）
     * <p>
     * 默认行为：只使用 PDF 自带的字体程序；没有内嵌程序时直接回退路径绘制。
     * 显式设置目录后，PDF 未内嵌的字体按“字体名/字族名 + 字重 + 斜体”精确匹配该目录下的字体文件，
     * 匹配成功后原样嵌入（UTF 字形与 PDF 编码的对应关系由 CGTransform 表达），匹配失败仍回退路径绘制。
     * <p>
     * 系统字体不属于“PDF 内嵌字体”，因此本方法只影响未内嵌字体，不改变内嵌字体的原样嵌入策略。
     *
     * @param fontDir 字体目录；{@code null} 表示关闭系统字体匹配（默认）
     */
    public void setFontDir(Path fontDir) {
        this.fontDir = fontDir;
        if (fontRegistry != null) {
            fontRegistry.setFontDir(fontDir);
        }
    }

    /**
     * 设置是否开启字体子集化（默认关闭）
     * <p>
     * 开启后，字体写出时会按实际使用字形子集化并重建 cmap、裁剪冗余表；
     * 与之配套的 CGTransform GID 会在 OFD 打包前按 old→new 映射统一改写，保证字形不错位。
     * <b>默认关闭</b>：用户要求“PDF 内嵌字体直接作为 OFD 内嵌字体”，子集化仅作为体积优化选项。
     *
     * @param subsetFont true 开启子集化；false（默认）原样嵌入
     */
    public void setSubsetFont(boolean subsetFont) {
        this.subsetFont = subsetFont;
        if (fontRegistry != null) {
            fontRegistry.setSubsetFont(subsetFont);
        }
    }

    /**
     * 设置每毫米容纳多少个用户单元（PDF单位）
     *
     * @param uuPmm 每毫米用户单元数
     */
    public void setUUPMM(double uuPmm) {
        this.uuPmm = uuPmm;
    }

    /**
     * 启用或禁用PDF附件向OFD复制
     *
     * @param enableCopyAttachFiles true - 启用复制； false - 禁用复制；默认为true 复制
     */
    public void setEnableCopyAttachFiles(boolean enableCopyAttachFiles) {
        this.enableCopyAttachFiles = enableCopyAttachFiles;
    }

    /**
     * 启用或禁用PDF书签向OFD复制
     *
     * @param enableCopyBookmarks true - 启用复制； false - 禁用复制；默认为true 复制
     */
    public void setEnableCopyBookmarks(boolean enableCopyBookmarks) {
        this.enableCopyBookmarks = enableCopyBookmarks;
    }

    /**
     * 递归的遍历文档大纲并导出PDF书签
     *
     * @param pdfDoc      PDF文档对象
     * @param bookmark    书签节点
     * @param pageID      页面对象ID
     * @param targetIndex 目标页面索引
     * @throws IOException PDF解析异常
     */
    private void exportBookmark(PDDocument pdfDoc, PDOutlineNode bookmark, ST_ID pageID, int targetIndex) throws IOException {
        if (bookmark == null) {
            return;
        }

        PDOutlineItem current = bookmark.getFirstChild();
        while (current != null) {

            PDPageDestination pd = null;

            // 获取页码信息
            PDDestination destination = current.getDestination();
            if (destination instanceof PDPageDestination) {
                pd = (PDPageDestination) destination;
            } else if (destination instanceof PDNamedDestination) {
                pd = pdfDoc.getDocumentCatalog().findNamedDestinationPage((PDNamedDestination) destination);
            }

            if (current.getAction() instanceof PDActionGoTo) {
                PDActionGoTo gta = (PDActionGoTo) current.getAction();
                if (gta.getDestination() instanceof PDPageDestination) {
                    pd = (PDPageDestination) gta.getDestination();
                } else if (gta.getDestination() instanceof PDNamedDestination) {
                    pd = pdfDoc.getDocumentCatalog().findNamedDestinationPage((PDNamedDestination) gta.getDestination());
                }
            }
            // 根据书签目的地类型创建不同类型的书签
            if (pd != null && pd.retrievePageNumber() == targetIndex) {
                CT_Dest ctDest = null;
                if (destination instanceof PDPageXYZDestination) {
                    PDPageXYZDestination dst = (PDPageXYZDestination) destination;

                    ctDest = new CT_Dest().setPageID(pageID.ref());
                    ctDest.setType(DestType.XYZ);

                    float left = dst.getLeft();
                    if (left > 0) {
                        ctDest.setLeft(left / uuPmm);
                    }
                    if (dst.getTop() > 0) {
                        float top = dst.getPage().getBBox().getHeight() - dst.getTop();
                        ctDest.setTop(top / uuPmm);
                    }
                    float zoom = dst.getZoom();
                    if (zoom > 0) {
                        ctDest.setZoom(zoom);
                    }
//                    System.out.println("Page: " + dst.retrievePageNumber() + " ZXY left: " + dst.getLeft() + " top:" + dst.getTop() + " zoom: " + dst.getZoom() + " title: " + current.getTitle());
                } else if (destination instanceof PDPageFitHeightDestination) {
                    PDPageFitHeightDestination dst = (PDPageFitHeightDestination) destination;
                    ctDest = new CT_Dest().setPageID(pageID.ref());
                    ctDest.setType(DestType.FitV);
                    int left = dst.getLeft();
                    if (left > 0) {
                        ctDest.setLeft(left / uuPmm);
                    }
                } else if (destination instanceof PDPageFitWidthDestination) {
                    PDPageFitWidthDestination dst = (PDPageFitWidthDestination) destination;
                    ctDest = new CT_Dest().setPageID(pageID.ref());
                    ctDest.setType(DestType.FitH);

                    if (dst.getTop() > 0) {
                        float top = dst.getPage().getBBox().getHeight() - dst.getTop();
                        ctDest.setTop(top / uuPmm);
                    }
                } else if (destination instanceof PDPageFitRectangleDestination) {
                    PDPageFitRectangleDestination dst = (PDPageFitRectangleDestination) destination;
                    ctDest = new CT_Dest().setPageID(pageID.ref());
                    ctDest.setType(DestType.FitR);
                    float height = dst.getPage().getBBox().getHeight();

                    int left = dst.getLeft();
                    if (left > 0) {
                        ctDest.setLeft(left / uuPmm);
                    }
                    if (dst.getTop() > 0) {
                        float top = height - dst.getTop();
                        ctDest.setTop(top / uuPmm);
                    }
                    int right = dst.getRight();
                    if (right > 0) {
                        ctDest.setRight(right);
                    }
                    if (dst.getBottom() > 0) {
                        float bottom = height - dst.getBottom();
                        ctDest.setTop(bottom / uuPmm);
                    }
                }
                if (ctDest != null) {
                    if (bookmarks == null) {
                        bookmarks = new Bookmarks();
                        ofdDoc.document.setBookmarks(bookmarks);
                    }
                    // 添加书签
                    bookmarks.addBookmark(new Bookmark(current.getTitle(), ctDest));
                }
            }

            exportBookmark(pdfDoc, current, pageID, targetIndex);
            current = current.getNextSibling();
        }
    }

    /**
     * 复制文档中以及页面注释中的附件文件
     * <p>
     * ref: <a href="https://svn.apache.org/repos/asf/pdfbox/trunk/examples/src/main/java/org/apache/pdfbox/examples/pdmodel/ExtractEmbeddedFiles.java">PDFBox ExtractEmbeddedFiles.java</a>
     *
     * @param document    PDF文档
     * @param targetPages 目标页面
     * @throws IOException 文件复制异常
     */
    private void copyAttachFiles(PDDocument document, List<Integer> targetPages) throws IOException {
        PDDocumentNameDictionary namesDictionary = new PDDocumentNameDictionary(document.getDocumentCatalog());
        // 从全局获取文档附件
        PDEmbeddedFilesNameTreeNode efTree = namesDictionary.getEmbeddedFiles();
        if (efTree != null) {
            extractFilesFromEFTree(efTree);
        }

        // 从页面注解中获取附件
        for (Integer index : targetPages) {
            extractFilesFromPage(document.getPage(index));
        }
    }

    /**
     * 抽取某页PDF内注释相关联的附件到OFD
     *
     * @param page PDF页面
     * @throws IOException 文件复制异常
     */
    private void extractFilesFromPage(PDPage page) throws IOException {
        for (PDAnnotation annotation : page.getAnnotations()) {
            if (annotation instanceof PDAnnotationFileAttachment) {
                PDAnnotationFileAttachment annotationFileAttachment = (PDAnnotationFileAttachment) annotation;
                PDFileSpecification fileSpec = annotationFileAttachment.getFile();
                if (fileSpec instanceof PDComplexFileSpecification) {
                    addFileToOFD((PDComplexFileSpecification) fileSpec);
                }
            }
        }
    }

    /**
     * 从页树中抽取附件到OFD
     *
     * @param efTree 页树
     * @throws IOException 文件复制异常
     */
    private void extractFilesFromEFTree(PDNameTreeNode<PDComplexFileSpecification> efTree) throws IOException {
        Map<String, PDComplexFileSpecification> names = efTree.getNames();
        if (names != null) {
            for (Map.Entry<String, PDComplexFileSpecification> entry : names.entrySet()) {
                PDComplexFileSpecification fileSpec = entry.getValue();
                addFileToOFD(fileSpec);

            }
        } else {
            List<PDNameTreeNode<PDComplexFileSpecification>> kids = efTree.getKids();
            if (kids == null) {
                return;
            }
            for (PDNameTreeNode<PDComplexFileSpecification> node : kids) {
                extractFilesFromEFTree(node);
            }
        }
    }

    /**
     * 添加PDF附件文件到OFD内
     *
     * @param fileSpec 文件描述
     * @throws IOException 文件复制异常
     */
    private void addFileToOFD(PDComplexFileSpecification fileSpec) throws IOException {
        PDEmbeddedFile embeddedFile = getEmbeddedFile(fileSpec);
        if (embeddedFile == null) {
            return;
        }

        String filename = fileSpec.getFilename();
        String desp = fileSpec.getFileDescription();
        Calendar creationDate = embeddedFile.getCreationDate();
        Calendar modDate = embeddedFile.getModDate();
        String subtype = embeddedFile.getSubtype();

        CT_Attachment attObj = new CT_Attachment();
        if (desp != null && desp.length() > 0) {
            attObj.setUsage(desp);
        }
        String name = filename;
        int off = filename.lastIndexOf('.');
        if (off > 0) {
            name = filename.substring(0, off);
        }

        attObj.setAttachmentName(name);
        if (creationDate == null) {
            attObj.setCreationDate(LocalDateTime.now());
        } else {
            attObj.setCreationDate(LocalDateTime.ofInstant(creationDate.toInstant(), creationDate.getTimeZone().toZoneId()));
        }
        if (modDate != null) {
            attObj.setCreationDate(LocalDateTime.ofInstant(modDate.toInstant(), modDate.getTimeZone().toZoneId()));
        }
        attObj.setFormat(subtype);

        try (COSInputStream inputStream = embeddedFile.createInputStream()) {
            ofdDoc.addAttachment(attObj, inputStream);
        }
    }


    /**
     * 获取PDF文件附件对象
     *
     * @param fileSpec 附件描述
     * @return PDF附件
     */
    private PDEmbeddedFile getEmbeddedFile(PDComplexFileSpecification fileSpec) {
        // search for the first available alternative of the embedded file
        PDEmbeddedFile embeddedFile = null;
        if (fileSpec != null) {
            embeddedFile = fileSpec.getEmbeddedFileUnicode();
            if (embeddedFile == null) {
                embeddedFile = fileSpec.getEmbeddedFileDos();
            }
            if (embeddedFile == null) {
                embeddedFile = fileSpec.getEmbeddedFileMac();
            }
            if (embeddedFile == null) {
                embeddedFile = fileSpec.getEmbeddedFileUnix();
            }
            if (embeddedFile == null) {
                embeddedFile = fileSpec.getEmbeddedFile();
            }
        }
        return embeddedFile;
    }

    @Override
    public void close() throws IOException {
        if (closed) {
            return;
        }
        closed = true;
        if (fontRegistry != null) {
            try {
                fontRegistry.close();
            } catch (RuntimeException e) {
                // 字体写入失败不阻断 OFD 打包
            }
        }
        if (ofdDoc != null) {
            ofdDoc.close();
        }
    }
}
