# 签名签章 验证 快速入门

***免责：电子文档的电子签章流程需要符合国家相关标准和规范。要产生具有效力的电子签名/签章，请使用符合国家密码局要求具有相关型号证书的密码设备进行。***


`ofdrw-sign`已经对OFD文档的电子签章过程完成比较高程度的封装，
开发者只需要关注如何实现签章即可。

引入依赖
```xml
<dependency>
    <groupId>org.ofdrw</groupId>
    <artifactId>ofdrw-sign</artifactId>
    <version>1.7.3</version>
</dependency>
```


实现电子签章你需要下面步骤：

1. 通过OFD解析器创建OFDSigner（签名引擎）。
2. **实现扩展数字签名容器**。
3. 设置签名模式。
4. 设置签名使用的扩展签名容器。
5. 执行签名。
6. 关闭签名引擎，生成文档。

```java
public static void main() throws Exception {
     Path src = Paths.get("src/test/resources", "helloworld.ofd");
     Path out = Paths.get("target/DigitalSign.ofd");
     // 1. 构造签名引擎
     try (OFDReader reader = new OFDReader(src);
          OFDSigner signer = new OFDSigner(reader, out)) {
         // 2. 实现电子签章容器
         ExtendSignatureContainer signContainer = new YouImpContainer();
         // 3. 设置签名模式
         //  signer.setSignMode(SignMode.WholeProtected);
         signer.setSignMode(SignMode.ContinueSign);
         // 4. 设置签名使用的扩展签名容器
         signer.setSignContainer(signContainer);
         // 5. 执行签名
         signer.exeSign();
         // 6. 关闭签名引擎，生成文档。
     }
     System.out.println(">> 生成文件位置: " + out.toAbsolutePath().toAbsolutePath());
 }
```

为了便于测试的需要OFD R&W Sign模块中提供几个用于测试的签名扩展实现

- [数字电子签名实现 DigitalSignContainer](../../src/main/java/org/ofdrw/sign/signContainer/DigitalSignContainer.java)
    - [测试用例](../../src/test/java/org/ofdrw/sign/signContainer/DigitalSignContainerTest.java)
- [GM/T 0031-2014 电子签章结构实现 SESV1Container](../../src/main/java/org/ofdrw/sign/signContainer/SESV1Container.java)
    - [测试用例](../../src/test/java/org/ofdrw/sign/signContainer/SESV1ContainerTest.java)
- [GB/T 38540-2020 电子签章结构实现 SESV4Container](../../src/main/java/org/ofdrw/sign/signContainer/SESV4Container.java)
    - [测试用例](../../src/test/java/org/ofdrw/sign/signContainer/SESV4ContainerTest.java)


## 实现自定义的扩展数字签名容器

请参考 [扩展数字签名容器接口](../../src/main/java/org/ofdrw/sign/ExtendSignatureContainer.java) 实现电子签章功能。

假如你有一台符合国家标准规范具有型号证书的签章服务器，那么你可以这样实现：

```java
public class MySESV4ontainer implements ExtendSignatureContainer {
    @Override
    public MessageDigest getDigestFnc() {
        return new SM3.Digest();
    }

    @Override
    public ASN1ObjectIdentifier getSignAlgOID() {
        return GMObjectIdentifiers.sm2sign_with_sm3;
    }

    @Override
    public byte[] sign(InputStream inData, String propertyInfo) throws IOException, GeneralSecurityException {
        MessageDigest md = getDigestFnc();
        // 签名原文杂凑值，也就是Signature.xml 文件的杂凑值
        byte[] dataHash = md.digest(IOUtils.toByteArray(inData));
        // TODO 调用电子签章服务器提供的电子签章接口完成电子签章。
    }
    @Override
    public byte[] getSeal() throws IOException {
        // TODO 从电子签章服务器上获取电子印章
    }
    @Override
    public SigType getSignType() {
        return SigType.Seal;
    }
}
```

## OFD 签名签章验证


**免责声明：为了合法验证OFD的电子签章有效性，请根据电子签章版本按照下面规范章节实现验证流程：**

- 《GM/T 0031-2014》 6.2.3 电子签章验证流程
- 《GB/T 38540-2020》 7.3 电子签章验证流程

**证书的验证也请由独立合法第三方机构进行验证！**

OFD的签名签章验证过程有一下几步：

1. 使用OFD解析器构造一个验证引擎。
2. **实现验证容器**。
3. 设置验证容器。
4. 执行验签验章。
5. 如果没有异常抛出说明验证成功。

```java
// 1. 构造一个验证引擎。
try (OFDReader reader = new OFDReader(src);
     OFDValidator validator = new OFDValidator(reader)) {
    // 2. 实现验证容器。
    SignedDataValidateContainer dsc = new YouImpContainer(cert);
    // 3. 实现验证容器。
    validator.setValidator(dsc);
    // 4. 执行验签验章。
    validator.exeValidate();
    // 5. 如果没有异常抛出说明验证成功。
}
```

> 如果想返还更多验证信息，请在`SignedDataValidateContainer`中对电子签章数据进行解析。

OFD R&W Sign模块中提供几个用于参考的验证容器实现，这些容器均只是验证签名值是否正确：

- [数字电子签名验证实现 DigitalValidateContainer](../../src/main/java/org/ofdrw/sign/verify/container/DigitalValidateContainer.java)
    - [测试用例](../../src/test/java/org/ofdrw/sign/verify/container/DigitalValidateContainerTest.java)
- [GM/T 0031-2014 电子签章验证实现 SESV1Container](../../src/main/java/org/ofdrw/sign/verify/container/SESV1ValidateContainer.java)
    - [测试用例](../../src/test/java/org/ofdrw/sign/verify/container/SESV1ValidateContainerTest.java)
- [GB/T 38540-2020 电子签章验证实现 SESV4Container](../../src/main/java/org/ofdrw/sign/verify/container/SESV4ValidateContainer.java)
    - [测试用例](../../src/test/java/org/ofdrw/sign/verify/container/SESV4ValidateContainerTest.java)

## 签章定位

- [骑缝章](../../src/test/java/org/ofdrw/sign/stamppos/RidingStampPosTest.java)
- [对开章](../../src/test/java/org/ofdrw/sign/stamppos/RidingStampPosTest.java)
- [关键字定位签章](../../src/test/java/org/ofdrw/sign/keyword/KeywordPosSignTest.java)