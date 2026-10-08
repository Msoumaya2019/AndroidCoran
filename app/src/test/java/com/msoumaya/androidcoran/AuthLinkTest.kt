package com.msoumaya.androidcoran
import com.msoumaya.androidcoran.domain.*
import org.junit.Assert.*
import org.junit.Test
class AuthLinkTest {
    @Test fun existingImplicitRedirectPreservesTokensAndRecoveryWithoutLoggingCredentials() {
        val link=mobileAuthLink("coranmemoire://auth#access_token=dummy-access&refresh_token=dummy%2Brefresh&type=recovery&expires_in=3600")!!
        assertEquals("dummy-access",link.accessToken);assertEquals("dummy+refresh",link.refreshToken)
        assertTrue(link.recovery);assertEquals(3600L,link.expiresIn);assertFalse(link.toString().contains("dummy"))
    }
    @Test fun unrelatedHostsPathsAndSchemesAreIgnored() {
        listOf("https://auth#access_token=a","coranmemoire://auth.evil#access_token=a","coranmemoire://auth/other#access_token=a","coranmemoire://evil@auth#access_token=a").forEach { assertNull(mobileAuthLink(it)) }
    }
    @Test fun incompleteDuplicateExpiredAndInvalidDurationLinksAreRejected() {
        listOf("coranmemoire://auth#access_token=a","coranmemoire://auth#error=expired&error_description=dummy-secret","coranmemoire://auth#access_token=a&access_token=b&refresh_token=c","coranmemoire://auth#access_token=a&refresh_token=b&expires_in=-1").forEach { url->
            val error=runCatching { mobileAuthLink(url) }.exceptionOrNull();assertNotNull(error);assertFalse(error!!.message.orEmpty().contains("dummy-secret"))
        }
    }
    @Test fun confirmationDoesNotEnablePasswordRecoveryAndMissingExpiryRequestsRefresh() {
        val link=mobileAuthLink("coranmemoire://auth#access_token=a&refresh_token=b&type=signup")!!
        assertFalse(link.recovery);assertEquals(0L,link.expiresIn)
    }
}
