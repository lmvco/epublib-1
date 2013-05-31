package nl.siegmann.epublib.epub.impl;

import nl.siegmann.epublib.domain.*;
import nl.siegmann.epublib.epub.PackageDocumentWriter;
import nl.siegmann.epublib.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;

/**
 * package document writer for epub3
 *
 * @author LinQ
 * @version 2013-05-29
 */
public class Epub3PackageDocumentWriter extends PackageDocumentWriter {
    private static final Logger logger = LoggerFactory.getLogger(Epub3PackageDocumentWriter.class);

    public Epub3PackageDocumentWriter(Book book, XmlSerializer serializer) {
        super(book, serializer);
    }

    @Override
    protected void writeMetadata() throws IOException {
        new Epub3PackageDocumentMetadataWriter(book, serializer).writeMetaData();
    }

    @Override
    protected void writeManifest() throws IOException {
        serializer.startTag(NAMESPACE_OPF, OPFTags.manifest);
        if (StringUtil.isNotBlank(book.getManifest().getId())) {
            serializer.attribute(EMPTY_NAMESPACE_PREFIX, OPFAttributes.id, book.getManifest().getId());
        }

        for(ManifestItemReference reference : book.getManifest().getReferences()) {
            writeItem(reference);
        }

        serializer.endTag(NAMESPACE_OPF, OPFTags.manifest);
    }

    private void writeItem(ManifestItemReference reference) throws IllegalArgumentException, IllegalStateException, IOException {
        Resource resource = reference.getResource();
        MediaTypeProperty mediaTypeProperty = reference.getMediaTypeProperty();
        if(resource == null) {
            return;
        }
        if(StringUtil.isBlank(resource.getId())) {
            logger.error("resource id must not be empty (href: " + resource.getHref() + ", mediatype:" + mediaTypeProperty + ")");
            return;
        }
        if(StringUtil.isBlank(resource.getHref())) {
            logger.error("resource href must not be empty (id: " + resource.getId() + ", mediatype:" + mediaTypeProperty + ")");
            return;
        }
        if(mediaTypeProperty == null) {
            logger.error("resource mediatype must not be empty (id: " + resource.getId() + ", href:" + resource.getHref() + ")");
            return;
        }
        serializer.startTag(NAMESPACE_OPF, OPFTags.item);
        serializer.attribute(EMPTY_NAMESPACE_PREFIX, OPFAttributes.id, resource.getId());
        serializer.attribute(EMPTY_NAMESPACE_PREFIX, OPFAttributes.href, resource.getHref());
        serializer.attribute(EMPTY_NAMESPACE_PREFIX, OPFAttributes.media_type, mediaTypeProperty.getName());
        if (StringUtil.isNotBlank(reference.getFallback())) {
            serializer.attribute(EMPTY_NAMESPACE_PREFIX, OPFAttributes.fallback, reference.getFallback());
        }
        if (reference.getProperties() != null) {
            serializer.attribute(EMPTY_NAMESPACE_PREFIX, OPFAttributes.properties, reference.getProperties().getName());
        }
        if (StringUtil.isNotBlank(reference.getMediaOverlay())) {
            serializer.attribute(EMPTY_NAMESPACE_PREFIX, OPFAttributes.mediaOverlay, reference.getMediaOverlay());
        }
        serializer.endTag(NAMESPACE_OPF, OPFTags.item);
    }

    @Override
    protected void writeSpine() throws IOException {
        serializer.startTag(NAMESPACE_OPF, OPFTags.spine);
        Spine spine = book.getSpine();
        serializer.attribute(EMPTY_NAMESPACE_PREFIX, OPFAttributes.toc, spine.getTocResource().getId());
        if (StringUtil.isNotBlank(spine.getId())) {
            serializer.attribute(EMPTY_NAMESPACE_PREFIX, OPFAttributes.id, spine.getId());
        }
        if (spine.getDirection() != null) {
            serializer.attribute(EMPTY_NAMESPACE_PREFIX, OPFAttributes.pageProgressionDirection, spine.getDirection().getValue());
        }

        if(book.getCoverPage() != null // there is a cover page
                &&	spine.findFirstResourceById(book.getCoverPage().getId()) < 0) { // cover page is not already in the spine
            // write the cover html file
            serializer.startTag(NAMESPACE_OPF, OPFTags.itemref);
            serializer.attribute(EMPTY_NAMESPACE_PREFIX, OPFAttributes.idref, book.getCoverPage().getId());
            serializer.attribute(EMPTY_NAMESPACE_PREFIX, OPFAttributes.linear, "no");
            serializer.endTag(NAMESPACE_OPF, OPFTags.itemref);
        }
        writeSpineItems(spine, serializer);
        serializer.endTag(NAMESPACE_OPF, OPFTags.spine);
    }

