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

import { ChildProcessWithoutNullStreams, execFileSync, spawn } from "child_process";
import { app, BrowserWindow, dialog, ipcMain, IpcMainEvent, Menu, MenuItem, MessageBoxReturnValue, nativeTheme, net, Rectangle, session, shell } from "electron";
import { IncomingMessage } from "electron/main";
import { appendFileSync, existsSync, readFileSync, unlinkSync, writeFile, writeFileSync } from "fs";
import { ClientRequest, request } from "http";
import { I18n } from "./i18n";
import { Locations, Point } from "./locations";
import { MessageTypes } from "./messageTypes";
import { Updater } from "./updater";

class Bunghole {

    static path = require('path');

    static mainWindow: BrowserWindow;
    static messagesWindow: BrowserWindow;
    static aboutWindow: BrowserWindow;
    static licensesWindow: BrowserWindow;
    static settingsWindow: BrowserWindow;
    static newFileWindow: BrowserWindow;
    static changeLanguagesWindow: BrowserWindow;
    static replaceTextWindow: BrowserWindow;
    static updatesWindow: BrowserWindow;
    static systemInfoWindow: BrowserWindow;
    static aiCostWindow: BrowserWindow;

    static alignmentStatus: any = { aligning: false, alignError: '', status: '' };
    static loadingStatus: any = { loading: false, loadError: '', status: '' };
    static savingStatus: any = { saving: false, saveError: '', status: '' };

    static currentPreferences: Preferences;
    static currentTheme: string;
    static locations: Locations;
    currentDefaults: Rectangle;

    javapath: string;
    ls: ChildProcessWithoutNullStreams;

    static currentFile: string;
    static srcLang: string;
    static tgtLang: string;
    static saved: boolean = true;
    static shouldQuit: boolean;
    stopping: boolean = false;

    static argFile: string = '';
    static isReady: boolean = false;

    static messageParam: any;
    static latestVersion: string;
    static downloadLink: string;

    static i18n: I18n;
    static appLang: string = 'en';

    constructor(args: string[]) {
        // Initialize javapath based on platform
        this.javapath = Bunghole.path.join(app.getAppPath(), 'bin', process.platform === 'win32' ? 'java.exe' : 'java');

        if (!app.requestSingleInstanceLock()) {
            app.quit();
        } else {
            if (Bunghole.mainWindow) {
                // Someone tried to run a second instance, we should focus our window.
                if (Bunghole.mainWindow.isMinimized()) {
                    Bunghole.mainWindow.restore();
                }
                Bunghole.mainWindow.focus();
            }
        }
        if (process.platform === 'win32' && args.length > 1 && args[1] !== '.') {
            Bunghole.argFile = ''
            for (let i = 1; i < args.length; i++) {
                if (args[i] !== '.') {
                    if (Bunghole.argFile !== '') {
                        Bunghole.argFile = Bunghole.argFile + ' ';
                    }
                    Bunghole.argFile = Bunghole.argFile + args[i];
                }
            }
        }
        this.loadDefaults();
        Bunghole.loadPreferences();
        Bunghole.i18n = new I18n(Bunghole.path.join(app.getAppPath(), 'i18n', 'bunghole_' + Bunghole.appLang + '.json'));

        this.ls = spawn(this.javapath, ['--module-path', 'lib', '-m', 'bunghole/com.norjordet.bunghole.BungholeServer', '-port', '8040', '-lang', Bunghole.appLang], { cwd: app.getAppPath(), windowsHide: true });
        if (!app.isPackaged) {
            this.ls.stdout.on('data', (data: Buffer | string) => {
                console.log(data instanceof Buffer ? data.toString() : data);
            });
            this.ls.stderr.on('data', (data: Buffer | string) => {
                console.error(data instanceof Buffer ? data.toString() : data);
            });
        }
        execFileSync(this.javapath, ['--module-path', 'lib', '-m', 'bunghole/com.norjordet.bunghole.CheckURL', 'http://localhost:8040/'], { cwd: app.getAppPath(), windowsHide: true });

        Bunghole.locations = new Locations(Bunghole.path.join(app.getPath('appData'), app.name, 'locations.json'));
        app.on('ready', () => {
            // Set app name now that app is ready
            app.name = 'Bunghole';
            if (app.setName) {
                app.setName('Bunghole');
            }

            Bunghole.currentFile = '';
            this.createWindow();
            let filePath = Bunghole.path.join(app.getAppPath(), 'html', Bunghole.appLang, 'index.html');
            let fileUrl: URL = new URL('file://' + filePath);
            Bunghole.mainWindow.loadURL(fileUrl.href);
            Bunghole.mainWindow.on('resize', () => {
                this.saveDefaults();
            });
            Bunghole.mainWindow.on('move', () => {
                this.saveDefaults();
            });
            Bunghole.mainWindow.once('ready-to-show', () => {
                Bunghole.isReady = true;
                Bunghole.mainWindow.setBounds(this.currentDefaults);
                Bunghole.setLocation(Bunghole.mainWindow, 'index.html');
                Bunghole.mainWindow.show();
                Bunghole.checkUpdates(true);
                Bunghole.mainLoaded();
                if (process.platform === 'darwin' && app.runningUnderARM64Translation) {
                    dialog.showMessageBoxSync(Bunghole.mainWindow, {
                        type: MessageTypes.warning,
                        message: Bunghole.i18n.getString('Bunghole', 'arm64')
                    });
                }
            });
            Bunghole.mainWindow.on('close', (ev) => {
                if (!Bunghole.saved) {
                    Bunghole.shouldQuit = true;
                    Bunghole.closeFile();
                    ev.preventDefault();
                }
            });
        });

        app.on('before-quit', (ev) => {
            if (!Bunghole.saved) {
                ev.preventDefault();
                Bunghole.shouldQuit = true;
                Bunghole.closeFile();
            }
        });
        app.on('will-quit', (ev) => {
            if (!Bunghole.saved) {
                ev.preventDefault();
                Bunghole.shouldQuit = true;
                Bunghole.closeFile();
            }
        });
        app.on('open-file', (event, filePath) => {
            event.preventDefault();
            if (Bunghole.isReady) {
                Bunghole.openFile(filePath);
            } else {
                Bunghole.argFile = filePath;
            }
        });
        app.on('quit', (ev) => {
            if (!Bunghole.saved) {
                ev.preventDefault();
                Bunghole.shouldQuit = true;
                Bunghole.closeFile();
                return;
            }
            this.stopServer();
        });

        app.on('will-quit', (ev: Event) => {
            if (!Bunghole.saved) {
                ev.preventDefault();
                Bunghole.shouldQuit = true;
                Bunghole.closeFile();
                return;
            }
            this.stopServer();
            app.quit();
        });

        nativeTheme.on('updated', () => {
            Bunghole.loadPreferences();
            Bunghole.setTheme();
        });
        ipcMain.on('get-theme', (event: IpcMainEvent) => {
            event.sender.send('set-theme', Bunghole.currentTheme);
        });
        ipcMain.on('set-height', (event: IpcMainEvent, arg: { window: string, width: number, height: number }) => {
            Bunghole.setHeight(arg);
        });
        ipcMain.on('get-version', (event: IpcMainEvent) => {
            event.sender.send('set-version', app.name + ' ' + app.getVersion());
        });
        ipcMain.on('replace-text', () => {
            Bunghole.replaceText();
        })
        ipcMain.on('replace-request', (event: IpcMainEvent, arg: any) => {
            Bunghole.replace(arg);
        });
        ipcMain.on('browse-srx', (event: IpcMainEvent) => {
            this.browseSRX(event);
        });
        ipcMain.on('browse-catalog', (event: IpcMainEvent) => {
            this.browseCatalog(event);
        });
        ipcMain.on('browse-alignment', (event: IpcMainEvent) => {
            this.browseAlignment(event);
        });
        ipcMain.on('browse-source', (event: IpcMainEvent) => {
            this.browseSource(event);
        });
        ipcMain.on('browse-target', (event: IpcMainEvent) => {
            this.browseTarget(event);
        });
        ipcMain.on('save-preferences', (event: IpcMainEvent, arg: Preferences) => {
            Bunghole.settingsWindow.close();
            Bunghole.currentPreferences = arg;
            Bunghole.savePreferences();
        });
        ipcMain.on('get-preferences', (event: IpcMainEvent) => {
            event.sender.send('set-preferences', Bunghole.currentPreferences);
        });
        ipcMain.on('system-info-clicked', () => {
            Bunghole.showSystemInfo();
        });
        ipcMain.on('close-systemInfo', () => {
            Bunghole.systemInfoWindow.close();
        });
        ipcMain.on('get-system-info', (event: IpcMainEvent) => {
            Bunghole.getSystemInformation(event);
        });
        ipcMain.on('licenses-clicked', () => {
            Bunghole.showLicenses('about');
        });
        ipcMain.on('open-license', (event: IpcMainEvent, arg: any) => {
            Bunghole.openLicense(arg.type);
        })
        ipcMain.on('show-help', () => {
            Bunghole.showHelp();
        });
        ipcMain.on('get-languages', (event: IpcMainEvent) => {
            this.getLanguages(event);
        });
        ipcMain.on('get-appLanguage', (event: IpcMainEvent) => {
            event.sender.send('set-appLanguage', Bunghole.appLang);
        });
        ipcMain.on('get-types', (event: IpcMainEvent) => {
            this.getTypes(event);
        });
        ipcMain.on('get-charsets', (event: IpcMainEvent) => {
            this.getCharsets(event);
        });
        ipcMain.on('new-file', () => {
            Bunghole.newFile();
        });
        ipcMain.on('open-file', () => {
            Bunghole.openFileDialog();
        });
        ipcMain.on('save-file', () => {
            Bunghole.saveFile();
        });
        ipcMain.on('export-tmx', () => {
            Bunghole.exportTMX();
        });
        ipcMain.on('export-csv', () => {
            Bunghole.exportCSV();
        });
        ipcMain.on('export-excel', () => {
            Bunghole.exportExcel();
        });
        ipcMain.on('remove-tags', () => {
            Bunghole.removeTags();
        });
        ipcMain.on('remove-duplicates', () => {
            Bunghole.removeDuplicates();
        });
        ipcMain.on('change-languages', () => {
            Bunghole.changeLanguages();
        });
        ipcMain.on('create-alignment', (event: IpcMainEvent, arg: any) => {
            Bunghole.createAlignment(arg);
        });
        ipcMain.on('get-rows', (event: IpcMainEvent, arg: any) => {
            Bunghole.getRows(arg);
        });
        ipcMain.on('file-languages', (event: IpcMainEvent) => {
            event.sender.send('language-pair', { srcLang: Bunghole.srcLang, tgtLang: Bunghole.tgtLang });
        });
        ipcMain.on('save-languages', (event: IpcMainEvent, arg: any) => {
            Bunghole.setLanguages(arg);
        });
        ipcMain.on('save-data', (event: IpcMainEvent, arg: any) => {
            Bunghole.saveData(arg);
        });
        ipcMain.on('split-data', (event: IpcMainEvent, arg: any) => {
            Bunghole.split(arg);
        });
        ipcMain.on('segment-down', (event: IpcMainEvent, arg: any) => {
            Bunghole.segmentDown(arg);
        });
        ipcMain.on('segment-up', (event: IpcMainEvent, arg: any) => {
            Bunghole.segmentUp(arg);
        });
        ipcMain.on('merge-next', (event: IpcMainEvent, arg: any) => {
            Bunghole.mergeNext(arg);
        });
        ipcMain.on('remove-data', (event: IpcMainEvent, arg: any) => {
            Bunghole.removeData(arg);
        });
        ipcMain.on('close-about', () => {
            Bunghole.aboutWindow.close();
        });
        ipcMain.on('close-change=languages', () => {
            Bunghole.changeLanguagesWindow.close();
        });
        ipcMain.on('close-licenses', () => {
            Bunghole.licensesWindow.close();
        });
        ipcMain.on('close-messages', () => {
            Bunghole.messagesWindow.close();
        });
        ipcMain.on('close-new-file', () => {
            Bunghole.newFileWindow.close();
        });
        // AI Review handlers
        ipcMain.on('ai-review', () => {
            Bunghole.showAICostDialog();
        });
        ipcMain.on('estimate-ai-cost', (event: IpcMainEvent) => {
            Bunghole.estimateAICost(event);
        });
        ipcMain.on('proceed-with-ai', () => {
            Bunghole.proceedWithAI();
        });
        ipcMain.on('close-ai-cost-dialog', () => {
            if (Bunghole.aiCostWindow) {
                Bunghole.aiCostWindow.close();
            }
        });
        // Segment quality management handlers
        ipcMain.on('toggle-manual-mark', (event: IpcMainEvent, arg: any) => {
            Bunghole.toggleManualMark(arg);
        });
        ipcMain.on('move-target-up', (event: IpcMainEvent, arg: any) => {
            Bunghole.moveTargetSegment(arg, 'up');
        });
        ipcMain.on('move-target-down', (event: IpcMainEvent, arg: any) => {
            Bunghole.moveTargetSegment(arg, 'down');
        });
        ipcMain.on('close-preferences', () => {
            Bunghole.settingsWindow.close();
        });
        ipcMain.on('close-search-replace', () => {
            Bunghole.replaceTextWindow.close();
        });
        ipcMain.on('get-versions', (event: IpcMainEvent) => {
            event.sender.send('set-versions', { current: app.getVersion(), latest: Bunghole.latestVersion });
        });
        ipcMain.on('close-updates', () => {
            Bunghole.updatesWindow.close();
        });
        ipcMain.on('release-history', () => {
            Bunghole.showReleaseHistory();
        });
        ipcMain.on('download-latest', () => {
            Bunghole.downloadLatest();
        });
    }

