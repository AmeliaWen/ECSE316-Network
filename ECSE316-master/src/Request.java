import java.nio.ByteBuffer;
import java.util.Random;

public class Request {
    private QueryType queryType;
    private String domain;
    private int HEADER_SIZE = 12;
    private int QTYE_QCLASS_SIZE = 5;

    public Request (QueryType queryType, String domain){
        this.domain = domain;
        this.queryType = queryType;
    }
    public byte[] getRequest () throws Exception{
        try{
            int qLength = domain.length();
            ByteBuffer req = ByteBuffer.allocate(HEADER_SIZE+QTYE_QCLASS_SIZE+qLength+1);
            ByteBuffer header = ByteBuffer.allocate(HEADER_SIZE);
            byte[] randomID = new byte[2];
            new Random().nextBytes(randomID);
            header.put(randomID);
            header.put(new byte[]{0x01, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00});
            req.put(header.array());
            ByteBuffer addrBuffer = ByteBuffer.allocate(domain.length()+QTYE_QCLASS_SIZE+1);
            for (String label: this.domain.split("\\.")){
                addrBuffer.put((byte)label.length());
                for (int j = 0; j<label.length();j++){
                    addrBuffer.put((byte)label.charAt(j));
                }
            }
            //System.out.println(domain);
            addrBuffer.put((byte)0x00); //end of address
            //start put query type
            addrBuffer.put((byte)0x00);
            switch (queryType){
                case A:
                    addrBuffer.put((byte)0x01);
                    break;
                case MX:
                    addrBuffer.put((byte)0x0f);
                    break;
                case NS:
                    addrBuffer.put((byte)0x02);
                    break;
            }
            addrBuffer.put(new byte[]{0x00,0x01}); //always 0x0001 for internet address
            req.put(addrBuffer.array());
            return req.array();
        }catch (Exception e){
            e.printStackTrace();
            throw new Exception(e.getMessage());
        }
    }
}
