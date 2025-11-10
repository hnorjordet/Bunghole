/*******************************************************************************
 * Copyright (c) 2008 - 2025 Håvard Nørjordet.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-v10.html
 *
 * Contributors:
 *     Håvard Nørjordet - initial API and implementation
 *******************************************************************************/

module bunghole {

	exports com.norjordet.bunghole;
	
	requires javabcp47;
	requires java.base;
	requires java.xml;
	requires transitive openxliff;
	requires transitive jdk.httpserver;
	requires transitive json;
	requires transitive xmljava;
	requires java.logging;
}