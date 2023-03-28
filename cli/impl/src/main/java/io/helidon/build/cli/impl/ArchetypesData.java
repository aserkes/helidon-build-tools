/*
 * Copyright (c) 2023 Oracle and/or its affiliates.
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

package io.helidon.build.cli.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import io.helidon.build.common.Lists;
import io.helidon.build.common.maven.MavenVersion;
import io.helidon.build.common.maven.VersionRange;

/**
 * Information about archetypes versions.
 */
class ArchetypesData {

    private final static Pattern MAVEN_PATTERN = Pattern.compile("^(?<major>[0-9]+).+$");
    private final List<Version> versions;
    private final List<Rule> rules;
    private MavenVersion latestVersion;

    private ArchetypesData(Builder builder) {
        this.versions = builder.versions;
        this.rules = builder.rules;
    }

    List<MavenVersion> latestMajorVersions(List<String> versions) {
        Map<String, List<String>> majorVersionsMap = versions.stream().collect(Collectors.groupingBy(this::groupVersions));
        Map<VersionRange, List<MavenVersion>> versionRangeListMap =
                majorVersionsMap.entrySet().stream()
                                .filter(entry -> !entry.getKey().isEmpty())
                                .collect(Collectors.toMap(
                                        e -> VersionRange.createFromVersionSpec("[" + e.getKey() + ",)"),
                                        e -> Lists.map(e.getValue(), MavenVersion::toMavenVersion))
                                );
        return versionRangeListMap.entrySet().stream()
                                  .map(this::latestVersion)
                                  .collect(Collectors.toList());
    }

    List<MavenVersion> latestMajorVersions() {
        List<String> versionsId = Lists.map(versions, Version::id);
        return latestMajorVersions(versionsId);
    }

    private MavenVersion latestVersion(Map.Entry<VersionRange, List<MavenVersion>> entry) {
        MavenVersion mavenVersion = entry.getKey().matchVersion(entry.getValue());
        if (mavenVersion == null) {
            return entry.getValue().get(entry.getValue().size() - 1);
        }
        return mavenVersion;
    }

    private String groupVersions(String version) {
        Matcher matcher = MAVEN_PATTERN.matcher(version);
        if (matcher.find()) {
            return matcher.group("major");
        }
        return "";
    }

    MavenVersion latestVersion(List<String> versions) {
        VersionRange versionRange = VersionRange.createFromVersionSpec("[0,)");
        List<MavenVersion> mavenVersions = Lists.map(versions, MavenVersion::toMavenVersion);
        return versionRange.matchVersion(mavenVersions);
    }

    MavenVersion latestVersion() {
        if (latestVersion != null) {
            return latestVersion;
        }
        List<MavenVersion> mavenVersions = versions.stream().map(version -> MavenVersion.toMavenVersion(version.id()))
                                                   .collect(Collectors.toList());
        VersionRange versionRange = VersionRange.createFromVersionSpec("[0,)");
        latestVersion = versionRange.matchVersion(mavenVersions);
        return latestVersion;
    }

    List<Version> versions() {
        return versions;
    }

    List<Rule> rules() {
        return rules;
    }

    /**
     * Create a new builder.
     *
     * @return builder
     */
    static Builder builder() {
        return new Builder();
    }

    /**
     * ArchetypesData builder.
     */
    static class Builder {

        private List<Version> versions = new ArrayList<>();
        private List<Rule> rules = new ArrayList<>();

        private Builder() {
        }

        List<Version> versions() {
            return versions;
        }

        List<Rule> rules() {
            return rules;
        }

        void addVersion(Version version) {
            versions.add(version);
        }

        void addRule(Rule rule) {
            rules.add(rule);
        }

        /**
         * Create new instance of {@link ArchetypesData}.
         *
         * @return version
         */
        ArchetypesData build() {
            return new ArchetypesData(this);
        }
    }

    static class Version {
        private final String id;
        private final boolean isDefault;

        Version(String id) {
            this.id = id;
            this.isDefault = false;
        }

        Version(String id, boolean isDefault) {
            this.id = id;
            this.isDefault = isDefault;
        }

        String id() {
            return id;
        }

        boolean isDefault() {
            return isDefault;
        }
    }

    static class Rule {
        private final VersionRange archetypeRange;
        private final VersionRange cliRange;

        Rule(VersionRange archetypeRange, VersionRange cliRange) {
            this.archetypeRange = archetypeRange;
            this.cliRange = cliRange;
        }

        VersionRange archetypeRange() {
            return archetypeRange;
        }

        VersionRange cliRange() {
            return cliRange;
        }
    }
}
