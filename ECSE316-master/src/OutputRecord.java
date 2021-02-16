/**
 * this class contains the basic information for a record
 * useful when print records
 */
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


    /**
     * this method prints the formatted information for each output record
     */
    public void printAnswer() {
        QueryType qType = this.queryType;
        String auth = this.isAnth() ? "auth" : "nonauth";
        if(qType.equals(QueryType.A)) {
            System.out.println("IP\t"+this.rData+"\t"+this.ttl+"\t"+ auth);
        }
        if(qType.equals(QueryType.CNAME)) {
            System.out.println("CNAME\t"+this.rData+"\t"+this.ttl+"\t"+ auth);
        }
        if(qType.equals(QueryType.MX)) {
            System.out.println("MX\t"+this.rData+"\t"+this.ttl+"\t"+ auth);
        }
        if(qType.equals(QueryType.NS)) {
            System.out.println("NS\t"+this.rData+"\t"+this.ttl+"\t"+ auth);
        }
    }
}

