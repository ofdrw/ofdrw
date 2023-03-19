package org.ofdrw.layout.element.canvas;

import org.ofdrw.core.basicType.ST_Array;
import org.ofdrw.core.basicType.ST_Box;
import org.ofdrw.core.graph.pathObj.AbbreviatedData;
import org.ofdrw.core.graph.pathObj.CT_Path;
import org.ofdrw.core.pageDescription.clips.Area;
import org.ofdrw.core.pageDescription.clips.CT_Clip;
import org.ofdrw.core.pageDescription.clips.Clips;

/**
 * 裁剪区域构造器
 *
 * @author 权观宇
 * @since 2020-05-04 16:11:55
 */
public class ClipFactory implements Cloneable {

    /**
     * 裁剪路径数据
     */
    private AbbreviatedData data;

    /**
     * 裁剪路径的变换矩阵
     */
    private ST_Array ctm;



    public ClipFactory(AbbreviatedData data, ST_Array ctm) {
        this.data = data;
        this.ctm = ctm;
    }

    public ClipFactory(AbbreviatedData data) {
        this.data = data;
    }

    public ClipFactory() {
    }


    public AbbreviatedData getData() {
        return data;
    }

    public ClipFactory setData(AbbreviatedData data) {
        this.data = data;
        return this;
    }

    public ST_Array getCtm() {
        return ctm;
    }

    public ClipFactory setCtm(ST_Array ctm) {
        this.ctm = ctm;
        return this;
    }
    /**
     * 构造裁剪对象
     *
     * @return 裁剪区
     */
    public Clips clips(){
        return clips(new ST_Box(0, 0, 210d, 297d));
    }
    /**
     * 构造裁剪对象
     *
     * @param boundary 裁剪边界
     * @return 裁剪区
     */
    public Clips clips(ST_Box boundary) {
        Clips clips = new Clips();
        Area area = new Area();
        if (ctm != null) {
            area.setCTM(ctm.clone());
        }
        CT_Path clipObj = new CT_Path().setAbbreviatedData(data.clone());
        clipObj.setBoundary(boundary);
        area.setClipObj(clipObj);
        return clips.addClip(new CT_Clip().addArea(area));
    }

    @Override
    public ClipFactory clone() {
        ClipFactory that = new ClipFactory();
        if (ctm != null) {
            that.ctm = this.ctm.clone();
        }
        if (data != null) {
            that.data = this.data.clone();
        }
        return that;
    }
}
