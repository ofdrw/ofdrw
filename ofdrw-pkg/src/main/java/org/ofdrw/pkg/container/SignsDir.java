package org.ofdrw.pkg.container;

import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.ofdrw.core.signatures.Signatures;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.Path;

/**
 * 签名容器
 *
 * @author 权观宇
 * @since 2020-01-18 03:34:34
 */
public class SignsDir extends VirtualContainer {

    /**
     * 签名Index最大值 + 1
     * <p>
     * 也就是签名容器数量，因为签名容器Index从0起算
     */
    private int maxSignIndex = 0;

    /**
     * 签名列表文件名称
     */
    public static final String SignaturesFileName = "Signatures.xml";


    public SignsDir(Path fullDir) throws IllegalArgumentException {
        super(fullDir);
        initContainer();
    }

    /**
     * 初始化容器
     */
    private void initContainer() {
        File fullDirFile = new File(getSysAbsPath());
        File[] files = fullDirFile.listFiles();
        if (files != null) {
            // 遍历容器中已经有的签名目录，初始签名数量
            for (File f : files) {
                String dirName = f.getName();
                // 签名目录名为： Sign_N
                if (dirName.startsWith(SignDir.SignContainerPrefix)) {
                    String numb = dirName.replace(SignDir.SignContainerPrefix, "");
                    int num = Integer.parseInt(numb);
                    if (maxSignIndex <= num) {
                        maxSignIndex = num + 1;
                    }
                }
            }
        }
    }

    /**
     * 获取 签名列表文件
     *
     * @return 签名列表文件
     * @throws FileNotFoundException 容器中不存在该文件
     * @throws DocumentException     XML文件解析异常，可能是格式不正确
     */
    public Signatures getSignatures() throws FileNotFoundException, DocumentException {
        Element element = this.getObj(SignaturesFileName);
        return new Signatures(element);
    }

    /**
     * 设置 签名列表文件
     *
     * @param signatures 签名列表文件
     * @return this
     */
    public SignsDir setSignatures(Signatures signatures) {
        this.putObj(SignaturesFileName, signatures);
        return this;
    }

    /**
     * 创建一个签名/章虚拟容器
     *
     * @return 签名/章虚拟容器
     */
    public SignDir newSignDir() {
        // 新的签名容器一定是最大Index，并且此时目录中并不存在该目录
        String name = SignDir.SignContainerPrefix + maxSignIndex;
        maxSignIndex++;
        // 创建容器
        return this.obtainContainer(name, SignDir::new);
    }

    /**
     * 获取指定签名容器
     *
     * @param index 第几个签名
     * @return 签名容器
     * @throws FileNotFoundException 指定的签名容器不存在
     */
    public SignDir getByIndex(Integer index) throws FileNotFoundException {
        if (index == null || index <= 0) {
            throw new NumberFormatException("签名容器index必须大于0");
        }
        String containerName = SignDir.SignContainerPrefix + index;
        return this.getContainer(containerName, SignDir::new);
    }
    public SignDir getSignDir(String containerName) throws FileNotFoundException {
        return this.getContainer(containerName, SignDir::new);
    }
}
