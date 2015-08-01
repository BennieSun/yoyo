package yoyo.common.utils;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class JMD5Encrypt {

	//
	private static MessageDigest messageDigest = null;
	/**
	 * MD5�����ַ���
	 * @param pszSrcData	��������
	 * @return	���ܽ��
	 */
	public static String _EncryptData(String pszSrcData){
		return _EncryptData(pszSrcData,"GB2312");
	}
	/**
	 * MD5�����ַ���
	 * @param pszSrcData	��������
	 * @param charset	���뷽ʽ  ������ʹ��UTF-8��GB2312���ܽ���ǲ�ͬ�ġ�
	 * @return	MD5 ���ܽ��
	 */
	public static String _EncryptData(String pszSrcData,String charset){
		if(pszSrcData==null) return null;
		
		if(messageDigest==null){
			try {
				messageDigest = MessageDigest.getInstance("MD5");
			} catch (NoSuchAlgorithmException e) {
				e.printStackTrace();
			}
		}
		
		messageDigest.reset();
		try {
			messageDigest.update(pszSrcData.getBytes(charset));
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return _bytes2String( messageDigest.digest() );
	}
	/**
	 * �ֽ�ת�ַ���
	 * @param bBytes
	 * @return
	 */
	private static String _bytes2String(byte[] bBytes){
		String szHex;
		StringBuffer md5StrBuff = new StringBuffer();
		for (int i = 0; i < bBytes.length; i++){
			szHex = Integer.toHexString(0xFF & bBytes[i]);
			if (szHex.length() == 2) {
				md5StrBuff.append(szHex);
			} else{
				md5StrBuff.append("0").append(szHex);
			}
		}
		return md5StrBuff.toString();
	}
}
