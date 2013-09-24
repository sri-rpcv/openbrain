package openbrain.peoplesearch.wiki;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class WikiPageHandler extends DefaultHandler {

	private WikiPage wikiPage = null;
	private String value = "";
	private StringBuffer sb = new StringBuffer();

	@Override
	public void startElement(String uri, String localName, String qName,
			Attributes attributes) throws SAXException {
		if (qName.equals(IConstants.FIELD_PAGE)) {
			wikiPage = new WikiPage();
		}
	}

	@Override
	public void characters(char[] ch, int start, int length)
			throws SAXException {
		value = new String(ch, start, length);
		sb.append(value);
		sb.append(" ");
	}

	@Override
	public void endElement(String uri, String localName, String qName)
			throws SAXException {
		if (qName.equals(IConstants.FIELD_PAGE)) {
			if (wikiPage != null) {
				WikiPageExtractor.addWikiPage(wikiPage);
			}
		} else if (qName.equals(IConstants.FIELD_TITLE)) {
			wikiPage.setTitle(sb.toString());
		} else if (qName.equals(IConstants.FIELD_TEXT)) {
			wikiPage.setText(sb.toString());
		}
		sb = new StringBuffer();
	}
}
