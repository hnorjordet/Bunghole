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

interface Preferences {
    srcLang: string
    tgtLang: string;
    appLang: string;
    theme: "system" | "light" | "dark" | "highcontrast";
    catalog: string;
    srx: string;
    // NEW: Claude AI settings
    claudeAPIKey?: string;
    enableAI?: boolean;
}