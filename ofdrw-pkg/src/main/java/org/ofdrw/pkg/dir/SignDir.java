package org.ofdrw.pkg.dir;

import org.ofdrw.core.signatures.sig.Signature;

import java.nio.file.Path;

/**
 * 签名资源容器
 *
 * @author 权观宇
 * @since 2020-01-18 03:43:52
 */
public class SignDir {

    /**
     * 表示第几个签名
     */
    private Integer index;

    /**
     * 签名/签章 描述文件
     */
    private Signature signature;

    /**
     * 电子印章文件
     * <p>
     * Seal.esl
     */
    private Path seal;

    /**
     * 签名值文件
     */
    private Path signedValue;

    public SignDir() {
        this.index = 1;
    }

    /**
     * @return 第几个签名，从1开始
     */
    public Integer getIndex() {
        return index;
    }


    /**
     * 设置页码
     *
     * @param index 第几个签名
     */
    public SignDir setIndex(Integer index) {
        this.index = index;
        return this;
    }

    /**
     * @return 签名/签章 描述文件
     */
    public Signature getSignature() {
        return signature;
    }

    /**
     * 设置 签名/签章 描述文件
     *
     * @param signature 签名/签章 描述文件
     * @return this
     */
    public SignDir setSignature(Signature signature) {
        this.signature = signature;
        return this;
    }

    /**
     * @return 电子印章文件
     */
    public Path getSeal() {
        return seal;
    }

    /**
     * 设置电子印章文件
     *
     * @param seal 电子印章文件
     * @return this
     */
    public SignDir setSeal(Path seal) {
        this.seal = seal;
        return this;
    }

    /**
     * @return 签名值文件
     */
    public Path getSignedValue() {
        return signedValue;
    }

    /**
     * 设置签名值文件
     *
     * @param signedValue 签名值文件
     * @return this
     */
    public SignDir setSignedValue(Path signedValue) {
        this.signedValue = signedValue;
        return this;
    }
}