    static destroyWindow(window: BrowserWindow): void {
        if (window) {
            try {
                let parent: BrowserWindow = window.getParentWindow();
                window.hide();
                window.destroy();
                window = undefined;
                if (parent) {
                    parent.focus();
                } else {
                    Bunghole.mainWindow.focus();
                }
            } catch (e) {
                console.log(e);
            }
        }
    }

    stopServer(): void {
        if (!this.stopping) {
            this.stopping = true;
            this.ls.kill(15);
        }
    }

    static setHeight(arg: { window: string, width: number, height: number }): void {
        if ('about' === arg.window) {
            this.aboutWindow.setContentSize(arg.width, arg.height, true);
        }
        if ('licenses' === arg.window) {
            this.licensesWindow.setContentSize(arg.width, arg.height, true);
        }
        if ('preferences' === arg.window) {
            this.settingsWindow.setContentSize(arg.width, arg.height, true);
        }
        if ('replaceText' === arg.window) {
            this.replaceTextWindow.setContentSize(arg.width, arg.height, true);
        }
        if ('systemInfo' === arg.window) {
            this.systemInfoWindow.setContentSize(arg.width, arg.height, true);
        }
        if ('newFile' === arg.window) {
            this.newFileWindow.setContentSize(arg.width, arg.height, true);
        }
        if ('messages' === arg.window) {
            this.messagesWindow.setContentSize(arg.width, arg.height, true);
        }
        if ('languages' === arg.window) {
            this.changeLanguagesWindow.setContentSize(arg.width, arg.height, true);
        }
        if ('updates' === arg.window) {
            this.updatesWindow.setContentSize(arg.width, arg.height, true);
        }
    }

    static mainLoaded(): void {
        if (Bunghole.argFile !== '') {
            setTimeout(() => {
                Bunghole.openFile(Bunghole.argFile);
                Bunghole.argFile = '';
            }, 2000);

        }
    }

