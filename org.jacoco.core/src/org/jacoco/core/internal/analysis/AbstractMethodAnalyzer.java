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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jacoco.core.analysis.ISourceNode;
import org.jacoco.core.internal.analysis.filter.EnumFilter;
import org.jacoco.core.internal.analysis.filter.IFilter;
import org.jacoco.core.internal.analysis.filter.IFilterOutput;
import org.jacoco.core.internal.analysis.filter.LombokGeneratedFilter;
import org.jacoco.core.internal.analysis.filter.SynchronizedFilter;
import org.jacoco.core.internal.analysis.filter.SyntheticFilter;
import org.jacoco.core.internal.flow.IFrame;
import org.jacoco.core.internal.flow.Instruction;
import org.jacoco.core.internal.flow.LabelInfo;
import org.jacoco.core.internal.flow.MethodProbesVisitor;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TryCatchBlockNode;

/**
 * A {@link MethodProbesVisitor} that analyzes which statements and branches of
 * a method have been executed based on given probe data.
 */
public abstract class AbstractMethodAnalyzer extends MethodProbesVisitor
		implements IFilterOutput {

	private static final IFilter[] FILTERS = new IFilter[] { new EnumFilter(),
			new SyntheticFilter(), new SynchronizedFilter(),
			new LombokGeneratedFilter() };

	private final String className;

	private final String superClassName;

	private int currentLine = ISourceNode.UNKNOWN_LINE;

	@SuppressWarnings("javadoc")
	protected int firstLine = ISourceNode.UNKNOWN_LINE;

	@SuppressWarnings("javadoc")
	protected int lastLine = ISourceNode.UNKNOWN_LINE;

	// Due to ASM issue #315745 there can be more than one label per instruction
	private final List<Label> currentLabel = new ArrayList<Label>(2);

	/** List of all analyzed instructions */
	protected final List<Instruction> instructions = new ArrayList<Instruction>();

	/** List of all jumps encountered */
	protected final List<Jump> jumps = new ArrayList<Jump>();

	/** Last instruction in byte code sequence */
	protected Instruction lastInsn;

	/**
	 * New Method analyzer for the given probe data.
	 * 
	 * @param className
	 *            class name
	 * @param superClassName
	 *            superclass name
	 */
	public AbstractMethodAnalyzer(final String className,
			final String superClassName) {
		super();
		this.className = className;
		this.superClassName = superClassName;
	}

	/**
	 * {@link MethodNode#accept(MethodVisitor)}
	 */
	@Override
	public void accept(final MethodNode methodNode,
			final MethodVisitor methodVisitor) {
		this.ignored.clear();
		for (final IFilter filter : FILTERS) {
			filter.filter(className, superClassName, methodNode, this);
		}

		for (final TryCatchBlockNode n : methodNode.tryCatchBlocks) {
			n.accept(methodVisitor);
		}
		currentNode = methodNode.instructions.getFirst();
		while (currentNode != null) {
			currentNode.accept(methodVisitor);
			currentNode = currentNode.getNext();
		}
		methodVisitor.visitEnd();
	}

	@SuppressWarnings("javadoc")
	protected final Set<AbstractInsnNode> ignored = new HashSet<AbstractInsnNode>();
	private AbstractInsnNode currentNode;

	public void ignore(final AbstractInsnNode fromInclusive,
			final AbstractInsnNode toInclusive) {
		for (AbstractInsnNode i = fromInclusive; i != toInclusive; i = i
				.getNext()) {
			ignored.add(i);
		}
		ignored.add(toInclusive);
	}

	@Override
	public void visitLabel(final Label label) {
		currentLabel.add(label);
		if (!LabelInfo.isSuccessor(label)) {
			lastInsn = null;
		}
	}

	@Override
	public void visitLineNumber(final int line, final Label start) {
		currentLine = line;
		if (firstLine > line || lastLine == ISourceNode.UNKNOWN_LINE) {
			firstLine = line;
		}
		if (lastLine < line) {
			lastLine = line;
		}
	}

	@SuppressWarnings("javadoc")
	protected void visitInsn() {
		final Instruction insn = createInsn(currentNode, currentLine);
		instructions.add(insn);
		if (lastInsn != null) {
			insn.setPredecessor(lastInsn);
		}
		final int labelCount = currentLabel.size();
		if (labelCount > 0) {
			for (int i = labelCount; --i >= 0;) {
				LabelInfo.setInstruction(currentLabel.get(i), insn);
			}
			currentLabel.clear();
		}
		lastInsn = insn;
	}

	/**
	 * init instruction.
	 * 
	 * @param node
	 *            asm node
	 * @param line
	 *            line number
	 * @return
	 */
	protected abstract Instruction createInsn(AbstractInsnNode node, int line);

	@Override
	public void visitInsn(final int opcode) {
		visitInsn();
	}

	@Override
	public void visitIntInsn(final int opcode, final int operand) {
		visitInsn();
	}

	@Override
	public void visitVarInsn(final int opcode, final int var) {
		visitInsn();
	}

	@Override
	public void visitTypeInsn(final int opcode, final String type) {
		visitInsn();
	}

	@Override
	public void visitFieldInsn(final int opcode, final String owner,
			final String name, final String desc) {
		visitInsn();
	}

	@Override
	public void visitMethodInsn(final int opcode, final String owner,
			final String name, final String desc, final boolean itf) {
		visitInsn();
	}

	@Override
	public void visitInvokeDynamicInsn(final String name, final String desc,
			final Handle bsm, final Object... bsmArgs) {
		visitInsn();
	}

	@Override
	public void visitJumpInsn(final int opcode, final Label label) {
		visitInsn();
		jumps.add(new Jump(lastInsn, label));
	}

	@Override
	public void visitLdcInsn(final Object cst) {
		visitInsn();
	}

	@Override
	public void visitIincInsn(final int var, final int increment) {
		visitInsn();
	}

	@Override
	public void visitTableSwitchInsn(final int min, final int max,
			final Label dflt, final Label... labels) {
		visitSwitchInsn(dflt, labels);
	}

	@Override
	public void visitLookupSwitchInsn(final Label dflt, final int[] keys,
			final Label[] labels) {
		visitSwitchInsn(dflt, labels);
	}

	private void visitSwitchInsn(final Label dflt, final Label[] labels) {
		visitInsn();
		LabelInfo.resetDone(labels);
		jumps.add(new Jump(lastInsn, dflt));
		LabelInfo.setDone(dflt);
		for (final Label l : labels) {
			if (!LabelInfo.isDone(l)) {
				jumps.add(new Jump(lastInsn, l));
				LabelInfo.setDone(l);
			}
		}
	}

	@Override
	public void visitMultiANewArrayInsn(final String desc, final int dims) {
		visitInsn();
	}

	@Override
	public void visitProbe(final int probeId) {
		addProbe(probeId);
		lastInsn = null;
	}

	@Override
	public void visitJumpInsnWithProbe(final int opcode, final Label label,
			final int probeId, final IFrame frame) {
		visitInsn();
		addProbe(probeId);
	}

	@Override
	public void visitInsnWithProbe(final int opcode, final int probeId) {
		visitInsn();
		addProbe(probeId);
	}

	@Override
	public void visitTableSwitchInsnWithProbes(final int min, final int max,
			final Label dflt, final Label[] labels, final IFrame frame) {
		visitSwitchInsnWithProbes(dflt, labels);
	}

	@Override
	public void visitLookupSwitchInsnWithProbes(final Label dflt,
			final int[] keys, final Label[] labels, final IFrame frame) {
		visitSwitchInsnWithProbes(dflt, labels);
	}

	private void visitSwitchInsnWithProbes(final Label dflt,
			final Label[] labels) {
		visitInsn();
		LabelInfo.resetDone(dflt);
		LabelInfo.resetDone(labels);
		visitSwitchTarget(dflt);
		for (final Label l : labels) {
			visitSwitchTarget(l);
		}
	}

	private void visitSwitchTarget(final Label label) {
		final int id = LabelInfo.getProbeId(label);
		if (!LabelInfo.isDone(label)) {
			if (id == LabelInfo.NO_PROBE) {
				jumps.add(new Jump(lastInsn, label));
			} else {
				addProbe(id);
			}
			LabelInfo.setDone(label);
		}
	}

	@Override
	public void visitEnd() {
		// Wire jumps:
		for (final Jump j : jumps) {
			LabelInfo.getInstruction(j.target).setPredecessor(j.source);
		}
	}

	/**
	 * @param probeId
	 *            probe index in probes array
	 */
	protected abstract void addProbe(final int probeId);

	/**
	 * to store jump infor for jump inst
	 */
	protected static class Jump {

		final Instruction source;
		final Label target;

		@SuppressWarnings("javadoc")
		public Jump(final Instruction source, final Label target) {
			this.source = source;
			this.target = target;
		}

		/**
		 * @return the target
		 */
		public Label getTarget() {
			return target;
		}

		/**
		 * @return the source
		 */
		public Instruction getSource() {
			return source;
		}
	}

}
