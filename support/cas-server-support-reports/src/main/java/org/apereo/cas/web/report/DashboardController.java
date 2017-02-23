package org.apereo.cas.web.report;

import org.apereo.cas.configuration.CasConfigurationProperties;
import org.apereo.cas.web.report.util.ControllerUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.endpoint.EndpointProperties;
import org.springframework.boot.actuate.endpoint.EnvironmentEndpoint;
import org.springframework.boot.actuate.endpoint.ShutdownEndpoint;
import org.springframework.cloud.bus.BusProperties;
import org.springframework.cloud.config.server.config.ConfigServerProperties;
import org.springframework.cloud.context.restart.RestartEndpoint;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * This is {@link DashboardController}.
 *
 * @author Misagh Moayyed
 * @since 5.0.0
 */
public class DashboardController extends BaseCasMvcEndpoint {
    
    @Autowired(required = false)
    private BusProperties busProperties;

    @Autowired
    private ConfigServerProperties configServerProperties;

    @Autowired
    private RestartEndpoint restartEndpoint;

    @Autowired
    private ShutdownEndpoint shutdownEndpoint;

    @Autowired
    private EndpointProperties endpointProperties;

    @Autowired
    private EnvironmentEndpoint environmentEndpoint;

    @Autowired
    private Environment environment;

    @Autowired
    private ApplicationContext applicationContext;

    private CasConfigurationProperties casProperties;
    
    public DashboardController(final CasConfigurationProperties casProperties) {
        super("casdashboard", "/dashboard", casProperties.getMonitor().getEndpoints().getDashboard());
        this.casProperties = casProperties;
    }

    /**
     * Handle request internal model and view.
     *
     * @param request  the request
     * @param response the response
     * @return the model and view
     * @throws Exception the exception
     */
    @GetMapping
    public ModelAndView handle(final HttpServletRequest request,
                               final HttpServletResponse response) throws Exception {
        
        ensureEndpointAccessIsAuthorized(request, response);
        
        final Map<String, Object> model = new HashMap<>();
        final String path = request.getContextPath();
        ControllerUtils.configureModelMapForConfigServerCloudBusEndpoints(busProperties, configServerProperties, path, model);
        model.put("restartEndpointEnabled", restartEndpoint.isEnabled() && endpointProperties.getEnabled());
        model.put("environmentEndpointEnabled", environmentEndpoint.isEnabled() && endpointProperties.getEnabled());
        model.put("shutdownEndpointEnabled", shutdownEndpoint.isEnabled() && endpointProperties.getEnabled());
        model.put("serverFunctionsEnabled",
                (Boolean) model.get("restartEndpointEnabled") || (Boolean) model.get("shutdownEndpointEnabled"));

        model.put("actuatorEndpointsEnabled", casProperties.getAdminPagesSecurity().isActuatorEndpointsEnabled());

        final boolean isNativeProfile = Arrays.stream(environment.getActiveProfiles())
                .anyMatch(s -> s.equalsIgnoreCase("native"));

        final boolean isDefaultProfile = Arrays.stream(environment.getActiveProfiles())
                .anyMatch(s -> s.equalsIgnoreCase("default"));

        model.put("isNativeProfile", isNativeProfile);
        model.put("isDefaultProfile", isDefaultProfile);

        model.put("trustedDevicesEnabled", this.applicationContext.containsBean("trustedDevicesController"));
        model.put("authenticationEventsRepositoryEnabled", this.applicationContext.containsBean("casEventRepository"));

        return new ModelAndView("monitoring/viewDashboard", model);
    }
}
