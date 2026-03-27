package com.ticketing.config;

import com.azure.identity.ClientSecretCredential;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.microsoft.graph.serviceclient.GraphServiceClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configures the Microsoft Graph client using the OAuth2 Client Credentials flow.
 *
 * Required Azure AD app registration permissions (application, not delegated):
 *   - Mail.Read          — read messages from any mailbox in the tenant
 *   - Mail.ReadWrite     — mark messages as read
 *
 * How to set up:
 *   1. Go to portal.azure.com → Azure Active Directory → App registrations → New registration
 *   2. Name it (e.g. "TicketFlow"), choose "Accounts in this org directory only"
 *   3. Go to API permissions → Add → Microsoft Graph → Application → Mail.Read + Mail.ReadWrite → Grant admin consent
 *   4. Go to Certificates & secrets → New client secret → copy the value
 *   5. Set the three env vars below (tenant ID is on the Overview page)
 */
@Configuration
@Slf4j
public class GraphConfig {

    @Value("${app.graph.tenant-id}")     private String tenantId;
    @Value("${app.graph.client-id}")     private String clientId;
    @Value("${app.graph.client-secret}") private String clientSecret;

    @Bean
    public GraphServiceClient graphServiceClient() {
        ClientSecretCredential credential = new ClientSecretCredentialBuilder()
                .tenantId(tenantId)
                .clientId(clientId)
                .clientSecret(clientSecret)
                .build();

        // Scopes for app-only auth — ".default" uses all granted application permissions
        String[] scopes = { "https://graph.microsoft.com/.default" };

        log.info("Microsoft Graph client initialised for tenant: {}", tenantId);
        return new GraphServiceClient(credential, scopes);
    }
}
