package com.jihox.composition.queue;

import com.jihox.composition.common.BaseTaskQueue;

public class UploadTaskQueue extends BaseTaskQueue {

	private static UploadTaskQueue queue;
	
	public UploadTaskQueue(){
		super();
		queue = this;
	}
	
	public synchronized static UploadTaskQueue getCurrentQueue (){
		if (queue==null){
			queue= new UploadTaskQueue();
		}
		return queue;
	}
}
