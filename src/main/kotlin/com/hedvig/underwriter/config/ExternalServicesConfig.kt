package com.hedvig.underwriter.config

import com.hedvig.underwriter.serviceIntegration.priceEngine.PriceEngineService
import com.hedvig.underwriter.serviceIntegration.priceEngine.PriceEngineServiceImpl
import com.hedvig.underwriter.serviceIntegration.priceEngine.PriceEngineServiceStub
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration


@Configuration
class ExternalServicesConfig @Autowired constructor(private val context: ApplicationContext) {
    @Bean
    fun priceEngineService(@Value("\${hedvig.price-engine.stub}") stub: Boolean): PriceEngineService {
        val factory = context.autowireCapableBeanFactory
        return if (stub) factory.createBean(PriceEngineServiceStub::class.java) else factory.createBean(
            PriceEngineServiceImpl::class.java
        )
    }

}
