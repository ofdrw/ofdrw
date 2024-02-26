# OFDRW 附件操作

OFDRW提供了对OFD文档中附件的操作支持，包括文档中附件查询与编辑。

- 查询：`OFDReader`实现，包括附件列表查询、附件文件获取。
- 编辑：`OFDDoc`实现，包括附件的添加、替换、删除。

## 附件查询

### 附件列表

OFD文档中的附件列表可以通过 `OFDReader` 对象的 `getAttachments` 方法获取。

```java
class Main {
    public static void main(String[] args) {
        // define path ... 
        try (OFDReader reader = new OFDReader(path)) {
            List<CT_Attachment> attachmentList = reader.getAttachmentList();
            for (CT_Attachment ctAttachment : attachmentList) {
                // 取得附件信息
                String name = ctAttachment.getAttachmentName();
                Path file = reader.getAttachmentFile(name);
                // System.out.println(">> Attachment file name: " + name + " size: " + Files.size(file) + "B");
            }
        }
    }
}
```

完整示例代码见：[AttachmentTest.java #getAttachmentList](../../src/test/java/org/ofdrw/layout/AttachmentTest.java)

### 附件获取

通过 `OFDReader` 对象的 `getAttachmentFile` 方法可以获取指定附件的文件。

- `getAttachment`：方法获取附件详细信息，例如 附件ID、名称、大小、创建/修改事件、格式等。
- `getAttachmentFile`：方法获取附件文件本身。

```java
class Main {
    public static void main(String[] args) {
        // define path ... 
        try (OFDReader reader = new OFDReader(path)) {
            // 获取附件信息
            CT_Attachment attachment = reader.getAttachment("AAABBB");
            // ...
            // 获取附件文件
            Path file = reader.getAttachmentFile("Gao");
            // ...
        }
    }
}
```

完整示例代码见：[AttachmentTest.java #getAttachment](../../src/test/java/org/ofdrw/layout/AttachmentTest.java)

## 附件编辑

### 附件添加/替换

通过 `OFDDoc` 对象的 `addAttachment` 方法可以添加附件，支持两种方式：

添加附件：

- 默认存储位置：默认存储于`/Doc_0/Res/`目录下。
- 指定存储位置：适用于对附件文件存储位置有明确要求的场景。

替换附件：

- 替换附件：替换附件与添加附件采用相同接口，如果附件名称相同，则会替换原有附件。

```java
class Main {
    public static void main(String[] args) {
        // define path ... 
        try (OFDReader reader = new OFDReader(inP);
             OFDDoc ofdDoc = new OFDDoc(reader, outP)) {
            // 默认存储位置加入附件文件
            ofdDoc.addAttachment(new Attachment("youfilename.txt", file));
            // 指定存储位置加入附件文件
            ofdDoc.addAttachment("/Doc_0/MY_DIR/MY_PATH/", new Attachment("Gao", file));
        }
    }
}
```

完整示例代码见：

- 添加附件：[AttachmentTest.java #addAttachmentTest](../../src/test/java/org/ofdrw/layout/AttachmentTest.java)
- 向指定位置添加附件：[AttachmentTest.java #addAttachmentToDirTest](../../src/test/java/org/ofdrw/layout/AttachmentTest.java)
- 替换附件：[AttachmentTest.java #replaceAttachmentTest](../../src/test/java/org/ofdrw/layout/AttachmentTest.java)

### 附件删除

通过 `OFDDoc` 对象的 `deleteAttachment` 方法可以删除指定附件。

```java
class Main {
    public static void main(String[] args) {
        // define path ... 
        try (OFDReader reader = new OFDReader(inP);
             OFDDoc ofdDoc = new OFDDoc(reader, outP)) {
            ofdDoc.deleteAttachment("Gao");
        }
    }
}
```

完整示例代码见：[AttachmentTest.java #deleteAttachment](../../src/test/java/org/ofdrw/layout/AttachmentTest.java)

