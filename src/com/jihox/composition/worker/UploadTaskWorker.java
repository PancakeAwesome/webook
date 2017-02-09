package com.jihox.composition.worker;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import com.jihox.composition.common.BaseWorker;
import com.jihox.composition.manager.PropertiesManager;
import com.jihox.composition.task.UploadTask;
import com.jihox.composition.utils.CompositionUtils;
import com.jihox.composition.utils.HttpUtils;

public class UploadTaskWorker extends BaseWorker {

	private static Logger logger=Logger.getLogger(UploadTaskWorker.class);
	@Override
	protected void dobusiness() {

		UploadTask task = (UploadTask)this.task;

		logger.info("UploadTaskWorker "+getWorkerName()+" start uploading");

//		String paths = task.getKeyvalue();
//
//		if (!paths.contains(",")
//				||paths.split(",").length!=2
//				||StringUtils.isEmpty(paths.split(",")[0])
//				||StringUtils.isEmpty(paths.split(",")[1])
//				){
//			logger.info("UploadTaskWorker "+getWorkerName()+" error: can not find source file "+ paths);
//			renewTask();
//			return;
//		}

//		String finalOrderZipPath = paths.split(",")[0];
//		String finalWorkZipPath = paths.split(",")[1];
		
		String finalOrderZipPath = task.getKeyvalue();
		
//		String uploadWorkresponse = uploadCompositedWorkzip(finalWorkZipPath);
		String uploadedWorlUrl="/"+task.getWorkid();
//		try {
//			Map<String,Object> jsonObj = CompositionUtils.getJsonResponse(uploadWorkresponse);
//			if (jsonObj.containsKey("fileurl")){
//				uploadedWorlUrl = jsonObj.get("fileurl")+"";
//			}
//		} catch (Exception e) {
//			logger.info(e);
//		}
//		if (StringUtils.isEmpty(uploadedWorlUrl)){
//			logger.info("UploadTaskWorker "+getWorkerName()+" upload "+finalWorkZipPath+" error.");
//			renewTask();
//			return;
//		}
		
//		logger.info("UploadTaskWorker "+getWorkerName()+" upload "+finalWorkZipPath+" done.");
		
		//add logic to delete server file
//		logger.info("rename work file on server");
//		String workfilename = FilenameUtils.getName(finalWorkZipPath);
//		boolean deleteworkresult = CompositionUtils.renameServerFile(workfilename);
//		if (deleteworkresult){
//			logger.info("rename work file on server done.");
//		} else {
//			logger.info("rename work file on server falied.");
//		}
		
		Map<String, String> workmap = new HashMap<String, String>();
		workmap.put("url", uploadedWorlUrl);

		String workid = task.getWorkid();
		String updateWorkUrl = PropertiesManager.getUpdateCompositedWorkUrl()+workid+"/";
		boolean worksuccess = updateWorkStatus(updateWorkUrl, workmap);
		if (!worksuccess){
			logger.info("UploadTaskWorker "+getWorkerName()+" update work status error for "+workid);
			renewTask();
			return;
		}
		logger.info("UploadTaskWorker "+getWorkerName()+" update work status done for "+workid);

		
//		logger.info("rename original file on server");
//		String originalname = CompositionUtils.getOriginalName(task.getOrderid(), task.getWorkid());
//		boolean deleteoriginalesult = CompositionUtils.renameServerFile(originalname);
//		if (deleteoriginalesult){
//			logger.info("rename original file on server done.");
//		} else {
//			logger.info("rename original file on server falied.");
//		}
		
		String uploadOrderResponse = uploadCompositedOrderzip(finalOrderZipPath);
		String uploadOrderUrl = "";
		
		try {
			Map<String,Object> jsonObj = CompositionUtils.getJsonResponse(uploadOrderResponse);
			if (jsonObj.containsKey("fileurl")){
				uploadOrderUrl = jsonObj.get("fileurl")+"";
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}

		if (StringUtils.isEmpty(uploadOrderUrl)){
			logger.info("UploadTaskWorker "+getWorkerName()+" upload "+finalOrderZipPath+" error");
			renewTask();
			return;
		}
		
		logger.info("UploadTaskWorker "+getWorkerName()+" upload "+finalOrderZipPath+" done.");

		Map<String, String> ordermap = new HashMap<String, String>();
		ordermap.put("url", uploadOrderUrl);

		String orderid = task.getOrderid();
		String updateOrderUrl = PropertiesManager.getUpdateCompositedOrderUrl()+"/"+orderid;
		boolean ordersuccess = updateOrderStatus(updateOrderUrl, ordermap);
		if (!ordersuccess){
			logger.info("UploadTaskWorker "+getWorkerName()+" update order status error for "+orderid);
			renewTask();
			return;
		}
		logger.info("UploadTaskWorker "+getWorkerName()+" update order status done for "+orderid);

//		try {
//			logger.info("UploadTaskWorker "+getWorkerName()+" deleting local files: "+finalOrderZipPath+", "+finalWorkZipPath);
//			Files.delete(Paths.get(finalOrderZipPath));
//			Files.delete(Paths.get(finalWorkZipPath));
//			logger.info("UploadTaskWorker "+getWorkerName()+" delete local files: "+finalOrderZipPath+", "+finalWorkZipPath+" done.");
//		} catch (IOException e) {
//			logger.info(e);
//		}
		
		try {
			logger.info("UploadTaskWorker "+getWorkerName()+" deleting local files: "+finalOrderZipPath);
			Files.delete(Paths.get(finalOrderZipPath));
			logger.info("UploadTaskWorker "+getWorkerName()+" delete local files: "+finalOrderZipPath+" done.");
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
		}
		
		CompositionUtils.clearTempFiles(orderid, workid);
		
		logger.info("UploadTaskWorker "+getWorkerName()+" end uploading");
	}

//	private String uploadCompositedWorkzip (String localpath){
//		String uploadurl = PropertiesManager.getUploadUrl();
//		String uploadedurl = HttpUtils.uploadFile(localpath, uploadurl);
//		return uploadedurl;
//	}

	private String uploadCompositedOrderzip (String localpath){
		String uploadurl = PropertiesManager.getUploadUrl();
		String uploadedurl = HttpUtils.uploadFile(localpath, uploadurl);
		return uploadedurl;
	}

	private boolean updateWorkStatus(String compositedWorkUrl, Map<String, String> paramMap){
		boolean ret = false;
		String result = HttpUtils.postData(paramMap, compositedWorkUrl);
		try {
			Map<String,Object> jsonresult = CompositionUtils.getJsonResponse(result);
			if (jsonresult.get("success")!=null&&(Boolean)jsonresult.get("success")==true){
				ret = true;
			} else {
				logger.info(jsonresult.get("msg"));
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		return ret;
	}

	private boolean updateOrderStatus(String compositedOrderUrl, Map<String, String> paramMap){
		boolean ret = false;
		String result = HttpUtils.postData(paramMap, compositedOrderUrl);
		try {
			Map<String,Object> jsonresult = CompositionUtils.getJsonResponse(result);
			if (jsonresult.get("success")!=null&&(Boolean)jsonresult.get("success")==true){
				ret = true;
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		return ret;
	}
}
