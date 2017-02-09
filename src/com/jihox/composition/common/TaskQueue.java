package com.jihox.composition.common;

public interface TaskQueue {
	public void registerWatcher(TaskQueueWatcher watcher);
	public int reportSpace();
	public boolean addTask(BaseTask task);
}
