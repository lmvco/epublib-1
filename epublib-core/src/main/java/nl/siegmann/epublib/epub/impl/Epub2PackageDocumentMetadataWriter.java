package nl.siegmann.epublib.epub.impl;

import nl.siegmann.epublib.Constants;
import nl.siegmann.epublib.domain.*;
import nl.siegmann.epublib.epub.PackageDocumentMetadataWriter;
import nl.siegmann.epublib.util.StringUtil;
import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * package document metadata writer for epub2
 *
 * @author LinQ
 * @version 2013-05-29
 */
public class Epub2PackageDocumentMetadataWriter extends PackageDocumentMetadataWriter {

    protected Epub2PackageDocumentMetadataWriter(Book book, XmlSerializer xmlSerializer) {
        super(book, xmlSerializer);
    }

    @Override
    public void writeMetaData() throws IOException {
        serializer.setPrefix(PREFIX_OPF, NAMESPACE_OPF);
        serializer.startTag(NAMESPACE_OPF, OPFTags.metadata);
        serializer.setPrefix(PREFIX_DUBLIN_CORE, NAMESPACE_DUBLIN_CORE);

        writeIdentifiers(book.getMetadata().getIdentifiers(), serializer);
        writeSimpleMetdataElements(DCTags.title, book.getMetadata().getTitles(), serializer);
        writeSimpleMetdataElements(DCTags.subject, book.getMetadata().getSubjects(), serializer);
        writeSimpleMetdataElements(DCTags.description, book.getMetadata().getDescriptions(), serializer);
        writeSimpleMetdataElements(DCTags.publisher, book.getMetadata().getPublishers(), serializer);
        writeSimpleMetdataElements(DCTags.type, book.getMetadata().getTypes(), serializer);
        writeSimpleMetdataElements(DCTags.rights, book.getMetadata().getRights(), serializer);

        // write authors
        for(Author author: book.getMetadata().getAuthors()) {
            serializer.startTag(NAMESPACE_DUBLIN_CORE, DCTags.creator);
            serializer.attribute(NAMESPACE_OPF, OPFAttributes.role, author.getRelator().getCode());
            serializer.attribute(NAMESPACE_OPF, OPFAttributes.file_as, author.getLastname() + ", " + author.getFirstname());
            serializer.text(author.getFirstname() + " " + author.getLastname());
            serializer.endTag(NAMESPACE_DUBLIN_CORE, DCTags.creator);
        }

        // write contributors
        for(Author author: book.getMetadata().getContributors()) {
            serializer.startTag(NAMESPACE_DUBLIN_CORE, DCTags.contributor);
            serializer.attribute(NAMESPACE_OPF, OPFAttributes.role, author.getRelator().getCode());
            serializer.attribute(NAMESPACE_OPF, OPFAttributes.file_as, author.getLastname() + ", " + author.getFirstname());
            serializer.text(author.getFirstname() + " " + author.getLastname());
            serializer.endTag(NAMESPACE_DUBLIN_CORE, DCTags.contributor);
        }

        // write dates
        for (Date date: book.getMetadata().getDates()) {
            serializer.startTag(NAMESPACE_DUBLIN_CORE, DCTags.date);
            if (date.getEvent() != null) {
                serializer.attribute(NAMESPACE_OPF, OPFAttributes.event, date.getEvent().toString());
            }
            serializer.text(date.getValue());
            serializer.endTag(NAMESPACE_DUBLIN_CORE, DCTags.date);
        }

        // write language
        if(book.getMetadata().getLanguages() != null && book.getMetadata().getLanguages().size() > 0) {
            serializer.startTag(NAMESPACE_DUBLIN_CORE, "language");
            serializer.text(book.getMetadata().getLanguages().get(0).getValue());
            serializer.endTag(NAMESPACE_DUBLIN_CORE, "language");
        }

        // write other properties
        if(book.getMetadata().getMetas() != null) {
            for (Meta meta : book.getMetadata().getMetas()) {
                serializer.startTag(NAMESPACE_OPF, OPFTags.meta);
                for (Map.Entry<String, String> entry : meta.getCustomProperties().entrySet()) {
                    serializer.attribute(EMPTY_NAMESPACE_PREFIX, entry.getKey(), entry.getValue());
                }
                serializer.endTag(NAMESPACE_OPF, OPFTags.meta);
            }
        }

        // write coverimage
        if(book.getCoverImage() != null) { // write the cover image
            serializer.startTag(NAMESPACE_OPF, OPFTags.meta);
            serializer.attribute(EMPTY_NAMESPACE_PREFIX, OPFAttributes.name, OPFValues.meta_cover);
            serializer.attribute(EMPTY_NAMESPACE_PREFIX, OPFAttributes.content, book.getCoverImage().getId());
            serializer.endTag(NAMESPACE_OPF, OPFTags.meta);
        }

        // write generator
        serializer.startTag(NAMESPACE_OPF, OPFTags.meta);
        serializer.attribute(EMPTY_NAMESPACE_PREFIX, OPFAttributes.name, OPFValues.generator);
        serializer.attribute(EMPTY_NAMESPACE_PREFIX, OPFAttributes.content, Constants.EPUBLIB_GENERATOR_NAME);
        serializer.endTag(NAMESPACE_OPF, OPFTags.meta);

        serializer.endTag(NAMESPACE_OPF, OPFTags.metadata);
    }

    private void writeSimpleMetdataElements(String tagName, List<DcmesElement> values, XmlSerializer serializer) throws IllegalArgumentException, IllegalStateException, IOException {
        for(DcmesElement element: values) {
            if (StringUtil.isBlank(element.getValue())) {
                continue;
            }
            serializer.startTag(NAMESPACE_DUBLIN_CORE, tagName);
            serializer.text(element.getValue());
            serializer.endTag(NAMESPACE_DUBLIN_CORE, tagName);
        }
    }


    /**
     * Writes out the complete list of Identifiers to the package document.
     * The first identifier for which the bookId is true is made the bookId identifier.
     * If no identifier has bookId == true then the first bookId identifier is written as the primary.
     *
     * @param identifiers identifiers
     * @param serializer serializer
     * @throws IOException
     * @throws IllegalStateException
     * @throws IllegalArgumentException
     * @
     */
    private void writeIdentifiers(List<Identifier> identifiers, XmlSerializer serializer) throws IllegalArgumentException, IllegalStateException, IOException  {
        Identifier bookIdIdentifier = Identifier.getBookIdIdentifier(identifiers);
        if(bookIdIdentifier == null) {
            return;
        }

        serializer.startTag(NAMESPACE_DUBLIN_CORE, DCTags.identifier);
        serializer.attribute(EMPTY_NAMESPACE_PREFIX, DCAttributes.id, book.getUniqueId());
        serializer.attribute(NAMESPACE_OPF, OPFAttributes.scheme, bookIdIdentifier.getScheme());
        serializer.text(bookIdIdentifier.getValue());
        serializer.endTag(NAMESPACE_DUBLIN_CORE, DCTags.identifier);

        for(Identifier identifier: identifiers.subList(1, identifiers.size())) {
            if(identifier == bookIdIdentifier) {
                continue;
            }
            serializer.startTag(NAMESPACE_DUBLIN_CORE, DCTags.identifier);
            serializer.attribute(NAMESPACE_OPF, "scheme", identifier.getScheme());
            serializer.text(identifier.getValue());
            serializer.endTag(NAMESPACE_DUBLIN_CORE, DCTags.identifier);
        }
    }
}
