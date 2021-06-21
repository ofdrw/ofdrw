package org.ofdrw.core.crypto;

/**
 * 密码保护方案标识符
 * <p>
 * 见GMT 0099-2020 A.1 密码保护方案 表 A.1 密码保护方案标识符
 *
 * @author 权观宇
 * @since 2021-06-21 18:38:16
 */
public enum ProtectionCaseID {

    /**
     * 采用口令方式加密（国密）
     * <p>
     * 对称算法 SM4，CBC模式
     * CBC模式的初始化向量放入附录C密钥描述文件内，
     * 填充算法遵循PKCS#7，分块长度为16字节（8位）
     */
    EncryptGMPassword("1.1.1"),
    /**
     * 采用口证书方式加密（国密）
     * <p>
     * 对称算法 SM4，CBC模式。非对称算法SM2。
     * CBC模式的初始化向量放入附录C密钥描述文件内，
     * 填充算法遵循PKCS#7，分块长度为16字节（8位）
     */
    EncryptGMCert("1.1.2"),
    /*
     * 1.1.3 ~ 1.1.255 保留
     */

    /**
     * 采用证书进行数字签名
     * <p>
     * 签名应遵循GT/T 35275 信息安全技术 SM2 密码算法加密签名消息语法规范
     */
    Signature("1.2.1");
    /*
     * 1.2.2 ~ 1.1.255 保留
     */

    /**
     * 方案标识符
     */
    private String id;

    ProtectionCaseID(String id) {
        this.id = id;
    }

    /**
     * 获取ID实例
     *
     * @param id id字符串
     * @return 实例
     */
    public static ProtectionCaseID getInstance(String id) {
        switch (id) {
            case "1.1.1":
                return EncryptGMPassword;
            case "1.1.2":
                return EncryptGMCert;
            case "1.2.1":
                return Signature;
            default:
                return null;
        }
    }

    public String getId() {
        return id;
    }

    @Override
    public String toString() {
        return id;
    }
}