    /**
     * List all spine references
     * @throws java.io.IOException
     * @throws IllegalStateException
     * @throws IllegalArgumentException
     */
    private void writeSpineItems(Spine spine, XmlSerializer serializer) throws IllegalArgumentException, IllegalStateException, IOException {
        for(SpineReference spineReference: spine.getSpineReferences()) {
            serializer.startTag(NAMESPACE_OPF, OPFTags.itemref);
            serializer.attribute(EMPTY_NAMESPACE_PREFIX, OPFAttributes.idref, spineReference.getResourceId());
            if (! spineReference.isLinear()) {
                serializer.attribute(EMPTY_NAMESPACE_PREFIX, OPFAttributes.linear, OPFValues.no);
            }
            if (StringUtil.isNotBlank(spineReference.getId())) {
                serializer.attribute(EMPTY_NAMESPACE_PREFIX, OPFAttributes.id, spineReference.getId());
            }
            if (spineReference.getProperties() != null) {
                serializer.attribute(EMPTY_NAMESPACE_PREFIX, OPFAttributes.properties, spineReference.getProperties().getName());
            }
            serializer.endTag(NAMESPACE_OPF, OPFTags.itemref);
        }
    }

    @Override
    protected void writeGuide() throws IOException {
        if (book.getGuide().getReferences().size() == 0)
            return;
        serializer.startTag(NAMESPACE_OPF, OPFTags.guide);
        ensureCoverPageGuideReferenceWritten(book.getGuide(), serializer);
        for (GuideReference reference: book.getGuide().getReferences()) {
            writeGuideReference(reference, serializer);
        }
        serializer.endTag(NAMESPACE_OPF, OPFTags.guide);
    }

    private void ensureCoverPageGuideReferenceWritten(Guide guide, XmlSerializer serializer) throws IllegalArgumentException, IllegalStateException, IOException {
        if (! (guide.getGuideReferencesByType(GuideReference.COVER).isEmpty())) {
            return;
        }
        Resource coverPage = guide.getCoverPage();
        if (coverPage != null) {
            writeGuideReference(new GuideReference(guide.getCoverPage(), GuideReference.COVER, GuideReference.COVER), serializer);
        }
    }

    private void writeGuideReference(GuideReference reference, XmlSerializer serializer) throws IllegalArgumentException, IllegalStateException, IOException {
        if (reference == null) {
            return;
        }
        serializer.startTag(NAMESPACE_OPF, OPFTags.reference);
        serializer.attribute(EMPTY_NAMESPACE_PREFIX, OPFAttributes.type, reference.getType());
        serializer.attribute(EMPTY_NAMESPACE_PREFIX, OPFAttributes.href, reference.getCompleteHref());
        if (StringUtil.isNotBlank(reference.getTitle())) {
            serializer.attribute(EMPTY_NAMESPACE_PREFIX, OPFAttributes.title, reference.getTitle());
        }
        serializer.endTag(NAMESPACE_OPF, OPFTags.reference);
    }

    @Override
    protected void writeBindings() throws IOException {
        if (book.getBindings().getMediaTypes().size() > 0) {
            serializer.startTag(NAMESPACE_OPF, OPFTags.link);
            for (MediaType mediaType : book.getBindings().getMediaTypes()) {
                serializer.startTag(NAMESPACE_OPF, OPFTags.mediaType);
                serializer.attribute(EMPTY_NAMESPACE_PREFIX, OPFAttributes.media_type, mediaType.getMediaTypeProperty().getName());
                serializer.attribute(EMPTY_NAMESPACE_PREFIX, OPFAttributes.handler, mediaType.getHandler());
                serializer.endTag(NAMESPACE_OPF, OPFTags.mediaType);
            }
            serializer.endTag(NAMESPACE_OPF, OPFTags.link);
        }
    }

    @Override
    protected String getEpubVersion() {
        return Version.V3.getValue();
    }
}
