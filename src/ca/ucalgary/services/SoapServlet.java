package ca.ucalgary.services;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

//import ca.ucalgary.services.util.DataRecorder;
import ca.ucalgary.services.util.SourceMap;

//import javax.servlet.ServletException;
//import javax.servlet.ServletInputStream;
//import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamSource;


import javax.xml.ws.Dispatch;
import javax.xml.ws.Service;

import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

/**
 * SoapServlet.java
 *
 * Provides an HTML interface for invoking SOAP-based Web Services. 
 * The appearance and functionality of the class can be extended 
 * using stylesheets and registering a DataRecorder (e.g. Daggoo 
 * registers PBERecorder to get its functionality).
 *
 * @author     Paul Gordon <gordonp@ucalgary.ca>
 * @copyright  2008-2009 Paul Gordon
 * @license    http://www.debian.org/misc/bsd.license  BSD License (3 Clause)
 * @link       http://biomoby.open-bio.org/CVS_CONTENT/moby-live/Java/docs/soapServlet.html
 */

public class SoapServlet extends WrappingServlet{
	
	public static final String SITE_BASE_URL = "http://sandbox.biocatalogue.org";

	public static final boolean WRAP_SEMANTICALLY = false; 

	public static final String SERVICE_SPEC_PARAM = "service";
	public static final String WSDL_HTTP_PARAM = "srcSpec";
	public static final String OPERATION_HTTP_PARAM = "operation";


	private static final String ARRAY_TYPE_SENTINEL = "ar_TyPe";
	private static final String BASIC_TYPE_SENTINEL = "baSIc_TyPe";
	private static final String BASIC_NAME_SENTINEL = "baSIc_naMe";
	public static final String DOC_PARAM_SENTINEL = "_is_doc_style";

	private static final String DEFERRED_NAMESPACE_URI = "http://my.deferred.sentinel.for.schema.references/";
	public static final String INDENTATION_XSL_RESOURCE = "ca/ucalgary/services/resources/indent.xsl";

	private static final String COMMA_OPTION = "comma (,)";
	private static final String NEW_LINE_OPTION = "new line";
	private static final String TAB_OPTION = "tab";
	private static final String COLON_OPTION = "colon (:)";
	private static final String SEMICOLON_OPTION = "semi-colon (;)";
	private static final String SLASH_OPTION = "slash (/)";
	private static final String WHITESPACE_OPTION = "any whitespace";
	private static final String DQUOTE_OPTION = "double quotes (&quot;...&quot;)";
	private static final String SQUOTE_OPTION = "single quotes (&apos;...&apos;)";

	private static DocumentBuilder docBuilder;

	private static Logger logger = Logger.getLogger(SoapServlet.class.getName());

	static{
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		dbf.setNamespaceAware(true);
		try{
			docBuilder = dbf.newDocumentBuilder();
		} catch(Exception e){
			e.printStackTrace();
		}
	}

	public void init() throws javax.servlet.ServletException{
		super.init();

		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		try{
			URL u = getClass().getClassLoader().getResource(INDENTATION_XSL_RESOURCE);
			if(u != null){
				// for pretty copying of SOAP response XML
				setResponseTransformer(transformerFactory.newTransformer(new StreamSource(u.openStream())));  
			}
			else{
				// verbatim copy as backup plan
				setResponseTransformer(transformerFactory.newTransformer());  
			}
		} catch (Exception e){
			logger.log(Level.SEVERE,
					"Could not create an XSLT transformer: " + e,
					e);
		}
	}

