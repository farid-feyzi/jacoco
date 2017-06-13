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

import static java.lang.String.format;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Constants and utilities for byte code instrumentation.
 */
public class InstrSupport implements IInstrSupport {

	/** ASM API version */
	public static final int ASM_API_VERSION = Opcodes.ASM5;
	// === Data Field ===

	/**
	 * Name of the field that stores coverage information of a class.
	 */
	static final String DATAFIELD_NAME = "$jacocoData";

	/**
	 * Access modifiers of the field that stores coverage information of a
	 * class.
	 *
	 * According to Java Virtual Machine Specification <a href=
	 * "https://docs.oracle.com/javase/specs/jvms/se8/html/jvms-6.html#jvms-6.5.putstatic">
	 * §6.5.putstatic</a> this field must not be final:
	 *
	 * <blockquote>
	 * <p>
	 * if the field is final, it must be declared in the current class, and the
	 * instruction must occur in the {@code <clinit>} method of the current
	 * class.
	 * </p>
	 * </blockquote>
	 */
	static final int DATAFIELD_ACC = Opcodes.ACC_SYNTHETIC | Opcodes.ACC_PRIVATE
			| Opcodes.ACC_STATIC | Opcodes.ACC_TRANSIENT;

	/**
	 * Access modifiers of the field that stores coverage information of a Java
	 * 8 interface.
	 *
	 * According to Java Virtual Machine Specification <a href=
	 * "https://docs.oracle.com/javase/specs/jvms/se8/html/jvms-4.html#jvms-4.5-200-A.3">
	 * §4.5</a>:
	 *
	 * <blockquote>
	 * <p>
	 * Fields of interfaces must have their ACC_PUBLIC, ACC_STATIC, and
	 * ACC_FINAL flags set; they may have their ACC_SYNTHETIC flag set and must
	 * not have any of the other flags.
	 * </p>
	 * </blockquote>
	 */
	static final int DATAFIELD_INTF_ACC = Opcodes.ACC_SYNTHETIC
			| Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL;

	/**
	 * Data type of the field that stores coverage information for a class (
	 * <code>boolean[]</code>).
	 */
	static final String DATAFIELD_DESC = "[Z";

	// === Init Method ===

	/**
	 * Name of the initialization method.
	 */
	static final String INITMETHOD_NAME = "$jacocoInit";

	/**
	 * Descriptor of the initialization method.
	 */
	static final String INITMETHOD_DESC = "()[Z";

	/**
	 * Access modifiers of the initialization method.
	 */
	static final int INITMETHOD_ACC = Opcodes.ACC_SYNTHETIC
			| Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC;

	/**
	 * Name of the interface initialization method.
	 *
	 * According to Java Virtual Machine Specification <a href=
	 * "https://docs.oracle.com/javase/specs/jvms/se8/html/jvms-2.html#jvms-2.9-200">
	 * §2.9</a>:
	 *
	 * <blockquote>
	 * <p>
	 * A class or interface has at most one class or interface initialization
	 * method and is initialized by invoking that method. The initialization
	 * method of a class or interface has the special name {@code <clinit>},
	 * takes no arguments, and is void.
	 * </p>
	 * <p>
	 * Other methods named {@code <clinit>} in a class file are of no
	 * consequence. They are not class or interface initialization methods. They
	 * cannot be invoked by any Java Virtual Machine instruction and are never
	 * invoked by the Java Virtual Machine itself.
	 * </p>
	 * <p>
	 * In a class file whose version number is 51.0 or above, the method must
	 * additionally have its ACC_STATIC flag set in order to be the class or
	 * interface initialization method.
	 * </p>
	 * <p>
	 * This requirement was introduced in Java SE 7. In a class file whose
	 * version number is 50.0 or below, a method named {@code <clinit>} that is
	 * void and takes no arguments is considered the class or interface
	 * initialization method regardless of the setting of its ACC_STATIC flag.
	 * </p>
	 * </blockquote>
	 *
	 * And <a href=
	 * "https://docs.oracle.com/javase/specs/jvms/se8/html/jvms-4.html#jvms-4.6-200-A.6">
	 * §4.6</a>:
	 *
	 * <blockquote>
	 * <p>
	 * Class and interface initialization methods are called implicitly by the
	 * Java Virtual Machine. The value of their access_flags item is ignored
	 * except for the setting of the ACC_STRICT flag.
	 * </p>
	 * </blockquote>
	 */
	static final String CLINIT_NAME = "<clinit>";

