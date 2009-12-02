<?xml version="1.0" encoding="ISO-8859-1"?>
<xsl:stylesheet version="1.0"
xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
<xsl:template match="/">
	<html>
		<body>
			<h2>Soap Response</h2>
			<form>
				<textarea cols="100" rows="20" id="data_field" name='Sequence'  readonly="false" >
					<xsl:for-each select="/">
						<xsl:value-of select="."/>
					</xsl:for-each>
				</textarea>
			</form>
		</body>
	</html>
</xsl:template>


</xsl:stylesheet>