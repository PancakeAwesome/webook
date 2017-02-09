package com.jihox.composition.common;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import org.apache.log4j.Logger;

import com.jihox.composition.manager.PropertiesManager;

public abstract class BaseTaskQueue implements TaskQueue {

	private static Logger logger=Logger.getLogger(BaseTaskQueue.class);
	
	protected TaskQueueWatcher watcher;
	protected BlockingQueue<BaseTask> TaskQueue; 
	
	protected static String lock="lock";
	
	public void registerWatcher(TaskQueueWatcher watcher) {
		this.watcher = watcher;
	}
	
	protected BaseTaskQueue(){
		TaskQueue = new ArrayBlockingQueue<BaseTask>(PropertiesManager.getMaxThread());
	}
	
	protected TaskQueueWatcher getWatcher(){
		return watcher;
	}
	
	public boolean addTask(BaseTask task) {
		try {
			if (reportSpace()>0){
				TaskQueue.put(task);
			} else {
				return false;
			}
		} catch (InterruptedException e) {
			logger.error(e.getMessage(),e);
			return false;
		}
		watcher.doWork(task);
		TaskQueue.remove(task);
		return true;
	}
	
	public synchronized int reportSpace() {
		return PropertiesManager.getMaxThread()-this.TaskQueue.size();
	}
	
	public synchronized boolean isEmpty(){
		return TaskQueue.isEmpty();
	}
}
