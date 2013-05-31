package nl.siegmann.epublib.epub;

import nl.siegmann.epublib.domain.Book;
import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;

public abstract class PackageDocumentMetadataWriter extends PackageDocumentBase {
    protected Book book;
    protected XmlSerializer serializer;

    protected PackageDocumentMetadataWriter(Book book, XmlSerializer xmlSerializer) {
        this.book = book;
        this.serializer = xmlSerializer;
    }

    public abstract void writeMetaData() throws IOException;
}
