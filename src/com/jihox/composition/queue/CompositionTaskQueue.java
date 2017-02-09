package com.jihox.composition.queue;

import com.jihox.composition.common.BaseTaskQueue;

public class CompositionTaskQueue extends BaseTaskQueue {

	private static CompositionTaskQueue queue;
	
	public CompositionTaskQueue(){
		super();
		queue = this;
	}
	
	public synchronized static CompositionTaskQueue getCurrentQueue (){
		if (queue==null){
			queue= new CompositionTaskQueue();
		}
		return queue;
	}
}
