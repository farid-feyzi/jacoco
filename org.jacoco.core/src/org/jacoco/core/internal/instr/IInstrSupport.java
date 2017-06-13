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

/**
 * @author LLT
 *
 */
public interface IInstrSupport {

	/**
	 * @return Name of the field that stores coverage information of a class.
	 */
	public String getDatafieldName();

	/**
	 * @return Access modifiers of the field that stores coverage information of a
	 * class.
	 */
	public int getDatafieldAcc();

	/**
	 * @return Access modifiers of the field that stores coverage information of a Java
	 * 8 interface.
	 */
	public int getDatafieldIntfAcc();

	/**
	 * @return Data type of the field that stores coverage information for a class
	 */
	public String getDatafieldDesc();

	/**
	 * @return Name of the added initialization method for instrumentation purpose.
	 */
	public String getInitmethodName();

	/**
	 * @return Descriptor of the initialization method.
	 */
	public String getInitmethodDesc();

	/**
	 * @return Access modifiers of the initialization method.
	 */
	public int getInitmethodAcc();

	/**
	 * @return Name of the interface initialization method.
	 */
	public String getClinitName();

	/**
	 * @return Descriptor of the interface initialization method.
	 */
	public String getClinitDesc();

	/**
	 * @return Access flags of the interface initialization method generated by JaCoCo.
	 */
	public int getClinitAcc();

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
	public void assertNotInstrumented(String member, String owner) throws IllegalStateException;

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
	public void push(MethodVisitor mv, int value);

	/**
	 * Generate instructions to update probes.
	 * @param mv  visitor to emit the instruction
	 * @param id probes index
	 * @param variable variable index of probe array 
	 */
	public void insertProbe(MethodVisitor mv, int id, int variable);

	/**
	 * @return Stack usage of code to modify probe array 
	 */
	public int getInsertProbeStackSize();

}
