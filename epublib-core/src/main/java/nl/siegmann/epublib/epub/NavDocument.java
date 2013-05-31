package nl.siegmann.epublib.epub;

import nl.siegmann.epublib.Constants;
import nl.siegmann.epublib.domain.*;
import nl.siegmann.epublib.service.MediatypeService;
import nl.siegmann.epublib.util.ResourceUtil;
import nl.siegmann.epublib.util.StringUtil;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xmlpull.v1.XmlSerializer;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static nl.siegmann.epublib.epub.PackageDocumentBase.EMPTY_NAMESPACE_PREFIX;
import static nl.siegmann.epublib.epub.PackageDocumentBase.NAMESPACE_OPF;

/**
 * nav document read and write
 *
 * @author LinQ
 * @version 2013-05-24
 */
public class NavDocument {
    private static final Logger log = LoggerFactory.getLogger(NavDocument.class);
    public static final String NAMESPACE_HTML = "http://www.w3.org/1999/xhtml";
    public static final String NAV_ITEM_ID = "nav";
    public static final String DEFAULT_NAV_HREF = "nav.xhtml";

    private interface NAVTags {
        String html = "html";
        String head = "head";
        String title = "title";
        String meta = "meta";
        String body = "body";
        String section = "section";
        String header = "header";
        String h1 = "h1";
        String h2 = "h2";
        String nav = "nav";
        String ol = "ol";
        String li = "li";
        String a = "a";
        String span = "span";
    }

    private interface NAVAttributes {
        String epubType = "epub:type";
        String href = "href";
        String id = "id";
        String charset = "charset";
    }

    private interface NAVAttributeValues {
        String toc = "toc";
        String utf8 = "utf-8";
    }

    public static Resource read(Book book) {
        Resource navResource = null;
        Manifest manifest = book.getManifest();
        for (ManifestItemReference reference : manifest.getReferences()) {
            if (reference.getProperties() == ManifestItemProperties.NAV) {
                navResource =  reference.getResource();
                break;
            }
        }

        if (book.getVersion() == Version.V3 && navResource == null) {
            log.error("Book does not contain nav resource");
            return null;
        }

        if (book.getTableOfContents().getTocReferences().size() == 0) {
            List<TOCReference> tocReferences = read(navResource, book);
            book.setTableOfContents(new TableOfContents(tocReferences));
        }


        return navResource;
    }

