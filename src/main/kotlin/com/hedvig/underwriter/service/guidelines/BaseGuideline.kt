package com.hedvig.underwriter.service.guidelines

import com.hedvig.underwriter.model.QuoteData
import kotlin.reflect.KClass
import kotlin.reflect.full.cast

interface BaseGuideline<in T : QuoteData> {

    val breachedGuideline: BreachedGuidelineCode
    val validate: (T) -> Boolean

    val skipAfter: Boolean
        get() = false

    fun invokeValidate(data: T): BreachedGuidelineCode? {
        if (validate.invoke(data)) {
            return breachedGuideline
        }
        return null
    }
}

class TypedGuideline<G : QuoteData, Q : QuoteData>(
    private val guideline: BaseGuideline<Q>,
    private val q: KClass<Q>
) : BaseGuideline<G> {

    override val breachedGuideline: BreachedGuidelineCode
        get() = guideline.breachedGuideline
    override val validate: (G) -> Boolean
        get() = { t -> guideline.validate(q.cast(t)) }
}
