package org.ofdrw.pkg.dir;

import org.ofdrw.core.signatures.Signatures;
import org.ofdrw.pkg.tool.DocObjDump;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * 签名容器
 *
 * @author 权观宇
 * @since 2020-01-18 03:34:34
 */
public class SignsDir implements DirCollect {

    /**
     * 容器
     */
    private List<SignDir> container;

    /**
     * 签名列表文件
     */
    private Signatures signatures;

    public SignsDir() {
        this.container = new ArrayList<>(5);
    }

    /**
     * @return 签名列表文件
     */
    public Signatures getSignatures() {
        return signatures;
    }

    /**
     * 设置 签名列表文件
     *
     * @param signatures 签名列表文件
     * @return this
     */
    public SignsDir setSignatures(Signatures signatures) {
        this.signatures = signatures;
        return this;
    }

    /**
     * 增加页面签名
     *
     * @param page 页面容器
     * @return this
     */
    public SignsDir add(SignDir page) {
        if (container == null) {
            container = new ArrayList<>(5);
        }
        this.container.add(page);
        return this;
    }

    /**
     * 获取指定签名容器
     *
     * @param numberOfSign 第几个签名
     * @return this
     */
    public SignDir get(Integer numberOfSign) {
        if (container == null) {
            return null;
        }
        for (SignDir sign : container) {
            if (sign.getIndex().equals(numberOfSign)) {
                return sign;
            }
        }
        return null;
    }

    /**
     * 创建目录并复制文件
     *
     * @param base 基础路径
     * @return 创建的目录路径
     * @throws IOException IO异常
     */
    @Override
    public Path collect(String base) throws IOException {
        if (container == null || container.isEmpty()) {
            throw new IllegalArgumentException("缺少签名文件（SignDir）");
        }
        if (signatures == null) {
            throw new IllegalArgumentException("缺少签名列表文件（signatures）");
        }

        Path path = Paths.get(base, "Signs");
        path = Files.createDirectories(path);
        String dir = path.toAbsolutePath().toString();

        DocObjDump.dump(this.signatures, Paths.get(dir, "Signatures.xml"));
        for (SignDir p : container) {
            p.collect(dir);
        }
        return path;
    }
}
