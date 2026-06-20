package pojo;

/**
 * The "support" block returned by reqres.in responses.
 * Included to show how arbitrarily nested objects are mapped during deserialisation.
 */
public class Support {

    private String url;
    private String text;

    public String getUrl()          { return url; }
    public void   setUrl(String u)  { this.url = u; }

    public String getText()          { return text; }
    public void   setText(String t)  { this.text = t; }
}
