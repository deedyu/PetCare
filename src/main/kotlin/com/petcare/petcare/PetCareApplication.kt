package com.petcare.petcare

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
class PetCareApplication

fun main(args: Array<String>) {
	runApplication<PetCareApplication>(*args)
}
