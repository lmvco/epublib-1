package nl.siegmann.epublib.epub;

import nl.siegmann.epublib.Constants;
import nl.siegmann.epublib.domain.Book;
import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;


/**
 * Writes the opf package document as defined by namespace http://www.idpf.org/2007/opf
 *  
 * @author paul
 *
 */
public abstract class PackageDocumentWriter extends PackageDocumentBase {

    protected Book book;
    protected XmlSerializer serializer;

    public PackageDocumentWriter(Book book, XmlSerializer serializer) {
        this.book = book;
        this.serializer = serializer;
    }

    public void write() throws IOException {
        serializer.startDocument(Constants.CHARACTER_ENCODING, false);
        serializer.setPrefix("", NAMESPACE_OPF);
        serializer.startTag(NAMESPACE_OPF, OPFTags.packageTag);
        serializer.attribute(EpubWriter.EMPTY_NAMESPACE_PREFIX, OPFAttributes.version, getEpubVersion());
        serializer.attribute(EpubWriter.EMPTY_NAMESPACE_PREFIX, OPFAttributes.uniqueIdentifier, book.getUniqueId());
        serializer.setPrefix(PREFIX_DUBLIN_CORE, NAMESPACE_DUBLIN_CORE);

        writeMetadata();
        writeManifest();
        writeSpine();
        writeGuide();
        writeBindings();

        serializer.endTag(NAMESPACE_OPF, OPFTags.packageTag);
        serializer.endDocument();
        serializer.flush();
    }

    /**
     * write package document metadata
     * @throws IOException
     */
    protected abstract void writeMetadata() throws IOException;

    /**
     * wite manifest
     * @throws IOException
     */
    protected abstract void writeManifest() throws IOException;

    /**
     * write spine
     * @throws IOException
     */
    protected abstract void writeSpine() throws IOException;

    /**
     * write guide
     * @throws IOException
     */
    protected abstract void writeGuide() throws IOException;

    /**
     * write bindings
     * @throws IOException
     */
    protected abstract void writeBindings() throws IOException;

    /**
     * get output epub version
     * @return output epub version
     */
    protected abstract String getEpubVersion();

    public Book getBook() {
        return book;
    }
}