    createWindow(): void {
        Bunghole.mainWindow = new BrowserWindow({
            title: app.name,
            width: this.currentDefaults.width,
            height: this.currentDefaults.height,
            minWidth: 900,
            minHeight: 400,
            x: this.currentDefaults.x,
            y: this.currentDefaults.y,
            webPreferences: {
                nodeIntegration: true,
                contextIsolation: false
            },
            show: false,
            icon: Bunghole.path.join(app.getAppPath(), 'icons', 'icon.png')
        });
        let fileMenu: Menu = Menu.buildFromTemplate([
            { label: Bunghole.i18n.getString('menu', 'newAlignment'), accelerator: 'CmdOrCtrl+N', click: () => { Bunghole.newFile(); } },
            { label: Bunghole.i18n.getString('menu', 'openAlignment'), accelerator: 'CmdOrCtrl+O', click: () => { Bunghole.openFileDialog(); } },
            { label: Bunghole.i18n.getString('menu', 'closeAlignment'), accelerator: 'CmdOrCtrl+W', click: () => { Bunghole.closeFile(); } },
            { label: Bunghole.i18n.getString('menu', 'saveAlignment'), accelerator: 'CmdOrCtrl+S', click: () => { Bunghole.saveFile(); } },
            { label: Bunghole.i18n.getString('menu', 'saveAlignmentAs'), accelerator: 'CmdOrCtrl+Shift+S', click: () => { Bunghole.saveFileAs(); } },
            new MenuItem({ type: 'separator' }),
            { label: Bunghole.i18n.getString('menu', 'exportTMX'), click: () => { Bunghole.exportTMX(); } },
            { label: Bunghole.i18n.getString('menu', 'exportExcel'), click: () => { Bunghole.exportExcel(); } },
            { label: Bunghole.i18n.getString('menu', 'exportCSV'), click: () => { Bunghole.exportCSV(); } }
        ]);
        let recentFiles: string[] = Bunghole.loadRecents();
        if (recentFiles.length > 0) {
            fileMenu.append(new MenuItem({ type: 'separator' }));
            let length: number = recentFiles.length;
            for (let i = 0; i < length; i++) {
                let file: string = recentFiles[i];
                fileMenu.append(new MenuItem({ label: file, click: () => { Bunghole.openFile(file) } }));
            }
        }
        let editMenu: Menu = Menu.buildFromTemplate([
            { label: Bunghole.i18n.getString('menu', 'replaceText'), accelerator: 'CmdOrCtrl+F', click: () => { Bunghole.replaceText(); } },
            new MenuItem({ type: 'separator' }),
            { label: Bunghole.i18n.getString('menu', 'confirmEdit'), accelerator: 'Alt+Enter', click: () => { Bunghole.saveEdit(); } },
            { label: Bunghole.i18n.getString('menu', 'cancelEdit'), accelerator: 'Esc', click: () => { Bunghole.cancelEdit(); } },
            new MenuItem({ type: 'separator' }),
            { label: Bunghole.i18n.getString('menu', 'moveDown'), accelerator: 'Alt+CmdOrCtrl+Down', click: () => { Bunghole.moveSegmentDown(); } },
            { label: Bunghole.i18n.getString('menu', 'moveUp'), accelerator: 'Alt+CmdOrCtrl+Up', click: () => { Bunghole.moveSegmentUp(); } },
            { label: Bunghole.i18n.getString('menu', 'splitSegment'), accelerator: 'CmdOrCtrl+L', click: () => { Bunghole.splitSegment(); } },
            { label: Bunghole.i18n.getString('menu', 'mergeNext'), accelerator: 'CmdOrCtrl+M', click: () => { Bunghole.mergeSegment(); } },
            { label: Bunghole.i18n.getString('menu', 'removeSegment'), accelerator: 'CmdOrCtrl+D', click: () => { Bunghole.removeSegment(); } },
            new MenuItem({ type: 'separator' }),
            { label: Bunghole.i18n.getString('menu', 'undo'), accelerator: 'CmdOrCtrl+Z', click: () => { BrowserWindow.getFocusedWindow().webContents.undo(); } },
            { label: Bunghole.i18n.getString('menu', 'cut'), accelerator: 'CmdOrCtrl+X', click: () => { BrowserWindow.getFocusedWindow().webContents.cut(); } },
            { label: Bunghole.i18n.getString('menu', 'copy'), accelerator: 'CmdOrCtrl+C', click: () => { BrowserWindow.getFocusedWindow().webContents.copy(); } },
            { label: Bunghole.i18n.getString('menu', 'paste'), accelerator: 'CmdOrCtrl+V', click: () => { BrowserWindow.getFocusedWindow().webContents.paste(); } },
            { label: Bunghole.i18n.getString('menu', 'selectAll'), accelerator: 'CmdOrCtrl+A', click: () => { BrowserWindow.getFocusedWindow().webContents.selectAll(); } }
        ]);
        let viewMenu: Menu = Menu.buildFromTemplate([
            { label: Bunghole.i18n.getString('menu', 'firstPage'), accelerator: 'CmdOrCtrl+Home', click: () => { Bunghole.firstPage(); } },
            { label: Bunghole.i18n.getString('menu', 'previousPage'), accelerator: 'CmdOrCtrl+PageUp', click: () => { Bunghole.previousPage(); } },
            { label: Bunghole.i18n.getString('menu', 'nextPage'), accelerator: 'CmdOrCtrl+PageDown', click: () => { Bunghole.nextPage(); } },
            { label: Bunghole.i18n.getString('menu', 'lastPage'), accelerator: 'CmdOrCtrl+End', click: () => { Bunghole.lastPage(); } },
            new MenuItem({ type: 'separator' }),
            new MenuItem({ label: Bunghole.i18n.getString('menu', 'toggleFullScreen'), role: 'togglefullscreen' })
        ]);
        if (!app.isPackaged) {
            viewMenu.append(new MenuItem({ label: Bunghole.i18n.getString('menu', 'toggleDevTools'), accelerator: 'F12', role: 'toggleDevTools' }));
        }
        let tasksMenu: Menu = Menu.buildFromTemplate([
            { label: Bunghole.i18n.getString('menu', 'removeAllTags'), click: () => { Bunghole.removeTags(); } },
            { label: Bunghole.i18n.getString('menu', 'removeDuplicates'), click: () => { Bunghole.removeDuplicates(); } },
            { label: Bunghole.i18n.getString('menu', 'changeLanguageCodes'), click: () => { Bunghole.changeLanguages(); } },
        ]);
        let helpMenu: Menu = Menu.buildFromTemplate([
            { label: Bunghole.i18n.getString('menu', 'userGuide'), accelerator: 'F1', click: () => { Bunghole.showHelp(); } },
            new MenuItem({ type: 'separator' }),
            { label: Bunghole.i18n.getString('menu', 'checkUpdates'), click: () => { Bunghole.checkUpdates(false); } },
            { label: Bunghole.i18n.getString('menu', 'viewLicenses'), click: () => { Bunghole.showLicenses('main'); } },
            new MenuItem({ type: 'separator' }),
            { label: Bunghole.i18n.getString('menu', 'releaseHistory'), click: () => { Bunghole.showReleaseHistory(); } },
            { label: Bunghole.i18n.getString('menu', 'supportGroup'), click: () => { Bunghole.showSupportGroup(); } }
        ]);
        let template: MenuItem[] = [
            new MenuItem({ label: Bunghole.i18n.getString('menu', 'fileMenu'), role: 'fileMenu', submenu: fileMenu }),
            new MenuItem({ label: Bunghole.i18n.getString('menu', 'editMenu'), role: 'editMenu', submenu: editMenu }),
            new MenuItem({ label: Bunghole.i18n.getString('menu', 'viewMenu'), role: 'viewMenu', submenu: viewMenu }),
            new MenuItem({ label: Bunghole.i18n.getString('menu', 'tasksMenu'), submenu: tasksMenu }),
            new MenuItem({ label: Bunghole.i18n.getString('menu', 'helpMenu'), role: 'help', submenu: helpMenu })
        ];
        if (process.platform === 'darwin') {
            let appleMenu: Menu = Menu.buildFromTemplate([
                new MenuItem({ label: Bunghole.i18n.getString('menu', 'about'), click: () => { Bunghole.showAbout(); } }),
                new MenuItem({
                    label: Bunghole.i18n.getString('menu', 'preferences'), submenu: [
                        { label: Bunghole.i18n.getString('menu', 'settingsMac'), accelerator: 'Cmd+,', click: () => { Bunghole.showSettings(); } }
                    ]
                }),
                new MenuItem({ type: 'separator' }),
                new MenuItem({
                    label: Bunghole.i18n.getString('menu', 'services'), role: 'services', submenu: [
                        { label: Bunghole.i18n.getString('menu', 'noServices'), enabled: false }
                    ]
                }),
                new MenuItem({ type: 'separator' }),
                new MenuItem({ label: Bunghole.i18n.getString('menu', 'quitMac'), accelerator: 'Cmd+Q', role: 'quit', click: () => { app.quit(); } })
            ]);
            // Explicitly set label to 'Bunghole' for macOS app menu
            template.unshift(new MenuItem({ label: 'Bunghole', submenu: appleMenu }));
        } else {
            let help: MenuItem = template.pop();
            template.push(new MenuItem({
                label: Bunghole.i18n.getString('menu', 'settingsMenu'), submenu: [
                    { label: Bunghole.i18n.getString('menu', 'preferences'), click: () => { Bunghole.showSettings(); } }
                ]
            }));
            template.push(help);
        }
        if (process.platform === 'win32') {
            template[0].submenu.append(new MenuItem({ type: 'separator' }));
            template[0].submenu.append(new MenuItem({ label: Bunghole.i18n.getString('menu', 'quitWindows'), accelerator: 'Alt+F4', role: 'quit', click: () => { app.quit(); } }));
            template[5].submenu.append(new MenuItem({ type: 'separator' }));
            template[5].submenu.append(new MenuItem({ label: Bunghole.i18n.getString('menu', 'about'), click: () => { Bunghole.showAbout(); } }));
        }
        if (process.platform === 'linux') {
            template[0].submenu.append(new MenuItem({ type: 'separator' }));
            template[0].submenu.append(new MenuItem({ label: Bunghole.i18n.getString('menu', 'quitLinux'), accelerator: 'Ctrl+Q', role: 'quit', click: () => { app.quit(); } }));
            template[5].submenu.append(new MenuItem({ type: 'separator' }));
            template[5].submenu.append(new MenuItem({ label: Bunghole.i18n.getString('menu', 'about'), click: () => { Bunghole.showAbout(); } }));
        }
        Menu.setApplicationMenu(Menu.buildFromTemplate(template));
    }

    loadDefaults(): void {
        let defaultsFile: string = Bunghole.path.join(app.getPath('appData'), app.name, 'defaults.json');
        this.currentDefaults = { width: 900, height: 700, x: 0, y: 0 };
        if (existsSync(defaultsFile)) {
            try {
                let data: Buffer = readFileSync(defaultsFile);
                this.currentDefaults = JSON.parse(data.toString());
            } catch (err) {
                console.log(err);
            }
        }
    }

    static loadPreferences(): void {
        let preferencesFile = this.path.join(app.getPath('appData'), app.name, 'preferences.json');
        if (!existsSync(preferencesFile)) {
            this.currentPreferences = {
                srcLang: 'none',
                tgtLang: 'none',
                appLang: 'en',
                theme: 'system',
                catalog: this.path.join(app.getAppPath(), 'catalog', 'catalog.xml'),
                srx: this.path.join(app.getAppPath(), 'srx', 'default.srx')
            };
            this.savePreferences();
        }
        try {
            let data: Buffer = readFileSync(preferencesFile);
            this.currentPreferences = JSON.parse(data.toString());
        } catch (err) {
            console.log(err);
        }

        if (this.currentPreferences.appLang) {
            if (app.isReady() && this.currentPreferences.appLang !== Bunghole.appLang) {
                dialog.showMessageBox({
                    type: 'question',
                    message: Bunghole.i18n.getString('Bunghole', 'languageChanged'),
                    buttons: [Bunghole.i18n.getString('Bunghole', 'restart'), Bunghole.i18n.getString('Bunghole', 'dismiss')],
                    cancelId: 1
                }).then((value: MessageBoxReturnValue) => {
                    if (value.response == 0) {
                        app.relaunch();
                        app.quit();
                    }
                });
            }
        } else {
            this.currentPreferences.appLang = 'en';
        }
        Bunghole.appLang = this.currentPreferences.appLang;
        let light = 'file://' + this.path.join(app.getAppPath(), 'css', 'light.css');
        let dark = 'file://' + this.path.join(app.getAppPath(), 'css', 'dark.css');
        let highcontrast = 'file://' + this.path.join(app.getAppPath(), 'css', 'highcontrast.css');
        if (this.currentPreferences.theme === 'system') {
            if (nativeTheme.shouldUseDarkColors) {
                this.currentTheme = dark;
            } else {
                this.currentTheme = light;
            }
            if (nativeTheme.shouldUseHighContrastColors) {
                this.currentTheme = highcontrast;
            }
        }
        if (this.currentPreferences.theme === 'dark') {
            this.currentTheme = dark;
        }
        if (this.currentPreferences.theme === 'light') {
            this.currentTheme = light;
        }
        if (this.currentPreferences.theme === 'highcontrast') {
            this.currentTheme = highcontrast;
        }
        this.setTheme();
    }

    static savePreferences(): void {
        let preferencesFile = this.path.join(app.getPath('appData'), app.name, 'preferences.json');
        writeFileSync(preferencesFile, JSON.stringify(this.currentPreferences, null, 2));
        this.loadPreferences();
    }

    saveDefaults(): void {
        let defaultsFile: string = Bunghole.path.join(app.getPath('appData'), app.name, 'defaults.json');
        writeFileSync(defaultsFile, JSON.stringify(Bunghole.mainWindow.getBounds()));
    }

    static setTheme(): void {
        let windows: BrowserWindow[] = BrowserWindow.getAllWindows();
        for (let window of windows) {
            window.webContents.send('set-theme', this.currentTheme);
        }
    }

    static checkUpdates(silent: boolean): void {
        // Use new GitHub-based auto-updater
        if (silent) {
            Updater.checkForUpdatesSilently();
        } else {
            Updater.checkForUpdatesManually();
        }
    }

