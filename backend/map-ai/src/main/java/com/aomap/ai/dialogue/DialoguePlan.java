package com.aomap.ai.dialogue;

import java.util.List;

public record DialoguePlan(String reply, List<MapAction> actions, List<FollowUp> followUps) {
}
