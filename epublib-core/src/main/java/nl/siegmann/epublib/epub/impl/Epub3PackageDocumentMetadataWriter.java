package nl.siegmann.epublib.epub.impl;

import nl.siegmann.epublib.domain.*;
import nl.siegmann.epublib.epub.PackageDocumentMetadataWriter;
import nl.siegmann.epublib.util.StringUtil;
import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;
import java.util.List;

/**
 *  package document metadata writer for epub3
 *
 * @author LinQ
 * @version 2013-05-29
 */
public class Epub3PackageDocumentMetadataWriter extends PackageDocumentMetadataWriter {

    protected Epub3PackageDocumentMetadataWriter(Book book, XmlSerializer xmlSerializer) {
        super(book, xmlSerializer);
    }

    @Override
    public void writeMetaData() throws IOException {
        serializer.startTag(NAMESPACE_OPF, OPFTags.metadata);

        writeIdentifiers(book.getMetadata().getIdentifiers());
        writeDcmesElements(DCTags.title, book.getMetadata().getTitles());
        writeDcmesElements(DCTags.language, book.getMetadata().getLanguages());
        writeDcmesElements(DCTags.subject, book.getMetadata().getSubjects());
        writeDcmesElements(DCTags.description, book.getMetadata().getDescriptions());
        writeDcmesElements(DCTags.publisher, book.getMetadata().getPublishers());
        writeDcmesElements(DCTags.type, book.getMetadata().getTypes());
        writeDcmesElements(DCTags.rights, book.getMetadata().getRights());
        writeAuthorElements(DCTags.creator, book.getMetadata().getAuthors());
        writeAuthorElements(DCTags.contributor, book.getMetadata().getContributors());
        writeDcmesElement(DCTags.source, book.getMetadata().getSource());
        writeDcmesElements(DCTags.type, book.getMetadata().getTypes());
        writeLinkElements(book.getMetadata().getLinks());
        if (book.getMetadata().getDates().size() > 0) {
            writeDcmesElement(DCTags.date, book.getMetadata().getDates().get(0));
        }

        // write other properties
        writeMeta(book.getMetadata().getMetas());

        serializer.endTag(NAMESPACE_OPF, OPFTags.metadata);
    }

    private void writeDcmesElements(String tagName, List<DcmesElement> values) throws IllegalArgumentException, IllegalStateException, IOException {
        for(DcmesElement element: values) {
            if (StringUtil.isBlank(element.getValue())) {
                continue;
            }
            writeDcmesElement(tagName, element);
        }
    }

    private void writeDcmesElement(String tagName, DcmesElement element) throws IOException {
        if (element != null && StringUtil.isNotBlank(element.getValue())) {
            serializer.startTag(NAMESPACE_DUBLIN_CORE, tagName);
            if (StringUtil.isNotBlank(element.getId())) {
                serializer.attribute(EMPTY_NAMESPACE_PREFIX, DCAttributes.id, element.getId());
            }
            if (StringUtil.isNotBlank(element.getLang())) {
                serializer.attribute(EMPTY_NAMESPACE_PREFIX, DCAttributes.lang, element.getLang());
            }
            if (StringUtil.isNotBlank(element.getDirection())) {
                serializer.attribute(EMPTY_NAMESPACE_PREFIX, DCAttributes.dir, element.getDirection());
            }
            serializer.text(element.getValue());
            serializer.endTag(NAMESPACE_DUBLIN_CORE, tagName);
            writeMeta(element.getMetas());
        }
    }

    private void writeAuthorElements(String tagName, List<Author> values) throws IOException {
        for (Author author : values) {
            if (StringUtil.isBlank(author.getValue())) {
                continue;
            }
            serializer.startTag(NAMESPACE_DUBLIN_CORE, tagName);
            if (StringUtil.isNotBlank(author.getId())) {
                serializer.attribute(EMPTY_NAMESPACE_PREFIX, DCAttributes.id, author.getId());
            }
            serializer.text(author.getValue());
            serializer.endTag(NAMESPACE_DUBLIN_CORE, tagName);
            writeMeta(author.getMetas());
        }
    }

    private void writeLinkElements(List<Link> links) throws IOException {
        for (Link link : links) {
            serializer.startTag(NAMESPACE_OPF, OPFTags.link);
            if (StringUtil.isNotBlank(link.getRefines())) {
                serializer.attribute(EMPTY_NAMESPACE_PREFIX, DCAttributes.refines, link.getRefines());
            }
            serializer.attribute(EMPTY_NAMESPACE_PREFIX, DCAttributes.rel, link.getRel());
            serializer.attribute(EMPTY_NAMESPACE_PREFIX, DCAttributes.href, link.getHref());
            if (StringUtil.isNotBlank(link.getId())) {
                serializer.attribute(EMPTY_NAMESPACE_PREFIX, DCAttributes.id, link.getId());
            }
            if (StringUtil.isNotBlank(link.getMediaType())) {
                serializer.attribute(EMPTY_NAMESPACE_PREFIX, DCAttributes.mediaType, link.getMediaType());
            }
            serializer.endTag(NAMESPACE_OPF, OPFTags.link);
        }
    }


    /**
     * Writes out the complete list of Identifiers to the package document.
     * The first identifier for which the bookId is true is made the bookId identifier.
     * If no identifier has bookId == true then the first bookId identifier is written as the primary.
     *
     * @param identifiers identifiers
     * @throws IOException
     * @throws IllegalStateException
     * @throws IllegalArgumentException
     * @
     */
    private void writeIdentifiers(List<Identifier> identifiers) throws IllegalArgumentException, IllegalStateException, IOException {
        for (Identifier identifier : identifiers) {
            writeDcmesElement(DCTags.identifier, identifier);
        }
    }

    private void writeMeta(List<Meta> metas) throws IOException {
        for (Meta meta : metas) {
            if (StringUtil.isBlank(meta.getProperty()))
                continue;
            serializer.startTag(NAMESPACE_OPF, DCTags.meta);
            if (StringUtil.isNotBlank(meta.getRefines())) {
                serializer.attribute(EMPTY_NAMESPACE_PREFIX, DCAttributes.refines, meta.getRefines());
            }
            serializer.attribute(EMPTY_NAMESPACE_PREFIX, DCAttributes.property, meta.getProperty());
            if (StringUtil.isNotBlank(meta.getScheme())) {
                serializer.attribute(EMPTY_NAMESPACE_PREFIX, DCAttributes.scheme, meta.getScheme());
            }
            serializer.text(meta.getValue());
            serializer.endTag(NAMESPACE_OPF, DCTags.meta);
        }
    }
}
