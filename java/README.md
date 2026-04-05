# Java Spring Boot RESTCONF Demo

This folder contains the Java conversion of the Remote Reset Button project.

## Build

```bash
cd java
mvn clean package
```

## Run against the local mock server

Start the mock server first, then run:

```bash
mvn spring-boot:run -Dspring-boot.run.arguments="find Camera 2"
mvn spring-boot:run -Dspring-boot.run.arguments="reboot GigabitEthernet2"
```

## Run against Cisco DevNet IOS-XE sandbox

Override the RESTCONF settings at runtime:

```bash
mvn spring-boot:run \
  -Dspring-boot.run.arguments="find Camera 2" \
  -Dspring-boot.run.jvmArguments="-Drestconf.host=devnetsandboxiosxe.cisco.com -Drestconf.port=443 -Drestconf.username=developer -Drestconf.password=C1sco12345 -Drestconf.use-https=true -Drestconf.skip-ssl-verification=true"
```

Use `reboot GigabitEthernet2` for the power-cycle flow.

## Notes

- The app uses OkHttp for RESTCONF calls.
- Requests use `application/yang-data+json`.
- PATCH updates only the `enabled` field.