	/**
	 * Post is for service submission (if the wsdl URL is provided)
	 */
	public void callService(HttpServletRequest request,
			HttpServletResponse response,
			URL url,  //url of wsdl file
			PrintStream out){

		String serviceSpec = request.getParameter(SERVICE_SPEC_PARAM);
		if(serviceSpec == null || serviceSpec.trim().length() == 0){
			out.print("<html><head><title>Error</title>\n"+
					"<link type=\"text/css\" rel=\"stylesheet\" href=\"stylesheets/input_ask.css\" />\n"+
					"</head><body>No '"+SERVICE_SPEC_PARAM+"' parameter (specifying " +
			"the service/port/operation/action/use) was specified in the POST request</body></html>");
			return;
		}
		String[] serviceSpecs = serviceSpec.split(" ");
		if(serviceSpecs.length != 10){
			out.print("<html><head><title>Error</title>\n"+
					"<link type=\"text/css\" rel=\"stylesheet\" href=\"stylesheets/input_ask.css\" />\n"+
					"</head><body>The '"+SERVICE_SPEC_PARAM+"' parameter (specifying " +
			"the service/port/operation/action/use) did not contain 10 space-separated values as expected</body></html>");
			return;
		}

		QName serviceQName = new QName(serviceSpecs[0], serviceSpecs[1]);	
		Service service = null;
		try{
			service = Service.create(url, serviceQName);
		} catch(Exception e){
			out.print("<html><head><title>Error</title>\n"+
					"<link type=\"text/css\" rel=\"stylesheet\" href=\"stylesheets/input_ask.css\" />\n"+
					"</head><body>" + e.getClass().getName() + " while using JAX-WS to create a handle for " +
					"the service " + serviceQName + 
			", either the WSDL or the expected service name is wrong<br/><pre>");
			e.printStackTrace(out);
			out.print("</pre></body></html>");
			return;
		}

		QName portQName = new QName(serviceSpecs[2], serviceSpecs[3]);
		Dispatch<Source> dispatch = null;
		try{
			dispatch = service.createDispatch(portQName,
					Source.class,
					Service.Mode.PAYLOAD);
		} catch(Exception e){
			out.print("<html><head><title>Error</title>\n"+
					"<link type=\"text/css\" rel=\"stylesheet\" href=\"stylesheets/input_ask.css\" />\n"+
					"</head><body>" +
					e.getClass().getName() + " while using JAX-WS to create a dispatch for a port on " +
					"the service " + serviceQName + ", either the WSDL or the WSDLConfig's " +
					"portQName parsed (" + portQName + ") is wrong:<pre>");	    
			e.printStackTrace(out);
			out.print("</pre></body></html>");
			return;
		}

		String messageNSURI = serviceSpecs[4];
		String messageName = serviceSpecs[5];
		String soapAction = serviceSpecs[6];
		String opName = serviceSpecs[7];
		String style = serviceSpecs[8];  // doc or rpc
		String use = serviceSpecs[9];  // literal or encoded

		QName qName = "rpc".equals(style) ? 
				new QName(messageNSURI, opName) : 
					new QName(messageNSURI, messageName);

				SourceMap source = new SourceMap(qName, use);
				// Populate the input data
				for (Enumeration e = request.getParameterNames(); e.hasMoreElements();){
					String paramName = (String) e.nextElement();
					if(SERVICE_SPEC_PARAM.equals(paramName) || 
							SRC_PARAM.equals(paramName) || 
							ID_PARAM.equals(paramName)){
						continue;
					}
					if(paramName.startsWith(ARRAY_TYPE_SENTINEL+":")){
						continue;
					}
					//System.err.println("Checking param "+paramName);
					String value = request.getParameter(paramName);
					if(paramName.endsWith(":opt")){
						if(value == null || value.trim().length() == 0){
							continue;
						}
						paramName = paramName.substring(0, paramName.length()-4);
					}
					if(value == null){
						continue;
					}
					String delimiter = request.getParameter(ARRAY_TYPE_SENTINEL+":"+paramName);
					if(delimiter != null && delimiter.trim().length() != 0){
						String[] values = null;

						if(delimiter.equals(DQUOTE_OPTION) || delimiter.equals(SQUOTE_OPTION)){
							value = value.trim();
							if(delimiter.equals(DQUOTE_OPTION) && !value.startsWith("\"")){
								out.print("<html><head><title>Error</title>\n"+
										"<link type=\"text/css\" rel=\"stylesheet\" href=\"stylesheets/input_ask.css\" />\n"+
										"</head><body>" +
										"The delimiter for field " + paramName + " was double quotes" +
								" but the value does not start with a double quote</body></html>");
							}
							if(delimiter.equals(SQUOTE_OPTION) && !value.startsWith("'")){
								out.print("<html><head><title>Error</title>\n"+
										"<link type=\"text/css\" rel=\"stylesheet\" href=\"stylesheets/input_ask.css\" />\n"+
										"</head><body>" +
										"The delimiter for field " + paramName + " was single quotes" +
								" but the value does not start with a single quote</body></html>");
							}
							if(delimiter.equals(DQUOTE_OPTION) && !value.endsWith("\"")){
								out.print("<html><head><title>Error</title>\n"+
										"<link type=\"text/css\" rel=\"stylesheet\" href=\"stylesheets/input_ask.css\" />\n"+
										"</head><body>" +
										"The delimiter for field " + paramName + " was double quotes" +
								" but the value does not end with a double quote</body></html>");
							}
							if(delimiter.equals(SQUOTE_OPTION) && !value.endsWith("'")){
								out.print("<html><head><title>Error</title>\n"+
										"<link type=\"text/css\" rel=\"stylesheet\" href=\"stylesheets/input_ask.css\" />\n"+
										"</head><body>" +
										"The delimiter for field " + paramName + " was single quotes" +
								" but the value does not end with a single quote</body></html>");
							}		    
							if(value.length() < 2){
								continue;  // consider it empty
							}
							else if(value.length() == 2){
								values = new String[]{""};
							}
							else{
								String d = delimiter.equals(DQUOTE_OPTION) ? "\"" : "'";
								values = value.substring(1, value.length()-2).split(d+".*?"+d);
							}
						}

						else{
							String regex = null;
							if(delimiter.equals(WHITESPACE_OPTION)){
								regex = "\\s+";
							}
							else if(delimiter.equals(COMMA_OPTION)){
								regex = ",";
							}
							else if(delimiter.equals(NEW_LINE_OPTION)){
								regex = "\\r?\\n";
							}
							else if(delimiter.equals(TAB_OPTION)){
								regex = "\\t";
							}
							else if(delimiter.equals(COLON_OPTION)){
								regex = ":";
							}
							else if(delimiter.equals(SEMICOLON_OPTION)){
								regex = ";";
							}
							else if(delimiter.equals(SLASH_OPTION)){
								regex = "/";
							}
							else{
								regex = delimiter; // take as-is
							}
							values = value.split(regex);
						}
						//System.err.println("encoding param "+paramName+", values ");
						//for(String v: values){
						//    System.err.println("\""+v+"\"");
						//}
						source.put(paramName, values);		
					}
					else{  // single value
						//System.err.println("encoding param "+paramName+", value \""+value+"\"");
						source.put(paramName, value);
					}
				}

				if(recorder != null){  //PBE needs to know what the input looked like
					recorder.setInputSource(request, source);
				}

				// Some servers need the soap action set to know what method to invoke
				if(soapAction != null && soapAction.length() != 0){
					Map<String,Object> context = dispatch.getRequestContext();	
					context.put(Dispatch.SOAPACTION_USE_PROPERTY, Boolean.TRUE);
					context.put(Dispatch.SOAPACTION_URI_PROPERTY, soapAction);
				}

				// Call the service
				Source resultSource = dispatch.invoke(source);

				String answer = null;
				// let the recorder do whatever is necessary for semantically wrapping the service
				if(WRAP_SEMANTICALLY && recorder != null){
					try{
						answer = recorder.markupResponse(resultSource, request);
					} catch(Exception e){
						//todo
						answer = "<pre>Exception in DataRecorder subsystem:\n"+e.toString()+"\n";
						for(StackTraceElement ste: e.getStackTrace()){
							answer += ste.toString()+"\n";
						}	    
						answer += "</pre>";
					}
				}
				else{
					// Does two duties: fixes indentation, and outputs to a Java stream we can print easily
					ByteArrayOutputStream stringResult = new ByteArrayOutputStream();
					try{
						Transformer responseTransformer = getResponseTransformer();
						synchronized(responseTransformer){
							responseTransformer.transform(resultSource, 
									new javax.xml.transform.stream.StreamResult(stringResult));
						}
					} catch(Exception e){
						out.print("<html><head><title>Error</title>\n"+
								"<link type=\"text/css\" rel=\"stylesheet\" href=\"stylesheets/input_ask.css\" />\n"+
								"</head><body>" +
								e.getClass().getName() + " while transforming response from " +
								"the service " + serviceQName + " (probably an internal error):<pre>");	    
						e.printStackTrace(out);
						out.print("</pre></body></html>");
						return;
					}

					answer=stringResult.toString();
					String[] lines=answer.split(">");
					String answer2=lines[0]+">\n";
					answer2=answer2+"<?xml-stylesheet type=\"text/xsl\" href=\"/xsl/soap_servlet.xsl\"?>\n";
					answer2=answer2+"<root>\n";
					answer2=answer2+"<output>\n";
					for(int i=1;i<lines.length;i++){
						answer2=answer2+lines[i]+">";
					}
					answer2=answer2+"</output>\n";
					answer2=answer2+"<input>\n";
					answer2=answer2+source.toString();
					answer2=answer2+"</input>\n";
					answer2=answer2+"</root>\n";
					answer=answer2;
				}
				out.print(answer);

				//				out.print("<html><head><title>Service Response</title>\n"+
				//						"<link type=\"text/css\" rel=\"stylesheet\" href=\"stylesheets/wsdl_result.css\" /></head>\n" +
				//						"<body>"+answer+"</body></html>");
	}

	// Asks for the WSDL file
	private void writeInputForm(HttpServletRequest request,
			HttpServletResponse response,
			PrintStream out){
		out.print(generateHtmlTopBit(null));
		out.print("Enter the URL of the WSDL file below: <form action=''><input name='");
		out.print(SRC_PARAM+"' type='text' size='50'/>");
		out.print("</form>");
		out.print(generateHtmlBottomBit());
	}

