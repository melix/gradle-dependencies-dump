/*
 * Copyright 2003-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package me.champeau.gradle.deps;

import groovy.json.JsonOutput;
import org.gradle.api.DefaultTask;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.result.DependencyResult;
import org.gradle.api.artifacts.result.ResolvedDependencyResult;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class GenerateDependencyGraph extends DefaultTask {

    @Internal
    abstract ListProperty<Configuration> getConfigurations();

    @InputFiles
    Set<File> getFiles() {
        return getConfigurations().get()
                .stream()
                .flatMap(conf -> conf.getIncoming().getFiles().getFiles().stream())
                .collect(Collectors.toSet());
    }

    @OutputFile
    abstract RegularFileProperty getJsonFile();

    @TaskAction
    void generate() throws IOException {
        Map<String, List<Map<String, Object>>> result = new LinkedHashMap<>();
        getConfigurations().get().forEach(config -> result.put(config.getName(),
                config.getIncoming()
                        .getResolutionResult()
                        .getRoot()
                        .getDependencies()
                        .stream()
                        .map(d -> walk(d, new HashSet<>()))
                        .collect(Collectors.toList())
        ));
        Files.writeString(getJsonFile().getAsFile().get().toPath(), JsonOutput.prettyPrint(JsonOutput.toJson(result)));
    }

    static Map<String, Object> walk(DependencyResult dep, Set<DependencyResult> seen) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("requested", dep.getRequested().getDisplayName());
        if (dep instanceof ResolvedDependencyResult) {
            ResolvedDependencyResult resolved = (ResolvedDependencyResult) dep;
            result.put("resolved", resolved.getResolvedVariant().getDisplayName());
            if (seen.add(dep)) {
                List<Map<String, Object>> dependencies = resolved.getSelected().getDependencies().stream().map(r -> walk(r, seen)).collect(Collectors.toList());
                result.put("dependencies", dependencies);
            } else {
                result.put("alreadySeen", true);
            }
        } else {
            result.put("resolved", false);
        }
        return result;
    }
}
