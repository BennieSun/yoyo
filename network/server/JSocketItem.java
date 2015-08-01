package yoyo.network.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.ConcurrentLinkedQueue;

import yoyo.Globals;
import yoyo.common.logger.JLogger;
import yoyo.common.utils.JUtils;
import yoyo.network.event.KernelCMD;
import yoyo.network.event.TCP_Head;

class JSocketItem implements CompletionHandler<AsynchronousSocketChannel, ISocketItemSink>{
	// ��������
	private int							m_dwClientAddr;				// ���ӵ�ַ
	private int							m_dwClientPort;				// ���Ӷ˿�
	private long						m_lConnectTime;				// ����ʱ��
	
	// ���ı���
	protected short						m_wRoundID;					// ѭ������(״̬�仯һ��,��ֵ����һ��,�Ա�֤�ýṹ����һ��ʹ�ò�ͬ)
	protected AsynchronousSocketChannel	m_ClientSocketChannel;		// Socket�첽����ͨ��
	
	// �ڲ�����
	private short						m_wIndex;					// ��������
	private ISocketItemSink				m_pISocketItemSink;			// �ص��ӿ�
	
	// ״̬����
	private boolean						m_bShutDown;				// �رձ�ʶ
	private boolean						m_bAllowBatch;				// �Ƿ�Ⱥ��
	private short						m_wRecvBufferSize;			// ���ջ������ݴ�С
	private ByteBuffer					m_ReadBuffer;				// ���ݶ�ȡ������
	

	// ��������
	private long						m_lSendTickTime;			// ����ʱ��
	private long						m_lRecvTickTime;			// ����ʱ��
	private int							m_dwSendPacketCount;		// ���ͼ���
	private int							m_dwRecvPacketCount;		// ���ܼ���	

	private SendCompletionHandle		m_SendCompletionHandle;		//������ɶ˿�
	private ReadCompletionhandle		m_ReadCompletionHandle;		//��ȡ��ɶ˿�
	
	/**
	 * 
	 */
	public JSocketItem(short wIndex)
	{
		// ���ûص��ӿ�
		ResetSocketItemData(wIndex);
	}
	public boolean ResetSocketItemData(short wIndex)
	{
		// ��������
		m_wIndex = wIndex;
		// ��ʼ������
		m_wRoundID = JUtils._max((short) 1, m_wRoundID++);
		m_dwClientAddr = 0;
		m_dwClientPort = 0;
		m_lConnectTime = 0;

		m_bShutDown = false;
		m_bAllowBatch = false;

		m_wRecvBufferSize = 0;
		if (m_ReadBuffer == null) {
			m_ReadBuffer = ByteBuffer.allocate(Globals.SOCKET_TCP_BUFFER);
		}
		m_ReadBuffer.clear();
		m_lSendTickTime = 0;
		m_lRecvTickTime = 0;
		m_dwSendPacketCount = 0;
		m_dwRecvPacketCount = 0;
		m_pISocketItemSink = null;
		return true;
	}
	/**
	 * 
	 * @param wRoundID
	 * @return
	 */
	public boolean CloseSocket(int wRoundID)
	{
		if (wRoundID != m_wRoundID) return false;
		//�ȹر�����
		m_ReadCompletionHandle = null;
		if(m_SendCompletionHandle!=null){
			m_SendCompletionHandle.CloseCompletionHandle();
			m_SendCompletionHandle = null;
		}
		if (m_ClientSocketChannel != null)
		{
			synchronized (m_ClientSocketChannel) {
				if (m_ClientSocketChannel.isOpen())
				{
					try
					{
						m_ClientSocketChannel.close();
					} catch (IOException e)
					{
						System.out.println("�ر�Socketͨ������(" + wRoundID + "):");
						e.printStackTrace();
					}
				}
				m_ClientSocketChannel = null;
			}
		}
		// ֪ͨ
		return m_pISocketItemSink.SocketCloseEvent(this);
	}
	/**
	 * 
	 * @param wRoundID
	 * @return
	 */
	public boolean ShutDownSocket(short wRoundID)
	{
		if (wRoundID != m_wRoundID)
			return false;
		if (m_ClientSocketChannel == null)
			return false;
		m_bShutDown = true;
		return true;
	}
	/**
	 * 
	 * @param wRoundID
	 * @param bAllowBatch
	 * @return
	 */
	public boolean AllowBatchSend(short wRoundID, boolean bAllowBatch)
	{
		if (wRoundID != m_wRoundID)
			return false;
		if (m_ClientSocketChannel == null)
			return false;
		m_bAllowBatch = bAllowBatch;
		return true;
	}
	
