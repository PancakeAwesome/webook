package com.jihox.composition.main;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import com.jihox.composition.manager.CompositionManager;
import com.jihox.composition.manager.Configuration;
import com.jihox.composition.manager.DuplicateTaskManager;
import com.jihox.composition.manager.FetchTaskManager;
import com.jihox.composition.manager.PropertiesManager;
import com.jihox.composition.manager.UploadManager;
import com.jihox.composition.queue.CompositionTaskQueue;
import com.jihox.composition.queue.DownloadTaskQueue;
import com.jihox.composition.queue.UploadTaskQueue;
import com.jihox.composition.task.DownloadTask;
import com.jihox.composition.utils.CompositionUtils;
import com.jihox.composition.utils.HttpUtils;

public class Startup {
	private static Logger logger=Logger.getLogger(Startup.class);

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Configuration.read();

		PropertyConfigurator.configure(Configuration.LogProperty);

		DownloadTaskQueue dqueue = new DownloadTaskQueue();
		CompositionTaskQueue cqueue = new CompositionTaskQueue();
		UploadTaskQueue uqueue = new UploadTaskQueue();

		dqueue.registerWatcher(FetchTaskManager.getInstance());
		cqueue.registerWatcher(CompositionManager.getInstance());
		uqueue.registerWatcher(UploadManager.getInstance());

		CompositionUtils.prepareTempDirectories();
		
		Startup startup = new Startup();
		List<DownloadTask> initialtasks = new ArrayList<DownloadTask>();
		if (args!=null&&args.length>0){
			initialtasks = startup.makeSingleTask(args);
			for (DownloadTask task : initialtasks){
				dqueue.addTask(task);
			}
		} else {
			while(true){
				if (dqueue.isEmpty()){
					try {
						initialtasks = startup.getDownloadTasks();	
					} catch (Exception e){
						logger.error(e.getMessage(), e);
					}

					for (DownloadTask task : initialtasks){
						dqueue.addTask(task);
					}
				} 
				try {
					Thread.sleep(20000);
				} catch (InterruptedException e) {
					logger.error(e.getMessage(), e);
				}
			}
		}
	}

	private List<DownloadTask> getDownloadTasks(){
		List<DownloadTask> tasks= new ArrayList<DownloadTask>();
		String responsebody = HttpUtils.getTextResult(PropertiesManager.getUncompositedTaskUrl());
		Map<String, Object> responsemap;
		try {
			responsemap = CompositionUtils.getJsonResponse(responsebody);
			List<String> strTasks = (List<String>)responsemap.get(PropertiesManager.getUncompositedTaskKey());
			for (String taskurl: strTasks){
				if (StringUtils.isEmpty(taskurl)||!taskurl.startsWith("http")){
					//					logger.error("Incorrect path for task url "+taskurl+", will ignore and go to the next task.");
					continue;
				}
				if (DuplicateTaskManager.getInstance().isDuplicateTask(taskurl)){
					continue;
				}
				DuplicateTaskManager.getInstance().recordTask(taskurl);
				DownloadTask task = new DownloadTask();
				task.setCheckValue(taskurl);
				task.setKeyvalue(taskurl);
				String filename = taskurl.substring(taskurl.lastIndexOf("/")+1);
				task.setName(filename);
				task.setOrderid(CompositionUtils.getOrderIdFromFileName(filename));
				task.setWorkid(CompositionUtils.getWorkIdFromFileName(filename));
				task.setDescription("Task to download "+filename);
				tasks.add(task);
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		return tasks;
	}
	
	private List<DownloadTask> makeSingleTask(String[] args){
		List<DownloadTask> tasks= new ArrayList<DownloadTask>();
		for (String arg : args){
			if (!StringUtils.isEmpty(arg)){
				arg += "_"+arg+"-001.zip"; 
				String taskurl = "http://www.jihox.com:8080/PBMFS/download/"+arg;
				DownloadTask task = new DownloadTask();
				task.setCheckValue(taskurl);
				task.setKeyvalue(taskurl);
				String filename = taskurl.substring(taskurl.lastIndexOf("/")+1);
				task.setName(filename);
				task.setOrderid(CompositionUtils.getOrderIdFromFileName(filename));
				task.setWorkid(CompositionUtils.getWorkIdFromFileName(filename));
				task.setDescription("Task to download "+filename);
				tasks.add(task);
			}
		}
		return tasks;
	}
}
