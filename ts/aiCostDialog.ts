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

class AICostDialog {

    electron = require('electron');

    constructor() {
        this.electron.ipcRenderer.send('get-theme');
        this.electron.ipcRenderer.on('set-theme', (event: Electron.IpcRendererEvent, arg: any) => {
            (document.getElementById('theme') as HTMLLinkElement).href = arg;
        });

        this.electron.ipcRenderer.on('set-cost-estimate', (event: Electron.IpcRendererEvent, arg: any) => {
            this.setCostEstimate(arg);
        });

        document.getElementById('proceed').addEventListener('click', () => {
            this.proceedWithAI();
        });

        document.getElementById('cancel').addEventListener('click', () => {
            this.electron.ipcRenderer.send('close-ai-cost-dialog');
        });

        document.addEventListener('keydown', (event: KeyboardEvent) => {
            if (event.key === 'Escape') {
                this.electron.ipcRenderer.send('close-ai-cost-dialog');
            }
        });

        // Request cost estimate
        this.showStatus('Calculating cost estimate...', 'info');
        this.electron.ipcRenderer.send('estimate-ai-cost');

        setTimeout(() => {
            this.electron.ipcRenderer.send('set-height', {
                window: 'aiCostDialog',
                width: document.body.clientWidth,
                height: document.body.clientHeight
            });
        }, 200);
    }

    setCostEstimate(data: any): void {
        if (data.status === 'Success') {
            this.hideStatus();

            if (!data.needsReview) {
                // Hide cost info and show message
                document.getElementById('costInfo').style.display = 'none';
                document.getElementById('breakdown').style.display = 'none';
                (document.querySelector('.warning') as HTMLElement).style.display = 'none';
                this.showStatus('No uncertain alignments found. All alignments have high confidence!\n\nYou can manually mark segments for AI review by right-clicking on the row number.', 'info');
                (document.getElementById('proceed') as HTMLButtonElement).disabled = true;
                return;
            }

            // Show cost info with actual values
            document.getElementById('costInfo').style.display = 'block';
            document.getElementById('pairsCount').innerText = data.pairsToReview.toString();
            document.getElementById('inputTokens').innerText = data.inputTokens.toLocaleString();
            document.getElementById('outputTokens').innerText = data.outputTokens.toLocaleString();
            document.getElementById('estimatedCost').innerText = data.formattedCost;

            if (data.breakdown) {
                document.getElementById('breakdown').innerText = data.breakdown;
            }
        } else {
            this.showStatus('Error: ' + data.reason, 'error');
            (document.getElementById('proceed') as HTMLButtonElement).disabled = true;
        }
    }

    showStatus(message: string, type: string): void {
        let statusDiv = document.getElementById('statusMessage');
        statusDiv.innerText = message;
        statusDiv.className = 'status ' + type;
        statusDiv.style.whiteSpace = 'pre-line';
    }

    hideStatus(): void {
        let statusDiv = document.getElementById('statusMessage');
        statusDiv.style.display = 'none';
    }

    proceedWithAI(): void {
        (document.getElementById('proceed') as HTMLButtonElement).disabled = true;
        (document.getElementById('cancel') as HTMLButtonElement).disabled = true;
        this.showStatus('Processing with Claude AI... This may take a moment.', 'info');
        this.electron.ipcRenderer.send('proceed-with-ai');
    }
}
