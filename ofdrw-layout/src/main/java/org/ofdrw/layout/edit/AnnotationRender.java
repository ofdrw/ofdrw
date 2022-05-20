package org.ofdrw.layout.edit;

import org.dom4j.DocumentException;
import org.ofdrw.core.annotation.Annotations;
import org.ofdrw.core.annotation.pageannot.AnnPage;
import org.ofdrw.core.annotation.pageannot.Annot;
import org.ofdrw.core.annotation.pageannot.Appearance;
import org.ofdrw.core.annotation.pageannot.PageAnnot;
import org.ofdrw.core.basicStructure.doc.Document;
import org.ofdrw.core.basicType.ST_Box;
import org.ofdrw.core.basicType.ST_ID;
import org.ofdrw.core.basicType.ST_Loc;
import org.ofdrw.layout.element.canvas.DrawContext;
import org.ofdrw.layout.element.canvas.Drawer;
import org.ofdrw.layout.engine.ResManager;
import org.ofdrw.layout.engine.render.RenderException;
import org.ofdrw.pkg.container.DocDir;
import org.ofdrw.pkg.container.PageDir;
import org.ofdrw.pkg.container.VirtualContainer;
import org.ofdrw.reader.BadOFDException;
import org.ofdrw.reader.ErrorPathException;
import org.ofdrw.reader.PageInfo;
import org.ofdrw.reader.ResourceLocator;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 注释对象渲染器
 *
 * @author 权观宇
 * @since 2020-05-15 18:42:49
 */
public class AnnotationRender {

    /**
     * 文档目录
     */
    private DocDir docDir;

    /**
     * 注释目录
     * <p>
     * 可能为空，为空时应该创建该目录
     * <p>
     * GMT0099 OFD 2.0
     */
    private VirtualContainer annotsDir;

    /**
     * 注释文件目录
     */
    private Annotations annotations = null;

    private ResourceLocator rl;
    private final ResManager prm;
    private final AtomicInteger maxUnitID;

    public AnnotationRender(DocDir docDir, ResManager prm, AtomicInteger maxUnitID) {
        this.docDir = docDir;
        this.prm = prm;
        this.maxUnitID = maxUnitID;
    }

    /**
     * 初始化注释文件结构
     * <p>
     * - 如果 注释容器 存在，则切换路径至该容器中。
     * - 如果 注释入口文件 不存在，则创建。
     */
    private void init() {
        if (annotations != null) {
            return;
        }

        Document document;
        try {
            document = docDir.getDocument();
        } catch (FileNotFoundException | DocumentException e) {
            throw new BadOFDException("错误的OFD结构无法获取到Document.xml文件", e);
        }

        // 文件加载器
        rl = new ResourceLocator(this.docDir);

        // 注释列表的文件
        ST_Loc annListFileLoc = document.getAnnotations();
        if (annListFileLoc != null && !rl.exist(annListFileLoc.toString())) {
            // 文件不存在则置空
            annListFileLoc = null;
        }

        // 文件不存在时创建注释列表文件。
        if (annListFileLoc != null) {
            try {
                annotations = rl.get(annListFileLoc, Annotations::new);
                String parent = annListFileLoc.parent();
                if (parent.toLowerCase().endsWith(DocDir.AnnotsDir.toLowerCase())) {
                    annotsDir = rl.getContainer(parent);
                } else {
                    // 如果目录不规范，那么创建新的目录
                    annotsDir = null;
                }
                // 切换目录到注解目录文件所在目录中
                rl.cd(parent);
            } catch (FileNotFoundException | DocumentException e) {
                System.err.println(e.getMessage());
                // 无法获取到注解对象，因此重建注解对象
            }
        }
        if (annotations == null) {
            // 创建 注释容器
            annotsDir = docDir.obtainAnnots();
            // 不存在注释列表文件需要创建该文件
            annotations = new Annotations();
            annotsDir.putObj(DocDir.AnnotationsFileName, annotations);
            document.setAnnotations(annotsDir.getAbsLoc().cat(DocDir.AnnotationsFileName));
        }

    }


    /**
     * 注释 渲染器
     *
     * @param pageInfo 需要渲染注释的页面信息
     * @param build    注释对象构造器
     * @throws RenderException 渲染发生错误
     * @throws IOException     文件操作异常
     */
    public void render(PageInfo pageInfo, Annotation build) throws RenderException, IOException {
        Drawer drawer = build.getDrawer();
        if (drawer == null) {
            return;
        }
        init();

        ST_ID id = pageInfo.getId();
        // 查找已经存在的页面注释文件
        AnnPage record = annotations.getByPageId(id.toString());
        // 用于存放注解对象的容器
        PageAnnot pageAnnot = null;
        if (record == null) {
            // 不存在该页的注释文件，创建新的页面注释条目
            record = new AnnPage().setPageID(pageInfo.getId());
            annotations.addPage(record);
        } else {
            // 获取已经存在的 分页注释文件
            ST_Loc annPageFileLoc = record.getFileLoc();
            try {
                // 从文件中加载
                pageAnnot = rl.get(annPageFileLoc, PageAnnot::new);
            } catch (FileNotFoundException | ErrorPathException | DocumentException e) {
                // 如果文件不存
                pageAnnot = null;
            }
        }

        // 页面关联的分页注释文件不存在则创建
        if (pageAnnot == null) {
            if (annotsDir == null) {
                // 若不存在注释目录则创建
                annotsDir = docDir.obtainAnnots();
            }

            // 获取注释容器
            String containerName = PageDir.PageContainerPrefix + pageInfo.getPageN();
            PageDir pageDir = annotsDir.obtainContainer(containerName, PageDir::new);

            // 创建 分页注释文件 PageAnnot
            pageAnnot = new PageAnnot();
            ST_Loc pageAnnotLoc = pageDir.addAnnot(pageAnnot);
            // 重新设置分页注释条目位置为页面所处位置加上文件名
            record.setFileLoc(pageAnnotLoc);
        }

        // 获取注解对象，设置ID
        Annot annot = build.build();
        annot.setObjID(maxUnitID.incrementAndGet());
        // 加入注释容器中
        pageAnnot.addAnnot(annot);
        Appearance container = annot.getAppearance();
        ST_Box box = container.getBoundary()
                .clone()
                .setTopLeftX(0d)
                .setTopLeftY(0d);
        // 创建绘制上下文
        DrawContext ctx = new DrawContext(container, box, maxUnitID, prm);
        // 绘制注解内容
        drawer.draw(ctx);
    }
}
