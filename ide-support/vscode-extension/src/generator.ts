/*
 * Copyright (c) 2021, 2022 Oracle and/or its affiliates. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import * as path from 'path';
import { InputBox, QuickInput, QuickInputButtons, QuickPick, QuickPickItem, Uri } from 'vscode';
import { getSubstringBetween, QuickPickData, QuickPickItemExt } from "./common";
import { VSCodeAPI } from "./VSCodeAPI";
import { FileSystemAPI } from "./FileSystemAPI";
import { ChildProcessAPI } from "./ChildProcessAPI";
import { BaseCommand, GeneratorData, OptionCommand, TextCommand } from "./GeneratorCommand";

export interface ProjectData extends QuickPickItem {
    base: string;
    flavor: string;
    pkg: string;
}

export async function showHelidonGenerator(extensionPath: string, steps: any) {

    const PROJECT_READY: string = 'Your new project is ready.';
    const NEW_WINDOW: string = 'Open in new window';
    const CURRENT_WINDOW: string = 'Open in current window';
    const ADD_TO_WORKSPACE: string = 'Add to current workspace';

    const SELECT_FOLDER = 'Select folder';
    const OVERWRITE_EXISTING = 'Overwrite';
    const NEW_DIR = 'Choose new directory';
    const EXISTING_FOLDER = ' already exists in selected directory.';

    function notEmptyValidator(value: string): string | undefined {
        if (value) {
            return undefined;
        }
        return "Value cannot be empty.";
    }

    async function generateProject(projectData: Map<string, string>) {
        const artifactId = <string>projectData.get('artifactId');
        const targetDir = await obtainTargetFolder(artifactId);
        if (!targetDir) {
            throw new Error('Helidon project generation has been canceled.');
        }

        VSCodeAPI.showInformationMessage('Your Helidon project is being created...');

        const archetypeValues = prepareProperties(projectData);
        const cmd = `java -jar ${extensionPath}/target/cli/helidon.jar init --batch \
            --reset --url file://${extensionPath}/cli-data \
            ${archetypeValues}`;

        const channel = VSCodeAPI.createOutputChannel('helidon');
        channel.appendLine(cmd);

        const opts = {
            cwd: targetDir.fsPath
        };
        ChildProcessAPI.execProcess(cmd, opts, (error: string, stdout: string, stderr: string) => {
            channel.appendLine(stdout);
            if (stdout.includes("Switch directory to ")) {
                const projectDir = getSubstringBetween(stdout, "Switch directory to ", "to use CLI");
                VSCodeAPI.showInformationMessage('Project generated...');
                openPreparedProject(projectDir);
            } else {
                VSCodeAPI.showInformationMessage('Project generation failed...');
            }
            if (stderr) {
                channel.appendLine(stderr);
            }
            if (error) {
                channel.appendLine(error);
            }
        });
    }

    function prepareProperties(propsMap: Map<string, string>): string {
        let result = "";
        for (let [name, value] of propsMap) {
            result += ` -D${name}=${value}`;
        }
        return result;
    }

    async function obtainTargetFolder(artifactId: string) {

        const specificFolderMessage = `'${artifactId}'` + EXISTING_FOLDER;
        let directory: Uri | undefined = await VSCodeAPI.showOpenFolderDialog({openLabel: SELECT_FOLDER});

        while (directory && FileSystemAPI.isPathExistsSync(path.join(directory.fsPath, artifactId))) {
            const choice: string | undefined = await VSCodeAPI.showWarningMessage(specificFolderMessage, OVERWRITE_EXISTING, NEW_DIR);
            if (choice === OVERWRITE_EXISTING) {
                // Following line deletes target folder recursively
                require("rimraf").sync(path.join(directory.fsPath, artifactId));
                break;
            } else if (choice === NEW_DIR) {
                directory = await VSCodeAPI.showOpenFolderDialog({openLabel: SELECT_FOLDER});
            } else {
                directory = undefined;
                break;
            }
        }

        return directory;
    }

    async function openPreparedProject(projectDir: string): Promise<void> {

        const openFolderCommand = 'vscode.openFolder';
        const newProjectFolderUri = getNewProjectFolder(projectDir);

        if (VSCodeAPI.getWorkspaceFolders()) {
            const input: string | undefined = await VSCodeAPI.showInformationMessage(PROJECT_READY, NEW_WINDOW, ADD_TO_WORKSPACE);
            if (!input) {
                return;
            } else if (input === ADD_TO_WORKSPACE) {
                VSCodeAPI.updateWorkspaceFolders(
                    VSCodeAPI.getWorkspaceFolders() ? VSCodeAPI.getWorkspaceFolders()!.length : 0,
                    undefined,
                    {uri: newProjectFolderUri}
                );
            } else {
                await VSCodeAPI.executeCommand(openFolderCommand, newProjectFolderUri, true);
            }
        } else if (VSCodeAPI.getVisibleTextEditors().length > 0) {
            // If VS does not have any project opened, but has some file opened in it.
            const input: string | undefined = await VSCodeAPI.showInformationMessage(PROJECT_READY, NEW_WINDOW, CURRENT_WINDOW);
            if (input) {
                await VSCodeAPI.executeCommand(openFolderCommand, newProjectFolderUri, NEW_WINDOW === input);
            }
        } else {
            await VSCodeAPI.executeCommand(openFolderCommand, newProjectFolderUri, false);
        }

    }

    function getNewProjectFolder(projectDir: string): Uri {
        return Uri.file(projectDir);
    }

    let generatorData: GeneratorData = {steps: [], elements: [], currentElementIndex: 0};
    let commandHistory: BaseCommand [] = [];
    // const steps: any[] = [];
    // const elements: any[] = [];

    function getChildren(children: any[]): any[] {
        const result: any [] = [];
        for (let child of children) {
            //todo need more checks (if element does not exist in the array) and recursive call for selected options
            //todo !!! we do not need recursion lets use only top level elements in the steps and add new elements if they are selected
            //todo !!! use command pattern
            generatorData.elements.push(child);
        }
        return result;
    }

    function init(initialTree: any) {
        if (!initialTree) {
            return;
        }
        if (initialTree.type === 'step-element') {
            if (initialTree.children) {
                if (initialTree.children.filter((child: any) => child.type === 'step-element').length > 0) {
                    generatorData.steps.push(...initialTree.children);
                } else {
                    generatorData.steps.push(initialTree);
                }
            }
        }

        for (let step of generatorData.steps) {
            if (step.children != null) {
                const childElements = getChildren(step.children);
                if (childElements.length > 0) {
                    generatorData.elements.push(childElements);
                }
            }
        }
    }

    function prepareQuickPickData(element: any, currentStep: number, totalSteps: number): QuickPickData {
        let options: QuickPickItemExt [] = [];
        if (element.type === 'boolean-element') {
            options.push(
                {label: 'yes', children: element.children, value: 'true'},
                {label: 'no', value: 'false'});
        } else {
            options.push(...element.options.map((o: any) => {
                return {
                    label: o.label,
                    children: o.children,
                    value: o.value
                }
            }));
        }
        //todo add default value processing
        // selectedItems
        return {
            title: element.label,
            currentStep: currentStep,
            totalSteps: totalSteps,
            placeholder: element.name ?? "",
            items: options
        };
    }

    function getTextInput(element: any, resolve: any, reject: any): InputBox {
        const data = {
            title: element.label,
            placeholder: element.defaultValue ?? "",
            value: element.defaultValue ?? "",
            prompt: element.label,
            totalSteps: generatorData.elements.length,
            currentStep: generatorData.currentElementIndex + 1,
            messageValidation: notEmptyValidator
        };
        const inputBox = VSCodeAPI.createInputBox(data);
        inputBox.buttons = [QuickInputButtons.Back]

        inputBox.show();
        let optionCommand = new TextCommand(generatorData);

        inputBox.onDidAccept(async () => {
            const insertedValue = inputBox.value;
            const validation: string | undefined = await data.messageValidation(insertedValue);
            if (validation) {
                inputBox.validationMessage = validation;
            } else {
                element.value = insertedValue;
                generatorData = optionCommand.execute();
                commandHistory.push(optionCommand);
                inputBox.dispose();
                resolve(insertedValue);
            }
        });
        inputBox.onDidChangeValue(async text => {
            const validation: string | undefined = await data.messageValidation(text);
            inputBox.validationMessage = validation;
        });
        inputBox.onDidTriggerButton(item => {
            if (item === QuickInputButtons.Back) {
                resolve(inputBox);
                if (commandHistory.length !== 0) {
                    generatorData = commandHistory.pop()!.undo();
                }
            }
        });
        inputBox.onDidHide(() => {
            inputBox.dispose();
        });

        return inputBox;
    }

    function getQuickPickInput(element: any, canSelectMany: boolean, resolve: any, reject: any): QuickPick<any> {
        const data = prepareQuickPickData(element, generatorData.currentElementIndex + 1, generatorData.elements.length);
        const quickPick = VSCodeAPI.createQuickPick(data);
        quickPick.canSelectMany = canSelectMany;
        quickPick.buttons = [QuickInputButtons.Back]

        quickPick.show();
        let optionCommand = new OptionCommand(generatorData);

        quickPick.onDidAccept(async () => {
            if (quickPick.selectedItems) {
                element.selectedValues = quickPick.selectedItems.map(item => (<QuickPickItemExt>item).value);
                let children: any[] = [];
                for (let item of quickPick.selectedItems) {
                    let option = <QuickPickItemExt>item;
                    if (option.children) {
                        children.push(...option.children);
                    }
                }
                optionCommand.selectedOptions(children);
                generatorData = optionCommand.execute();
                commandHistory.push(optionCommand);
                resolve(quickPick.selectedItems);
            }
        });
        quickPick.onDidTriggerButton(item => {
            if (item === QuickInputButtons.Back) {
                resolve(quickPick);
                if (commandHistory.length !== 0) {
                    generatorData = commandHistory.pop()!.undo();
                }
            }
        });
        quickPick.onDidHide(() => {
            quickPick.dispose();
        });
        return quickPick;
    }

    function getInput(element: any): Promise<QuickInput> | undefined {
        let result: Promise<QuickInput> | undefined;
        if (element.type === 'enum-element') {
            result = new Promise<QuickInput>((resolve, reject) => getQuickPickInput(element, false, resolve, reject));
        }
        if (element.type === 'list-element') {
            result = new Promise<QuickInput>((resolve, reject) => getQuickPickInput(element, true, resolve, reject));
        }
        if (element.type === 'boolean-element') {
            result = new Promise<QuickInput>((resolve, reject) => getQuickPickInput(element, false, resolve, reject));
        }
        if (element.type === 'text-element') {
            result = new Promise<QuickInput>((resolve, reject) => getTextInput(element, resolve, reject));
        }
        return result;
    }

    function prepareProjectData() {
        const result: Map<string, string> = new Map();
        for (let element of generatorData.elements) {
            let value: string = "";
            if (element.type === 'enum-element' || element.type === 'boolean-element') {
                value = element.selectedValues[0];
            } else if (element.type === 'list-element') {
                value = element.selectedValues.join(",")
            } else if (element.type === 'text-element') {
                value = element.value;
            }
            result.set(element.name, value);
        }
        return result;
    }

    try {
        init(steps);
        let maxIterationCount = 100;
        let currentInput: Promise<QuickInput> | undefined;
        while (generatorData.currentElementIndex < generatorData.elements.length) {
            let element = generatorData.elements[generatorData.currentElementIndex];
            currentInput = getInput(element);
            if (currentInput != null) {
                let result = await currentInput;
                console.log(result);
            }
            currentInput = undefined;
            if (maxIterationCount-- < 0) {
                break;
            }
        }

        await generateProject(prepareProjectData());
    } catch (e: any) {
        VSCodeAPI.showErrorMessage(e.message);
    }

}