	/**
	 * Descriptor of the interface initialization method.
	 *
	 * @see #CLINIT_NAME
	 */
	static final String CLINIT_DESC = "()V";

	/**
	 * Access flags of the interface initialization method generated by JaCoCo.
	 *
	 * @see #CLINIT_NAME
	 */
	static final int CLINIT_ACC = Opcodes.ACC_SYNTHETIC | Opcodes.ACC_STATIC;

	/**
	 * Ensures that the given member does not correspond to a internal member
	 * created by the instrumentation process. This would mean that the class is
	 * already instrumented.
	 * 
	 * @param member
	 *            name of the member to check
	 * @param owner
	 *            name of the class owning the member
	 * @throws IllegalStateException
	 *             thrown if the member has the same name than the
	 *             instrumentation member
	 */
	public void assertNotInstrumented(final String member, final String owner)
			throws IllegalStateException {
		if (member.equals(getDatafieldName())
				|| member.equals(INITMETHOD_NAME)) {
			throw new IllegalStateException(
					format("Class %s is already instrumented.", owner));
		}
	}

	/**
	 * Generates the instruction to push the given int value on the stack.
	 * Implementation taken from
	 * {@link org.objectweb.asm.commons.GeneratorAdapter#push(int)}.
	 * 
	 * @param mv
	 *            visitor to emit the instruction
	 * @param value
	 *            the value to be pushed on the stack.
	 */
	public void push(final MethodVisitor mv, final int value) {
		if (value >= -1 && value <= 5) {
			mv.visitInsn(Opcodes.ICONST_0 + value);
		} else if (value >= Byte.MIN_VALUE && value <= Byte.MAX_VALUE) {
			mv.visitIntInsn(Opcodes.BIPUSH, value);
		} else if (value >= Short.MIN_VALUE && value <= Short.MAX_VALUE) {
			mv.visitIntInsn(Opcodes.SIPUSH, value);
		} else {
			mv.visitLdcInsn(Integer.valueOf(value));
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.jacoco.core.internal.instr.IInstrSupport#insertProbe(int)
	 */
	public void insertProbe(final MethodVisitor mv, final int id,
			final int variable) {
		// For a probe we set the corresponding position in the boolean[] array
		// to true.

		mv.visitVarInsn(Opcodes.ALOAD, variable);

		// Stack[0]: [Z

		push(mv, id);

		// Stack[1]: I
		// Stack[0]: [Z

		mv.visitInsn(Opcodes.ICONST_1);

		// Stack[2]: I
		// Stack[1]: I
		// Stack[0]: [Z

		mv.visitInsn(Opcodes.BASTORE);
	}
	
	/* (non-Javadoc)
	 * @see org.jacoco.core.internal.instr.IInstrSupport#getInsertProbeStackSize()
	 * max stack size use for code generated in insertProbe()
	 */
	public int getInsertProbeStackSize() {
		return 3;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.jacoco.core.internal.instr.IInstrSupport#getDatafieldName()
	 */
	public String getDatafieldName() {
		return DATAFIELD_NAME;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.jacoco.core.internal.instr.IInstrSupport#getDatafieldAcc()
	 */
	public int getDatafieldAcc() {
		return DATAFIELD_ACC;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.jacoco.core.internal.instr.IInstrSupport#getDatafieldIntfAcc()
	 */
	public int getDatafieldIntfAcc() {
		return DATAFIELD_INTF_ACC;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.jacoco.core.internal.instr.IInstrSupport#getDatafieldDesc()
	 */
	public String getDatafieldDesc() {
		return DATAFIELD_DESC;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.jacoco.core.internal.instr.IInstrSupport#getInitmethodName()
	 */
	public String getInitmethodName() {
		return INITMETHOD_NAME;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.jacoco.core.internal.instr.IInstrSupport#getInitmethodDesc()
	 */
	public String getInitmethodDesc() {
		return INITMETHOD_DESC;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.jacoco.core.internal.instr.IInstrSupport#getInitmethodAcc()
	 */
	public int getInitmethodAcc() {
		return INITMETHOD_ACC;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.jacoco.core.internal.instr.IInstrSupport#getClinitName()
	 */
	public String getClinitName() {
		return CLINIT_NAME;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.jacoco.core.internal.instr.IInstrSupport#getClinitDesc()
	 */
	public String getClinitDesc() {
		return CLINIT_DESC;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.jacoco.core.internal.instr.IInstrSupport#getClinitAcc()
	 */
	public int getClinitAcc() {
		return CLINIT_ACC;
	}

}
