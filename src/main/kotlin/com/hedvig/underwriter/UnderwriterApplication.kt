package com.hedvig.underwriter

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cloud.openfeign.EnableFeignClients
import springfox.documentation.swagger2.annotations.EnableSwagger2

@SpringBootApplication(scanBasePackages = ["com.hedvig.service"])
@EnableFeignClients(basePackages = ["com.hedvig.client"])
@EnableSwagger2
class UnderwriterApplication

fun main(args: Array<String>) {
    runApplication<UnderwriterApplication>(*args)
}
