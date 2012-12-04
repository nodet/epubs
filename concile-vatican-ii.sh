#!/bin/bash                
                 
AUTHOR="Vatican"
ABBR="vatican-ii" 
UUID="3de381d5-a71f-4bdf-962a-2bc36abab4d8"
OUTPUT="Concile-Vatican-II.epub"     
      
if [ ! -d target ]
then
  mkdir target                                                            
fi                                                                        
cd target
if [ ! -d batik-1.7 ]
then
  echo "Downloading batik"
  wget http://apache.mirrors.multidist.eu//xmlgraphics/batik/batik-1.7.zip
  jar -xf batik-1.7.zip
fi             
if [ ! -d epubcheck-3.0-RC-1 ]
then
  echo "Downloading epubcheck"
  wget http://epubcheck.googlecode.com/files/epubcheck-3.0-RC-1.zip
  jar -xf epubcheck-3.0-RC-1.zip
fi
if [ ! -d $ABBR ]
then
  mkdir $ABBR
fi 
cd $ABBR 
cat >doc-list.xml <<EOF
<?xml version="1.0"?>
<books>                                 
  <book file='vat-ii_const_19631204_sacrosanctum-concilium_fr.html' type='Constitution' title='Sacrosanctum Concilium'/>
  <book file='vat-ii_const_19641121_lumen-gentium_fr.html' type='Constitution' title='Lumen Gentium'/>
  <book file='vat-ii_const_19651118_dei-verbum_fr.html' type='Constitution' title='Dei Verbum'/>
  <book file='vat-ii_const_19651207_gaudium-et-spes_fr.html' type='Constitution' title='Gaudium et Spes'/>
  <book file='vat-ii_decl_19651028_gravissimum-educationis_fr.html' type='Declaration' title='Gravissimum Educationis'/>
  <book file='vat-ii_decl_19651028_nostra-aetate_fr.html' type='Declaration' title='Nostra Aetate'/>
  <book file='vat-ii_decl_19651207_dignitatis-humanae_fr.html' type='Declaration' title='Dignitatis Humanae'/>
  <book file='vat-ii_decree_19631204_inter-mirifica_fr.html' type='Decret' title='Inter Mirifica'/>
  <book file='vat-ii_decree_19641121_orientalium-ecclesiarum_fr.html' type='Decret' title='Orientalium Ecclesiam'/>
  <book file='vat-ii_decree_19641121_unitatis-redintegratio_fr.html' type='Decret' title='Unitatis Redintegratio'/>
  <book file='vat-ii_decree_19651028_christus-dominus_fr.html' type='Decret' title='Christus Dominus'/>
  <book file='vat-ii_decree_19651028_optatam-totius_fr.html' type='Decret' title='Optatam Totius'/>
  <book file='vat-ii_decree_19651028_perfectae-caritatis_fr.html' type='Decret' title='Perfectae Caritatis'/>
</books>
EOF
if [ ! -d org ]
then
  mkdir org
fi         
cd org
FILES=`sed -e '/file/!d ; s/.*file=.// ; s/. type.*//' ../doc-list.xml`
for FILE in $FILES
do
  if [ ! -e $FILE ]
  then
    echo "Downloading $FILE"
    wget http://www.vatican.va/archive/hist_councils/ii_vatican_council/documents/$FILE
  fi
done
cd ..
if [ ! -d out ]
then
  mkdir out
fi        
echo "preserve-entities: yes" > tidy1.conf 
echo "preserve-entities: no" > tidy2.conf 
echo "input-encoding: latin1" >> tidy2.conf
echo "output-encoding: utf-8" >> tidy2.conf   

