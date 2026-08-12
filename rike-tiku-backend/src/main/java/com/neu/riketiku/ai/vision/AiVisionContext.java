package com.neu.riketiku.ai.vision;

import java.util.List;

public record AiVisionContext(String diagramType, String summary, List<String> visibleText,
                              List<String> relations, List<String> uncertainty) { }
