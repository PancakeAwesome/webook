package com.jihox.composition.manager;

import com.jihox.composition.common.BaseManager;
import com.jihox.composition.common.BaseWorker;
import com.jihox.composition.common.TaskQueueWatcher;
import com.jihox.composition.worker.CompositionWorker;

public class CompositionManager  extends BaseManager {

	protected static CompositionManager scheduledManager;

	private CompositionManager(){
		super();
	}

	public static TaskQueueWatcher getInstance(){
		if (scheduledManager==null){
			synchronized(lock){
				if (scheduledManager==null){
					scheduledManager = new CompositionManager();
				}
			}
		}
		return scheduledManager;
	}

	@Override
	protected BaseWorker createWorker() {
		CompositionWorker worker = new CompositionWorker();
		worker.setPool(workerPool);
		return worker;
	}
}
