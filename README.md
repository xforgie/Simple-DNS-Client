# Simple-DNS-Client
A basic DNS Client that supports A, AAAA, NS and CNAME Records with caching

## Running the client

Start with root server set to f.root-servers.net
```sh
java -jar SimpleDNSClient.jar -s f.root-servers.net
```
Start with tracing enabled
```sh
java -jar SimpleDNSClient.jar -s f.root-servers.net -t
```

## Using the client

There are only three commands:
```txt
    search <HOSTNAME>    Search using a fully qualified domain address
    help                 Displays list of available commands
    quit                 Quits the application
```
### Example use of the search command:
```txt
SDNS>search github.com
github.com                     A     60       140.82.112.3
```
# License

[BSD 3-Clause](LICENSE)
