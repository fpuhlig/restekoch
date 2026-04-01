package com.restekoch

import com.google.api.gax.core.CredentialsProvider
import com.google.api.gax.core.NoCredentialsProvider
import com.google.auth.Credentials
import com.google.cloud.NoCredentials
import io.quarkus.test.Mock
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.inject.Default
import jakarta.enterprise.inject.Produces
import jakarta.inject.Singleton

@Mock
@ApplicationScoped
class MockCredentialsProducer {
    @Produces
    @Singleton
    @Default
    fun googleCredential(): Credentials = NoCredentials.getInstance()

    @Produces
    @Singleton
    @Default
    fun credentialsProvider(): CredentialsProvider = NoCredentialsProvider.create()
}
