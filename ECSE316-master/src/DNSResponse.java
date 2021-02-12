import java.io.OutputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

public class DNSResponse {
    private byte[] response;
    private boolean AA;
    private int NSCount;
    private int ANCount;
    private int ARCount;
    private int QDCount;
    private int offset ;
    private int querySize;
    private OutputRecord [] answers;
    private OutputRecord [] additionals;
    int pointerOffset = 0;
    class Pair {
        String name;
        int index;
    }
    public DNSResponse (byte[] response, int qSize){
        this.response = response;
        this.querySize = qSize;
        this.offset = querySize;
        this.AA = getBit(response[2], 2) == 1;
        this.validateResponse();
        this.updateRecordCounts();
        answers = new OutputRecord[ANCount];
        additionals = new OutputRecord[ARCount];
    }
    public OutputRecord[] getAnswers() {
        return answers;
    }

    public OutputRecord[] getAdditionals() {
        return additionals;
    }

    private void validateResponse() {
        if(response.length <12){
            throw new RuntimeException("wrong response size");
        }
        if (getBit(this.response[2],7) != 1) {
            throw new RuntimeException("ERROR\tReceived response is a query, not a response.");
        }
        if (getBit(this.response[3],7) != 1) {
            throw new RuntimeException("ERROR\tServer does not support recursive queries.");
        }
        switch(getRCode(this.response[3])){
            case 1:
                throw new RuntimeException("ERROR\tInvalid format, the name server was unable to interpret the query.");
            case 2:
                throw new RuntimeException("ERROR\tServer failure, the name server was unable to process this query due to a problem with the name server.");
            case 3:
                throw new RuntimeException("ERROR\tName error, the domain name referenced in the query does not exist.");
            case 4:
                throw new RuntimeException("ERROR\tNot implemented, the name server does not support the requested kind of query.");
            case 5:
                throw new RuntimeException("ERROR\tRefused, the name server refuses to perform the requested operation for policy reasons.");
            default:
                break;
        }
    }
    private void updateRecordCounts() {
        byte[] ANCount = {this.response[6], this.response[7]};
        this.ANCount = getWord(ANCount);
        byte[] NSCount = {this.response[8], this.response[9]};
        this.NSCount = getWord(NSCount);
        byte[] ARCount = {this.response[10], this.response[11]};
        this.ARCount = getWord(ARCount);
    }

    public void readAnswers () throws Exception{
        int index = offset;
        for(int i = 0; i < ANCount; i ++){
            System.out.println(index);
            answers[i] = getAnswer(index);
            index += answers[i].getSize();
        }
        //ignore authority section
        for(int i = 0; i < NSCount; i++){
            //answers[i] = getAnswer(index);
            index += getAnswer(index).getSize();
        }

        for(int i = 0; i < ARCount; i++){
            additionals[i] = getAnswer(index);
            index += additionals[i].getSize();
        }


    }
    private OutputRecord getAnswer (int index) throws Exception{
        OutputRecord r = new OutputRecord();
        r.setAnth(AA);
        int readByte = index;
        Pair p = getDomain(readByte);
        //Pair p = readWordFromIndex(readByte);
        readByte += (int) p.index;
        byte[] ans_type = new byte[2];
        ans_type[0] = response[readByte];
        ans_type[1] = response[readByte+1];
        QueryType qt = getQueryTypeFromBytes(ans_type);
        r.setQueryType(qt);
        readByte+=2;
        byte[] bClass = { response[readByte], response[readByte + 1] };
        ByteBuffer buf = ByteBuffer.wrap(bClass);
        short qClass = buf.getShort();
        if(qClass != (short) 0x01) {
            throw new Exception("Answer error: answer CLASS should be 0x01, but " + qClass + " was found instead.");
        }
        readByte += 2;
        //TTL
        byte[] ttlBytes = {response[readByte], response[readByte + 1], response[readByte + 2], response[readByte + 3]};
        ByteBuffer wrapTtl = ByteBuffer.wrap(ttlBytes);
        r.setTtl(wrapTtl.getInt());
        readByte += 4;
        //RDLENGTH
        byte[] rdLen = {response[readByte], response[readByte + 1]};
        ByteBuffer wraprData = ByteBuffer.wrap(rdLen);
        int rdLength = wraprData.getShort();
        readByte += 2;
        Pair rData = new Pair();
        //RDATA
        switch(qt) {
            case A:
                rData.name = analyzeAData(readByte);
                break;
            case NS:
                rData = getDomain(readByte);
                break;
            case MX:
                //PREFERENCE
                rData = getDomain(readByte + 2);
                break;
            case CNAME:
                rData = getDomain(readByte);
                break;
        }
        readByte += rdLength;
        r.setSize(readByte - index);
        r.setrData(rData.name);
        return r;
    }

    private Pair getDomain(int index){
        byte wordSize = response[index];
        String domain = "";
        boolean start = true;
        int count = 0;
        while(wordSize != (short)0x0){
            if(start) {
                start = false;
            }else{
                domain+=".";
            }
            index++;
            count++;
            int read = wordSize;
            if((read & 0xC0)== (int)0xC0){
                byte[] offset = {(byte)(response[index-1]&0x3F), response[index]};
                ByteBuffer wrapped = ByteBuffer.wrap(offset);
                domain+= getDomain(wrapped.getShort()).name;
                index++;
                count++;
                break;
            }else{
                for(int i = 0; i < read; i++) {
                    domain += (char) response[index++];
                    count++;
                }

            }
            wordSize=response[index];
        }
        Pair p = new Pair();
        p.index = count;
        p.name = domain;
        return p;
    }

    private QueryType getQueryTypeFromBytes(byte[] type) {
        int qt = bytesToShort(type[0], type[1]);
        System.out.println(qt);
        switch(qt) {
            case ((short) 0x02):
                return QueryType.NS;
            case ((short) 0x0f):
                return QueryType.MX;
            case ((short) 0x05):
                return QueryType.CNAME;
        }
        return QueryType.A;
    }

    private String analyzeAData (int readByte){
        String address = "";
        byte[] byteAddress= {response[readByte], response[readByte + 1], response[readByte + 2], response[readByte + 3]};
        try{
            InetAddress inetaddress = InetAddress.getByAddress(byteAddress);
            address = inetaddress.toString().substring(1);
        }catch (UnknownHostException e){
            e.printStackTrace();
        }
        return address;
    }
    private static int getBit(byte b, int p){
        return (b>>p)&1;
    }
    private static int getRCode(byte b) {
        return ((b >> 0) & 1) + ((b >> 1) & 1) * 2 +((b >> 2) & 1) * 4 + ((b >> 3) & 1) * 8;
    }
    private static int getWord(byte[] bytes) {
        return ((bytes[0] & 0xff) << 8) + (bytes[1] & 0xff);
    }
    private short bytesToShort(byte b1, byte b2) {
        return (short) ((b1 << 8) | (b2 & 0xFF));
    }
}
