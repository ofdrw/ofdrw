# OFDRW 内容生成 事件处理

## 1. 事件概述

为了帮助开发者对OFD内容生成的不同阶段对文档进行干预，OFDRW提供了系列事件处理接口。

事件分类如下：

- **元素事件**
  - 渲染完成：在OFDRW元素转换为OFD对象后触发。
- **页事件**
  - 渲染前：在每个虚拟页面转换为OFD页内容生成之前触发。
- **文档事件**
  - 渲染完成：在所有页面生成OFD页对象后之后触发。


## 2. 元素事件

### 2.1 渲染完成 onRenderFinish

OFDRW支持当OFDRW元素（如Div或Canvas）在转换为OFD对象时触发事件。

OFDDoc在关闭时将会把所有虚拟页面中的OFDRW元素转换为OFD对象，这个过程中会触发元素渲染完成事件。

您可以在该事件中获取到过程中生成的OFD元素对象ID以及产生资源ID，并支持使用虚拟容器向OFD容器中添加文件等操作。

要是实现元素事件处理，你需要为您的OFDRW提供`OnRenderFinish`事件的回调函数，步骤如下：

1. 创建一个OFD元素，例如`Div`、`Paragraph`、`Canvas`等。
2. 为该元素设置`OnRenderFinish`事件处理器，事件处理器需要实现`org.ofdrw.layout.handler.ElementRenderFinishHandler`接口。
3. 把元素添加到虚拟页面或文档中。
4. 关闭文档。

```java
class Main {
    public static void main(String[] args) {
        // path ...
        try (OFDDoc ofdDoc = new OFDDoc(path)) {
            // 1. 创建一个OFD元素
            Paragraph p = new Paragraph("你好呀，OFD Reader&Writer！", 8d);
            // 2. 为该元素设置OnRenderFinish事件处理器
            p.setOnRenderFinish((loc, contentObjId, resObjIds) -> {
                // 保存
            });
            // 3. 把元素添加到虚拟页面或文档中
            ofdDoc.add(p);
            // 4. 关闭文档 try-with-resources 会自动调用close方法
        }
    }
}
```

完整示例代码见：[ElementRenderFinishHandlerTest.java #handle](../../src/test/java/org/ofdrw/layout/handler/ElementRenderFinishHandlerTest.java)


## 3. 页事件

### 3.1 渲染前 onPage

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
完整示例代码见：[VPageHandlerTest.java #handle](../../src/test/java/org/ofdrw/layout/handler/VPageHandlerTest.java)

## 4. 文档事件

### 4.1 渲染完成 onRenderFinish

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
完整示例代码见：[RenderFinishHandlerTest.java #handle](../../src/test/java/org/ofdrw/layout/handler/RenderFinishHandlerTest.java)

