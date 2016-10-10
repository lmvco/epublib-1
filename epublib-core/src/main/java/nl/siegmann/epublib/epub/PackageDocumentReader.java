package nl.siegmann.epublib.epub;

import nl.siegmann.epublib.Constants;
import nl.siegmann.epublib.domain.*;
import nl.siegmann.epublib.service.MediatypeService;
import nl.siegmann.epublib.util.ResourceUtil;
import nl.siegmann.epublib.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.*;

/**
 * Reads the opf package document as defined by namespace http://www.idpf.org/2007/opf
 *  
 * @author paul
 *
 */
public class PackageDocumentReader extends PackageDocumentBase {
	
	private static final Logger log = LoggerFactory.getLogger(PackageDocumentReader.class);
	private static final String[] POSSIBLE_NCX_ITEM_IDS = new String[] {"toc", "ncx"};
	
	
	public static void read(Resource packageResource, EpubReader epubReader, Book book, Resources resources) throws UnsupportedEncodingException, SAXException, IOException, ParserConfigurationException {
		Document packageDocument = ResourceUtil.getAsDocument(packageResource);
		String packageHref = packageResource.getHref();
		resources = fixHrefs(packageHref, resources);
        readPackageProperties(packageDocument, book);
        readGuide(packageDocument, epubReader, book, resources);
		
		// Books sometimes use non-identifier ids. We map these here to legal ones
		Map<String, String> idMapping = new HashMap<String, String>();
		
		readManifest(packageDocument, resources, book, idMapping);
        readCover(packageDocument, book);
        book.setMetadata(PackageDocumentMetadataReader.readMetadata(packageDocument, book.getResources()));
		book.setSpine(readSpine(packageDocument, epubReader, book.getResources(), idMapping));
        book.setNavResource(readNav(book.getManifest()));
        book.setBindings(readBindings(packageDocument));
        book.setPackageId(readPackageId(book.getMetadata()));

        // if we did not find a cover page then we make the first page of the book the cover page
		if (book.getCoverPage() == null && book.getSpine().size() > 0) {
			book.setCoverPage(book.getSpine().getResource(0));
		}
	}

    private static String readPackageId(Metadata metadata) {
        String result = Identifier.getBookIdIdentifier(metadata.getIdentifiers()).getValue();
        for (Meta meta : metadata.getMetas()) {
            if (meta.getProperty().equals(DCAttributes.modified)) {
                if (StringUtil.isNotBlank(meta.getValue())) {
                    return String.format("%s@%s", result, meta.getValue());
                }
            }
        }

        return result;
    }

    private static void readPackageProperties(Document packageDocument, Book book) {
        String version = packageDocument.getDocumentElement().getAttribute(OPFAttributes.version);
        String uniqueId = packageDocument.getDocumentElement().getAttribute(OPFAttributes.uniqueIdentifier);
        if (StringUtil.isNotBlank(version)) {
            book.setVersion(Version.findVersion(version));
        }
        if (StringUtil.isBlank(uniqueId)) {
            uniqueId = BOOK_ID_ID;
        }
        book.setUniqueId(uniqueId);
    }

//	private static Resource readCoverImage(Element metadataElement, Resources resources) {
//		String coverResourceId = DOMUtil.getFindAttributeValue(metadataElement.getOwnerDocument(), NAMESPACE_OPF, OPFTags.meta, OPFAttributes.name, OPFValues.meta_cover, OPFAttributes.content);
//		if (StringUtil.isBlank(coverResourceId)) {
//			return null;
//		}
//		Resource coverResource = resources.getByIdOrHref(coverResourceId);
//		return coverResource;
//	}