    static downloadLatest(): void {
        let downloadsFolder = app.getPath('downloads');
        let url: URL = new URL(Bunghole.downloadLink);
        let path: string = url.pathname;
        path = path.substring(path.lastIndexOf('/') + 1);
        let file: string = downloadsFolder + (process.platform === 'win32' ? '\\' : '/') + path;
        if (existsSync(file)) {
            unlinkSync(file);
        }
        let req: Electron.ClientRequest = net.request({
            url: Bunghole.downloadLink,
            session: session.defaultSession
        });
        Bunghole.mainWindow.webContents.send('set-status', 'Downloading...');
        Bunghole.updatesWindow.close();
        req.on('response', (response: IncomingMessage) => {
            let fileSize = Number.parseInt(response.headers['content-length'] as string);
            let received: number = 0;
            let downloadedMessage: string = Bunghole.i18n.getString('Bunghole', 'downloaded');
            response.on('data', (chunk: Buffer) => {
                received += chunk.length;
                if (process.platform === 'win32' || process.platform === 'darwin') {
                    Bunghole.mainWindow.setProgressBar(received / fileSize);
                }
                let message: string = Bunghole.i18n.format(downloadedMessage, ['' + Math.trunc(received * 100 / fileSize)]);
                Bunghole.mainWindow.webContents.send('set-status', message);
                appendFileSync(file, chunk);
            });
            response.on('end', () => {
                Bunghole.mainWindow.webContents.send('set-status', '');
                dialog.showMessageBox({
                    type: MessageTypes.info,
                    message: Bunghole.i18n.getString('Bunghole', 'updateDownloaded')
                });
                if (process.platform === 'win32' || process.platform === 'darwin') {
                    Bunghole.mainWindow.setProgressBar(0);
                    shell.openPath(file).then(() => {
                        app.quit();
                    }).catch((reason: string) => {
                        dialog.showErrorBox(Bunghole.i18n.getString('Bunghole', 'error'), reason);
                    });
                }
                if (process.platform === 'linux') {
                    shell.showItemInFolder(file);
                }
            });
            response.on('error', (error: Error) => {
                Bunghole.mainWindow.webContents.send('set-status', '');
                dialog.showErrorBox(Bunghole.i18n.getString('Bunghole', 'error'), error.message);
                if (process.platform === 'win32' || process.platform === 'darwin') {
                    Bunghole.mainWindow.setProgressBar(0);
                }
            });
        });
        req.end();
    }

    static showAbout(): void {
        this.aboutWindow = new BrowserWindow({
            parent: this.mainWindow,
            width: 350,
            height: 490,
            minimizable: false,
            maximizable: false,
            resizable: false,
            show: false,
            icon: this.path.join(app.getAppPath(), 'icons', 'icon.png'),
            webPreferences: {
                nodeIntegration: true,
                contextIsolation: false
            }
        });
        this.aboutWindow.setMenu(null);
        this.aboutWindow.on('closed', () => {
            this.mainWindow.focus();
        });
        let filePath = Bunghole.path.join(app.getAppPath(), 'html', Bunghole.appLang, 'about.html');
        let fileUrl: URL = new URL('file://' + filePath);
        this.aboutWindow.loadURL(fileUrl.href);
        this.aboutWindow.once('ready-to-show', () => {
            this.aboutWindow.show();
        });
        Bunghole.setLocation(this.aboutWindow, 'about.html');
    }

    static showSettings(): void {
        this.settingsWindow = new BrowserWindow({
            parent: this.mainWindow,
            width: 590,
            height: 330,
            minimizable: false,
            maximizable: false,
            resizable: false,
            show: false,
            icon: this.path.join(app.getAppPath(), 'icons', 'icon.png'),
            webPreferences: {
                nodeIntegration: true,
                contextIsolation: false
            }
        });
        this.settingsWindow.setMenu(null);
        this.settingsWindow.on('closed', () => {
            this.mainWindow.focus();
        });
        let filePath = Bunghole.path.join(app.getAppPath(), 'html', Bunghole.appLang, 'preferences.html');
        let fileUrl: URL = new URL('file://' + filePath);
        this.settingsWindow.loadURL(fileUrl.href);
        this.settingsWindow.once('ready-to-show', () => {
            this.settingsWindow.show();
        });
        Bunghole.setLocation(this.settingsWindow, 'preferences.html');
    }

    static showHelp(): void {
        let filePath = this.path.join(app.getAppPath(), 'bunghole_' + Bunghole.appLang + '.pdf');
        let fileUrl: URL = new URL('file://' + filePath);
        shell.openExternal(fileUrl.href);
    }

    static showSystemInfo() {
        this.systemInfoWindow = new BrowserWindow({
            parent: Bunghole.aboutWindow,
            width: 420,
            height: 230,
            minimizable: false,
            maximizable: false,
            resizable: false,
            show: false,
            icon: this.path.join(app.getAppPath(), 'icons', 'icon.png'),
            webPreferences: {
                nodeIntegration: true,
                contextIsolation: false
            }
        });
        this.systemInfoWindow.setMenu(null);
        let filePath = Bunghole.path.join(app.getAppPath(), 'html', Bunghole.appLang, 'systemInfo.html');
        let fileUrl: URL = new URL('file://' + filePath);
        this.systemInfoWindow.loadURL(fileUrl.href);
        this.systemInfoWindow.once('ready-to-show', () => {
            this.systemInfoWindow.show();
        });
        this.systemInfoWindow.on('close', () => {
            Bunghole.aboutWindow.focus();
        });
        Bunghole.setLocation(this.systemInfoWindow, 'systemInfo.html');
    }

    static getSystemInformation(event: IpcMainEvent) {
        this.sendRequest('/systemInfo', {},
            (data: any) => {
                if (data.status === 'Success') {
                    data.electron = process.versions.electron;
                    event.sender.send('set-system-info', data);
                } else {
                    dialog.showMessageBoxSync(Bunghole.mainWindow, { type: MessageTypes.error, message: data.reason });
                }
            },
            (reason: string) => {
                dialog.showMessageBoxSync(Bunghole.mainWindow, { type: MessageTypes.error, message: reason });
            }
        );
    }

    static setLocation(window: BrowserWindow, key: string): void {
        if (Bunghole.locations.hasLocation(key)) {
            let position: Point = Bunghole.locations.getLocation(key);
            window.setPosition(position.x, position.y, true);
        }
        window.addListener('moved', () => {
            let bounds: Rectangle = window.getBounds();
            Bunghole.locations.setLocation(key, bounds.x, bounds.y);
        });
    }

    static showLicenses(from: string): void {
        let parent: BrowserWindow = from === 'about' ? this.aboutWindow : this.mainWindow;
        this.licensesWindow = new BrowserWindow({
            parent: parent,
            width: 420,
            height: 390,
            minimizable: false,
            maximizable: false,
            resizable: false,
            show: false,
            icon: this.path.join(app.getAppPath(), 'icons', 'icon.png'),
            webPreferences: {
                nodeIntegration: true,
                contextIsolation: false
            }
        });
        this.licensesWindow.setMenu(null);
        let filePath = Bunghole.path.join(app.getAppPath(), 'html', Bunghole.appLang, 'licenses.html');
        let fileUrl: URL = new URL('file://' + filePath);
        this.licensesWindow.loadURL(fileUrl.href);
        this.licensesWindow.once('ready-to-show', () => {
            this.licensesWindow.show();
        });
        this.licensesWindow.on('close', () => {
            parent.focus();
        });
        Bunghole.setLocation(this.licensesWindow, 'licenses.html');
    }

    static openLicense(type: string) {
        let licenseFile = '';
        let title = '';
        switch (type) {
            case 'Bunghole':
                licenseFile = 'EclipsePublicLicense1.0.html';
                title = 'Eclipse Public License 1.0';
                break;
            case "electron":
                licenseFile = 'electron.txt';
                title = 'MIT License';
                break;
            case "MapDB":
                licenseFile = 'Apache2.0.html';
                title = 'Apache 2.0';
                break;
            case "Java":
                licenseFile = 'java.html';
                title = 'GPL2 with Classpath Exception';
                break;
            case "OpenXLIFF":
            case "XMLJava":
                licenseFile = 'EclipsePublicLicense1.0.html';
                title = 'Eclipse Public License 1.0';
                break;
            case "JSON":
                licenseFile = 'json.txt';
                title = 'JSON.org License';
                break;
            case "jsoup":
                licenseFile = 'jsoup.txt';
                title = 'MIT License';
                break;
            case "DTDParser":
                licenseFile = 'LGPL2.1.txt';
                title = 'LGPL 2.1';
                break;
            default:
                dialog.showErrorBox(Bunghole.i18n.getString('Bunghole', 'error'), 'Unknow license');
                return;
        }
        let filePath = Bunghole.path.join(app.getAppPath(), 'html', 'licenses', licenseFile);
        let fileUrl: URL = new URL('file://' + filePath);

        let licenseWindow = new BrowserWindow({
            parent: Bunghole.licensesWindow,
            width: 680,
            height: 400,
            show: false,
            title: title,
            icon: this.path.join(app.getAppPath(), 'icons', 'icon.png'),
            webPreferences: {
                nodeIntegration: true,
                contextIsolation: false
            }
        });
        licenseWindow.setMenu(null);
        licenseWindow.loadURL(fileUrl.href);
        licenseWindow.show();
        licenseWindow.on('close', () => {
            Bunghole.licensesWindow.focus();
        });
    }

    static showReleaseHistory(): void {
        shell.openExternal('https://github.com/hnorjordet/Bunghole/releases');
    }

    static showSupportGroup(): void {
        shell.openExternal('https://github.com/hnorjordet/Bunghole/discussions');
    }

    static sendRequest(url: string, json: any, success: Function, error: Function) {
        let postData: string = JSON.stringify(json);
        let options = {
            hostname: '127.0.0.1',
            port: 8040,
            path: url,
            headers: {
                'Content-Type': 'application/json',
                'Content-Length': Buffer.byteLength(postData)
            }
        };
        // Make a request
        let req: ClientRequest = request(options);
        req.on('response',
            (res: any) => {
                res.setEncoding('utf-8');
                if (res.statusCode !== 200) {
                    error('sendRequest() error: ' + res.statusMessage);
                }
                let rawData: string = '';
                res.on('data', (chunk: string) => {
                    rawData += chunk;
                });
                res.on('end', () => {
                    try {
                        success(JSON.parse(rawData));
                    } catch (e) {
                        error(e.message);
                    }
                });
            }
        );
        req.write(postData);
        req.end();
    }

