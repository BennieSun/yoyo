package yoyo.common.exception;

public class SingleCaseException extends Exception {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public SingleCaseException() {
		super("����Ϊ ����ģʽ,�������ظ�����,��ʹ�� getInstance()��ȡ");
	}
	
}
