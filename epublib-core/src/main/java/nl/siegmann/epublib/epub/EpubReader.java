package nl.siegmann.epublib.epub;

import nl.siegmann.epublib.Constants;
import nl.siegmann.epublib.domain.*;
import nl.siegmann.epublib.service.MediatypeService;
import nl.siegmann.epublib.util.IOUtil;
import nl.siegmann.epublib.util.ResourceUtil;
import nl.siegmann.epublib.util.StringUtil;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.*;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

/**
 * Reads an epub file.
 * 
 * @author paul
 *
 */
public class EpubReader {

	private static final Logger log = LoggerFactory.getLogger(EpubReader.class);
    private static final String UNZIP_PATH = "tmp/";
    private static final int LIMIT_SIZE = 100 * 1024 * 1024;
    private BookProcessor bookProcessor = BookProcessor.IDENTITY_BOOKPROCESSOR;

    /**
	 * Reads this EPUB if file size bigger than LIMIT_SIZE, will read lazily, else will all read into memory
	 *
	 * @param fileName the file to load
	 * @param encoding the encoding for XHTML files
	 *
	 * @return the book represents the epub
	 * @throws IOException
	 */
	public Book readEpub( String fileName, String encoding ) throws IOException {
		return readEpub(fileName, encoding, Arrays.asList(MediatypeService.mediatypes) );
	}

    /**
     * read epub all into memory
     * @param inputStream inputStream
     * @return book
     * @throws IOException
     */
    public Book readEpub(InputStream inputStream) throws IOException {
        Resources resources = readResources(new ZipInputStream(inputStream), Constants.CHARACTER_ENCODING);
        return readEpub(resources);
    }

    /**
     * read epub all into memory
     * @param inputStream inputStream
     * @param encoding resource encoding
     * @return book domain
     * @throws IOException
     */
    public Book readEpub(InputStream inputStream, String encoding) throws IOException {
        Resources resources = readResources(new ZipInputStream(inputStream), encoding);
        return readEpub(resources);
    }

    /**
     * Reads this EPUB if file size bigger than LIMIT_SIZE, will read lazily, else will all read into memory
     *
     * @param fileName the file to load
     * @param encoding the encoding for XHTML files
     * @param lazyLoadedTypes a list of the MediaType to load lazily
     * @return book
     * @throws IOException
     */
    public Book readEpub( String fileName, String encoding, List<MediaTypeProperty> lazyLoadedTypes ) throws IOException {
        Book result = new Book();
        Resources resources;
        if (FileUtils.sizeOf(new File(fileName)) >= LIMIT_SIZE) {
            String outPath = UNZIP_PATH + Thread.currentThread().getId() + "_" + System.currentTimeMillis();
            unZip(new File(fileName), outPath);
            resources = readLazyResources(fileName, outPath, encoding, lazyLoadedTypes);
            result.setZipPath(outPath);
        } else {
            resources = readResources(new ZipInputStream(new FileInputStream(fileName)), Constants.CHARACTER_ENCODING);
        }
        return readEpub(resources);
    }

    public Book readEpub(Resources resources) {
        Book result = new Book();
        handleMimeType(result, resources);
        String packageResourceHref = getPackageResourceHref(resources);
        Resource packageResource = processPackageResource(packageResourceHref, result, resources);
        result.setOpfResource(packageResource);
        Resource ncxResource = processNcxResource(packageResource, result);
        result.setNcxResource(ncxResource);
        Resource navResource = processNavResource(result);
        result.setNavResource(navResource);
        result = postProcessBook(result);
        if (resources.containsByHref("META-INF/encryption.xml")) {
            result.getResources().add(resources.getByHref("META-INF/encryption.xml"));
        }
        return result;
    }

	private Book postProcessBook(Book book) {
		if (bookProcessor != null) {
			book = bookProcessor.processBook(book);
		}
		return book;
	}

	private Resource processNcxResource(Resource packageResource, Book book) {
		return NCXDocument.read(book, this);
	}

    private Resource processNavResource(Book book) {
        return NavDocument.read(book);
    }

	private Resource processPackageResource(String packageResourceHref, Book book, Resources resources) {
		Resource packageResource = resources.remove(packageResourceHref);
		try {
			PackageDocumentReader.read(packageResource, this, book, resources);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		return packageResource;
	}

	private String getPackageResourceHref(Resources resources) {
		String defaultResult = "OEBPS/content.opf";
		String result = defaultResult;

		Resource containerResource = resources.remove("META-INF/container.xml");
		if(containerResource == null) {
			return result;
		}
		try {
			Document document = ResourceUtil.getAsDocument(containerResource);
			Element rootFileElement = (Element) ((Element) document.getDocumentElement().getElementsByTagName("rootfiles").item(0)).getElementsByTagName("rootfile").item(0);
			result = rootFileElement.getAttribute("full-path");
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		if(StringUtil.isBlank(result)) {
			result = defaultResult;
		}
		return result;
	}

	private void handleMimeType(Book result, Resources resources) {
		resources.remove("mimetype");
	}
	
	private Resources readLazyResources( String fileName, String outPath, String defaultHtmlEncoding,
			List<MediaTypeProperty> lazyLoadedTypes) throws IOException {
				
		ZipInputStream in = new ZipInputStream(new FileInputStream(fileName));
		
		Resources result = new Resources();
		for(ZipEntry zipEntry = in.getNextEntry(); zipEntry != null; zipEntry = in.getNextEntry()) {
			if(zipEntry.isDirectory()) {
				continue;
			}
			
			String href = zipEntry.getName();
			MediaTypeProperty mediaTypeProperty = MediatypeService.determineMediaType(href);
			
			Resource resource;
			
			if ( lazyLoadedTypes.contains(mediaTypeProperty) ) {
				resource = new Resource(outPath + "/" + href, zipEntry.getSize(), href);
			} else {			
				resource = new Resource( in, outPath + "/" + href, (int) zipEntry.getSize(), href );
			}
			
			if(resource.getMediaTypeProperty() == MediatypeService.XHTML) {
				resource.setInputEncoding(defaultHtmlEncoding);
			}
			result.add(resource);
		}
		
		return result;
	}	

	private Resources readResources(ZipInputStream in, String defaultHtmlEncoding) throws IOException {
		Resources result = new Resources();
		for(ZipEntry zipEntry = in.getNextEntry(); zipEntry != null; zipEntry = in.getNextEntry()) {
			if(zipEntry.isDirectory()) {
				continue;
			}
			Resource resource = ResourceUtil.createResource(zipEntry, in);
			if(resource.getMediaTypeProperty() == MediatypeService.XHTML) {
				resource.setInputEncoding(defaultHtmlEncoding);
			}
			result.add(resource);
		}
		return result;
	}

    public static void unZip(File file, String destDir) throws IOException {
        ZipFile zipFile;
        zipFile = new ZipFile(file);
        Enumeration enumeration = zipFile.entries();
        ZipEntry zipEntry;
        while (enumeration.hasMoreElements()) {
            zipEntry = (ZipEntry) enumeration.nextElement();
            File loadFile = new File(destDir + "/" + zipEntry.getName());
            if (zipEntry.isDirectory()) {
                loadFile.mkdirs();
            } else {
                if (!loadFile.getParentFile().exists())
                    loadFile.getParentFile().mkdirs();
                OutputStream outputStream = new FileOutputStream(loadFile);
                InputStream inputStream = zipFile.getInputStream(zipEntry);
                IOUtil.copy(inputStream, outputStream);
                inputStream.close();
                outputStream.close();
            }
        }
    }
}
