package org.ofdrw.sign.verify.exceptions;

import org.ofdrw.core.basicType.ST_Loc;

/**
 * 文件完整性验证异常
 *
 * @author 权观宇
 * @since 2020-04-22 01:14:10
 */
public class FileIntegrityException extends OFDVerifyException {

    /**
     * 被检测文件在OFD容器中的绝对路径
     */
    private ST_Loc fileAbsPath;

    /**
     * 预期的文件杂凑值
     */
    private byte[] exceptDataHash;

    /**
     * 实际的文件杂凑值
     */
    private byte[] actualDataHash;

    public FileIntegrityException(ST_Loc fileAbsPath, byte[] exceptDataHash, byte[] actualDataHash) {
        super(fileAbsPath.getLoc() + " 文件被篡改");
        this.fileAbsPath = fileAbsPath;
        this.exceptDataHash = exceptDataHash;
        this.actualDataHash = actualDataHash;
    }

    public FileIntegrityException() {
    }

    public FileIntegrityException(String msg) {
        super(msg);
    }

    public FileIntegrityException(String message, Throwable cause) {
        super(message, cause);
    }

    public ST_Loc getFileAbsPath() {
        return fileAbsPath;
    }

    public byte[] getExceptDataHash() {
        return exceptDataHash;
    }

    public byte[] getActualDataHash() {
        return actualDataHash;
    }
}