	// Presents the WSDL file as a CGI form
	protected void writeServiceForm(HttpServletRequest request,
			HttpServletResponse response,
			URL url,
			PrintStream out,String operationName){

		// Normal web-browser form fill-in, ask for the WSDL to wrap
		// Useful so this servlet can standalone and borker WSDL services.
		if(url == null){
			writeInputForm(request, response, out);
			return;
		}

		try{
			out.print(generateHtmlTopBit("Input interface for WSDL Services at " + url));
			
			
			
			if(recorder != null){
				out.print(recorder.getHead(request));
			}
			// allow the recorder to insert any body events required
			out.print("</head>\n<body " + (recorder==null?"":recorder.getBodyAttrs(request)) + ">\n");
			if(recorder != null){
				out.print(recorder.getBody(request));
			}
			//print disclaimer
			out.print("PLEASE NOTE:<br/>  Your service calls might be logged and used for analysis and/or annotation of services in the future.<br/> Therefore do not use any secure or confidential data here.<br/> ");
			
			// Use JAX-WS to get the service and port list
			// Verify if the service info was actually parsed properly by trying to use it with JAX-WS

			Document wsdlDoc = docBuilder.parse(url.openStream());

			// Before we do anything else, let's inplace edit the DOM for any import or include statements
			doImports(wsdlDoc, url);

			NodeList serviceElements = wsdlDoc.getDocumentElement().getElementsByTagNameNS("http://schemas.xmlsoap.org/wsdl/", 
			"service");

			if(serviceElements == null || serviceElements.getLength() == 0){
				out.print("Could not find any service elements in the WSDL namespace (" +
						"http://schemas.xmlsoap.org/wsdl/) in the given WSDL file (" +
						url + ")");
				out.print(generateHtmlBottomBit());
				return;
			}

			int anonCount = 0; // used to generate unique names for anonymously declared parts of the WSDL def
			for(int i = 0; i < serviceElements.getLength(); i++){
				Element serviceElement = (Element) serviceElements.item(i);
				String serviceName = serviceElement.getAttribute("name");
				if(serviceName == null || serviceName.trim().length() == 0){
					out.print("A service element in the WSDL file (" +
							url + ") did not have a 'name' attribute.");
					out.print(generateHtmlBottomBit());
					return;
				}

				String serviceNamespaceURI = null;
				for(Node element = serviceElement; 
				element != null && element instanceof Element; 
				element = serviceElement.getParentNode()){

					String tns = ((Element) element).getAttribute("targetNamespace");
					if(tns != null && tns.trim().length() > 0){
						serviceNamespaceURI = tns;
						break;
					}
				}
				if(serviceNamespaceURI == null){
					out.print("A target namespace declaration (targetNamespace attribute) " +
							"at or above the service element in the WSDL file (" +
							url + ") could not be found.");
					out.print(generateHtmlBottomBit());
					return;
				}

				QName serviceQName = new QName(serviceNamespaceURI, serviceName);

				Service service = null;
				try{
					service = Service.create(url, serviceQName);
				} catch(Exception e){
					out.print(e.getClass().getName() + " while using JAX-WS to create a handle for " +
							"the service " + serviceQName + 
					", either the WSDL or the expected service name is wrong<br/><pre>");
					e.printStackTrace(out);
					out.print("</pre>");
					out.print(generateHtmlBottomBit());
					return;
				}
				out.println("<h2>Service " + serviceName + " (namespace " + serviceNamespaceURI + ")</h2>");

				Iterator<QName> portQNames = service.getPorts();
				if(!portQNames.hasNext()){
					continue;
				}
				do{
					QName portQName = portQNames.next();
					out.print("<h3>Port " + portQName.getLocalPart() +
							" (namespace " +  portQName.getNamespaceURI() + ")</h3>");

					// <wsdl:port binding="impl:OntologyQuerySoapBinding" name="OntologyQuery">

					NodeList portElements = 
						wsdlDoc.getDocumentElement().getElementsByTagNameNS("http://schemas.xmlsoap.org/wsdl/", 
						"port");
					for(int j = 0; j < portElements.getLength(); j++){
						Element portElement = (Element) portElements.item(j);
						if(!portQName.getLocalPart().equals(portElement.getAttribute("name"))){
							continue;
						}
						String binding = portElement.getAttribute("binding");
						if(binding == null || binding.trim().length() == 0){
							out.print("Error: Could not find binding attribute for port " + 
									portQName.getLocalPart());
							out.print(generateHtmlBottomBit());
							return;
						}
						if(binding.indexOf(":") != -1){
							binding = binding.substring(binding.indexOf(":")+1); // lop off prefix
						}
						NodeList bindingElements = 
							wsdlDoc.getDocumentElement().getElementsByTagNameNS("http://schemas.xmlsoap.org/wsdl/",
							"binding");

						for(int k = 0; k < bindingElements.getLength(); k++){
							Element bindingElement = (Element) bindingElements.item(k);
							if(!binding.equals(bindingElement.getAttribute("name"))){
								continue;
							}
							String type = bindingElement.getAttribute("type");
							if(type.indexOf(":") != -1){
								type = type.substring(type.indexOf(":")+1); // lop off prefix
							}

							NodeList soapBindings = 
								bindingElement.getElementsByTagNameNS("http://schemas.xmlsoap.org/wsdl/soap/",
								"binding");

							if(soapBindings.getLength() == 0){ // ignore GET, POST, etc. bindings for now
								continue;
							}

							Map<QName,String> op2Action = new LinkedHashMap<QName,String>();
							Map<QName,QName> op2InMsg = new LinkedHashMap<QName,QName>();
							Map<QName,QName> op2OutMsg = new LinkedHashMap<QName,QName>();
							Map<QName,String> msg2Use = new LinkedHashMap<QName,String>();
							Map<QName,Map<String,QName>> msg2Parts = new LinkedHashMap<QName,Map<String,QName>>();
							Map<String,String> part2Type = new LinkedHashMap<String,String>();

							// There is a soap binding!
							String style = null;
							for(int m = 0; m < soapBindings.getLength(); m++){
								Element soapBinding = (Element) soapBindings.item(m);
								if(soapBinding.getAttribute("style") != null){
									style = soapBinding.getAttribute("style");
									break;
								}
							}
							if(style == null){
								out.print("Error: Could not find style of soap binding for " + 
										binding);
								out.print(generateHtmlBottomBit());
								return;
							}

							// read through to get the encoding and soap action
							NodeList ops =
								bindingElement.getElementsByTagNameNS("http://schemas.xmlsoap.org/wsdl/",
								"operation");
							for(int m = 0; m < ops.getLength(); m++){
								Element op = (Element) ops.item(m);
								QName opName = getQName(op.getAttribute("name"), op);
								NodeList soapOps =
									op.getElementsByTagNameNS("http://schemas.xmlsoap.org/wsdl/soap/",
									"operation");
								String soapAction = null;
								for(int n = 0; n < soapOps.getLength(); n++){
									Element soapOp = (Element) soapOps.item(n);
									soapAction = soapOp.getAttribute("soapAction");
									if(soapAction != null && soapAction.trim().length() > 0){					
										break;
									}
								}
								if(soapAction == null){
									out.print("Error: Could not find a soapAction attribute for operation " + 
											opName.getLocalPart()+"(NS "+opName.getNamespaceURI()+")");
									out.print(generateHtmlBottomBit());
									return;
								}
								op2Action.put(opName, soapAction);

								NodeList inputs =
									op.getElementsByTagNameNS("http://schemas.xmlsoap.org/wsdl/",
									"input");
								String inputMsgName = null;
								QName inputMsgQName = null;
								for(int n = 0; n < inputs.getLength(); n++){
									Element input = (Element) inputs.item(n);
									inputMsgName = input.getAttribute("name");

									NodeList soapInputs =
										input.getElementsByTagNameNS("http://schemas.xmlsoap.org/wsdl/soap/",
										"body");
									if(soapInputs == null || soapInputs.getLength() == 0){
										out.print("Error: Could not find a SOAP body definition for operation " + 
												opName.getLocalPart()+"(NS "+opName.getNamespaceURI()+")");
										out.print(generateHtmlBottomBit());
										return;
									}
									Element bodyDef = (Element) soapInputs.item(0);
									String use = bodyDef.getAttribute("use");
									if(use == null || use.trim().length() == 0){
										out.print("Error: Could not find a SOAP body definition " +
												"'use' attribute for input of operation " + 
												opName.getLocalPart()+"(NS "+opName.getNamespaceURI()+")");
										out.print(generateHtmlBottomBit());
										return;
									}
									if(inputMsgName == null || inputMsgName.trim().length() == 0){
										msg2Use.put(opName, use); // applies to all inputs
									}
									else{					
										msg2Use.put(getQName(inputMsgName, input), use);
									}
									// for doc/lit, rpc/enc will come from portType def
									//if(use.equals("literal")){
									//	op2InMsg.put(opName, getQName(inputMsgName, input));  
									//}
								}

								NodeList outputs =
									op.getElementsByTagNameNS("http://schemas.xmlsoap.org/wsdl/",
									"output");
								String outputMsgName = null;
								QName outputMsgQName = null;
								for(int n = 0; n < outputs.getLength(); n++){
									Element output = (Element) outputs.item(n);
									outputMsgName = output.getAttribute("name");
									if(outputMsgName == null || outputMsgName.trim().length() == 0){
										outputMsgName = opName.getLocalPart() + "Response";
									}
									outputMsgQName = getQName(outputMsgName, output);
									NodeList soapOutputs =
										output.getElementsByTagNameNS("http://schemas.xmlsoap.org/wsdl/soap/",
										"body");
									if(soapOutputs == null || soapOutputs.getLength() == 0){
										out.print("Error: Could not find a SOAP body definition for operation " + 
												opName.getLocalPart()+"(NS "+opName.getNamespaceURI()+")");
										out.print(generateHtmlBottomBit());
										return;
									}
									Element bodyDef = (Element) soapOutputs.item(0);
									String use = bodyDef.getAttribute("use");
									if(use == null || use.trim().length() == 0){
										out.print("Error: Could not find a SOAP body definition " +
												"'use' attribute for output of operation " + 
												opName.getLocalPart()+"(NS "+opName.getNamespaceURI()+")n");
										out.print(generateHtmlBottomBit());
										return;
									}
									msg2Use.put(outputMsgQName, use);
								}
								if(outputMsgName != null && outputMsgName.trim().length() != 0){
									op2OutMsg.put(opName, outputMsgQName);  // for doc/lit, rpc/enc will come from portType def
								}			    
							}  // for wsdl:operations
							// Before we can print the service info, we need to collect datatype information

							Map<QName,QName> msg2MsgDef = new LinkedHashMap<QName,QName>();
							NodeList portTypeBindings = 
								wsdlDoc.getElementsByTagNameNS("http://schemas.xmlsoap.org/wsdl/",
								"portType");
							//System.err.println("There are " + portTypeBindings.getLength() + " port type bindings");
							for(int m = 0; m < portTypeBindings.getLength(); m++){
								Element portTypeBinding = (Element) portTypeBindings.item(m);
								if(type.equals(portTypeBinding.getAttribute("name"))){
									//out.print("<h4>SOAP Binding " + binding + "/ portType " + 
									//      type + " / style " + style+"</h4>"); 
									NodeList operations = 
										portTypeBinding.getElementsByTagNameNS("http://schemas.xmlsoap.org/wsdl/",
										"operation");
									//System.err.println("There are " + operations.getLength() + " operation bindings");
									for(int n = 0; n < operations.getLength(); n++){
										Element operation = (Element) operations.item(n);
										QName opName = getQName(operation.getAttribute("name"), operation);

										NodeList inputs =
											operation.getElementsByTagNameNS("http://schemas.xmlsoap.org/wsdl/",
											"input");
										//System.err.println("There are " + inputs.getLength() + " input bindings");
										if(inputs == null || inputs.getLength() == 0){
											out.print("Error: Could not find a WSDL input definition for operation " + 
													opName.getLocalPart()+"(NS "+opName.getNamespaceURI()+")");
											out.print(generateHtmlBottomBit());
											return;
										}
										Element input = (Element) inputs.item(0);
										String inputMessage = input.getAttribute("message");
										if(inputMessage == null || inputMessage.trim().length() == 0){
											out.print("Error: Could not find a WSDL portType input message type for operation " + 
													opName.getLocalPart()+"(NS "+opName.getNamespaceURI()+")");
											out.print(generateHtmlBottomBit());
											return;
										}
										String inputName = input.getAttribute("name");
										String use = msg2Use.get(getQName(inputMessage, input));
										if(use == null){
											// lit/enc may be defined for whole op rather than individual messages
											use = msg2Use.get(opName);  
										}
										if(use != null && use.equals("literal")){
											msg2MsgDef.put(getQName(inputName, input), 
													getQName(inputMessage, input));
										}
										//System.err.println("Recorded op " + opName + " mapping to " + getQName(inputMessage, input));
										op2InMsg.put(opName, getQName(inputMessage, input)); // rpc/enc 

										NodeList outputs =
											operation.getElementsByTagNameNS("http://schemas.xmlsoap.org/wsdl/",
											"output");
										if(outputs == null || outputs.getLength() == 0){
											out.print("Error: Could not find a WSDL output definition for operation " + 
													opName.getLocalPart()+"(NS "+opName.getNamespaceURI()+")");
											out.print(generateHtmlBottomBit());
											return;
										}
										Element output = (Element) outputs.item(0);
										String outputMessage = output.getAttribute("message");
										if(outputMessage == null || outputMessage.trim().length() == 0){
											out.print("Error: Could not find a WSDL portType output message type for operation " + 
													opName.getLocalPart()+"(NS "+opName.getNamespaceURI()+")");
											out.print(generateHtmlBottomBit());
											return;
										}
										String outputName = output.getAttribute("name");
										if(outputName == null || outputName.trim().length() == 0){
											op2OutMsg.put(opName, getQName(outputMessage, output)); // rpc/enc doesn't have a name
										}
										else{
											msg2MsgDef.put(getQName(outputName, output),
													getQName(outputMessage, output));  // for doc/lit
										}
									}
								}
							}  // for wsdl:portType

							// Check the messages to actually populate the forms
							NodeList messages = wsdlDoc.getElementsByTagNameNS("http://schemas.xmlsoap.org/wsdl/",
							"message");
							for(int m = 0; m < messages.getLength(); m++){
								Element message = (Element) messages.item(m);
								String messageName = message.getAttribute("name");
								if(messageName == null || messageName.trim().length() == 0){
									out.print("Error: Could not find message name attribute (message #" + 
											m + " in the WSDL"); 
									return;				    
								}
								QName messageQName = getQName(messageName, message);
								NodeList parts = message.getElementsByTagNameNS("http://schemas.xmlsoap.org/wsdl/",
								"part");
								Map<String,QName> partsMap = new LinkedHashMap<String,QName>();
								for(int n = 0; n < parts.getLength(); n++){
									Element part = (Element) parts.item(n);
									String partName = part.getAttribute("name");
									// type is encoded, literal if element
									String datatype = part.getAttribute("type");
									String element = part.getAttribute("element");
									if(datatype != null && datatype.trim().length() != 0){
										partsMap.put(partName, getQName(datatype, part));  // rpc/encoded
									}
									else if(element != null && element.trim().length() != 0){  
										// it's an element we need to chase down for the types later
										partsMap.put(partName, getQName(element, part));
									}
									else{
										out.print("Error: Could not find either an 'element' for " +
												"'type' attribute for message part " + partName + 
												" of message " + messageName);
										out.print(generateHtmlBottomBit());
										return;
									}
								}
								msg2Parts.put(messageQName, partsMap);
							}

							NodeList types = wsdlDoc.getElementsByTagNameNS("http://schemas.xmlsoap.org/wsdl/",
							"types");  
							Map<QName,Map<String,QName>> element2Members = 
								new LinkedHashMap<QName,Map<String,QName>>();	   
							Map<QName,Map<String,QName>> type2Members = 
								new LinkedHashMap<QName,Map<String,QName>>();	   
							for(int m = 0; m < types.getLength(); m++){
								Element typeTag = (Element) types.item(m);
								MyNodeList typeDefs = new MyNodeList();
								// Get the list of data types under each schema tag
								NodeList typeChildren = typeTag.getChildNodes();
								for(int n = 0; n < typeChildren.getLength(); n++){
									Node typeChild = typeChildren.item(n);
									if(typeChild instanceof Element &&
											"schema".equals(typeChild.getLocalName())){
										typeDefs.add(getTypes((Element) typeChild));
									}
								}

								for(int n = 0; n < typeDefs.getLength(); n++){
									Element element = (Element) typeDefs.item(n); 

									String elementName = element.getAttribute("name");
									if(elementName == null || elementName.trim().length() == 0){
										out.print("Error: Could not find the name attribute for a schema element (#" +
												n + " of type declaration block #" + m);
										out.print(generateHtmlBottomBit());
										return;
									}

									Map<String,QName> memberMap = new LinkedHashMap<String,QName>();

									// It could either be a basic type (handled right here), or a complex one with subfields
									String elementType = element.getAttribute("type");
									if(elementType != null && elementType.trim().length() != 0){
										QName eT = getQName(elementType, element);
										if(eT.getNamespaceURI() == null ||
												eT.getNamespaceURI().equals("http://www.w3.org/2001/XMLSchema")){
											memberMap.put(BASIC_TYPE_SENTINEL, eT);
											memberMap.put(BASIC_NAME_SENTINEL, new QName(null, elementName));
										}
										else{
											// pointer to complex type elsewhere in the schema tag
											memberMap.put(BASIC_TYPE_SENTINEL, getQName(getRef(eT), element));
											memberMap.put(BASIC_NAME_SENTINEL, new QName(null, elementName));
										}
									}

									NodeList subelements = element.getElementsByTagNameNS("http://www.w3.org/2001/XMLSchema",
									"element");
									// If there are no subelements, it could be a simple type such as an enumeration, 
									// the other base condition besides the basic type handled above
									if(subelements.getLength() == 0){
										NodeList restrictions = element.getElementsByTagNameNS("http://www.w3.org/2001/XMLSchema",
										"restriction");
										if(restrictions.getLength() == 0){
											restrictions = element.getElementsByTagNameNS("http://www.w3.org/2001/XMLSchema",
											"extension");
											if(restrictions.getLength() != 0){
												String baseAttr = restrictions.getLength() == 0 ? "xs:string" : 
													((Element) restrictions.item(0)).getAttribute("base");
												QName baseType = getQName(baseAttr, element);
												QName anonQName = getQName(baseAttr+"SubType"+anonCount++, element);
												memberMap.put(BASIC_TYPE_SENTINEL, anonQName);
												memberMap.put(BASIC_NAME_SENTINEL, new QName(null, elementName));
											}
											// else, some time you just get a blank type...
										}
										else{
											// TODO handle extension properly

										}
									}
									for(int p = 0; p < subelements.getLength(); p++){
										Element subelement = (Element) subelements.item(p);
										String subelementRef = subelement.getAttribute("ref");
										String subelementType = subelement.getAttribute("type");
										if(subelementRef != null && subelementRef.trim().length() != 0){
											// dereference
											QName subq = getQName(subelementRef, subelement);
											subelementType = getRef(subq);
										}
										String subelementName = subelement.getAttribute("name");
										if(subelementName == null || subelementName.trim().length() == 0){
											if(subelementRef == null || subelementRef.trim().length() == 0){
												out.print("Error: Could not find the name attribute for a schema subelement (#" +
														p + " of schema element " + elementName);
												out.print(generateHtmlBottomBit());
												return;
											}
											else{
												subelementName = subelementRef;
											}
										}
										if(subelementType == null || subelementType.trim().length() == 0){
											// Last ditch, see if the type is declared directly and anonymously
											// in the subelement declaration.  If so, generate a new datatype
											// that'll be added to the parsing list being processed in this loop...
											NodeList exts = getTypes(subelement);
											if(exts.getLength() == 1){
												// Assume the first complexType tag is the top level one
												QName anonQName = getQName("anonymousType"+anonCount++, element);
												subelementType = getRef(anonQName);
												Element anonTypeDef = (Element) exts.item(0).cloneNode(true); //true = deep copy
												anonTypeDef.setAttribute("name", anonQName.getLocalPart());
												// Now insert the anonymous type into the parse tree and types list
												// so it is resolved correctly with QName and all
												element.getParentNode().appendChild(anonTypeDef);
												typeDefs.add(anonTypeDef);
												//System.err.println("Created data type " + anonQName + " for subelement " + 
												//		   subelementName + " of element " + elementName);
											}
											else{
												out.print("Error: Could not find the type attribute for a schema subelement (#" +
														p + " of schema element " + elementName + "), found " + 
														exts.getLength() + " candidates");
												out.print(generateHtmlBottomBit());
												return;
											}
										}

										// Check for array types, doc/lit
										String maxOccurs = subelement.getAttribute("maxOccurs");
										if("unbounded".equals(maxOccurs)){
											subelementType = subelementType + "[]"; //[] notes the array type
										}
										String minOccurs = subelement.getAttribute("minOccurs");
										if("0".equals(minOccurs)){
											// QName does not check for XML spec's NCName compatability, so '~' is safe
											subelementType = subelementType + "~";
											//System.err.println("Made "+subelementName+" optional");
										}
										memberMap.put(subelementName, getQName(subelementType, subelement));
									}
									// Check for array types, rpc/encoded 
									// e.g. <xsd:attribute ref="soapenc:arrayType" wsdl:arrayType="typens:Subtype[]"/>
									NodeList attrElements = element.getElementsByTagNameNS("http://www.w3.org/2001/XMLSchema",
									"attribute");
									for(int p = 0; attrElements != null && p < attrElements.getLength(); p++){
										Element attrElement = (Element) attrElements.item(p);
										String wsdlArrayType = attrElement.getAttributeNS("http://schemas.xmlsoap.org/wsdl/",
										"arrayType");
										if(wsdlArrayType != null && wsdlArrayType.trim().length() > 0){
											memberMap.put(ARRAY_TYPE_SENTINEL, getQName(wsdlArrayType, attrElement));
										}
									}

									if(element.getLocalName().equals("element")){					
										element2Members.put(getQName(elementName, element), memberMap);
									}
									else{ //probably complexType def
										type2Members.put(getQName(elementName, element), memberMap);
									}
								}  //end for <schema><element|complexType>...
							}  //for wsdl:types

							// Now we have enough info to print forms for the operations
							//System.err.println("About to print services");
							for(QName opQName: op2InMsg.keySet()){
								if (operationName==null || opQName.getLocalPart().equals(operationName)){
									//System.err.println("printing service for " + opQName);
									QName messageQName = op2InMsg.get(opQName);
									if(messageQName == null){
										System.err.println("Got null value for message of " + opQName);
										continue;
									}
									// set the encapsulating element to the operation name if rpc, or element name if doc
									String actionSpec = serviceQName.getNamespaceURI()+" "+serviceQName.getLocalPart()+" "+
									portQName.getNamespaceURI() + " " + portQName.getLocalPart() + " ";
									out.print("<a name='" + opQName.getLocalPart() + "'></a><div class='operation'><h3>"+
											opQName.getLocalPart()+"</h3><form action='' method='post'>\n"+
											"<input type='hidden' name='"+SRC_PARAM+"' value='"+url+"'/>\n");
									Map<String,QName> partsMap = msg2Parts.get(messageQName);
									if(partsMap == null){
										System.err.println("Got null parts map for message " + messageQName);
										continue;
									} 
									boolean isRpcStyle = "rpc".equals(style);

									String use = msg2Use.get(messageQName);
									if(use == null){
										use = msg2Use.get(opQName);
									}

									for(Map.Entry<String,QName> part: partsMap.entrySet()){
										// doc style, should be only one part
										if(!isRpcStyle){ // assume document style
											QName dataType = part.getValue();
											actionSpec = actionSpec + dataType.getNamespaceURI()+ " " +
											dataType.getLocalPart();
											Map<String,QName> subpartsMap = element2Members.get(dataType);
											if(subpartsMap == null){
												if(dataType.getNamespaceURI().equals("http://schemas.xmlsoap.org/soap/encoding/")){
													writeDataType(out, part.getKey(),
															part.getValue(), element2Members, type2Members, "");
												}
												else{
													out.print("Error: cannot find definition for data type " + 
															dataType + "\nValid types are:");
													for(QName key: element2Members.keySet()){
														out.print(" "+key);
													}
												}
												continue;
											}
											if(subpartsMap.containsKey(BASIC_TYPE_SENTINEL)){
												QName t = subpartsMap.get(BASIC_TYPE_SENTINEL);
												while(DEFERRED_NAMESPACE_URI.equals(t.getNamespaceURI())){
													String[] p = t.getLocalPart().split("_deferred_");
													t = new QName(decode(p[0]), p[1]);
												}
												if(t.getNamespaceURI().equals("http://www.w3.org/2001/XMLSchema")){
													throw new Exception("Got bare XSD type as contents of WSDL message");
												}
												subpartsMap = type2Members.get(t);
											}
											for(Map.Entry<String,QName> subpart: subpartsMap.entrySet()){
												writeDataType(out, subpart.getKey(), subpart.getValue(), type2Members, type2Members, "");
											}
										}
										else{ // rpc style
											writeDataType(out, part.getKey(), part.getValue(), element2Members, type2Members, "");
										}
									}  // for parts
									// special condition for rpc calls, still need to give the op ns & name
									if(isRpcStyle){
										actionSpec = actionSpec + opQName.getNamespaceURI() + " " + opQName.getLocalPart();
									}
									actionSpec = actionSpec + " " + op2Action.get(opQName) + " " + 
									opQName.getLocalPart() + " " + style + " " + use;
									out.print("<input type='hidden' name='"+SERVICE_SPEC_PARAM+"' value='"+actionSpec+"'/>");
									out.print("<input type='submit' value='Execute service'"+sub()+"/></form>\n");
								}  // for ops
							}
						} // for wsdl:binding    
					}  //for wsdl port
				}while(portQNames.hasNext());
				// for jax-ws ports
			} // for services

			out.print(generateHtmlBottomBit());
		}
		catch(java.io.IOException ioe){
			logger.log(Level.SEVERE, "While printing HTML form to servlet output stream", ioe);
			ioe.printStackTrace();
			return;
		}
		catch(org.xml.sax.SAXException saxe){
			logger.log(Level.SEVERE, "While parsing WSDL URL "+url, saxe);
			saxe.printStackTrace();
			return;
		}
		catch(Exception e){
			logger.log(Level.SEVERE, "While compiling WSDL URL "+url, e);
			e.printStackTrace();
			return;
		}
	}

