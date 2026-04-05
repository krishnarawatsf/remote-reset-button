# Remote Reset Button (Java)

This repository now keeps the Java Spring Boot implementation as the primary runtime.

## Run

From the workspace root:

```bash
cd java
mvn spring-boot:run -Dspring-boot.run.arguments="find Camera 2"
```

Or run the reboot flow:

```bash
cd java
mvn spring-boot:run -Dspring-boot.run.arguments="reboot GigabitEthernet2"
```

## Build

```bash
cd java
mvn clean package
```

## Test

```bash
cd java
mvn test
```

## Notes

- Java app entrypoint: `java/src/main/java/com/remote/resetbutton/RemoteResetButtonApplication.java`
- Java module README: `java/README.md`
- Root verification helper: `tools/verify.sh`