    getLanguages(event: IpcMainEvent): void {
        Bunghole.sendRequest('/getLanguages', {},
            (data: any) => {
                if (data.status === 'Success') {
                    data.srcLang = Bunghole.currentPreferences.srcLang;
                    data.tgtLang = Bunghole.currentPreferences.tgtLang;
                    data.appLang = Bunghole.currentPreferences.appLang;
                    event.sender.send('set-languages', data);
                } else {
                    dialog.showErrorBox(Bunghole.i18n.getString('Bunghole', 'error'), data.reason);
                }
            },
            (reason: string) => {
                dialog.showErrorBox(Bunghole.i18n.getString('Bunghole', 'error'), reason);
            }
        );
    }

    getTypes(event: IpcMainEvent): void {
        Bunghole.sendRequest('/getTypes', {},
            (data: any) => {
                if (data.status === 'Success') {
                    event.sender.send('set-types', data);
                } else {
                    dialog.showErrorBox(Bunghole.i18n.getString('Bunghole', 'error'), data.reason);
                }
            },
            (reason: string) => {
                dialog.showErrorBox(Bunghole.i18n.getString('Bunghole', 'error'), reason);
            }
        );
    }

    getCharsets(event: IpcMainEvent): void {
        Bunghole.sendRequest('/getCharsets', {},
            (data: any) => {
                if (data.status === 'Success') {
                    event.sender.send('set-charsets', data);
                } else {
                    dialog.showErrorBox(Bunghole.i18n.getString('Bunghole', 'error'), data.reason);
                }
            },
            (reason: string) => {
                dialog.showErrorBox(Bunghole.i18n.getString('Bunghole', 'error'), reason);
            }
        );
    }

    static newFile(): void {
        this.newFileWindow = new BrowserWindow({
            parent: this.mainWindow,
            width: 840,
            height: 370,
            minimizable: false,
            maximizable: false,
            resizable: false,
            show: false,
            icon: this.path.join(app.getAppPath(), 'icons', 'icon.png'),
            webPreferences: {
                nodeIntegration: true,
                contextIsolation: false
            }
        });
        this.newFileWindow.on('closed', () => {
            this.mainWindow.focus();
        });
        this.newFileWindow.setMenu(null);
        let filePath = Bunghole.path.join(app.getAppPath(), 'html', Bunghole.appLang, 'newFile.html');
        let fileUrl: URL = new URL('file://' + filePath);
        this.newFileWindow.loadURL(fileUrl.href);
        this.newFileWindow.once('ready-to-show', () => {
            this.newFileWindow.show();
        });
        Bunghole.setLocation(this.newFileWindow, 'newFile.html');
    }

    browseSRX(event: IpcMainEvent): void {
        dialog.showOpenDialog({
            title: 'Default SRX File',
            defaultPath: Bunghole.currentPreferences.srx,
            properties: ['openFile'],
            filters: [
                { name: Bunghole.i18n.getString('Bunghole', 'srxFile'), extensions: ['srx'] },
                { name: Bunghole.i18n.getString('Bunghole', 'anyFile'), extensions: ['*'] }
            ]
        }).then((value) => {
            if (!value.canceled) {
                event.sender.send('set-srx', value.filePaths[0]);
            }
        }).catch((error) => {
            console.log(error);
        });
    }

    browseCatalog(event: IpcMainEvent): void {
        dialog.showOpenDialog({
            title: 'Default Catalog',
            defaultPath: Bunghole.currentPreferences.catalog,
            properties: ['openFile'],
            filters: [
                { name: Bunghole.i18n.getString('Bunghole', 'xmlFile'), extensions: ['xml'] },
                { name: Bunghole.i18n.getString('Bunghole', 'anyFile'), extensions: ['*'] }
            ]
        }).then((value) => {
            if (!value.canceled) {
                event.sender.send('set-catalog', value.filePaths[0]);
            }
        }).catch((error) => {
            console.log(error);
        });
    }

    browseAlignment(event: IpcMainEvent): void {
        dialog.showSaveDialog({
            title: Bunghole.i18n.getString('Bunghole', 'newAlignmentFile'),
            properties: ['createDirectory', 'showOverwriteConfirmation'],
            filters: [
                { name: Bunghole.i18n.getString('Bunghole', 'algnFile'), extensions: ['algn'] },
                { name: Bunghole.i18n.getString('Bunghole', 'anyFile'), extensions: [''] }
            ]
        }).then((value) => {
            if (!value.canceled) {
                event.sender.send('set-alignment', value.filePath);
            }
        }).catch((error) => {
            console.log(error);
        });
    }

    browseSource(event: IpcMainEvent): void {
        let filters: any[] = [
            { name: Bunghole.i18n.getString('FileFormats', 'anyFile'), extensions: ['*'] },
            { name: Bunghole.i18n.getString('FileFormats', 'icml'), extensions: ['icml'] },
            { name: Bunghole.i18n.getString('FileFormats', 'inx'), extensions: ['inx'] },
            { name: Bunghole.i18n.getString('FileFormats', 'idml'), extensions: ['idml'] },
            { name: Bunghole.i18n.getString('FileFormats', 'ditamap'), extensions: ['ditamap', 'dita', 'xml'] },
            { name: Bunghole.i18n.getString('FileFormats', 'html'), extensions: ['html', Bunghole.appLang, 'htm'] },
            { name: Bunghole.i18n.getString('FileFormats', 'javascript'), extensions: ['js'] },
            { name: Bunghole.i18n.getString('FileFormats', 'properties'), extensions: ['properties'] },
            { name: Bunghole.i18n.getString('FileFormats', 'json'), extensions: ['json'] },
            { name: Bunghole.i18n.getString('FileFormats', 'mif'), extensions: ['mif'] },
            { name: Bunghole.i18n.getString('FileFormats', 'office'), extensions: ['docx', 'xlsx', 'pptx'] },
            { name: Bunghole.i18n.getString('FileFormats', 'openOffice1'), extensions: ['sxw', 'sxc', 'sxi', 'sxd'] },
            { name: Bunghole.i18n.getString('FileFormats', 'openOffice2'), extensions: ['odt', 'ods', 'odp', 'odg'] },
            { name: Bunghole.i18n.getString('FileFormats', 'php'), extensions: ['php'] },
            { name: Bunghole.i18n.getString('FileFormats', 'plainText'), extensions: ['txt'] },
            { name: Bunghole.i18n.getString('FileFormats', 'qti'), extensions: ['xml'] },
            { name: Bunghole.i18n.getString('FileFormats', 'qtipackage'), extensions: ['zip'] },
            { name: Bunghole.i18n.getString('FileFormats', 'rc'), extensions: ['rc'] },
            { name: Bunghole.i18n.getString('FileFormats', 'resx'), extensions: ['resx'] },
            { name: Bunghole.i18n.getString('FileFormats', 'srt'), extensions: ['srt'] },
            { name: Bunghole.i18n.getString('FileFormats', 'svg'), extensions: ['svg'] },
            { name: Bunghole.i18n.getString('FileFormats', 'visio'), extensions: ['vsdx'] },
            { name: Bunghole.i18n.getString('FileFormats', 'xml'), extensions: ['xml'] }
        ];
        dialog.showOpenDialog({
            title: Bunghole.i18n.getString('Bunghole', 'sourceFile'),
            properties: ['openFile'],
            filters: filters
        }).then((value) => {
            if (!value.canceled) {
                this.getFileType(event, value.filePaths[0], 'set-source');
            }
        }).catch((error) => {
            console.log(error);
        });
    }

    browseTarget(event: IpcMainEvent): void {
        let filters: any[] = [
            { name: Bunghole.i18n.getString('FileFormats', 'anyFile'), extensions: ['*'] },
            { name: Bunghole.i18n.getString('FileFormats', 'icml'), extensions: ['icml'] },
            { name: Bunghole.i18n.getString('FileFormats', 'inx'), extensions: ['inx'] },
            { name: Bunghole.i18n.getString('FileFormats', 'idml'), extensions: ['idml'] },
            { name: Bunghole.i18n.getString('FileFormats', 'ditamap'), extensions: ['ditamap', 'dita', 'xml'] },
            { name: Bunghole.i18n.getString('FileFormats', 'html'), extensions: ['html', Bunghole.appLang, 'htm'] },
            { name: Bunghole.i18n.getString('FileFormats', 'javascript'), extensions: ['js'] },
            { name: Bunghole.i18n.getString('FileFormats', 'properties'), extensions: ['properties'] },
            { name: Bunghole.i18n.getString('FileFormats', 'json'), extensions: ['json'] },
            { name: Bunghole.i18n.getString('FileFormats', 'mif'), extensions: ['mif'] },
            { name: Bunghole.i18n.getString('FileFormats', 'office'), extensions: ['docx', 'xlsx', 'pptx'] },
            { name: Bunghole.i18n.getString('FileFormats', 'openOffice1'), extensions: ['sxw', 'sxc', 'sxi', 'sxd'] },
            { name: Bunghole.i18n.getString('FileFormats', 'openOffice2'), extensions: ['odt', 'ods', 'odp', 'odg'] },
            { name: Bunghole.i18n.getString('FileFormats', 'php'), extensions: ['php'] },
            { name: Bunghole.i18n.getString('FileFormats', 'plainText'), extensions: ['txt'] },
            { name: Bunghole.i18n.getString('FileFormats', 'qti'), extensions: ['xml'] },
            { name: Bunghole.i18n.getString('FileFormats', 'qtipackage'), extensions: ['zip'] },
            { name: Bunghole.i18n.getString('FileFormats', 'rc'), extensions: ['rc'] },
            { name: Bunghole.i18n.getString('FileFormats', 'resx'), extensions: ['resx'] },
            { name: Bunghole.i18n.getString('FileFormats', 'srt'), extensions: ['srt'] },
            { name: Bunghole.i18n.getString('FileFormats', 'svg'), extensions: ['svg'] },
            { name: Bunghole.i18n.getString('FileFormats', 'visio'), extensions: ['vsdx'] },
            { name: Bunghole.i18n.getString('FileFormats', 'xml'), extensions: ['xml'] }
        ];
        dialog.showOpenDialog({
            title: Bunghole.i18n.getString('Bunghole', 'targetFile'),
            properties: ['openFile'],
            filters: filters
        }).then((value) => {
            if (!value.canceled) {
                this.getFileType(event, value.filePaths[0], 'set-target');
            }
        }).catch((error) => {
            console.log(error);
        });
    }

