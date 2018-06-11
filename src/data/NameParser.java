package data;

public class NameParser {
    String text;
    String name,date; int type;
    public NameParser(String text) {
        this.text = text;
        int x = text.indexOf('-');
        name = text.substring(0,x-1);
        type = text.charAt(x-1)-'0';
        date = text.substring(x+1);
    }
    public String withoutType() {
        return name+"-"+date;
    }

    public String getName() {
        return name;
    }

    public String getDate() {
        return date;
    }

    public int getType() {
        return type;
    }
}