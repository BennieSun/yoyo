package yoyo.timer;

import java.util.ArrayList;
import java.util.LinkedList;

import yoyo.common.logger.JLogger;
import yoyo.common.thread.JThread;
import yoyo.queue.face.IAddQueueSink;
import yoyo.timer.event.NTY_TimerEvent;
import yoyo.timer.face.ITimerService;

class TimerData{
	public int					m_dwTimerID;			//��ʱ��ID
	public long 				m_lElapse;				//ʱ����
	public long 				m_lTimeLeave;			//����ʱʣ��ʱ��
	public int					m_dwRepeatTimes;		//�ظ�����
	public Object				m_bindParamter;			//�󶨵Ĳ���
	
	public TimerData(){
		ResetData(0,0,0,null);
	}
	public void ResetData(int dwTimerID, long lElapse, int dwRepeat,Object bindParamter){
		m_dwTimerID				= dwTimerID;
		m_lElapse				= lElapse;
		m_lTimeLeave			= lElapse;
		m_dwRepeatTimes			= dwRepeat;
		m_bindParamter			= bindParamter;
	}
}

class JTimerThread extends JThread{
	
	private JTimerService m_pTimerService;
	
	public JTimerThread(int dwPriority) {
		super("Timer-Thread",dwPriority);
	}
	public boolean InitThread(JTimerService pTimerService){
		if( IsRunning() ){
			return false;
		}
		if(pTimerService == null) {
			return false;
		}
		m_pTimerService = pTimerService;
		return true;
	}
	@Override
	protected boolean OnEventThreadStart(){
		return true;
	}
	@Override
	protected boolean OnEventThreadClose(){
		return true;
	}
	@Override
	protected boolean OnEventThreadRun(){
		//����ʱֹͣһ����λ
		sleep(JTimerService.TIMER_SPACE);
		return m_pTimerService.OnTimerServiceRun();
	}
}

public class JTimerService implements ITimerService {
	/**
	 * ʱ���¼���ʶ
	 */
	public static final int	EVENT_TIMER				= 0x0001;
	
	protected static final long		TIMER_SPACE	= 25;
	
	private volatile boolean		m_bService;

	private LinkedList<TimerData>	m_timerTaskFree;		// ��������
	private ArrayList<TimerData>	m_timerTaskActive;		// �����
	private IAddQueueSink			m_pTimerAddQueueSink;
	
	private JTimerThread			m_JTimerThread;
	
	public JTimerService(){
		m_bService 				= false;
		m_pTimerAddQueueSink	= null;
		m_timerTaskFree			= new LinkedList<TimerData>();
		m_timerTaskActive		= new ArrayList<TimerData>();
	}
	
	@Override
	public boolean StartService() {
		if(m_bService){
			JLogger.warning("ʱ������Ѿ�����,������������!");
			return true;
		}
		m_JTimerThread = new JTimerThread(Thread.MAX_PRIORITY);
		if( m_JTimerThread.InitThread(this) == false){
			JLogger.warning("ʱ������̳߳�ʼ��ʧ��!");
			return false;
		}
		//�����߳�
		if( m_JTimerThread.StartThread() == false){
			JLogger.warning("ʱ������߳�����ʧ��!");
			return false;
		}
		m_bService = true;
		return true;
	}

	@Override
	public boolean StopService() {
		if(!m_bService) return false;
		synchronized (m_timerTaskActive) {
			m_JTimerThread.ChoseThread();
			m_timerTaskFree.addAll(m_timerTaskActive);
			m_timerTaskActive.clear();
		}
		m_bService = false;
		return true;
	}

	@Override
	public boolean SetAddQueueSink(IAddQueueSink pITimerAddQueueSink) {
		if (m_bService)
			return false;
		m_pTimerAddQueueSink = pITimerAddQueueSink;
		return true;
	}