    getFileType(event: IpcMainEvent, file: string, arg: string): void {
        Bunghole.sendRequest('/getFileType', { file: file },
            (data: any) => {
                event.sender.send(arg, data);
            },
            (reason: string) => {
                dialog.showErrorBox(Bunghole.i18n.getString('Bunghole', 'error'), reason);
            }
        );
    }

    static createAlignment(params: any): void {
        this.newFileWindow.close();
        Bunghole.mainWindow.webContents.send('start-waiting');
        Bunghole.mainWindow.webContents.send('set-status', Bunghole.i18n.getString('Bunghole', 'preparingFiles'));
        params.catalog = Bunghole.currentPreferences.catalog;
        params.srx = Bunghole.currentPreferences.srx;
        params.xmlfilter = this.path.join(app.getAppPath(), 'xmlfilter');
        this.sendRequest('/alignFiles', params,
            (data: any) => {
                if (data.status === 'Success') {
                    Bunghole.alignmentStatus.aligning = true;
                    Bunghole.alignmentStatus.status = Bunghole.i18n.getString('Bunghole', 'preparingFiles');
                    let intervalObject = setInterval(() => {
                        if (Bunghole.alignmentStatus.aligning) {
                            Bunghole.mainWindow.webContents.send('set-status', Bunghole.alignmentStatus.status);
                        } else {
                            clearInterval(intervalObject);
                            Bunghole.mainWindow.webContents.send('end-waiting');
                            Bunghole.mainWindow.webContents.send('set-status', '');
                            if (Bunghole.alignmentStatus.alignError !== '') {
                                dialog.showErrorBox(Bunghole.i18n.getString('Bunghole', 'error'), Bunghole.alignmentStatus.alignError);
                            } else {
                                Bunghole.openFile(params.alignmentFile);
                                let save: boolean = false;
                                if (Bunghole.currentPreferences.srcLang === 'none') {
                                    Bunghole.currentPreferences.srcLang = params.srcLang;
                                    save = true;
                                }
                                if (Bunghole.currentPreferences.tgtLang === 'none') {
                                    Bunghole.currentPreferences.tgtLang = params.tgtLang;
                                    save = true;
                                }
                                if (save) {
                                    Bunghole.savePreferences();
                                }
                            }
                        }
                        Bunghole.getAlignmentStatus();
                    }, 500);
                } else {
                    Bunghole.mainWindow.webContents.send('end-waiting');
                    Bunghole.mainWindow.webContents.send('set-status', '');
                    dialog.showErrorBox(Bunghole.i18n.getString('Bunghole', 'error'), data.reason);
                }
            },
            (reason: string) => {
                Bunghole.mainWindow.webContents.send('end-waiting');
                Bunghole.mainWindow.webContents.send('set-status', '');
                dialog.showErrorBox(Bunghole.i18n.getString('Bunghole', 'error'), reason);
            }
        );
    }

    static getAlignmentStatus(): void {
        this.sendRequest('/alignmentStatus', {},
            (data: any) => {
                Bunghole.alignmentStatus = data;
            },
            (reason: string) => {
                Bunghole.alignmentStatus.aligning = false;
                Bunghole.alignmentStatus.alignError = reason;
                Bunghole.alignmentStatus.status = '';
            }
        );
    }

    static openFileDialog(): void {
        dialog.showOpenDialog({
            title: Bunghole.i18n.getString('Bunghole', 'alignmentFile'),
            properties: ['openFile'],
            filters: [
                { name: Bunghole.i18n.getString('Bunghole', 'algnFile'), extensions: ['algn'] },
                { name: Bunghole.i18n.getString('Bunghole', 'anyFile'), extensions: ['*'] }
            ]
        }).then((value) => {
            if (!value.canceled) {
                Bunghole.openFile(value.filePaths[0]);
            }
        }).catch((error) => {
            console.log(error);
        });
    }

    static openFile(file: string): void {
        if (this.currentFile !== '') {
            this.closeFile();
        }
        Bunghole.mainWindow.webContents.send('start-waiting');
        Bunghole.mainWindow.webContents.send('set-status', Bunghole.i18n.getString('Bunghole', 'loadingFile'));
        this.sendRequest('/openFile', { file: file },
            (data: any) => {
                if (data.status === 'Success') {
                    Bunghole.loadingStatus.loading = true;
                    Bunghole.loadingStatus.status = Bunghole.i18n.getString('Bunghole', 'loadingFile');
                    let intervalObject = setInterval(() => {
                        if (Bunghole.loadingStatus.loading) {
                            // keep waiting
                        } else {
                            clearInterval(intervalObject);
                            Bunghole.mainWindow.webContents.send('end-waiting');
                            Bunghole.mainWindow.webContents.send('set-status', '');
                            if (Bunghole.loadingStatus.loadError !== '') {
                                dialog.showErrorBox(Bunghole.i18n.getString('Bunghole', 'error'), Bunghole.loadingStatus.loadError);
                            } else {
                                Bunghole.currentFile = file;
                                Bunghole.saved = true;
                                Bunghole.getFileInfo();
                                Bunghole.saveRecent(Bunghole.currentFile);
                            }
                        }
                        Bunghole.getLoadingStatus();
                    }, 500);
                } else {
                    Bunghole.mainWindow.webContents.send('end-waiting');
                    Bunghole.mainWindow.webContents.send('set-status', '');
                    dialog.showErrorBox(Bunghole.i18n.getString('Bunghole', 'error'), data.reason);
                }
            },
            (reason: string) => {
                Bunghole.mainWindow.webContents.send('end-waiting');
                Bunghole.mainWindow.webContents.send('set-status', '');
                dialog.showErrorBox(Bunghole.i18n.getString('Bunghole', 'error'), reason);
            }
        );
    }

    static getLoadingStatus(): void {
        this.sendRequest('/loadingStatus', {},
            (data: any) => {
                Bunghole.loadingStatus = data;
            },
            (reason: string) => {
                Bunghole.loadingStatus.loading = false;
                Bunghole.loadingStatus.LoadError = reason;
                Bunghole.loadingStatus.status = '';
            }
        );
    }

    static getFileInfo(): void {
        this.sendRequest('/getFileInfo', {},
            (data: any) => {
                Bunghole.srcLang = data.srcLang.code;
                Bunghole.tgtLang = data.tgtLang.code;
                Bunghole.mainWindow.webContents.send('file-info', data);
            },
            (reason: string) => {
                dialog.showErrorBox(Bunghole.i18n.getString('Bunghole', 'error'), reason);
            }
        );
    }

    static getRows(params: any): void {
        this.sendRequest('/getRows', params,
            (data: any) => {
                Bunghole.mainWindow.webContents.send('set-rows', data);
            },
            (reason: string) => {
                dialog.showErrorBox(Bunghole.i18n.getString('Bunghole', 'error'), reason);
            }
        );
    }

    static firstPage(): void {
        Bunghole.mainWindow.webContents.send('first-page');
    }

    static previousPage(): void {
        Bunghole.mainWindow.webContents.send('previous-page');
    }

    static nextPage(): void {
        Bunghole.mainWindow.webContents.send('next-page');
    }

    static lastPage(): void {
        Bunghole.mainWindow.webContents.send('last-page');
    }

    static closeFile(): void {
        if (this.currentFile === '') {
            return;
        }
        if (!this.saved) {
            let clicked: number = dialog.showMessageBoxSync(Bunghole.mainWindow, {
                type: 'question',
                title: Bunghole.i18n.getString('Bunghole', 'saveChanges'),
                message: Bunghole.i18n.getString('Bunghole', 'unsavedChanges'),
                buttons: [
                    Bunghole.i18n.getString('Bunghole', 'dontSave'),
                    Bunghole.i18n.getString('Bunghole', 'cancel'),
                    Bunghole.i18n.getString('Bunghole', 'save')],
                defaultId: 2
            });
            if (clicked === 0) {
                Bunghole.saved = true;
                Bunghole.mainWindow.setDocumentEdited(false);
                if (this.shouldQuit) {
                    app.quit();
                }
            }
            if (clicked === 1) {
                this.shouldQuit = false;
                return;
            }
            if (clicked === 2) {
                this.saveFile();
                return;
            }
        }
        if (!this.shouldQuit) {
            this.sendRequest('/closeFile', {},
                (data: any) => {
                    Bunghole.saved = true;
                    Bunghole.mainWindow.setDocumentEdited(false);
                    Bunghole.mainWindow.webContents.send('clear-file');
                },
                (reason: string) => {
                    dialog.showErrorBox(Bunghole.i18n.getString('Bunghole', 'error'), reason);
                }
            );
        }
    }

    static saveFile(): void {
        if (this.currentFile === '') {
            return;
        }
        Bunghole.mainWindow.webContents.send('start-waiting');
        Bunghole.mainWindow.webContents.send('set-status', Bunghole.i18n.getString('Bunghole', 'savingFile'));
        this.sendRequest("/saveFile", {},
            (data: any) => {
                if (data.status === 'Success') {
                    Bunghole.savingStatus.saving = true;
                    Bunghole.savingStatus.status = Bunghole.i18n.getString('Bunghole', 'savingFile');
                    let intervalObject = setInterval(() => {
                        if (Bunghole.savingStatus.saving) {
                            // keep waiting
                        } else {
                            clearInterval(intervalObject);
                            Bunghole.mainWindow.webContents.send('end-waiting');
                            Bunghole.mainWindow.webContents.send('set-status', '');
                            if (Bunghole.savingStatus.saveError !== '') {
                                dialog.showErrorBox(Bunghole.i18n.getString('Bunghole', 'error'), Bunghole.savingStatus.saveError);
                            } else {
                                Bunghole.saved = true;
                                Bunghole.mainWindow.setDocumentEdited(false);
                                if (Bunghole.shouldQuit) {
                                    app.quit();
                                }
                            }
                        }
                        Bunghole.getSavingStatus();
                    }, 200);
                } else {
                    Bunghole.mainWindow.webContents.send('end-waiting');
                    Bunghole.mainWindow.webContents.send('set-status', '');
                    dialog.showErrorBox(Bunghole.i18n.getString('Bunghole', 'error'), data.reason);
                }

            },
            (reason: string) => {
                dialog.showErrorBox(Bunghole.i18n.getString('Bunghole', 'error'), reason);
            }
        );
    }

