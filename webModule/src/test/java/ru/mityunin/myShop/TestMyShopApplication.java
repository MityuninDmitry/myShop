package ru.mityunin.myShop;

import org.springframework.boot.SpringApplication;
import org.springframework.cache.annotation.EnableCaching;

public class TestMyShopApplication {

	public static void main(String[] args) {
		SpringApplication.from(MyShopApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