    public static void readManifest(Document packageDocument, Resources resources, Book book, Map<String, String> idMapping) {
        Element manifestElement = DOMUtil.getFirstElementByTagNameNS(packageDocument.getDocumentElement(), NAMESPACE_OPF, OPFTags.manifest);
        if(manifestElement == null) {
            log.error("Package document does not contain element " + OPFTags.manifest);
            return;
        }

        Manifest manifest = book.getManifest();
        book.setResources(manifest.getResources());

        NodeList itemElements = manifestElement.getElementsByTagNameNS(NAMESPACE_OPF, OPFTags.item);
        for(int i = 0; i < itemElements.getLength(); i++) {
            Element itemElement = (Element) itemElements.item(i);
            String id = DOMUtil.getAttribute(itemElement, NAMESPACE_OPF, OPFAttributes.id);
            String href = DOMUtil.getAttribute(itemElement, NAMESPACE_OPF, OPFAttributes.href);
            String properties = DOMUtil.getAttribute(itemElement, NAMESPACE_OPF, OPFAttributes.properties);
            String fallback = DOMUtil.getAttribute(itemElement, NAMESPACE_OPF, OPFAttributes.fallback);
            String mediaOverlay = DOMUtil.getAttribute(itemElement, NAMESPACE_OPF, OPFAttributes.mediaOverlay);
            try {
                href = URLDecoder.decode(href, Constants.CHARACTER_ENCODING);
            } catch (UnsupportedEncodingException e) {
                log.error(e.getMessage());
            }
            String mediaTypeName = DOMUtil.getAttribute(itemElement, NAMESPACE_OPF, OPFAttributes.media_type);
            Resource resource = resources.remove(href);
            if(resource == null) {
                log.error("resource with href '" + href + "' not found");
                continue;
            }
            resource.setId(id);
            MediaTypeProperty mediaTypeProperty = MediatypeService.getMediaType(href, mediaTypeName);
            if(mediaTypeProperty != null) {
                resource.setMediaTypeProperty(mediaTypeProperty);
            }
            ManifestItemReference manifestItem = new ManifestItemReference(resource, ManifestItemProperties.findProperties(properties));
            manifestItem.setFallback(fallback);
            manifestItem.setMediaOverlay(mediaOverlay);
            manifest.addReference(manifestItem);
            idMapping.put(id, resource.getId());
        }
    }


