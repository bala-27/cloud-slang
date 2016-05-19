/*******************************************************************************
 * (c) Copyright 2014 Hewlett-Packard Development Company, L.P.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License v2.0 which accompany this distribution.
 *
 * The Apache License is available at
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 *******************************************************************************/
package io.cloudslang.lang.compiler;

import io.cloudslang.lang.compiler.configuration.SlangCompilerSpringConfig;
import io.cloudslang.lang.compiler.modeller.model.Executable;
import io.cloudslang.lang.compiler.modeller.model.Flow;
import io.cloudslang.lang.compiler.modeller.model.Step;
import io.cloudslang.lang.entities.CompilationArtifact;
import io.cloudslang.lang.entities.ParallelLoopStatement;
import io.cloudslang.lang.entities.ResultNavigation;
import io.cloudslang.lang.entities.ScoreLangConstants;
import io.cloudslang.lang.entities.bindings.Output;
import io.cloudslang.score.api.ExecutionPlan;
import io.cloudslang.score.api.ExecutionStep;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.collections4.CollectionUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Date: 3/25/2015
 *
 * @author Bonczidai Levente
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = SlangCompilerSpringConfig.class)
public class CompileParallelLoopFlowTest {

    @Autowired
    private SlangCompiler compiler;

    @Test
    public void testPreCompileParallelLoopFlow() throws Exception {
        Step step = getStepsAfterPrecompileFlow("/loops/parallel_loop/simple_parallel_loop.sl").getFirst();

        verifyParallelLoopStatement(step);

        List<Output> publishValues = getPublishOutputs(step);
        assertEquals("publish list is not empty", 0, publishValues.size());

        List<Map<String, String>> expectedNavigationStrings = new ArrayList<>();
        Map<String, String> successMap = new HashMap<>();
        successMap.put(ScoreLangConstants.SUCCESS_RESULT, "SUCCESS");
        Map<String, String> failureMap = new HashMap<>();
        failureMap.put(ScoreLangConstants.FAILURE_RESULT, "FAILURE");
        expectedNavigationStrings.add(successMap);
        expectedNavigationStrings.add(failureMap);
        verifyNavigationStrings(expectedNavigationStrings, step);

        assertTrue(step.isParallelLoop());
    }

    @Test
    public void testPreCompileParallelLoopFlowPublish() throws Exception {
        Step step = getStepsAfterPrecompileFlow("/loops/parallel_loop/parallel_loop_publish.sl").getFirst();

        verifyParallelLoopStatement(step);

        List<Output> publishValues = getPublishOutputs(step);
        assertEquals(2, publishValues.size());
        assertEquals("${ map(lambda x:str(x['name']), branches_context) }", publishValues.get(0).getValue());

        List<Map<String, String>> expectedNavigationStrings = new ArrayList<>();
        Map<String, String> successMap = new HashMap<>();
        successMap.put(ScoreLangConstants.SUCCESS_RESULT, "SUCCESS");
        Map<String, String> failureMap = new HashMap<>();
        failureMap.put(ScoreLangConstants.FAILURE_RESULT, "FAILURE");
        expectedNavigationStrings.add(successMap);
        expectedNavigationStrings.add(failureMap);
        verifyNavigationStrings(expectedNavigationStrings, step);

        assertTrue(step.isParallelLoop());
    }

    @Test
    public void testPreCompileParallelLoopFlowNavigate() throws Exception {
        Deque<Step> steps = getStepsAfterPrecompileFlow("/loops/parallel_loop/parallel_loop_navigate.sl");
        assertEquals(2, steps.size());

        Step parallelStep = steps.getFirst();

        verifyParallelLoopStatement(parallelStep);

        List<Output> publishValues = getPublishOutputs(parallelStep);
        assertEquals(0, publishValues.size());

        List<Map<String, String>> expectedNavigationStrings = new ArrayList<>();
        Map<String, String> successMap = new HashMap<>();
        successMap.put(ScoreLangConstants.SUCCESS_RESULT, "print_list");
        Map<String, String> failureMap = new HashMap<>();
        failureMap.put(ScoreLangConstants.FAILURE_RESULT, "FAILURE");
        expectedNavigationStrings.add(successMap);
        expectedNavigationStrings.add(failureMap);
        verifyNavigationStrings(expectedNavigationStrings, parallelStep);

        assertTrue(parallelStep.isParallelLoop());
    }

