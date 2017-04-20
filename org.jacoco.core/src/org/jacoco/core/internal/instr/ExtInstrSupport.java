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
package org.jacoco.core.internal.instr;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * @author LLT
 *
 */
public class ExtInstrSupport extends InstrSupport implements IInstrSupport {

	/**
	 * Data type of the field that stores coverage information for a class (
	 * <code>int[]</code>).
	 */
	static final String EXT_DATAFIELD_DESC = "[I";

	// === Init Method ===
	/**
	 * Descriptor of the initialization method.
	 */
	static final String EXT_INITMETHOD_DESC = "()[I";

	@Override
	public String getDatafieldDesc() {
		return EXT_DATAFIELD_DESC;
	}

	@Override
	public String getInitmethodDesc() {
		return EXT_INITMETHOD_DESC;
	}

	/**
	 * @param id
	 *            probeId
	 * @param variable
	 *            order of probe array variable
	 */
	@Override
	public void insertProbe(final MethodVisitor mv, final int id,
			final int variable) {
		/*
		 * For a probe we increase 1 for the value at corresponding position in
		 * the int[] array
		 */
		mv.visitVarInsn(Opcodes.ALOAD, variable);

		// Stack[0]: [I

		push(mv, id);

		// Stack[1]: I
		// Stack[0]: [I

		mv.visitInsn(Opcodes.DUP2);

		// Stack[3]: I
		// Stack[2]: [I
		// Stack[1]: I
		// Stack[0]: [I

		mv.visitInsn(Opcodes.IALOAD);

		// Stack[2]: I
		// Stack[1]: I
		// Stack[0]: [I

		mv.visitInsn(Opcodes.ICONST_1);

		// Stack[3]: I
		// Stack[2]: I
		// Stack[1]: I
		// Stack[0]: [I

		mv.visitInsn(Opcodes.IADD);

		// Stack[2]: I
		// Stack[1]: I
		// Stack[0]: [I

		mv.visitInsn(Opcodes.IASTORE);
	}

}