    /**
	 * Reads the manifest containing the resource ids, hrefs and mediatypes.
	 *  
	 * @param packageDocument
	 * @param packageHref
	 * @param epubReader
	 * @param resources
	 * @param idMapping
	 * @return a Map with resources, with their id's as key.
	 */
	private static Resources readManifest(Document packageDocument, String packageHref,
			EpubReader epubReader, Resources resources, Map<String, String> idMapping) {
		Element manifestElement = DOMUtil.getFirstElementByTagNameNS(packageDocument.getDocumentElement(), NAMESPACE_OPF, OPFTags.manifest);
		Resources result = new Resources();
		if(manifestElement == null) {
			log.error("Package document does not contain element " + OPFTags.manifest);
			return result;
		}
		NodeList itemElements = manifestElement.getElementsByTagNameNS(NAMESPACE_OPF, OPFTags.item);
		for(int i = 0; i < itemElements.getLength(); i++) {
			Element itemElement = (Element) itemElements.item(i);
			String id = DOMUtil.getAttribute(itemElement, NAMESPACE_OPF, OPFAttributes.id);
			String href = DOMUtil.getAttribute(itemElement, NAMESPACE_OPF, OPFAttributes.href);
            String properties = DOMUtil.getAttribute(itemElement, NAMESPACE_OPF, OPFAttributes.properties);
            try {
				href = URLDecoder.decode(href, Constants.CHARACTER_ENCODING);
			} catch (UnsupportedEncodingException e) {
				log.error(e.getMessage());
			}
			String mediaTypeName = DOMUtil.getAttribute(itemElement, NAMESPACE_OPF, OPFAttributes.media_type);
			Resource resource = resources.remove(href);
			if(resource == null) {
				log.error("resource with href '" + href + "' not found");
				continue;
			}
			resource.setId(id);
			MediaTypeProperty mediaTypeProperty = MediatypeService.getMediaTypeByName(mediaTypeName);
			if(mediaTypeProperty != null) {
				resource.setMediaTypeProperty(mediaTypeProperty);
			}
			result.add(resource);
			idMapping.put(id, resource.getId());
		}
		return result;
	}	

	
	
	
	/**
	 * Reads the book's guide.
	 * Here some more attempts are made at finding the cover page.
	 * 
	 * @param packageDocument
	 * @param epubReader
	 * @param book
	 * @param resources
	 */
	private static void readGuide(Document packageDocument,
			EpubReader epubReader, Book book, Resources resources) {
		Element guideElement = DOMUtil.getFirstElementByTagNameNS(packageDocument.getDocumentElement(), NAMESPACE_OPF, OPFTags.guide);
		if(guideElement == null) {
			return;
		}
		Guide guide = book.getGuide();
		NodeList guideReferences = guideElement.getElementsByTagNameNS(NAMESPACE_OPF, OPFTags.reference);
		for (int i = 0; i < guideReferences.getLength(); i++) {
			Element referenceElement = (Element) guideReferences.item(i);
			String resourceHref = DOMUtil.getAttribute(referenceElement, NAMESPACE_OPF, OPFAttributes.href);
			if (StringUtil.isBlank(resourceHref)) {
				continue;
			}
			Resource resource = resources.getByHref(StringUtil.substringBefore(resourceHref, Constants.FRAGMENT_SEPARATOR_CHAR));
			if (resource == null) {
				log.error("Guide is referencing resource with href " + resourceHref + " which could not be found");
				continue;
			}
			String type = DOMUtil.getAttribute(referenceElement, NAMESPACE_OPF, OPFAttributes.type);
			if (StringUtil.isBlank(type)) {
				log.error("Guide is referencing resource with href " + resourceHref + " which is missing the 'type' attribute");
				continue;
			}
			String title = DOMUtil.getAttribute(referenceElement, NAMESPACE_OPF, OPFAttributes.title);
			if (GuideReference.COVER.equalsIgnoreCase(type)) {
				continue; // cover is handled elsewhere
			}
			GuideReference reference = new GuideReference(resource, type, title, StringUtil.substringAfter(resourceHref, Constants.FRAGMENT_SEPARATOR_CHAR));
			guide.addReference(reference);
		}
	}


	/**
	 * Strips off the package prefixes up to the href of the packageHref.
	 * 
	 * Example:
	 * If the packageHref is "OEBPS/content.opf" then a resource href like "OEBPS/foo/bar.html" will be turned into "foo/bar.html"
	 * 
	 * @param packageHref
	 * @param resourcesByHref
	 * @return
	 */
	private static Resources fixHrefs(String packageHref,
			Resources resourcesByHref) {
		int lastSlashPos = packageHref.lastIndexOf('/');
		if(lastSlashPos < 0) {
			return resourcesByHref;
		}
		Resources result = new Resources();
        for (Resource resource : resourcesByHref.getAll()) {
            if (!resource.getHref().contains("META-INF")) {
                if (StringUtil.isNotBlank(resource.getHref())
					|| resource.getHref().length() > lastSlashPos) {
				resource.setHref(resource.getHref().substring(lastSlashPos + 1));
			}
            }
			result.add(resource);
		}
		return result;
	}

