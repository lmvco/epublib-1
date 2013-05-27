package nl.siegmann.epublib.epub;

import nl.siegmann.epublib.Constants;
import nl.siegmann.epublib.domain.*;
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

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * nav document read and write
 *
 * @author LinQ
 * @version 2013-05-24
 */
public class NavDocument {
    private static final Logger log = LoggerFactory.getLogger(NavDocument.class);
    public static final String NAMESPACE_HTML = "http://www.w3.org/1999/xhtml";

    private interface NAVTags {
        String nav = "nav";
        String ol = "ol";
        String li = "li";
        String a = "a";
        String span = "span";
    }

    private interface NAVAttributes {
        String epubType = "epub:type";
        String href = "href";
    }

    private interface NAVAttributeValues {
        String toc = "toc";
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

    public static void main(String[] args) throws IOException, SAXException {
        Book book = new EpubReader().readEpub(new FileInputStream("F:\\TDDOWNLOAD\\epub3.epub"));
        printToc(book.getTableOfContents().getTocReferences(), "");

    }

    private static void printToc(List<TOCReference> tocReferences, String iden) {
        for (TOCReference tocReference : tocReferences) {
            System.out.println(iden + tocReference.getTitle() + "^^^^^^" + tocReference.getResourceId());
            printToc(tocReference.getChildren(), "--|" + iden);
        }
    }
}
