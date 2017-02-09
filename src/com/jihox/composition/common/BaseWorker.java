package com.jihox.composition.common;

import java.util.Set;

import org.apache.log4j.Logger;

import com.jihox.composition.manager.DuplicateTaskManager;

public abstract class BaseWorker implements Runnable {

	private static Logger logger=Logger.getLogger(BaseWorker.class);
	
	private Set pool;

	protected BaseTask task;

	private String workerName;

	public void setPool(Set pool){
		this.pool = pool;
	}

	protected synchronized void finish() {
		pool.add(this);
	}

	public void setTask(BaseTask task){
		this.task = task;
	}

	public String getWorkerName() {
		return workerName;
	}

	public void setWorkerName(String workerName) {
		this.workerName = workerName;
	}

	@Override
	public void run() {
		try {
			dobusiness();
		} catch (Exception e){
			logger.error(e.getMessage(), e);
		} finally {
			finish();
		}
	}
	
	protected void renewTask (){
		DuplicateTaskManager.getInstance().eliminateTask(task.getCheckValue());
	}

	protected abstract void dobusiness ();

}
