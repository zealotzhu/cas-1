package org.apereo.cas.web.consent.config;

import org.apereo.cas.authentication.principal.Service;
import org.apereo.cas.authentication.principal.ServiceFactory;
import org.apereo.cas.authentication.principal.WebApplicationService;
import org.apereo.cas.configuration.CasConfigurationProperties;
import org.apereo.cas.consent.services.ConsentServiceRegistry;
import org.apereo.cas.services.RegexRegisteredService;
import org.apereo.cas.services.ReturnAllowedAttributeReleasePolicy;
import org.apereo.cas.services.ServiceRegistryExecutionPlan;
import org.apereo.cas.services.ServiceRegistryExecutionPlanConfigurer;
import org.apereo.cas.services.consent.DefaultRegisteredServiceConsentPolicy;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * This is {@link CasConsentReviewConfiguration}.
 *
 * @author Arnold Bergner
 * @since 5.2.0
 */
@Configuration("casConsentReviewConfiguration")
@Slf4j
public class CasConsentReviewConfiguration {
    @Autowired
    private CasConfigurationProperties casProperties;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    @Qualifier("webApplicationServiceFactory")
    private ServiceFactory<WebApplicationService> webApplicationServiceFactory;

    @Bean
    public Service consentCallbackService() {
        return this.webApplicationServiceFactory.createService(
            casProperties.getServer().getPrefix().concat("/consentReview/callback"));
    }

    @Bean
    @ConditionalOnMissingBean(name = "consentServiceRegistryExecutionPlanConfigurer")
    public ServiceRegistryExecutionPlanConfigurer consentServiceRegistryExecutionPlanConfigurer() {
        return new ServiceRegistryExecutionPlanConfigurer() {
            @Override
            public void configureServiceRegistry(final ServiceRegistryExecutionPlan plan) {
                val service = new RegexRegisteredService();
                service.setEvaluationOrder(Integer.MAX_VALUE);
                service.setName("CAS Consent Review");
                service.setDescription("Review consent decisions for attribute release");
                service.setServiceId(consentCallbackService().getId());
                val policy = new ReturnAllowedAttributeReleasePolicy();
                val consentPolicy = new DefaultRegisteredServiceConsentPolicy();
                consentPolicy.setEnabled(false);
                policy.setConsentPolicy(consentPolicy);
                service.setAttributeReleasePolicy(policy);

                LOGGER.debug("Saving consent service [{}] into the registry", service);
                plan.registerServiceRegistry(new ConsentServiceRegistry(eventPublisher, service));
            }
        };
    }
}
