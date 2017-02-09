package com.jihox.composition.worker;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.ImagingOpException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

import sun.font.FontDesignMetrics;

import org.apache.commons.compress.utils.IOUtils;
import org.apache.log4j.Logger;
import org.apache.pdfbox.exceptions.COSVisitorException;
import org.apache.pdfbox.io.RandomAccessFile;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.edit.PDPageContentStream;
import org.apache.pdfbox.pdmodel.graphics.xobject.PDCcitt;
import org.apache.pdfbox.pdmodel.graphics.xobject.PDJpeg;
import org.apache.pdfbox.pdmodel.graphics.xobject.PDPixelMap;
import org.apache.pdfbox.pdmodel.graphics.xobject.PDXObjectImage;

import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfWriter;
import com.jihox.composition.common.BaseWorker;
import com.jihox.composition.manager.PropertiesManager;
import com.jihox.composition.model.book.org.example.photobook.WeBook;
import com.jihox.composition.model.book.org.example.photobook.WeBookContent;
import com.jihox.composition.model.book.org.example.photobook.WeBookPage;
import com.jihox.composition.queue.UploadTaskQueue;
import com.jihox.composition.task.CompositionTask;
import com.jihox.composition.task.UploadTask;
import com.jihox.composition.utils.CompositionUtils;
import com.jihox.composition.utils.Constants;
import com.jihox.composition.utils.HttpUtils;


public class CompositionWorker extends BaseWorker {

	private static Logger logger=Logger.getLogger(CompositionWorker.class);
	@Override
	protected void dobusiness() {

		CompositionTask task = (CompositionTask)this.task;

		logger.info("CompositionWorker "+getWorkerName()+" start "+task.getDescription());

		String originalZip = CompositionUtils.ComposePath(PropertiesManager.getZipFolderPath(), task.getName());

		String decompresspath = CompositionUtils.ComposeFolderPath(PropertiesManager.getUnzipPath(), task.getId());

		boolean unzipsuccess = CompositionUtils.decompress(originalZip, decompresspath);
		if (!unzipsuccess){
			logger.info("CompositionWorker "+getWorkerName()+" decompress error for"+task.getName());
			renewTask();
			return;
		}
		//String DescriptorPath = CompositionUtils.ComposePath(decompresspath , task.getId()+".phb");

		WeBook book = CompositionUtils.getWeBookInfo(task.getWorkid());
//		List<WeBookPage> pages = CompositionUtils.getPages(task.getWorkid());
		List<WeBookPage> pages = book.getPages();
		task.setPages(pages);

		String workPdfPath = CompositionUtils.ComposeFolderPath(PropertiesManager.getPDFoutputFolder(), task.getWorkid());

		if (pages==null){
			logger.info("CompositionWorker "+getWorkerName()+" parse descriptor error for "+task.getWorkid());
			renewTask();
			return;
		} else {
			boolean printPdfResultSuccess = printProjectToPDF(book,workPdfPath);
			if (!printPdfResultSuccess){
				logger.info("Print pdf failed for: "+task.getWorkid());
				return;
			}
		}

		String orderpdfPath = CompositionUtils.ComposePath(PropertiesManager.getOrderDetailPDFPath(), task.getOrderid()+".pdf");

		boolean orderdetailsuccess = printTextOrderToPDF(task.getOrderid(), orderpdfPath);
		if (!orderdetailsuccess){
			logger.info("CompositionWorker "+getWorkerName()+" print order detail error for"+task.getOrderid());
			renewTask();
			return;
		}

//		String finalWorkZipPath = CompositionUtils.ComposePath(PropertiesManager.getFinalWorkZipPath(), task.getWorkid()+"_composited.zip");

		File folder = new File(workPdfPath);
		if (!folder.exists()||!folder.isDirectory()){
			logger.info("CompositionWorker "+getWorkerName()+" print order detail error for"+task.getOrderid());
			renewTask();
			return;
		}

//		CompositionUtils.compress(workfilelist, finalWorkZipPath);

		String finalOrderZipPath = CompositionUtils.ComposePath(PropertiesManager.getFinalOrderZipPath(), task.getOrderid()+".zip");

		List<String> orderfilelist = new ArrayList<String>();
		orderfilelist.add(orderpdfPath);
		File[] files = folder.listFiles();
		for (File f : files){
			orderfilelist.add(f.getAbsolutePath());
		}

		CompositionUtils.compress(orderfilelist,finalOrderZipPath);

		task.setResultValue(finalOrderZipPath);

		UploadTask nextTask = new UploadTask();
		nextTask.setKeyvalue(task.getResultValue());
		nextTask.setCheckValue(task.getCheckValue());
		nextTask.setName(task.getName());
		nextTask.setDescription("Upload file "+task.getName());
		nextTask.setOrderid(task.getOrderid());
		nextTask.setWorkid(task.getWorkid());
		UploadTaskQueue.getCurrentQueue().addTask(nextTask);

		logger.info("CompositionWorker "+getWorkerName()+" end "+task.getDescription());
	}

