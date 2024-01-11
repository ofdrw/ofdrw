# OFDRW 内容生成 事件处理

为了帮助开发者对OFD内容生成的不同阶段对文档进行干预，OFDRW提供了系列事件处理接口。

包括以下事件：

- **页事件(onPage)**：在每个页面内容生成之前触发。
- **渲染完成事件(onRenderFinish)**：在所有页面生成完成之后触发。

## 页事件

在每个页面内容生成之前触发，通过对该事件的处理可以在虚拟页面中的内容生成OFD Content.xml 前对页面进行干预，例如：向页面中添加页头、页脚、水印等。

事件处理方式如下：

1. 实现 `VPageHandler` 接口。
2. 注册 事件处理器。

```java
class Main {
    public static void main(String[] args) {
        // path ...
        try (OFDDoc ofdDoc = new OFDDoc(path)) {
            // 1. 使用匿名类实现 VPageHandler 接口
            // 2. 注册事件处理器
            ofdDoc.onPage((page) -> {
                // 对 page 追加内容
            });
            // ...
        }
    }
}
```

完整示例代码见：[OFDDocTest.java #setOnPage](../../src/test/java/org/ofdrw/layout/OFDDocTest.java)

## 渲染完成事件

在所有页面生成完成之后触发，通过对该事件的处理可以在所有页面生成OFD Content.xml 后对文档进行干预，例如：添加动作点、添加扩展内容等。

事件处理方式如下：

1. 实现 `RenderFinishHandler` 接口。
2. 注册 事件处理器。

```java
class Main {
    public static void main(String[] args) {
        // path ...
        try (OFDDoc ofdDoc = new OFDDoc(path)) {
            // 1. 使用匿名类实现 RenderFinishHandler 接口
            // 2. 注册事件处理器
            ofdDoc.onRenderFinish((maxUnitID, ofdDir, index) -> {
                // 对 doc 追加内容
            });
            // ...
        }
    }
}
```

完整示例代码见：[OFDDocTest.java #onRenderFinished](../../src/test/java/org/ofdrw/layout/OFDDocTest.java)