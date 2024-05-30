package org.ofdrw.layout.handler;

import org.junit.jupiter.api.Test;

import org.ofdrw.layout.OFDDoc;
import org.ofdrw.layout.element.Paragraph;
import org.ofdrw.pkg.container.OFDDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;


import static org.junit.jupiter.api.Assertions.*;

class ElementRenderFinishHandlerTest {

    /**
     * OFDRW元素渲染结束时触发的回调函数
     */
    @Test
    void handle() throws IOException{
        Path path = Paths.get("target/ofd-element-ids.ofd").toAbsolutePath();
        try (OFDDoc ofdDoc = new OFDDoc(path)) {
            Paragraph p = new Paragraph("你好呀，OFD Reader&Writer！", 8d);
            OFDDir ofdDir = ofdDoc.getOfdDir();
            p.onRenderFinish((loc, contentObjId, resObjIds) -> {
                System.out.println("OFD元素所处页面在OFD容器内的绝对路径: " + loc.toString());
                System.out.println("内容产生的OFD对象ID: " + contentObjId.toString());
                System.out.println("元素依赖资源对象ID: " + resObjIds.toString());

                // 生成一个自定义的XML文件，记录生成的文件ID
                String xml = String.format("<doc><dom>%s</dom></doc>", contentObjId.get(0));
                try {
                    ofdDir.putFile("custom.xml", xml.getBytes(StandardCharsets.UTF_8));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
            ofdDoc.add(p);
        }
        System.out.println("生成文档位置: " + path.toAbsolutePath());
    }
}