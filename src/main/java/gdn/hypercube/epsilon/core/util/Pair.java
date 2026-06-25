package gdn.hypercube.epsilon.core.util;

import com.github.bsideup.jabel.Desugar;

@Desugar
public record Pair<L, R>(L left, R right) {}