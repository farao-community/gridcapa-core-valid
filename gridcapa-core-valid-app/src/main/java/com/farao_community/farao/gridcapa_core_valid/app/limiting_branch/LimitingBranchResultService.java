/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.gridcapa_core_valid.app.limiting_branch;

import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_creation.creator.fb_constraint.crac_creator.FbConstraintCreationContext;
import com.farao_community.farao.data.rao_result_api.RaoResult;
import com.farao_community.farao.data.rao_result_json.RaoResultImporter;
import com.farao_community.farao.gridcapa_core_valid.app.services.UrlValidationService;
import com.farao_community.farao.gridcapa_core_valid.app.study_point.StudyPoint;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Theo Pascoli {@literal <theo.pascoli at rte-france.com>}
 */
@Component
public class LimitingBranchResultService {

    private final UrlValidationService urlValidationService;
    private final RaoResultImporter raoResultImporter;

    public LimitingBranchResultService(UrlValidationService urlValidationService) {
        this.urlValidationService = urlValidationService;
        this.raoResultImporter = new RaoResultImporter();
    }

    public List<LimitingBranchResult> importRaoResult(StudyPoint studyPoint, FbConstraintCreationContext cracCreationContext, String raoResultUrl) {
        String studyPointId = studyPoint.getId();
        RaoResult raoResult = raoResultImporter.importRaoResult(urlValidationService.openUrlStream(raoResultUrl), cracCreationContext.getCrac());
        List<LimitingBranchResult> listLimitingBranches = new ArrayList<>();
        cracCreationContext.getBranchCnecCreationContexts().forEach(branchCnecCreationContext -> {
            if (branchCnecCreationContext.isImported()) {
                String criticalBranchId = branchCnecCreationContext.getNativeId();
                Map<Instant, String> flowCnecs = branchCnecCreationContext.getCreatedCnecsIds();
                flowCnecs.forEach((instant, s) -> {
                    FlowCnec cnec = cracCreationContext.getCrac().getFlowCnec(s);
                    LimitingBranchResult branchResult = new LimitingBranchResult(studyPointId, criticalBranchId, raoResult, cnec);
                    listLimitingBranches.add(branchResult);
                });
            }
        });
        return listLimitingBranches;
    }
}