	public boolean SendData(short wMainCmdID, short wSubCmdID,short wRoundID){
		
		return SendData(wMainCmdID,wSubCmdID,null,(short)0,wRoundID);
	}
	
	public boolean SendData(short wMainCmdID, short wSubCmdID,
			ByteBuffer pData, short wDataSize, short wRoundID){
		if (m_bShutDown == true) return false;
		if (m_wRoundID != wRoundID) return false;
		if (m_dwRecvPacketCount == 0) return false;
		if (IsValidSocket() == false) return false;
		if(wDataSize>0 && pData==null) {
			System.out.println("���ݷ���Դ����(" + wMainCmdID + "|" + wSubCmdID+")");
			return false;
		}
		short wPacketSize = (short) (8 + wDataSize);
		
		// ��ǰͨ�Ű汾��
		byte cbVersion = 0;
		// ���ݼ��ܺ��У����
		byte cbCheckCode = 0;
		// ����ѹ�뷢�ͻ���
		ByteBuffer sendBuffer = ByteBuffer.allocate(wPacketSize);
		// ����ѹ�뷢�ͻ���
		sendBuffer.put( cbVersion );
		sendBuffer.put( cbCheckCode );
		sendBuffer.putShort( wPacketSize );
		sendBuffer.putShort( wMainCmdID );
		sendBuffer.putShort( wSubCmdID );
		if(pData != null){
			pData.position(0);
			pData.limit(pData.capacity());
			sendBuffer.put(pData);
			pData.clear();
			pData = null;
		}
		m_SendCompletionHandle.AddSendData(sendBuffer);
		return true;
	}
	// ----------------------------------------------------------��������
	/**
	 * ��ȡ����
	 */
	public short GetIndex() { return m_wIndex; }
	/**
	 * ��ȡѭ������
	 * @return
	 */
	public short GetRountID() { return m_wRoundID; }