    @Test
    public void testPreCompileParallelLoopFlowPublishNavigate() throws Exception {
        Deque<Step> steps = getStepsAfterPrecompileFlow("/loops/parallel_loop/parallel_loop_publish_navigate.sl");
        assertEquals(2, steps.size());

        Step parallelStep = steps.getFirst();

        verifyParallelLoopStatement(parallelStep);

        List<Output> publishValues = getPublishOutputs(parallelStep);
        assertEquals(2, publishValues.size());
        assertEquals("${ map(lambda x:str(x['name']), branches_context) }", publishValues.get(0).getValue());

        List<Map<String, String>> expectedNavigationStrings = new ArrayList<>();
        Map<String, String> successMap = new HashMap<>();
        successMap.put(ScoreLangConstants.SUCCESS_RESULT, "print_list");
        Map<String, String> failureMap = new HashMap<>();
        failureMap.put(ScoreLangConstants.FAILURE_RESULT, "FAILURE");
        expectedNavigationStrings.add(successMap);
        expectedNavigationStrings.add(failureMap);
        verifyNavigationStrings(expectedNavigationStrings, parallelStep);

        assertTrue(parallelStep.isParallelLoop());
    }

    @Test
    public void testCompileParallelLoopFlow() throws Exception {
        URI flow = getClass().getResource("/loops/parallel_loop/simple_parallel_loop.sl").toURI();
        URI operation = getClass().getResource("/loops/parallel_loop/print_branch.sl").toURI();
        Set<SlangSource> path = new HashSet<>();
        path.add(SlangSource.fromFile(operation));
        CompilationArtifact artifact = compiler.compile(SlangSource.fromFile(flow), path);
        assertNotNull("artifact is null", artifact);

        ExecutionPlan executionPlan = artifact.getExecutionPlan();
        assertNotNull("executionPlan is null", executionPlan);

        ExecutionStep addBranchesStep = executionPlan.getStep(2L);
        assertTrue("add branches step is not marked as split step", addBranchesStep.isSplitStep());
        Map<String, ?> addBranchesActionData = addBranchesStep.getActionData();
        verifyParallelLoopStatement(addBranchesActionData);

        assertNotNull("branch begin step method not found", executionPlan.getStep(3L));
        assertNotNull("branch end step method not found", executionPlan.getStep(4L));
        assertNotNull("join branches method not found", executionPlan.getStep(5L));
    }

    @Test
    public void testCompileParallelLoopFlowPublish() throws Exception {
        URI flow = getClass().getResource("/loops/parallel_loop/parallel_loop_publish.sl").toURI();
        URI operation = getClass().getResource("/loops/parallel_loop/print_branch.sl").toURI();
        Set<SlangSource> path = new HashSet<>();
        path.add(SlangSource.fromFile(operation));
        CompilationArtifact artifact = compiler.compile(SlangSource.fromFile(flow), path);
        assertNotNull("artifact is null", artifact);

        ExecutionPlan executionPlan = artifact.getExecutionPlan();
        assertNotNull("executionPlan is null", executionPlan);

        ExecutionStep addBranchesStep = executionPlan.getStep(2L);
        assertTrue("add branches step is not marked as split step", addBranchesStep.isSplitStep());
        Map<String, ?> addBranchesActionData = addBranchesStep.getActionData();

        verifyParallelLoopStatement(addBranchesActionData);

        ExecutionStep joinBranchesStep = executionPlan.getStep(5L);
        Map<String, ?> joinBranchesActionData = joinBranchesStep.getActionData();

        verifyPublishValues(joinBranchesActionData);

        assertNotNull("branch begin step method not found", executionPlan.getStep(3L));
        ExecutionStep branchEndStepExecutionStep = executionPlan.getStep(4L);
        assertNotNull("branch end step method not found", branchEndStepExecutionStep);

        verifyBranchPublishValuesIsEmpty(branchEndStepExecutionStep.getActionData());
    }

