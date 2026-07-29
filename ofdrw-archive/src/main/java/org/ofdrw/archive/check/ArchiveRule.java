package org.ofdrw.archive.check;

import org.ofdrw.archive.model.ArchiveViolation;
import org.ofdrw.pkg.container.OFDDir;
import org.ofdrw.reader.OFDReader;

import java.util.List;

/**
 * OFD-A 检查规则
 * <p>
 * 每条规则验证 OFD 文档的某一个约束是否满足 GB/T 42133-2022 标准。
 * 规则实现应为<b>无状态</b>，允许多次调用。
 * <p>
 * 实现类命名格式: XxxRule，与对应的处理器（XxxHandler）对应。
 *
 * @author 权观宇
 * @since 2.3.9
 */
@FunctionalInterface
public interface ArchiveRule {

    /**
     * 对打开的 OFD 文档执行检查
     * <p>
     * 检查过程只读，不修改文档内容。
     *
     * @param reader OFD 阅读器，提供资源、页面、附件等高级访问能力
     * @param ofdDir OFD 虚拟容器，提供低级的文件和目录操作
     * @return 发现的违规项列表（非 null），文档合规时返回空列表
     */
    List<ArchiveViolation> check(OFDReader reader, OFDDir ofdDir);
}
