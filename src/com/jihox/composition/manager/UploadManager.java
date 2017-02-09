package com.jihox.composition.manager;

import com.jihox.composition.common.BaseManager;
import com.jihox.composition.common.BaseWorker;
import com.jihox.composition.common.TaskQueueWatcher;
import com.jihox.composition.worker.UploadTaskWorker;

public class UploadManager extends BaseManager {

	protected static UploadManager scheduledManager;

	private UploadManager(){
		super();
	}

	@Override
	protected BaseWorker createWorker() {
		UploadTaskWorker worker = new UploadTaskWorker();
		worker.setPool(workerPool);
		return worker;
	}

	public static TaskQueueWatcher getInstance(){
		if (scheduledManager==null){
			synchronized(lock){
				if (scheduledManager==null){
					scheduledManager = new UploadManager();
				}
			}
		}
		return scheduledManager;
	}
}
