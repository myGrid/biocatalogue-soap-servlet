<?xml version="1.0" encoding="ISO-8859-1"?>
<xsl:stylesheet version="1.0"
xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
<xsl:template match="/">
	<html>
		<body>
			<h2>Soap Response</h2>
			<xsl:for-each select="/">
				<xsl:call-template name="replace">
					<xsl:with-param name="string" select="."/>
				</xsl:call-template>
			</xsl:for-each>
		</body>
	</html>
</xsl:template>
<xsl:template name="replace">
	<xsl:param name="string"/>
	<xsl:choose>
		<xsl:when test="contains($string,'&#10;')">
			<xsl:value-of select="substring-before($string,'&#10;')"/>
			<br/>
			<xsl:call-template name="replace">
				<xsl:with-param name="string" select="substring-after($string,'&#10;')"/>
			</xsl:call-template>
		</xsl:when>
		<xsl:otherwise>
			<xsl:value-of select="$string"/>
		</xsl:otherwise>
	</xsl:choose>
</xsl:template>
</xsl:stylesheet>