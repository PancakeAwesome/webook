package com.jihox.composition.utils;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

import javax.imageio.ImageIO;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.zip.Zip64Mode;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.util.JSONPObject;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.jihox.composition.manager.DuplicateTaskManager;
import com.jihox.composition.manager.PropertiesManager;
import com.jihox.composition.model.book.org.example.photobook.WeBook;
import com.jihox.composition.model.book.org.example.photobook.WeBookContent;
import com.jihox.composition.model.book.org.example.photobook.WeBookPage;
import com.jihox.composition.task.DownloadTask;
import com.jihox.composition.worker.CompositionWorker;

public class CompositionUtils {

	private static Logger logger=Logger.getLogger(CompositionUtils.class);

	public static WeBook getWeBookInfo(String workId) {
		WeBook book = null;
		
		String responsebody = HttpUtils.getTextResult(PropertiesManager.getWorkPagesUrl()+workId+"/");
		Map<String, Object> responsemap = null;
		
		try {
//			responsemap = CompositionUtils.getJsonResponse(responsebody);
//			Double thickness = (Double) responsemap.get("thickness");
//			book.setThickness(thickness.floatValue());
			
			
//			List<WeBookPage> pages = new ArrayList<WeBookPage>();
//			
			ObjectMapper mapper = new ObjectMapper();

//			String pagesString = responsemap.get("pages").toString();
//			
//			System.out.println(pagesString);

			book = mapper.readValue(responsebody, WeBook.class);
			
//			for (int i = 0; i < webookpages.length; i ++) {
//				pages.add(webookpages[i]);
//			}
			
//			book.setPages(pages);
//			book.setPages((List<WeBookPage>)responsemap.get("pages"));
			
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		
		return book;
	}
	
	public static List<WeBookPage> getPages(String workId){

		List<WeBookPage> pages = new ArrayList<WeBookPage>();
		
		String responsebody = HttpUtils.getTextResult(PropertiesManager.getWorkPagesUrl()+workId+"/");
		Map<String, Object> responsemap = null;
		try {
			responsemap = CompositionUtils.getJsonResponse(responsebody);
			
//			ObjectMapper mapper = new ObjectMapper();
//			
//			WeBookPage[] webookpages = mapper.readValue(responsebody, WeBookPage[].class);
//			
//			for (int i = 0; i < webookpages.length; i ++) {
//				pages.add(webookpages[i]);
//			}

			
			
////			List<String> strTasks = (List<String>)responsemap.get(PropertiesManager.getUncompositedTaskKey());
////			for (String taskurl: strTasks){
////				if (StringUtils.isEmpty(taskurl)||!taskurl.startsWith("http")){
////					//					logger.error("Incorrect path for task url "+taskurl+", will ignore and go to the next task.");
////					continue;
////				}
////				if (DuplicateTaskManager.getInstance().isDuplicateTask(taskurl)){
////					continue;
////				}
////				DuplicateTaskManager.getInstance().recordTask(taskurl);
////				DownloadTask task = new DownloadTask();
////				task.setCheckValue(taskurl);
////				task.setKeyvalue(taskurl);
////				String filename = taskurl.substring(taskurl.lastIndexOf("/")+1);
////				task.setName(filename);
////				task.setOrderid(CompositionUtils.getOrderIdFromFileName(filename));
////				task.setWorkid(CompositionUtils.getWorkIdFromFileName(filename));
////				task.setDescription("Task to download "+filename);
////				tasks.add(task);
////			}
//			
//			
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}

		
//		
//		WeBookPage cover = new WeBookPage();
//		cover.setHeight(210);
//		cover.setWidth(148);
//		cover.setType("cover1");
//		
//		List<WeBookContent> contents = new  ArrayList<WeBookContent>();
//		WeBookContent content1 = new WeBookContent();
//		content1.setType("text");
//		content1.setText("Kevin的微信书");
//		content1.setSize(30);
//		content1.setTop(80);
//		content1.setLeft(74);
//		content1.setWidth(74);
//		content1.setHeight(20);
//		content1.setFace("ht");
//		
//		
//		WeBookContent content2 = new WeBookContent();
//		content2.setType("background");
//		content2.setUri("background1.jpg");
//		content2.setTop(0);
//		content2.setLeft(0);
//		content2.setWidth(148);
//		content2.setHeight(210);
//		
//		WeBookContent content3 = new WeBookContent();
//		content3.setType("text");
//		content3.setText("Kevin . 作品");
//		content3.setSize(12);
//		content3.setTop(110);
//		content3.setLeft(104);
//		content3.setWidth(44);
//		content3.setHeight(15);
//		content3.setFace("ht");
//		
//		contents.add(content2);
//		contents.add(content1);
//		contents.add(content3);
//		cover.setContents(contents);
//		pages.add(cover);
//		
//		WeBookPage backCover = new WeBookPage();
//		backCover.setHeight(210);
//		backCover.setWidth(148);
//		backCover.setType("cover2");
//		WeBookContent content4 = new WeBookContent();
//		content4.setType("background");
//		content4.setUri("background2.jpg");
//		content4.setTop(0);
//		content4.setLeft(0);
//		content4.setWidth(148);
//		content4.setHeight(210);
//		contents = new  ArrayList<WeBookContent>();
//		contents.add(content4);
//		
//		content3 = new WeBookContent();
//		content3.setType("decorate");
//		content3.setUri("code128.jpg");
//		content3.setTop(160);
//		content3.setLeft(100);
//		content3.setWidth(40);
//		content3.setHeight(40);
//		contents.add(content3);
//		
//		backCover.setContents(contents);
//		pages.add(backCover);
//		
//		WeBookPage page1 = new WeBookPage();
//		page1.setHeight(210);
//		page1.setWidth(148);
//		page1.setType("normal");
//		content4 = new WeBookContent();
//		content4.setType("decorate");
//		content4.setUri("decorate1.jpg");
//		content4.setTop(20);
//		content4.setLeft(20);
//		content4.setWidth(105);
//		content4.setHeight(145);
//		contents = new  ArrayList<WeBookContent>();
//		
//		content3 = new WeBookContent();
//		content3.setType("text");
//		content3.setText("毕竟西湖六月中，毕竟西湖六月下。");
//		content3.setLinespacing(3);
//		content3.setSize(12);
//		content3.setTop(180);
//		content3.setLeft(20);
//		content3.setWidth(96);
//		content3.setHeight(12);
//		content3.setFace("ht");
//		contents.add(content4);
//		contents.add(content3);
//		page1.setContents(contents);
//		pages.add(page1);
//		
//		WeBookPage page2 = new WeBookPage();
//		page2.setHeight(210);
//		page2.setWidth(148);
//		page2.setType("normal");
////		content4 = new WeBookContent();
////		content4.setType("picture");
////		content4.setUri("decorate1.jpg");
////		content4.setTop(20);
////		content4.setLeft(20);
////		content4.setWidth(105);
////		content4.setHeight(145);
////		contents = new  ArrayList<WeBookContent>();
//		contents = new  ArrayList<WeBookContent>();
//		content3 = new WeBookContent();
//		content3.setType("text");
//		content3.setText("01/周六");
//		content3.setSize(12);
//		content3.setTop(10);
//		content3.setLeft(10);
//		content3.setWidth(60);
//		content3.setHeight(12);
//		content3.setFace("ht");
//		
//		content2 = new WeBookContent();
//		content2.setType("text");
//		content2.setText("09：11");
//		content2.setSize(12);
//		content2.setTop(10);
//		content2.setLeft(56);
//		content2.setWidth(60);
//		content2.setHeight(12);
//		content2.setFace("ht");
//		
//		content4 = new WeBookContent();
//		content4.setType("picture");
//		content4.setUri("1.jpg");
//		content4.setTop(36);
//		content4.setLeft(10);
//		content4.setWidth(60);
//		content4.setHeight(80);
//		
//		WeBookContent content5 = new WeBookContent();
//		content5.setType("text");
//		content5.setText("Kevin的测试微信书，没有什么内容，乱写的行吗？");
//		content5.setSize(12);
//		content5.setTop(22);
//		content5.setLeft(10);
//		content5.setWidth(60);
//		content5.setHeight(12);
//		content5.setFace("ht");
//		
//		WeBookContent content6 = new WeBookContent();
//		content6.setType("picture");
//		content6.setUri("2.jpg");
//		content6.setTop(120);
//		content6.setLeft(10);
//		content6.setWidth(60);
//		content6.setHeight(40);
//		
//		WeBookContent content7 = new WeBookContent();
//		content7.setType("picture");
//		content7.setUri("3.jpg");
//		content7.setTop(164);
//		content7.setLeft(10);
//		content7.setWidth(60);
//		content7.setHeight(40);
//		
//		WeBookContent content8 = new WeBookContent();
//		content8.setType("picture");
//		content8.setUri("4.jpg");
//		content8.setTop(10);
//		content8.setLeft(78);
//		content8.setWidth(60);
//		content8.setHeight(40);
//		
//		WeBookContent content9 = new WeBookContent();
//		content9.setType("text");
//		content9.setText("01/周六");
//		content9.setSize(12);
//		content9.setTop(54);
//		content9.setLeft(78);
//		content9.setWidth(30);
//		content9.setHeight(12);
//		content9.setFace("ht");
//		
//		WeBookContent content10 = new WeBookContent();
//		content10.setType("text");
//		content10.setText("09：11");
//		content10.setSize(12);
//		content10.setTop(54);
//		content10.setLeft(124);
//		content10.setWidth(14);
//		content10.setHeight(12);
//		content10.setFace("ht");
//		
//		WeBookContent content11 = new WeBookContent();
//		content11.setType("picture");
//		content11.setUri("5.jpg");
//		content11.setTop(66);
//		content11.setLeft(78);
//		content11.setWidth(60);
//		content11.setHeight(100);
//		
//		contents.add(content4);
//		contents.add(content3);
//		contents.add(content2);
//		contents.add(content5);
//		contents.add(content6);
//		contents.add(content7);
//		contents.add(content8);
//		contents.add(content9);
//		contents.add(content10);
//		contents.add(content11);
//		page2.setContents(contents);
//		pages.add(page2);
		return pages;
	}

	public static boolean deleteServerFile(String filename){
		boolean ret = false;
		String url = PropertiesManager.getRemoveFileURL()+"/"+filename;
		String result = HttpUtils.getTextResult(url);
		try {
			Map<String,Object> map = getJsonResponse(result);
			if (map.containsKey(PropertiesManager.getSuccessKey())){
				boolean success = (boolean)map.get(PropertiesManager.getSuccessKey());
				if(success){
					ret = true;
				}
			}
		} catch (Exception e) {
			logger.info("error delete server file");
			logger.error(e.getMessage(), e);
		}

		return ret;
	}

	public static boolean renameServerFile(String filename){
		boolean ret = false;
		String url = ComposePath(PropertiesManager.getRenameServerFileURL(), filename);
		String result = HttpUtils.getTextResult(url);
		try {
			Map<String,Object> map = getJsonResponse(result);
			if (map.containsKey(PropertiesManager.getSuccessKey())){
				boolean success = (boolean)map.get(PropertiesManager.getSuccessKey());
				if(success){
					ret = true;
				}
			}
		} catch (Exception e) {
			logger.info("error rename server file");
			logger.error(e.getMessage(), e);
		}

		return ret;
	}




	public static String getOriginalName (String orderid, String workid){
		return orderid+"_"+workid+".zip";
	}

	public static List<WeBookPage> getJsonArrayResponse(String responseBody) throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		List<WeBookPage> rootAsMap = mapper.readValue(responseBody, List.class);
		return rootAsMap;
	}
	