cat >quotes.sed <<EOF
#----------------------------------------------------------------------
# proper_quotes_win.sed
# Copyright Stephen Poley.  May be freely used for non-commercial purposes.
# http://www.xs4all.nl/~sbpoley/
#----------------------------------------------------------------------
#
# This sed script changes plain quotation marks in HTML to proper
# typographical quotation marks, and hyphens used as dashes to dash
# characters. It also replaces numeric character references
# with character entity references for consistency.
#
# Windows version: also tidies up the dubious Windows versions of these
# characters (and a few others).
#
# Any quotes which must not be changed can be represented by &#39; (single) or
# &#34; or &quot; (double).
#
# A few characters will be left unchanged in situations where it is hard to
# be sure if a left or right quotation mark is appropriate.
#
# Running a file through this script multiple times should produce the same
# result as running it once; it doesn't matter if a file has already been fully
# or partially converted to proper quotation marks.
#
# Restrictions:
# 1) Doesn't recognise multi-line HTML comments, so any quotes in such
#    comments will also be converted.
# 2) Within the body, can't handle HTML attributes which span a line break
#    (these are probably rare and arguably bad practice anyway).
# 3) Within the head, HTML attributes spanning a line break (Meta elements)
#    are handled OK provided the <HEAD> and </HEAD> tags are present.
#
# Last updates: 
#   1-4-2006, handle digit-quote-punctuation sequence
#  14-4-2006, handle some extra cases
#----------------------------------------------------------------------

# skip DOCTYPE
/<![Dd][Oo][Cc]/n

# Handle HEAD. 'h' in hold space signifies processing head element
/<[Hh][Ee][Aa][Dd]>/ {
x
s/^/h/
x
}

/<\/[Hh][Ee][Aa][Dd]>/ {
x
s/h//
x
}

x
/h/ {
x
b endscr
}
x

# replace any degree characters, so can use them temporarily
s/°/\&deg;/g

# temporarily replace quotes in attributes and comments
s/="\([^"]*\)"/=°\1°/g
: commdq
s/\(<!--[^>]*\)"\([^>]*>\)/\1°\2/g
t commdq

# update real quotes
s/"\([A-Za-z]\)/\&ldquo;\1/g
s/\([A-Za-z]\)"/\1\&rdquo;/g
s/ "\([^ ]\)/ \&ldquo;\1/g
s/^"\([^ ]\)/\&ldquo;\1/
s/\([^ ]\)" /\1\&rdquo; /g
s/\([^ ]\)"$/\1\&rdquo;/
# digit-quote-punctuation sequence
s/\([0-9]\)"\([;,:.]\)/\1\&rdquo;\2/g
# sentence-end - quote - tag
s/\([.?!]\)"</\1\&rdquo;</g
# sentence-end - quote - closing parenthesis
s/\([.?!]\)")/\1\&rdquo;)/g
# closing parenthesis - quote - sentence-end
s/)"\([.?!]\)/)\&rdquo;\1/g

# put attribute quotes back
s/°/"/g

# now repeat for single quotes. Remember right single quote is also an
# apostrophe, so do right quotes first.
s/='\([^']*\)'/=°\1°/g
: commsq
s/\(<!--[^>]*\)'\([^>]*>\)/\1°\2/g
t commsq
s/\([A-Za-z]\)'/\1\&rsquo;/g
s/'\([A-Za-z]\)/\&lsquo;\1/g
s/\([^ ]\)' /\1\&rsquo; /g
s/\([^ ]\)'$/\1\&rsquo;/
s/ '\([^ ]\)/ \&lsquo;\1/g
s/^'\([^ ]\)/\&lsquo;\1/
s/\([0-9]\)'\([;,:.]\)/\1\&rsquo;\2/g
s/\([.?!]\)'</\1\&rsquo;</g
s/\([.?!]\)')/\1\&rsquo;)/g
s/)'\([.?!]\)/)\&rsquo;\1/g

s/°/'/g

# dashes:
# - replace a hyphen surrounded by spaces with an en-dash;
# - replace a double hyphen which isn't part of an HTML comment delimiter
#   with an em-dash.
s/ - / \&ndash; /g
s/\([^-!]\)[ ]*--[ ]*\([^->]\)/\1\&mdash;\2/g

# Mend the commoner dubious Windows characters
# (range 128-159 - these are undefined in HTML)
s/“/\&ldquo;/g
s/”/\&rdquo;/g
s/‘/\&lsquo;/g
s/’/\&rsquo;/g
s/–/\&ndash;/g
s/—/\&mdash;/g

s/€/\&euro;/g
s/…/\&hellip;/g
s/˜/\&tilde;/g
s/†/\&dagger;/g

