# 发布记录和特性

## Version 1.20.1 2022-10-10 19:17:17

> Alias: Ougonkyou

新增：

- 文档内容替换支持CGTransform类型文字内容替换。


## Version 1.20.0 2022-9-27 23:04:31

> Alias: OVERLORD IV

新增：

- `DocContentReplace` 实现文档内容替换。
- `Paragraph`段落支持对已经添加到段落中的文字统一设置字体。
- `ContentExtractor`支持了OFD TextObject的提取，用于支持文本替换。

修复：

- `Span` 在内容不足1时导致的OOR。



## Version 1.19.1 2022-8-22 20:54:52

> Alias: KuroNoShoukanshi

新增：

- `OFDReader`新增获取所有附件的方法`getAttachmentList()`
- OFD转换PDF时，附件也将复制到PDF中。

## Version 1.19.0 2022-8-16 21:50:33

> Alias: OVERLORD Ⅳ

修复：

- OFD转换PDF转换后的线条与实际不符的问题。

新增：

- `Div`元素支持设置图层，使用`Div#setLayer`方法。
  - 测试用例见 [OFDDocTest.java](ofdrw-layout/src/test/java/org/ofdrw/layout/OFDDocTest.java) 的 `vPageLayerTest` 用例。
- `VirtualPage` 虚拟页面支持添加模板，使用`VirtualPage#addTemplate`方法。
  - 测试用例见 [DocEditDemos.java](ofdrw-layout/src/test/java/org/ofdrw/layout/DocEditDemos.java) 的 `vPageUseTemplateTest` 用例。

## Version 1.18.2 2022-7-20 19:58:24

> Alias: Summer Time Rendering

修复：

- 增加了命名空间兼容模式的设置开关`OFDReader#setNamespaceStrictMode` 或 `OFDElement.NSStrictMode`设置。
  - `true`：使用严格模式，只解析带有`<ofd:>`命名空间的元素。
  - `false`：默认值，兼容模式只要元素名称相同就当做OFD元素解析。

新增：

- OFDReader简化了用于获取页面尺寸的方法，`ST_Box getPageSize(int num)`。


## Version 1.18.1 2022-7-13 22:01:44

> Alias: Tomodachi Game

修复：

- 兼容了非标准命名空间的元素的获取。

## Version 1.18.0 2022-6-24 23:23:08

> Alias: Hanlu

新增：

- 增加用兼容CMS格式的GBT35275签名容器，`GBT35275PKCS9DSContainer`，数字签名可通过数科验证。

修复：

- 出于兼容性考虑，签名ID默认构造采用不含前缀0的数字表示，开发者可以通过实现`SignIDProvider`自定义签名ID。
- 兼容了CMS格式的GBT35275签名。

## Version 1.17.19 2022-6-20 18:40:10

> Alias: Qiufen

修复：

- 兼容了错误Tag的绘制参数。
- 兼容了ImageObject对象没有ResourceID导致NPE。

## Version 1.17.18 2022-5-20 21:55:20

> Alias: Bailu

修复：

- 资源加载加载资源路径错误导致转换失败问题。


## Version 1.17.17 2022-5-16 23:09:40

> Alias: Chushu

修复：

- 重复添加水印异常问题。

## Version 1.17.16 2022-5-10 20:37:59

> Alias: Liqiu

修复：

- 文件占用 reader无法关闭。
- 修复XML4j XXE漏洞。
- 编辑时虚拟页面Content NPE问题。
- 移除log4j配置文件。

## Version 1.17.15 2022-4-24 20:26:52

> Alias: Dashu

修复：

- 解析文字空页面NPE问题

## Version 1.17.14 2022-4-7 20:48:40

> Alias: XiaoShu

修复：

- 文档合并异常。 

## Version 1.17.13 2022-3-21 22:37:18

> Alias: XiaZhi

- GBT35275 格式签名和验签问题修复：
  - 修复了GB35275格式签名生成和解析的结构解析错误。
  - 兼容了文件Hash的BASE64格式编码作为签名原文，增加了`GBT35275DSContainer#setEnableFileHashBase64`开关来切换是否对文件Hash进行Base64编码。

## Version 1.17.12 2022-3-14 22:14:35

> Alias: MangZhong

- 修复图元参数错误的默认值标注


