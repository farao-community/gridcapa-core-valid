package com.farao_community.farao.gridcapa_core_valid.app;

import com.rte_france.farao.rao_runner.api.resource.RaoRequest;
import com.rte_france.farao.rao_runner.api.resource.RaoResponse;
import com.rte_france.farao.rao_runner.starter.RaoRunnerClient;
import org.springframework.stereotype.Component;

@Component
public class RaoRunner {
    private final RaoRunnerClient raoRunnerClient;

    public RaoRunner(RaoRunnerClient raoRunnerClient) {
        this.raoRunnerClient = raoRunnerClient;
    }

    public RaoResponse runRao(RaoRequest raoRequest) {
        return raoRunnerClient.runRao(raoRequest);
    }

}
