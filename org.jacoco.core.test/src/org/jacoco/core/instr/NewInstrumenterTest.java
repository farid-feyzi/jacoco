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
package org.jacoco.core.instr;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import org.jacoco.core.data.ExecutionData;
import org.jacoco.core.data.ExecutionData.ProbesType;
import org.jacoco.core.runtime.URLStreamHandlerRuntime;
import org.junit.Test;

/**
 * @author LLT
 *
 */
public class NewInstrumenterTest {
	// private final String SRC_FOLDER =
	// "/Users/lylytran/Projects/Ziyuan-master/app/tzuyu.tools/target/classes/tools/bytecode";
	// private final String TARGET_FOLDER =
	// "/Users/lylytran/src/tools/bytecode";
	private final String SRC_FOLDER = "/Users/lylytran/apache-common-math-2.2/apache-common-math-2.2/bin";
	private final String TARGET_FOLDER = "/Users/lylytran/src";

	@Test
	public void writeFile() throws Exception {
		ExecutionData.setProbesType(ProbesType.INTEGER);
		String classSimpleName = "FastMath";
		String className = "org.apache.commons.math.util." + classSimpleName;
		String classPath = className.replace(".", "/") + ".class";
		String clazzFile = new StringBuilder("/").append(classPath).toString();

		File outFile = getFile(TARGET_FOLDER, clazzFile);
		FileOutputStream out = new FileOutputStream(outFile);
		System.out.println(outFile.getAbsolutePath());
		File inFile = new File(SRC_FOLDER + clazzFile);
		FileInputStream in = new FileInputStream(inFile);

		byte[] data = new byte[(int) outFile.getTotalSpace()];
		in.read(data);
		data = instrument(data, className);
		System.out.println(new String(data));
		out.write(data);
		out.close();
		in.close();
		ExecutionData.setProbesType(ProbesType.BOOLEAN);
	}

	private File getFile(String folder, String fileName) throws Exception {
		File dir = new File(folder);
		if (!dir.exists()) {
			dir.mkdirs();
		}
		File file = new File(folder + fileName);
		if (!file.exists()) {
			file.createNewFile();
		}
		return file;
	}

	private byte[] instrument(byte[] data, String className) throws Exception {
		Instrumenter inst = new Instrumenter(new URLStreamHandlerRuntime());
		return inst.instrument(data, className);
	}
}
