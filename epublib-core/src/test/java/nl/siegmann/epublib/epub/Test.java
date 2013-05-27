package nl.siegmann.epublib.epub;

import nl.siegmann.epublib.domain.Book;
import org.xml.sax.SAXException;

import java.io.FileInputStream;
import java.io.IOException;

/**
 * TODO
 *
 * @author LinQ
 * @version 2013-3-20
 */
public class Test {
    public static void main(String[] args) throws IOException, SAXException {
        Book book = new EpubReader().readEpub(new FileInputStream("F:\\TDDOWNLOAD\\test.epub"));
//        new EpubWriter().write(book, new FileOutputStream("F:\\TDDOWNLOAD\\zz.epub"));
        System.out.println(book.getTitle());
    }
}
