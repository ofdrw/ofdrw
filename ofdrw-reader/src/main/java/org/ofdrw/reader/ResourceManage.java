package org.ofdrw.reader;

import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.ofdrw.core.basicStructure.doc.CT_CommonData;
import org.ofdrw.core.basicStructure.doc.Document;
import org.ofdrw.core.basicStructure.ofd.DocBody;
import org.ofdrw.core.basicStructure.ofd.OFD;
import org.ofdrw.core.basicStructure.res.CT_MultiMedia;
import org.ofdrw.core.basicStructure.res.OFDResource;
import org.ofdrw.core.basicStructure.res.Res;
import org.ofdrw.core.basicStructure.res.resources.*;
import org.ofdrw.core.basicType.ST_Loc;
import org.ofdrw.core.compositeObj.CT_VectorG;
import org.ofdrw.core.pageDescription.color.colorSpace.CT_ColorSpace;
import org.ofdrw.core.pageDescription.drawParam.CT_DrawParam;
import org.ofdrw.core.text.font.CT_Font;
import org.ofdrw.pkg.container.OFDDir;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 资源管理器
 * <p>
 * 使用ID随机访问文档中出现的资源对象
 * <p>
 * 包括 公共资源序列（PublicRes） 和 文档资源序列（DocumentRes）
 * <p>
 * 注意：资源管理器提供的资源对象均为只读对象（副本），不允许对资源进行修改。
 *
 * @author 权观宇
 * @since 2021-04-10 11:06:00
 */
public class ResourceManage {
    /**
     * 颜色空间
     */
    private final Map<String, CT_ColorSpace> colorSpaceMap = new HashMap<>();
    /**
     * 绘制参数
     */
    private final Map<String, CT_DrawParam> drawParamMap = new HashMap<>();
    /**
     * 字形
     */
    private final Map<String, CT_Font> fontMap = new HashMap<>();
    /**
     * 多媒体对象
     */
    private final Map<String, CT_MultiMedia> multiMediaMap = new HashMap<>();
    /**
     * 矢量图像
     */
    private final Map<String, CT_VectorG> compositeGraphicUnitMap = new HashMap<>();


    private final OFDReader ofdReader;


    /**
     * 创建资源管理器
     * <p>
     * 选择默认文档（Doc_0）进行资源的加载
     *
     * @param ofdReader OFD解析器
     */
    public ResourceManage(OFDReader ofdReader) {
        this.ofdReader = ofdReader;
        try {
            loadDefaultDoc();
        } catch (Exception e) {
            throw new RuntimeException("文档结构解析异常", e);
        }
    }

    /**
     * 指定文档创建资源管理器
     *
     * @param ofdReader OFD解析器
     * @param docNum    文档序号，从0起
     */
    public ResourceManage(OFDReader ofdReader, int docNum) {
        this.ofdReader = ofdReader;
        try {
            loadDoc(docNum);
        } catch (Exception e) {
            throw new RuntimeException("文档结构解析异常", e);
        }
    }

    /**
     * 获取绘制参数
     * <p>
     * 注意：资源管理器提供的资源对象均为只读对象（副本），不允许对资源进行修改。
     *
     * @param id 资源ID
     * @return 绘制参数，不存在返回null
     */
    public CT_DrawParam getDrawParam(String id) {
        return drawParamMap.get(id);
    }

    /**
     * 获取多媒体对象
     * <p>
     * 注意：资源管理器提供的资源对象均为只读对象（副本），不允许对资源进行修改。
     *
     * @param id 资源ID
     * @return 多媒体对象，不存在返回null
     */
    public CT_MultiMedia getMultiMedia(String id) {
        return multiMediaMap.get(id);
    }

    /**
     * 获取 字形
     * <p>
     * 注意：资源管理器提供的资源对象均为只读对象（副本），不允许对资源进行修改。
     *
     * @param id 资源ID
     * @return 字形，不存在返回null
     */
    public CT_Font getFont(String id) {
        return fontMap.get(id);
    }

    /**
     * 获取颜色空间
     * <p>
     * 注意：资源管理器提供的资源对象均为只读对象（副本），不允许对资源进行修改。
     *
     * @param id 资源ID
     * @return 颜色空间，不存在返回null
     */
    public CT_ColorSpace getColorSpace(String id) {
        return colorSpaceMap.get(id);
    }