	// Find all declarations immediately below the given element if they are of the form element, simpleType or complexType
	private MyNodeList getTypes(Element parent){
		MyNodeList nl = new MyNodeList();
		NodeList candidates = parent.getElementsByTagNameNS("http://www.w3.org/2001/XMLSchema",
		"complexType");
		for(int i = 0; i < candidates.getLength(); i++){
			// Immediate descendent
			Node candidate = candidates.item(i);
			if(candidate.getParentNode() == parent){
				nl.add(candidate);
			}
		}
		candidates = parent.getElementsByTagNameNS("http://www.w3.org/2001/XMLSchema",
		"simpleType");
		for(int i = 0; i < candidates.getLength(); i++){
			// Immediate descendent
			Node candidate = candidates.item(i);
			if(candidate.getParentNode() == parent){
				nl.add(candidate);
			}
		}
		candidates = parent.getElementsByTagNameNS("http://www.w3.org/2001/XMLSchema",
		"element");
		for(int i = 0; i < candidates.getLength(); i++){
			// Immediate descendent
			Node candidate = candidates.item(i);
			if(candidate.getParentNode() == parent){
				nl.add(candidate);
			}
		}
		return nl;
	}

	// Use to reference another qname, for dereferencing later
	private String getRef(QName qname){
		return "deferred:"+encode(qname.getNamespaceURI())+
		"_deferred_"+qname.getLocalPart();
	}

