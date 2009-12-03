<?xml version="1.0" encoding="ISO-8859-1"?>
<xsl:stylesheet version="1.0"
xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
	
<xsl:import href="xls-to-string.xsl"/>

	
  <xsl:template match="/">
		<html>
			<head>
				<title>SOAP results</title>
				<link type="text/css" rel="stylesheet" href="/css/base_packaged.css" />
				<link type="text/css" rel="stylesheet" href="/css/results.css" />
      </head>

			<body>
				<h2>Output</h2>
				<form>
					<textarea cols="100" rows="20" id="data_field" name='Sequence'  readonly="false" >
						<xsl:value-of select="/root/output"/>
					</textarea>
				</form>
        <h2>Original Soap Input Message</h2>
        If there were some &gt; or &lt; signs in the input values, they might be improperly displayed
        <form>
<textarea cols="100" rows="20" id="data_field" name='Sequence'  readonly="false" >&lt;?xml version=&quot;1.0&quot; encoding=&quot;UTF-8&quot;?&gt;
<xsl:for-each select="/root/input/*">
<xsl:call-template name="xml-to-string"/>
</xsl:for-each>
</textarea>
        </form>
        <h2>Original Soap Output Message</h2>
				<br/> If there were some &gt; or &lt; signs in the output values, they might be improperly displayed
        <form>
<textarea cols="100" rows="20" id="data_field" name='Sequence'  readonly="false" >&lt;?xml version=&quot;1.0&quot; encoding=&quot;UTF-8&quot;?&gt;
<xsl:for-each select="/root/output/*">
<xsl:call-template name="xml-to-string"/>
</xsl:for-each>
</textarea>
        </form>
			</body>
		</html>
  </xsl:template>


</xsl:stylesheet>