    /**
     * 获取矢量图形
     * <p>
     * 注意：资源管理器提供的资源对象均为只读对象（副本），不允许对资源进行修改。
     *
     * @param id 资源ID
     * @return 矢量图形，不存在返回null
     */
    public CT_VectorG getCompositeGraphicUnit(String id) {
        return compositeGraphicUnitMap.get(id);
    }

    /**
     * 加载指定文档的资源
     * <p>
     * 由于每个文档的ID体系都是独立的，
     * <p>
     * 所以资源也是独立的，因此每次加载都会对上一个文档的资源进行清理。
     *
     * @param docNum 文档序号，从0起
     * @return this
     * @throws IOException       文件读写异常
     * @throws DocumentException 文档解析异常
     */
    public ResourceManage loadDoc(int docNum) throws IOException, DocumentException {

        final OFDDir ofdDir = ofdReader.getOFDDir();
        // 根目录: /OFD.xml
        final OFD ofd = ofdDir.getOfd();
        final DocBody docBody = ofd.getDocBody(docNum);

        // 由于每个文档的ID体系都是独立的，所以资源也是独立的
        // 这里对上一个文档的资源进行清理
        colorSpaceMap.clear();
        drawParamMap.clear();
        fontMap.clear();
        multiMediaMap.clear();
        compositeGraphicUnitMap.clear();
        // 重新加载资源
        loadDocRes(docBody);
        return this;
    }

    /**
     * 多文档资源加载
     *
     * @throws IOException       文件读写异常
     * @throws DocumentException 文档解析异常
     */
    private void loadDefaultDoc() throws IOException, DocumentException {
        final OFDDir ofdDir = ofdReader.getOFDDir();
        // 根目录: /OFD.xml
        final OFD ofd = ofdDir.getOfd();
        // 加载OFD中出现的第一个文档
        loadDocRes(ofd.getDocBody());
    }

    /**
     * 加载文档中的资源
     *
     * @param docBody 文档描述信息
     * @throws IOException       文件读写异常
     * @throws DocumentException 文档解析异常
     */
    private void loadDocRes(DocBody docBody) throws IOException, DocumentException {
        final ResourceLocator rl = ofdReader.getResourceLocator();
        try {
            rl.save();
            final ST_Loc docRoot = docBody.getDocRoot();
            // 路径解析对象获取并缓存虚拟容器
            Document document = rl.get(docRoot, Document::new);
            rl.cd(docRoot.parent());

            final CT_CommonData commonData = document.getCommonData();
            // 公共资源序列（PublicRes）
            loadResFile(rl, commonData.getPublicRes());
            // 文档资源序列（DocumentRes）
            loadResFile(rl, commonData.getDocumentRes());
        } finally {
            rl.restore();
        }
    }

