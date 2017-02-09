package com.jihox.composition.manager;

import java.util.HashSet;
import java.util.Set;

public class DuplicateTaskManager {
	private Set<String> queuedTask = new HashSet<String>();
	
	private static DuplicateTaskManager manager;
	
	private DuplicateTaskManager(){
		
	}
	
	public synchronized static DuplicateTaskManager getInstance(){
		if (manager==null){
			manager = new DuplicateTaskManager();
		}
		return manager;
	}
	
	public synchronized boolean isDuplicateTask(String taskValue){
		return queuedTask.contains(taskValue);
	}
	
	public synchronized void recordTask(String taskValue){
		queuedTask.add(taskValue);
	}
	
	public synchronized void eliminateTask(String taskValue){
		queuedTask.remove(taskValue);
	}
}
