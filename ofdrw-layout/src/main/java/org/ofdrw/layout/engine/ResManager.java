package org.ofdrw.layout.engine;

import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.ofdrw.core.OFDElement;
import org.ofdrw.core.basicStructure.doc.Document;
import org.ofdrw.core.basicStructure.res.CT_MultiMedia;
import org.ofdrw.core.basicStructure.res.MediaType;
import org.ofdrw.core.basicStructure.res.OFDResource;
import org.ofdrw.core.basicStructure.res.Res;
import org.ofdrw.core.basicStructure.res.resources.*;
import org.ofdrw.core.basicType.ST_ID;
import org.ofdrw.core.basicType.ST_Loc;
import org.ofdrw.core.compositeObj.CT_VectorG;
import org.ofdrw.core.pageDescription.color.colorSpace.CT_ColorSpace;
import org.ofdrw.core.pageDescription.drawParam.CT_DrawParam;
import org.ofdrw.core.text.font.CT_Font;
import org.ofdrw.font.Font;
import org.ofdrw.pkg.container.DocDir;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 公共资源管理器
 * <p>
 * 管理待加入文档中所有资源
 *
 * @author 权观宇
 * @since 2020-03-22 16:20:07
 */
public class ResManager {

    /**
     * 自增的ID生成器
     */
    private AtomicInteger maxUnitID;
    /**
     * 文档容器
     */
    private DocDir docDir;

    /**
     * 媒体资源列表
     * <p>
     * 位于文档资源列表
     */
    private MultiMedias medias;

    /**
     * 绘制参数列表
     * <p>
     * 位于文档资源列表
     */
    private DrawParams drawParams;


    /**
     * 字体资源列表
     * <p>
     * 位于公共资源列表
     */
    private Fonts fonts;

    /**
     * 颜色空间的描述列表
     * <p>
     * 位于公共资源列表
     */
    private ColorSpaces colorSpaces;

    /**
     * 矢量图像列表
     * <p>
     * 位于文档资源列表
     */
    private CompositeGraphicUnits compositeGraphicUnits;


    /**
     * 绘制参数Hash
     * <p>
     * KEY: 资源对象的去除ID后的XML字符串的hashCode
     * VALUE: 文档中的对象ID。
     * <p>
     * 该缓存表用于解决绘制参数冗余造成的资源浪费。
     */
    private final HashMap<Integer, ST_ID> resObjHash = new HashMap<>();

    private ResManager() {
    }


    /**
     * 创建资源管理
     *
     * @param docDir    文档虚拟容器
     * @param maxUnitID 自增最大ID提供者
     */
    public ResManager(DocDir docDir, AtomicInteger maxUnitID) {
        this();
        this.docDir = docDir;
        this.maxUnitID = maxUnitID;

        // 如果存在公共资源，尝试加载
        if (docDir.exist(DocDir.PublicResFileName)) {
            try {
                reloadRes(docDir.getPublicRes());
            } catch (FileNotFoundException e) {
                // ignore 文件不存在，不解析
            } catch (DocumentException e) {
                throw new RuntimeException("已有 PublicRes.xml 资源文件解析失败", e);
            }
        }
        // 如果存在文档资源，尝试加载
        if (docDir.exist(DocDir.DocumentResFileName)) {
            try {
                reloadRes(docDir.getDocumentRes());
            } catch (FileNotFoundException e) {
                // ignore 文件不存在，不解析
            } catch (DocumentException e) {
                throw new RuntimeException("已有 DocumentRes.xml 资源文件解析失败", e);
            }
        }
    }

    /**
     * 重载公共资源缓存
     */
    private void reloadRes(Res res) {
        List<OFDResource> resources = res.getResources();
        if (resources == null || resources.isEmpty()) {
            return;
        }
        for (OFDResource resource : resources) {
            // 获取各个集合下的资源对象
            List<Element> elements = resource.elements();
            if (elements == null || elements.isEmpty()) {
                continue;
            }
            for (Element ctResObj : elements) {
                // 获取原来资源对象的ID
                ST_ID id = ST_ID.getInstance(ctResObj.attributeValue("ID"));
                if (id == null) {
                    return;
                }
                // 遍历每一个资源对象，复制对象，删除对象ID，序列化为XML字符串
                Element copy = (Element) ctResObj.clone();
                copy.remove(copy.attribute("ID"));
                String key = copy.asXML();
                resObjHash.put(key.hashCode(), id);
            }
        }

    }

