# Dumping Gradle dependency graphs

This repository demonstrates several ways to get a dependency graph dump out of Gradle projects.

First thing to understand is that in Gradle there is no single dependency graph: there's for example one dependency graph for compilation, one for runtime, one for test, etc.
Those are represented by the notion of `Configuration`.
A dependency graph is basically a `Configuration` which has the flag `canBeResolved` set to true, which has been resolved.

Therefore, our goal is to generate a JSON file which, for a project, dumps all resolvable configurations.

Second, a Gradle project can be a multiproject, so the dump must apply to each project.
In this example, we generate a JSON file for each project in their respective `build/` directory.

## Approach 1: using an init script

See [demo](demos/using-init/README.md).

This approach makes use of an init script, which needs to be copied by the user in their repository.

An init script will inject the required tasks transparently, and the user simply has to invoke using `./gradlew -I init.gradle generateDependencyGraph`.

Pros:
   - Decorrelates the user build from the plugin logic
   - the init script can be added by 3rd party tools (this is typically how IDEs inject behavior in Gradle builds to support their features)
   - doesn't know about the project structure, so has to do things like "allprojects" even if it doesn't make sense

Cons:
    - If the init script changes, everyone has to get the new version
    - Has to be self contained, which often means that the script is "quick and dirty"

## Approach 2: using a remote file

See [demo](demos/using-remote/README.md).

This approach reuses the init script from above, but makes it possible to apply it from a remote location.

In this case, the script **must** be applied in the root project, via an `apply from:` directive.

Pros:
    - Decorrelates the user build from the plugin logic
    - the script contents can evolve over time without having to update the project
    - the script itself can be applied via an init script

Cons:
    - requires updating the builds
    - can't be applied from command-line
    - has to be self-contained, which often means that the script is "quick and dirty"

## Approach 3: using a published plugin

See [demo](demos/using-published-plugin/README.md).

Starting from now, all approaches share the same plugin code.
It's a full fledged plugin.
In the first case, we assume that the plugin is published on Gradle plugin portal.
If it's not the case, the plugin can still be applied if it's published on a Maven repository, granted some user configuration.

Pros:
    - much cleaner approach than the init script
    - each project can be configured independently and tell if they need dependencies to be dumped

Cons:
    - each project of a multi-project build has to apply the plugin in order to dump their dependencies

We listed the fact that the plugin can be applied to a single project of a multi-project build as both a pro and a con.
In practice, it will often be the case that for an application, we only care about a single project of the multi-project build (e.g, the web application).

Because this plugin is actually _not_ published, the demo cannot work, which leads us to the next approach.

## Approach 4: using an unpublished plugin

This approach makes use of the plugin above, but it doesn't need to be published: using the [includegit gradle plugin](https://melix.github.io/includegit-gradle-plugin/latest/index.html) we can actually include the plugin in the build without having to publish it.
