package org.ofdrw.converter.pdf.model;

/**
 * 复用页面资源表单指令
 * <p>
 * 当一个图元（例如模板页、复合对象、印章）已经被转换并注册为可复用的资源时，页面内通过本指令引用它，
 * 对应 PDF 中的 {@code /XObject}（Form）与 {@code Do}。
 * <p>
 * 设计 3.2 节：缩放只由“图元所处容器是否与页面框不同”这一显式上下文决定；
 * 因此放置矩阵由渲染器显式计算后写入本指令，写入器不再嗅探任何父节点类型。
 *
 * @author 权观宇
 * @since 2.5.1
 */
public final class FormCommand extends AbstractCommand {

    /**
     * 表单资源标识
     */
    private final String formRef;

    /**
     * 放置矩阵（PDF 页面坐标中引用表单坐标系的变换矩阵）
     */
    private final AffineMatrix placementMatrix;

    /**
     * 构造表单引用指令
     *
     * @param formRef         表单资源标识，不能为空
     * @param placementMatrix 放置矩阵，为空时按单位矩阵
     */
    public FormCommand(String formRef, AffineMatrix placementMatrix) {
        super(CoordSpace.PAGE);
        if (formRef == null || formRef.isEmpty()) {
            throw new IllegalArgumentException("表单资源标识不能为空");
        }
        this.formRef = formRef;
        this.placementMatrix = placementMatrix == null ? AffineMatrix.IDENTITY : placementMatrix;
    }

    /**
     * @return 表单资源标识
     */
    public String getFormRef() {
        return formRef;
    }

    /**
     * @return 放置矩阵
     */
    public AffineMatrix getPlacementMatrix() {
        return placementMatrix;
    }

    @Override
    public String toString() {
        return "Form(" + formRef + ", " + placementMatrix + ")";
    }
}