    /**
     * 增加字体资源
     * <p>
     * 如果字体已经被加入，那么不会重复加入
     *
     * @param font 字体描述对象
     * @return 字体的对象ID
     * @throws IOException 文件复制异常
     */
    public ST_ID addFont(Font font) throws IOException {
        // 获取字体全名
        String familyName = font.getFamilyName();
        // 新建一个OFD字体对象
        CT_Font ctFont = new CT_Font()
                .setFontName(font.getName())
                .setFamilyName(familyName);
        Path fontFile = font.getFontFile();
        if (fontFile != null) {
            // 将字体文件加入到文档容器中
            fontFile = docDir.addResourceWithPath(fontFile);
            ctFont.setFontFile(fontFile.getFileName().toString());
        }

        // 设置特殊字族属性
        if (familyName != null) {
            switch (familyName.toLowerCase()) {
                case "serif":
                    ctFont.setSerif(true);
                    break;
                case "bold":
                    ctFont.setBold(true);
                    break;
                case "italic":
                    ctFont.setItalic(true);
                    break;
                case "fixedwidth":
                    ctFont.setFixedWidth(true);
                    break;
            }
        }
        return addRawWithCache(ctFont);
    }

    /**
     * 加入一个图片资源
     * <p>
     * 如果图片已经存在那么不会重复加入
     *
     * @param imgPath 图片路径，请避免资源和文档中已经存在的资源重复
     * @return 资源ID
     * @throws IOException 文件复制异常
     */
    public ST_ID addImage(Path imgPath) throws IOException {
        // 将文件加入资源容器中，并获取资源在文件中的绝对路径
        Path imgCtnPath = docDir.addResourceWithPath(imgPath);
        // 获取在容器中的文件名称
        String fileName = imgCtnPath.getFileName().toString();


        // 获取图片文件后缀名称
        String fileSuffix = pictureFormat(fileName);
        // 创建图片对象
        CT_MultiMedia multiMedia = new CT_MultiMedia()
                .setType(MediaType.Image)
                .setFormat(fileSuffix)
                .setMediaFile(ST_Loc.getInstance(fileName));
        // 添加到资源列表中
        return addRawWithCache(multiMedia);
    }

    /**
     * 加入一个绘制参数
     * <p>
     * 如果图片已经存在那么不会重复加入
     *
     * @param param 绘制参数
     * @return 资源ID
     */
    public ST_ID addDrawParam(CT_DrawParam param) {
        return addRawWithCache(param);
    }

    /**
     * 根据图片名称推断图片格式
     *
     * @param fileName 图片文件名称
     * @return 图片格式字符串
     */
    private String pictureFormat(String fileName) {
        String fileSuffix = fileName.substring(fileName.lastIndexOf(".") + 1).toUpperCase();
        switch (fileSuffix) {
            case "JPG":
                return "JPEG";
            case "TIF":
                return "TIFF";
            default:
                return fileSuffix;
        }
    }

    /**
     * 获取公共资源清单
     * <p>
     * 如： 图形、字体等需要共用的资源
     *
     * @return 公共资源清单
     */
    public Res pubRes() {
        try {
            return docDir.getPublicRes();
        } catch (FileNotFoundException | DocumentException e) {
            // 如果不存在那么创建一个公共资源清单，容器目录为文档根目录下的Res目录
            Res publicRes = new Res().setBaseLoc(ST_Loc.getInstance("Res"));
            docDir.setPublicRes(publicRes);
            document().getCommonData().addPublicRes(ST_Loc.getInstance("PublicRes.xml"));
            return publicRes;
        }
    }

    /**
     * 文档资源清单
     * <p>
     * 与文档相关的资源：图片、视频等
     *
     * @return 文档资源清单
     */
    public Res docRes() {
        try {
            return docDir.getDocumentRes();
        } catch (FileNotFoundException | DocumentException e) {
            // 如果不存在那么创建一个公共资源清单，容器目录为文档根目录下的Res目录
            Res docRes = new Res().setBaseLoc(ST_Loc.getInstance("Res"));
            docDir.setDocumentRes(docRes);
            document().getCommonData().addDocumentRes(ST_Loc.getInstance("DocumentRes.xml"));
            return docRes;
        }
    }

    /**
     * 忽略无法获取到得到错误信息
     *
     * @return document对象
     */
    private Document document() {
        try {
            return docDir.getDocument();
        } catch (FileNotFoundException | DocumentException ex) {
            throw new RuntimeException("文档中缺少Document.xml 文件");
        }
    }


