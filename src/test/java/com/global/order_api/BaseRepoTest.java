package com.global.order_api;


import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/// to make spring run dataBase parts only (entities and repos) => make test is very fast
/// built in => @Transactional to make DB is clean after each test
@DataJpaTest
/// to tell junit5 that class contains docker containers to monitor it
/// to run containers before starting test and remove containers after finishing test
//@Testcontainers
/// to use test containers not H2
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
/// abstract because this class only template
public abstract class BaseRepoTest {
    /// this to tell junit this container should be monitored
//    @Container
    /// to avoid writing complex code to tell spring that url ,username and password changed to docker
    /// not in application.prop
    /// so this annotation inject url ,user,password from container to spring boot
    /// override any settings of database
    @ServiceConnection
    /// MYSQLCONTAINER => static => to run db once when run the class and then close when finish the class
    /// mysql8.0 => image from docker hub
    /// db , user , password => settings of this db run in container
    /// serviceconnection => read it and inject it into spring boot
    /// test container library talk to docker inside github server
    /// to pull mysql:8.0 image and open container and these the details
    private static final MySQLContainer<?> mySqlContainer = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("order_api_test")
            .withUsername("test")
            .withPassword("test");

    static {
        mySqlContainer.start();
    }
}