	private QName getQName(String typeName, Node contextNode) throws Exception{
		if(typeName.indexOf(":") != -1){
			String p[] = typeName.split(":");
			if(p.length != 2){
				throw new Exception("Error: type attribute value '" +
						typeName+"' does not have the expected 'ns:value' form.");
			}
			String tns = null;
			if(p[0].equals("deferred")){
				tns = DEFERRED_NAMESPACE_URI;
			}
			else{
				tns = contextNode.lookupNamespaceURI(p[0]);
			}
			if(p[0].equals("tns")){  // sometimes the tns prefix is not declared, but used anyway
				return getTargetNSQName(p[1], contextNode);
			}
			else{
				return new QName(tns, p[1]);
			}
		}
		else{
			return getTargetNSQName(typeName, contextNode);
		}
	}

	private QName getTargetNSQName(String typeName, Node contextNode){
		// See if targetNamespace is defined in an ancestor element, otherwise the NS will be null
		String targetNSURI = null;
		for(Element ancestor = (Element) contextNode; ancestor != null; ancestor = (Element) ancestor.getParentNode()){
			String tns = ancestor.getAttribute("targetNamespace");
			if(tns != null && tns.trim().length() != 0){
				targetNSURI = tns;
				break;
			}
		}
		return new QName(targetNSURI, typeName);
	}