    @Test
    public void testCompileParallelLoopFlowNavigate() throws Exception {
        URI flow = getClass().getResource("/loops/parallel_loop/parallel_loop_navigate.sl").toURI();
        URI operation1 = getClass().getResource("/loops/parallel_loop/print_branch.sl").toURI();
        URI operation2 = getClass().getResource("/loops/parallel_loop/print_list.sl").toURI();
        Set<SlangSource> path = new HashSet<>();
        path.add(SlangSource.fromFile(operation1));
        path.add(SlangSource.fromFile(operation2));
        CompilationArtifact artifact = compiler.compile(SlangSource.fromFile(flow), path);
        assertNotNull("artifact is null", artifact);

        ExecutionPlan executionPlan = artifact.getExecutionPlan();
        assertNotNull("executionPlan is null", executionPlan);

        ExecutionStep addBranchesStep = executionPlan.getStep(2L);
        assertTrue("add branches step is not marked as split step", addBranchesStep.isSplitStep());
        Map<String, ?> addBranchesActionData = addBranchesStep.getActionData();

        verifyParallelLoopStatement(addBranchesActionData);

        ExecutionStep joinBranchesStep = executionPlan.getStep(5L);
        Map<String, ?> joinBranchesActionData = joinBranchesStep.getActionData();

        verifyNavigationValues(joinBranchesActionData);

        assertNotNull("branch begin step method not found", executionPlan.getStep(3L));
        assertNotNull("branch end step method not found", executionPlan.getStep(4L));
    }

    @Test
    public void testCompileParallelLoopFlowPublishNavigate() throws Exception {
        URI flow = getClass().getResource("/loops/parallel_loop/parallel_loop_publish_navigate.sl").toURI();
        URI operation1 = getClass().getResource("/loops/parallel_loop/print_branch.sl").toURI();
        URI operation2 = getClass().getResource("/loops/parallel_loop/print_list.sl").toURI();
        Set<SlangSource> path = new HashSet<>();
        path.add(SlangSource.fromFile(operation1));
        path.add(SlangSource.fromFile(operation2));
        CompilationArtifact artifact = compiler.compile(SlangSource.fromFile(flow), path);
        assertNotNull("artifact is null", artifact);

        ExecutionPlan executionPlan = artifact.getExecutionPlan();
        assertNotNull("executionPlan is null", executionPlan);

        ExecutionStep addBranchesStep = executionPlan.getStep(2L);
        assertTrue("add branches step is not marked as split step", addBranchesStep.isSplitStep());
        Map<String, ?> addBranchesActionData = addBranchesStep.getActionData();

        verifyParallelLoopStatement(addBranchesActionData);

        ExecutionStep joinBranchesStep = executionPlan.getStep(5L);
        Map<String, ?> joinBranchesActionData = joinBranchesStep.getActionData();

        verifyPublishValues(joinBranchesActionData);

        verifyNavigationValues(joinBranchesActionData);

        assertNotNull("branch begin step method not found", executionPlan.getStep(3L));
        ExecutionStep branchEndStepExecutionStep = executionPlan.getStep(4L);
        assertNotNull("branch end step method not found", branchEndStepExecutionStep);

        verifyBranchPublishValuesIsEmpty(branchEndStepExecutionStep.getActionData());
    }