    /**
     * 加载资源文文件中描述的资源对象
     * <p>
     * 该方法不应该抛出异常所有异常均应该被忽略以便程序继续执行
     *
     * @param rl     资源加载器
     * @param resLoc 资源文件位置
     */
    private void loadResFile(ResourceLocator rl, ST_Loc resLoc) {
        if (resLoc == null) {
            return;
        }
        try {
            rl.save();
            rl.cd(resLoc.parent());
            Res res = rl.get(resLoc, Res::new);
            // 如果资源文件的通用存储路径
            final ST_Loc baseLoc = res.getBaseLoc();
            // 遍历每一个资源对象
            for (OFDResource ofdResource : res.getResources()) {
                // 颜色空间
                if (ofdResource instanceof ColorSpaces) {
                    for (CT_ColorSpace colorSpace : ((ColorSpaces) ofdResource).getColorSpaces()) {
                        // 复制副本，作为只读对象
                        CT_ColorSpace item = new CT_ColorSpace((Element) colorSpace.clone());
                        // 如果文件路径存在，则转换为绝对路径
                        if (item.getProfile() != null) {
                            // 转换文件路径为绝对地址
                            ST_Loc absProfile = abs(rl, baseLoc, item.getProfile());
                            // 替换地址
                            item.setProfile(absProfile);
                        }
                        colorSpaceMap.put(item.getID().toString(), item);
                    }
                    continue;
                }
                // 绘制参数
                if (ofdResource instanceof DrawParams) {
                    for (CT_DrawParam drawParam : ((DrawParams) ofdResource).getDrawParams()) {
                        // 复制副本，作为只读对象
                        CT_DrawParam item = new CT_DrawParam((Element) drawParam.clone());
                        drawParamMap.put(item.getID().toString(), item);
                    }
                    continue;
                }
                // 字体
                if (ofdResource instanceof Fonts) {
                    for (CT_Font font : ((Fonts) ofdResource).getFonts()) {
                        // 复制副本，作为只读对象
                        CT_Font item = new CT_Font((Element) font.clone());
                        // 如果地址存在，则转换为绝对路径
                        if (item.getFontFile() != null) {
                            // 转换文件路径为绝对地址
                            ST_Loc absFontFile = abs(rl, baseLoc, item.getFontFile());
                            // 替换地址
                            item.setFontFile(absFontFile);
                        }
                        fontMap.put(item.getID().toString(), item);
                    }
                    continue;
                }
                // 媒体对象
                if (ofdResource instanceof MultiMedias) {
                    for (CT_MultiMedia multiMedia : ((MultiMedias) ofdResource).getMultiMedias()) {
                        // 复制副本，作为只读对象
                        CT_MultiMedia item = new CT_MultiMedia((Element) multiMedia.clone());
                        // 如果地址存在，则转换为绝对路径
                        if (item.getMediaFile() != null) {
                            // 转换文件路径为绝对地址
                            ST_Loc absMediaFile = abs(rl, baseLoc, item.getMediaFile());
                            item.setMediaFile(absMediaFile);
                        }
                        multiMediaMap.put(item.getID().toString(), item);
                    }
                    continue;
                }

                // 矢量图形
                if (ofdResource instanceof CompositeGraphicUnits) {
                    for (CT_VectorG ctVectorG : ((CompositeGraphicUnits) ofdResource).getCompositeGraphicUnits()) {
                        // 复制副本，作为只读对象
                        CT_VectorG item = new CT_VectorG((Element) ctVectorG.clone());
                        compositeGraphicUnitMap.put(item.getID().toString(), item);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("无法解析资源描述文件 " + resLoc.toString() + " " + e.getMessage());
        } finally {
            rl.restore();
        }
    }

    /**
     * 获取资源的绝对地址
     *
     * @param rl     资源加载器
     * @param base   资源文件的通用存储路径
     * @param target 资源文件路径
     * @return 资源文件绝对地址
     */
    private ST_Loc abs(ResourceLocator rl, ST_Loc base, ST_Loc target) {
        // 目标路径不存在那么就返还null
        if (target == null) {
            return null;
        }

        ST_Loc absLoc;
        if (base != null) {
            // 如果存在 资源文件的通用存储路径，那么以 通用存储路径 为基础拼接目标路径作为绝对路径
            absLoc = rl.getAbsTo(base);
            absLoc = absLoc.cat(target);
        } else {
            // 不存在 通用存储路径 直接根据但前目录位置获取到绝对路径
            absLoc = rl.getAbsTo(target);
        }
        return absLoc;
    }

    /**
     * 获取文档中所有 颜色空间
     * <p>
     * 注意：资源管理器提供的资源对象均为只读对象（副本），不允许对资源进行修改。
     *
     * @return 颜色空间列表
     */
    public List<CT_ColorSpace> getColorSpaces() {
        return new ArrayList<CT_ColorSpace>(colorSpaceMap.values());
    }

    /**
     * 获取文档中所有 绘制参数
     * <p>
     * 注意：资源管理器提供的资源对象均为只读对象（副本），不允许对资源进行修改。
     *
     * @return 绘制参数
     */
    public List<CT_DrawParam> getDrawParams() {
        return new ArrayList<>(drawParamMap.values());
    }

    /**
     * 获取文档中所有 字形
     * <p>
     * 注意：资源管理器提供的资源对象均为只读对象（副本），不允许对资源进行修改。
     *
     * @return 字形
     */
    public List<CT_Font> getFonts() {
        return new ArrayList<>(fontMap.values());
    }

    /**
     * 获取文档中所有 媒体对象
     * <p>
     * 注意：资源管理器提供的资源对象均为只读对象（副本），不允许对资源进行修改。
     *
     * @return 媒体对象
     */
    public List<CT_MultiMedia> getMultiMedias() {
        return new ArrayList<>(multiMediaMap.values());
    }

    /**
     * 获取文档中所有 矢量图形
     * <p>
     * 注意：资源管理器提供的资源对象均为只读对象（副本），不允许对资源进行修改。
     *
     * @return 矢量图形
     */
    public List<CT_VectorG> getCompositeGraphicUnits() {
        return new ArrayList<>(compositeGraphicUnitMap.values());
    }
}
