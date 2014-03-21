Welcome to the cascading-release-maven-plugin wiki!

Most Maven users use the maven-release-plugin and for most people it works pretty well.
Although, on bigger projects, you sometimes need something more.

# Problem

Let's say you have a project with the following structure:
* [project root]
    * api
        * api-1
        * api-2
        * api-3
        * api-4
        * api-5
        * api-6
    * utilities
        * util-1
        * util-2
        * util-3
    * service
        * service-api
        * service-impl
        * service-rest
        * service-jms
    * framework
        * framework-jar
        * framework-war
        * framework-ear
        * framework-config
    * etc
        * etc-1
    * and-some-more-modules
        * 1
        * 2

Each "module" has it's own lifecycle and versioning scheme, so for example api-1 can have version 2.5 and api-2 can have version 1.2, etc.

And let's say that there are dependencies from one to the other module. It then quickly becomes a pain to work in this project, either because you have to release individual modules very often, or because you have a lot of work (at the end of your Sprint) making the release.

This should be easier and this is where this plugin steps in. It allows you to have SNAPSHOT dependencies among your modules and when you would like to make a release, it automatically finds out which modules need to be released, releases them, updates dependency versions in the dependent poms, and all of this in the correct order.

# Workings
Basically what you would like to release is an EAR, maybe with some config files and maybe accompanied with some docs, like an install guide, release notes, etc.

Next to the EAR POM.xml then you need to specify a JSON file which contains some meta information about the project you would like to release. Below is a small example of such a file:

    {
        "name": "Example releasable module",
        "parentPathFromBase": "parent",
        "pathToBase": "../../",
        "earPathFromBase": "framework/framework-ear",
        "dependenciesSorted": [
            {
                "groupId": "nl.jkva.api",
                "artifactId": "api1",
                "pathFromBase": "api/api1"
            },
            {
                "groupId": "nl.jkva.api",
                "artifactId": "api2",
                "pathFromBase": "api/api2"
            },
            {
                "groupId": "nl.jkva.api",
                "artifactId": "api3",
                "pathFromBase": "api/api3"
            }
        ]
    }

TODO: This file is a mess and some things are probably unneeded. Determine whether or not to keep this config in here.

If you have this file, all you have to do, is run the plugin from your EAR directory, like this:
mvn nl.jkva:cascading-release-maven-plugin:1.0-SNAPSHOT:cascading-release -Dversion=Windows7

The version (currently mandatory) can be used for specifying a functional version for your release. Currently this parameter doesn't do anything.

# Prerequisites
* You have Maven and SVN installed (SVN is currently the only supported VCS, Git is underway, but I personally need SVN support for now).
* You have an environment variable called M2_BIN and SVN_BIN, which point to respectively the Maven (like /usr/share/maven3/bin/mvn) and the SVN executables (like /opt/local/bin/svn).
* In your parent pom (if any) you don't specify the versions of any modules you would like to release (which is bad practice anyway). This means not just <dependencies> or <dependencyManagement>, but also any <properties> that you refer to in your child poms.
* You have no local modifications in your working copy (the plugin checks for this when it starts).
* You're mentally prepared to hand over the responsibility of releasing automatically, instead of manually.

# Disclaimer
This is just a first attempt for such a plugin. Just a couple of hours of work went into it and there are still a lot of bugs, so use at your own risk (for now).