	/**
	 * Reads the document's spine, containing all sections in reading order.
	 * 
	 * @param packageDocument
	 * @param epubReader
	 * @param resources
	 * @param idMapping
	 * @return
	 */
	private static Spine readSpine(Document packageDocument, EpubReader epubReader, Resources resources, Map<String, String> idMapping) {
		
		Element spineElement = DOMUtil.getFirstElementByTagNameNS(packageDocument.getDocumentElement(), NAMESPACE_OPF, OPFTags.spine);
		if (spineElement == null) {
			log.error("Element " + OPFTags.spine + " not found in package document, generating one automatically");
			return generateSpineFromResources(resources);
		}
		Spine result = new Spine();
        result.setId(spineElement.getAttribute(OPFAttributes.id));
        result.setDirection(PageProgressionDirection.findDirection(spineElement.getAttribute(OPFAttributes.pageProgressionDirection)));
		result.setTocResource(findTableOfContentsResource(spineElement, resources));
		NodeList spineNodes = packageDocument.getElementsByTagNameNS(NAMESPACE_OPF, OPFTags.itemref);
		List<SpineReference> spineReferences = new ArrayList<SpineReference>(spineNodes.getLength());
		for(int i = 0; i < spineNodes.getLength(); i++) {
			Element spineItem = (Element) spineNodes.item(i);
			String itemref = DOMUtil.getAttribute(spineItem, NAMESPACE_OPF, OPFAttributes.idref);
			if(StringUtil.isBlank(itemref)) {
				log.error("itemref with missing or empty idref"); // XXX
				continue;
			}
			String id = idMapping.get(itemref);
			if (id == null) {
				id = itemref;
			}
			Resource resource = resources.getByIdOrHref(id);
			if(resource == null) {
				log.error("resource with id \'" + id + "\' not found");
				continue;
			}
			
			SpineReference spineReference = new SpineReference(resource);
            spineReference.setIdref(itemref);
            String properties = DOMUtil.getAttribute(spineItem, NAMESPACE_OPF, OPFAttributes.properties);
            spineReference.setProperties(SpineItemRefProperties.findProperties(properties));
			if (OPFValues.no.equalsIgnoreCase(DOMUtil.getAttribute(spineItem, NAMESPACE_OPF, OPFAttributes.linear))) {
				spineReference.setLinear(false);
			}
			spineReferences.add(spineReference);
		}
		result.setSpineReferences(spineReferences);
		return result;
	}

    private static Resource readNav(Manifest manifest) {
        for (ManifestItemReference reference : manifest.getReferences()) {
            if (reference.getProperties() == ManifestItemProperties.NAV) {
                return reference.getResource();
            }
        }
        return null;
    }

    public static Bindings readBindings(Document packageDocument) {
        Bindings result = new Bindings();
        Element bindingsElement = DOMUtil.getFirstElementByTagNameNS(packageDocument.getDocumentElement(), NAMESPACE_OPF, OPFTags.bindings);
        if (bindingsElement != null) {
            NodeList nodeList = bindingsElement.getElementsByTagNameNS(NAMESPACE_OPF, OPFTags.mediaType);
            for (int i = 0; i < nodeList.getLength(); i++) {
                MediaType mediaType = new MediaType();
                result.addMediaType(mediaType);
                Element element = (Element) nodeList.item(i);
                String mediaTypeName = DOMUtil.getAttribute(element, NAMESPACE_OPF, OPFAttributes.media_type);
                String handler = DOMUtil.getAttribute(element, NAMESPACE_OPF, OPFAttributes.handler);
                mediaType.setMediaTypeProperty(MediatypeService.getMediaTypeByName(mediaTypeName));
                mediaType.setHandler(handler);
            }
        }

        return result;
    }

