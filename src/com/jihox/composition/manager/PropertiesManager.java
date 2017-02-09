package com.jihox.composition.manager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PropertiesManager {
	
	private static PropertiesManager instance;
	
	private Map<String, String> properties;
	
	private static String lock="lock";
	
	private PropertiesManager(){
		if (properties==null){
			properties = new HashMap<>();
		}
	}
	
	public static PropertiesManager getInstance(){
		if (instance==null){
			synchronized(lock){
				if (instance==null){
					instance = new PropertiesManager();
				}
			}
		}
		return instance;
	}
	
	public String getPropertyValue(String key){
		String value = "";
		if (properties.containsKey(key)){
			value = properties.get(key);
		}
		return value;
	}
	
	public Map<String, String> getAuthenticationMap(){
		Map<String, String> map = new HashMap<String, String>();
		map.put(getConfiguredUserNameKey(), getConfiguredUserNameValue());
		map.put(getConfiguredPasswordKey(), getConfiguredPasswordValue());
		return map;
	}
	
	public String getConfiguredUserNameKey(){
		return Configuration.ConfiguredUserNameKey;
	}
	
	public String getConfiguredPasswordKey(){
		return Configuration.ConfiguredPasswordKey;
	}
	
	public String getConfiguredUserNameValue(){
		return Configuration.ConfiguredUserNameValue;
	}
	
	public String getConfiguredPasswordValue(){
		return Configuration.ConfiguredPasswordValue;
	}
	
	public static int getMaxThread(){
		return Configuration.MaxThread;
	}
	
	public static String getUncompositedTaskKey (){
		return Configuration.UncompositedTaskKey;
	}
	
	public static String getOrderDetailUrl(){
		return getHostAddress()+":"+getHostPort()+Configuration.OrderDetailUrl;
	}
	
	public static String getUncompositedTaskUrl (){
		return getHostAddress()+":"+getHostPort()+Configuration.UncompositedTaskUrl;
	}
	
	public static String getWorkPagesUrl (){
		return getHostAddress()+":"+getHostPort()+Configuration.WorkPagesUrl;
	}
	
	public static String getZipFolderPath(){
		return Configuration.ZipFolderPath;
	}
	
	public static String getUpdateCompositedWorkUrl (){
		return getHostAddress()+":"+getHostPort()+Configuration.UpdateCompositedWorkUrl;
	}
	
	public static String getUpdateCompositedOrderUrl(){
		return getHostAddress()+":"+getHostPort()+Configuration.UpdateCompositedOrderUrl;
	}
	
	public static String getFaceUrl(){
		return getHostAddress()+":"+getHostPort()+Configuration.GetFaceUrl;
	}
	
	public static String getHostAddress (){
		return Configuration.HostAddress; 
	}
	
	public static String getHostPort (){
		return Configuration.HostPort;
	}
	
	public static String getUploadUrl (){
		return getHostAddress()+":"+getHostPort()+Configuration.UploadUrl;
	}
	
	public static String getFinalOrderZipPath(){
		return Configuration.FinalOrderZipPath;
	}
	
	public static String getFinalWorkZipPath(){
		return Configuration.FinalWorkZipPath;
	}
	
	public static String getOrderDetailPDFPath(){
		return Configuration.OrderDetailPDFPath;
	}
	
	public static String getUnzipPath (){
		return Configuration.UnzipPath;
	}
	
	public static String getPDFoutputFolder(){
		return Configuration.PDFoutputFolder;
	}
	
	public static String getBarcodeFolder(){
		return Configuration.BarcodeFolder;
	}
	
	public static String getMaterialPath(){
		return Configuration.MaterialPath;
	}
	
	public static List<String> getOrderDetailPdfMapper (){
		return Configuration.OrderDetailPdfMapper;
	}
	
	public static int getDPI (){
		return Configuration.DPI;
	}
	
	public static float getScaleFactorByDPI(){
		return Configuration.DPI/72;
	}

	public static float getPixelDescriptionTextHeight() {
		return Configuration.PixelDescriptionTextHeight;
	}

	public static float getBleedingLineLength() {
		return Configuration.BleedingLineLength;
	}

	public static String getRemoveFileURL(){
		return getHostAddress()+":"+getHostPort()+Configuration.RemoveFileUrl;
	}
	
	public static String getSuccessKey(){
		return Configuration.ServerSuccessKey;
	}
	
	public static float getCoverBleedingMMWidth() {
		return Configuration.CoverBleedingMMWidth;
	}

	public static float getBleedingLineWidth() {
		return Configuration.BleedingLineWidth;
	}

	public static float getSpineLineLength() {
		return Configuration.SpineLineLength;
	}

	public static float getLeavesBleedingMMWidth() {
		return Configuration.LeavesBleedingMMWidth;
	}

//	public static float getSpineMMWidth() {
//		return Configuration.SpineMMWidth;
//	}
	
	public static float getFlapLineLength() {
		return Configuration.FlapLineLength;
	}
	
	public static String getRenameServerFileURL(){
		return getHostAddress()+":"+getHostPort()+Configuration.RenameFileUrl;
	}
	
	public static String getRenderingHint(){
		return Configuration.RenderingHint;
	}
}