    /**
     * 直接向资源列表中加入资源对象
     * <p>
     * 注意：该方法是一个原生方法，具有一定的资源重复风险。
     *
     * @param resObj 资源对象
     * @return this
     * @deprecated {@link #addRawWithCache(OFDElement)}
     */
    @Deprecated
    public ResManager addRaw(OFDElement resObj) {
        if (resObj == null) {
            return this;
        }

        if (resObj instanceof CT_ColorSpace) {
            Res resMenu = pubRes();
            if (colorSpaces == null) {
                this.colorSpaces = new ColorSpaces();
                resMenu.addResource(colorSpaces);
            }
            colorSpaces.addColorSpace((CT_ColorSpace) resObj);
        } else if (resObj instanceof CT_Font) {
            Res resMenu = pubRes();
            if (fonts == null) {
                this.fonts = new Fonts();
                resMenu.addResource(fonts);
            }
            fonts.addFont((CT_Font) resObj);
        } else if (resObj instanceof CT_DrawParam) {
            Res resMenu = docRes();
            if (drawParams == null) {
                this.drawParams = new DrawParams();
                resMenu.addResource(drawParams);
            }
            drawParams.addDrawParam((CT_DrawParam) resObj);
        } else if (resObj instanceof CT_MultiMedia) {
            Res resMenu = docRes();
            if (medias == null) {
                this.medias = new MultiMedias();
                resMenu.addResource(medias);
            }
            medias.addMultiMedia((CT_MultiMedia) resObj);
        } else if (resObj instanceof CT_VectorG) {
            Res resMenu = docRes();
            if (compositeGraphicUnits == null) {
                this.compositeGraphicUnits = new CompositeGraphicUnits();
                resMenu.addResource(compositeGraphicUnits);
            }
            compositeGraphicUnits.addCompositeGraphicUnit((CT_VectorG) resObj);
        }
        return this;
    }


    /**
     * 直接向资源列表中加入资源对象
     * <p>
     * 加入资源时将优先检查缓存是否存在完全一致的资源，如果存在则复用对象。
     * <p>
     * 注意：加入对象的ID将被忽略，对象ID有资源管理器生成并设置。
     *
     * @param resObj 资源对象
     * @return 对象在文档中的资源ID
     */
    public ST_ID addRawWithCache(OFDElement resObj) {
        if (resObj == null) {
            return null;
        }

        // 移除对象上已经存在的用于基于资源本身的Hash值
        resObj.removeAttr("ID");
        int key = resObj.asXML().hashCode();

        ST_ID objId = this.resObjHash.get(key);
        if (objId != null) {
            // 文档中已经存在相同资源，则复用该资源。
            resObj.setObjID(objId);
            return objId;
        } else {
            // 文档中不存在该资源则资源ID，并缓存
            objId = new ST_ID(maxUnitID.incrementAndGet());
            resObj.setObjID(objId);
            this.resObjHash.put(key, objId);
        }

        // 判断资源类型加入到合适的资源列表中
        if (resObj instanceof CT_ColorSpace) {
            Res resMenu = pubRes();
            if (colorSpaces == null) {
                this.colorSpaces = new ColorSpaces();
                resMenu.addResource(colorSpaces);
            }
            colorSpaces.addColorSpace((CT_ColorSpace) resObj);
        } else if (resObj instanceof CT_Font) {
            Res resMenu = pubRes();
            if (fonts == null) {
                this.fonts = new Fonts();
                resMenu.addResource(fonts);
            }
            fonts.addFont((CT_Font) resObj);
        } else if (resObj instanceof CT_DrawParam) {
            Res resMenu = docRes();
            if (drawParams == null) {
                this.drawParams = new DrawParams();
                resMenu.addResource(drawParams);
            }
            drawParams.addDrawParam((CT_DrawParam) resObj);
        } else if (resObj instanceof CT_MultiMedia) {
            Res resMenu = docRes();
            if (medias == null) {
                this.medias = new MultiMedias();
                resMenu.addResource(medias);
            }
            medias.addMultiMedia((CT_MultiMedia) resObj);
        } else if (resObj instanceof CT_VectorG) {
            Res resMenu = docRes();
            if (compositeGraphicUnits == null) {
                this.compositeGraphicUnits = new CompositeGraphicUnits();
                resMenu.addResource(compositeGraphicUnits);
            }
            compositeGraphicUnits.addCompositeGraphicUnit((CT_VectorG) resObj);
        }
        return objId;
    }

    /**
     * 通过字族名获取字体对象，如果无法找到则返还null
     *
     * @param name 字体名称
     * @return 字体对象 或 null
     */
    public CT_Font getFont(String name) {
        if ("".equals(name)) {
            return null;
        }
        name = name.toLowerCase();

        // 尝试从公共资源中获取 字体清单
        Res resMenu = pubRes();
        List<Fonts> fontsList = resMenu.getFonts();
        for (Fonts fonts : fontsList) {
            List<CT_Font> arr = fonts.getFonts();
            for (CT_Font ctFont : arr) {
                // 忽略大小写的比较
                String fontName = ctFont.getFontName().toLowerCase();
                String familyName = ctFont.getFamilyName().toLowerCase();
                if (fontName.equals(name) || familyName.equals(name)) {
                    return ctFont;
                }
            }
        }

        // 尝试从文档资源中获取 字体清单
        resMenu = docRes();
        fontsList = resMenu.getFonts();
        for (Fonts fonts : fontsList) {
            List<CT_Font> arr = fonts.getFonts();
            for (CT_Font ctFont : arr) {
                // 忽略大小写的比较
                String fontName = ctFont.getFontName().toLowerCase();
                String familyName = ctFont.getFamilyName().toLowerCase();
                if (fontName.equals(name) || familyName.equals(name)) {
                    return ctFont;
                }
            }
        }
        return null;
    }
}