## Version 1.17.11 2022-3-4 22:01:54

> Alias: XiaoMan

修复：

- 转换时颜色参数复制导致异常。
- 修复javax Holder兼容问题。
- 关键自定位，修复sun包在高版本JDK NOT FOUND问题
- 关键自定位，添加了 [高亮测试用例](./ofdrw-layout/src/test/java/org/ofdrw/layout/highlight/TestHighlight.java)，用于观察定位参数。
- 修复了竖排字体关键自定位区域错误的问题。

## Version 1.17.10 2022-3-1 20:40:00

> Alias: LiXia

修复：

- 解决了部分OFD无法解压问题。
- 修复了图片转换字符重叠问题。
- 升级zip4j。
- 调整了系统字体加载日志等级。


## Version 1.17.9 2022-2-17 19:43:27

> Alias: Guyu

修复：

- 修复了资源加载exist 路径拼接错误问题。
- 隐藏了OFD接口非法时，加载不到文件的错误消息。

新增：

- 增加了可配置文件解压大小限制。

## Version 1.17.8 2022-1-13 20:51:55

> Alias: Qingming

修复：

- 修复某些精心构造的ZIP文件导致的解压文件特别巨大。

## Version 1.17.7 2022-1-10 21:21:35

> Alias: Chunfen

修复：

- 合并文件资源复制异常造成的乱码。
- 修复通用转换接口内部没有关闭流造成的，文件占用问题。
- 修复了转换模块字体加载后缀名大小写兼容问题。

## Version 1.17.6 2021-12-22 22:04:25

> Alias：Jingzhe

修复：

- 添加水印时导致页面内容消失问题。

## Version 1.17.5 2021-12-19 18:17:26

> Alias: Yushui

修复：

- 修复了添加水印时，由于程序解析Annots目录错误导致的EPE问题。
- 修复了GBT35275验证容器逻辑判断错误问题。

## Version 1.17.4 2021-12-15 19:13:53

> Alias：Lichun

修复：

- v4电子印章解析遗漏的return语句导致的NPE

## Version 1.17.3 2021-12-14 18:38:24

> Alias：Dahan

修复：

- CVE-2021-44228 log4j漏洞

## Version 1.17.2 2021-12-4 18:05:38

> Alias: Xiaohan

兼容：

- 兼容标的电子印章数据字段


## Version 1.17.1 2021-11-24 21:18:40

> Alias: Dongzhi

修复：

- 电子印章解析兼容而外长度出现。
- 合并`ofdrw-tool`到`ofdrw-full`中便于引入。 

## Version 1.17.0 2021-11-14 22:53:35

> Alias: Daxue

新增：

- 增加文档操作工具模块[ofdrw-tool](./ofdrw-tool)
  - 文档合并功能，见 [测试用用例](./ofdrw-tool/src/test/java/org/ofdrw/tool/merge/OFDMergerTest.java)

修复：

- 外部字体复制BUG。

## Version 1.16.0 2021-10-15 20:42:17

> Alias: Xiaoxue

- 图片转换字体绘制部分逻辑，解决大部分图片转换乱码问题。
  - 文字绘制逻辑优化。
  - 兼容了绘制文字中含有非法换行符的情况。
- 重构了字体加载模块
  - 正确理解了字形和字族的关系。
  - 解决了Windows环境下用户安装字体的加载。
  - 解决了CFF特殊压缩裁剪字体的解析。
  - 解决了OTF、TTF裁剪字体的解析。
  - 重构了字体映射和加载相关API。
- 签名器增加了扩展额外属性的支持。

## Version 1.15.6 2021-10-8 20:09:35

> Alias: Lidong

修复：

- 修复了没有印章验章没有检查印章与电子签章数据中印章的匹配性问题。
- 修复了core模块`CT_CommonData` 对多`PublicRes`和`DocumentRes`的支持。
- 修复了`PublicRes.xml`和`DocumentRes.xml`可能存在多个导致的渲染乱码。
- 对`Paragraph`的预处理`doPrepare`方法标注了额外警告说明文字。
- 关闭了字体加载对`type14`类型字体的警告日志。

新增：

- 字体加载器 `FontLoader`增加了语义化的预加载方法`Preload`。 

