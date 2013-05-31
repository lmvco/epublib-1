package nl.siegmann.epublib.epub.impl;

import nl.siegmann.epublib.domain.*;
import nl.siegmann.epublib.epub.PackageDocumentWriter;
import nl.siegmann.epublib.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;

/**
 * package document writer for epub2
 *
 * @author LinQ
 * @version 2013-05-29
 */
public class Epub2PackageDocumentWriter extends PackageDocumentWriter {
    private static final Logger logger = LoggerFactory.getLogger(Epub2PackageDocumentWriter.class);

    public Epub2PackageDocumentWriter(Book book, XmlSerializer serializer) {
        super(book, serializer);
    }

    @Override
    protected void writeMetadata() throws IOException {
        new Epub2PackageDocumentMetadataWriter(book, serializer).writeMetaData();
    }

    @Override
    protected void writeManifest() throws IOException {
        serializer.startTag(NAMESPACE_OPF, OPFTags.manifest);

        for(ManifestItemReference reference : book.getManifest().getReferences()) {
            writeItem(book, reference, serializer);
        }

        serializer.endTag(NAMESPACE_OPF, OPFTags.manifest);
    }

    private void writeItem(Book book, ManifestItemReference reference, XmlSerializer serializer) throws IllegalArgumentException, IllegalStateException, IOException {
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
        serializer.endTag(NAMESPACE_OPF, OPFTags.item);
    }

    @Override
    protected void writeSpine() throws IOException {
        serializer.startTag(NAMESPACE_OPF, OPFTags.spine);
        serializer.attribute(EMPTY_NAMESPACE_PREFIX, OPFAttributes.toc, book.getSpine().getTocResource().getId());

        if(book.getCoverPage() != null // there is a cover page
                &&	book.getSpine().findFirstResourceById(book.getCoverPage().getId()) < 0) { // cover page is not already in the spine
            // write the cover html file
            serializer.startTag(NAMESPACE_OPF, OPFTags.itemref);
            serializer.attribute(EMPTY_NAMESPACE_PREFIX, OPFAttributes.idref, book.getCoverPage().getId());
            serializer.attribute(EMPTY_NAMESPACE_PREFIX, OPFAttributes.linear, "no");
            serializer.endTag(NAMESPACE_OPF, OPFTags.itemref);
        }
        writeSpineItems(book.getSpine(), serializer);
        serializer.endTag(NAMESPACE_OPF, OPFTags.spine);
    }

    /**
     * List all spine references
     * @throws IOException
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
            serializer.endTag(NAMESPACE_OPF, OPFTags.itemref);
        }
    }

    @Override
    protected void writeGuide() throws IOException {
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
    protected void writeBindings() {
    }

    @Override
    protected String getEpubVersion() {
        return Version.V2.getValue();
    }
}
