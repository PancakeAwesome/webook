package com.jihox.composition.queue;

import com.jihox.composition.common.BaseTaskQueue;

public class DownloadTaskQueue extends BaseTaskQueue{

	private static DownloadTaskQueue queue;
	
	public DownloadTaskQueue(){
		super();
		queue = this;
	}
	
	public synchronized static DownloadTaskQueue getCurrentQueue (){
		if (queue==null){
			queue= new DownloadTaskQueue();
		}
		return queue;
	}
}