## Version 1.15.5 2021-10-3 18:14:23

> Alias: shuangjiang

修复：

- 裁剪了PDFBox的字体模块，解决了字体无法解析的大部分问题，字解析工具见 [./ofdrw-converter/src/main/java/org/ofdrw/converter/font/README.md]

## Version 1.15.4 2021-9-23 21:26:11

> Alias: hanlu

修复：

- PDF转换对白色背景的电子印章图片进行特殊处理，扣除白色背景。

## Version 1.15.3 2021-9-22 20:33:27

> Alias: Qiufen

修复：

- 兼容了不规范OFD命名空间导致IAE问题。

## Version 1.15.2 2021-9-15 20:46:07

> Alias: Qiu2

修复：

- 对不规范OFD字体资源格式的解析NPE问题，采用默认字体替换
- Span LineBreak无效问题

新增：

- 创建`OFDDoc`对象增加
  - `#getOfdDir` 获取OFD虚拟容器。
  - `#getOfdDocument` 获取文档根节点。
  - `#onRenderFinish` 用于设置渲染完成的回调函数
  - 各个方法见使用见 [测试用例](ofdrw-layout/src/test/java/org/ofdrw/layout/OFDDocTest.java) 
    - `#onRenderFinished`
    - `#genDocAndGetDocInfo`

## Version 1.15.1 2021-9-1 20:44:57

> Alias: Move On

修复：

- 转换OFD时，工作空间无法删除的BUG：字体加载器没有释放加载的字体文件导致。

## Version 1.15.0 2021-8-25 19:54:45

> Alias: Shokugeki no Soma

新增：

- OFD完整性协议
  - 完整性保护 [见测试用例](ofdrw-crypto/src/test/java/org/ofdrw/crypto/integrity/OFDIntegrityTest.java)
  - 完整性验证 [见测试用例](ofdrw-crypto/src/test/java/org/ofdrw/crypto/integrity/OFDIntegrityVerifierTest.java)


## Version 1.14.0 2021-8-16 19:29:22

> Alias: SSSS.Gridman

新增：

- 通过矩形区域提取文本的接口 [见测试用例](ofdrw-reader/src/test/java/org/ofdrw/reader/ContentExtractorTest.java)
- 新增《GM/T 0099》部分实现，[见测试用例](ofdrw-crypto/src/test/java/org/ofdrw/crypto/OFDEncryptorTest.java)
  - 口令加密OFD文件
  - 证书加密OFD文件
- 新增了基于 《GB/T 35275》实现的数字签名和验证的容器
  - [签名 测试用例](ofdrw-sign/src/test/java/org/ofdrw/sign/signContainer/GBT35275DSContainerTest.java)
  - [验签 测试用例](ofdrw-sign/src/test/java/org/ofdrw/sign/verify/container/GBT35275ValidateContainerTest.java)
- gm模块实现了 《GB/T 35276》 相关的数据结构和构造方法。

## Version 1.13.5 2021-7-27 20:21:41

> Alias: Remake Our Life

新增：

- 兼容了而非标准的历史命名空间，文件在被修改后自动升级命名空间。
  - 修复了由于命名空间问题导致的签名无法被识别的问题。
  
## Version 1.13.4 2021-7-22 20:17:35

> Alias: EDENS ZERO

修复：

- 编辑文档时，设置 背景、边框无效的问题：路径数据clone时丢失信息
- 向虚拟页面页面追加内容时NPE的问题：add时主动进行预处理取得元素高度。
- ST_Box、ST_Array 写入数字时，对于小数保留两位小数（四舍五入）减少无意义的精度。


## Version 1.13.3 2021-7-14 18:37:47

> Alias: Cat's Eye

修复：

- 修复不规范的OFD路径操作符合操作数，造成无法转换PDF、图片的问题。

## Version 1.13.2 2021-7-6 19:55:36

> Alias: So I'm a Spider, So What?

修复：

- 修复了jar包内嵌字体无法加载导致NPE


## Version 1.13.1 2021-7-5 19:37:24

> Alias: ODD TAXI

新增:

- 段落布局增加了行内的文字浮动方向配置
  - [测试用例](./ofdrw-layout/src/test/java/org/ofdrw/layout/ParagraphLayoutDemo.java)

