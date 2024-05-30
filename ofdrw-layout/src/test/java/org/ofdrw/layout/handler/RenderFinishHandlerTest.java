package org.ofdrw.layout.handler;

import org.dom4j.DocumentException;
import org.junit.jupiter.api.Test;
import org.ofdrw.core.action.Actions;
import org.ofdrw.core.action.CT_Action;
import org.ofdrw.core.action.EventType;
import org.ofdrw.core.action.actionType.URI;
import org.ofdrw.core.basicStructure.doc.Document;
import org.ofdrw.layout.OFDDoc;
import org.ofdrw.layout.element.Paragraph;
import org.ofdrw.pkg.container.DocDir;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

class RenderFinishHandlerTest {
    /**
     * 在生成文档的过程中获取文档信息
     */
    @Test
    void handle() throws IOException{
        Path path = Paths.get("target/AddInfoAfterRender.ofd").toAbsolutePath();
        try (OFDDoc ofdDoc = new OFDDoc(path)) {
            Paragraph p = new Paragraph("你好呀，OFD Reader&Writer！", 8d);
            ofdDoc.add(p);
            // 通过回调函数向文档加入一个点击动作
            ofdDoc.onRenderFinish(((maxUnitID, ofdDir, index) -> {
                try {
                    final DocDir docDir = ofdDir.getDocByIndex(index);
                    final Document document = docDir.getDocument();
                    Actions actions = new Actions();
                    CT_Action myAction = new CT_Action(EventType.DO, new URI("https://gitee.com/ofdrw/ofdrw"));
                    myAction.setObjID(maxUnitID.incrementAndGet());
                    actions.addAction(myAction);
                    document.setActions(actions);
                } catch (IOException | DocumentException e) {
                    e.printStackTrace();
                }
            }));
        }
        System.out.println("生成文档位置: " + path.toAbsolutePath());
    }
}