	/**
	 * Make the url safe for use as a datatype, i.e. get rid of ':'
	 */
	private String encode(String q){
		return q.replaceAll(":", "cOL_On");
	}

	private String decode(String q){
		return q.replaceAll("cOL_On", ":");
	}

	private void writeDataType(java.io.PrintStream out, String memberName, QName dataType, 
			Map<QName,Map<String,QName>> msg2Parts, 
			Map<QName,Map<String,QName>> type2Parts, String prefix){

		String datatype = dataType.getLocalPart();

		// check if optional
		boolean isOptional = false;
		if(datatype.endsWith("~")){
			//System.err.println("Printing optional "+memberName+" with type" + datatype);
			isOptional = true;
			datatype = datatype.substring(0, datatype.length()-1);
			dataType = new QName(dataType.getNamespaceURI(), datatype);
		}

		// check if it's a reference
		while(DEFERRED_NAMESPACE_URI.equals(dataType.getNamespaceURI())){
			String[] p = dataType.getLocalPart().split("_deferred_");
			dataType = new QName(decode(p[0]), p[1]);
		}

		datatype = dataType.getLocalPart();

		// check if its an array
		boolean isArray = false;
		if(datatype.endsWith("[]")){
			isArray = true;
			datatype = datatype.substring(0, datatype.length()-2);
			dataType = new QName(dataType.getNamespaceURI(), datatype);
		}

		if("http://www.w3.org/2001/XMLSchema".equals(dataType.getNamespaceURI()) ||
				"http://schemas.xmlsoap.org/soap/encoding/".equals(dataType.getNamespaceURI())){
			if(!isArray){
				if(memberName.equals(ARRAY_TYPE_SENTINEL)){
					memberName = ""; // composite is an array, ignore the sentinel name
				}
				if(datatype.equals("string")){
					out.print(memberName+" ("+(isOptional?"optional, ":"")+
							"string): " + "<textarea cols=\"50\" id=\"data_field\" name='"+prefix+memberName+(isOptional?":opt":"")+"' rows=\"3\" "+rec()+"></textarea>");
					//							"<input type='text' name='"+prefix+memberName+(isOptional?":opt":"")+"' size='30'"+rec()+"/>\n");
				}
				else if(datatype.equals("int")){
					out.print(memberName+" ("+(isOptional?"optional, ":"")+
							"integer): <input type='text' name='"+prefix+memberName+(isOptional?":opt":"")+"' size='10'"+rec()+"/>\n");
				}
				else if(datatype.equals("double")){
					out.print(memberName+" ("+(isOptional?"optional, ":"")+
							"double): <input type='text' name='"+prefix+memberName+(isOptional?":opt":"")+"' size='10'"+rec()+"/>\n");
				}
				else if(datatype.equals("float")){
					out.print(memberName+" ("+(isOptional?"optional, ":"")+
							"float): <input type='text' name='"+prefix+memberName+(isOptional?":opt":"")+"' size='10'"+rec()+"/>\n");
				}
				else if(datatype.equals("boolean")){
					out.print(memberName+"<select name='"+prefix+memberName+"'"+rec()+"><option>false</option><option>true</option></select>\n");
				}
				else{
					out.print(memberName+" ("+(isOptional?"optional, ":"")+
							datatype+"): <input type='text' name='"+prefix+memberName+(isOptional?":opt":"")+"' size='30'"+rec()+"/>\n");
				}
				// todo: deal with string enum
			}
			else{  //isArray
				out.print("One or more...");
				if(memberName.equals(ARRAY_TYPE_SENTINEL)){
					memberName = ""; // composite is an array, ignore the sentinel name
					if(prefix.endsWith(":")){ // get rid of blank prefix in this instance
						prefix = prefix.substring(0, prefix.length()-1);
					}
				}
				if(datatype.equals("string")){
					out.print(memberName+" ("+(isOptional?"optional, ":"")+
							"string): <textarea name='"+prefix+memberName+(isOptional?":opt":"")+
							"' cols='30' rows='4'"+rec()+"></textarea>\n");
				}
				else if(datatype.equals("int")){
					out.print(memberName+" ("+(isOptional?"optional, ":"")+
							"integer): <textarea name='"+prefix+memberName+(isOptional?":opt":"")+
							"' cols='20' rows='4'"+rec()+"></textarea>\n");
				}
				else if(datatype.equals("double")){
					out.print(memberName+" ("+(isOptional?"optional, ":"")+
							"double): <textarea name='"+prefix+memberName+(isOptional?":opt":"")+
							"' cols='20' rows='4'"+rec()+"></textarea>\n");
				}
				else if(datatype.equals("float")){
					out.print(memberName+" ("+(isOptional?"optional, ":"")+
							"float): <textarea name='"+prefix+memberName+(isOptional?":opt":"")+
							"' cols='20' rows='4'"+rec()+"></textarea>\n");
				}
				else if(datatype.equals("boolean")){
					out.print(memberName+" ("+(isOptional?"optional, ":"")+
							"true/false): <textarea name='"+prefix+memberName+(isOptional?":opt":"")+
							"' cols='20' rows='4'"+rec()+"></textarea>\n");
				}
				else{
					out.print(memberName+" ("+(isOptional?"optional, ":"")+
							datatype+"): <textarea name='"+prefix+memberName+(isOptional?":opt":"")+
					"' cols='30' rows='4'></textarea>\n");
				}
				out.print("...separated by <select name='"+ARRAY_TYPE_SENTINEL+":"+
						prefix+memberName+"'><option selected='selected'>"+WHITESPACE_OPTION+"</option>"+
						"<option>"+COMMA_OPTION+"</option>" +
						"<option>"+NEW_LINE_OPTION+"</option>"+
						"<option>"+TAB_OPTION+"</option>"+
						"<option>"+COLON_OPTION+"</option>"+
						"<option>"+SEMICOLON_OPTION+"</option>"+
						"<option>"+SLASH_OPTION+"</option>"+
						"<option>"+DQUOTE_OPTION+"</option>"+
						"<option>"+SQUOTE_OPTION+"</option></select>");
			}
			out.print("<br/>\n");
		}
		else{  // doc/lit or complex rpc type
			Map<String,QName> subparts = msg2Parts.get(dataType);
			if(subparts == null){
				subparts = type2Parts.get(dataType);
				if(subparts == null){
					out.print("Error: cannot find definition for data type " + dataType); //+"\nValid types are:");
					// 	for(QName key: msg2Parts.keySet()){
					// 		    out.print(" "+key);
					// 		}
					return;
				}
			}
			else if(subparts.containsKey(BASIC_TYPE_SENTINEL)){
				QName typeName = subparts.get(BASIC_TYPE_SENTINEL);
				if(isArray){
					typeName = new QName(typeName.getNamespaceURI(), typeName.getLocalPart()+"[]");
				}
				if(isOptional){
					typeName = new QName(typeName.getNamespaceURI(), typeName.getLocalPart()+"~");
				}
				writeDataType(out, memberName, typeName, type2Parts, type2Parts, prefix);
			}
			else{
				// currently we don't handle arrays of composites in the form...
				out.print("<table border=\"1\"><tr bgcolor=\"#DDDDDD\"><td>Composite "+ memberName +
						" ("+(isOptional?"optional, ":"")+ datatype + "):</td></tr><tr><td>");
				for(Map.Entry<String,QName> subpart: subparts.entrySet()){
					writeDataType(out, subpart.getKey(), subpart.getValue(), type2Parts, type2Parts, prefix+memberName+":");
				}
				out.print("</td></tr></table>\n");
			}
		}

	}

