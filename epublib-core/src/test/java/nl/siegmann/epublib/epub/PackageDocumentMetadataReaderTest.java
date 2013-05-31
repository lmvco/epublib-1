package nl.siegmann.epublib.epub;

import junit.framework.Assert;
import junit.framework.TestCase;
import nl.siegmann.epublib.domain.Metadata;
import nl.siegmann.epublib.domain.Resources;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import java.io.FileInputStream;
import java.io.IOException;

public class PackageDocumentMetadataReaderTest extends TestCase {
	
	public void test1() {
		try {
			Document document = EpubProcessorSupport.createDocumentBuilder().parse(PackageDocumentMetadataReader.class.getResourceAsStream("/opf/test2.opf"));
			Resources resources = new Resources();
			Metadata metadata = PackageDocumentMetadataReader.readMetadata(document, resources);
			assertEquals(1, metadata.getAuthors().size());
		} catch (Exception e) {
			e.printStackTrace();
			assertTrue(false);
		}
	}

    public void testEpub3() {
        try {
            Document document = EpubProcessorSupport.createDocumentBuilder().parse(PackageDocumentMetadataReader.class.getResourceAsStream("/opf/test3.opf"));
            Resources resources = new Resources();
            Metadata metadata = PackageDocumentMetadataReader.readMetadata(document, resources);
            Assert.assertEquals("Creative Commons - A Shared Culture", metadata.getTitles().get(0).getValue());
            Assert.assertEquals("Jesse Dylan", metadata.getAuthors().get(0).getValue());
            Assert.assertEquals("code.google.com.epub-samples.cc-shared-culture", metadata.getIdentifiers().get(0).getValue());
            Assert.assertEquals("en-US", metadata.getLanguages().get(0).getValue());
            Assert.assertEquals("Creative Commons", metadata.getPublishers().get(0).getValue());
            Assert.assertEquals("mgylling", metadata.getContributors().get(0).getValue());
            Assert.assertEquals("Multiple video tests (see Navigation Document (toc) for details)", metadata.getDescriptions().get(0).getValue());
            Assert.assertEquals("This work is licensed under a Creative Commons Attribution-Noncommercial-Share Alike (CC BY-NC-SA) license.", metadata.getRights().get(0).getValue());
            Assert.assertEquals(3, metadata.getLinks().size());
            Assert.assertEquals(2, metadata.getMetas().size());
            System.out.println(metadata.getMetas());
        } catch (Exception e) {
            e.printStackTrace();
            assertTrue(false);
        }
    }

    public void testReadsLanguage() {
        Metadata metadata = getMetadata("/opf/test_language.opf");
        assertEquals("fi", metadata.getLanguages());
    }

    public void testDefaultsToEnglish() {
        Metadata metadata = getMetadata("/opf/test_default_language.opf");
        assertEquals("en", metadata.getLanguages());
    }

    private Metadata getMetadata(String file) {
        try {
            Document document = EpubProcessorSupport.createDocumentBuilder().parse(PackageDocumentMetadataReader.class.getResourceAsStream(file));
            Resources resources = new Resources();

            return PackageDocumentMetadataReader.readMetadata(document, resources);
        } catch (Exception e) {
            e.printStackTrace();
            assertTrue(false);

            return null;
        }
    }

    public static void main(String[] args) throws IOException, SAXException {
        Document document = EpubProcessorSupport.createDocumentBuilder().parse(new FileInputStream("F:\\TDDOWNLOAD\\package.opf"));
        Resources resources = new Resources();
        Metadata metadata = PackageDocumentMetadataReader.readMetadata(document, resources);
        System.out.println(metadata.getLinks());
    }
}
