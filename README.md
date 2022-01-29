# Inline plugin for Apache Maven

This plugin inlines dependencies to a library or other application into the main artifact.

See also the [Maven site for the plugin](https://basepom.github.io/inline-maven-plugin).

## When to use this plugin

The main use case is for libraries or components that have external dependencies but should not "bleed" these dependencies into other code that wants to use
them. The plugin will take these dependencies, inline them and rewrite the POM for the main artifact to no longer refer to the dependencies that have been
inlined.

## How to use the plugin

Inline a single dependency:

```xml

<dependencies>
    <dependency>
        <groupId>org.antlr</groupId>
        <artifactId>antlr4-runtime</artifactId>
        <version>4.9.2</version>
        <scope>provided</scope>
        <optional>true</optional>
    </dependency>
</dependencies>
<build>
<plugins>
    <plugin>
        <groupId>org.basepom.maven</groupId>
        <artifactId>inline-maven-plugin</artifactId>
        <version>0.1-SNAPSHOT</version>
        <configuration>
            <prefix>relocated</prefix>
            <inlineDependencies>
                <inlineDependency>
                    <groupId>org.antlr</groupId>
                    <artifactId>antlr4-runtime</artifactId>
                </inlineDependency>
            </inlineDependencies>
        </configuration>
        <executions>
            <execution>
                <phase>package</phase>
                <goals>
                    <goal>inline</goal>
                </goals>
            </execution>
        </executions>
    </plugin>
</plugins>
</build>
```

By default, the dependency must be marked as `optional` and in scope
`provided`. The plugin will put all content of the inlined dependency under the `relocated` prefix and rewrite the code to use these relocated classes and
resources.

The `inline:inline` goal is the main goal and should be called in the `package` phase of the lifecycle.

### Supported configuration options

```xml

<configuration>
    <prefix> .... relocation prefix ...</prefix>

    <failOnNoMatch>true|false</failOnNoMatch>
    <inlinedArtifactAttached>true|false</inlinedArtifactAttached>
    <inlinedClassifierName>inlined</inlinedClassifierName>
    <outputDirectory>${project.build.directory}</outputDirectory>
    <outputJarFile>... file name ...</outputJarFile>
    <outputPomFile>... file name ...</outputPomFile>
    <pomFile>${project.file}</pomFile>
    <quiet>true|false</quiet>
    <requireOptional>true|false</requireOptional>
    <requireProvided>true|false</requireProvided>
    <replacePomFile>true|false</replacePomFile>
    <skip>true|false</skip>
    <inlineDependencies>
        <inlineDependency>
            <artifactId>... artifact id ...</artifactId>
            <groupId>... group id ...</groupId>
            <hideClasses>true|false</hideClasses>
            <transitive>true|false</transitive>
        </inlineDependency>
    </inlineDependencies>
</configuration>
```

#### Required

| Option | Type | Default | Function |
| ------ |-------------------------------------|---------|--------------------|
| `prefix` | string | - | Defined the root package for all relocated classes. |


| Option | Type | Default | Function |
| ------ |-------------------------------------|---------|-------------------------------------------------------------------|
| `failOnNoMatch` | boolean | true | Each `inlineDependency` item must match a project dependency. Fail the build otherwise. |
| `inlinedArtifactAttached` | boolean | false | |
| `inlinedClassifierName` | string `inlined` | | |
| `outputDirectory` | string | `${project.build.directory}` | |
| `outputJarFile` | string | - | Sets an explicit output file for the rewritten jar file. If unused, write the jar in the project build directory using the `inlinedClassifierName` classifier. |
| `outputPomFile` | string | - | Sets an explicit output file for the rewritten pom file. If unused, write the POM in the project build directory as `new-pom.xml`. |
| `pomFile` | string | `${project.file}` | The POM file for the project. This will be read to create the rewritten POM. |
| `quiet` | boolean | false | If true, do not output any information besides errors or warnings. |
| `requireOptional` | boolean | true | If true, dependencies to inline must be marked as `optional` in the POM. |
| `requireProvided` | boolean | true | If true, dependencies to inline must be in `provided` scope. |
| `replacePomFile` | boolean | true | Replace the POM file in the build cycle with the rewritten POM file. This does *NOT* rewrite the POM file on disk but uses it for all subsequent steps in the build cycle (including `install` and `deploy`). |
| `skip` | boolean | false | If true, skips execution of the plugin. |
| `inlineDependencies` | list of `inlineDependency` elements | - | see below. |


Defining dependencies to inline with `inlineDependency`:

| Option | Type | Default | Function |
| ------ | ---- | ------- | -------- |
| `groupId` | string | - | Defines the group id for a dependency. |
| `artifactId` | string | - | Defines the artifact id for a dependency. |
| `hideClasses` | boolean | false | If true, rewrites all classes in a jar to be not visible for IDE autocompletion. |
| `transitive` | boolean | false | If true, also inline all transitive dependencies. If false, transitive dependencies are added to the rewritten POM file as direct dependencies. |


## When not to use this plugin

There is a wealth of other plugins that do similar things. This plugin is *NOT* intended to create executable jars, all-in-one deployable services or support a large number of customizations. For any of those, better choices exist, e.g.

* [Maven Shade Plugin](https://maven.apache.org/plugins/maven-shade-plugin/)
* [Maven Assembly Plugin](https://maven.apache.org/plugins/maven-assembly-plugin/)
* [Spring Boot Maven Plugin](https://docs.spring.io/spring-boot/docs/current/maven-plugin/reference/htmlsingle/)
* [JarJar plugin](https://sonatype.github.io/jarjar-maven-plugin/)

Each of those is a good choice for use cases that are not covered by the this plugin.