	/**
	 * Expand any import or include statements in-place.
	 */
	public static void doImports(Document doc, URL baseURL) throws Exception{
		Element docElement = doc.getDocumentElement();
		NodeList schemaNodes = docElement.getElementsByTagNameNS("http://www.w3.org/2001/XMLSchema",
		"schema");
		if("schema".equals(docElement.getLocalName()) &&
				"http://www.w3.org/2001/XMLSchema".equals(docElement.getNamespaceURI())){
			schemaNodes = new MyNodeList();
			((MyNodeList) schemaNodes).add(docElement);
		}
		//System.err.println("Found " + schemaNodes.getLength() + " schema nodes on import pass of "+baseURL);
		boolean DEEP = true;
		for(int i = 0; i < schemaNodes.getLength(); i++){
			Element schemaElement = (Element) schemaNodes.item(i);
			NodeList importNodes = schemaElement.getElementsByTagNameNS("http://www.w3.org/2001/XMLSchema",
			"import");
			if(importNodes.getLength() == 0){
				continue;
			}
			//System.err.println("Found " + importNodes.getLength() + " import nodes in "+baseURL);

			Element importElement = (Element) importNodes.item(0); // assuming one import max per schema element
			String schemaLocation = importElement.getAttribute("schemaLocation");
			if(schemaLocation == null || schemaLocation.length() == 0){
				continue; // blank is actually allowed, but is not informative for us
			}
			URL importURL = new URL(baseURL, schemaLocation);
			Document importDoc = docBuilder.parse(importURL.openStream());
			// recurse as imports may have imports, etc.
			doImports(importDoc, importURL);
			// maybe we should have checked for circular references somehow?

			Element newSchemaElement = importDoc.getDocumentElement();
			if(!"schema".equals(newSchemaElement.getLocalName()) ||
					!"http://www.w3.org/2001/XMLSchema".equals(newSchemaElement.getNamespaceURI())){
				throw new Exception("Don't know how to import an XML Schema file " +
						"without a root 'schema' tag in the XML schema namespace..." +
						" (instead found '" + newSchemaElement.getLocalName() + "' in namespace " +
						newSchemaElement.getNamespaceURI() + ")");
			}

			// todo: should copy namespace from import statement to replaced subtree
			Node dupe = doc.importNode(newSchemaElement, DEEP);
			schemaElement.getParentNode().replaceChild(dupe, schemaElement);
		}

		// refresh, in case an import had an include, etc.
		schemaNodes = docElement.getElementsByTagNameNS("http://www.w3.org/2001/XMLSchema",
		"schema");
		if("schema".equals(docElement.getLocalName()) &&
				"http://www.w3.org/2001/XMLSchema".equals(docElement.getNamespaceURI())){
			schemaNodes = new MyNodeList();
			((MyNodeList) schemaNodes).add(docElement);
		}
		//System.err.println("Found " + schemaNodes.getLength() + " schema nodes on include pass of "+baseURL);
		for(int i = 0; i < schemaNodes.getLength(); i++){
			NodeList inclNodes = ((Element) schemaNodes.item(i)).getElementsByTagNameNS("http://www.w3.org/2001/XMLSchema",
			"include");
			//System.err.println("Found " + inclNodes.getLength() + " include nodes on pass of "+baseURL);
			if(inclNodes.getLength() == 0){
				continue;
			}

			for(int j = 0; j < inclNodes.getLength(); j++){
				Element inclNode = (Element) inclNodes.item(j);
				String schemaLocation = inclNode.getAttribute("schemaLocation");
				if(schemaLocation == null || schemaLocation.length() == 0){
					continue; // blank may allowed, dunno, but is not informative for us anyway
				}
				URL inclURL = new URL(baseURL, schemaLocation);
				Document inclDoc = docBuilder.parse(inclURL.openStream());
				// recurse as imports may have imports, etc.
				doImports(inclDoc, inclURL);
				// maybe we should have checked for circular references somehow?

				// remove the "include" statement, replace it with the schema bits referenced
				Node inclParentNode = inclNode.getParentNode();
				inclParentNode.removeChild(inclNode);
				Element inclRoot = inclDoc.getDocumentElement();
				NodeList newData = inclRoot.getElementsByTagNameNS("http://www.w3.org/2001/XMLSchema",
				"schema");
				if("schema".equals(inclRoot.getLocalName()) &&
						"http://www.w3.org/2001/XMLSchema".equals(inclRoot.getNamespaceURI())){
					newData = new MyNodeList();
					((MyNodeList) newData).add(inclRoot);
				}
				for(int k = 0; k < newData.getLength(); k++){  //should be only one, but we'll do this anyway...
					NodeList schemaParts = ((Element) newData.item(k)).getChildNodes();
					for(int l = 0; l < schemaParts.getLength(); l++){
						Node dupe = doc.importNode(schemaParts.item(l), DEEP);
						inclParentNode.appendChild(dupe);
					}
				}
			}
		}
	}
	
