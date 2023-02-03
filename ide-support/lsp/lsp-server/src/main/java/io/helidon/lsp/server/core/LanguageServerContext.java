/*
 * Copyright (c) 2022, 2023 Oracle and/or its affiliates.
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

package io.helidon.lsp.server.core;

import java.util.List;

import org.eclipse.lsp4j.WorkspaceFolder;
import org.eclipse.lsp4j.services.LanguageClient;

/**
 * Context data for the running instance of the Helidon Language Server.
 */
public class LanguageServerContext {

    private static final LanguageServerContext INSTANCE = new LanguageServerContext();
    private List<WorkspaceFolder> workspaceFolders;
    private LanguageClient client;

    private LanguageServerContext() {
    }

    /**
     * Get LanguageClient.
     *
     * @return LanguageClient
     */
    public LanguageClient client() {
        return client;
    }

    /**
     * Set LanguageClient.
     *
     * @param client LanguageClient
     */
    public void client(LanguageClient client) {
        this.client = client;
    }

    /**
     * Get the instance of the class.
     *
     * @return instance of the class.
     */
    public static LanguageServerContext instance() {
        return INSTANCE;
    }

    /**
     * Get workspace folders in IDE.
     *
     * @return List of workspace folders.
     */
    public List<WorkspaceFolder> workspaceFolders() {
        return workspaceFolders;
    }

    /**
     * Set workspace folders.
     *
     * @param workspaceFolders List of workspace folders.
     */
    public void workspaceFolders(List<WorkspaceFolder> workspaceFolders) {
        this.workspaceFolders = workspaceFolders;
    }

}
