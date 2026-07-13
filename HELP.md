# Getting Started

### Reference Documentation
For further reference, please consider the following sections:

* [Official Apache Maven documentation](https://maven.apache.org/guides/index.html)
* [Spring Boot Maven Plugin Reference Guide](https://docs.spring.io/spring-boot/4.1.0/maven-plugin)
* [Create an OCI image](https://docs.spring.io/spring-boot/4.1.0/maven-plugin/build-image.html)
* [Coroutines section of the Spring Framework Documentation](https://docs.spring.io/spring-framework/reference/7.0.8/languages/kotlin/coroutines.html)
* [Spring Reactive Web](https://docs.spring.io/spring-boot/4.1.0/reference/web/reactive.html)
* [Reactive HTTP Client](https://docs.spring.io/spring-boot/4.1.0/reference/io/rest-client.html#io.rest-client.webclient)
* [Spring for Apache Kafka](https://docs.spring.io/spring-boot/4.1.0/reference/messaging/kafka.html)
* [Spring Data JPA](https://docs.spring.io/spring-boot/4.1.0/reference/data/sql.html#data.sql.jpa-and-spring-data)

### Guides
The following guides illustrate how to use some features concretely:

* [Building a Reactive RESTful Web Service](https://spring.io/guides/gs/reactive-rest-service/)
* [Accessing Data with JPA](https://spring.io/guides/gs/accessing-data-jpa/)

### Maven Parent overrides

Due to Maven's design, elements are inherited from the parent POM to the project POM.
While most of the inheritance is fine, it also inherits unwanted elements like `<license>` and `<developers>` from the parent.
To prevent this, the project POM contains empty overrides for these elements.
If you manually switch to a different parent and actually want the inheritance, you need to remove those overrides.

