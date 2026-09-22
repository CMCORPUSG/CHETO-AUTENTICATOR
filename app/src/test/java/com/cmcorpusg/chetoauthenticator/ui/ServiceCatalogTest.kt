package com.cmcorpusg.chetoauthenticator.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ServiceCatalogTest {
    @Test
    fun knownServicesResolveToExpectedDomains() {
        assertEquals("google.com", ServiceCatalog.domainFor("Google"))
        assertEquals("twitch.tv", ServiceCatalog.domainFor("Twitch"))
        assertEquals("kick.com", ServiceCatalog.domainFor("Kick"))
        assertEquals("microsoft.com", ServiceCatalog.domainFor("Microsoft"))
    }

    @Test
    fun explicitDomainIsAcceptedForCustomServices() {
        assertEquals("notion.so", ServiceCatalog.domainFor("notion.so"))
        assertTrue(ServiceCatalog.logoUrlFor("notion.so")!!.contains("notion.so"))
    }
}
