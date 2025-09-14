package net.shibori.meiwei

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableAsync

@SpringBootApplication
@EnableAsync
class AiReviewerWebApplication

fun main(args: Array<String>) {
	runApplication<AiReviewerWebApplication>(*args)
}
