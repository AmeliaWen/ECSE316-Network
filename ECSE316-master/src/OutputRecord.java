public class OutputRecord {
    private boolean anth;
    private QueryType queryType;
    private int ttl;
    private String rData;
    private int size;

    public int getSize() {
        return size;
    }

    public boolean isAnth() {
        return anth;
    }

    public int getTtl() {
        return ttl;
    }

    public QueryType getQueryType() {
        return queryType;
    }

    public String getrData() {
        return rData;
    }

    public void setQueryType(QueryType queryType) {
        this.queryType = queryType;
    }

    public void setAnth(boolean anth) {
        this.anth = anth;
    }

    public void setrData(String rData) {
        this.rData = rData;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public void setTtl(int ttl) {
        this.ttl = ttl;
    }
}

