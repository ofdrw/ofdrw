package org.ofdrw.converter.pdf.render;

/**
 * 图像数据来源：按资源 ID 提供图像原始字节与尺寸
 * <p>
 * 抽出接口是为了让图像渲染器与 OFD 文档解析解耦：生产实现为
 * {@link ImageResourceLoader}（读取 OFD 资源文件），测试可用内存桩实现，
 * 从而对“JPEG 原始字节不重编码”等保真约束做渲染器无关的断言。
 *
 * @author 权观宇
 * @since 2.5.1
 */
public interface ImageSource {

    /**
     * 加载图像资源
     *
     * @param resourceId 图像资源 ID（{@code ST_RefID} 的字符串形式）
     * @return 图像数据；资源缺失或读取失败时返回 {@code null}
     */
    ImageResourceLoader.ImageData load(String resourceId);
}
