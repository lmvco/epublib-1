package nl.siegmann.epublib.epub;

import nl.siegmann.epublib.domain.*;
import nl.siegmann.epublib.epub.impl.Epub2PackageDocumentWriter;
import nl.siegmann.epublib.epub.impl.Epub3PackageDocumentWriter;
import nl.siegmann.epublib.service.MediatypeService;
import nl.siegmann.epublib.util.IOUtil;
import nl.siegmann.epublib.util.StringUtil;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmlpull.v1.XmlSerializer;

import java.io.*;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Generates an epub file. Not thread-safe, single use object.
 * 
 * @author paul
 *
 */
public class EpubWriter {
	
	private final static Logger log = LoggerFactory.getLogger(EpubWriter.class); 
	
	// package
	static final String EMPTY_NAMESPACE_PREFIX = "";
	
	private BookProcessor bookProcessor = BookProcessor.IDENTITY_BOOKPROCESSOR;

	public EpubWriter() {
		this(BookProcessor.IDENTITY_BOOKPROCESSOR);
	}
	
	
	public EpubWriter(BookProcessor bookProcessor) {
		this.bookProcessor = bookProcessor;
	}


	public void write(Book book, OutputStream out) throws IOException {
		write(book, out, Version.V2);
    }

    public void writeEpub3(Book book, OutputStream out) throws IOException{
        write(book, out, Version.V3);
    }

    public void write(Book book, OutputStream out, Version version) throws IOException {
        book = processBook(book);
        ZipOutputStream resultStream = new ZipOutputStream(out);
        writeMimeType(resultStream);
        writeContainer(resultStream);
        if (book.getResources().containsByHref("META-INF/encryption.xml")) {
            writeEncryptFile(resultStream, book);
        }
        initTOCResource(book);
        if (version == Version.V3) {
            initNavResource(book);
        }
        writeResources(book, resultStream);
        writePackageDocument(book, resultStream, version);
        resultStream.close();
        if (StringUtil.isNotBlank(book.getZipPath())) {
            FileUtils.deleteQuietly(new File(book.getZipPath()));
        }
    }

	private Book processBook(Book book) {
		if (bookProcessor != null) {
			book = bookProcessor.processBook(book);
		}
		return book;
	}

	private void initTOCResource(Book book) {
		Resource tocResource;
		try {
			tocResource = NCXDocument.createNCXResource(book);
			Resource currentTocResource = book.getSpine().getTocResource();
			if (currentTocResource != null) {
				book.getResources().remove(currentTocResource.getHref());
                book.getManifest().removeManifestItem(currentTocResource.getHref());
            }
			book.getSpine().setTocResource(tocResource);
			book.getResources().add(tocResource);
            book.getManifest().addReference(new ManifestItemReference(tocResource, null));
        } catch (Exception e) {
			log.error("Error writing table of contents: " + e.getClass().getName() + ": " + e.getMessage());
		}
	}

    private void initNavResource(Book book) {
        if (book.getNavResource() != null)
            return;
        Resource navResource;
        try {
            navResource = NavDocument.createNavResource(book);
            book.getResources().add(navResource);
            book.getManifest().addReference(new ManifestItemReference(navResource, ManifestItemProperties.NAV));
        } catch (IOException e) {
            log.error("Error writeing nav document: " + e.getClass().getName() + ": " + e.getMessage());
        }
    }


    private void writeResources(Book book, ZipOutputStream resultStream) throws IOException {
		for(Resource resource: book.getResources().getAll()) {
			writeResource(resource, resultStream);
		}
	}

	/**
	 * Writes the resource to the resultStream.
	 * 
	 * @param resource
	 * @param resultStream
	 * @throws IOException
	 */
	private void writeResource(Resource resource, ZipOutputStream resultStream)
			throws IOException {
		if(resource == null) {
			return;
		}
		try {
			resultStream.putNextEntry(new ZipEntry("OEBPS/" + resource.getHref()));
			InputStream inputStream = resource.getInputStream();
			IOUtil.copy(inputStream, resultStream);
			inputStream.close();
		} catch(Exception e) {
			log.error(e.getMessage(), e);
		}
	}
	

	private void writePackageDocument(Book book, ZipOutputStream resultStream, Version version) throws IOException {
		resultStream.putNextEntry(new ZipEntry("OEBPS/content.opf"));
		XmlSerializer xmlSerializer = EpubProcessorSupport.createXmlSerializer(resultStream);
        PackageDocumentWriter writer;
        if (version == Version.V2) {
            writer = new Epub2PackageDocumentWriter(book, xmlSerializer);
       } else {
            writer = new Epub3PackageDocumentWriter(book, xmlSerializer);
        }

        writer.write();
		xmlSerializer.flush();
	}

	/**
	 * Writes the META-INF/container.xml file.
	 * 
	 * @param resultStream
	 * @throws IOException
	 */
	private void writeContainer(ZipOutputStream resultStream) throws IOException {
		resultStream.putNextEntry(new ZipEntry("META-INF/container.xml"));
		Writer out = new OutputStreamWriter(resultStream);
		out.write("<?xml version=\"1.0\"?>\n");
		out.write("<container version=\"1.0\" xmlns=\"urn:oasis:names:tc:opendocument:xmlns:container\">\n");
		out.write("\t<rootfiles>\n");
		out.write("\t\t<rootfile full-path=\"OEBPS/content.opf\" media-type=\"application/oebps-package+xml\"/>\n");
		out.write("\t</rootfiles>\n");
		out.write("</container>");
		out.flush();
	}
    
    /**
	 * Writes the META-INF/encryption.xml file.
     *
     * @param resultStream
     * @throws IOException
     */
    private void writeEncryptFile(ZipOutputStream resultStream, Book book) throws IOException {
        resultStream.putNextEntry(new ZipEntry("META-INF/encryption.xml"));

        InputStream inputStream = book.getResources().getByHref("META-INF/encryption.xml").getInputStream();
        book.getResources().remove("META-INF/encryption.xml");
        IOUtil.copy(inputStream, resultStream);
        inputStream.close();
    }

	/**
	 * Stores the mimetype as an uncompressed file in the ZipOutputStream.
	 * 
	 * @param resultStream
	 * @throws IOException
	 */
	private void writeMimeType(ZipOutputStream resultStream) throws IOException {
		ZipEntry mimetypeZipEntry = new ZipEntry("mimetype");
		mimetypeZipEntry.setMethod(ZipEntry.STORED);
		byte[] mimetypeBytes = MediatypeService.EPUB.getName().getBytes();
		mimetypeZipEntry.setSize(mimetypeBytes.length);
		mimetypeZipEntry.setCrc(calculateCrc(mimetypeBytes));
		resultStream.putNextEntry(mimetypeZipEntry);
		resultStream.write(mimetypeBytes);
	}

	private long calculateCrc(byte[] data) {
		CRC32 crc = new CRC32();
		crc.update(data);
		return crc.getValue();
	}

	String getNcxId() {
		return "ncx";
	}
	
	String getNcxHref() {
		return "toc.ncx";
	}

	String getNcxMediaType() {
		return "application/x-dtbncx+xml";
	}

	public BookProcessor getBookProcessor() {
		return bookProcessor;
	}
	
	
	public void setBookProcessor(BookProcessor bookProcessor) {
		this.bookProcessor = bookProcessor;
	}
	
}
