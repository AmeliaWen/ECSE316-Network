import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.Random;

/**
 * This class compacts information for a DNS question as a Request object.
 */
public class Request {
    private QueryType queryType;
    private String domain;
    private int HEADER_SIZE = 12;
    private int QTYE_QCLASS_SIZE = 5;

    public Request(){
    }
    /**
     * this method constructs the request byte array, this is the input of DatagramSocket.send
     * @return
     * @throws Exception
     */
    public byte[] getRequest () throws Exception{
        try{
            int qLength = domain.length();
            ByteBuffer req = ByteBuffer.allocate(HEADER_SIZE+QTYE_QCLASS_SIZE+qLength+1);
            ByteBuffer header = ByteBuffer.allocate(HEADER_SIZE);
            //the first 2 bytes for a header is randomly generated id.
            byte[] randomID = new byte[2];
            new Random().nextBytes(randomID);
            header.put(randomID);
            //these are the flags indicating it is a query
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

    /**
     * this method takes parsed input and uses the helper method getRequest
     * to send the request and receive response
     * @param name
     * @param qType
     * @param timeout
     * @param ip
     * @param port
     * @param maxRetries
     * @return
     * @throws Exception
     */
    public DNSResponse sendRequest (String name, QueryType qType, int timeout, byte[]ip, int port, int maxRetries) throws Exception{
        if(maxRetries <1){
            throw new Exception("invalid number of retries");
        }
        for (int i= maxRetries; i>0;i--){
            try{
                DatagramSocket client = new DatagramSocket();
                client.setSoTimeout(timeout);
                InetAddress address = InetAddress.getByAddress(ip);
                this.domain = name;
                this.queryType = qType;
                byte[] reqData = getRequest();
                byte[] response = new byte[1024];
                long startTime = System.currentTimeMillis();
                DatagramPacket requestP = new DatagramPacket(reqData, reqData.length, address, port);
                client.send(requestP);
                DatagramPacket responseP = new DatagramPacket(response, 1024);
                client.receive(responseP);
                long endTime = System.currentTimeMillis();
                client.close();
                long time = endTime-startTime;
                System.out.println("Response received after "+ time/1000 + " seconds ("+ (maxRetries-i) + " retries)");
                DNSResponse response1 = new DNSResponse(responseP.getData(), reqData.length);
                return response1;
            }catch (Exception e){
                System.out.println("Error at retry: "+i+" "+e.getMessage());
            }
        }
        throw new Exception ("Exceed number of retries");
    }
}
