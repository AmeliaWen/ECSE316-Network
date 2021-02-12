import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;

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
                throw new Exception("illegal ip address");
            }
            int byteNum = 0;
            String[] b = serverAddress.split("\\.");
            if (b.length >4) {
                throw new Exception("wrong length of ip");
            }else{
                for (String str: b){
                    int bNum = Integer.parseInt(str);
                    if (bNum<0||bNum>255){
                        throw new Exception("wrong num");
                    }else{
                        ipAddress[byteNum++] = (byte)bNum;
                    }
                }
            }
        }
        String name = args[args.length-1];
        Client client = new Client();
        client.infoPrint(name, serverAddress, type);
        DNSResponse r = client.sendRequest(name, type, timeout, ipAddress, port, maxRetries);
        client.readResponse(r);
    }
}
