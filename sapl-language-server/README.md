# Run
```shell
mvn spring-boot:run
```

# Package
```shell
mvn package
```
The jar will be available under `target/sapl-language-server-${version}.jar`
and can be started with (${version} needs to be replaced with the current SAPL version):
```shell
java -jar target/sapl-language-server-${version}.jar
```
