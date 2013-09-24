package openbrain.peoplesearch.wiki;

public interface IConstants {
	
	public static String FIELD_PAGE = "page";
	public static String FIELD_ID = "id";
	public static String FIELD_TITLE = "title";
	public static String FIELD_ANCHOR = "anchor";
	public static String FIELD_HEADING = "heading";
	public static String FIELD_TEXT = "text";
	public static String FIELD_ALL = "all";
	
	public static String REGEX = "([.,!&?/():*@%#|;\"]|\\s)+";
	public static String TITLE_REGEX = "title";
	public static String ANCHOR_REGEX = "a";
	
	public static String NER_CLASSIFIER = "classifiers/english.muc.7class.distsim.crf.ser.gz";
	
	public static String WIKI_MEDIA_FIRST_LINE = "<mediawiki xmlns=\"http://www.mediawiki.org/xml/export-0.6/\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://www.mediawiki.org/xml/export-0.6/ http://www.mediawiki.org/xml/export-0.6.xsd\" version=\"0.6\" xml:lang=\"en\">";
}
