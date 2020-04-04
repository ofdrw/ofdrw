package org.ofdrw.layout.engine.render;

/**
 * 渲染异常
 *
 * @author 权观宇
 * @since 2020-04-04 23:45:57
 */
public class RenderException extends RuntimeException {
    public RenderException() {
    }

    public RenderException(String message) {
        super(message);
    }

    public RenderException(String message, Throwable cause) {
        super(message, cause);
    }

    public RenderException(Throwable cause) {
        super(cause);
    }
}
