import java.net.*;

public class Client {
    public DNSResponse sendRequest (String name, QueryType qType, int timeout, byte[]ip, int port, int maxRetries) throws Exception{
        for (int i= maxRetries; i>0;i--){
            try{
                DatagramSocket client = new DatagramSocket();
                client.setSoTimeout(timeout);
                InetAddress address = InetAddress.getByAddress(ip);
                Request request = new Request(qType, name);
                byte[] reqData = request.getRequest();
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
        throw new Exception ("exceed retries");
    }

    public void readResponse(DNSResponse response) throws Exception {
        response.readAnswers();
        OutputRecord[] ar = response.getAnswers();
        OutputRecord[] adr = response.getAdditionals();
        if(ar.length == 0 && adr.length == 0) {
            System.out.println("NOTFOUND");
        }
        if(ar.length != 0) {
            System.out.println("***Answer Section ("+ar.length+" records)***");
            for(int i=0; i< ar.length;i++) {
                OutputRecord answer = ar[i];
                printAnswer(answer);
            }
        }
        if(adr.length != 0) {
            System.out.println("***Additional Section ("+adr.length+" records)***");
            for(int i=0; i< adr.length;i++) {
                OutputRecord adrAnswer = adr[i];
                printAnswer(adrAnswer);
            }
        }
    }


    public void printAnswer(OutputRecord answer) {
        QueryType qType = answer.getQueryType();
        String auth = answer.isAnth() ? "auth" : "nonauth";
        if(qType.equals(QueryType.A)) {
            System.out.println("IP\t"+answer.getrData()+"\t"+answer.getTtl()+"\t"+ auth);
        }
        if(qType.equals(QueryType.CNAME)) {
            System.out.println("CNAME\t"+answer.getrData()+"\t"+answer.getTtl()+"\t"+ auth);
        }
        if(qType.equals(QueryType.MX)) {
            System.out.println("MX\t"+answer.getrData()+"\t"+answer.getTtl()+"\t"+ auth);
        }
        if(qType.equals(QueryType.NS)) {
            System.out.println("NS\t"+answer.getrData()+"\t"+answer.getTtl()+"\t"+ auth);
        }
    }

    public void infoPrint(String name, String dnsHost, QueryType t){
        System.out.println("DnsClient sending request for "+name);
        System.out.println("Server: "+dnsHost);
        switch (t){
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
    }





}
