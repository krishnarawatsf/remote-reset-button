# CRUD Operations with RESTCONF and Java

## Objective
To implement CRUD operations using Java by interacting with a simulated RESTCONF API. The program sends HTTP requests to create, retrieve, update, and delete a network configuration resource and displays the server responses.

This project demonstrates how Java can be used to interact with RESTCONF-based APIs for managing network resources. A simulated RESTCONF server is used, and Java sends **POST, GET, PUT, and DELETE** requests to perform CRUD operations on a network interface resource. The responses from the API are captured and displayed in the console. Testing can also be done using **curl** or **Postman**.

## Tools Used
* Java 17
* Maven
* OkHttp
* Gson
* Spring Boot (for the RESTCONF mock server)
* curl / Postman

## Project Structure
- `java/` : A clean, pure Java client built with Maven, OkHttp, and Gson.
- `restconf-mock-server/` : A standalone mock Spring Boot RESTCONF server that intercepts API calls on `http://localhost:8080`.

## Endpoints Implemented

* **Create**: `POST /restconf/data/ietf-interfaces:interfaces`
* **Read**: `GET /restconf/data/ietf-interfaces:interfaces/interface={name}`
* **Update**: `PUT /restconf/data/ietf-interfaces:interfaces/interface={name}`
* **Delete**: `DELETE /restconf/data/ietf-interfaces:interfaces/interface={name}`

---

## How to Run

### Step 1: Start the simulated RESTCONF Mock Server
Open a terminal window and navigate to the server folder:
```bash
cd restconf-mock-server
mvn spring-boot:run
```
*(Wait until you see `Tomcat started on port 8080`)*. Keep this terminal running!

### Step 2: Run the Java RESTCONF Client
Open a **second** terminal window and run:
```bash
cd java
mvn clean compile
mvn exec:java
```

### Expected Output
```
===== CREATE (POST) =====
Status Code: 200
Success: true
Body: {"message":"Interface created","data":{"ietf-interfaces:interface":{"name":"Loopback100",...}}}

===== READ (GET) =====
Status Code: 200
...

===== UPDATE (PUT) =====
Status Code: 200
...

===== DELETE (DELETE) =====
Status Code: 200
Success: true
Body: {"message":"Interface deleted"}
```

---

## curl Testing

If you'd like to test the Mock server endpoints directly using `curl`, run these while the mock server is running:

### Create
```bash
curl -u admin:admin -X POST \
-H "Content-Type: application/yang-data+json" \
-H "Accept: application/yang-data+json" \
-d '{
  "ietf-interfaces:interface": {
    "name": "Loopback100",
    "description": "Demo interface",
    "type": "iana-if-type:softwareLoopback",
    "enabled": true,
    "ipAddress": "10.10.10.1",
    "prefixLength": 32
  }
}' \
http://localhost:8080/restconf/data/ietf-interfaces:interfaces
```

### Read
```bash
curl -u admin:admin -X GET \
-H "Accept: application/yang-data+json" \
http://localhost:8080/restconf/data/ietf-interfaces:interfaces/interface=Loopback100
```

### Update
```bash
curl -u admin:admin -X PUT \
-H "Content-Type: application/yang-data+json" \
-H "Accept: application/yang-data+json" \
-d '{
  "ietf-interfaces:interface": {
    "name": "Loopback100",
    "description": "Updated loopback interface",
    "type": "iana-if-type:softwareLoopback",
    "enabled": false,
    "ipAddress": "10.10.10.1",
    "prefixLength": 32
  }
}' \
http://localhost:8080/restconf/data/ietf-interfaces:interfaces/interface=Loopback100
```

### Delete
```bash
curl -u admin:admin -X DELETE \
-H "Accept: application/yang-data+json" \
http://localhost:8080/restconf/data/ietf-interfaces:interfaces/interface=Loopback100
```

## Postman Testing
- **Method**: POST / GET / PUT / DELETE
- **URL**: same as above
- **Authorization**: Basic Auth (Username: `admin`, Password: `admin`)
- **Headers**:
  - `Content-Type: application/yang-data+json`
  - `Accept: application/yang-data+json`
- For POST and PUT, place the JSON payload snippet in the Body Tab.

---

## Conclusion
The project successfully demonstrates RESTCONF CRUD operations using Java. By using OkHttp to send HTTP requests and Gson to process JSON data, the application can create, read, update, and delete network configuration resources. This helps in understanding how REST APIs are used in network automation and configuration management.
