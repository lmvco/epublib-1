package nl.siegmann.epublib.epub;

import nl.siegmann.epublib.domain.*;
import nl.siegmann.epublib.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * Reads the package document metadata.
 * 
 * In its own separate class because the PackageDocumentReader became a bit large and unwieldy.
 * 
 * @author paul
 *
 */
// package
class PackageDocumentMetadataReader extends PackageDocumentBase {
	private static final Logger log = LoggerFactory.getLogger(PackageDocumentMetadataReader.class);
    private static final List<String> META_PROPERTIES = Arrays.asList(
            DCAttributes.property, DCAttributes.refines,
            DCAttributes.id, DCAttributes.scheme, DCAttributes.lang);

	public static Metadata readMetadata(Document packageDocument, Resources resources) {
		Metadata result = new Metadata();
		Element metadataElement = DOMUtil.getFirstElementByTagNameNS(packageDocument.getDocumentElement(), NAMESPACE_OPF, OPFTags.metadata);
		if(metadataElement == null) {
			log.error("Package does not contain element " + OPFTags.metadata);
			return result;
		}
        result.setIdentifiers(readIdentifiers(metadataElement));
        result.setTitles(readDcmesElements(DCTags.title, metadataElement, result));
        result.setLanguages(readDcmesElements(DCTags.language, metadataElement, result));
        result.setContributors(readContributors(metadataElement, result));
        result.setAuthors(readCreators(metadataElement, result));
        result.setDates(readDates(metadataElement));
        result.setSource(readDcmesElement(DCTags.source, metadataElement, result));
        result.setTypes(readDcmesElements(DCTags.type, metadataElement, result));
        result.setPublishers(readDcmesElements(DCTags.publisher, metadataElement, result));
		result.setDescriptions(readDcmesElements(DCTags.description, metadataElement, result));
		result.setRights(readDcmesElements(DCTags.rights, metadataElement, result));
		result.setSubjects(readDcmesElements(DCTags.subject, metadataElement, result));
        result.setMetas(readMetas(metadataElement));
        resolveRefines(result.getMetas(), result);
        result.setLinks(readLinks(metadataElement));

		return result;
	}

    private static List<Link> readLinks(Element metadataElement) {
        NodeList nodeList = metadataElement.getElementsByTagNameNS(NAMESPACE_OPF, OPFTags.link);
        List<Link> result = new ArrayList<Link>();
        for (int i = 0; i < nodeList.getLength(); i++) {
            Element element = (Element) nodeList.item(i);
            Link link = new Link();
            link.setHref(element.getAttribute(DCAttributes.href));
            link.setRel(element.getAttribute(DCAttributes.rel));
            link.setId(element.getAttribute(DCAttributes.id));
            link.setRefines(element.getAttribute(DCAttributes.refines));
            link.setMediaType(element.getAttribute(DCAttributes.mediaType));
            result.add(link);
        }
        return result;
    }

    private static void resolveRefines(List<Meta> metas, Metadata result) {
        Iterator<Meta> iterator = metas.iterator();
        while (iterator.hasNext()) {
            Meta meta = iterator.next();
            String refines = meta.getRefines();
            if (refines != null && refines.startsWith("#")) {
                String id = refines.substring(1);
                DcmesElement dcmesElement = result.getDcmesElementMap().get(id);
                if (dcmesElement != null) {
                    dcmesElement.addMeta(meta);
                    iterator.remove();
                }
            }
        }
    }

    private static List<DcmesElement> readDcmesElements(String tag, Element metadataElement, Metadata metadata) {
        NodeList nodeList = metadataElement.getElementsByTagNameNS(NAMESPACE_DUBLIN_CORE, tag);
        List<DcmesElement> result = new ArrayList<DcmesElement>();
        for (int i = 0; i < nodeList.getLength(); i++) {
            Element element = (Element) nodeList.item(i);
            DcmesElement dcmes = makeDcmesElement(element);
            result.add(dcmes);
            metadata.addDcmesMap(dcmes.getId(), dcmes);
        }
        return result;
    }

    private static DcmesElement makeDcmesElement(Element element) {
        DcmesElement dcmes = new DcmesElement();
        readDcmesCommonProperties(element, dcmes);
        dcmes.setValue(element.getTextContent());
        return dcmes;
    }

    private static DcmesElement readDcmesElement(String tag, Element metadataElement, Metadata metadata) {
        NodeList nodeList = metadataElement.getElementsByTagNameNS(NAMESPACE_DUBLIN_CORE, tag);
        if (nodeList.getLength() < 1)
            return null;
        Element element = (Element) nodeList.item(0);
        DcmesElement dcmes = makeDcmesElement(element);
        metadata.addDcmesMap(dcmes.getId(), dcmes);
        return dcmes;
    }

