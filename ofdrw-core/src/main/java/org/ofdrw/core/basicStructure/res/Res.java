package org.ofdrw.core.basicStructure.res;

import org.dom4j.Element;
import org.ofdrw.core.OFDElement;
import org.ofdrw.core.basicStructure.res.resources.Fonts;
import org.ofdrw.core.basicType.ST_Loc;

import java.util.ArrayList;
import java.util.List;

/**
 * 资源
 * <p>
 * 资源是绘制图元时所需数据（如绘制参数、颜色空间、字形、图像、音视频等）的集合。
 * 在页面中出现的资源数据内容都保存在容器的特定文件夹内，但其索引信息保存在资源文件中。
 * 一个文档可能包含一个或多个资源文件。资源根据作用范围分为公共资源和页资源，公共资源文件
 * 在文档根节点中进行制定，页资源文件在页对象中进行制定。
 * <p>
 * 7.9 资源 图 20
 *
 * @author 权观宇
 * @since 2019-10-11 06:00:24
 */
public class Res extends OFDElement {

    public Res(Element proxy) {
        super(proxy);
    }

    public Res() {
        super("Res");
    }

    /**
     * 【必选 属性】
     * 设置 此资源文件的通用数据存储路径。
     * <p>
     * BaseLoc属性的意义在于明确资源文件存储位置，
     * 比如 R1.xml 中可以指定 BaseLoc为“./Res”，
     * 表明该资源文件中所有数据文件的默认存储位置在
     * 当前路径的 Res 目录下。
     *
     * @param baseLoc 此资源文件的通用数据存储路径
     * @return this
     */
    public Res setBaseLoc(ST_Loc baseLoc) {
        this.addAttribute("BaseLoc", baseLoc.toString());
        return this;
    }

    /**
     * 【必选 属性】
     * 获取 此资源文件的通用数据存储路径。
     * <p>
     * BaseLoc属性的意义在于明确资源文件存储位置，
     * 比如 R1.xml 中可以指定 BaseLoc为“./Res”，
     * 表明该资源文件中所有数据文件的默认存储位置在
     * 当前路径的 Res 目录下。
     *
     * @return 此资源文件的通用数据存储路径
     */
    public ST_Loc getBaseLoc() {
        return ST_Loc.getInstance(this.attributeValue("BaseLoc"));
    }

    /**
     * 【可选】
     * 添加 资源
     * <p>
     * 一个资源文件可描述0到多个资源
     *
     * @param resource 资源
     * @return this
     */
    public Res addResource(OFDResource resource) {
        this.add(resource);
        return this;
    }

    /**
     * 获取字体资源文件
     *
     * @return 字体资源列表
     */
    public List<Fonts> getFonts() {
        List<Fonts> fontsList = new ArrayList<>();
        for (OFDResource item : getResources()) {
            if (item instanceof Fonts) {
                fontsList.add((Fonts) item);
            }
        }
        return fontsList;
    }

    /**
     * 【可选】
     * 获取 资源列表
     * <p>
     * 一个资源文件可描述0到多个资源
     * <p>
     * tip：可以使用<code>instanceof</code>判断是哪一种资源
     *
     * @return 资源列表
     */
    public List<OFDResource> getResources() {
        List<Element> elements = this.elements();
        List<OFDResource> res = new ArrayList<>(elements.size());
        elements.forEach(item -> res.add(OFDResource.getInstance(item)));
        return res;
    }
}
