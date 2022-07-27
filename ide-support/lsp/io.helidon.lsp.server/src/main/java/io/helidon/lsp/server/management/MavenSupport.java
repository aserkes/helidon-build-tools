/*
 * Copyright (c) 2022 Oracle and/or its affiliates.
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

package io.helidon.lsp.server.management;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import io.helidon.build.common.maven.MavenCommand;

/**
 * Support operations with maven.
 */
public class MavenSupport {

    private static final Logger LOGGER = Logger.getLogger(MavenSupport.class.getName());
    private static final String POM_FILE_NAME = "pom.xml";
    private static MavenSupport instance;

    private boolean isMavenInstalled = false;

    private MavenSupport() {
        initialize();
    }

    /**
     * Return instance of the MavenSupport class (singleton pattern).
     *
     * @return Instance of the MavenSupport class.
     */
    public static MavenSupport getInstance() {
        if (instance == null) {
            instance = new MavenSupport();
        }
        return instance;
    }

    private void initialize() {
        Path mavenPath = null;
        try {
            mavenPath = MavenCommand.mavenExecutable();
        } catch (IllegalStateException e) {
            LOGGER.log(Level.SEVERE, "Maven is not installed in the system", e);
            return;
        }
        if (mavenPath != null) {
            isMavenInstalled = true;
        }
    }

    private static class MavenPrintStream extends PrintStream {

        private final List<String> content = new ArrayList<>();

        public MavenPrintStream() {
            super(new ByteArrayOutputStream());
        }

        @Override
        public void println(String string) {
            content.add(string);
        }

        @Override
        public void print(String string) {
            content.add(string);
        }

        public List<String> content() {
            return content;
        }
    }

    /**
     * Get paths for the dependency jars for the given pom file.
     *
     * @param pomPath Path to the pom file.
     * @return List that contains paths for the dependency jars.
     */
    public List<String> getDependencies(final String pomPath) {
        if (!isMavenInstalled) {
            return null;
        }

        List<String> output = new ArrayList<>();
        List<String> result = new ArrayList<>();
        String mvnCommand = "dependency:build-classpath";//build-classpath
        String dependencyMarker = "Dependencies classpath:";
        String stopDependenciesMarker = "-------------";

        MavenPrintStream mavenPrintStream = new MavenPrintStream();
        try {
            MavenCommand.builder()
                        .addArgument(mvnCommand)
                        .stdOut(mavenPrintStream)//mavenPrintStream  System.out
                        .directory(new File(pomPath).getParentFile())
                        .verbose(false)
                        .build().execute();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error when executing the maven command - " + mvnCommand, e);
            return Collections.emptyList();
        }
        for (int x = 0; x < mavenPrintStream.content().size(); x++) {
            if (mavenPrintStream.content().get(x).contains(dependencyMarker)) {
                StringBuilder dependencies = new StringBuilder();

                for (int y = x + 1; y < mavenPrintStream.content().size(); y++) {
                    String content = mavenPrintStream.content().get(y);
                    if (content.contains(stopDependenciesMarker)) {
                        break;
                    }
                    dependencies.append(content);
                }
                result.addAll(Arrays.asList(dependencies.toString().split(File.pathSeparator)));
                break;
            }
        }

        return result;
    }

    public List<String> getAllDependencies(final String pomPath) {
        if (!isMavenInstalled) {
            return null;
        }

        int[] serverPort={0};
        System.out.println("Thread Running");
        try (ServerSocket serverSocket = new ServerSocket(33133)) {
            Socket clientSocket;
            PrintWriter out;
            BufferedReader in;

            serverPort[0] = serverSocket.getLocalPort();
/////////////////////////////////////////
//            Thread thread = new Thread(){
//                public void run(){
//
//                }
//            };
//
//            thread.start();

////////////////////////////////////////

            String mvnCommand = "io.helidon.ide-support:helidon-lsp-maven-plugin:dependency-counter";//build-classpath

            MavenPrintStream mavenPrintStream = new MavenPrintStream();
            try {
                MavenCommand.builder()
                            .addArgument(mvnCommand)
                            .stdOut(mavenPrintStream)//mavenPrintStream  System.out
                            .directory(new File(pomPath).getParentFile())
                            .verbose(false)
                            .build().execute();
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error when executing the maven command - " + mvnCommand, e);
//                        return Collections.emptyList();
            }

            clientSocket = serverSocket.accept();
            out = new PrintWriter(clientSocket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            String greeting = in.readLine();
            System.out.println(greeting);
            out.println("hello client");
            in.close();
            out.close();
            clientSocket.close();

        } catch (IOException e) {
            System.out.println("Port is not available");
        }



//        try {
//            Thread.sleep(1000);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }



        return List.of();
    }

    /**
     * Get pom file for the file from the maven project.
     *
     * @param fileName File name.
     * @return Get pom file for the given file or null if pom.xml is not found.
     * @throws IOException IOException
     */
    public String getPomForFile(final String fileName) throws IOException {
        final Path currentPath = Paths.get(fileName);
        Path currentDirPath;
        if (currentPath.toFile().isDirectory()) {
            currentDirPath = currentPath;
        } else {
            currentDirPath = currentPath.getParent();
        }
        String pomForDir = findPomForDir(currentDirPath);
        while (pomForDir == null && currentDirPath != null) {
            currentDirPath = currentDirPath.getParent();
            pomForDir = findPomForDir(currentDirPath);
        }
        return pomForDir;
    }

    private String findPomForDir(final Path directoryPath) throws IOException {
        if (directoryPath == null) {
            return null;
        }
        File[] listFiles = directoryPath.toFile().listFiles();
        if (listFiles == null) {
            return null;
        }
        return Arrays.stream(listFiles)
                     .filter(file ->
                             file.isFile() && file.getName().equals(POM_FILE_NAME))
                     .findFirst()
                     .map(File::getAbsolutePath).orElse(null);
    }

}
