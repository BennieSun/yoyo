package yoyo.database;

import java.util.ArrayList;
import java.util.concurrent.LinkedBlockingQueue;

import yoyo.common.logger.JLogger;
import yoyo.database.event.NTY_DBEvent;
import yoyo.database.face.IDBService;
import yoyo.database.face.IDBSink;

public class JDBService implements IDBService {

	private volatile boolean 					m_bService;
	
	private LinkedBlockingQueue<NTY_DBEvent>	m_DBEventQueue;
	private ArrayList<IDBSink>					m_pDBEngineSinks;
	
	public JDBService(){
		m_bService = false;
		m_DBEventQueue = new LinkedBlockingQueue<NTY_DBEvent>();
		m_pDBEngineSinks = new ArrayList<IDBSink>();
	}
	@Override
	public boolean StartService() {
		if(m_bService) {
			JLogger.warning("���ݿ�����������,����������!");
			return true;
		}
		
		if(m_pDBEngineSinks.size() == 0){
			JLogger.severe("���ݿ⴦���Ӳ�����!");
			return false;
		}
		
		for(int i = 0;i<m_pDBEngineSinks.size();i++)
		{
			if( m_pDBEngineSinks.get(i).DBEngineStart() == false ){
				JLogger.severe("���ݿ⴦��������ʧ��!");
				return false;
			}
		}
		
		m_bService = true;
		return true;
	}

	@Override
	public boolean StopService() {
		if(!m_bService) return true;
		
		for(int i = 0;i<m_pDBEngineSinks.size();i++)
		{
			if( m_pDBEngineSinks.get(i).DBEngineStop() == false ){
				JLogger.severe("ֹͣ���ݿ⴦����ʱ����!");
				return false;
			}
		}
		m_pDBEngineSinks.clear();
		m_bService = false;
		return true;
	}
	@Override
	public boolean  AddDBSink(IDBSink pDBSink){
		if(m_bService || pDBSink== null) return false;
		pDBSink.SetIDBService( this );
		m_pDBEngineSinks.add(pDBSink);
		return true;
	}

	@Override
	public boolean PostDBRequest(int dwRequestID, int dwSocketID, Object pData) {
		if(!m_bService) {
			JLogger.severe("���ݿ��������δ�����ɹ�,��������ʧ��!");
			return false;
		}
		NTY_DBEvent dbEvent = new NTY_DBEvent(dwRequestID, dwSocketID, pData);
		try {
			m_DBEventQueue.put(dbEvent);
		} catch (InterruptedException e) {
			JLogger.severe("���ݿ��������ʧ��["+dwRequestID+"]:" + e.getMessage());
		}
		return true;
	}
	@Override
	public NTY_DBEvent GetDBEvent() {
		NTY_DBEvent dbEvent = null;
		try {
			dbEvent = m_DBEventQueue.take();
		} catch (InterruptedException e) {
			if(m_bService){
				JLogger.severe("���ݿ��ȡ����ʧ��:" + e.getMessage());
			}
		}
		return dbEvent;
	}

}
