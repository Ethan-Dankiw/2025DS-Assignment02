# Project Purpose

The purpose of this assignment is to design a concurrent network that can
receive requests using a RESTful API. The requests can either fetch or store
weather data, and must order requests such that stale data is not served to
clients.

# Project Setup

Ensure maven is installed, see https://maven.apache.org/install.html

Open the project folder in IntelliJ
run `mvn compile`

This will install all dependencies and build the project.

# Run the Project

1. Start the Aggregation server by running one of the following:
    - `mvn exec:java -Dexec.mainClass="net.ethandankiw.aggregation.LoadBalancer" -Dexec.args="<SERVER_PORT>"`
    - `mvn exec:java -Dexec.mainClass="net.ethandankiw.aggregation.LoadBalancer"`

2. Make a PUT request using the ContentServer
    - `mvn exec:java -Dexec.mainClass="net.ethandankiw.content.ContentServer" -Dexec.args="<SERVER_URL> <FILE_PATH>"`
    - Same file path formatting applies to the GetClient and the ContentServer
    - By default, for this project files are stored in `./src/main/java/net/ethandankiw/data/store/files`.
    - However, the `FileManager` already prepends this path to any file requests, 
      meaning for the file at ./files/weatherdata/IDS60901.txt. only `IDS60901.txt`
      needs to be provided as a file path

3. Make a GET request using the GetClient
    - `mvn exec:java -Dexec.mainClass="net.ethandankiw.client.GetClient" -Dexec.args="<SERVER_URL> <FILE_PATH>"`
    - Same file path formatting applies to the GetClient and the ContentServer

# Test the Project

run `mvn test`