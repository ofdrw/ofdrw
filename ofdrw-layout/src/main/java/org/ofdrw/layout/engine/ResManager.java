package org.ofdrw.layout.engine;

import org.ofdrw.core.OFDElement;
import org.ofdrw.core.basicStructure.res.CT_MultiMedia;
import org.ofdrw.core.basicStructure.res.MediaType;
import org.ofdrw.core.basicStructure.res.Res;
import org.ofdrw.core.basicStructure.res.resources.Fonts;
import org.ofdrw.core.basicStructure.res.resources.MultiMedias;
import org.ofdrw.core.basicType.ST_ID;
import org.ofdrw.core.basicType.ST_Loc;
import org.ofdrw.core.text.font.CT_Font;
import org.ofdrw.font.Font;
import org.ofdrw.pkg.dir.DocDir;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
     */
    private MultiMedias medias;

    /**
     * 字体资源列表
     */
    private Fonts fonts;

    /**
     * 资源缓存
     * <p>
     * 防止资源重复添加
     */
    private Map<String, OFDElement> cache;

    private ResManager() {
    }


    public ResManager(DocDir docDir, AtomicInteger maxUnitID) {
        this.docDir = docDir;
        this.maxUnitID = maxUnitID;
        this.cache = new HashMap<>();

        // 初始化字体缓存
        Res docRes = docDir.getDocumentRes();
        if (docRes != null) {
            List<Fonts> fontsList = docRes.getFonts();
            for (Fonts f : fontsList) {
                f.getFonts().forEach(item -> {
                    String completeFontName = item.getFontName();
                    String familyName = item.getFamilyName();
                    if (familyName != null && familyName.length() > 0) {
                        completeFontName += "-" + familyName;
                    }
                    // 加入缓存防止重复加入
                    cache.put(completeFontName, item);
                });
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
     */
    public ST_ID addFont(Font font) {
        Res resMenu = pubRes();
        if (this.fonts == null) {
            this.fonts = new Fonts();
            resMenu.addResource(this.fonts);
        }

        // 获取字体全名
        String completeFontName = font.getCompleteFontName();
        // 检查缓存
        if (cache.get(completeFontName) == null) {
            // 生成加入资源的ID
            ST_ID id = new ST_ID(maxUnitID.incrementAndGet());
            String familyName = font.getFamilyName();
            // 新建一个OFD字体对象
            CT_Font ctFont = new CT_Font()
                    .setFontName(font.getName())
                    .setFamilyName(familyName)
                    .setID(id)
                    .setFontFile("Res/" + font.getFontFileName());
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
            // 将字体文件加入到文档容器中
            docDir.addResource(font.getFontFile());
            // 把字体加入到字体清单中
            fonts.addFont(ctFont);
            // 缓存字体
            cache.put(completeFontName, ctFont);
            return id;
        } else {
            // 该资源已经加入过返回资源的ID
            return cache.get(completeFontName).getObjID();
        }
    }

    /**
     * 加入一个图片资源
     * <p>
     * 如果图片已经存在那么不会重复加入
     *
     * @param imgPath 图片路径
     * @return 资源ID
     */
    public ST_ID addImage(Path imgPath) {
        Res resMenu = docRes();
        if (medias == null) {
            this.medias = new MultiMedias();
            resMenu.addResource(medias);
        }
        String absPath = imgPath.toAbsolutePath().toString();
        String fileName = imgPath.getFileName().toString();
        if (cache.get(absPath) == null) {
            // 生成加入资源的ID
            ST_ID id = new ST_ID(maxUnitID.incrementAndGet());
            // 获取图片文件后缀名称
            String fileSuffix = pictureFormat(fileName);
            // 将文件加入资源容器中
            docDir.addResource(imgPath);
            // 创建图片对象
            CT_MultiMedia multiMedia = new CT_MultiMedia()
                    .setType(MediaType.Image)
                    .setFormat(fileSuffix)
                    .setMediaFile(ST_Loc.getInstance(fileName))
                    .setID(id);
            // 加入媒体类型清单
            medias.addMultiMedia(multiMedia);
            // 加入缓存
            cache.put(absPath, multiMedia);
            return id;
        } else {
            // 该资源已经加入过返回资源的ID
            return cache.get(absPath).getObjID();
        }
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
        Res publicRes = docDir.getPublicRes();
        if (publicRes == null) {
            // 如果不存在那么创建一个公共资源清单，容器目录为文档根目录下的Res目录
            publicRes = new Res()
                    .setBaseLoc(ST_Loc.getInstance("Res"));
            docDir.setPublicRes(publicRes);
            docDir.getDocument().getCommonData().setPublicRes(ST_Loc.getInstance("PublicRes.xml"));
        }
        return publicRes;
    }

    /**
     * 文档资源清单
     * <p>
     * 与文档相关的资源：图片、视频等
     *
     * @return 文档资源清单
     */
    public Res docRes() {
        Res docRes = docDir.getDocumentRes();
        if (docRes == null) {
            // 如果不存在那么创建一个公共资源清单，容器目录为文档根目录下的Res目录
            docRes = new Res()
                    .setBaseLoc(ST_Loc.getInstance("Res"));
            docDir.setDocumentRes(docRes);
            docDir.getDocument().getCommonData().setDocumentRes(ST_Loc.getInstance("DocumentRes.xml"));
        }
        return docRes;
    }


}