    static getSavingStatus(): void {
        this.sendRequest('/savingStatus', {},
            (data: any) => {
                Bunghole.savingStatus = data;
            },
            (reason: string) => {
                Bunghole.savingStatus.saving = false;
                Bunghole.savingStatus.saveError = reason;
                Bunghole.savingStatus.status = '';
            }
        );
    }

    static saveFileAs(): void {
        if (this.currentFile === '') {
            return;
        }
        dialog.showSaveDialog(this.mainWindow, {
            title: Bunghole.i18n.getString('Bunghole', 'saveFileAs'),
            properties: ['createDirectory', 'showOverwriteConfirmation'],
            filters: [
                { name: Bunghole.i18n.getString('Bunghole', 'algnFile'), extensions: ['algn'] },
                { name: Bunghole.i18n.getString('Bunghole', 'anyFile'), extensions: ['*'] }
            ],
            defaultPath: Bunghole.currentFile
        }).then((value) => {
            if (!value.canceled) {
                this.currentFile = value.filePath;
                this.sendRequest('/renameFile', { file: this.currentFile },
                    (data: any) => {
                        Bunghole.mainWindow.webContents.send('file-renamed', Bunghole.currentFile);
                        Bunghole.saveFile();
                        Bunghole.saveRecent(Bunghole.currentFile);
                    },
                    (reason: string) => {
                        dialog.showErrorBox(Bunghole.i18n.getString('Bunghole', 'error'), reason);
                    }
                );
            }
        }).catch((error) => {
            console.log(error);
        });
    }

    static exportTMX(): void {
        if (this.currentFile === '') {
            return;
        }
        dialog.showSaveDialog(this.mainWindow, {
            title: Bunghole.i18n.getString('Bunghole', 'exportTMX'),
            properties: ['createDirectory', 'showOverwriteConfirmation'],
            filters: [
                { name: Bunghole.i18n.getString('Bunghole', 'tmxFile'), extensions: ['tmx'] },
                { name: Bunghole.i18n.getString('Bunghole', 'anyFile'), extensions: ['*'] }
            ]
        }).then((value) => {
            if (!value.canceled) {
                this.sendRequest("/exportTMX", { file: value.filePath },
                    (data: any) => {
                        dialog.showMessageBoxSync(Bunghole.mainWindow, { type: MessageTypes.info, message: Bunghole.i18n.getString('Bunghole', 'fileExported') });
                    },
                    (reason: string) => {
                        dialog.showErrorBox(Bunghole.i18n.getString('Bunghole', 'error'), reason);
                    }
                );
            }
        }).catch((error) => {
            console.log(error);
        });
    }

    static exportCSV(): void {
        if (this.currentFile === '') {
            return;
        }
        dialog.showSaveDialog(this.mainWindow, {
            title: Bunghole.i18n.getString('Bunghole', 'exportTabDelimited'),
            properties: ['createDirectory', 'showOverwriteConfirmation'],
            filters: [
                { name: Bunghole.i18n.getString('Bunghole', 'tsvFile'), extensions: ['tsv'] },
                { name: Bunghole.i18n.getString('Bunghole', 'csvFile'), extensions: ['csv'] },
                { name: Bunghole.i18n.getString('Bunghole', 'textFile'), extensions: ['txt'] },
                { name: Bunghole.i18n.getString('Bunghole', 'anyFile'), extensions: ['*'] }
            ]
        }).then((value) => {
            if (!value.canceled) {
                this.sendRequest("/exportCSV", { file: value.filePath },
                    (data: any) => {
                        dialog.showMessageBoxSync(Bunghole.mainWindow, { type: MessageTypes.info, message: Bunghole.i18n.getString('Bunghole', 'fileExported') });
                    },
                    (reason: string) => {
                        dialog.showErrorBox(Bunghole.i18n.getString('Bunghole', 'error'), reason);
                    }
                );
            }
        }).catch((error) => {
            console.log(error);
        });
    }

    static exportExcel(): void {
        if (this.currentFile === '') {
            return;
        }
        dialog.showSaveDialog(this.mainWindow, {
            title: Bunghole.i18n.getString('Bunghole', 'exportExcel'),
            properties: ['createDirectory', 'showOverwriteConfirmation'],
            filters: [
                { name: Bunghole.i18n.getString('Bunghole', 'excelFile'), extensions: ['xlsx'] },
                { name: Bunghole.i18n.getString('Bunghole', 'anyFile'), extensions: ['*'] }
            ]
        }).then((value) => {
            if (!value.canceled) {
                this.sendRequest("/exportExcel", { file: value.filePath },
                    (data: any) => {
                        if (data.status === 'Success') {
                            dialog.showMessageBoxSync(Bunghole.mainWindow, { type: MessageTypes.info, message: Bunghole.i18n.getString('Bunghole', 'fileExported') });
                        } else {
                            dialog.showErrorBox(Bunghole.i18n.getString('Bunghole', 'error'), data.reason);
                        }
                    },
                    (reason: string) => {
                        dialog.showErrorBox(Bunghole.i18n.getString('Bunghole', 'error'), reason);
                    }
                );
            }
        }).catch((error) => {
            console.log(error);
        });
    }

    static removeTags(): void {
        if (this.currentFile === '') {
            return;
        }
        Bunghole.mainWindow.webContents.send('start-waiting');
        this.sendRequest("/removeTags", {},
            (data: any) => {
                Bunghole.saved = false;
                Bunghole.mainWindow.setDocumentEdited(true);
                Bunghole.mainWindow.webContents.send('end-waiting');
                Bunghole.mainWindow.webContents.send('refresh-page');
            },
            (reason: string) => {
                dialog.showErrorBox(Bunghole.i18n.getString('Bunghole', 'error'), reason);
            }
        );
    }

    static removeDuplicates(): void {
        if (this.currentFile === '') {
            return;
        }
        Bunghole.mainWindow.webContents.send('start-waiting');
        this.sendRequest("/removeDuplicates", {},
            (data: any) => {
                Bunghole.saved = false;
                Bunghole.mainWindow.setDocumentEdited(true);
                Bunghole.mainWindow.webContents.send('end-waiting');
                Bunghole.getFileInfo();
            },
            (reason: string) => {
                dialog.showErrorBox(Bunghole.i18n.getString('Bunghole', 'error'), reason);
            }
        );
    }

    static changeLanguages(): void {
        if (this.currentFile === '') {
            return;
        }
        this.changeLanguagesWindow = new BrowserWindow({
            parent: this.mainWindow,
            width: 540,
            height: 190,
            minimizable: false,
            maximizable: false,
            resizable: false,
            show: false,
            icon: this.path.join(app.getAppPath(), 'icons', 'icon.png'),
            webPreferences: {
                nodeIntegration: true,
                contextIsolation: false
            }
        });
        this.changeLanguagesWindow.setMenu(null);
        this.changeLanguagesWindow.on('closed', () => {
            this.mainWindow.focus();
        });
        let filePath = Bunghole.path.join(app.getAppPath(), 'html', Bunghole.appLang, 'changeLanguages.html');
        let fileUrl: URL = new URL('file://' + filePath);
        this.changeLanguagesWindow.loadURL(fileUrl.href);
        this.changeLanguagesWindow.once('ready-to-show', () => {
            this.changeLanguagesWindow.show();
        });
        Bunghole.setLocation(this.changeLanguagesWindow, 'changeLanguages.html');
    }

    static replaceText(): void {
        if (this.currentFile === '') {
            return;
        }
        this.replaceTextWindow = new BrowserWindow({
            parent: this.mainWindow,
            width: 440,
            height: 270,
            minimizable: false,
            maximizable: false,
            resizable: false,
            show: false,
            icon: this.path.join(app.getAppPath(), 'icons', 'icon.png'),
            webPreferences: {
                nodeIntegration: true,
                contextIsolation: false
            }
        });
        this.replaceTextWindow.setMenu(null);
        this.replaceTextWindow.on('closed', () => {
            this.mainWindow.focus();
        });
        let filePath = Bunghole.path.join(app.getAppPath(), 'html', Bunghole.appLang, 'searchReplace.html');
        let fileUrl: URL = new URL('file://' + filePath);
        this.replaceTextWindow.loadURL(fileUrl.href);
        this.replaceTextWindow.once('ready-to-show', () => {
            this.replaceTextWindow.show();
        });
        Bunghole.setLocation(this.replaceTextWindow, 'searchReplace.html');
    }

    static replace(data: any): void {
        this.sendRequest('/replaceText', data,
            (data: any) => {
                Bunghole.saved = false;
                Bunghole.mainWindow.setDocumentEdited(true);
                Bunghole.mainWindow.webContents.send('refresh-page');
            },
            (reason: string) => {
                dialog.showErrorBox(Bunghole.i18n.getString('Bunghole', 'error'), reason);
            }
        );
    }

    static saveEdit(): void {
        if (this.currentFile === '') {
            return;
        }
        Bunghole.mainWindow.webContents.send('save-edit');
    }

    static cancelEdit(): void {
        if (this.currentFile === '') {
            return;
        }
        Bunghole.mainWindow.webContents.send('cancel-edit');
    }

    static moveSegmentDown(): void {
        if (this.currentFile === '') {
            return;
        }
        Bunghole.mainWindow.webContents.send('move-down');
    }

    static moveSegmentUp(): void {
        if (this.currentFile === '') {
            return;
        }
        Bunghole.mainWindow.webContents.send('move-up');
    }

    static splitSegment(): void {
        if (this.currentFile === '') {
            return;
        }
        Bunghole.mainWindow.webContents.send('split-segment');
    }