	/**
	 * Creates a spine out of all resources in the resources.
	 * The generated spine consists of all XHTML pages in order of their href.
	 * 
	 * @param resources
	 * @return
	 */
	private static Spine generateSpineFromResources(Resources resources) {
		Spine result = new Spine();
		List<String> resourceHrefs = new ArrayList<String>();
		resourceHrefs.addAll(resources.getAllHrefs());
		Collections.sort(resourceHrefs, String.CASE_INSENSITIVE_ORDER);
		for (String resourceHref: resourceHrefs) {
			Resource resource = resources.getByHref(resourceHref);
			if (resource.getMediaTypeProperty() == MediatypeService.NCX) {
				result.setTocResource(resource);
			} else if (resource.getMediaTypeProperty() == MediatypeService.XHTML) {
				result.addSpineReference(new SpineReference(resource));
			}
		}
		return result;
	}

	
	/**
	 * The spine tag should contain a 'toc' attribute with as value the resource id of the table of contents resource.
	 * 
	 * Here we try several ways of finding this table of contents resource.
	 * We try the given attribute value, some often-used ones and finally look through all resources for the first resource with the table of contents mimetype.
	 * 
	 * @param spineElement
	 * @param resources
	 * @return
	 */
	private static Resource findTableOfContentsResource(Element spineElement, Resources resources) {
		String tocResourceId = DOMUtil.getAttribute(spineElement, NAMESPACE_OPF, OPFAttributes.toc);
		Resource tocResource = null;
		if (StringUtil.isNotBlank(tocResourceId)) {
			tocResource = resources.getByIdOrHref(tocResourceId);
		}
		
		/*if (tocResource != null) {
			return tocResource;
		}
		
		for (int i = 0; i < POSSIBLE_NCX_ITEM_IDS.length; i++) {
			tocResource = resources.getByIdOrHref(POSSIBLE_NCX_ITEM_IDS[i]);
			if (tocResource != null) {
				return tocResource;
			}
			tocResource = resources.getByIdOrHref(POSSIBLE_NCX_ITEM_IDS[i].toUpperCase());
			if (tocResource != null) {
				return tocResource;
			}
		}
		
		// get the first resource with the NCX mediatype
		tocResource = resources.findFirstResourceByMediaType(MediatypeService.NCX);

		if (tocResource == null) {
			log.error("Could not find table of contents resource. Tried resource with id '" + tocResourceId + "', " + Constants.DEFAULT_TOC_ID + ", " + Constants.DEFAULT_TOC_ID.toUpperCase() + " and any NCX resource.");
		}*/
		return tocResource;
	}


	/**
	 * Find all resources that have something to do with the coverpage and the cover image.
	 * Search the meta tags and the guide references
	 * 
	 * @param packageDocument
	 * @return
	 */
	// package
	static Set<String> findCoverHrefs(Document packageDocument, Manifest manifest) {
		
		Set<String> result = new HashSet<String>();
		
		// try and find a meta tag with name = 'cover' and a non-blank id
		String coverResourceId = DOMUtil.getFindAttributeValue(packageDocument, NAMESPACE_OPF,
											OPFTags.meta, OPFAttributes.name, OPFValues.meta_cover,
											OPFAttributes.content);

		if (StringUtil.isNotBlank(coverResourceId)) {
			String coverHref = DOMUtil.getFindAttributeValue(packageDocument, NAMESPACE_OPF,
					OPFTags.item, OPFAttributes.id, coverResourceId,
					OPFAttributes.href);
			if (StringUtil.isNotBlank(coverHref)) {
				result.add(coverHref);
			} else {
				result.add(coverResourceId); // maybe there was a cover href put in the cover id attribute
			}
		}
		// try and find a reference tag with type is 'cover' and reference is not blank
		String coverHref = DOMUtil.getFindAttributeValue(packageDocument, NAMESPACE_OPF,
											OPFTags.reference, OPFAttributes.type, OPFValues.reference_cover,
											OPFAttributes.href);
        if (StringUtil.isNotBlank(coverHref)) {
			result.add(coverHref);
		}

        for (ManifestItemReference reference : manifest.getReferences()) {
            if (reference.getProperties() == ManifestItemProperties.COVER_IMAGE) {
                result.add(reference.getResource().getHref());
            }
        }
        return result;
	}

	/**
	 * Finds the cover resource in the packageDocument and adds it to the book if found.
	 * Keeps the cover resource in the resources map
	 * @param packageDocument
	 * @param book
	 * @return
	 */
	private static void readCover(Document packageDocument, Book book) {
		
		Collection<String> coverHrefs = findCoverHrefs(packageDocument, book.getManifest());
		for (String coverHref: coverHrefs) {
			Resource resource = book.getResources().getByHref(coverHref);
			if (resource == null) {
				log.error("Cover resource " + coverHref + " not found");
				continue;
			}
			if (resource.getMediaTypeProperty() == MediatypeService.XHTML) {
				book.setCoverPage(resource);
			} else if (MediatypeService.isBitmapImage(resource.getMediaTypeProperty())) {
				book.setCoverImage(resource);
			}
		}
	}
	

}