	protected String generateHtmlTopBit(String title) {
		StringBuilder output = new StringBuilder();
		
		output.append("<html>");
		output.append("\n");
		output.append("<head>");
		output.append("\n");
		output.append("<meta http-equiv=\"content-type\" content=\"text/html;charset=UTF-8\" />");
		output.append("\n");
		
		if(title != null && title.length() > 0) {
			output.append("<title>BioCatalogue.org &#45; Generic SOAP Client &#45; " + title + "</title>");
		} else {
			output.append("<title>BioCatalogue.org &#45; Generic SOAP Client</title>");
		}
		output.append("\n");
		
		output.append("<link type=\"text/css\" rel=\"stylesheet\" href=\"/css/base_packaged.css\" />");
		output.append("\n");
		output.append("<link type=\"text/css\" rel=\"stylesheet\" href=\"/css/wsdl_ask.css\" />");
		output.append("\n");
		output.append("<link type=\"text/css\" rel=\"stylesheet\" href=\"/css/input_ask.css\" />");
		output.append("\n");
		
		output.append("<script src=\"/javascripts/boxover.js?\" type=\"text/javascript\"></script>");
		output.append("\n");
		
		output.append("<link rel=\"icon\" href=\"/images/favicon.ico\" type=\"image/x-icon\" />");
		output.append("\n");
		output.append("<link rel=\"shortcut icon\" href=\"/images/favicon.ico\" type=\"image/x-icon\"/>");
		output.append("\n");
		
		output.append("<link href=\"" + SITE_BASE_URL + "/announcements.atom\" rel=\"alternate\" title=\"BioCatalogue.org - Site Announcements\" type=\"application/atom+xml\" />");
		output.append("\n");
		output.append("<link href=\"" + SITE_BASE_URL + "/services.atom\" rel=\"alternate\" title=\"BioCatalogue.org - Latest Services\" type=\"application/atom+xml\" />");
		output.append("\n");
		output.append("<link rel=\"" + SITE_BASE_URL + "/search\" type=\"application/opensearchdescription+xml\" title=\"BioCatalogue Search\" href=\"/opensearch.xml\" />");
		output.append("\n");
		
		output.append("</head>");
		output.append("\n");
		
		output.append("<body>");
		output.append("\n");
		
		output.append("<div id=\"header-region\" class=\"clear-block\"></div>");
		output.append("\n");

		output.append("<div id=\"wrapper\">");
		output.append("\n");

		output.append("<div id=\"container\" class=\"clear-block\">");
		output.append("\n");

		output.append("<div id=\"header\">");
		output.append("\n");
		
		output.append("<div id=\"logo-floater\">");
		output.append("\n");
		output.append("<a href=\"" + SITE_BASE_URL + "\"><img alt=\"www.biocatalogue.org\" src=\"/images/logo_beta_small.png?\" /></a>");
		output.append("\n");
		output.append("</div>");
		output.append("\n");
							
		output.append("<ul class=\"primary-links\">");
		output.append("\n");
		output.append("<li><a href=\"http://www.biocatalogue.org/wiki\" target=\"_blank\">About Us</a></li>");
		output.append("\n");
		output.append("<li class=\"separator\"><a class=\"end\" href=\"" + SITE_BASE_URL + "/contact\">Contact Us</a></li>");
		output.append("\n");
		output.append("</ul>");
		output.append("\n");
		
		output.append("</div>");
		output.append("\n");
		
		output.append("<div id=\"center\"><div id=\"squeeze\"><div class=\"right-corner\"><div class=\"left-corner\">");
		output.append("\n");
		
		output.append("<div id=\"action_bar\">");
		output.append("\n");
		
		output.append("<form action=\"" + SITE_BASE_URL + "/search\" class=\"search_form\" method=\"get\" style=\"display: inline;\">");
		output.append("\n");
		output.append("Search: ");
		output.append("<input id=\"action_bar_search_field\" name=\"q\" size=\"28\" type=\"text\" /> ");
		output.append("<input class=\"search_button\" name=\"commit\" type=\"submit\" value=\"\" /> ");
		output.append("\n");
		output.append("</form>");
		output.append("\n");
		
		output.append("<span id=\"action_links\">");
		output.append("\n");
		output.append("<a href=\"" + SITE_BASE_URL + "\"><span class=\"label\">Home</span></a>");
		output.append("<a href=\"" + SITE_BASE_URL + "/services\"><img alt=\"Service\" src=\"/images/service.png\" style=\"vertical-align: middle; margin-right: 0.4em;\" /><span class=\"label\">Services</span></a>");
		output.append("<a href=\"" + SITE_BASE_URL + "/services/new\"><img alt=\"Add\" src=\"/images/add.png\" style=\"vertical-align: middle; margin-right: 0.4em;\" /><span class=\"label\">Register a Service</span></a>");
		output.append("<a href=\"" + SITE_BASE_URL + "/service_providers\"><img alt=\"Group_gear\" src=\"/images/group_gear.png\" style=\"vertical-align: middle; margin-right: 0.4em;\" /><span class=\"label\">Providers</span></a>");
		output.append("\n");
		output.append("<span style=\"background: transparent url(../images/green_separator.gif) no-repeat left;\">&nbsp;</span>");
		output.append("\n");
		output.append("</span>");
		output.append("\n");

		output.append("<span id=\"search_dropdown_button\"><ul class=\"p7menubar\"><li><a href=\"#\" class=\"trigger\"><img alt=\"dropdown menu\" src=\"/images/dropdown.png\" /></a><ul><li><a href=\"" + SITE_BASE_URL + "/search/by_data\">Search by Data</a></li></ul></li></ul></span>");
		output.append("\n");
		
		output.append("</div>");
		output.append("\n");

		output.append("<div id=\"action_icons\">");
		output.append("\n");
		output.append("<a href=\"" + SITE_BASE_URL + "/services.atom\" target=\"_blank\" title=\"header=[] body=[Subscribe to the &lt;b&gt;Latest Services&lt;/b&gt; feed] cssheader=[boxoverTooltipHeader] cssbody=[boxoverTooltipBody] delay=[200]\"><img alt=\"Feed_icon\" src=\"/images/feed_icon.png\" /></a>");
		output.append("\n");
		output.append("</div>");
		
		output.append("<div id=\"breadcrumbs_bar\"><table><tr><td><ul class=\"breadcrumb_list\"><li><a href=\"" + SITE_BASE_URL + "\">Home</a></li><li> <b>&#187;</b> </li><li><span>Generic SOAP Client</span></li></ul></td>");
		output.append("<td style=\"text-align: right; overflow: hidden;\"><!-- AddThis Button BEGIN --><script type=\"text/javascript\">var addthis_pub = 'biocatalogue';</script><a href=\"http://www.addthis.com/bookmark.php?v=20\" onmouseover=\"return addthis_open(this, '', '[URL]', '[TITLE]')\" onmouseout=\"addthis_close()\" onclick=\"return addthis_sendto()\" title=\"\"><img src=\"https://secure.addthis.com/static/btn/lg-share-en.gif\" width=\"125\" height=\"16\" border=\"0\" alt=\"Bookmark and Share\" /></a><script type=\"text/javascript\" src=\"https://secure.addthis.com/js/200/addthis_widget.js\"></script><!-- AddThis Button END --></td>");
		output.append("</tr></table></div>");
		output.append("\n");
		
		output.append("<div id=\"content\">");
		output.append("\n");
			
		return output.toString();
	}
	
	protected String generateHtmlBottomBit() {
		StringBuilder output = new StringBuilder();
		
		output.append("<html>");
		output.append("\n");
		
		output.append("</div>");        
		output.append("\n");
		
		output.append("<span class=\"clear\"></span>");
		output.append("\n");
		
		output.append("</div></div></div></div> <!-- /.left-corner, /.right-corner, /#squeeze, /#center --> </div> <!-- /container -->");
		output.append("\n");
		
		output.append("<div id=\"footer\">");
		output.append("\n");
		output.append("<div id=\"footer_links\"><ul><li><a href=\"" + SITE_BASE_URL + "/termsofuse\">Terms of use</a></li><li class=\"separator\"><a href=\"http://www.biocatalogue.org/wiki\">About us</a></li>");
		output.append("<li class=\"separator\"><a href=\"" + SITE_BASE_URL + "/contact\">Contact us</a></li>");
		output.append("<li class=\"separator\"><a href=\"http://listserv.manchester.ac.uk/cgi-bin/wa?SUBED1=biocatalogue-friends&A=1\" target=\"_blank\">Join the friends list</a></li></ul>");
		output.append("\n");
		output.append("</div>");
		output.append("\n");
		
		output.append("<span class=\"clear\">&nbsp;</span>");
		output.append("\n");
		
		output.append("<center><div class=\"logos\"><p style=\"font-size: 108%;\"><b>The BioCatalogue is brought to you by:</b></p>");
		output.append("<p><a href=\"http://www.manchester.ac.uk/\" target=\"_blank\"><img alt=\"University of Manchester\" src=\"/images/manchester_logo.png\" /></a>");
		output.append("<a href=\"http://www.ebi.ac.uk/\" target=\"_blank\"><img alt=\"EMBL-EBI\" src=\"/images/ebi_logo.png\" /></a>");
		output.append("<span>and the same people who brought you</span>");
		output.append("<a href=\"http://www.myexperiment.org/\" target=\"_blank\"><img alt=\"myExperiment\" src=\"/images/myexp_logo.png\" /></a>");
		output.append("<a href=\"http://www.taverna.org.uk/\" target=\"_blank\"><img alt=\"Taverna\" src=\"/images/taverna_logo.png\" /></a></p>");
		output.append("</div></center>");
		output.append("\n");
		
		output.append("<p>The BioCatalogue project is funded by the <a target=\"_blank\" href=\"http://www.bbsrc.ac.uk/\">BBSRC</a> (BB/F01046X/1, BB/F010540/1)</p>");
		output.append("<p style=\"margin-top: 1em;\"><a href=\"http://www.bbsrc.ac.uk/\" target=\"_blank\"><img alt=\"BBSRC\" src=\"/images/bbsrc_logo.png\" /></a>	</p>");
		output.append("\n");
		output.append("<p class=\"copyright\">Copyright (c) 2008 - 2009<a href=\"http://www.manchester.ac.uk/\" target=\"_blank\">The University of Manchester</a> and the<a href=\"http://www.ebi.ac.uk/\" target=\"_blank\">European Bioinformatics Institute (EMBL-EBI)</a></p>");
		output.append("\n");

		output.append("</div>");
		output.append("\n");

		output.append("</div> <!-- /wrapper -->");
		output.append("\n");
		output.append("</body>");
		output.append("</html>");
			
		return output.toString();
	}
}
