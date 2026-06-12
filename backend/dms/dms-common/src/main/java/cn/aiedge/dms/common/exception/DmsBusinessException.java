package cn.aiedge.dms.common.exception;

import cn.aiedge.common.exception.BusinessException;

/**
 * DMS 业务异常
 */
public class DmsBusinessException extends BusinessException {
    public DmsBusinessException(String message) { super(message); }
    public DmsBusinessException(int code, String message) { super(code, message); }
}
