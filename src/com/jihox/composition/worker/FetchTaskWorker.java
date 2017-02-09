package com.jihox.composition.worker;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import com.jihox.composition.common.BaseWorker;
import com.jihox.composition.queue.CompositionTaskQueue;
import com.jihox.composition.task.CompositionTask;
import com.jihox.composition.task.DownloadTask;
import com.jihox.composition.utils.CompositionUtils;
import com.jihox.composition.utils.HttpUtils;

public class FetchTaskWorker extends BaseWorker{

	private static Logger logger=Logger.getLogger(FetchTaskWorker.class);
	@Override
	protected void dobusiness() {

		DownloadTask task = (DownloadTask)this.task;

		String url = task.getKeyvalue();

		logger.info("FetchTaskWorker "+getWorkerName()+" start "+task.getDescription());

		String downloadedPath = HttpUtils.DownloadOriginalZip(url);
		if (!StringUtils.isEmpty(downloadedPath)&&downloadedPath.endsWith("null")||downloadedPath==null){
			logger.info("FetchTaskWorker "+getWorkerName()+" failed to download file from "+url+", file may have been corrupted in server side.");
			if (Files.exists(Paths.get(downloadedPath))){
				try {
					Files.delete(Paths.get(downloadedPath));
				} catch (IOException e) {
					logger.error(e.getMessage(), e);
				}
			}
			renewTask();
			return;
		} else if (StringUtils.isEmpty(downloadedPath)){
			logger.info("FetchTaskWorker "+getWorkerName()+" failed to download file from "+url+", file may have been corrupted in server side.");
			renewTask();
			return;
		}
		CompositionTask nextTask = new CompositionTask();
		nextTask.setKeyvalue(downloadedPath);
		nextTask.setCheckValue(task.getCheckValue());
		String filename = downloadedPath.substring(downloadedPath.lastIndexOf("/")+1);
		nextTask.setName(filename);
		String orderid = CompositionUtils.getOrderIdFromFileName(filename);
		String workid = CompositionUtils.getWorkIdFromFileName(filename);
		nextTask.setId(orderid+"_"+workid);
		nextTask.setOrderid(orderid);
		nextTask.setWorkid(workid);
		nextTask.setDescription("Composite file "+filename);

		CompositionTaskQueue.getCurrentQueue().addTask(nextTask);

		logger.info("FetchTaskWorker "+getWorkerName()+" end "+task.getDescription());
	}

}
