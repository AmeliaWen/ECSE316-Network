# ECSE316P1
This program uses Java sockets to perform the following actions. <br/>

* Is invoked from the command line (STDIN);
* Sends a query to the server for the given domain name using a UDP socket;
* Waits for the response to be returned from the server;
* Interprets the response and outputs the result to terminal display (STDOUT).

This program is capable of performing the following actions:

* Send queries for A (IP addresses), MX (mail server), and NS (name server) records;
* Interpret responses that contain A records (iPad dresses) and CNAME records (Unaliases);
* Retransmit queries that are lost.

It will also print out error messages if there is an error occurred (ex.invalid input, error when sending request and reading responses, etc.) 


This program can be invoked from command line by 

cd src/<br/>
javac *.java<br/>
java DNSClient [-t timeout] [-r max-retries] [-p port] [-mx|-ns] @server name<br/>

The arguments are defined as follows: 
* timeout (optional) gives how long to wait, in seconds, before retransmitting an
unanswered query. Default value: 5.
* max-retries(optional) is the maximum number of times to retransmit an
unanswered query before giving up. Default value: 3.
* port (optional) is the UDP port number of the DNS server. Default value: 53.
* -mx or -ns flags (optional) indicate whether to send a MX (mail server) or NS (name server) query. At most one of these can be given, and if neither is given then the client should send a type A (IP address) query.
* server (required) is the IPv4 address of the DNS server, in a.b.c.d. Format
* name (required) is the domain name to query for.

**this is the link to project report 
https://docs.google.com/document/d/1nMRa5Wc4aRcDGXnMF095Pg7nYgvI8oaOz9LeLo0GIWQ/edit?usp=sharing