    public static List<TOCReference> read(Resource navResource, Book book) {
        try {
            Document navDocument = ResourceUtil.getAsDocument(navResource);
            NodeList navList = navDocument.getDocumentElement().getElementsByTagName(NAVTags.nav);
            for (int i = 0; i < navList.getLength(); i++) {
                Element navElement = (Element) navList.item(i);
                if (navElement.hasAttribute(NAVAttributes.epubType) && navElement.getAttribute(NAVAttributes.epubType).equals(NAVAttributeValues.toc)) {
                    NodeList list = navElement.getElementsByTagName(NAVTags.ol);
                    Element olElement = (Element) list.item(0);
                     return readTOCReferences(olElement, book);
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

        return Collections.emptyList();
    }

    public static List<TOCReference> readTOCReferences(Element olElement, Book book) {
        NodeList list = olElement.getChildNodes();
        List<TOCReference> result = new ArrayList<TOCReference>(list.getLength());
        for (int i = 0; i < list.getLength(); i++) {
            Node node = list.item(i);
            if (node.getNodeType() != Document.ELEMENT_NODE) {
                continue;
            }
            if (! (node.getLocalName().equals(NAVTags.li))) {
                continue;
            }
            TOCReference tocReference = readTOCReference((Element) node, book);
            result.add(tocReference);
        }
        return result;
    }

    public static TOCReference readTOCReference(Element liElement, Book book) {
        TOCReference tocReference = new TOCReference();

        Element aElement = DOMUtil.getFirstElementByTagNameNS(liElement, NAMESPACE_HTML, NAVTags.a);
        Element olElement = DOMUtil.getFirstElementByTagNameNS(liElement, NAMESPACE_HTML, NAVTags.ol);
        if (aElement != null) {
            String path = FilenameUtils.getPathNoEndSeparator(book.getNavResource().getHref());
            String reference = aElement.getAttribute(NAVAttributes.href);
            reference = FilenameUtils.concat(path, reference);
            reference = FilenameUtils.separatorsToUnix(reference);
            String href = StringUtil.substringBefore(reference, Constants.FRAGMENT_SEPARATOR_CHAR);
            String fragmentId = StringUtil.substringAfter(reference, Constants.FRAGMENT_SEPARATOR_CHAR);
            Resource resource = book.getResources().getByHref(href);
            String title = aElement.getTextContent();
            tocReference.setTitle(title);
            tocReference.setResource(resource);
            tocReference.setFragmentId(fragmentId);
        }
        if (olElement != null) {
            if (StringUtil.isBlank(tocReference.getTitle())) {
                Element spanElement = DOMUtil.getFirstElementByTagNameNS(liElement, NAMESPACE_HTML, NAVTags.span);
                if (spanElement != null) {
                    String title = spanElement.getTextContent();
                    tocReference.setTitle(title);
                }
            }
            List<TOCReference> children = readTOCReferences(olElement, book);
            tocReference.setChildren(children);
        }
        return tocReference;
    }

    public static Resource createNavResource(Book book) throws IOException {
        ByteArrayOutputStream data = new ByteArrayOutputStream();
        XmlSerializer out = EpubProcessorSupport.createXmlSerializer(data);
        write(out, book);
        out.flush();
        return new Resource(NAV_ITEM_ID, data.toByteArray(), DEFAULT_NAV_HREF, MediatypeService.XHTML);
    }

    public static void write(XmlSerializer serializer, Book book) throws IOException {
        serializer.startDocument(Constants.CHARACTER_ENCODING, false);
        serializer.setPrefix("", NAMESPACE_HTML);
        serializer.setPrefix("epub", NAMESPACE_OPF);
        serializer.startTag(NAMESPACE_HTML, NAVTags.html);
        serializer.startTag(NAMESPACE_HTML, NAVTags.head);
        serializer.startTag(NAMESPACE_HTML, NAVTags.title);
        serializer.text(book.getTitle().getValue());
        serializer.endTag(NAMESPACE_HTML, NAVTags.title);
        serializer.startTag(NAMESPACE_HTML, NAVTags.meta);
        serializer.attribute(EMPTY_NAMESPACE_PREFIX, NAVAttributes.charset, NAVAttributeValues.utf8);
        serializer.endTag(NAMESPACE_HTML, NAVTags.meta);
        serializer.endTag(NAMESPACE_HTML, NAVTags.head);
        serializer.startTag(NAMESPACE_HTML, NAVTags.body);
        serializer.startTag(NAMESPACE_HTML, NAVTags.nav);
        serializer.attribute(EMPTY_NAMESPACE_PREFIX, NAVAttributes.epubType, NAVAttributeValues.toc);
        serializer.attribute(EMPTY_NAMESPACE_PREFIX, NAVAttributes.id, NAVAttributeValues.toc);
        serializer.startTag(NAMESPACE_HTML, NAVTags.ol);
        writeTOCReferences(serializer, book.getTableOfContents().getTocReferences());
        serializer.endTag(NAMESPACE_HTML, NAVTags.ol);
        serializer.endTag(NAMESPACE_HTML, NAVTags.nav);
        serializer.endTag(NAMESPACE_HTML, NAVTags.body);
        serializer.endTag(NAMESPACE_HTML, NAVTags.html);
    }

    public static void writeTOCReferences(XmlSerializer serializer, List<TOCReference> tocReferences) throws IOException {
        for (TOCReference tocReference : tocReferences) {
            if (tocReference.getChildren().size() > 0) {
                if (StringUtil.isNotBlank(tocReference.getTitle())) {
                    serializer.startTag(NAMESPACE_HTML, NAVTags.li);
                    serializer.startTag(NAMESPACE_HTML, NAVTags.span);
                    serializer.text(tocReference.getTitle());
                    serializer.endTag(NAMESPACE_HTML, NAVTags.span);
                    serializer.endTag(NAMESPACE_HTML, NAVTags.li);
                }
                serializer.startTag(NAMESPACE_HTML, NAVTags.ol);
                writeTOCReferences(serializer, tocReference.getChildren());
                serializer.endTag(NAMESPACE_HTML, NAVTags.ol);
            } else {
                serializer.startTag(NAMESPACE_HTML, NAVTags.li);
                serializer.startTag(NAMESPACE_HTML, NAVTags.a);
                serializer.attribute(EMPTY_NAMESPACE_PREFIX, NAVAttributes.href, tocReference.getCompleteHref());
                serializer.text(tocReference.getTitle());
                serializer.endTag(NAMESPACE_HTML, NAVTags.a);
                serializer.endTag(NAMESPACE_HTML, NAVTags.li);
            }
        }
    }

    public static void main(String[] args) throws IOException, SAXException {
        Book book = new EpubReader().readEpub(new FileInputStream("F:\\TDDOWNLOAD\\epub3\\cc-shared-culture-20120130.epub"));
        new EpubWriter().write(book, new FileOutputStream("F:\\TDDOWNLOAD\\epub3\\out.epub"));

    }
}
