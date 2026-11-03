package com.eventcampus.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import com.eventcampus.backend.entity.User;

@SpringBootApplication
public class BackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(BackendApplication.class, args);
		User user = new User();
     user.setFirstName("BUTOYI");
    user.setLastName("Kim");
    System.out.println("First Name: " + user.getFirstName());
    System.out.println("Last Name: " + user.getLastName());
    System.out.println("ID: " + user.getIdUser());
	}

}
