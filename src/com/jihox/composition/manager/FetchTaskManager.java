package com.jihox.composition.manager;

import com.jihox.composition.common.BaseManager;
import com.jihox.composition.common.BaseWorker;
import com.jihox.composition.common.TaskQueueWatcher;
import com.jihox.composition.worker.FetchTaskWorker;

public class FetchTaskManager extends BaseManager {

	protected static FetchTaskManager scheduledManager;
	
	private FetchTaskManager(){
		super();
	}

	public static TaskQueueWatcher getInstance(){
		if (scheduledManager==null){
			synchronized(lock){
				if (scheduledManager==null){
					scheduledManager = new FetchTaskManager();
				}
			}
		}
		return scheduledManager;
	}
	
	@Override
	protected BaseWorker createWorker() {
		FetchTaskWorker worker = new FetchTaskWorker();
		worker.setPool(workerPool);
		return worker;
	}
}
