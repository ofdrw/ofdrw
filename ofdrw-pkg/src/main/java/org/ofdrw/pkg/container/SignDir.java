package org.ofdrw.pkg.container;

import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.ofdrw.core.signatures.sig.Signature;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;

/**
 * 签名资源容器
 *
 * @author 权观宇
 * @since 2020-4-3 19:41:29
 */
public class SignDir extends VirtualContainer {

    /**
     * 签名容器名称前缀
     */
    public static final String SignContainerPrefix = "Sign_";

    /**
     * 电子印章文件名
     */
    public static final String SealFileName = "Seal.esl";
    /**
     * 签名/签章 描述文件名
     */
    public static final String SignatureFileName = "Signature.xml";

    /**
     * 签名值文件名
     */
    public static final String SignedValueFileName = "SignedValue.dat";

    /**
     * 表示第几个签名
     */
    private Integer index = 0;

    public SignDir(Path fullDir) throws IllegalArgumentException {
        super(fullDir);
        // 标准的签名目录名为 Sign_N (N代表第几个签名)
        String indexStr = this.getContainerName()
                .replace(SignContainerPrefix, "");
        try {
            index = Integer.parseInt(indexStr);
        } catch (NumberFormatException e) {
            clean();
            throw new IllegalArgumentException("不合法的文件目录名称：" + this.getContainerName() + "，目录名称应为 Sign_N");
        }
    }

    /**
     * @return 第几个签名，从1开始
     */
    public Integer getIndex() {
        return index;
    }


    /**
     * 获取 签名/签章 描述文件
     *
     * @return 签名/签章 描述文件
     * @throws FileNotFoundException 容器中不存在该文件
     * @throws DocumentException     XML文件解析异常，可能是格式不正确
     */
    public Signature getSignature() throws FileNotFoundException, DocumentException {
        Element ele = this.getObj(SignatureFileName);
        return new Signature(ele);
    }

    /**
     * 设置 签名/签章 描述文件
     *
     * @param signature 签名/签章 描述文件
     * @return this
     */
    public SignDir setSignature(Signature signature) {
        this.putObj(SignatureFileName, signature);
        return this;
    }

    /**
     * 获取 电子印章文件
     *
     * @return 电子印章文件
     * @throws FileNotFoundException 文件不存在
     */
    public Path getSeal() throws FileNotFoundException {
        return this.getFile(SealFileName);
    }

    /**
     * 设置电子印章文件
     *
     * @param seal 电子印章文件
     * @return this
     * @throws IOException 文件复制过程中异常
     */
    public SignDir setSeal(Path seal) throws IOException {
        this.putFile(seal);
        return this;
    }

    /**
     * 获取 签名值文件
     *
     * @return 签名值文件
     * @throws FileNotFoundException 文件不存在
     */
    public Path getSignedValue() throws FileNotFoundException {
        return this.getFile(SignedValueFileName);
    }

    /**
     * 设置签名值文件
     *
     * @param signedValue 签名值文件
     * @return this
     * @throws IOException 文件复制过程中异常
     */
    public SignDir setSignedValue(Path signedValue) throws IOException {
        this.putFile(signedValue);
        return this;
    }
}
