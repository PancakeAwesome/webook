package com.jihox.composition.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;

import com.jihox.composition.manager.PropertiesManager;


public class HttpUtils {
	
	private static Logger logger=Logger.getLogger(HttpUtils.class);
	
	public static String getTextResult(String url){
		String responseBody ="";
		CloseableHttpClient httpclient = HttpClients.createDefault();
		try {
			HttpGet httpget = new HttpGet(url);
			appendAuthenticationInfo(httpget);
			ResponseHandler<String> responseHandler = new ResponseHandler<String>() {
				@Override
				public String handleResponse( final HttpResponse response) throws ClientProtocolException, IOException {
					int status = response.getStatusLine().getStatusCode();
					if (status >= 200 && status < 300) {
						HttpEntity entity = response.getEntity();
						
						String result = null;
						if (entity != null){
							result = EntityUtils.toString(entity, "UTF-8");
//							result = EntityUtils.toString(entity);
						}

						EntityUtils.consume(entity);

						return result;

					} else {
						throw new ClientProtocolException("Unexpected response status: " + status);
					}
				}
			};
			
			responseBody = httpclient.execute(httpget, responseHandler);
			
			logger.info(responseBody);
		} catch (ClientProtocolException e) {
			logger.error(e.getMessage(), e);
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
		}  finally {
			try {
				httpclient.close();
			} catch (IOException e) {
				logger.error(e.getMessage(), e);
			}
		}
		return responseBody;
	}

	private static void appendAuthenticationInfo(HttpRequestBase httprequest){
		Map<String, String> map = PropertiesManager.getInstance().getAuthenticationMap();
		for (String key : map.keySet()){
			httprequest.addHeader(key, map.get(key));
		}
	}

	public static boolean DeleteServerFile (String filename){
		boolean ret = false;
		
		return ret;
	}
	
	public static String DownloadOriginalZip (String url){
		CloseableHttpClient httpclient = HttpClients.createDefault();

		HttpGet httpget = new HttpGet(url);
		appendAuthenticationInfo(httpget);

		String downloadedpath = "";

		ResponseHandler<String> responseHandler = new ResponseHandler<String>() {

			@Override
			public String handleResponse( final HttpResponse response) throws ClientProtocolException, IOException {
				int status = response.getStatusLine().getStatusCode();
				String result = "";
				if (status >= 200 && status < 300) {
					HttpEntity entity = response.getEntity();
					if (entity!=null){
						InputStream instream = entity.getContent();
						String fileName = getFileNameFromResponse(response);
						if(fileName==null||StringUtils.isEmpty(fileName)){
							throw new ClientProtocolException("Unexpected response header, file download fail");
						}
						result = PropertiesManager.getZipFolderPath()+fileName;
						OutputStream outputStream = new FileOutputStream(result);
						int read = 0;
						byte[] bytes = new byte[1];
						while ((read = instream.read(bytes)) != -1) {
							outputStream.write(bytes, 0, read);
						}
						instream.close();
						outputStream.close();

						EntityUtils.consume(entity);

						return result;
					} else {
						throw new ClientProtocolException("Unexpected response status: " + status);
					}
				} else {
					throw new ClientProtocolException("Unexpected response status: " + status);
				}
			}
		};

		try {
			downloadedpath = httpclient.execute(httpget, responseHandler);
		} catch (ClientProtocolException e) {
			logger.error(e.getMessage(), e);
		} catch (IOException e) {  
			logger.error(e.getMessage(), e);
		}

		return downloadedpath;
	}

	private static String getFileNameFromResponse (final HttpResponse response){
		Header contentHeader = response.getFirstHeader("Content-Disposition");  
		String filename = null;  
		if (contentHeader != null) {  
			HeaderElement[] values = contentHeader.getElements();  
			if (values.length == 1) {  
				NameValuePair param = values[0].getParameterByName("filename");  
				if (param != null) {  
					try {  
						filename = param.getValue();  
					} catch (Exception e) {  
						logger.error(e.getMessage(), e);
					}  
				}  
			}  
		}
		return filename;
	}

	public static String uploadFile (String localPath, String targetUrl){
		CloseableHttpClient httpclient = HttpClients.createDefault();
		HttpPost httpPost = new HttpPost(targetUrl);
		appendAuthenticationInfo(httpPost);

		String uploadedurl ="";
		
		if (CompositionUtils.fileExists(localPath)){
			
			long filesize = CompositionUtils.getFileSize(localPath);
			
//			long filecrc = CompositionUtils.getCRC32(localPath);
			
			HttpEntity reqEntity = MultipartEntityBuilder.create()
					.addTextBody("filesize", filesize+"")
//					.addTextBody("filecrc", filecrc+"")
					.addPart("file", new FileBody(new File(localPath)))
					.build();
			httpPost.setEntity(reqEntity);

			ResponseHandler<String> responseHandler = new ResponseHandler<String>() {
				@Override
				public String handleResponse( final HttpResponse response) throws ClientProtocolException, IOException {
					int status = response.getStatusLine().getStatusCode();
					if (status >= 200 && status < 300) {
						HttpEntity entity = response.getEntity();
						String resp = null;
						if (entity != null){
							resp = EntityUtils.toString(entity);
						}
						return resp;
					} else {
						throw new ClientProtocolException("Unexpected response status: " + status);
					}
				}
			};

			try {
				uploadedurl = httpclient.execute(httpPost, responseHandler);
			} catch (ClientProtocolException e) {
				logger.error(e.getMessage(), e);
			} catch (IOException e) {
				logger.error(e.getMessage(), e);
			}
		} 
		
		return uploadedurl;
	}
	
	public static String postData(Map<String, String> paramMap, String targetUrl){
		CloseableHttpClient httpclient = HttpClients.createDefault();
		HttpPost httpPost = new HttpPost(targetUrl);
		appendAuthenticationInfo(httpPost);
		
		String result = "";
		
		if (paramMap!=null){
			List<NameValuePair> nvps = new ArrayList<NameValuePair>(); 
			for (String key : paramMap.keySet()){
				nvps.add(new BasicNameValuePair(key, paramMap.get(key)));
			}	
			UrlEncodedFormEntity form = null;
			try {
				form = new UrlEncodedFormEntity(nvps,"UTF-8");
			} catch (UnsupportedEncodingException e) {
				logger.error(e.getMessage(), e);
			}
			
			httpPost.setEntity(form);
		}
		
		ResponseHandler<String> responseHandler = new ResponseHandler<String>() {

			@Override
			public String handleResponse( final HttpResponse response) throws ClientProtocolException, IOException {
				int status = response.getStatusLine().getStatusCode();
				if (status >= 200 && status < 300) {
					HttpEntity entity = response.getEntity();
					String result = entity != null ? EntityUtils.toString(entity) : null;

					EntityUtils.consume(entity);

					return result;

				} else {
					throw new ClientProtocolException("Unexpected response status: " + status);
				}
			}
		};
		
		try {
			result = httpclient.execute(httpPost, responseHandler);
		} catch (ClientProtocolException e) {
			logger.error(e.getMessage(), e);
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
		}
		
		return result;
	}
}
