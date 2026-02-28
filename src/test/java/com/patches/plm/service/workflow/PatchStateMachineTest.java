package com.patches.plm.service.workflow;

import com.patches.plm.domain.enums.GateType;
import com.patches.plm.domain.enums.PatchAction;
import com.patches.plm.domain.enums.PatchState;
import com.patches.plm.domain.enums.StageType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class PatchStateMachineTest {

    private final PatchStateMachine stateMachine = new PatchStateMachine();

    @Test
    void shouldFindReviewApproveTransition() {
        var transition = stateMachine.next(PatchState.REVIEWING, PatchAction.APPROVE_REVIEW);
        Assertions.assertTrue(transition.isPresent());
        Assertions.assertEquals(PatchState.REVIEW_PASSED, transition.get().toState());
        Assertions.assertEquals(StageType.REVIEW, transition.get().stage());
        Assertions.assertEquals(GateType.EXIT, transition.get().gateType());
        Assertions.assertTrue(transition.get().requireKpi());
        Assertions.assertTrue(transition.get().requireQa());
    }

    @Test
    void shouldRejectInvalidTransition() {
        var transition = stateMachine.next(PatchState.DRAFT, PatchAction.RELEASE);
        Assertions.assertTrue(transition.isEmpty());
    }
}
