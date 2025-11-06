package com.capstone.smart_parcel;

import com.capstone.smart_parcel.config.StorageProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(StorageProperties.class)
public class SmartParcelApplication {

	public static void main(String[] args) {
		SpringApplication.run(SmartParcelApplication.class, args);
	}

}
