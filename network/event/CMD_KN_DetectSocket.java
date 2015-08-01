package yoyo.network.event;

import java.nio.ByteBuffer;

public class CMD_KN_DetectSocket {
	public final short size = 16;
	public long lSendTime;				//����ʱ��
	public long lRecvTime;				//����ʱ��
	public CMD_KN_DetectSocket() {
		lSendTime = 0;
		lRecvTime = 0;
	}
	public ByteBuffer ToByteBuffer(){
		ByteBuffer result = ByteBuffer.allocate( size );
		result.putLong(lSendTime);
		result.putLong(lRecvTime);
		return result;
	}
}
