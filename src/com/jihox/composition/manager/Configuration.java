package com.jihox.composition.manager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

public class Configuration {
	
	private static Logger logger=Logger.getLogger(Configuration.class);
	
	public static void read() {
		try {
			URL jarLocation = Configuration.class.getProtectionDomain().getCodeSource().getLocation();
			String proLocation=new File(jarLocation.toURI()).getParent()+"/properties";
			BufferedReader br = new BufferedReader(
					new InputStreamReader(new FileInputStream(proLocation),"utf-8")
					);
			String line = br.readLine();
			while (line != null) {
				analysis(line);
				line = br.readLine();
			}
			br.close();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}

	private static void analysis(String line) throws Exception {
		line = line.trim();
		if (line.equals("") == false) {
			if (line.contains("##")) {
				line = line.substring(0, line.indexOf("##"));
			}
			if(line.equals(""))return;
			
			Class clazz = new Configuration().getClass();
			
			if (line.contains("=")) {
				String[] strs = line.split("=");
				if (strs[0].startsWith("#")) {
					line=line.substring(1);
					if(line.substring(0, line.indexOf(".")).equals("OrderDetailPdfMapper")){
						String mapline=line.substring(line.indexOf(".")+1);
						OrderDetailPdfMapper.add(mapline);
					}
				} else {
					Field field=null;
					try {
						field = clazz.getDeclaredField(strs[0]);
					} catch (Exception e) {
						return;
					}
					String v=strs.length>=2?strs[1]:null;
					Object value = v;
					if (field.getType() == int.class) {
						value = Integer.parseInt(v==null?"0":v);
					}else if(field.getType() == float.class)
						value=Float.parseFloat(v==null?"0.0":v);
					field.set(null, value);
				}
			}else if(line.startsWith("#")){
				line=line.substring(1);
				if(line.substring(0, line.indexOf(".")).equals("OrderDetailPdfMapper")){
					String mapline=line.substring(line.indexOf(".")+1);
					OrderDetailPdfMapper.add(mapline);
				}
			}
		}
	}
	
	public static List<String> OrderDetailPdfMapper = new ArrayList<String>();

	public static String ConfiguredUserNameKey;
	public static String ConfiguredPasswordKey;
	public static String ConfiguredUserNameValue;
	public static String ConfiguredPasswordValue;
	public static int MaxThread;
	public static String UncompositedTaskKey;
	public static String WorkPagesUrl;
	public static String OrderDetailUrl;
	public static String UncompositedTaskUrl;
	public static String ZipFolderPath;
	public static String UpdateCompositedWorkUrl;
	public static String UpdateCompositedOrderUrl;
	public static String GetFaceUrl;
	public static String RemoveFileUrl;
	public static String RenameFileUrl;
	public static String HostAddress;
	public static String HostPort;
	public static String UploadUrl;
	public static String FinalOrderZipPath;
	public static String FinalWorkZipPath;
	public static String OrderDetailPDFPath;
	public static String UnzipPath;
	public static String PDFoutputFolder;
	public static String BarcodeFolder;
	public static String MaterialPath;
	public static String LogProperty;
	public static String ServerSuccessKey;
	public static String RenderingHint;
	public static int DPI;

	public static float PixelDescriptionTextHeight;
	public static float BleedingLineLength;
	public static float CoverBleedingMMWidth;
	public static float BleedingLineWidth;
	public static float SpineLineLength;
	public static float LeavesBleedingMMWidth;
//	public static float SpineMMWidth;
	public static float FlapLineLength;
}
