package ru.mityunin.myShop;

import org.springframework.boot.SpringApplication;

public class TestMyShopApplication {

	public static void main(String[] args) {
		SpringApplication.from(MyShopApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