	public static Map<String,Object> getJsonResponse(String responsebody) throws Exception{
		ObjectMapper mapper = new ObjectMapper();
		Map<String,Object> rootAsMap = mapper.readValue(responsebody, Map.class);
		return rootAsMap;
	}

	public static boolean decompress(String srouceZip, String dstFolder) {
		boolean ret = false;
		File file = new File(srouceZip);
		if(file.exists()) {
			InputStream is = null;
			ZipArchiveInputStream zais = null;
			try {
				is = new FileInputStream(file);
				zais = new ZipArchiveInputStream(is);
				ArchiveEntry  archiveEntry = null;
				while((archiveEntry = zais.getNextEntry()) != null) {
					File outputFile = new File(dstFolder, archiveEntry.getName());
					OutputStream os = new FileOutputStream(outputFile);
					IOUtils.copy(zais, os);  
					os.close();    
				}
				ret = true;
			}catch(Exception e) {
				throw new RuntimeException(e);
			}finally {
				if (zais!=null){
					IOUtils.closeQuietly(zais);
				}
				if (is!=null){
					IOUtils.closeQuietly(is);
				}
			}
		}
		return ret;
	}

	public static boolean compress(List<String> filelist, String dstZip) {
		boolean ret = false;
		ZipArchiveOutputStream zaos = null;
		File zipFile = new File(dstZip);
		InputStream is = null;

		try {
			zaos = new ZipArchiveOutputStream(zipFile);
			//Use Zip64 extensions for all entries where they are required
			zaos.setUseZip64(Zip64Mode.AsNeeded);

			for(String f : filelist) {
				File file = new File(f);
				if(file != null&&file.exists()) {
					zaos.putArchiveEntry(new ZipArchiveEntry(file.getName()));
					try {
						is = new FileInputStream(file);
						IOUtils.copy(is, zaos);
					} catch (Exception e) {
						logger.error(e.getMessage(), e);
					} finally {
						if (is!=null){
							IOUtils.closeQuietly(is);
						}
					}
					zaos.closeArchiveEntry();  
				}
			}
			zaos.finish();
			ret = true;
		} catch (IOException e1) {
			logger.info("Error when compressing files to "+dstZip);
			e1.printStackTrace();
		} finally {
			if (zaos!=null){
				IOUtils.closeQuietly(zaos);
			}
		}

		return ret;
	}

