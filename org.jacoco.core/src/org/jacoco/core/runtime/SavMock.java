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

package org.jacoco.core.runtime;

import static java.lang.String.format;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Field;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.List;

import org.jacoco.core.data.ExecutionData;
import org.jacoco.core.data.ExecutionDataWriter;
import org.jacoco.core.data.SessionInfo;

/**
 * @author lylytran
 *
 */
public class SavMock {
	/**
	 * savMock field in instrumented class.
	 */
	public static final String accessFieldName = "_savMock";
	
	private RuntimeData data;
	private List<ByteArrayOutputStream> buffs = new ArrayList<ByteArrayOutputStream>();
	private File destFile;
	private boolean append;
	private File logFile;
	private String errorLogFile;
	
	/**
	 * @param data runtimeData
	 * @param options agent options.
	 * @throws IOException 
	 */
	public SavMock(RuntimeData data, AgentOptions options) throws IOException {
		this.data = data;
		this.destFile = new File(options.getDestfile()).getAbsoluteFile();
		this.append = options.getAppend();
		this.errorLogFile = options.getSavLogFile();
		final File folder = destFile.getParentFile();
		if (folder != null) {
			folder.mkdirs();
		}
		// Make sure we can write to the file:
		openFile().close();
	}
	
	@SuppressWarnings("javadoc")
	protected OutputStream openFile() throws IOException {
		final FileOutputStream file = new FileOutputStream(destFile, append);
		// Avoid concurrent writes from different agents running in parallel:
		file.getChannel().lock();
		return file;
	}

	/**
	 * @param data runtimeData
	 * @param options agent options.
	 * @return SavMock instance.
	 * @throws IOException 
	 */
	public static SavMock create(RuntimeData data, AgentOptions options) throws IOException {
		final SavMock savMock = new SavMock(data, options);
		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {

			public void run() {
				try {
					savMock.shutdown();
				} catch (IOException e) {
					savMock.log("exception when shutting down" + e.getMessage() + e.getStackTrace().toString());
				}
			}
		}));
		return savMock;
	}

	/**
	 * @throws IOException
	 */
	protected void shutdown() throws IOException {
		writeData();
	}

	/**
	 * @throws IOException
	 */
	private void writeData() throws IOException {
		log("write file");
		final OutputStream output = openFile();
		for (ByteArrayOutputStream buff : buffs) {
			output.write(buff.toByteArray());
		}
		output.close();
		log("write file ok");
		buffs.clear();
	}
	
	/**
	 * this method is refered in JaCoCoMockJunitRunner. Update accordingly if this name is changed.
	 * collectData(final String sessionId) 
	 * @param sessionId 
	 */
	public void collectData(final String sessionId) {
		log("collectData call " + sessionId);
		final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		try {
			final ExecutionDataWriter writer = new ExecutionDataWriter(buffer) {
				@Override
				public void visitSessionInfo(org.jacoco.core.data.SessionInfo info) {
					if (sessionId != null) {
						SessionInfo newInfo = new SessionInfo(sessionId,
								info.getStartTimeStamp(),
								info.getDumpTimeStamp());
						super.visitSessionInfo(newInfo);
					} else {
						super.visitSessionInfo(info);
					}
				};
				@Override
				public void visitClassExecution(ExecutionData data) {
					super.visitClassExecution(data);
					log(data.getRawProbes().toString());
				}
			};
			data.collect(writer, writer, true);
			
			buffs.add(buffer);
		} catch (final IOException e) {
			log(e.getMessage() + e.getStackTrace());
			// Must not happen with ByteArrayOutputStream
			throw new AssertionError(e);
		}
		log(buffer.toString());
	}
	
	private void log(String log) {
		if (errorLogFile == null) {
			return;
		}
		try {
			logFile = new File(errorLogFile);
			if (!logFile.exists()) {
				logFile.createNewFile();
			}
			FileOutputStream out = new FileOutputStream(logFile, true);
			out.write(log.getBytes());
			out.write("\n".getBytes());
			out.close();
		} catch (Exception e) {
			// do nothing
		}
	}

	/**
	 * @param agentOptions agent options
	 * @param inst instrumentation interface
	 *            name of the added runtime access field
	 * @param data runtimeData
	 * @throws Exception exception when trying to instrument, and look up class.
	 */
	public static void startup(AgentOptions agentOptions, Instrumentation inst, RuntimeData data)
			throws Exception {
		final String mockClassName = agentOptions.getSavmockClassName();
		if (mockClassName != null) {
			final String transformedClassName = mockClassName.replace('.', '/');
			final ClassFileTransformer transformer = new ClassFileTransformer() {
				public byte[] transform(final ClassLoader loader,
						final String name, final Class<?> classBeingRedefined,
						final ProtectionDomain protectionDomain,
						final byte[] source)
						throws IllegalClassFormatException {
					if (name.equals(transformedClassName)) {
						return ModifiedSystemClassRuntime.instrument(source, accessFieldName);
					}
					return null;
				}
			};
			inst.addTransformer(transformer);
			final Class<?> clazz = Class.forName(mockClassName);
			inst.removeTransformer(transformer);
			try {
				final Field field = clazz.getField(accessFieldName);
				field.set(null, SavMock.create(data, agentOptions));
			} catch (final NoSuchFieldException e) {
				throw new RuntimeException(format(
						"Class %s could not be instrumented.", mockClassName), e);
			} 
		}
	}

}