    static mergeSegment(): void {
        if (this.currentFile === '') {
            return;
        }
        Bunghole.mainWindow.webContents.send('merge-segment');
    }

    static removeSegment(): void {
        if (this.currentFile === '') {
            return;
        }
        Bunghole.mainWindow.webContents.send('remove-segment');
    }

    static loadRecents(): string[] {
        let recentsFile = this.path.join(app.getPath('appData'), app.name, 'recent.json');
        if (!existsSync(recentsFile)) {
            return [];
        }
        try {
            let data: Buffer = readFileSync(recentsFile);
            let recents = JSON.parse(data.toString());
            let list: string[] = [];
            let length = recents.files.length;
            for (let i = 0; i < length; i++) {
                let file: string = recents.files[i];
                if (existsSync(file)) {
                    list.push(file);
                }
            }
            return list;
        } catch (err) {
            console.log(err);
            return [];
        }
    }

    static saveRecent(file: string) {
        let recentsFile = this.path.join(app.getPath('appData'), app.name, 'recent.json');
        let files: string[] = this.loadRecents();
        files = files.filter((f: string) => {
            return f !== file;
        });
        files.unshift(file);
        if (files.length > 5) {
            files = files.slice(0, 5);
        }
        let jsonData: any = { files: files };
        writeFile(recentsFile, JSON.stringify(jsonData), (error) => {
            if (error) {
                dialog.showMessageBoxSync(Bunghole.mainWindow, { type: MessageTypes.error, message: error.message });
            }
        });
    }

    static setLanguages(langs: any): void {
        this.changeLanguagesWindow.close();
        Bunghole.mainWindow.focus();
        this.sendRequest('/setLanguages', langs,
            (data: any) => {
                Bunghole.saved = false;
                Bunghole.mainWindow.setDocumentEdited(true);
                Bunghole.getFileInfo();
            },
            (reason: string) => {
                dialog.showErrorBox(Bunghole.i18n.getString('Bunghole', 'error'), reason);
            }
        );
    }

    static saveData(data: any): void {
        this.sendRequest('/saveData', data,
            (data: any) => {
                Bunghole.saved = false;
                Bunghole.mainWindow.setDocumentEdited(true);
                Bunghole.mainWindow.webContents.send('refresh-page');
            },
            (reason: string) => {
                dialog.showErrorBox(Bunghole.i18n.getString('Bunghole', 'error'), reason);
            }
        );
    }

    static split(data: any): void {
        this.sendRequest('/splitSegment', data,
            (data: any) => {
                Bunghole.saved = false;
                Bunghole.mainWindow.setDocumentEdited(true);
                Bunghole.mainWindow.webContents.send('refresh-page');
            },
            (reason: string) => {
                dialog.showErrorBox(Bunghole.i18n.getString('Bunghole', 'error'), reason);
            }
        );
    }

    static segmentDown(data: any): void {
        this.sendRequest('/segmentDown', data,
            (data: any) => {
                Bunghole.saved = false;
                Bunghole.mainWindow.setDocumentEdited(true);
                Bunghole.mainWindow.webContents.send('refresh-page');
            },
            (reason: string) => {
                dialog.showErrorBox(Bunghole.i18n.getString('Bunghole', 'error'), reason);
            }
        );
    }

    static segmentUp(data: any): void {
        this.sendRequest('/segmentUp', data,
            (data: any) => {
                Bunghole.saved = false;
                Bunghole.mainWindow.setDocumentEdited(true);
                Bunghole.mainWindow.webContents.send('refresh-page');
            },
            (reason: string) => {
                dialog.showErrorBox(Bunghole.i18n.getString('Bunghole', 'error'), reason);
            }
        );
    }

    static mergeNext(data: any): void {
        this.sendRequest('/mergeNext', data,
            (data: any) => {
                Bunghole.saved = false;
                Bunghole.mainWindow.setDocumentEdited(true);
                Bunghole.mainWindow.webContents.send('refresh-page');
            },
            (reason: string) => {
                dialog.showErrorBox(Bunghole.i18n.getString('Bunghole', 'error'), reason);
            }
        );
    }

    static removeData(data: any): void {
        this.sendRequest('/removeSegment', data,
            (data: any) => {
                Bunghole.saved = false;
                Bunghole.mainWindow.setDocumentEdited(true);
                Bunghole.mainWindow.webContents.send('refresh-page');
            },
            (reason: string) => {
                dialog.showErrorBox(Bunghole.i18n.getString('Bunghole', 'error'), reason);
            }
        );
    }

    // ==================== AI REVIEW METHODS ====================

    static showAICostDialog(): void {
        if (!Bunghole.currentFile) {
            dialog.showErrorBox('No File Open', 'Please open an alignment file first.');
            return;
        }

        // Check if API key is configured
        if (!Bunghole.currentPreferences.claudeAPIKey || Bunghole.currentPreferences.claudeAPIKey.trim() === '') {
            dialog.showMessageBox(Bunghole.mainWindow, {
                type: 'warning',
                title: 'Claude API Key Required',
                message: 'Claude API key is not configured.',
                detail: 'Please set your Claude API key in Preferences before using AI Review.',
                buttons: ['Open Preferences', 'Cancel']
            }).then((result) => {
                if (result.response === 0) {
                    Bunghole.showSettings();
                }
            });
            return;
        }

        // Set API key in Java backend
        this.sendRequest('/setClaudeAPIKey', { apiKey: Bunghole.currentPreferences.claudeAPIKey },
            (data: any) => {
                if (data.status === 'Success') {
                    // Open cost dialog
                    this.aiCostWindow = new BrowserWindow({
                        parent: this.mainWindow,
                        width: 500,
                        height: 450,
                        minimizable: false,
                        maximizable: false,
                        resizable: false,
                        show: false,
                        icon: this.path.join(app.getAppPath(), 'icons', 'icon.png'),
                        webPreferences: {
                            nodeIntegration: true,
                            contextIsolation: false
                        }
                    });
                    this.aiCostWindow.setMenu(null);
                    let filePath = Bunghole.path.join(app.getAppPath(), 'html', Bunghole.appLang, 'aiCostDialog.html');
                    let fileUrl: URL = new URL('file://' + filePath);
                    this.aiCostWindow.loadURL(fileUrl.href);
                    this.aiCostWindow.once('ready-to-show', () => {
                        this.aiCostWindow.show();
                    });
                    this.aiCostWindow.on('close', () => {
                        this.mainWindow.focus();
                    });
                    Bunghole.setLocation(this.aiCostWindow, 'aiCostDialog.html');
                } else {
                    dialog.showErrorBox('Error', 'Failed to set API key: ' + data.reason);
                }
            },
            (reason: string) => {
                dialog.showErrorBox('Error', 'Failed to set API key: ' + reason);
            }
        );
    }

    static estimateAICost(event: IpcMainEvent): void {
        this.sendRequest('/estimateAICost', {},
            (data: any) => {
                event.sender.send('set-cost-estimate', data);
            },
            (reason: string) => {
                event.sender.send('set-cost-estimate', {
                    status: 'Error',
                    reason: reason
                });
            }
        );
    }

    static proceedWithAI(): void {
        Bunghole.mainWindow.webContents.send('start-waiting');
        Bunghole.mainWindow.webContents.send('set-status', 'AI is reviewing alignments...');

        this.sendRequest('/improveWithAI', {},
            (data: any) => {
                Bunghole.mainWindow.webContents.send('end-waiting');
                Bunghole.mainWindow.webContents.send('set-status', '');

                if (this.aiCostWindow) {
                    this.aiCostWindow.close();
                }

                if (data.status === 'Success') {
                    let message = `AI Review Complete!\n\n` +
                        `Improved pairs: ${data.improved}\n` +
                        `Remaining uncertain: ${data.remainingUncertain}\n` +
                        `Overall confidence: ${(data.overallConfidence * 100).toFixed(1)}%`;

                    dialog.showMessageBox(Bunghole.mainWindow, {
                        type: 'info',
                        title: 'AI Review Complete',
                        message: message,
                        buttons: ['OK']
                    });

                    // Refresh the page to show updated alignments
                    Bunghole.mainWindow.webContents.send('refresh-page');
                    Bunghole.saved = false;
                    Bunghole.mainWindow.setDocumentEdited(true);
                } else {
                    dialog.showErrorBox('AI Review Failed', data.reason || 'Unknown error occurred');
                }
            },
            (reason: string) => {
                Bunghole.mainWindow.webContents.send('end-waiting');
                Bunghole.mainWindow.webContents.send('set-status', '');

                if (this.aiCostWindow) {
                    this.aiCostWindow.close();
                }

                dialog.showErrorBox('AI Review Failed', reason);
            }
        );
    }

    static toggleManualMark(arg: any): void {
        this.sendRequest('/toggleManualMark', { segmentId: arg.segmentId },
            (data: any) => {
                if (data.status === 'Success') {
                    // Refresh the current page to show updated badge
                    Bunghole.mainWindow.webContents.send('refresh-page');
                    Bunghole.saved = false;
                    Bunghole.mainWindow.setDocumentEdited(true);
                } else {
                    dialog.showErrorBox('Error', data.reason || 'Failed to toggle manual mark');
                }
            },
            (reason: string) => {
                dialog.showErrorBox('Error', reason);
            }
        );
    }

    static moveTargetSegment(arg: any, direction: 'up' | 'down'): void {
        const endpoint = direction === 'up' ? '/moveTargetUp' : '/moveTargetDown';
        this.sendRequest(endpoint, { segmentId: arg.segmentId },
            (data: any) => {
                if (data.status === 'Success') {
                    // Refresh the current page to show reordered segments
                    Bunghole.mainWindow.webContents.send('refresh-page');
                    Bunghole.saved = false;
                    Bunghole.mainWindow.setDocumentEdited(true);
                } else {
                    dialog.showErrorBox('Error', data.reason || 'Failed to move segment');
                }
            },
            (reason: string) => {
                dialog.showErrorBox('Error', reason);
            }
        );
    }

}

// Instantiate the application
try {
    new Bunghole(process.argv);
} catch (error) {
    console.error(error);
}
