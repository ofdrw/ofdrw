package org.ofdrw.archive.convert;

import org.ofdrw.pkg.container.OFDDir;
import org.ofdrw.reader.OFDReader;

import java.io.IOException;

/**
 * OFD-A 转换处理器
 * <p>
 * 每个处理器执行一个具体的"去技术化"操作，<b>就地修改</b> OFDDir 虚拟容器中的文件。
 * 处理器按编排顺序依次执行（如先解密后改 DocType）。
 * 处理器实现应为<b>无状态</b>，允许多次调用。
 * <p>
 * 实现类命名格式: XxxHandler，与对应的检查规则（XxxRule）对应。
 *
 * @author xxx
 * @since 2.3.9
 */
@FunctionalInterface
public interface ArchiveHandler {

    /**
     * 执行去技术化转换操作，就地修改 OFD 容器
     * <p>
     * 处理结果直接写入 ofdDir，调用者负责最终持久化和打包。
     *
     * @param reader OFD 阅读器，提供文档结构访问
     * @param ofdDir OFD 虚拟容器，修改结果写入此处
     * @throws IOException 文件操作异常
     */
    void handle(OFDReader reader, OFDDir ofdDir) throws IOException;
}
