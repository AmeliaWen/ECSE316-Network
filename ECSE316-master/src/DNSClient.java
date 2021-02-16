import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;

/**
 * this class is where the main method locates at.
 * It parses the input then calls the method in Client class.
 */
public class DNSClient {
    private static int timeout = 5000;
    private static int maxRetries = 3;
    private static int port = 53;
    private static QueryType type = QueryType.A;
    private static byte[] ipAddress = new byte[4];
    private static String serverAddress;
    public static void main(String args[]) throws Exception {
        if(args.length < 2 || args.length >4){
            throw new Exception("incorrect number of arguments");
        }
        for (int i = 0; i<args.length; i++){
            switch (args[i]){
                case "-t":
                    timeout = Integer.parseInt(args[i+1])*1000;
                    break ;
                case "-r":
                    maxRetries = Integer.parseInt(args[i+1]);
                    break;
                case "-p":
                    port = Integer.parseInt(args[i+1]);
                    break;
                case "-mx":
                    type = QueryType.MX;
                    break;
                case "-ns":
                    type = QueryType.NS;
                    break;
                default:
                    continue;
            }
        }
        if(args[args.length-2]!=null){
            if (args[args.length-2].charAt(0)=='@'){
                serverAddress = args[args.length-2].substring(1);
            }
            else{
                throw new Exception("Illegal IP address! Correct format: @8.8.8.8");
            }
            int byteNum = 0;
            String[] b = serverAddress.split("\\.");
            if (b.length >4) {
                throw new Exception("Wrong length of ip! Please input valid IP address");
            }else{
                for (String str: b){
                    int bNum = Integer.parseInt(str);
                    if (bNum<0||bNum>255){
                        throw new Exception("Invalid IP address!");
                    }else{
                        ipAddress[byteNum++] = (byte)bNum;
                    }
                }
            }
        }
        String name = args[args.length-1];
        System.out.println("DnsClient sending request for "+name);
        System.out.println("Server: "+serverAddress);
        switch (type){
            case A:
                System.out.println("Request type: A");
                break;
            case MX:
                System.out.println("Request type: MX");
                break;
            case NS:
                System.out.println("Request type: NS");
                break;
        }
        Request request = new Request();
        DNSResponse r = request.sendRequest(name, type, timeout, ipAddress, port, maxRetries);
        r.readResponse();
    }
}
