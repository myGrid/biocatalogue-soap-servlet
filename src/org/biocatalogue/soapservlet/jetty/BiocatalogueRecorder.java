package org.biocatalogue.soapservlet.jetty;



import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import ca.ucalgary.services.SoapServlet;
import ca.ucalgary.services.util.DataRecorder;
import ca.ucalgary.services.util.SourceMap;



public class BiocatalogueRecorder implements DataRecorder {

	public static final String dirPath="/home/jerzyo/tmp/SoapServlet/";
	
	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response) {
		System.out.println("doGet");

	}

	@Override
	public String getBody(HttpServletRequest formRequest) {
		System.out.println("getBody");
		String result="";
		return result;
	}



	@Override
	public String getHead(HttpServletRequest formRequest) {
		System.out.println("getHead");
		String result="";
		return result;
	}

	@Override
	public String getOnEventText() {
		System.out.println("getOnEventText");
		return "";
	}

	@Override
	public String getOnSubmitText() {
		System.out.println("getOnSubmitText");
		return "";
	}

	@Override
	public void interceptRequest(HttpServletRequest request,
			HttpServletResponse response) {
		System.out.println("interceptRequest");

	}

	@Override
	public String markupResponse(Source resultSource,
			HttpServletRequest submissionRequest) throws Exception {
		System.out.println("markuprResponse");
		return null;
	}

	@Override
	public void setInputSource(HttpServletRequest submissionRequest,
			SourceMap source) {
		System.out.println("setInputSource");
		String wsdlLoc = submissionRequest.getParameter(SoapServlet.WSDL_HTTP_PARAM);
		String sourceString=source.toString();
		String serviceSpec = submissionRequest.getParameter(SoapServlet.SERVICE_SPEC_PARAM);
		String data="";
		Map<String,String[]> map=submissionRequest.getParameterMap();
		for(String key : map.keySet()){
			data=data+key+":[";
			String[] values=map.get(key);
			for(String value:values){
				data=data+value+",";
			}
			data=data+"];\n";
		}
	    try {
	        BufferedWriter out = new BufferedWriter(new FileWriter(dirPath+String.valueOf(new Random().nextInt()+"txt")));
	        out.write("wsdl:"+wsdlLoc+"\n");
	        out.write("input:"+sourceString+"\n");
	        out.write("data:"+data+"\n");
	        out.close();
	    } catch (IOException e) {
	    }
		

			
		
		
		

	}

	@Override
	public void setTransformer(Transformer transformer) {
		System.out.println("setTransformer");

	}

	@Override
	public boolean shouldIntercept(HttpServletRequest request) {
		System.out.println("shouldIntercept");
		return false;
	}

	@Override
	public void startRecording(HttpServletRequest initialRequest) {
		System.out.println("startrecording");

	}

	@Override
	public String writeWrapperForm(HttpServletRequest request) {
		System.out.println("writeWrapperForm");
		return "";
	}

	@Override
	public Node[] getBodyAsDOM(HttpServletRequest formRequest, Document owner) {
		System.out.println("getBodyAsDOM");
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getBodyAttrs(HttpServletRequest formRequest) {
		System.out.println("getBodyAttrs");
		// TODO Auto-generated method stub
		return "";
	}

	@Override
	public Attr[] getBodyAttrsAsDOM(HttpServletRequest formRequest,
			Document owner) {
		System.out.println("getBodyAttrsAsDOM");
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Node[] getHeadAsDOM(HttpServletRequest formRequest, Document owner) {
		System.out.println("getHeadAsDOM");
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Attr getOnEventAsDOM(Document owner) {
		System.out.println("getOnEventAsDOM");
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Attr getOnSubmitAsDOM(Document owner) {
		System.out.println("getOnSubmitAsDOM");
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public byte[] markupResponse(byte[] responseBody, String contentType,
			String charSetEncoding, HttpServletRequest submissionRequest)
			throws Exception {
		System.out.println("markupResponse");
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setInputParams(HttpServletRequest submissionRequest,
			Map<String, byte[]> httpParams, List<String> hiddenParams) {
		System.out.println("markupResponse");
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setParameter(HttpSession session, String paramName,
			String paramValue) {
		System.out.println("markupResponse");
		// TODO Auto-generated method stub
		
	}

}
