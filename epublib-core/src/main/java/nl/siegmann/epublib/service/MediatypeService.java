package nl.siegmann.epublib.service;

import nl.siegmann.epublib.domain.MediaTypeProperty;
import nl.siegmann.epublib.util.StringUtil;

import java.util.HashMap;
import java.util.Map;


/**
 * Manages mediatypes that are used by epubs
 * 
 * @author paul
 *
 */
public class MediatypeService {

	public static final MediaTypeProperty XHTML = new MediaTypeProperty("application/xhtml+xml", ".xhtml", new String[] {".htm", ".html", ".xhtml"});
	public static final MediaTypeProperty EPUB = new MediaTypeProperty("application/epub+zip", ".epub");
	public static final MediaTypeProperty NCX = new MediaTypeProperty("application/x-dtbncx+xml", ".ncx");
	
	public static final MediaTypeProperty JAVASCRIPT = new MediaTypeProperty("text/javascript", ".js");
	public static final MediaTypeProperty CSS = new MediaTypeProperty("text/css", ".css");

	// images
	public static final MediaTypeProperty JPG = new MediaTypeProperty("image/jpeg", ".jpg", new String[] {".jpg", ".jpeg"});
	public static final MediaTypeProperty PNG = new MediaTypeProperty("image/png", ".png");
	public static final MediaTypeProperty GIF = new MediaTypeProperty("image/gif", ".gif");
	
	public static final MediaTypeProperty SVG = new MediaTypeProperty("image/svg+xml", ".svg");

	// fonts
	public static final MediaTypeProperty TTF = new MediaTypeProperty("application/x-truetype-font", ".ttf");
	public static final MediaTypeProperty OPENTYPE = new MediaTypeProperty("application/vnd.ms-opentype", ".otf");
	public static final MediaTypeProperty WOFF = new MediaTypeProperty("application/font-woff", ".woff");
	
	// audio
	public static final MediaTypeProperty MP3 = new MediaTypeProperty("audio/mpeg", ".mp3");
	public static final MediaTypeProperty MP4 = new MediaTypeProperty("audio/mp4", ".mp4");
	public static final MediaTypeProperty OGG = new MediaTypeProperty("audio/ogg", ".ogg");

	public static final MediaTypeProperty SMIL = new MediaTypeProperty("application/smil+xml", ".smil");
	public static final MediaTypeProperty XPGT = new MediaTypeProperty("application/adobe-page-template+xml", ".xpgt");
	public static final MediaTypeProperty PLS = new MediaTypeProperty("application/pls+xml", ".pls");
	
	public static MediaTypeProperty[] mediatypes = new MediaTypeProperty[] {
		XHTML, EPUB, JPG, PNG, GIF, CSS, SVG, TTF, NCX, XPGT, OPENTYPE, WOFF, SMIL, PLS, JAVASCRIPT, MP3, MP4, OGG
	};
	
	public static Map<String, MediaTypeProperty> mediaTypesByName = new HashMap<String, MediaTypeProperty>();
	static {
		for(int i = 0; i < mediatypes.length; i++) {
			mediaTypesByName.put(mediatypes[i].getName(), mediatypes[i]);
		}
	}
	
	public static boolean isBitmapImage(MediaTypeProperty mediaTypeProperty) {
		return mediaTypeProperty == JPG || mediaTypeProperty == PNG || mediaTypeProperty == GIF;
	}
	
	/**
	 * Gets the MediaType based on the file extension.
	 * Null of no matching extension found.
	 * 
	 * @param filename
	 * @return
	 */
	public static MediaTypeProperty determineMediaType(String filename) {
		for(int i = 0; i < mediatypes.length; i++) {
			MediaTypeProperty mediatype = mediatypes[i];
			for(String extension: mediatype.getExtensions()) {
				if(StringUtil.endsWithIgnoreCase(filename, extension)) {
					return mediatype;
				}
			}
		}
		return null;
	}

	public static MediaTypeProperty getMediaTypeByName(String mediaTypeName) {
		return mediaTypesByName.get(mediaTypeName);
	}

    public static MediaTypeProperty getMediaType(String href, String mediaTypeName) {
        MediaTypeProperty mediaTypeProperty = getMediaTypeByName(mediaTypeName);
        if (mediaTypeProperty != null)
            return mediaTypeProperty;
        String extention = StringUtil.substringAfterLast(href, '.');
        mediaTypeProperty = new MediaTypeProperty(mediaTypeName, extention);
        return mediaTypeProperty;
    }

    public static void main(String[] args) {
        System.out.println(JPG.getDefaultExtension());
    }
}
