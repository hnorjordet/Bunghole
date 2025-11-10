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

import { dialog } from 'electron';
import { autoUpdater } from 'electron-updater';

export class Updater {

    /**
     * Check for updates automatically
     */
    static checkForUpdates(silent: boolean = false): void {
        // Configure auto-updater
        autoUpdater.autoDownload = false;
        autoUpdater.autoInstallOnAppQuit = true;

        // Don't check in development
        if (process.env.NODE_ENV === 'development') {
            if (!silent) {
                console.log('Update check skipped (development mode)');
            }
            return;
        }

        // Check for updates
        autoUpdater.checkForUpdates().catch(err => {
            if (!silent) {
                console.error('Update check failed:', err);
            }
        });

        // Update available
        autoUpdater.on('update-available', (info) => {
            dialog.showMessageBox({
                type: 'info',
                title: 'Update Available',
                message: `Version ${info.version} is available!`,
                detail: 'Would you like to download it now?\n\nThe update will be installed when you restart the application.',
                buttons: ['Download', 'Later'],
                defaultId: 0,
                cancelId: 1
            }).then((result) => {
                if (result.response === 0) {
                    autoUpdater.downloadUpdate();

                    // Show download progress
                    dialog.showMessageBox({
                        type: 'info',
                        title: 'Downloading Update',
                        message: 'Update is being downloaded in the background.',
                        detail: 'You will be notified when it\'s ready to install.',
                        buttons: ['OK']
                    });
                }
            });
        });

        // Update not available
        autoUpdater.on('update-not-available', () => {
            if (!silent) {
                dialog.showMessageBox({
                    type: 'info',
                    title: 'No Updates',
                    message: 'You are running the latest version!',
                    buttons: ['OK']
                });
            }
        });

        // Update downloaded
        autoUpdater.on('update-downloaded', (info) => {
            dialog.showMessageBox({
                type: 'info',
                title: 'Update Ready',
                message: `Version ${info.version} has been downloaded.`,
                detail: 'The update will be installed when you restart Bunghole.\n\nWould you like to restart now?',
                buttons: ['Restart Now', 'Later'],
                defaultId: 0,
                cancelId: 1
            }).then((result) => {
                if (result.response === 0) {
                    autoUpdater.quitAndInstall(false, true);
                }
            });
        });

        // Download progress (optional - for progress bar)
        autoUpdater.on('download-progress', (progressObj) => {
            let logMessage = `Download speed: ${progressObj.bytesPerSecond}`;
            logMessage = logMessage + ` - Downloaded ${progressObj.percent}%`;
            logMessage = logMessage + ` (${progressObj.transferred}/${progressObj.total})`;
            console.log(logMessage);
        });

        // Error handling
        autoUpdater.on('error', (err) => {
            if (!silent) {
                console.error('Update error:', err);
                dialog.showMessageBox({
                    type: 'error',
                    title: 'Update Error',
                    message: 'Failed to check for updates',
                    detail: err.message || 'Please try again later.',
                    buttons: ['OK']
                });
            }
        });
    }

    /**
     * Check for updates manually (from menu)
     */
    static checkForUpdatesManually(): void {
        Updater.checkForUpdates(false);
    }

    /**
     * Check for updates silently (on app start)
     */
    static checkForUpdatesSilently(): void {
        Updater.checkForUpdates(true);
    }
}
