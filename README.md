# OFD reader & Wirter

根据[《GB/T 33190-2016 电子文件存储与交换格式版式文档》](./GBT_33190-2016_电子文件存储与交换格式版式文档.pdf)标准实现版式文档OFD库（含有书签）。


标准中出现的对象或描述带有`ST`、`CT`分别代表：
 
- `ST` simpleType 表示定义一个简单类型XML元素，对应XSD为：`<xs:simpleType></xs:simpleType>`
- `CT` complexType 表示定义一个复杂类型XML元素，对应XSD为：`<xs:complexType></xs:complexType>`

项目结构

- [**ofdrw-core**](./ofdrw-core/) 核心API，参考[《GB/T 33190-2016 电子文件存储与交换格式版式文档》](./GBT_33190-2016_电子文件存储与交换格式版式文档.pdf)实现的基础
    - 实施状态： **阶段性完成**。


欢迎大家一同参与项目。