package nl.siegmann.epublib.epub;

import junit.framework.TestCase;
import nl.siegmann.epublib.domain.Manifest;
import org.w3c.dom.Document;

import java.util.Collection;

public class PackageDocumentReaderTest extends TestCase {
	
	public void testFindCoverHref_content1() {
		EpubReader epubReader = new EpubReader();
		Document packageDocument;
		try {
			packageDocument = EpubProcessorSupport.createDocumentBuilder().parse(PackageDocumentReaderTest.class.getResourceAsStream("/opf/test1.opf"));
			Collection<String> coverHrefs = PackageDocumentReader.findCoverHrefs(packageDocument, new Manifest());
			assertEquals(1, coverHrefs.size());
			assertEquals("cover.html", coverHrefs.iterator().next());
		} catch (Exception e) {
			e.printStackTrace();
			assertTrue(false);
		}
	}
}
