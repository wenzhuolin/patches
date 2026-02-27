package com.patches.plm.service.workflow;

import com.patches.plm.domain.enums.GateType;
import com.patches.plm.domain.enums.PatchAction;
import com.patches.plm.domain.enums.PatchState;
import com.patches.plm.domain.enums.StageType;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

@Component
public class PatchStateMachine {

    private final Map<PatchState, Map<PatchAction, TransitionDefinition>> transitions = new EnumMap<>(PatchState.class);

    public PatchStateMachine() {
        register(PatchState.DRAFT, PatchAction.SUBMIT_REVIEW,
                new TransitionDefinition(PatchState.REVIEWING, null, null, false, false));

        register(PatchState.REVIEWING, PatchAction.APPROVE_REVIEW,
                new TransitionDefinition(PatchState.REVIEW_PASSED, StageType.REVIEW, GateType.EXIT, true, true));
        register(PatchState.REVIEWING, PatchAction.REJECT_REVIEW,
                new TransitionDefinition(PatchState.DRAFT, null, null, false, false));

        register(PatchState.REVIEW_PASSED, PatchAction.TRANSFER_TO_TEST,
                new TransitionDefinition(PatchState.TESTING, StageType.TRANSFER_TEST, GateType.ENTRY, true, true));

        register(PatchState.TESTING, PatchAction.PASS_TEST,
                new TransitionDefinition(PatchState.TEST_PASSED, null, null, false, false));
        register(PatchState.TESTING, PatchAction.FAIL_TEST,
                new TransitionDefinition(PatchState.DRAFT, null, null, false, false));

        register(PatchState.TEST_PASSED, PatchAction.PREPARE_RELEASE,
                new TransitionDefinition(PatchState.RELEASE_READY, StageType.RELEASE, GateType.ENTRY, true, false));

        register(PatchState.RELEASE_READY, PatchAction.RELEASE,
                new TransitionDefinition(PatchState.RELEASED, StageType.RELEASE, GateType.EXIT, true, false));

        register(PatchState.RELEASED, PatchAction.ARCHIVE,
                new TransitionDefinition(PatchState.ARCHIVED, null, null, false, false));
    }

    private void register(PatchState from, PatchAction action, TransitionDefinition definition) {
        transitions.computeIfAbsent(from, k -> new EnumMap<>(PatchAction.class))
                .put(action, definition);
    }

    public Optional<TransitionDefinition> next(PatchState current, PatchAction action) {
        return Optional.ofNullable(transitions.getOrDefault(current, Map.of()).get(action));
    }

    public record TransitionDefinition(PatchState toState,
                                       StageType stage,
                                       GateType gateType,
                                       boolean requireKpi,
                                       boolean requireQa) {
    }
}
