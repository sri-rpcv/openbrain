package openbrain.peoplesearch.wiki;

public class WikiPage {

	private String title = "";
	private String text = "";
	private int id;
	
	public WikiPage() {
	}

	public void setText(String text) {
		this.text = text;
	}
	
	public String getText() {
		return text;
	}

	public void setTitle(String title) {
		this.title = title;
	}
	
	public String getTitle() {
		return title;
	}
	
	public void setId(int id) {
		this.id = id;
	}
	
	public int getId() {
		return id;
	}
	
	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("<page>\n");
		sb.append("<id>");
		sb.append(getId());
		sb.append("</id>\n");
		sb.append("<title>");
		sb.append(getTitle());
		sb.append("</title>\n");
		sb.append("<text>\n");
		sb.append(getText());
		sb.append("\n</text>");
		sb.append("\n</page>");
		return sb.toString();
	}
}