	@Override
	public boolean SetTimer(int dwTimerID, long lElapse, int dwRepeat,Object bindParamter) {
		if (dwTimerID < 0 || lElapse < TIMER_SPACE || dwRepeat<0)
		{
			JLogger.severe("SetTimer Error dwTimerID<0 or lElapse < 25 or dwRepeat<0");
			return false;
		}
		synchronized(m_timerTaskActive)
		{
			// �����Ƿ������ͬID�Ķ�ʱ��
			boolean bTimerExist = false;
			TimerData tempTimerData = null;
			int lens = m_timerTaskActive.size();
			for (int i = 0; i < lens; i++)
			{
				tempTimerData = m_timerTaskActive.get(i);
				if (tempTimerData.m_dwTimerID == dwTimerID)
				{
					bTimerExist = true;
					JLogger.warning("SetTimer ������ͬ��ID:dwTimerID=" + dwTimerID);
					break;
				}
			}
			// ��������ڴ����µĶ�ʱ��
			// ������� ���ظ�ID�����¸�ֵ(�˴�����Ҫע����,ͬһ��Ӧ���ﲻ���Գ��ֶԸ���ͬID�Ķ�ʱ��)
			if (bTimerExist == false)
			{
				if (m_timerTaskFree.size() > 0)
				{
					tempTimerData = m_timerTaskFree.removeFirst();
				} else
				{
					tempTimerData = new TimerData();
				}
			}
			tempTimerData.ResetData(dwTimerID,lElapse,dwRepeat,bindParamter);
			
			// ���ִֻ��һ�� ��ǰ5�����֪ͨ  ֮ǰ��10��
			tempTimerData.m_lTimeLeave = (dwRepeat==1)?(Math.max(TIMER_SPACE, lElapse - TIMER_SPACE * 5)):lElapse;
			m_timerTaskActive.add(tempTimerData);
			m_timerTaskActive.notify();
		}
		return true;
	}

	@Override
	public boolean KillTimer(int dwTimerID) {
		TimerData tempTimerData = null;
		synchronized(m_timerTaskActive)
		{
			for (int i = 0; i < m_timerTaskActive.size(); i++)
			{
				tempTimerData = m_timerTaskActive.get(i);
				if (tempTimerData.m_dwTimerID == dwTimerID)
				{
					m_timerTaskActive.remove(i);
					m_timerTaskFree.add(tempTimerData);
					return true;
				}
			}
		}
		return true;
	}

	@Override
	public boolean KillAllTimer() {
		synchronized (m_timerTaskActive) {
			m_timerTaskFree.addAll(m_timerTaskActive);
			m_timerTaskActive.clear();
			try {
				m_timerTaskActive.wait();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		return true;
	}
	protected boolean OnTimerServiceRun(){
		synchronized (m_timerTaskActive) {
			int len = m_timerTaskActive.size() - 1;
			if(len<0){
				try {
					m_timerTaskActive.wait();
				} catch (InterruptedException e) {
					if(m_bService){
						e.printStackTrace();
					}
					return false;
				}
			}else{

				TimerData tempTimerData = null;
				for (int i = len; i >= 0; i--)
				{
					tempTimerData = m_timerTaskActive.get(i);
					if(tempTimerData==null){
						m_timerTaskActive.remove(i);
						continue;
					}
					if (tempTimerData.m_lTimeLeave > TIMER_SPACE){
						tempTimerData.m_lTimeLeave -= TIMER_SPACE;
					}else{
						tempTimerData.m_lTimeLeave = 0;
					}
					
					if (tempTimerData.m_lTimeLeave <= 0)
					{
						NTY_TimerEvent timerEvent = new NTY_TimerEvent(tempTimerData.m_dwTimerID, tempTimerData.m_dwRepeatTimes, tempTimerData.m_bindParamter);
						m_pTimerAddQueueSink.AddToQueueSink(EVENT_TIMER, timerEvent);
						if (tempTimerData.m_dwRepeatTimes != 0)
						{
							tempTimerData.m_dwRepeatTimes--;
							switch (tempTimerData.m_dwRepeatTimes)
							{
								case 0:
								{
									m_timerTaskActive.remove(i);
									m_timerTaskFree.add(tempTimerData);
									break;
								}
								case 1:
								{
									tempTimerData.m_lTimeLeave = Math.max(TIMER_SPACE, tempTimerData.m_lElapse - TIMER_SPACE * 5);
									break;
								}
								default:
								{
									tempTimerData.m_lTimeLeave = tempTimerData.m_lElapse;
								}
							}
						}else
						{
							tempTimerData.m_lTimeLeave = tempTimerData.m_lElapse;
						}
					}
				}
			}
		}
		return true;
	}
}