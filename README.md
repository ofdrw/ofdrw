# OFD reader & Wirter

根据[《GB/T 33190-2016 电子文件存储与交换格式版式文档》](./GBT_33190-2016_电子文件存储与交换格式版式文档.pdf)标准实现版式文档OFD库。


标准中出现的对象或描述带有`ST`、`CT`分别代表：
 
- `ST` simpleType 表示定义一个简单类型XML元素，对应XSD为：`<xs:simpleType></xs:simpleType>`
- `CT` complexType 表示定义一个复杂类型XML元素，对应XSD为：`<xs:complexType></xs:complexType>`

在实现过程中为避免`Array`容易与`java.lang`重名，统一采用 **类型名称 + ST** 后缀的方式命名。