	public int GetSocketID() { return ((m_wIndex & 0xffff) | (m_wRoundID & 0xffff) << 16); }
	/**
	 * ��ȡ�ͻ���IP��ַ
	 * @return
	 */
	public int GetClientAddr() { return m_dwClientAddr; }
	/**
	 * ��ȡ�ͻ������Ӷ˿�
	 * @return
	 */
	public int GetClientPort() { return m_dwClientPort; }
	/**
	 * ����ʱ��
	 * @return
	 */
	public long GetConnectDuration() { return System.currentTimeMillis() - m_lConnectTime; }
	/**
	 * ��ȡ������ʱ��
	 * @return
	 */
	public long GetRecvTickTime() { return m_lRecvTickTime; }
	/**
	 * ��ȡ�����ʱ��
	 * @return
	 */
	public long GetSendTickTime() { return m_lSendTickTime; }
	/**
	 * ��ȡ�������ݰ�����
	 * @return
	 */
	public int GetRecvPackageCount() { return m_dwRecvPacketCount; }
	/**
	 * ��ȡ�������ݰ�����
	 * @return
	 */
	public int GetSendPackCount() { return m_dwSendPacketCount; }
	/**
	 * �Ƿ����Ⱥ��
	 * @return
	 */
	public boolean IsAllowBatch() { return m_bAllowBatch; }
	/**
	 * �жϷ������Ƿ����������������
	 * @return
	 */
	public boolean IsReadySend() { return m_dwRecvPacketCount > 0; }
	/**
	 * ��ȡ�Ƿ���Ч
	 * @return
	 */
	public boolean IsValidSocket()
	{
		if (m_ClientSocketChannel == null)
			return false;
		return m_ClientSocketChannel.isOpen();
	}
	//-------------------------------
	//--CompletionHandler
	//-------------------------------
	@Override
	public void completed(AsynchronousSocketChannel channel, ISocketItemSink sink) {
		// �û�����ʱ��
		m_lConnectTime = System.currentTimeMillis();
		// ��һ�������������ݽ���ʱ��
		m_lRecvTickTime = m_lConnectTime;
		// �����û�
		m_ClientSocketChannel = channel;
		
		m_pISocketItemSink = sink;
		try
		{
			InetSocketAddress inetAddress = (InetSocketAddress) m_ClientSocketChannel.getRemoteAddress();
			m_dwClientPort = inetAddress.getPort();
			m_dwClientAddr = JUtils._AddressToInt(inetAddress.getHostString());
		} catch (IOException e)
		{
			e.printStackTrace();
		}
		m_pISocketItemSink.SocketAcceptEvent(this);
		
		//��ʼ�����ͻ��˷��͵���Ϣ
		m_ReadBuffer.clear();
		m_ReadCompletionHandle = new ReadCompletionhandle();
		m_ClientSocketChannel.read(m_ReadBuffer, this, m_ReadCompletionHandle);
		//��ʼ����������
		m_SendCompletionHandle = new SendCompletionHandle(this);
	}
	@Override
	public void failed(Throwable result, ISocketItemSink sink) {
		
	}
	
	//------------------------
	//--�Զ���
	//------------------------
	
	protected void OnRecvCompleted(int wRecvSize)
	{
		if (wRecvSize <= 0 || wRecvSize >= Globals.SOCKET_TCP_BUFFER)
		{
			CloseSocket(m_wRoundID);
			return;
		}
		// �ж��ж�
		if (m_bShutDown == true) { return; }
		// ��ת������ ��ʼ���ж�ȡ����
		m_ReadBuffer.flip();
		// �ۼƽ������ݻ����С
		m_wRecvBufferSize += wRecvSize;
		// ��¼���һ�ν��տͻ�����Ϣʱ��
		m_lRecvTickTime = System.currentTimeMillis();
		
		while (m_wRecvBufferSize >= TCP_Head.size)
		{
			byte cbVersion 			= m_ReadBuffer.get();
			//���а汾��У��
			if(cbVersion != 0){
				m_wRecvBufferSize = 0;
				JLogger.severe("���ݰ��汾����!");
				break;
			}
			byte cbCheckCode		= m_ReadBuffer.get();
			if(cbCheckCode!=0){
				m_wRecvBufferSize = 0;
				JLogger.severe("���ݰ�У�������!");
				break;
			}
			short wPacketSize		= m_ReadBuffer.getShort();
			if(wPacketSize < TCP_Head.size){
				JLogger.severe("���ݰ�����̫��(" + wPacketSize + ")!");
				CloseSocket(m_wRoundID);
				return;
			}
			if (m_wRecvBufferSize < wPacketSize)
			{
				m_ReadBuffer.position(m_ReadBuffer.position() - TCP_Head.size);
				m_ReadBuffer.limit( m_ReadBuffer.capacity() );
				break;
			}

			short wMainCmdID = m_ReadBuffer.getShort();
			short wSubCmdID = m_ReadBuffer.getShort();
			if (KernelCMD.MDM_KN_COMMAND == wMainCmdID) {
				// �����ں�����
				switch(wSubCmdID)
				{
					case KernelCMD.SUB_KN_DETECT_SOCKET:
					{
						break;
					}
					default:
					{
						throw new IllegalArgumentException("�Ƿ�������");
					}
				}
			} else {
				// �����ݿ������µ��ֽ�������
				short wDataSize = (short) (wPacketSize - TCP_Head.size);
				if (wDataSize > 0) {
					ByteBuffer recvBuffer = ByteBuffer.allocate(wDataSize);
					byte[] recvBytes = new byte[wDataSize];
					m_ReadBuffer = m_ReadBuffer.get(recvBytes, 0, wDataSize);
					recvBuffer.put(recvBytes);
					recvBuffer.flip();
					m_pISocketItemSink.SocketReadEvent(wMainCmdID ,wSubCmdID ,recvBuffer, wDataSize ,this);
				} else {
					m_pISocketItemSink.SocketReadEvent(wMainCmdID ,wSubCmdID ,null ,wDataSize ,this);
				}
			}
			
			// ���ñ���
			m_dwRecvPacketCount++;
			m_wRecvBufferSize -= wPacketSize;
		}
		if (m_wRecvBufferSize > 0) {
			m_ReadBuffer.compact();
		} else {
			m_ReadBuffer.clear();
		}
	}
	
