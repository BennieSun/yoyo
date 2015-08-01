package yoyo.common.exception;

public class ErrorDefine {
	public static final int ERROR_ACCOUNT 				= 1000;	//�ʺŻ��������
	public static final int ERROR_MULTI_DEVICE 			= 1002; //�˻��������豸��¼
	public static final int ERROR_MULTI_TIMEOUT 		= 1003; //��¼��ʱ
	public static final int ERROR_ACCOUNT_DISABLE	 	= 1010; //�˻���ͣ��
	
	public static final int ERROR_SIGNED				= 5101; // ������ǩ��
	
	public static final int ERROR_MODIFY_MULTI			= 5203; //"������Ϣ�޸ĳɹ�������⵽���ڶ������ݼ�¼!";
	public static final int ERROR_MODIFY_NULL			= 5299;	//"������Ϣָ�����!";
	
	public static final int ERROR_SAFE_PASSWORD			= 5301;	// "�������������!";
	public static final int ERROR_SAFE_SAVELESS			= 5302;	// "�������Ǯʧ�ܣ��������С�ڴ�������!";
	public static final int ERROR_SAFE_TAKELESS			= 5303;	// "������ȡǮʧ�ܣ����������С��ȡ������!";
	public static final int ERROR_SAFE_NULL				= 5399;	//"��������Ϣָ�����!";
	
	public static final int ERROR_LESS_MONEY			= 5401;	//"ˮ�ϱҲ���!";
	public static final int ERROR_LESS_GOLD				= 5402;	//"��Ϸ�Ҳ���!";
	public static final int ERROR_PROP_NULL				= 5403;	//"���߲�����!";
	public static final int ERROR_PROP_NULLTARGET		= 5404;	// "���Ͷ��󲻴���!";
	public static final int ERROR_PROP_NULLACTION		= 5405;	//"��Ч����ĵ��߲�������,ֻ����ͨ��[��������]���ܽ�������!";
	public static final int ERROR_PROP_LESS				= 5406;	//"�����ϵ�����������,�޷�����!";
	public static final int ERROR_PROP_NULLTARGETID		= 5407;	//"�����û�ID����,�޷�����!";
	public static final int ERROR_PROP_SHOP_NUM			= 5408;	//"���߹�����������Ϊ0!";
	public static final int ERROR_PROP_VIP				= 5421;	//"��������ʹ�ø��߼���VIP��,��˴�VIP���޷�Я��������,ͬʱ��VIP�������͵ĵ����Ѵ�ŵ����ĵ��߰���,���͵���Ϸ���Ѵ��������ʺ�!";
	
	
	
	public static final int ERROR_APPLYCHAIR_OCCUPANCY	= 8000;	//"����������һ��,��ѡ��������λ";
	public static final int ERROR_APPLYCHAIR_NULLACCOUNT= 8001;	//"�����˺���Ϣ����,�޷�����.";
	public static final int ERROR_APPLYCHAIR_ALREADY	= 8002;	//"���Ѿ�����,�����ظ�����.";
	
	
	public static final int ERROR_UNKNOW				= 9999;	//"ϵͳ����";
	
	//public static final int ERROR_SIGNED				= 10001] = "�ʺų��Ȳ��ܳ���16λ";
	//public static final int ERROR_SIGNED				= 10002] = "�ʺ����벻��Ϊ��";
	//public static final int ERROR_SIGNED				= 10003] = "����δ��¼";
}