## Version 1.13.0 2021-6-30 18:49:52

> Alias: Back arrow

修复：

- 资源加载器对路径是否存在的BUG，导致的转换PDF无印章情况。
- 修复从OFDDoc获取Layout给页面赋值并配置时候配置无效的问题。

新增：

- 《GB/T 32918》 中描述的密钥派生函数 KDF
- 《GM/T 0099-2020》:
  - 标准中提及的所有数据结构：加解密、签名、防止夹带。
  - 增加了 OFD 2.0 中规定的各类目录结构，如：模板目录（Temps）、注释目录（Annots）。
- 调整的注释生成的存放位置为专用的`Annots`目录。

## Version 1.12.1 2021-6-16 19:03:54

> Alias: Your Lie in April

修复：

- 文件转换过程中由于印章解析异常导致NPE。
- 升级PDFBox防止可能发生OOM攻击BUG。


## Version 1.12.0 2021-6-11 20:06:55

> Alias: Sword Art Online

修复：

- 转换PDF对路径对象解析越界问题。

新增：

- 流式布局的页面插入功能
    - 见测试用例 [DocEditDemos#streamInsertTest](./ofdrw-layout/src/test/java/org/ofdrw/layout/DocEditDemos.java)

## Version 1.11.2 2021-6-10 19:11:42

> Alias: Ghost in the Shell

修复：

- 对签名列表文件、注释列表文件、附件列表文件的存在性检查，修复 出现路径存在文件不存在BUG。
- 修复 转换PDF时描边颜色转换错误的问题。

## Version 1.11.1 2021-6-9 19:05:23

> Alias: A Certain Scientific Railgun

修复：

- 多个相同关键字只能检索到1个的问题。
- 修复了测试 OFD转图片、SVG、HTML中示例OFDReader没有关闭造成的临时文件没有删除的问题。

## Version 1.11.0 2021-6-8 21:46:30

>  Alias: Hori-san to Miyamura-kun

修复：

- 颜色空间导致的NPE。
- 修复在字体无法加载时使用系统字体进行模糊替换的BUG。

新增：

- [OFD转换HTML](./ofdrw-converter/README.md)


## Version 1.10.0 2021-6-2 23:20:49

> Alias: MissKobayashiDragonMaid

修复：

- 修复了段落内容分页造成的没有重设高度造成的死循环。
- 修复Clear和 Float共同作用式发生得到布局错误。
- 内部布局调用容器直接添加到虚拟页面，而不是add接口，跳过参数检测带来的意料之外的错误提示打印。

新增：

- 修改了Div内大量参数的返还值，现在通过子类也可以对父类的链式调用。
- Div中加入了快捷的配置方法减少调用者配置负担。
- 支持了虚拟页面的随意位置插入，[见测试用用例 DocEditDemos#pageInsertTest](./ofdrw-layout/src/test/java/org/ofdrw/layout/DocEditDemos.java)
- 段落内增加了新的带有字体大小的构造器，引导用户创建字体时进行配置。

## Version 1.9.8 2021-5-31 19:08:43

> Alias: MEGALOBOX

修复：

- `FontLoader`：
  - 对于系统字体不再使用嵌入式字体的加载方式。 
  - 增加了开关用于控制强制启用加载字体为嵌入式字体。`FontLoader#enableForceEmbedded`

## Version 1.9.7 2021-5-28 22:16:44

> Alias: Re:Zero

修复：

- PDF转换TTC格式字体加载失败问题。
- 修复PDF转换字体无法加载时使用相近字体替代。

## Version 1.9.6 2021-5-26 22:05:33

> Alias: TheJourneyofElaina

修复：

- 修复流式布局下，段落布局问题：
  - 居中不正确
  - 设置宽度无效
  - 设置高度无效
  - 见 [段落布局示例](./ofdrw-layout/src/test/java/org/ofdrw/layout/cases/content/ParagraphCase.java)

## Version 1.9.5 2021-5-24 20:52:26

> Alias: ToYourEternity

新增：

- 数字签名清理工具，见[示例](./ofdrw-sign/src/test/java/org/ofdrw/sign/SignCleanerTest.java)
- PKCS工具增加了流式输入参数解析私钥。

## Version 1.9.4 2021-5-19 21:03:33

> Alias: Kaguya-samaLoveIsWar

修复：

- 图片转换骑缝章图章绘制错误问题。

## Version 1.9.3 2021-5-18 20:16:13

> Alias: EromangaSensei

修复：

- 向OFD文件添加水印文件时，发生的强制类型转换错误的问题。

## Version 1.9.2 2021-5-12 21:20:05

> Alias: CrayonShinChan

修复：

- Core模块中对 CT_Permissions实例错误定义，实例应该是Permissions。


## Version 1.9.1 2021-5-11 22:01:07

> Alias: TIGER×DRAGON!

新增：

- 图片转换接口新增 `double`类型的PPM设置


## Version 1.9.0 2021-5-7 22:18:27

> Alias: Natsume'sBookOfFriends

新增：

- OFD转换SVG，相较于转换图片，速度更快，可以无失真的缩放。
  - [快速入门 ofdrw-converter/README.md](./ofdrw-converter/README.md)
  - [测试用例 OFD2SVGTest.java](./ofdrw-converter/src/test/java/OFD2SVGTest.java)

修复：

- PDF转换文字重叠一起，TextCode DeltaX 没有提供，缺少的DeltaX使用重复最后出现的DeltaX。
- 修复由于iText包provider引入造成的NCE问题。

## Version 1.8.9 2021-4-26 18:39:53

> Alias: OnePunchMan

修复

- Reader解析页面大小造成的NPE

## Version 1.8.8 2021-4-25 21:15:46

> Alias: DailyLivesOfHighSchoolBoys

修复：

- 修复了获取页面存储，没有考虑模板页面的问题。
- 修复了绘制参数存在继承情况的clone异常的问题。

新增:

- 替换了PDF转换模块的实现的为iText，部分内嵌字体问题缺失表的情况仍然没有解决。

## Version 1.8.7 2021-4-24 20:32:36

> Alias: ScissorSeven

修复：

- 图片转换绘制参数没有正确设置的问题。

新增：

- 图片转换支持 符合对象。
- 支持对Path对象紧缩表示的解析。

## Version 1.8.6 2021-4-21 20:58:47

> Alias: GINTAMA

修复:

- 解析印章过程由于路径错误造成无法读取印章的问题。
- 优化了Canvas字体配置构造的复杂性问题。
- Layout布局在绝对定位方式下需要手动指定坐标的友好提示。

## Version 1.8.5 2021-4-17 21:46:59

> Alias:  Pokemon

修复:

- 内嵌字体加载异常问题
- 修复了字体加载的Bug

新增:

- 优化Reader的代码结构
- 迁移`DLOFDReader` 主要功能到`OFDReader`
- `OFDReader`:
  - 增签章获取PageInfo的能力，简化转换模块解析工作。
  - 增加获取注解信息的方法。`org.ofdrw.reader.OFDReader.getStampAnnots`
  - 增加了获取电子签章信息方法`org.ofdrw.reader.OFDReader.getAnnotationEntities`
  - 增加了获取页面集合方法`org.ofdrw.reader.OFDReader.getPageList`

## Version 1.8.4 2021-4-15 23:27:22

> Alias:  PlasticMemories

新增:

- 字体加载器简化字体加载`FontLoader`

修复:

- 修复了转换过程内嵌字体无法加载的大部分问题。
- 优化了PDF的代码。
- 修复了PDF转换过程中无法获取部分资源造成页面内容缺失。
- 兼容了坐标字符可能含有非数字成分问题。

## Version 1.8.3 2021-4-10 14:30:55

> Alias:  ZOMBIELANDSAGA

新增:

- Reader模块下新增资源加载器`ResourceManage`用于简化资源的获取:
  - 资源涵盖 公共资源序列（`PublicRes.xml`） 和 文档资源序列（`DocumentRes`.xml）中所有资源
  - 通过资源ID就可以获取到资源对象。
  - 资源对象中如果存在文件路径，将全部替换为绝对路径。
  - 获取到的所有资源对象均为只读副本，不允许修改。

修复:

- OFD转图片：
    - 电子印章位置不正确的问题。
    - 修复了空白页面没有`ofd:Content`导致的NPE问题。
- 修复了资源加载器无法获取到`PublicRes.xml`下的图片资源问题。

## Version 1.8.2 2021-3-30 18:49:24

> Alias: JujutsuKaisen

修复:

- Img元素无法解析CMYK图片造成的IIOException。
- `Img(java.nio.file.Path)` 构造器不再推荐使用，添加图片应该手动指定图片大小`Img(double, double, java.nio.file.Path)`

## Version 1.8.1 2021-3-18 19:24:18

> Alias: InitialD

修复:

- 图片转换电子印章缺失。
- 增加了Span的字体填充属性`fill`。
- 调整了项目结构优化包引用。
- 修改了图片转换实例构造的参数名称为`ppm`，像素每毫米


## Version 1.8.0 2021-3-15 20:41:01

> Alias: MyYouthRomanticComedyIsWrongAsIExpected

新增：

- [支持OFD转换图片](./ofdrw-converter/README.md)

修复：

- 无法获取相对路径附件的问题。

## Version 1.7.4 2021-3-8 19:43:42

> Alias: AttackOnTitan

修复：

- OFD转PDF text兼容hScale
- 修复了多环境下`PdfBoxFontHolder`造成的并发异常。

## Version 1.7.3 2021-1-15 22:37:53

> Alias: LogHorizon

新增

- OFD转换PDF方法支持使用已经解压的OFD文档作为输入。
- OFDReader构造方法支持，文件路径以及解压的OFD文档作为输入。

修复

- 向已经存在附件的文件中添加附件时，重覆盖了原Attachment.xml文件的问题。
- 修复了错误的附件大小计算。
- 签章v1容器的签章时间格式改为"yyyy-MM-dd HH:mm:ss" 以适应数科阅读器验证。

## Version 1.7.2 2020-12-8 20:11:15

> Alias: CellsAtWork

新增

- 关键字定位：增加后缀匹配解决跨TextObject定位。

修复

- 调整了full模块的打包方式为jar并且增加了占位class，修复了一些情况下无法打包和镜像参数无法同步的问题。

## Version 1.7.1 2020-12-1 19:16:34

> Alias: HYOUKA

修复

- OFDReader中zip没有关闭，导致文件占用的问题。
- SealOFDReader 向后兼容了Path的构造参数。

## Version 1.7.0 2020-11-20 19:48:21

> Alias: Fate/Zero

新增

- OFD转换模块`ofdrw-converter`发布，支持[OFD转换PDF](ofdrw-converter/src/test/java/HelloWorld.java)
- RW中主要API均增加流参数支持，如：OFDReader、OFDDoc、OFDSigner、ConvertHelper。

修复

- OFDDoc使用Path对象生成文档是的IAE。
- 修改了OFD转换PDF的渐变处理

## Version 1.6.10 2020-11-18 11:05:50

> Alias: MobileSuitGundamSEED

新增

- 增加了通过类型获取CT_Color实例的方法。
- 关键字搜索支持通知检索多个关键字。
- 转换模块支持了PageBlock的嵌套。

修复

- Canvas上下文中增加了默认字体属性的解决了没有设置字体导致的NPE。
- 解决了转换部分错误。

## Version 1.6.9 2020-11-12 19:22:21

> Alias: 	MobuSaikoHyaku

新增

- ofdrw-converter 模块用于转换OFD到PDF [开发中暂不可用]
- 文字提取增加了指定页码的方法参数

修复:

- 错误CT_CGTransform名称
- 提升了文字提取的精度

## Version 1.6.8 2020-11-9 21:49:46

> Alias: FlyMeToTheMoon

新增:

- 支持了[指定宽度的首行缩进配置](ofdrw-layout/src/test/java/org/ofdrw/layout/cases/content/ParagraphCase.java)

修复:

- 首行缩进可能出现的被分割的情况。
- 调整了占位span的实现方式，采用构造矩形的方式替换的原有的字形分析。

## Version 1.6.7 2020-11-6 19:20:12

> Alias: OriginalGod

新增:

- 签章容器增加了时间戳的设置方法
- 增加了新的Reader对象用于转换时的元素解析。

修复:

- Page对象中Template应该可以有0到多个

## Version 1.6.6 2020-10-29 20:35:01

> Alias: KimetsuNoYaiba

新增:

- [对侧对开骑缝章](ofdrw-sign/src/test/java/org/ofdrw/sign/stamppos/RidingStampPosTest.java)。

修复:

- CT_Dest 错误的PageId属性设置。
- Signature.xml中MaxSignId重复设置导致多个MaxSignId出现。
- V4的签名容器增加了签名时间的时区指定。

## Version 1.6.5 2020-10-26 21:55:01

> Alias: GrandBlue

修复:

- Canvas API 叠加矩阵变换错误的问题。

新增

- Canvas文档增加了使用HTML Canvas调试的[示例](ofdrw-layout/doc/canvas/README.md)。

## Version 1.6.4 2020-10-23 23:33:38

> Alias: BlackLagoon

修复:

- 虚拟容器：修复了在修改模式下由于虚拟容器缓存导致，读取的xml文件在没有变更的情况仍然写回文档，但是在序列化时导致和源文档不一致的问题，该问题会导致签名的完整性验证失败。
- 签章：修复了在Reader模式下，默认文档不是Doc_0 导致签章之后结果生成Doc_0的问题。
- 签章：修复了在Signatures.xml被保护是签章没有报错的BUG。
- 签章定位：骑缝章指定分割数量，在页码不足分割数量时重新调整分割数量适应剩余页码。
- 文字提取：优化文本宽度计算，解决字号大小不等于deltaX时，宽度计算错误。

## Version 1.6.3 2020-10-21 20:08:32

> Alias: HimoutoUmaru

新增:

- core模块中增加了jaxen库的引用，用于支持xpath。
- 骑缝章增加了页面的边距和切割份数的支持。
- 优化关键字定位，增加了断字断句文档和CTM处理。

## Version 1.6.2 2020-10-19 19:08:55 

> 为了增加版本趣味性，增加了别名

> Alias: overlord

修复：

- 修改文档时向文档添加已经存在文件导致的不友好报错。
- PageBlock文字提取修复

## Version 1.6.1 2020-10-15 20:51:04

新增：

- [命名空间修改](ofdrw-reader/src/test/java/org/ofdrw/reader/tools/NameSpaceModifierTest.java)和[清理](ofdrw-reader/src/test/java/org/ofdrw/reader/tools/NameSpaceCleanerTest.java)的功能用于迁移部分老本版OFD命名空间问题。

修复:

- 修复了getInstance中错误的QName导致无法获取对应元素的BUG。
- 修复了由于字体大小大于可用最大宽度在分析阶段导致的死循环。

## Version 1.6.0 2020-10-13 20:22:28

新功能：

- [关键定位功能](ofdrw-reader/src/test/java/org/ofdrw/reader/keyword/KeywordExtractorTest.java)
- [骑缝章的自由定位](ofdrw-sign/src/test/java/org/ofdrw/sign/stamppos/RidingStampPosTest.java)
- [不同版本电子签章兼容解析](ofdrw-gm/src/test/java/org/ofdrw/gm/ses/parse/VersionParserTest.java)

修复:

- 修复了 CustomDatas 在解析CustomData 序列时候错误的关键字使用。
- 修复了V4版本电子签章可选参数tag0的错误。
- 兼容非标准推荐的签名ID，如"sN"、"N"类型的解析。
- 支持了坐标偏移中厂商定义的`g`参数偏移量。

## Version 1.5.6 2020-9-21 23:14:58

- 修复了无法解析OFD内含有中文目录的文件错误。
- 新增了OFD页面文字抽取的方法：
    
    文字抽取参考[ContentExtractorTest.java](ofdrw-reader/src/test/java/org/ofdrw/reader/ContentExtractorTest.java)

## Version 1.5.5 2020-9-15 21:43:31

- 修复了ST_Array 数组含有多空格抛出IAE的问题。
- 兼容了旧的OFD命名空间。

## Version 1.5.4 2020-8-26 20:10:15

- 简化了`ofdrw-font`字体库以及相关API，移除了内嵌的几个noto字体，减少库体积。

## Version 1.5.3 2020-8-24 21:02:49

- OFDSigner 可以使用自己构造的签名ID提供器，而不是标准推荐的 "s'NNN'"格式
    
    需要手动实现 `org.ofdrw.sign.SignIDProvider`接口

## Version 1.5.2 2020-6-20 10:04:37

- 修复了注释对象参数无法添加多个的问题。

## Version 1.5.1 2020-6-6 11:06:28

- CVE-2020-10683 dom4j库可能造成XXE 攻击，升级该库至2.1.3。
- 增加了替换附件的功能。

## Version 1.5.0 2020-5-19 20:45:50

- 首页加入了免责声明，以及咨询入口。
- 修复了电子印章为非必选参数，签章容器不设置电子印章就无法进行签章操作的问题。
- 为Canvas API 提供线条的各种参数设置包括
    - 回线条的结束端点样式
    - 两条线相交时，所创建的拐角类型
    - 最大斜接长度，也就是结合点长度截断值
    - 填充线时使用虚线模式，设置虚线间隔

Canvas 测试用例见 [DrawContextTest](./ofdrw-layout/src/test/java/org/ofdrw/layout/element/canvas/DrawContextTest.java)

- `#setLineDash`
- `#setMiterLimit`
- `#setLineJoin`
- `#setLineCap`

签章测试用用例见 [NoSealSignTest](./ofdrw-sign/src/test/java/org/ofdrw/sign/signContainer/NoSealSignTest.java)

## Old

- *2020-05-15* 完成OFD的注释功能

    测试用例参考`org.ofdrw.layout.OFDDocTest#addAnnotation(void)`。
- *2020-05-14* 启动注解对象构想，用于支持水印等注解的加入。
- *2020-05-10* 完成了Canvas系列API的开发。
- *2020-05-01* 增加了Canvas设计，用于更加灵活的绘制和水印效果。
- *2020-04-23* 发布`1.2.0`版本，支持OFD电子签章。
- *2020-04-22* 实现了行内的换行符换行功能。
- *2020-04-20* ofdrw-sign 成功完成一次数字签名。
- *2020-04-19* 增加了ofdrw-gm 模块用于支持电子签章。
- *2020-04-18* 增加了ofdrw-sign模块【未完成】，加入了ofdrw-gv用于共享全局变量。
- *2020-04-15* 启动了ofdrw-sign 模块分析设计。
- *2020-04-12* 发布ofdrw 1.1.0版本。
- *2020-04-11* 【里程碑】实现了向已有OFD增加内容，以及追加内容的功能，考虑发布 1.1.0-RELEASE版本。
- *2020-04-04* 采用虚拟容器的方式重写ofdrw-pkg，为文档的反序列化和修改做准备。
- *2020-04-01* 启动ofdrw-reader的初期设计。
- *2020-03-31* 【里程碑】 OFD R&W 正式发布第一个版本，版本号 1.0.0。
- *2020-03-29* 完成了《OFD R&W 布局设计》的各种情况测试，准备发布版本。
- *2020-03-28* 增加了文字下划线、段落的首行缩进的支持。
- *2020-03-26* 完善了少内容段落的布局，能够使用center居中，增加HelloWorld Demo。
- *2020-03-24* 【里程碑】 基本完成段落渲染器开发，第一次成功生成一份含有文字的OFD文档。
    
    可以运行`org.ofdrw.layout.OFDDocTest#paragraphTest`测试用用例，查看效果。
- *2020-03-23* 确定了图片渲染方式，并生成了含有图片的OFD文档，开始策划开发段落渲染器。

    可以运行`org.ofdrw.layout.OFDDocTest#imgTest`测试用用例，查看效果。
- *2020-03-22* 使用ofdrw-layout 采用固定布局虚拟页面的方式生成了第一个图形OFD文档。

    可以运行`org.ofdrw.layout.OFDDocTest#divBoxTest`测试用用例，查看效果。
- *2020-03-18* 策划并启动 font的开发。
- *2020-02-28* 开始Layout的开发。
- *2020-01-22* 启动 Layout 设计。
- *2020-01-20* ofd r&w 首个OFD文件生成成功，并能够通过 [数科OFD阅读器](http://www.suwell.cn/product/index.html) 正确打开。
    
    可通过运行`org.ofdrw.pkg.dir.OFDDirTest#jar` 在项目target目录下生成一个名为hello.ofd的文件。
- *2019-11-21* 完成基础库的开发工作，开始策划`ofdrw-pkg`。
- *2019-09-27* 项目策划完成并开始实施。

如果该项目有兴趣不妨给个Star，欢迎大家一同参与项目。
