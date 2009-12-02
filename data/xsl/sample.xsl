<?xml version="1.0" encoding="ISO-8859-1"?>
<xsl:stylesheet version="1.0"
xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
	
<xsl:import href="xls-to-string.xsl"/>

	
  <xsl:template match="/">
		<html>
			<body>
				<h2>Soap Response</h2>
				<form>
					<textarea cols="100" rows="20" id="data_field" name='Sequence'  readonly="false" >
						<xsl:value-of select="."/>
					</textarea>
				</form>
        <h2>Original Soap XML</h2>
				Note that xml tag is stripped of and additions xsl stylesheet tag is added
        <form>
          <textarea cols="100" rows="20" id="data_field" name='Sequence'  readonly="false" >    	
            <xsl:call-template name="xml-to-string"/>
          </textarea>
        </form>
			</body>
		</html>
  </xsl:template>


</xsl:stylesheet>