	public static long getFileSize (String filepath) {
		long size = 0l;
		try {
			Path path =Paths.get(filepath);
			size = Files.size(path);
		}  catch (IOException e) {
			logger.error(e.getMessage(), e);
		}
		return size;
	}

	public static boolean fileExists(String filepath){
		boolean ret = false;
		Path path;
		path = Paths.get(filepath);
		ret = Files.exists(path);
		return ret;
	}

	public static long getCRC32 (String filepath) {
		Checksum checksum = new CRC32();

		FileInputStream inputStream = null;
		try {
			inputStream = new FileInputStream(filepath);
			FileChannel fileChannel = inputStream.getChannel();
			int len = (int) fileChannel.size();

			MappedByteBuffer buffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, len);
			for (int cnt = 0; cnt < len; cnt++) {
				int i = buffer.get(cnt);
				checksum.update(i);
			}
			fileChannel.close();
		} catch (FileNotFoundException e) {
			logger.error(e.getMessage(), e);
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
		} finally {
			if (inputStream!=null){
				IOUtils.closeQuietly(inputStream);
			}
		}

		return checksum.getValue();
	}

	public static String getOrderIdFromFileName(String filename){
		String orderid = "";
		if (!StringUtils.isEmpty(filename)){
			if (filename.contains("_")){
				orderid =  filename.split("_")[0];
			}
		} 
		return orderid;
	}

	public static String getWorkIdFromFileName(String filename){
		String workid = "";
		if (!StringUtils.isEmpty(filename)){
			if (filename.contains("_")){
				String[] names = filename.split("_");
				if (names.length==2){
					workid =  filename.split("_")[1];
				}else if (names.length==3){
					workid =  names[1] + "_" + names[2];
				}
				//cross out extension
				if (workid.contains(".")){
					workid = workid.substring(0, workid.indexOf("."));
				}
			}
		} 
		return workid;
	}

	public static String generateBarcode(String content, int dwidth, int dheight){

		String finalPath = "";

		String barcodePath = CompositionUtils.ComposePath(PropertiesManager.getBarcodeFolder(), content+".png");

		int codeWidth = 3 + // start guard  
				(7 * 6) + // left bars  
				5 + // middle guard  
				(7 * 6) + // right bars  
				3; // end guard  
		codeWidth = Math.max(codeWidth, dwidth);
		BitMatrix bitMatrix;

		float fontsize = 10f;
		ClassLoader classloader = Thread.currentThread().getContextClassLoader();
		InputStream is = classloader.getResourceAsStream("SourceHanSansCN-Normal.ttf");
		Font font = null;
		int fdCount=5;
		try {
			bitMatrix = new MultiFormatWriter().encode(content, BarcodeFormat.CODE_128, codeWidth*fdCount, (int)(dheight-fontsize)*fdCount, null);

			// generate barcode
			BufferedImage bufferedImage = MatrixToImageWriter.toBufferedImage(bitMatrix);
			// append text at the bottom
			int width = bufferedImage.getWidth();
			int height = (bufferedImage.getHeight()+(int)fontsize*fdCount);
			BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR);
			Graphics2D g2d = img.createGraphics();
			CompositionUtils.setRenderingHint(g2d);
			g2d.fillRect(0, 0, width, height);
			g2d.drawImage(bufferedImage, 0, 0, null);
			g2d.setPaint(Color.BLACK);
			font = Font.createFont( Font.TRUETYPE_FONT,is);
			font=font.deriveFont(Font.PLAIN, fontsize*fdCount);
			g2d.setFont(font);
			FontMetrics fm = g2d.getFontMetrics(font);
			int x = (img.getWidth() - fm.stringWidth(content))/2;
			int y = height-2*fdCount;
			g2d.drawString(content, x, y);
			g2d.dispose();
			ImageIO.write(img, "png",  new File(barcodePath));

			finalPath=barcodePath;
		} catch (WriterException e) {
			logger.info("Error occured while generating barcode.");
			logger.error(e.getMessage(), e);
		} catch (IOException e) {
			logger.info("Error occured while writing barcode to file "+barcodePath);
			logger.error(e.getMessage(), e);
		} catch (FontFormatException e) {
			logger.error(e.getMessage(), e);
		}  

		return finalPath;
	}

	public static String ComposePath (String folder, String file){
		if (StringUtils.isEmpty(folder)||StringUtils.isEmpty(file)){
			return "";
		} else {
			if (!folder.endsWith("/")){
				return folder+"/"+file;
			} else {
				return folder+file;
			}
		}
	}

	public static String ComposeFolderPath (String parentFolder, String subFolder){
		if (StringUtils.isEmpty(parentFolder)||StringUtils.isEmpty(subFolder)){
			return "";
		} else {
			String folderPath = "";
			if (!parentFolder.endsWith("/")){
				folderPath = parentFolder+"/"+subFolder+"/";
			}  else {
				folderPath = parentFolder+"/"+subFolder;
			}
			if (!StringUtils.isEmpty(folderPath)){
				File filedirectory = new File(folderPath);
				if(!filedirectory.exists()) {
					filedirectory.mkdirs();
				}
			}
			return folderPath;
		}
	}

	public static void clearTempFiles(String orderid, String workid) {
		logger.info("deleting temp file for work "+workid);
		deleteOriginalZip(orderid, workid);
		deleteUnZipFiles(orderid, workid);
		deletePDFs(orderid, workid);
		deleteCompositedWorkZip(orderid, workid);
		deleteOrderDetailPDF(orderid, workid);
		deleteCompositedOrderZip(orderid, workid);
		deleteBarcode(orderid, workid);
		logger.info("delete temp file for work "+workid+" done");
	}

	private static void deleteOriginalZip(String orderid, String workid){
		String basename = composeBaseFileName(orderid, workid);
		if (!StringUtils.isEmpty(basename)){
			String filename = ComposePath(PropertiesManager.getZipFolderPath(), new StringBuilder(basename).append(".zip").toString());
			deleteFilebyName(filename);
		}
	}

	private static void deleteUnZipFiles(String orderid, String workid){
		String basename = composeBaseFileName(orderid, workid);
		if (!StringUtils.isEmpty(basename)){
			String filename = ComposeFolderPath(PropertiesManager.getUnzipPath(), new StringBuilder(basename).toString());
			deleteFolderbyName(filename);
		}
	}

	private static void deletePDFs(String orderid, String workid){
		String basename = composeBaseFileName(orderid, workid);
		if (!StringUtils.isEmpty(basename)){
			String filename = ComposeFolderPath(PropertiesManager.getPDFoutputFolder(), workid);
			deleteFolderbyName(filename);
		}
	}

	private static void deleteCompositedWorkZip(String orderid, String workid){
		if (!StringUtils.isEmpty(workid)){
			String filename = ComposePath(PropertiesManager.getFinalWorkZipPath(), new StringBuilder(workid).append("_composited.zip").toString());
			deleteFilebyName(filename);
		}
	}

	private static void deleteOrderDetailPDF(String orderid, String workid){
		String basename = composeBaseFileName(orderid, workid);
		if (!StringUtils.isEmpty(basename)){
			String filename = ComposePath(PropertiesManager.getOrderDetailPDFPath(), new StringBuilder(orderid).append(".pdf").toString());
			deleteFilebyName(filename);
		}
	}

	private static void deleteCompositedOrderZip(String orderid, String workid){
		if (!StringUtils.isEmpty(orderid)){
			String filename = ComposePath(PropertiesManager.getFinalOrderZipPath(), new StringBuilder(orderid).append(".zip").toString());
			deleteFilebyName(filename);
		}
	}

	private static void deleteBarcode(String orderid, String workid){
		if (!StringUtils.isEmpty(orderid)){
			String filename = ComposePath(PropertiesManager.getBarcodeFolder(), new StringBuilder(workid).append(".png").toString());
			deleteFilebyName(filename);
		}
	}

	public static String composeBaseFileName(String orderid, String workid){
		StringBuilder builder = new StringBuilder();
		if (!StringUtils.isEmpty(orderid)&&!StringUtils.isEmpty(workid)){
			builder.append(orderid).append("_").append(workid);
		}
		return builder.toString();
	}

	public static void deleteFolderbyName(String filename){
		Path path = Paths.get(filename);
		try {
			Files.walkFileTree(path, new FileVisitor<Path>() {

				@Override
				public FileVisitResult postVisitDirectory(Path dir, IOException exc)
						throws IOException {
//					logger.info("deleting directory :"+ dir);
					Files.delete(dir);
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult preVisitDirectory(Path dir,
						BasicFileAttributes attrs) throws IOException {
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult visitFile(Path file,
						BasicFileAttributes attrs) throws IOException {
//					logger.info("Deleting file: "+file);
					Files.delete(file);
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult visitFileFailed(Path file, IOException exc)
						throws IOException {
					return FileVisitResult.CONTINUE;
				}
			});
		} catch (IOException e1) {
			logger.info("delete temp file "+path+" failed.");
			logger.error(e1.getMessage(), e1);
		}

	}

	public static void deleteFilebyName(String filename){
		Path path = Paths.get(filename);
		if (Files.exists(path)){
			try {
				Files.delete(path);
			} catch (IOException e) {
				logger.info("delete temp file "+filename+" failed.");
				logger.error(e.getMessage(), e);
			}
		}
	}

	public static float getPointValueFromMM(float mm){
		//		double ret = mm * 2.8346* PropertiesManager.getDPI() / 72; 
		double ret = mm * 2.8347; 
		return (float)ret;
	}

	public static void setRenderingHint(Graphics2D graphics){
		if("on".equalsIgnoreCase(PropertiesManager.getRenderingHint())) {
			graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		}
	}
	
	public static void prepareTempDirectories(){
		createDirectory(PropertiesManager.getZipFolderPath());
		createDirectory(PropertiesManager.getUnzipPath());
		createDirectory(PropertiesManager.getPDFoutputFolder());
		createDirectory(PropertiesManager.getBarcodeFolder());
		createDirectory(PropertiesManager.getOrderDetailPDFPath());
		createDirectory(PropertiesManager.getFinalWorkZipPath());
		createDirectory(PropertiesManager.getFinalOrderZipPath());
	}
	
	public static void createDirectory (String dicPath){
		if (StringUtils.isEmpty(dicPath)){
			logger.info("Invalid directory configuration");
		}
		Path path = Paths.get(dicPath);
		if (!Files.exists(path)){
			try {
				Files.createDirectories(path);
			} catch (IOException e) {
				logger.info("create temp folder "+dicPath+" failed.");
				logger.error(e.getMessage(), e);
			}
		}
	}
	
	public static boolean isEmpty(Collection collection){
		boolean ret = false;
		if (collection==null||collection.isEmpty()){
			ret = true;
		}
		return ret;
	}
	
	public static String getFaceImg(String code) {
		String url = null;
		String tag = null;
		//判断code的类型
		
		if (CompositionWorker.isQQFace(code)){
			//读取qq表情库
			tag = "1";
		}else{
			//读取emoji表情库
			tag = "2";
		}
		 url = HttpUtils.getTextResult(PropertiesManager.getFaceUrl()+code+"/"+tag+"/");
		
		return url;
	}
	
}
