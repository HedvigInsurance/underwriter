package com.hedvig.underwriter.service

import com.hedvig.underwriter.model.Quote
import com.hedvig.underwriter.model.QuoteRepository
import com.hedvig.underwriter.service.exceptions.NotFoundException
import com.hedvig.underwriter.serviceIntegration.apigateway.ApiGatewayService
import com.hedvig.underwriter.serviceIntegration.memberService.MemberService
import com.hedvig.underwriter.serviceIntegration.notificationService.NotificationService
import com.hedvig.underwriter.serviceIntegration.productPricing.ProductPricingService
import com.hedvig.underwriter.util.logger
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

@Service
class GdprServiceImpl(
    val quoteService: QuoteService,
    val notificationService: NotificationService,
    val apiGatewayService: ApiGatewayService,
    val memberService: MemberService,
    val productPricingService: ProductPricingService,
    val quoteRepository: QuoteRepository
) : GdprService {

    @Value("\${features.gdpr.retention-days:-1}")
    private var daysConfig: Long = -1

    @Value("\${features.gdpr.dry-run:false}")
    private var dryRunConfig: Boolean = false

    override fun clean(requestedDryRun: Boolean?, requestedDays: Long?) {
        try {
            val dryRun = requestedDryRun ?: this.dryRunConfig
            var days = requestedDays ?: this.daysConfig

            // Ignore requested days if disabled or less than configured days
            if (this.daysConfig < 0 || days < this.daysConfig) {
                days = this.daysConfig
            }

            run(dryRun, days)
        } catch (e: Exception) {
            logger.error("Failed to finish executing cleaning job: $e", e)
        }
    }

    private fun run(dryRun: Boolean, days: Long) {

        logger.info("Clean out quotes older than $days days ${if (dryRun) "(DRY-RUN)" else ""}")

        if (days <= 0) {
            logger.info("Cleaning disabled")
            return
        }

        val quotesToDelete = getQuotesToDelete(days)
        val membersToDelete = getMembersToDelete(quotesToDelete)

        logger.info("Found ${quotesToDelete.size} quote(s) to delete")
        logger.info("Found ${membersToDelete.size} member(s) to delete")

        deleteMembers(membersToDelete, dryRun)
        deleteQuotes(quotesToDelete, dryRun)

        logger.info("Successfully deleted ${quotesToDelete.size} quote(s) and ${membersToDelete.size} member(s)")
    }

    private fun getQuotesToDelete(days: Long): List<Quote> {
        val before = Instant.now().minus(days, ChronoUnit.DAYS)

        logger.info("Lookup quotes created before $before that has no agreement")

        return quoteRepository.findOldQuotesToDelete(before)
    }

    private fun getMembersToDelete(quotes: List<Quote>): List<String> {

        // Set of quotes to clean
        val quoteIds = quotes
            .map { it.id }
            .toSet()

        // Set of members having quotes to clean
        val memberIds = quotes
            .map { it.memberId }
            .filterNotNull()
            .toSet()

        // If a member has no other quotes than in those to clean,
        // then the member can be deleted
        val candidates = memberIds
            .filter { hasNoOtherQuotes(it, quoteIds) }

        // Double check with PP if any contract exists for member. This is an extra
        // safety belt to not delete members that was created and signed before
        // Underwriter was born...
        val hasContracts = candidates.filter { hasContracts(it) }

        hasContracts.forEach {
            logger.warn("Member $it has a contract in PP but it is not registered in Underwriter, skipping it")
        }

        return candidates - hasContracts
    }

    private fun hasNoOtherQuotes(memberId: String, quotes: Set<UUID>): Boolean =
        quoteRepository.findByMemberId(memberId)
            .map { it.id }
            .all { quotes.contains(it) }

    private fun deleteMembers(memberIds: List<String>, dryRun: Boolean) {

        for (id in memberIds) {

            if (dryRun) {
                logger.info("DRYRUN: Deleting member $id in other services skipped.")
                continue
            }

            deleteMemberInApiGateway(id)
            deleteMemberInNotificationService(id)
            deleteMemberInMemberService(id)
        }
    }

    private fun deleteQuotes(quotes: List<Quote>, dryRun: Boolean) {
        // Lookup Service
        // TODO: Endpoint not available yet

        // Underwriter, we are deleting these last to get automatic retries
        // if any failure to delete members or quotes in other sources
        for (quote in quotes) {
            logger.info("Deleting quote ${quote.id} (${quote.data::class.simpleName}) created at ${quote.createdAt} in Underwriter ${if (dryRun) "(DRYRUN, SKIPPING)" else ""}")
            if (dryRun) {
                continue
            }
            quoteService.deleteQuote(quote.id)
        }
    }

    private fun hasContracts(memberId: String): Boolean {
        try {
            return productPricingService.hasContract(memberId)
        } catch (e: Exception) {
            logger.error("Failed to check if member $memberId has contracts in Product and Pricing Service: ${e.message}")
            throw e
        }
    }

    private fun deleteMemberInNotificationService(memberId: String) {
        try {
            logger.info("Deleting member $memberId in Notification Service")

            notificationService.deleteMember(memberId)
        } catch (e: NotFoundException) {
            logger.info("Member $memberId not found in Notification Service")
        } catch (e: Exception) {
            logger.error("Failed to delete member $memberId in Notification Service: ${e.message}")
            throw e
        }
    }

    private fun deleteMemberInApiGateway(memberId: String) {
        try {
            logger.info("Deleting member $memberId in API Gateway")

            apiGatewayService.deleteMember(memberId)
        } catch (e: NotFoundException) {
            logger.info("Member $memberId not found in API Gateway")
        } catch (e: Exception) {
            logger.error("Failed to delete member $memberId in API Gateway: ${e.message}")
            throw e
        }
    }

    private fun deleteMemberInMemberService(memberId: String) {
        try {
            logger.info("Deleting member $memberId in Member Service")

            memberService.deleteMember(memberId)
        } catch (e: NotFoundException) {
            logger.info("Member $memberId not found in Member Service")
        } catch (e: Exception) {
            logger.error("Failed to delete member $memberId in Member Service: ${e.message}")
            throw e
        }
    }
}