    /**
	 * consumes meta tags that have a property attribute as defined in the standard. For example:
	 * &lt;meta property="rendition:layout"&gt;pre-paginated&lt;/meta&gt;
	 * @param metadataElement metadataElement
	 * @return Meta list
	 */
	private static List<Meta> readMetas(Element metadataElement) {
		List<Meta> result = new ArrayList<Meta>();
		
		NodeList metaTags = metadataElement.getElementsByTagNameNS(NAMESPACE_OPF, OPFTags.meta);
		for (int i = 0; i < metaTags.getLength(); i++) {
			Element element = (Element) metaTags.item(i);
            String id = element.getAttribute(DCAttributes.id);
            String property = element.getAttribute(DCAttributes.property);
            Meta meta = new Meta();
            if (property != null) {
                meta.setId(id);
                meta.setProperty(property);
                meta.setLang(element.getAttribute(DCAttributes.lang));
                meta.setScheme(element.getAttribute(DCAttributes.scheme));
                meta.setRefines(element.getAttribute(DCAttributes.refines));
            }
            meta.setValue(element.getTextContent());
            readCustomProperties(element, meta);
            result.add(meta);
        }
		
		return result;
	}

    private static void readCustomProperties(Element element, Meta meta) {
        NamedNodeMap attributes = element.getAttributes();
        for (int i = 0; i < attributes.getLength(); i++) {
            Node item = attributes.item(i);
            meta.addCustomProperties(item.getNodeName(), item.getNodeValue());
        }
    }


    private static String getBookIdId(Document document) {
        Element packageElement = document.getDocumentElement();
        if(packageElement == null) {
            return null;
        }
        return packageElement.getAttribute(OPFAttributes.uniqueIdentifier);
	}
		
	private static List<Author> readCreators(Element metadataElement, Metadata result) {
		return readAuthors(DCTags.creator, metadataElement, result);
	}
	
	private static List<Author> readContributors(Element metadataElement, Metadata result) {
		return readAuthors(DCTags.contributor, metadataElement, result);
	}
	
	private static List<Author> readAuthors(String authorTag, Element metadataElement, Metadata metadata) {
		NodeList elements = metadataElement.getElementsByTagNameNS(NAMESPACE_DUBLIN_CORE, authorTag);
		List<Author> result = new ArrayList<Author>(elements.getLength());
		for(int i = 0; i < elements.getLength(); i++) {
			Element authorElement = (Element) elements.item(i);
			Author author = createAuthor(authorElement);
			if (author != null) {
				result.add(author);
                metadata.addDcmesMap(author.getId(), author);
			}
		}
		return result;
		
	}

	private static List<Date> readDates(Element metadataElement) {
		NodeList elements = metadataElement.getElementsByTagNameNS(NAMESPACE_DUBLIN_CORE, DCTags.date);
		List<Date> result = new ArrayList<Date>(elements.getLength());
		for(int i = 0; i < elements.getLength(); i++) {
			Element dateElement = (Element) elements.item(i);
			Date date;
			try {
				date = new Date(DOMUtil.getTextChildrenContent(dateElement), dateElement.getAttributeNS(NAMESPACE_OPF, OPFAttributes.event));
                readDcmesCommonProperties(dateElement, date);
				result.add(date);
			} catch(IllegalArgumentException e) {
				log.error(e.getMessage());
			}
		}
		return result;
		
	}

	private static Author createAuthor(Element authorElement) {
		String authorString = DOMUtil.getTextChildrenContent(authorElement);
		if (StringUtil.isBlank(authorString)) {
			return null;
		}
		int spacePos = authorString.lastIndexOf(' ');
		Author result;
		if(spacePos < 0) {
			result = new Author(authorString);
		} else {
			result = new Author(authorString.substring(0, spacePos), authorString.substring(spacePos + 1));
		}
		result.setRole(authorElement.getAttributeNS(NAMESPACE_OPF, OPFAttributes.role));
        readDcmesCommonProperties(authorElement, result);
		return result;
	}

    public static void readDcmesCommonProperties(Element element, DcmesElement result) {
        if (element == null)
            return;
        result.setId(element.getAttributeNS(NAMESPACE_OPF, DCAttributes.id));
        result.setLang(element.getAttributeNS(NAMESPACE_OPF, DCAttributes.lang));
        result.setDirection(element.getAttributeNS(NAMESPACE_OPF, DCAttributes.dir));
    }


    private static List<Identifier> readIdentifiers(Element metadataElement) {
		NodeList identifierElements = metadataElement.getElementsByTagNameNS(NAMESPACE_DUBLIN_CORE, DCTags.identifier);
		if(identifierElements.getLength() == 0) {
			log.error("Package does not contain element " + DCTags.identifier);
			return new ArrayList<Identifier>();
		}
		String bookIdId = getBookIdId(metadataElement.getOwnerDocument());
		List<Identifier> result = new ArrayList<Identifier>(identifierElements.getLength());
		for(int i = 0; i < identifierElements.getLength(); i++) {
			Element identifierElement = (Element) identifierElements.item(i);
			String schemeName = identifierElement.getAttributeNS(NAMESPACE_OPF, DCAttributes.scheme);
			String identifierValue = DOMUtil.getTextChildrenContent(identifierElement);
			if (StringUtil.isBlank(identifierValue)) {
				continue;
			}
			Identifier identifier = new Identifier(schemeName, identifierValue);
            String identifierId = identifierElement.getAttribute(DCAttributes.id);
            if(identifierId != null && identifierId.equals(bookIdId) ) {
                identifier.setId(identifierId);
				identifier.setBookId(true);
			}
			result.add(identifier);
		}
		return result;
	}
}
