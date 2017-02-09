package com.jihox.composition.common;

import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;

import com.jihox.composition.manager.PropertiesManager;

public abstract class BaseManager implements TaskQueueWatcher {

	private static Logger logger=Logger.getLogger(BaseManager.class);
	protected static String lock="lock";
	protected Set workerPool;
	
	protected BaseManager(){
		logger.info("Initialize "+PropertiesManager.getMaxThread()+" workers for the pool.");
		createWorkerPool();
	}
	
	protected void createWorkerPool(){
		workerPool = new HashSet<BaseWorker>();
		for (int i=0;i<PropertiesManager.getMaxThread();i++){
			BaseWorker worker = createWorker();
			worker.setWorkerName(i+"");
			workerPool.add(worker);
		}
	}
	
	protected abstract BaseWorker createWorker();
	
	public void doWork(BaseTask task) {
		BaseWorker worker = getWorker();
		worker.setTask(task);
		new Thread(worker).start();
	}
	
	protected synchronized BaseWorker getWorker() {
		BaseWorker worker = null;
		while (workerPool.isEmpty()){
			try {
				logger.info("No worker available, wait 20 seconds for another try.");
				Thread.sleep(20000);
			} catch (InterruptedException e) {
				logger.error(e.getMessage(),e);
			}
		}
		
		Object[] arrWorker = workerPool.toArray();
		worker =(BaseWorker)arrWorker[arrWorker.length-1];
		
		workerPool.remove(worker);

		return worker;
	}
}