    private void verifyPublishValues(Map<String, ?> joinBranchesActionData) {
        assertTrue(joinBranchesActionData.containsKey(ScoreLangConstants.STEP_PUBLISH_KEY));
        @SuppressWarnings("unchecked") List<Output> actualPublishOutputs =
                (List<Output>) joinBranchesActionData.get(ScoreLangConstants.STEP_PUBLISH_KEY);
        List<Output> expectedPublishOutputs = new ArrayList<>();
        expectedPublishOutputs.add(new Output("name_list", "${ map(lambda x:str(x['name']), branches_context) }"));
        expectedPublishOutputs.add(new Output("number_from_last_branch", "${ branches_context[-1]['number'] }"));
        assertEquals("publish values not as expected", expectedPublishOutputs, actualPublishOutputs);
    }

    private void verifyBranchPublishValuesIsEmpty(Map<String, ?> actionData) {
        @SuppressWarnings("unchecked") List<Output> publishValues =
                (List<Output>) actionData.get(ScoreLangConstants.STEP_PUBLISH_KEY);
        assertTrue(CollectionUtils.isEmpty(publishValues));
    }

    private void verifyNavigationValues(Map<String, ?> joinBranchesActionData) {
        assertTrue(joinBranchesActionData.containsKey(ScoreLangConstants.STEP_NAVIGATION_KEY));
        @SuppressWarnings("unchecked") Map<String, ResultNavigation> actualNavigateValues =
                (Map<String, ResultNavigation>) joinBranchesActionData.get(ScoreLangConstants.STEP_NAVIGATION_KEY);
        Map<String, ResultNavigation> expectedNavigationValues = new HashMap<>();
        expectedNavigationValues.put("SUCCESS", new ResultNavigation(6L, null));
        expectedNavigationValues.put("FAILURE", new ResultNavigation(0L, "FAILURE"));
        assertEquals("navigation values not as expected", expectedNavigationValues, actualNavigateValues);
    }

    private void verifyParallelLoopStatement(Map<String, ?> addBranchesActionData) {
        assertTrue(addBranchesActionData.containsKey(ScoreLangConstants.PARALLEL_LOOP_STATEMENT_KEY));
        ParallelLoopStatement parallelLoopStatement =
                (ParallelLoopStatement) addBranchesActionData.get(ScoreLangConstants.PARALLEL_LOOP_STATEMENT_KEY);
        assertEquals("parallel loop statement value not as expected", "value", parallelLoopStatement.getVarName());
        assertEquals("parallel loop statement expression not as expected", "values", parallelLoopStatement.getExpression());
    }

    private Deque<Step> getStepsAfterPrecompileFlow(String flowPath) throws URISyntaxException {
        URI flow = getClass().getResource(flowPath).toURI();
        Executable executable = compiler.preCompile(SlangSource.fromFile(flow));
        assertNotNull("executable is null", executable);

        return ((Flow) executable).getWorkflow().getSteps();
    }

    private void verifyParallelLoopStatement(Step step) {
        assertTrue(step.getPreStepActionData().containsKey(SlangTextualKeys.PARALLEL_LOOP_KEY));
        ParallelLoopStatement parallelLoopStatement = (ParallelLoopStatement) step.getPreStepActionData()
                .get(SlangTextualKeys.PARALLEL_LOOP_KEY);
        assertEquals("values", parallelLoopStatement.getExpression());
        assertEquals("value", parallelLoopStatement.getVarName());
    }

    private List<Output> getPublishOutputs(Step step) {
        assertTrue(step.getPostStepActionData().containsKey(SlangTextualKeys.PUBLISH_KEY));
        @SuppressWarnings("unchecked") List<Output> publishValues = (List<Output>) step.getPostStepActionData().get(SlangTextualKeys.PUBLISH_KEY);
        assertNotNull("publish list is null", publishValues);
        return publishValues;
    }

    private void verifyNavigationStrings(List<Map<String, String>> expectedNavigationStrings, Step step) {
        List<Map<String, String>> actualNavigationStrings = step.getNavigationStrings();
        assertEquals(expectedNavigationStrings, actualNavigationStrings);
    }

}