# And finally tidy up any numerical character references for consistency
s/\&#8211;/\&ndash;/g
s/\&#8212;/\&mdash;/g
s/\&#8216;/\&lsquo;/g
s/\&#8217;/\&rsquo;/g
s/\&#8220;/\&ldquo;/g
s/\&#8221;/\&rdquo;/g

: endscr
EOF

cd out
cp ../org/*.html .

echo "Creating index"
cat >index.xml <<EOF
<?xml version="1.0"?>
<books>
EOF
sed -e '/file/!d' ../doc-list.xml >> index.xml
cat >>index.xml <<EOF
</books>
EOF
cat >../doc-index.xsl <<"EOF"
<?xml version="1.0"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
<xsl:output encoding="UTF-8"/>
<xsl:key name="type" match="///book" use="@type"/>	
<xsl:template match="/">
  <html>              
	<head>
	  <meta http-equiv="Content-Type" content="text/html; charset=iso-8859-1" />
	  <title>Concile Vatican II</title>
	  <link href="../stylesheet.css" type="text/css" rel="stylesheet"/>
	  <link href="../page_styles.css" type="text/css" rel="stylesheet"/>
	</head>
  <body>
    <h2>Concile Vatican II</h2>
    <xsl:for-each select="//book">
      <xsl:variable name="type" select="@type"/>
      <xsl:variable name="senior" select="key('type', $type)[1]"/>
	  <xsl:if test="$senior/@file = @file">
		<h3><xsl:value-of select="@type"/></h3>
		<ul> 
			<xsl:for-each select="//book[@type = $type]">
			  <li><xsl:element name="a">
				<xsl:attribute name="href">
					<xsl:value-of select="@file"/>
				</xsl:attribute>
				<xsl:value-of select="@title"/>
			  </xsl:element></li>
			</xsl:for-each>
		</ul>
	  </xsl:if>
	</xsl:for-each>
  </body>
  </html>
</xsl:template>                             
</xsl:stylesheet>
EOF
xsltproc ../doc-index.xsl index.xml > index.html 2> /dev/null
rm index.xml

for FILE in *.html
do           
  echo "Processing $FILE"
  perl -e '
      # Load file
      $file = $ARGV[0];  local( *FH ) ;  open( FH, $file ); my $text = do { local( $/ ) ; <FH> } ; 
      # Remove <o:p> tags
      $text =~ s/<o:p>.*?<\/o:p>//sg;
      # Fix footnotes with missing paragraph and missing closing div tag
      $text =~ s/(<div.*?>\s*)(?!<p)(.*?\S)(\s*)(<div)/$1<p class="c5">$2<\/p><\/div>$3<div/sg;  
      $text =~ s/(<div.*?<\/p>)/$1<\/div>/sg;  
      # Fix html
	  $text =~ s/<p><i><b><a name="1.">1.<\/a> <\/b><\/i><\/p>\s*<p>/<p><i><b><a name="1.">1.<\/a> <\/b><\/i>/sg;
      # Transform special chars into entities
	  $text =~ s/\&#39;/\&rsquo;/g;
	  $text =~ s/\&#171;/\&laquo;/g;        
	  $text =~ s/\&#187;/\&raquo;/g;        
	  $text =~ s/\&#156;/\&oelig;/g;   
	  $text =~ s/\&#151;/\&ndash;/g;    
	  $text =~ s/\&#339;/\&oelig;/g;   
	  $text =~ s/\&#151;/\&ndash;/g;
	  $text =~ s/ lang="fr"//g;
	  $text =~ s/ xml:lang="fr"//g; 
	  $text =~ s/\x92/\&rsquo;/g;
	  $text =~ s/\x96/\&ndash;/g;
	  print $text;' $FILE > $FILE.tmp ; cp $FILE.tmp $FILE ; rm $FILE.tmp
  tidy -config ../tidy1.conf -asxml -i -n -c -m -w 9999 -latin1 $FILE 2> /dev/null
  perl -e '
      # Load file
      $file = $ARGV[0];  local( *FH ) ;  open( FH, $file ); my $text = do { local( $/ ) ; <FH> } ; 
      # Fix missing accents
	  $text =~ s/Evangile/\&Eacute;vangile/g;
	  $text =~ s/Eglise/\&Eacute;glise/g;
	  $text =~ s/Ecriture/\&Eacute;criture/g;
	  $text =~ s/Egypte/\&Eacute;gypte/g;
	  $text =~ s/Epoux/\&Eacute;poux/g;
	  $text =~ s/Eternel/\&Eacute;ternel/g;
	  $text =~ s/Ev\&ecirc;que/\&Eacute;v\&ecirc;que/g;
	  $text =~ s/Elisabeth/\&Eacute;lisabeth/g;
	  $text =~ s/EGLISE/\&Eacute;GLISE/g;
	  $text =~ s/MERE/M\&Egrave;RE/g;
	  $text =~ s/PERE/P\&Egrave;RE/g;
	  $text =~ s/MEDIATION/M\&Eacute;DIATION/g;
	  $text =~ s/MISERICORDE/MIS\&Eacute;RICORDE/g;
	  $text =~ s/HERITAGE/H\&Eacute;RITAGE/g;     
	  $text =~ s/MYSTERE/MYST\&Egrave;RE/g;        
	  $text =~ s/VERITE/V\&Eacute;RIT\&Eacute;/g;
	  $text =~ s/AETATE/\&AElig;TATE/g;
	  $text =~ s/A[Ee]tate/\&AElig;tate/g;
	  $text =~ s/\baetate\b/\&aelig;tate/g;
	  $text =~ s/\b(perfect|Perfect|human|Human)ae\b/$1\&aelig;/g;
	  $text =~ s/(PERFECT|HUMAN)AE/$1\&AElig;/g;     
	  $text =~ s/Decret/D&eacute;cret/g;
	  $text =~ s/Declaration/D&eacute;claration/g;
      # Fix typos
	  $text =~ s/Incarnation ed la/Incarnation de la/g; 
	  $text =~ s/mul- tiples/multiples/g;
	  $text =~ s/subis- sent/subissent/g;
	  $text =~ s/sh<sup>e<\/sup>ma/shema/g;     
	  $text =~ s/Elle est<br \/>/Elle est/g;
	  $text =~ s/>A />\&Agrave; /g;
	  $text =~ s/> A /> \&Agrave; /g;
	  $text =~ s/\. A /. \&Agrave; /g;
	  $text =~ s/L '"'"'/L'"'"'/g;                  
	  # Fix bad html
	  $text =~ s/<!\[if !supportFootnotes\]>//g;
	  $text =~ s/<!\[endif\]>//g;
	  $text =~ s/ encoding="iso-8859-1"//g;
	  $text =~ s/(<a[^>]*) name="[^"]*"/\1/g;
	  print $text;' $FILE > $FILE.tmp ; cp $FILE.tmp $FILE ; rm $FILE.tmp              
  iconv -f ISO-8859-1 -t UTF-8 $FILE > $FILE.tmp ; cp $FILE.tmp $FILE ; rm $FILE.tmp
  gsed -f ../quotes.sed $FILE > $FILE.tmp ; cp $FILE.tmp $FILE ; rm $FILE.tmp 
  sed -f ../whitespaces.sed $FILE > $FILE.tmp ; cp $FILE.tmp $FILE ; rm $FILE.tmp 
  iconv -t ISO-8859-1 -f UTF-8 $FILE > $FILE.tmp ; cp $FILE.tmp $FILE ; rm $FILE.tmp
  perl -e '                                                                                
      # Load file
      $file = $ARGV[0];  local( *FH ) ;  open( FH, $file ); my $text = do { local( $/ ) ; <FH> } ; 
	  # Remove search box
	  $text =~ s/<table.*?psearch_fill.*?<\/table>//sg; 
	  # Remove empty paragraphs
	  $text =~ s/<p class="c5"><\/p>//g;
	  # Remove images
	  $text =~ s/<img.*?\/>//sg; 
	  # Remove fix width
	  $text =~ s/ (width|topmargin|border|marginwidth|marginheight|cellspacing|cellpadding)=".*?"//g;
      # Foot notes	                                        
	  $text =~ s/<\/head>/<style type="text\/css"> span.footnote { font-family: Verdana; font-size: 80%; font-weight: bold }<\/style><\/head>/;
	  $text =~ s/<\/a>([A-Z])/<\/a> $1/g;              
	  $text =~ s/<\/a><\/span><span>/<\/a><\/span> <span>/g;
  	  $text =~ s/<span[^>]*><sup>(<a[^>]*[^<]*<\/a>)<\/sup><\/span>/$1/g;
	  $text =~ s/\(([12][0-9][0-9]|[1-9][0-9]?)\)(?!([^<]*<br \/>[^<]*)*<\/span>)/<a href="_fnt$1">$1<\/a>/g;
 	  $text =~ s/<a id="([^"]*)">\(<\/a><a href="([^"]*)">([^"]*)<\/a>\)/<a href="$2" id="$1">$3<\/a> /g;
	  $text =~ s/\[(<a [^>]*>[0-9]*<\/a>)\]/$1/g;
	  $text =~ s/(<a [^>]*>)\[([0-9]*)\](<\/a>)/$1\2\3/g;
	  $text =~ s/\. *(<a [^>]*>[0-9]*<\/a>)/$1\./g;
	  $text =~ s/, *(<a [^>]*>[0-9]*<\/a>)/$1,/g;
	  $text =~ s/(<a href=[^>]*>[0-9]*<\/a>)/<span class="footnote"><sup>$1<\/sup><\/span>/g;
	  print $text;' $FILE > $FILE.tmp ; cp $FILE.tmp $FILE ; rm $FILE.tmp
  tidy -config ../tidy2.conf -asxml -i -n -c -m -w 9999 $FILE 2> /dev/null
done
                                         
mkdir OEBPS 2>/dev/null   
rm -f OEBPS/*
mv *.html OEBPS
echo -n "application/epub+zip" > mimetype
mkdir META-INF 2>/dev/null
cat > META-INF/container.xml <<EOF
<?xml version="1.0"?>
<container version="1.0" xmlns="urn:oasis:names:tc:opendocument:xmlns:container">
  <rootfiles>
    <rootfile full-path="content.opf" media-type="application/oebps-package+xml"/>
  </rootfiles>
</container>
EOF
cat > titlepage.xhtml <<EOF
<?xml version='1.0' encoding='utf-8'?>
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en">
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
        <title>Cover</title>
        <style type="text/css" title="override_css">
            @page {padding: 0pt; margin:0pt}
            body { text-align: center; padding:0pt; margin: 0pt; }
        </style>
    </head>
    <body>
        <div>
            <svg xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" version="1.1" width="100%" height="100%" viewBox="0 0 531 751" preserveAspectRatio="none">
                <image width="531" height="751" xlink:href="cover.jpeg"/>
            </svg>
        </div>
    </body>
</html>
EOF
                       
if [ ! -f cover.jpeg ]
then                        
  echo "Creating cover page"     
  cat >cover.svg <<EOF
<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" 
	 id="svg2" version="1.1"
     width="531" height="751">
    <rect x="0" y="0" width="100%" height="100%" fill="white" />
	<text style="font-size:72px;font-style:normal;font-weight:bold;fill:#000000;font-family:Brush Script MT" 
		  x="265" y="240">
		<tspan style="text-align:center;text-anchor:middle">Documents</tspan>
	</text>
	<text style="font-size:40px;font-style:normal;font-weight:bold;fill:#000000;font-family:Brush Script MT" 
		  x="265" y="307">
        <tspan style="text-align:center;text-anchor:middle">du Concile</tspan>
    </text>
	<text style="font-size:64px;font-style:normal;font-weight:bold;fill:#000000;font-family:Brush Script MT" 
		  x="265" y="374">
        <tspan style="text-align:center;text-anchor:middle">Vatican II</tspan>
    </text>
</svg>
EOF
  java -jar ../../batik-1.7/batik-rasterizer.jar -m image/jpeg -q 0.9 cover.svg > /dev/null
  rm cover.svg                                                             
  mv cover.jpg cover.jpeg
fi

cat >content.opf <<EOF                                         
<?xml version='1.0' encoding='utf-8'?>
<package xmlns="http://www.idpf.org/2007/opf" version="2.0" unique-identifier="uuid_id">
  <metadata xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:opf="http://www.idpf.org/2007/opf" xmlns:dcterms="http://purl.org/dc/terms/" xmlns:calibre="http://calibre.kovidgoyal.net/2009/metadata" xmlns:dc="http://purl.org/dc/elements/1.1/">
    <dc:title>Concile Vatican II</dc:title>
    <dc:language>fr</dc:language>
    <dc:creator opf:file-as="$AUTHOR" opf:role="aut">$AUTHOR</dc:creator>
    <meta name="cover" content="cover"/>
    <dc:identifier id="uuid_id" opf:scheme="uuid">$UUID</dc:identifier>
  </metadata>
  <manifest>
    <item id="cover" href="cover.jpeg" media-type="image/jpeg"/>
    <item id="titlepage" href="titlepage.xhtml" media-type="application/xhtml+xml"/>
    <item id="s01" media-type="application/xhtml+xml" href="OEBPS/index.html" />
EOF
FILES=`sed -e '/file/!d ; s/.*file=.// ; s/. type.*>// ' ../doc-list.xml`
i=1
for FILE in $FILES
do
  i=$(($i + 1))
  is=`printf "%02.f" $i`
  echo "    <item id=\"s$is\" media-type=\"application/xhtml+xml\" href=\"OEBPS/$FILE\"/>" >> content.opf
done
cat >>content.opf <<EOF       
    <!--                                  
    <item href="page_styles.css" id="page_css" media-type="text/css"/>
    <item href="stylesheet.css" id="css" media-type="text/css"/>
    -->
    <item href="toc.ncx" media-type="application/x-dtbncx+xml" id="ncx"/>
  </manifest>
  <spine toc="ncx"> 
    <itemref idref="titlepage"/>
    <itemref idref="s01"/>
EOF
i=1
for FILE in $FILES
do
  i=$(($i + 1))
  is=`printf "%02.f" $i`
  echo "    <itemref idref=\"s$is\"/>" >> content.opf
done
cat >>content.opf <<EOF                                         
  </spine>
  <guide>
    <reference href="titlepage.xhtml" type="cover" title="Cover"/>
  </guide>
</package>
EOF
                      
echo "    Generating toc"
ENC=`sed -e '/file/!d ; s/.*file=.// ; s/. type=.* title=./|/ ; s/.\s*\/>// ;  s/ /*/g' ../doc-list.xml`
cat >toc.ncx <<EOF                                         
<?xml version='1.0' encoding='utf-8'?>
<ncx xmlns="http://www.daisy.org/z3986/2005/ncx/" version="2005-1" xml:lang="eng">
  <head>
    <meta content="1" name="dtb:depth"/>
  </head>
  <docTitle>
    <text>Concile Vatican II</text>
  </docTitle>
  <navMap>
EOF
i=0
for FILE in $ENC
do  
    i=$(($i + 1))
	echo "    <navPoint id=\"$i\" playOrder=\"$i\">" >>toc.ncx
	echo "      <navLabel>" >>toc.ncx
	echo "        <text>"`echo "$FILE" | sed 's/.*|//;s/*/ /g'`"</text>" >>toc.ncx
	echo "      </navLabel>" >>toc.ncx
	echo "      <content src=\"OEBPS/"`echo "$FILE" | sed 's/|.*//'`"\"/>" >>toc.ncx
	echo "    </navPoint>" >>toc.ncx
done
cat >>toc.ncx <<EOF                                         
  </navMap>
</ncx>
EOF

echo "    Zipping"
zip -X $OUTPUT mimetype
zip -rg $OUTPUT META-INF -x \*.DS_Store
zip $OUTPUT content.opf
zip $OUTPUT cover.jpeg
zip -rg $OUTPUT OEBPS -x \*.DS_Store
#zip $OUTPUT page_styles.css
#zip $OUTPUT stylesheet.css
zip $OUTPUT titlepage.xhtml
zip $OUTPUT toc.ncx
cp $OUTPUT ../..