	private Map<String,Object> getOrderDetail(String orderid){
		Map<String,Object> content = null;
		String url = PropertiesManager.getOrderDetailUrl()+"/"+orderid;
		String plaintextContent = HttpUtils.getTextResult(url);
		try {
			content = CompositionUtils.getJsonResponse(plaintextContent);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		return content;
	}

	private boolean printProjectToPDF(WeBook book, String targetPDF){
		boolean ret = false;
	
		List<WeBookPage> pages = book.getPages();
		
		if (pages==null||pages.isEmpty()){
			logger.info("Can not find page object for "+task.getOrderid());
			return false;
		}
		List<WeBookPage> covers = new ArrayList<WeBookPage>();
		List<WeBookPage> leaves = new ArrayList<WeBookPage>();

		covers = getCovers(pages);
		leaves = getLeaves(pages);
		try {
			String coverspdfpath = CompositionUtils.ComposePath(CompositionUtils.ComposeFolderPath(PropertiesManager.getPDFoutputFolder(), task.getWorkid()), task.getWorkid()+"_cover.pdf");
			if (covers.size()>0){
				printBackCover(covers, coverspdfpath, book.getThickness());
			}
			String leavespdfpath = CompositionUtils.ComposePath(CompositionUtils.ComposeFolderPath(PropertiesManager.getPDFoutputFolder(), task.getWorkid()), task.getWorkid()+".pdf");	
			if (leaves.size()>0){
				printLeavesToPDF(leaves, leavespdfpath);
			}
			ret = true;
		} catch (Exception e){
			logger.info("Print to pdf error.");
			logger.error(e.getMessage(), e);
		}
		
		return ret;
	}

	public List<WeBookPage> getCovers(List<WeBookPage> allPages) {
		List<WeBookPage> covers = new ArrayList<WeBookPage>();
		for (int i=0;i<allPages.size();i++){
			WeBookPage p = allPages.get(i);
			if (p.getType()!=null && p.getType().contains("cover")){
				covers.add(p);
			}
		}
		return covers;
	}

	public List<WeBookPage> getLeaves(List<WeBookPage> allPages) {
		List<WeBookPage> leaves = new ArrayList<WeBookPage>();
		for (int i=0;i<allPages.size();i++){
			WeBookPage p = allPages.get(i);
			if (p.getType()==null || p.getType().isEmpty() || !p.getType().contains("cover")){
				leaves.add(p);
			}
		}
		return leaves;
	}


	private void printLeavesToPDF (List<WeBookPage> leaves, String targetPDF) throws IllegalArgumentException, ImagingOpException, IOException, COSVisitorException {
		if (leaves.isEmpty()){
			logger.info("No leaf defined to be printed to pdf");
			return;
		}
		
		try {
			int pageCount = 0;
			int times = 0;
			for (WeBookPage p : leaves){
				PDDocument document = null;

				if (times == 0) {
					document = new PDDocument();
					Date date = new Date();
					System.out.println(date.toString());
				} else {
					document = PDDocument.load(targetPDF);
				}
				
				times ++;
				
//				if (times >= 5) {
//					break;
//				}

				if (p==null){
					continue;
				}

				printPageToPDF(p, document, ++pageCount);
				
				document.save(targetPDF);
				document.close();
			}

		} catch(Exception e) {
			logger.error(e.getMessage(), e);
		} finally{
			Date date = new Date();
			System.out.println(date.toString());
		}
	}
	
	private Color String2Color(String str) {  
        int i = Integer.parseInt(str.substring(1), 16);  
        return new Color(i);  
    }  
	
	private void printTextToPDF(WeBookContent content,Font font,PDDocument document,float leavesBleedingWidth,PDPageContentStream contentStream,PDPage page) throws IOException{
		
		float fontsize = content.getSize();
		 
		//actually draw the caption to image and append to pdf
		int width = new Float(CompositionUtils.getPointValueFromMM(content.getWidth())).intValue() ;
		int height = new Float(CompositionUtils.getPointValueFromMM(fontsize)).intValue();

		String text = content.getText();
		
		boolean printCompleted = true;

		BufferedImage image = new BufferedImage((int)PropertiesManager.getScaleFactorByDPI()*width,
				(int)PropertiesManager.getScaleFactorByDPI()*height, BufferedImage.TYPE_4BYTE_ABGR);
		Graphics2D graphics = image.createGraphics();

		fontsize = fontsize * PropertiesManager.getScaleFactorByDPI();

		font=font.deriveFont(Font.PLAIN, fontsize);
		if (content.getColor()==null || content.getColor().isEmpty()){
			graphics.setColor(Color.BLACK);
		}else{
			graphics.setColor(String2Color(content.getColor()));
		}
		
		if (content.getBackgroundcolor()==null || content.getBackgroundcolor().isEmpty()){

		}else{
			Color color = String2Color(content.getBackgroundcolor());
			
			for (int i = image.getMinX(); i < image.getWidth(); i ++) {
				for (int j = image.getMinY(); j < image.getHeight(); j ++) {
					image.setRGB(i, j, color.getRGB());
				}
			}
		}
		
		graphics.setFont(font);
		CompositionUtils.setRenderingHint(graphics);
		
		float contentWidth = PropertiesManager.getScaleFactorByDPI()*CompositionUtils.getPointValueFromMM(content.getWidth());
		double textWidth = graphics.getFontMetrics().getStringBounds(content.getText(), graphics).getWidth();
		if (contentWidth<textWidth){
			printCompleted = false;
			int textLen = text.length();
			for(int i=1;i<=textLen;i++){
				text = text.substring(0,textLen -i);
				textWidth = graphics.getFontMetrics().getStringBounds(text, graphics).getWidth();
				if (contentWidth>=textWidth){
					break;
				}
			}
		}
		
		//对状态中表情做处理
		//对表情字符串进行分割替换处理
		Font defaultFont = new Font("SourceHanSansCN-Normal", Font.PLAIN, (int)(content.getSize() * 1));
		FontMetrics fm = sun.font.FontDesignMetrics.getMetrics(defaultFont);
		int base = fm.charWidth('我');
		String temp = text;
		int horizontalOffset = 0;
		String url = null;
		//字符串的分割处理
		Map emojiFaceInfo = emojiAddress(temp);
		Map qqFaceInfo = qqfaceAddress(temp);
		List emojiList = (List) emojiFaceInfo.get("content");
		List qqList = (List) qqFaceInfo.get("content");
		if(emojiList.size() != 0){
			//emoji表情的处理
			int insert = 0;//用于切割表情字符串
			float offset = 0;
			
			for(Object face : emojiList){
				url = CompositionUtils.getFaceImg((String)face);//获取表情的图片url
				float holderwidth = 4/3*base;
				float holderheight = 4/3*base;
				//字符串分割算法
				String fra = text.substring(0,text.indexOf((String)face));
				//对切割的第一部分进行组版
				//组版信息的重新获取
				//fra text打入pdf
				
				graphics.drawString(fra, getHorizontalOffsetByAlign(fra,horizontalOffset,graphics), getVerticalOffsetBydocAlign(content, graphics) + 10);
				PDXObjectImage ximage = new PDPixelMap(document, image);
				
				image = null;
				
				float x = CompositionUtils.getPointValueFromMM(content.getLeft()+horizontalOffset)+leavesBleedingWidth;
				float y = page.getMediaBox().getHeight()-CompositionUtils.getPointValueFromMM(content.getTop())-height-leavesBleedingWidth;
				contentStream.drawXObject(ximage, x, y, width, height);
				
				ximage = null;
				System.gc();
				
				if("".equals(fra)){
					horizontalOffset += (fra.length()*base)+holderwidth;
				}else{
					horizontalOffset += 0 + holderwidth;
				}
				
				//表情图片打入pdf
				File file = new File(url);
				if (!file.exists()) {
					logger.info("No image found for "+url);
					continue;
				}
				PDXObjectImage ximage1 = null;
				if( url.toLowerCase().endsWith(".jpg")) {
					BufferedImage awtImage = ImageIO.read(file);

					ximage1 = new PDJpeg(document, awtImage);
			
					awtImage = null;
				}
				else if (url.toLowerCase().endsWith(".tif") || url.toLowerCase().endsWith(".tiff")) {
					ximage1 = new PDCcitt(document, new RandomAccessFile(file,"r"));
				}
				else {
					BufferedImage awtImage = ImageIO.read(file);
					ximage1 = new PDPixelMap(document, awtImage);
			
					awtImage = null;
				}
				
				CompositionUtils.setRenderingHint(ximage1.getRGBImage().createGraphics());
				offset = horizontalOffset - holderwidth;
				float horizontaloffset = content.getLeft() + offset;
				horizontaloffset = CompositionUtils.getPointValueFromMM(horizontaloffset)+leavesBleedingWidth;
//				float verticaloffset = page.geth-content.getHeight()-content.getTop();
//				verticaloffset = CompositionUtils.getPointValueFromMM(verticaloffset)+leavesBleedingWidth;
				contentStream.drawXObject(ximage1, horizontaloffset, y, holderwidth, holderheight);
				
				ximage1 = null;
				
				System.gc();
				
				//初始切割段的重新生成
				insert = ((String)face).length();
				text = text.substring(text.indexOf((String)face) + insert);
			}
			//最后一个text fra打入pdf
			
			graphics.drawString(text, getHorizontalOffsetByAlign(text,horizontalOffset,graphics), getVerticalOffsetBydocAlign(content, graphics) + 10);
			PDXObjectImage ximage = new PDPixelMap(document, image);
			
			image = null;
			
			float x = CompositionUtils.getPointValueFromMM(content.getLeft()+horizontalOffset)+leavesBleedingWidth;
			float y = page.getMediaBox().getHeight()-CompositionUtils.getPointValueFromMM(content.getTop())-height-leavesBleedingWidth;
			contentStream.drawXObject(ximage, x, y, width, height);
			
			ximage = null;
			System.gc();
		}else if(qqList.size() != 0){
			//qq表情的处理
			int insert = 0;//用于切割表情字符串
			float offset = 0;
			for(Object face : qqList){
				url = CompositionUtils.getFaceImg((String)face);//获取表情的图片url
				float holderwidth = base;
				float holderheight = base;
				//字符串分割算法
				String fra = text.substring(0,text.indexOf((String)face));
				//对切割的第一部分进行组版
				//组版信息的重新获取
				//fra text打入pdf
				
				graphics.drawString(fra, getHorizontalOffsetByAlign(fra,horizontalOffset,graphics), getVerticalOffsetBydocAlign(content, graphics) + 10);
				PDXObjectImage ximage = new PDPixelMap(document, image);
				
				image = null;
				
				float x = CompositionUtils.getPointValueFromMM(content.getLeft()+horizontalOffset)+leavesBleedingWidth;
				float y = page.getMediaBox().getHeight()-CompositionUtils.getPointValueFromMM(content.getTop())-height-leavesBleedingWidth;
				contentStream.drawXObject(ximage, x, y, width, height);
				
				ximage = null;
				System.gc();
				
				if("".equals(fra)){
					horizontalOffset += (fra.length()*base)+holderwidth;
				}else{
					horizontalOffset += 0 + holderwidth;
				}
				//表情图片打入pdf
				File file = new File(url);
				if (!file.exists()) {
					logger.info("No image found for "+url);
					continue;
				}
				PDXObjectImage ximage1 = null;
				if( url.toLowerCase().endsWith(".jpg")) {
					BufferedImage awtImage = ImageIO.read(file);

					ximage1 = new PDJpeg(document, awtImage);
			
					awtImage = null;
				}
				else if (url.toLowerCase().endsWith(".tif") || url.toLowerCase().endsWith(".tiff")) {
					ximage1 = new PDCcitt(document, new RandomAccessFile(file,"r"));
				}
				else {
					BufferedImage awtImage = ImageIO.read(file);
					ximage1 = new PDPixelMap(document, awtImage);
			
					awtImage = null;
				}
				
				CompositionUtils.setRenderingHint(ximage1.getRGBImage().createGraphics());
				offset =horizontalOffset -  holderwidth;
				float horizontaloffset = content.getLeft() + offset;
				horizontaloffset = CompositionUtils.getPointValueFromMM(horizontaloffset)+leavesBleedingWidth;
//				float verticaloffset = page.geth-content.getHeight()-content.getTop();
//				verticaloffset = CompositionUtils.getPointValueFromMM(verticaloffset)+leavesBleedingWidth;
				contentStream.drawXObject(ximage1, horizontaloffset, y, holderwidth, holderheight);
				
				ximage1 = null;
				
				System.gc();
				
				//初始切割段的重新生成
				insert = ((String)face).length();
				text = text.substring(text.indexOf((String)face) + insert);
			}
			//最后一个text fra打入pdf
			
			graphics.drawString(text, getHorizontalOffsetByAlign(text,horizontalOffset,graphics), getVerticalOffsetBydocAlign(content, graphics) + 10);
			PDXObjectImage ximage = new PDPixelMap(document, image);
			
			image = null;
			
			float x = CompositionUtils.getPointValueFromMM(content.getLeft()+horizontalOffset)+leavesBleedingWidth;
			float y = page.getMediaBox().getHeight()-CompositionUtils.getPointValueFromMM(content.getTop())-height-leavesBleedingWidth;
			contentStream.drawXObject(ximage, x, y, width, height);
			
			ximage = null;
			System.gc();
		}else{
			graphics.drawString(text, getHorizontalOffsetBydocAlign(content, graphics), getVerticalOffsetBydocAlign(content, graphics) + 10);
			PDXObjectImage ximage = new PDPixelMap(document, image);
			
			image = null;
			
			float x = CompositionUtils.getPointValueFromMM(content.getLeft())+leavesBleedingWidth;
			float y = page.getMediaBox().getHeight()-CompositionUtils.getPointValueFromMM(content.getTop())-height-leavesBleedingWidth;
			contentStream.drawXObject(ximage, x, y, width, height);
			
			ximage = null;
			System.gc();
			
			if (!printCompleted){
				content.setText(content.getText().substring(text.length()));
				
				content.setTop(content.getTop()+ (int)(content.getSize()*0.35)+content.getLinespacing());
				printTextToPDF(content,font,document,leavesBleedingWidth,contentStream,page);
			}
		}
	}

	private void printPageToPDF(WeBookPage p, PDDocument document, int pageNumber) throws IllegalArgumentException, ImagingOpException, IOException{
		String decompresspath = CompositionUtils.ComposeFolderPath(PropertiesManager.getUnzipPath(), task.getId());
		PDPage page = new PDPage();

		float leavesBleedingWidth = CompositionUtils.getPointValueFromMM(PropertiesManager.getLeavesBleedingMMWidth());

		leavesBleedingWidth+=PropertiesManager.getPixelDescriptionTextHeight();
		float pagewidth = CompositionUtils.getPointValueFromMM(p.getWidth())+2*leavesBleedingWidth;
		float pageheight = CompositionUtils.getPointValueFromMM(p.getHeight())+2*leavesBleedingWidth;
		PDRectangle mediaBox = new PDRectangle(pagewidth, pageheight); 
		page.setMediaBox(mediaBox);

		PDPageContentStream contentStream = null;

		try{
			contentStream = new PDPageContentStream(document, page, true, true);
			for (WeBookContent content : p.getContents()){

				if (content.getType().equals("text")){
					ClassLoader classloader = Thread.currentThread().getContextClassLoader();
					InputStream is = classloader.getResourceAsStream("SourceHanSansCN-Normal.ttf");
					Font font = null;

					font = Font.createFont( Font.TRUETYPE_FONT,is);
					float fontsize = content.getSize();
					int textPerRow = (int) (content.getWidth()/fontsize);
					
					printTextToPDF(content,font,document,leavesBleedingWidth,contentStream,page);
					
				} else if (content.getType().equals("day") || content.getType().equals("time")) {
					ClassLoader classloader = Thread.currentThread().getContextClassLoader();
					InputStream is = classloader.getResourceAsStream("SourceHanSansCN-Normal.ttf");
					Font font = null;

					font = Font.createFont( Font.TRUETYPE_FONT,is);
					float fontsize = content.getSize();
					int textPerRow = (int) (content.getWidth()/fontsize);
					
					printTextToPDF(content,font,document,leavesBleedingWidth,contentStream,page);
				} else {
					float holderwidth = CompositionUtils.getPointValueFromMM(content.getWidth());
					float holderheight = CompositionUtils.getPointValueFromMM(content.getHeight());

					// get image from configured path and file name retrieved from project file
					String imagepath = "";
					if (content.getType().equals("picture")){
						imagepath = decompresspath + content.getUri();
					}else if (content.getType().equals("decorate") || content.getType().equals("background")){
						imagepath = CompositionUtils.ComposePath(PropertiesManager.getMaterialPath(), content.getUri());
					}
					
					//replace code128 to real barcode
					if (imagepath.indexOf("code128")!=-1){
						imagepath = CompositionUtils.generateBarcode(this.task.getWorkid(), (int)holderwidth, (int)holderheight);
					}

					File file = new File(imagepath);
					if (!file.exists()) {
						logger.info("No image found for "+imagepath);
						continue;
					}
					PDXObjectImage ximage = null;
					if( imagepath.toLowerCase().endsWith(".jpg")) {
						BufferedImage awtImage = ImageIO.read(file);

						ximage = new PDJpeg(document, awtImage);
				
						awtImage = null;
					}
					else if (imagepath.toLowerCase().endsWith(".tif") || imagepath.toLowerCase().endsWith(".tiff")) {
						ximage = new PDCcitt(document, new RandomAccessFile(file,"r"));
					}
					else {
						BufferedImage awtImage = ImageIO.read(file);
						ximage = new PDPixelMap(document, awtImage);
				
						awtImage = null;
					}
					
					CompositionUtils.setRenderingHint(ximage.getRGBImage().createGraphics());
					float horizontaloffset = content.getLeft();
					horizontaloffset = CompositionUtils.getPointValueFromMM(horizontaloffset)+leavesBleedingWidth;
					float verticaloffset = p.getHeight()-content.getHeight()-content.getTop();
					verticaloffset = CompositionUtils.getPointValueFromMM(verticaloffset)+leavesBleedingWidth;
					contentStream.drawXObject(ximage, horizontaloffset, verticaloffset, holderwidth, holderheight);
					
					ximage = null;
					
					System.gc();
				}
				
			}

			drawLeavesBleedingLine((int)PropertiesManager.getLeavesBleedingMMWidth(), contentStream, mediaBox);
			CompositionTask task = (CompositionTask)this.task;
			List<WeBookPage> pages = task.getPages();
			int totalpage = 0;
			if (pages!=null){
				List<WeBookPage> leaves = getLeaves(pages);
				for(WeBookPage pageObj : leaves){
					if (pageObj==null||pageObj.getContents()==null){
						continue;
					}
					totalpage++;
				}
			}
			String pageDescription = generatePageDescription(Constants.LEAF_TYPE, contentStream, pageNumber, totalpage);
			float offset = CompositionUtils.getPointValueFromMM(PropertiesManager.getLeavesBleedingMMWidth()+PropertiesManager.getBleedingLineLength());
			appendPageDescription(pageDescription, offset, contentStream, mediaBox ,document);
			contentStream.close();
			
			contentStream = null;
			System.gc();
			document.addPage(page);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		} 
	}

	private void printBackCover(List<WeBookPage> covers, String targetPDF, float bookThickness) {
		float pagewidth = 0f;
		float pageheight = 0f;

		Map<WeBookContent, Float> contentOffsetMapper = new LinkedHashMap<WeBookContent, Float>();

		Collections.reverse(covers);

//		int spineWidth = (int)PropertiesManager.getSpineMMWidth();

		float spineWidth =  bookThickness;
		
		float flapWidth = 0;
		int i = 0;

		boolean spineAdded = false;

		// cover pdf is composed into one page, hereby calculate the new size and the element new horizontal offset
		for (WeBookPage page : covers){
			if (i == 0) {
				flapWidth = page.getFlapwidth();
				pagewidth += page.getFlapwidth();
			}
			
			for(WeBookContent content: page.getContents()){
				contentOffsetMapper.put(content, pagewidth+content.getLeft());
			}
			if (!spineAdded){
				pagewidth += spineWidth;
				spineAdded = true;
			}
			
			pagewidth += page.getWidth();
			
			if (i ++ == covers.size() - 1) {
				pagewidth += flapWidth;
			}

			if (page.getHeight()>pageheight){
				pageheight = page.getHeight();
			}
		}

		float coversBleedingWidth =CompositionUtils.getPointValueFromMM(PropertiesManager.getCoverBleedingMMWidth());
		pagewidth = CompositionUtils.getPointValueFromMM(pagewidth)+2*coversBleedingWidth;
		pageheight = CompositionUtils.getPointValueFromMM(pageheight)+2*coversBleedingWidth;

		PDPage page = null;
		PDDocument document = null;
		PDPageContentStream contentStream = null;
		try {
			page = new PDPage();
			PDRectangle mediaBox = new PDRectangle(pagewidth, pageheight); 
			page.setMediaBox(mediaBox);

			document = new PDDocument();
			contentStream = new PDPageContentStream(document, page, true, true);
			String decompresspath = CompositionUtils.ComposeFolderPath(PropertiesManager.getUnzipPath(), task.getId());

			// add pictures
			for (WeBookContent content: contentOffsetMapper.keySet()){
//				if (pagepicture.getImage()==null){
//					logger.info("No image info found in project file.");
//					continue;
//				}
//				PictureHolder holder = pagepicture.getPictureHolder();

				

				String imagepath = "";
				// get image from configured path and file name retrieved from project file
				if (content.getType().equals("text")){
					//actually draw the caption to image and append to pdf
					int width = new Float(CompositionUtils.getPointValueFromMM(content.getWidth())).intValue() ;
					int height = new Float(CompositionUtils.getPointValueFromMM(content.getHeight())).intValue();
					BufferedImage image = new BufferedImage((int)PropertiesManager.getScaleFactorByDPI()*width,
							(int)PropertiesManager.getScaleFactorByDPI()*height, BufferedImage.TYPE_4BYTE_ABGR);

					Graphics2D graphics = image.createGraphics();

					float fontsize = content.getSize();
					fontsize = fontsize * PropertiesManager.getScaleFactorByDPI();

					ClassLoader classloader = Thread.currentThread().getContextClassLoader();
					InputStream is = classloader.getResourceAsStream("SourceHanSansCN-Normal.ttf");
					Font font = null;

					font = Font.createFont( Font.TRUETYPE_FONT,is);
					font=font.deriveFont(Font.PLAIN, fontsize);
					if (content.getColor()==null || content.getColor().isEmpty()){
						graphics.setColor(Color.BLACK);
					}else{
						graphics.setColor(String2Color(content.getColor()));
					}
					graphics.setFont(font);
					CompositionUtils.setRenderingHint(graphics);
					graphics.drawString(content.getText(), getHorizontalOffsetBydocAlign(content, graphics), getVerticalOffsetBydocAlign(content, graphics) * 2/3);

					PDXObjectImage ximage = new PDPixelMap(document, image);

					float x = CompositionUtils.getPointValueFromMM(contentOffsetMapper.get(content))+coversBleedingWidth;
					float y = page.getMediaBox().getHeight()-CompositionUtils.getPointValueFromMM(content.getTop())-height-coversBleedingWidth;

					contentStream.drawXObject(ximage, x, y, width, height);
				}else{
					float holderwidth = CompositionUtils.getPointValueFromMM(content.getWidth());
					float holderheight = CompositionUtils.getPointValueFromMM(content.getHeight());
					if (content.getType().equals("picture")){
						imagepath = decompresspath + content.getUri();
					}else if (content.getType().equals("decorate") || content.getType().equals("background")){
						imagepath = CompositionUtils.ComposePath(PropertiesManager.getMaterialPath(), content.getUri());
					}
					
					//replace code128 to real barcode
					if (imagepath.indexOf("code128")!=-1){
						imagepath = CompositionUtils.generateBarcode(this.task.getWorkid(), (int)holderwidth, (int)holderheight);
					}

					File file = new File(imagepath);
					if (!file.exists()) {
						logger.info("No image found for "+imagepath);
						continue;
					}
					PDXObjectImage ximage = null;
					if( imagepath.toLowerCase().endsWith(".jpg")) {
						BufferedImage awtImage = ImageIO.read(file);
						ximage = new PDJpeg(document, awtImage);
					}
					else if (imagepath.toLowerCase().endsWith(".tif") || imagepath.toLowerCase().endsWith(".tiff")) {
						ximage = new PDCcitt(document, new RandomAccessFile(file,"r"));
					}
					else {
						BufferedImage awtImage = ImageIO.read(file);
						ximage = new PDPixelMap(document, awtImage);
					}
					CompositionUtils.setRenderingHint(ximage.getRGBImage().createGraphics());
					float horizontaloffset = contentOffsetMapper.get(content);
					horizontaloffset = CompositionUtils.getPointValueFromMM(horizontaloffset)+coversBleedingWidth;
					float verticaloffset = content.getTop();
					verticaloffset = pageheight - CompositionUtils.getPointValueFromMM(verticaloffset)-holderheight-coversBleedingWidth;
					contentStream.drawXObject(ximage, horizontaloffset, verticaloffset, holderwidth, holderheight);
				}
				
			}

			drawCoversBleedingLine((int)PropertiesManager.getCoverBleedingMMWidth(), contentStream, mediaBox);
			drawSpineLine(spineWidth, contentStream, mediaBox);
			drawFlapLine(flapWidth, contentStream, mediaBox);
			String pageDescription = generatePageDescription(Constants.COVER_TYPE, contentStream, 1, 1);
			float offset = CompositionUtils.getPointValueFromMM(PropertiesManager.getCoverBleedingMMWidth());
			appendPageDescription(pageDescription, offset + flapWidth, contentStream, mediaBox ,document);
			contentStream.close();
			document.addPage(page);
			document.save(targetPDF);
			document.close();
		} catch (FontFormatException e) {
			logger.info("Font corrupted.");
			logger.error(e.getMessage(), e);
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
		} catch (COSVisitorException e) {
			logger.error(e.getMessage(), e);
		} 
	}

	private boolean printTextOrderToPDF(String orderid, String targetPdfPath){
		boolean ret= false;
		Map<String,Object> content = (Map<String,Object>)getOrderDetail(orderid).get("order");
		List<String> lineContent = new ArrayList<String>();
		if (content!=null){
//			List<String> attrMapper = PropertiesManager.getOrderDetailPdfMapper();
//			for (String key : attrMapper){
//				String[] array = key.split("=");
//				key = array[0];
//				Object value = null;
//				if (key.contains(".")){
//					value = getContentLine(key.split("\\."), content, 0);
//				} else {
//					value = content.get(key)+"";
//				}
//				lineContent.add(array[1]+" : "+value);
//			}
			

			Date date = new Date((long)content.get("createdDate"));
			String dateString = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").format(date);
			lineContent.add("订购日期:" + dateString);
			lineContent.add("订单号:" + content.get("id"));
			
//			lineContent.add("产品:" + content.get("productText"));
			lineContent.add("");
			
			List<Map<String, Object>> orderItems = (List<Map<String, Object>>)content.get("orderitemvos");
			
			for (int i = 1; i <= orderItems.size(); i ++) {
				Map<String, Object> orderItem = orderItems.get(i - 1);
				lineContent.add("订单行" + i + ":");
				
				Map<String, Object> product = (Map<String, Object>)orderItem.get("productVo");
				lineContent.add("产品名称:" + product.get("name"));
				lineContent.add("产品描述:" + product.get("description"));
				lineContent.add("产品长:" + product.get("length"));
				lineContent.add("产品宽:" + product.get("width"));
				lineContent.add("装帧方式:" + product.get("bindingWay"));
				lineContent.add("封面纸张:" + product.get("coverPaper"));
				lineContent.add("内页纸张:" + product.get("insidePaper"));
				lineContent.add("封面纸厚:" + product.get("heightOfCover"));
				lineContent.add("内页纸厚:" + product.get("heightOfInsidePage"));
			}
			
			lineContent.add("");
			lineContent.add("邮费:" + content.get("freight"));
			
			lineContent.add("订单总额(包含邮费):"+content.get("orderprice"));
			lineContent.add("收货人:"+content.get("consignee"));
			lineContent.add("送货国家:"+content.get("country"));
			lineContent.add("送货省:"+content.get("province"));
			lineContent.add("送货城市:"+content.get("city"));
			lineContent.add("送货县:"+content.get("district"));
			lineContent.add("收货地址(街道):"+content.get("addressDetail"));
			lineContent.add("完整地址:" + content.get("completeAddress"));
			lineContent.add("邮编:"+content.get("zipcode"));
			lineContent.add("客户手机:"+content.get("mobile"));
			lineContent.add("客户固定电话:"+content.get("tel"));
//			lineContent.add("最佳收货日期:"+content.get("bestdate"));
			
//			lineContent.add("订购日期:"+content.get("date"));
//			lineContent.add("订单号:"+content.get("orderid"));
//			List<Map<String, String>> products = (List<Map<String, String>>)content.get("products");
//			if (products!=null){
//				int i=0;
//				for (Map<String, String> product: products){
//					if (products.size()==1){
//						lineContent.add("产品名称:"+product.get("name"));
//						lineContent.add("产品规格:"+product.get("description"));
//						lineContent.add("产品规格备注:"+product.get("comment"));
//						lineContent.add("产品单价:"+product.get("price"));
//						lineContent.add("产品数量:"+product.get("quantity"));			
//					} else {
//						i++;
//						lineContent.add("产品"+i+"名称:"+product.get("name"));
//						lineContent.add("产品"+i+"规格:"+product.get("description"));
//						lineContent.add("产品"+i+"规格备注:"+product.get("comment"));
//						lineContent.add("产品"+i+"单价:"+product.get("price"));
//						lineContent.add("产品"+i+"数量:"+product.get("quantity"));
//					}
//					lineContent.add("作品编号:"+product.get("workname"));
//				}
//			}
//			lineContent.add("订单产品种类数量:"+content.get("productcount"));
//			lineContent.add("邮费:"+content.get("delivery"));
//			List<Map<String, String>> coupons = (List<Map<String, String>>)content.get("coupons");
//			if (products!=null){
//				int i=0;
//				for (Map<String, String> coupon: coupons){
//					for (String key:coupon.keySet()) {
//						if (coupons.size()==1){
//							lineContent.add("优惠券代码:"+key);
//							lineContent.add("优惠券名称:"+coupon.get(key));
//						} else {
//							i++;
//							lineContent.add("优惠券"+i+"代码:"+key);
//							lineContent.add("优惠券"+i+"名称:"+coupon.get(key));
//						}
//					}
//				}
//			}
//			
//			lineContent.add("订单总额(包含邮费):"+content.get("totalfee"));
//			lineContent.add("客户姓名:"+content.get("customername"));
//			lineContent.add("送货国家:"+content.get("country"));
//			lineContent.add("送货省:"+content.get("province"));
//			lineContent.add("送货城市:"+content.get("city"));
//			lineContent.add("送货县:"+content.get("dist"));
//			lineContent.add("收货地址(街道):"+content.get("address"));
//			lineContent.add("邮编:"+content.get("postal"));
//			lineContent.add("客户手机:"+content.get("mobile"));
//			lineContent.add("客户固定电话:"+content.get("telephone"));
//			lineContent.add("最佳收货日期:"+content.get("bestdate"));

			Document document = new Document();
			try {
				ClassLoader classloader = Thread.currentThread().getContextClassLoader();
				InputStream is = classloader.getResourceAsStream("SourceHanSansCN-Normal.ttf");
				byte[] bytes = IOUtils.toByteArray(is);
				
				BaseFont bf = BaseFont.createFont("SourceHanSansCN-Normal.ttf", BaseFont.IDENTITY_H, BaseFont.EMBEDDED, false, bytes,null);

				com.itextpdf.text.Font textFont = new com.itextpdf.text.Font(bf, 10, com.itextpdf.text.Font.NORMAL);
				PdfWriter.getInstance(document, new FileOutputStream(targetPdfPath));
				document.open();
				for (String line:lineContent){
					document.add(new Paragraph(line, textFont));
				}
				document.close();
				ret = true;
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (DocumentException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		return ret;
	}

	//	private boolean printOrderToPDF(String orderid, String targetPdfPath){
	//		boolean ret= false;
	//		Map<String,Object> content = (Map<String,Object>)getOrderDetail(orderid).get("order");
	//		List<String> lineContent = new ArrayList<String>();
	//		if (content!=null){
	//			List<String> attrMapper = PropertiesManager.getOrderDetailPdfMapper();
	//			for (String key : attrMapper){
	//				String[] array = key.split("=");
	//				key = array[0];
	//				Object value = null;
	//				if (key.contains(".")){
	//					value = getContentLine(key.split("\\."), content, 0);
	//				} else {
	//					value = content.get(key)+"";
	//				}
	//				lineContent.add(array[1]+" : "+value);
	//			}
	//
	//			PDDocument document = new PDDocument();
	//			PDPage page = new PDPage();
	//			PDPageContentStream contentStream;
	//			InputStream is = null;
	//			try {
	//				contentStream = new PDPageContentStream(document, page, true, true);
	//
	//				ClassLoader classloader = Thread.currentThread().getContextClassLoader();
	//				is = classloader.getResourceAsStream("SourceHanSansCN-Normal.ttf");
	//				Font font =Font.createFont( Font.TRUETYPE_FONT,is);
	//
	//				float fontsize = 20f;
	//
	//				//actually draw the caption to image and append to pdf
	//				BufferedImage image = new BufferedImage((int)(page.getMediaBox().getWidth()), (int)(page.getMediaBox().getHeight()), BufferedImage.TYPE_4BYTE_ABGR);
	//
	//				Graphics2D graphics = image.createGraphics();
	//
	//				font=font.deriveFont(Font.PLAIN, fontsize);
	//				int textverticaloffset = graphics.getFontMetrics().getHeight();
	//				CompositionUtils.setRenderingHint(graphics);
	//				for (String line:lineContent){
	//					textverticaloffset+=graphics.getFontMetrics().getHeight();
	//					graphics.setColor(Color.BLACK);
	//					graphics.setFont(font);
	//					graphics.drawString(line,graphics.getFontMetrics().getHeight(), textverticaloffset);
	//				}
	//
	//				PDXObjectImage ximage = new PDPixelMap(document, image);
	//				contentStream.drawXObject(ximage, 0, 0, page.getMediaBox().getWidth(), page.getMediaBox().getHeight());
	//				document.addPage(page);
	//				contentStream.close();
	//				document.save(targetPdfPath);
	//
	//				ret = true;
	//			} catch (IOException e) {
	//				logger.info(e);
	//			} catch (COSVisitorException e) {
	//				logger.info(e);
	//			} catch (FontFormatException e) {
	//				logger.info(e);
	//			} finally {
	//				if (is!=null){
	//					IOUtils.closeQuietly(is);
	//				}
	//				try {
	//					if (document!=null){
	//						document.close();
	//					}
	//				} catch (IOException e) {
	//					logger.info(e);
	//				}
	//			}
	//		}
	//		return ret;
	//	}

	private String getContentLine (String[] keys, Object obj, int sequence){
		String retr ="";
		if (sequence>=keys.length){
			return retr;
		}
		String key = keys[sequence];
		if (obj instanceof List){
			List<Object> lst = (List<Object>)obj;
			for (Object lstobj:lst){
				retr+= " "+getContentLine(keys, lstobj, sequence);
			}
		} else if (obj instanceof Map){
			Map<Object, Object> map = (Map<Object, Object>)obj;
			if (map.containsKey(key)){
				if (map.get(key) instanceof Map||map.get(key) instanceof Collection){
					retr = getContentLine(keys, map.get(key), ++sequence);
				} else {
					retr = map.get(key)+"";
					if (map.get(key)==null){
						retr="";
					}
				}
			}
		} else {
			retr = obj+"";
		}

		return retr;
	}

	private int getHorizontalOffsetBydocAlign (WeBookContent content, Graphics context){
		int offset = 0;
		int captionwidth = (int)(PropertiesManager.getScaleFactorByDPI()*CompositionUtils.getPointValueFromMM(content.getWidth()));
		String docalign = content.getTextalign();
		if (docalign == null || docalign.isEmpty()){
			return offset;
		}
		switch (docalign) {
		case "left":
			offset = 0;
			break;
		case "center":
			offset =(int)(captionwidth - context.getFontMetrics().getStringBounds(content.getText(), context).getWidth())/2;
			break;
		case "right":
			offset =(int)(captionwidth - context.getFontMetrics().getStringBounds(content.getText(), context).getWidth());
			break;
		default:
			break;
		}
		return offset;
	}

	private int getHorizontalOffsetByAlign (String text,int width, Graphics context){
		int offset = 0;
		int captionwidth = (int)(PropertiesManager.getScaleFactorByDPI()*CompositionUtils.getPointValueFromMM(width));
		offset =(int)(captionwidth - context.getFontMetrics().getStringBounds(text, context).getWidth())/2;
	
		return offset;
	}
	private int getVerticalOffsetBydocAlign (WeBookContent content, Graphics context){
		int offset = 0;
		offset=(int)(context.getFontMetrics().getStringBounds(content.getText(), context).getHeight());
		return offset;
	}

	private void drawCoversBleedingLine(int bleedingmmvalue, PDPageContentStream contentStream, PDRectangle pageBounds) throws IOException{
		float pixelPageWidth = pageBounds.getWidth();
		float pixelPageHeight = pageBounds.getHeight();

		float pixelBleeding = CompositionUtils.getPointValueFromMM(bleedingmmvalue);
		float lineLength = CompositionUtils.getPointValueFromMM(PropertiesManager.getBleedingLineLength());
		contentStream.setLineWidth(PropertiesManager.getBleedingLineWidth());

		//append bleeding line
		contentStream.setNonStrokingColor(Color.BLACK);

		//horizontal lines
		contentStream.addLine(0, pixelBleeding, lineLength, pixelBleeding);
		contentStream.addLine(0, pixelPageHeight-pixelBleeding, lineLength, pixelPageHeight-pixelBleeding);
		contentStream.addLine(pixelPageWidth-lineLength, pixelBleeding, pixelPageWidth, pixelBleeding);
		contentStream.addLine(pixelPageWidth-lineLength, pixelPageHeight-pixelBleeding, pixelPageWidth, pixelPageHeight-pixelBleeding);
		//vertical lines
		contentStream.addLine(pixelBleeding, 0, pixelBleeding, lineLength);
		contentStream.addLine(pixelBleeding, pixelPageHeight, pixelBleeding, pixelPageHeight-lineLength);
		contentStream.addLine(pixelPageWidth-pixelBleeding, 0, pixelPageWidth-pixelBleeding, lineLength);
		contentStream.addLine(pixelPageWidth-pixelBleeding, pixelPageHeight, pixelPageWidth-pixelBleeding, pixelPageHeight-lineLength);

		contentStream.stroke();
	}

	private void drawLeavesBleedingLine (int bleedingmmvalue, PDPageContentStream contentStream, PDRectangle pageBounds) throws IOException{
		float pixelBleeding = CompositionUtils.getPointValueFromMM(bleedingmmvalue);
		float pixelPageWidth = pageBounds.getWidth();
		float pixelPageHeight = pageBounds.getHeight();
		contentStream.setLineWidth(PropertiesManager.getBleedingLineWidth());

		int pixelDescriptionTextHeight = (int)PropertiesManager.getPixelDescriptionTextHeight();

		//append bleeding line
		contentStream.setNonStrokingColor(Color.BLACK);
		contentStream.addLine(0, pixelDescriptionTextHeight, pixelDescriptionTextHeight, pixelDescriptionTextHeight);
		contentStream.addLine(0, pixelDescriptionTextHeight+pixelBleeding, pixelDescriptionTextHeight, pixelDescriptionTextHeight+pixelBleeding);
		contentStream.addLine(0, pixelPageHeight-pixelDescriptionTextHeight, pixelDescriptionTextHeight, pixelPageHeight-pixelDescriptionTextHeight);
		contentStream.addLine(0, pixelPageHeight-pixelDescriptionTextHeight-pixelBleeding, pixelDescriptionTextHeight, pixelPageHeight-pixelDescriptionTextHeight-pixelBleeding);

		contentStream.addLine(pixelPageWidth, pixelDescriptionTextHeight, pixelPageWidth-pixelDescriptionTextHeight, pixelDescriptionTextHeight);
		contentStream.addLine(pixelPageWidth, pixelDescriptionTextHeight+pixelBleeding, pixelPageWidth-pixelDescriptionTextHeight, pixelDescriptionTextHeight+pixelBleeding);
		contentStream.addLine(pixelPageWidth, pixelPageHeight-pixelDescriptionTextHeight, pixelPageWidth-pixelDescriptionTextHeight, pixelPageHeight-pixelDescriptionTextHeight);
		contentStream.addLine(pixelPageWidth, pixelPageHeight-pixelDescriptionTextHeight-pixelBleeding, pixelPageWidth-pixelDescriptionTextHeight, pixelPageHeight-pixelDescriptionTextHeight-pixelBleeding);

		contentStream.addLine(pixelDescriptionTextHeight, 0, pixelDescriptionTextHeight, pixelDescriptionTextHeight);
		contentStream.addLine(pixelDescriptionTextHeight+pixelBleeding, 0, pixelDescriptionTextHeight+pixelBleeding, pixelDescriptionTextHeight);
		contentStream.addLine(pixelPageWidth-pixelDescriptionTextHeight, 0, pixelPageWidth-pixelDescriptionTextHeight, pixelDescriptionTextHeight);
		contentStream.addLine(pixelPageWidth-pixelDescriptionTextHeight-pixelBleeding, 0, pixelPageWidth-pixelDescriptionTextHeight-pixelBleeding, pixelDescriptionTextHeight);

		contentStream.addLine(pixelDescriptionTextHeight, pixelPageHeight, pixelDescriptionTextHeight, pixelPageHeight-pixelDescriptionTextHeight);
		contentStream.addLine(pixelDescriptionTextHeight+pixelBleeding, pixelPageHeight, pixelDescriptionTextHeight+pixelBleeding, pixelPageHeight-pixelDescriptionTextHeight);
		contentStream.addLine(pixelPageWidth-pixelDescriptionTextHeight, pixelPageHeight, pixelPageWidth-pixelDescriptionTextHeight, pixelPageHeight-pixelDescriptionTextHeight);
		contentStream.addLine(pixelPageWidth-pixelDescriptionTextHeight-pixelBleeding, pixelPageHeight, pixelPageWidth-pixelDescriptionTextHeight-pixelBleeding, pixelPageHeight-pixelDescriptionTextHeight);


		contentStream.stroke();
	}

	private String generatePageDescription(int pageType ,PDPageContentStream contentStream, int pageNumber, int totalNumber) {
		String workid = task.getWorkid();
		String typecode = "";
		String type = "";
		switch (pageType) {
		case Constants.LEAF_TYPE:
			typecode = "LEAF";
			type = "内页";
			break;
		case Constants.COVER_TYPE:
			typecode = "COVER";
			type = "封面";
			break;
		}
		String pageInfo = pageNumber+"/"+totalNumber;
		return workid+" "+typecode+" "+type+" "+pageInfo;
	}

	private void appendPageDescription (String text, float textOffset, PDPageContentStream contentStream, PDRectangle pageBounds, PDDocument document) throws IOException{
		ClassLoader classloader = Thread.currentThread().getContextClassLoader();
		InputStream is = classloader.getResourceAsStream("SourceHanSansCN-Normal.ttf");
		Font font = null;
		try {
			font = Font.createFont( Font.TRUETYPE_FONT,is);

			String content = text;
			int fontsize = 6;
			int width = text.length()*fontsize;
			int height = fontsize;

			//actually draw the caption to image and append to pdf
			BufferedImage image = new BufferedImage((int)PropertiesManager.getScaleFactorByDPI()*width, (int)PropertiesManager.getScaleFactorByDPI()*height, BufferedImage.TYPE_4BYTE_ABGR);
			Graphics2D graphics = image.createGraphics();

			font=font.deriveFont(Font.PLAIN, PropertiesManager.getScaleFactorByDPI()*fontsize);
			graphics.setColor(Color.BLACK);
			graphics.setFont(font);
			CompositionUtils.setRenderingHint(graphics);
			graphics.drawString(content, 10, (int)(PropertiesManager.getScaleFactorByDPI()*fontsize)-5);

			graphics.dispose();
			PDXObjectImage ximage = new PDPixelMap(document, image);
			contentStream.drawXObject(ximage, 3*textOffset, pageBounds.getHeight()-height, width, height);

		} catch (FontFormatException e) {
			logger.info("Font corrupted.");
			logger.error(e.getMessage(), e);
		}
	}

	private void drawSpineLine(float SpinemmWidth, PDPageContentStream contentStream, PDRectangle pageBounds) throws IOException {
		float pixelSpineWidth = CompositionUtils.getPointValueFromMM(SpinemmWidth);
		float spineX = (pageBounds.getWidth()-pixelSpineWidth)/2;
		float lineLength = CompositionUtils.getPointValueFromMM(PropertiesManager.getSpineLineLength());
		float pixelPageHeight = pageBounds.getHeight();
		contentStream.setLineWidth(PropertiesManager.getBleedingLineWidth());

		//append spine line
		contentStream.setNonStrokingColor(Color.BLACK);
		contentStream.addLine(spineX, 0, spineX, lineLength);
		contentStream.addLine(spineX+pixelSpineWidth, 0, spineX+pixelSpineWidth, lineLength);
		contentStream.addLine(spineX, pixelPageHeight, spineX, pixelPageHeight-lineLength);
		contentStream.addLine(spineX+pixelSpineWidth, pixelPageHeight, spineX+pixelSpineWidth, pixelPageHeight-lineLength);

		contentStream.stroke();
	}
	
	private void drawFlapLine(float flapMMWidth, PDPageContentStream contentStream, PDRectangle pageBounds) throws IOException {
		float pixelFlapWidth = CompositionUtils.getPointValueFromMM(flapMMWidth);
		
		float coverBleedWidth = CompositionUtils.getPointValueFromMM(PropertiesManager.getCoverBleedingMMWidth());
		float flapX = pixelFlapWidth + coverBleedWidth;
		float lineLength = CompositionUtils.getPointValueFromMM(PropertiesManager.getFlapLineLength());
		float pixelPageHeight = pageBounds.getHeight();
		float pixelPageWidth = pageBounds.getWidth();
		contentStream.setLineWidth(PropertiesManager.getBleedingLineWidth());

		//append spine line
		contentStream.setNonStrokingColor(Color.BLACK);
		contentStream.addLine(flapX, 0, flapX, lineLength);
		contentStream.addLine(pixelPageWidth - flapX, 0, pixelPageWidth - flapX, lineLength);
		contentStream.addLine(flapX, pixelPageHeight, flapX, pixelPageHeight-lineLength);
		contentStream.addLine(pixelPageWidth - flapX, pixelPageHeight, pixelPageWidth - flapX, pixelPageHeight-lineLength);

		contentStream.stroke();
	}
	public static Map faceAddress(String args){
		//匹配qq中文表情字符
//		String qqRegex = "\\[[\u4E00-\u9FA5]+\\]";
		String qqRegex = "\\[(\\\\x[a-fA-F0-9]{2}){6}\\]";
		String emojiRegex ="(\\\\x[fF]0\\\\x9[fF])(\\\\x[A-Fa-f0-9]{2}){2}";
		String myRegex = "[(\\[(\\\\x[a-fA-F0-9]{2}){6}\\])((\\\\x[fF]0\\\\x9[fF])(\\\\x[A-Fa-f0-9]{2}){2})]";
		Map contentMap = new LinkedHashMap();
		String tag = null;
		try {
			args  =str2Hex(args);
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Pattern p = Pattern.compile(myRegex);
		Matcher m = p.matcher(args);
		Pattern qq = Pattern.compile(qqRegex);
		
		while (m.find()){
			String matchString = m.group();
			Matcher mq = qq.matcher(matchString);
			if(mq.find()){
				tag = "qq";
			}else{
				tag = "emoji";
			}
			try {
				contentMap.put(hex2Str(matchString), tag);
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		return contentMap;
	}
	
	public static boolean isQQFace(String args){
		//匹配qq中文表情字符
		boolean tag = true;
		String qqRegex = "\\[[\u4E00-\u9FA5]+\\]";
		Pattern p = Pattern.compile(qqRegex);
		Matcher m = p.matcher(args);
		
		if (!m.find()){
			tag = false;
		}
		return tag;
	}
	//String转成hex
	public static String str2Hex(String str) throws UnsupportedEncodingException {
        String hexRaw = String.format("%x", new BigInteger(1, str.getBytes("UTF-8")));
        char[] hexRawArr = hexRaw.toCharArray();
        StringBuilder hexFmtStr = new StringBuilder();
        final String SEP = "\\x";
        for (int i = 0; i < hexRawArr.length; i++) {
            hexFmtStr.append(SEP).append(hexRawArr[i]).append(hexRawArr[++i]);
        }
        return hexFmtStr.toString();
    }
	
	//hex转成String
	public static String hex2Str(String str) throws UnsupportedEncodingException {
        String strArr[] = str.split("\\\\"); // 分割拿到形如 xE9 的16进制数据
        byte[] byteArr = new byte[strArr.length - 1];
        for (int i = 1; i < strArr.length; i++) {
            Integer hexInt = Integer.decode("0" + strArr[i]);
            byteArr[i - 1] = hexInt.byteValue();
        }
 
        return new String(byteArr, "UTF-8");
    }
	//emoji表情的识别
	public static Map emojiAddress(String args)
	{		
//		String testString = "\\xf0\\x9f\\x90\\x85\\xf0\\x9f\\x98\\xbb\\xf0\\x9f\\x91\\xb8\\xf0\\x9f\\x8f\\xbf\\xf0\\x9f\\x91\\xb8\\xf0\\x9f\\x8f\\xbb\\xf0\\x9f\\x91\\xb8\\xf0\\x9f\\x8f\\xbc\\xf0\\x9f\\x91\\xb8\\xf0\\x9f\\x8f\\xbd\\xf0\\x9f\\x90\\xa4\\xf0\\x9f\\x90\\x81\\xf0\\x9f\\x90\\xb5\\xf0\\x9f\\x98\\x92\\xf0\\x9f\\x90\\x81\\xf0\\x9f\\x90\\x8f\\xf0\\x9f\\x91\\xb8\\xf0\\x9f\\x99\\x8b\\xf0\\x9f\\x8f\\xbf\\xf0\\x9f\\x90\\xb9\\xf0\\x9f\\x8e\\xb8\\xf0\\x9f\\x98\\x88\\xf0\\x9f\\x98\\x8a\\xf0\\x9f\\x98\\x88\\xf0\\x9f\\x98\\x88\\xe2\\x98\\xba\\xef\\xb8\\x8f\\xf0\\x9f\\x98\\x8f\\xf0\\x9f\\x91\\xa8\\xe2\\x80\\x8d\\xf0\\x9f\\x91\\xa9\\xe2\\x80\\x8d\\xf0\\x9f\\x91\\xa7\\xf0\\x9f\\x91\\xa8\\xf0\\x9f\\x8f\\xbc\\xf0\\x9f\\x91\\xa9\\xe2\\x80\\x8d\\xf0\\x9f\\x91\\xa9\\xe2\\x80\\x8d\\xf0\\x9f\\x91\\xa7\\xe2\\x80\\x8d\\xf0\\x9f\\x91\\xa6\\xf0\\x9f\\x91\\xa8\\xe2\\x80\\x8d\\xf0\\x9f\\x91\\xa8\\xe2\\x80\\x8d\\xf0\\x9f\\x91\\xa7\\xf0\\x9f\\x91\\xa9\\xe2\\x80\\x8d\\xf0\\x9f\\x91\\xa9\\xe2\\x80\\x8d\\xf0\\x9f\\x91\\xa6\\xe2\\x80\\x8d\\xf0\\x9f\\x91\\xa6\\xf0\\x9f\\x91\\x87\\xf0\\x9f\\x8f\\xbb\\xf0\\x9f\\x91\\x88\\xf0\\x9f\\x91\\x88\\xf0\\x9f\\x8c\\xb9\\xf0\\x9f\\x92\\x90\\xf0\\x9f\\x8d\\x84\\xf0\\x9f\\x90\\xae\\xf0\\x9f\\x90\\xae\\xf0\\x9f\\x90\\x8b\\xf0\\x9f\\x90\\xb3\\xf0\\x9f\\x90\\xb8\\xf0\\x9f\\x90\\x9c\\xf0\\x9f\\x90\\x9d\\xf0\\x9f\\x8d\\x8e\\xf0\\x9f\\x8d\\x8a\\xf0\\x9f\\x8d\\x8b\\xf0\\x9f\\x8d\\x93\\xf0\\x9f\\x8d\\x99\\xf0\\x9f\\x8d\\x99\\xf0\\x9f\\x8e\\x86\\xf0\\x9f\\x8e\\x8a\\xf0\\x9f\\x8e\\x91\\xf0\\x9f\\x92\\xab\\xf0\\x9f\\x91\\x91\\xf0\\x9f\\x91\\x91\\xf0\\x9f\\x8f\\x86\\xe2\\x9a\\xbe\\xef\\xb8\\x8f\\xf0\\x9f\\x8e\\xbb\\xf0\\x9f\\x8e\\xa9\\xf0\\x9f\\x8e\\xbd\\xf0\\x9f\\x9a\\x91\\xf0\\x9f\\x9a\\x87\\xf0\\x9f\\x9a\\x8c\\xf0\\x9f\\x9a\\x96\\xf0\\x9f\\x9a\\x9a\\xf0\\x9f\\x9a\\x9b\\xf0\\x9f\\x87\\xb5\\xf0\\x9f\\x87\\xb9\\xf0\\x9f\\x87\\xb3\\xf0\\x9f\\x87\\xb1\\xf0\\x9f\\x87\\xb5\\xf0\\x9f\\x87\\xad\\xf0\\x9f\\x87\\xbf\\xf0\\x9f\\x87\\xa6\\xf0\\x9f\\x87\\xbf\\xf0\\x9f\\x87\\xa6\\xf0\\x9f\\x93\\x9e\\xf0\\x9f\\x93\\x9e\\xf0\\x9f\\x94\\x8c\\xf0\\x9f\\x92\\xb0\\xf0\\x9f\\x91\\x9d\\xf0\\x9f\\x92\\xb0\\xe2\\x9c\\x89\\xef\\xb8\\x8f\\xf0\\x9f\\x93\\xa8\\xf0\\x9f\\x94\\x96\\xf0\\x9f\\x93\\xac\\xf0\\x9f\\x93\\xac\\xf0\\x9f\\x9a\\xb1\\xe2\\x9b\\x94\\xef\\xb8\\x8f\\xf0\\x9f\\x89\\x91\\xf0\\x9f\\x88\\xb2\\xf0\\x9f\\x88\\xb2\\xf0\\x9f\\x86\\x93\\xf0\\x9f\\x8f\\xa7\\xe2\\x99\\x8b\\xef\\xb8\\x8f\\xe2\\x99\\x8f\\xef\\xb8\\x8f\\xf0\\x9f\\x94\\x810\\xe2\\x83\\xa36\\xe2\\x83\\xa32\\xe2\\x83\\xa3\\xe2\\x9b\\x8e\\xf0\\x9f\\x92\\xaf\\xf0\\x9f\\x92\\xaf\\xf0\\x9f\\x94\\x9b\\xf0\\x9f\\x94\\xaf\\xe2\\x97\\xbd\\xef\\xb8\\x8f\\xe2\\x97\\xbd\\xef\\xb8\\x8f\\xf0\\x9f\\x95\\x96\\xf0\\x9f\\x95\\x9a\\xf0\\x9f\\x95\\x96\\xf0\\x9f\\x95\\xa0\\xf0\\x9f\\x95\\x9c\\xf0\\x9f\\x95\\x9b\\xf0\\x9f\\x95\\x9f\\xf0\\x9f\\x95\\x97\\xf0\\x9f\\x95\\x92\\xf0\\x9f\\x94\\xb8\\xf0\\x9f\\x94\\xb9";
		Map map = new HashMap();
		Map contentMap = new LinkedHashMap();
		List faceList = new ArrayList();
		int i = 0;
		int n = 0;
		String myRegex ="(\\\\x[fF]0\\\\x9[fF])(\\\\x[A-Fa-f0-9]{2}){2}";
		try {
			args = str2Hex(args);
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Pattern p = Pattern.compile(myRegex);
		Matcher matcher = p.matcher(args);
		
		while (matcher.find()){
			String matchStr = matcher.group();
			n+=matchStr.length();
			int length = (matcher.end() - matcher.start())+1;
			try {
				matchStr = hex2Str(matchStr);
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			faceList.add(matchStr);
			++i;
		}
		map.put("faceNumber", i);
		map.put("byteNumber", n);
		map.put("content", faceList);
		
		return map;
	}
	//qq表情的识别
public static Map qqfaceAddress(String args){
		
//		String myRegex = "\\[(\\\\x[a-fA-F0-9]{2}){6}\\]";
		//匹配qq中文表情字符
		String myRegex = "\\[[\u4E00-\u9FA5]+\\]";
		Map map = new HashMap();
		Map contentMap = new LinkedHashMap();
		List faceList = new ArrayList();
		int i = 0;
		int n = 0;
//		String testString = "\\xe8\\xa1\\xa8\\xe6\\x83\\x85\\xe5\\x8a\\xa0\\xe5\\x9b\\xbe\\xe7\\x89\\x87[\\xe6\\xb5\\x81\\xe6\\xb3\\xaa][\\xe5\\x86\\xb7\\xe6\\xb1\\x97][\\xe5\\x8f\\x91\\xe6\\x80\\x92]\\xf0\\x9f\\x91\\xbf\\xf0\\x9f\\x8e\\x88\\xf0\\x9f\\x91\\xbb";
		
//		String testString = "[大哭]";
		Pattern p = Pattern.compile(myRegex);
		Matcher m = p.matcher(args);
		
		while (m.find()){
			
			String matchString = m.group();
			//去掉中括号后 打印输出
			String qqString = matchString.substring(matchString.indexOf("[")+1, matchString.lastIndexOf("]"));
			n+=matchString.length();
			int[] indexs = new int[2];//用于存放每个表情的index信息
//			indexs[0] = i;
//			indexs[0] = m.start();
//			indexs[1] = m.end()-1;
			faceList.add(matchString);
//			contentMap.put(matchString, indexs);
			++i;
		}
		map.put("faceNumber", i);
		map.put("byteNumber", n);
		map.put("content", faceList);
		
		return map;
	}
}
