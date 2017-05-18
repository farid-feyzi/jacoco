/*******************************************************************************
 * Copyright (c) 2009, 2017 Mountainminds GmbH & Co. KG and Contributors
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Marc R. Hoffmann - initial API and implementation
 *    
 *******************************************************************************/
package org.jacoco.core.internal.analysis;

import java.util.ArrayList;
import java.util.List;

import org.jacoco.core.analysis.ICounter;
import org.jacoco.core.analysis.IMethodCoverage;
import org.jacoco.core.internal.flow.Instruction;
import org.jacoco.core.internal.flow.MethodProbesVisitor;
import org.objectweb.asm.tree.AbstractInsnNode;

/**
 * A {@link MethodProbesVisitor} that analyzes which statements and branches of
 * a method have been executed based on given probe data.
 */
public class MethodAnalyzer extends AbstractMethodAnalyzer {

	private final boolean[] probes;

	private final MethodCoverageImpl coverage;

	/** List of all predecessors of covered probes */
	private final List<Instruction> coveredProbes = new ArrayList<Instruction>();

	/**
	 * New Method analyzer for the given probe data.
	 * 
	 * @param className
	 *            class name
	 * @param superClassName
	 *            superclass name
	 * @param name
	 *            method name
	 * @param desc
	 *            method descriptor
	 * @param signature
	 *            optional parameterized signature
	 * 
	 * @param probes
	 *            recorded probe date of the containing class or
	 *            <code>null</code> if the class is not executed at all
	 */
	public MethodAnalyzer(final String className, final String superClassName,
			final String name, final String desc, final String signature,
			final boolean[] probes) {
		super(className, superClassName);
		this.probes = probes;
		this.coverage = new MethodCoverageImpl(name, desc, signature);
	}

	/**
	 * Returns the coverage data for this method after this visitor has been
	 * processed.
	 * 
	 * @return coverage data for this method
	 */
	public IMethodCoverage getCoverage() {
		return coverage;
	}

	@Override
	protected Instruction createInsn(final AbstractInsnNode node,
			final int line) {
		return new Instruction(node, line);
	}

	@Override
	public void visitEnd() {
		super.visitEnd();
		// Propagate probe values:
		for (final Instruction p : coveredProbes) {
			p.setCovered();
		}
		// Report result:
		coverage.ensureCapacity(firstLine, lastLine);
		for (final Instruction i : instructions) {
			if (ignored.contains(i.getNode())) {
				continue;
			}

			final int total = i.getBranches();
			final int covered = i.getCoveredBranches();
			final ICounter instrCounter = covered == 0 ? CounterImpl.COUNTER_1_0
					: CounterImpl.COUNTER_0_1;
			final ICounter branchCounter = total > 1
					? CounterImpl.getInstance(total - covered, covered)
					: CounterImpl.COUNTER_0_0;
			coverage.increment(instrCounter, branchCounter, i.getLine());
		}
		coverage.incrementMethodCounter();
	}

	@Override
	protected void addProbe(final int probeId) {
		lastInsn.addBranch();
		if (probes != null && probes[probeId]) {
			coveredProbes.add(lastInsn);
		}
	}

}
