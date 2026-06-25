package gdn.hypercube.epsilon.core.util;

import com.github.bsideup.jabel.Desugar;

@Desugar
public record Triple<L, M, R>(L left, M middle, R right) {}