	public boolean OnSendCompleted()
	{
		return true;
	}
	//-----------------------
	//--��ȡ��ɶ˿�
	//-----------------------
	class ReadCompletionhandle implements CompletionHandler<Integer, JSocketItem>{

		@Override
		public void completed(Integer result, JSocketItem attachment) {
			attachment.OnRecvCompleted(result);
			if( attachment.IsValidSocket() ){
				attachment.m_ClientSocketChannel.read(attachment.m_ReadBuffer, attachment, this);
			}
		}

		@Override
		public void failed(Throwable exc, JSocketItem attachment) {
			if( attachment.IsValidSocket() ){
				//JLogger.severe("ReadCompletionhandle:" + exc.getMessage());
				attachment.CloseSocket(attachment.GetRountID());
			}
		}
	}
	//-----------------------
	//--������ɶ˿�
	//-----------------------
	private class SendCompletionHandle implements CompletionHandler<Integer, ByteBuffer>{
		
		private JSocketItem m_pJSocketItem = null;
		private ConcurrentLinkedQueue<ByteBuffer> m_SendBufferQueue;	//�������ݶ���
		private volatile boolean			m_bSendIng;					// ���ͱ�־
		public SendCompletionHandle(JSocketItem pJSocketItem) {
			m_bSendIng = false;
			m_pJSocketItem = pJSocketItem;
			m_SendBufferQueue = new ConcurrentLinkedQueue<ByteBuffer>();
		}
		protected void AddSendData(ByteBuffer pBuffer){
			synchronized (m_SendBufferQueue) {
				if(m_bSendIng){
					m_SendBufferQueue.offer(pBuffer); 
				}else{
					m_bSendIng = true;
					pBuffer.flip();
					m_pJSocketItem.m_ClientSocketChannel.write(pBuffer, pBuffer, this);
				}
			}
		}
		protected void CloseCompletionHandle() {
			m_bSendIng = false;
			m_SendBufferQueue.clear();
			m_SendBufferQueue = null;
			m_pJSocketItem = null;
		}
		@Override
		public void completed(Integer result, ByteBuffer attachment) {
			if(attachment.hasRemaining()){
				System.out.println("SendCompletionHandle:"+attachment.position());
				m_pJSocketItem.m_ClientSocketChannel.write(attachment, attachment, this);
				
			}else{
				attachment.clear();
				attachment = null;
				ByteBuffer pBuffer = null;
				synchronized (m_SendBufferQueue) {
					pBuffer= m_SendBufferQueue.poll();
					if(pBuffer!=null){
						pBuffer.flip();
						m_pJSocketItem.m_ClientSocketChannel.write(pBuffer, pBuffer, this);
					}else{
						m_bSendIng = false;
					}
				}
			}
		}
		@Override
		public void failed(Throwable exc, ByteBuffer attachment) {
			if(m_pJSocketItem!=null && m_pJSocketItem.IsValidSocket() ){
				m_pJSocketItem.CloseSocket(m_pJSocketItem.GetRountID());
			}
			JLogger.severe("SendCompletionHandle:" + exc.getMessage());
		}
	}
	
}
