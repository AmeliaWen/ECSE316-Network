import java.io.OutputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

/**
 * this class compacts information needed for DNSResponse as an instance of this class
 */
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

    //pair is used for connect the domain info and offset
    class Pair {
        String name;
        int index;
    }

    /**
     * this constructor initializes a response given the info received from DatagramSocket.receive
     * @param response
     * @param qSize
     */
    public DNSResponse (byte[] response, int qSize) throws Exception {
        this.response = response;
        this.querySize = qSize;
        this.offset = querySize;
        this.AA = getBit(response[2], 2) == 1;
        this.validateResponse();
        this.updateRecordCounts();
        answers = new OutputRecord[ANCount];
        additionals = new OutputRecord[ARCount];
        this.readAnswers();
    }


    /**
     * this method print out response given DNSResponse
     * it calls the method in Output record class
     * @throws Exception
     */
    public void readResponse(){
        OutputRecord[] ar = this.answers;
        OutputRecord[] adr = this.additionals;
        if(ar.length == 0 && adr.length == 0) {
            System.out.println("Record not found");
        }
        if(ar.length != 0) {
            System.out.println("***Answer Section ("+ar.length+" records)***");
            for(int i=0; i< ar.length;i++) {
                OutputRecord answer = ar[i];
                answer.printAnswer();
            }
        }
        if(adr.length != 0) {
            System.out.println("***Additional Section ("+adr.length+" records)***");
            for(int i=0; i< adr.length;i++) {
                OutputRecord adrAnswer = adr[i];
                adrAnswer.printAnswer();
            }
        }
    }

    /**
     * this method checks error code for the response
     * @throws Exception
     */
    private void validateResponse() throws Exception {
        if(response.length <12){
            throw new Exception("wrong response size");
        }
        if (getBit(this.response[2],7) != 1) {
            throw new Exception("ERROR\tReceived response is a query, not a response.");
        }
        if (getBit(this.response[3],7) != 1) {
            throw new Exception("ERROR\tServer does not support recursive queries.");
        }
        switch(getRCode(this.response[3])){
            case 1:
                throw new Exception("ERROR\tFormat error: the name server was unable to interpret the query.");
            case 2:
                throw new Exception("ERROR\tServer failure: the name server was unable to process this query due to a problem with the name server.");
            case 3:
                throw new Exception("ERROR\tName error: meaningful only for responses from an authoritative name server, the code signifies that the domain name referenced in the query does not exist.");
            case 4:
                throw new Exception("ERROR\tNot implemented: the name server does not support the requested kind of query.");
            case 5:
                throw new Exception("ERROR\tRefused: the name server refuses to perform the requested operation for policy reasons.");
            default:
                break;
        }
    }

    /**
     * if there is not error code exists, this method update the ANCount, NSCount, ARCount for the response instance
     */
    private void updateRecordCounts() {
        byte[] ANCount = {this.response[6], this.response[7]};
        this.ANCount = getWord(ANCount);
        byte[] NSCount = {this.response[8], this.response[9]};
        this.NSCount = getWord(NSCount);
        byte[] ARCount = {this.response[10], this.response[11]};
        this.ARCount = getWord(ARCount);
    }

    /**
     * this method put the answers and additionals into corresponding arrays
     * it calls helper method gerAnswer
     * @throws Exception
     */
    public void readAnswers () throws Exception{
        int index = offset;
        for(int i = 0; i < ANCount; i ++){
            answers[i] = getAnswer(index);
            index += answers[i].getSize();
        }
        //ignore authority section
        for(int i = 0; i < NSCount; i++){
            index += getAnswer(index).getSize();
        }

        for(int i = 0; i < ARCount; i++){
            additionals[i] = getAnswer(index);
            index += additionals[i].getSize();
        }
    }

    /**
     * this method is a helper method that reads one answer from the index
     * @param index
     * @return
     * @throws Exception
     */
    private OutputRecord getAnswer (int index) throws Exception{
        OutputRecord r = new OutputRecord();
        //sets the auth/nonauth
        r.setAnth(AA);
        //reads the domain name
        int readByte = index;
        Pair p = getDomain(readByte);
        readByte += (int) p.index;
        //reads the response type
        byte[] ans_type = new byte[2];
        ans_type[0] = response[readByte];
        ans_type[1] = response[readByte+1];
        QueryType qt = getQueryTypeFromBytes(ans_type);
        r.setQueryType(qt);
        readByte+=2;
        //reads the CLASS section to check error
        byte[] bClass = { response[readByte], response[readByte + 1] };
        ByteBuffer buf = ByteBuffer.wrap(bClass);
        short qClass = buf.getShort();
        if(qClass != (short) 0x01) {
            throw new Exception("Answer error: answer CLASS should be 0x01, but " + qClass + " was found instead.");
        }
        readByte += 2;
        //TTL 32bit
        byte[] ttlBytes = {response[readByte], response[readByte + 1], response[readByte + 2], response[readByte + 3]};
        ByteBuffer wrapTtl = ByteBuffer.wrap(ttlBytes);
        r.setTtl(wrapTtl.getInt());
        readByte += 4;
        //RDLENGTH: length of RDATA field
        byte[] rdLen = {response[readByte], response[readByte + 1]};
        ByteBuffer wraprData = ByteBuffer.wrap(rdLen);
        int rdLength = wraprData.getShort();
        readByte += 2;
        Pair rData = new Pair();
        //RDATA
        //it returns IP address for typeA, name of server for type NS, preferences for type MX
        switch(qt) {
            case A:
                rData.name = analyzeAData(readByte);
                break;
            case NS:
            case CNAME:
                rData = getDomain(readByte);
                break;
            case MX:
                //PREFERENCE
                rData = getDomain(readByte + 2);
                break;
        }
        readByte += rdLength;
        r.setSize(readByte - index);
        r.setrData(rData.name);
        return r;
    }

    /**
     * this is the helper method that returns a pair that contains domain name and its length
     * @param index
     * @return
     */
    private Pair getDomain(int index){
        int wordSize = response[index];
        String domain = "";
        boolean start = true;
        int count = 0;
        while(wordSize != 0){
            if (!start){
                domain+=".";
            }
            //it is a pointer
            if((wordSize & 0xC0)== (int)0xC0){
                byte[] offset = {(byte)(response[index]&0x3F), response[index+1]};
                ByteBuffer wrapped = ByteBuffer.wrap(offset);
                domain+= getDomain(wrapped.getShort()).name;
                //index+=2;
                count+=2;
                //wordSize = 0;
                break;
            }else{
                //if it is not a pointer, get the name directly
                String substring = "";
                int substringsize = response[index];
                for(int i = 0; i < substringsize; i++) {
                    substring += (char) response[index+i+1];
                }
                domain+=substring;
                index += wordSize+1;
                count += wordSize+1;
                wordSize = response[index];
            }
            start = false;
        }
        Pair p = new Pair();
        p.index = count;
        p.name = domain;
        return p;
    }

    /**
     * this is a helper method that returns queryType from a 2-byte array
     * by default, it returns type A
     * @param type
     * @return
     */
    private QueryType getQueryTypeFromBytes(byte[] type) {
        int qt = getWord(type);
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

    /**
     * if it is a type A record, return the IP address
     * @param readByte
     * @return
     */
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

    //the following are some helper methods for bit comparision
    private static int getBit(byte b, int p){
        return (b>>p)&1;
    }
    private static int getRCode(byte b) {
        return ((b >> 0) & 1) + ((b >> 1) & 1) * 2 +((b >> 2) & 1) * 4 + ((b >> 3) & 1) * 8;
    }
    private static int getWord(byte[] bytes) {
        return ((bytes[0] & 0xff) << 8) + (bytes[1] & 0xff);
    }
}
