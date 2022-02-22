# Inline plugin for Apache Maven

This plugin inlines dependencies to a library or other application into the main artifact.

See also the [Maven site for the plugin](https://basepom.github.io/inline-maven-plugin).

## When to use this plugin

Easily create components that have external dependencies but should not "bleed" these dependencies into other code that wants to use
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
    </dependency>
</dependencies>
<build>
<plugins>
    <plugin>
        <groupId>org.basepom.maven</groupId>
        <artifactId>inline-maven-plugin</artifactId>
        <version>1.0</version>
        <configuration>
            <prefix>relocated</prefix>
            <inlineDependencies>
                <inlineDependency>
                    <artifact>org.antlr:antlr4-runtime</artifact>
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

The plugin will put all content of the inlined dependency under the `relocated` prefix and rewrite the code to use these relocated classes and resources.

The `inline:inline` goal is the main goal and should be called in the `package` phase of the lifecycle.

### Supported configuration options

```xml
<configuration>
    <inlineDependencies>
        <inlineDependency>
            <artifact> ...groupId:artifactId... </artifact>
            <inlineTransitive>true|false</inlineTransitive>
            <inlineOptionals>true|false|</inlineOptionals>
        </inlineDependency>
        ....
    </inlineDependencies>

    <prefix> ...relocation prefix... </prefix>

    <includes>
        <include> ...groupId:artifactId... </include>
        ....
    </includes>

    <excludes>
        <exclude> ...groupId:artifactId... </exclude>
        ....
    </excludes>

    <failOnDuplicate>true (false)</failOnDuplicate>
    <failOnNoMatch>true (false)</failOnNoMatch>
    <hideClasses>true (false)</hideClasses>
    <inlinedArtifactAttached>false (true)</inlinedArtifactAttached>
    <quiet>false (true)</quiet>
    <requireOptional>false (true)</requireOptional>
    <requireProvided>false (true)</requireProvided>
    <replacePomFile>true (false)</replacePomFile>
    <skip>false (true)</skip>

    <inlinedClassifierName>inlined</inlinedClassifierName>
    <outputJarFile> ...file name... </outputJarFile>
    <outputPomFile> ...file name... </outputPomFile>

    <additionalProcessors>
        <additionalProcessor> ... class name of processor... </additionalProcessor>
        ....
    <additionalProcessors>

    <outputDirectory>${project.build.directory}</outputDirectory>
    <pomFile>${project.file}</pomFile>
</configuration>
```

#### Required configuration paramters

| Option | Type | Default | Function |
| ------ |-------------------------------------|---------|--------------------|
| `prefix` | string | - | Defined the root package for all relocated classes. |

#### Optional configuration parameters

| Option | Type | Default | Function |
| ------ |-------------------------------------|---------|-------------------------------------------------------------------|
| `failOnDuplicate` | boolean | `true` | Any duplicate entry in the rewritten jar file will fail the build. If `false`, duplicates will be discarded. |
| `failOnNoMatch` | boolean | `true` | Each `inlineDependency` item must match a project dependency. Fail the build otherwise. |
| `hideClasses` | boolean | `true` | If true, rewrites all classes in a jar to be not visible for IDE auto-completion. |
| `inlinedArtifactAttached` | boolean | `false` | If true, attach the rewritten jar using the `inlinedClassifierName`, otherwise replace the main artifact. |
| `inlinedClassifierName` | string | `inlined` | If the rewritten jar gets attached, use this value as the classifier. |
| `outputDirectory` | string | `${project.build.directory}` | The plugin writes the rewritten jar file in this directory. |
| `outputJarFile` | string | - | Sets an explicit output file for the rewritten jar file. If unused, write the jar in the project build directory using the `inlinedClassifierName` classifier. |
| `outputPomFile` | string | - | Sets an explicit output file for the rewritten pom file. If unused, write the POM using the name of the original POM file, prefixed with `new-`. |
| `pomFile` | string | `${project.file}` | The POM file for the project. This will be read to create the rewritten POM. |
| `quiet` | boolean | `false` | If true, do not output any information besides errors or warnings. |
| `requireOptional` | boolean | `false` | If true, dependencies to inline must be marked as `optional` in the POM. |
| `requireProvided` | boolean | `false` | If true, dependencies to inline must be in `provided` scope. |
| `replacePomFile` | boolean | `true` | Replace the POM file in the build cycle with the rewritten POM file. This does *NOT* rewrite the POM file on disk but uses it for all subsequent steps in the build cycle (including `install` and `deploy`). |
| `skip` | boolean | `false` | If true, skips execution of the plugin. |
| `inlineDependencies` | list of `inlineDependency` elements | - | see below. |
| `includes` | explicit list of dependencies to include | - | see below. |
| `excludes` | explicit list of dependencies to exclude | - | see below. |
| `additionalProcessors` | list of class names | - | Additional jar processors to rewrite the jar. See [Additional Processors] for more information.


#### Defining dependencies to inline with `inlineDependencies`:

| Option | Type | Default | Function |
| ------ | ---- | ------- | -------- |
| `artifact` | string | - | Defines the artifact to inline. Must be given as `groupId:artifactId`. |
| `inlineTransitive` | boolean | `false` | If true, also inline all transitive dependencies. If false, transitive dependencies are added to the rewritten POM file as direct dependencies. |
| `inlineOptionals` | boolean `false` | If true, add all optional, transitive dependencies as well. If false, ignore optional dependencies. |


#### Including and excluding dependencies

It should not be necessary to explicitly specify includes and excludes. If a dependency is inlined and has transitive dependencies that are also used by the project itself, these will be automatically excluded.

The `<includes>` and `<excludes>` options can be used in special cases. If the `includes` list is empty, everything that has been specified using `inlineDependencies` will be automatically included (direct and transitive) and any exclude will remove dependencies. If the `include` list is not empty, any transitive dependency included *must* be specified as included. If both includes and excludes are defined, the order is "included, then excluded".


## When not to use this plugin

There is a wealth of other plugins that do similar things. This plugin is *NOT* intended to create executable jars, all-in-one deployable services or support a large number of customizations. For any of those, better choices exist, e.g.

* [Maven Shade Plugin](https://maven.apache.org/plugins/maven-shade-plugin/)
* [Maven Assembly Plugin](https://maven.apache.org/plugins/maven-assembly-plugin/)
* [Spring Boot Maven Plugin](https://docs.spring.io/spring-boot/docs/current/maven-plugin/reference/htmlsingle/)
* [JarJar plugin](https://sonatype.github.io/jarjar-maven-plugin/)

Each of those is a good choice for use cases that are not covered by the this plugin.
