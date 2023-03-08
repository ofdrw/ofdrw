package org.ofdrw.converter.export;

import org.ofdrw.converter.GeneralConvertException;
import org.ofdrw.converter.SVGMaker;
import org.ofdrw.reader.OFDReader;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * OFD SVG转换器
 *
 * @author 权观宇
 * @since 2023-3-8 21:45:48
 */
public class SVGExporter implements OFDExporter {

    /**
     * OFD解析器
     */
    final OFDReader ofdReader;
    /**
     * 图片转换器
     */
    final SVGMaker svgMaker;

    /**
     * SVG文件输出路径
     */
    final Path outDirPath;

    /**
     * 转换生成的图片文件序列
     */
    List<Path> svgFileArr;

    /**
     * 是否已经关闭
     */
    private boolean closed = false;

    /**
     * 构造图片转换器
     *
     * @param ofdFilePath 待转换OFD文件
     * @param imgDirPath  生成SVG存放目录
     * @throws IOException 文件解析异常
     */
    public SVGExporter(Path ofdFilePath, Path imgDirPath) throws IOException {
        this(ofdFilePath, imgDirPath, 15);
    }

    /**
     * 构造图片转换器
     *
     * @param ofdInput   待转换OFD文件流，该流由调用者负责关闭
     * @param imgDirPath 生成SVG存放目录
     * @throws IOException 文件解析异常
     */
    public SVGExporter(InputStream ofdInput, Path imgDirPath) throws IOException {
        this(ofdInput, imgDirPath, 15);
    }

    /**
     * 构造图片转换器
     *
     * @param ofdFilePath 待转换OFD文件
     * @param imgDirPath  生成SVG存放目录
     * @param ppm         转换SVG质量，每毫米像素数量(Pixels per millimeter)
     * @throws IOException 文件解析异常
     */
    public SVGExporter(Path ofdFilePath, Path imgDirPath, double ppm) throws IOException {
        ofdReader = new OFDReader(ofdFilePath);

        if (imgDirPath == null) {
            throw new IllegalArgumentException("导出图片文件路径为空");
        }
        imgDirPath = imgDirPath.toAbsolutePath();
        if (Files.exists(imgDirPath) && !Files.isDirectory(imgDirPath)) {
            throw new IllegalArgumentException("已经存在同名文件");
        }
        if (!Files.exists(imgDirPath)) {
            Files.createDirectories(imgDirPath);
        }
        svgMaker = new SVGMaker(ofdReader, ppm);
        svgMaker.config.setDrawBoundary(false);
        svgMaker.config.setClip(false);
        svgFileArr = new ArrayList<>();
        outDirPath = imgDirPath;
    }

    /**
     * 构造图片转换器
     *
     * @param ofdInput   待转换OFD文件流，该文件流由调用者负责关闭
     * @param imgDirPath 生成SVG存放目录
     * @param ppm        转换SVG质量，每毫米像素数量(Pixels per millimeter)
     * @throws IOException 文件解析异常
     */
    public SVGExporter(InputStream ofdInput, Path imgDirPath, double ppm) throws IOException {
        ofdReader = new OFDReader(ofdInput);
        if (imgDirPath == null) {
            throw new IllegalArgumentException("导出图片文件路径为空");
        }
        imgDirPath = imgDirPath.toAbsolutePath();
        if (Files.exists(imgDirPath) && !Files.isDirectory(imgDirPath)) {
            throw new IllegalArgumentException("已经存在同名文件");
        }
        if (!Files.exists(imgDirPath)) {
            Files.createDirectories(imgDirPath);
        }
        svgMaker = new SVGMaker(ofdReader, ppm);
        svgMaker.config.setDrawBoundary(false);
        svgMaker.config.setClip(false);
        svgFileArr = new ArrayList<>();
        outDirPath = imgDirPath;
    }

    /**
     * 导出指定OFD页为SVG
     *
     * @param indexes 页码序列，如果为空表示全部页码（注意：页码从0起）
     * @throws GeneralConvertException 转换异常
     */
    @Override
    public void export(int... indexes) throws GeneralConvertException {
        List<Integer> targetPages = new LinkedList<>();
        if (indexes == null || indexes.length == 0) {
            for (int i = 0; i < ofdReader.getNumberOfPages(); i++) {
                targetPages.add(i);
            }
        } else {
            int maxPageIndex = ofdReader.getNumberOfPages();
            // 获取指定页面信息
            for (int index : indexes) {
                if (index < 0 || index >= maxPageIndex) {
                    continue;
                }
                targetPages.add(index);
            }
        }
        try {
            for (Integer index : targetPages) {
                String svg = svgMaker.makePage(index);
                Path dst = this.outDirPath.resolve(svgFileArr.size() + ".svg");
                Files.write(dst, svg.getBytes());
                this.svgFileArr.add(dst);
            }
        } catch (IOException e) {
            throw new GeneralConvertException("SVG转换异常", e);
        }
    }

    @Override
    public void close() throws IOException {
        if (closed) {
            return;
        }
        closed = true;
        if (ofdReader != null) {
            ofdReader.close();
        }
    }

    /**
     * 获取已经转换完成的页面的图片路径
     *
     * @return 页面图片路径
     */
    public List<Path> getSvgFilePaths() {
        return svgFileArr;
    }

    /**
     * 设置转换SVG质量
     * <p>
     * 请在调用 {@link #export(int...)} 方法之前设置PPM！
     *
     * @param ppm 每毫米像素数量(Pixels per millimeter)
     */
    public void setPPM(double ppm) {
        if (svgMaker == null) {
            return;
        }
        svgMaker.setPPM(ppm);
    }
}
