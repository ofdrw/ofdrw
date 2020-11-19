package org.ofdrw.converter.image;

/**
 * 图像媒体对象
 *
 * @author minghu.zhang
 * @since 12:10 2020/11/14
 **/
public class ImageMedia {

    /**
     * 图像格式
     */
    private String fromat;
    /**
     * 图像数据
     */
    private byte[] data;

    public String getFromat() {
        return fromat;
    }

    public void setFromat(String fromat) {
        this.fromat = fromat;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }
}
